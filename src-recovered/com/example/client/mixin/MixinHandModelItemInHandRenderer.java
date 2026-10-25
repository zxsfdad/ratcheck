package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.anubis.client.module.impl.render.HandModelModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1007;
import net.minecraft.class_11659;
import net.minecraft.class_2960;
import net.minecraft.class_4587;
import net.minecraft.class_759;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(class_759.class)
public abstract class MixinHandModelItemInHandRenderer {
   @WrapOperation(
      method = {"method_3219", "method_3216"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_1007;method_4220(Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;ILnet/minecraft/class_2960;Z)V"
      )
   )
   private void anubis$longRightArm(
      class_1007<?> renderer, class_4587 pose, class_11659 collector, int light, class_2960 skin, boolean sleeve, Operation<Void> original
   ) {
      original.call(new Object[]{renderer, pose, collector, light, skin, sleeve});
      if (HandModelModule.active != null) {
         pose.method_22903();
         HandModelModule.shoulder(pose, true, renderer);
         original.call(new Object[]{renderer, pose, collector, light, skin, sleeve});
         pose.method_22909();
      }
   }

   @WrapOperation(
      method = {"method_3219", "method_3216"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_1007;method_4221(Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;ILnet/minecraft/class_2960;Z)V"
      )
   )
   private void anubis$longLeftArm(
      class_1007<?> renderer, class_4587 pose, class_11659 collector, int light, class_2960 skin, boolean sleeve, Operation<Void> original
   ) {
      original.call(new Object[]{renderer, pose, collector, light, skin, sleeve});
      if (HandModelModule.active != null) {
         pose.method_22903();
         HandModelModule.shoulder(pose, false, renderer);
         original.call(new Object[]{renderer, pose, collector, light, skin, sleeve});
         pose.method_22909();
      }
   }
}
