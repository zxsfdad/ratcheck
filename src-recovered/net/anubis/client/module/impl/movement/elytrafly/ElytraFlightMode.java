package net.anubis.client.module.impl.movement.elytrafly;

import net.anubis.client.event.impl.PlayerMoveEvent;
import net.anubis.client.module.impl.movement.ElytraFlyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1268;
import net.minecraft.class_1304;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_243;
import net.minecraft.class_2596;
import net.minecraft.class_2848;
import net.minecraft.class_310;
import net.minecraft.class_746;
import net.minecraft.class_2848.class_2849;

@Environment(EnvType.CLIENT)
public abstract class ElytraFlightMode {
   protected final class_310 mc = class_310.method_1551();
   protected final ElytraFlyModule elytraFly;
   protected boolean lastJumpPressed;
   protected boolean incrementJumpTimer;
   public boolean lastForwardPressed;
   protected int jumpTimer;
   public double velX;
   public double velY;
   public double velZ;
   protected double ticksLeft;
   public class_243 forward;
   public class_243 right;
   protected double acceleration;

   protected ElytraFlightMode(ElytraFlyModule elytraFly) {
      this.elytraFly = elytraFly;
   }

   public void onTick() {
      class_746 player = this.mc.field_1724;
      if (player != null) {
         if (this.elytraFly.autoReplenish.get()) {
            int fireworks = ElytraFlyInventory.find(stack -> stack.method_7909() == class_1802.field_8639);
            if (fireworks != -1 && !ElytraFlyInventory.isHotbar(fireworks)) {
               ElytraFlyInventory.moveToHotbar(fireworks, this.elytraFly.replenishSlot.getInt() - 1);
            }
         }

         if (this.elytraFly.replace.get()) {
            class_1799 chestStack = player.method_6118(class_1304.field_6174);
            if (chestStack.method_7909() == class_1802.field_8833) {
               int threshold = this.elytraFly.replaceDurability.getInt();
               if (chestStack.method_7936() - chestStack.method_7919() <= threshold) {
                  int elytra = ElytraFlyInventory.find(
                     stack -> stack.method_7936() - stack.method_7919() > threshold && stack.method_7909() == class_1802.field_8833
                  );
                  ElytraFlyInventory.moveToChest(elytra);
               }
            }
         }
      }
   }

   public void onPreTick() {
   }

   public void onPacketSend(class_2596<?> packet) {
   }

   public void onPositionPacket() {
   }

   public void onPlayerMove() {
   }

   public void onActivate() {
      this.lastJumpPressed = false;
      this.jumpTimer = 0;
      this.ticksLeft = 0.0;
      this.acceleration = 0.0;
   }

   public void onDeactivate() {
   }

   public void autoTakeoff() {
      class_746 player = this.mc.field_1724;
      if (this.incrementJumpTimer) {
         this.jumpTimer++;
      }

      boolean jumpPressed = this.mc.field_1690.field_1903.method_1434();
      ElytraFlightModes mode = this.elytraFly.flightModeType();
      if (this.elytraFly.autoTakeOff.get() && mode != ElytraFlightModes.Pitch40 && mode != ElytraFlightModes.Bounce
         || !this.elytraFly.manualTakeoff.get() && mode == ElytraFlightModes.Bounce && jumpPressed) {
         if (!this.lastJumpPressed && !player.method_6128()) {
            this.jumpTimer = 0;
            this.incrementJumpTimer = true;
         }

         if (this.jumpTimer >= 8) {
            this.jumpTimer = 0;
            this.incrementJumpTimer = false;
            player.method_6100(false);
            player.method_5728(true);
            player.method_6043();
            player.field_3944.method_52787(new class_2848(player, class_2849.field_12982));
         }
      }

      this.lastJumpPressed = jumpPressed;
   }

   public void handleAutopilot() {
      class_746 player = this.mc.field_1724;
      if (player.method_6128()) {
         if (this.elytraFly.autoPilot.get()
            && player.method_23318() > this.elytraFly.autoPilotMinimumHeight.get()
            && this.elytraFly.flightModeType() != ElytraFlightModes.Bounce) {
            this.mc.field_1690.field_1894.method_23481(true);
            this.lastForwardPressed = true;
         }

         if (this.elytraFly.useFireworks.get()) {
            if (this.ticksLeft <= 0.0) {
               this.ticksLeft = this.elytraFly.autoPilotFireworkDelay.get() * 20.0F;
               int slot = ElytraFlyInventory.findInHotbar(stack -> stack.method_7909() == class_1802.field_8639);
               if (slot == -1 || this.mc.field_1761 == null) {
                  return;
               }

               if (slot == 40) {
                  this.mc.field_1761.method_2919(player, class_1268.field_5810);
                  player.method_6104(class_1268.field_5810);
               } else {
                  ElytraFlyInventory.swap(slot, true);
                  this.mc.field_1761.method_2919(player, class_1268.field_5808);
                  player.method_6104(class_1268.field_5808);
                  ElytraFlyInventory.swapBack();
               }
            }

            this.ticksLeft--;
         }
      }
   }

   public void handleHorizontalSpeed(PlayerMoveEvent event) {
      boolean a = false;
      boolean b = false;
      if (this.mc.field_1690.field_1894.method_1434()) {
         this.velX = this.velX + this.forward.field_1352 * this.getSpeed() * 10.0;
         this.velZ = this.velZ + this.forward.field_1350 * this.getSpeed() * 10.0;
         a = true;
      } else if (this.mc.field_1690.field_1881.method_1434()) {
         this.velX = this.velX - this.forward.field_1352 * this.getSpeed() * 10.0;
         this.velZ = this.velZ - this.forward.field_1350 * this.getSpeed() * 10.0;
         a = true;
      }

      if (this.mc.field_1690.field_1849.method_1434()) {
         this.velX = this.velX + this.right.field_1352 * this.getSpeed() * 10.0;
         this.velZ = this.velZ + this.right.field_1350 * this.getSpeed() * 10.0;
         b = true;
      } else if (this.mc.field_1690.field_1913.method_1434()) {
         this.velX = this.velX - this.right.field_1352 * this.getSpeed() * 10.0;
         this.velZ = this.velZ - this.right.field_1350 * this.getSpeed() * 10.0;
         b = true;
      }

      if (a && b) {
         double diagonal = 1.0 / Math.sqrt(2.0);
         this.velX *= diagonal;
         this.velZ *= diagonal;
      }
   }

   public void handleVerticalSpeed(PlayerMoveEvent event) {
      if (this.mc.field_1690.field_1903.method_1434()) {
         this.velY = this.velY + 0.5 * this.elytraFly.verticalSpeed.get();
      } else if (this.mc.field_1690.field_1832.method_1434()) {
         this.velY = this.velY - 0.5 * this.elytraFly.verticalSpeed.get();
      }
   }

   public void handleFallMultiplier() {
      if (this.velY < 0.0) {
         this.velY = this.velY * this.elytraFly.fallMultiplier.get();
      } else if (this.velY > 0.0) {
         this.velY = 0.0;
      }
   }

   public void handleAcceleration() {
      if (this.elytraFly.acceleration.get()) {
         if (!this.isMoving()) {
            this.acceleration = 0.0;
         }

         this.acceleration = Math.min(
            this.acceleration + this.elytraFly.accelerationMin.get() + this.elytraFly.accelerationStep.get() * 0.1, this.elytraFly.horizontalSpeed.get()
         );
      } else {
         this.acceleration = 0.0;
      }
   }

   public void zeroAcceleration() {
      this.acceleration = 0.0;
   }

   protected double getSpeed() {
      return this.elytraFly.acceleration.get() ? this.acceleration : this.elytraFly.horizontalSpeed.get();
   }

   private boolean isMoving() {
      class_746 player = this.mc.field_1724;
      return player.field_6250 != 0.0F || player.field_6212 != 0.0F;
   }
}
