package com.example.client.mixin;

import net.anubis.client.gui.AnubisTitleScreen;
import net.anubis.client.gui.mainmenu.DuskBackdrop;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;
import net.minecraft.class_437;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_437.class)
public abstract class MixinDuskBackdropScreen {
   @Shadow
   public int field_22789;
   @Shadow
   public int field_22790;

   @Inject(method = "method_57728", at = @At("HEAD"), cancellable = true)
   private void anubis$duskBackdrop(class_332 graphics, float partialTick, CallbackInfo ci) {
      if (AnubisTitleScreen.enabled()) {
         ci.cancel();
         DuskBackdrop.draw(graphics, this.field_22789, this.field_22790);
         graphics.method_71048();
      }
   }
}
