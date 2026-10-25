package net.anubis.client.module.impl.movement.elytrafly;

import net.anubis.client.module.impl.movement.ElytraFlyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1294;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_2596;
import net.minecraft.class_2680;
import net.minecraft.class_2848;
import net.minecraft.class_310;
import net.minecraft.class_3481;
import net.minecraft.class_746;
import net.minecraft.class_2848.class_2849;

@Environment(EnvType.CLIENT)
public final class BounceMode extends ElytraFlightMode {
   private boolean rubberbanded = false;
   private int tickDelay;
   private double prevFov;

   public BounceMode(ElytraFlyModule elytraFly) {
      super(elytraFly);
      this.tickDelay = elytraFly.restartDelay.getInt();
   }

   @Override
   public void onTick() {
      super.onTick();
      class_746 player = this.mc.field_1724;
      if (player != null) {
         if (this.mc.field_1690.field_1903.method_1434() && !player.method_6128() && !this.elytraFly.manualTakeoff.get()) {
            sendStartFallFlying(player);
         }

         if (checkConditions(player)) {
            if (!this.rubberbanded) {
               if (this.prevFov != 0.0 && !this.elytraFly.sprint.get()) {
                  this.mc.field_1690.method_42454().method_41748(0.0);
               }

               if (this.elytraFly.autoJump.get()) {
                  this.mc.field_1690.field_1903.method_23481(true);
               }

               this.mc.field_1690.field_1894.method_23481(true);
               player.method_36456(this.getYawDirection(player));
               if (this.elytraFly.lockPitch.get()) {
                  player.method_36457(this.elytraFly.pitch.get());
               }
            }

            if (!this.elytraFly.sprint.get()) {
               if (player.method_6128()) {
                  player.method_5728(player.method_24828());
               } else {
                  player.method_5728(true);
               }
            }

            if (this.rubberbanded && this.elytraFly.restart.get()) {
               if (this.tickDelay > 0) {
                  this.tickDelay--;
               } else {
                  sendStartFallFlying(player);
                  this.rubberbanded = false;
                  this.tickDelay = this.elytraFly.restartDelay.getInt();
               }
            }
         }
      }
   }

   @Override
   public void onPreTick() {
      super.onPreTick();
      class_746 player = this.mc.field_1724;
      if (player != null && checkConditions(player) && this.elytraFly.sprint.get()) {
         player.method_5728(true);
      }
   }

   private void unpress() {
      this.mc.field_1690.field_1894.method_23481(false);
      if (this.elytraFly.autoJump.get()) {
         this.mc.field_1690.field_1903.method_23481(false);
      }
   }

   @Override
   public void onPositionPacket() {
      this.rubberbanded = true;
      class_746 player = this.mc.field_1724;
      if (player != null) {
         player.method_66281();
      }
   }

   @Override
   public void onPacketSend(class_2596<?> packet) {
      if (packet instanceof class_2848 command && command.method_12365() == class_2849.field_12982 && !this.elytraFly.sprint.get()) {
         this.mc.field_1724.method_5728(true);
      }
   }

   public static boolean recastElytra(class_746 player) {
      if (checkConditions(player) && startGliding(player)) {
         sendStartFallFlying(player);
         return true;
      } else {
         return false;
      }
   }

   public static boolean checkConditions(class_746 player) {
      class_2680 blockState = player.method_55667();
      boolean isClimbing = blockState.method_26164(class_3481.field_22414) && !blockState.method_26164(class_3481.field_63247);
      return !player.method_31549().field_7479 && !player.method_5765() && !isClimbing && !player.method_5799() && !player.method_6059(class_1294.field_5902);
   }

   private static boolean startGliding(class_746 player) {
      for (class_1304 equipmentSlot : class_1304.field_54086) {
         if (class_1309.method_63624(player.method_6118(equipmentSlot), equipmentSlot)) {
            class_310.method_1551().method_19537(player::method_23669);
            return true;
         }
      }

      return false;
   }

   private static void sendStartFallFlying(class_746 player) {
      player.field_3944.method_52787(new class_2848(player, class_2849.field_12982));
   }

   private float getYawDirection(class_746 player) {
      return switch (this.elytraFly.yawLockMode.get()) {
         case "Smart" -> Math.round((player.method_36454() + 1.0F) / 45.0F) * 45.0F;
         case "Simple" -> this.elytraFly.yaw.get();
         default -> player.method_36454();
      };
   }

   @Override
   public void onActivate() {
      this.prevFov = (Double)this.mc.field_1690.method_42454().method_41753();
   }

   @Override
   public void onDeactivate() {
      this.unpress();
      this.rubberbanded = false;
      if (this.prevFov != 0.0 && !this.elytraFly.sprint.get()) {
         this.mc.field_1690.method_42454().method_41748(this.prevFov);
      }
   }
}
