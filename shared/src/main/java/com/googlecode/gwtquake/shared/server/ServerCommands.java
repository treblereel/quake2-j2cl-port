/*
Copyright (C) 1997-2001 Id Software, Inc.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/
/* Modifications
   Copyright 2003-2004 Bytonic Software
   Copyright 2010 Google Inc.
*/
package com.googlecode.gwtquake.shared.server;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.Calendar;

import com.googlecode.gwtquake.shared.common.Buffer;
import com.googlecode.gwtquake.shared.common.Buffers;
import com.googlecode.gwtquake.shared.common.CM;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.Command;
import com.googlecode.gwtquake.shared.common.Compatibility;
import com.googlecode.gwtquake.shared.common.ConsoleVariables;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.ExecutableCommand;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.common.NetworkAddress;
import com.googlecode.gwtquake.shared.common.NetworkChannel;
import com.googlecode.gwtquake.shared.common.QuakeFileSystem;
import com.googlecode.gwtquake.shared.game.Commands;
import com.googlecode.gwtquake.shared.game.ConsoleVariable;
import com.googlecode.gwtquake.shared.game.EndianHandler;
import com.googlecode.gwtquake.shared.game.GameSVCmds;
import com.googlecode.gwtquake.shared.game.GameSave;
import com.googlecode.gwtquake.shared.game.Info;
import com.googlecode.gwtquake.shared.sys.NET;
import com.googlecode.gwtquake.shared.sys.Sys;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.QuakeFile;
import com.googlecode.gwtquake.shared.util.Vargs;

public class ServerCommands {

	/*
	===============================================================================

	OPERATOR CONSOLE ONLY COMMANDS

	These commands can only be entered from stdin or by a remote operator datagram
	===============================================================================
	*/

	private static final Command EMPTY_COMMAND = new Command() {
		public void execute() {
		}
	};

	/*
	====================
	SV_SetMaster_f

	Specify a list of master servers
	====================
	*/
	public static void SV_SetMaster_f() {
		int i, slot;

		// only dedicated servers send heartbeats
		if (Globals.dedicated.value == 0) {
			Com.Printf("Only dedicated servers use masters.\n");
			return;
		}

		// make sure the server is listed public
		ConsoleVariables.Set("public", "1");

		for (i = 1; i < Constants.MAX_MASTERS; i++)
			ServerMain.master_adr[i] = new NetworkAddress();

		slot = 1; // slot 0 will always contain the id master
		for (i = 1; i < Commands.Argc(); i++) {
			if (slot == Constants.MAX_MASTERS)
				break;

			if (!NET.StringToAdr(Commands.Argv(i), ServerMain.master_adr[i])) {
				Com.Printf("Bad address: " + Commands.Argv(i) + "\n");
				continue;
			}
			if (ServerMain.master_adr[slot].port == 0)
				ServerMain.master_adr[slot].port = Constants.PORT_MASTER;

			Com.Printf("Master server at " + NET.AdrToString(ServerMain.master_adr[slot]) + "\n");
			Com.Printf("Sending a ping.\n");

			NetworkChannel.OutOfBandPrint(Constants.NS_SERVER, ServerMain.master_adr[slot], "ping");

			slot++;
		}

		ServerInit.svs.last_heartbeat = -9999999;
	}
	/*
	==================
	SV_SetPlayer

	Sets sv_client and sv_player to the player with idnum Cmd.Argv(1)
	==================
	*/
	public static boolean SV_SetPlayer() {
		ClientData cl;
		int i;
		int idnum;
		String s;

		if (Commands.Argc() < 2)
			return false;

		s = Commands.Argv(1);

		// numeric values are just slot numbers
		if (s.charAt(0) >= '0' && s.charAt(0) <= '9') {
			idnum = Lib.atoi(Commands.Argv(1));
			if (idnum < 0 || idnum >= ServerMain.maxclients.value) {
				Com.Printf("Bad client slot: " + idnum + "\n");
				return false;
			}

			ServerMain.sv_client = ServerInit.svs.clients[idnum];
			User.sv_player = ServerMain.sv_client.edict;
			if (0 == ServerMain.sv_client.state) {
				Com.Printf("Client " + idnum + " is not active\n");
				return false;
			}
			return true;
		}

		// check for a name match
		for (i = 0; i < ServerMain.maxclients.value; i++) {
			cl = ServerInit.svs.clients[i];
			if (0 == cl.state)
				continue;
			if (0 == Lib.strcmp(cl.name, s)) {
				ServerMain.sv_client = cl;
				User.sv_player = ServerMain.sv_client.edict;
				return true;
			}
		}

		Com.Printf("Userid " + s + " is not on the server\n");
		return false;
	}
	/*
	===============================================================================

	SAVEGAME FILES

	===============================================================================
	*/

	public static void remove(String name) {
		try {
			new File(name).delete();
		}
		catch (Exception e) {
        	Compatibility.printStackTrace(e);
		}
	}

	/** Delete save files save/(number)/.  */
	public static void SV_WipeSavegame(String savename) {

		String name;

		Com.DPrintf("SV_WipeSaveGame(" + savename + ")\n");

		name = QuakeFileSystem.Gamedir() + "/save/" + savename + "/server.ssv";
		remove(name);

		name = QuakeFileSystem.Gamedir() + "/save/" + savename + "/game.ssv";
		remove(name);

		name = QuakeFileSystem.Gamedir() + "/save/" + savename + "/*.sav";

		File f = Sys.FindFirst(name, 0, 0);
		while (f != null) {
			f.delete();
			f = Sys.FindNext();
		}
		Sys.FindClose();

		name = QuakeFileSystem.Gamedir() + "/save/" + savename + "/*.sv2";

		f = Sys.FindFirst(name, 0, 0);

		while (f != null) {
			f.delete();
			f = Sys.FindNext();
		}
		Sys.FindClose();
	}
	/*
	================
	CopyFile
	================
	*/
	public static void CopyFile(String src, String dst) {
		RandomAccessFile f1, f2;
		int l = -1;
		byte buffer[] = new byte[65536];

		//Com.DPrintf("CopyFile (" + src + ", " + dst + ")\n");
		try {
			f1 = new RandomAccessFile(src, "r");
		}
		catch (Exception e) {

        	Compatibility.printStackTrace(e);

			return;
		}
		try {
			f2 = new RandomAccessFile(dst, "rw");
		}
		catch (Exception e) {
			try {
				f1.close();
			}
			catch (IOException e1) {
				Compatibility.printStackTrace(e1);
			}
			return;
		}

		while (true) {

			try {
				l = f1.read(buffer, 0, 65536);
			}
			catch (IOException e1) {

				Compatibility.printStackTrace(e1);
			}
			if (l == -1)
				break;
			try {
				f2.write(buffer, 0, l);
			}
			catch (IOException e2) {

				Compatibility.printStackTrace(e2);
			}
		}

		try {
			f1.close();
		}
		catch (IOException e1) {

			Compatibility.printStackTrace(e1);
		}
		try {
			f2.close();
		}
		catch (IOException e2) {

			Compatibility.printStackTrace(e2);
		}
	}
	/*
	================
	SV_CopySaveGame
	================
	*/
	public static void SV_CopySaveGame(String src, String dst) {
		//char name[MAX_OSPATH], name2[MAX_OSPATH];
		int l, len;
		File found;

		String name, name2;

		Com.DPrintf("SV_CopySaveGame(" + src + "," + dst + ")\n");

		SV_WipeSavegame(dst);

		// copy the savegame over
		name = QuakeFileSystem.Gamedir() + "/save/" + src + "/server.ssv";
		name2 = QuakeFileSystem.Gamedir() + "/save/" + dst + "/server.ssv";
		QuakeFileSystem.CreatePath(name2);
		CopyFile(name, name2);

		name = QuakeFileSystem.Gamedir() + "/save/" + src + "/game.ssv";
		name2 = QuakeFileSystem.Gamedir() + "/save/" + dst + "/game.ssv";
		CopyFile(name, name2);

		String name1 = QuakeFileSystem.Gamedir() + "/save/" + src + "/";
		len = name1.length();
		name = QuakeFileSystem.Gamedir() + "/save/" + src + "/*.sav";

		found = Sys.FindFirst(name, 0, 0);

		while (found != null) {
			name = name1 + found.getName();
			name2 = QuakeFileSystem.Gamedir() + "/save/" + dst + "/" + found.getName();

			CopyFile(name, name2);

			// change sav to sv2
			name = name.substring(0, name.length() - 3) + "sv2";
			name2 = name2.substring(0, name2.length() - 3) + "sv2";

			CopyFile(name, name2);

			found = Sys.FindNext();
		}
		Sys.FindClose();
	}
	/*
	==============
	SV_WriteLevelFile

	==============
	*/
	public static void SV_WriteLevelFile() {

		String name;
		QuakeFile f;

		Com.DPrintf("SV_WriteLevelFile()\n");

		name = QuakeFileSystem.Gamedir() + "/save/current/" + ServerInit.sv.name + ".sv2";

		try {
			f = new QuakeFile(name, "rw");

			for (int i = 0; i < Constants.MAX_CONFIGSTRINGS; i++)
				f.writeString(ServerInit.sv.configstrings[i]);

			CM.CM_WritePortalState(f);
			f.close();
		}
		catch (Exception e) {
			Com.Printf("Failed to open " + name + "\n");
			Compatibility.printStackTrace(e);
		}

		name = QuakeFileSystem.Gamedir() + "/save/current/" + ServerInit.sv.name + ".sav";
		//GameSave.WriteLevel(name);
		Globals.game.WriteLevel.accept(name);
	}
	/*
	==============
	SV_ReadLevelFile
	==============
	*/
	public static void SV_ReadLevelFile() {
		//char name[MAX_OSPATH];
		String name;
		QuakeFile f;

		Com.DPrintf("SV_ReadLevelFile()\n");

		name = QuakeFileSystem.Gamedir() + "/save/current/" + ServerInit.sv.name + ".sv2";
		try {
			f = new QuakeFile(name, "r");

			for (int n = 0; n < Constants.MAX_CONFIGSTRINGS; n++)
				ServerInit.sv.configstrings[n] = f.readString();

			CM.CM_ReadPortalState(f);

			f.close();
		}
		catch (IOException e1) {
			Com.Printf("Failed to open " + name + "\n");
			Compatibility.printStackTrace(e1);
		}

		name = QuakeFileSystem.Gamedir() + "/save/current/" + ServerInit.sv.name + ".sav";
		//GameSave.ReadLevel(name);
		Globals.game.ReadLevel.accept(name);
	}
	/*
	==============
	SV_WriteServerFile
	==============
	*/
	public static void SV_WriteServerFile(boolean autosave) {
		QuakeFile f;
		ConsoleVariable var;

		String filename, name, string, comment;

		Com.DPrintf("SV_WriteServerFile(" + (autosave ? "true" : "false") + ")\n");

		filename = QuakeFileSystem.Gamedir() + "/save/current/server.ssv";
		try {
			f = new QuakeFile(filename, "rw");

			if (!autosave) {
				Calendar c = Calendar.getInstance();
				comment =
					Com.sprintf(
						"%2i:%2i %2i/%2i  ",
						new Vargs().add(c.get(Calendar.HOUR_OF_DAY)).add(c.get(Calendar.MINUTE)).add(
							c.get(Calendar.MONTH) + 1).add(
							c.get(Calendar.DAY_OF_MONTH)));
				comment += ServerInit.sv.configstrings[Constants.CS_NAME];
			}
			else {
				// autosaved
				comment = "ENTERING " + ServerInit.sv.configstrings[Constants.CS_NAME];
			}

			f.writeString(comment);
			f.writeString(ServerInit.svs.mapcmd);

			// write the mapcmd

			// write all CVAR_LATCH cvars
			// these will be things like coop, skill, deathmatch, etc
			for (var = Globals.cvar_vars; var != null; var = var.next) {
				if (0 == (var.flags & Constants.CVAR_LATCH))
					continue;
				if (var.name.length() >= Constants.MAX_OSPATH - 1 || var.string.length() >= 128 - 1) {
					Com.Printf("Cvar too long: " + var.name + " = " + var.string + "\n");
					continue;
				}

				name = var.name;
				string = var.string;
				try {
					f.writeString(name);
					f.writeString(string);
				}
				catch (IOException e2) {
				}

			}
			// rst: for termination.
			f.writeString(null);
			f.close();
		}
		catch (Exception e) {
        	Compatibility.printStackTrace(e);

			Com.Printf("Couldn't write " + filename + "\n");
		}

		// write game state
		filename = QuakeFileSystem.Gamedir() + "/save/current/game.ssv";
		//GameSave.WriteGame(filename, autosave);
		Globals.game.WriteGame.accept(filename, autosave);
	}
	/*
	==============
	SV_ReadServerFile

	==============
	*/
	public static void SV_ReadServerFile() {
		String filename="", name = "", string, comment, mapcmd;
		try {
			QuakeFile f;

			mapcmd = "";

			Com.DPrintf("SV_ReadServerFile()\n");

			filename = QuakeFileSystem.Gamedir() + "/save/current/server.ssv";

			f = new QuakeFile(filename, "r");

			// read the comment field
			comment = f.readString();

			// read the mapcmd
			mapcmd = f.readString();

			// read all CVAR_LATCH cvars
			// these will be things like coop, skill, deathmatch, etc
			while (true) {
				name = f.readString();
				if (name == null)
					break;
				string = f.readString();

				Com.DPrintf("Set " + name + " = " + string + "\n");
				ConsoleVariables.ForceSet(name, string);
			}

			f.close();

			// start a new game fresh with new cvars
			//ServerInit.SV_InitGame();
			Globals.game.Init.run();

			ServerInit.svs.mapcmd = mapcmd;

			// read game state
			filename = QuakeFileSystem.Gamedir() + "/save/current/game.ssv";
			//GameSave.ReadGame(filename);
			Globals.game.ReadGame.accept(filename);
		}
		catch (Exception e) {
			Com.Printf("Couldn't read file " + filename + "\n");
			Compatibility.printStackTrace(e);
		}
	}
	//=========================================================

	/*
	==================
	SV_DemoMap_f

	Puts the server in demo mode on a specific map/cinematic
	==================
	*/
	public static void SV_DemoMap_f() {
		ServerInit.SV_Map(true, Commands.Argv(1), false, EMPTY_COMMAND);
	}
	/*
	==================
	SV_GameMap_f

	Saves the state of the map just being exited and goes to a new map.

	If the initial character of the map string is '*', the next map is
	in a new unit, so the current savegame directory is cleared of
	map files.

	Example:

	*inter.cin+jail

	Clears the archived maps, plays the inter.cin cinematic, then
	goes to map jail.bsp.
	==================
	*/
	public static void SV_GameMap_f() {
		String map;
		int i;
		ClientData cl;
		boolean savedInuse[];

		if (Commands.Argc() != 2) {
			Com.Printf("USAGE: gamemap <map>\n");
			return;
		}

		Com.DPrintf("SV_GameMap(" + Commands.Argv(1) + ")\n");

		QuakeFileSystem.CreatePath(QuakeFileSystem.Gamedir() + "/save/current/");

		// check for clearing the current savegame
		map = Commands.Argv(1);
		if (map.charAt(0) == '*') {
			// wipe all the *.sav files
			SV_WipeSavegame("current");
		}
		else { // save the map just exited
			if (ServerInit.sv.state == Constants.ss_game) {
				// clear all the client inuse flags before saving so that
				// when the level is re-entered, the clients will spawn
				// at spawn points instead of occupying body shells
				savedInuse = new boolean[(int) ServerMain.maxclients.value];
				for (i = 0; i < ServerMain.maxclients.value; i++) {
					cl = ServerInit.svs.clients[i];
					savedInuse[i] = cl.edict.inuse;
					cl.edict.inuse = false;
				}

				SV_WriteLevelFile();

				// we must restore these for clients to transfer over correctly
				for (i = 0; i < ServerMain.maxclients.value; i++) {
					cl = ServerInit.svs.clients[i];
					cl.edict.inuse = savedInuse[i];

				}
				savedInuse = null;
			}
		}

		Command continueCmd = new Command() {

			public void execute() {
				// archive server state
				ServerInit.svs.mapcmd = Commands.Argv(1);

				// copy off the level to the autosave slot
				if (0 == Globals.dedicated.value) {
					SV_WriteServerFile(true);
					SV_CopySaveGame("current", "save0");
				}
			}
		};

		// start up the next map
		ServerInit.SV_Map(false, Commands.Argv(1), false, continueCmd);


	}
	/*
	==================
	SV_Map_f

	Goes directly to a given map without any savegame archiving.
	For development work
	==================
	*/
	public static void SV_Map_f() {
		String map;
		//char expanded[MAX_QPATH];
		String expanded;

		// if not a pcx, demo, or cinematic, check to make sure the level exists
		map = Commands.Argv(1);
//		if (map.indexOf(".") < 0) {
//			expanded = "maps/" + map + ".bsp";
//			if (FS.LoadFile(expanded) == null) {
//
//				Com.Printf("Can't find " + expanded + "\n");
//				return;
//			}
//		}

		ServerInit.sv.state = Constants.ss_dead; // don't save current level when changing

		SV_WipeSavegame("current");
		SV_GameMap_f();
	}
	/*
	=====================================================================

	  SAVEGAMES

	=====================================================================
	*/

	/*
	==============
	SV_Loadgame_f

	==============
	*/
	public static void SV_Loadgame_f() {

		String name;
		RandomAccessFile f;
		String dir;

		if (Commands.Argc() != 2) {
			Com.Printf("USAGE: loadgame <directory>\n");
			return;
		}

		Com.Printf("Loading game...\n");

		dir = Commands.Argv(1);
		if ( (dir.indexOf("..") > -1) || (dir.indexOf("/") > -1) || (dir.indexOf("\\") > -1)) {
			Com.Printf("Bad savedir.\n");
		}

		// make sure the server.ssv file exists
		name = QuakeFileSystem.Gamedir() + "/save/" + Commands.Argv(1) + "/server.ssv";
		try {
			f = new RandomAccessFile(name, "r");
		}
		catch (FileNotFoundException e) {
			Com.Printf("No such savegame: " + name + "\n");

        	Compatibility.printStackTrace(e);

			return;
		}

		try {
			f.close();
		}
		catch (IOException e1) {
			Compatibility.printStackTrace(e1);
		}

		SV_CopySaveGame(Commands.Argv(1), "current");
		SV_ReadServerFile();

		// go to the map
		ServerInit.sv.state = Constants.ss_dead; // don't save current level when changing
		ServerInit.SV_Map(false, ServerInit.svs.mapcmd, true, EMPTY_COMMAND);
	}
	/*
	==============
	SV_Savegame_f

	==============
	*/
	public static void SV_Savegame_f() {
		String dir;

		if (ServerInit.sv.state != Constants.ss_game) {
			Com.Printf("You must be in a game to save.\n");
			return;
		}

		if (Commands.Argc() != 2) {
			Com.Printf("USAGE: savegame <directory>\n");
			return;
		}

		if (ConsoleVariables.VariableValue("deathmatch") != 0) {
			Com.Printf("Can't savegame in a deathmatch\n");
			return;
		}

		if (0 == Lib.strcmp(Commands.Argv(1), "current")) {
			Com.Printf("Can't save to 'current'\n");
			return;
		}

		if (ServerMain.maxclients.value == 1 && ServerInit.svs.clients[0].edict.client.ps.stats[Constants.STAT_HEALTH] <= 0) {
			Com.Printf("\nCan't savegame while dead!\n");
			return;
		}

		dir = Commands.Argv(1);
		if ( (dir.indexOf("..") > -1) || (dir.indexOf("/") > -1) || (dir.indexOf("\\") > -1)) {
			Com.Printf("Bad savedir.\n");
		}

		Com.Printf("Saving game...\n");

		// archive current level, including all client edicts.
		// when the level is reloaded, they will be shells awaiting
		// a connecting client
		SV_WriteLevelFile();

		// save server state
		try {
			SV_WriteServerFile(false);
		}
		catch (Exception e) {
			Com.Printf("IOError in SV_WriteServerFile: " + e);
        	Compatibility.printStackTrace(e);
		}

		// copy it off
		SV_CopySaveGame("current", dir);
		Com.Printf("Done.\n");
	}
	//===============================================================
	/*
	==================
	SV_Kick_f

	Kick a user off of the server
	==================
	*/
	public static void SV_Kick_f() {
		if (!ServerInit.svs.initialized) {
			Com.Printf("No server running.\n");
			return;
		}

		if (Commands.Argc() != 2) {
			Com.Printf("Usage: kick <userid>\n");
			return;
		}

		if (!SV_SetPlayer())
			return;

		ServerSend.SV_BroadcastPrintf(Constants.PRINT_HIGH, ServerMain.sv_client.name + " was kicked\n");
		// print directly, because the dropped client won't get the
		// SV_BroadcastPrintf message
		ServerSend.SV_ClientPrintf(ServerMain.sv_client, Constants.PRINT_HIGH, "You were kicked from the game\n");
		ServerMain.SV_DropClient(ServerMain.sv_client);
		ServerMain.sv_client.lastmessage = ServerInit.svs.realtime; // min case there is a funny zombie
	}
	/*
	================
	SV_Status_f
	================
	*/
	public static void SV_Status_f() {
		int i, j, l;
		ClientData cl;
		String s;
		int ping;
		if (ServerInit.svs.clients == null) {
			Com.Printf("No server running.\n");
			return;
		}
		Com.Printf("map              : " + ServerInit.sv.name + "\n");

		Com.Printf("num score ping name            lastmsg address               qport \n");
		Com.Printf("--- ----- ---- --------------- ------- --------------------- ------\n");
		for (i = 0; i < ServerMain.maxclients.value; i++) {
			cl = ServerInit.svs.clients[i];
			if (0 == cl.state)
				continue;

			Com.Printf("%3i ", new Vargs().add(i));
			Com.Printf("%5i ", new Vargs().add(cl.edict.client.ps.stats[Constants.STAT_FRAGS]));

			if (cl.state == Constants.cs_connected)
				Com.Printf("CNCT ");
			else if (cl.state == Constants.cs_zombie)
				Com.Printf("ZMBI ");
			else {
				ping = cl.ping < 9999 ? cl.ping : 9999;
				Com.Printf("%4i ", new Vargs().add(ping));
			}

			Com.Printf("%s", new Vargs().add(cl.name));
			l = 16 - cl.name.length();
			for (j = 0; j < l; j++)
				Com.Printf(" ");

			Com.Printf("%7i ", new Vargs().add(ServerInit.svs.realtime - cl.lastmessage));

			s = NET.AdrToString(cl.netchan.remote_address);
			Com.Printf(s);
			l = 22 - s.length();
			for (j = 0; j < l; j++)
				Com.Printf(" ");

			Com.Printf("%5i", new Vargs().add(cl.netchan.qport));

			Com.Printf("\n");
		}
		Com.Printf("\n");
	}
	/*
	==================
	SV_ConSay_f
	==================
	*/
	public static void SV_ConSay_f() {
		ClientData client;
		int j;
		String p;
		String text; // char[1024];

		if (Commands.Argc() < 2)
			return;

		text = "console: ";
		p = Commands.Args();

		if (p.charAt(0) == '"') {
			p = p.substring(1, p.length() - 1);
		}

		text += p;

		for (j = 0; j < ServerMain.maxclients.value; j++) {
			client = ServerInit.svs.clients[j];
			if (client.state != Constants.cs_spawned)
				continue;
			ServerSend.SV_ClientPrintf(client, Constants.PRINT_CHAT, text + "\n");
		}
	}
	/*
	==================
	SV_Heartbeat_f
	==================
	*/
	public static void SV_Heartbeat_f() {
		ServerInit.svs.last_heartbeat = -9999999;
	}
	/*
	===========
	SV_Serverinfo_f

	  Examine or change the serverinfo string
	===========
	*/
	public static void SV_Serverinfo_f() {
		Com.Printf("Server info settings:\n");
		Info.Print(ConsoleVariables.Serverinfo());
	}
	/*
	===========
	SV_DumpUser_f

	Examine all a users info strings
	===========
	*/
	public static void SV_DumpUser_f() {
		if (Commands.Argc() != 2) {
			Com.Printf("Usage: info <userid>\n");
			return;
		}

		if (!SV_SetPlayer())
			return;

		Com.Printf("userinfo\n");
		Com.Printf("--------\n");
		Info.Print(ServerMain.sv_client.userinfo);

	}
	/*
	==============
	SV_ServerRecord_f

	Begins server demo recording.  Every entity and every message will be
	recorded, but no playerinfo will be stored.  Primarily for demo merging.
	==============
	*/
	public static void SV_ServerRecord_f() {
		//char	name[MAX_OSPATH];
		String name;
		int len;
		int i;

		if (Commands.Argc() != 2) {
			Com.Printf("serverrecord <demoname>\n");
			return;
		}

		if (ServerInit.svs.demofile != null) {
			Com.Printf("Already recording.\n");
			return;
		}

		if (ServerInit.sv.state != Constants.ss_game) {
			Com.Printf("You must be in a level to record.\n");
			return;
		}

		//
		// open the demo file
		//
		name = QuakeFileSystem.Gamedir() + "/demos/" + Commands.Argv(1) + ".dm2";

		Com.Printf("recording to " + name + ".\n");
		QuakeFileSystem.CreatePath(name);
		try {
			ServerInit.svs.demofile = new RandomAccessFile(name, "rw");
		}
		catch (Exception e) {
			Com.Printf("ERROR: couldn't open.\n");
        	Compatibility.printStackTrace(e);
			return;
		}

		// setup a buffer to catch all multicasts
		ServerInit.svs.demo_multicast = Buffer.wrap(ServerInit.svs.demo_multicast_buf).order(ByteOrder.LITTLE_ENDIAN);

		//
		// write a single giant fake message with all the startup info
		//
		Buffer buf = Buffer.allocate(32768).order(ByteOrder.LITTLE_ENDIAN);

		//
		// serverdata needs to go over for all types of servers
		// to make sure the protocol is right, and to set the gamedir
		//
		// send the serverdata
		Buffers.writeByte(buf, Constants.svc_serverdata);
		buf.putInt(Constants.PROTOCOL_VERSION);
		buf.putInt(ServerInit.svs.spawncount);
		// 2 means server demo
		Buffers.writeByte(buf, 2); // demos are always attract loops
		Buffers.WriteString(buf, ConsoleVariables.VariableString("gamedir"));
		buf.WriteShort(-1);
		// send full levelname
		Buffers.WriteString(buf, ServerInit.sv.configstrings[Constants.CS_NAME]);

		for (i = 0; i < Constants.MAX_CONFIGSTRINGS; i++)
			if (ServerInit.sv.configstrings[i].length() == 0) {
				Buffers.writeByte(buf, Constants.svc_configstring);
				buf.WriteShort(i);
				Buffers.WriteString(buf, ServerInit.sv.configstrings[i]);
			}

		// write it to the demo file
		Com.DPrintf("signon message length: " + buf.cursize + "\n");
		len = EndianHandler.swapInt(buf.cursize);
		//fwrite(len, 4, 1, svs.demofile);
		//fwrite(buf.data, buf.cursize, 1, svs.demofile);
		try {
			ServerInit.svs.demofile.writeInt(len);
			ServerInit.svs.demofile.write(buf.data, 0, buf.cursize);
		}
		catch (IOException e1) {
			// TODO: do quake2 error handling!
			Compatibility.printStackTrace(e1);
		}

		// the rest of the demo file will be individual frames
	}
	/*
	==============
	SV_ServerStop_f

	Ends server demo recording
	==============
	*/
	public static void SV_ServerStop_f() {
		if (ServerInit.svs.demofile == null) {
			Com.Printf("Not doing a serverrecord.\n");
			return;
		}
		try {
			ServerInit.svs.demofile.close();
		}
		catch (IOException e) {
			Compatibility.printStackTrace(e);
		}
		ServerInit.svs.demofile = null;
		Com.Printf("Recording completed.\n");
	}
	/*
	===============
	SV_KillServer_f

	Kick everyone off, possibly in preparation for a new game

	===============
	*/
	public static void SV_KillServer_f() {
		if (!ServerInit.svs.initialized)
			return;
		ServerMain.SV_Shutdown("Server was killed.\n", false);
		NET.Config(false); // close network sockets
	}
	/*
	===============
	SV_ServerCommand_f

	Let the game dll handle a command
	===============
	*/
	public static void SV_ServerCommand_f() {

		GameSVCmds.ServerCommand();
	}
	//===========================================================

	/*
	==================
	SV_InitOperatorCommands
	==================
	*/
	public static void SV_InitOperatorCommands() {
		Commands.addCommand("heartbeat", new ExecutableCommand() {
			public void execute() {
				SV_Heartbeat_f();
			}
		});
		Commands.addCommand("kick", new ExecutableCommand() {
			public void execute() {
				SV_Kick_f();
			}
		});
		Commands.addCommand("status", new ExecutableCommand() {
			public void execute() {
				SV_Status_f();
			}
		});
		Commands.addCommand("serverinfo", new ExecutableCommand() {
			public void execute() {
				SV_Serverinfo_f();
			}
		});
		Commands.addCommand("dumpuser", new ExecutableCommand() {
			public void execute() {
				SV_DumpUser_f();
			}
		});

		Commands.addCommand("map", new ExecutableCommand() {
			public void execute() {
				SV_Map_f();
			}
		});
		Commands.addCommand("demomap", new ExecutableCommand() {
			public void execute() {
				SV_DemoMap_f();
			}
		});
		Commands.addCommand("gamemap", new ExecutableCommand() {
			public void execute() {
				SV_GameMap_f();
			}
		});
		Commands.addCommand("setmaster", new ExecutableCommand() {
			public void execute() {
				SV_SetMaster_f();
			}
		});

		if (Globals.dedicated.value != 0)
			Commands.addCommand("say", new ExecutableCommand() {
			public void execute() {
				SV_ConSay_f();
			}
		});

		Commands.addCommand("serverrecord", new ExecutableCommand() {
			public void execute() {
				SV_ServerRecord_f();
			}
		});
		Commands.addCommand("serverstop", new ExecutableCommand() {
			public void execute() {
				SV_ServerStop_f();
			}
		});

		Commands.addCommand("save", new ExecutableCommand() {
			public void execute() {
				SV_Savegame_f();
			}
		});
		Commands.addCommand("load", new ExecutableCommand() {
			public void execute() {
				SV_Loadgame_f();
			}
		});

		Commands.addCommand("killserver", new ExecutableCommand() {
			public void execute() {
				SV_KillServer_f();
			}
		});

		Commands.addCommand("sv", new ExecutableCommand() {
			public void execute() {
				SV_ServerCommand_f();
			}
		});
	}
}
