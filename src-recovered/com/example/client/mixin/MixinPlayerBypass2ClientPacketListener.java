package com.example.client.mixin;

import net.anubis.client.util.playerbypass2.SimulationSignals;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10264;
import net.minecraft.class_2604;
import net.minecraft.class_2626;
import net.minecraft.class_2637;
import net.minecraft.class_2663;
import net.minecraft.class_2672;
import net.minecraft.class_2678;
import net.minecraft.class_2684;
import net.minecraft.class_2708;
import net.minecraft.class_2716;
import net.minecraft.class_2724;
import net.minecraft.class_2726;
import net.minecraft.class_2739;
import net.minecraft.class_2744;
import net.minecraft.class_2775;
import net.minecraft.class_4273;
import net.minecraft.class_634;
import net.minecraft.class_6682;
import net.minecraft.class_8143;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(class_634.class)
public abstract class MixinPlayerBypass2ClientPacketListener {
   private static final String ON_MAIN_THREAD = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V";

   @Inject(
      method = "method_11120",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$pb2Login(class_2678 packet, CallbackInfo ci) {
      SimulationSignals.onLogin(packet.comp_169(), packet.comp_98(), packet.comp_1727().comp_1554());
   }

   @Inject(
      method = "method_11117",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$pb2Respawn(class_2724 packet, CallbackInfo ci) {
      SimulationSignals.onRespawn(packet.comp_1728().comp_1554());
   }

   @Inject(
      method = "method_11157",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$pb2Teleport(class_2708 packet, CallbackInfo ci) {
      SimulationSignals.onTeleport();
   }

   @Inject(
      method = "method_39025",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$pb2Simulation(class_6682 packet, CallbackInfo ci) {
      SimulationSignals.onSimulationDistance(packet.comp_170());
   }

   @Inject(
      method = "method_20203",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$pb2View(class_4273 packet, CallbackInfo ci) {
      SimulationSignals.onViewDistance(packet.method_20206());
   }

   @Inject(method = "method_11128", at = @At("TAIL"))
   private void anubis$pb2Chunk(class_2672 packet, CallbackInfo ci) {
      SimulationSignals.onChunk(packet.method_11523(), packet.method_11524());
   }

   @Inject(
      method = "method_11155",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$pb2Move(class_2684 packet, CallbackInfo ci) {
      SimulationSignals.onMove(packet);
   }

   @Inject(
      method = "method_64553",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$pb2PositionSync(class_10264 packet, CallbackInfo ci) {
      SimulationSignals.onPositionSync(packet);
   }

   @Inject(
      method = "method_11139",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$pb2Head(class_2726 packet, CallbackInfo ci) {
      SimulationSignals.onHeadRotation(packet);
   }

   @Inject(
      method = "method_11093",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$pb2EntityData(class_2739 packet, CallbackInfo ci) {
      SimulationSignals.onEntityData(packet);
   }

   @Inject(
      method = "method_11148",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$pb2EntityEvent(class_2663 packet, CallbackInfo ci) {
      SimulationSignals.onEntityEvent(packet);
   }

   @Inject(
      method = "method_11112",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$pb2AddEntity(class_2604 packet, CallbackInfo ci) {
      SimulationSignals.onAddEntity(packet);
   }

   @Inject(
      method = "method_11095",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$pb2RemoveEntities(class_2716 packet, CallbackInfo ci) {
      SimulationSignals.onRemoveEntities(packet.method_36548());
   }

   @Inject(
      method = "method_11136",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$pb2Block(class_2626 packet, CallbackInfo ci) {
      SimulationSignals.onBlockUpdate(packet);
   }

   @Inject(
      method = "method_11100",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$pb2Section(class_2637 packet, CallbackInfo ci) {
      SimulationSignals.onSectionUpdate(packet);
   }

   @Inject(
      method = "method_11150",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$pb2TakeItem(class_2775 packet, CallbackInfo ci) {
      SimulationSignals.onTakeItem(packet);
   }

   @Inject(
      method = "method_49034",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$pb2Damage(class_8143 packet, CallbackInfo ci) {
      SimulationSignals.onDamage(packet);
   }

   @Inject(
      method = "method_11151",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )
   )
   private void anubis$pb2Equipment(class_2744 packet, CallbackInfo ci) {
      SimulationSignals.onEquipment(packet);
   }
}
