package net.anubis.client.module.impl.movement;

import com.example.client.mixin.AccessorElytraFlyBlockBehaviour;
import net.anubis.client.event.EventPriority;
import net.anubis.client.event.ListenerHandle;
import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.PacketReceiveNettyEvent;
import net.anubis.client.event.impl.PacketSendEvent;
import net.anubis.client.event.impl.PlayerMoveEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.misc.FreecamModule;
import net.anubis.client.module.impl.movement.elytrafly.BounceMode;
import net.anubis.client.module.impl.movement.elytrafly.ElytraFlightMode;
import net.anubis.client.module.impl.movement.elytrafly.ElytraFlightModes;
import net.anubis.client.module.impl.movement.elytrafly.ElytraFlyInventory;
import net.anubis.client.module.impl.movement.elytrafly.PacketMode;
import net.anubis.client.module.impl.movement.elytrafly.Pitch40Mode;
import net.anubis.client.module.impl.movement.elytrafly.VanillaMode;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1304;
import net.minecraft.class_1802;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_2708;
import net.minecraft.class_2848;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_746;
import net.minecraft.class_9334;
import net.minecraft.class_239.class_240;
import net.minecraft.class_2828.class_5911;
import net.minecraft.class_2848.class_2849;
import net.minecraft.class_3959.class_242;
import net.minecraft.class_3959.class_3960;

@Environment(EnvType.CLIENT)
public final class ElytraFlyModule extends ClientModule {
   public static volatile ElytraFlyModule active;
   private static final float ELYTRA_MAX_REPLACE_DURABILITY = 431.0F;
   public final ModeSetting flightMode = this.addSetting("mode", new ModeSetting("Mode", "Vanilla", "Vanilla", "Packet", "Pitch40", "Bounce"));
   public final BooleanSetting autoTakeOff = this.addSetting("auto-take-off", new BooleanSetting("Auto Take Off", false));
   public final NumberSetting fallMultiplier = this.addSetting("fall-multiplier", new NumberSetting("Fall Multiplier", 0.01F, 0.0F, 2.0F).step(0.01F));
   public final NumberSetting horizontalSpeed = this.addSetting("horizontal-speed", new NumberSetting("Horizontal Speed", 1.0F, 0.0F, 10.0F).step(0.1F));
   public final NumberSetting verticalSpeed = this.addSetting("vertical-speed", new NumberSetting("Vertical Speed", 1.0F, 0.0F, 10.0F).step(0.1F));
   public final BooleanSetting acceleration = this.addSetting("acceleration", new BooleanSetting("Acceleration", false));
   public final NumberSetting accelerationStep = this.addSetting("acceleration-step", new NumberSetting("Acceleration Step", 1.0F, 0.1F, 5.0F).step(0.1F));
   public final NumberSetting accelerationMin = this.addSetting("acceleration-start", new NumberSetting("Acceleration Start", 0.0F, 0.0F, 10.0F).step(0.1F));
   public final BooleanSetting stopInWater = this.addSetting("stop-in-water", new BooleanSetting("Stop In Water", true));
   public final BooleanSetting dontGoIntoUnloadedChunks = this.addSetting("no-unloaded-chunks", new BooleanSetting("No Unloaded Chunks", true));
   public final BooleanSetting autoHover = this.addSetting("auto-hover", new BooleanSetting("Auto Hover", false));
   public final BooleanSetting noCrash = this.addSetting("no-crash", new BooleanSetting("No Crash", false));
   public final NumberSetting crashLookAhead = this.addSetting("crash-look-ahead", new NumberSetting("Crash Look Ahead", 5.0F, 1.0F, 15.0F).step(1.0F));
   private final BooleanSetting instaDrop = this.addSetting("insta-drop", new BooleanSetting("Insta Drop", false));
   public final NumberSetting pitch40lowerBounds = this.addSetting(
      "pitch40-lower-bounds", new NumberSetting("Pitch40 Lower Bounds", 180.0F, -128.0F, 360.0F).step(1.0F)
   );
   public final NumberSetting pitch40upperBounds = this.addSetting(
      "pitch40-upper-bounds", new NumberSetting("Pitch40 Upper Bounds", 220.0F, -128.0F, 360.0F).step(1.0F)
   );
   public final NumberSetting pitch40rotationSpeedUp = this.addSetting(
      "pitch40-rotate-speed-up", new NumberSetting("Pitch40 Rotate Speed Up", 5.45F, 1.0F, 20.0F).step(0.05F)
   );
   public final NumberSetting pitch40rotationSpeedDown = this.addSetting(
      "pitch40-rotate-speed-down", new NumberSetting("Pitch40 Rotate Speed Down", 0.9F, 0.5F, 2.0F).step(0.05F)
   );
   public final BooleanSetting autoJump = this.addSetting("auto-jump", new BooleanSetting("Auto Jump", true));
   public final ModeSetting yawLockMode = this.addSetting("yaw-lock", new ModeSetting("Yaw Lock", "Smart", "Smart", "Simple", "None"));
   public final NumberSetting yaw = this.addSetting("yaw", new NumberSetting("Yaw", 0.0F, 0.0F, 360.0F).step(1.0F));
   public final BooleanSetting lockPitch = this.addSetting("pitch-lock", new BooleanSetting("Pitch Lock", true));
   public final NumberSetting pitch = this.addSetting("pitch", new NumberSetting("Pitch", 85.0F, 0.0F, 90.0F).step(1.0F));
   public final BooleanSetting restart = this.addSetting("restart", new BooleanSetting("Restart", true));
   public final NumberSetting restartDelay = this.addSetting("restart-delay", new NumberSetting("Restart Delay", 7.0F, 0.0F, 20.0F).step(1.0F));
   public final BooleanSetting sprint = this.addSetting("sprint-constantly", new BooleanSetting("Sprint Constantly", true));
   public final BooleanSetting manualTakeoff = this.addSetting("manual-takeoff", new BooleanSetting("Manual Takeoff", false));
   public final BooleanSetting replace = this.addSetting("elytra-replace", new BooleanSetting("Elytra Replace", false));
   public final NumberSetting replaceDurability = this.addSetting("replace-durability", new NumberSetting("Replace Durability", 2.0F, 1.0F, 431.0F).step(1.0F));
   public final ModeSetting chestSwap = this.addSetting("chest-swap", new ModeSetting("Chest Swap", "Never", "Always", "Never", "WaitForGround"));
   public final BooleanSetting autoReplenish = this.addSetting("replenish-fireworks", new BooleanSetting("Replenish Fireworks", false));
   public final NumberSetting replenishSlot = this.addSetting("replenish-slot", new NumberSetting("Replenish Slot", 9.0F, 1.0F, 9.0F).step(1.0F));
   public final BooleanSetting autoPilot = this.addSetting("auto-pilot", new BooleanSetting("Auto Pilot", false));
   public final BooleanSetting useFireworks = this.addSetting("use-fireworks", new BooleanSetting("Use Fireworks", false));
   public final NumberSetting autoPilotFireworkDelay = this.addSetting("firework-delay", new NumberSetting("Firework Delay", 8.0F, 1.0F, 20.0F).step(0.5F));
   public final NumberSetting autoPilotMinimumHeight = this.addSetting(
      "minimum-height", new NumberSetting("Minimum Height", 120.0F, -128.0F, 260.0F).step(1.0F)
   );
   private ElytraFlightModes modeType = ElytraFlightModes.Vanilla;
   private ElytraFlightMode currentMode;
   private boolean modeRequestedDisable;
   private volatile boolean positionPacketReceived;
   private boolean moveIsVelocity;
   private class_746 recastPlayer;
   private boolean previousGliding;
   private ListenerHandle<PlayerMoveEvent> groundListener;
   private ListenerHandle<TickEvent> instaDropListener;

   public ElytraFlyModule() {
      super("elytra-fly", "Elytra Fly", "Gives you more control over your elytra.", ModuleCategory.MOVEMENT);
      this.usesNotifications();
      this.flightMode.onChange(this::onModeChanged);
      this.autoTakeOff.visibleWhen(this::steerableMode);
      this.fallMultiplier.visibleWhen(this::steerableMode);
      this.horizontalSpeed.visibleWhen(this::steerableMode);
      this.verticalSpeed.visibleWhen(this::steerableMode);
      this.acceleration.visibleWhen(this::steerableMode);
      this.accelerationStep.visibleWhen(() -> this.steerableMode() && this.acceleration.get());
      this.accelerationMin.visibleWhen(() -> this.steerableMode() && this.acceleration.get());
      this.stopInWater.visibleWhen(() -> !this.flightMode.is("Bounce"));
      this.autoHover.visibleWhen(() -> !this.flightMode.is("Bounce"));
      this.noCrash.visibleWhen(() -> !this.flightMode.is("Bounce"));
      this.crashLookAhead.visibleWhen(() -> this.noCrash.get() && !this.flightMode.is("Bounce"));
      this.instaDrop.visibleWhen(() -> !this.flightMode.is("Bounce"));
      this.pitch40lowerBounds.visibleWhen(() -> this.flightMode.is("Pitch40"));
      this.pitch40upperBounds.visibleWhen(() -> this.flightMode.is("Pitch40"));
      this.pitch40rotationSpeedUp.visibleWhen(() -> this.flightMode.is("Pitch40"));
      this.pitch40rotationSpeedDown.visibleWhen(() -> this.flightMode.is("Pitch40"));
      this.autoJump.visibleWhen(() -> this.flightMode.is("Bounce"));
      this.yawLockMode.visibleWhen(() -> this.flightMode.is("Bounce"));
      this.yaw.visibleWhen(() -> this.flightMode.is("Bounce") && this.yawLockMode.is("Simple"));
      this.lockPitch.visibleWhen(() -> this.flightMode.is("Bounce"));
      this.pitch.visibleWhen(() -> this.flightMode.is("Bounce") && this.lockPitch.get());
      this.restart.visibleWhen(() -> this.flightMode.is("Bounce"));
      this.restartDelay.visibleWhen(() -> this.flightMode.is("Bounce") && this.restart.get());
      this.sprint.visibleWhen(() -> this.flightMode.is("Bounce"));
      this.manualTakeoff.visibleWhen(() -> this.flightMode.is("Bounce"));
      this.replaceDurability.visibleWhen(this.replace::get);
      this.replenishSlot.visibleWhen(this.autoReplenish::get);
      this.autoPilot.visibleWhen(this::steerableMode);
      this.useFireworks.visibleWhen(() -> this.autoPilot.get() && this.steerableMode());
      this.autoPilotFireworkDelay.visibleWhen(() -> this.useFireworks.get() && this.steerableMode());
      this.autoPilotMinimumHeight.visibleWhen(() -> this.autoPilot.get() && this.steerableMode());
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   private boolean steerableMode() {
      return !this.flightMode.is("Pitch40") && !this.flightMode.is("Bounce");
   }

   public ElytraFlightModes flightModeType() {
      return this.modeType;
   }

   @Override
   protected void onEnable() {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (player == null) {
         this.requestDisable();
      } else {
         this.cancelGroundListener();
         this.cancelInstaDropListener();
         this.modeRequestedDisable = false;
         this.positionPacketReceived = false;
         this.moveIsVelocity = false;
         this.recastPlayer = player;
         this.previousGliding = player.method_6128();
         this.modeType = ElytraFlightModes.valueOf(this.flightMode.get());
         this.currentMode = this.createMode(this.modeType);
         this.currentMode.onActivate();
         if (!this.modeRequestedDisable) {
            if ((this.chestSwap.is("Always") || this.chestSwap.is("WaitForGround"))
               && player.method_6118(class_1304.field_6174).method_7909() != class_1802.field_8833) {
               ElytraFlyInventory.chestSwap();
            }

            active = this;
            this.on(PlayerMoveEvent.TYPE, Stage.PRE, EventPriority.HIGHEST, this::captureMoveAliasing);
            this.on(PlayerMoveEvent.TYPE, this::onPlayerMove);
            this.on(TickEvent.TYPE, Stage.PRE, this::onPreTick);
            this.on(TickEvent.TYPE, Stage.POST, this::onPostTick);
            this.on(PacketSendEvent.TYPE, this::onPacketSend);
            this.on(PacketReceiveNettyEvent.TYPE, this::onPacketReceive);
         }
      }
   }

   @Override
   protected void onDisable() {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      ElytraFlightMode mode = this.currentMode;
      this.currentMode = null;

      try {
         if (player != null && mode != null) {
            if (this.autoPilot.get()) {
               mc.field_1690.field_1894.method_23481(false);
            }

            if (this.chestSwap.is("Always") && player.method_6118(class_1304.field_6174).method_7909() == class_1802.field_8833) {
               ElytraFlyInventory.chestSwap();
            } else if (this.chestSwap.is("WaitForGround")) {
               this.enableGroundListener();
            }

            if (player.method_6128() && this.instaDrop.get()) {
               this.enableInstaDropListener();
            }
         }

         if (mode != null) {
            mode.onDeactivate();
         }
      } finally {
         active = null;
         this.recastPlayer = null;
      }
   }

   private void onModeChanged() {
      this.modeType = ElytraFlightModes.valueOf(this.flightMode.get());
      if (this.isEnabled() && this.currentMode != null) {
         this.currentMode = this.createMode(this.modeType);
      }

      if (this.modeType == ElytraFlightModes.Pitch40) {
         this.autoPilot.set(false);
      }
   }

   private ElytraFlightMode createMode(ElytraFlightModes type) {
      return switch (type) {
         case Vanilla -> new VanillaMode(this);
         case Packet -> new PacketMode(this);
         case Pitch40 -> new Pitch40Mode(this);
         case Bounce -> new BounceMode(this);
      };
   }

   public void disableFromMode() {
      this.modeRequestedDisable = true;
      this.requestDisable();
   }

   private void onPreTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1724 != null && mc.field_1687 != null) {
         ElytraFlightMode mode = this.currentMode;
         if (mode != null) {
            if (this.positionPacketReceived) {
               this.positionPacketReceived = false;
               mode.zeroAcceleration();
               mode.onPositionPacket();
            }

            mode.onPreTick();
         }
      } else {
         this.requestDisable();
      }
   }

   private void onPostTick(TickEvent event) {
      ElytraFlightMode mode = this.currentMode;
      if (class_310.method_1551().field_1724 != null && mode != null) {
         mode.onTick();
      }
   }

   private void onPacketSend(PacketSendEvent event) {
      class_310 mc = class_310.method_1551();
      ElytraFlightMode mode = this.currentMode;
      if (mc.method_18854() && mc.field_1724 != null && mode != null) {
         mode.onPacketSend(event.packet);
      }
   }

   private void onPacketReceive(PacketReceiveNettyEvent event) {
      if (event.packet instanceof class_2708) {
         this.positionPacketReceived = true;
      }
   }

   private void captureMoveAliasing(PlayerMoveEvent event) {
      class_746 player = class_310.method_1551().field_1724;
      if (player == null) {
         this.moveIsVelocity = false;
      } else {
         class_243 velocity = player.method_18798();
         this.moveIsVelocity = velocity.field_1352 == event.x && velocity.field_1351 == event.y && velocity.field_1350 == event.z;
      }
   }

   private void onPlayerMove(PlayerMoveEvent event) {
      boolean aliased = this.moveIsVelocity;
      this.moveIsVelocity = false;
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      ElytraFlightMode mode = this.currentMode;
      if (player != null && mc.field_1687 != null && mode != null) {
         if (player.method_6118(class_1304.field_6174).method_57826(class_9334.field_54197)) {
            mode.autoTakeoff();
            boolean bounce = this.modeType == ElytraFlightModes.Bounce;
            if (player.method_6128()) {
               if (!bounce) {
                  mode.velX = 0.0;
                  mode.velY = event.y;
                  mode.velZ = 0.0;
                  mode.forward = class_243.method_1030(0.0F, player.method_36454()).method_1021(0.1);
                  mode.right = class_243.method_1030(0.0F, player.method_36454() + 90.0F).method_1021(0.1);
                  if (player.method_5799() && this.stopInWater.get()) {
                     player.field_3944.method_52787(new class_2848(player, class_2849.field_12982));
                     return;
                  }

                  mode.handleFallMultiplier();
                  mode.handleAutopilot();
                  mode.handleAcceleration();
                  mode.handleHorizontalSpeed(event);
                  mode.handleVerticalSpeed(event);
               }

               if (!bounce) {
                  int chunkX = (int)((player.method_23317() + mode.velX) / 16.0);
                  int chunkZ = (int)((player.method_23321() + mode.velZ) / 16.0);
                  if (this.dontGoIntoUnloadedChunks.get()) {
                     if (mc.field_1687.method_2935().method_12123(chunkX, chunkZ)) {
                        setMovement(player, aliased, event, mode.velX, mode.velY, mode.velZ);
                     } else {
                        mode.zeroAcceleration();
                        setMovement(player, aliased, event, 0.0, mode.velY, 0.0);
                     }
                  } else {
                     setMovement(player, aliased, event, mode.velX, mode.velY, mode.velZ);
                  }

                  mode.onPlayerMove();
               }
            } else if (mode.lastForwardPressed && !bounce) {
               mc.field_1690.field_1894.method_23481(false);
               mode.lastForwardPressed = false;
            }

            if (this.noCrash.get() && player.method_6128() && !bounce) {
               class_243 position = player.method_73189();
               class_243 lookAheadPos = position.method_1019(player.method_18798().method_1029().method_1021(this.crashLookAhead.getInt()));
               class_3959 context = new class_3959(
                  position,
                  new class_243(lookAheadPos.field_1352, player.method_23318(), lookAheadPos.field_1350),
                  class_3960.field_17558,
                  class_242.field_1348,
                  player
               );
               class_3965 hitResult = mc.field_1687.method_17742(context);
               if (hitResult != null && hitResult.method_17783() == class_240.field_1332) {
                  setMovement(player, aliased, event, 0.0, mode.velY, 0.0);
               }
            }

            if (this.autoHover.get() && player.field_3913.field_54155.comp_3164() && !isFreecamActive() && player.method_6128() && !bounce) {
               class_2338 below = player.method_24515().method_10074();
               class_2680 underState = mc.field_1687.method_8320(below);
               class_2680 under2State = mc.field_1687.method_8320(below.method_10074());
               boolean underCollidable = ((AccessorElytraFlyBlockBehaviour)underState.method_26204()).anubis$elytraFlyHasCollision()
                  || !underState.method_26227().method_15769();
               boolean under2Collidable = ((AccessorElytraFlyBlockBehaviour)under2State.method_26204()).anubis$elytraFlyHasCollision()
                  || !under2State.method_26227().method_15769();
               if (!underCollidable && under2Collidable) {
                  setMovement(player, aliased, event, event.x, -0.1F, event.z);
                  player.method_36457(class_3532.method_15363(player.method_61414(0.0F), -50.0F, 20.0F));
               }

               if (underCollidable) {
                  setMovement(player, aliased, event, event.x, -0.03F, event.z);
                  player.method_36457(class_3532.method_15363(player.method_61414(0.0F), -50.0F, 20.0F));
                  if (player.method_73189().field_1351 <= below.method_10264() + 1.34F) {
                     setMovement(player, aliased, event, event.x, 0.0, event.z);
                     player.method_5660(false);
                  }
               }
            }
         }
      }
   }

   private static void setMovement(class_746 player, boolean aliased, PlayerMoveEvent event, double x, double y, double z) {
      event.x = x;
      event.y = y;
      event.z = z;
      if (aliased) {
         player.method_18800(x, y, z);
      }
   }

   public boolean canPacketEfly(class_746 player) {
      return this.modeType == ElytraFlightModes.Packet
         && player.method_6118(class_1304.field_6174).method_57826(class_9334.field_54197)
         && !player.method_24828();
   }

   public boolean modifyFallFlying(class_746 player, boolean original) {
      boolean gliding = original || this.canPacketEfly(player);
      if (player != this.recastPlayer) {
         this.recastPlayer = player;
         this.previousGliding = false;
      }

      boolean wasGliding = this.previousGliding;
      this.previousGliding = gliding;
      return wasGliding && !gliding && this.modeType == ElytraFlightModes.Bounce ? BounceMode.recastElytra(player) : gliding;
   }

   private void enableGroundListener() {
      if (this.groundListener == null) {
         this.groundListener = PlayerMoveEvent.TYPE.listen(this, Stage.PRE, EventPriority.NORMAL, event -> {
            class_746 player = class_310.method_1551().field_1724;
            if (player != null && player.method_24828() && player.method_6118(class_1304.field_6174).method_7909() == class_1802.field_8833) {
               ElytraFlyInventory.chestSwap();
               this.cancelGroundListener();
            }
         });
      }
   }

   private void cancelGroundListener() {
      if (this.groundListener != null) {
         this.groundListener.unregister();
         this.groundListener = null;
      }
   }

   private void enableInstaDropListener() {
      if (this.instaDropListener == null) {
         this.instaDropListener = TickEvent.TYPE.listen(this, Stage.POST, EventPriority.NORMAL, event -> {
            class_746 player = class_310.method_1551().field_1724;
            if (player != null && player.method_6128()) {
               player.method_18800(0.0, 0.0, 0.0);
               player.field_3944.method_52787(new class_5911(true, player.field_5976));
            } else {
               this.cancelInstaDropListener();
            }
         });
      }
   }

   private void cancelInstaDropListener() {
      if (this.instaDropListener != null) {
         this.instaDropListener.unregister();
         this.instaDropListener = null;
      }
   }

   private static boolean isFreecamActive() {
      FreecamModule freecam = ModuleRegistry.get(FreecamModule.class);
      return freecam != null && freecam.isEnabled();
   }
}
