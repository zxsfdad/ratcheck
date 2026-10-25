package net.anubis.client.module.impl.combat.crystal;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1937;
import net.minecraft.class_243;
import net.minecraft.class_2596;
import net.minecraft.class_2716;
import net.minecraft.class_6880;
import net.minecraft.class_746;
import net.minecraft.class_8042;
import net.minecraft.class_8110;
import net.minecraft.class_8111;
import net.minecraft.class_8143;

@Environment(EnvType.CLIENT)
public final class HurtClock {
   private static final int NEVER = -1000;
   private static final int WINDOW = 10;
   private static final int MAX_WAIT = 20;
   private static final int MAX_MARGIN = 2;
   private static final int RELAX_STREAK = 8;
   private static final int REMOVE_GRACE = 3;
   private static final double ANCHOR_MATCH_SQR = 0.25;
   private static final Int2ObjectOpenHashMap<HurtClock.Mark> MARKS = new Int2ObjectOpenHashMap();
   private static class_746 lastPlayer;
   private static class_1937 lastLevel;
   private static class_2596<?> lastPacket;
   private static int now;
   private static int margin;
   private static int streak;

   private HurtClock() {
   }

   public static void note(class_746 player, class_2596<?> packet) {
      if (packet != lastPacket) {
         lastPacket = packet;
         if (packet instanceof class_8042 bundle) {
            for (class_2596<?> inner : bundle.method_48324()) {
               handle(player, inner);
            }
         } else {
            handle(player, packet);
         }
      }
   }

   public static void tick(class_746 player) {
      now = player.field_6012;
      if (player != lastPlayer || player.method_73183() != lastLevel) {
         MARKS.clear();
         margin = 0;
         streak = 0;
         lastPlayer = player;
         lastLevel = player.method_73183();
      }

      ObjectIterator<Entry<HurtClock.Mark>> iterator = MARKS.int2ObjectEntrySet().iterator();

      while (iterator.hasNext()) {
         Entry<HurtClock.Mark> entry = (Entry<HurtClock.Mark>)iterator.next();
         if (lastLevel.method_8469(entry.getIntKey()) instanceof class_1309 target && target.method_5805()) {
            HurtClock.Mark mark = (HurtClock.Mark)entry.getValue();
            if (mark.removedAt != -1000 && !mark.confirmed && now - mark.removedAt > 3) {
               margin = Math.min(2, margin + 1);
               streak = 0;
               mark.shotTick = -1000;
               mark.removedAt = -1000;
            }
         } else {
            iterator.remove();
         }
      }
   }

   public static boolean freshAt(class_1309 target, int ahead, int lead) {
      if (target.field_6008 - ahead - lead > 10) {
         return false;
      }

      HurtClock.Mark mark = (HurtClock.Mark)MARKS.get(target.method_5628());
      return mark == null || mark.shotTick == -1000 || now + ahead - mark.shotTick >= 10 + margin;
   }

   public static int opensIn(class_1309 target, int lead) {
      for (int ahead = 0; ahead < 20; ahead++) {
         if (freshAt(target, ahead, lead)) {
            return ahead;
         }
      }

      return 20;
   }

   public static float effective(CrystalDamage.Victim victim, float raw, int ahead, int lead) {
      class_1309 target = victim.entity();
      if (freshAt(target, ahead, lead)) {
         return victim.reduce(raw);
      }

      HurtClock.Mark mark = (HurtClock.Mark)MARKS.get(target.method_5628());
      float last = mark == null ? Float.MAX_VALUE : mark.lastRaw;
      return raw > last ? victim.reduce(raw - last) : 0.0F;
   }

   public static void shot(class_746 player, class_1309 target, float raw, Object key, int lead) {
      HurtClock.Mark mark = (HurtClock.Mark)MARKS.computeIfAbsent(target.method_5628(), id -> new HurtClock.Mark());
      if (freshAt(target, 0, lead)) {
         mark.shotTick = player.field_6012;
         mark.shotRaw = raw;
         mark.lastRaw = raw;
         mark.shotKey = key;
         mark.confirmed = false;
         mark.removedAt = -1000;
      } else {
         mark.lastRaw = Math.max(mark.lastRaw, raw);
      }
   }

   private static void handle(class_746 player, class_2596<?> packet) {
      if (packet instanceof class_8143 damage) {
         damaged(player, damage);
      } else if (packet instanceof class_2716 remove) {
         removed(remove.method_36548());
      }
   }

   private static void damaged(class_746 player, class_8143 packet) {
      if (player.method_73183().method_8469(packet.comp_1267()) instanceof class_1657 target && MeleeAttacks.isEnemy(player, target)) {
         HurtClock.Mark mark = (HurtClock.Mark)MARKS.computeIfAbsent(target.method_5628(), id -> new HurtClock.Mark());
         if (mark.shotTick != -1000 && !mark.confirmed && ours(player, mark, packet)) {
            mark.lastRaw = mark.shotRaw;
            mark.confirmed = true;
            if (mark.shotKey instanceof Integer && ++streak >= 8) {
               margin = Math.max(0, margin - 1);
               streak = 0;
            }
         } else {
            class_6880<class_8110> type = packet.comp_1268();
            mark.shotTick = -1000;
            mark.removedAt = -1000;
            mark.lastRaw = !type.method_40225(class_8111.field_42320) && !type.method_40225(class_8111.field_42360) ? Float.MAX_VALUE : 0.0F;
         }
      }
   }

   private static boolean ours(class_746 player, HurtClock.Mark mark, class_8143 packet) {
      class_6880<class_8110> type = packet.comp_1268();
      return type.method_40225(class_8111.field_42332)
         ? packet.comp_1269() == player.method_5628() && mark.shotKey instanceof Integer crystal && packet.comp_1270() == crystal
         : type.method_40225(class_8111.field_42334)
            && mark.shotKey instanceof class_243 center
            && packet.comp_1271().filter(at -> at.method_1025(center) < 0.25).isPresent();
   }

   private static void removed(IntList ids) {
      ObjectIterator var1 = MARKS.values().iterator();

      while (var1.hasNext()) {
         HurtClock.Mark mark = (HurtClock.Mark)var1.next();
         if (mark.shotTick != -1000 && !mark.confirmed && mark.shotKey instanceof Integer crystal && ids.contains(crystal)) {
            mark.removedAt = now;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Mark {
      float lastRaw = Float.MAX_VALUE;
      int shotTick = -1000;
      float shotRaw;
      Object shotKey;
      boolean confirmed;
      int removedAt = -1000;
   }
}
