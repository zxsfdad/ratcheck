package net.anubis.client.module.impl.combat.melee;

import net.anubis.client.module.CombatMacroLock;
import net.anubis.client.module.impl.combat.AutoInvTotemModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1661;
import net.minecraft.class_310;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class SlotReturn {
   private static final int NONE = -1;
   private final Object owner;
   private int previous = -1;
   private int taken = -1;

   public SlotReturn(Object owner) {
      this.owner = owner;
   }

   public boolean take(class_746 player, int slot) {
      class_1661 inventory = player.method_31548();
      if (inventory.method_67532() == slot && !this.holding()) {
         return true;
      }

      if (class_1661.method_7380(slot) && !AutoInvTotemModule.wantsHotbar() && CombatMacroLock.tryAcquire(this.owner)) {
         if (!this.holding()) {
            this.previous = inventory.method_67532();
         }

         this.taken = slot;
         inventory.method_61496(slot);
         return true;
      } else {
         return false;
      }
   }

   public boolean holding() {
      return this.taken != -1;
   }

   public void tick(class_310 mc, boolean back) {
      if (this.holding()) {
         class_746 player = mc.field_1724;
         if (player == null) {
            this.finish(null, false);
         } else if (AutoInvTotemModule.wantsHotbar()) {
            CombatMacroLock.release(this.owner);
         } else if (mc.field_1755 == null && !MeleeAttacks.actedThisTick(player)) {
            this.finish(player, back);
         }
      }
   }

   public void drop(class_310 mc, boolean back) {
      if (this.holding()) {
         this.finish(mc.field_1724, back);
      }
   }

   private void finish(class_746 player, boolean back) {
      if (back && player != null && player.method_31548().method_67532() == this.taken && this.previous != this.taken) {
         player.method_31548().method_61496(this.previous);
      }

      CombatMacroLock.release(this.owner);
      this.previous = -1;
      this.taken = -1;
   }
}
