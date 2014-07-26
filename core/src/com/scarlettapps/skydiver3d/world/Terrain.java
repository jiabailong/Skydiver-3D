// Copyright 2014 Michael Scarlett
// All rights reserved

package com.scarlettapps.skydiver3d.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.scarlettapps.skydiver3d.SkyDiver3D;
import com.scarlettapps.skydiver3d.resources.Graphics;
import com.scarlettapps.skydiver3d.world.gamestate.StatusManager.WorldState;
import com.scarlettapps.skydiver3d.world.utils.DSAlgorithm;
import com.scarlettapps.skydiver3d.world.utils.PerlinNoise;
import com.scarlettapps.skydiver3d.worldview.Renderer;

public class Terrain extends GameObject {
	
	private static final String GROUND_TEXTURE_0 = "data/lightwater.jpg";
	private static final String GROUND_TEXTURE_1 = "data/sandtile.jpg";
	private static final String GROUND_TEXTURE_2 = "data/foliage_tiled.png";
	private static final String GROUND_TEXTURE_3 = "data/grasstile.jpg";

	private Mesh mesh;
	private ShaderProgram shader;
	private Texture texture0;
	private Texture texture1;
	private Texture texture2;
	private Texture texture3;
	private int u_mvpMatrix;
	
	public Terrain() {
		super(false,true);
		
		CustomHeightmap heightmap = new CustomHeightmap(4, MathUtils.random(5,10), MathUtils.random(15,30), 217f, 2, 2790);
		mesh = heightmap.createMesh();
		mesh.setVertices(heightmap.vertices);
		mesh.setIndices(heightmap.indices);
		
		String vertexShader = "#ifdef GL_ES\n"
				+ "precision mediump float;\n"
				+ "#endif\n"
				+ "uniform mat4 u_mvpMatrix;                   \n"
				+ "attribute vec4 a_position;                  \n"
				+ "attribute vec4 a_color;                  \n"
				+ "attribute vec2 a_texCoord0;                  \n"
				+ "attribute float a_texIntensity;                  \n"
				+ "varying vec4 v_color;                  \n"
				+ "varying vec2 v_texCoord0;                  \n"
				+ "varying float v_height;                  \n"
				+ "varying float v_texIntensity;\n"
				+ "void main()                                 \n"
				+ "{                                           \n"
				+ "   v_color = a_color;                       \n"
				+ "   v_height = a_position.z;                       \n"
				+ "   v_texCoord0 = a_texCoord0;                       \n"
				+ "   v_texIntensity = a_texIntensity; \n"
				+ "   vec4 pos = a_position;\n"
				+ "   pos.z = 0.0;\n"
				+ "   gl_Position = u_mvpMatrix *pos;  \n"
				+ "}                            \n";
		String fragmentShader = "#ifdef GL_ES\n"
				+ "precision mediump float;\n"
				+ "#endif\n"
				+ "uniform sampler2D u_texture0;           \n"
				+ "uniform sampler2D u_texture1;           \n"
				+ "uniform sampler2D u_texture2;           \n"
				+ "uniform sampler2D u_texture3;           \n"
				+ "varying vec4 v_color;                  \n"
				+ "varying vec2 v_texCoord0;                  \n"
				+ "varying float v_height;                  \n"
				+ "varying float v_texIntensity;\n"
				+"\n"
				+"vec2 truncate(vec2 v)  \n"
				+"\n{"
				+"float dx = v.x - float(int(v.x));\n"
				+"float dy = v.y - float(int(v.y));\n"
				+"return vec2(dx, dy);\n"
				+"}\n"
				+" \n"
				+ "void main()                                  \n"
				+ "{                                            \n"
				+ "float z = gl_FragCoord.z / gl_FragCoord.w;\n"
				+ "const float LOG2 = 1.442695;\n"
				+ "float fogFactor = clamp(exp2( - 0.00000000005*z*z * LOG2 ),0.0,1.0);\n"
				+ "vec4 fogColor = vec4(1.0, 1.0, 1.0, 1.0);\n"
				+ "vec4 texColor;"
				+ "vec2 newTexCoord = truncate(30.0*v_texCoord0);"
				+ "float h1 = -5.0;\n"
				+ "float h2 = 0.0;\n"
				+ "float h3 = 10.0;\n"
				+ "float h4 = 75.0;\n"
				+ "if (v_height < h1) {"
				+ "  texColor = texture2D(u_texture0, newTexCoord);\n"
				+ "} else if (v_height < h2) {"
				+ "  texColor = mix(texture2D(u_texture0, newTexCoord),texture2D(u_texture1, newTexCoord),(v_height-h1)/(h2-h1));\n"
				+ "} else if (v_height < h3) {"
				+ "  texColor = mix(texture2D(u_texture1, newTexCoord),texture2D(u_texture2, newTexCoord),(v_height-h2)/(h3-h2));\n"
				+ "} else if (v_height < h4) {"
				+ "  texColor = mix(texture2D(u_texture2, newTexCoord),texture2D(u_texture3, newTexCoord),(v_height-h3)/(h4-h3));\n"
				+ "} else {"
				+ "  texColor = texture2D(u_texture3, newTexCoord);\n"
				+ "}"
				+ "  gl_FragColor = mix( fogColor, mix (v_color, texColor, v_texIntensity), fogFactor);\n"
				+ "}";

		shader = new ShaderProgram(vertexShader, fragmentShader);
		
		if (SkyDiver3D.DEV_MODE) {
			if (!shader.isCompiled()) {
				Gdx.app.log(SkyDiver3D.LOG, shader.getLog());
			}
		}
		
		texture0 = Graphics.get(GROUND_TEXTURE_0, Texture.class);
		texture1 = Graphics.get(GROUND_TEXTURE_1, Texture.class);
		texture2 = Graphics.get(GROUND_TEXTURE_2, Texture.class);
		texture3 = Graphics.get(GROUND_TEXTURE_3, Texture.class);
		
		u_texture0 = shader.getUniformLocation("u_texture0");
		u_texture1 = shader.getUniformLocation("u_texture1");
		u_texture2 = shader.getUniformLocation("u_texture2");
		u_texture3 = shader.getUniformLocation("u_texture3");	
		
		u_mvpMatrix = shader.getUniformLocation("u_mvpMatrix");
	}
	
	private static class CustomHeightmap {
		
		private static final float DEFAULT_SEA_LEVEL = -5;

		private final float[] vertices;
		private final short[] indices;

		private final PerlinNoise p;
		private final float[] heightMap;
		private final float strength;
		private final float stretch;
		private final float seaLevel;
		private final int size;

		public CustomHeightmap(int iterations, int seed, int variation,
				float textureWidth, float strength, float stretch) {
			this(iterations, seed, variation, textureWidth, strength, stretch, DEFAULT_SEA_LEVEL);
		}

		public CustomHeightmap(int iterations, int seed, int variation,
				float textureWidth, float strength, float stretch, float seaLevel) {
			this.size = (1 << iterations) + 1;
			this.heightMap = DSAlgorithm.makeHeightMap(iterations, seed, variation);
			this.vertices = new float[heightMap.length * 8];
			this.indices = new short[size * size * 6];
			this.strength = strength;
			this.stretch = stretch;
			this.seaLevel = seaLevel;
			this.p = new PerlinNoise(MathUtils.random(1, 1000));
			createVertices();
			createIndices();
		}

		public void createVertices() {
			int pitch = size + 1;
			int idx = 0;
			int hIdx = 0;
			int boost = MathUtils.random(0, 10);
			for (int y = 0; y < pitch; y++) {
				for (int x = 0; x < pitch; x++) {
					float z = getHeightAvg(hIdx++) * strength + boost;
					vertices[idx++] = (x - pitch / 2) * stretch;
					vertices[idx++] = (y - pitch / 2) * stretch;
					vertices[idx++] = Math.max(z, seaLevel);
					vertices[idx++] = Colorizer.mix(z+MathUtils.random(-10,10));
					vertices[idx++] = ((float) x) / (pitch/10);
					vertices[idx++] = ((float) y) / (pitch/10);
					vertices[idx++] = MathUtils.clamp(p.turbulence2(x * 0.00573f, y * 0.00573f,
									128f) + 0.5f, 0.5f, 1)/2f;
				}
			}
		}

		public void createIndices() {
			int idx = 0;
			short pitch = (short) (size + 1);
			short i1 = 0;
			short i2 = 1;
			short i3 = (short) (1 + pitch);
			short i4 = pitch;

			short row = 0;

			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					indices[idx++] = (short) (i1);
					indices[idx++] = (short) (i2);
					indices[idx++] = (short) (i3);

					indices[idx++] = (short) (i3);
					indices[idx++] = (short) (i4);
					indices[idx++] = (short) (i1);

					i1++;
					i2++;
					i3++;
					i4++;
				}

				row += pitch;
				i1 = row;
				i2 = (short) (row + 1);
				i3 = (short) (i2 + pitch);
				i4 = (short) (row + pitch);
			}
		}

		public float getHeightAvg(int hIdx) {
			return (heightMap[mod(hIdx, heightMap.length)]
					+ heightMap[mod(hIdx + 1, heightMap.length)]
					+ heightMap[mod(hIdx + size, heightMap.length)]
					+ heightMap[mod(hIdx + size + 1, heightMap.length)]
					+ heightMap[mod(hIdx + size - 1, heightMap.length)]
					+ heightMap[mod(hIdx - size + 1, heightMap.length)]
					+ heightMap[mod(hIdx - size - 1, heightMap.length)]
					+ heightMap[mod(hIdx - size, heightMap.length)] + heightMap[mod(
						hIdx - 1, heightMap.length)]) / 9;
		}

		private static int mod(int a, int b) {
			return (((a % b) + b) % b);
		}

		public Mesh createMesh() {
			Mesh mesh = new Mesh(true, this.vertices.length / 3,
					this.indices.length, new VertexAttribute(
							VertexAttributes.Usage.Position, 3, "a_position"),
					new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4,
							"a_color"), new VertexAttribute(
							VertexAttributes.Usage.TextureCoordinates, 2,
							"a_texCoord0"),
					new VertexAttribute(VertexAttributes.Usage.Generic, 1,
							"a_texIntensity"));
			mesh.setVertices(this.vertices);
			mesh.setIndices(this.indices);
			return mesh;
		}

		private static class Colorizer {

			private static final Color DEEP_WATER = new Color(12 / 255f,
					39 / 255f, 136 / 255f, 255 / 255f);
			private static final Color SHALLOW_WATER = new Color(30 / 255f,
					160 / 255f, 206 / 255f, 255 / 255f);
			private static final Color SAND = new Color(200 / 255f, 175 / 255f,
					134 / 255f, 255 / 255f);
			private static final Color LIGHT_GRASS = new Color(113 / 255f,
					123 / 255f, 62 / 255f, 255 / 255f);
			private static final Color DARK_GRASS = new Color(129 / 255f,
					131 / 255f, 82 / 255f, 255 / 255f);

			private static final Color DIRT = new Color(109 / 255f, 167 / 255f,
					38 / 255f, 255 / 255f);
			private static final Color ROCK = new Color(156 / 255f, 156 / 255f,
					156 / 255f, 255 / 255f);

			public static final ColorInterval ZERO = new ColorInterval(
					DEEP_WATER, DEEP_WATER, -100000, -30);
			public static final ColorInterval ONE = new ColorInterval(
					DEEP_WATER, SHALLOW_WATER, -30, -5);
			public static final ColorInterval TWO = new ColorInterval(
					SHALLOW_WATER, SAND, -5, 0);
			public static final ColorInterval THREE = new ColorInterval(SAND,
					LIGHT_GRASS, 0, 10);
			public static final ColorInterval FOUR = new ColorInterval(
					LIGHT_GRASS, DARK_GRASS, 10, 45);
			public static final ColorInterval FIVE = new ColorInterval(
					DARK_GRASS, ROCK, 45, 75);
			public static final ColorInterval SIX = new ColorInterval(DIRT,
					ROCK, 75, 100);
			public static final ColorInterval SEVEN = new ColorInterval(ROCK,
					ROCK, 100, 100000);

			public static final ColorInterval[] colorIntervals = { ZERO, ONE,
					TWO, THREE, FOUR, FIVE, SIX, SEVEN };

			public static float mix(float height) {
				for (ColorInterval i : colorIntervals) {
					if (i.contains(height)) {
						return i.mix(height);
					}
				}
				return -1;
			}

			/**
			 * Class to linearly interpolate between two colors within a
			 * specified interval.
			 */
			public static class ColorInterval {
				/** Temp color reference for generating new color. */
				private static final Color tempColor = new Color();
				/** Color at the minimum of the interval. */
				public final Color minColor;
				/** color at the maximum of the interval */
				public final Color maxColor;
				/** Minimum of the interval (inclusive). */
				public final float min;
				/** Maximum of the interval (inclusive). */
				public final float max;

				/**
				 * Create a new ColorInterval.
				 * 
				 * @param minColor the color for the minimum of the interval.
				 * @param maxColor the color for the maximum of the interval.
				 * @param min the minimum of the interval.
				 * @param max the maximum of the interval.
				 */
				public ColorInterval(Color minColor, Color maxColor, float min,
						float max) {
					this.minColor = minColor;
					this.maxColor = maxColor;
					this.min = min;
					this.max = max;
				}

				/**
				 * Linearly interpolate within this color interval to produce a
				 * new color.
				 * 
				 * @param value the value within the interval.
				 * @return the float bits for the new color.
				 */
				public float mix(float value) {
					tempColor.set(minColor);
					float target = MathUtils.clamp(value - min, 0, max - min)
							/ (max - min);
					tempColor.lerp(maxColor, target);
					return tempColor.toFloatBits();
				}

				/**
				 * Determine if the number is between the min and max of this
				 * interval.
				 * 
				 * @param value the value to check.
				 * @return true if within interval, false otherwise.
				 */
				public boolean contains(float value) {
					return min <= value && value <= max;
				}
			}
		}

	}
	
	int u_texture0;
	int u_texture1;
	int u_texture2;
	int u_texture3;

	public void render(Matrix4 mat) {
		Gdx.gl.glEnable(GL20.GL_TEXTURE_2D);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		texture0.bind();
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE1);
		texture1.bind();
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE2);
		texture2.bind();
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE3);
		texture3.bind();
		shader.begin();
		shader.setUniformi(u_texture0, 0);
		shader.setUniformi(u_texture1, 1);
		shader.setUniformi(u_texture2, 2);
		shader.setUniformi(u_texture3, 3);
		shader.setUniformMatrix(u_mvpMatrix, mat);
		mesh.render(shader, GL20.GL_TRIANGLES);
		shader.end();
	}

	@Override
	protected void updateObject(float delta) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void renderObject(Renderer renderer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onWorldStateChanged(WorldState worldState) {
		// TODO Auto-generated method stub
		
	}
}