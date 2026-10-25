package com.example.client.mixin;

import net.anubis.client.module.impl.render.XrayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2248;
import net.minecraft.class_2350;
import net.minecraft.class_2680;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(class_2248.class)
public abstract class MixinBlock {
   @Inject(method = "method_9607", at = @At("HEAD"), cancellable = true)
   private static void anubis$xrayForceFace(class_2680 state, class_2680 neighborState, class_2350 direction, CallbackInfoReturnable<Boolean> cir) {
      XrayModule.Snapshot snapshot = XrayModule.active;
      if (snapshot != null && snapshot.isWhitelisted(state.method_26204()) && XrayModule.CURRENT_ALPHA.get()[0] >= 1.0F) {
         cir.setReturnValue(true);
      }
   }
}
