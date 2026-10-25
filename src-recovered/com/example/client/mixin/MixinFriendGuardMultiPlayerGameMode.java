package com.example.client.mixin;

import net.anubis.client.module.impl.combat.FriendGuardModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12125;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_310;
import net.minecraft.class_636;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_636.class)
public abstract class MixinFriendGuardMultiPlayerGameMode {
   @Inject(method = "method_2918", at = @At("HEAD"), cancellable = true, order = 900)
   private void anubis$friendGuard(class_1657 player, class_1297 target, CallbackInfo ci) {
      if (FriendGuardModule.intercepts(player, target)) {
         player.method_7350();
         ci.cancel();
      }
   }

   @Inject(method = "method_75407", at = @At("HEAD"), cancellable = true, order = 900)
   private void anubis$friendGuardStab(class_12125 weapon, CallbackInfo ci) {
      if (FriendGuardModule.interceptsStab(class_310.method_1551().field_1724)) {
         ci.cancel();
      }
   }
}
