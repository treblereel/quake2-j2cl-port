package com.googlecode.gwtquake.shared.game.bot;

import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.game.Entity;
import com.googlecode.gwtquake.shared.game.GameBase;
import com.googlecode.gwtquake.shared.util.Math3D;

public class BotNavigation {

    private static final int MAX_STACK_NODE = 1024;
    private static final int MAX_PATH_NODES = 256;
    private static final int STEPSIZE = 22;

    private static final PathNode[] stack1 = new PathNode[MAX_STACK_NODE];
    private static final PathNode[] stack2 = new PathNode[MAX_STACK_NODE];

    public static PathNode findClosestNode(Entity self) {
        if (PathNode.allNodes.isEmpty()) return null;

        PathNode best = null;
        PathNode iBest = null;
        float minDist = 1e32f;
        float minIDist = 1e32f;

        for (PathNode node : PathNode.allNodes) {
            float d = PathNode.dist(self.s.origin, node.origin);
            if (d < minIDist) {
                minIDist = d;
                iBest = node;
            }
            if (d > minDist) continue;
            if (isTouched(self, node.origin)) {
                best = node;
                break;
            }
            if (!PathNode.posReachable(self.s.origin, node.origin)) continue;
            best = node;
            minDist = d;
        }

        if (best == null) best = iBest;
        return best;
    }

    public static PathNode findClosestNodeAtPos(Entity self, float[] pos) {
        float[] saved = new float[3];
        Math3D.VectorCopy(self.s.origin, saved);
        Math3D.VectorCopy(pos, self.s.origin);
        PathNode node = findClosestNode(self);
        Math3D.VectorCopy(saved, self.s.origin);
        return node;
    }

    public static boolean findRoute(Entity self, float[] target, boolean important) {
        BotInfo bi = self.botInfo;
        bi.nextNode = null;

        bi.lastNode = findClosestNode(self);
        PathNode targetNode = findClosestNodeAtPos(self, target);
        bi.targetNode = targetNode;

        if (targetNode == null || bi.lastNode == null) return false;
        if (targetNode == bi.lastNode) return false;

        for (PathNode node : PathNode.allNodes) {
            node.routeDist = -1;
        }

        int skill = (self.botPers != null) ? self.botPers.skill : 5;
        int maxNodes = 20 + 3 * skill;
        if (important) maxNodes *= 2;
        if (maxNodes > (MAX_PATH_NODES - 2)) maxNodes = MAX_PATH_NODES - 2;

        bi.lastNode.routeDist = 0.001f;
        int stackNodes = 1;
        int newStackNodes = 1;
        stack1[0] = bi.lastNode;
        int curStack = 0;

        int nCount = 0;
        while (nCount < maxNodes) {
            stackNodes = newStackNodes;
            newStackNodes = 0;

            PathNode[] nodes;
            PathNode[] newNodes;
            if (curStack == 0) {
                nodes = stack1;
                newNodes = stack2;
                curStack = 1;
            } else {
                nodes = stack2;
                newNodes = stack1;
                curStack = 0;
            }

            float bestDist = 1e32f;
            for (int i = 0; i < stackNodes; i++) {
                PathNode node = nodes[i];
                float curDist = node.routeDist;
                if (bestDist > curDist) bestDist = curDist;

                for (int j = 0; j < PathNode.MAX_NODE_LINKS; j++) {
                    PathNode linkNode = node.linkTo[j];
                    if (linkNode == null) break;
                    float d = curDist + node.linkDist[j];

                    if (linkNode != targetNode && linkNode.routeDist < 0
                            && newStackNodes < MAX_STACK_NODE) {
                        newNodes[newStackNodes] = linkNode;
                        newStackNodes++;
                    }

                    if (linkNode.routeDist < 0 || linkNode.routeDist > d) {
                        linkNode.routeDist = d;
                    }
                }
            }

            if (newStackNodes == 0) break;
            if (targetNode.routeDist >= 0 && targetNode.routeDist < bestDist) break;

            nCount++;
        }

        if (targetNode.routeDist < 0) return false;

        bi.pathNodes = -1;
        nCount = 0;
        PathNode node = targetNode;
        while (nCount <= maxNodes) {
            float bestDist = 0;
            PathNode linkNode = null;
            for (int i = 0; i < PathNode.MAX_NODE_LINKS; i++) {
                PathNode nextLink = node.linkFrom[i];
                if (nextLink == null) break;
                float d = nextLink.routeDist;
                if (d >= 0 && (linkNode == null || bestDist > d)) {
                    linkNode = nextLink;
                    bestDist = d;
                }
            }

            bi.path[++bi.pathNodes] = node;
            if (linkNode == null || linkNode == bi.lastNode) break;
            node.routeDist = -2;
            node = linkNode;
            nCount++;
        }

        bi.nextNode = node;
        setMoveTarget(self, node.origin);
        return true;
    }

    public static void followPath(Entity self) {
        BotInfo bi = self.botInfo;
        if (bi.nextNode == null) {
            bi.pathNodes = -1;
            return;
        }

        if (isTouched(self, bi.moveTarget)) {
            bi.lastNode = bi.nextNode;
            bi.nextNode = getNextPathNode(self);
            if (bi.nextNode != null) {
                setMoveTarget(self, bi.nextNode.origin);
            } else {
                bi.pathNodes = -1;
            }
        }
    }

    public static boolean hasPath(Entity self) {
        return self.botInfo != null && self.botInfo.pathNodes >= 0 && self.botInfo.nextNode != null;
    }

    public static void clearPath(Entity self) {
        if (self.botInfo != null) {
            self.botInfo.pathNodes = -1;
            self.botInfo.nextNode = null;
            self.botInfo.targetNode = null;
        }
    }

    public static void handleStuck(Entity self) {
        BotInfo bi = self.botInfo;
        if (bi.lastNode != null && bi.nextNode != null) {
            PathNode.removeDirectRoute(bi.lastNode, bi.nextNode);
            if (bi.targetNode != null) {
                findRoute(self, bi.targetNode.origin, false);
            } else {
                clearPath(self);
            }
        }
    }

    public static void updateRoutes(Entity self) {
        if ((self.watertype & (Constants.CONTENTS_LAVA | Constants.CONTENTS_SLIME)) != 0) return;
        if (self.groundentity == null && self.waterlevel == 0) return;
        if (PathNode.allNodes.size() > PathNode.MAX_NODE_COUNT) return;

        BotInfo bi = self.botInfo;

        if (bi.lastNode != null) {
            if (isCloser(self.s.origin, bi.lastNode.origin, PathNode.NODE_MIN_DIST)
                    && PathNode.posReachable(self.s.origin, bi.lastNode.origin)) return;
        }
        if (bi.nextNode != null) {
            if (isCloser(self.s.origin, bi.nextNode.origin, PathNode.NODE_MIN_DIST)
                    && PathNode.posReachable(self.s.origin, bi.nextNode.origin)) return;
        }

        registerPosition(self, bi.bOnLadder ? PathNode.NF_LADDER : 0);
    }

    public static void registerPosition(Entity self, int flags) {
        float[] pos = self.s.origin;

        if (self.prevNode != null) {
            if (isCloser(pos, self.prevNode.origin, PathNode.NODE_MIN_DIST)
                    && PathNode.posReachable(pos, self.prevNode.origin)) return;
        }

        int n = 0;
        boolean tooClose = false;
        for (PathNode node : PathNode.allNodes) {
            float d = PathNode.dist(pos, node.origin);
            if (d < 50) { tooClose = true; self.prevNode = node; break; }
            if (d > PathNode.NODE_MAX_DIST * 0.7f) continue;
            if (!PathNode.posReachable(pos, node.origin)) continue;
            n++;
            if (d < PathNode.NODE_MIN_DIST && n >= 2) { tooClose = true; self.prevNode = node; break; }
        }

        if (!tooClose) {
            self.prevNode = PathNode.insertNode(pos, self.prevNode, flags);
        }
    }

    public static boolean isTouched(Entity self, float[] pos) {
        if (pos[0] < (self.s.origin[0] + self.mins[0])) return false;
        if (pos[0] > (self.s.origin[0] + self.maxs[0])) return false;
        if (pos[1] < (self.s.origin[1] + self.mins[1])) return false;
        if (pos[1] > (self.s.origin[1] + self.maxs[1])) return false;
        if (pos[2] < (self.s.origin[2] + self.mins[2])) return false;
        if (pos[2] > (self.s.origin[2] + self.maxs[2])) return false;
        return true;
    }

    public static void setMoveTarget(Entity self, float[] target) {
        Math3D.VectorCopy(target, self.botInfo.moveTarget);
        self.botInfo.moveTarget[2] += STEPSIZE;
    }

    static PathNode getNextPathNode(Entity self) {
        BotInfo bi = self.botInfo;
        bi.pathNodes--;
        if (bi.pathNodes < 0) {
            bi.pathNodes = -1;
            return null;
        }
        return bi.path[bi.pathNodes];
    }

    static boolean isCloser(float[] pos1, float[] pos2, float threshold) {
        return Math.abs(pos1[0] - pos2[0]) < threshold
                && Math.abs(pos1[1] - pos2[1]) < threshold
                && Math.abs(pos1[2] - pos2[2]) < threshold;
    }
}
