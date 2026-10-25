package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_4184;
import net.minecraft.class_757;
import net.minecraft.class_758;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(class_757.class)
public interface AccessorGameRenderer {
   @Invoker("method_3196")
   float anubis$getFov(class_4184 var1, float var2, boolean var3);

   @Accessor("field_60793")
   class_758 anubis$getFogRenderer();

   @Invoker("method_3202")
   boolean anubis$shouldRenderBlockOutline();
}
