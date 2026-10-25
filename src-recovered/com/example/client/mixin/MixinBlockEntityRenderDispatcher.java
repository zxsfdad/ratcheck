package com.example.client.mixin;

import net.anubis.client.module.impl.render.XrayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11954;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_824;
import net.minecraft.class_11683.class_11792;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(class_824.class)
public abstract class MixinBlockEntityRenderDispatcher {
   @Inject(method = "method_74348", at = @At("HEAD"), cancellable = true)
   private void anubis$xraySkipHiddenBlockEntity(class_2586 blockEntity, float partialTick, class_11792 overlay, CallbackInfoReturnable<class_11954> cir) {
      XrayModule.Snapshot snapshot = XrayModule.active;
      if (snapshot != null) {
         class_1937 level = blockEntity.method_10997();
         if (level != null) {
            class_2338 pos = blockEntity.method_11016();
            class_2680 state = blockEntity.method_11010();
            if (snapshot.alphaFor(level, state, pos) <= 0.0F) {
               cir.setReturnValue(null);
            }
         }
      }
   }
}
