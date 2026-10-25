package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;
import net.minecraft.class_2833;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(class_2833.class)
public interface AccessorBoatFlyServerboundMoveVehiclePacket {
   @Mutable
   @Accessor("comp_3350")
   void anubis$setPosition(class_243 var1);
}
