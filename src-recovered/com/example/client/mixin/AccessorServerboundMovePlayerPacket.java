package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2828;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(class_2828.class)
public interface AccessorServerboundMovePlayerPacket {
   @Mutable
   @Accessor("field_12886")
   void anubis$setY(double var1);

   @Mutable
   @Accessor("field_29179")
   void anubis$setOnGround(boolean var1);
}
