package net.anubis.client.module.impl.combat;

import net.anubis.client.event.EventPriority;
import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.KeyInputEvent;
import net.anubis.client.event.impl.MouseInputEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.input.ModuleKeybinds;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.CombatMacroLock;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.combat.crystal.Crystals;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.anubis.client.module.impl.combat.melee.SlotReturn;
import net.anubis.client.module.setting.KeySetting;
import net.anubis.client.util.pvp.InputControl;
import net.anubis.client.util.pvp.Rotation;
import net.anubis.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1268;
import net.minecraft.class_1661;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_310;
import net.minecraft.class_746;
import net.minecraft.class_1269.class_9860;
import net.minecraft.class_1269.class_9861;

@Environment(EnvType.CLIENT)
public final class KeyThrowModule extends ClientModule {
   private static final int BUFFER_TICKS = 10;
   private final KeySetting pearlKey = this.addSetting("pearl-key", new KeySetting("Pearl Key", -1));
   private final KeySetting windChargeKey = this.addSetting("wind-charge-key", new KeySetting("Wind Charge Key", -1));
   private final SlotReturn slots = new SlotReturn(this);
   private class_1792 pending;
   private int pendingTicks;

   public KeyThrowModule() {
      super("key-throw", "Key Throw", "Throws a pearl or a wind charge with a key and swaps back.", ModuleCategory.COMBAT);
   }

   @Override
   protected void onEnable() {
      this.pending = null;
      this.on(KeyInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onKey);
      this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onMouse);
      this.on(TickEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onTick);
   }

   @Override
   protected void onDisable() {
      this.pending = null;
      this.slots.drop(class_310.method_1551(), true);
   }

   private void onKey(KeyInputEvent event) {
      if (event.action == 1) {
         this.onPress(event.key);
      }
   }

   private void onMouse(MouseInputEvent event) {
      if (event.action == 1) {
         this.onPress(1000 + event.button);
      }
   }

   private void onPress(int pressed) {
      if (ModuleKeybinds.isBindable(pressed)) {
         class_1792 item = pressed == this.pearlKey.value() ? class_1802.field_8634 : (pressed == this.windChargeKey.value() ? class_1802.field_49098 : null);
         class_310 mc = class_310.method_1551();
         if (item != null && mc.field_1755 == null && mc.field_1724 != null) {
            this.pending = item;
            this.pendingTicks = 0;
         }
      }
   }

   private void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      this.slots.tick(mc, true);
      if (mc.field_1724 == null) {
         this.pending = null;
      } else if (this.pending != null) {
         this.tryThrow(mc, mc.field_1724);
      }
   }

   private void tryThrow(class_310 mc, class_746 player) {
      if (++this.pendingTicks > 10) {
         this.pending = null;
      } else if (MeleeAttacks.canAct(mc) && MeleeAttacks.handsFree(mc, player) && !MeleeAttacks.actedThisTick(player)) {
         if (!CombatMacroLock.heldByOther(this) && !AutoInvTotemModule.wantsHotbar()) {
            class_1661 inventory = player.method_31548();
            boolean offhand = player.method_6079().method_31574(this.pending);
            int slot = player.method_6047().method_31574(this.pending) ? inventory.method_67532() : Crystals.hotbarSlotOf(inventory, this.pending);
            if (!offhand && slot < 0) {
               this.pending = null;
            } else {
               class_1799 stack = offhand ? player.method_6079() : inventory.method_5438(slot);
               if (!player.method_7357().method_7904(stack)) {
                  boolean windCharge = this.pending == class_1802.field_49098;
                  if (!ServerRotation.pending() && (!windCharge || ServerRotation.canRotate(player))) {
                     if (offhand || this.slots.take(player, slot)) {
                        this.pending = null;
                        if (windCharge) {
                           ServerRotation.request(new Rotation(player.method_36454(), 90.0F));
                           if (player.method_24828()) {
                              InputControl.jump();
                           }
                        }

                        class_1268 hand = offhand ? class_1268.field_5810 : class_1268.field_5808;
                        if (mc.field_1761.method_2919(player, hand) instanceof class_9860 success) {
                           if (success.comp_2909() == class_9861.field_52427) {
                              player.method_6104(hand);
                           }

                           mc.field_1773.field_4012.method_3215(hand);
                        }

                        MeleeAttacks.claimTick(mc);
                     }
                  }
               }
            }
         }
      }
   }
}
