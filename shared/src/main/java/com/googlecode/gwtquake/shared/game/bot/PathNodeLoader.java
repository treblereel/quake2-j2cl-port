package com.googlecode.gwtquake.shared.game.bot;

import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.QuakeFileSystem;
import com.googlecode.gwtquake.shared.game.GameBase;

import elemental2.core.ArrayBuffer;
import elemental2.core.DataView;
import elemental2.core.Uint8Array;
import elemental2.dom.Blob;
import elemental2.dom.BlobPropertyBag;
import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLAnchorElement;
import elemental2.dom.MouseEvent;
import elemental2.dom.URL;
import jsinterop.base.Js;

public class PathNodeLoader {

    private static final String FILE_SIGNATURE = "CRBOT.ROUTE.MAP.01.";
    private static final int SIGNATURE_SIZE = 20;
    private static final int RESERVED_HEADER_SIZE = 256;
    private static final int RESERVED_PER_NODE = 80;
    private static final int NODE_DATA_SIZE = 16; // 3 floats + 1 int
    private static final int TERMINATOR_RESERVED_SIZE = 80;

    public static void loadRoutes() {
        String mapName = GameBase.level.mapname;
        if (mapName == null || mapName.isEmpty()) {
            Com.Printf("No map loaded\n");
            return;
        }

        String path = "nodemaps/" + mapName + ".crn";
        byte[] data = QuakeFileSystem.LoadFile(path);
        if (data == null) {
            Com.Printf("No node map found: " + path + "\n");
            return;
        }

        int offset = 0;

        if (data.length < SIGNATURE_SIZE + RESERVED_HEADER_SIZE) {
            Com.Printf("Node map file too small: " + path + "\n");
            return;
        }

        String sig = new String(data, 0, FILE_SIGNATURE.length());
        if (!sig.equals(FILE_SIGNATURE)) {
            Com.Printf("Invalid node map signature: " + path + "\n");
            return;
        }
        offset = SIGNATURE_SIZE + RESERVED_HEADER_SIZE;

        int nodeCount = 0;

        while (offset + RESERVED_PER_NODE <= data.length) {
            int firstReserved = readLittleEndianInt(data, offset);
            if (firstReserved != 0) break;
            offset += RESERVED_PER_NODE;

            if (offset + 16 > data.length) break;

            float[] pos = new float[3];
            pos[0] = readLittleEndianFloat(data, offset);
            pos[1] = readLittleEndianFloat(data, offset + 4);
            pos[2] = readLittleEndianFloat(data, offset + 8);
            int flags = readLittleEndianInt(data, offset + 12);
            offset += 16;

            PathNode.insertNode(pos, null, flags);
            nodeCount++;
        }

        Com.Printf("Loaded " + nodeCount + " nodes from " + path + "\n");
    }

    public static void saveRoutes() {
        String mapName = GameBase.level.mapname;
        if (mapName == null || mapName.isEmpty()) {
            Com.Printf("No map loaded\n");
            return;
        }

        int savedCount = 0;
        for (PathNode node : PathNode.allNodes) {
            if (node.item == null) savedCount++;
        }

        if (savedCount == 0) {
            Com.Printf("No nodes to save\n");
            return;
        }

        int recordSize = RESERVED_PER_NODE + NODE_DATA_SIZE;
        int totalSize = SIGNATURE_SIZE + RESERVED_HEADER_SIZE
                + savedCount * recordSize
                + TERMINATOR_RESERVED_SIZE;

        ArrayBuffer buffer = new ArrayBuffer(totalSize);
        DataView view = new DataView(buffer);
        Uint8Array bytes = new Uint8Array(buffer);
        int offset = 0;

        for (int i = 0; i < FILE_SIGNATURE.length(); i++) {
            bytes.setAt(offset + i, (double) FILE_SIGNATURE.charAt(i));
        }
        bytes.setAt(offset + FILE_SIGNATURE.length(), 0d); // null terminator
        offset = SIGNATURE_SIZE;

        // reserved header (256 bytes of zeros — already zero-initialized)
        offset += RESERVED_HEADER_SIZE;

        for (PathNode node : PathNode.allNodes) {
            if (node.item != null) continue;

            // reserved per-node (80 bytes of zeros)
            offset += RESERVED_PER_NODE;

            view.setFloat32(offset, node.origin[0], true);
            view.setFloat32(offset + 4, node.origin[1], true);
            view.setFloat32(offset + 8, node.origin[2], true);
            view.setInt32(offset + 12, node.flags, true);
            offset += NODE_DATA_SIZE;
        }

        // terminator: reserved[0] = 1
        view.setInt32(offset, 1, true);

        BlobPropertyBag options = BlobPropertyBag.create();
        options.setType("application/octet-stream");
        Blob.ConstructorBlobPartsArrayUnionType part =
                Blob.ConstructorBlobPartsArrayUnionType.of(buffer);
        Blob blob = new Blob(
                new Blob.ConstructorBlobPartsArrayUnionType[]{part}, options);

        String url = URL.createObjectURL(blob);
        HTMLAnchorElement a = Js.uncheckedCast(DomGlobal.document.createElement("a"));
        a.href = url;
        a.download = mapName + ".crn";
        a.dispatchEvent(new MouseEvent("click"));
        URL.revokeObjectURL(url);

        Com.Printf("Saved " + savedCount + " nodes to " + mapName + ".crn\n");
    }

    public static void dumpNodes() {
        if (PathNode.allNodes.isEmpty()) {
            Com.Printf("No nodes to dump\n");
            return;
        }

        Com.Printf("=== Node Map Dump (" + PathNode.allNodes.size() + " nodes) ===\n");
        Com.Printf("Map: " + GameBase.level.mapname + "\n");

        int index = 0;
        for (PathNode node : PathNode.allNodes) {
            if (node.item != null) continue;
            Com.Printf("N " + index + " "
                    + node.origin[0] + " " + node.origin[1] + " " + node.origin[2]
                    + " " + node.flags + "\n");
            index++;
        }

        int linkCount = 0;
        for (PathNode node : PathNode.allNodes) {
            for (int j = 0; j < PathNode.MAX_NODE_LINKS; j++) {
                if (node.linkTo[j] != null) linkCount++;
            }
        }

        Com.Printf("=== Total: " + index + " saved nodes, "
                + linkCount + " links ===\n");
    }

    private static int readLittleEndianInt(byte[] data, int offset) {
        return (data[offset] & 0xff)
                | ((data[offset + 1] & 0xff) << 8)
                | ((data[offset + 2] & 0xff) << 16)
                | ((data[offset + 3] & 0xff) << 24);
    }

    private static float readLittleEndianFloat(byte[] data, int offset) {
        return Float.intBitsToFloat(readLittleEndianInt(data, offset));
    }
}
