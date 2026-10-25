package com.example.client.mixin;

import net.anubis.client.module.impl.render.NoRenderModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11401;
import net.minecraft.class_4184;
import net.minecraft.class_638;
import net.minecraft.class_7285;
import net.minecraft.class_9779;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_11401.class)
public abstract class MixinNoRenderLavaFog {
   @Inject(method = "method_42591", at = @At("TAIL"))
   private void anubis$clearLavaFog(class_7285 fog, class_4184 camera, class_638 level, float renderDistance, class_9779 deltaTracker, CallbackInfo ci) {
      if (NoRenderModule.hideLavaFog()) {
         fog.field_60582 = -8.0F;
         fog.field_60584 = renderDistance;
         fog.field_60099 = renderDistance;
         fog.field_60100 = renderDistance;
      }
   }
}
