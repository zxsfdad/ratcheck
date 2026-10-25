package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_761;
import net.minecraft.class_769;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(class_761.class)
public interface AccessorPinkWaterLevelRenderer {
   @Accessor("field_4112")
   class_769 anubis$pinkWaterViewArea();
}
