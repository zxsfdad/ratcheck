package com.example.client.mixin;

import net.anubis.client.module.impl.movement.SneakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10185;
import net.minecraft.class_743;
import net.minecraft.class_744;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_743.class)
public abstract class MixinSneakKeyboardInput {
   @Inject(method = "method_3129", at = @At("TAIL"))
   private void anubis$forceSneak(CallbackInfo ci) {
      SneakModule sneak = SneakModule.active;
      if (sneak != null && sneak.doVanilla()) {
         class_744 input = (class_744)this;
         class_10185 keys = input.field_54155;
         if (!keys.comp_3164()) {
            input.field_54155 = new class_10185(
               keys.comp_3159(), keys.comp_3160(), keys.comp_3161(), keys.comp_3162(), keys.comp_3163(), true, keys.comp_3165()
            );
         }
      }
   }
}
