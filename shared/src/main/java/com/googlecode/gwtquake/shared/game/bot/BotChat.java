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

import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.game.Entity;
import com.googlecode.gwtquake.shared.game.GameBase;
import com.googlecode.gwtquake.shared.server.ServerGame;

/**
 * Bot chat message system.
 * Bots write messages on kill/death events.
 */
public class BotChat {

    private static final String[] KILL_MESSAGES = {
        "Got you!",
        "Too easy",
        "Nice try",
        "Better luck next time",
        "Owned",
        "Pwned"
    };

    private static final String[] DEATH_MESSAGES = {
        "Lucky shot",
        "I'll be back",
        "Grr...",
        "Not bad",
        "Impressive",
        "You win this round"
    };

    /**
     * Bot killed someone - maybe taunt.
     */
    public static void onKill(Entity bot, Entity victim) {
        BotInfo bi = bot.botInfo;
        if (bi == null) return;

        // Anti-spam timer (5 seconds)
        if (bi.timeLastMessage + 5.0f > GameBase.level.time) {
            return;
        }

        // Probability based on skill (higher skill = more talkative)
        float probability = 0.3f + bot.botPers.skill * 0.1f;
        if (Math.random() > probability) {
            return;
        }

        String msg = KILL_MESSAGES[(int)(Math.random() * KILL_MESSAGES.length)];
        ServerGame.PF_cprintf(null, Constants.PRINT_CHAT,
            bot.client.pers.netname + ": " + msg + "\n");

        bi.timeLastMessage = GameBase.level.time;
    }

    /**
     * Bot was killed - maybe comment.
     */
    public static void onDeath(Entity bot, Entity killer) {
        BotInfo bi = bot.botInfo;
        if (bi == null) return;

        // Anti-spam timer
        if (bi.timeLastMessage + 5.0f > GameBase.level.time) {
            return;
        }

        // Lower probability for death messages
        if (Math.random() > 0.2f) {
            return;
        }

        String msg = DEATH_MESSAGES[(int)(Math.random() * DEATH_MESSAGES.length)];
        ServerGame.PF_cprintf(null, Constants.PRINT_CHAT,
            bot.client.pers.netname + ": " + msg + "\n");

        bi.timeLastMessage = GameBase.level.time;
    }
}
