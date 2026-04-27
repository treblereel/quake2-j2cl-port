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

/**
 * Father of all GameObjects.
 */

package com.googlecode.gwtquake.shared.game;

import java.util.StringTokenizer;

import com.googlecode.gwtquake.shared.client.ClientMonsterMethods;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.CommandBuffer;
import com.googlecode.gwtquake.shared.common.ConsoleVariables;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.game.bot.BotMain;
import com.googlecode.gwtquake.shared.server.SV;
import com.googlecode.gwtquake.shared.server.ServerGame;
import com.googlecode.gwtquake.shared.server.ServerSend;
import com.googlecode.gwtquake.shared.server.World;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.Math3D;

public class GameBase {

    public final static float STOP_EPSILON = 0.1f;
    public static Plane dummyplane = new Plane();
    public static GameState game = new GameState();
    public static LevelLocals level = new LevelLocals();
    public static SpawnTemp st = new SpawnTemp();
    public static int sm_meat_index;
    public static int snd_fry;
    public static int meansOfDeath;
    public static int num_edicts;
    public static Entity g_edicts[] = new Entity[Constants.MAX_EDICTS];
    public static ConsoleVariable deathmatch = new ConsoleVariable();

    public static ConsoleVariable coop = new ConsoleVariable();

    public static ConsoleVariable dmflags = new ConsoleVariable();

    public static ConsoleVariable skill; // = new cvar_t();

    public static ConsoleVariable fraglimit = new ConsoleVariable();

    public static ConsoleVariable timelimit = new ConsoleVariable();

    public static ConsoleVariable password = new ConsoleVariable();

    public static ConsoleVariable spectator_password = new ConsoleVariable();

    public static ConsoleVariable needpass = new ConsoleVariable();

    public static ConsoleVariable maxclients = new ConsoleVariable();

    public static ConsoleVariable maxspectators = new ConsoleVariable();

    public static ConsoleVariable maxentities = new ConsoleVariable();

    public static ConsoleVariable g_select_empty = new ConsoleVariable();

    public static ConsoleVariable filterban = new ConsoleVariable();

    public static ConsoleVariable sv_maxvelocity = new ConsoleVariable();

    public static ConsoleVariable sv_gravity = new ConsoleVariable();

    public static ConsoleVariable sv_rollspeed = new ConsoleVariable();

    public static ConsoleVariable sv_rollangle = new ConsoleVariable();

    public static ConsoleVariable gun_x = new ConsoleVariable();

    public static ConsoleVariable gun_y = new ConsoleVariable();

    public static ConsoleVariable gun_z = new ConsoleVariable();

    public static ConsoleVariable run_pitch = new ConsoleVariable();

    public static ConsoleVariable run_roll = new ConsoleVariable();

    public static ConsoleVariable bob_up = new ConsoleVariable();

    public static ConsoleVariable bob_pitch = new ConsoleVariable();

    public static ConsoleVariable bob_roll = new ConsoleVariable();

    public static ConsoleVariable sv_cheats = new ConsoleVariable();

    public static ConsoleVariable flood_msgs = new ConsoleVariable();

    public static ConsoleVariable flood_persecond = new ConsoleVariable();

    public static ConsoleVariable flood_waitdelay = new ConsoleVariable();

    public static ConsoleVariable sv_maplist = new ConsoleVariable();
    /**
     * Searches all active entities for the next one that holds the matching
     * string at fieldofs (use the FOFS() macro) in the structure.
     *
     * Searches beginning at the edict after from, or the beginning if null null
     * will be returned if the end of the list is reached.
     */

    public static int MAXCHOICES = 8;
    public static float[] VEC_UP = {0, -1, 0};
    public static float[] MOVEDIR_UP = {0, 0, 1};
    public static float[] VEC_DOWN = {0, -2, 0};
    public static float[] MOVEDIR_DOWN = {0, 0, -1};
    public static Pushed pushed[] = new Pushed[Constants.MAX_EDICTS];
    public static int pushed_p;
    public static Entity obstacle;
    public static int c_yes, c_no;
    public static int STEPSIZE = 18;
    public static EntityFilter findByTarget = new EntityFilter() {
        public boolean matches(Entity e, String s) {
            if (e.targetname == null) {
                return false;
            }
            return e.targetname.equalsIgnoreCase(s);
        }
    };
    public static EntityFilter findByClass = new EntityFilter() {
        public boolean matches(Entity e, String s) {
            return e.classname.equalsIgnoreCase(s);
        }
    };
    public static PlayerMove.PointContentsAdapter pointcontents = new PlayerMove.PointContentsAdapter() {
        public int pointcontents(float[] o) {
            return 0;
        }
    };
    /**
     * G_TouchTriggers
     */

    static Entity touch[] = new Entity[Constants.MAX_EDICTS];

    static {
        for (int n = 0; n < Constants.MAX_EDICTS; n++) {
            g_edicts[n] = new Entity(n);
        }
    }

    static {
        for (int n = 0; n < Constants.MAX_EDICTS; n++) {
            pushed[n] = new Pushed();
        }
    }

    /**
     * Slide off of the impacting object returns the blocked flags (1 = floor, 2 =
     * step / wall).
     */
    public static int ClipVelocity(float[] in, float[] normal, float[] out,
                                   float overbounce) {
        float backoff;
        float change;
        int i, blocked;

        blocked = 0;
        if (normal[2] > 0) {
            blocked |= 1; // floor
        }
        if (normal[2] == 0.0f) {
            blocked |= 2; // step
        }

        backoff = Math3D.DotProduct(in, normal) * overbounce;

        for (i = 0; i < 3; i++) {
            change = normal[i] * backoff;
            out[i] = in[i] - change;
            if (out[i] > -STOP_EPSILON && out[i] < STOP_EPSILON) {
                out[i] = 0;
            }
        }

        return blocked;
    }

    // comfort version (rst)
    public static Entity G_FindEdict(EntityIterator from, EntityFilter eff,
                                     String s) {
        EntityIterator ei = G_Find(from, eff, s);
        if (ei == null) {
            return null;
        } else {
            return ei.o;
        }
    }

    /**
     * Searches all active entities for the next one that holds the matching
     * string at fieldofs (use the FOFS() macro) in the structure.
     *
     * Searches beginning at the edict after from, or the beginning if null null
     * will be returned if the end of the list is reached.
     *
     */

    public static EntityIterator G_Find(EntityIterator from, EntityFilter eff,
                                        String s) {

        if (from == null) {
            from = new EntityIterator(0);
        } else {
            from.i++;
        }

        for (; from.i < num_edicts; from.i++) {
            from.o = g_edicts[from.i];
            if (from.o.classname == null) {
                Com.Printf("edict with classname = null" + from.o.index);
            }

            if (!from.o.inuse) {
                continue;
            }

            if (eff.matches(from.o, s)) {
                return from;
            }
        }

        return null;
    }

    /**
     * Returns entities that have origins within a spherical area.
     */
    public static EntityIterator findradius(EntityIterator from, float[] org,
                                            float rad) {
        float[] eorg = {0, 0, 0};
        int j;

        if (from == null) {
            from = new EntityIterator(0);
        } else {
            from.i++;
        }

        for (; from.i < num_edicts; from.i++) {
            from.o = g_edicts[from.i];
            if (!from.o.inuse) {
                continue;
            }

            if (from.o.solid == Constants.SOLID_NOT) {
                continue;
            }

            for (j = 0; j < 3; j++) {
                eorg[j] = org[j]
                        - (from.o.s.origin[j] + (from.o.mins[j] + from.o.maxs[j]) * 0.5f);
            }

            if (Math3D.VectorLength(eorg) > rad) {
                continue;
            }
            return from;
        }

        return null;
    }

    public static Entity G_PickTarget(String targetname) {
        int num_choices = 0;
        Entity choice[] = new Entity[MAXCHOICES];

        if (targetname == null) {
            ServerGame.PF_dprintf("G_PickTarget called with null targetname\n");
            return null;
        }

        EntityIterator es = null;

        while ((es = G_Find(es, findByTarget, targetname)) != null) {
            choice[num_choices++] = es.o;
            if (num_choices == MAXCHOICES) {
                break;
            }
        }

        if (num_choices == 0) {
            ServerGame.PF_dprintf("G_PickTarget: target " + targetname + " not found\n");
            return null;
        }

        return choice[Lib.rand() % num_choices];
    }

    public static void G_SetMovedir(float[] angles, float[] movedir) {
        if (Math3D.VectorEquals(angles, VEC_UP)) {
            Math3D.VectorCopy(MOVEDIR_UP, movedir);
        } else if (Math3D.VectorEquals(angles, VEC_DOWN)) {
            Math3D.VectorCopy(MOVEDIR_DOWN, movedir);
        } else {
            Math3D.AngleVectors(angles, movedir, null, null);
        }

        Math3D.VectorClear(angles);
    }

    public static String G_CopyString(String in) {
        return new String(in);
    }

    public static void G_TouchTriggers(Entity ent) {
        int i, num;
        Entity hit;

        // dead things don't activate triggers!
        if ((ent.client != null || (ent.svflags & Constants.SVF_MONSTER) != 0)
                && (ent.health <= 0)) {
            return;
        }

        num = World.SV_AreaEdicts(ent.absmin, ent.absmax, touch, Constants.MAX_EDICTS, Constants.AREA_TRIGGERS);

        // be careful, it is possible to have an entity in this
        // list removed before we get to it (killtriggered)
        for (i = 0; i < num; i++) {
            hit = touch[i];

            if (!hit.inuse) {
                continue;
            }

            if (hit.touch == null) {
                continue;
            }

            hit.touch.touch(hit, ent, dummyplane, null);
        }
    }

    public static void ClearBounds(float[] mins, float[] maxs) {
        mins[0] = mins[1] = mins[2] = 99999;
        maxs[0] = maxs[1] = maxs[2] = -99999;
    }

    public static void AddPointToBounds(float[] v, float[] mins, float[] maxs) {
        int i;
        float val;

        for (i = 0; i < 3; i++) {
            val = v[i];
            if (val < mins[i]) {
                mins[i] = val;
            }
            if (val > maxs[i]) {
                maxs[i] = val;
            }
        }
    }

    public static void ShutdownGame() {
        ServerGame.PF_dprintf("==== ShutdownGame ====\n");
    }

    /**
     * G_RunFrame
     *
     * Advances the world by Defines.FRAMETIME (0.1) seconds.
     */
    public static void G_RunFrame() {
        int i;
        Entity ent;

        level.framenum++;
        level.time = level.framenum * Constants.FRAMETIME;

        // choose a client for monsters to target this frame
        GameAI.AI_SetSightClient();

        // exit intermissions

        if (level.exitintermission) {
            ExitLevel();
            return;
        }

        //
        // treat each object in turn
        // even the world gets a chance to think
        //

        for (i = 0; i < num_edicts; i++) {
            ent = g_edicts[i];
            if (!ent.inuse) {
                continue;
            }

            level.current_entity = ent;

            Math3D.VectorCopy(ent.s.origin, ent.s.old_origin);

            // if the ground entity moved, make sure we are still on it
            if ((ent.groundentity != null)
                    && (ent.groundentity.linkcount != ent.groundentity_linkcount)) {
                ent.groundentity = null;
                if (0 == (ent.flags & (Constants.FL_SWIM | Constants.FL_FLY))
                        && (ent.svflags & Constants.SVF_MONSTER) != 0) {
                    ClientMonsterMethods.M_CheckGround(ent);
                }
            }

            if (i > 0 && i <= maxclients.value) {
                // Bot entity - use standard entity processing with bot think callback
                if (ent.botInfo != null) {
                    Globals.game.RunEntity.accept(ent);
                    continue;
                }

                PlayerClient.ClientBeginServerFrame(ent);
                continue;
            }

            //G_RunEntity(ent);
            Globals.game.RunEntity.accept(ent);
        }

        // see if it is time to end a deathmatch
        CheckDMRules();

        // see if needpass needs updated
        CheckNeedPass();

        // build the playerstate_t structures for all players
        ClientEndServerFrames();
    }

    /**
     * Exits a level.
     */
    public static void ExitLevel() {
        int i;
        Entity ent;

        String command = "gamemap \"" + level.changemap + "\"\n";
        CommandBuffer.AddText(command);
        level.changemap = null;
        level.exitintermission = false;
        level.intermissiontime = 0;
        ClientEndServerFrames();

        // clear some things before going to next level
        for (i = 0; i < maxclients.value; i++) {
            ent = g_edicts[1 + i];
            if (!ent.inuse) {
                continue;
            }
            if (ent.health > ent.client.pers.max_health) {
                ent.health = ent.client.pers.max_health;
            }
        }
    }

    /**
     * G_RunEntity
     */
    public static void G_RunEntity(Entity ent) {

        if (ent.prethink != null) {
            ent.prethink.think(ent);
        }

        switch ((int) ent.movetype) {
            case Constants.MOVETYPE_PUSH:
            case Constants.MOVETYPE_STOP:
                SV.SV_Physics_Pusher(ent);
                break;
            case Constants.MOVETYPE_NONE:
                SV.SV_Physics_None(ent);
                break;
            case Constants.MOVETYPE_NOCLIP:
                SV.SV_Physics_Noclip(ent);
                break;
            case Constants.MOVETYPE_STEP:
                SV.SV_Physics_Step(ent);
                break;
            case Constants.MOVETYPE_TOSS:
            case Constants.MOVETYPE_BOUNCE:
            case Constants.MOVETYPE_FLY:
            case Constants.MOVETYPE_FLYMISSILE:
                SV.SV_Physics_Toss(ent);
                break;
            default:
                Com.Error(Constants.ERR_FATAL, "SV_Physics: bad movetype " + (int) ent.movetype);
        }
    }

    /**
     * CheckDMRules.
     */
    public static void CheckDMRules() {
        int i;
        GameClient cl;

        if (level.intermissiontime != 0) {
            return;
        }

        if (0 == deathmatch.value) {
            return;
        }

        if (timelimit.value != 0) {
            if (level.time >= timelimit.value * 60) {
                ServerSend.SV_BroadcastPrintf(Constants.PRINT_HIGH, "Timelimit hit.\n");
                EndDMLevel();
                return;
            }
        }

        if (fraglimit.value != 0) {
            for (i = 0; i < maxclients.value; i++) {
                cl = game.clients[i];
                if (!g_edicts[i + 1].inuse) {
                    continue;
                }

                if (cl.resp.score >= fraglimit.value) {
                    ServerSend.SV_BroadcastPrintf(Constants.PRINT_HIGH, "Fraglimit hit.\n");
                    EndDMLevel();
                    return;
                }
            }
        }
    }

    /**
     * CheckNeedPass.
     */
    public static void CheckNeedPass() {
        int need;

        // if password or spectator_password has changed, update needpass
        // as needed
        if (password.modified || spectator_password.modified) {
            password.modified = spectator_password.modified = false;

            need = 0;

            if ((password.string.length() > 0)
                    && 0 != Lib.Q_stricmp(password.string, "none")) {
                need |= 1;
            }
            if ((spectator_password.string.length() > 0)
                    && 0 != Lib.Q_stricmp(spectator_password.string, "none")) {
                need |= 2;
            }

            ConsoleVariables.Set("needpass", "" + need);
        }
    }

    /**
     * ClientEndServerFrames.
     */
    public static void ClientEndServerFrames() {
        int i;
        Entity ent;

        // calc the player views now that all pushing
        // and damage has been added
        for (i = 0; i < maxclients.value; i++) {
            ent = g_edicts[1 + i];
            if (!ent.inuse || null == ent.client) {
                continue;
            }
            PlayerView.ClientEndServerFrame(ent);
        }
    }

    /**
     * The timelimit or fraglimit has been exceeded.
     */
    public static void EndDMLevel() {
        Entity ent;
        //char * s, * t, * f;
        //static const char * seps = " ,\n\r";
        String s, t, f;
        String seps = " ,\n\r";

        // stay on same level flag
        if (((int) dmflags.value & Constants.DF_SAME_LEVEL) != 0) {
            PlayerHud.BeginIntermission(CreateTargetChangeLevel(level.mapname));
            return;
        }

        // see if it's in the map list
        if (sv_maplist.string.length() > 0) {
            s = sv_maplist.string;
            f = null;
            StringTokenizer tk = new StringTokenizer(s, seps);

            while (tk.hasMoreTokens()) {
                t = tk.nextToken();

                // store first map
                if (f == null) {
                    f = t;
                }

                if (t.equalsIgnoreCase(level.mapname)) {
                    // it's in the list, go to the next one
                    if (!tk.hasMoreTokens()) {
                        // end of list, go to first one
                        if (f == null) // there isn't a first one, same level
                        {
                            PlayerHud.BeginIntermission(CreateTargetChangeLevel(level.mapname));
                        } else {
                            PlayerHud.BeginIntermission(CreateTargetChangeLevel(f));
                        }
                    } else {
                        PlayerHud.BeginIntermission(CreateTargetChangeLevel(tk.nextToken()));
                    }
                    return;
                }
            }
        }

        //not in the map list
        if (level.nextmap.length() > 0) // go to a specific map
        {
            PlayerHud.BeginIntermission(CreateTargetChangeLevel(level.nextmap));
        } else { // search for a changelevel
            EntityIterator edit = null;
            edit = G_Find(edit, findByClass, "target_changelevel");
            if (edit == null) { // the map designer didn't include a
                // changelevel,
                // so create a fake ent that goes back to the same level
                PlayerHud.BeginIntermission(CreateTargetChangeLevel(level.mapname));
                return;
            }
            ent = edit.o;
            PlayerHud.BeginIntermission(ent);
        }
    }

    /**
     * Returns the created target changelevel.
     */
    public static Entity CreateTargetChangeLevel(String map) {
        Entity ent;

        ent = GameUtil.G_Spawn();
        ent.classname = "target_changelevel";
        level.nextmap = map;
        ent.map = level.nextmap;
        return ent;
    }
}
