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
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.Random;

import com.googlecode.gwtquake.shared.Game_Export_T;
import com.googlecode.gwtquake.shared.client.*;
import com.googlecode.gwtquake.shared.game.*;
import com.googlecode.gwtquake.shared.render.DummyRenderer;
import com.googlecode.gwtquake.shared.render.Model;

/**
 * Globals ist the collection of global variables and constants.
 * It is more elegant to use these vars by inheritance to separate
 * it with eclipse refactoring later.
 *
 * As consequence you dont have to touch that much code this time.
 */
public class Globals {

	/*
	 * global variables
	 */
	public static int curtime = 0;
	public static boolean cmd_wait;

	public static int alias_count;
	public static int c_traces;
	public static int c_brush_traces;
	public static int c_pointcontents;
	public static int server_state;

	public static ConsoleVariable cl_add_blend;
	public static ConsoleVariable cl_add_entities;
	public static ConsoleVariable cl_add_lights;
	public static ConsoleVariable cl_add_particles;
	public static ConsoleVariable cl_anglespeedkey;
	public static ConsoleVariable cl_autoskins;
	public static ConsoleVariable cl_footsteps;
	public static ConsoleVariable cl_forwardspeed;
	public static ConsoleVariable cl_gun;
	public static ConsoleVariable cl_maxfps;
	public static ConsoleVariable cl_noskins;
	public static ConsoleVariable cl_pitchspeed;
	public static ConsoleVariable cl_predict;
	public static ConsoleVariable cl_run;
	public static ConsoleVariable cl_sidespeed;
	public static ConsoleVariable cl_stereo;
	public static ConsoleVariable cl_stereo_separation;
	public static ConsoleVariable cl_timedemo = new ConsoleVariable();
	public static ConsoleVariable cl_timeout;
	public static ConsoleVariable cl_upspeed;
	public static ConsoleVariable cl_yawspeed;
	public static ConsoleVariable dedicated;
	public static ConsoleVariable developer;
	public static ConsoleVariable fixedtime;
	public static ConsoleVariable freelook;
	public static ConsoleVariable host_speeds;
	public static ConsoleVariable log_stats;
	public static ConsoleVariable logfile_active;
	public static ConsoleVariable lookspring;
	public static ConsoleVariable lookstrafe;
	public static ConsoleVariable nostdout;
	public static ConsoleVariable sensitivity;
	public static ConsoleVariable showtrace;
	public static ConsoleVariable timescale;
	public static ConsoleVariable in_mouse;
	public static ConsoleVariable in_joystick;
        public static ConsoleVariable autojoin = new ConsoleVariable();

	public static Buffer net_message = new Buffer();

	/*
	=============================================================================

							COMMAND BUFFER

	=============================================================================
	*/

    public static byte cmd_text_buf[] = new byte[8192];
    public static byte defer_text_buf[] = new byte[8192];
	public static Buffer cmd_text = Buffer.wrap(cmd_text_buf).order(ByteOrder.LITTLE_ENDIAN);


	public static CommandAlias cmd_alias;

	//=============================================================================

	public static byte[] net_message_buffer = new byte[Constants.MAX_MSGLEN];

	public static int time_before_game;
	public static int time_after_game;
	public static int time_before_ref;
	public static int time_after_ref;

	public static FileWriter log_stats_file = null;

	public static ConsoleVariable m_pitch;
	public static ConsoleVariable m_yaw;
	public static ConsoleVariable m_forward;
	public static ConsoleVariable m_side;

	public static ConsoleVariable cl_lightlevel;

	//
	//	   userinfo
	//
	public static ConsoleVariable info_password;
	public static ConsoleVariable info_spectator;
	public static ConsoleVariable name;
	public static ConsoleVariable skin;
	public static ConsoleVariable rate;
	public static ConsoleVariable fov;
	public static ConsoleVariable msg;
	public static ConsoleVariable hand;
	public static ConsoleVariable gender;
	public static ConsoleVariable gender_auto;

	public static ConsoleVariable cl_vwep;

	public static ClientStatic cls = new ClientStatic();
	public static ClientState cl = new ClientState();

	public static ClientEntity cl_entities[] = new ClientEntity[Constants.MAX_EDICTS];
	public static Game_Export_T game;

    static {
		for (int i = 0; i < cl_entities.length; i++) {
			cl_entities[i] = new ClientEntity();
		}
	}

	public static EntityState cl_parse_entities[] = new EntityState[Constants.MAX_PARSE_ENTITIES];

	static {
		for (int i = 0; i < cl_parse_entities.length; i++)
		{
			cl_parse_entities[i] = new EntityState(null);
		}
	}

	public static ConsoleVariable rcon_client_password;
	public static ConsoleVariable rcon_address;

	public static ConsoleVariable cl_shownet;
	public static ConsoleVariable cl_showmiss;
	public static ConsoleVariable cl_showclamp;

	public static ConsoleVariable cl_paused;

	// client/anorms.h
	public static final float bytedirs[][] = { /**
								*/
		{ -0.525731f, 0.000000f, 0.850651f }, {
			-0.442863f, 0.238856f, 0.864188f }, {
			-0.295242f, 0.000000f, 0.955423f }, {
			-0.309017f, 0.500000f, 0.809017f }, {
			-0.162460f, 0.262866f, 0.951056f }, {
			0.000000f, 0.000000f, 1.000000f }, {
			0.000000f, 0.850651f, 0.525731f }, {
			-0.147621f, 0.716567f, 0.681718f }, {
			0.147621f, 0.716567f, 0.681718f }, {
			0.000000f, 0.525731f, 0.850651f }, {
			0.309017f, 0.500000f, 0.809017f }, {
			0.525731f, 0.000000f, 0.850651f }, {
			0.295242f, 0.000000f, 0.955423f }, {
			0.442863f, 0.238856f, 0.864188f }, {
			0.162460f, 0.262866f, 0.951056f }, {
			-0.681718f, 0.147621f, 0.716567f }, {
			-0.809017f, 0.309017f, 0.500000f }, {
			-0.587785f, 0.425325f, 0.688191f }, {
			-0.850651f, 0.525731f, 0.000000f }, {
			-0.864188f, 0.442863f, 0.238856f }, {
			-0.716567f, 0.681718f, 0.147621f }, {
			-0.688191f, 0.587785f, 0.425325f }, {
			-0.500000f, 0.809017f, 0.309017f }, {
			-0.238856f, 0.864188f, 0.442863f }, {
			-0.425325f, 0.688191f, 0.587785f }, {
			-0.716567f, 0.681718f, -0.147621f }, {
			-0.500000f, 0.809017f, -0.309017f }, {
			-0.525731f, 0.850651f, 0.000000f }, {
			0.000000f, 0.850651f, -0.525731f }, {
			-0.238856f, 0.864188f, -0.442863f }, {
			0.000000f, 0.955423f, -0.295242f }, {
			-0.262866f, 0.951056f, -0.162460f }, {
			0.000000f, 1.000000f, 0.000000f }, {
			0.000000f, 0.955423f, 0.295242f }, {
			-0.262866f, 0.951056f, 0.162460f }, {
			0.238856f, 0.864188f, 0.442863f }, {
			0.262866f, 0.951056f, 0.162460f }, {
			0.500000f, 0.809017f, 0.309017f }, {
			0.238856f, 0.864188f, -0.442863f }, {
			0.262866f, 0.951056f, -0.162460f }, {
			0.500000f, 0.809017f, -0.309017f }, {
			0.850651f, 0.525731f, 0.000000f }, {
			0.716567f, 0.681718f, 0.147621f }, {
			0.716567f, 0.681718f, -0.147621f }, {
			0.525731f, 0.850651f, 0.000000f }, {
			0.425325f, 0.688191f, 0.587785f }, {
			0.864188f, 0.442863f, 0.238856f }, {
			0.688191f, 0.587785f, 0.425325f }, {
			0.809017f, 0.309017f, 0.500000f }, {
			0.681718f, 0.147621f, 0.716567f }, {
			0.587785f, 0.425325f, 0.688191f }, {
			0.955423f, 0.295242f, 0.000000f }, {
			1.000000f, 0.000000f, 0.000000f }, {
			0.951056f, 0.162460f, 0.262866f }, {
			0.850651f, -0.525731f, 0.000000f }, {
			0.955423f, -0.295242f, 0.000000f }, {
			0.864188f, -0.442863f, 0.238856f }, {
			0.951056f, -0.162460f, 0.262866f }, {
			0.809017f, -0.309017f, 0.500000f }, {
			0.681718f, -0.147621f, 0.716567f }, {
			0.850651f, 0.000000f, 0.525731f }, {
			0.864188f, 0.442863f, -0.238856f }, {
			0.809017f, 0.309017f, -0.500000f }, {
			0.951056f, 0.162460f, -0.262866f }, {
			0.525731f, 0.000000f, -0.850651f }, {
			0.681718f, 0.147621f, -0.716567f }, {
			0.681718f, -0.147621f, -0.716567f }, {
			0.850651f, 0.000000f, -0.525731f }, {
			0.809017f, -0.309017f, -0.500000f }, {
			0.864188f, -0.442863f, -0.238856f }, {
			0.951056f, -0.162460f, -0.262866f }, {
			0.147621f, 0.716567f, -0.681718f }, {
			0.309017f, 0.500000f, -0.809017f }, {
			0.425325f, 0.688191f, -0.587785f }, {
			0.442863f, 0.238856f, -0.864188f }, {
			0.587785f, 0.425325f, -0.688191f }, {
			0.688191f, 0.587785f, -0.425325f }, {
			-0.147621f, 0.716567f, -0.681718f }, {
			-0.309017f, 0.500000f, -0.809017f }, {
			0.000000f, 0.525731f, -0.850651f }, {
			-0.525731f, 0.000000f, -0.850651f }, {
			-0.442863f, 0.238856f, -0.864188f }, {
			-0.295242f, 0.000000f, -0.955423f }, {
			-0.162460f, 0.262866f, -0.951056f }, {
			0.000000f, 0.000000f, -1.000000f }, {
			0.295242f, 0.000000f, -0.955423f }, {
			0.162460f, 0.262866f, -0.951056f }, {
			-0.442863f, -0.238856f, -0.864188f }, {
			-0.309017f, -0.500000f, -0.809017f }, {
			-0.162460f, -0.262866f, -0.951056f }, {
			0.000000f, -0.850651f, -0.525731f }, {
			-0.147621f, -0.716567f, -0.681718f }, {
			0.147621f, -0.716567f, -0.681718f }, {
			0.000000f, -0.525731f, -0.850651f }, {
			0.309017f, -0.500000f, -0.809017f }, {
			0.442863f, -0.238856f, -0.864188f }, {
			0.162460f, -0.262866f, -0.951056f }, {
			0.238856f, -0.864188f, -0.442863f }, {
			0.500000f, -0.809017f, -0.309017f }, {
			0.425325f, -0.688191f, -0.587785f }, {
			0.716567f, -0.681718f, -0.147621f }, {
			0.688191f, -0.587785f, -0.425325f }, {
			0.587785f, -0.425325f, -0.688191f }, {
			0.000000f, -0.955423f, -0.295242f }, {
			0.000000f, -1.000000f, 0.000000f }, {
			0.262866f, -0.951056f, -0.162460f }, {
			0.000000f, -0.850651f, 0.525731f }, {
			0.000000f, -0.955423f, 0.295242f }, {
			0.238856f, -0.864188f, 0.442863f }, {
			0.262866f, -0.951056f, 0.162460f }, {
			0.500000f, -0.809017f, 0.309017f }, {
			0.716567f, -0.681718f, 0.147621f }, {
			0.525731f, -0.850651f, 0.000000f }, {
			-0.238856f, -0.864188f, -0.442863f }, {
			-0.500000f, -0.809017f, -0.309017f }, {
			-0.262866f, -0.951056f, -0.162460f }, {
			-0.850651f, -0.525731f, 0.000000f }, {
			-0.716567f, -0.681718f, -0.147621f }, {
			-0.716567f, -0.681718f, 0.147621f }, {
			-0.525731f, -0.850651f, 0.000000f }, {
			-0.500000f, -0.809017f, 0.309017f }, {
			-0.238856f, -0.864188f, 0.442863f }, {
			-0.262866f, -0.951056f, 0.162460f }, {
			-0.864188f, -0.442863f, 0.238856f }, {
			-0.809017f, -0.309017f, 0.500000f }, {
			-0.688191f, -0.587785f, 0.425325f }, {
			-0.681718f, -0.147621f, 0.716567f }, {
			-0.442863f, -0.238856f, 0.864188f }, {
			-0.587785f, -0.425325f, 0.688191f }, {
			-0.309017f, -0.500000f, 0.809017f }, {
			-0.147621f, -0.716567f, 0.681718f }, {
			-0.425325f, -0.688191f, 0.587785f }, {
			-0.162460f, -0.262866f, 0.951056f }, {
			0.442863f, -0.238856f, 0.864188f }, {
			0.162460f, -0.262866f, 0.951056f }, {
			0.309017f, -0.500000f, 0.809017f }, {
			0.147621f, -0.716567f, 0.681718f }, {
			0.000000f, -0.525731f, 0.850651f }, {
			0.425325f, -0.688191f, 0.587785f }, {
			0.587785f, -0.425325f, 0.688191f }, {
			0.688191f, -0.587785f, 0.425325f }, {
			-0.955423f, 0.295242f, 0.000000f }, {
			-0.951056f, 0.162460f, 0.262866f }, {
			-1.000000f, 0.000000f, 0.000000f }, {
			-0.850651f, 0.000000f, 0.525731f }, {
			-0.955423f, -0.295242f, 0.000000f }, {
			-0.951056f, -0.162460f, 0.262866f }, {
			-0.864188f, 0.442863f, -0.238856f }, {
			-0.951056f, 0.162460f, -0.262866f }, {
			-0.809017f, 0.309017f, -0.500000f }, {
			-0.864188f, -0.442863f, -0.238856f }, {
			-0.951056f, -0.162460f, -0.262866f }, {
			-0.809017f, -0.309017f, -0.500000f }, {
			-0.681718f, 0.147621f, -0.716567f }, {
			-0.681718f, -0.147621f, -0.716567f }, {
			-0.850651f, 0.000000f, -0.525731f }, {
			-0.688191f, 0.587785f, -0.425325f }, {
			-0.587785f, 0.425325f, -0.688191f }, {
			-0.425325f, 0.688191f, -0.587785f }, {
			-0.425325f, -0.688191f, -0.587785f }, {
			-0.587785f, -0.425325f, -0.688191f }, {
			-0.688191f, -0.587785f, -0.425325f }
	};

	public static boolean userinfo_modified = false;

	public static ConsoleVariable cvar_vars;
	public static final ConsoleData con = new ConsoleData();
	public static ConsoleVariable con_notifytime;
	public static Dimension viddef = new Dimension();
	// Renderer interface used by VID, SCR, ...
	public static Renderer re = new DummyRenderer();

	public static String[] keybindings = new String[256];
	public static boolean[] keydown = new boolean[256];
	public static boolean chat_team = false;
	public static String chat_buffer = "";
	public static byte[][] key_lines = new byte[32][];
	public static int key_linepos;
	static {
		for (int i = 0; i < key_lines.length; i++)
			key_lines[i] = new byte[Constants.MAXCMDLINE];
	};
	public static int edit_line;

	public static ConsoleVariable crosshair;
	public static RectangleList scr_vrect = new RectangleList();
	public static int sys_frame_time;
	public static int chat_bufferlen = 0;
	public static int gun_frame;
	public static Model gun_model;
	public static NetworkAddress net_from = new NetworkAddress();

	// logfile
	public static RandomAccessFile logfile = null;

	public static float vec3_origin[] = { 0.0f, 0.0f, 0.0f };

	public static ConsoleVariable m_filter;
	public static int vidref_val = Constants.VIDREF_GL;

	public static Random rnd = new Random();
}
