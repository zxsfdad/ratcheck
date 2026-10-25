package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.anubis.client.module.impl.movement.JesusModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_310;
import net.minecraft.class_5635;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(class_5635.class)
public abstract class MixinJesusPowderSnowBlock {
   @ModifyReturnValue(method = "method_32355", at = @At("RETURN"))
   private static boolean anubis$jesusWalkOnPowderSnow(boolean original, class_1297 entity) {
      if (original) {
         return true;
      }

      JesusModule jesus = JesusModule.active;
      return jesus != null && entity == class_310.method_1551().field_1724 && jesus.canWalkOnPowderSnow();
   }
}
