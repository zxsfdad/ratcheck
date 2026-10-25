package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.anubis.client.module.impl.render.PinkWater;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1959;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(class_1959.class)
public abstract class MixinPinkWaterBiome {
   @Unique
   private volatile byte anubis$cave;

   @ModifyReturnValue(method = "method_8687", at = @At("RETURN"))
   private int anubis$pinkWater(int color) {
      if (!PinkWater.shows()) {
         return color;
      }

      byte cave = this.anubis$cave;
      if (cave == 0) {
         cave = PinkWater.caveBiome((class_1959)this);
         if (cave != 0) {
            this.anubis$cave = cave;
         }
      }

      return PinkWater.water(color, cave == 2);
   }
}
