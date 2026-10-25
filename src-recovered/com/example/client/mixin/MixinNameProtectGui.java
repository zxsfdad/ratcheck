package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.anubis.client.module.impl.misc.NameProtectModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_329;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(class_329.class)
public abstract class MixinNameProtectGui {
   @ModifyVariable(method = "method_34004", at = @At("HEAD"), argsOnly = true)
   private class_2561 anubis$nameProtectTitle(class_2561 title) {
      return NameProtectModule.protect(title);
   }

   @ModifyVariable(method = "method_34002", at = @At("HEAD"), argsOnly = true)
   private class_2561 anubis$nameProtectSubtitle(class_2561 subtitle) {
      return NameProtectModule.protect(subtitle);
   }

   @ModifyVariable(method = "method_1758", at = @At("HEAD"), argsOnly = true)
   private class_2561 anubis$nameProtectActionBar(class_2561 message) {
      return NameProtectModule.protect(message);
   }

   @ModifyExpressionValue(method = "method_1757", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_266;method_1114()Lnet/minecraft/class_2561;"))
   private class_2561 anubis$nameProtectSidebarTitle(class_2561 original) {
      return NameProtectModule.protect(original);
   }

   @ModifyExpressionValue(
      method = "method_1757",
      at = @At(value = "FIELD", opcode = 180, ordinal = 0, target = "Lnet/minecraft/class_329$class_9016;comp_2131:Lnet/minecraft/class_2561;")
   )
   private class_2561 anubis$nameProtectSidebarLineWidth(class_2561 original) {
      return NameProtectModule.protect(original);
   }

   @ModifyExpressionValue(
      method = "method_1757",
      at = @At(value = "FIELD", opcode = 180, ordinal = 1, target = "Lnet/minecraft/class_329$class_9016;comp_2131:Lnet/minecraft/class_2561;")
   )
   private class_2561 anubis$nameProtectSidebarLineDraw(class_2561 original) {
      return NameProtectModule.protect(original);
   }
}
