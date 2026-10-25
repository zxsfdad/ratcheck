package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.anubis.client.module.impl.misc.FreecamModule;
import net.anubis.client.module.impl.render.FreelookModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2338;
import net.minecraft.class_4184;
import net.minecraft.class_4604;
import net.minecraft.class_761;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(class_761.class)
public abstract class MixinFreecamLevelRenderer {
   @Inject(method = "method_40050", at = @At("HEAD"), cancellable = true)
   private void anubis$freecamEntitiesReady(class_2338 pos, CallbackInfoReturnable<Boolean> cir) {
      if (FreecamModule.active != null) {
         cir.setReturnValue(true);
      }
   }

   @WrapOperation(
      method = "method_22710",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/class_761;method_74752(Lnet/minecraft/class_4184;Lnet/minecraft/class_4604;Z)V")
   )
   private void anubis$freelookCullInsideBlocks(class_761 renderer, class_4184 camera, class_4604 frustum, boolean spectator, Operation<Void> original) {
      original.call(new Object[]{renderer, camera, frustum, spectator || camera.method_19333() && FreelookModule.throughBlocks()});
   }
}
