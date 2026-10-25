package com.example.client.mixin;

import net.anubis.client.module.impl.render.JumpCirclesModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1309;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_1309.class)
public abstract class MixinJumpCirclesLivingEntity {
   @Inject(method = "method_6043", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1309;method_18800(DDD)V"))
   private void anubis$jumpCircle(CallbackInfo ci) {
      if (JumpCirclesModule.active != null) {
         JumpCirclesModule.onJump((class_1309)this);
      }
   }
}
