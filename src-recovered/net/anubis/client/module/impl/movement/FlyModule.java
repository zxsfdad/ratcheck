package net.anubis.client.module.impl.movement;

import com.example.client.mixin.AccessorLocalPlayer;
import com.example.client.mixin.AccessorServerboundMovePlayerPacket;
import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.PacketReceiveNettyEvent;
import net.anubis.client.event.impl.PacketSendEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.misc.FreecamModule;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1656;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2696;
import net.minecraft.class_2793;
import net.minecraft.class_2828;
import net.minecraft.class_310;
import net.minecraft.class_634;
import net.minecraft.class_746;
import net.minecraft.class_2828.class_2829;
import net.minecraft.class_2828.class_2830;
import net.minecraft.class_4970.class_4971;

@Environment(EnvType.CLIENT)
public final class FlyModule extends ClientModule {
   public static volatile FlyModule active;
   private static final String ABILITIES = "Abilities";
   private static final String VELOCITY = "Velocity";
   private static final String KICK_NORMAL = "Normal";
   private static final String KICK_PACKET = "Packet";
   private static final String KICK_NONE = "None";
   private static final double ANTI_KICK_DROP = 0.0313;
   private static final float VANILLA_FLYING_SPEED = 0.05F;
   private static final String DEFAULT_MODE = "Abilities";
   private static final float DEFAULT_SPEED = 0.1F;
   private final ModeSetting mode = this.addSetting("fly-mode", new ModeSetting("Mode", "Abilities", "Abilities", "Velocity"));
   private final NumberSetting speed = this.addSetting("fly-speed", new NumberSetting("Speed", 0.1F, 0.0F, 10.0F).step(0.05F));
   private final BooleanSetting verticalSpeedMatch = this.addSetting("vertical-speed-match", new BooleanSetting("Vertical Speed Match", false));
   private final BooleanSetting noSneak = this.addSetting("no-sneak", new BooleanSetting("No Sneak", false));
   private final ModeSetting antiKickMode = this.addSetting("anti-kick-mode", new ModeSetting("Anti Kick Mode", "Packet", "Normal", "Packet", "None"));
   private final NumberSetting delay = this.addSetting("delay", new NumberSetting("Anti Kick Delay", 20.0F, 1.0F, 200.0F).step(1.0F));
   private final NumberSetting offTime = this.addSetting("off-time", new NumberSetting("Anti Kick Off Time", 1.0F, 1.0F, 20.0F).step(1.0F));
   private int delayLeft = 20;
   private int offLeft = 1;
   private boolean flip;
   private float lastYaw;
   private double lastPacketY = Double.MAX_VALUE;
   private class_746 trackedPlayer;

   public FlyModule() {
      super("fly", "Fly", "Lets you fly.", ModuleCategory.MOVEMENT);
      this.noSneak.visibleWhen(() -> this.mode.is("Velocity"));
      this.mode.onChange(() -> {
         if (this.isEnabled()) {
            class_746 player = class_310.method_1551().field_1724;
            if (player != null) {
               abilitiesOff(player);
            }
         }
      });
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      class_746 player = class_310.method_1551().field_1724;
      if (player == null) {
         this.requestDisable();
      } else {
         this.delayLeft = this.delay.getInt();
         this.offLeft = this.offTime.getInt();
         this.resetPlayerState(player);
         active = this;
         if (this.mode.is("Abilities") && !player.method_7325()) {
            class_1656 abilities = player.method_31549();
            abilities.field_7479 = true;
            if (!abilities.field_7477) {
               abilities.field_7478 = true;
            }
         }

         this.on(TickEvent.TYPE, Stage.PRE, this::onPreTick);
         this.on(TickEvent.TYPE, Stage.POST, this::onPostTick);
         this.on(PacketSendEvent.TYPE, this::onPacketSend);
         this.on(PacketReceiveNettyEvent.TYPE, this::onPacketReceiveNetty);
      }
   }

   @Override
   protected void onDisable() {
      active = null;
      class_746 player = class_310.method_1551().field_1724;
      if (player != null && this.mode.is("Abilities")) {
         abilitiesOff(player);
      }

      this.trackedPlayer = null;
   }

   private void resetPlayerState(class_746 player) {
      this.trackedPlayer = player;
      this.lastYaw = player.method_36454();
      this.lastPacketY = Double.MAX_VALUE;
   }

   private void onPreTick(TickEvent event) {
      class_746 player = class_310.method_1551().field_1724;
      if (player == null) {
         this.requestDisable();
      } else {
         if (player != this.trackedPlayer) {
            this.resetPlayerState(player);
         }

         float currentYaw = player.method_36454();
         if (player.field_6017 >= 3.0 && currentYaw == this.lastYaw && player.method_18798().method_1033() < 0.003) {
            player.method_36456(currentYaw + (this.flip ? 1 : -1));
            this.flip = !this.flip;
         }

         this.lastYaw = currentYaw;
      }
   }

   private void onPostTick(TickEvent event) {
      class_746 player = class_310.method_1551().field_1724;
      if (player != null) {
         if (player != this.trackedPlayer) {
            this.resetPlayerState(player);
         }

         if (this.delayLeft > 0) {
            this.delayLeft--;
         }

         if (this.offLeft <= 0 && this.delayLeft <= 0) {
            this.delayLeft = this.delay.getInt();
            this.offLeft = this.offTime.getInt();
            if (this.antiKickMode.is("Packet")) {
               forcePositionPacket(player);
            }
         } else if (this.delayLeft <= 0) {
            boolean shouldReturn = false;
            if (this.antiKickMode.is("Normal")) {
               if (this.mode.is("Abilities")) {
                  abilitiesOff(player);
                  shouldReturn = true;
               }
            } else if (this.antiKickMode.is("Packet") && this.offLeft == this.offTime.getInt()) {
               forcePositionPacket(player);
            }

            this.offLeft--;
            if (shouldReturn) {
               return;
            }
         }

         if (player.method_36454() != this.lastYaw) {
            player.method_36456(this.lastYaw);
         }

         if (this.mode.is("Velocity")) {
            if (!player.method_7325()) {
               player.method_31549().field_7479 = false;
            }

            player.method_18800(0.0, 0.0, 0.0);
            class_243 velocity = player.method_18798();
            if (!isFreecamActive()) {
               class_310 mc = class_310.method_1551();
               double vertical = (double)this.speed.get() * (this.verticalSpeedMatch.get() ? 10.0F : 5.0F);
               if (mc.field_1690.field_1903.method_1434()) {
                  velocity = velocity.method_1031(0.0, vertical, 0.0);
               }

               if (mc.field_1690.field_1832.method_1434()) {
                  velocity = velocity.method_1023(0.0, vertical, 0.0);
               }
            }

            player.method_18799(velocity);
            if (this.noSneak.get()) {
               player.method_24830(false);
            }
         } else {
            if (player.method_7325()) {
               return;
            }

            class_1656 abilities = player.method_31549();
            abilities.method_7248(this.speed.get());
            abilities.field_7479 = true;
            if (abilities.field_7477) {
               return;
            }

            abilities.field_7478 = true;
         }
      }
   }

   private void onPacketSend(PacketSendEvent event) {
      class_310 mc = class_310.method_1551();
      if (mc.method_18854()) {
         if (event.packet instanceof class_2793) {
            this.lastPacketY = Double.MAX_VALUE;
         } else if (event.packet instanceof class_2828 packet && this.antiKickMode.is("Packet")) {
            class_746 player = mc.field_1724;
            class_634 connection = mc.method_1562();
            if (player != null && connection != null) {
               double currentY = packet.method_12268(Double.MAX_VALUE);
               if (currentY != Double.MAX_VALUE) {
                  this.antiKickPacket(player, packet, currentY);
               } else {
                  class_2828 fullPacket;
                  if (packet.method_36172()) {
                     fullPacket = new class_2830(
                        player.method_23317(),
                        player.method_23318(),
                        player.method_23321(),
                        packet.method_12271(0.0F),
                        packet.method_12270(0.0F),
                        packet.method_12273(),
                        player.field_5976
                     );
                  } else {
                     fullPacket = new class_2829(player.method_23317(), player.method_23318(), player.method_23321(), packet.method_12273(), player.field_5976);
                  }

                  event.setCancelled(true);
                  this.antiKickPacket(player, fullPacket, player.method_23318());
                  connection.method_52787(fullPacket);
               }
            }
         }
      }
   }

   private void antiKickPacket(class_746 player, class_2828 packet, double currentY) {
      if (this.delayLeft <= 0 && this.lastPacketY != Double.MAX_VALUE && shouldFlyDown(currentY, this.lastPacketY) && isOnAir(player)) {
         ((AccessorServerboundMovePlayerPacket)packet).anubis$setY(this.lastPacketY - 0.0313);
      } else {
         this.lastPacketY = currentY;
      }
   }

   private void onPacketReceiveNetty(PacketReceiveNettyEvent event) {
      if (event.packet instanceof class_2696 packet && this.mode.is("Abilities")) {
         event.setCancelled(true);
         class_310 mc = class_310.method_1551();
         mc.execute(() -> {
            class_746 player = mc.field_1724;
            if (player != null) {
               class_1656 abilities = player.method_31549();
               abilities.field_7480 = packet.method_11695();
               abilities.field_7477 = packet.method_11696();
               abilities.method_7250(packet.method_11691());
            }
         });
      }
   }

   private static boolean shouldFlyDown(double currentY, double lastY) {
      return currentY >= lastY ? true : lastY - currentY < 0.0313;
   }

   private static void forcePositionPacket(class_746 player) {
      ((AccessorLocalPlayer)player).anubis$setPositionReminder(20);
   }

   private static boolean isOnAir(class_746 player) {
      class_238 box = player.method_5829().method_1014(0.0625).method_1012(0.0, -0.55, 0.0);
      return player.method_73183().method_29546(box).allMatch(class_4971::method_26215);
   }

   private static boolean isFreecamActive() {
      FreecamModule freecam = ModuleRegistry.get(FreecamModule.class);
      return freecam != null && freecam.isEnabled();
   }

   private static void abilitiesOff(class_746 player) {
      if (!player.method_7325()) {
         class_1656 abilities = player.method_31549();
         abilities.field_7479 = false;
         abilities.method_7248(0.05F);
         if (!abilities.field_7477) {
            abilities.field_7478 = false;
         }
      }
   }

   public float getOffGroundSpeed(class_746 player) {
      return this.isEnabled() && this.mode.is("Velocity") ? this.speed.get() * (player.method_5624() ? 15.0F : 10.0F) : -1.0F;
   }

   public boolean noSneak() {
      return this.isEnabled() && this.mode.is("Velocity") && this.noSneak.get();
   }

   public boolean suppressFlightVerticalInput() {
      return this.isEnabled() && isFreecamActive();
   }
}
