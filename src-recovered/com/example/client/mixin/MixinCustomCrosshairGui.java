package com.example.client.mixin;

import net.anubis.client.module.impl.render.CustomCrosshairModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_329;
import net.minecraft.class_332;
import net.minecraft.class_9779;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_329.class)
public abstract class MixinCustomCrosshairGui {
   @Inject(method = "method_1736", at = @At("HEAD"), cancellable = true)
   private void anubis$hideVanillaCrosshair(class_332 graphics, class_9779 deltaTracker, CallbackInfo ci) {
      if (CustomCrosshairModule.active != null) {
         ci.cancel();
      }
   }
}
