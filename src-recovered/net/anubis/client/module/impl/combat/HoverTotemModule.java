package net.anubis.client.module.impl.combat;

import com.example.client.mixin.AccessorAbstractContainerScreen;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1703;
import net.minecraft.class_1713;
import net.minecraft.class_1735;
import net.minecraft.class_1802;
import net.minecraft.class_310;
import net.minecraft.class_465;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class HoverTotemModule extends ClientModule {
   private final BooleanSetting hotbarRefill = this.addSetting("hotbar-refill", new BooleanSetting("Hotbar Refill", false));
   private final NumberSetting hotbarSlot = this.addSetting("hotbar-slot", new NumberSetting("Hotbar Slot", 1.0F, 1.0F, 9.0F).step(1.0F));

   public HoverTotemModule() {
      super("hovertotem", "Hover Totem", "Swaps a hovered totem from any open container into the offhand or selected hotbar slot.", ModuleCategory.COMBAT);
      this.hotbarSlot.visibleWhen(this.hotbarRefill::get);
   }

   @EventHandler
   public void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (player != null && mc.field_1761 != null && !player.method_56992()) {
         if (mc.field_1755 instanceof class_465<?> screen && AutoInvTotemModule.clickSafe(player) && !AutoInvTotemModule.busy()) {
            class_1703 menu = screen.method_17577();
            class_1735 hovered = ((AccessorAbstractContainerScreen)screen).anubis$getHoveredSlot();
            if (hovered != null && menu.method_34255().method_7960() && hovered.method_7677().method_31574(class_1802.field_8288)) {
               if (hovered.field_7871 != player.method_31548() || hovered.method_34266() != 40) {
                  int swapButton;
                  if (!player.method_6079().method_31574(class_1802.field_8288)) {
                     swapButton = 40;
                  } else {
                     if (!this.hotbarRefill.get()) {
                        return;
                     }

                     swapButton = this.hotbarSlot.getInt() - 1;
                     if (player.method_31548().method_5438(swapButton).method_31574(class_1802.field_8288)) {
                        return;
                     }
                  }

                  mc.field_1761.method_2906(menu.field_7763, hovered.field_7874, swapButton, class_1713.field_7791, player);
               }
            }
         }
      }
   }
}
