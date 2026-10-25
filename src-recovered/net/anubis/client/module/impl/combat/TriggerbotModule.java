package net.anubis.client.module.impl.combat;

import net.anubis.client.event.EventHandler;
import net.anubis.client.event.EventPriority;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.CombatMacroLock;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.pvp.InputControl;
import net.anubis.client.util.pvp.Rotation;
import net.anubis.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12125;
import net.minecraft.class_12392;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3966;
import net.minecraft.class_746;
import net.minecraft.class_9334;

@Environment(EnvType.CLIENT)
public final class TriggerbotModule extends ClientModule {
   private static final String CRITS_OFF = "Off";
   private static final String CRITS_SMART = "Smart";
   private static final String CRITS_ONLY = "Only";
   private static final int TRACK_GRACE_TICKS = 4;
   private static final int SMART_MAX_WAIT_TICKS = 12;
   private final NumberSetting reaction = this.addSetting("reaction", new NumberSetting("Reaction", 3.0F, 0.0F, 10.0F).step(1.0F));
   private final ModeSetting crits = this.addSetting("crits", new ModeSetting("Crits", "Smart", "Off", "Smart", "Only"));
   private final BooleanSetting holdAttack = this.addSetting("hold-attack", new BooleanSetting("Hold Attack", false));
   private final BooleanSetting players = this.addSetting("players", new BooleanSetting("Players", true));
   private final BooleanSetting hostiles = this.addSetting("hostiles", new BooleanSetting("Hostiles", false));
   private final BooleanSetting invisible = this.addSetting("invisible", new BooleanSetting("Invisible", false));
   private final BooleanSetting weaponsOnly = this.addSetting("weapons-only", new BooleanSetting("Weapons Only", true));
   private int ticks;
   private int trackedId = -1;
   private int seenTick;
   private int reactionDoneTick;
   private int chargedTick = -1;
   private int chargePause;
   private int critWait;
   private int critWaitTick = -1;

   public TriggerbotModule() {
      super("triggerbot", "Triggerbot", "Hits the entity under the crosshair once the swing is charged, with a reaction delay.", ModuleCategory.COMBAT);
   }

   @Override
   protected void onEnable() {
      this.reset();
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   private void reset() {
      this.ticks = 0;
      this.trackedId = -1;
      this.seenTick = 0;
      this.reactionDoneTick = 0;
      this.chargedTick = -1;
      this.chargePause = 0;
      this.critWait = 0;
      this.critWaitTick = -1;
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onTick(TickEvent event) {
      this.ticks++;
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (!MeleeAttacks.canAct(mc)) {
         this.trackedId = -1;
      } else {
         this.trackCharge(player);
         class_3966 hit = MeleeAttacks.crosshairHit(mc);
         if (hit != null && hit.method_17782() instanceof class_1309 target && this.isTarget(player, target)) {
            if (target.method_5628() != this.trackedId) {
               this.trackedId = target.method_5628();
               this.reactionDoneTick = this.ticks + this.reaction.getInt() + MeleeAttacks.spread(2);
            }

            this.seenTick = this.ticks;
            if (this.ticks >= this.reactionDoneTick && this.chargedTick >= 0 && this.ticks - this.chargedTick >= this.chargePause) {
               if (!this.holdAttack.get() || mc.field_1690.field_1886.method_1434()) {
                  if (!this.weaponsOnly.get() || MeleeAttacks.isMeleeWeapon(player.method_6047())) {
                     if (!CombatMacroLock.heldByOther(this) && !MeleeAttacks.wastedOnShield(player, target)) {
                        if (mc.field_1771 <= 0 && !MeleeAttacks.clickPending(mc) && !MeleeAttacks.rightClickComing(mc) && !ServerRotation.pending()) {
                           boolean stabbing = player.method_6047().method_57826(class_9334.field_63631);
                           if (stabbing || this.critsAllow(mc, player, target)) {
                              if (stabbing) {
                                 hit = this.stabHit(player, target);
                                 if (hit == null) {
                                    return;
                                 }
                              }

                              if (stabbing ? MeleeAttacks.stab(mc, hit) : MeleeAttacks.attack(mc, hit)) {
                                 this.chargedTick = -1;
                                 MeleeAttacks.claimTick(mc);
                              }
                           }
                        }
                     }
                  }
               }
            }
         } else {
            if (this.ticks - this.seenTick > 4) {
               this.trackedId = -1;
            }
         }
      }
   }

   private void trackCharge(class_746 player) {
      if (MeleeAttacks.charge(player) < 1.0F) {
         this.chargedTick = -1;
      } else if (this.chargedTick < 0) {
         this.chargedTick = this.ticks;
         this.chargePause = MeleeAttacks.spread(1);
      }
   }

   private class_3966 stabHit(class_746 player, class_1309 target) {
      Rotation sent = ServerRotation.sent();
      if (sent == null) {
         return null;
      }

      class_12392 range = player.method_76693();
      class_243 look = class_243.method_1030(sent.pitch(), sent.yaw());
      class_243 eye = player.method_33571();
      class_243 start = eye.method_1019(look.method_1021(range.method_76732(player)));
      class_243 end = eye.method_1019(look.method_1021(range.method_76739(player) + Math.max(0.0, player.method_60478().method_1026(look))));
      float margin = range.comp_5262();
      class_243 at = (class_243)target.method_5829().method_1014(margin).method_992(eye, end).orElse(null);
      if (at != null && MeleeAttacks.lineIsClear(player, eye, at)) {
         for (class_1297 entity : player.method_73183().method_8335(player, new class_238(start, end).method_1014(1.0))) {
            if (entity != target && entity instanceof class_1309 other && class_12125.method_75244(player, entity) && !this.isTarget(player, other)) {
               class_238 box = entity.method_5829().method_1014(margin);
               if (box.method_1006(start) || box.method_992(start, end).isPresent()) {
                  return null;
               }
            }
         }

         return new class_3966(target, at);
      } else {
         return null;
      }
   }

   private boolean isTarget(class_746 self, class_1309 entity) {
      if (!MeleeAttacks.isEnemy(self, entity)) {
         return false;
      } else if (entity.method_5767() && !this.invisible.get()) {
         return false;
      } else {
         return entity instanceof class_1657 ? this.players.get() : this.hostiles.get() && MeleeAttacks.isHostile(entity);
      }
   }

   private boolean critsAllow(class_310 mc, class_746 player, class_1309 target) {
      return switch (this.crits.get()) {
         case "Only" -> {
            if (MeleeAttacks.airborne(player) && player.method_5624() && player.field_6017 > 0.0) {
               InputControl.dropSprint();
            }

            yield MeleeAttacks.wouldCrit(player, target);
         }
         case "Smart" -> {
            if (MeleeAttacks.wouldCrit(player, target)) {
               yield true;
            } else {
               boolean jumping = player.method_24828() && mc.field_1690.field_1903.method_1434() && !player.method_5624();
               if (!MeleeAttacks.critStance(player, target) && !jumping) {
                  yield true;
               } else {
                  if (this.critWaitTick != this.ticks - 1) {
                     this.critWait = 0;
                  }

                  this.critWaitTick = this.ticks;
                  yield this.critWait++ >= 12;
               }
            }
         }
         default -> true;
      };
   }
}
