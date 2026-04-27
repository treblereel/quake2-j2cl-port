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
import com.googlecode.gwtquake.shared.game.Commands;
import com.googlecode.gwtquake.shared.game.Entity;
import com.googlecode.gwtquake.shared.game.PlayerClient;
import com.googlecode.gwtquake.shared.game.GameBase;
import com.googlecode.gwtquake.shared.server.ServerMain;

/**
 * Console command handlers for bot management.
 * Commands: sv addbot, sv addbots, sv removebot
 */
public class BotCommands {

    /**
     * Main server command router.
     */
    public static void serverCommand() {
        String cmd = Commands.Argv(1);

        if ("addbot".equals(cmd)) {
            // sv addbot <skill> <name> [skin] [model]
            addBot();
        }
        else if ("addbots".equals(cmd)) {
            // sv addbots <skill> <count>
            addBots();
        }
        else if ("removebot".equals(cmd)) {
            // sv removebot <name>
            removeBot();
        }
    }

    private static void addBot() {
        int skill = parseInt(Commands.Argv(2), 1);
        String name = Commands.Argv(3);
        String skin = Commands.Argv(4);
        String model = Commands.Argv(5);

        if (name.isEmpty()) name = "Bot";
        if (skin.isEmpty()) skin = "male/grunt";
        if (model.isEmpty()) model = "male";

        spawnBot(name, skill, skin, model);
    }

    private static void addBots() {
        int skill = parseInt(Commands.Argv(2), 1);
        int count = parseInt(Commands.Argv(3), 1);

        for (int i = 0; i < count; i++) {
            String name = "Bot" + (i + 1);
            spawnBot(name, skill, "male/grunt", "male");
        }
    }

    private static void removeBot() {
        String name = Commands.Argv(2);

        // Find bot by name
        for (int i = 1; i <= ServerMain.maxclients.value; i++) {
            Entity ent = GameBase.g_edicts[i];

            if (!ent.inuse || ent.botInfo == null) continue;
            if (ent.botPers.name.equals(name)) {
                PlayerClient.ClientDisconnect(ent);
                Com.Printf("Bot '" + name + "' removed\n");
                return;
            }
        }

        Com.Printf("Bot '" + name + "' not found\n");
    }

    /**
     * Spawn a bot into the game.
     */
    public static void spawnBot(String name, int skill, String skin, String model) {
        // 1. Find free client slot
        Entity ent = findFreeClientSlot();
        if (ent == null) {
            Com.Printf("No free client slots\n");
            return;
        }

        // 2. Create BotInfoPers
        BotInfoPers pers = new BotInfoPers();
        pers.name = name;
        pers.skill = Math.max(0, Math.min(3, skill));
        pers.skin = skin;
        pers.model = model;
        pers.speed = 400f;
        pers.rotSpeed = 5f;
        pers.attackRange = 1000f;
        pers.engageRange = 1500f;
        pers.playerNum = ent.index - 1;

        // 3. Create BotInfo
        BotInfo info = new BotInfo();
        info.strafeDir = 1f;

        // 4. Configure Entity
        ent.botInfo = info;
        ent.botPers = pers;
        ent.client = GameBase.game.clients[ent.index - 1];

        // Set bot AI think callback
        ent.think = BotMain.thinkAdapter;
        ent.nextthink = GameBase.level.time + Constants.FRAMETIME;

        // 5. Build userinfo string
        String userinfo = "\\name\\" + name +
                         "\\skin\\" + skin +
                         "\\model\\" + model;

        // 6. Connect as client
        PlayerClient.ClientConnect(ent, userinfo);
        PlayerClient.ClientBegin(ent);

        // 7. Register bot
        BotMain.registerBot(pers);

        Com.Printf("Bot '" + name + "' (skill " + skill + ") spawned\n");
    }

    /**
     * Find first free client slot.
     */
    private static Entity findFreeClientSlot() {
        for (int i = 1; i <= ServerMain.maxclients.value; i++) {
            Entity ent = GameBase.g_edicts[i];
            if (!ent.inuse) {
                return ent;
            }
        }
        return null;
    }

    /**
     * Parse int with default value.
     */
    private static int parseInt(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
