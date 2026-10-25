package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.anubis.client.module.impl.render.SwingAnimationModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_4587;
import net.minecraft.class_759;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(class_759.class)
public abstract class MixinSwingAnimationItemInHandRenderer {
   @WrapOperation(
      method = "method_3228",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/class_759;method_65816(FLnet/minecraft/class_4587;ILnet/minecraft/class_1306;)V")
   )
   private void anubis$swingStyle(
      class_759 renderer, float progress, class_4587 poseStack, int side, class_1306 arm, Operation<Void> original, @Local(argsOnly = true) class_1268 hand
   ) {
      SwingAnimationModule module = SwingAnimationModule.active;
      if (module != null && module.covers(hand == class_1268.field_5808)) {
         module.pose(poseStack, progress, side);
      } else {
         original.call(new Object[]{renderer, progress, poseStack, side, arm});
      }
   }
}
