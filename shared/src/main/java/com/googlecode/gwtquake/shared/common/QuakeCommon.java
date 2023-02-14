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
package com.googlecode.gwtquake.shared.common;


import java.io.FileWriter;
import java.io.IOException;

import com.googlecode.gwtquake.shared.Game_Export_T;
import com.googlecode.gwtquake.shared.Game_Import_T;
import com.googlecode.gwtquake.shared.GetGameAPI;
import com.googlecode.gwtquake.shared.client.*;
import com.googlecode.gwtquake.shared.game.Commands;
import com.googlecode.gwtquake.shared.server.ServerMain;
import com.googlecode.gwtquake.shared.sys.*;
import com.googlecode.gwtquake.shared.util.Vargs;
import elemental2.dom.DomGlobal;

import static com.googlecode.gwtquake.shared.game.Commands.ClientCommand;
import static com.googlecode.gwtquake.shared.game.GameBase.*;
import static com.googlecode.gwtquake.shared.game.GameSave.*;
import static com.googlecode.gwtquake.shared.game.GameSpawn.SpawnEntities;
import static com.googlecode.gwtquake.shared.game.PlayerClient.*;
import static com.googlecode.gwtquake.shared.server.ServerInit.SV_InitGame;

/**
 * Qcommon contains some  basic routines for the game engine
 * namely initialization, shutdown and frame generation.
 */
public final class QuakeCommon {

	public static final String BUILDSTRING = "Java " + System.getProperty("java.version");;
	//public static final String CPUSTRING = System.getProperty("os.arch"); //TODO
	public static final String CPUSTRING = "x86";
	public static QuakeDialog q2Dialog;

	/**
	 * This function initializes the different subsystems of
	 * the game engine. The setjmp/longjmp mechanism of the original
	 * was replaced with exceptions.
	 * @param args the original unmodified command line arguments
	 */
	public static void Init(String[] args) {
		try {

			Globals.game = ((GetGameAPI) import_t -> {
				Game_Export_T result = new Game_Export_T();
				result.Init = () -> SV_InitGame();
				result.Shutdown = () -> ShutdownGame();
				result.SpawnEntities = (mapname, entities,
										spawnpoint) -> SpawnEntities(mapname, entities, spawnpoint);
				result.WriteGame = (filename, autosave) -> WriteGame(filename, autosave);
				result.ReadGame = (filename) -> ReadGame(filename);

				result.WriteLevel = (filename) -> WriteLevel(filename);
				result.ReadLevel = (filename) -> ReadLevel(filename);

				result.ClientConnect = (ent, userinfo) -> ClientConnect(ent, userinfo);
				result.ClientBegin = (ent) -> ClientBegin(ent);
				result.ClientUserinfoChanged = (ent, userinfo) -> ClientUserinfoChanged(ent, userinfo);
				result.ClientDisconnect = (ent) -> ClientDisconnect(ent);
				result.ClientCommand = (ent) -> ClientCommand(ent);
				result.ClientThink = (ent, ucmd) -> ClientThink(ent, ucmd);
				result.RunEntity = (ent) -> G_RunEntity(ent);
				result.RunFrame = () -> G_RunFrame();
				return result;
			}).GetGameAPI(new Game_Import_T());

			// prepare enough of the subsystems to handle
			// cvar and command buffer management
			Com.InitArgv(args);


			Commands.Init();
			ConsoleVariables.Init();

	  Key.Init();

      // we need to add the early commands twice, because
      // a basedir or cddir needs to be set before execing
      // config files, but we want other parms to override
      // the settings of the config files
      CommandBuffer.AddEarlyCommands(false);
      CommandBuffer.Execute();

			if (q2Dialog != null)
				q2Dialog.setStatus("initializing filesystem...");

      QuakeFileSystem.InitFilesystem();

			if (q2Dialog != null)
				q2Dialog.setStatus("loading config...");

      reconfigure(false);

			QuakeFileSystem.setCDDir(); // use cddir from config.cfg
			QuakeFileSystem.markBaseSearchPaths(); // mark the default search paths

			if (q2Dialog != null)
				q2Dialog.testQ2Data(); // test for valid baseq2

			reconfigure(true); // reload default.cfg and config.cfg

			//
			// init commands and vars
			//
			Commands.addCommand("error", Com.Error_f);

			Globals.host_speeds= ConsoleVariables.Get("host_speeds", "0", 0);
			Globals.log_stats= ConsoleVariables.Get("log_stats", "0", 0);
			Globals.developer= ConsoleVariables.Get("developer", "0", Constants.CVAR_ARCHIVE);
			Globals.timescale= ConsoleVariables.Get("timescale", "0", 0);
			Globals.fixedtime= ConsoleVariables.Get("fixedtime", "0", 0);
			Globals.logfile_active= ConsoleVariables.Get("logfile", "0", 0);
			Globals.showtrace= ConsoleVariables.Get("showtrace", "0", 0);
			Globals.dedicated= ConsoleVariables.Get("dedicated", "0", Constants.CVAR_NOSET);

			String s = Com.sprintf("%4.2f %s %s %s",
					new Vargs(4)
						.add(Constants.VERSION)
						.add(CPUSTRING)
						.add(Constants.__DATE__)
						.add(BUILDSTRING));

			ConsoleVariables.Get("version", s, Constants.CVAR_SERVERINFO | Constants.CVAR_NOSET);

			if (q2Dialog != null)
				q2Dialog.setStatus("initializing network subsystem...");

			NET.Init();	//ok
			NetworkChannel.Netchan_Init();	//ok

			if (q2Dialog != null)
				q2Dialog.setStatus("initializing server subsystem...");
			ServerMain.SV_Init();	//ok

			if (q2Dialog != null)
				q2Dialog.setStatus("initializing client subsystem...");

			Client.init();

			// add + commands from command line
			if (!CommandBuffer.AddLateCommands()) {
				// if the user didn't give any commands, run default action
                              if (Globals.autojoin.value == 1.0f) {
                                 Menu.JoinNetworkServerFunc(null);
                              }
			      else if (Globals.dedicated.value == 0)
			          CommandBuffer.AddText ("menu_main\n"); // ("d1\n");
			      else
			          CommandBuffer.AddText ("dedicated_start\n");

				CommandBuffer.Execute();
			} else {
				// the user asked for something explicit
				// so drop the loading plaque
				Screen.EndLoadingPlaque();
      }

			Com.Printf("====== Quake2 Initialized ======\n\n");

			// save config when configuration is completed
			Client.writeConfiguration();

			if (q2Dialog != null)
				q2Dialog.dispose();

		} catch (LongJmpException e) {
			Sys.Error("Error during initialization");
		}
	}

	/**
	 * Trigger generation of a frame for the given time. The setjmp/longjmp
	 * mechanism of the original was replaced with exceptions.
	 * @param msec the current game time
	 */
	public static void Frame(int msec) {
		try {

			if (Globals.log_stats.modified) {
				Globals.log_stats.modified= false;

				if (Globals.log_stats.value != 0.0f) {

					if (Globals.log_stats_file != null) {
						try {
							Globals.log_stats_file.close();
						} catch (IOException e) {
						}
						Globals.log_stats_file= null;
					}

					try {
						Globals.log_stats_file= new FileWriter("stats.log");
					} catch (IOException e) {
						Globals.log_stats_file= null;
					}
					if (Globals.log_stats_file != null) {
						try {
							Globals.log_stats_file.write("entities,dlights,parts,frame time\n");
						} catch (IOException e) {
						}
					}

				} else {

					if (Globals.log_stats_file != null) {
						try {
							Globals.log_stats_file.close();
						} catch (IOException e) {
						}
						Globals.log_stats_file= null;
					}
				}
			}

			if (Globals.fixedtime.value != 0.0f) {
				msec= (int) Globals.fixedtime.value;
			} else if (Globals.timescale.value != 0.0f) {
				msec *= Globals.timescale.value;
				if (msec < 1)
					msec= 1;
			}

			if (Globals.showtrace.value != 0.0f) {
				Com.Printf("%4i traces  %4i points\n",
					new Vargs(2).add(Globals.c_traces)
								.add(Globals.c_pointcontents));


				Globals.c_traces= 0;
				Globals.c_brush_traces= 0;
				Globals.c_pointcontents= 0;
			}

			CommandBuffer.Execute();

			int time_before= 0;
			int time_between= 0;
			int time_after= 0;

			if (Globals.host_speeds.value != 0.0f)
				time_before= Timer.Milliseconds();
			Com.debugContext = "SV:";
			ServerMain.SV_Frame(msec);

			if (Globals.host_speeds.value != 0.0f)
				time_between= Timer.Milliseconds();

			Com.debugContext = "CL:";
			Client.frame(msec);

			if (Globals.host_speeds.value != 0.0f) {
				time_after= Timer.Milliseconds();

				int all= time_after - time_before;
				int sv= time_between - time_before;
				int cl= time_after - time_between;
				int gm= Globals.time_after_game - Globals.time_before_game;
				int rf= Globals.time_after_ref - Globals.time_before_ref;
				sv -= gm;
				cl -= rf;

				Com.Printf("all:%3i sv:%3i gm:%3i cl:%3i rf:%3i\n",
					new Vargs(5).add(all).add(sv).add(gm).add(cl).add(rf));
			}

		} catch (LongJmpException e) {
			Com.DPrintf("lonjmp exception:" + e);
		}
	}

	static void reconfigure(boolean clear) {
		String dir = ConsoleVariables.Get("cddir", "", Constants.CVAR_ARCHIVE).string;

		CommandBuffer.AddText(DefaultCfg.DEFAULT_CFG);

		CommandBuffer.AddText("bind MWHEELUP weapnext\n");
		CommandBuffer.AddText("bind MWHEELDOWN weapprev\n");
		CommandBuffer.AddText("bind w +forward\n");
		CommandBuffer.AddText("bind s +back\n");
		CommandBuffer.AddText("bind a +moveleft\n");
		CommandBuffer.AddText("bind d +moveright\n");
		 CommandBuffer.AddText("set cddir \"war/baseq2\"\n");
		CommandBuffer.Execute();
		ConsoleVariables.Set("vid_fullscreen", "0");
		CommandBuffer.AddText("exec config.cfg\n");

	    CommandBuffer.AddEarlyCommands(clear);
	    CommandBuffer.Execute();
	    if (!("".equals(dir))) ConsoleVariables.Set("cddir", dir);
  }
}
