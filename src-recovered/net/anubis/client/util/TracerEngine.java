package net.anubis.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_408;
import net.minecraft.class_4184;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class TracerEngine {
   private static final float WIDTH = 1.5F;
   private static boolean active;
   private static class_243 cameraPos = class_243.field_1353;
   private static class_243 origin = class_243.field_1353;
   private static FoglessLines lines;
   private static final Matrix4f viewProjection = new Matrix4f();

   private TracerEngine() {
   }

   public static void beginFrame(Matrix4fc viewRotation, Matrix4fc projection) {
      active = false;
      lines = null;
      class_310 mc = class_310.method_1551();
      if (mc.field_1724 != null && mc.field_1687 != null) {
         if (mc.field_1755 == null || mc.field_1755 instanceof class_408) {
            class_4184 camera = mc.field_1773.method_19418();
            if (camera != null && camera.method_19332()) {
               viewProjection.set(projection).mul(viewRotation);
               Matrix4f inverse = viewProjection.invert(new Matrix4f());
               Vector4f centre = inverse.transform(new Vector4f(0.0F, 0.0F, 0.0F, 1.0F));
               Vector4f eye = inverse.transform(new Vector4f(0.0F, 0.0F, -1.0F, 0.0F));
               cameraPos = camera.method_71156();
               origin = cameraPos.method_1031(centre.x / centre.w, centre.y / centre.w, centre.z / centre.w);
               class_243 eyePos = cameraPos.method_1031(eye.x / eye.w, eye.y / eye.w, eye.z / eye.w);
               Vector3fc forward = new Vector3f()
                  .set(
                     (float)(origin.field_1352 - eyePos.field_1352),
                     (float)(origin.field_1351 - eyePos.field_1351),
                     (float)(origin.field_1350 - eyePos.field_1350)
                  )
                  .normalize();
               double pixel = 2.0 / (projection.m11() * Math.max(1, mc.method_22683().method_4506()));
               lines = FoglessLines.forView(eyePos, forward, pixel);
               active = true;
            }
         }
      }
   }

   public static void draw(class_243 target, int argb) {
      if (active) {
         lines.line(origin, target, argb, 1.5F);
      }
   }

   public static void stem(class_243 feet, double height, int argb) {
      if (active) {
         lines.line(feet, feet.method_1031(0.0, height, 0.0), argb, 1.5F);
      }
   }

   public static Vector4f clip(class_243 world) {
      return !active
         ? null
         : viewProjection.transform(
            new Vector4f(
               (float)(world.field_1352 - cameraPos.field_1352),
               (float)(world.field_1351 - cameraPos.field_1351),
               (float)(world.field_1350 - cameraPos.field_1350),
               1.0F
            )
         );
   }

   public static boolean isInFront(class_4184 camera, class_243 target) {
      class_243 delta = target.method_1020(camera.method_71156());
      Vector3fc fwd = camera.method_19335();
      return delta.field_1352 * fwd.x() + delta.field_1351 * fwd.y() + delta.field_1350 * fwd.z() > 0.05;
   }
}
