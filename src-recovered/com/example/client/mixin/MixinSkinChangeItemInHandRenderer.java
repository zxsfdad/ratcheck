package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.misc.SkinChangeModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1007;
import net.minecraft.class_11659;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_742;
import net.minecraft.class_746;
import net.minecraft.class_759;
import net.minecraft.class_8685;
import net.minecraft.class_898;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(class_759.class)
public abstract class MixinSkinChangeItemInHandRenderer {
   @Shadow
   @Final
   private class_310 field_4050;
   @Shadow
   @Final
   private class_898 field_4046;

   @WrapOperation(
      method = {"method_3219", "method_3216"},
      at = {
            @At(
               value = "INVOKE",
               target = "Lnet/minecraft/class_1007;method_4220(Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;ILnet/minecraft/class_2960;Z)V"
            ),
            @At(
               value = "INVOKE",
               target = "Lnet/minecraft/class_1007;method_4221(Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;ILnet/minecraft/class_2960;Z)V"
            )
      }
   )
   private void anubis$skinChangeArm(
      class_1007<?> renderer, class_4587 poseStack, class_11659 collector, int light, class_2960 texture, boolean sleeve, Operation<Void> original
   ) {
      SkinChangeModule mod = ModuleRegistry.get(SkinChangeModule.class);
      class_746 player = this.field_4050.field_1724;
      if (mod != null && mod.isEnabled() && player != null) {
         class_8685 own = player.method_52814();
         class_8685 skin = mod.skinFor(player.method_5628(), own);
         if (skin != own) {
            texture = skin.comp_1626().comp_3627();
            class_1007<class_742> byModel = ((AccessorSkinChangeEntityRenderDispatcher)this.field_4046)
               .anubis$skinChangePlayerRenderers()
               .get(skin.comp_1629());
            if (byModel != null) {
               renderer = byModel;
            }
         }
      }

      original.call(new Object[]{renderer, poseStack, collector, light, texture, sleeve});
   }
}
