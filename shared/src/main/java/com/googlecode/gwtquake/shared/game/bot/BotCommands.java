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
            // sv addbots <count>
            addBots();
        }
        else if ("removebot".equals(cmd)) {
            // sv removebot <name>
            removeBot();
        }
    }

    public static void addBot() {
        Com.Printf(">>> addBot() called\n");
        int skill = parseInt(Commands.Argv(1), 1);
        String name = Commands.Argv(2);
        String skin = Commands.Argv(3);
        String model = Commands.Argv(4);

        if (name.isEmpty()) name = "Bot";
        if (skin.isEmpty()) skin = "male/grunt";
        if (model.isEmpty()) model = "male";

        Com.Printf(">>> addBot: skill=" + skill + " name=" + name + " skin=" + skin + " model=" + model + "\n");
        spawnBot(name, skill, skin, model);
    }

    public static void addBots() {
        Com.Printf(">>> addBots() called\n");
        int count = parseInt(Commands.Argv(1), 1);
        Com.Printf(">>> addBots: spawning " + count + " bots\n");

        for (int i = 0; i < count; i++) {
            // Random skill 0-3
            int skill = (int)(Math.random() * 4);
            String name = "Bot" + (i + 1);
            Com.Printf(">>> addBots: spawning bot " + (i+1) + "/" + count + "\n");
            spawnBot(name, skill, "male/grunt", "male");
        }
    }

    public static void removeBot() {
        String name = Commands.Argv(1);

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
        Com.Printf(">>> spawnBot START: name=" + name + " skill=" + skill + "\n");

        // 1. Find free client slot
        Entity ent = findFreeClientSlot();
        if (ent == null) {
            Com.Printf(">>> spawnBot FAILED: No free client slots\n");
            return;
        }
        Com.Printf(">>> spawnBot: found slot index=" + ent.index + "\n");

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
        Com.Printf(">>> spawnBot: BotInfoPers created\n");

        // 3. Create BotInfo
        BotInfo info = new BotInfo();
        info.strafeDir = 1f;
        Com.Printf(">>> spawnBot: BotInfo created\n");

        // 4. CRITICAL: Mark entity as in-use and initialize client BEFORE ClientConnect
        // Otherwise ClientBegin will call G_InitEdict and reset everything
        ent.inuse = true;
        ent.client = GameBase.game.clients[ent.index - 1];

        // Initialize client persistent data to clear any old spectator/etc flags
        // This is normally done in ClientConnect if inuse==false, but we set inuse=true
        PlayerClient.InitClientPersistant(ent.client);
        Com.Printf(">>> spawnBot: marked inuse=true, client initialized\n");

        // 5. Build userinfo string (add spectator=0 to force non-spectator mode)
        String userinfo = "\\name\\" + name +
                         "\\skin\\" + skin +
                         "\\model\\" + model +
                         "\\spectator\\0";
        Com.Printf(">>> spawnBot: calling ClientConnect with userinfo=" + userinfo + "\n");

        // 6. Connect as client
        try {
            PlayerClient.ClientConnect(ent, userinfo);
            Com.Printf(">>> spawnBot: ClientConnect done\n");
            Com.Printf(">>> After ClientConnect: spectator=" + ent.client.pers.spectator + " solid=" + ent.solid + " svflags=" + ent.svflags + "\n");
        } catch (Exception e) {
            Com.Printf(">>> spawnBot ERROR in ClientConnect: " + e.getMessage() + "\n");
            e.printStackTrace();
            return;
        }

        // CRITICAL: Force spectator=false AFTER ClientConnect
        // ClientConnect may set it based on userinfo parsing
        ent.client.pers.spectator = false;
        Com.Printf(">>> Forced spectator=false after ClientConnect\n");

        try {
            Com.Printf(">>> Before ClientBegin: spectator=" + ent.client.pers.spectator + "\n");
            PlayerClient.ClientBegin(ent);
            Com.Printf(">>> spawnBot: ClientBegin done\n");
            Com.Printf(">>> After ClientBegin: solid=" + ent.solid + " svflags=" + ent.svflags + " classname=" + ent.classname + "\n");
        } catch (Exception e) {
            Com.Printf(">>> spawnBot ERROR in ClientBegin: " + e.getMessage() + "\n");
            e.printStackTrace();
            return;
        }

        // 7. Configure bot-specific fields AFTER ClientBegin
        // This ensures they aren't lost during initialization
        ent.botInfo = info;
        ent.botPers = pers;
        Com.Printf(">>> spawnBot: bot fields attached\n");

        // Set bot AI think callback
        ent.think = BotMain.thinkAdapter;
        ent.nextthink = GameBase.level.time + Constants.FRAMETIME;
        Com.Printf(">>> spawnBot: think callback set\n");

        // 8. Register bot
        BotMain.registerBot(pers);

        Com.Printf(">>> spawnBot SUCCESS: Bot '" + name + "' (skill " + skill + ") spawned at index " + ent.index + "\n");
        Com.Printf(">>> Bot state: inuse=" + ent.inuse + " pos=[" + ent.s.origin[0] + "," + ent.s.origin[1] + "," + ent.s.origin[2] + "]");
        Com.Printf(" modelindex=" + ent.s.modelindex + " solid=" + ent.solid + " svflags=" + ent.svflags + " classname=" + ent.classname + "\n");
        Com.Printf(">>> Bot fields: botInfo=" + (ent.botInfo != null) + " botPers=" + (ent.botPers != null) +
                   " think=" + (ent.think != null) + " nextthink=" + ent.nextthink + "\n");
    }

    /**
     * Find first free client slot.
     */
    private static Entity findFreeClientSlot() {
        Com.Printf(">>> findFreeClientSlot: searching...\n");
        for (int i = 1; i <= ServerMain.maxclients.value; i++) {
            Entity ent = GameBase.g_edicts[i];
            if (!ent.inuse) {
                Com.Printf(">>> findFreeClientSlot: found slot " + i + "\n");
                return ent;
            }
        }
        Com.Printf(">>> findFreeClientSlot: NO FREE SLOTS (maxclients=" + (int)ServerMain.maxclients.value + ")\n");
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
