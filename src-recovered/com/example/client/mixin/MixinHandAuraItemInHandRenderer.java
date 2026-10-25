package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.anubis.client.module.impl.render.HandAuraModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11684;
import net.minecraft.class_310;
import net.minecraft.class_759;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(class_759.class)
public abstract class MixinHandAuraItemInHandRenderer {
   @WrapOperation(method = "method_22976", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_11684;method_73002()V"))
   private void anubis$handAura(class_11684 features, Operation<Void> original) {
      if (HandAuraModule.active != null && HandAuraModule.beginCapture()) {
         boolean drawn = false;

         try {
            original.call(new Object[]{features});
            class_310.method_1551().method_22940().method_23000().method_22993();
            drawn = true;
         } finally {
            HandAuraModule.endCapture(drawn);
         }
      } else {
         original.call(new Object[]{features});
      }
   }
}
