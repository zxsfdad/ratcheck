package com.example.client.mixin;

import net.anubis.client.module.impl.render.NoRenderModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_5636;
import net.minecraft.class_7286;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(class_7286.class)
public abstract class MixinMobEffectFogEnvironment {
   @Inject(method = "method_42593", at = @At("HEAD"), cancellable = true)
   private void anubis$cancelBlindnessDarknessFog(class_5636 fogType, class_1297 entity, CallbackInfoReturnable<Boolean> cir) {
      if (NoRenderModule.hideBlindnessDarkness()) {
         cir.setReturnValue(false);
      }
   }
}
