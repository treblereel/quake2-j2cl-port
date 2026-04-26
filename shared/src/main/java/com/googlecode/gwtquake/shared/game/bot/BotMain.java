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
import com.googlecode.gwtquake.shared.game.Entity;
import com.googlecode.gwtquake.shared.game.Commands;

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
     * Initialize bot subsystem (called from SV_InitGame).
     */
    public static void init() {
        Com.Printf("Bot subsystem initializing...\n");

        // Register console commands
        // TODO: Commands.addCommand("sv", BotCommands::serverCommand);

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

        // TODO: Implement AI logic
        // 1. Update sensors
        // 2. Find enemies
        // 3. Navigate
        // 4. Combat
        // 5. Find pickups
        // 6. Update movement
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
