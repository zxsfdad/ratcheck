package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.anubis.client.module.impl.movement.TridentBoostModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1657;
import net.minecraft.class_1835;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(class_1835.class)
public abstract class MixinTridentBoostTridentItem {
   @WrapOperation(method = "method_7840", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1657;method_5762(DDD)V"), require = 0)
   private void anubis$tridentBoostPush(class_1657 player, double x, double y, double z, Operation<Void> original) {
      double multiplier = TridentBoostModule.riptideMultiplier();
      original.call(new Object[]{player, x * multiplier, y * multiplier, z * multiplier});
   }

   @ModifyExpressionValue(method = "method_7836", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1657;method_5721()Z"), require = 0)
   private boolean anubis$tridentBoostUseOutOfWater(boolean original) {
      return original || TridentBoostModule.allowOutOfWater();
   }

   @ModifyExpressionValue(method = "method_7840", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1657;method_5721()Z"), require = 0)
   private boolean anubis$tridentBoostReleaseOutOfWater(boolean original) {
      return original || TridentBoostModule.allowOutOfWater();
   }
}
