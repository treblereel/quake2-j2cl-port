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
package com.googlecode.gwtquake.shared.game;


import java.io.File;
import java.util.*;
import java.util.function.Function;

import com.googlecode.gwtquake.shared.common.*;
import com.googlecode.gwtquake.shared.game.bot.BotCommands;
import com.googlecode.gwtquake.shared.game.bot.PathNodeLoader;
import com.googlecode.gwtquake.shared.game.monsters.MonsterPlayer;
import com.googlecode.gwtquake.shared.server.ServerGame;
import com.googlecode.gwtquake.shared.util.Lib;

/**
 * Cmd
 */
public final class Commands {

    public static Function<String, byte[]> serverFileLoader;

    static ExecutableCommand List_f = new ExecutableCommand() {
        public void execute() {
            CommandFunction cmd = Commands.cmd_functions;
            int i = 0;

            while (cmd != null) {
                Com.Printf(cmd.name + '\n');
                i++;
                cmd = cmd.next;
            }
            Com.Printf(i + " commands\n");
        }
    };

    static ExecutableCommand Exec_f = new ExecutableCommand() {
        public void execute() {
            if (Commands.Argc() != 2) {
                Com.Printf("exec <filename> : execute a script file\n");
                return;
            }

            byte[] f = null;

            if(serverFileLoader != null ) {
                f = serverFileLoader.apply(Commands.Argv(1));
            } else {
                f = QuakeFileSystem.LoadFile(Commands.Argv(1));
            }

            if (f == null) {
                Com.Printf("couldn't exec " + Commands.Argv(1) + "\n");
                return;
            }
            Com.Printf("execing " + Commands.Argv(1) + "\n");

            CommandBuffer.InsertText(Compatibility.newString(f));

            QuakeFileSystem.FreeFile(f);
        }
    };

    static ExecutableCommand Echo_f = new ExecutableCommand() {
        public void execute() {
            for (int i = 1; i < Commands.Argc(); i++) {
                Com.Printf(Commands.Argv(i) + " ");
            }
            Com.Printf("'\n");
        }
    };

    static ExecutableCommand Alias_f = new ExecutableCommand() {
        public void execute() {
            CommandAlias a = null;
            if (Commands.Argc() == 1) {
                Com.Printf("Current alias commands:\n");
                for (a = Globals.cmd_alias; a != null; a = a.next) {
                    Com.Printf(a.name + " : " + a.value);
                }
                return;
            }

            String s = Commands.Argv(1);
            if (s.length() > Constants.MAX_ALIAS_NAME) {
                Com.Printf("Alias name is too long\n");
                return;
            }

            // if the alias already exists, reuse it
            for (a = Globals.cmd_alias; a != null; a = a.next) {
                if (s.equalsIgnoreCase(a.name)) {
                    a.value = null;
                    break;
                }
            }

            if (a == null) {
                a = new CommandAlias();
                a.next = Globals.cmd_alias;
                Globals.cmd_alias = a;
            }
            a.name = s;

            // copy the rest of the command line
            String cmd = "";
            int c = Commands.Argc();
            for (int i = 2; i < c; i++) {
                cmd = cmd + Commands.Argv(i);
                if (i != (c - 1))
                    cmd = cmd + " ";
            }
            cmd = cmd + "\n";

            a.value = cmd;
        }
    };

    public static ExecutableCommand Wait_f = new ExecutableCommand() {
        public void execute() {
            Globals.cmd_wait = true;
        }
    };

    public static CommandFunction cmd_functions = null;

    public static int cmd_argc;

    public static String[] cmd_argv = new String[Constants.MAX_STRING_TOKENS];

    public static String cmd_args;

    public static final int ALIAS_LOOP_COUNT = 16;

    /**
     * Register our commands.
     */
    public static void Init() {

        Commands.addCommand("exec", Exec_f);
        Commands.addCommand("echo", Echo_f);
        Commands.addCommand("cmdlist", List_f);
        Commands.addCommand("alias", Alias_f);
        Commands.addCommand("wait", Wait_f);
    }

    private static char expanded[] = new char[Constants.MAX_STRING_CHARS];

    private static char temporary[] = new char[Constants.MAX_STRING_CHARS];

    public static Comparator PlayerSort = new Comparator() {
        public int compare(Object o1, Object o2) {
            int anum = ((Integer) o1).intValue();
            int bnum = ((Integer) o2).intValue();
    
            int anum1 = GameBase.game.clients[anum].ps.stats[Constants.STAT_FRAGS];
            int bnum1 = GameBase.game.clients[bnum].ps.stats[Constants.STAT_FRAGS];
    
            if (anum1 < bnum1)
                return -1;
            if (anum1 > bnum1)
                return 1;
            return 0;
        }
    };

    /** 
     * Cmd_MacroExpandString.
     */
    public static char[] MacroExpandString(char text[], int len) {
        int i, j, count;
        boolean inquote;

        char scan[];

        String token;
        inquote = false;

        scan = text;

        if (len >= Constants.MAX_STRING_CHARS) {
            Com.Printf("Line exceeded " + Constants.MAX_STRING_CHARS
                    + " chars, discarded.\n");
            return null;
        }

        count = 0;

        for (i = 0; i < len; i++) {
            if (scan[i] == '"')
                inquote = !inquote;

            if (inquote)
                continue; // don't expand inside quotes

            if (scan[i] != '$')
                continue;

            // scan out the complete macro, without $
            Com.ParseHelp ph = new Com.ParseHelp(text, i + 1);
            token = Com.Parse(ph);

            if (ph.data == null)
                continue;

            token = ConsoleVariables.VariableString(token);

            j = token.length();

            len += j;

            if (len >= Constants.MAX_STRING_CHARS) {
                Com.Printf("Expanded line exceeded " + Constants.MAX_STRING_CHARS
                        + " chars, discarded.\n");
                return null;
            }

            System.arraycopy(scan, 0, temporary, 0, i);
            System.arraycopy(token.toCharArray(), 0, temporary, i, token.length());
            System.arraycopy(ph.data, ph.index, temporary, i + j, len - ph.index - j);

            System.arraycopy(temporary, 0, expanded, 0, 0);
            scan = expanded;
            i--;
            if (++count == 100) {
                Com.Printf("Macro expansion loop, discarded.\n");
                return null;
            }
        }

        if (inquote) {
            Com.Printf("Line has unmatched quote, discarded.\n");
            return null;
        }

        return scan;
    }

    /**
     * Cmd_TokenizeString
     * 
     * Parses the given string into command line tokens. $Cvars will be expanded
     * unless they are in a quoted token.
     */
    public static void TokenizeString(char text[], boolean macroExpand) {
        String com_token;

        cmd_argc = 0;
        cmd_args = "";

        int len = Lib.strlen(text);

        // macro expand the text
        if (macroExpand)
            text = MacroExpandString(text, len);

        if (text == null)
            return;

        len = Lib.strlen(text);

        Com.ParseHelp ph = new Com.ParseHelp(text);

        while (true) {

            // skip whitespace up to a /n
            char c = ph.skipwhitestoeol();

            if (c == '\n') { // a newline seperates commands in the buffer
                c = ph.nextchar();
                break;
            }

            if (c == 0)
                return;

            // set cmd_args to everything after the first arg
            if (cmd_argc == 1) {
                cmd_args = new String(text, ph.index, len - ph.index);
                cmd_args.trim();
            }

            com_token = Com.Parse(ph);

            if (ph.data == null)
                return;

            if (cmd_argc < Constants.MAX_STRING_TOKENS) {
                cmd_argv[cmd_argc] = com_token;
                cmd_argc++;
            }
        }
    }

    public static void addCommand(String cmd_name, ExecutableCommand function) {
        CommandFunction cmd;
        //Com.DPrintf("Cmd_AddCommand: " + cmd_name + "\n");
        // fail if the command is a variable name
        if ((ConsoleVariables.VariableString(cmd_name)).length() > 0) {
            Com.Printf("Cmd_AddCommand: " + cmd_name
                    + " already defined as a var\n");
            return;
        }

        // fail if the command already exists
        for (cmd = cmd_functions; cmd != null; cmd = cmd.next) {
            if (cmd_name.equals(cmd.name)) {
                Com
                        .Printf("Cmd_AddCommand: " + cmd_name
                                + " already defined\n");
                return;
            }
        }

        cmd = new CommandFunction();
        cmd.name = cmd_name;

        cmd.function = function;
        cmd.next = cmd_functions;
        cmd_functions = cmd;
    }

    /**
     * Cmd_RemoveCommand 
     */
    public static void RemoveCommand(String cmd_name) {
        CommandFunction cmd, back = null;

        back = cmd = cmd_functions;

        while (true) {

            if (cmd == null) {
                Com.Printf("Cmd_RemoveCommand: " + cmd_name + " not added\n");
                return;
            }
            if (0 == Lib.strcmp(cmd_name, cmd.name)) {
                if (cmd == cmd_functions)
                    cmd_functions = cmd.next;
                else
                    back.next = cmd.next;
                return;
            }
            back = cmd;
            cmd = cmd.next;
        }
    }

    /** 
     * Cmd_Exists 
     */
    public static boolean Exists(String cmd_name) {
        CommandFunction cmd;

        for (cmd = cmd_functions; cmd != null; cmd = cmd.next) {
            if (cmd.name.equals(cmd_name))
                return true;
        }

        return false;
    }

    public static int Argc() {
        return cmd_argc;
    }

    public static String Argv(int i) {
        if (i < 0 || i >= cmd_argc)
            return "";
        return cmd_argv[i];
    }

    public static String Args() {
        return new String(cmd_args);
    }

    /**
     * Cmd_ExecuteString
     * 
     * A complete command line has been parsed, so try to execute it 
     * FIXME: lookupnoadd the token to speed search? 
     */
    public static void ExecuteString(String text) {

        CommandFunction cmd;
        CommandAlias a;

        TokenizeString(text.toCharArray(), true);

        // execute the command line
        if (Argc() == 0)
            return; // no tokens

        // check functions
        for (cmd = cmd_functions; cmd != null; cmd = cmd.next) {
            if (cmd_argv[0].equalsIgnoreCase(cmd.name)) {
                if (null == cmd.function) { // forward to server command
                    Commands.ExecuteString("cmd " + text);
                } else {
                    cmd.function.execute();
                }
                return;
            }
        }

        // check alias
        for (a = Globals.cmd_alias; a != null; a = a.next) {

            if (cmd_argv[0].equalsIgnoreCase(a.name)) {

                if (++Globals.alias_count == ALIAS_LOOP_COUNT) {
                    Com.Printf("ALIAS_LOOP_COUNT\n");
                    return;
                }
                CommandBuffer.InsertText(a.value);
                return;
            }
        }

        // check cvars
        if (ConsoleVariables.Command())
            return;

        // send it as a server command if we are connected
        Commands.ForwardToServer();
    }

    /**
     * Cmd_Give_f
     * 
     * Give items to a client.
     */
    public static void Give_f(Entity ent) {
        String name;
        GameItem it;
        int index;
        int i;
        boolean give_all;
        Entity it_ent;

        if (GameBase.deathmatch.value != 0 && GameBase.sv_cheats.value == 0) {
            ServerGame.PF_cprintfhigh(ent,
            	"You must run the server with '+set cheats 1' to enable this command.\n");
            return;
        }

        name = Commands.Args();

        if (0 == Lib.Q_stricmp(name, "all"))
            give_all = true;
        else
            give_all = false;

        if (give_all || 0 == Lib.Q_stricmp(Commands.Argv(1), "health")) {
            if (Commands.Argc() == 3)
                ent.health = Lib.atoi(Commands.Argv(2));
            else
                ent.health = ent.max_health;
            if (!give_all)
                return;
        }

        if (give_all || 0 == Lib.Q_stricmp(name, "weapons")) {
            for (i = 1; i < GameBase.game.num_items; i++) {
                it = GameItemList.itemlist[i];
                if (null == it.pickup)
                    continue;
                if (0 == (it.flags & Constants.IT_WEAPON))
                    continue;
                ent.client.pers.inventory[i] += 1;
            }
            if (!give_all)
                return;
        }

        if (give_all || 0 == Lib.Q_stricmp(name, "ammo")) {
            for (i = 1; i < GameBase.game.num_items; i++) {
                it = GameItemList.itemlist[i];
                if (null == it.pickup)
                    continue;
                if (0 == (it.flags & Constants.IT_AMMO))
                    continue;
                GameItems.Add_Ammo(ent, it, 1000);
            }
            if (!give_all)
                return;
        }

        if (give_all || Lib.Q_stricmp(name, "armor") == 0) {
            GameItemArmor info;

            it = GameItems.FindItem("Jacket Armor");
            ent.client.pers.inventory[GameItems.ITEM_INDEX(it)] = 0;

            it = GameItems.FindItem("Combat Armor");
            ent.client.pers.inventory[GameItems.ITEM_INDEX(it)] = 0;

            it = GameItems.FindItem("Body Armor");
            info = (GameItemArmor) it.info;
            ent.client.pers.inventory[GameItems.ITEM_INDEX(it)] = info.max_count;

            if (!give_all)
                return;
        }

        if (give_all || Lib.Q_stricmp(name, "Power Shield") == 0) {
            it = GameItems.FindItem("Power Shield");
            it_ent = GameUtil.G_Spawn();
            it_ent.classname = it.classname;
            GameItems.SpawnItem(it_ent, it);
            GameItems.Touch_Item(it_ent, ent, GameBase.dummyplane, null);
            if (it_ent.inuse)
                GameUtil.G_FreeEdict(it_ent);

            if (!give_all)
                return;
        }

        if (give_all) {
            for (i = 1; i < GameBase.game.num_items; i++) {
                it = GameItemList.itemlist[i];
                if (it.pickup != null)
                    continue;
                if ((it.flags & (Constants.IT_ARMOR | Constants.IT_WEAPON | Constants.IT_AMMO)) != 0)
                    continue;
                ent.client.pers.inventory[i] = 1;
            }
            return;
        }

        it = GameItems.FindItem(name);
        if (it == null) {
            name = Commands.Argv(1);
            it = GameItems.FindItem(name);
            if (it == null) {
                ServerGame.PF_cprintf(ent, Constants.PRINT_HIGH, "unknown item\n");
                return;
            }
        }

        if (it.pickup == null) {
            ServerGame.PF_cprintf(ent, Constants.PRINT_HIGH, "non-pickup item\n");
            return;
        }

        index = GameItems.ITEM_INDEX(it);

        if ((it.flags & Constants.IT_AMMO) != 0) {
            if (Commands.Argc() == 3)
                ent.client.pers.inventory[index] = Lib.atoi(Commands.Argv(2));
            else
                ent.client.pers.inventory[index] += it.quantity;
        } else {
            it_ent = GameUtil.G_Spawn();
            it_ent.classname = it.classname;
            GameItems.SpawnItem(it_ent, it);
            GameItems.Touch_Item(it_ent, ent, GameBase.dummyplane, null);
            if (it_ent.inuse)
                GameUtil.G_FreeEdict(it_ent);
        }
    }

    /** 
     * Cmd_God_f
     * 
     * Sets client to godmode
     * 
     * argv(0) god
     */
    public static void God_f(Entity ent) {
        String msg;

        if (GameBase.deathmatch.value != 0 && GameBase.sv_cheats.value == 0) {
            ServerGame.PF_cprintfhigh(ent,
            		"You must run the server with '+set cheats 1' to enable this command.\n");
            return;
        }

        ent.flags ^= Constants.FL_GODMODE;
        if (0 == (ent.flags & Constants.FL_GODMODE))
            msg = "godmode OFF\n";
        else
            msg = "godmode ON\n";

        ServerGame.PF_cprintf(ent, Constants.PRINT_HIGH, msg);
    }

    /** 
     * Cmd_Notarget_f
     * 
     * Sets client to notarget
     * 
     * argv(0) notarget.
     */
    public static void Notarget_f(Entity ent) {
        String msg;

        if (GameBase.deathmatch.value != 0 && GameBase.sv_cheats.value == 0) {
            ServerGame.PF_cprintfhigh(ent, 
            	"You must run the server with '+set cheats 1' to enable this command.\n");
            return;
        }

        ent.flags ^= Constants.FL_NOTARGET;
        if (0 == (ent.flags & Constants.FL_NOTARGET))
            msg = "notarget OFF\n";
        else
            msg = "notarget ON\n";

        ServerGame.PF_cprintfhigh(ent, msg);
    }

    /**
     * Cmd_Noclip_f
     * 
     * argv(0) noclip.
     */
    public static void Noclip_f(Entity ent) {
        String msg;

        if (GameBase.deathmatch.value != 0 && GameBase.sv_cheats.value == 0) {
            ServerGame.PF_cprintfhigh(ent, 
            	"You must run the server with '+set cheats 1' to enable this command.\n");
            return;
        }

        if (ent.movetype == Constants.MOVETYPE_NOCLIP) {
            ent.movetype = Constants.MOVETYPE_WALK;
            msg = "noclip OFF\n";
        } else {
            ent.movetype = Constants.MOVETYPE_NOCLIP;
            msg = "noclip ON\n";
        }

        ServerGame.PF_cprintfhigh(ent, msg);
    }

    /**
     * Cmd_Use_f
     * 
     * Use an inventory item.
     */
    public static void Use_f(Entity ent) {
        int index;
        GameItem it;
        String s;

        s = Commands.Args();

        it = GameItems.FindItem(s);
        Com.dprintln("using:" + s);
        if (it == null) {
            ServerGame.PF_cprintfhigh(ent, "unknown item: " + s + "\n");
            return;
        }
        if (it.use == null) {
            ServerGame.PF_cprintfhigh(ent, "Item is not usable.\n");
            return;
        }
        index = GameItems.ITEM_INDEX(it);
        if (0 == ent.client.pers.inventory[index]) {
            ServerGame.PF_cprintfhigh(ent, "Out of item: " + s + "\n");
            return;
        }

        it.use.use(ent, it);
    }

    /**
     * Cmd_Drop_f
     * 
     * Drop an inventory item.
     */
    public static void Drop_f(Entity ent) {
        int index;
        GameItem it;
        String s;

        s = Commands.Args();
        it = GameItems.FindItem(s);
        if (it == null) {
            ServerGame.PF_cprintfhigh(ent, "unknown item: " + s + "\n");
            return;
        }
        if (it.drop == null) {
            ServerGame.PF_cprintf(ent, Constants.PRINT_HIGH,
                    "Item is not dropable.\n");
            return;
        }
        index = GameItems.ITEM_INDEX(it);
        if (0 == ent.client.pers.inventory[index]) {
            ServerGame.PF_cprintfhigh(ent, "Out of item: " + s + "\n");
            return;
        }

        it.drop.drop(ent, it);
    }

    /**
     * Cmd_Inven_f.
     */
    public static void Inven_f(Entity ent) {
        int i;
        GameClient cl;

        cl = ent.client;

        cl.showscores = false;
        cl.showhelp = false;

        if (cl.showinventory) {
            cl.showinventory = false;
            return;
        }

        cl.showinventory = true;

        ServerGame.PF_WriteByte(Constants.svc_inventory);
        for (i = 0; i < Constants.MAX_ITEMS; i++) {
            ServerGame.PF_WriteShort(cl.pers.inventory[i]);
        }
        ServerGame.PF_Unicast(ent, true);
    }

    /**
     * Cmd_InvUse_f.
     */
    public static void InvUse_f(Entity ent) {
        GameItem it;

        Commands.ValidateSelectedItem(ent);

        if (ent.client.pers.selected_item == -1) {
            ServerGame.PF_cprintfhigh(ent, "No item to use.\n");
            return;
        }

        it = GameItemList.itemlist[ent.client.pers.selected_item];
        if (it.use == null) {
            ServerGame.PF_cprintfhigh(ent, "Item is not usable.\n");
            return;
        }
        it.use.use(ent, it);
    }

    /**
     * Cmd_WeapPrev_f.
     */
    public static void WeapPrev_f(Entity ent) {
        GameClient cl;
        int i, index;
        GameItem it;
        int selected_weapon;

        cl = ent.client;

        if (cl.pers.weapon == null)
            return;

        selected_weapon = GameItems.ITEM_INDEX(cl.pers.weapon);

        // scan for the next valid one
        for (i = 1; i <= Constants.MAX_ITEMS; i++) {
            index = (selected_weapon + i) % Constants.MAX_ITEMS;
            if (0 == cl.pers.inventory[index])
                continue;

            it = GameItemList.itemlist[index];
            if (it.use == null)
                continue;

            if (0 == (it.flags & Constants.IT_WEAPON))
                continue;
            it.use.use(ent, it);
            if (cl.pers.weapon == it)
                return; // successful
        }
    }

    /**
     * Cmd_WeapNext_f.
     */
    public static void WeapNext_f(Entity ent) {
        GameClient cl;
        int i, index;
        GameItem it;
        int selected_weapon;

        cl = ent.client;

        if (null == cl.pers.weapon)
            return;

        selected_weapon = GameItems.ITEM_INDEX(cl.pers.weapon);

        // scan for the next valid one
        for (i = 1; i <= Constants.MAX_ITEMS; i++) {
            index = (selected_weapon + Constants.MAX_ITEMS - i)
                    % Constants.MAX_ITEMS;
            //bugfix rst
            if (index == 0)
                index++;
            if (0 == cl.pers.inventory[index])
                continue;
            it = GameItemList.itemlist[index];
            if (null == it.use)
                continue;
            if (0 == (it.flags & Constants.IT_WEAPON))
                continue;
            it.use.use(ent, it);
            if (cl.pers.weapon == it)
                return; // successful
        }
    }

    /** 
     * Cmd_WeapLast_f.
     */
    public static void WeapLast_f(Entity ent) {
        GameClient cl;
        int index;
        GameItem it;

        cl = ent.client;

        if (null == cl.pers.weapon || null == cl.pers.lastweapon)
            return;

        index = GameItems.ITEM_INDEX(cl.pers.lastweapon);
        if (0 == cl.pers.inventory[index])
            return;
        it = GameItemList.itemlist[index];
        if (null == it.use)
            return;
        if (0 == (it.flags & Constants.IT_WEAPON))
            return;
        it.use.use(ent, it);
    }

    /**
     * Cmd_InvDrop_f 
     */
    public static void InvDrop_f(Entity ent) {
        GameItem it;

        Commands.ValidateSelectedItem(ent);

        if (ent.client.pers.selected_item == -1) {
            ServerGame.PF_cprintfhigh(ent, "No item to drop.\n");
            return;
        }

        it = GameItemList.itemlist[ent.client.pers.selected_item];
        if (it.drop == null) {
            ServerGame.PF_cprintfhigh(ent, "Item is not dropable.\n");
            return;
        }
        it.drop.drop(ent, it);
    }

    /** 
     * Cmd_Score_f
     * 
     * Display the scoreboard.
     * 
     */
    public static void Score_f(Entity ent) {
        ent.client.showinventory = false;
        ent.client.showhelp = false;

        if (0 == GameBase.deathmatch.value && 0 == GameBase.coop.value)
            return;

        if (ent.client.showscores) {
            ent.client.showscores = false;
            return;
        }

        ent.client.showscores = true;
        PlayerHud.DeathmatchScoreboard(ent);
    }

    /**
     * Cmd_Help_f
     * 
     * Display the current help message. 
     *
     */
    public static void Help_f(Entity ent) {
        // this is for backwards compatability
        if (GameBase.deathmatch.value != 0) {
            Score_f(ent);
            return;
        }

        ent.client.showinventory = false;
        ent.client.showscores = false;

        if (ent.client.showhelp
                && (ent.client.pers.game_helpchanged == GameBase.game.helpchanged)) {
            ent.client.showhelp = false;
            return;
        }

        ent.client.showhelp = true;
        ent.client.pers.helpchanged = 0;
        PlayerHud.HelpComputer(ent);
    }

    /**
     * Cmd_Kill_f
     */
    public static void Kill_f(Entity ent) {
        if ((GameBase.level.time - ent.client.respawn_time) < 5)
            return;
        ent.flags &= ~Constants.FL_GODMODE;
        ent.health = 0;
        GameBase.meansOfDeath = Constants.MOD_SUICIDE;
        PlayerClient.player_die.die(ent, ent, ent, 100000, Globals.vec3_origin);
    }

    /**
     * Cmd_PutAway_f
     */
    public static void PutAway_f(Entity ent) {
        ent.client.showscores = false;
        ent.client.showhelp = false;
        ent.client.showinventory = false;
    }

    /**
     * Cmd_Players_f
     */
    public static void Players_f(Entity ent) {
        int i;
        int count;
        String small;
        String large;

        Integer index[] = new Integer[256];

        count = 0;
        for (i = 0; i < GameBase.maxclients.value; i++) {
            if (GameBase.game.clients[i].pers.connected) {
                index[count] = new Integer(i);
                count++;
            }
        }

        // sort by frags
        Arrays.sort(index, 0, count - 1, Commands.PlayerSort);

        // print information
        large = "";

        for (i = 0; i < count; i++) {
            small = GameBase.game.clients[index[i].intValue()].ps.stats[Constants.STAT_FRAGS]
                    + " "
                    + GameBase.game.clients[index[i].intValue()].pers.netname
                    + "\n";

            if (small.length() + large.length() > 1024 - 100) {
                // can't print all of them in one packet
                large += "...\n";
                break;
            }
            large += small;
        }

        ServerGame.PF_cprintfhigh(ent, large + "\n" + count + " players\n");
    }

    /**
     * Cmd_Wave_f
     */
    public static void Wave_f(Entity ent) {
        int i;

        i = Lib.atoi(Commands.Argv(1));

        // can't wave when ducked
        if ((ent.client.ps.pmove.pm_flags & PlayerMove.PMF_DUCKED) != 0)
            return;

        if (ent.client.anim_priority > Constants.ANIM_WAVE)
            return;

        ent.client.anim_priority = Constants.ANIM_WAVE;

        switch (i) {
        case 0:
            ServerGame.PF_cprintfhigh(ent, "flipoff\n");
            ent.s.frame = MonsterPlayer.FRAME_flip01 - 1;
            ent.client.anim_end = MonsterPlayer.FRAME_flip12;
            break;
        case 1:
            ServerGame.PF_cprintfhigh(ent, "salute\n");
            ent.s.frame = MonsterPlayer.FRAME_salute01 - 1;
            ent.client.anim_end = MonsterPlayer.FRAME_salute11;
            break;
        case 2:
            ServerGame.PF_cprintfhigh(ent, "taunt\n");
            ent.s.frame = MonsterPlayer.FRAME_taunt01 - 1;
            ent.client.anim_end = MonsterPlayer.FRAME_taunt17;
            break;
        case 3:
            ServerGame.PF_cprintfhigh(ent, "wave\n");
            ent.s.frame = MonsterPlayer.FRAME_wave01 - 1;
            ent.client.anim_end = MonsterPlayer.FRAME_wave11;
            break;
        case 4:
        default:
            ServerGame.PF_cprintfhigh(ent, "point\n");
            ent.s.frame = MonsterPlayer.FRAME_point01 - 1;
            ent.client.anim_end = MonsterPlayer.FRAME_point12;
            break;
        }
    }

    /**
     * Command to print the players own position.
     */
    public static void ShowPosition_f(Entity ent) {
        ServerGame.PF_cprintfhigh(ent, "pos=" + Lib.vtofsbeaty(ent.s.origin) + "\n");
    }

    /**
     * Cmd_Say_f
     */
    public static void Say_f(Entity ent, boolean team, boolean arg0) {

        int i, j;
        Entity other;
        String text;
        GameClient cl;

        if (Commands.Argc() < 2 && !arg0)
            return;

        if (0 == ((int) (GameBase.dmflags.value) & (Constants.DF_MODELTEAMS | Constants.DF_SKINTEAMS)))
            team = false;

        if (team)
            text = "(" + ent.client.pers.netname + "): ";
        else
            text = "" + ent.client.pers.netname + ": ";

        if (arg0) {
            text += Commands.Argv(0);
            text += " ";
            text += Commands.Args();
        } else {
            if (Commands.Args().startsWith("\""))
                text += Commands.Args().substring(1, Commands.Args().length() - 1);
            else
                text += Commands.Args();
        }

        // don't let text be too long for malicious reasons
        if (text.length() > 150)
            //text[150] = 0;
            text = text.substring(0, 150);

        text += "\n";

        if (GameBase.flood_msgs.value != 0) {
            cl = ent.client;

            if (GameBase.level.time < cl.flood_locktill) {
                ServerGame.PF_cprintfhigh(ent, "You can't talk for "
                                        + (int) (cl.flood_locktill - GameBase.level.time)
                                        + " more seconds\n");
                return;
            }
            i = (int) (cl.flood_whenhead - GameBase.flood_msgs.value + 1);
            if (i < 0)
                i = (10) + i;
            if (cl.flood_when[i] != 0
                    && GameBase.level.time - cl.flood_when[i] < GameBase.flood_persecond.value) {
                cl.flood_locktill = GameBase.level.time + GameBase.flood_waitdelay.value;
                ServerGame.PF_cprintf(ent, Constants.PRINT_CHAT,
                        "Flood protection:  You can't talk for "
                                + (int) GameBase.flood_waitdelay.value
                                + " seconds.\n");
                return;
            }

            cl.flood_whenhead = (cl.flood_whenhead + 1) % 10;
            cl.flood_when[cl.flood_whenhead] = GameBase.level.time;
        }

        if (Globals.dedicated.value != 0)
            ServerGame.PF_cprintf(null, Constants.PRINT_CHAT, "" + text + "");

        for (j = 1; j <= GameBase.game.maxclients; j++) {
            other = GameBase.g_edicts[j];
            if (!other.inuse)
                continue;
            if (other.client == null)
                continue;
            if (team) {
                if (!GameUtil.OnSameTeam(ent, other))
                    continue;
            }
            ServerGame.PF_cprintf(other, Constants.PRINT_CHAT, "" + text + "");
        }

    }

    /**
     * Returns the playerlist. TODO: The list is badly formatted at the moment.
     */
    public static void PlayerList_f(Entity ent) {
        int i;
        String st;
        String text;
        Entity e2;

        // connect time, ping, score, name
        text = "";

        for (i = 0; i < GameBase.maxclients.value; i++) {
            e2 = GameBase.g_edicts[1 + i];
            if (!e2.inuse)
                continue;

            st = ""
                    + (GameBase.level.framenum - e2.client.resp.enterframe)
                    / 600
                    + ":"
                    + ((GameBase.level.framenum - e2.client.resp.enterframe) % 600)
                    / 10 + " " + e2.client.ping + " " + e2.client.resp.score
                    + " " + e2.client.pers.netname + " "
                    + (e2.client.resp.spectator ? " (spectator)" : "") + "\n";

            if (text.length() + st.length() > 1024 - 50) {
                text += "And more...\n";
                ServerGame.PF_cprintfhigh(ent, "" + text + "");
                return;
            }
            text += st;
        }
        ServerGame.PF_cprintfhigh(ent, text);
    }

    /**
     * Adds the current command line as a clc_stringcmd to the client message.
     * things like godmode, noclip, etc, are commands directed to the server, so
     * when they are typed in at the console, they will need to be forwarded.
     */
    public static void ForwardToServer() {
        String cmd;

        cmd = Commands.Argv(0);
        if (Globals.cls.state <= Constants.ca_connected || cmd.charAt(0) == '-'
                || cmd.charAt(0) == '+') {
            Com.Printf("Unknown command \"" + cmd + "\"\n");
            return;
        }

        Buffers.writeByte(Globals.cls.netchan.message, Constants.clc_stringcmd);
        Buffers.Print(Globals.cls.netchan.message, cmd);
        if (Commands.Argc() > 1) {
            Buffers.Print(Globals.cls.netchan.message, " ");
            Buffers.Print(Globals.cls.netchan.message, Commands.Args());
        }
    }

    /**
     * Cmd_CompleteCommand.
     */
    public static Vector CompleteCommand(String partial) {
        Vector cmds = new Vector();

        // check for match
        for (CommandFunction cmd = cmd_functions; cmd != null; cmd = cmd.next)
            if (cmd.name.startsWith(partial))
                cmds.add(cmd.name);
        for (CommandAlias a = Globals.cmd_alias; a != null; a = a.next)
            if (a.name.startsWith(partial))
                cmds.add(a.name);

        return cmds;
    }

    /**
     * Processes the commands the player enters in the quake console.
     */
    public static void ClientCommand(Entity ent) {
        String cmd;
    
        if (ent.client == null)
            return; // not fully in game yet
    
        cmd = Commands.Argv(0).toLowerCase();
    
        if (cmd.equals("players")) {
            Players_f(ent);
            return;
        }
        if (cmd.equals("say")) {
            Say_f(ent, false, false);
            return;
        }
        if (cmd.equals("say_team")) {
            Say_f(ent, true, false);
            return;
        }
        if (cmd.equals("score")) {
            Score_f(ent);
            return;
        }
        if (cmd.equals("help")) {
            Help_f(ent);
            return;
        }
    
        if (GameBase.level.intermissiontime != 0)
            return;
    
        if (cmd.equals("use"))
            Use_f(ent);
        else if (cmd.equals("drop"))
            Drop_f(ent);
        else if (cmd.equals("give"))
            Give_f(ent);
        else if (cmd.equals("god"))
            God_f(ent);
        else if (cmd.equals("notarget"))
            Notarget_f(ent);
        else if (cmd.equals("noclip"))
            Noclip_f(ent);
        else if (cmd.equals("inven"))
            Inven_f(ent);
        else if (cmd.equals("invnext"))
            GameItems.SelectNextItem(ent, -1);
        else if (cmd.equals("invprev"))
            GameItems.SelectPrevItem(ent, -1);
        else if (cmd.equals("invnextw"))
            GameItems.SelectNextItem(ent, Constants.IT_WEAPON);
        else if (cmd.equals("invprevw"))
            GameItems.SelectPrevItem(ent, Constants.IT_WEAPON);
        else if (cmd.equals("invnextp"))
            GameItems.SelectNextItem(ent, Constants.IT_POWERUP);
        else if (cmd.equals("invprevp"))
            GameItems.SelectPrevItem(ent, Constants.IT_POWERUP);
        else if (cmd.equals("invuse"))
            InvUse_f(ent);
        else if (cmd.equals("invdrop"))
            InvDrop_f(ent);
        else if (cmd.equals("weapprev"))
            WeapPrev_f(ent);
        else if (cmd.equals("weapnext"))
            WeapNext_f(ent);
        else if (cmd.equals("weaplast"))
            WeapLast_f(ent);
        else if (cmd.equals("kill"))
            Kill_f(ent);
        else if (cmd.equals("putaway"))
            PutAway_f(ent);
        else if (cmd.equals("wave"))
            Wave_f(ent);
        else if (cmd.equals("playerlist"))
            PlayerList_f(ent);
        else if (cmd.equals("showposition"))
            ShowPosition_f(ent);
        else if (cmd.equals("addbot"))
            BotCommands.addBot();
        else if (cmd.equals("addbots"))
            BotCommands.addBots();
        else if (cmd.equals("removebot"))
            BotCommands.removeBot();
        else if (cmd.equals("loadnodes"))
            PathNodeLoader.loadRoutes();
        else if (cmd.equals("savenodes"))
            PathNodeLoader.saveRoutes();
        else if (cmd.equals("dumpnodes"))
            PathNodeLoader.dumpNodes();
        else
            // anything that doesn't match a command will be a chat
            Say_f(ent, false, true);
    }

    public static void ValidateSelectedItem(Entity ent) {    	
        GameClient cl = ent.client;
    
        if (cl.pers.inventory[cl.pers.selected_item] != 0)
            return; // valid
    
        GameItems.SelectNextItem(ent, -1);
    }
}
