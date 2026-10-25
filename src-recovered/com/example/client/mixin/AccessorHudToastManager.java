package com.example.client.mixin;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_374;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(class_374.class)
public interface AccessorHudToastManager {
   @Accessor("field_2239")
   List<?> anubis$hudVisibleToasts();
}
