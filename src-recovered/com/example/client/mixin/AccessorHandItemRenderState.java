package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10444;
import net.minecraft.class_10444.class_10446;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(class_10444.class)
public interface AccessorHandItemRenderState {
   @Accessor("field_55340")
   class_10446[] anubis$handLayers();

   @Accessor("field_55339")
   int anubis$handLayerCount();
}
