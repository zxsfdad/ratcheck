package com.example.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.anubis.client.module.impl.movement.SpeedModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1313;
import net.minecraft.class_243;
import net.minecraft.class_746;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(class_746.class)
public abstract class MixinSpeedLocalPlayer {
   @ModifyVariable(method = "method_5784", at = @At("HEAD"), argsOnly = true)
   private class_243 anubis$speedMove(class_243 movement, @Local(argsOnly = true) class_1313 type) {
      SpeedModule speed = SpeedModule.active;
      return speed != null && type == class_1313.field_6308 ? speed.onSelfMove((class_746)this, movement) : movement;
   }
}
