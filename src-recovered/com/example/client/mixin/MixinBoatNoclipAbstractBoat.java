package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.anubis.client.module.impl.movement.BoatNoclipModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10255;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(class_10255.class)
public abstract class MixinBoatNoclipAbstractBoat {
   @ModifyReturnValue(method = "method_30949", at = @At("RETURN"))
   private boolean anubis$boatNoclipCollide(boolean original) {
      if (original && BoatNoclipModule.active != null) {
         class_10255 boat = (class_10255)this;
         return !boat.method_73183().method_8608() || boat.method_5642() != class_310.method_1551().field_1724;
      } else {
         return original;
      }
   }
}
