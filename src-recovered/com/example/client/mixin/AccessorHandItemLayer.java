package com.example.client.mixin;

import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_804;
import net.minecraft.class_10444.class_10446;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(class_10446.class)
public interface AccessorHandItemLayer {
   @Accessor("field_56967")
   class_804 anubis$handTransform();

   @Accessor("field_56968")
   Supplier<Vector3fc[]> anubis$handExtents();
}
