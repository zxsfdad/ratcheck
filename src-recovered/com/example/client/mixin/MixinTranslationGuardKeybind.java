package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.anubis.client.util.TranslationGuard;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_2572;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(class_2572.class)
public abstract class MixinTranslationGuardKeybind {
   @Shadow
   @Final
   private String field_11767;

   @ModifyReturnValue(method = "method_27691", at = @At("RETURN"))
   private class_2561 anubis$guardKeybind(class_2561 original) {
      return TranslationGuard.active() ? TranslationGuard.keybind(this.field_11767) : original;
   }
}
