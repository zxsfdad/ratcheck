package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.anubis.client.module.impl.misc.FreecamModule;
import net.anubis.client.module.impl.render.NoRenderModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1799;
import net.minecraft.class_757;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_757.class)
public abstract class MixinGameRendererEffects {
   @ModifyExpressionValue(method = "method_3172", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_5498;method_31034()Z"))
   private boolean anubis$freecamHands(boolean firstPerson) {
      FreecamModule freecam = FreecamModule.active;
      return freecam != null && freecam.keepHands() || firstPerson;
   }

   @Inject(method = "method_3189", at = @At("HEAD"), cancellable = true)
   private void anubis$cancelTotemAnimation(class_1799 stack, CallbackInfo ci) {
      if (NoRenderModule.hideTotemAnimation()) {
         ci.cancel();
      }
   }

   @WrapOperation(method = "method_3188", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
   private float anubis$cancelNauseaWobble(float portal, float nausea, Operation<Float> original) {
      return NoRenderModule.hideNauseaPortal() ? 0.0F : (Float)original.call(new Object[]{portal, nausea});
   }
}
