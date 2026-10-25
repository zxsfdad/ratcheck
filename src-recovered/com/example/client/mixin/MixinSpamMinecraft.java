package com.example.client.mixin;

import net.anubis.client.module.impl.misc.SpamModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_419;
import net.minecraft.class_437;
import net.minecraft.class_638;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_310.class)
public abstract class MixinSpamMinecraft {
   @Shadow
   public class_638 field_1687;

   @Inject(method = "method_18096", at = @At("HEAD"))
   private void anubis$spamGameLeft(class_437 screen, boolean keepResourcePacks, boolean stopSound, CallbackInfo ci) {
      if (SpamModule.active != null && this.field_1687 != null) {
         SpamModule.onGameLeft();
      }
   }

   @Inject(method = "method_1507", at = @At("HEAD"))
   private void anubis$spamDisconnectedScreen(class_437 screen, CallbackInfo ci) {
      if (SpamModule.active != null && this.field_1687 != null && screen instanceof class_419) {
         SpamModule.onDisconnectedScreen();
      }
   }
}
