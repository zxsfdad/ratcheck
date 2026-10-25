package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.io.InputStream;
import java.util.List;
import java.util.function.BiConsumer;
import net.anubis.client.util.TranslationGuard;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1078;
import net.minecraft.class_3298;
import net.minecraft.class_3300;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(class_1078.class)
public abstract class MixinTranslationGuardClientLanguage {
   @Inject(method = "method_4675", at = @At("HEAD"), require = 0)
   private static void anubis$beginLoad(class_3300 resourceManager, List<String> languages, boolean rightToLeft, CallbackInfoReturnable<class_1078> cir) {
      TranslationGuard.beginLoad();
   }

   @WrapOperation(
      method = "method_4676",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/class_2477;method_29425(Ljava/io/InputStream;Ljava/util/function/BiConsumer;)V"),
      require = 0
   )
   private static void anubis$tapLoad(InputStream stream, BiConsumer<String, String> output, Operation<Void> original, @Local class_3298 resource) {
      original.call(new Object[]{stream, TranslationGuard.tap(resource, output)});
   }

   @Inject(method = "method_4675", at = @At("RETURN"), require = 0)
   private static void anubis$endLoad(class_3300 resourceManager, List<String> languages, boolean rightToLeft, CallbackInfoReturnable<class_1078> cir) {
      TranslationGuard.endLoad();
   }
}
