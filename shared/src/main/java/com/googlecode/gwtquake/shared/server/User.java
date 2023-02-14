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


import java.nio.ByteBuffer;

import com.googlecode.gwtquake.shared.common.Buffers;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.CommandBuffer;
import com.googlecode.gwtquake.shared.common.ConsoleVariables;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.common.Delta;
import com.googlecode.gwtquake.shared.common.QuakeFileSystem;
import com.googlecode.gwtquake.shared.common.ResourceLoader;
import com.googlecode.gwtquake.shared.game.Commands;
import com.googlecode.gwtquake.shared.game.Entity;
import com.googlecode.gwtquake.shared.game.EntityState;
import com.googlecode.gwtquake.shared.game.GameBase;
import com.googlecode.gwtquake.shared.game.Info;
import com.googlecode.gwtquake.shared.game.PlayerClient;
import com.googlecode.gwtquake.shared.game.UserCommand;
import com.googlecode.gwtquake.shared.util.Lib;

public class User {

    static Entity sv_player;

    public static class ucmd_t {
        public ucmd_t(String n, Runnable r) {
            name = n;
            this.r = r;
        }

        String name;

        Runnable r;
    }

    static ucmd_t u1 = new ucmd_t("new", new Runnable() {
        public void run() {
            User.SV_New_f();
        }
    });

    static ucmd_t ucmds[] = {
    // auto issued
            new ucmd_t("new", new Runnable() {
                public void run() {
                    User.SV_New_f();
                }
            }), new ucmd_t("configstrings", new Runnable() {
                public void run() {
                    User.SV_Configstrings_f();
                }
            }), new ucmd_t("baselines", new Runnable() {
                public void run() {
                    User.SV_Baselines_f();
                }
            }), new ucmd_t("begin", new Runnable() {
                public void run() {
                    User.SV_Begin_f();
                }
            }), new ucmd_t("nextserver", new Runnable() {
                public void run() {
                    User.SV_Nextserver_f();
                }
            }), new ucmd_t("disconnect", new Runnable() {
                public void run() {
                    User.SV_Disconnect_f();
                }
            }),

            // issued by hand at client consoles
            new ucmd_t("info", new Runnable() {
                public void run() {
                    User.SV_ShowServerinfo_f();
                }
            }), new ucmd_t("download", new Runnable() {
                public void run() {
                    User.SV_BeginDownload_f();
                }
            }), new ucmd_t("nextdl", new Runnable() {
                public void run() {
                    User.SV_NextDownload_f();
                }
            }) };

    public static final int MAX_STRINGCMDS = 8;

    /*
     * ============================================================
     *
     * USER STRINGCMD EXECUTION
     *
     * sv_client and sv_player will be valid.
     * ============================================================
     */

    /*
     * ================== SV_BeginDemoServer ==================
     */
    public static void SV_BeginDemoserver() {
        String name;

        name = "demos/" + ServerInit.sv.name;

        ResourceLoader.loadResourceAsync(name, new ResourceLoader.Callback() {

			public void onSuccess(ByteBuffer result) {
				ServerInit.sv.demofile = result;
			}
		});

    }

    /*
     * ================ SV_New_f
     *
     * Sends the first message from the server to a connected client. This will
     * be sent on the initial connection and upon each server load.
     * ================
     */
    public static void SV_New_f() {
        String gamedir;
        int playernum;
        Entity ent;

        Com.DPrintf("New() from " + ServerMain.sv_client.name + "\n");

        if (ServerMain.sv_client.state != Constants.cs_connected) {
            Com.Printf("New not valid -- already spawned\n");
            return;
        }

        // demo servers just dump the file message
        if (ServerInit.sv.state == Constants.ss_demo) {
            SV_BeginDemoserver();
            return;
        }

        //
        // serverdata needs to go over for all types of servers
        // to make sure the protocol is right, and to set the gamedir
        //
        gamedir = ConsoleVariables.VariableString("gamedir");

        // send the serverdata
        Buffers.writeByte(ServerMain.sv_client.netchan.message,
                        Constants.svc_serverdata);
        ServerMain.sv_client.netchan.message.putInt(
                Constants.PROTOCOL_VERSION);

        ServerMain.sv_client.netchan.message.putInt(ServerInit.svs.spawncount);
        Buffers.writeByte(ServerMain.sv_client.netchan.message,
                ServerInit.sv.attractloop ? 1 : 0);
        Buffers.WriteString(ServerMain.sv_client.netchan.message, gamedir);

        if (ServerInit.sv.state == Constants.ss_cinematic
                || ServerInit.sv.state == Constants.ss_pic)
            playernum = -1;
        else
            //playernum = sv_client - svs.clients;
            playernum = ServerMain.sv_client.serverindex;

        ServerMain.sv_client.netchan.message.WriteShort(playernum);

        // send full levelname
        Buffers.WriteString(ServerMain.sv_client.netchan.message,
                ServerInit.sv.configstrings[Constants.CS_NAME]);

        //
        // game server
        //
        if (ServerInit.sv.state == Constants.ss_game) {
            // set up the entity for the client
            ent = GameBase.g_edicts[playernum + 1];
            ent.s.number = playernum + 1;
            ServerMain.sv_client.edict = ent;
            ServerMain.sv_client.lastcmd = new UserCommand();

            // begin fetching configstrings
            Buffers.writeByte(ServerMain.sv_client.netchan.message,
                    Constants.svc_stufftext);
            Buffers.WriteString(ServerMain.sv_client.netchan.message,
                    "cmd configstrings " + ServerInit.svs.spawncount + " 0\n");
        }

    }

    /*
     * ================== SV_Configstrings_f ==================
     */
    public static void SV_Configstrings_f() {
        int start;

        Com.DPrintf("Configstrings() from " + ServerMain.sv_client.name + "\n");

        if (ServerMain.sv_client.state != Constants.cs_connected) {
            Com.Printf("configstrings not valid -- already spawned\n");
            return;
        }

        // handle the case of a level changing while a client was connecting
        if (Lib.atoi(Commands.Argv(1)) != ServerInit.svs.spawncount) {
            Com.Printf("SV_Configstrings_f from different level\n");
            SV_New_f();
            return;
        }

        start = Lib.atoi(Commands.Argv(2));

        // write a packet full of data

        while (ServerMain.sv_client.netchan.message.cursize < Constants.MAX_MSGLEN / 2
                && start < Constants.MAX_CONFIGSTRINGS) {
            if (ServerInit.sv.configstrings[start] != null
                    && ServerInit.sv.configstrings[start].length() != 0) {
                Buffers.writeByte(ServerMain.sv_client.netchan.message,
                        Constants.svc_configstring);
                ServerMain.sv_client.netchan.message.WriteShort(start);
                Buffers.WriteString(ServerMain.sv_client.netchan.message,
                        ServerInit.sv.configstrings[start]);
            }
            start++;
        }

        // send next command

        if (start == Constants.MAX_CONFIGSTRINGS) {
            Buffers.writeByte(ServerMain.sv_client.netchan.message,
                    Constants.svc_stufftext);
            Buffers.WriteString(ServerMain.sv_client.netchan.message, "cmd baselines "
                    + ServerInit.svs.spawncount + " 0\n");
        } else {
            Buffers.writeByte(ServerMain.sv_client.netchan.message,
                    Constants.svc_stufftext);
            Buffers.WriteString(ServerMain.sv_client.netchan.message,
                    "cmd configstrings " + ServerInit.svs.spawncount + " " + start
                            + "\n");
        }
    }

    /*
     * ================== SV_Baselines_f ==================
     */
    public static void SV_Baselines_f() {
        int start;
        EntityState nullstate;
        EntityState base;

        Com.DPrintf("Baselines() from " + ServerMain.sv_client.name + "\n");

        if (ServerMain.sv_client.state != Constants.cs_connected) {
            Com.Printf("baselines not valid -- already spawned\n");
            return;
        }

        // handle the case of a level changing while a client was connecting
        if (Lib.atoi(Commands.Argv(1)) != ServerInit.svs.spawncount) {
            Com.Printf("SV_Baselines_f from different level\n");
            SV_New_f();
            return;
        }

        start = Lib.atoi(Commands.Argv(2));

        //memset (&nullstate, 0, sizeof(nullstate));
        nullstate = new EntityState(null);

        // write a packet full of data

        while (ServerMain.sv_client.netchan.message.cursize < Constants.MAX_MSGLEN / 2
                && start < Constants.MAX_EDICTS) {
            base = ServerInit.sv.baselines[start];
            if (base.modelindex != 0 || base.sound != 0 || base.effects != 0) {
                Buffers.writeByte(ServerMain.sv_client.netchan.message,
                        Constants.svc_spawnbaseline);
                Delta.WriteDeltaEntity(nullstate, base,
                        ServerMain.sv_client.netchan.message, true, true);
            }
            start++;
        }

        // send next command

        if (start == Constants.MAX_EDICTS) {
            Buffers.writeByte(ServerMain.sv_client.netchan.message,
                    Constants.svc_stufftext);
            Buffers.WriteString(ServerMain.sv_client.netchan.message, "precache "
                    + ServerInit.svs.spawncount + "\n");
        } else {
            Buffers.writeByte(ServerMain.sv_client.netchan.message,
                    Constants.svc_stufftext);
            Buffers.WriteString(ServerMain.sv_client.netchan.message, "cmd baselines "
                    + ServerInit.svs.spawncount + " " + start + "\n");
        }
    }

    /*
     * ================== SV_Begin_f ==================
     */
    public static void SV_Begin_f() {
        Com.DPrintf("Begin() from " + ServerMain.sv_client.name + "\n");

        // handle the case of a level changing while a client was connecting
        if (Lib.atoi(Commands.Argv(1)) != ServerInit.svs.spawncount) {
            Com.Printf("SV_Begin_f from different level\n");
            SV_New_f();
            return;
        }

        ServerMain.sv_client.state = Constants.cs_spawned;

        // call the game begin function
        //PlayerClient.ClientBegin(User.sv_player);
        Globals.game.ClientBegin.accept(User.sv_player);
        CommandBuffer.InsertFromDefer();
    }

    //=============================================================================

    /*
     * ================== SV_NextDownload_f ==================
     */
    public static void SV_NextDownload_f() {
        int r;
        int percent;
        int size;

        if (ServerMain.sv_client.download == null)
            return;

        r = ServerMain.sv_client.downloadsize - ServerMain.sv_client.downloadcount;
        if (r > 1024)
            r = 1024;

        Buffers.writeByte(ServerMain.sv_client.netchan.message, Constants.svc_download);
        ServerMain.sv_client.netchan.message.WriteShort(r);

        ServerMain.sv_client.downloadcount += r;
        size = ServerMain.sv_client.downloadsize;
        if (size == 0)
            size = 1;
        percent = ServerMain.sv_client.downloadcount * 100 / size;
        Buffers.writeByte(ServerMain.sv_client.netchan.message, percent);
        Buffers.Write(ServerMain.sv_client.netchan.message, ServerMain.sv_client.download,
                ServerMain.sv_client.downloadcount - r, r);

        if (ServerMain.sv_client.downloadcount != ServerMain.sv_client.downloadsize)
            return;

        QuakeFileSystem.FreeFile(ServerMain.sv_client.download);
        ServerMain.sv_client.download = null;
    }

    /*
     * ================== SV_BeginDownload_f ==================
     */
    public static void SV_BeginDownload_f() {
        String name;
        int offset = 0;

        name = Commands.Argv(1);

        if (Commands.Argc() > 2)
            offset = Lib.atoi(Commands.Argv(2)); // downloaded offset

        // hacked by zoid to allow more conrol over download
        // first off, no .. or global allow check

        if (name.indexOf("..") != -1
                || ServerMain.allow_download.value == 0 // leading dot is no good
                || name.charAt(0) == '.' // leading slash bad as well, must be
                                         // in subdir
                || name.charAt(0) == '/' // next up, skin check
                || (name.startsWith("players/") && 0 == ServerMain.allow_download_players.value) // now
                                                                                              // models
                || (name.startsWith("models/") && 0 == ServerMain.allow_download_models.value) // now
                                                                                            // sounds
                || (name.startsWith("sound/") && 0 == ServerMain.allow_download_sounds.value)
                // now maps (note special case for maps, must not be in pak)
                || (name.startsWith("maps/") && 0 == ServerMain.allow_download_maps.value) // MUST
                                                                                        // be
                                                                                        // in a
                                                                                        // subdirectory
                || name.indexOf('/') == -1) { // don't allow anything with ..
                                              // path
            Buffers.writeByte(ServerMain.sv_client.netchan.message,
                    Constants.svc_download);
            ServerMain.sv_client.netchan.message.WriteShort(-1);
            Buffers.writeByte(ServerMain.sv_client.netchan.message, 0);
            return;
        }

        if (ServerMain.sv_client.download != null)
            QuakeFileSystem.FreeFile(ServerMain.sv_client.download);

        ServerMain.sv_client.download = QuakeFileSystem.LoadFile(name);

        // rst: this handles loading errors, no message yet visible
        if (ServerMain.sv_client.download == null)
        {
        	return;
        }

        ServerMain.sv_client.downloadsize = ServerMain.sv_client.download.length;
        ServerMain.sv_client.downloadcount = offset;

        if (offset > ServerMain.sv_client.downloadsize)
            ServerMain.sv_client.downloadcount = ServerMain.sv_client.downloadsize;

        if (ServerMain.sv_client.download == null // special check for maps, if it
                                               // came from a pak file, don't
                                               // allow
                							   // download ZOID
                || (name.startsWith("maps/") && QuakeFileSystem.file_from_pak != 0)) {
            Com.DPrintf("Couldn't download " + name + " to "
                    + ServerMain.sv_client.name + "\n");
            if (ServerMain.sv_client.download != null) {
                QuakeFileSystem.FreeFile(ServerMain.sv_client.download);
                ServerMain.sv_client.download = null;
            }

            Buffers.writeByte(ServerMain.sv_client.netchan.message,
                    Constants.svc_download);
            ServerMain.sv_client.netchan.message.WriteShort(-1);
            Buffers.writeByte(ServerMain.sv_client.netchan.message, 0);
            return;
        }

        SV_NextDownload_f();
        Com.DPrintf("Downloading " + name + " to " + ServerMain.sv_client.name
                + "\n");
    }

    //============================================================================

    /*
     * ================= SV_Disconnect_f
     *
     * The client is going to disconnect, so remove the connection immediately
     * =================
     */
    public static void SV_Disconnect_f() {
        //	SV_EndRedirect ();
        ServerMain.SV_DropClient(ServerMain.sv_client);
    }

    /*
     * ================== SV_ShowServerinfo_f
     *
     * Dumps the serverinfo info string ==================
     */
    public static void SV_ShowServerinfo_f() {
        Info.Print(ConsoleVariables.Serverinfo());
    }

    public static void SV_Nextserver() {
        String v;

        //ZOID, ss_pic can be nextserver'd in coop mode
        if (ServerInit.sv.state == Constants.ss_game
                || (ServerInit.sv.state == Constants.ss_pic &&
                        0 == ConsoleVariables.VariableValue("coop")))
            return; // can't nextserver while playing a normal game

        ServerInit.svs.spawncount++; // make sure another doesn't sneak in
        v = ConsoleVariables.VariableString("nextserver");
        //if (!v[0])
        if (v.length() == 0)
            CommandBuffer.AddText("killserver\n");
        else {
            CommandBuffer.AddText(v);
            CommandBuffer.AddText("\n");
        }
        ConsoleVariables.Set("nextserver", "");
    }

    /*
     * ================== SV_Nextserver_f
     *
     * A cinematic has completed or been aborted by a client, so move to the
     * next server, ==================
     */
    public static void SV_Nextserver_f() {
        if (Lib.atoi(Commands.Argv(1)) != ServerInit.svs.spawncount) {
            Com.DPrintf("Nextserver() from wrong level, from "
                    + ServerMain.sv_client.name + "\n");
            return; // leftover from last server
        }

        Com.DPrintf("Nextserver() from " + ServerMain.sv_client.name + "\n");

        SV_Nextserver();
    }

    /*
     * ================== SV_ExecuteUserCommand ==================
     */
    public static void SV_ExecuteUserCommand(String s) {

        Com.dprintln("SV_ExecuteUserCommand:" + s );
        User.ucmd_t u = null;

        Commands.TokenizeString(s.toCharArray(), true);
        User.sv_player = ServerMain.sv_client.edict;

        //	SV_BeginRedirect (RD_CLIENT);

        int i = 0;
        for (; i < User.ucmds.length; i++) {
            u = User.ucmds[i];
            if (Commands.Argv(0).equals(u.name)) {
                u.r.run();
                break;
            }
        }

        if (i == User.ucmds.length && ServerInit.sv.state == Constants.ss_game) {
            //Commands.ClientCommand(User.sv_player);
            Globals.game.ClientCommand.accept(User.sv_player);
        }
        //	SV_EndRedirect ();
    }

    /*
     * ===========================================================================
     *
     * USER CMD EXECUTION
     *
     * ===========================================================================
     */

    public static void SV_ClientThink(ClientData cl, UserCommand cmd) {
        cl.commandMsec -= cmd.msec & 0xFF;

        if (cl.commandMsec < 0 && ServerMain.sv_enforcetime.value != 0) {
            Com.DPrintf("commandMsec underflow from " + cl.name + "\n");
            return;
        }

        //PlayerClient.ClientThink(cl.edict, cmd);
        Globals.game.ClientThink.accept(cl.edict, cmd);
    }

    /*
     * =================== SV_ExecuteClientMessage
     *
     * The current net_message is parsed for the given client
     * ===================
     */
    public static void SV_ExecuteClientMessage(ClientData cl) {
        int c;
        String s;

        UserCommand nullcmd = new UserCommand();
        UserCommand oldest = new UserCommand(), oldcmd = new UserCommand(), newcmd = new UserCommand();
        int net_drop;
        int stringCmdCount;
        int checksum, calculatedChecksum;
        int checksumIndex;
        boolean move_issued;
        int lastframe;

        ServerMain.sv_client = cl;
        User.sv_player = ServerMain.sv_client.edict;

        // only allow one move command
        move_issued = false;
        stringCmdCount = 0;

        while (true) {
            if (Globals.net_message.readcount > Globals.net_message.cursize) {
                Com.Printf("SV_ReadClientMessage: bad read:\n");
                Com.Printf(Lib.hexDump(Globals.net_message.data, 32, false));
                ServerMain.SV_DropClient(cl);
                return;
            }

            c = Buffers.readUnsignedByte(Globals.net_message);
            if (c == -1)
                break;

            switch (c) {
            default:
                Com.Printf("SV_ReadClientMessage: unknown command char\n");
                ServerMain.SV_DropClient(cl);
                return;

            case Constants.clc_nop:
                break;

            case Constants.clc_userinfo:
                cl.userinfo = Buffers.getString(Globals.net_message);
                ServerMain.SV_UserinfoChanged(cl);
                break;

            case Constants.clc_move:
                if (move_issued)
                    return; // someone is trying to cheat...

                move_issued = true;
                checksumIndex = Globals.net_message.readcount;
                checksum = Buffers.readUnsignedByte(Globals.net_message);
                lastframe = Globals.net_message.getInt();

                if (lastframe != cl.lastframe) {
                    cl.lastframe = lastframe;
                    if (cl.lastframe > 0) {
                        cl.frame_latency[cl.lastframe
                                & (Constants.LATENCY_COUNTS - 1)] = ServerInit.svs.realtime
                                - cl.frames[cl.lastframe & Constants.UPDATE_MASK].senttime;
                    }
                }

                //memset (nullcmd, 0, sizeof(nullcmd));
                nullcmd = new UserCommand();
                Delta.ReadDeltaUsercmd(Globals.net_message, nullcmd, oldest);
                Delta.ReadDeltaUsercmd(Globals.net_message, oldest, oldcmd);
                Delta.ReadDeltaUsercmd(Globals.net_message, oldcmd, newcmd);

                if (cl.state != Constants.cs_spawned) {
                    cl.lastframe = -1;
                    break;
                }

                // if the checksum fails, ignore the rest of the packet

                calculatedChecksum = Com.BlockSequenceCRCByte(
                        Globals.net_message.data, checksumIndex + 1,
                        Globals.net_message.readcount - checksumIndex - 1,
                        cl.netchan.incoming_sequence);

                if ((calculatedChecksum & 0xff) != checksum) {
                    Com.DPrintf("Failed command checksum for " + cl.name + " ("
                            + calculatedChecksum + " != " + checksum + ")/"
                            + cl.netchan.incoming_sequence + "\n");
                    return;
                }

                if (0 == ServerMain.sv_paused.value) {
                    net_drop = cl.netchan.dropped;
                    if (net_drop < 20) {

                        //if (net_drop > 2)

                        //	Com.Printf ("drop %i\n", net_drop);
                        while (net_drop > 2) {
                            SV_ClientThink(cl, cl.lastcmd);

                            net_drop--;
                        }
                        if (net_drop > 1)
                            SV_ClientThink(cl, oldest);

                        if (net_drop > 0)
                            SV_ClientThink(cl, oldcmd);

                    }
                    SV_ClientThink(cl, newcmd);
                }

                // copy.
                cl.lastcmd.set(newcmd);
                break;

            case Constants.clc_stringcmd:
                s = Buffers.getString(Globals.net_message);

                // malicious users may try using too many string commands
                if (++stringCmdCount < User.MAX_STRINGCMDS)
                    SV_ExecuteUserCommand(s);

                if (cl.state == Constants.cs_zombie)
                    return; // disconnect command
                break;
            }
        }
    }
}
