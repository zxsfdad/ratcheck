package com.example.client.mixin;

import net.anubis.client.module.impl.render.AtmosphereModule;
import net.anubis.client.module.impl.render.PeepWorld;
import net.anubis.client.module.impl.render.PinkWater;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1294;
import net.minecraft.class_1309;
import net.minecraft.class_4184;
import net.minecraft.class_5636;
import net.minecraft.class_638;
import net.minecraft.class_758;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(class_758.class)
public abstract class MixinAtmosphereFogRenderer {
   @Inject(method = "method_62185", at = @At("RETURN"))
   private void anubis$fogColour(
      class_4184 camera, float partialTick, class_638 level, int renderDistance, float darkness, CallbackInfoReturnable<Vector4f> cir
   ) {
      class_5636 fluid = camera.method_19334();
      if (fluid == class_5636.field_27886) {
         PinkWater.underwater((Vector4f)cir.getReturnValue());
      }

      if (fluid == class_5636.field_27888 && !(darkness > 0.0F)) {
         if (!(camera.method_19331() instanceof class_1309 living && (living.method_6059(class_1294.field_5919) || living.method_6059(class_1294.field_38092)))
            )
          {
            AtmosphereModule atmosphere = AtmosphereModule.get();
            if (atmosphere != null) {
               atmosphere.fog(camera, level, (Vector4f)cir.getReturnValue());
            }

            PeepWorld.fog((Vector4f)cir.getReturnValue());
         }
      }
   }
}
