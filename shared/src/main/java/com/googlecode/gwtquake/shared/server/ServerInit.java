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



import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;

import com.googlecode.gwtquake.shared.client.Client;
import com.googlecode.gwtquake.shared.client.Screen;
import com.googlecode.gwtquake.shared.common.*;
import com.googlecode.gwtquake.shared.game.*;
import com.googlecode.gwtquake.shared.game.bot.BotMain;
import com.googlecode.gwtquake.shared.sys.NET;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.Math3D;

public class ServerInit {

    /**
     * SV_FindIndex.
     */
    public static int SV_FindIndex(String name, int start, int max,
            boolean create) {
        int i;

        if (name == null || name.length() == 0)
            return 0;

        for (i = 1; i < max && sv.configstrings[start + i] != null; i++)
            if (0 == Lib.strcmp(sv.configstrings[start + i], name))
                return i;

        if (!create)
            return 0;

        if (i == max)
            Com.Error(Constants.ERR_DROP, "*Index: overflow");

        sv.configstrings[start + i] = name;

        if (sv.state != Constants.ss_loading) {
            // send the update to everyone
            sv.multicast.clear();
            Buffers.writeByte(sv.multicast, Constants.svc_configstring);
            sv.multicast.WriteShort(start + i);
            Buffers.WriteString(sv.multicast, name);
            ServerSend.SV_Multicast(Globals.vec3_origin, Constants.MULTICAST_ALL_R);
        }

        return i;
    }

    public static int SV_ModelIndex(String name) {
        return SV_FindIndex(name, Constants.CS_MODELS, Constants.MAX_MODELS, true);
    }

    public static int SV_SoundIndex(String name) {
        return SV_FindIndex(name, Constants.CS_SOUNDS, Constants.MAX_SOUNDS, true);
    }

    public static int SV_ImageIndex(String name) {
        return SV_FindIndex(name, Constants.CS_IMAGES, Constants.MAX_IMAGES, true);
    }

    /**
     * SV_CreateBaseline
     *
     * Entity baselines are used to compress the update messages to the clients --
     * only the fields that differ from the baseline will be transmitted.
     */
    public static void SV_CreateBaseline() {
        Entity svent;
        int entnum;

        for (entnum = 1; entnum < GameBase.num_edicts; entnum++) {
            svent = GameBase.g_edicts[entnum];

            if (!svent.inuse)
                continue;
            if (0 == svent.s.modelindex && 0 == svent.s.sound
                    && 0 == svent.s.effects)
                continue;

            svent.s.number = entnum;

            // take current state as baseline
            Math3D.VectorCopy(svent.s.origin, svent.s.old_origin);
            sv.baselines[entnum].set(svent.s);
        }
    }

    /**
     * SV_CheckForSavegame.
     */
    public static void SV_CheckForSavegame() {

        String name;
        RandomAccessFile f;

        int i;

        if (ServerMain.sv_noreload.value != 0)
            return;

        if (ConsoleVariables.VariableValue("deathmatch") != 0)
            return;

        name = QuakeFileSystem.Gamedir() + "/save/current/" + sv.name + ".sav";
        try {
            f = new RandomAccessFile(name, "r");
        }

        catch (Exception e) {
        	Compatibility.printStackTrace(e);
            return;
        }

        try {
            f.close();
        } catch (IOException e1) {
            Compatibility.printStackTrace(e1);
        }

        World.SV_ClearWorld();

        // get configstrings and areaportals
        ServerCommands.SV_ReadLevelFile();

        if (!sv.loadgame) {
            // coming back to a level after being in a different
            // level, so run it for ten seconds

            // rlava2 was sending too many lightstyles, and overflowing the
            // reliable data. temporarily changing the server state to loading
            // prevents these from being passed down.
            int previousState; // PGM

            previousState = sv.state; // PGM
            sv.state = Constants.ss_loading; // PGM
            for (i = 0; i < 100; i++)
                //GameBase.G_RunFrame();
                Globals.game.RunFrame.run();

            sv.state = previousState; // PGM
        }
    }

    /**
     * SV_SpawnServer.
     *
     * Change the server to a new map, taking all connected clients along with
     * it.
     */
    public static void SV_SpawnServer(String server, final String spawnpoint,
            final int serverstate, boolean attractloop, boolean loadgame, final Command continueCommand) {
        int i;
        int checksum = 0;

        if (attractloop)
          ConsoleVariables.Set("paused", "0");

        Com.Printf("------- Server Initialization -------\n");

        Com.DPrintf("SpawnServer: " + server + "\n");
        sv.demofile = null;

        // any partially connected client will be restarted
        svs.spawncount++;

        sv.state = Constants.ss_dead;

        Globals.server_state = sv.state;

        // wipe the entire per-level structure
        sv = new ServerState();

        svs.realtime = 0;
        sv.loadgame = loadgame;
        sv.attractloop = attractloop;

        // save name for levels that don't set message
        sv.configstrings[Constants.CS_NAME] = server;

        if (ConsoleVariables.VariableValue("deathmatch") != 0) {
            sv.configstrings[Constants.CS_AIRACCEL] = ""
                    + ServerMain.sv_airaccelerate.value;
            PlayerMovements.pm_airaccelerate = ServerMain.sv_airaccelerate.value;
        } else {
            sv.configstrings[Constants.CS_AIRACCEL] = "0";
            PlayerMovements.pm_airaccelerate = 0;
        }

        sv.multicast = Buffer.wrap(sv.multicast_buf).order(ByteOrder.LITTLE_ENDIAN);

        sv.name = server;

        // leave slots at start for clients only
        for (i = 0; i < ServerMain.maxclients.value; i++) {
            // needs to reconnect
            if (svs.clients[i].state > Constants.cs_connected)
                svs.clients[i].state = Constants.cs_connected;
            svs.clients[i].lastframe = -1;
        }

        sv.time = 1000;

        sv.name = server;
        sv.configstrings[Constants.CS_NAME] = server;

        final int iw[] = {checksum };

        CM.ModelCallback onLoad = new CM.ModelCallback() {

			public void onSuccess(Model response) {
				sv.models[1] = response;
				int checksum = iw[0];
		        sv.configstrings[Constants.CS_MAPCHECKSUM] = "" + checksum;
				// clear physics interaction links

				World.SV_ClearWorld();

				for (int i = 1; i < CM.CM_NumInlineModels(); i++) {
					sv.configstrings[Constants.CS_MODELS + 1 + i] = "*" + i;

					// copy references
					sv.models[i + 1] = CM.InlineModel(sv.configstrings[Constants.CS_MODELS + 1 + i]);
				}


				// spawn the rest of the entities on the map

				// precache and static commands can be issued during
				// map initialization

				sv.state = Constants.ss_loading;
				Globals.server_state = sv.state;

				// load and spawn all other entities
				//GameSpawn.SpawnEntities(sv.name, CM.CM_EntityString(), spawnpoint);
                Globals.game.SpawnEntities.apply(sv.name, CM.CM_EntityString(), spawnpoint);

				// run two frames to allow everything to settle
				//GameBase.G_RunFrame();
				//GameBase.G_RunFrame();
                Globals.game.RunFrame.run();
                Globals.game.RunFrame.run();

				// all precaches are complete
				sv.state = serverstate;
				Globals.server_state = sv.state;

				// create a baseline for more efficient communications
				SV_CreateBaseline();

				// check for a savegame
				SV_CheckForSavegame();

				// set serverinfo variable
				ConsoleVariables.FullSet("mapname", sv.name, Constants.CVAR_SERVERINFO
						| Constants.CVAR_NOSET);

        		continueCommand.execute();
        	}

        };


        if (serverstate != Constants.ss_game) {
        	CM.CM_LoadMap("", false, iw, onLoad); // no real map
        } else {
            sv.configstrings[Constants.CS_MODELS + 1] = "maps/" + server + ".bsp";
            CM.CM_LoadMap(sv.configstrings[Constants.CS_MODELS + 1], false, iw, onLoad);
        }


    }


    /**
     * SV_InitGame.
     *
     * A brand new game has been started.
     */
    public static void SV_InitGame() {
        int i;
        Entity ent;
        //char idmaster[32];
        String idmaster;

        if (svs.initialized) {
            // cause any connected clients to reconnect
            ServerMain.SV_Shutdown("Server restarted\n", true);
        } else {
            // make sure the client is down
            Client.drop();
            Screen.BeginLoadingPlaque();
        }

        // get any latched variable changes (maxclients, etc)
        ConsoleVariables.GetLatchedVars();

        svs.initialized = true;

        if (ConsoleVariables.VariableValue("coop") != 0
                && ConsoleVariables.VariableValue("deathmatch") != 0) {
            Com.Printf("Deathmatch and Coop both set, disabling Coop\n");
            ConsoleVariables.FullSet("coop", "0", Constants.CVAR_SERVERINFO
                    | Constants.CVAR_LATCH);
        }

        // dedicated servers are can't be single player and are usually DM
        // so unless they explicity set coop, force it to deathmatch
        if (Globals.dedicated.value != 0) {
            if (0 == ConsoleVariables.VariableValue("coop"))
                ConsoleVariables.FullSet("deathmatch", "1", Constants.CVAR_SERVERINFO
                        | Constants.CVAR_LATCH);
        }

        // init clients
        if (ConsoleVariables.VariableValue("deathmatch") != 0) {
            if (ServerMain.maxclients.value <= 1)
                ConsoleVariables.FullSet("maxclients", "8", Constants.CVAR_SERVERINFO
                        | Constants.CVAR_LATCH);
            else if (ServerMain.maxclients.value > Constants.MAX_CLIENTS)
                ConsoleVariables.FullSet("maxclients", "" + Constants.MAX_CLIENTS,
                        Constants.CVAR_SERVERINFO | Constants.CVAR_LATCH);
        } else if (ConsoleVariables.VariableValue("coop") != 0) {
            if (ServerMain.maxclients.value <= 1 || ServerMain.maxclients.value > 4)
                ConsoleVariables.FullSet("maxclients", "4", Constants.CVAR_SERVERINFO
                        | Constants.CVAR_LATCH);

        } else // non-deathmatch, non-coop is one player
        {
            ConsoleVariables.FullSet("maxclients", "1", Constants.CVAR_SERVERINFO
                    | Constants.CVAR_LATCH);
        }

        svs.spawncount = Lib.rand();
        svs.clients = new ClientData[(int) ServerMain.maxclients.value];
        for (int n = 0; n < svs.clients.length; n++) {
            svs.clients[n] = new ClientData();
            svs.clients[n].serverindex = n;
        }
        svs.num_client_entities = ((int) ServerMain.maxclients.value)
                * Constants.UPDATE_BACKUP * 64; //ok.

        svs.client_entities = new EntityState[svs.num_client_entities];
        for (int n = 0; n < svs.client_entities.length; n++)
            svs.client_entities[n] = new EntityState(null);

        // init network stuff
        NET.Config((ServerMain.maxclients.value > 1));

        // heartbeats will always be sent to the id master
        svs.last_heartbeat = -99999; // send immediately
        idmaster = "192.246.40.37:" + Constants.PORT_MASTER;
        NET.StringToAdr(idmaster, ServerMain.master_adr[0]);

        // init game
        ServerGame.SV_InitGameProgs();

        for (i = 0; i < ServerMain.maxclients.value; i++) {
            ent = GameBase.g_edicts[i + 1];
            svs.clients[i].edict = ent;
            svs.clients[i].lastcmd = new UserCommand();
        }

        // Initialize bot subsystem
        BotMain.init();
    }

    private static String firstmap = "";

    /**
     * SV_Map
     *
     * the full syntax is:
     *
     * map [*] <map>$ <startspot>+ <nextserver>
     *
     * command from the console or progs. Map can also be a.cin, .pcx, or .dm2 file.
     *
     * Nextserver is used to allow a cinematic to play, then proceed to
     * another level:
     *
     * map tram.cin+jail_e3
     */
    public static void SV_Map(boolean attractloop, String levelstring, boolean loadgame,
    		final Command continueCmd) {
        int l;
        String level, spawnpoint;

        sv.loadgame = loadgame;
        sv.attractloop = attractloop;

        if (sv.state == Constants.ss_dead && !sv.loadgame) {
            //SV_InitGame(); // the game is just starting
            Globals.game.Init.run();
        }

        level = levelstring; // bis hier her ok.

        // if there is a + in the map, set nextserver to the remainder

        int c = level.indexOf('+');
        if (c != -1) {
            ConsoleVariables.Set("nextserver", "gamemap \"" + level.substring(c + 1) + "\"");
            level = level.substring(0, c);
        } else {
            ConsoleVariables.Set("nextserver", "");
        }

        // rst: base1 works for full, damo1 works for demo, so we need to store first map.
        if (firstmap.length() == 0)
        {
        	if (!levelstring.endsWith(".cin") && !levelstring.endsWith(".pcx") && !levelstring.endsWith(".dm2"))
        	{
        		int pos = levelstring.indexOf('+');
        		firstmap = levelstring.substring(pos + 1);
        	}
        }

        // ZOID: special hack for end game screen in coop mode
        if (ConsoleVariables.VariableValue("coop") != 0 && level.equals("victory.pcx"))
            ConsoleVariables.Set("nextserver", "gamemap \"*" + firstmap + "\"");

        // if there is a $, use the remainder as a spawnpoint
        int pos = level.indexOf('$');
        if (pos != -1) {
            spawnpoint = level.substring(pos + 1);
            level = level.substring(0, pos);

        } else
            spawnpoint = "";

        // skip the end-of-unit flag * if necessary
        if (level.charAt(0) == '*')
            level = level.substring(1);

        l = level.length();

        Command continueCmd2 = new Command() {
			public void execute() {
		        ServerSend.SV_BroadcastCommand("reconnect\n");
		        continueCmd.execute();
			}
        };

        if (l > 4 && level.endsWith(".cin")) {
            Screen.BeginLoadingPlaque(); // for local system
            ServerSend.SV_BroadcastCommand("changing\n");
            SV_SpawnServer(level, spawnpoint, Constants.ss_cinematic, attractloop,
                     loadgame, continueCmd2);
        } else if (l > 4 && level.endsWith(".dm2")) {
            Screen.BeginLoadingPlaque(); // for local system
            ServerSend.SV_BroadcastCommand("changing\n");
            SV_SpawnServer(level, spawnpoint, Constants.ss_demo, attractloop,
                    loadgame, continueCmd2);
        } else if (l > 4 && level.endsWith(".pcx")) {
            Screen.BeginLoadingPlaque(); // for local system
            ServerSend.SV_BroadcastCommand("changing\n");
            SV_SpawnServer(level, spawnpoint, Constants.ss_pic, attractloop,
                    loadgame, continueCmd2);
        } else {
            Screen.BeginLoadingPlaque(); // for local system
            ServerSend.SV_BroadcastCommand("changing\n");
            ServerSend.SV_SendClientMessages();
            SV_SpawnServer(level, spawnpoint, Constants.ss_game, attractloop,
                    loadgame, new Command() {
            	public void execute() {
            		CommandBuffer.CopyToDefer();
            		ServerSend.SV_BroadcastCommand("reconnect\n");
            		continueCmd.execute();
            	}
            });
        }
    }

    public static ServerStatic svs = new ServerStatic(); // persistant
                                                               // server info

    public static ServerState sv = new ServerState(); // local server
}
