package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.anubis.client.module.impl.movement.JesusModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1309;
import net.minecraft.class_310;
import net.minecraft.class_3610;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(class_1309.class)
public abstract class MixinJesusLivingEntity {
   @ModifyReturnValue(method = "method_26319", at = @At("RETURN"))
   private boolean anubis$jesusCanStandOnFluid(boolean original, class_3610 fluidState) {
      if (original) {
         return true;
      }

      JesusModule jesus = JesusModule.active;
      return jesus != null && this == class_310.method_1551().field_1724 ? jesus.canWalkOnFluid(fluidState) : false;
   }
}
