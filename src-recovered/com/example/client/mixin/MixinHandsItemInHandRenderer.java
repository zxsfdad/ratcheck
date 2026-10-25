package com.example.client.mixin;

import net.anubis.client.module.impl.render.HandsModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11659;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1799;
import net.minecraft.class_4587;
import net.minecraft.class_742;
import net.minecraft.class_759;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_759.class)
public abstract class MixinHandsItemInHandRenderer {
   @Inject(method = "method_3228", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_4587;method_22903()V", shift = Shift.AFTER))
   private void anubis$handsPose(
      class_742 player,
      float partialTick,
      float pitch,
      class_1268 hand,
      float swing,
      class_1799 stack,
      float equip,
      class_4587 poseStack,
      class_11659 collector,
      int light,
      CallbackInfo ci
   ) {
      if (HandsModule.live != null) {
         boolean mainHand = hand == class_1268.field_5808;
         class_1306 arm = mainHand ? player.method_6068() : player.method_6068().method_5928();
         HandsModule.shape(poseStack, mainHand, arm == class_1306.field_6183 ? 1 : -1);
      }
   }
}
