package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_4970;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(class_4970.class)
public interface AccessorElytraFlyBlockBehaviour {
   @Accessor("field_23159")
   boolean anubis$elytraFlyHasCollision();
}
