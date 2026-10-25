package com.example.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.module.impl.render.NoRenderModule;
import net.anubis.client.util.ShaderOverlay;
import net.anubis.client.util.TracerEngine;
import net.anubis.client.util.WorldQuads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_4184;
import net.minecraft.class_4604;
import net.minecraft.class_5636;
import net.minecraft.class_761;
import net.minecraft.class_12180.class_12181;
import net.minecraft.class_758.class_4596;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(class_761.class)
public abstract class MixinLevelRenderer {
   @ModifyArg(
      method = "method_22710",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/class_863;method_23099(Lnet/minecraft/class_4604;DDDF)V"),
      index = 0
   )
   private class_4604 anubis$onRenderLevel(
      class_4604 frustum, @Local(argsOnly = true, ordinal = 0) Matrix4f viewRotation, @Local(argsOnly = true, ordinal = 1) Matrix4f projection
   ) {
      float partialTick = class_310.method_1551().method_61966().method_60637(false);
      TracerEngine.beginFrame(viewRotation, projection);
      WorldQuads.beginFrame(class_310.method_1551().field_1773.method_19418().method_71156());
      class_12181 capture = ShaderOverlay.capture();

      try {
         Render3DEvent.dispatch(partialTick, frustum);
      } catch (Throwable var9) {
         if (capture != null) {
            try {
               capture.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }
         }

         throw var9;
      }

      if (capture != null) {
         capture.close();
      }

      return frustum;
   }

   @ModifyArg(
      method = "method_22710",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_761;method_62199(Lnet/minecraft/class_9909;Lnet/minecraft/class_12075;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Matrix4f;)V"
      ),
      index = 2
   )
   private GpuBufferSlice anubis$noFluidFogOnTop(GpuBufferSlice fog, @Local(argsOnly = true) class_4184 camera) {
      return anubis$onTopFog(fog, camera);
   }

   @Unique
   private static GpuBufferSlice anubis$onTopFog(GpuBufferSlice fog, class_4184 camera) {
      return camera.method_19334() == class_5636.field_27888
         ? fog
         : ((AccessorGameRenderer)class_310.method_1551().field_1773).anubis$getFogRenderer().method_71109(class_4596.field_60101);
   }

   @Inject(method = "method_22710", at = @At("TAIL"))
   private void anubis$overlayAfterShaderPack(
      CallbackInfo ci,
      @Local(argsOnly = true) GpuBufferSlice fog,
      @Local(argsOnly = true) class_4184 camera,
      @Local(argsOnly = true, ordinal = 0) Matrix4f viewRotation,
      @Local(argsOnly = true, ordinal = 1) Matrix4f projection
   ) {
      WorldQuads.render(viewRotation, projection);
      if (ShaderOverlay.pending()) {
         ShaderOverlay.render(camera, viewRotation, projection, fog, anubis$onTopFog(fog, camera));
      }
   }

   @Inject(method = "method_22710", at = @At("TAIL"))
   private void anubis$worldFogForHand(CallbackInfo ci, @Local(argsOnly = true) GpuBufferSlice fog, @Local(argsOnly = true) class_4184 camera) {
      if (camera.method_19334() != class_5636.field_27888) {
         RenderSystem.setShaderFog(fog);
      }
   }

   @Inject(method = "method_43788", at = @At("HEAD"), cancellable = true)
   private void anubis$keepSkyDuringBlindnessDarkness(class_4184 camera, CallbackInfoReturnable<Boolean> cir) {
      if (NoRenderModule.hideBlindnessDarkness()) {
         cir.setReturnValue(false);
      }
   }
}
