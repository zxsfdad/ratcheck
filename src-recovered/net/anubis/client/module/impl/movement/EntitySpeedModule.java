package net.anubis.client.module.impl.movement;

import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.movement.entitycontrol.EntityControlVehicleHook;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;

@Environment(EnvType.CLIENT)
public final class EntitySpeedModule extends ClientModule {
   public static volatile EntitySpeedModule active;
   private final NumberSetting horizontalSpeed = this.addSetting("horizontal-speed", new NumberSetting("Horizontal Speed", 10.0F, 0.0F, 50.0F).step(0.5F));
   private final BooleanSetting onlyOnGround = this.addSetting("only-on-ground", new BooleanSetting("Only On Ground", false));
   private final BooleanSetting inWater = this.addSetting("in-water", new BooleanSetting("In Water", true));

   public EntitySpeedModule() {
      super("entity-speed", "Entity Speed", "Makes you go faster horizontally when riding entities.", ModuleCategory.MOVEMENT);
   }

   @Override
   protected void onEnable() {
      active = this;
      EntityControlVehicleHook.refresh();
   }

   @Override
   protected void onDisable() {
      active = null;
      EntityControlVehicleHook.refresh();
   }

   public boolean appliesTo(class_1297 vehicle) {
      return (!this.onlyOnGround.get() || vehicle.method_24828() || vehicle.method_70987()) && (this.inWater.get() || !vehicle.method_5799());
   }

   public double horizontalSpeed() {
      return this.horizontalSpeed.get();
   }
}
