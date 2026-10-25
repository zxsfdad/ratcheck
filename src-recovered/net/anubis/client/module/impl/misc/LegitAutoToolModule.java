package net.anubis.client.module.impl.misc;

import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.util.antivanish.AntiVanishText;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1292;
import net.minecraft.class_1293;
import net.minecraft.class_1294;
import net.minecraft.class_1304;
import net.minecraft.class_1661;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2338;
import net.minecraft.class_2561;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3486;
import net.minecraft.class_3489;
import net.minecraft.class_3965;
import net.minecraft.class_5134;
import net.minecraft.class_746;
import net.minecraft.class_9290;
import net.minecraft.class_9334;
import net.minecraft.class_239.class_240;

@Environment(EnvType.CLIENT)
public final class LegitAutoToolModule extends ClientModule {
   private static final int SWITCH_DELAY_TICKS = 2;
   private static final String SHARD_NAME = "shard pickaxe";
   private static final String SHARD_LORE = "breaks 9 blocks at once";
   private final BooleanSetting drill = this.addSetting("use-drill-for-instabreak", new BooleanSetting("Drill Instabreak", true));
   private final BooleanSetting delayed = this.addSetting("delay", new BooleanSetting("Delay", true));
   private class_2338 target;
   private int pending = -1;
   private int delay;
   private int origin = -1;
   private int ourSlot = -1;
   private int returnDelay;

   public LegitAutoToolModule() {
      super("legit-auto-tool", "Legit Auto Tool", "Auto tool that only switches hotbar slots like a player.", ModuleCategory.MISC);
   }

   @Override
   protected void onEnable() {
      this.reset();
   }

   @Override
   protected void onDisable() {
      if (this.origin != -1 && class_310.method_1551().field_1724 != null) {
         this.select(this.origin);
      }

      this.reset();
   }

   private void reset() {
      this.target = null;
      this.pending = -1;
      this.delay = 0;
      this.origin = -1;
      this.ourSlot = -1;
      this.returnDelay = 0;
   }

   @EventHandler
   public void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (player != null && mc.field_1687 != null) {
         int selected = player.method_31548().method_67532();
         if (this.origin != -1 && selected != this.ourSlot) {
            this.origin = -1;
         }

         class_2338 aimed = mc.field_1755 == null && mc.field_1690.field_1886.method_1434() ? aimedBlock(mc) : null;
         class_2680 state = aimed == null ? null : mc.field_1687.method_8320(aimed);
         if (aimed != null && !state.method_26215()) {
            if (!aimed.equals(this.target)) {
               this.target = aimed.method_10062();
               this.pending = -1;
               this.delay = 0;
            }

            int shard = shardSlot(player.method_31548(), state, selected);
            if (this.drill.get()) {
               int drillSlot = this.drillSlot(mc, state, aimed, shard);
               if (drillSlot != -1) {
                  if (drillSlot != selected) {
                     this.select(drillSlot);
                  }

                  this.pending = -1;
                  return;
               }
            }

            int best = this.bestSlot(mc, state, aimed, selected, shard);
            if (best == selected) {
               this.pending = -1;
            } else {
               if (this.pending != best) {
                  this.pending = best;
                  this.delay = this.switchDelay();
               }

               if (this.delay > 0) {
                  this.delay--;
               } else {
                  this.select(best);
                  this.pending = -1;
               }
            }
         } else {
            this.target = null;
            this.pending = -1;
            this.delay = 0;
            this.giveBack();
         }
      } else {
         this.reset();
      }
   }

   private static class_2338 aimedBlock(class_310 mc) {
      return mc.field_1765 instanceof class_3965 hit && hit.method_17783() == class_240.field_1332 ? hit.method_17777() : null;
   }

   private int drillSlot(class_310 mc, class_2680 state, class_2338 pos, int shard) {
      if (shard != -1) {
         return breakingDelta(mc, shard, state, pos) >= 1.0F ? shard : -1;
      }

      class_1661 inventory = mc.field_1724.method_31548();

      for (int slot = 0; slot < 9; slot++) {
         class_1799 stack = inventory.method_5438(slot);
         if (stack.method_31574(class_1802.field_22024) && !stack.method_63692() && breakingDelta(mc, slot, state, pos) >= 1.0F) {
            return slot;
         }
      }

      return -1;
   }

   private int bestSlot(class_310 mc, class_2680 state, class_2338 pos, int selected, int shard) {
      class_1661 inventory = mc.field_1724.method_31548();
      class_1799 held = inventory.method_5438(selected);
      boolean dropHeld = selected == this.ourSlot && this.origin != -1 && held.method_63692();
      int best = selected;
      float bestDelta = dropHeld ? -1.0F : breakingDelta(mc, selected, state, pos);

      for (int slot = 0; slot < 9; slot++) {
         class_1799 stack = inventory.method_5438(slot);
         if (slot != selected && !stack.method_7960() && !stack.method_63692()) {
            float delta = breakingDelta(mc, slot, state, pos);
            if (delta > bestDelta) {
               bestDelta = delta;
               best = slot;
            }
         }
      }

      return shard != -1 && inventory.method_5438(best).method_31573(class_3489.field_42614) ? shard : best;
   }

   private static int shardSlot(class_1661 inventory, class_2680 state, int selected) {
      if (isShardFor(inventory.method_5438(selected), state)) {
         return selected;
      }

      for (int slot = 0; slot < 9; slot++) {
         if (slot != selected && isShardFor(inventory.method_5438(slot), state)) {
            return slot;
         }
      }

      return -1;
   }

   private static boolean isShardFor(class_1799 stack, class_2680 state) {
      return !stack.method_63692() && stack.method_7951(state) && isShardPickaxe(stack);
   }

   private static boolean isShardPickaxe(class_1799 stack) {
      if (!stack.method_31573(class_3489.field_42614)) {
         return false;
      }

      if (AntiVanishText.normalize(stack.method_7964().getString()).contains("shard pickaxe")) {
         return true;
      }

      for (class_2561 line : ((class_9290)stack.method_58695(class_9334.field_49632, class_9290.field_49340)).comp_2400()) {
         if (AntiVanishText.normalize(line.getString()).contains("breaks 9 blocks at once")) {
            return true;
         }
      }

      return false;
   }

   private static float breakingDelta(class_310 mc, int slot, class_2680 state, class_2338 pos) {
      float hardness = state.method_26214(mc.field_1687, pos);
      if (hardness == -1.0F) {
         return 0.0F;
      }

      class_746 player = mc.field_1724;
      class_1799 stack = player.method_31548().method_5438(slot);
      float speed = stack.method_7924(state);
      if (speed > 1.0F) {
         double[] own = new double[]{0.0};
         stack.method_57354(class_1304.field_6173, (attribute, modifier) -> {
            if (attribute == class_5134.field_51581) {
               own[0] += modifier.comp_2449();
            }
         });
         speed += (float)own[0];
      }

      boolean correct = !state.method_29291() || stack.method_7951(state);
      return withMiningModifiers(player, speed) / hardness / (correct ? 30 : 100);
   }

   private static float withMiningModifiers(class_746 player, float speed) {
      if (class_1292.method_5576(player)) {
         speed *= 1.0F + (class_1292.method_5575(player) + 1) * 0.2F;
      }

      class_1293 fatigue = player.method_6112(class_1294.field_5901);
      if (fatigue != null) {
         speed *= switch (fatigue.method_5578()) {
            case 0 -> 0.3F;
            case 1 -> 0.09F;
            case 2 -> 0.0027F;
            default -> 8.1E-4F;
         };
      }

      speed *= (float)player.method_45325(class_5134.field_49076);
      if (player.method_5777(class_3486.field_15517)) {
         speed *= (float)player.method_45325(class_5134.field_51576);
      }

      if (!player.method_24828()) {
         speed /= 5.0F;
      }

      return speed;
   }

   private void select(int slot) {
      class_1661 inventory = class_310.method_1551().field_1724.method_31548();
      if (this.origin == -1) {
         this.origin = inventory.method_67532();
      }

      inventory.method_61496(slot);
      this.ourSlot = slot;
      this.returnDelay = this.switchDelay();
   }

   private int switchDelay() {
      return this.delayed.get() ? 2 : 0;
   }

   private void giveBack() {
      if (this.origin != -1) {
         if (this.returnDelay > 0) {
            this.returnDelay--;
         } else {
            this.select(this.origin);
            this.origin = -1;
         }
      }
   }
}
