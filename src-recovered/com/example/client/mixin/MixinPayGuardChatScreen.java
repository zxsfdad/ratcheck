package com.example.client.mixin;

import net.anubis.client.security.PayGuard;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11908;
import net.minecraft.class_342;
import net.minecraft.class_408;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(class_408.class)
public abstract class MixinPayGuardChatScreen {
   @Shadow
   protected class_342 field_2382;

   @Inject(method = "method_25404", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_408;method_44056(Ljava/lang/String;Z)V"))
   private void anubis$allowTypedPay(class_11908 event, CallbackInfoReturnable<Boolean> cir) {
      PayGuard.allowTyped(this.field_2382.method_1882());
   }
}
