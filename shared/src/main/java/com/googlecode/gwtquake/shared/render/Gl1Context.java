/*
Copyright (C) 2011 Copyright 2010 Google Inc.

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
package com.googlecode.gwtquake.shared.render;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import com.googlecode.gwtquake.shared.util.Lib;

/**
 * Abstract GL1 context that implements the stuff that quake2 needs from GL1.x that is not present
 * in GL 2 ES.
 */
public abstract class Gl1Context {
  public static final int GL_ADD = 0x0104;
  public static final int GL_ALIASED_LINE_WIDTH_RANGE = 0x846E;
  public static final int GL_ALIASED_POINT_SIZE_RANGE = 0x846D;
  public static final int GL_ALPHA = 0x1906;
  public static final int GL_ALPHA_BITS = 0x0D55;
  public static final int GL_ALWAYS = 0x0207;
  public static final int GL_AMBIENT = 0x1200;
  public static final int GL_AMBIENT_AND_DIFFUSE = 0x1602;
  public static final int GL_AND = 0x1501;
  public static final int GL_AND_INVERTED = 0x1504;
  public static final int GL_AND_REVERSE = 0x1502;
  public static final int GL_BACK = 0x0405;
  public static final int GL_BLEND = 0x0BE2;
  public static final int GL_BLUE_BITS = 0x0D54;
  public static final int GL_BYTE = 0x1400;
  public static final int GL_CCW = 0x0901;
  public static final int GL_CLAMP_TO_EDGE = 0x812F;
  public static final int GL_CLEAR = 0x1500;
  public static final int GL_COLOR_ARRAY = 0x8076;
  public static final int GL_COLOR_BUFFER_BIT = 0x4000;
  public static final int GL_COLOR_LOGIC_OP = 0x0BF2;
  public static final int GL_COLOR_MATERIAL = 0x0B57;
  public static final int GL_COMPRESSED_TEXTURE_FORMATS = 0x86A3;
  public static final int GL_CONSTANT_ATTENUATION = 0x1207;
  public static final int GL_COPY = 0x1503;
  public static final int GL_COPY_INVERTED = 0x150C;
  public static final int GL_CULL_FACE = 0x0B44;
  public static final int GL_CW = 0x0900;
  public static final int GL_DECAL = 0x2101;
  public static final int GL_DECR = 0x1E03;
  public static final int GL_DEPTH_BITS = 0x0D56;
  public static final int GL_DEPTH_BUFFER_BIT = 0x0100;
  public static final int GL_DEPTH_TEST = 0x0B71;
  public static final int GL_DIFFUSE = 0x1201;
  public static final int GL_DITHER = 0x0BD0;
  public static final int GL_DONT_CARE = 0x1100;
  public static final int GL_DST_ALPHA = 0x0304;
  public static final int GL_DST_COLOR = 0x0306;
  public static final int GL_EMISSION = 0x1600;
  public static final int GL_EQUAL = 0x0202;
  public static final int GL_EQUIV = 0x1509;
  public static final int GL_EXP = 0x0800;
  public static final int GL_EXP2 = 0x0801;
  public static final int GL_EXTENSIONS = 0x1F03;
  public static final int GL_FALSE = 0;
  public static final int GL_FASTEST = 0x1101;
  public static final int GL_FIXED = 0x140C;
  public static final int GL_FLAT = 0x1D00;
  public static final int GL_FLOAT = 0x1406;
  public static final int GL_FOG = 0x0B60;
  public static final int GL_FOG_COLOR = 0x0B66;
  public static final int GL_FOG_DENSITY = 0x0B62;
  public static final int GL_FOG_END = 0x0B64;
  public static final int GL_FOG_HINT = 0x0C54;
  public static final int GL_FOG_MODE = 0x0B65;
  public static final int GL_FOG_START = 0x0B63;
  public static final int GL_FRAMEBUFFER = 0x8D40;
  public static final int GL_FRONT = 0x0404;
  public static final int GL_FRONT_AND_BACK = 0x0408;
  public static final int GL_GEQUAL = 0x0206;
  public static final int GL_GREATER = 0x0204;
  public static final int GL_GREEN_BITS = 0x0D53;
  public static final int GL_IMPLEMENTATION_COLOR_READ_FORMAT_OES = 0x8B9B;
  public static final int GL_IMPLEMENTATION_COLOR_READ_TYPE_OES = 0x8B9A;
  public static final int GL_INCR = 0x1E02;
  public static final int GL_INVALID_ENUM = 0x0500;
  public static final int GL_INVALID_OPERATION = 0x0502;
  public static final int GL_INVALID_VALUE = 0x0501;
  public static final int GL_INVERT = 0x150A;
  public static final int GL_KEEP = 0x1E00;
  public static final int GL_LEQUAL = 0x0203;
  public static final int GL_LESS = 0x0201;
  public static final int GL_LIGHT_MODEL_AMBIENT = 0x0B53;
  public static final int GL_LIGHT_MODEL_TWO_SIDE = 0x0B52;
  public static final int GL_LIGHT0 = 0x4000;
  public static final int GL_LIGHT1 = 0x4001;
  public static final int GL_LIGHT2 = 0x4002;
  public static final int GL_LIGHT3 = 0x4003;
  public static final int GL_LIGHT4 = 0x4004;
  public static final int GL_LIGHT5 = 0x4005;
  public static final int GL_LIGHT6 = 0x4006;
  public static final int GL_LIGHT7 = 0x4007;
  public static final int GL_LIGHTING = 0x0B50;
  public static final int GL_LINE_LOOP = 0x0002;
  public static final int GL_LINE_SMOOTH = 0x0B20;
  public static final int GL_LINE_SMOOTH_HINT = 0x0C52;
  public static final int GL_LINE_STRIP = 0x0003;
  public static final int GL_LINEAR = 0x2601;
  public static final int GL_LINEAR_ATTENUATION = 0x1208;
  public static final int GL_LINEAR_MIPMAP_LINEAR = 0x2703;
  public static final int GL_LINEAR_MIPMAP_NEAREST = 0x2701;
  public static final int GL_LINES = 0x0001;
  public static final int GL_LUMINANCE = 0x1909;
  public static final int GL_LUMINANCE_ALPHA = 0x190A;
  public static final int GL_MATRIX_MODE = 0x0ba0;
  public static final int GL_MAX_ELEMENTS_INDICES = 0x80E9;
  public static final int GL_MAX_ELEMENTS_VERTICES = 0x80E8;
  public static final int GL_MAX_LIGHTS = 0x0D31;
  public static final int GL_MAX_MODELVIEW_STACK_DEPTH = 0x0D36;
  public static final int GL_MAX_PROJECTION_STACK_DEPTH = 0x0D38;
  public static final int GL_MAX_TEXTURE_SIZE = 0x0D33;
  public static final int GL_MAX_TEXTURE_STACK_DEPTH = 0x0D39;
  public static final int GL_MAX_TEXTURE_UNITS = 0x84E2;
  public static final int GL_MAX_VIEWPORT_DIMS = 0x0D3A;
  public static final int GL_MODELVIEW = 0x1700;
  public static final int GL_MODULATE = 0x2100;
  public static final int GL_MULTISAMPLE = 0x809D;
  public static final int GL_NAND = 0x150E;
  public static final int GL_NEAREST = 0x2600;
  public static final int GL_NEAREST_MIPMAP_LINEAR = 0x2702;
  public static final int GL_NEAREST_MIPMAP_NEAREST = 0x2700;
  public static final int GL_NEVER = 0x0200;
  public static final int GL_NICEST = 0x1102;
  public static final int GL_NO_ERROR = 0;
  public static final int GL_NOOP = 0x1505;
  public static final int GL_NOR = 0x1508;
  public static final int GL_NORMAL_ARRAY = 0x8075;
  public static final int GL_NORMALIZE = 0x0BA1;
  public static final int GL_NOTEQUAL = 0x0205;
  public static final int GL_NUM_COMPRESSED_TEXTURE_FORMATS = 0x86A2;
  public static final int GL_ONE = 1;
  public static final int GL_ONE_MINUS_DST_ALPHA = 0x0305;
  public static final int GL_ONE_MINUS_DST_COLOR = 0x0307;
  public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
  public static final int GL_ONE_MINUS_SRC_COLOR = 0x0301;
  public static final int GL_OR = 0x1507;
  public static final int GL_OR_INVERTED = 0x150D;
  public static final int GL_OR_REVERSE = 0x150B;
  public static final int GL_OUT_OF_MEMORY = 0x0505;
  public static final int GL_PACK_ALIGNMENT = 0x0D05;
  public static final int GL_PALETTE4_R5_G6_B5_OES = 0x8B92;
  public static final int GL_PALETTE4_RGB5_A1_OES = 0x8B94;
  public static final int GL_PALETTE4_RGB8_OES = 0x8B90;
  public static final int GL_PALETTE4_RGBA4_OES = 0x8B93;
  public static final int GL_PALETTE4_RGBA8_OES = 0x8B91;
  public static final int GL_PALETTE8_R5_G6_B5_OES = 0x8B97;
  public static final int GL_PALETTE8_RGB5_A1_OES = 0x8B99;
  public static final int GL_PALETTE8_RGB8_OES = 0x8B95;
  public static final int GL_PALETTE8_RGBA4_OES = 0x8B98;
  public static final int GL_PALETTE8_RGBA8_OES = 0x8B96;
  public static final int GL_PERSPECTIVE_CORRECTION_HINT = 0x0C50;
  public static final int GL_POINT_SMOOTH = 0x0B10;
  public static final int GL_POINT_SMOOTH_HINT = 0x0C51;
  public static final int GL_POINTS = 0x0000;
  public static final int GL_POINT_FADE_THRESHOLD_SIZE = 0x8128;
  public static final int GL_POINT_SIZE = 0x0B11;
  public static final int GL_POLYGON_OFFSET_FILL = 0x8037;
  public static final int GL_POLYGON_SMOOTH_HINT = 0x0C53;
  public static final int GL_POSITION = 0x1203;
  public static final int GL_PROJECTION = 0x1701;
  public static final int GL_QUADRATIC_ATTENUATION = 0x1209;
  public static final int GL_RED_BITS = 0x0D52;
  public static final int GL_RENDERER = 0x1F01;
  public static final int GL_REPEAT = 0x2901;
  public static final int GL_REPLACE = 0x1E01;
  public static final int GL_RESCALE_NORMAL = 0x803A;
  public static final int GL_RGB = 0x1907;
  public static final int GL_RGBA = 0x1908;
  public static final int GL_SAMPLE_ALPHA_TO_COVERAGE = 0x809E;
  public static final int GL_SAMPLE_ALPHA_TO_ONE = 0x809F;
  public static final int GL_SAMPLE_COVERAGE = 0x80A0;
  public static final int GL_SCISSOR_TEST = 0x0C11;
  public static final int GL_SET = 0x150F;
  public static final int GL_SHININESS = 0x1601;
  public static final int GL_SHORT = 0x1402;
  public static final int GL_SMOOTH = 0x1D01;
  public static final int GL_SMOOTH_LINE_WIDTH_RANGE = 0x0B22;
  public static final int GL_SMOOTH_POINT_SIZE_RANGE = 0x0B12;
  public static final int GL_SPECULAR = 0x1202;
  public static final int GL_SPOT_CUTOFF = 0x1206;
  public static final int GL_SPOT_DIRECTION = 0x1204;
  public static final int GL_SPOT_EXPONENT = 0x1205;
  public static final int GL_SRC_ALPHA = 0x0302;
  public static final int GL_SRC_ALPHA_SATURATE = 0x0308;
  public static final int GL_SRC_COLOR = 0x0300;
  public static final int GL_STACK_OVERFLOW = 0x0503;
  public static final int GL_STACK_UNDERFLOW = 0x0504;
  public static final int GL_STENCIL_BITS = 0x0D57;
  public static final int GL_STENCIL_BUFFER_BIT = 0x0400;
  public static final int GL_STENCIL_TEST = 0x0B90;
  public static final int GL_SUBPIXEL_BITS = 0x0D50;
  public static final int GL_TEXTURE = 0x1702;
  public static final int GL_TEXTURE_2D = 0x0DE1;
  public static final int GL_TEXTURE_COORD_ARRAY = 0x8078;
  public static final int GL_TEXTURE_ENV = 0x2300;
  public static final int GL_TEXTURE_ENV_COLOR = 0x2201;
  public static final int GL_TEXTURE_ENV_MODE = 0x2200;
  public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
  public static final int GL_TEXTURE_MIN_FILTER = 0x2801;
  public static final int GL_TEXTURE_WRAP_S = 0x2802;
  public static final int GL_TEXTURE_WRAP_T = 0x2803;
  public static final int GL_TEXTURE0 = 0x84C0;
  public static final int GL_TEXTURE1 = 0x84C1;
  public static final int GL_TEXTURE2 = 0x84C2;
  public static final int GL_TEXTURE3 = 0x84C3;
  public static final int GL_TEXTURE4 = 0x84C4;
  public static final int GL_TEXTURE5 = 0x84C5;
  public static final int GL_TEXTURE6 = 0x84C6;
  public static final int GL_TEXTURE7 = 0x84C7;
  public static final int GL_TEXTURE8 = 0x84C8;
  public static final int GL_TEXTURE9 = 0x84C9;
  public static final int GL_TEXTURE10 = 0x84CA;
  public static final int GL_TEXTURE11 = 0x84CB;
  public static final int GL_TEXTURE12 = 0x84CC;
  public static final int GL_TEXTURE13 = 0x84CD;
  public static final int GL_TEXTURE14 = 0x84CE;
  public static final int GL_TEXTURE15 = 0x84CF;
  public static final int GL_TEXTURE16 = 0x84D0;
  public static final int GL_TEXTURE17 = 0x84D1;
  public static final int GL_TEXTURE18 = 0x84D2;
  public static final int GL_TEXTURE19 = 0x84D3;
  public static final int GL_TEXTURE20 = 0x84D4;
  public static final int GL_TEXTURE21 = 0x84D5;
  public static final int GL_TEXTURE22 = 0x84D6;
  public static final int GL_TEXTURE23 = 0x84D7;
  public static final int GL_TEXTURE24 = 0x84D8;
  public static final int GL_TEXTURE25 = 0x84D9;
  public static final int GL_TEXTURE26 = 0x84DA;
  public static final int GL_TEXTURE27 = 0x84DB;
  public static final int GL_TEXTURE28 = 0x84DC;
  public static final int GL_TEXTURE29 = 0x84DD;
  public static final int GL_TEXTURE30 = 0x84DE;
  public static final int GL_TEXTURE31 = 0x84DF;
  public static final int GL_TRIANGLE_FAN = 0x0006;
  public static final int GL_TRIANGLE_STRIP = 0x0005;
  public static final int GL_TRIANGLES = 0x0004;
  public static final int GL_TRUE = 1;
  public static final int GL_UNPACK_ALIGNMENT = 0x0CF5;
  public static final int GL_UNSIGNED_BYTE = 0x1401;
  public static final int GL_UNSIGNED_SHORT = 0x1403;
  public static final int GL_UNSIGNED_SHORT_4_4_4_4 = 0x8033;
  public static final int GL_UNSIGNED_SHORT_5_5_5_1 = 0x8034;
  public static final int GL_UNSIGNED_SHORT_5_6_5 = 0x8363;
  public static final int GL_VENDOR = 0x1F00;
  public static final int GL_VERSION = 0x1F02;
  public static final int GL_VERTEX_ARRAY = 0x8074;
  public static final int GL_XOR = 0x1506;
  public static final int GL_ZERO = 0;

  public static final int GL_POINT_SIZE_MAX = 0x00008127;
  public static final int GL_POINT_SIZE_MIN = 0x00008126;
  public static final int GL_POINT_DISTANCE_ATTENUATION = 0x00008129;

  /** Only supported in begin..end */
  public static final int _GL_QUADS = 7;
  public static final int _GL_POLYGON = GL_TRIANGLE_FAN;
  public static final int _GL_MODELVIEW_MATRIX = 2982;
  public static final int SIMPLE_TEXUTRED_QUAD = -1;

  private static final int BEGIN_END_MAX_VERTICES = 16384;

  public static final int ARRAY_TEXCOORD_1 = 3;
  public static final int ARRAY_TEXCOORD_0 = 2;
  public static final int ARRAY_COLOR = 1;
  public static final int ARRAY_POSITION = 0;

  private int mode;
  private int staticBufferId = 0;

  // private ShortBuffer shortBuffer = createShortBuffer(64);
  private FloatBuffer texCoordBuf = Lib.newFloatBuffer(BEGIN_END_MAX_VERTICES * 2);
  private FloatBuffer vertexBuf = Lib.newFloatBuffer(BEGIN_END_MAX_VERTICES * 3);
  private FloatBuffer st0011 = Lib.newFloatBuffer(8);
  private int st0011BufferId = generateStaticBufferId();
  private int matrixMode = GL_MODELVIEW;
  protected int viewportX;
  protected int viewportY;
  protected int viewportW;
  protected int viewportH;
  protected float[] projectionMatrix = new float[16];
  protected float[] modelViewMatrix = new float[16];
  protected float[] textureMatrix = new float[16];
  private ArrayList<float[]> projectionMatrixStack = new ArrayList<float[]>();
  private ArrayList<float[]> modelViewMatrixStack = new ArrayList<float[]>();
  private ArrayList<float[]> textureMatrixStack = new ArrayList<float[]>();
  private float[] currentMatrix = modelViewMatrix;
  private ArrayList<float[]> currentMatrixStack = modelViewMatrixStack;
  float[] tmpMatrix = new float[16];
  protected float[] mvpMatrix = new float[16];
  private boolean mvpDirty = true;
 
  protected Gl1Context() {
    Matrix.setIdentityM(modelViewMatrix, 0);
    Matrix.setIdentityM(projectionMatrix, 0);
    Matrix.setIdentityM(textureMatrix, 0);
   
    st0011.put(0);
    st0011.put(0);

    st0011.put(1);
    st0011.put(0);

    st0011.put(1);
    st0011.put(1);

    st0011.put(0);
    st0011.put(1);

    st0011.flip();
  }

  public void glColor3f(float r, float g, float b) {
    glColor4f(r, g, b, 1.0f);
  }

  public int generateStaticBufferId() {
    return staticBufferId++;
  }

  public abstract void glColor4f(float r, float g, float b, float a);

  public abstract void glDisable(int name);

  public abstract void glDisableClientState(int state);

  public abstract void glDrawArrays(int mode, int first, int count);

  public abstract void glEnable(int name);

  public abstract void glEnableClientState(int state);

  public abstract void glTexCoordPointer(int size, int byteStride,
      FloatBuffer buf);

  public abstract void glTexImage2D(int target, int level, int internalformat,
      int width, int height, int border, int format, int type, ByteBuffer pixels);

  public abstract void glTexImage2D(int target, int level, int internalformat,
      int width, int height, int border, int format, int type, IntBuffer pixels);

  public abstract void glTexParameterf(int target, int pname, float param);

  public abstract void glTexSubImage2D(int target, int level, int xoffset,
      int yoffset, int width, int height, int format, int type,
      ByteBuffer pixels);

  public abstract void glTexSubImage2D(int target, int level, int xoffset,
      int yoffset, int width, int height, int format, int type, IntBuffer pixels);

  public abstract void glVertexPointer(int size, int byteStride, FloatBuffer buf);

  public abstract void swapBuffers();

  public void glBegin(int mode) {
    this.mode = mode;
    vertexBuf.clear();
    texCoordBuf.clear();
  }

  public void glTexCoord2f(float x, float y) {
    texCoordBuf.put(x);
    texCoordBuf.put(y);
  }

  public void glVertex2f(int x, int y) {
    glVertex3f(x, y, 0);
  }

  public void glVertex3f(float x, float y, float z) {
    vertexBuf.put(x);
    vertexBuf.put(y);
    vertexBuf.put(z);
  }

  public void glEnd() {
    int count = vertexBuf.position() / 3;

    vertexBuf.flip();

    // TODO: Save & restore state!!!

    glEnableClientState(GL_VERTEX_ARRAY);
    glVertexPointer(3, 0, vertexBuf);

    if (mode == SIMPLE_TEXUTRED_QUAD) {
      glEnableClientState(GL_TEXTURE_COORD_ARRAY);
      glVertexAttribPointer(ARRAY_TEXCOORD_0, 2, GL_FLOAT, false, 0, 0, st0011,
          st0011BufferId);
      glTexCoordPointer(2, 0, st0011);
      glDrawArrays(Gl1Context.GL_TRIANGLE_FAN, 0, 4);
    } else {
      if (texCoordBuf.position() > 0) {
        texCoordBuf.flip();
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glTexCoordPointer(2, 0, texCoordBuf);
      }

      if (mode == _GL_QUADS) {
        // Log.w("GLX", "glDrawQuads; count: " + count);
        for (int i = 0; i < count; i += 4) {
          glDrawArrays(Gl1Context.GL_TRIANGLE_FAN, i, 4);
        }
      } else {
        glDrawArrays(mode, 0, count);
      }
    }
    // glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    // glDisableClientState(GL_VERTEX_ARRAY);
  }

  public void glColor3ub(byte r, byte g, byte b) {
    glColor4ub(r, g, b, (byte) 255);
  }

  public void glColor4ub(byte r, byte g, byte b, byte a) {
    glColor4f((r & 255) / 255.0f, (g & 255) / 255.0f, (b & 255) / 255f,
        (a & 255) / 255.0f);
  }

  public abstract void glDeleteTextures(IntBuffer texnumBuffer);

  public abstract void glTexEnvi(int glTextureEnv, int glTextureEnvMode,
      int mode);

  public abstract void glTexParameteri(int glTexture2d, int glTextureMinFilter,
      int glFilterMin);

  public abstract void glBindTexture(int glTexture2d, int texnum);

  public abstract void glDepthMask(boolean b);

  public abstract void glShadeModel(int sm);

  public abstract void glBlendFunc(int glOne, int glOne2);

  public abstract void glCullFace(int face);

  public abstract void glClear(int bits);

  public abstract void glDepthFunc(int func);

  public abstract void glDepthRange(float gldepthmin, float gldepthmax);

  public abstract void glFinish();

  public abstract void glPolygonMode(int i, int j);

  public abstract void glAlphaFunc(int i, float j);

  public abstract void glClearColor(float r, float g, float b, float a);

  public abstract void glPixelStorei(int i, int j);

  public abstract void glReadPixels(int x, int y, int width, int height,
      int glBgr, int glUnsignedByte, ByteBuffer image);

  public abstract void glDrawElements(int mode, ShortBuffer srcIndexBuf);

  public abstract void glColorPointer(int size, int j, FloatBuffer colorArrayBuf);

  public abstract void glColorPointer(int i, boolean b, int j,
      ByteBuffer colorAsByteBuffer);

  public abstract void glScissor(int x, int i, int width, int height);

  public abstract void glPointSize(float value);

  public abstract String glGetString(int glVendor);

  public abstract int glGetError();

  public abstract void glDrawBuffer(int buf);

  public abstract void glActiveTexture(int texture);

  public abstract void glClientActiveTexture(int texture);



  public abstract void shutdow();

  public void log(String msg) {
    System.out.println(msg);
  }

  public void debugHighlight(boolean on) {

  }

  public abstract void glPointParameterf(int id, float value);

  public void glPointParameterfv(int id, FloatBuffer params) {
  }

  public void updatTCBuffer(FloatBuffer dstTextureCoords, int minIdx, int i) {
    // TODO Auto-generated method stub

  }

  public void logBuffer(String string, FloatBuffer buf, int ofs, int count) {
    StringBuilder sb = new StringBuilder(string);
    for (int i = 0; i < count; i++) {
      sb.append(buf.get(i + ofs));
      sb.append(", ");
    }
    log(sb.toString());
  }

  public void glVertexAttribPointer(int bufIndex, int size, int type,
      boolean normalize, int byteStride, int byteOffset, Buffer nioBuffer,
      int staticDrawId) {
    if (nioBuffer instanceof ByteBuffer) {
      ByteBuffer bb = (ByteBuffer) nioBuffer;
      int pos = bb.position();
      bb.position(pos + byteOffset);
      if (bufIndex != ARRAY_COLOR) {
        throw new UnsupportedOperationException();
      }
      glColorPointer(size, type == GL_UNSIGNED_BYTE, byteStride, bb);
      bb.position(pos);
    } else {
      FloatBuffer fb = (FloatBuffer) nioBuffer;
      int pos = fb.position();
      fb.position(pos + byteOffset / 4);
      switch (bufIndex) {
      case ARRAY_COLOR:
        glColorPointer(size, byteStride, fb);
        break;
      case ARRAY_POSITION:
        glVertexPointer(size, byteStride, fb);
        break;

      case ARRAY_TEXCOORD_0:
      case ARRAY_TEXCOORD_1:
        glTexCoordPointer(size, byteStride, fb);
        break;

      default:
        throw new IllegalArgumentException();
      }
      fb.position(pos);
    }
  }

  public void glLoadIdentity() {
    Matrix.setIdentityM(currentMatrix, 0);
    mvpDirty = true;
  }

  public void glMatrixMode(int mm) {
    switch (mm) {
    case GL_MODELVIEW:
      currentMatrix = modelViewMatrix;
      currentMatrixStack = modelViewMatrixStack;
      break;
    case GL_PROJECTION:
      currentMatrix = projectionMatrix;
      currentMatrixStack = projectionMatrixStack;
      break;
    case GL_TEXTURE:
      currentMatrix = textureMatrix;
      currentMatrixStack = textureMatrixStack;
      break;
    default:
      throw new IllegalArgumentException("Unrecoginzed matrix mode: " + mm);
    }
    this.matrixMode = mm;
  }

  public void glGetInteger(int what, IntBuffer params) {
    switch (what) {
    case GL_MATRIX_MODE:
      params.put(matrixMode);
      break;
    default:
      throw new IllegalArgumentException();
    }
  }

  public final void glMultMatrixf(float[] matrix, int ofs) {
    Matrix.multiplyMM(tmpMatrix, 0, currentMatrix, 0, matrix, ofs);
    System.arraycopy(tmpMatrix, 0, currentMatrix, 0, 16);
    mvpDirty = true;
  }

  public void glPushMatrix() {
    float[] copy = new float[16];
    System.arraycopy(currentMatrix, 0, copy, 0, 16);
    currentMatrixStack.add(copy);
  }

  public void glPopMatrix() {
    float[] top = currentMatrixStack.remove(currentMatrixStack.size() - 1);
    System.arraycopy(top, 0, currentMatrix, 0, 16);
    mvpDirty = true;
  }

  public void glRotatef(float angle, float x, float y, float z) {
    if (x != 0 || y != 0 || z != 0) {
      // right thing to do? or rotate around a default axis?
      Matrix.rotateM(currentMatrix, 0, angle, x, y, z);
    }
    mvpDirty = true;
  }

  public void glScalef(float x, float y, float z) {
    Matrix.scaleM(currentMatrix, 0, x, y, z);
    mvpDirty = true;
  }

  public void glTranslatef(float tx, float ty, float tz) {
    Matrix.translateM(currentMatrix, 0, tx, ty, tz);
    mvpDirty = true;
  }

  public void glViewport(int x, int y, int w, int h) {
    viewportX = x;
    viewportY = y;
    viewportW = w;
    viewportH = h;
  }

  public void glFrustum(double left, double right, double bottom, double top,
      double znear, double zfar) {
    float[] matrix = new float[16];
    double temp, temp2, temp3, temp4;
    temp = 2 * znear;
    temp2 = right - left;
    temp3 = top - bottom;
    temp4 = zfar - znear;
    matrix[0] = (float) (temp / temp2);
    matrix[1] = 0;
    matrix[2] = 0;
    matrix[3] = 0;
    matrix[4] = 0;
    matrix[5] = (float) (temp / temp3);
    matrix[6] = 0;
    matrix[7] = 0;
    matrix[8] = (float) ((right + left) / temp2);
    matrix[9] = (float) ((top + bottom) / temp3);
    matrix[10] = (float) ((-zfar - znear) / temp4);
    matrix[11] = -1;
    matrix[12] = 0;
    matrix[13] = 0;
    matrix[14] = (float) ((-temp * zfar) / temp4);
    matrix[15] = 0;

    glMultMatrixf(matrix, 0);
  }

  protected boolean updateMvpMatrix() {
    if (!mvpDirty) {
      return false;
    }
    Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);
    mvpDirty = false;
    return true;
  }

  public void glOrtho(int left, int right, int bottom, int top, int near,
      int far) {
    float l = left;
    float r = right;
    float b = bottom;
    float n = near;
    float f = far;
    float t = top;

    float[] matrix = { 2f / (r - l), 0, 0, 0,

    0, 2f / (t - b), 0, 0,

    0, 0, -2f / f - n, 0,

    -(r + l) / (r - l), -(t + b) / (t - b), -(f + n) / (f - n), 1f };

    glMultMatrixf(matrix, 0);
    mvpDirty = true;
  }

  public boolean project(float objX, float objY, float objZ, int[] view,
      float[] win) {
    float[] v = { objX, objY, objZ, 1f };

    float[] v2 = new float[4];

    Matrix.multiplyMV(v2, 0, mvpMatrix, 0, v, 0);

    float w = v2[3];
    if (w == 0.0f) {
      return false;
    }

    float rw = 1.0f / w;

    win[0] = viewportX + viewportW * (v2[0] * rw + 1.0f) * 0.5f;
    win[1] = viewportY + viewportH * (v2[1] * rw + 1.0f) * 0.5f;
    win[2] = (v2[2] * rw + 1.0f) * 0.5f;

    return true;
  }

  public void glGetFloat(int name, FloatBuffer result) {
    switch (name) {
    case GL_MODELVIEW:
    case _GL_MODELVIEW_MATRIX:
      // if (AUTO_REWIND) {
      // result.rewind();
      // }
      int p = result.position();
      result.put(modelViewMatrix);
      result.position(p);
      break;
    default:
      throw new IllegalArgumentException("glGetFloat(" + Gl1DebugWrapper.c(name)
          + ")");
    }
  }

  public void glLoadMatrix(FloatBuffer m) {
    // if (AUTO_REWIND) {
    // m.rewind();
    // }
    int p = m.position();
    m.get(currentMatrix);
    m.position(p);
    mvpDirty = true;
  }

  
}
