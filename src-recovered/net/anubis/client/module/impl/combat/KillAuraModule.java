package net.anubis.client.module.impl.combat;

import net.anubis.client.event.EventHandler;
import net.anubis.client.event.EventPriority;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.CombatMacroLock;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.combat.crystal.CrystalDamage;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.util.pvp.InputControl;
import net.anubis.client.util.pvp.Rotation;
import net.anubis.client.util.pvp.RotationUtils;
import net.anubis.client.util.pvp.ServerRotation;
import net.anubis.client.util.pvp.TargetingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12125;
import net.minecraft.class_12392;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3966;
import net.minecraft.class_746;
import net.minecraft.class_9334;

@Environment(EnvType.CLIENT)
public final class KillAuraModule extends ClientModule {
   private static final String PRIORITY_DISTANCE = "Distance";
   private static final String PRIORITY_HEALTH = "Health";
   private static final String PRIORITY_ANGLE = "Angle";
   private static final String CRITS_OFF = "Off";
   private static final String CRITS_WAIT = "Wait";
   private static final String CRITS_JUMP = "Jump";
   private static final int CRIT_HOLD_TICKS = 8;
   private static final int JUMP_LEAD = 7;
   private static final double REACH_MARGIN = 0.01;
   private static final double RIDING_BONUS = 2.5;
   private static final double MAX_STEP_SQ = 1.0;
   private static final double[] AIM_HEIGHTS = new double[]{0.85, 0.5, 0.15};
   private final BooleanSetting players = this.addSetting("players", new BooleanSetting("Players", true));
   private final BooleanSetting hostiles = this.addSetting("hostiles", new BooleanSetting("Hostiles", false));
   private final ModeSetting priority = this.addSetting("priority", new ModeSetting("Priority", "Distance", "Distance", "Health", "Angle"));
   private final ModeSetting crits = this.addSetting("crits", new ModeSetting("Crits", "Wait", "Off", "Wait", "Jump"));
   private class_1309 target;
   private int critWait;

   public KillAuraModule() {
      super("kill-aura", "Kill Aura", "Attacks nearby targets without moving your camera.", ModuleCategory.COMBAT);
   }

   @Override
   protected void onDisable() {
      this.target = null;
      this.critWait = 0;
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (MeleeAttacks.canAct(mc)
         && !player.method_29504()
         && !player.method_7325()
         && mc.field_1771 <= 0
         && MeleeAttacks.handsFree(mc, player)
         && !CombatMacroLock.heldByOther(this)
         && !MeleeAttacks.actedThisTick(player)
         && !MeleeAttacks.rightClickComing(mc)) {
         boolean riding = player.method_5765();
         class_1799 held = player.method_6047();
         boolean spear = held.method_57826(class_9334.field_63631);
         double reach = spear ? player.method_76693().method_76739(player) - 0.01 : player.method_55755() - 0.01 + (riding ? 2.5 : 0.0);
         class_243 eye = player.method_33571();
         this.target = this.pick(mc, player, eye, reach);
         if (this.target == null) {
            this.critWait = 0;
         } else {
            MaceModule mace = ModuleRegistry.get(MaceModule.class);
            boolean smash = !spear && mace != null && mace.smashReady(player, this.target);
            boolean ready = spear ? !player.method_75202(held, 0) : smash || MeleeAttacks.charge(player) >= 1.0F;
            boolean aiming = !ready && MeleeAttacks.ticksToFull(player) <= 1;
            boolean jump = !spear
               && this.crits.is("Jump")
               && !riding
               && player.method_24828()
               && !player.method_5799()
               && MeleeAttacks.ticksToFull(player) <= 7;
            if (jump) {
               InputControl.jump();
            }

            class_238 raw = this.target.method_5829();
            if (aiming) {
               raw = raw.method_997(stride(this.target));
            }

            class_243 point = aimPoint(player, eye, raw);
            if (point != null) {
               double side = Math.min(0.03, raw.method_17939() * 0.15);
               class_238 hitBox = raw.method_35580(side, 0.01, side);
               Rotation aim = RotationUtils.fromTo(eye, point);
               Rotation next = !riding && !hitBox.method_1006(eye) && ServerRotation.canRotate(player) && !ServerRotation.pending()
                  ? ServerRotation.next(player, aim)
                  : null;
               class_243 nextAt = hits(player, eye, hitBox, reach, next);
               if (!ready) {
                  if (aiming && nextAt != null) {
                     ServerRotation.request(aim);
                  }
               } else {
                  Rotation look = riding ? RotationUtils.entityRotation(player) : ServerRotation.sent();
                  class_243 at = hits(player, eye, hitBox, reach, look);
                  if (at != null || nextAt != null) {
                     if (spear) {
                        if (at == null) {
                           ServerRotation.request(aim);
                        } else if (!hitsOthers(player, this.target, eye, look) && MeleeAttacks.stab(mc, new class_3966(this.target, at))) {
                           MeleeAttacks.claimTick(mc);
                        }
                     } else {
                        boolean hold = smash ? mace.holdsSmash(player, this.target) : !this.crits.is("Off") && !riding && this.waitForCrit(player, jump);
                        if (hold) {
                           if (smash) {
                              InputControl.dropSprint();
                           }

                           if (nextAt != null && !mc.field_1690.field_1886.method_1434() && !MeleeAttacks.clickPending(mc)) {
                              ServerRotation.request(aim);
                           }
                        } else {
                           if (at == null) {
                              ServerRotation.request(aim);
                              at = nextAt;
                           }

                           if (MeleeAttacks.attack(mc, new class_3966(this.target, at), riding ? 2.5 : 0.0)) {
                              this.critWait = 0;
                              if (smash) {
                                 mace.noteSmash();
                              }

                              MeleeAttacks.claimTick(mc);
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         this.critWait = 0;
      }
   }

   private boolean waitForCrit(class_746 player, boolean jump) {
      if (MeleeAttacks.airborne(player)) {
         if (player.method_5624() || !(player.field_6017 > 0.0) && this.critWait++ < 8) {
            InputControl.dropSprint();
            return true;
         } else {
            return false;
         }
      } else {
         return player.method_24828() && (jump || player.field_3913.field_54155.comp_3163()) && this.critWait++ < 8;
      }
   }

   private class_1309 pick(class_310 mc, class_746 player, class_243 eye, double reach) {
      class_1309 best = null;
      double bestRank = 0.0;
      double bestDistance = 0.0;

      for (class_1297 entity : mc.field_1687.method_18112()) {
         if (entity instanceof class_1309 living) {
            class_238 raw = living.method_5829();
            double distance = eye.method_1022(TargetingUtils.closestPoint(eye, raw));
            if (!(distance > reach) && this.wanted(player, living)) {
               class_243 point = aimPoint(player, eye, raw);
               if (point != null) {
                  if (living == this.target) {
                     return living;
                  }

                  double rank = this.rank(player, eye, living, point, distance);
                  if (best != null) {
                     int order = Double.compare(rank, bestRank);
                     if (order == 0) {
                        order = Boolean.compare(living.field_6235 > 0, best.field_6235 > 0);
                     }

                     if (order == 0) {
                        order = Double.compare(distance, bestDistance);
                     }

                     if (order >= 0) {
                        continue;
                     }
                  }

                  best = living;
                  bestRank = rank;
                  bestDistance = distance;
               }
            }
         }
      }

      return best;
   }

   private double rank(class_746 player, class_243 eye, class_1309 living, class_243 point, double distance) {
      if (this.priority.is("Health")) {
         return CrystalDamage.health(living);
      }

      if (!this.priority.is("Angle")) {
         return distance;
      }

      Rotation sent = ServerRotation.sent();
      Rotation look = sent != null ? sent : RotationUtils.entityRotation(player);
      return RotationUtils.angleTo(look, RotationUtils.fromTo(eye, point));
   }

   private boolean wanted(class_746 player, class_1309 living) {
      if (!MeleeAttacks.isEnemy(player, living) || MeleeAttacks.wastedOnShield(player, living)) {
         return false;
      } else {
         return living instanceof class_1657 ? this.players.get() : this.hostiles.get() && MeleeAttacks.isHostile(living);
      }
   }

   private static class_243 aimPoint(class_746 player, class_243 eye, class_238 raw) {
      double side = Math.min(0.15, raw.method_17939() * 0.3);
      class_243 near = TargetingUtils.closestPoint(eye, raw.method_35580(side, Math.min(0.2, raw.method_17940() * 0.25), side));
      if (MeleeAttacks.lineIsClear(player, eye, near)) {
         return near;
      }

      class_243 center = raw.method_1005();

      for (double height : AIM_HEIGHTS) {
         class_243 point = new class_243(center.field_1352, raw.field_1322 + raw.method_17940() * height, center.field_1350);
         if (MeleeAttacks.lineIsClear(player, eye, point)) {
            return point;
         }
      }

      return null;
   }

   private static class_243 stride(class_1309 entity) {
      double dx = entity.method_23317() - entity.field_6014;
      double dz = entity.method_23321() - entity.field_5969;
      return dx * dx + dz * dz > 1.0 ? class_243.field_1353 : new class_243(dx, 0.0, dz);
   }

   private static class_243 hits(class_746 player, class_243 eye, class_238 box, double reach, Rotation look) {
      if (look == null) {
         return null;
      }

      class_243 direction = class_243.method_1030(look.pitch(), look.yaw());
      class_243 at = box.method_992(eye, eye.method_1019(direction.method_1021(reach))).orElse(box.method_1006(eye) ? eye : null);
      return at != null && eye.method_1022(at) <= reach && MeleeAttacks.lineIsClear(player, eye, at) ? at : null;
   }

   private static boolean hitsOthers(class_746 player, class_1297 target, class_243 eye, Rotation look) {
      class_12392 range = player.method_76693();
      class_243 direction = class_243.method_1030(look.pitch(), look.yaw());
      class_243 start = eye.method_1019(direction.method_1021(range.method_76732(player)));
      double carried = Math.max(0.0, player.method_60478().method_1026(direction));
      class_243 end = eye.method_1019(direction.method_1021(range.method_76739(player) + carried));

      for (class_1297 entity : player.method_73183().method_8335(player, new class_238(start, end).method_1014(1.0))) {
         if (entity != target && entity instanceof class_1309 && class_12125.method_75244(player, entity) && !MeleeAttacks.isEnemy(player, entity)) {
            class_238 box = entity.method_5829().method_1014(range.comp_5262());
            if (box.method_1006(start) || box.method_992(start, end).isPresent()) {
               return true;
            }
         }
      }

      return false;
   }
}
