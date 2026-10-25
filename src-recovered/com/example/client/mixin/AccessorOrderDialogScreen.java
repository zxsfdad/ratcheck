package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11419;
import net.minecraft.class_11473;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(class_11473.class)
public interface AccessorOrderDialogScreen {
   @Accessor("field_60761")
   class_11419 anubis$dialog();
}
