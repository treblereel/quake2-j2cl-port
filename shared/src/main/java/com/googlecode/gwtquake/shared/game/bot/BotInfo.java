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

import com.googlecode.gwtquake.shared.game.Entity;

/**
 * Runtime bot state (reset on respawn).
 * Equivalent to C struct bot_info_t.
 */
public class BotInfo {
    // Timers (when next action is allowed)
    public float timeLastStuck;
    public float timeNextEnemy;
    public float timeNextPickup;
    public float timeLastStrafeSwitch;
    public float timeNextShot;
    public float timeStopShooting;
    public float timeChase;
    public float timeNextChaseUpdate;
    public float timeNextAssignmentCheck;
    public float timeNextRoamDirChange;
    public float timeStuckCheck;
    public float timeNextCrouch;
    public float timeLastMessage;
    public float timeNextFightMessage;
    public float timeNextJump;
    public float timeNextWeaponChange;
    public float timeNextRocketAvoid;
    public float timeWeaponSpinUp;
    public float timeWeaponSpinDown;
    public float timeNextSpecialAssignment;
    public float timeNextSolid;
    public float timeNextCallForHelp;
    public float timeNextSalute;
    public float timeLastMoveTarget;

    // Movement direction and targets
    public float strafeDir;                    // -1 or +1 (strafe direction)
    public float[] moveTarget = new float[3]; // Where to move
    public float[] oldOrigin = new float[3]; // Previous position
    public float[] shootLastTarget = new float[3]; // Last shot target
    public float[] lastMoveTarget = new float[3]; // Previous move target

    // Pickup targeting
    public Entity pickupTarget;                // Item to pick up
    public float pickupTargetScore;            // Item priority

    // Pathfinding state
    public PathNode lastNode;                  // Previous node
    public PathNode nextNode;                  // Next node in path
    public PathNode targetNode;                // Final destination node

    // Movement state
    public boolean bCrouch;                    // Crouching
    public boolean bOnSlope;                   // On sloped surface
    public boolean bAirborne;                  // In the air
    public boolean bShotThisFrame;             // Shot this frame
    public boolean bOnPlatform;                // On a platform
    public boolean bOnLadder;                  // On a ladder
    public float[] ladderDir = new float[3]; // Ladder direction

    // Stuck detection
    public int stuckCount;                     // Stuck detection counter
    public int moveBlockCount;                 // Movement blocked counter

    // Unreachable tracking (avoid repeatedly trying unreachable items)
    public Entity[] unreachable = new Entity[12];
    public float[] timeUnreachable = new float[12];

    // Path storage
    public PathNode[] path = new PathNode[256];  // Current path
    public int pathNodes;                        // Path length

    // Assignment and patrol
    public int botAssignment;                  // Assignment type (ASSN_*)
    public float[] botAnchor = new float[3];  // Anchor point for assignments
    public Entity teamLeader;                  // Team leader entity

    // Function pointer (think function)
    public PostThinkFunction postThink;        // Post-think callback

    /**
     * Interface for post-think callback
     */
    public interface PostThinkFunction {
        void postThink(Entity self);
    }

    public BotInfo() {
        this.strafeDir = 1f;
        this.pathNodes = 0;
        this.botAssignment = 0;
        this.timeLastStuck = 0f;
        this.timeNextEnemy = 0f;
        this.timeNextPickup = 0f;
        this.timeLastStrafeSwitch = 0f;
        this.timeNextShot = 0f;
        this.timeStopShooting = 0f;
        this.timeChase = 0f;
        this.timeNextChaseUpdate = 0f;
        this.timeNextAssignmentCheck = 0f;
        this.timeNextRoamDirChange = 0f;
        this.timeStuckCheck = 0f;
        this.timeNextCrouch = 0f;
        this.timeLastMessage = 0f;
        this.timeNextFightMessage = 0f;
        this.timeNextJump = 0f;
        this.timeNextWeaponChange = 0f;
        this.timeNextRocketAvoid = 0f;
        this.timeWeaponSpinUp = 0f;
        this.timeWeaponSpinDown = 0f;
        this.timeNextSpecialAssignment = 0f;
        this.timeNextSolid = 0f;
        this.timeNextCallForHelp = 0f;
        this.timeNextSalute = 0f;
        this.timeLastMoveTarget = 0f;
        this.pickupTargetScore = 0f;
        this.stuckCount = 0;
        this.moveBlockCount = 0;
    }
}
