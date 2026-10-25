package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1917;
import net.minecraft.class_1952;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(class_1917.class)
public interface AccessorSpawnerNametagsBaseSpawner {
   @Accessor("field_9155")
   class_1952 anubis$spawnerNametagsNextSpawnData();

   @Accessor("field_9158")
   int anubis$spawnerNametagsRequiredPlayerRange();
}
