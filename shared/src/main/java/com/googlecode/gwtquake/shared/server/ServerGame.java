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
package com.googlecode.gwtquake.shared.server;

import com.googlecode.gwtquake.shared.common.*;
import com.googlecode.gwtquake.shared.game.*;
import com.googlecode.gwtquake.shared.util.Math3D;


public class ServerGame {

    /**
     * PF_Unicast
     *
     * Sends the contents of the mutlicast buffer to a single client.
     */
    public static void PF_Unicast(Entity ent, boolean reliable) {
        int p;
        ClientData client;

        if (ent == null)
            return;

        p = ent.index;
        if (p < 1 || p > ServerMain.maxclients.value)
            return;

        client = ServerInit.svs.clients[p - 1];

        if (reliable)
            Buffers.Write(client.netchan.message, ServerInit.sv.multicast.data,
                    ServerInit.sv.multicast.cursize);
        else
            Buffers.Write(client.datagram, ServerInit.sv.multicast.data,
                    ServerInit.sv.multicast.cursize);

        ServerInit.sv.multicast.clear();
    }

    /**
     * PF_dprintf
     *
     * Debug print to server console.
     */
    public static void PF_dprintf(String fmt) {
        Com.Printf(fmt);
    }


    /**
     * Centerprintf for critical messages.
     */
    public static void PF_cprintfhigh(Entity ent, String fmt) {
    	PF_cprintf(ent, Constants.PRINT_HIGH, fmt);
    }

    /**
     * PF_cprintf
     *
     * Print to a single client.
     */
    public static void PF_cprintf(Entity ent, int level, String fmt) {

        int n = 0;

        if (ent != null) {
            n = ent.index;
            if (n < 1 || n > ServerMain.maxclients.value)
                Com.Error(Constants.ERR_DROP, "cprintf to a non-client");
        }

        if (ent != null)
            ServerSend.SV_ClientPrintf(ServerInit.svs.clients[n - 1], level, fmt);
        else
            Com.Printf(fmt);
    }

    /**
     * PF_centerprintf
     *
     * centerprint to a single client.
     */
    public static void PF_centerprintf(Entity ent, String fmt) {
        int n;

        n = ent.index;
        if (n < 1 || n > ServerMain.maxclients.value)
            return; // Com_Error (ERR_DROP, "centerprintf to a non-client");

        Buffers.writeByte(ServerInit.sv.multicast, Constants.svc_centerprint);
        Buffers.WriteString(ServerInit.sv.multicast, fmt);
        PF_Unicast(ent, true);
    }

    /**
     *  PF_error
     *
     *  Abort the server with a game error.
     */
    public static void PF_error(String fmt) {
        Com.Error(Constants.ERR_DROP, "Game Error: " + fmt);
    }

    public static void PF_error(int level, String fmt) {
        Com.Error(level, fmt);
    }

    /**
     * PF_setmodel
     *
     * Also sets mins and maxs for inline bmodels.
     */
    public static void PF_setmodel(Entity ent, String name) {
        int i;
        Model mod;

        if (name == null)
            Com.Error(Constants.ERR_DROP, "PF_setmodel: NULL");

        i = ServerInit.SV_ModelIndex(name);

        ent.s.modelindex = i;

        // if it is an inline model, get the size information for it
        if (name.startsWith("*")) {
            mod = CM.InlineModel(name);
            Math3D.VectorCopy(mod.mins, ent.mins);
            Math3D.VectorCopy(mod.maxs, ent.maxs);
            World.SV_LinkEdict(ent);
        }
    }

    /**
     *  PF_Configstring
     */
    public static void PF_Configstring(int index, String val) {
        if (index < 0 || index >= Constants.MAX_CONFIGSTRINGS)
            Com.Error(Constants.ERR_DROP, "configstring: bad index " + index
                    + "\n");

        if (val == null)
            val = "";

        // change the string in sv
        ServerInit.sv.configstrings[index] = val;

        if (ServerInit.sv.state != Constants.ss_loading) { // send the update to
                                                      // everyone
            ServerInit.sv.multicast.clear();
            Buffers.writeByte(ServerInit.sv.multicast, Constants.svc_configstring);
            ServerInit.sv.multicast.WriteShort(index);
            Buffers.WriteString(ServerInit.sv.multicast, val);

            ServerSend.SV_Multicast(Globals.vec3_origin, Constants.MULTICAST_ALL_R);
        }
    }

    public static void PF_WriteChar(int c) {
        Buffers.writeByte(ServerInit.sv.multicast, c);
    }

    public static void PF_WriteByte(int c) {
        Buffers.writeByte(ServerInit.sv.multicast, c);
    }

    public static void PF_WriteShort(int c) {
        ServerInit.sv.multicast.WriteShort(c);
    }

    public static void PF_WriteLong(int c) {
        ServerInit.sv.multicast.putInt(c);
    }

    public static void PF_WriteFloat(float f) {
        ServerInit.sv.multicast.putFloat(f);
    }

    public static void PF_WriteString(String s) {
        Buffers.WriteString(ServerInit.sv.multicast, s);
    }

    public static void PF_WritePos(float[] pos) {
        Buffers.WritePos(ServerInit.sv.multicast, pos);
    }

    public static void PF_WriteDir(float[] dir) {
        ServerGame.WriteDir(ServerInit.sv.multicast, dir);
    }

    public static void PF_WriteAngle(float f) {
        Buffers.WriteAngle(ServerInit.sv.multicast, f);
    }

    /**
     * PF_inPVS
     *
     * Also checks portalareas so that doors block sight.
     */
    public static boolean PF_inPVS(float[] p1, float[] p2) {
        int leafnum;
        int cluster;
        int area1, area2;
        byte mask[];

        leafnum = CM.CM_PointLeafnum(p1);
        cluster = CM.CM_LeafCluster(leafnum);
        area1 = CM.CM_LeafArea(leafnum);
        mask = CM.CM_ClusterPVS(cluster);

        leafnum = CM.CM_PointLeafnum(p2);
        cluster = CM.CM_LeafCluster(leafnum);
        area2 = CM.CM_LeafArea(leafnum);

        // quake2 bugfix
        if (cluster == -1)
            return false;
        if (mask != null && (0 == (mask[cluster >>> 3] & (1 << (cluster & 7)))))
            return false;

        if (!CM.CM_AreasConnected(area1, area2))
            return false; // a door blocks sight

        return true;
    }

    /**
     * PF_inPHS.
     *
     * Also checks portalareas so that doors block sound.
     */
    public static boolean PF_inPHS(float[] p1, float[] p2) {
        int leafnum;
        int cluster;
        int area1, area2;
        byte mask[];

        leafnum = CM.CM_PointLeafnum(p1);
        cluster = CM.CM_LeafCluster(leafnum);
        area1 = CM.CM_LeafArea(leafnum);
        mask = CM.CM_ClusterPHS(cluster);

        leafnum = CM.CM_PointLeafnum(p2);
        cluster = CM.CM_LeafCluster(leafnum);
        area2 = CM.CM_LeafArea(leafnum);

        // quake2 bugfix
        if (cluster == -1)
            return false;
        if (mask != null && (0 == (mask[cluster >> 3] & (1 << (cluster & 7)))))
            return false; // more than one bounce away
        if (!CM.CM_AreasConnected(area1, area2))
            return false; // a door blocks hearing

        return true;
    }

    public static void PF_StartSound(Entity entity, int channel,
            int sound_num, float volume, float attenuation, float timeofs) {

        if (null == entity)
            return;
        ServerSend.SV_StartSound(null, entity, channel, sound_num, volume,
                attenuation, timeofs);

    }


    /**
     *  SV_ShutdownGameProgs
     *
     * Called when either the entire server is being killed, or it is changing
     * to a different game directory.
     */
    public static void SV_ShutdownGameProgs() {
        //GameBase.ShutdownGame();
        Globals.game.Shutdown.run();
    }

    /**
     * SV_InitGameProgs
     *
     * Init the game subsystem for a new map.
     */

    public static void SV_InitGameProgs() {

        // unload anything we have now
        SV_ShutdownGameProgs();

        GameBase.pointcontents = new PlayerMove.PointContentsAdapter() {
            public int pointcontents(float[] o) {
                return World.SV_PointContents(o);
            }
        };

        GameSave.InitGame();
    }

    //should be ok.
    public static void WriteDir(Buffer sb, float[] dir) {
        int i, best;
        float d, bestd;

        if (dir == null) {
            Buffers.writeByte(sb, 0);
            return;
        }

        bestd = 0;
        best = 0;
        for (i = 0; i < Constants.NUMVERTEXNORMALS; i++) {
            d = Math3D.DotProduct(dir, Globals.bytedirs[i]);
            if (d > bestd) {
                bestd = d;
                best = i;
            }
        }
        Buffers.writeByte(sb, best);
    }
}
