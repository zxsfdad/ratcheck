package com.example.client.mixin;

import net.anubis.client.module.impl.render.AtmosphereModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_9955;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(class_9955.class)
public abstract class MixinAtmosphereCloudRenderer {
   @ModifyVariable(method = "method_62168", at = @At("HEAD"), argsOnly = true, ordinal = 0)
   private int anubis$cloudColour(int color) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      return atmosphere != null ? atmosphere.clouds(color) : color;
   }
}
