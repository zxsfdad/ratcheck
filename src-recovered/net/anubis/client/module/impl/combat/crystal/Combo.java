package net.anubis.client.module.impl.combat.crystal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10584;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1802;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_742;
import net.minecraft.class_746;
import net.minecraft.class_2350.class_2353;

@Environment(EnvType.CLIENT)
public final class Combo {
   public static final double TARGET_RANGE = 10.0;
   public static final int MAX_TARGETS = 3;
   public static final float LETHAL_RANK = 1000.0F;
   public static final float POP_RANK = 500.0F;
   public static final float SELF_WEIGHT = 0.5F;
   public static final float ANCHOR_EDGE = 1.0F;
   private static final float BLAST_PROOF = 600.0F;
   private static final double FOCUS_BONUS = 2.0;
   private static final double WEAK_BONUS = 2.0;
   private static final float WEAK_HEALTH = 10.0F;
   private static final double MAX_STEP_SQR = 1.0;
   private static final double GRAVITY = 0.08;
   private static final double AIR_DRAG = 0.98;
   private static final Combo.Kind[] SETUP = new Combo.Kind[]{Combo.Kind.CHARGE, Combo.Kind.CRYSTAL, Combo.Kind.OBSIDIAN, Combo.Kind.ANCHOR};
   private static final Map<class_1309, Combo.Track> TRACKS = new WeakHashMap<>();
   private static class_1309 focus;

   private Combo() {
   }

   public static List<class_1309> enemies(class_310 mc, class_746 player) {
      List<class_1309> enemies = new ArrayList<>();

      for (class_742 other : mc.field_1687.method_18456()) {
         if (MeleeAttacks.isEnemy(player, other) && player.method_5858(other) < 100.0) {
            enemies.add(other);
         }
      }

      enemies.sort(
         Comparator.comparingDouble(enemy -> player.method_5739(enemy) - (enemy == focus ? 2.0 : 0.0) - (CrystalDamage.health(enemy) <= 10.0F ? 2.0 : 0.0))
      );
      focus = enemies.isEmpty() ? null : enemies.get(0);
      return enemies.size() > 3 ? enemies.subList(0, 3) : enemies;
   }

   public static List<class_1309> friends(class_310 mc, class_746 player) {
      List<class_1309> friends = new ArrayList<>();

      for (class_742 other : mc.field_1687.method_18456()) {
         if (other != player && other.method_5805() && MeleeAttacks.isFriend(other) && player.method_5858(other) < 100.0) {
            friends.add(other);
         }
      }

      return friends;
   }

   public static class_243 lead(class_1309 entity, int ticks) {
      class_10584 handler = entity.method_66233();
      class_243 server = handler == null ? entity.method_73189() : handler.method_66265();
      class_243 lag = server.method_1020(entity.method_73189());
      int ahead = ticks;
      class_243 velocity;
      if (entity instanceof class_746) {
         velocity = entity.method_73189().method_1023(entity.field_6014, entity.field_6036, entity.field_5969);
      } else {
         Combo.Track track = TRACKS.computeIfAbsent(entity, ignored -> new Combo.Track());
         track.note(entity, server);
         velocity = track.velocity(entity);
         ahead += Math.min(track.age(entity), entity.method_5864().method_18388());
      }

      return ticks > 0 && !(velocity.method_37268() > 1.0) ? lag.method_1019(travel(entity, server, velocity, ahead)) : lag;
   }

   private static class_243 travel(class_1309 entity, class_243 from, class_243 velocity, int ticks) {
      class_238 box = entity.method_5829().method_997(from.method_1020(entity.method_73189()));
      boolean floating = entity.method_6128() || entity.method_52535() || entity.method_6101() || entity.method_5740() || entity.method_5765();
      double x = velocity.field_1352;
      double y = velocity.field_1351;
      double z = velocity.field_1350;
      class_243 moved = class_243.field_1353;

      for (int tick = 0; tick < ticks; tick++) {
         if (!floating) {
            y = (y - 0.08) * 0.98;
         }

         class_243 step = class_1297.method_20736(entity, new class_243(x, y, z), box, entity.method_73183(), List.of());
         if (step.field_1352 != x) {
            x = 0.0;
         }

         if (step.field_1351 != y) {
            y = 0.0;
         }

         if (step.field_1350 != z) {
            z = 0.0;
         }

         box = box.method_997(step);
         moved = moved.method_1019(step);
      }

      return moved;
   }

   public static boolean inHole(class_1309 target) {
      class_2338 feet = target.method_24515();

      for (class_2350 side : class_2353.field_11062) {
         class_2680 state = target.method_73183().method_8320(feet.method_10093(side));
         if (state.method_26204().method_9520() < 600.0F) {
            return false;
         }
      }

      return true;
   }

   public static boolean kills(float effective, class_1309 target) {
      return effective >= CrystalDamage.health(target);
   }

   public static float value(float effective, class_1309 target) {
      float health = CrystalDamage.health(target);
      float rank = effective < health ? 0.0F : (target.method_24518(class_1802.field_8288) ? 500.0F : 1000.0F);
      return Math.min(effective, health) + rank;
   }

   public static Combo.Shot pick(List<Combo.Shot> shots) {
      Combo.Shot crystal = best(shots, Combo.Kind.BREAK);
      Combo.Shot anchor = best(shots, Combo.Kind.DETONATE);
      if (crystal == null && anchor == null) {
         for (Combo.Kind kind : SETUP) {
            Combo.Shot shot = best(shots, kind);
            if (shot != null) {
               return shot;
            }
         }

         return null;
      } else {
         boolean anchorWins = crystal == null || anchor != null && (anchor.value() > crystal.value() + 1.0F || anchor.kills() && !crystal.kills());
         return anchorWins ? anchor : crystal;
      }
   }

   private static Combo.Shot best(List<Combo.Shot> shots, Combo.Kind kind) {
      Combo.Shot best = null;

      for (Combo.Shot shot : shots) {
         if (shot.kind() == kind && (best == null || shot.value() > best.value() || shot.value() == best.value() && shot.readyIn() < best.readyIn())) {
            best = shot;
         }
      }

      return best;
   }

   @Environment(EnvType.CLIENT)
   public enum Kind {
      BREAK,
      CRYSTAL,
      OBSIDIAN,
      ANCHOR,
      CHARGE,
      DETONATE;
   }

   @Environment(EnvType.CLIENT)
   public record Shot(Combo.Kind kind, class_243 aim, int route, float value, boolean kills, int readyIn, Object plan) {
   }

   @Environment(EnvType.CLIENT)
   private static final class Track {
      private class_243 last;
      private int lastAt;
      private int sampledAt;
      private class_243 velocity = class_243.field_1353;

      void note(class_1309 entity, class_243 server) {
         int now = entity.field_6012;
         if (now < this.sampledAt) {
            this.last = null;
         }

         boolean watched = now - this.sampledAt <= 1;
         this.sampledAt = now;
         if (!server.equals(this.last)) {
            int interval = entity.method_5864().method_18388();
            int span = watched ? interval : Math.max(interval, now - this.lastAt);
            this.velocity = this.last == null ? class_243.field_1353 : server.method_1020(this.last).method_1021(1.0 / span);
            this.last = server;
            this.lastAt = now;
         }
      }

      int age(class_1309 entity) {
         return entity.field_6012 - this.lastAt;
      }

      class_243 velocity(class_1309 entity) {
         return this.age(entity) > entity.method_5864().method_18388() + 1 ? class_243.field_1353 : this.velocity;
      }
   }
}
