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
/* Modifications
   Copyright 2003-2004 Bytonic Software
   Copyright 2010 Google Inc.
*/
package com.googlecode.gwtquake.shared.game.bot;

/**
 * Persistent bot data that survives respawns.
 * Equivalent to C struct bot_info_pers_t.
 */
public class BotInfoPers {
    public int skill;              // 0-3 (0=novice, 3=expert)
    public int teamNo;             // Team number (for team play)
    public int adaptCount;         // Adaptation counter
    public String skin;            // Skin (e.g., "male/grunt")
    public String model;           // Model (e.g., "male")
    public String name;            // Bot display name
    public float speed;            // Base movement speed
    public float rotSpeed;         // Turn rate
    public float attackRange;      // Attack distance threshold
    public float engageRange;      // Enemy engagement distance
    public boolean bInuse;         // Bot is in use
    public boolean bAdapting;      // Bot is adapting
    public int playerNum;          // Client slot index

    public BotInfoPers() {
        this.skill = 1;
        this.teamNo = 0;
        this.adaptCount = 0;
        this.skin = "male/grunt";
        this.model = "male";
        this.name = "Bot";
        this.speed = 400f;
        this.rotSpeed = 5f;
        this.attackRange = 1000f;
        this.engageRange = 1500f;
        this.bInuse = false;
        this.bAdapting = false;
        this.playerNum = -1;
    }
}
