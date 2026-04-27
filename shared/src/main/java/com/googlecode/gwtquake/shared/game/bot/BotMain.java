/*
 * Copyright (C) 1997-2001 Id Software, Inc.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 *
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

package com.googlecode.gwtquake.shared.game.bot;

import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.ExecutableCommand;
import com.googlecode.gwtquake.shared.game.Entity;
import com.googlecode.gwtquake.shared.game.Commands;
import com.googlecode.gwtquake.shared.game.GameBase;
import com.googlecode.gwtquake.shared.game.GameUtil;
import com.googlecode.gwtquake.shared.game.PlayerClient;
import com.googlecode.gwtquake.shared.game.adapters.EntityThinkAdapter;
import com.googlecode.gwtquake.shared.server.ServerMain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central bot AI controller.
 * Equivalent to cr_main.c from quake2-crbot.
 */
public class BotMain {

    private static List<BotInfoPers> globalBots = new ArrayList<>();

    /**
     * Entity think adapter for bot AI.
     */
    public static EntityThinkAdapter thinkAdapter = new EntityThinkAdapter() {
        public String getID() { return "bot_think"; }
        public boolean think(Entity self) {
            BotMain.think(self);
            return true;
        }
    };

    /**
     * Initialize bot subsystem (called from SV_InitGame).
     */
    public static void init() {
        Com.Printf("Bot subsystem initializing...\n");

        // Register console commands
        Commands.addCommand("sv", new ExecutableCommand() {
            public void execute() {
                BotCommands.serverCommand();
            }
        });

        // Clear path nodes
        PathNode.clearAll();
        globalBots.clear();

        Com.Printf("Bot subsystem initialized\n");
    }

    /**
     * Main bot think loop (called once per frame from G_RunFrame).
     */
    public static void think(Entity bot) {
        if (bot.botInfo == null) {
            return;  // Not a bot
        }

        // Find enemies
        findEnemy(bot);

        // Combat
        combat(bot);

        // Find pickups
        findPickups(bot);

        // Update movement (roaming for now)
        updateMovement(bot);

        // Set elapsed time for movement physics
        bot.client.userCommand.msec = 100;  // FRAMETIME in milliseconds

        // Execute the bot's command through the player movement system
        PlayerClient.ClientThink(bot, bot.client.userCommand);

        // Reschedule for next frame
        bot.nextthink = GameBase.level.time + Constants.FRAMETIME;
    }

    /**
     * Aim bot at current enemy with predictive leading.
     */
    private static void aimAtEnemy(Entity bot) {
        if (bot.enemy == null) return;

        float[] dir = new float[3];
        dir[0] = bot.enemy.s.origin[0] - bot.s.origin[0];
        dir[1] = bot.enemy.s.origin[1] - bot.s.origin[1];
        dir[2] = bot.enemy.s.origin[2] - bot.s.origin[2];

        // Simple predictive leading
        float dist = (float)Math.sqrt(dir[0]*dir[0] + dir[1]*dir[1] + dir[2]*dir[2]);
        float timeToHit = dist / 1000f;  // Projectile speed estimate

        float[] predicted = new float[3];
        predicted[0] = bot.enemy.s.origin[0] + bot.enemy.velocity[0] * timeToHit;
        predicted[1] = bot.enemy.s.origin[1] + bot.enemy.velocity[1] * timeToHit;
        predicted[2] = bot.enemy.s.origin[2] + bot.enemy.velocity[2] * timeToHit;

        // Calculate angles to predicted position
        dir[0] = predicted[0] - bot.s.origin[0];
        dir[1] = predicted[1] - bot.s.origin[1];
        dir[2] = predicted[2] - bot.s.origin[2];

        float yaw = (float)Math.atan2(dir[1], dir[0]) * 180f / (float)Math.PI;
        float pitch = (float)Math.atan2(-dir[2], Math.sqrt(dir[0]*dir[0] + dir[1]*dir[1])) * 180f / (float)Math.PI;

        bot.client.ps.viewangles[0] = pitch;
        bot.client.ps.viewangles[1] = yaw;
    }

    /**
     * Combat logic - aim and shoot at enemy.
     */
    private static void combat(Entity bot) {
        if (bot.enemy == null) return;

        BotInfo bi = bot.botInfo;

        // Aim at enemy
        aimAtEnemy(bot);

        // Shoot with skill-based accuracy
        if (bi.timeNextShot < GameBase.level.time) {
            float accuracy = 0.5f + (bot.botPers.skill * 0.15f);

            if (Math.random() < accuracy) {
                bot.client.userCommand.buttons |= Constants.BUTTON_ATTACK;
                bi.timeNextShot = GameBase.level.time + 0.1f;
            }
        }

        // Strafe evasion - switch direction every 0.5-1.0 seconds
        if (bi.timeLastStrafeSwitch < GameBase.level.time) {
            bi.strafeDir = -bi.strafeDir;
            bi.timeLastStrafeSwitch = GameBase.level.time + 0.5f + (float)Math.random() * 0.5f;
        }
    }

    /**
     * Scan for visible enemies and select best target.
     */
    private static void findEnemy(Entity bot) {
        BotInfo bi = bot.botInfo;

        // Throttle enemy search based on skill
        if (bi.timeNextEnemy > GameBase.level.time) {
            return;
        }

        bi.timeNextEnemy = GameBase.level.time + 0.1f * (4 - bot.botPers.skill);

        Entity bestEnemy = null;
        float bestScore = 0;

        // Scan all players
        for (int i = 1; i <= ServerMain.maxclients.value; i++) {
            Entity target = GameBase.g_edicts[i];

            if (!target.inuse || target == bot) continue;
            if (target.health <= 0) continue;
            if (target.botInfo != null) continue;  // Don't attack bots (yet)

            // Check visibility
            if (!GameUtil.visible(bot, target)) continue;

            // Score target (closer = better, wounded = higher priority)
            float dx = target.s.origin[0] - bot.s.origin[0];
            float dy = target.s.origin[1] - bot.s.origin[1];
            float dz = target.s.origin[2] - bot.s.origin[2];
            float dist = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);

            float score = 1000f / (dist + 1);

            if (target.health < 50) {
                score *= 1.5f;  // Priority to wounded
            }

            if (score > bestScore) {
                bestScore = score;
                bestEnemy = target;
            }
        }

        bot.enemy = bestEnemy;
    }

    /**
     * Random roaming when bot has no target.
     */
    private static void roam(Entity bot) {
        BotInfo bi = bot.botInfo;

        // Change direction every 2-5 seconds
        if (bi.timeNextRoamDirChange < GameBase.level.time) {
            bot.client.ps.viewangles[1] = (float)(Math.random() * 360);
            bi.timeNextRoamDirChange = GameBase.level.time + 2f + (float)Math.random() * 3f;
        }

        // Move forward
        bot.client.userCommand.forwardmove = 400;
    }

    /**
     * Move bot toward target position.
     */
    private static void moveToTarget(Entity bot, float[] target) {
        float[] dir = new float[3];
        dir[0] = target[0] - bot.s.origin[0];
        dir[1] = target[1] - bot.s.origin[1];
        dir[2] = 0;  // Ignore Z

        // Calculate yaw
        float yaw = (float)Math.atan2(dir[1], dir[0]) * 180f / (float)Math.PI;
        bot.client.ps.viewangles[1] = yaw;

        // Move forward
        bot.client.userCommand.forwardmove = 400;
    }

    /**
     * Check if item is in unreachable list.
     */
    private static boolean isUnreachable(BotInfo bi, Entity item) {
        for (int i = 0; i < bi.unreachable.length; i++) {
            if (bi.unreachable[i] == item) {
                if (bi.timeUnreachable[i] > GameBase.level.time) {
                    return true;  // Still unreachable
                }
                // Expired - clear slot
                bi.unreachable[i] = null;
            }
        }
        return false;
    }

    /**
     * Score item based on bot's needs.
     */
    private static float evaluateItem(Entity bot, Entity item) {
        float score = 0;

        // Check item type and assign base score
        if (item.item == null) return 0;

        // Weapons
        if ((item.item.flags & Constants.IT_WEAPON) != 0) {
            score = 100;
        }
        // Armor
        else if ((item.item.flags & Constants.IT_ARMOR) != 0) {
            score = 80;
        }
        // Ammo
        else if ((item.item.flags & Constants.IT_AMMO) != 0) {
            score = 50;
        }
        // Powerups (includes health items, since they don't have a specific flag)
        else if ((item.item.flags & Constants.IT_POWERUP) != 0) {
            if (bot.health < 100) {
                score = 90 * (100 - bot.health) / 100f;
            } else {
                score = 70;  // Powerups still valuable
            }
        }
        // Other pickable items (health items that aren't powerups)
        else {
            if (bot.health < 100) {
                score = 85 * (100 - bot.health) / 100f;
            }
        }

        // Distance penalty
        float dx = item.s.origin[0] - bot.s.origin[0];
        float dy = item.s.origin[1] - bot.s.origin[1];
        float dz = item.s.origin[2] - bot.s.origin[2];
        float dist = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);

        score = score * 1000f / (dist + 100f);

        return score;
    }

    /**
     * Scan for items to pick up.
     */
    private static void findPickups(Entity bot) {
        if (bot.enemy != null) return;  // Don't pick up during combat

        BotInfo bi = bot.botInfo;

        if (bi.timeNextPickup > GameBase.level.time) {
            return;
        }

        bi.timeNextPickup = GameBase.level.time + 0.5f;

        Entity bestItem = null;
        float bestScore = 0;

        // Scan all entities
        for (int i = (int)ServerMain.maxclients.value + 1; i < GameBase.num_edicts; i++) {
            Entity item = GameBase.g_edicts[i];

            if (!item.inuse) continue;
            if (item.item == null) continue;
            if ((item.svflags & 1) != 0) continue;  // SVF_NOCLIENT

            if (isUnreachable(bi, item)) continue;

            float score = evaluateItem(bot, item);

            if (score > bestScore) {
                bestScore = score;
                bestItem = item;
            }
        }

        bi.pickupTarget = bestItem;
        bi.pickupTargetScore = bestScore;
    }

    /**
     * Update UserCommand based on bot intentions.
     */
    private static void updateMovement(Entity bot) {
        BotInfo bi = bot.botInfo;

        // Reset command
        bot.client.userCommand.forwardmove = 0;
        bot.client.userCommand.sidemove = 0;
        bot.client.userCommand.upmove = 0;
        bot.client.userCommand.buttons = 0;

        // Priority 1: Combat
        if (bot.enemy != null) {
            // Move forward toward enemy (aim angles set by combat())
            bot.client.userCommand.forwardmove = 400;
            bot.client.userCommand.sidemove = (short)(bi.strafeDir * 400);  // Strafe
        }
        // Priority 2: Pickup
        else if (bi.pickupTarget != null) {
            moveToTarget(bot, bi.pickupTarget.s.origin);
        }
        // Default: Roam
        else {
            roam(bot);
        }

        // Apply view angles to command
        bot.client.userCommand.angles[0] = (short)(bot.client.ps.viewangles[0] * 65536 / 360);
        bot.client.userCommand.angles[1] = (short)(bot.client.ps.viewangles[1] * 65536 / 360);
        bot.client.userCommand.angles[2] = (short)(bot.client.ps.viewangles[2] * 65536 / 360);
    }

    /**
     * Add bot to registry.
     */
    static void registerBot(BotInfoPers pers) {
        if (pers != null) {
            globalBots.add(pers);
        }
    }

    /**
     * Get all registered bots.
     */
    public static List<BotInfoPers> getAllBots() {
        return Collections.unmodifiableList(globalBots);
    }
}
