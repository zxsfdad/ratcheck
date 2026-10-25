package net.anubis.client.module.impl.combat.totem;

import net.anubis.client.module.impl.combat.crystal.CrystalDamage;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1511;
import net.minecraft.class_1661;
import net.minecraft.class_1802;
import net.minecraft.class_310;
import net.minecraft.class_638;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class DoubleHand {
   private static final float DANGER_HEALTH = 6.0F;
   private static final double DANGER_RANGE = 6.0;
   private static final int SAFE_TICKS = 10;
   private boolean holding;
   private boolean rearmed = true;
   private int holdSlot = -1;
   private int returnSlot = -1;
   private int safeTicks;

   public boolean holding() {
      return this.holding;
   }

   public void tick(class_310 mc, class_746 player, boolean on) {
      class_1661 inventory = player.method_31548();
      int selected = inventory.method_67532();
      boolean mainTotem = player.method_6047().method_31574(class_1802.field_8288);
      boolean danger = on && inDanger(mc.field_1687, player);
      this.safeTicks = danger ? 0 : Math.min(this.safeTicks + 1, 10);
      if (this.safeTicks == 10) {
         this.holding = false;
         this.rearmed = true;
      }

      if (this.holding && selected != this.holdSlot && !mainTotem) {
         this.holding = false;
         this.rearmed = false;
         this.returnSlot = -1;
      }

      boolean clean = mc.field_1755 == null && !player.method_6115() && !MeleeAttacks.actedThisTick(player);
      if (this.holding) {
         if (mainTotem || !clean) {
            return;
         }

         int next = hotbarTotem(inventory);
         if (next != -1) {
            this.holdSlot = next;
            inventory.method_61496(next);
            return;
         }

         this.holding = false;
      } else if (danger && this.rearmed) {
         if (mainTotem) {
            this.holding = true;
            if (selected != this.holdSlot) {
               this.returnSlot = -1;
            }

            this.holdSlot = selected;
            return;
         }

         int slot = hotbarTotem(inventory);
         if (slot != -1 && clean && player.method_6079().method_31574(class_1802.field_8288)) {
            this.holding = true;
            this.holdSlot = slot;
            this.returnSlot = selected;
            inventory.method_61496(slot);
            return;
         }
      }

      if (this.returnSlot != -1 && clean) {
         this.putBack(inventory);
      }
   }

   public void putBack(class_1661 inventory) {
      if (this.returnSlot != -1) {
         if (inventory.method_67532() == this.holdSlot) {
            inventory.method_61496(this.returnSlot);
         }

         this.returnSlot = -1;
      }
   }

   public void reset() {
      this.holding = false;
      this.rearmed = true;
      this.holdSlot = -1;
      this.returnSlot = -1;
      this.safeTicks = 0;
   }

   private static boolean inDanger(class_638 level, class_746 player) {
      float health = CrystalDamage.health(player);
      if (health <= 6.0F) {
         return true;
      }

      for (class_1511 crystal : level.method_18467(class_1511.class, player.method_5829().method_1014(6.0))) {
         if (player.method_5739(crystal) <= 6.0 && CrystalDamage.estimate(crystal.method_73189(), player) >= health) {
            return true;
         }
      }

      return false;
   }

   private static int hotbarTotem(class_1661 inventory) {
      int selected = inventory.method_67532();

      for (int i = 0; i < 9; i++) {
         if (i != selected && inventory.method_5438(i).method_31574(class_1802.field_8288)) {
            return i;
         }
      }

      return inventory.method_5438(selected).method_31574(class_1802.field_8288) ? selected : -1;
   }
}
