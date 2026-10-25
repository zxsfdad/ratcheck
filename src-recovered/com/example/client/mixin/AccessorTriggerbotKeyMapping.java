package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_304;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(class_304.class)
public interface AccessorTriggerbotKeyMapping {
   @Accessor("field_1661")
   int anubis$triggerbotClickCount();
}
