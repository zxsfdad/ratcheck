package com.example.client.mixin;

import net.anubis.client.module.impl.render.XrayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_778;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin(class_778.class)
public abstract class MixinModelBlockRenderer {
   @ModifyArg(
      method = "method_23073",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/class_4588;method_22920(Lnet/minecraft/class_4587$class_4665;Lnet/minecraft/class_777;[FFFFF[II)V"),
      index = 6
   )
   private float anubis$xrayAlpha(float alpha) {
      return XrayModule.active == null ? alpha : alpha * XrayModule.CURRENT_ALPHA.get()[0];
   }
}
