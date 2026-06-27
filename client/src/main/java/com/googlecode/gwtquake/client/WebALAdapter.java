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
package com.googlecode.gwtquake.client;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

import com.googlecode.gwtquake.shared.sound.ALAdapter;
import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLAudioElement;
import elemental2.dom.HTMLSourceElement;
import jsinterop.base.Js;

/**
 * Absolutely minimal implementation of AL API using HTML5 audio.
 */
public class WebALAdapter extends ALAdapter {

    int nextBufferId = 1;
    int nextSourceId = 1;
    private float alListenerGain;
    private float alListenerX;
    private float alListenerY;
    private float alListenerZ;
    private int alDistanceModel;
    private Map<Integer, BufferData> bufferData
            = new HashMap<Integer, BufferData>();
    private Map<Integer, SourceData> sourceData
            = new HashMap<Integer, SourceData>();

    @Override
    public void alBufferData(int buffer, int format, ByteBuffer data, int freq) {
        // TODO(cromwellian)
    }

    @Override
    public void alBufferData(int buffer, int format, IntBuffer data, int freq) {
        // TODO(cromwellian)
    }

    @Override
    public void alBufferData(int buffer, int format, ShortBuffer data, int freq) {
        // TODO(cromwellian)
    }

    @Override
    public void alDeleteBuffers(IntBuffer buffers) {
        for (int bid : buffers.array()) {
            bufferData.remove(bid);
        }
    }

    @Override
    public void alDeleteSources(IntBuffer sources) {
        // TODO(cromwellian)
    }

    @Override
    public void alDisable(int capability) {
        // TODO(cromwellian)
    }

    @Override
    public void alDistanceModel(int value) {
        alDistanceModel = value;
    }

    @Override
    public void alDopplerFactor(float value) {
        // TODO(cromwellian)
    }

    @Override
    public void alDopplerVelocity(float value) {
        // TODO(cromwellian)
    }

    @Override
    public void alEnable(int capability) {
        // TODO(cromwellian)
    }

    @Override
    public void alGenBuffers(IntBuffer buffers) {
        int len = buffers.capacity();
        for (int i = 0; i < buffers.capacity(); i++) {
            buffers.put(nextBufferId++);
        }
    }

    @Override
    public void alGenSources(IntBuffer sources) {
        int len = sources.capacity();
        for (int i = 0; i < sources.capacity(); i++) {
            int sid = nextSourceId++;
            sources.put(sid);
            sourceData.put(sid, new SourceData());
        }
    }

    @Override
    public boolean alGetBoolean(int pname) {
        return false;  // TODO(cromwellian)
    }

    @Override
    public float alGetBufferf(int buffer, int pname) {
        return 0;  // TODO(cromwellian)
    }

    @Override
    public int alGetBufferi(int buffer, int pname) {
        return 0;  // TODO(cromwellian)
    }

    @Override
    public double alGetDouble(int pname) {
        return 0;  // TODO(cromwellian)
    }

    @Override
    public void alGetDouble(int pname, DoubleBuffer data) {
        // TODO(cromwellian)
    }

    @Override
    public int alGetEnumValue(String ename) {
        return 0;  // TODO(cromwellian)
    }

    @Override
    public int alGetError() {
        return AL_NO_ERROR;
    }

    @Override
    public float alGetFloat(int pname) {
        return 0;  // TODO(cromwellian)
    }

    @Override
    public void alGetFloat(int pname, FloatBuffer data) {
        // TODO(cromwellian)
    }

    @Override
    public int alGetInteger(int pname) {
        return 0;  // TODO(cromwellian)
    }

    @Override
    public void alGetInteger(int pname, IntBuffer data) {
        // TODO(cromwellian)
    }

    @Override
    public void alGetListener(int pname, FloatBuffer floatdata) {
        // TODO(cromwellian)
    }

    @Override
    public float alGetListenerf(int pname) {
        return 0;  // TODO(cromwellian)
    }

    @Override
    public int alGetListeneri(int pname) {
        return 0;  // TODO(cromwellian)
    }

    @Override
    public void alGetSource(int source, int pname, FloatBuffer floatdata) {
        // TODO(cromwellian)
    }

    @Override
    public float alGetSourcef(int source, int pname) {
        return 0;  // TODO(cromwellian)
    }

    @Override
    public int alGetSourcei(int source, int pname) {
        SourceData sd = sourceData.get(source);
        if (sd != null) {
            switch (pname) {
                case AL_SOURCE_STATE:
                    if (sd.started) {
                        if (sd.buffer != null) {
                            if (!sd.buffer.element.ended() && !sd.buffer.element.paused()) {
                                return AL_PLAYING;
                            }
                        }
                    }
            }
        } else {
            return 0;
        }
        return 0;
    }

    @Override
    public String alGetString(int i) {
        return null;  // TODO(cromwellian)
    }

    @Override
    public boolean alIsBuffer(int buffer) {
        return false;  // TODO(cromwellian)
    }

    @Override
    public boolean alIsEnabled(int capability) {
        return false;  // TODO(cromwellian)
    }

    @Override
    public boolean alIsExtensionPresent(String fname) {
        return false;  // TODO(cromwellian)
    }

    @Override
    public boolean alIsSource(int id) {
        return false;  // TODO(cromwellian)
    }

    @Override
    public void alListener(int pname, FloatBuffer value) {
        // TODO(cromwellian)
    }

    @Override
    public void alListener3f(int pname, float v1, float v2, float v3) {
        switch (pname) {
            case AL_POSITION:
                alListenerX = v1;
                alListenerY = v2;
                alListenerZ = v3;
                break;
            default:
        }
    }

    @Override
    public void alListenerf(int pname, float value) {
        switch (pname) {
            case AL_GAIN:
                alListenerGain = value;
                break;
            default:
        }
    }

    @Override
    public void alListeneri(int pname, int value) {
        // TODO(cromwellian)
    }

    @Override
    public void alSource(int source, int pname, FloatBuffer value) {
        SourceData sd = sourceData.get(source);
        if (sd != null) {
            switch (pname) {
                case AL_POSITION:
                    sd.sourceX = value.get(0);
                    sd.sourceY = value.get(1);
                    sd.sourceZ = value.get(2);
                    sd.updateState();
                    break;
            }
        }
    }

    @Override
    public void alSource3f(int source, int pname, float v1, float v2, float v3) {
        // TODO(cromwellian)
    }

    @Override
    public void alSourcef(int source, int pname, float value) {
        SourceData sd = sourceData.get(source);
        if (sd != null) {
            switch (pname) {
                case AL_GAIN:
                    sd.alGain = value;
                    break;
                case AL_PITCH:
                    sd.alPitch = value;
                    break;
                case AL_LOOPING:
                    sd.alLooping = value == AL_TRUE;
                    break;
                case AL_REFERENCE_DISTANCE:
                    sd.alRefDist = value;
                    break;
                case AL_MIN_GAIN:
                    sd.alMinGain = value;
                    break;
                case AL_MAX_GAIN:
                    sd.alMaxGain = value;
                    break;
                case AL_ROLLOFF_FACTOR:
                    sd.alRolloffFactor = value;
                    break;
            }
            sd.updateState();
        }
    }

    @Override
    public void alSourcei(int source, int pname, int value) {
        SourceData sd = sourceData.get(source);
        if (sd != null) {
            switch (pname) {

                case AL_LOOPING:
                    sd.alLooping = value == AL_TRUE;
                    break;
                case AL_BUFFER:
                    sd.buffer = bufferData.get(value);
                    break;
                case AL_SOURCE_ABSOLUTE:
                    sd.alSrcAbsolute = value == AL_TRUE;
                    break;
                case AL_SOURCE_RELATIVE:
                    sd.alSrcRelative = value == AL_TRUE;
                    break;
            }
        }
    }

    @Override
    public void alSourcePause(IntBuffer sources) {
        // TODO(cromwellian)
    }

    @Override
    public void alSourcePause(int source) {
        // TODO(cromwellian)
    }

    @Override
    public void alSourcePlay(IntBuffer sources) {
        for (int i = sources.position(); i < sources.limit(); i++) {
            alSourcePlay(sources.get(i));
        }
    }

    @Override
    public void alSourcePlay(int source) {
        SourceData sd = sourceData.get(source);
        if (sd != null) {
            sd.started = true;
            if (sd.buffer != null && sd.buffer.element != null) {
                sd.buffer.element = sd.buffer.element.playme();
            }
        }
    }

    @Override
    public void alSourceQueueBuffers(int source, IntBuffer buffers) {
        // TODO(cromwellian)
    }

    @Override
    public void alSourceRewind(IntBuffer sources) {
        // TODO(cromwellian)
    }

    @Override
    public void alSourceRewind(int source) {
        // TODO(cromwellian)
    }

    @Override
    public void alSourceStop(IntBuffer sources) {
        for (int i = sources.position(); i < sources.limit(); i++) {
            alSourceStop(sources.get(i));
        }
    }

    @Override
    public void alSourceStop(int source) {
        SourceData sd = sourceData.get(source);
        if (sd != null) {
            if (sd.buffer != null && sd.buffer.element != null) {
                sd.buffer.element.stop();
            }
        }
    }

    @Override
    public void alSourceUnqueueBuffers(int source, IntBuffer buffers) {
        // TODO(cromwellian)
    }

    @Override
    public void create(String deviceArguments, int contextFrequency,
                       int contextRefresh, boolean contextSynchronized) {
        // TODO(cromwellian)
    }

    @Override
    public void create(String deviceArguments, int contextFrequency,
                       int contextRefresh, boolean contextSynchronized, boolean openDevice) {
        // TODO(cromwellian)
    }

    @Override
    public void alBufferData(int buffer, String soundUrl) {
        bufferData.put(buffer, new BufferData(soundUrl));
    }

    static class HTML5AudioElement {

        private HTMLAudioElement audio;

        protected HTML5AudioElement(String name) {
            audio = (HTMLAudioElement)DomGlobal.document.createElement("audio");
            DomGlobal.document.body.appendChild(audio);
            //audio.src = name;
            audio.muted = true;
            audio.preload = "auto";

            HTMLSourceElement source = (HTMLSourceElement)DomGlobal.document.createElement("source");
            source.src = name;
            source.type="audio/mp3";
            audio.appendChild(source);

            //audio.setAttribute("preload", "auto");
        }

        public static HTML5AudioElement create(String name) {
            return new HTML5AudioElement(name);
        }

        public final HTML5AudioElement playme() {
            if (audio.currentTime > 0) {
                if (audio.seekable != null && audio.duration > 0) {
                    audio.currentTime = 0;
                    audio.play();
                    return this;
                } else {
                    HTML5AudioElement x = new HTML5AudioElement(audio.src);
                    x.playme();
                    return Js.uncheckedCast(x);
                }
            }
            audio.muted = false;
            audio.play();
            return this;
        }

        public final void stop() {
            audio.pause();
            audio.currentTime = 0;
        }

        public final void setVolume(float gain) {
            audio.volume = gain;
        }

        public final boolean ended() {
            return audio.ended;
        }

        public final boolean paused() {
            return audio.paused;
        }
    }

    static class BufferData {

        Buffer data;

        int freq;

        int format;

        String location;

        private HTML5AudioElement element;

        BufferData(String location) {
            this.location = location;
            element = HTML5AudioElement.create(location);
        }
    }

    class SourceData {

        public float alGain = 1.0f;

        public float alPitch = 1.0f;

        public boolean alLooping = false;

        public float alRefDist = 1.0f;

        public float alMinGain = 0.0f;

        public float alMaxGain = 1.0f;

        public BufferData buffer;

        public boolean alSrcRelative = false;

        public boolean alSrcAbsolute;

        public float alRolloffFactor = 1.0f;

        public boolean started = false;

        public float sourceX;

        public float sourceY;

        public float sourceZ;

        private float alMaxDist = Float.MAX_VALUE;

        public void updateState() {
            if (buffer != null && buffer.element != null) {
                if (alDistanceModel != ALAdapter.AL_INVERSE_DISTANCE_CLAMPED) {
                    buffer.element.setVolume(alListenerGain * alGain);
                } else {
                    float gain = alRefDist / (alRefDist + alRolloffFactor * (Math.min(Math.max(distance(), alRefDist), alMaxDist)
                            - alRefDist));
                    buffer.element.setVolume(alListenerGain * Math.max(alMinGain, Math.min(alGain * gain, alMaxGain)));
                }
            }
        }

        private float distance() {
            float dx = sourceX - alListenerX;
            float dy = sourceY - alListenerY;
            float dz = sourceZ - alListenerZ;
            return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }
}