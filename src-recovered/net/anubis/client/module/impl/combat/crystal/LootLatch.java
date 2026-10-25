package net.anubis.client.module.impl.combat.crystal;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.anubis.client.event.impl.PacketAppliedEvent;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_156;
import net.minecraft.class_1657;
import net.minecraft.class_2663;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_638;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class LootLatch {
   private static final double RANGE_SQR = 100.0;
   private static final int GRACE_TICKS = 10;
   private static final long ACTIVE_MS = 250L;
   private static final Int2IntOpenHashMap DEAD = new Int2IntOpenHashMap();
   private static class_746 self;
   private static int selfTick = -1;
   private static int clock;
   private static boolean reported;
   private static boolean latched;
   private static boolean killed;
   private static long updatedAt;

   private LootLatch() {
   }

   public static void update(class_310 mc, class_746 player) {
      if (player != self || player.field_6012 != selfTick) {
         self = player;
         selfTick = player.field_6012;
         class_304 use = mc.field_1690.field_1904;
         evaluate(mc.field_1687, player, Crystals.isPhysicallyDown(mc, use), MeleeAttacks.clickPending(use));
      }
   }

   public static boolean latched() {
      return latched;
   }

   public static boolean killedNow() {
      return killed;
   }

   public static boolean active() {
      return latched && class_156.method_658() - updatedAt < 250L;
   }

   private static void evaluate(class_638 level, class_746 player, boolean down, boolean newPress) {
      int now = ++clock;
      updatedAt = class_156.method_658();
      DEAD.int2IntEntrySet()
         .removeIf(entry -> now - entry.getIntValue() > 10 && !(level.method_8469(entry.getIntKey()) instanceof class_1657 otherx && otherx.method_29504()));
      boolean fresh = reported;
      reported = false;

      for (class_1657 other : level.method_18456()) {
         if (other != player && other.method_29504() && !DEAD.containsKey(other.method_5628())) {
            DEAD.put(other.method_5628(), now);
            if (other.method_5858(player) <= 100.0) {
               fresh = true;
            }
         }
      }

      killed = fresh;
      if (fresh && down) {
         latched = true;
      } else if (!down || latched && newPress) {
         latched = false;
      }
   }

   private static void onPacket(PacketAppliedEvent event) {
      if (event.packet instanceof class_2663 packet && packet.method_11470() == 3) {
         class_310 mc = class_310.method_1551();
         if (mc.field_1687 != null && mc.field_1724 != null) {
            if (packet.method_11469(mc.field_1687) instanceof class_1657 other && other != mc.field_1724 && !DEAD.containsKey(other.method_5628())) {
               DEAD.put(other.method_5628(), clock);
               if (other.method_5858(mc.field_1724) <= 100.0) {
                  reported = true;
               }
            }
         }
      }
   }

   static {
      PacketAppliedEvent.TYPE.listen(LootLatch.class, LootLatch::onPacket);
   }
}
