package net.anubis.client.module.impl.misc;

import java.util.Set;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.EventPriority;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.CombatMacroLock;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.combat.AutoInvTotemModule;
import net.anubis.client.module.impl.combat.crystal.Crystals;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.anubis.client.module.impl.combat.melee.SlotReturn;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1268;
import net.minecraft.class_1661;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_310;
import net.minecraft.class_4174;
import net.minecraft.class_746;
import net.minecraft.class_9334;

@Environment(EnvType.CLIENT)
public final class AutoEatModule extends ClientModule {
   private static final Set<class_1792> NEVER_EAT = Set.of(
      class_1802.field_8511,
      class_1802.field_8680,
      class_1802.field_8635,
      class_1802.field_8323,
      class_1802.field_8726,
      class_1802.field_8233,
      class_1802.field_8766,
      class_1802.field_8367
   );
   private static final int NOT_FOUND = -1;
   private static final int OFFHAND_SLOT = -2;
   private static final float HEALING_BONUS = 100.0F;
   private static AutoEatModule instance;
   private final NumberSetting hunger = this.addSetting("hunger", new NumberSetting("Hunger", 16.0F, 0.0F, 20.0F).step(1.0F));
   private final NumberSetting health = this.addSetting("health", new NumberSetting("Health", 10.0F, 0.0F, 20.0F).step(1.0F));
   private final SlotReturn slots = new SlotReturn(this);
   private boolean eating;

   public AutoEatModule() {
      super("auto-eat", "Auto Eat", "Eats hotbar or offhand food once hunger or health drops too low.", ModuleCategory.MISC);
      instance = this;
   }

   @Override
   protected void onDisable() {
      class_310 mc = class_310.method_1551();
      this.stop(mc);
      this.slots.drop(mc, true);
   }

   public static boolean isEating() {
      AutoEatModule module = instance;
      return module != null && module.isEnabled() && module.eating;
   }

   @EventHandler(priority = EventPriority.HIGH)
   public void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      this.eat(mc);
      if (!this.eating) {
         this.slots.tick(mc, true);
      }
   }

   private void eat(class_310 mc) {
      class_746 player = mc.field_1724;
      if (player != null && mc.field_1687 != null && mc.field_1761 != null && mc.field_1755 == null && !AutoInvTotemModule.wantsHotbar()) {
         if (player.method_6115() && player.method_6030().method_57826(class_9334.field_50075)) {
            if (this.eating) {
               mc.field_1690.field_1904.method_23481(true);
            }
         } else if (!this.shouldEat(player)) {
            this.stop(mc);
         } else {
            int slot = this.findBestSlot(player);
            if (slot == -1 || MeleeAttacks.actedThisTick(player) || !MeleeAttacks.handsFree(mc, player)) {
               this.stop(mc);
            } else if (CombatMacroLock.heldByOther(this)) {
               this.eating = true;
            } else {
               if (slot == -2) {
                  mc.field_1761.method_2919(player, class_1268.field_5810);
                  MeleeAttacks.claimTick(mc);
               } else if (!this.slots.take(player, slot)) {
                  this.stop(mc);
                  return;
               }

               this.eating = true;
               MeleeAttacks.takeAttackClicks(mc);
               mc.field_1690.field_1904.method_23481(true);
            }
         }
      } else {
         this.stop(mc);
      }
   }

   private boolean hungry(class_746 player) {
      return player.method_7344().method_7586() <= this.hunger.getInt();
   }

   private boolean shouldEat(class_746 player) {
      boolean hurt = this.health.getInt() > 0 && player.method_6032() <= this.health.getInt();
      return this.hungry(player) || hurt;
   }

   private int findBestSlot(class_746 player) {
      boolean healing = !this.hungry(player);
      class_1661 inventory = player.method_31548();
      int best = -1;
      float bestScore = -1.0F;

      for (int slot = 0; slot < 9; slot++) {
         class_4174 food = foodOf(player, inventory.method_5438(slot));
         if (food != null) {
            float score = score(food, healing);
            if (score > bestScore) {
               bestScore = score;
               best = slot;
            }
         }
      }

      class_4174 offhandFood = foodOf(player, player.method_6079());
      if (offhandFood != null && score(offhandFood, healing) > bestScore) {
         best = -2;
      }

      return best;
   }

   private static float score(class_4174 food, boolean healing) {
      float score = food.comp_2491() + food.comp_2492();
      return healing && food.comp_2493() ? score + 100.0F : score;
   }

   private static class_4174 foodOf(class_746 player, class_1799 stack) {
      if (!stack.method_7960() && !NEVER_EAT.contains(stack.method_7909()) && !player.method_7357().method_7904(stack)) {
         class_4174 food = (class_4174)stack.method_58694(class_9334.field_50075);
         return food != null && player.method_7332(food.comp_2493()) ? food : null;
      } else {
         return null;
      }
   }

   private void stop(class_310 mc) {
      if (this.eating) {
         this.eating = false;
         mc.field_1690.field_1904.method_23481(Crystals.isPhysicallyDown(mc, mc.field_1690.field_1904));
      }
   }
}
