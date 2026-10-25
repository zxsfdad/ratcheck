package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2338;
import net.minecraft.class_636;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(class_636.class)
public interface AccessorBlockOverlayMultiPlayerGameMode {
   @Accessor("field_3715")
   float anubis$blockOverlayDestroyProgress();

   @Accessor("field_3714")
   class_2338 anubis$blockOverlayDestroyPos();
}
