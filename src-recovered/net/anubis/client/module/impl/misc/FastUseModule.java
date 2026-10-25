package net.anubis.client.module.impl.misc;

import com.example.client.mixin.AccessorCombatMinecraft;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.EventPriority;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1747;
import net.minecraft.class_1799;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class FastUseModule extends ClientModule {
   private final NumberSetting delay = this.addSetting("delay", new NumberSetting("Delay", 0.0F, 0.0F, 3.0F).step(1.0F));
   private final BooleanSetting blocks = this.addSetting("blocks", new BooleanSetting("Blocks", true));
   private final BooleanSetting items = this.addSetting("items", new BooleanSetting("Items", true));

   public FastUseModule() {
      super("fast-use", "Fast Use", "Places blocks and uses items faster while use is held.", ModuleCategory.MISC);
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (player != null && mc.field_1755 == null && mc.field_1690.field_1904.method_1434()) {
         if (!player.method_6115() && this.covers(player)) {
            AccessorCombatMinecraft access = (AccessorCombatMinecraft)mc;
            int wait = access.anubis$rightClickDelay();
            if (!ServerRotation.pending() && !MeleeAttacks.actedThisTick(player)) {
               if (wait > this.delay.getInt() + 1) {
                  access.anubis$setRightClickDelay(this.delay.getInt() + 1);
               }
            } else {
               access.anubis$setRightClickDelay(Math.max(wait, 2));
               class_304 use = mc.field_1690.field_1904;

               while (use.method_1436()) {
               }
            }
         }
      }
   }

   private boolean covers(class_746 player) {
      class_1799 held = player.method_6047();
      if (held.method_7960()) {
         held = player.method_6079();
      }

      if (held.method_7960()) {
         return false;
      } else {
         return held.method_7909() instanceof class_1747 ? this.blocks.get() : this.items.get();
      }
   }
}
