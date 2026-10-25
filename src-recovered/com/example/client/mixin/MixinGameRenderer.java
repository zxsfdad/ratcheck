package com.example.client.mixin;

import net.anubis.client.imgui.ImGuiManager;
import net.anubis.client.module.impl.misc.noinvleak.StreamOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1041;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_1041.class)
public abstract class MixinGameRenderer {
   @Inject(method = "method_15998", at = @At("HEAD"))
   private void anubis$beforeSwap(CallbackInfo ci) {
      StreamOverlay.endFrame();
      ImGuiManager.getInstance().renderIfActive();
   }
}
