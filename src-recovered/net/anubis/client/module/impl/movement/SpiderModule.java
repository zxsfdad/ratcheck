package net.anubis.client.module.impl.movement;

import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class SpiderModule extends ClientModule {
   private static final double CLIMB_VELOCITY_THRESHOLD = 0.2;
   private final NumberSetting climbSpeed = this.addSetting("climb-speed", new NumberSetting("Climb Speed", 0.2F, 0.0F, 10.0F).step(0.1F));

   public SpiderModule() {
      super("spider", "Spider", "Allows you to climb walls like a spider.", ModuleCategory.MOVEMENT);
   }

   @Override
   protected void onEnable() {
      this.on(TickEvent.TYPE, Stage.POST, this::onPostTick);
   }

   private void onPostTick(TickEvent event) {
      class_746 player = class_310.method_1551().field_1724;
      if (player != null && player.field_5976) {
         class_243 velocity = player.method_18798();
         if (!(velocity.field_1351 >= 0.2)) {
            player.method_18800(velocity.field_1352, this.climbSpeed.get(), velocity.field_1350);
         }
      }
   }
}
