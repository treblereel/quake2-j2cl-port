package com.googlecode.gwtquake.shared.game.bot;

import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.game.Entity;
import com.googlecode.gwtquake.shared.game.GameBase;
import com.googlecode.gwtquake.shared.game.GameFunc;
import com.googlecode.gwtquake.shared.game.GameMisc;

public class BotNodeInit {

    private static final int STEPSIZE = 22;

    public static void initNodeNet() {
        PathNode.clearAll();

        int itemCount = 0;
        float[] pos = new float[3];

        for (int i = 1; i < GameBase.num_edicts; i++) {
            Entity hit = GameBase.g_edicts[i];
            if (!hit.inuse) continue;

            if (hit.touch == GameMisc.teleporter_touch) {
                if (hit.target == null) continue;
                Entity other = GameBase.G_FindEdict(null, GameBase.findByTarget, hit.target);
                if (other == null) continue;
                PathNode node = PathNode.insertNode(hit.s.origin, null, 0);
                PathNode node2 = PathNode.insertNode(other.s.origin, null, PathNode.NF_TELEPORT);
                PathNode.addDirectRoute(node, node2, false);
                continue;
            }

            if (hit.touch == GameFunc.Touch_Plat_Center) {
                pos[0] = (hit.absmin[0] + hit.absmax[0]) / 2;
                pos[1] = (hit.absmin[1] + hit.absmax[1]) / 2;
                pos[2] = hit.absmin[2] + STEPSIZE;
                PathNode node = PathNode.insertNode(pos, null, PathNode.NF_ELEVATOR);
                pos[2] = hit.absmax[2] + STEPSIZE;
                PathNode node2 = PathNode.insertNode(pos, null, PathNode.NF_ELEVATOR);
                PathNode.addDirectRouteBidi(node, node2);
                continue;
            }

            if (hit.use == GameFunc.door_use) {
                pos[0] = (hit.absmax[0] + hit.absmin[0]) / 2;
                pos[1] = (hit.absmax[1] + hit.absmin[1]) / 2;
                pos[2] = (hit.absmax[2] + hit.absmin[2] * 3) / 4;
                PathNode.insertNode(pos, null, PathNode.NF_DOOR);
                continue;
            }

            if (hit.item == null || hit.item.pickup == null) continue;

            PathNode node = PathNode.insertNode(hit.s.origin, null, 0);
            node.item = hit;
            itemCount++;
        }

        Com.Printf("Bot navigation: " + PathNode.allNodes.size()
                + " nodes (" + itemCount + " items)\n");
    }
}
