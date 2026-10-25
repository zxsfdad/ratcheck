package com.example.client.mixin;

import net.anubis.client.module.impl.render.XrayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1922;
import net.minecraft.class_2338;
import net.minecraft.class_4970.class_4971;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(class_4971.class)
public abstract class MixinBlockStateBase {
   @Inject(method = "method_26210", at = @At("HEAD"), cancellable = true)
   private void anubis$xrayNoAmbientOcclusion(class_1922 level, class_2338 pos, CallbackInfoReturnable<Float> cir) {
      if (XrayModule.active != null) {
         cir.setReturnValue(1.0F);
      }
   }
}
