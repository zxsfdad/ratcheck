package com.example.client.mixin;

import net.anubis.client.module.impl.render.AtmosphereModule;
import net.anubis.client.module.impl.render.SkyLyrics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12076;
import net.minecraft.class_12131;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_638;
import net.minecraft.class_9975;
import net.minecraft.class_2874.class_12326;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_9975.class)
public abstract class MixinAtmosphereSkyRenderer {
   @Inject(method = "method_74926", at = @At("TAIL"))
   private void anubis$readSky(class_638 level, float partialTick, class_4184 camera, class_12076 state, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && state.field_64464 == class_12326.field_64386) {
         atmosphere.extract(level, partialTick, state);
      }
   }

   @Inject(method = "method_62302", at = @At("HEAD"), cancellable = true)
   private void anubis$dome(int color, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && atmosphere.skyDisc()) {
         ci.cancel();
      }
   }

   @Inject(method = "method_62302", at = @At("TAIL"))
   private void anubis$afterDome(int color, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null) {
         atmosphere.afterSkyDisc();
      }
   }

   @Inject(method = "method_62306", at = @At("HEAD"), cancellable = true)
   private void anubis$noSunriseFan(class_4587 pose, float sunAngle, int color, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && atmosphere.replacesSunrise()) {
         ci.cancel();
      }
   }

   @Inject(method = "method_62310", at = @At("HEAD"), cancellable = true)
   private void anubis$stars(float starBrightness, class_4587 pose, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && atmosphere.stars(pose)) {
         ci.cancel();
      }
   }

   @Inject(
      method = "method_62307",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/class_9975;method_62304(Lnet/minecraft/class_12131;FLnet/minecraft/class_4587;)V")
   )
   private void anubis$beforeMoon(
      class_4587 pose, float sunAngle, float moonAngle, float starAngle, class_12131 moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
   ) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null) {
         atmosphere.beforeMoon(pose);
      }
   }

   @Inject(
      method = "method_62307",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_9975;method_62304(Lnet/minecraft/class_12131;FLnet/minecraft/class_4587;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$afterMoon(
      class_4587 pose, float sunAngle, float moonAngle, float starAngle, class_12131 moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
   ) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null) {
         atmosphere.afterMoon(pose);
      }
   }

   @Inject(method = "method_62307", at = @At("TAIL"))
   private void anubis$afterCelestials(
      class_4587 pose, float sunAngle, float moonAngle, float starAngle, class_12131 moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
   ) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null) {
         atmosphere.afterCelestials();
      }

      SkyLyrics.render();
   }

   @Inject(method = "method_62312", at = @At("HEAD"), cancellable = true)
   private void anubis$endSky(CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && atmosphere.endSky()) {
         ci.cancel();
      }
   }
}
