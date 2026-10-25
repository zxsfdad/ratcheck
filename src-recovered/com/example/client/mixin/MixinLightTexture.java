package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.render.FullbrightModule;
import net.anubis.client.module.impl.render.NoRenderModule;
import net.anubis.client.module.impl.render.XrayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_765;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_765.class)
public abstract class MixinLightTexture {
   @Shadow
   @Final
   private GpuTexture field_57927;

   @Inject(
      method = "method_3313",
      at = @At(value = "FIELD", target = "Lnet/minecraft/class_765;field_4135:Z", opcode = 181, shift = Shift.AFTER),
      cancellable = true
   )
   private void anubis$xrayFullbright(float partialTick, CallbackInfo ci) {
      if (XrayModule.active != null) {
         RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.field_57927, -1);
         ci.cancel();
      }
   }

   @ModifyExpressionValue(method = "method_3313", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_746;method_66279(Lnet/minecraft/class_6880;F)F"))
   private float anubis$hideDarknessPulse(float blendFactor) {
      return NoRenderModule.hideBlindnessDarkness() ? 0.0F : blendFactor;
   }

   @ModifyArg(
      method = "method_3313",
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 6),
      index = 0
   )
   private float anubis$fullbrightGamma(float gamma) {
      FullbrightModule fullbright = ModuleRegistry.get(FullbrightModule.class);
      return fullbright != null && fullbright.isEnabled() ? fullbright.gamma() : gamma;
   }
}
