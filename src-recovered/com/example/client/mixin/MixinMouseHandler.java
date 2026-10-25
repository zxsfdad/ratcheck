package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.anubis.client.event.impl.MouseInputEvent;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.combat.AimAssistModule;
import net.anubis.client.module.impl.misc.FreecamModule;
import net.anubis.client.module.impl.render.FreelookModule;
import net.anubis.client.module.impl.render.ZoomModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11910;
import net.minecraft.class_310;
import net.minecraft.class_312;
import net.minecraft.class_746;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_312.class)
public abstract class MixinMouseHandler {
   @Inject(method = "method_1601", at = @At("HEAD"), cancellable = true)
   private void anubis$onMouseButton(long window, class_11910 buttonInfo, int action, CallbackInfo ci) {
      if (MouseInputEvent.dispatch(window, buttonInfo.comp_4801(), action, buttonInfo.comp_4797())) {
         ci.cancel();
      }
   }

   @Inject(method = "method_1598", at = @At("HEAD"), cancellable = true)
   private void anubis$wheel(long window, double horizontal, double vertical, CallbackInfo ci) {
      class_310 mc = class_310.method_1551();
      if (vertical != 0.0 && window == mc.method_22683().method_4490() && mc.field_1755 == null && mc.method_18506() == null) {
         if (ZoomModule.scroll(vertical) || FreecamModule.scroll(vertical)) {
            ci.cancel();
         }
      }
   }

   @WrapOperation(method = "method_1606", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_746;method_5872(DD)V"))
   private void anubis$cameraTurn(class_746 player, double yRot, double xRot, Operation<Void> original, @Local(argsOnly = true) double movementTime) {
      double scale = ZoomModule.turnScale();
      yRot *= scale;
      xRot *= scale;
      FreecamModule freecam = FreecamModule.active;
      if (freecam != null) {
         freecam.turn(yRot, xRot);
      } else {
         FreelookModule freelook = ModuleRegistry.get(FreelookModule.class);
         if (freelook != null && freelook.cameraMode()) {
            freelook.turnCamera(yRot, xRot);
         } else {
            AimAssistModule assist = ModuleRegistry.get(AimAssistModule.class);
            if (assist != null && assist.isEnabled()) {
               double[] turn = assist.nudge(player, yRot, xRot, movementTime);
               yRot += turn[0];
               xRot += turn[1];
            }

            original.call(new Object[]{player, yRot, xRot});
         }
      }
   }
}
