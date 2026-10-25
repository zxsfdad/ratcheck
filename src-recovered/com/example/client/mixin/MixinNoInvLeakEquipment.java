package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.anubis.client.module.impl.misc.NoInvLeakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(class_1309.class)
public abstract class MixinNoInvLeakEquipment {
   @ModifyReturnValue(method = "method_6118", at = @At("RETURN"))
   private class_1799 anubis$noInvLeakBareModel(class_1799 stack) {
      return NoInvLeakModule.modelItem((class_1309)this, stack);
   }
}
