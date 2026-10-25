package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_636;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(class_636.class)
public interface AccessorElytraFlyMultiPlayerGameMode {
   @Invoker("method_2911")
   void anubis$elytraFlySyncSelected();
}
