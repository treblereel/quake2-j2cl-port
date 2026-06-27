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
import com.googlecode.gwtquake.shared.game.PlayerMove;
import com.googlecode.gwtquake.shared.game.Trace;
import com.googlecode.gwtquake.shared.game.adapters.EntityDieAdapter;
import com.googlecode.gwtquake.shared.game.adapters.EntityThinkAdapter;
import com.googlecode.gwtquake.shared.game.monsters.MonsterPlayer;
import com.googlecode.gwtquake.shared.server.ServerGame;
import com.googlecode.gwtquake.shared.server.ServerInit;
import com.googlecode.gwtquake.shared.server.ServerMain;
import com.googlecode.gwtquake.shared.server.World;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.Math3D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central bot AI controller.
 * Equivalent to cr_main.c from quake2-crbot.
 */
public class BotMain {

    private static List<BotInfoPers> globalBots = new ArrayList<>();
    private static final float MOVE_SPEED = 320f;
    private static final float STRAFE_SPEED = 280f;
    private static final float CLOSE_COMBAT_RANGE = 220f;
    private static final float GOOD_COMBAT_RANGE = 650f;
    private static final float MAX_DIRECT_ITEM_RANGE = 1000f;

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
     * Bots are not backed by a real network client, so the normal player death
     * path must not open the scoreboard or send unicast messages to them.
     */
    private static int botDieAnim = 0;

    public static EntityDieAdapter dieAdapter = new EntityDieAdapter() {
        public String getID() { return "bot_die"; }
        public void die(Entity self, Entity inflictor, Entity attacker, int damage, float[] point) {
            Math3D.VectorClear(self.avelocity);

            self.takedamage = Constants.DAMAGE_YES;
            self.movetype = Constants.MOVETYPE_TOSS;
            self.s.modelindex2 = 0;
            self.s.angles[0] = 0;
            self.s.angles[2] = 0;
            self.s.sound = 0;
            self.maxs[2] = -8;
            self.svflags |= Constants.SVF_DEADMONSTER;

            if (self.deadflag == 0 && self.client != null) {
                self.client.respawn_time = GameBase.level.time + 1.0f;
                self.client.ps.pmove.pm_type = Constants.PM_DEAD;

                self.client.anim_priority = Constants.ANIM_DEATH;
                if ((self.client.ps.pmove.pm_flags & PlayerMove.PMF_DUCKED) != 0) {
                    self.s.frame = MonsterPlayer.FRAME_crdeath1 - 1;
                    self.client.anim_end = MonsterPlayer.FRAME_crdeath5;
                } else {
                    botDieAnim = (botDieAnim + 1) % 3;
                    switch (botDieAnim) {
                    case 0:
                        self.s.frame = MonsterPlayer.FRAME_death101 - 1;
                        self.client.anim_end = MonsterPlayer.FRAME_death106;
                        break;
                    case 1:
                        self.s.frame = MonsterPlayer.FRAME_death201 - 1;
                        self.client.anim_end = MonsterPlayer.FRAME_death206;
                        break;
                    case 2:
                        self.s.frame = MonsterPlayer.FRAME_death301 - 1;
                        self.client.anim_end = MonsterPlayer.FRAME_death308;
                        break;
                    }
                }

                ServerGame.PF_StartSound(self, Constants.CHAN_VOICE,
                    ServerInit.SV_SoundIndex("*death" + ((Lib.rand() % 4) + 1) + ".wav"),
                    1.0f, (float) Constants.ATTN_NORM, 0f);
            }

            self.deadflag = Constants.DEAD_DEAD;
            self.think = thinkAdapter;
            self.nextthink = GameBase.level.time + Constants.FRAMETIME;
            World.SV_LinkEdict(self);
        }
    };

    /**
     * Initialize bot subsystem (called from SV_InitGame).
     */
    public static void init() {
        Com.Printf("=== Bot subsystem initializing ===\n");

        // Register console commands individually
        try {
            Commands.addCommand("addbot", new ExecutableCommand() {
                public void execute() {
                    Com.Printf("addbot command executed\n");
                    BotCommands.addBot();
                }
            });

            Commands.addCommand("addbots", new ExecutableCommand() {
                public void execute() {
                    Com.Printf("addbots command executed\n");
                    BotCommands.addBots();
                }
            });

            Commands.addCommand("removebot", new ExecutableCommand() {
                public void execute() {
                    Com.Printf("removebot command executed\n");
                    BotCommands.removeBot();
                }
            });

            Commands.addCommand("loadnodes", new ExecutableCommand() {
                public void execute() {
                    PathNodeLoader.loadRoutes();
                }
            });

            Commands.addCommand("savenodes", new ExecutableCommand() {
                public void execute() {
                    PathNodeLoader.saveRoutes();
                }
            });

            Commands.addCommand("dumpnodes", new ExecutableCommand() {
                public void execute() {
                    PathNodeLoader.dumpNodes();
                }
            });

            Com.Printf("Bot commands registered: addbot, addbots, removebot, loadnodes, savenodes\n");
        } catch (Exception e) {
            Com.Printf("ERROR registering bot commands: " + e.getMessage() + "\n");
            e.printStackTrace();
        }

        PathNode.clearAll();
        globalBots.clear();

        BotNodeInit.initNodeNet();

        Com.Printf("=== Bot subsystem initialized ===\n");
    }

    /**
     * Main bot think loop (called once per frame from G_RunFrame).
     */
    public static void think(Entity bot) {
        if (bot.botInfo == null) {
            Com.Printf(">>> BotMain.think: botInfo is NULL for entity " + bot.index + "\n");
            return;  // Not a bot
        }

        if (bot.deadflag != 0) {
            if (bot.client != null && GameBase.level.time > bot.client.respawn_time) {
                respawnBot(bot);
            } else {
                bot.nextthink = GameBase.level.time + Constants.FRAMETIME;
            }
            return;
        }

        // Debug output once per second
        if (bot.botInfo.timeNextEnemy <= GameBase.level.time) {
            Com.Printf(">>> BotMain.think: Bot '" + bot.botPers.name + "' thinking at pos=[" +
                bot.s.origin[0] + "," + bot.s.origin[1] + "," + bot.s.origin[2] + "]\n");
        }

        findEnemy(bot);
        findPickups(bot);
        BotNavigation.updateRoutes(bot);
        updateMovement(bot);
        combat(bot);
        applyCommandAngles(bot);

        // Set elapsed time for movement physics
        bot.client.userCommand.msec = 100;  // FRAMETIME in milliseconds

        // Execute the bot's command through the player movement system
        if ((bot.client.userCommand.buttons & Constants.BUTTON_ATTACK) != 0) {
            bot.client.buttons = 0;
            bot.client.weapon_thunk = false;
        }
        PlayerClient.ClientThink(bot, bot.client.userCommand);

        // Reschedule for next frame
        bot.nextthink = GameBase.level.time + Constants.FRAMETIME;
    }

    private static void respawnBot(Entity bot) {
        BotInfoPers pers = bot.botPers;
        if (pers == null) {
            return;
        }

        PlayerClient.respawn(bot);

        bot.classname = "bot";
        bot.movetype = Constants.MOVETYPE_STEP;
        bot.die = dieAdapter;
        bot.svflags &= ~Constants.SVF_NOCLIENT;
        bot.svflags &= ~Constants.SVF_DEADMONSTER;
        bot.deadflag = Constants.DEAD_NO;
        bot.botPers = pers;
        bot.botInfo = new BotInfo();
        bot.botInfo.strafeDir = 1f;
        Math3D.VectorCopy(bot.s.origin, bot.botInfo.oldOrigin);
        bot.think = thinkAdapter;
        bot.nextthink = GameBase.level.time + Constants.FRAMETIME;

        World.SV_LinkEdict(bot);
        Com.Printf(">>> BotMain.respawnBot: Bot '" + pers.name + "' respawned\n");
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

        // Shoot with skill-based accuracy, but only while the target is still visible.
        if (GameUtil.visible(bot, bot.enemy) && bi.timeNextShot < GameBase.level.time) {
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
            if (isFriendlyBot(bot, target)) continue;

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

        bot.client.userCommand.forwardmove = (short) MOVE_SPEED;
    }

    /**
     * Move bot toward target position.
     */
    private static void moveToTarget(Entity bot, float[] target) {
        setYawToward(bot, target);

        if (distance2d(bot.s.origin, target) > 48f) {
            bot.client.userCommand.forwardmove = (short) MOVE_SPEED;
        }
    }

    private static void setYawToward(Entity bot, float[] target) {
        float dx = target[0] - bot.s.origin[0];
        float dy = target[1] - bot.s.origin[1];
        bot.client.ps.viewangles[1] = (float)Math.atan2(dy, dx) * 180f / (float)Math.PI;
    }

    private static float distance(Entity a, Entity b) {
        float dx = a.s.origin[0] - b.s.origin[0];
        float dy = a.s.origin[1] - b.s.origin[1];
        float dz = a.s.origin[2] - b.s.origin[2];
        return (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static float distance2d(float[] a, float[] b) {
        float dx = a[0] - b[0];
        float dy = a[1] - b[1];
        return (float)Math.sqrt(dx * dx + dy * dy);
    }

    private static float normalizeYaw(float yaw) {
        while (yaw >= 360f) yaw -= 360f;
        while (yaw < 0f) yaw += 360f;
        return yaw;
    }

    private static boolean isFriendlyBot(Entity bot, Entity target) {
        return bot.botPers != null
            && target.botPers != null
            && bot.botPers.teamNo != 0
            && bot.botPers.teamNo == target.botPers.teamNo;
    }

    private static Trace traceMove(Entity bot, float yaw, float distance) {
        float radians = yaw * (float)Math.PI / 180f;
        float[] end = new float[3];
        end[0] = bot.s.origin[0] + (float)Math.cos(radians) * distance;
        end[1] = bot.s.origin[1] + (float)Math.sin(radians) * distance;
        end[2] = bot.s.origin[2];
        return World.SV_Trace(bot.s.origin, bot.mins, bot.maxs, end, bot, Constants.MASK_PLAYERSOLID);
    }

    private static boolean canMove(Entity bot, float yaw, float distance) {
        Trace trace = traceMove(bot, yaw, distance);
        return !trace.allsolid && !trace.startsolid && trace.fraction > 0.85f;
    }

    private static void avoidObstacles(Entity bot) {
        if (bot.client.userCommand.forwardmove == 0 && bot.client.userCommand.sidemove == 0) {
            return;
        }

        float baseYaw = bot.client.ps.viewangles[1];
        float moveYaw = baseYaw + (float)Math.atan2(
            bot.client.userCommand.sidemove,
            bot.client.userCommand.forwardmove) * 180f / (float)Math.PI;

        if (canMove(bot, moveYaw, 72f)) {
            bot.botInfo.moveBlockCount = 0;
            return;
        }

        bot.botInfo.moveBlockCount++;

        float[] offsets = {45f, -45f, 90f, -90f, 135f, -135f, 180f};
        for (float offset : offsets) {
            float candidate = normalizeYaw(baseYaw + offset);
            if (canMove(bot, candidate, 72f)) {
                bot.client.ps.viewangles[1] = candidate;
                bot.client.userCommand.forwardmove = (short) MOVE_SPEED;
                bot.client.userCommand.sidemove = 0;
                return;
            }
        }

        bot.client.userCommand.forwardmove = (short)-160;
        bot.client.userCommand.sidemove = (short)(bot.botInfo.strafeDir * STRAFE_SPEED);
        if (bot.groundentity != null && bot.botInfo.timeNextJump < GameBase.level.time) {
            bot.client.userCommand.upmove = 250;
            bot.botInfo.timeNextJump = GameBase.level.time + 0.8f;
        }
    }

    private static void recoverIfStuck(Entity bot) {
        BotInfo bi = bot.botInfo;

        if (bi.timeStuckCheck > GameBase.level.time) {
            return;
        }

        float moved = distance2d(bot.s.origin, bi.oldOrigin);
        if ((bot.client.userCommand.forwardmove != 0 || bot.client.userCommand.sidemove != 0) && moved < 12f) {
            bi.stuckCount++;
        } else {
            bi.stuckCount = 0;
        }

        Math3D.VectorCopy(bot.s.origin, bi.oldOrigin);
        bi.timeStuckCheck = GameBase.level.time + 0.6f;

        if (bi.stuckCount >= 2) {
            if (BotNavigation.hasPath(bot)) {
                BotNavigation.handleStuck(bot);
            }
            bot.client.ps.viewangles[1] = normalizeYaw(bot.client.ps.viewangles[1] + 90f + (float)Math.random() * 120f);
            bot.client.userCommand.forwardmove = (short)-160;
            bot.client.userCommand.sidemove = (short)(bi.strafeDir * STRAFE_SPEED);
            if (bot.groundentity != null && bi.timeNextJump < GameBase.level.time) {
                bot.client.userCommand.upmove = 250;
                bi.timeNextJump = GameBase.level.time + 0.8f;
            }
            bi.stuckCount = 0;
        }
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
            if (!GameUtil.visible(bot, item)) continue;
            if (distance2d(bot.s.origin, item.s.origin) > MAX_DIRECT_ITEM_RANGE) continue;

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
            float enemyDistance = distance(bot, bot.enemy);
            if (GameUtil.visible(bot, bot.enemy)) {
                setYawToward(bot, bot.enemy.s.origin);
                if (enemyDistance < CLOSE_COMBAT_RANGE) {
                    bot.client.userCommand.forwardmove = -220;
                    bot.client.userCommand.sidemove = (short)(bi.strafeDir * STRAFE_SPEED);
                } else if (enemyDistance < GOOD_COMBAT_RANGE) {
                    bot.client.userCommand.forwardmove = 80;
                    bot.client.userCommand.sidemove = (short)(bi.strafeDir * STRAFE_SPEED);
                } else {
                    bot.client.userCommand.forwardmove = (short) MOVE_SPEED;
                    bot.client.userCommand.sidemove = (short)(bi.strafeDir * 160);
                }
                BotNavigation.clearPath(bot);
            } else {
                if (bi.timeNextChaseUpdate < GameBase.level.time) {
                    bi.timeNextChaseUpdate = GameBase.level.time + 0.5f;
                    BotNavigation.findRoute(bot, bot.enemy.s.origin, true);
                }
                if (BotNavigation.hasPath(bot)) {
                    BotNavigation.followPath(bot);
                    moveToTarget(bot, bi.moveTarget);
                } else {
                    moveToTarget(bot, bot.enemy.s.origin);
                }
            }
        }
        // Priority 2: Following a path (to pickup or exploration target)
        else if (BotNavigation.hasPath(bot)) {
            BotNavigation.followPath(bot);
            moveToTarget(bot, bi.moveTarget);
        }
        // Priority 3: Direct pickup (visible, nearby)
        else if (bi.pickupTarget != null) {
            if (distance2d(bot.s.origin, bi.pickupTarget.s.origin) < 200f) {
                moveToTarget(bot, bi.pickupTarget.s.origin);
            } else {
                if (BotNavigation.findRoute(bot, bi.pickupTarget.s.origin, false)) {
                    BotNavigation.followPath(bot);
                    moveToTarget(bot, bi.moveTarget);
                } else {
                    moveToTarget(bot, bi.pickupTarget.s.origin);
                }
            }
        }
        // Default: Roam
        else {
            roam(bot);
        }

        avoidObstacles(bot);
        recoverIfStuck(bot);
    }

    private static void applyCommandAngles(Entity bot) {
        for (int i = 0; i < 3; i++) {
            int desired = Math3D.ANGLE2SHORT(bot.client.ps.viewangles[i]);
            bot.client.userCommand.angles[i] = (short)(desired - bot.client.ps.pmove.delta_angles[i]);
        }
    }

    /**
     * Add bot to registry.
     */
    static void registerBot(BotInfoPers pers) {
        if (pers != null) {
            globalBots.add(pers);
        }
    }

    static void unregisterBot(BotInfoPers pers) {
        if (pers != null) {
            globalBots.remove(pers);
        }
    }

    /**
     * Get all registered bots.
     */
    public static List<BotInfoPers> getAllBots() {
        return Collections.unmodifiableList(globalBots);
    }
}
