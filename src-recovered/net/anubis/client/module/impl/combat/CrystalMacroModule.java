package net.anubis.client.module.impl.combat;

import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.PlayerAttackEntityEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.CombatMacroLock;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.combat.crystal.CrystalDamage;
import net.anubis.client.module.impl.combat.crystal.Crystals;
import net.anubis.client.module.impl.combat.crystal.LootLatch;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.anubis.client.module.impl.misc.AutoEatModule;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1511;
import net.minecraft.class_1661;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_239;
import net.minecraft.class_2680;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3489;
import net.minecraft.class_3965;
import net.minecraft.class_3966;
import net.minecraft.class_638;
import net.minecraft.class_746;
import net.minecraft.class_239.class_240;

@Environment(EnvType.CLIENT)
public final class CrystalMacroModule extends ClientModule {
   private static final int COUNTER_CAP = 1000;
   private static final int WEAPON_SETTLE_TICKS = 2;
   private final NumberSetting breakDelay = this.addSetting("break-delay", new NumberSetting("Break Delay", 0.0F, 0.0F, 10.0F).step(1.0F));
   private final NumberSetting placeDelay = this.addSetting("place-delay", new NumberSetting("Place Delay", 0.0F, 0.0F, 10.0F).step(1.0F));
   private final BooleanSetting autoObsidian = this.addSetting("auto-obsidian", new BooleanSetting("Auto Obsidian", true));
   private final NumberSetting obsidianDelay = this.addSetting("obsidian-delay", new NumberSetting("Obsidian Delay", 2.0F, 0.0F, 10.0F).step(1.0F));
   private final BooleanSetting switchBack = this.addSetting("switch-back", new BooleanSetting("Switch Back", true));
   private boolean engaged;
   private int savedSlot = -1;
   private int ownSlot = -1;
   private int ticksSinceAction = 1000;
   private class_1799 lastMain;
   private int mainAge = 1000;
   private int pendingBreakId = -1;
   private int pendingBreakAge = 1000;
   private class_2338 pendingCrystalBase;
   private int pendingCrystalAge = 1000;
   private class_2338 pendingObsidian;
   private int pendingObsidianAge = 1000;

   public CrystalMacroModule() {
      super("crystal-macro", "Crystal Macro", "Breaks and places crystals while you hold right-click.", ModuleCategory.COMBAT);
      this.obsidianDelay.visibleWhen(this.autoObsidian::get);
   }

   @Override
   public int settingsVersion() {
      return 1;
   }

   @Override
   public void migrateSettings(int savedVersion) {
      if (savedVersion < 1) {
         this.breakDelay.set(this.breakDelay.get() - 1.0F);
         this.placeDelay.set(this.placeDelay.get() - 1.0F);
         this.obsidianDelay.set(this.obsidianDelay.get() - 1.0F);
      }
   }

   @Override
   protected void onEnable() {
      this.resetState();
   }

   @Override
   protected void onDisable() {
      class_310 mc = class_310.method_1551();
      this.endHold(mc);
      class_746 player = mc.field_1724;
      if (player != null && !hotbarTakenByOther()) {
         this.restoreSlot(player);
      }

      this.resetState();
   }

   @EventHandler
   public void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      this.ageCounters();
      class_746 player = mc.field_1724;
      if (player != null && mc.field_1687 != null && mc.field_1761 != null && !player.method_29504()) {
         class_1799 main = player.method_6047();
         if (main != this.lastMain) {
            this.lastMain = main;
            this.mainAge = 0;
         }

         LootLatch.update(mc, player);
         if (LootLatch.latched()) {
            this.endHold(mc);
            if (!player.method_6115() && !AutoEatModule.isEating() && (this.ownsSelection(player) || activationAllows(player))) {
               suppressVanillaUse(mc);
            }
         } else if (!this.holdActive(mc, player)) {
            this.endHold(mc);
            if (mc.field_1755 == null && !hotbarTakenByOther() && !MeleeAttacks.actedThisTick(player)) {
               this.restoreSlot(player);
            }
         } else if (!AutoInvTotemModule.wantsHotbar() && !CombatMacroLock.heldByOther(this)) {
            if (this.engaged) {
               if (!this.keepEngaged(player)) {
                  this.endHold(mc);
                  return;
               }
            } else if (player.method_6115() || !this.ownsSelection(player) && !activationAllows(player)) {
               return;
            }

            CrystalMacroModule.Plan plan = this.plan(mc, player);
            if (this.engaged || plan != null || this.ownsSelection(player)) {
               this.engaged = true;
               CombatMacroLock.tryAcquire(this);
               suppressVanillaUse(mc);
               if (plan != null && MeleeAttacks.handsFree(mc, player) && !MeleeAttacks.actedThisTick(player) && !ServerRotation.pending()) {
                  switch (plan.step()) {
                     case BREAK:
                        this.breakCrystal(mc, player, plan);
                        break;
                     case CRYSTAL:
                        if (this.useOnCrosshair(mc, player, this.placeDelay, plan)) {
                           this.pendingCrystalBase = plan.target();
                           this.pendingCrystalAge = 0;
                        }
                        break;
                     case OBSIDIAN:
                        if (this.useOnCrosshair(mc, player, this.obsidianDelay, plan)) {
                           this.pendingObsidian = plan.target();
                           this.pendingObsidianAge = 0;
                        }
                  }
               }
            }
         } else {
            CombatMacroLock.release(this);
            if (this.engaged) {
               suppressVanillaUse(mc);
            }
         }
      } else {
         this.endHold(mc);
         this.forgetSlots();
      }
   }

   @EventHandler
   public void onAttack(PlayerAttackEntityEvent event) {
      if (event.target instanceof class_1511 crystal && crystal.method_73183().method_8608()) {
         this.markBreak(crystal);
      }
   }

   private boolean holdActive(class_310 mc, class_746 player) {
      return MeleeAttacks.canAct(mc) && mc.method_1569() && !player.method_7325() && Crystals.isPhysicallyDown(mc, mc.field_1690.field_1904);
   }

   private static boolean hotbarTakenByOther() {
      return AutoEatModule.isEating() || AutoInvTotemModule.wantsHotbar();
   }

   private boolean keepEngaged(class_746 player) {
      if (this.ownsSelection(player)) {
         return true;
      }

      this.forgetSlots();
      return activationAllows(player);
   }

   private boolean ownsSelection(class_746 player) {
      return this.ownSlot != -1 && player.method_31548().method_67532() == this.ownSlot;
   }

   private static boolean activationAllows(class_746 player) {
      class_1799 main = player.method_6047();
      class_1799 offhand = player.method_6079();
      return !main.method_31574(class_1802.field_8301) && (!offhand.method_31574(class_1802.field_8301) || !Crystals.isCombatItem(main))
         ? (main.method_31573(class_3489.field_42611) || main.method_31573(class_3489.field_42612) || main.method_31574(class_1802.field_49814))
            && (offhand.method_7960() || offhand.method_31574(class_1802.field_8288))
            && Crystals.hotbarSlotOf(player.method_31548(), class_1802.field_8301) != -1
         : true;
   }

   private void endHold(class_310 mc) {
      CombatMacroLock.release(this);
      if (this.engaged) {
         this.engaged = false;
         mc.field_1690.field_1904.method_23481(mc.field_1755 == null && Crystals.isPhysicallyDown(mc, mc.field_1690.field_1904));
      }
   }

   private void restoreSlot(class_746 player) {
      if (this.ownSlot != -1) {
         class_1661 inventory = player.method_31548();
         if (this.switchBack.get() && class_1661.method_7380(this.savedSlot) && this.savedSlot != this.ownSlot && inventory.method_67532() == this.ownSlot) {
            inventory.method_61496(this.savedSlot);
         }

         this.forgetSlots();
      }
   }

   private void forgetSlots() {
      this.savedSlot = -1;
      this.ownSlot = -1;
   }

   private void resetState() {
      this.engaged = false;
      this.forgetSlots();
      this.ticksSinceAction = 1000;
      this.lastMain = null;
      this.mainAge = 1000;
      this.pendingBreakId = -1;
      this.pendingBreakAge = 1000;
      this.pendingCrystalBase = null;
      this.pendingCrystalAge = 1000;
      this.pendingObsidian = null;
      this.pendingObsidianAge = 1000;
      CombatMacroLock.release(this);
   }

   private static void suppressVanillaUse(class_310 mc) {
      class_304 use = mc.field_1690.field_1904;
      use.method_23481(false);

      while (use.method_1436()) {
      }
   }

   private CrystalMacroModule.Plan plan(class_310 mc, class_746 player) {
      class_239 hit = player.method_76762(1.0F, player);
      int window = Crystals.confirmWindow(mc, player);
      if (this.pendingCrystalBase != null && Crystals.hasCrystal(mc.field_1687, this.pendingCrystalBase, null)) {
         this.pendingCrystalBase = null;
      }

      if (hit instanceof class_3966 entityHit) {
         if (!(entityHit.method_17782() instanceof class_1511 crystal && crystal.method_5805())) {
            return null;
         } else {
            if (crystal.method_5628() == this.pendingBreakId && this.pendingBreakAge < window) {
               return null;
            }

            if (CrystalDamage.endangers(player, CrystalDamage.estimate(crystal.method_73189(), player))) {
               return null;
            }

            int weapon = Crystals.weaponRoute(player);
            return weapon == -1 ? null : CrystalMacroModule.Plan.forBreak(crystal, weapon);
         }
      } else if (hit instanceof class_3965 blockHit && blockHit.method_17783() == class_240.field_1332) {
         class_638 level = mc.field_1687;
         class_2338 pos = blockHit.method_17777();
         if (!level.method_8621().method_11952(pos)) {
            return null;
         }

         class_2680 state = level.method_8320(pos);
         if (Crystals.isBase(state)) {
            if (pos.equals(this.pendingCrystalBase) && this.pendingCrystalAge < window) {
               return null;
            }

            if (!Crystals.canHost(level, pos, null)) {
               return null;
            }

            int route = Crystals.route(player, class_1802.field_8301, state, this.savedSlot);
            return route == -1 ? null : CrystalMacroModule.Plan.forPlace(CrystalMacroModule.Step.CRYSTAL, blockHit, pos, route);
         } else {
            if (!this.autoObsidian.get()) {
               return null;
            }

            if (Crystals.route(player, class_1802.field_8301, class_2246.field_10540.method_9564(), this.savedSlot) == -1) {
               return null;
            }

            int route = Crystals.route(player, class_1802.field_8281, state, this.savedSlot);
            if (route == -1) {
               return null;
            }

            class_2338 target = Crystals.obsidianTarget(level, player, blockHit, state);
            return target != null && (!target.equals(this.pendingObsidian) || this.pendingObsidianAge >= window)
               ? CrystalMacroModule.Plan.forPlace(CrystalMacroModule.Step.OBSIDIAN, blockHit, target, route)
               : null;
         }
      } else {
         return null;
      }
   }

   private void breakCrystal(class_310 mc, class_746 player, CrystalMacroModule.Plan plan) {
      if (this.ticksSinceAction > this.breakDelay.getInt()) {
         if (plan.route() != -2) {
            this.select(player, plan.route());
         }

         if (this.mainAge >= 2 || Crystals.enchantsAddDamage(player.method_6047()) || Crystals.dealsDamage(player, class_1799.field_8037)) {
            if (Crystals.strike(mc, player, plan.crystal())) {
               MeleeAttacks.claimTick(mc);
               this.markBreak(plan.crystal());
               this.ticksSinceAction = 0;
            }
         }
      }
   }

   private boolean useOnCrosshair(class_310 mc, class_746 player, NumberSetting delay, CrystalMacroModule.Plan plan) {
      if (this.ticksSinceAction <= delay.getInt()) {
         return false;
      }

      if (plan.route() != -2) {
         this.select(player, plan.route());
      }

      if (!Crystals.rightClick(mc, player, plan.blockHit())) {
         return false;
      }

      MeleeAttacks.claimTick(mc);
      this.ticksSinceAction = 0;
      return true;
   }

   private void select(class_746 player, int slot) {
      class_1661 inventory = player.method_31548();
      if (this.savedSlot == -1) {
         this.savedSlot = inventory.method_67532();
      }

      inventory.method_61496(slot);
      this.ownSlot = slot;
      this.lastMain = player.method_6047();
      this.mainAge = 0;
   }

   private void markBreak(class_1511 crystal) {
      this.pendingBreakId = crystal.method_5628();
      this.pendingBreakAge = 0;
   }

   private void ageCounters() {
      this.ticksSinceAction = Math.min(this.ticksSinceAction + 1, 1000);
      this.mainAge = Math.min(this.mainAge + 1, 1000);
      this.pendingBreakAge = Math.min(this.pendingBreakAge + 1, 1000);
      this.pendingCrystalAge = Math.min(this.pendingCrystalAge + 1, 1000);
      this.pendingObsidianAge = Math.min(this.pendingObsidianAge + 1, 1000);
   }

   @Environment(EnvType.CLIENT)
   private record Plan(CrystalMacroModule.Step step, class_1511 crystal, class_3965 blockHit, class_2338 target, int route) {
      static CrystalMacroModule.Plan forBreak(class_1511 crystal, int weapon) {
         return new CrystalMacroModule.Plan(CrystalMacroModule.Step.BREAK, crystal, null, null, weapon);
      }

      static CrystalMacroModule.Plan forPlace(CrystalMacroModule.Step step, class_3965 blockHit, class_2338 target, int route) {
         return new CrystalMacroModule.Plan(step, null, blockHit, target, route);
      }
   }

   @Environment(EnvType.CLIENT)
   private enum Step {
      BREAK,
      CRYSTAL,
      OBSIDIAN;
   }
}
