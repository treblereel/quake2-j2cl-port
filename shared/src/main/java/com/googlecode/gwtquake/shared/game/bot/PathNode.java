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

import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.game.Entity;
import com.googlecode.gwtquake.shared.game.GameBase;
import com.googlecode.gwtquake.shared.game.Trace;
import com.googlecode.gwtquake.shared.server.World;
import com.googlecode.gwtquake.shared.util.Math3D;

public class PathNode {

    public static final int MAX_NODE_LINKS = 6;
    public static final int MAX_NODE_COUNT = 2000;
    public static final float NODE_MAX_DIST = 280f;
    public static final float NODE_MIN_DIST = 90f;

    public static final int NF_ELEVATOR = 0x0001;
    public static final int NF_TELEPORT = 0x0002;
    public static final int NF_DOOR     = 0x0004;
    public static final int NF_BUTTON   = 0x0008;
    public static final int NF_LADDER   = 0x0010;

    private static final int MASK_REACHABLE =
        Constants.CONTENTS_SOLID | Constants.CONTENTS_WINDOW |
        Constants.CONTENTS_SLIME | Constants.CONTENTS_LAVA |
        Constants.CONTENTS_PLAYERCLIP;

    public static List<PathNode> allNodes = new ArrayList<>();

    public float[] origin = new float[3];
    public int flags;
    public PathNode[] linkTo = new PathNode[MAX_NODE_LINKS];
    public PathNode[] linkFrom = new PathNode[MAX_NODE_LINKS];
    public float[] linkDist = new float[MAX_NODE_LINKS];
    public Entity item;
    public float time;
    public float routeDist;

    public PathNode() {
        this.flags = 0;
        this.routeDist = -1;
    }

    public static void clearAll() {
        allNodes.clear();
    }

    public static boolean posReachable(float[] spot1, float[] spot2) {
        Trace trace = World.SV_Trace(spot1, Globals.vec3_origin, Globals.vec3_origin,
                spot2, null, MASK_REACHABLE);
        return trace.fraction == 1.0f;
    }

    public static void addDirectRoute(PathNode node1, PathNode node2, boolean computeDistance) {
        if (node1 == null || node2 == null || node1 == node2) return;

        int i;
        boolean canAdd = false;
        for (i = 0; i < MAX_NODE_LINKS; i++) {
            if (node1.linkTo[i] == node2) return;
            if (node1.linkTo[i] == null) { canAdd = true; break; }
        }
        if (!canAdd) return;

        int j;
        canAdd = false;
        for (j = 0; j < MAX_NODE_LINKS; j++) {
            if (node2.linkFrom[j] == node1 || node2.linkFrom[j] == null) {
                canAdd = true;
                break;
            }
        }
        if (!canAdd) return;

        node1.linkTo[i] = node2;
        node2.linkFrom[j] = node1;

        float d;
        if (computeDistance) {
            float dx = node1.origin[0] - node2.origin[0];
            float dy = node1.origin[1] - node2.origin[1];
            float dz = node1.origin[2] - node2.origin[2];
            d = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        } else {
            d = 1f;
        }
        node1.linkDist[i] = d;
    }

    public static void addDirectRouteBidi(PathNode node1, PathNode node2) {
        if (node1 == null || node2 == null || node1 == node2) return;
        addDirectRoute(node1, node2, true);
        addDirectRoute(node2, node1, true);
    }

    public static void removeDirectRoute(PathNode node1, PathNode node2) {
        if (node1 == null || node2 == null || node1 == node2) return;

        int r = -1;
        int last = -1;
        for (int i = 0; i < MAX_NODE_LINKS; i++) {
            if (node1.linkTo[i] == null) break;
            if (node1.linkTo[i] == node2) r = i;
            last = i;
        }
        if (last >= 0 && r >= 0) {
            node1.linkTo[r] = node1.linkTo[last];
            node1.linkDist[r] = node1.linkDist[last];
            node1.linkTo[last] = null;
            node1.linkDist[last] = 0;
        }

        r = -1;
        last = -1;
        for (int i = 0; i < MAX_NODE_LINKS; i++) {
            if (node2.linkFrom[i] == null) break;
            if (node2.linkFrom[i] == node1) r = i;
            last = i;
        }
        if (last >= 0 && r >= 0) {
            node2.linkFrom[r] = node2.linkFrom[last];
            node2.linkFrom[last] = null;
        }
    }

    public static void addLinksRadius(PathNode newNode) {
        float maxDist = NODE_MIN_DIST * 1.4f;
        for (PathNode node : allNodes) {
            if (node == newNode) continue;
            if (dist(newNode.origin, node.origin) < maxDist
                    && posReachable(newNode.origin, node.origin)) {
                addDirectRouteBidi(node, newNode);
            }
            if (newNode.linkTo[MAX_NODE_LINKS - 1] != null) return;
        }

        maxDist = NODE_MAX_DIST;
        for (PathNode node : allNodes) {
            if (node == newNode) continue;
            if (dist(newNode.origin, node.origin) < maxDist
                    && posReachable(newNode.origin, node.origin)) {
                addDirectRouteBidi(node, newNode);
            }
            if (newNode.linkTo[MAX_NODE_LINKS - 1] != null) return;
        }
    }

    public static PathNode insertNode(float[] pos, PathNode prevNode, int flags) {
        PathNode newNode = new PathNode();
        newNode.time = GameBase.level.time;
        Math3D.VectorCopy(pos, newNode.origin);
        newNode.flags = flags;

        if (prevNode != null) {
            addDirectRouteBidi(newNode, prevNode);
        }

        addLinksRadius(newNode);
        allNodes.add(newNode);
        return newNode;
    }

    public static PathNode findNearest(float[] pos) {
        if (allNodes.isEmpty()) return null;

        PathNode nearest = null;
        float minDistance = Float.MAX_VALUE;
        for (PathNode node : allNodes) {
            float d = dist(pos, node.origin);
            if (d < minDistance) {
                minDistance = d;
                nearest = node;
            }
        }
        return nearest;
    }

    public static float dist(float[] a, float[] b) {
        float dx = a[0] - b[0];
        float dy = a[1] - b[1];
        float dz = a[2] - b[2];
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
