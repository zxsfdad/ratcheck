package net.anubis.client.util.pvp;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class RotationUtils {
   private RotationUtils() {
   }

   public static Rotation fromTo(class_243 from, class_243 to) {
      double dx = to.field_1352 - from.field_1352;
      double dy = to.field_1351 - from.field_1351;
      double dz = to.field_1350 - from.field_1350;
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)(class_3532.method_15349(dz, dx) * 180.0F / (float)Math.PI) - 90.0F;
      float pitch = (float)(-(class_3532.method_15349(dy, horizontal) * 180.0F / (float)Math.PI));
      return new Rotation(class_3532.method_15393(yaw), class_3532.method_15363(pitch, -90.0F, 90.0F));
   }

   public static Rotation entityRotation(class_1297 entity) {
      return new Rotation(entity.method_36454(), entity.method_36455());
   }

   public static float yawDelta(float fromYaw, float toYaw) {
      return class_3532.method_15393(toYaw - fromYaw);
   }

   public static float pitchDelta(float fromPitch, float toPitch) {
      return toPitch - fromPitch;
   }

   public static double angleTo(Rotation from, Rotation to) {
      float yaw = yawDelta(from.yaw(), to.yaw());
      float pitch = pitchDelta(from.pitch(), to.pitch());
      return Math.hypot(yaw, pitch);
   }

   public static double mouseScale(class_310 mc, class_746 player) {
      double speed = (Double)mc.field_1690.method_42495().method_41753() * 0.6F + 0.2F;
      return speed * speed * speed * (mc.field_1690.method_31044().method_31034() && player.method_31550() ? 1.0 : 8.0);
   }
}
