package com.example.client.mixin;

import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.misc.SkinChangeModule;
import net.anubis.client.module.impl.render.NametagsModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10055;
import net.minecraft.class_1007;
import net.minecraft.class_11659;
import net.minecraft.class_11890;
import net.minecraft.class_12075;
import net.minecraft.class_4587;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_1007.class)
public abstract class MixinPlayerRenderer {
   @Inject(method = "method_62604", at = @At("TAIL"))
   private void anubis$injectSkin(class_11890 entity, class_10055 state, float partialTick, CallbackInfo ci) {
      SkinChangeModule mod = ModuleRegistry.get(SkinChangeModule.class);
      if (mod != null && mod.isEnabled()) {
         state.field_53520 = mod.skinFor(state.field_53528, state.field_53520);
      }
   }

   @Inject(method = "method_4213", at = @At("HEAD"), cancellable = true)
   private void anubis$suppressVanillaNameTag(class_10055 state, class_4587 poseStack, class_11659 collector, class_12075 cameraState, CallbackInfo ci) {
      if (NametagsModule.hideVanillaNametags()) {
         ci.cancel();
      }
   }
}
