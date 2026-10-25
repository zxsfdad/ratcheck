package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.anubis.client.util.TranslationGuard;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1703;
import net.minecraft.class_1799;
import net.minecraft.class_471;
import org.spongepowered.asm.mixin.Mixin;

@Environment(EnvType.CLIENT)
@Mixin(class_471.class)
public abstract class MixinTranslationGuardAnvilScreen {
   @WrapMethod(method = "method_7635")
   private void anubis$guardSlot(class_1703 menu, int slot, class_1799 stack, Operation<Void> original) {
      TranslationGuard.run(() -> original.call(new Object[]{menu, slot, stack}));
   }

   @WrapMethod(method = "method_2403")
   private void anubis$guardName(String name, Operation<Void> original) {
      TranslationGuard.run(() -> original.call(new Object[]{name}));
   }
}
