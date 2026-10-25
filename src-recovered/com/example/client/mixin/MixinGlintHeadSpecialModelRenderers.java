package com.example.client.mixin;

import com.mojang.serialization.MapCodec;
import net.anubis.client.gui.GlintHeadRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10517;
import net.minecraft.class_2960;
import net.minecraft.class_10515.class_10516;
import net.minecraft.class_5699.class_10388;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_10517.class)
public abstract class MixinGlintHeadSpecialModelRenderers {
   @Shadow
   @Final
   private static class_10388<class_2960, MapCodec<? extends class_10516>> field_55453;

   @Inject(method = "method_65712", at = @At("TAIL"))
   private static void anubis$registerGlintHead(CallbackInfo ci) {
      field_55453.method_65325(GlintHeadRenderer.ID, GlintHeadRenderer.Unbaked.MAP_CODEC);
   }
}
