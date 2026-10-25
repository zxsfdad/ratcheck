package net.anubis.client.module.impl.misc;

import com.example.client.mixin.AccessorMovementClientInput;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.movement.AutoWalkModule;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10185;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_1923;
import net.minecraft.class_241;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_315;
import net.minecraft.class_3532;
import net.minecraft.class_4050;
import net.minecraft.class_5498;
import net.minecraft.class_631;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class FreecamModule extends ClientModule {
   private static final int WALK_FWD = 1;
   private static final int WALK_BACK = 2;
   private static final int WALK_LEFT = 4;
   private static final int WALK_RIGHT = 8;
   private static final int WALK_JUMP = 16;
   private static final int WALK_SNEAK = 32;
   private static final int WALK_SPRINT = 64;
   private static final int WALK_FLAT = 15;
   private static final int LEARN_TICKS = 40;
   private static final int CHUNK_CAP = 32;
   private static final float SCROLL_STEP = 1.2F;
   public static volatile FreecamModule active;
   private final NumberSetting speed = this.addSetting("speed", new NumberSetting("Speed", 1.0F, 0.1F, 50.0F).step(0.1F));
   private final NumberSetting verticalSpeed = this.addSetting("vertical-speed", new NumberSetting("Vertical", 1.0F, 0.2F, 5.0F).step(0.05F));
   private final BooleanSetting smoothing = this.addSetting("smoothing", new BooleanSetting("Smooth", true));
   private final BooleanSetting showOwnBody = this.addSetting("show-own-body", new BooleanSetting("Body", true));
   private final BooleanSetting showHands = this.addSetting("show-hands", new BooleanSetting("Hands", true));
   private final BooleanSetting bodyFollowsKeys = this.addSetting("body-uses-movement-keys", new BooleanSetting("Keys Move Body", false));
   private final BooleanSetting keepWalking = this.addSetting("keep-walking", new BooleanSetting("Keep Walking", true));
   private final BooleanSetting scrollSpeed = this.addSetting("scroll-speed", new BooleanSetting("Scroll Speed", true));
   private final BooleanSetting scrollResets = this.addSetting("scroll-resets", new BooleanSetting("Reset Scroll Speed", true));
   private final NumberSetting chunkDistance = this.addSetting("chunk-distance", new NumberSetting("Chunks", 12.0F, 2.0F, 32.0F).step(1.0F));
   private class_243 pos = class_243.field_1353;
   private class_243 prev = class_243.field_1353;
   private float yaw;
   private float pitch;
   private boolean flyingBefore;
   private boolean cullBefore;
   private class_5498 viewBefore;
   private boolean viewPushed;
   private class_1923 focus;
   private int radius = -1;
   private boolean waiting;
   private boolean live;
   private int walk;
   private int learn;
   private float scrolled = 1.0F;

   public FreecamModule() {
      super("freecam", "Freecam", "Detached camera.", ModuleCategory.MISC);
      this.scrollResets.visibleWhen(this.scrollSpeed::get);
      this.speed.onChange(() -> this.scrolled = 1.0F);
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      this.waiting = true;
      this.live = false;
      this.on(TickEvent.TYPE, this::onTick);
   }

   @Override
   protected void onDisable() {
      class_310 mc = class_310.method_1551();
      boolean wasLive = this.live;
      this.waiting = false;
      this.live = false;
      if (active == this) {
         active = null;
      }

      this.walk = 0;
      this.learn = 0;
      this.scrolled = 1.0F;
      if (this.viewPushed) {
         mc.field_1690.method_31043(this.viewBefore);
         this.viewPushed = false;
      }

      if (wasLive) {
         mc.field_1730 = this.cullBefore;
         if (mc.field_1761 != null) {
            mc.field_1761.method_2925();
         }

         this.releaseChunks(mc);
         if (mc.field_1724 != null) {
            mc.field_1724.method_31549().field_7479 = this.flyingBefore;
         }
      }
   }

   private void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      this.open(mc);
      class_746 player = mc.field_1724;
      if (this.live && player != null) {
         if (this.learn > 0) {
            this.learn--;
            this.absorb(player, false);
         }

         this.prev = this.pos;
         if (player.method_31549().field_7477) {
            player.method_31549().field_7479 = false;
         }

         this.stepCamera(mc.field_1690);
         this.followChunks(mc);
      }
   }

   private void open(class_310 mc) {
      class_746 player = mc.field_1724;
      if (this.waiting && player != null && mc.field_1687 != null) {
         this.flyingBefore = player.method_31549().field_7479;
         this.pos = this.prev = new class_243(player.method_23317(), player.method_23318() + player.method_18381(class_4050.field_18076), player.method_23321());
         this.yaw = player.method_36454();
         this.pitch = player.method_36455();
         this.viewPushed = false;
         if (this.showOwnBody.get()) {
            this.viewBefore = mc.field_1690.method_31044();
            if (this.viewBefore.method_31034()) {
               mc.field_1690.method_31043(class_5498.field_26665);
               this.viewPushed = true;
            }
         }

         this.waiting = false;
         this.live = true;
         active = this;
         this.walk = 0;
         this.absorb(player, true);
         this.learn = 40;
         this.focus = null;
         this.radius = -1;
         this.cullBefore = mc.field_1730;
         mc.field_1730 = false;
         this.followChunks(mc);
         drive(mc);
      }
   }

   private void stepCamera(class_315 options) {
      double ahead = (options.field_1894.method_1434() ? 1 : 0) - (options.field_1881.method_1434() ? 1 : 0);
      double side = (options.field_1913.method_1434() ? 1 : 0) - (options.field_1849.method_1434() ? 1 : 0);
      double rise = (options.field_1903.method_1434() ? 1 : 0) - (options.field_1832.method_1434() ? 1 : 0);
      double step = this.flySpeed() * (this.smoothing.get() ? 0.5 : 1.0);
      class_243 look = class_243.method_1030(0.0F, this.yaw);
      class_243 left = new class_243(look.field_1350, 0.0, -look.field_1352);
      this.pos = this.pos
         .method_1031(
            (look.field_1352 * ahead + left.field_1352 * side) * step,
            rise * step * this.verticalSpeed.get(),
            (look.field_1350 * ahead + left.field_1350 * side) * step
         );
   }

   private float flySpeed() {
      return !this.scrollResets.get() ? this.speed.get() : class_3532.method_15363(this.speed.get() * this.scrolled, this.speed.min(), this.speed.max());
   }

   private void absorb(class_746 player, boolean opening) {
      class_315 options = class_310.method_1551().field_1690;
      class_10185 keys = player.field_3913.field_54155;
      boolean carry = this.keepWalking.get();
      boolean typed = carry && (opening || this.bodyFollowsKeys.get());
      if (typed) {
         this.setWalk(1, options.field_1894.method_1434() || keys.comp_3159());
         this.setWalk(2, options.field_1881.method_1434() || keys.comp_3160());
         this.setWalk(4, options.field_1913.method_1434() || keys.comp_3161());
         this.setWalk(8, options.field_1849.method_1434() || keys.comp_3162());
         this.setWalk(16, options.field_1903.method_1434() || keys.comp_3163());
         this.setWalk(32, options.field_1832.method_1434() || keys.comp_3164() || opening && player.method_5715());
         this.setWalk(64, options.field_1867.method_1434() || keys.comp_3165());
      } else if (player.method_5715()) {
         this.walk |= 32;
      }

      AutoWalkModule autoWalk = ModuleRegistry.get(AutoWalkModule.class);
      if (autoWalk != null && autoWalk.isEnabled()) {
         this.walk |= 1;
      }

      if (carry) {
         if (player.field_6250 > 0.02F) {
            this.walk |= 1;
         }

         if (player.field_6250 < -0.02F) {
            this.walk |= 2;
         }

         if (player.field_6212 > 0.02F) {
            this.walk |= 4;
         }

         if (player.field_6212 < -0.02F) {
            this.walk |= 8;
         }

         if (!opening || (this.walk & 15) == 0) {
            this.aimFromMotion(player);
         }
      }
   }

   private void setWalk(int bit, boolean on) {
      if (on) {
         this.walk |= bit;
      }
   }

   private void aimFromMotion(class_746 player) {
      class_243 motion = player.method_18798();
      double vx = motion.field_1352;
      double vz = motion.field_1350;
      if (!(vx * vx + vz * vz < 1.0E-8)) {
         class_243 look = class_243.method_1030(0.0F, player.method_36454());
         double along = vx * look.field_1352 + vz * look.field_1350;
         double across = vz * look.field_1352 - vx * look.field_1350;
         if (Math.abs(along) >= Math.abs(across)) {
            if (along > 0.01) {
               this.walk |= 1;
            } else if (along < -0.01) {
               this.walk |= 2;
            }
         } else if (across > 0.01) {
            this.walk |= 4;
         } else if (across < -0.01) {
            this.walk |= 8;
         }
      }
   }

   public static void drive(class_310 mc) {
      FreecamModule module = active;
      if (module != null && mc.field_1724 != null) {
         module.pushWalk(mc);
      }
   }

   private void pushWalk(class_310 mc) {
      class_315 options = mc.field_1690;
      boolean liveKeys = this.bodyFollowsKeys.get();
      boolean forward = this.on(1) || liveKeys && options.field_1894.method_1434();
      boolean back = this.on(2) || liveKeys && options.field_1881.method_1434();
      boolean left = this.on(4) || liveKeys && options.field_1913.method_1434();
      boolean right = this.on(8) || liveKeys && options.field_1849.method_1434();
      boolean jump = this.on(16);
      boolean sneak = this.on(32) || liveKeys && options.field_1832.method_1434();
      boolean sprint = this.on(64) || liveKeys && options.field_1867.method_1434();
      if (liveKeys) {
         if (options.field_1881.method_1434()) {
            this.walk &= -2;
         }

         if (options.field_1894.method_1434()) {
            this.walk &= -3;
         }

         if (options.field_1849.method_1434()) {
            this.walk &= -5;
         }

         if (options.field_1913.method_1434()) {
            this.walk &= -9;
         }
      }

      class_746 player = mc.field_1724;
      player.field_3913.field_54155 = new class_10185(forward, back, left, right, jump, sneak, sprint);
      float sideways = (left ? 1.0F : 0.0F) - (right ? 1.0F : 0.0F);
      float ahead = (forward ? 1.0F : 0.0F) - (back ? 1.0F : 0.0F);
      ((AccessorMovementClientInput)player.field_3913).anubis$setMoveVector(new class_241(sideways, ahead).method_35581());
      player.method_5660(sneak);
   }

   private boolean on(int bit) {
      return (this.walk & bit) != 0;
   }

   private void followChunks(class_310 mc) {
      if (this.live && mc.field_1687 != null && mc.field_1724 != null) {
         class_631 cache = mc.field_1687.method_2935();
         class_1923 camera = new class_1923(class_3532.method_15357(this.pos.field_1352) >> 4, class_3532.method_15357(this.pos.field_1350) >> 4);
         class_1923 feet = mc.field_1724.method_31476();
         int span = Math.max(Math.abs(camera.field_9181 - feet.field_9181), Math.abs(camera.field_9180 - feet.field_9180));
         int preference = class_3532.method_15340(this.chunkDistance.getInt(), 2, 32);
         class_1923 next = span > preference
            ? new class_1923(feet.field_9181 + (camera.field_9181 - feet.field_9181) / 2, feet.field_9180 + (camera.field_9180 - feet.field_9180) / 2)
            : camera;
         int needed = class_3532.method_15340(Math.max(preference, span / 2 + 3), 2, 32);
         if (needed != this.radius) {
            cache.method_20180(needed);
            this.radius = needed;
         }

         if (this.focus == null || next.field_9181 != this.focus.field_9181 || next.field_9180 != this.focus.field_9180) {
            cache.method_20317(next.field_9181, next.field_9180);
            this.focus = next;
            if (mc.field_1769 != null) {
               mc.field_1769.method_3292();
            }
         }
      }
   }

   private void releaseChunks(class_310 mc) {
      this.focus = null;
      this.radius = -1;
      if (mc.field_1687 != null) {
         class_631 cache = mc.field_1687.method_2935();
         if (mc.field_1724 != null) {
            class_1923 feet = mc.field_1724.method_31476();
            cache.method_20317(feet.field_9181, feet.field_9180);
         }

         cache.method_20180(mc.field_1690.method_38521());
         if (mc.field_1769 != null) {
            mc.field_1769.method_3292();
         }
      }
   }

   public boolean drawsSelf(class_1297 entity, class_1657 viewer) {
      class_310 mc = class_310.method_1551();
      return this.showOwnBody.get() && mc.field_1724 != null && entity == mc.field_1724 && viewer == mc.field_1724;
   }

   public boolean keepHands() {
      return this.showHands.get();
   }

   public boolean showBody() {
      return this.showOwnBody.get();
   }

   public static boolean scroll(double amount) {
      FreecamModule module = active;
      if (module != null && module.scrollSpeed.get() && amount != 0.0) {
         float factor = amount > 0.0 ? 1.2F : 0.8333333F;
         NumberSetting speed = module.speed;
         if (module.scrollResets.get()) {
            module.scrolled = class_3532.method_15363(module.scrolled * factor, speed.min() / speed.get(), speed.max() / speed.get());
         } else {
            speed.set(speed.get() * factor);
         }

         return true;
      } else {
         return false;
      }
   }

   public void turn(double yRot, double xRot) {
      this.yaw += (float)yRot * 0.15F;
      this.pitch = class_3532.method_15363(this.pitch + (float)xRot * 0.15F, -90.0F, 90.0F);
   }

   public double getX(float tickDelta) {
      return class_3532.method_16436(tickDelta, this.prev.field_1352, this.pos.field_1352);
   }

   public double getY(float tickDelta) {
      return class_3532.method_16436(tickDelta, this.prev.field_1351, this.pos.field_1351);
   }

   public double getZ(float tickDelta) {
      return class_3532.method_16436(tickDelta, this.prev.field_1350, this.pos.field_1350);
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }
}
