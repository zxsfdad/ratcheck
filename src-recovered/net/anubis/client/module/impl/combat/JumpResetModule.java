package net.anubis.client.module.impl.combat;

import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.PacketAppliedEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.util.pvp.InputControl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;
import net.minecraft.class_2743;
import net.minecraft.class_310;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class JumpResetModule extends ClientModule {
   private static final double MIN_KNOCKBACK = 0.1;
   private boolean pending;

   public JumpResetModule() {
      super("jump-reset", "Jump Reset", "Jumps the tick knockback lands to take less of it.", ModuleCategory.COMBAT);
   }

   @Override
   protected void onDisable() {
      this.pending = false;
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent event) {
      if (event.packet instanceof class_2743 motion) {
         class_746 player = class_310.method_1551().field_1724;
         if (player != null && motion.method_11818() == player.method_5628()) {
            class_243 knockback = motion.method_73085();
            if (knockback.method_37267() >= 0.1 && knockback.field_1351 > 0.0) {
               this.pending = true;
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (this.pending) {
         this.pending = false;
         class_310 mc = class_310.method_1551();
         class_746 player = mc.field_1724;
         if (player != null && mc.field_1755 == null) {
            if (player.method_24828() && player.method_5624() && player.field_3913.field_54155.comp_3159()) {
               if (!player.method_5799() && !player.method_5771() && !player.method_6101() && !player.method_5765() && !player.method_6128()) {
                  InputControl.jump();
               }
            }
         }
      }
   }
}
