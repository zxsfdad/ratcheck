package net.anubis.client.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1301;
import net.minecraft.class_1676;
import net.minecraft.class_1776;
import net.minecraft.class_1802;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_3486;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_746;
import net.minecraft.class_239.class_240;
import net.minecraft.class_3959.class_242;
import net.minecraft.class_3959.class_3960;

@Environment(EnvType.CLIENT)
public final class PearlTrajectorySimulator {
   private static final double SPAWN_EYE_OFFSET = 0.1;
   private static final double GRAVITY = 0.03;
   private static final float AIR_DRAG = 0.99F;
   private static final float WATER_DRAG = 0.8F;
   private static final int MAX_TICKS = 300;
   private static final double ENTITY_MARGIN = 0.3;

   private PearlTrajectorySimulator() {
   }

   public static PearlTrajectorySimulator.SimulationResult simulate(class_1937 level, class_746 player, float partialTick) {
      class_243 eye = player.method_5836(partialTick);
      class_243 pos = new class_243(eye.field_1352, eye.field_1351 - 0.1, eye.field_1350);
      class_243 velocity = player.method_5828(partialTick).method_1021(class_1776.field_55033);
      class_243 motion = player.method_60478();
      velocity = velocity.method_1031(motion.field_1352, player.method_24828() ? 0.0 : motion.field_1351, motion.field_1350);
      return simulate(level, pos, velocity, player, null);
   }

   public static PearlTrajectorySimulator.SimulationResult simulate(class_1937 level, class_1676 pearl) {
      return simulate(level, pearl.method_73189(), pearl.method_18798(), pearl, pearl.method_24921());
   }

   public static PearlTrajectorySimulator.SimulationResult simulate(
      class_1937 level, class_243 start, class_243 velocity, class_1297 ignore, class_1297 alsoIgnore
   ) {
      List<class_243> points = new ArrayList<>(64);
      points.add(start);
      class_243 pos = start;
      class_3965 blockHit = null;
      class_1297 entityHit = null;
      int ticks = 0;

      while (ticks < 300) {
         ticks++;
         class_243 var15 = velocity.method_1031(0.0, -0.03, 0.0);
         velocity = var15.method_1021(isInWater(level, pos) ? 0.8F : 0.99F);
         class_243 next = pos.method_1019(velocity);
         class_239 blockResult = level.method_17742(new class_3959(pos, next, class_3960.field_17558, class_242.field_1348, ignore));
         class_243 end = blockResult.method_17783() != class_240.field_1333 ? blockResult.method_17784() : next;
         class_1297 entity = firstEntityOnSegment(level, pos, end, ignore, alsoIgnore);
         if (entity != null) {
            class_243 at = entity.method_5829().method_1014(0.3).method_992(pos, end).orElse(end);
            points.add(at);
            entityHit = entity;
            break;
         }

         if (blockResult instanceof class_3965 hit && hit.method_17783() != class_240.field_1333) {
            points.add(end);
            blockHit = hit;
            break;
         }

         pos = next;
         points.add(pos);
         if (pos.field_1351 < level.method_31607() - 64) {
            break;
         }
      }

      return new PearlTrajectorySimulator.SimulationResult(List.copyOf(points), points.getLast(), blockHit, entityHit, ticks);
   }

   private static class_1297 firstEntityOnSegment(class_1937 level, class_243 from, class_243 to, class_1297 ignore, class_1297 alsoIgnore) {
      class_238 sweep = new class_238(from, to).method_1014(1.0);
      class_1297 closest = null;
      double closestSq = Double.MAX_VALUE;

      for (class_1297 entity : level.method_8333(ignore, sweep, PearlTrajectorySimulator::canHit)) {
         if (entity != alsoIgnore) {
            Optional<class_243> hit = entity.method_5829().method_1014(0.3).method_992(from, to);
            if (!hit.isEmpty()) {
               double distanceSq = from.method_1025(hit.get());
               if (distanceSq < closestSq) {
                  closestSq = distanceSq;
                  closest = entity;
               }
            }
         }
      }

      return closest;
   }

   private static boolean canHit(class_1297 entity) {
      return class_1301.field_6155.test(entity) && entity.method_5863() && !(entity instanceof class_746);
   }

   private static boolean isInWater(class_1937 level, class_243 pos) {
      return level.method_8316(class_2338.method_49638(pos)).method_15767(class_3486.field_15517);
   }

   public static boolean isHoldingPearl(class_746 player) {
      return player.method_6047().method_31574(class_1802.field_8634) || player.method_6079().method_31574(class_1802.field_8634);
   }

   @Environment(EnvType.CLIENT)
   public record SimulationResult(List<class_243> points, class_243 landing, class_3965 blockHit, class_1297 entityHit, int ticks) {
      public boolean landed() {
         return this.blockHit != null || this.entityHit != null;
      }
   }
}
