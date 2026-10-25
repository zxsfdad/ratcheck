package net.anubis.client.module.impl.combat.anchor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.anubis.client.module.impl.combat.crystal.Crystals;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.anubis.client.util.pvp.Rotation;
import net.anubis.client.util.pvp.RotationUtils;
import net.anubis.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1268;
import net.minecraft.class_1661;
import net.minecraft.class_1747;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2343;
import net.minecraft.class_2346;
import net.minecraft.class_2350;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_265;
import net.minecraft.class_2680;
import net.minecraft.class_2682;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_638;
import net.minecraft.class_746;
import net.minecraft.class_1269.class_9860;
import net.minecraft.class_1269.class_9861;
import net.minecraft.class_2350.class_2351;
import net.minecraft.class_2350.class_2352;
import net.minecraft.class_2350.class_2353;
import net.minecraft.class_239.class_240;
import net.minecraft.class_3959.class_242;
import net.minecraft.class_3959.class_3960;

@Environment(EnvType.CLIENT)
public final class AnchorGuard {
   public static final float BLAST_POWER = 5.0F;
   private static final double MIN_SHADE = 0.15;
   private static final double FACE_INSET = 0.3;
   private static final double AIM_INSET = 0.1;

   private AnchorGuard() {
   }

   public static AnchorGuard.Spot find(class_638 level, class_746 player, class_2338 anchor, class_243 sightline) {
      class_243 blast = class_243.method_24953(anchor);
      class_238 body = player.method_5829();
      List<class_243> points = bodyPoints(body);
      List<class_243> open = new ArrayList<>(points.size());

      for (class_243 point : points) {
         class_3965 hit = level.method_17742(new class_3959(point, blast, class_3960.field_17558, class_242.field_1348, player));
         if (hit.method_17783() == class_240.field_1333 || hit.method_17777().equals(anchor)) {
            open.add(point);
         }
      }

      class_2338 feet = player.method_24515();
      if (open.size() < Math.ceil(points.size() * 0.15)) {
         return new AnchorGuard.Spot(feet, null, null, null);
      }

      int needed = (int)Math.ceil(open.size() * 0.15);
      class_243 eye = player.method_33571();
      Set<class_2338> around = new LinkedHashSet<>();

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            if (dx != 0 || dz != 0) {
               around.add(feet.method_10069(dx, 0, dz));
            }
         }
      }

      for (class_2350 side : class_2353.field_11062) {
         around.add(anchor.method_10093(side));
      }

      List<AnchorGuard.Cell> cells = new ArrayList<>(around.size());

      for (class_2338 pos : around) {
         class_238 box = new class_238(pos);
         if (!pos.equals(anchor) && !box.method_994(body) && !box.method_992(eye, sightline).isPresent()) {
            int shade = shade(box, blast, open);
            if (shade >= needed) {
               cells.add(new AnchorGuard.Cell(pos, shade, box.method_1005().method_1025(blast)));
            }
         }
      }

      cells.sort(Comparator.comparingInt(AnchorGuard.Cell::shade).reversed().thenComparingDouble(AnchorGuard.Cell::distance));

      for (AnchorGuard.Cell cell : cells) {
         class_2680 state = level.method_8320(cell.pos());
         if (state.method_45474()) {
            AnchorGuard.Spot spot = state.method_26218(level, cell.pos()).method_1110()
               ? supportFor(level, player, eye, cell.pos())
               : direct(level, player, eye, cell.pos(), state);
            if (spot != null) {
               return spot;
            }
         }
      }

      return null;
   }

   public static class_3965 hitAlong(class_638 level, class_746 player, Rotation look, AnchorGuard.Spot spot) {
      class_3965 hit = trace(level, player, look);
      if (hit == null || !hit.method_17777().equals(spot.support())) {
         return null;
      }

      if (spot.face() != null && hit.method_17780() != spot.face()) {
         return null;
      }

      class_2338 placed = Crystals.placedObsidian(level, player, hit, level.method_8320(spot.support()));
      return spot.pos().equals(placed) ? hit : null;
   }

   public static class_3965 anchorHit(class_638 level, class_746 player, class_2338 anchor) {
      Rotation sent = ServerRotation.sent();
      class_3965 hit = sent == null ? null : trace(level, player, sent);
      return hit != null && hit.method_17777().equals(anchor) ? hit : null;
   }

   public static boolean isGuardBlock(class_1799 stack) {
      if (stack.method_7909() instanceof class_1747 item) {
         class_2248 block = item.method_7711();
         return !(block instanceof class_2343) && !(block instanceof class_2346) && block != class_2246.field_23152 && block != class_2246.field_10375
            ? block.method_9564().method_26234(class_2682.field_12294, class_2338.field_10980)
            : false;
      } else {
         return false;
      }
   }

   public static int detonatorSlot(class_1661 inventory) {
      int totem = Crystals.hotbarSlotOf(inventory, class_1802.field_8288);
      if (totem != -1) {
         return totem;
      }

      for (int slot = 0; slot < class_1661.method_7368(); slot++) {
         if (Crystals.isCombatItem(inventory.method_5438(slot))) {
            return slot;
         }
      }

      return -1;
   }

   public static int detonatorRoute(class_1661 inventory, class_1799 main) {
      if (Crystals.isCombatItem(main)) {
         return -2;
      }

      int slot = detonatorSlot(inventory);
      return slot == -1 ? -1 : slot;
   }

   public static class_243 anchorAim(class_638 level, class_746 player, class_2338 anchor) {
      class_243 eye = player.method_33571();
      class_238 box = new class_238(anchor).method_1011(0.1);
      class_243 point = new class_243(
         class_3532.method_15350(eye.field_1352, box.field_1323, box.field_1320),
         class_3532.method_15350(eye.field_1351, box.field_1322, box.field_1325),
         class_3532.method_15350(eye.field_1350, box.field_1321, box.field_1324)
      );
      class_3965 hit = trace(level, player, ServerRotation.next(player, RotationUtils.fromTo(eye, point)));
      return hit != null && hit.method_17777().equals(anchor) ? point : null;
   }

   public static void click(class_310 mc, class_746 player, class_3965 hit) {
      class_1799 held = player.method_6047();
      int countBefore = held.method_7947();
      if (mc.field_1761.method_2896(player, class_1268.field_5808, hit) instanceof class_9860 success && success.comp_2909() == class_9861.field_52427) {
         player.method_6104(class_1268.field_5808);
         if (!held.method_7960() && (held.method_7947() != countBefore || player.method_56992())) {
            mc.field_1773.field_4012.method_3215(class_1268.field_5808);
         }
      }

      MeleeAttacks.claimTick(mc);
   }

   private static AnchorGuard.Spot direct(class_638 level, class_746 player, class_243 eye, class_2338 pos, class_2680 state) {
      AnchorGuard.Spot spot = new AnchorGuard.Spot(pos, pos, null, state.method_26218(level, pos).method_1107().method_996(pos).method_1005());
      Rotation look = ServerRotation.next(player, RotationUtils.fromTo(eye, spot.aim()));
      return hitAlong(level, player, look, spot) != null ? spot : null;
   }

   public static AnchorGuard.Spot supportFor(class_638 level, class_746 player, class_243 eye, class_2338 cell) {
      AnchorGuard.Spot best = null;
      double bestDistance = Double.MAX_VALUE;

      for (class_2350 side : class_2350.values()) {
         class_2338 support = cell.method_10093(side);
         class_2680 state = level.method_8320(support);
         if (!state.method_45474()) {
            class_265 shape = state.method_26218(level, support);
            if (!shape.method_1110()) {
               class_2350 face = side.method_10153();
               AnchorGuard.Spot spot = null;

               for (class_243 aim : faceAims(shape.method_1107().method_996(support), face)) {
                  AnchorGuard.Spot candidate = new AnchorGuard.Spot(cell, support, face, aim);
                  Rotation look = ServerRotation.next(player, RotationUtils.fromTo(eye, aim));
                  if (hitAlong(level, player, look, candidate) != null) {
                     spot = candidate;
                     break;
                  }
               }

               if (spot != null) {
                  double distance = side == class_2350.field_11033 ? -1.0 : eye.method_1025(spot.aim());
                  if (distance < bestDistance) {
                     best = spot;
                     bestDistance = distance;
                  }
               }
            }
         }
      }

      return best;
   }

   private static List<class_243> faceAims(class_238 box, class_2350 face) {
      class_2351 axis = face.method_10166();
      double plane = face.method_10171() == class_2352.field_11056 ? box.method_990(axis) : box.method_1001(axis);
      class_243 center = box.method_1005().method_38499(axis, plane);
      List<class_243> aims = new ArrayList<>(5);
      aims.add(center);

      for (class_2350 toward : class_2350.values()) {
         class_2351 along = toward.method_10166();
         if (along != axis) {
            aims.add(center.method_43206(toward, (box.method_990(along) - box.method_1001(along)) * 0.3));
         }
      }

      return aims;
   }

   public static class_3965 trace(class_638 level, class_746 player, Rotation look) {
      class_243 eye = player.method_33571();
      class_243 reach = class_243.method_1030(look.pitch(), look.yaw()).method_1021(player.method_55754());
      class_3965 hit = level.method_17742(new class_3959(eye, eye.method_1019(reach), class_3960.field_17559, class_242.field_1348, player));
      return hit.method_17783() == class_240.field_1332 ? hit : null;
   }

   private static int shade(class_238 box, class_243 blast, List<class_243> points) {
      int blocked = 0;

      for (class_243 point : points) {
         if (box.method_992(point, blast).isPresent()) {
            blocked++;
         }
      }

      return blocked;
   }

   private static List<class_243> bodyPoints(class_238 body) {
      double stepX = 1.0 / ((body.field_1320 - body.field_1323) * 2.0 + 1.0);
      double stepY = 1.0 / ((body.field_1325 - body.field_1322) * 2.0 + 1.0);
      double stepZ = 1.0 / ((body.field_1324 - body.field_1321) * 2.0 + 1.0);
      double offsetX = (1.0 - Math.floor(1.0 / stepX) * stepX) / 2.0;
      double offsetZ = (1.0 - Math.floor(1.0 / stepZ) * stepZ) / 2.0;
      List<class_243> points = new ArrayList<>();

      for (double fx = 0.0; fx <= 1.0; fx += stepX) {
         for (double fy = 0.0; fy <= 1.0; fy += stepY) {
            for (double fz = 0.0; fz <= 1.0; fz += stepZ) {
               points.add(
                  new class_243(
                     class_3532.method_16436(fx, body.field_1323, body.field_1320) + offsetX,
                     class_3532.method_16436(fy, body.field_1322, body.field_1325),
                     class_3532.method_16436(fz, body.field_1321, body.field_1324) + offsetZ
                  )
               );
            }
         }
      }

      return points;
   }

   @Environment(EnvType.CLIENT)
   private record Cell(class_2338 pos, int shade, double distance) {
   }

   @Environment(EnvType.CLIENT)
   public record Spot(class_2338 pos, class_2338 support, class_2350 face, class_243 aim) {
      public boolean standing() {
         return this.support == null;
      }
   }
}
