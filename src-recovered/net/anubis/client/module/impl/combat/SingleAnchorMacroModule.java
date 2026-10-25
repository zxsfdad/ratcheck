package net.anubis.client.module.impl.combat;

import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.PacketAppliedEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.CombatMacroLock;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.combat.anchor.AnchorGuard;
import net.anubis.client.module.impl.combat.crystal.CrystalDamage;
import net.anubis.client.module.impl.combat.crystal.Crystals;
import net.anubis.client.module.impl.combat.crystal.LootLatch;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.anubis.client.module.impl.misc.AutoEatModule;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.pvp.Rotation;
import net.anubis.client.util.pvp.RotationUtils;
import net.anubis.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1661;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2626;
import net.minecraft.class_2637;
import net.minecraft.class_2680;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3965;
import net.minecraft.class_465;
import net.minecraft.class_4969;
import net.minecraft.class_638;
import net.minecraft.class_746;
import net.minecraft.class_239.class_240;

@Environment(EnvType.CLIENT)
public final class SingleAnchorMacroModule extends ClientModule {
   private static final String SINGLE = "Single";
   private static final String DOUBLE = "Double";
   private static final String DETONATE = "Detonate";
   private static final String CANCEL = "Cancel";
   private static final int MAX_STEPS_PER_TICK = 4;
   private static final int GUARD_TRIES = 2;
   private static final int SETTLE_TICKS = 5;
   private static final float BLAST_PROOF = 600.0F;
   private final ModeSetting mode = this.addSetting("mode", new ModeSetting("Mode", "Single", "Single", "Double"));
   private final NumberSetting delay = this.addSetting("delay", new NumberSetting("Delay", 0.0F, 0.0F, 5.0F).step(1.0F));
   private final BooleanSetting blastGuard = this.addSetting("safe-anchor", new BooleanSetting("Safe Anchor", true));
   private final ModeSetting withoutGuard = this.addSetting("without-guard", new ModeSetting("If Unguarded", "Detonate", "Detonate", "Cancel"));
   private final BooleanSetting switchBack = this.addSetting("switch-back", new BooleanSetting("Switch Back", false));
   private SingleAnchorMacroModule.Step step = SingleAnchorMacroModule.Step.IDLE;
   private int tick;
   private int readyAt;
   private int deadline;
   private int previousSlot = -1;
   private int ownSlot = -1;
   private boolean refilled;
   private class_2338 anchorPos;
   private class_243 sightline;
   private class_638 activeLevel;
   private AnchorGuard.Spot guard;
   private class_2248 guardBlock;
   private class_2680 guardReported;
   private int guardTries;

   public SingleAnchorMacroModule() {
      super("anchor-macro", "Anchor Macro", "Places, charges and blows up respawn anchors on right-click.", ModuleCategory.COMBAT);
      this.withoutGuard.visibleWhen(this.blastGuard::get);
   }

   @Override
   protected void onEnable() {
      this.stop();
   }

   @Override
   protected void onDisable() {
      class_310 mc = class_310.method_1551();
      if (this.step != SingleAnchorMacroModule.Step.IDLE && mc.field_1724 != null && !(mc.field_1755 instanceof class_465)) {
         class_1661 inventory = mc.field_1724.method_31548();
         if (this.switchBackDue(inventory)) {
            inventory.method_61496(this.previousSlot);
         }
      }

      this.stop();
   }

   @EventHandler
   public void onTick(TickEvent event) {
      this.tick++;
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (player != null && mc.field_1687 != null && mc.field_1761 != null && !player.method_29504()) {
         LootLatch.update(mc, player);
         class_1661 inventory = player.method_31548();
         if (mc.field_1755 != null) {
            if (this.step != SingleAnchorMacroModule.Step.IDLE && this.step != SingleAnchorMacroModule.Step.SWITCH_BACK) {
               this.wrapUp(inventory);
            }
         } else if (!AutoEatModule.isEating() && !AutoInvTotemModule.wantsHotbar()) {
            if (this.step != SingleAnchorMacroModule.Step.IDLE && (this.hotbarMovedByPlayer(inventory) || mc.field_1687 != this.activeLevel)) {
               this.stop();
            }

            if ((LootLatch.latched() || LootLatch.killedNow())
               && this.step != SingleAnchorMacroModule.Step.IDLE
               && this.step != SingleAnchorMacroModule.Step.SWITCH_BACK) {
               this.wrapUp(inventory);
            }

            if (LootLatch.latched() && this.step == SingleAnchorMacroModule.Step.IDLE) {
               if (!player.method_6115() && anchorInPlay(mc, player)) {
                  suppressUse(mc);
               }
            } else if (this.step != SingleAnchorMacroModule.Step.IDLE || this.tryStart(mc, player)) {
               swallowClicks(mc);
               if (this.step == SingleAnchorMacroModule.Step.SWITCH_BACK) {
                  if (!MeleeAttacks.actedThisTick(player)) {
                     if (this.switchBackDue(inventory)) {
                        inventory.method_61496(this.previousSlot);
                     }

                     this.stop();
                  }
               } else {
                  for (int i = 0;
                     i < 4
                        && this.tick >= this.readyAt
                        && this.step != SingleAnchorMacroModule.Step.IDLE
                        && this.step != SingleAnchorMacroModule.Step.SWITCH_BACK;
                     i++
                  ) {
                     if (this.tick >= this.deadline) {
                        this.expire(inventory);
                     } else {
                        if (MeleeAttacks.actedThisTick(player) || !MeleeAttacks.handsFree(mc, player)) {
                           break;
                        }

                        this.runStep(mc, player);
                     }
                  }
               }
            }
         } else {
            this.stop();
         }
      } else {
         this.stop();
      }
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent event) {
      if (this.step == SingleAnchorMacroModule.Step.AWAIT_GUARD && this.guard != null) {
         class_2338 spot = this.guard.pos();
         if (event.packet instanceof class_2626 update) {
            if (update.method_11309().equals(spot)) {
               this.guardReported = update.method_11308();
            }
         } else if (event.packet instanceof class_2637 section) {
            section.method_30621((pos, state) -> {
               if (pos.equals(spot)) {
                  this.guardReported = state;
               }
            });
         }
      }
   }

   private boolean tryStart(class_310 mc, class_746 player) {
      if (MeleeAttacks.rightClickComing(mc) && !CombatMacroLock.heldByOther(this)) {
         if (!player.method_7325()
            && canClick(player)
            && !MeleeAttacks.actedThisTick(player)
            && MeleeAttacks.handsFree(mc, player)
            && !player.method_6079().method_31574(class_1802.field_8801)) {
            if (player.method_5745(player.method_55754(), 1.0F, false) instanceof class_3965 hit && hit.method_17783() == class_240.field_1332) {
               class_638 level = mc.field_1687;
               class_2680 clicked = level.method_8320(hit.method_17777());
               boolean onAnchor = clicked.method_27852(class_2246.field_23152);
               class_2338 target;
               if (onAnchor) {
                  target = hit.method_17777().method_10062();
               } else {
                  if (!player.method_6047().method_31574(class_1802.field_23141) || ServerRotation.pending()) {
                     return false;
                  }

                  target = Crystals.placedObsidian(level, player, hit, clicked);
               }

               if (target != null && level.method_27983() != class_1937.field_25180) {
                  class_1661 inventory = player.method_31548();
                  boolean charged = chargeOf(clicked) > 0;
                  if (!charged && findHotbar(inventory, class_1802.field_8801) == -1) {
                     return false;
                  }

                  if (AnchorGuard.detonatorSlot(inventory) == -1) {
                     return false;
                  }

                  if (this.blastGuard.get() && this.withoutGuard.is("Cancel") && guardSlot(inventory, this.glowstoneNeeded(charged)) == -1) {
                     return false;
                  }

                  if (!CombatMacroLock.tryAcquire(this)) {
                     return false;
                  }

                  this.previousSlot = inventory.method_67532();
                  this.activeLevel = level;
                  this.anchorPos = target;
                  this.sightline = hit.method_17784();
                  SingleAnchorMacroModule.Step first = this.blastGuard.get() ? SingleAnchorMacroModule.Step.GUARD : SingleAnchorMacroModule.Step.CHARGE;
                  if (onAnchor) {
                     this.scheduleNow(first);
                  } else {
                     AnchorGuard.click(mc, player, hit);
                     this.schedule(first);
                  }

                  return true;
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void swallowClicks(class_310 mc) {
      MeleeAttacks.takeAttackClicks(mc);
      suppressUse(mc);
   }

   private static boolean anchorInPlay(class_310 mc, class_746 player) {
      class_1799 main = player.method_6047();
      return !main.method_31574(class_1802.field_23141) && !main.method_31574(class_1802.field_8801)
         ? mc.field_1765 instanceof class_3965 hit
            && hit.method_17783() == class_240.field_1332
            && mc.field_1687.method_8320(hit.method_17777()).method_27852(class_2246.field_23152)
         : true;
   }

   private static void suppressUse(class_310 mc) {
      class_304 use = mc.field_1690.field_1904;
      use.method_23481(false);

      while (use.method_1436()) {
      }
   }

   private void runStep(class_310 mc, class_746 player) {
      switch (this.step) {
         case GUARD:
            this.placeGuard(mc, player);
            break;
         case AWAIT_GUARD:
            this.awaitGuard(mc, player);
            break;
         case CHARGE:
            this.charge(mc, player);
            break;
         case DETONATE:
            this.detonate(mc, player);
      }
   }

   private void expire(class_1661 inventory) {
      if (this.step != SingleAnchorMacroModule.Step.GUARD && this.step != SingleAnchorMacroModule.Step.AWAIT_GUARD) {
         this.wrapUp(inventory);
      } else {
         this.skipGuard(inventory);
      }
   }

   private void placeGuard(class_310 mc, class_746 player) {
      class_1661 inventory = player.method_31548();
      class_2680 anchor = mc.field_1687.method_8320(this.anchorPos);
      if (!anchor.method_27852(class_2246.field_23152)) {
         this.wrapUp(inventory);
      } else {
         AnchorGuard.Spot spot = AnchorGuard.find(mc.field_1687, player, this.anchorPos, this.sightline);
         if (spot != null && spot.standing()) {
            this.guarded();
         } else {
            int slot = guardSlot(inventory, this.glowstoneNeeded(chargeOf(anchor) > 0));
            Rotation sent = ServerRotation.sent();
            class_3965 hit = spot != null && sent != null ? AnchorGuard.hitAlong(mc.field_1687, player, sent, spot) : null;
            if (spot != null && slot != -1 && (hit != null || ServerRotation.canRotate(player))) {
               if (hit == null) {
                  ServerRotation.request(RotationUtils.fromTo(player.method_33571(), spot.aim()));
                  MeleeAttacks.claimTick(mc);
                  this.readyAt = this.tick + 1;
               } else {
                  this.guard = spot;
                  this.guardBlock = class_2248.method_9503(inventory.method_5438(slot).method_7909());
                  this.guardReported = null;
                  this.select(inventory, slot);
                  AnchorGuard.click(mc, player, hit);
                  ServerRotation.request(RotationUtils.fromTo(player.method_33571(), class_243.method_24953(this.anchorPos)));
                  this.guardTries++;
                  if (this.withoutGuard.is("Cancel")) {
                     this.schedule(SingleAnchorMacroModule.Step.AWAIT_GUARD);
                     this.deadline = this.readyAt + Crystals.confirmWindow(mc, player);
                  } else {
                     this.schedule(SingleAnchorMacroModule.Step.CHARGE);
                  }
               }
            } else {
               this.skipGuard(inventory);
            }
         }
      }
   }

   private void awaitGuard(class_310 mc, class_746 player) {
      boolean shown = mc.field_1687.method_8320(this.guard.pos()).method_27852(this.guardBlock);
      if (shown && this.guardReported != null && this.guardReported.method_27852(this.guardBlock)) {
         this.guarded();
      } else if (shown) {
         this.readyAt = this.tick + 1;
      } else if (this.guardTries < 2) {
         this.scheduleNow(SingleAnchorMacroModule.Step.GUARD);
      } else {
         this.skipGuard(player.method_31548());
      }
   }

   private void guarded() {
      this.guard = null;
      this.scheduleNow(SingleAnchorMacroModule.Step.CHARGE);
   }

   private void skipGuard(class_1661 inventory) {
      this.guard = null;
      if (this.withoutGuard.is("Cancel")) {
         this.wrapUp(inventory);
      } else {
         this.scheduleNow(SingleAnchorMacroModule.Step.CHARGE);
      }
   }

   private void charge(class_310 mc, class_746 player) {
      class_1661 inventory = player.method_31548();
      class_2680 state = mc.field_1687.method_8320(this.anchorPos);
      if (!state.method_27852(class_2246.field_23152)) {
         this.wrapUp(inventory);
      } else if (chargeOf(state) > 0 && !this.refilled) {
         this.scheduleNow(SingleAnchorMacroModule.Step.DETONATE);
      } else {
         int glowstone = findHotbar(inventory, class_1802.field_8801);
         if (glowstone != -1 && canClick(player)) {
            class_3965 hit = AnchorGuard.anchorHit(mc.field_1687, player, this.anchorPos);
            if (hit == null) {
               this.readyAt = this.tick + 1;
            } else {
               this.select(inventory, glowstone);
               AnchorGuard.click(mc, player, hit);
               this.schedule(SingleAnchorMacroModule.Step.DETONATE);
            }
         } else {
            this.wrapUp(inventory);
         }
      }
   }

   private void detonate(class_310 mc, class_746 player) {
      class_1661 inventory = player.method_31548();
      if (chargeOf(mc.field_1687.method_8320(this.anchorPos)) != 0 && canClick(player) && !player.method_6079().method_31574(class_1802.field_8801)) {
         class_3965 hit = AnchorGuard.anchorHit(mc.field_1687, player, this.anchorPos);
         if (hit == null) {
            this.readyAt = this.tick + 1;
         } else {
            int refillSlot = this.mode.is("Double")
                  && !this.refilled
                  && hit.method_17780() == class_2350.field_11036
                  && this.anchorPos.method_10264() >= player.method_31478()
                  && findHotbar(inventory, class_1802.field_8801) != -1
               ? findHotbar(inventory, class_1802.field_23141)
               : -1;
            int slot = refillSlot != -1 ? refillSlot : AnchorGuard.detonatorSlot(inventory);
            if (slot == -1) {
               this.wrapUp(inventory);
            } else if (this.lethal(player, inventory.method_5438(slot))) {
               this.wrapUp(inventory);
            } else {
               this.select(inventory, slot);
               AnchorGuard.click(mc, player, hit);
               if (slot == refillSlot) {
                  AnchorGuard.click(mc, player, hit);
                  this.refilled = true;
                  this.schedule(SingleAnchorMacroModule.Step.CHARGE);
               } else {
                  this.wrapUp(inventory);
               }
            }
         }
      } else {
         this.wrapUp(inventory);
      }
   }

   private boolean lethal(class_746 player, class_1799 held) {
      if (!held.method_31574(class_1802.field_8288) && !player.method_6079().method_31574(class_1802.field_8288)) {
         float damage = CrystalDamage.estimate(class_243.method_24953(this.anchorPos), player, 5.0F, this.anchorPos);
         return CrystalDamage.endangers(player, damage);
      } else {
         return false;
      }
   }

   private void wrapUp(class_1661 inventory) {
      if (this.switchBackDue(inventory)) {
         this.step = SingleAnchorMacroModule.Step.SWITCH_BACK;
      } else {
         this.stop();
      }
   }

   private boolean switchBackDue(class_1661 inventory) {
      return this.switchBack.get()
         && class_1661.method_7380(this.previousSlot)
         && this.ownSlot != -1
         && this.ownSlot != this.previousSlot
         && inventory.method_67532() == this.ownSlot;
   }

   private boolean hotbarMovedByPlayer(class_1661 inventory) {
      return inventory.method_67532() != (this.ownSlot != -1 ? this.ownSlot : this.previousSlot);
   }

   private void stop() {
      this.step = SingleAnchorMacroModule.Step.IDLE;
      this.anchorPos = null;
      this.sightline = null;
      this.activeLevel = null;
      this.previousSlot = -1;
      this.ownSlot = -1;
      this.refilled = false;
      this.guard = null;
      this.guardBlock = null;
      this.guardReported = null;
      this.guardTries = 0;
      CombatMacroLock.release(this);
   }

   private void schedule(SingleAnchorMacroModule.Step next) {
      this.step = next;
      this.readyAt = this.tick + 1 + this.delay.getInt();
      this.deadline = this.readyAt + 5;
   }

   private void scheduleNow(SingleAnchorMacroModule.Step next) {
      this.step = next;
      this.readyAt = this.tick;
      this.deadline = this.tick + 5;
   }

   private void select(class_1661 inventory, int slot) {
      if (inventory.method_67532() != slot) {
         inventory.method_61496(slot);
         this.ownSlot = slot;
      }
   }

   private static boolean canClick(class_746 player) {
      return !player.method_21823();
   }

   private static int chargeOf(class_2680 state) {
      return state.method_27852(class_2246.field_23152) ? (Integer)state.method_11654(class_4969.field_23153) : 0;
   }

   private int glowstoneNeeded(boolean charged) {
      int needed = charged ? 0 : 1;
      return this.mode.is("Double") ? needed + 1 : needed;
   }

   private static int guardSlot(class_1661 inventory, int glowstoneNeeded) {
      int weaker = -1;

      for (int i = 0; i < 9; i++) {
         class_1799 stack = inventory.method_5438(i);
         if (!stack.method_31574(class_1802.field_8801) && AnchorGuard.isGuardBlock(stack)) {
            if (class_2248.method_9503(stack.method_7909()).method_9520() >= 600.0F) {
               return i;
            }

            if (weaker == -1) {
               weaker = i;
            }
         }
      }

      if (weaker != -1) {
         return weaker;
      }

      int glowstone = findHotbar(inventory, class_1802.field_8801);
      return glowstone != -1 && countHotbar(inventory, class_1802.field_8801) > glowstoneNeeded ? glowstone : -1;
   }

   private static int findHotbar(class_1661 inventory, class_1792 item) {
      for (int i = 0; i < 9; i++) {
         if (inventory.method_5438(i).method_31574(item)) {
            return i;
         }
      }

      return -1;
   }

   private static int countHotbar(class_1661 inventory, class_1792 item) {
      int count = 0;

      for (int i = 0; i < 9; i++) {
         class_1799 stack = inventory.method_5438(i);
         if (stack.method_31574(item)) {
            count += stack.method_7947();
         }
      }

      return count;
   }

   @Environment(EnvType.CLIENT)
   private enum Step {
      IDLE,
      GUARD,
      AWAIT_GUARD,
      CHARGE,
      DETONATE,
      SWITCH_BACK;
   }
}
