package com.example.client.mixin;

import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.render.BlockOverlayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_761;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_761.class)
public abstract class MixinBlockOverlayLevelRenderer {
   @Inject(method = "method_62210", at = @At("HEAD"), cancellable = true)
   private void anubis$hideVanillaOutline(CallbackInfo ci) {
      BlockOverlayModule module = ModuleRegistry.get(BlockOverlayModule.class);
      if (module != null && module.isEnabled()) {
         ci.cancel();
      }
   }
}
