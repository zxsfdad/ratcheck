package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.anubis.client.module.impl.misc.TridentUtilModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1835;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(class_1835.class)
public abstract class MixinTridentItem {
   @ModifyExpressionValue(
      method = {"method_7836", "method_7840"},
      at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1657;method_5721()Z"),
      require = 0
   )
   private boolean anubis$allowOutOfWater(boolean original) {
      return original || TridentUtilModule.allowOutOfWater();
   }

   @ModifyExpressionValue(method = "method_7840", at = @At(value = "CONSTANT", args = "intValue=10"), require = 0)
   private int anubis$scaleMinCharge(int original) {
      return TridentUtilModule.minChargeTicks(original);
   }
}
