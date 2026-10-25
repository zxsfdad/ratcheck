package com.example.client.mixin;

import net.anubis.client.module.impl.misc.FakeStats;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_266;
import net.minecraft.class_269;
import net.minecraft.class_8646;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_269.class)
public abstract class MixinFakeStatsScoreboard {
   @Inject(method = "method_1158", at = @At("HEAD"), cancellable = true)
   private void anubis$fakeSidebar(class_8646 slot, class_266 objective, CallbackInfo ci) {
      if (FakeStats.redirect((class_269)this, slot, objective)) {
         ci.cancel();
      }
   }
}
