package com.example.client.mixin;

import net.anubis.client.module.impl.movement.BoatFlyModule;
import net.anubis.client.module.impl.movement.BoatNoclipModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2692;
import net.minecraft.class_634;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_634.class)
public abstract class MixinBoatFlyClientPacketListener {
   @Inject(
      method = "method_11134",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/class_2833;method_65307(Lnet/minecraft/class_1297;)Lnet/minecraft/class_2833;")
   )
   private void anubis$boatFlyVehicleSetback(class_2692 packet, CallbackInfo ci) {
      BoatFlyModule fly = BoatFlyModule.active;
      if (fly != null) {
         fly.onVehicleSetback();
      }

      BoatNoclipModule noclip = BoatNoclipModule.active;
      if (noclip != null) {
         noclip.onVehicleSetback();
      }
   }
}
