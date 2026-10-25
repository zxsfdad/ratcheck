package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.anubis.client.module.impl.movement.ElytraFlyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_310;
import net.minecraft.class_4050;
import net.minecraft.class_746;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(class_1297.class)
public abstract class MixinElytraFlyEntity {
   @ModifyReturnValue(method = "method_18376", at = @At("RETURN"))
   private class_4050 anubis$elytraFlyGetPose(class_4050 original) {
      ElytraFlyModule module = ElytraFlyModule.active;
      if (module == null) {
         return original;
      } else {
         class_310 mc = class_310.method_1551();
         class_746 player = mc.field_1724;
         if (this == player && mc.method_18854()) {
            return module.canPacketEfly(player) ? class_4050.field_18077 : original;
         } else {
            return original;
         }
      }
   }
}
