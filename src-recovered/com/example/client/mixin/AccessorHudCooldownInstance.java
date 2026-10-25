package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(targets = "net/minecraft/class_1796$class_1797")
public interface AccessorHudCooldownInstance {
   @Accessor("comp_3083")
   int anubis$hudStartTime();

   @Accessor("comp_3084")
   int anubis$hudEndTime();
}
