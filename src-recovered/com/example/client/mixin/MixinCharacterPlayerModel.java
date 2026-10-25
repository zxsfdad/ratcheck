package com.example.client.mixin;

import net.anubis.client.module.impl.render.character.CharacterLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10055;
import net.minecraft.class_1306;
import net.minecraft.class_4587;
import net.minecraft.class_591;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_591.class)
public abstract class MixinCharacterPlayerModel {
   @Inject(method = "method_72859", at = @At("HEAD"), cancellable = true)
   private void anubis$characterHand(class_10055 state, class_1306 arm, class_4587 poseStack, CallbackInfo ci) {
      if (CharacterLayer.translateToHand((class_591)this, state, arm, poseStack)) {
         ci.cancel();
      }
   }
}
