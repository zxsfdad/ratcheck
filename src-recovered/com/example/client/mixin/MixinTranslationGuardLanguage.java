package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.anubis.client.util.TranslationGuard;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2477;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(class_2477.class)
public abstract class MixinTranslationGuardLanguage {
   @ModifyReturnValue(method = "method_10517", at = @At("RETURN"))
   private static class_2477 anubis$guardLanguage(class_2477 original) {
      return TranslationGuard.active() ? TranslationGuard.language(original) : original;
   }
}
