package net.anubis.client.util.glyph;

import java.util.Arrays;
import net.anubis.client.util.WorldQuads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12175;
import net.minecraft.class_12177;
import net.minecraft.class_12178;
import net.minecraft.class_12180;
import net.minecraft.class_243;
import net.minecraft.class_3532;
import net.minecraft.class_4184;
import net.minecraft.class_9848;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public final class GlyphCanvas implements class_12175 {
   public static final int MAX_QUADS = 12000;
   private static final int MAX_LINES = 4000;
   private static final int MIN_ALPHA = 3;
   private static final int VERTEX_BITS = 9;
   private static final double RIBBON_NEAR = 0.05;
   private final class_243[] vertices = new class_243[512];
   private double[] quadPos = new double[3072];
   private int[] quadColor = new int[256];
   private int quads;
   private double[] linePos = new double[384];
   private int[] lineColor = new int[64];
   private float[] lineWidth = new float[64];
   private int lines;
   private double camX;
   private double camY;
   private double camZ;
   private double rightX;
   private double rightY;
   private double rightZ;
   private double upX;
   private double upY;
   private double upZ;
   private double lookX;
   private double lookY;
   private double lookZ;

   public void begin(class_4184 camera) {
      this.quads = 0;
      this.lines = 0;
      class_243 eye = camera.method_71156();
      this.camX = eye.field_1352;
      this.camY = eye.field_1351;
      this.camZ = eye.field_1350;
      Vector3fc left = camera.method_35689();
      Vector3fc up = camera.method_19336();
      Vector3fc forward = camera.method_19335();
      this.rightX = -left.x();
      this.rightY = -left.y();
      this.rightZ = -left.z();
      this.upX = up.x();
      this.upY = up.y();
      this.upZ = up.z();
      this.lookX = forward.x();
      this.lookY = forward.y();
      this.lookZ = forward.z();
   }

   public void submit(boolean onTop) {
      if (this.quads > 0) {
         WorldQuads.layer(onTop).quads(this.quadPos, this.quadColor, this.quads);
      }

      this.quads = 0;
      if (this.lines != 0) {
         class_12178 properties = class_12180.method_75553(this);
         if (onTop) {
            properties.method_75533();
         }
      }
   }

   public double camX() {
      return this.camX;
   }

   public double camY() {
      return this.camY;
   }

   public double camZ() {
      return this.camZ;
   }

   public double depth(double x, double y, double z) {
      return (x - this.camX) * this.lookX + (y - this.camY) * this.lookY + (z - this.camZ) * this.lookZ;
   }

   public double distanceSqr(double x, double y, double z) {
      double dx = x - this.camX;
      double dy = y - this.camY;
      double dz = z - this.camZ;
      return dx * dx + dy * dy + dz * dz;
   }

   public void quad(
      double ax, double ay, double az, double bx, double by, double bz, double cx, double cy, double cz, double dx, double dy, double dz, int argb
   ) {
      if (class_9848.method_61320(argb) >= 3 && this.reserveQuad()) {
         int p = this.quads * 12;
         double[] out = this.quadPos;
         out[p] = ax;
         out[p + 1] = ay;
         out[p + 2] = az;
         out[p + 3] = bx;
         out[p + 4] = by;
         out[p + 5] = bz;
         out[p + 6] = cx;
         out[p + 7] = cy;
         out[p + 8] = cz;
         out[p + 9] = dx;
         out[p + 10] = dy;
         out[p + 11] = dz;
         this.quadColor[this.quads++] = argb;
      }
   }

   public void outwardTriangle(
      double ax, double ay, double az, double bx, double by, double bz, double cx, double cy, double cz, double ox, double oy, double oz, int argb
   ) {
      double ux = bx - ax;
      double uy = by - ay;
      double uz = bz - az;
      double vx = cx - ax;
      double vy = cy - ay;
      double vz = cz - az;
      double nx = uy * vz - uz * vy;
      double ny = uz * vx - ux * vz;
      double nz = ux * vy - uy * vx;
      double mx = (ax + bx + cx) / 3.0 - ox;
      double my = (ay + by + cy) / 3.0 - oy;
      double mz = (az + bz + cz) / 3.0 - oz;
      if (nx * mx + ny * my + nz * mz >= 0.0) {
         this.quad(ax, ay, az, bx, by, bz, cx, cy, cz, cx, cy, cz, argb);
      } else {
         this.quad(ax, ay, az, cx, cy, cz, bx, by, bz, bx, by, bz, argb);
      }
   }

   public void line(double ax, double ay, double az, double bx, double by, double bz, int argb, float width) {
      if (class_9848.method_61320(argb) >= 3 && this.reserveLine()) {
         int p = this.lines * 6;
         double[] out = this.linePos;
         out[p] = ax;
         out[p + 1] = ay;
         out[p + 2] = az;
         out[p + 3] = bx;
         out[p + 4] = by;
         out[p + 5] = bz;
         this.lineColor[this.lines] = argb;
         this.lineWidth[this.lines++] = width;
      }
   }

   public void ribbon(double ax, double ay, double az, double bx, double by, double bz, int argb, float width, double pixel) {
      double depthA = this.depth(ax, ay, az);
      double depthB = this.depth(bx, by, bz);
      if (!(depthA < 0.05) || !(depthB < 0.05)) {
         if (depthA < 0.05) {
            double t = (0.05 - depthA) / (depthB - depthA);
            ax += (bx - ax) * t;
            ay += (by - ay) * t;
            az += (bz - az) * t;
            depthA = 0.05;
         } else if (depthB < 0.05) {
            double t = (0.05 - depthB) / (depthA - depthB);
            bx += (ax - bx) * t;
            by += (ay - by) * t;
            bz += (az - bz) * t;
            depthB = 0.05;
         }

         double fx = ax - this.camX;
         double fy = ay - this.camY;
         double fz = az - this.camZ;
         double ex = bx - ax;
         double ey = by - ay;
         double ez = bz - az;
         double sx = fy * ez - fz * ey;
         double sy = fz * ex - fx * ez;
         double sz = fx * ey - fy * ex;
         double length = Math.sqrt(sx * sx + sy * sy + sz * sz);
         if (!(length < 1.0E-9)) {
            double scale = width * 0.5 * pixel / length;
            double ka = scale * depthA;
            double kb = scale * depthB;
            this.quad(
               ax + sx * ka,
               ay + sy * ka,
               az + sz * ka,
               bx + sx * kb,
               by + sy * kb,
               bz + sz * kb,
               bx - sx * kb,
               by - sy * kb,
               bz - sz * kb,
               ax - sx * ka,
               ay - sy * ka,
               az - sz * ka,
               argb
            );
         }
      }
   }

   public void shape(float[] shape, double x, double y, double z, double ux, double uy, double uz, double vx, double vy, double vz, int argb) {
      if (class_9848.method_61320(argb) >= 3) {
         for (int i = 0; i + 7 < shape.length; i += 8) {
            float u0 = shape[i];
            float v0 = shape[i + 1];
            float u1 = shape[i + 2];
            float v1 = shape[i + 3];
            float u2 = shape[i + 4];
            float v2 = shape[i + 5];
            float u3 = shape[i + 6];
            float v3 = shape[i + 7];
            this.quad(
               x + ux * u0 + vx * v0,
               y + uy * u0 + vy * v0,
               z + uz * u0 + vz * v0,
               x + ux * u1 + vx * v1,
               y + uy * u1 + vy * v1,
               z + uz * u1 + vz * v1,
               x + ux * u2 + vx * v2,
               y + uy * u2 + vy * v2,
               z + uz * u2 + vz * v2,
               x + ux * u3 + vx * v3,
               y + uy * u3 + vy * v3,
               z + uz * u3 + vz * v3,
               argb
            );
         }
      }
   }

   public void sprite(float[] shape, double x, double y, double z, float size, float angle, int argb) {
      this.sprite(shape, x, y, z, size, size, angle, argb);
   }

   public void sprite(float[] shape, double x, double y, double z, float width, float height, float angle, int argb) {
      this.sprite(shape, x, y, z, width, height, angle, 0.0F, 0.0F, argb);
   }

   public void sprite(float[] shape, double x, double y, double z, float width, float height, float angle, float shiftU, float shiftV, int argb) {
      if (class_9848.method_61320(argb) >= 3) {
         float cos = class_3532.method_15362(angle);
         float sin = class_3532.method_15374(angle);
         double ux = (this.rightX * cos + this.upX * sin) * width;
         double uy = (this.rightY * cos + this.upY * sin) * width;
         double uz = (this.rightZ * cos + this.upZ * sin) * width;
         double vx = (this.upX * cos - this.rightX * sin) * height;
         double vy = (this.upY * cos - this.rightY * sin) * height;
         double vz = (this.upZ * cos - this.rightZ * sin) * height;
         this.shape(shape, x + ux * shiftU + vx * shiftV, y + uy * shiftU + vy * shiftV, z + uz * shiftU + vz * shiftV, ux, uy, uz, vx, vy, vz, argb);
      }
   }

   public void flat(float[] shape, double x, double y, double z, float size, float angle, int argb) {
      float cos = class_3532.method_15362(angle) * size;
      float sin = class_3532.method_15374(angle) * size;
      double flip = this.camY >= y ? -1.0 : 1.0;
      this.shape(shape, x, y, z, cos, 0.0, flip * sin, -sin, 0.0, flip * cos, argb);
   }

   public void band(double x, double y, double z, float inner, float outer, float from, float to, int segments, int argb) {
      if (!(outer <= inner) && segments > 0 && class_9848.method_61320(argb) >= 3) {
         double flip = this.camY >= y ? -1.0 : 1.0;
         float step = (to - from) / segments;
         float cosA = class_3532.method_15362(from);
         float sinA = class_3532.method_15374(from);

         for (int i = 1; i <= segments; i++) {
            float angle = from + step * i;
            float cosB = class_3532.method_15362(angle);
            float sinB = class_3532.method_15374(angle);
            this.quad(
               x + cosA * inner,
               y,
               z + flip * sinA * inner,
               x + cosA * outer,
               y,
               z + flip * sinA * outer,
               x + cosB * outer,
               y,
               z + flip * sinB * outer,
               x + cosB * inner,
               y,
               z + flip * sinB * inner,
               argb
            );
            cosA = cosB;
            sinA = sinB;
         }
      }
   }

   public void ring(double x, double y, double z, float inner, float outer, int segments, int argb) {
      this.band(x, y, z, inner, outer, 0.0F, (float) (Math.PI * 2), segments, argb);
   }

   public void method_75531(class_12177 out, float alpha) {
      boolean faded = alpha < 1.0F;
      double[] l = this.linePos;
      int i = 0;

      for (int p = 0; i < this.lines; p += 6) {
         int color = faded ? class_9848.method_75601(this.lineColor[i], alpha) : this.lineColor[i];
         out.method_75473(this.vertex(l[p], l[p + 1], l[p + 2]), this.vertex(l[p + 3], l[p + 4], l[p + 5]), color, this.lineWidth[i]);
         i++;
      }
   }

   private class_243 vertex(double x, double y, double z) {
      long hash = Double.doubleToRawLongBits(x) * -7046029254386353131L
         ^ Double.doubleToRawLongBits(y) * -4417276706812531889L
         ^ Double.doubleToRawLongBits(z) * 1609587929392839161L;
      int slot = (int)(hash >>> 55);
      class_243 cached = this.vertices[slot];
      if (cached != null && cached.field_1352 == x && cached.field_1351 == y && cached.field_1350 == z) {
         return cached;
      }

      class_243 made = new class_243(x, y, z);
      this.vertices[slot] = made;
      return made;
   }

   private boolean reserveQuad() {
      if (this.quads < this.quadColor.length) {
         return true;
      }

      if (this.quads >= 12000) {
         return false;
      }

      int grown = Math.min(12000, this.quadColor.length * 2);
      this.quadColor = Arrays.copyOf(this.quadColor, grown);
      this.quadPos = Arrays.copyOf(this.quadPos, grown * 12);
      return true;
   }

   private boolean reserveLine() {
      if (this.lines < this.lineColor.length) {
         return true;
      }

      if (this.lines >= 4000) {
         return false;
      }

      int grown = Math.min(4000, this.lineColor.length * 2);
      this.lineColor = Arrays.copyOf(this.lineColor, grown);
      this.lineWidth = Arrays.copyOf(this.lineWidth, grown);
      this.linePos = Arrays.copyOf(this.linePos, grown * 6);
      return true;
   }
}
