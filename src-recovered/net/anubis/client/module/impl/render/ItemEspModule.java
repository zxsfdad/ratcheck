package net.anubis.client.module.impl.render;

import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ItemColorListSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.TracerEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1542;
import net.minecraft.class_1792;
import net.minecraft.class_243;
import net.minecraft.class_310;

@Environment(EnvType.CLIENT)
public final class ItemEspModule extends ClientModule {
   private static ItemEspModule instance;
   private final NumberSetting sRange = this.addSetting("range", new NumberSetting("Range", 128.0F, 10.0F, 512.0F).step(1.0F));
   private final BooleanSetting sOutline = this.addSetting("outline", new BooleanSetting("Outline", true));
   private final BooleanSetting sTracers = this.addSetting("tracers", new BooleanSetting("Tracers", false));
   private final ItemColorListSetting sItemColors = this.addSetting(ItemColorListSetting.createDefault());

   public ItemEspModule() {
      super("item-esp", "Item ESP", "Highlights selected dropped items with model outlines and tracers.", ModuleCategory.RENDER);
      instance = this;
   }

   public static boolean shouldGlow(class_1297 entity) {
      ItemEspModule module = instance;
      return module != null && module.isEnabled() && module.sOutline.get() ? entity instanceof class_1542 item && module.wants(item, 1.0F) : false;
   }

   public static int glowColor(class_1297 entity) {
      ItemEspModule module = instance;
      return module != null && entity instanceof class_1542 item ? module.sItemColors.colorArgb(item.method_6983().method_7909()) & 16777215 : 16777215;
   }

   private boolean wants(class_1542 item, float partialTick) {
      if (item.method_31481()) {
         return false;
      }

      class_1792 kind = item.method_6983().method_7909();
      if (!this.sItemColors.isEnabled(kind)) {
         return false;
      }

      class_310 mc = class_310.method_1551();
      if (mc.field_1724 == null) {
         return true;
      }

      float range = Math.max(1.0F, this.sRange.get());
      return item.method_30950(partialTick).method_1025(mc.field_1724.method_5836(partialTick)) <= (double)range * range;
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      if (this.sTracers.get() && this.sItemColors.hasAnyEnabled()) {
         class_310 mc = class_310.method_1551();
         if (mc.field_1687 != null) {
            for (class_1297 entity : mc.field_1687.method_18112()) {
               if (entity instanceof class_1542 item && this.wants(item, event.partialTick)) {
                  class_243 pos = item.method_30950(event.partialTick);
                  TracerEngine.draw(pos, this.sItemColors.colorArgb(item.method_6983().method_7909()));
               }
            }
         }
      }
   }
}
