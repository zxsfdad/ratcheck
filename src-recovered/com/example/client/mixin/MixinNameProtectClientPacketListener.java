package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.anubis.client.module.impl.misc.NameProtectModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_2703;
import net.minecraft.class_634;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_634.class)
public abstract class MixinNameProtectClientPacketListener {
   @Inject(method = "method_11113", at = @At("TAIL"))
   private void anubis$nameProtectPlayerList(class_2703 packet, CallbackInfo ci) {
      NameProtectModule.onPlayerInfoApplied(packet);
   }

   @ModifyExpressionValue(method = "method_34075", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_5892;comp_2276()Lnet/minecraft/class_2561;"))
   private class_2561 anubis$nameProtectDeathMessage(class_2561 message) {
      return NameProtectModule.protect(message);
   }
}
