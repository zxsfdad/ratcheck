package com.example.client.mixin;

import net.anubis.client.module.impl.misc.SpamModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2678;
import net.minecraft.class_634;
import net.minecraft.class_638;
import net.minecraft.class_8588;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_634.class)
public abstract class MixinSpamClientPacketListener {
   @Shadow
   private class_638 field_3699;

   @Inject(
      method = "method_11120",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$spamRelogin(class_2678 packet, CallbackInfo ci) {
      if (SpamModule.active != null && this.field_3699 != null) {
         SpamModule.onGameLeft();
      }
   }

   @Inject(
      method = "method_52798",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$spamReconfiguration(class_8588 packet, CallbackInfo ci) {
      if (SpamModule.active != null) {
         SpamModule.onGameLeft();
      }
   }
}
