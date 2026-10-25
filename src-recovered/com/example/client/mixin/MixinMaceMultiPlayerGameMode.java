package com.example.client.mixin;

import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.combat.MaceModule;
import net.anubis.client.module.impl.combat.ShieldBreakerModule;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_310;
import net.minecraft.class_636;
import net.minecraft.class_746;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_636.class)
public abstract class MixinMaceMultiPlayerGameMode {
   @Inject(method = "method_2918", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_636;method_2911()V"))
   private void anubis$weaponBeforeAttack(class_1657 player, class_1297 target, CallbackInfo ci) {
      class_746 self = class_310.method_1551().field_1724;
      if (player == self && !MeleeAttacks.actedThisTick(self)) {
         ShieldBreakerModule shieldBreaker = ModuleRegistry.get(ShieldBreakerModule.class);
         if (shieldBreaker != null && shieldBreaker.isEnabled()) {
            shieldBreaker.beforeAttack(self, target);
         }

         MaceModule mace = ModuleRegistry.get(MaceModule.class);
         if (mace != null && mace.isEnabled()) {
            mace.beforeAttack(self, target);
         }
      }
   }
}
