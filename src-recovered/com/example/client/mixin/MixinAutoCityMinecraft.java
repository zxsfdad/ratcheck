package com.example.client.mixin;

import net.anubis.client.module.impl.combat.AutoCityModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_310.class)
public abstract class MixinAutoCityMinecraft {
   @Inject(method = "method_1590", at = @At("HEAD"), cancellable = true)
   private void anubis$autoCityContinueAttack(boolean down, CallbackInfo ci) {
      if (AutoCityModule.mining()) {
         ci.cancel();
      }
   }
}
