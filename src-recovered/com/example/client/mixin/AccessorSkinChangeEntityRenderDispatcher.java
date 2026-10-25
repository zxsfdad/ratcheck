package com.example.client.mixin;

import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1007;
import net.minecraft.class_742;
import net.minecraft.class_7920;
import net.minecraft.class_898;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(class_898.class)
public interface AccessorSkinChangeEntityRenderDispatcher {
   @Accessor("field_4687")
   Map<class_7920, class_1007<class_742>> anubis$skinChangePlayerRenderers();
}
