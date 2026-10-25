package com.example.client.mixin;

import net.anubis.client.module.impl.render.JumpCirclesModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11658;
import net.minecraft.class_4587;
import net.minecraft.class_761;
import net.minecraft.class_4597.class_4598;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_761.class)
public abstract class MixinJumpCirclesLevelRenderer {
   @Inject(method = "method_62206", at = @At("HEAD"))
   private void anubis$jumpCircles(class_4587 poseStack, class_4598 crumblingBuffers, class_11658 levelRenderState, CallbackInfo ci) {
      if (JumpCirclesModule.active != null) {
         JumpCirclesModule.renderLevel(levelRenderState.field_63082);
      }
   }
}
