package net.anubis.client.module.impl.movement;

import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class AutoJumpModule extends ClientModule {
   private final ModeSetting mode = this.addSetting("mode", new ModeSetting("Mode", "Jump", "Jump", "LowHop"));
   private final ModeSetting jumpIf = this.addSetting("jump-if", new ModeSetting("Jump If", "Always", "Sprinting", "Walking", "Always"));
   private final NumberSetting velocityHeight = this.addSetting("velocity-height", new NumberSetting("Velocity Height", 0.25F, 0.0F, 2.0F).step(0.05F));

   public AutoJumpModule() {
      super("auto-jump", "Auto Jump", "Automatically jumps.", ModuleCategory.MOVEMENT);
   }

   @Override
   protected void onEnable() {
      this.on(TickEvent.TYPE, Stage.PRE, this::onPreTick);
   }

   private void onPreTick(TickEvent event) {
      class_746 player = class_310.method_1551().field_1724;
      if (player != null) {
         if (player.method_24828() && !player.method_5715() && this.shouldJump(player)) {
            if (this.mode.is("Jump")) {
               player.method_6043();
            } else {
               class_243 velocity = player.method_18798();
               player.method_18800(velocity.field_1352, this.velocityHeight.get(), velocity.field_1350);
            }
         }
      }
   }

   private boolean shouldJump(class_746 player) {
      boolean moving = player.field_6250 != 0.0F || player.field_6212 != 0.0F;

      return switch (this.jumpIf.get()) {
         case "Sprinting" -> player.method_5624() && moving;
         case "Walking" -> moving;
         default -> true;
      };
   }
}
