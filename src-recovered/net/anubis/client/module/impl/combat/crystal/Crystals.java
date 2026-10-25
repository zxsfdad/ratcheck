package net.anubis.client.module.impl.combat.crystal;

import com.example.client.mixin.AccessorLocalPlayer;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.anubis.client.input.AnubisKeyPolling;
import net.anubis.client.input.ModuleKeybinds;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10192;
import net.minecraft.class_1268;
import net.minecraft.class_1291;
import net.minecraft.class_1293;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1320;
import net.minecraft.class_1322;
import net.minecraft.class_1324;
import net.minecraft.class_1511;
import net.minecraft.class_1661;
import net.minecraft.class_1750;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1887;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2343;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3481;
import net.minecraft.class_3489;
import net.minecraft.class_3532;
import net.minecraft.class_3675;
import net.minecraft.class_3726;
import net.minecraft.class_3965;
import net.minecraft.class_5134;
import net.minecraft.class_634;
import net.minecraft.class_638;
import net.minecraft.class_640;
import net.minecraft.class_6880;
import net.minecraft.class_746;
import net.minecraft.class_9334;
import net.minecraft.class_9698;
import net.minecraft.class_9701;
import net.minecraft.class_9711;
import net.minecraft.class_9723;
import net.minecraft.class_1269.class_9860;
import net.minecraft.class_1269.class_9861;
import net.minecraft.class_3675.class_306;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class Crystals {
   public static final int READY = -2;
   public static final int NO_ROUTE = -1;
   private static final int MIN_CONFIRM_TICKS = 4;
   private static final int MAX_CONFIRM_TICKS = 20;
   private static final float RTT_SMOOTHING = 0.25F;
   private static final double RESERVE_RANGE = 12.0;
   private static final Set<class_2248> USABLE_BLOCKS = Set.of(
      class_2246.field_23152,
      class_2246.field_10179,
      class_2246.field_10363,
      class_2246.field_10450,
      class_2246.field_10091,
      class_2246.field_10183,
      class_2246.field_17563,
      class_2246.field_10081
   );
   private static final Long2IntOpenHashMap PLACED_AT = new Long2IntOpenHashMap();
   private static float measuredRtt;

   private Crystals() {
   }

   public static boolean isBase(class_2680 state) {
      return state.method_27852(class_2246.field_10540) || state.method_27852(class_2246.field_9987);
   }

   public static class_243 explosionAt(class_2338 base) {
      return class_243.method_24955(base.method_10084());
   }

   public static class_238 boxOn(class_2338 base) {
      return class_1299.field_6110.method_18386().method_30757(explosionAt(base));
   }

   public static boolean hasCrystal(class_1937 level, class_2338 base, class_1297 ignore) {
      class_2338 above = base.method_10084();

      for (class_1511 crystal : level.method_18467(class_1511.class, new class_238(above))) {
         if (crystal != ignore && crystal.method_24515().equals(above)) {
            return true;
         }
      }

      return false;
   }

   public static boolean canHost(class_1937 level, class_2338 base, class_1297 ignore) {
      class_2338 above = base.method_10084();
      if (!level.method_22347(above)) {
         return false;
      }

      class_238 space = new class_238(
         above.method_10263(), above.method_10264(), above.method_10260(), above.method_10263() + 1.0, above.method_10264() + 2.0, above.method_10260() + 1.0
      );

      for (class_1297 entity : level.method_8335(ignore, space)) {
         if (entity.method_5805()) {
            return false;
         }
      }

      return true;
   }

   public static class_2338 obsidianTarget(class_638 level, class_746 player, class_3965 hit, class_2680 clicked) {
      class_2338 target = placedObsidian(level, player, hit, clicked);
      return target != null && canHost(level, target, null) ? target : null;
   }

   public static class_2338 placedObsidian(class_638 level, class_746 player, class_3965 hit, class_2680 clicked) {
      if (!player.method_21823() && reactsToUse(level, hit.method_17777(), clicked)) {
         return null;
      } else {
         class_1750 context = new class_1750(player, class_1268.field_5808, new class_1799(class_1802.field_8281), hit);
         if (!context.method_7716()) {
            return null;
         } else {
            class_2338 target = context.method_8037();
            if (level.method_31606(target) || !level.method_8621().method_11952(target)) {
               return null;
            } else {
               return !level.method_8628(class_2246.field_10540.method_9564(), target, class_3726.method_67715(player)) ? null : target;
            }
         }
      }
   }

   private static boolean reactsToUse(class_1937 level, class_2338 pos, class_2680 state) {
      class_2248 block = state.method_26204();
      if (block instanceof class_2343 || USABLE_BLOCKS.contains(block)) {
         return true;
      } else {
         return !state.method_26164(class_3481.field_15495)
               && !state.method_26164(class_3481.field_15487)
               && !state.method_26164(class_3481.field_25147)
               && !state.method_26164(class_3481.field_15493)
               && !state.method_26164(class_3481.field_26985)
               && !state.method_26164(class_3481.field_26984)
               && !state.method_26164(class_3481.field_15470)
            ? state.method_26196(level, pos) != null
            : true;
      }
   }

   public static int route(class_746 player, class_1792 item, class_2680 clicked, int preferredSlot) {
      class_1799 main = player.method_6047();
      if (main.method_31574(item)) {
         return -2;
      }

      boolean inOffhand = player.method_6079().method_31574(item);
      if (inOffhand && letsClickThrough(main, clicked)) {
         return -2;
      }

      class_1661 inventory = player.method_31548();
      int slot = hotbarSlotOf(inventory, item);
      if (slot != -1) {
         return slot;
      }

      if (!inOffhand) {
         return -1;
      }

      if (class_1661.method_7380(preferredSlot) && letsClickThrough(inventory.method_5438(preferredSlot), clicked)) {
         return preferredSlot;
      }

      for (int var8 = 0; var8 < class_1661.method_7368(); var8++) {
         if (letsClickThrough(inventory.method_5438(var8), clicked)) {
            return var8;
         }
      }

      return -1;
   }

   public static boolean letsClickThrough(class_1799 stack, class_2680 clicked) {
      if (stack.method_7960()) {
         return true;
      } else if (!stack.method_57826(class_9334.field_53964) && !stack.method_57826(class_9334.field_56396) && !stack.method_57826(class_9334.field_63632)) {
         class_10192 equippable = (class_10192)stack.method_58694(class_9334.field_54196);
         return equippable != null && equippable.comp_3213()
            ? false
            : stack.method_31573(class_3489.field_42611)
               || stack.method_31574(class_1802.field_49814)
               || stack.method_31574(class_1802.field_8288)
               || stack.method_31573(class_3489.field_42612) && isBase(clicked);
      } else {
         return false;
      }
   }

   public static boolean isCombatItem(class_1799 stack) {
      return stack.method_7960()
         || stack.method_31573(class_3489.field_42611)
         || stack.method_31573(class_3489.field_42612)
         || stack.method_31574(class_1802.field_49814)
         || stack.method_31574(class_1802.field_8288);
   }

   public static int hotbarSlotOf(class_1661 inventory, class_1792 item) {
      for (int slot = 0; slot < class_1661.method_7368(); slot++) {
         if (inventory.method_5438(slot).method_31574(item)) {
            return slot;
         }
      }

      return -1;
   }

   public static boolean strike(class_310 mc, class_746 player, class_1297 target) {
      if (player != null && mc.field_1761 != null && target != null && !target.method_31481() && !player.method_7325()) {
         if (!MeleeAttacks.actedThisTick(player)
            && MeleeAttacks.handsFree(mc, player)
            && player.method_5624() == ((AccessorLocalPlayer)player).anubis$wasSprinting()) {
            mc.field_1761.method_2918(player, target);
            player.method_6104(class_1268.field_5808);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean rightClick(class_310 mc, class_746 player, class_3965 hit) {
      if (!MeleeAttacks.actedThisTick(player) && MeleeAttacks.handsFree(mc, player)) {
         class_1268 hand = letsClickThrough(player.method_6047(), mc.field_1687.method_8320(hit.method_17777()))
            ? class_1268.field_5810
            : class_1268.field_5808;
         class_1799 stack = player.method_5998(hand);
         if (!stack.method_45435(mc.field_1687.method_45162())) {
            return false;
         }

         int countBefore = stack.method_7947();
         if (mc.field_1761.method_2896(player, hand, hit) instanceof class_9860 success && success.comp_2909() == class_9861.field_52427) {
            player.method_6104(hand);
            if (!stack.method_7960() && (stack.method_7947() != countBefore || player.method_56992())) {
               mc.field_1773.field_4012.method_3215(hand);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static int weaponRoute(class_746 player) {
      class_1799 main = player.method_6047();
      if (!swings(player, main) || !enchantsAddDamage(main) && !dealsDamage(player, main)) {
         class_1661 inventory = player.method_31548();
         int fallback = -1;

         for (int slot = 0; slot < class_1661.method_7368(); slot++) {
            class_1799 stack = inventory.method_5438(slot);
            if (swings(player, stack)) {
               if (enchantsAddDamage(stack)) {
                  return slot;
               }

               if (fallback == -1 && dealsDamage(player, stack)) {
                  fallback = slot;
               }
            }
         }

         return fallback;
      } else {
         return -2;
      }
   }

   private static boolean swings(class_746 player, class_1799 stack) {
      return !stack.method_57826(class_9334.field_63631) && !player.method_75202(stack, 0);
   }

   public static boolean enchantsAddDamage(class_1799 weapon) {
      float bonus = 0.0F;

      for (Entry<class_6880<class_1887>> entry : weapon.method_58657().method_57539()) {
         class_1887 enchantment = (class_1887)((class_6880)entry.getKey()).comp_349();

         for (class_9698<class_9723> effect : enchantment.method_60034(class_9701.field_51661)) {
            if (effect.comp_2681().isPresent() || !(effect.comp_2680() instanceof class_9711 add)) {
               return false;
            }

            bonus += add.comp_2704().method_60188(entry.getIntValue());
         }
      }

      return bonus > 0.0F;
   }

   public static boolean dealsDamage(class_1309 player, class_1799 mainHand) {
      class_1324 damage = new class_1324(class_5134.field_23721, changed -> {});
      damage.method_6192(player.method_45326(class_5134.field_23721));

      for (class_1293 effect : player.method_6026()) {
         ((class_1291)effect.method_5579().comp_349()).method_55650(effect.method_5578(), (attribute, modifier) -> include(damage, attribute, modifier));
      }

      for (class_1304 slot : class_1304.field_54086) {
         class_1799 stack = slot == class_1304.field_6173 ? mainHand : player.method_6118(slot);
         if (!stack.method_7960() && !stack.method_61657()) {
            stack.method_57354(slot, (attribute, modifier) -> include(damage, attribute, modifier));
         }
      }

      return (float)damage.method_6194() > 0.0F;
   }

   private static void include(class_1324 damage, class_6880<class_1320> attribute, class_1322 modifier) {
      if (class_5134.field_23721.equals(attribute)) {
         damage.method_6200(modifier.comp_2447());
         damage.method_26835(modifier);
      }
   }

   public static int confirmWindow(class_310 mc, class_746 player) {
      return class_3532.method_15340(rttTicks(mc, player) + 2, 4, 20);
   }

   public static int rttTicks(class_310 mc, class_746 player) {
      return measuredRtt > 0.0F ? Math.round(measuredRtt) : latencyMs(mc, player) / 50 + 1;
   }

   public static void notePlace(class_746 player, class_2338 base) {
      ObjectIterator<it.unimi.dsi.fastutil.longs.Long2IntMap.Entry> iterator = PLACED_AT.long2IntEntrySet().iterator();

      while (iterator.hasNext()) {
         int age = player.field_6012 - ((it.unimi.dsi.fastutil.longs.Long2IntMap.Entry)iterator.next()).getIntValue();
         if (age < 0 || age > 20) {
            iterator.remove();
         }
      }

      PLACED_AT.put(base.method_10063(), player.field_6012);
   }

   public static void noteSpawn(class_746 player, class_2338 base) {
      long key = base.method_10063();
      if (PLACED_AT.containsKey(key)) {
         int ticks = player.field_6012 - PLACED_AT.remove(key);
         if (ticks > 0 && ticks <= 20) {
            measuredRtt = measuredRtt > 0.0F ? measuredRtt + (ticks - measuredRtt) * 0.25F : ticks;
         }
      }
   }

   public static boolean placing(class_746 player, class_2338 base, int window) {
      long key = base.method_10063();
      return PLACED_AT.containsKey(key) && sent(player, PLACED_AT.get(key), window);
   }

   public static boolean anyPlacing(class_746 player, int window) {
      IntIterator var2 = PLACED_AT.values().iterator();

      while (var2.hasNext()) {
         int tick = (Integer)var2.next();
         if (sent(player, tick, window)) {
            return true;
         }
      }

      return false;
   }

   public static List<class_238> reservedBoxes(class_746 player, int window) {
      List<class_238> boxes = new ArrayList<>();

      for (class_1511 crystal : player.method_73183().method_18467(class_1511.class, player.method_5829().method_1014(12.0))) {
         if (crystal.method_5805()) {
            boxes.add(crystal.method_5829());
         }
      }

      ObjectIterator var5 = PLACED_AT.long2IntEntrySet().iterator();

      while (var5.hasNext()) {
         it.unimi.dsi.fastutil.longs.Long2IntMap.Entry entry = (it.unimi.dsi.fastutil.longs.Long2IntMap.Entry)var5.next();
         if (sent(player, entry.getIntValue(), window)) {
            boxes.add(boxOn(class_2338.method_10092(entry.getLongKey())));
         }
      }

      return boxes;
   }

   private static boolean sent(class_746 player, int tick, int window) {
      int age = player.field_6012 - tick;
      return age >= 0 && age < window;
   }

   public static void forgetRtt() {
      PLACED_AT.clear();
      measuredRtt = 0.0F;
   }

   private static int latencyMs(class_310 mc, class_746 player) {
      class_634 connection = mc.method_1562();
      if (connection == null) {
         return 0;
      }

      class_640 info = connection.method_2871(player.method_5667());
      return info == null ? 0 : Math.max(info.method_2959(), 0);
   }

   public static boolean isKeyDown(class_310 mc, int key) {
      if (!ModuleKeybinds.isBindable(key)) {
         return false;
      } else {
         return key >= 1000 ? GLFW.glfwGetMouseButton(mc.method_22683().method_4490(), key - 1000) == 1 : AnubisKeyPolling.isKeyDown(mc.method_22683(), key);
      }
   }

   public static boolean isPhysicallyDown(class_310 mc, class_304 mapping) {
      class_306 key;
      try {
         key = class_3675.method_15981(mapping.method_1428());
      } catch (IllegalArgumentException unknown) {
         return false;
      }

      int value = key.method_1444();
      if (value < 0) {
         return false;
      }

      return switch (key.method_1442()) {
         case field_1672 -> GLFW.glfwGetMouseButton(mc.method_22683().method_4490(), value) == 1;
         case field_1668 -> AnubisKeyPolling.isKeyDown(mc.method_22683(), value);
         default -> false;
      };
   }
}
