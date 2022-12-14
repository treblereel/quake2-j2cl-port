/*
Copyright (C) 2010 Copyright 2010 Google Inc.

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
package com.googlecode.gwtquake.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class WAVConverter extends Converter {


    public WAVConverter() {
        super("wav", "wav");
    }

    @Override
    public void convert(byte[] raw, File outFile, int[] size) throws IOException {
        String outPath = lowerFile(outFile);
        try {
            //write raw bytes to tmpfile ending in .wav
            File tmpFile = File.createTempFile("q2audio", ".wav");
            tmpFile.deleteOnExit();
            String tmpPath = lowerFile(tmpFile);
            Files.write(tmpFile.toPath(), raw);
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", tmpPath, "-filter_complex", "[0:a]asplit[a1][a2]", "-map", "[a1]", "-qscale:a", "2", outPath + ".mp3", "-map", "[a2]", "-qscale:a", "2", outPath + ".ogg");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            InputStream is = p.getInputStream();
            byte[] buffer = new byte[1024];
            int read = 0;
            while ((read = is.read(buffer)) != -1) System.out.println(new String(buffer, 0, read));
            is.close();
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String lowerFile(File outFile) throws IOException {
        String lowerFileName = outFile.getCanonicalFile().getName().toLowerCase();
        String pathName = outFile.getCanonicalFile().getParent();
        return pathName + File.separator + lowerFileName;
    }
}