package com.example.client.mixin;

import net.anubis.client.module.impl.movement.JesusModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_1297.class)
public abstract class MixinJesusEntity {
   @Inject(method = "method_5700", at = @At("HEAD"))
   private void anubis$jesusAboveBubbleColumn(CallbackInfo ci) {
      this.anubis$jesusMarkBubbleColumn();
   }

   @Inject(method = "method_5764", at = @At("HEAD"))
   private void anubis$jesusInsideBubbleColumn(CallbackInfo ci) {
      this.anubis$jesusMarkBubbleColumn();
   }

   @Unique
   private void anubis$jesusMarkBubbleColumn() {
      JesusModule jesus = JesusModule.active;
      if (jesus != null && this == class_310.method_1551().field_1724) {
         jesus.markInBubbleColumn();
      }
   }
}
