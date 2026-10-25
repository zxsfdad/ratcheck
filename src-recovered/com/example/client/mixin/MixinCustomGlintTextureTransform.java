package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.anubis.client.module.impl.render.CustomGlintModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12250;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(class_12250.class)
public abstract class MixinCustomGlintTextureTransform {
   @ModifyVariable(method = "method_76031", at = @At("HEAD"), argsOnly = true)
   private static float anubis$glintPatternScale(float scale) {
      CustomGlintModule module = CustomGlintModule.active;
      return module == null ? scale : scale * module.uvScaleMultiplier();
   }

   @ModifyExpressionValue(method = "method_76031", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_156;method_658()J"))
   private static long anubis$glintClock(long millis) {
      CustomGlintModule module = CustomGlintModule.active;
      return module == null ? millis : module.glintMillis(millis);
   }
}
