package com.example.client.mixin;

import net.anubis.client.module.impl.misc.NameProtectModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_303;
import net.minecraft.class_5348;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin(class_303.class)
public abstract class MixinNameProtectGuiMessage {
   @ModifyArg(
      method = "method_75757",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/class_341;method_1850(Lnet/minecraft/class_5348;ILnet/minecraft/class_327;)Ljava/util/List;"),
      index = 0
   )
   private class_5348 anubis$nameProtectChatLine(class_5348 text) {
      return NameProtectModule.protectChatLine(text);
   }
}
