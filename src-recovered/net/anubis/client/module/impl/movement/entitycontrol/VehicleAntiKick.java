package net.anubis.client.module.impl.movement.entitycontrol;

import com.example.client.mixin.AccessorBoatFlyServerboundMoveVehiclePacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_243;
import net.minecraft.class_2833;
import net.minecraft.class_310;
import net.minecraft.class_634;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class VehicleAntiKick {
   private static final double Y_OFFSET = 0.0313;
   private static final long HOLD_NANOS = 50000000L;
   private int delayLeft;
   private double lastPacketY = Double.MAX_VALUE;
   private boolean sentPacket;
   private long dipNanos;
   private class_1297 trackedVehicle;

   public void start(int delay) {
      this.delayLeft = delay;
      this.forget();
      class_746 player = class_310.method_1551().field_1724;
      this.trackedVehicle = player != null ? player.method_5854() : null;
   }

   public void stop() {
      this.trackedVehicle = null;
   }

   public void tick() {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      class_1297 vehicle = player != null ? player.method_5854() : null;
      this.trackVehicle(vehicle);
      if (this.sentPacket && System.nanoTime() - this.dipNanos >= 50000000L) {
         class_634 connection = mc.method_1562();
         if (vehicle != null && connection != null && this.lastPacketY != Double.MAX_VALUE) {
            class_2833 current = class_2833.method_65307(vehicle);
            class_243 pos = current.comp_3350();
            connection.method_52787(
               new class_2833(new class_243(pos.field_1352, this.lastPacketY, pos.field_1350), current.comp_3351(), current.comp_3352(), current.comp_3353())
            );
         }

         this.sentPacket = false;
      }

      if (this.delayLeft > 0) {
         this.delayLeft--;
      }
   }

   public boolean onPacketSend(class_2833 packet, int delay) {
      class_310 mc = class_310.method_1551();
      if (!mc.method_18854()) {
         return false;
      }

      class_746 player = mc.field_1724;
      class_1297 vehicle = player != null ? player.method_5854() : null;
      if (vehicle == null) {
         return false;
      }

      this.trackVehicle(vehicle);
      class_243 pos = packet.comp_3350();
      double currentY = pos.field_1351;
      if (this.sentPacket && System.nanoTime() - this.dipNanos < 50000000L) {
         this.lastPacketY = currentY;
         return true;
      }

      if (this.delayLeft <= 0 && !this.sentPacket && this.shouldFlyDown(currentY) && EntityControlVehicleHook.isOnAir(vehicle) && !vehicle.method_70987()) {
         ((AccessorBoatFlyServerboundMoveVehiclePacket)packet).anubis$setPosition(new class_243(pos.field_1352, this.lastPacketY - 0.0313, pos.field_1350));
         this.sentPacket = true;
         this.dipNanos = System.nanoTime();
         this.delayLeft = delay;
      }

      this.lastPacketY = currentY;
      return false;
   }

   public void observe(class_2833 packet) {
      this.lastPacketY = packet.comp_3350().field_1351;
   }

   public boolean isHolding() {
      return this.sentPacket && System.nanoTime() - this.dipNanos < 50000000L;
   }

   public void forget() {
      this.lastPacketY = Double.MAX_VALUE;
      this.sentPacket = false;
   }

   private boolean shouldFlyDown(double currentY) {
      return currentY >= this.lastPacketY ? true : this.lastPacketY - currentY < 0.0313;
   }

   private void trackVehicle(class_1297 vehicle) {
      if (vehicle != this.trackedVehicle) {
         this.trackedVehicle = vehicle;
         this.forget();
      }
   }
}
