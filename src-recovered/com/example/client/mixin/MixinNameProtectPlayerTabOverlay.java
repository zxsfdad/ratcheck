package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.anubis.client.module.impl.misc.NameProtectModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_355;
import net.minecraft.class_5348;
import net.minecraft.class_640;
import net.minecraft.class_8685;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin(class_355.class)
public abstract class MixinNameProtectPlayerTabOverlay {
   @ModifyReturnValue(method = "method_1918", at = @At("RETURN"))
   private class_2561 anubis$nameProtectTabName(class_2561 original) {
      return NameProtectModule.protect(original);
   }

   @ModifyArg(
      method = "method_1919",
      at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/class_327;method_1728(Lnet/minecraft/class_5348;I)Ljava/util/List;"),
      index = 0
   )
   private class_5348 anubis$nameProtectTabHeader(class_5348 header) {
      return NameProtectModule.protectText(header);
   }

   @ModifyArg(
      method = "method_1919",
      at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/class_327;method_1728(Lnet/minecraft/class_5348;I)Ljava/util/List;"),
      index = 0
   )
   private class_5348 anubis$nameProtectTabFooter(class_5348 footer) {
      return NameProtectModule.protectText(footer);
   }

   @WrapOperation(method = "method_1919", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_640;method_52810()Lnet/minecraft/class_8685;"))
   private class_8685 anubis$nameProtectTabHead(class_640 info, Operation<class_8685> original) {
      class_8685 real = (class_8685)original.call(new Object[]{info});
      return NameProtectModule.hidesName(info.method_2966().name()) ? NameProtectModule.hiddenHead(info.method_2966().id(), real) : real;
   }
}
