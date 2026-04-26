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

import java.util.ArrayList;
import java.util.List;

/**
 * Navigation graph node for bot pathfinding.
 * Equivalent to C struct path_node_t.
 */
public class PathNode {
    // Flag constants
    public static final int TELEPORTER = 1;
    public static final int LADDER = 2;
    public static final int PLATFORM = 4;
    public static final int JUMP = 8;

    // Static storage for all nodes
    public static List<PathNode> allNodes = new ArrayList<>();

    // Instance fields
    public float[] origin = new float[3];
    public int index;
    public PathNode[] neighbours;
    public int neighbourCount;
    public int flags;

    /**
     * Creates a new PathNode with default values.
     * Initializes neighbourCount to 0, allocates neighbour array with capacity 16,
     * and sets flags to 0.
     */
    public PathNode() {
        this.neighbourCount = 0;
        this.neighbours = new PathNode[16];
        this.flags = 0;
    }

    /**
     * Finds the nearest PathNode to the given position.
     *
     * @param pos the position as a float array [x, y, z]
     * @return the nearest PathNode, or null if no nodes exist
     */
    public static PathNode findNearest(float[] pos) {
        if (allNodes.isEmpty()) {
            return null;
        }

        PathNode nearest = null;
        float minDistance = Float.MAX_VALUE;

        for (PathNode node : allNodes) {
            float dx = node.origin[0] - pos[0];
            float dy = node.origin[1] - pos[1];
            float dz = node.origin[2] - pos[2];
            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distance < minDistance) {
                minDistance = distance;
                nearest = node;
            }
        }

        return nearest;
    }

    /**
     * Clears all stored nodes.
     */
    public static void clearAll() {
        allNodes.clear();
    }

}
