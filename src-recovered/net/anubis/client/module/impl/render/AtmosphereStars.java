package net.anubis.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_287;
import net.minecraft.class_3532;
import net.minecraft.class_4587;
import net.minecraft.class_5819;
import net.minecraft.class_9848;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
final class AtmosphereStars {
   private static final float RADIUS = 100.0F;
   private static final int STARS = 2600;
   private static final float GLOW_ABOVE = 0.8F;
   private static final long TWINKLE_NANOS = 50000000L;
   private static final int BAND_COLUMNS = 180;
   private static final int BAND_ROWS = 12;
   private static final float BAND_HALF_WIDTH = 17.0F;
   private static final int NEBULA_COLUMNS = 72;
   private static final int NEBULA_ROWS = 36;
   private static final Vector3f BAND_NORMAL = new Vector3f(0.42F, 0.5F, 0.76F).normalize();
   private final SkyMesh starMesh = new SkyMesh("Anubis stars", SkyMesh.GLOW, 5200);
   private final SkyMesh bandMesh = new SkyMesh("Anubis milky way", SkyMesh.GLOW, 2160);
   private final SkyMesh nebulaMesh = new SkyMesh("Anubis end nebulae", SkyMesh.GLOW, 2592);
   private final float[] dir = new float[7800];
   private final float[] size = new float[2600];
   private final float[] light = new float[2600];
   private final float[] roll = new float[2600];
   private final float[] phase = new float[2600];
   private final float[] speed = new float[2600];
   private final int[] rgb = new int[2600];
   private boolean placed;
   private long lastTwinkle;
   private float builtBrightness = -1.0F;
   private int bandTint;

   void renderStars(class_4587 pose, float alpha, float brightness) {
      long now = System.nanoTime();
      if (now - this.lastTwinkle > 50000000L || brightness != this.builtBrightness) {
         this.lastTwinkle = now;
         this.builtBrightness = brightness;
         this.buildStars((float)(now % 3600000000000L) / 1.0E9F, brightness);
      }

      this.starMesh.draw(pose, alpha);
   }

   void renderBand(class_4587 pose, float alpha, int tint) {
      if (!this.bandMesh.isBuilt() || tint != this.bandTint) {
         this.bandTint = tint;
         this.buildBand(tint);
      }

      this.bandMesh.draw(pose, alpha);
   }

   void renderNebulae(class_4587 pose, float alpha) {
      if (!this.nebulaMesh.isBuilt()) {
         this.buildNebulae();
      }

      this.nebulaMesh.draw(pose, alpha);
   }

   private void place() {
      class_5819 random = class_5819.method_43049(10842L);
      Vector3f a = new Vector3f();
      Vector3f b = new Vector3f();
      bandAxes(a, b);

      for (int i = 0; i < 2600; i++) {
         float x;
         float y;
         float z;
         if (random.method_43057() < 0.3F) {
            float along = random.method_43057() * (float) (Math.PI * 2);
            float across = (float)random.method_43059() * 9.0F * (float) (Math.PI / 180.0);
            float cosAcross = class_3532.method_15362(across);
            float sinAcross = class_3532.method_15374(across);
            x = cosAcross * (class_3532.method_15362(along) * a.x + class_3532.method_15374(along) * b.x) + sinAcross * BAND_NORMAL.x;
            y = cosAcross * (class_3532.method_15362(along) * a.y + class_3532.method_15374(along) * b.y) + sinAcross * BAND_NORMAL.y;
            z = cosAcross * (class_3532.method_15362(along) * a.z + class_3532.method_15374(along) * b.z) + sinAcross * BAND_NORMAL.z;
         } else {
            y = random.method_43057() * 2.0F - 1.0F;
            float around = random.method_43057() * (float) (Math.PI * 2);
            float flat = class_3532.method_15355(1.0F - y * y);
            x = flat * class_3532.method_15362(around);
            z = flat * class_3532.method_15374(around);
         }

         this.dir[i * 3] = x;
         this.dir[i * 3 + 1] = y;
         this.dir[i * 3 + 2] = z;
         float big = (float)Math.pow(random.method_43057(), 6.0);
         this.size[i] = 0.1F + 0.5F * big;
         this.light[i] = class_3532.method_15363(0.3F + 0.7F * big + 0.25F * random.method_43057(), 0.0F, 1.0F);
         this.roll[i] = random.method_43057() * (float) (Math.PI * 2);
         this.phase[i] = random.method_43057() * (float) (Math.PI * 2);
         this.speed[i] = 1.5F + 3.5F * random.method_43057();
         this.rgb[i] = starColour(random.method_43057());
      }

      this.placed = true;
   }

   private static int starColour(float t) {
      if (t < 0.18F) {
         return class_9848.method_61319(t / 0.18F, 16757870, 16774116);
      } else {
         return t < 0.75F ? 16775408 : class_9848.method_61319((t - 0.75F) / 0.25F, 16774134, 11125503);
      }
   }

   private void buildStars(float time, float brightness) {
      if (!this.placed) {
         this.place();
      }

      class_287 builder = this.starMesh.begin();

      for (int i = 0; i < 2600; i++) {
         float twinkle = 0.62F + 0.38F * class_3532.method_15374(time * this.speed[i] + this.phase[i]);
         float alpha = class_3532.method_15363(this.light[i] * twinkle * brightness, 0.0F, 1.0F);
         this.star(builder, i, this.size[i], alpha);
         if (this.light[i] > 0.8F) {
            this.star(builder, i, this.size[i] * 4.0F, alpha * 0.12F);
         }
      }

      this.starMesh.upload(builder);
   }

   private void star(class_287 builder, int i, float half, float alpha) {
      float x = this.dir[i * 3];
      float y = this.dir[i * 3 + 1];
      float z = this.dir[i * 3 + 2];
      float ux;
      float uy;
      float uz;
      if (Math.abs(y) < 0.9F) {
         ux = z;
         uy = 0.0F;
         uz = -x;
      } else {
         ux = 0.0F;
         uy = -z;
         uz = y;
      }

      float ul = class_3532.method_15355(ux * ux + uy * uy + uz * uz);
      ux /= ul;
      uy /= ul;
      uz /= ul;
      float vx = y * uz - z * uy;
      float vy = z * ux - x * uz;
      float vz = x * uy - y * ux;
      float c = class_3532.method_15362(this.roll[i]) * half;
      float s = class_3532.method_15374(this.roll[i]) * half;
      float ax = ux * c + vx * s;
      float ay = uy * c + vy * s;
      float az = uz * c + vz * s;
      float bx = vx * c - ux * s;
      float by = vy * c - uy * s;
      float bz = vz * c - uz * s;
      float cx = x * 100.0F;
      float cy = y * 100.0F;
      float cz = z * 100.0F;
      int argb = Math.round(alpha * 255.0F) << 24 | this.rgb[i];
      SkyMesh.vertex(builder, cx - ax - bx, cy - ay - by, cz - az - bz, argb);
      SkyMesh.vertex(builder, cx + ax - bx, cy + ay - by, cz + az - bz, argb);
      SkyMesh.vertex(builder, cx + ax + bx, cy + ay + by, cz + az + bz, argb);
      SkyMesh.vertex(builder, cx - ax + bx, cy - ay + by, cz - az + bz, argb);
   }

   private static void bandAxes(Vector3f a, Vector3f b) {
      new Vector3f(0.0F, 1.0F, 0.0F).cross(BAND_NORMAL, a).normalize();
      new Vector3f(BAND_NORMAL).cross(a, b).normalize();
   }

   private void buildBand(int tint) {
      Vector3f a = new Vector3f();
      Vector3f b = new Vector3f();
      bandAxes(a, b);
      float[][] px = new float[181][13];
      float[][] py = new float[181][13];
      float[][] pz = new float[181][13];
      int[][] colour = new int[181][13];

      for (int c = 0; c <= 180; c++) {
         float along = c / 180.0F * (float) (Math.PI * 2);
         float ax = class_3532.method_15362(along) * a.x + class_3532.method_15374(along) * b.x;
         float ay = class_3532.method_15362(along) * a.y + class_3532.method_15374(along) * b.y;
         float az = class_3532.method_15362(along) * a.z + class_3532.method_15374(along) * b.z;
         float width = 0.55F + 0.45F * fbm(ax * 1.7F, ay * 1.7F, az * 1.7F);
         float rift = 3.0F * (fbm(ax * 2.3F + 5.0F, ay * 2.3F, az * 2.3F) - 0.5F);
         float riftDepth = class_3532.method_15363(fbm(ax * 1.4F, ay * 1.4F + 9.0F, az * 1.4F) * 1.6F - 0.35F, 0.0F, 1.0F);

         for (int r = 0; r <= 12; r++) {
            float acrossDeg = (r / 12.0F * 2.0F - 1.0F) * 17.0F;
            float across = acrossDeg * (float) (Math.PI / 180.0);
            float cosAcross = class_3532.method_15362(across);
            float sinAcross = class_3532.method_15374(across);
            float x = cosAcross * ax + sinAcross * BAND_NORMAL.x;
            float y = cosAcross * ay + sinAcross * BAND_NORMAL.y;
            float z = cosAcross * az + sinAcross * BAND_NORMAL.z;
            px[c][r] = x * 100.0F;
            py[c][r] = y * 100.0F;
            pz[c][r] = z * 100.0F;
            float edge = acrossDeg / (17.0F * width);
            float core = (float)Math.exp(-edge * edge * 2.2F);
            float dust = 0.35F + 0.65F * fbm(x * 6.0F, y * 6.0F, z * 6.0F);
            float lane = 1.0F - 0.75F * riftDepth * (float)Math.exp(-sq((acrossDeg - rift) / 2.6F));
            float glow = class_3532.method_15363(core * dust * lane, 0.0F, 1.0F);
            int rgb = class_9848.method_61319(core, 7306440, tint) & 16777215;
            colour[c][r] = Math.round(glow * 0.42F * 255.0F) << 24 | rgb;
         }
      }

      class_287 builder = this.bandMesh.begin();

      for (int c = 0; c < 180; c++) {
         for (int r = 0; r < 12; r++) {
            SkyMesh.vertex(builder, px[c][r], py[c][r], pz[c][r], colour[c][r]);
            SkyMesh.vertex(builder, px[c + 1][r], py[c + 1][r], pz[c + 1][r], colour[c + 1][r]);
            SkyMesh.vertex(builder, px[c + 1][r + 1], py[c + 1][r + 1], pz[c + 1][r + 1], colour[c + 1][r + 1]);
            SkyMesh.vertex(builder, px[c][r + 1], py[c][r + 1], pz[c][r + 1], colour[c][r + 1]);
         }
      }

      this.bandMesh.upload(builder);
   }

   private void buildNebulae() {
      float[][] px = new float[73][37];
      float[][] py = new float[73][37];
      float[][] pz = new float[73][37];
      int[][] colour = new int[73][37];

      for (int c = 0; c <= 72; c++) {
         float around = c / 72.0F * (float) (Math.PI * 2);

         for (int r = 0; r <= 36; r++) {
            float el = (r / 36.0F - 0.5F) * (float) Math.PI;
            float x = class_3532.method_15362(el) * class_3532.method_15362(around);
            float y = class_3532.method_15374(el);
            float z = class_3532.method_15362(el) * class_3532.method_15374(around);
            px[c][r] = x * 100.0F;
            py[c][r] = y * 100.0F;
            pz[c][r] = z * 100.0F;
            float density = AtmosphereAurora.smoothstep((fbm(x * 2.2F, y * 2.2F, z * 2.2F) - 0.42F) / 0.35F);
            float hue = fbm(x * 1.3F + 11.0F, y * 1.3F, z * 1.3F);
            int rgb = hue < 0.5F ? class_9848.method_61319(hue * 2.0F, 8011007, 16732104) : class_9848.method_61319((hue - 0.5F) * 2.0F, 16732104, 3989712);
            colour[c][r] = Math.round(density * 0.55F * 255.0F) << 24 | rgb & 16777215;
         }
      }

      class_287 builder = this.nebulaMesh.begin();

      for (int c = 0; c < 72; c++) {
         for (int r = 0; r < 36; r++) {
            SkyMesh.vertex(builder, px[c][r], py[c][r], pz[c][r], colour[c][r]);
            SkyMesh.vertex(builder, px[c + 1][r], py[c + 1][r], pz[c + 1][r], colour[c + 1][r]);
            SkyMesh.vertex(builder, px[c + 1][r + 1], py[c + 1][r + 1], pz[c + 1][r + 1], colour[c + 1][r + 1]);
            SkyMesh.vertex(builder, px[c][r + 1], py[c][r + 1], pz[c][r + 1], colour[c][r + 1]);
         }
      }

      this.nebulaMesh.upload(builder);
   }

   static float fbm(float x, float y, float z) {
      float sum = 0.0F;
      float amplitude = 0.5F;
      float total = 0.0F;

      for (int octave = 0; octave < 4; octave++) {
         sum += amplitude * noise(x, y, z);
         total += amplitude;
         x *= 2.03F;
         y *= 2.03F;
         z *= 2.03F;
         amplitude *= 0.5F;
      }

      return sum / total;
   }

   private static float noise(float x, float y, float z) {
      int ix = class_3532.method_15375(x);
      int iy = class_3532.method_15375(y);
      int iz = class_3532.method_15375(z);
      float fx = x - ix;
      float fy = y - iy;
      float fz = z - iz;
      fx = fx * fx * (3.0F - 2.0F * fx);
      fy = fy * fy * (3.0F - 2.0F * fy);
      fz = fz * fz * (3.0F - 2.0F * fz);
      float x00 = class_3532.method_16439(fx, hash(ix, iy, iz), hash(ix + 1, iy, iz));
      float x10 = class_3532.method_16439(fx, hash(ix, iy + 1, iz), hash(ix + 1, iy + 1, iz));
      float x01 = class_3532.method_16439(fx, hash(ix, iy, iz + 1), hash(ix + 1, iy, iz + 1));
      float x11 = class_3532.method_16439(fx, hash(ix, iy + 1, iz + 1), hash(ix + 1, iy + 1, iz + 1));
      return class_3532.method_16439(fz, class_3532.method_16439(fy, x00, x10), class_3532.method_16439(fy, x01, x11));
   }

   private static float hash(int x, int y, int z) {
      int h = x * 374761393 + y * 668265263 + z * 1274126177;
      h = (h ^ h >>> 13) * 1274126177;
      return ((h ^ h >>> 16) & 16777215) / 1.6777215E7F;
   }

   private static float sq(float v) {
      return v * v;
   }

   void close() {
      this.starMesh.close();
      this.bandMesh.close();
      this.nebulaMesh.close();
      this.builtBrightness = -1.0F;
   }
}
