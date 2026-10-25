package net.anubis.client.module.impl.combat;

import java.util.concurrent.ThreadLocalRandom;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.combat.CombatTarget;
import net.anubis.client.util.pvp.Rotation;
import net.anubis.client.util.pvp.RotationUtils;
import net.anubis.client.util.pvp.TargetingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_156;
import net.minecraft.class_1657;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class AimAssistModule extends ClientModule {
   private static final long LAST_HIT_MS = 1500L;
   private static final double STICK_RANGE = 0.5;
   private static final double STICK_FOV = 1.3;
   private static final long MOUSE_IDLE_MS = 150L;
   private static final double[] STILL = new double[]{0.0, 0.0};
   private final NumberSetting range = this.addSetting("range", new NumberSetting("Range", 4.0F, 3.0F, 6.0F).step(0.1F));
   private final NumberSetting fov = this.addSetting("fov", new NumberSetting("FOV", 60.0F, 10.0F, 180.0F).step(5.0F));
   private final NumberSetting speed = this.addSetting("speed", new NumberSetting("Speed", 60.0F, 10.0F, 180.0F).step(5.0F));
   private final BooleanSetting weaponsOnly = this.addSetting("weapons-only", new BooleanSetting("Weapons Only", true));
   private final BooleanSetting vertical = this.addSetting("vertical", new BooleanSetting("Vertical", true));
   private class_1657 target;
   private double pace;
   private double pendingYaw;
   private double pendingPitch;
   private long movedAt;
   private double userYawSign;
   private double userPitchSign;

   public AimAssistModule() {
      super("aim-assist", "Aim Assist", "Eases your crosshair onto the nearest enemy player while you move the mouse.", ModuleCategory.COMBAT);
   }

   @Override
   public int settingsVersion() {
      return 1;
   }

   @Override
   public void migrateSettings(int savedVersion) {
      if (savedVersion < 1) {
         this.speed.set(60.0F);
      }
   }

   @Override
   protected void onDisable() {
      this.follow(null);
   }

   @EventHandler
   public void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (MeleeAttacks.canAct(mc)
         && !player.method_6115()
         && !mc.field_1690.field_1904.method_1434()
         && (!this.weaponsOnly.get() || MeleeAttacks.isMeleeWeapon(player.method_6047()))) {
         this.follow(this.choose(mc, player));
      } else {
         this.follow(null);
      }
   }

   public double[] nudge(class_746 player, double userYaw, double userPitch, double seconds) {
      long now = class_156.method_658();
      if (userYaw != 0.0 || userPitch != 0.0) {
         this.movedAt = now;
      }

      if (userYaw != 0.0) {
         this.userYawSign = Math.signum(userYaw);
      }

      if (userPitch != 0.0) {
         this.userPitchSign = Math.signum(userPitch);
      }

      Rotation want = this.target != null && now - this.movedAt <= 150L ? this.aim(player, player.method_33571(), core(this.target)) : null;
      if (want == null) {
         this.pendingYaw = 0.0;
         this.pendingPitch = 0.0;
         this.userYawSign = 0.0;
         this.userPitchSign = 0.0;
         return STILL;
      }

      double dt = Math.min(seconds, 0.05);
      float yawError = class_3532.method_15393(want.yaw() - player.method_36454());
      float pitchError = this.vertical.get() ? want.pitch() - player.method_36455() : 0.0F;
      if (this.userYawSign * yawError < 0.0) {
         yawError = 0.0F;
      }

      if (this.userPitchSign * pitchError < 0.0) {
         pitchError = 0.0F;
      }

      double cap = this.speed.get() * this.pace * dt;
      double ease = 1.0 - Math.exp(-12.0 * dt);
      double k = RotationUtils.mouseScale(class_310.method_1551(), player);
      double degreesPerCount = (float)k * 0.15F;
      this.pendingYaw = this.pendingYaw + class_3532.method_15350(yawError * ease, -cap, cap) / degreesPerCount;
      this.pendingPitch = this.pendingPitch + class_3532.method_15350(pitchError * ease * 0.6, -cap, cap) / degreesPerCount;
      long yawCounts = (long)this.pendingYaw;
      long pitchCounts = (long)this.pendingPitch;
      this.pendingYaw -= yawCounts;
      this.pendingPitch -= pitchCounts;
      return new double[]{yawCounts * k, pitchCounts * k};
   }

   private void follow(class_1657 next) {
      if (next != this.target) {
         this.target = next;
         this.pace = ThreadLocalRandom.current().nextDouble(0.85, 1.15);
         this.pendingYaw = 0.0;
         this.pendingPitch = 0.0;
      }
   }

   private class_1657 choose(class_310 mc, class_746 player) {
      class_243 eye = player.method_33571();
      double halfFov = this.fov.get() / 2.0;
      if (CombatTarget.lastHit(1500L) instanceof class_1657 hit && this.offAngle(player, eye, hit, this.range.get()) <= halfFov) {
         return hit;
      } else {
         if (this.target != null
            && this.target.method_73183() == player.method_73183()
            && this.offAngle(player, eye, this.target, this.range.get() + 0.5) <= halfFov * 1.3) {
            return this.target;
         }

         class_1657 best = null;
         double bestAngle = halfFov;

         for (class_1657 candidate : mc.field_1687.method_18456()) {
            double angle = this.offAngle(player, eye, candidate, this.range.get());
            if (!(angle > bestAngle)) {
               best = candidate;
               bestAngle = angle;
            }
         }

         return best;
      }
   }

   private double offAngle(class_746 player, class_243 eye, class_1657 candidate, double reach) {
      if (MeleeAttacks.isEnemy(player, candidate) && !candidate.method_5767()) {
         if (eye.method_1022(TargetingUtils.closestPoint(eye, candidate.method_5829())) > reach) {
            return Double.MAX_VALUE;
         }

         class_238 core = core(candidate);
         if (!MeleeAttacks.lineIsClear(player, eye, core.method_1005())) {
            return Double.MAX_VALUE;
         }

         Rotation want = this.aim(player, eye, core);
         return want == null ? 0.0 : RotationUtils.angleTo(RotationUtils.entityRotation(player), want);
      } else {
         return Double.MAX_VALUE;
      }
   }

   private Rotation aim(class_746 player, class_243 eye, class_238 core) {
      class_243 view = player.method_5828(1.0F);
      if (core.method_992(eye, eye.method_1019(view.method_1021(this.range.get() + 1.0))).isPresent()) {
         return null;
      }

      class_243 near = TargetingUtils.closestPoint(eye.method_1019(view.method_1021(eye.method_1022(core.method_1005()))), core);
      return RotationUtils.fromTo(eye, near);
   }

   private static class_238 core(class_1657 player) {
      class_238 box = player.method_5829();
      return box.method_35580(box.method_17939() * 0.25, box.method_17940() * 0.1, box.method_17939() * 0.25);
   }
}
