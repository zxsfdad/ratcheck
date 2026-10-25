package com.example.client.mixin;

import net.anubis.client.module.impl.render.character.CharacterLayer;
import net.anubis.client.module.impl.render.character.CharacterModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10055;
import net.minecraft.class_1007;
import net.minecraft.class_11659;
import net.minecraft.class_11890;
import net.minecraft.class_11901;
import net.minecraft.class_2960;
import net.minecraft.class_4587;
import net.minecraft.class_591;
import net.minecraft.class_630;
import net.minecraft.class_922;
import net.minecraft.class_5617.class_5618;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_1007.class)
public abstract class MixinCharacterAvatarRenderer<T extends class_11890 & class_11901> extends class_922<T, class_10055, class_591> {
   protected MixinCharacterAvatarRenderer(class_5618 context, class_591 model, float shadowRadius) {
      super(context, model, shadowRadius);
   }

   @Inject(method = "<init>", at = @At("TAIL"))
   private void anubis$addCharacterLayer(class_5618 context, boolean slim, CallbackInfo ci) {
      this.method_4046(new CharacterLayer(this));
   }

   @Inject(method = "method_62604", at = @At("TAIL"))
   private void anubis$extractCharacter(class_11890 entity, class_10055 state, float partialTick, CallbackInfo ci) {
      CharacterModule.extract(entity, state);
   }

   @Inject(method = "method_23205", at = @At("HEAD"), cancellable = true)
   private void anubis$characterFirstPersonHand(
      class_4587 poseStack, class_11659 collector, int light, class_2960 skin, class_630 arm, boolean sleeve, CallbackInfo ci
   ) {
      if (CharacterLayer.firstPersonHand((class_591)this.method_4038(), poseStack, collector, light, arm)) {
         ci.cancel();
      }
   }
}
