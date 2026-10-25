package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_638;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(class_638.class)
public interface AccessorAtmosphereClientLevel {
   @Accessor("field_21526")
   int anubis$atmosphereSkyFlash();
}
