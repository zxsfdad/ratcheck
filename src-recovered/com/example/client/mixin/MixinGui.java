package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.anubis.client.event.impl.Render2DEvent;
import net.anubis.client.module.impl.misc.FreecamModule;
import net.anubis.client.module.impl.render.NoRenderModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2960;
import net.minecraft.class_329;
import net.minecraft.class_332;
import net.minecraft.class_9779;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_329.class)
public abstract class MixinGui {
   @ModifyExpressionValue(method = "method_1736", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_5498;method_31034()Z"))
   private boolean anubis$freecamCrosshair(boolean firstPerson) {
      FreecamModule freecam = FreecamModule.active;
      return freecam != null && freecam.showBody() && !firstPerson || firstPerson;
   }

   @Inject(method = "method_1753", at = @At("TAIL"))
   private void anubis$onRender2D(class_332 graphics, class_9779 deltaTracker, CallbackInfo ci) {
      Render2DEvent.dispatch(graphics, deltaTracker.method_60637(false));
   }

   @Inject(method = "method_1765", at = @At("HEAD"), cancellable = true)
   private void anubis$cancelVanillaEffects(class_332 guiGraphics, class_9779 deltaTracker, CallbackInfo ci) {
      ci.cancel();
   }

   @Inject(method = "method_31977", at = @At("HEAD"), cancellable = true)
   private void anubis$cancelTextureOverlay(class_332 graphics, class_2960 sprite, float alpha, CallbackInfo ci) {
      if (NoRenderModule.hidePumpkinPowderSnow()) {
         ci.cancel();
      }
   }

   @Inject(method = "method_1746", at = @At("HEAD"), cancellable = true)
   private void anubis$cancelPortalOverlay(class_332 graphics, float alpha, CallbackInfo ci) {
      if (NoRenderModule.hideNauseaPortal()) {
         ci.cancel();
      }
   }

   @Inject(method = "method_61980", at = @At("HEAD"), cancellable = true)
   private void anubis$cancelConfusionOverlay(class_332 graphics, float alpha, CallbackInfo ci) {
      if (NoRenderModule.hideNauseaPortal()) {
         ci.cancel();
      }
   }
}
