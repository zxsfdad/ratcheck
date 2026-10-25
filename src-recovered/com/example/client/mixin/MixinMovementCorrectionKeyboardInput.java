package com.example.client.mixin;

import net.anubis.client.util.pvp.InputControl;
import net.anubis.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10185;
import net.minecraft.class_241;
import net.minecraft.class_310;
import net.minecraft.class_743;
import net.minecraft.class_744;
import net.minecraft.class_746;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_743.class)
public abstract class MixinMovementCorrectionKeyboardInput {
   @Inject(method = "method_3129", at = @At("TAIL"))
   private void anubis$shapeInput(CallbackInfo ci) {
      class_744 input = (class_744)this;
      class_746 player = class_310.method_1551().field_1724;
      if (player != null && player.field_3913 == input) {
         class_10185 keys = input.field_54155;
         class_10185 corrected = InputControl.shape(player, ServerRotation.correctInput(keys));
         if (corrected != keys) {
            input.field_54155 = corrected;
            ((AccessorMovementClientInput)input)
               .anubis$setMoveVector(
                  new class_241(impulse(corrected.comp_3161(), corrected.comp_3162()), impulse(corrected.comp_3159(), corrected.comp_3160())).method_35581()
               );
         }
      }
   }

   private static float impulse(boolean positive, boolean negative) {
      return positive == negative ? 0.0F : (positive ? 1.0F : -1.0F);
   }
}
