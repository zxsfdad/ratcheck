package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.anubis.client.module.impl.misc.NoInvLeakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10017;
import net.minecraft.class_1309;
import net.minecraft.class_490;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(class_490.class)
public abstract class MixinNoInvLeakInventoryModel {
   @WrapOperation(
      method = "method_2486",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/class_490;method_48472(Lnet/minecraft/class_1309;)Lnet/minecraft/class_10017;")
   )
   private static class_10017 anubis$noInvLeakModel(class_1309 entity, Operation<class_10017> original) {
      if (!NoInvLeakModule.hidesModelOf(entity)) {
         return (class_10017)original.call(new Object[]{entity});
      }

      NoInvLeakModule.bareModel(true);

      try {
         return (class_10017)original.call(new Object[]{entity});
      } finally {
         NoInvLeakModule.bareModel(false);
      }
   }
}
