package com.example.client.mixin;

import net.anubis.client.module.impl.movement.entitycontrol.EntityControlVehicleHook;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1313;
import net.minecraft.class_243;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(class_1297.class)
public abstract class MixinEntityControlEntity {
   @ModifyVariable(method = "method_5784", at = @At("HEAD"), argsOnly = true)
   private class_243 anubis$entityControlVehicleMove(class_243 movement, class_1313 type) {
      return !EntityControlVehicleHook.anyActive ? movement : EntityControlVehicleHook.onEntityMove((class_1297)this, type, movement);
   }
}
