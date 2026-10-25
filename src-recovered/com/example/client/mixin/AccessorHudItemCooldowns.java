package com.example.client.mixin;

import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1796;
import net.minecraft.class_2960;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(class_1796.class)
public interface AccessorHudItemCooldowns {
   @Accessor("field_8024")
   Map<class_2960, ?> anubis$hudCooldowns();

   @Accessor("field_8025")
   int anubis$hudTickCount();
}
