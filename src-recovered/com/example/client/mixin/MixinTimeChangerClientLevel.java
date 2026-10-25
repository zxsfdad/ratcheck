package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.anubis.client.module.impl.render.TimeChangerModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_638;
import net.minecraft.class_638.class_5271;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(class_638.class)
public abstract class MixinTimeChangerClientLevel {
   @WrapOperation(method = "method_29089", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_638$class_5271;method_165(J)V"))
   private void anubis$timeChangerServerDayTime(class_5271 levelData, long dayTime, Operation<Void> original) {
      TimeChangerModule timeChanger = TimeChangerModule.active;
      if (timeChanger == null) {
         original.call(new Object[]{levelData, dayTime});
      } else {
         class_638 level = (class_638)this;
         long shown = levelData.method_217();
         long forced = timeChanger.interceptServerDayTime(level, dayTime);
         original.call(new Object[]{levelData, forced});
         if (forced != shown) {
            level.method_75728().method_76405();
         }
      }
   }
}
