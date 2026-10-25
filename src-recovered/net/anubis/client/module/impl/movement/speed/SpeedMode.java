package net.anubis.client.module.impl.movement.speed;

import net.anubis.client.module.impl.movement.SpeedModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1293;
import net.minecraft.class_1294;
import net.minecraft.class_243;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public abstract class SpeedMode {
   private static final double BASE_SPEED = 0.2873;
   protected final SpeedModule settings;
   protected int stage;
   protected double distance;
   protected double speed;

   protected SpeedMode(SpeedModule settings) {
      this.settings = settings;
      this.reset();
   }

   public void onTick(class_746 player) {
   }

   public class_243 onMove(class_746 player, class_243 movement) {
      return movement;
   }

   public void onRubberband() {
      this.reset();
   }

   public void onActivate() {
   }

   public void onDeactivate() {
   }

   protected double getDefaultSpeed(class_746 player) {
      double defaultSpeed = 0.2873;
      class_1293 speedEffect = player.method_6112(class_1294.field_5904);
      if (speedEffect != null) {
         defaultSpeed *= 1.0 + 0.2 * (speedEffect.method_5578() + 1);
      }

      class_1293 slowness = player.method_6112(class_1294.field_5909);
      if (slowness != null) {
         defaultSpeed /= 1.0 + 0.2 * (slowness.method_5578() + 1);
      }

      return defaultSpeed;
   }

   protected void reset() {
      this.stage = 0;
      this.distance = 0.0;
      this.speed = 0.2873;
   }

   protected double getHop(class_746 player, double height) {
      class_1293 jumpBoost = player.method_6112(class_1294.field_5913);
      if (jumpBoost != null) {
         height += (jumpBoost.method_5578() + 1) * 0.1F;
      }

      return height;
   }
}
