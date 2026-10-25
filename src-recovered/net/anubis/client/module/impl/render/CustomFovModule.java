package net.anubis.client.module.impl.render;

import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;

@Environment(EnvType.CLIENT)
public final class CustomFovModule extends ClientModule {
   public static volatile CustomFovModule active;
   private static float screenEffectScale = 1.0F;
   private final NumberSetting fov = this.addSetting("fov", new NumberSetting("FOV", 110.0F, 30.0F, 160.0F).step(1.0F));
   private int culledFov = Integer.MIN_VALUE;

   public CustomFovModule() {
      super("custom-fov", "Custom Fov", "Changes your field of view (FOV).", ModuleCategory.VISUALS);
      this.fov.onChange(this::onFovChanged);
   }

   @Override
   protected void onEnable() {
      active = this;
      this.culledFov = this.fov.getInt();
      scheduleTerrainUpdate();
   }

   @Override
   protected void onDisable() {
      active = null;
      this.culledFov = Integer.MIN_VALUE;
      scheduleTerrainUpdate();
   }

   public float fov() {
      return this.fov.getInt();
   }

   public static void hudFovApplied(float vanilla, float applied) {
      screenEffectScale = vanilla == applied ? 1.0F : (float)(Math.tan(Math.toRadians(applied) * 0.5) / Math.tan(Math.toRadians(vanilla) * 0.5));
   }

   public static float screenEffectScale() {
      return screenEffectScale;
   }

   private void onFovChanged() {
      if (active == this) {
         int value = this.fov.getInt();
         if (value != this.culledFov) {
            this.culledFov = value;
            scheduleTerrainUpdate();
         }
      }
   }

   private static void scheduleTerrainUpdate() {
      class_310 mc = class_310.method_1551();
      if (mc != null) {
         if (!mc.method_18854()) {
            mc.execute(CustomFovModule::scheduleTerrainUpdate);
         } else {
            if (mc.field_1687 != null && mc.field_1769 != null) {
               mc.field_1769.method_3292();
            }
         }
      }
   }
}
