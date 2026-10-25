package com.example.client.mixin;

import net.anubis.client.module.impl.render.ColorGradingModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_757;
import net.minecraft.class_9779;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_757.class)
public abstract class MixinColorGradingGameRenderer {
   @Inject(method = "method_3192", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_758;method_71108()V"))
   private void anubis$colorGrading(class_9779 deltaTracker, boolean renderLevel, CallbackInfo ci) {
      if (ColorGradingModule.active != null) {
         ColorGradingModule.afterWorld(renderLevel);
      }
   }
}
