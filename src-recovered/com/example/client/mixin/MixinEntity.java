package com.example.client.mixin;

import net.anubis.client.module.impl.misc.FreecamModule;
import net.anubis.client.module.impl.render.EspModule;
import net.anubis.client.module.impl.render.ItemEspModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(class_1297.class)
public class MixinEntity {
   @Inject(method = "method_5851", at = @At("HEAD"), cancellable = true)
   private void anubis$isCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
      class_1297 self = (class_1297)this;
      if (self.method_73183().method_8608()) {
         if (EspModule.shouldGlow(self) || ItemEspModule.shouldGlow(self)) {
            cir.setReturnValue(true);
         }
      }
   }

   @Inject(method = "method_22861", at = @At("HEAD"), cancellable = true)
   private void anubis$getTeamColor(CallbackInfoReturnable<Integer> cir) {
      class_1297 self = (class_1297)this;
      if (self.method_73183().method_8608()) {
         if (EspModule.shouldGlow(self)) {
            cir.setReturnValue(EspModule.glowColor(self));
         } else if (ItemEspModule.shouldGlow(self)) {
            cir.setReturnValue(ItemEspModule.glowColor(self));
         }
      }
   }

   @Inject(method = "method_5756", at = @At("HEAD"), cancellable = true)
   private void anubis$freecamOwnBody(class_1657 viewer, CallbackInfoReturnable<Boolean> cir) {
      FreecamModule freecam = FreecamModule.active;
      if (freecam != null && freecam.drawsSelf((class_1297)this, viewer)) {
         cir.setReturnValue(false);
      }
   }
}
