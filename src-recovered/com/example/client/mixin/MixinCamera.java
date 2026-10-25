package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.misc.FreecamModule;
import net.anubis.client.module.impl.render.FreelookModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1937;
import net.minecraft.class_4184;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_4184.class)
public abstract class MixinCamera {
   @Shadow
   protected abstract void method_19325(float var1, float var2);

   @Shadow
   protected abstract void method_19327(double var1, double var3, double var5);

   @WrapOperation(method = "method_19321", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_4184;method_19325(FF)V"))
   private void anubis$freelookRotation(class_4184 camera, float yRot, float xRot, Operation<Void> original) {
      FreelookModule freelook = ModuleRegistry.get(FreelookModule.class);
      if (FreecamModule.active == null && freelook != null && freelook.isEnabled()) {
         original.call(new Object[]{camera, freelook.cameraYaw(), freelook.cameraPitch()});
      } else {
         original.call(new Object[]{camera, yRot, xRot});
      }
   }

   @WrapOperation(method = "method_19321", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_4184;method_19318(F)F"))
   private float anubis$freelookThroughBlocks(class_4184 camera, float distance, Operation<Float> original) {
      return FreelookModule.throughBlocks() ? distance : (Float)original.call(new Object[]{camera, distance});
   }

   @Inject(method = "method_19321", at = @At("RETURN"))
   private void anubis$freecamCamera(class_1937 level, class_1297 entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
      FreecamModule freecam = FreecamModule.active;
      if (freecam != null) {
         this.method_19327(freecam.getX(partialTick), freecam.getY(partialTick), freecam.getZ(partialTick));
         this.method_19325(freecam.getYaw(), freecam.getPitch());
      }
   }
}
