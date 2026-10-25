package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.anubis.client.module.impl.misc.FakeStats;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_266;
import net.minecraft.class_269;
import net.minecraft.class_355;
import net.minecraft.class_5348;
import net.minecraft.class_640;
import net.minecraft.class_9013;
import net.minecraft.class_9015;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin(class_355.class)
public abstract class MixinFakeStatsPlayerTabOverlay {
   @ModifyReturnValue(method = "method_1918", at = @At("RETURN"))
   private class_2561 anubis$fakeStatsTabName(class_2561 original, @Local(argsOnly = true) class_640 info) {
      return FakeStats.tabName(info.method_2966().id(), original);
   }

   @WrapOperation(
      method = "method_1919",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_269;method_55430(Lnet/minecraft/class_9015;Lnet/minecraft/class_266;)Lnet/minecraft/class_9013;"
      )
   )
   private class_9013 anubis$fakeStatsTabScore(class_269 board, class_9015 holder, class_266 objective, Operation<class_9013> original) {
      return FakeStats.tabScore(holder, (class_9013)original.call(new Object[]{board, holder, objective}));
   }

   @ModifyArg(
      method = "method_1919",
      at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/class_327;method_1728(Lnet/minecraft/class_5348;I)Ljava/util/List;"),
      index = 0
   )
   private class_5348 anubis$fakeStatsTabHeader(class_5348 header) {
      return FakeStats.tabText(header);
   }

   @ModifyArg(
      method = "method_1919",
      at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/class_327;method_1728(Lnet/minecraft/class_5348;I)Ljava/util/List;"),
      index = 0
   )
   private class_5348 anubis$fakeStatsTabFooter(class_5348 footer) {
      return FakeStats.tabText(footer);
   }
}
