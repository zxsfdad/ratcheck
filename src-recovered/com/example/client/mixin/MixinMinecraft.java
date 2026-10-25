package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.anubis.client.event.EventBus;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.gui.AnubisTitleScreen;
import net.anubis.client.module.impl.misc.AutoEatModule;
import net.anubis.client.util.PerformanceBudget;
import net.anubis.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_155;
import net.minecraft.class_310;
import net.minecraft.class_437;
import net.minecraft.class_442;
import net.minecraft.class_638;
import net.minecraft.class_239.class_240;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(class_310.class)
public abstract class MixinMinecraft {
   @Shadow
   public class_638 field_1687;

   @Shadow
   public abstract void method_1507(class_437 var1);

   @Inject(method = "method_1507", at = @At("HEAD"), cancellable = true)
   private void anubis$replaceTitleScreen(class_437 screen, CallbackInfo ci) {
      if (!(screen instanceof AnubisTitleScreen)) {
         boolean title = screen instanceof class_442 || screen == null && this.field_1687 == null;
         if (title && AnubisTitleScreen.enabled()) {
            ci.cancel();
            this.method_1507(new AnubisTitleScreen());
         }
      }
   }

   @Inject(method = "method_24287", at = @At("HEAD"), cancellable = true)
   private void anubis$windowTitle(CallbackInfoReturnable<String> cir) {
      if (AnubisTitleScreen.enabled()) {
         cir.setReturnValue("Minecraft " + class_155.method_16673().comp_4025() + " - Anubis Client");
      }
   }

   @Inject(method = "method_1574", at = @At("HEAD"))
   private void anubis$onTickStart(CallbackInfo ci) {
      PerformanceBudget.beginTick();
      EventBus.INSTANCE.drainAsync();
      ServerRotation.startTick();
      TickEvent.dispatch();
   }

   @Inject(method = "method_1574", at = @At("RETURN"))
   private void anubis$onTickEnd(CallbackInfo ci) {
      TickEvent.dispatchPost();
   }

   @ModifyExpressionValue(
      method = "method_1583",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/class_239;method_17783()Lnet/minecraft/class_239$class_240;")
   )
   private class_240 anubis$autoEatIgnoreCrosshair(class_240 type) {
      return AutoEatModule.isEating() ? class_240.field_1333 : type;
   }
}
