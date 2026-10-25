package net.anubis.client.module.impl.combat;

import imgui.ImDrawList;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.PacketReceiveEvent;
import net.anubis.client.event.impl.PlayerAttackEntityEvent;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.CombatMacroLock;
import net.anubis.client.module.HudOverlayProvider;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.combat.crystal.Combo;
import net.anubis.client.module.impl.combat.crystal.CrystalDamage;
import net.anubis.client.module.impl.combat.crystal.Crystals;
import net.anubis.client.module.impl.combat.crystal.HurtClock;
import net.anubis.client.module.impl.combat.crystal.ShotView;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.anubis.client.module.impl.misc.AutoEatModule;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.anubis.client.module.setting.KeySetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.pvp.Rotation;
import net.anubis.client.util.pvp.RotationUtils;
import net.anubis.client.util.pvp.ServerRotation;
import net.anubis.client.util.pvp.TargetingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1511;
import net.minecraft.class_1657;
import net.minecraft.class_1661;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_238;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2596;
import net.minecraft.class_2604;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_3965;
import net.minecraft.class_3966;
import net.minecraft.class_638;
import net.minecraft.class_746;
import net.minecraft.class_8042;
import net.minecraft.class_2338.class_2339;
import net.minecraft.class_2350.class_2351;
import net.minecraft.class_2350.class_2353;

@Environment(EnvType.CLIENT)
public final class CrystalAuraModule extends ClientModule implements HudOverlayProvider {
   private static final int COUNTER_CAP = 1000;
   private static final int IDLE_TICKS = 10;
   private static final float FACE_PLACE_HEALTH = 10.0F;
   private static final float FACE_PLACE_DAMAGE = 2.0F;
   private static final float ARMOR_LOW = 0.1F;
   private static final float BREAK_FACTOR = 0.6F;
   private static final double AIM_INSET = 0.1;
   private static final int MAX_BREAK_ATTEMPTS = 3;
   private static final double CHAIN_RANGE = 12.0;
   private static final int WEAPON_SETTLE_TICKS = 2;
   private static final float LOOK_BONUS = 2.0F;
   private static final int MAX_LEAD_TICKS = 5;
   private static final float LOOK_MATCH_DEGREES = 2.0F;
   private static final double PLACE_BREAK_MARGIN = 0.1;
   private static final float ONE_LOOK_BONUS = 2.0F;
   private static final float OBSIDIAN_UPGRADE_BAR = 8.0F;
   private static final float OBSIDIAN_MIN_GAIN = 5.0F;
   private static final float OBSIDIAN_MIN_RATIO = 2.0F;
   private final BooleanSetting requireHold = this.addSetting("require-hold", new BooleanSetting("Require Hold", false));
   private final KeySetting holdKey = this.addSetting("hold-key", new KeySetting("Hold Key", 1003));
   private final NumberSetting placeDelay = this.addSetting("place-delay", new NumberSetting("Place Delay", 0.0F, 0.0F, 10.0F).step(1.0F));
   private final NumberSetting breakDelay = this.addSetting("break-delay", new NumberSetting("Break Delay", 0.0F, 0.0F, 10.0F).step(1.0F));
   private final NumberSetting minDamage = this.addSetting("min-damage", new NumberSetting("Min Damage", 4.0F, 0.0F, 20.0F).step(0.5F));
   private final NumberSetting maxSelfDamage = this.addSetting("max-self-damage", new NumberSetting("Max Self Damage", 8.0F, 0.0F, 20.0F).step(0.5F));
   private final BooleanSetting facePlace = this.addSetting("face-place", new BooleanSetting("Face Place", true));
   private final KeySetting forceFacePlace = this.addSetting("force-face-place", new KeySetting("Force Face Place", -1));
   private final BooleanSetting autoObsidian = this.addSetting("auto-obsidian", new BooleanSetting("Auto Obsidian", true));
   private final BooleanSetting switchBack = this.addSetting("switch-back", new BooleanSetting("Switch Back", true));
   private final BooleanSetting render = this.addSetting("render", new BooleanSetting("Render", true));
   private final ColorSetting renderColor = this.addSetting("render-color", new ColorSetting("Render Color", -2728705));
   private final ShotView view = new ShotView();
   private boolean engaged;
   private int idleTicks;
   private int savedSlot = -1;
   private int ownSlot = -1;
   private int ticksSinceBreak = 1000;
   private int ticksSincePlace = 1000;
   private class_1799 lastMain;
   private int mainAge = 1000;
   private int pendingBreakId = -1;
   private int pendingBreakAge = 1000;
   private class_2338 pendingObsidian;
   private int pendingObsidianAge = 1000;
   private final Int2IntOpenHashMap breakAttempts = new Int2IntOpenHashMap();
   private final Int2IntOpenHashMap doomed = new Int2IntOpenHashMap();

   public CrystalAuraModule() {
      super("crystal-aura", "Crystal Aura", "Places and breaks end crystals for the most damage.", ModuleCategory.COMBAT);
      this.holdKey.visibleWhen(this.requireHold::get);
      this.renderColor.visibleWhen(this.render::get);
   }

   @Override
   protected void onEnable() {
      this.resetState();
   }

   @Override
   protected void onDisable() {
      class_746 player = class_310.method_1551().field_1724;
      if (player != null && !AutoEatModule.isEating() && !AutoInvTotemModule.wantsHotbar()) {
         this.restoreSlot(player);
      }

      this.resetState();
      this.view.clear();
   }

   @EventHandler
   public void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      this.ageCounters();
      class_746 player = mc.field_1724;
      if (player != null && mc.field_1687 != null && mc.field_1761 != null && !player.method_29504()) {
         HurtClock.tick(player);
         this.forgetGone(mc.field_1687);
         class_1799 main = player.method_6047();
         if (main != this.lastMain) {
            this.lastMain = main;
            this.mainAge = 0;
         }

         CrystalAuraModule.Look look = ServerRotation.canRotate(player) ? CrystalAuraModule.Look.SILENT : CrystalAuraModule.Look.CROSSHAIR;
         boolean crystals = !this.requireHold.get() || Crystals.isKeyDown(mc, this.holdKey.value());
         AnchorAuraModule aura = ModuleRegistry.get(AnchorAuraModule.class);
         boolean anchors = aura != null && aura.isEnabled() && look == CrystalAuraModule.Look.SILENT;
         if (MeleeAttacks.canAct(mc) && !player.method_7325() && (crystals || anchors)) {
            if (!AutoInvTotemModule.wantsHotbar() && !AutoCityModule.mining() && !CombatMacroLock.heldByOther(this)) {
               if (this.ownSlot != -1 && !this.ownsSelection(player)) {
                  this.forgetSlots();
               }

               class_1799 offhand = player.method_6079();
               boolean offhandUse = !Crystals.isCombatItem(offhand)
                  && !offhand.method_31574(class_1802.field_8301)
                  && !offhand.method_31574(class_1802.field_8281);
               if (!MeleeAttacks.rightClickComing(mc) || !offhandUse && !main.method_31574(class_1802.field_23141) && !main.method_31574(class_1802.field_8801)
                  )
                {
                  if (!MeleeAttacks.actedThisTick(player) && MeleeAttacks.handsFree(mc, player) && (this.ownsSelection(player) || mayTakeHand(player, anchors))
                     )
                   {
                     List<class_1309> enemies = Combo.enemies(mc, player);
                     if (enemies.isEmpty()) {
                        this.disengage(mc, player);
                     } else {
                        Rotation fixedLook = null;
                        if (look == CrystalAuraModule.Look.CROSSHAIR) {
                           fixedLook = RotationUtils.entityRotation(player);
                           if (ServerRotation.pending() || !sameLook(ServerRotation.sent(), fixedLook)) {
                              this.idle(mc, player);
                              return;
                           }
                        }

                        int window = Crystals.confirmWindow(mc, player);
                        class_1511 lastHit = this.lastHit(mc.field_1687, window);
                        CrystalAuraModule.Fight fight = new CrystalAuraModule.Fight(
                           player,
                           mc.field_1687,
                           enemies,
                           Combo.friends(mc, player),
                           window,
                           Crystals.rttTicks(mc, player),
                           fixedLook,
                           look == CrystalAuraModule.Look.SILENT ? 2.0F : 0.0F,
                           Crystals.isKeyDown(mc, this.forceFacePlace.value())
                        );
                        Combo.Shot shot = this.choose(mc, fight, lastHit, crystals, anchors ? aura : null);
                        if (shot == null) {
                           this.idle(mc, player);
                        } else {
                           this.engage();
                           if (shot.plan() instanceof CrystalAuraModule.Plan plan) {
                              if (this.render.get()) {
                                 this.show(fight, plan);
                              }

                              if (!this.perform(mc, player, fight, plan, look) || look != CrystalAuraModule.Look.SILENT) {
                                 return;
                              }

                              CrystalAuraModule.Plan following = this.planCrystal(fight, this.lastHit(mc.field_1687, window), 1, this.stagedBreak(fight));
                              Rotation sent = ServerRotation.sent();
                              ServerRotation.request(following != null && !fight.holds(following) ? fight.lookAt(following.aim()) : sent);
                           } else {
                              aura.show(shot);
                              if (this.fire(mc, player, fight, aura, shot)) {
                                 ServerRotation.request(ServerRotation.sent());
                              }
                           }
                        }
                     }
                  } else {
                     this.idle(mc, player);
                  }
               } else {
                  this.disengage(mc, player);
               }
            } else {
               CombatMacroLock.release(this);
            }
         } else {
            this.disengage(mc, player);
         }
      } else {
         this.disengage(mc, null);
         Crystals.forgetRtt();
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      if (this.render.get()) {
         this.view.draw();
      }
   }

   @Override
   public boolean isHudOverlayEnabled() {
      return this.isEnabled() && this.render.get() && this.view.showing();
   }

   @Override
   public void renderHudOverlay(ImDrawList dl, float screenW, float screenH) {
      this.view.label(dl, screenW, screenH);
   }

   @EventHandler
   public void onAttack(PlayerAttackEntityEvent event) {
      if (event.target instanceof class_1511 crystal && crystal.method_73183().method_8608()) {
         this.markBreak(crystal);
      }
   }

   @EventHandler
   public void onPacketReceive(PacketReceiveEvent event) {
      class_746 player = class_310.method_1551().field_1724;
      if (player != null) {
         if (event.packet instanceof class_8042 bundle) {
            for (class_2596<?> packet : bundle.method_48324()) {
               this.note(player, packet);
            }
         } else {
            this.note(player, event.packet);
         }

         HurtClock.note(player, event.packet);
      }
   }

   private void note(class_746 player, class_2596<?> packet) {
      if (packet instanceof class_2604 spawn && spawn.method_11169() == class_1299.field_6110) {
         Crystals.noteSpawn(player, class_2338.method_49637(spawn.method_11175(), spawn.method_11174() - 1.0, spawn.method_11176()));
      }
   }

   private static boolean mayTakeHand(class_746 player, boolean anchors) {
      class_1799 main = player.method_6047();
      return main.method_31574(class_1802.field_8301)
         || main.method_31574(class_1802.field_8281)
         || Crystals.isCombatItem(main)
         || anchors && (main.method_31574(class_1802.field_23141) || main.method_31574(class_1802.field_8801));
   }

   private boolean ownsSelection(class_746 player) {
      return this.ownSlot != -1 && player.method_31548().method_67532() == this.ownSlot;
   }

   private void engage() {
      this.engaged = true;
      this.idleTicks = 0;
      CombatMacroLock.tryAcquire(this);
   }

   private void idle(class_310 mc, class_746 player) {
      if (!this.engaged || ++this.idleTicks >= 10) {
         this.disengage(mc, player);
      }
   }

   private void disengage(class_310 mc, class_746 player) {
      this.engaged = false;
      this.idleTicks = 0;
      CombatMacroLock.release(this);
      if (player == null) {
         this.forgetSlots();
      } else if (mc.field_1755 == null && !AutoEatModule.isEating() && !AutoInvTotemModule.wantsHotbar() && !MeleeAttacks.actedThisTick(player)) {
         this.restoreSlot(player);
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
      this.idleTicks = 0;
      this.forgetSlots();
      this.ticksSinceBreak = 1000;
      this.ticksSincePlace = 1000;
      this.lastMain = null;
      this.mainAge = 1000;
      this.pendingBreakId = -1;
      this.pendingBreakAge = 1000;
      this.pendingObsidian = null;
      this.pendingObsidianAge = 1000;
      this.breakAttempts.clear();
      this.doomed.clear();
      Crystals.forgetRtt();
      CombatMacroLock.release(this);
   }

   private void forgetGone(class_638 level) {
      forgetGone(level, this.breakAttempts.keySet());
      forgetGone(level, this.doomed.keySet());
   }

   private static void forgetGone(class_638 level, IntSet ids) {
      IntIterator iterator = ids.iterator();

      while (iterator.hasNext()) {
         if (level.method_8469(iterator.nextInt()) == null) {
            iterator.remove();
         }
      }
   }

   private static boolean oneLook(class_243 eye, class_238 crystal, class_243 placeAim, double reach) {
      class_243 direction = placeAim.method_1020(eye).method_1029();
      return crystal.method_992(eye, eye.method_1019(direction.method_1021(reach))).isPresent();
   }

   private boolean canHostSoon(CrystalAuraModule.Fight fight, class_2338 base, class_1297 breaking) {
      class_2338 above = base.method_10084();
      if (!fight.level.method_22347(above)) {
         return false;
      }

      class_238 space = new class_238(
         above.method_10263(), above.method_10264(), above.method_10260(), above.method_10263() + 1.0, above.method_10264() + 2.0, above.method_10260() + 1.0
      );

      for (class_1297 entity : fight.level.method_8335(breaking, space)) {
         if (entity.method_5805()) {
            if (!(entity instanceof class_1309 living) || !fight.enemies.contains(living)) {
               return false;
            }

            class_238 then = living.method_5829().method_997(Combo.lead(living, fight.arriveLead));
            boolean over = then.field_1322 >= space.field_1325;
            boolean aside = then.field_1320 <= space.field_1323
               || then.field_1323 >= space.field_1320
               || then.field_1324 <= space.field_1321
               || then.field_1321 >= space.field_1324;
            if (!over && !aside) {
               return false;
            }
         }
      }

      return true;
   }

   private class_1511 lastHit(class_638 level, int window) {
      if (this.pendingBreakId >= 0 && this.pendingBreakAge < window) {
         return level.method_8469(this.pendingBreakId) instanceof class_1511 crystal && crystal.method_5805() ? crystal : null;
      } else {
         return null;
      }
   }

   private Combo.Shot choose(class_310 mc, CrystalAuraModule.Fight fight, class_1511 lastHit, boolean crystals, AnchorAuraModule aura) {
      List<Combo.Shot> shots = new ArrayList<>();
      Combo.Shot aimed = null;
      float rival = 0.0F;
      if (crystals) {
         CrystalAuraModule.Plan opening = this.stagedBreak(fight);
         CrystalAuraModule.Plan plan = this.planCrystal(fight, lastHit, 0, opening);
         if (plan != null) {
            aimed = this.shot(fight, plan, false);
            if (this.due(plan, 0)) {
               shots.add(aimed);
            }
         }

         if (aura != null && opening != null) {
            rival = this.shot(fight, opening, true).value();
         }
      }

      if (aura != null) {
         shots.addAll(aura.shots(mc, fight.player, fight.enemies, fight.friends, fight.rtt, rival));
      }

      Combo.Shot picked = Combo.pick(shots);
      return picked != null ? picked : aimed;
   }

   private void show(CrystalAuraModule.Fight fight, CrystalAuraModule.Plan plan) {
      CrystalAuraModule.Side side = fight.side(plan.step());
      boolean baseToCome = plan.step() == CrystalAuraModule.Step.OBSIDIAN;
      class_243 center = plan.step() == CrystalAuraModule.Step.BREAK ? plan.crystal().method_73189() : Crystals.explosionAt(plan.target());
      float most = 0.0F;

      for (CrystalDamage.Victim enemy : side.enemies) {
         most = Math.max(most, enemy.damage(center, baseToCome));
      }

      class_2338 base = plan.step() == CrystalAuraModule.Step.BREAK ? plan.crystal().method_24515().method_10074() : plan.target();
      this.view.show(new class_238(base), this.renderColor.argb(), most, side.self.damage(center, baseToCome));
   }

   private CrystalAuraModule.Plan planCrystal(CrystalAuraModule.Fight fight, class_1511 lastHit, int ahead, CrystalAuraModule.Plan opening) {
      CrystalAuraModule.Plan breakPlan = this.planBreak(fight, false);
      if (breakPlan != null && this.due(breakPlan, ahead)) {
         return breakPlan;
      }

      boolean staged = breakPlan != null || opening != null || Crystals.anyPlacing(fight.player, fight.window);
      CrystalAuraModule.Plan placePlan = staged ? null : this.planPlacement(fight, lastHit, lastHit == null);
      return placePlan != null ? placePlan : breakPlan;
   }

   private CrystalAuraModule.Plan stagedBreak(CrystalAuraModule.Fight fight) {
      return this.planBreak(fight, true);
   }

   private Combo.Shot shot(CrystalAuraModule.Fight fight, CrystalAuraModule.Plan plan, boolean opening) {
      CrystalAuraModule.Step step = plan.step();
      class_243 center = step == CrystalAuraModule.Step.BREAK ? plan.crystal().method_73189() : Crystals.explosionAt(plan.target());
      int readyIn = step == CrystalAuraModule.Step.BREAK ? 0 : (step == CrystalAuraModule.Step.CRYSTAL ? fight.rtt : fight.rtt + 1);
      return new Combo.Shot(
         Combo.Kind.valueOf(step.name()),
         plan.aim(),
         plan.route(),
         this.score(fight, step, center, Float.NEGATIVE_INFINITY, opening),
         kills(fight, step, center, opening),
         readyIn,
         plan
      );
   }

   private CrystalAuraModule.Plan planBreak(CrystalAuraModule.Fight fight, boolean opening) {
      double reach = fight.player.method_55755();
      CrystalAuraModule.Plan best = null;
      float bestScore = Float.NEGATIVE_INFINITY;
      class_238 around = fight.player.method_5829().method_1014(reach + 2.0);

      for (class_1511 crystal : fight.level.method_18467(class_1511.class, around)) {
         if (crystal.method_5805()
            && this.breakAttempts.get(crystal.method_5628()) < 3
            && (crystal.method_5628() != this.pendingBreakId || this.pendingBreakAge >= fight.window)
            && (!this.doomed.containsKey(crystal.method_5628()) || fight.player.field_6012 - this.doomed.get(crystal.method_5628()) >= fight.window)) {
            class_243 eye = fight.eye;
            if (!(eye.method_1025(TargetingUtils.closestPoint(eye, crystal.method_5829())) > reach * reach)) {
               class_243 center = crystal.method_73189();
               if (!(this.bound(fight, CrystalAuraModule.Step.BREAK, center) + fight.lookBonus <= bestScore)) {
                  CrystalAuraModule.Plan plan = this.breakLook(fight, crystal);
                  if (plan != null) {
                     float score = this.score(fight, CrystalAuraModule.Step.BREAK, center, bestScore - fight.lookBonus, opening);
                     if (score != Float.NEGATIVE_INFINITY) {
                        score += fight.lookCredit(plan);
                        if (!(score <= bestScore)) {
                           best = plan;
                           bestScore = score;
                        }
                     }
                  }
               }
            }
         }
      }

      if (best == null) {
         return null;
      }

      int weapon = Crystals.weaponRoute(fight.player);
      return weapon == -1 ? null : best.withRoute(weapon);
   }

   private CrystalAuraModule.Plan breakLook(CrystalAuraModule.Fight fight, class_1511 crystal) {
      if (fight.fixedLook != null) {
         CrystalAuraModule.Plan plan = new CrystalAuraModule.Plan(CrystalAuraModule.Step.BREAK, crystal, null, null, crystal.method_5829().method_1005(), -2);
         return hitAlong(fight.player, plan, fight.eye, fight.fixedLook) != null ? plan : null;
      }

      class_2338 base = crystal.method_24515().method_10074();
      if (Crystals.isBase(fight.level.method_8320(base))) {
         CrystalAuraModule.Plan throughBase = new CrystalAuraModule.Plan(CrystalAuraModule.Step.BREAK, crystal, base, base, aimOnTop(fight.level, base), -2);
         if (hitAlong(fight.player, throughBase, fight.aimEye, fight.lookAt(throughBase.aim())) != null) {
            return throughBase;
         }
      }

      class_243 nearest = TargetingUtils.closestPoint(fight.aimEye, crystal.method_5829().method_1011(0.1));
      CrystalAuraModule.Plan plan = new CrystalAuraModule.Plan(CrystalAuraModule.Step.BREAK, crystal, null, null, nearest, -2);
      return hitAlong(fight.player, plan, fight.aimEye, fight.lookAt(nearest)) != null ? plan : null;
   }

   private CrystalAuraModule.Plan planPlacement(CrystalAuraModule.Fight fight, class_1511 breaking, boolean mayLayBase) {
      CrystalAuraModule.Plan crystal = this.planBase(fight, CrystalAuraModule.Step.CRYSTAL, breaking);
      if (this.autoObsidian.get() && mayLayBase) {
         float crystalDamage = crystal == null ? 0.0F : this.enemyDamage(fight, crystal);
         if (crystal != null && crystalDamage >= 8.0F) {
            return crystal;
         } else {
            CrystalAuraModule.Plan obsidian = this.planBase(fight, CrystalAuraModule.Step.OBSIDIAN, null);
            if (obsidian != null && crystal != null) {
               float obsidianDamage = this.enemyDamage(fight, obsidian);
               boolean upgrade = obsidianDamage >= crystalDamage + 5.0F && obsidianDamage >= crystalDamage * 2.0F;
               return upgrade ? obsidian : crystal;
            } else {
               return obsidian == null ? crystal : obsidian;
            }
         }
      } else {
         return crystal;
      }
   }

   private float enemyDamage(CrystalAuraModule.Fight fight, CrystalAuraModule.Plan plan) {
      class_243 center = Crystals.explosionAt(plan.target());
      float most = 0.0F;

      for (CrystalDamage.Victim enemy : fight.side(plan.step()).enemies) {
         most = Math.max(most, enemy.damage(center, plan.step() == CrystalAuraModule.Step.OBSIDIAN));
      }

      return most;
   }

   private CrystalAuraModule.Plan planBase(CrystalAuraModule.Fight fight, CrystalAuraModule.Step step, class_1511 breaking) {
      class_746 player = fight.player;
      class_638 level = fight.level;
      class_1792 item = step == CrystalAuraModule.Step.CRYSTAL ? class_1802.field_8301 : class_1802.field_8281;
      class_2680 anyBase = class_2246.field_10540.method_9564();
      if (Crystals.route(player, item, anyBase, this.savedSlot) == -1) {
         return null;
      }

      if (step == CrystalAuraModule.Step.OBSIDIAN && Crystals.route(player, class_1802.field_8301, anyBase, this.savedSlot) == -1) {
         return null;
      }

      double placeReach = player.method_55754();
      double breakable = player.method_55755() - 0.1;
      class_243 eye = fight.eye;
      class_2338 origin = class_2338.method_49638(eye);
      int radius = class_3532.method_15384(placeReach);
      float cycle = step == CrystalAuraModule.Step.CRYSTAL && fight.lookBonus > 0.0F ? 2.0F : 0.0F;
      List<CrystalAuraModule.Candidate> candidates = new ArrayList<>();
      class_2339 cell = new class_2339();
      class_2339 below = new class_2339();

      for (int dx = -radius; dx <= radius; dx++) {
         for (int dy = -radius; dy <= radius; dy++) {
            for (int dz = -radius; dz <= radius; dz++) {
               cell.method_25504(origin, dx, dy, dz);
               class_2680 state = level.method_8320(cell);
               if (step == CrystalAuraModule.Step.CRYSTAL ? Crystals.isBase(state) : state.method_45474()) {
                  below.method_25505(cell, class_2350.field_11033);
                  if ((step != CrystalAuraModule.Step.OBSIDIAN || level.method_8320(below).method_26206(level, below, class_2350.field_11036))
                     && (
                        step == CrystalAuraModule.Step.CRYSTAL
                           ? !Crystals.placing(player, cell, fight.window)
                           : !cell.equals(this.pendingObsidian) || this.pendingObsidianAge >= fight.window
                     )) {
                     class_2338 base = cell.method_10062();
                     class_2338 clicked = step == CrystalAuraModule.Step.CRYSTAL ? base : below.method_10062();
                     class_243 aim = aimOnTop(level, clicked);
                     if (!(eye.method_1025(aim) > placeReach * placeReach)) {
                        class_238 crystal = Crystals.boxOn(base);
                        if (!(eye.method_1025(TargetingUtils.closestPoint(eye, crystal)) > breakable * breakable)) {
                           float bound = this.bound(fight, step, Crystals.explosionAt(base));
                           if (bound != Float.NEGATIVE_INFINITY) {
                              candidates.add(new CrystalAuraModule.Candidate(new CrystalAuraModule.Plan(step, null, clicked, base, aim, -2), bound));
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      candidates.sort(Comparator.comparingDouble(CrystalAuraModule.Candidate::bound).reversed());
      CrystalAuraModule.Plan best = null;
      float bestScore = Float.NEGATIVE_INFINITY;

      for (CrystalAuraModule.Candidate candidate : candidates) {
         if (candidate.bound() + fight.lookBonus + cycle <= bestScore) {
            break;
         }

         CrystalAuraModule.Plan found = candidate.plan();
         float score = this.score(fight, step, Crystals.explosionAt(found.target()), bestScore - fight.lookBonus - cycle, false);
         if (score != Float.NEGATIVE_INFINITY
            && level.method_8621().method_11952(found.clicked())
            && this.canHostSoon(fight, found.target(), step == CrystalAuraModule.Step.CRYSTAL ? breaking : null)) {
            CrystalAuraModule.Plan plan = this.reachable(fight, found);
            if (plan != null) {
               score += fight.lookCredit(plan);
               if (cycle > 0.0F && oneLook(fight.aimEye, Crystals.boxOn(plan.target()), plan.aim(), breakable)) {
                  score += cycle;
               }

               if (!(score <= bestScore)) {
                  best = plan;
                  bestScore = score;
               }
            }
         }
      }

      if (best == null) {
         return null;
      }

      int route = Crystals.route(player, item, level.method_8320(best.clicked()), this.savedSlot);
      return route == -1 ? null : best.withRoute(route);
   }

   private CrystalAuraModule.Plan reachable(CrystalAuraModule.Fight fight, CrystalAuraModule.Plan plan) {
      if (hitAlong(fight.player, plan, fight.aimEye, fight.lookAt(plan.aim())) != null) {
         return plan;
      }

      if (plan.step() == CrystalAuraModule.Step.CRYSTAL && fight.fixedLook == null) {
         class_243 middle = class_243.method_24953(plan.clicked());

         for (class_2350 side : class_2353.field_11062) {
            class_243 face = middle.method_1031(side.method_10148() * 0.4, 0.0, side.method_10165() * 0.4);
            if (!((fight.aimEye.field_1352 - face.field_1352) * side.method_10148() + (fight.aimEye.field_1350 - face.field_1350) * side.method_10165() <= 0.0)
               )
             {
               CrystalAuraModule.Plan aimed = new CrystalAuraModule.Plan(plan.step(), null, plan.clicked(), plan.target(), face, plan.route());
               if (hitAlong(fight.player, aimed, fight.aimEye, fight.lookAt(face)) != null) {
                  return aimed;
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static class_243 aimOnTop(class_638 level, class_2338 pos) {
      double top = level.method_8320(pos).method_26218(level, pos).method_1105(class_2351.field_11052);
      return new class_243(pos.method_10263() + 0.5, pos.method_10264() + top - 0.1, pos.method_10260() + 0.5);
   }

   private float need(class_1309 target, boolean breaking, boolean forced) {
      float damage = !forced && (!this.facePlace.get() || !facePlaced(target)) ? this.minDamage.get() : 2.0F;
      return breaking ? damage * 0.6F : damage;
   }

   private static boolean facePlaced(class_1309 target) {
      return CrystalDamage.health(target) <= 10.0F || Combo.inHole(target) || armorLow(target);
   }

   private static boolean armorLow(class_1309 target) {
      for (class_1304 slot : class_1304.field_54086) {
         if (slot.method_46643()) {
            class_1799 stack = target.method_6118(slot);
            if (stack.method_7963() && stack.method_7936() - stack.method_7919() <= stack.method_7936() * 0.1F) {
               return true;
            }
         }
      }

      return false;
   }

   private float bound(CrystalAuraModule.Fight fight, CrystalAuraModule.Step step, class_243 center) {
      float best = Float.NEGATIVE_INFINITY;

      for (CrystalDamage.Victim enemy : fight.side(step).enemies) {
         class_1309 entity = enemy.entity();
         float upper = enemy.upperBound(center);
         if (Combo.kills(upper, entity) || !(upper < this.need(entity, step == CrystalAuraModule.Step.BREAK, fight.forced))) {
            best = Math.max(best, Combo.value(upper, entity));
         }
      }

      return best;
   }

   private float score(CrystalAuraModule.Fight fight, CrystalAuraModule.Step step, class_243 center, float toBeat, boolean opening) {
      CrystalAuraModule.Side side = fight.side(step);
      boolean baseToCome = step == CrystalAuraModule.Step.OBSIDIAN;
      float selfDamage = side.self.damage(center, baseToCome);
      if (!(selfDamage > this.maxSelfDamage.get()) && !CrystalDamage.endangers(fight.player, selfDamage)) {
         for (CrystalDamage.Victim friend : side.friends) {
            float damage = friend.damage(center, baseToCome);
            if (damage > this.maxSelfDamage.get() || CrystalDamage.endangers((class_1657)friend.entity(), damage)) {
               return Float.NEGATIVE_INFINITY;
            }
         }

         float best = Float.NEGATIVE_INFINITY;

         for (CrystalDamage.Victim enemy : side.enemies) {
            class_1309 entity = enemy.entity();
            float effective = effective(fight, enemy, step, center, opening);
            if (Combo.kills(effective, entity) || !(effective < this.need(entity, step == CrystalAuraModule.Step.BREAK, fight.forced))) {
               best = Math.max(best, Combo.value(effective, entity) - 0.5F * selfDamage);
            }
         }

         return best > toBeat ? best : Float.NEGATIVE_INFINITY;
      } else {
         return Float.NEGATIVE_INFINITY;
      }
   }

   private static boolean kills(CrystalAuraModule.Fight fight, CrystalAuraModule.Step step, class_243 center, boolean opening) {
      for (CrystalDamage.Victim enemy : fight.side(step).enemies) {
         if (Combo.kills(effective(fight, enemy, step, center, opening), enemy.entity())) {
            return true;
         }
      }

      return false;
   }

   private static float effective(CrystalAuraModule.Fight fight, CrystalDamage.Victim enemy, CrystalAuraModule.Step step, class_243 center, boolean opening) {
      int ahead = step == CrystalAuraModule.Step.BREAK ? (opening ? HurtClock.opensIn(enemy.entity(), fight.rtt) : 0) : fight.rtt;
      return HurtClock.effective(enemy, enemy.raw(center, step == CrystalAuraModule.Step.OBSIDIAN), ahead, fight.rtt);
   }

   private static class_239 hitAlong(class_746 player, CrystalAuraModule.Plan plan, class_243 eye, Rotation look) {
      if (look == null) {
         return null;
      }

      class_243 direction = class_243.method_1030(look.pitch(), look.yaw());
      if (plan.step() == CrystalAuraModule.Step.BREAK) {
         class_238 box = plan.crystal().method_5829();
         if (box.method_1006(eye)) {
            return new class_3966(plan.crystal(), eye);
         }

         class_243 at = (class_243)box.method_992(eye, eye.method_1019(direction.method_1021(player.method_55755()))).orElse(null);
         return at != null && MeleeAttacks.lineIsClear(player, eye, at) ? new class_3966(plan.crystal(), at) : null;
      } else {
         class_638 level = (class_638)player.method_73183();
         class_2680 clicked = level.method_8320(plan.clicked());
         class_3965 hit = clicked.method_26218(level, plan.clicked())
            .method_1092(eye, eye.method_1019(direction.method_1021(player.method_55754())), plan.clicked());
         if (hit != null && MeleeAttacks.lineIsClear(player, eye, hit.method_17784())) {
            return plan.step() == CrystalAuraModule.Step.OBSIDIAN && !plan.target().equals(Crystals.placedObsidian(level, player, hit, clicked)) ? null : hit;
         } else {
            return null;
         }
      }
   }

   private boolean perform(class_310 mc, class_746 player, CrystalAuraModule.Fight fight, CrystalAuraModule.Plan plan, CrystalAuraModule.Look look) {
      class_243 eye = player.method_33571();
      Rotation sent = ServerRotation.sent();
      Rotation next = look == CrystalAuraModule.Look.SILENT ? sent : RotationUtils.entityRotation(player);
      class_239 hit = sent == null ? null : hitAlong(player, plan, eye, sent);
      if (hit != null && next != sent && hitAlong(player, plan, eye, next) == null) {
         hit = null;
      }

      if (plan.route() != -2) {
         this.select(player, plan.route());
      }

      boolean acted = hit != null
         && this.due(plan, 0)
         && (plan.step() == CrystalAuraModule.Step.BREAK ? this.strike(mc, player, fight, plan.crystal()) : this.place(mc, player, plan, (class_3965)hit));
      if (acted) {
         MeleeAttacks.claimTick(mc);
      } else if (look == CrystalAuraModule.Look.SILENT) {
         ServerRotation.request(hit != null ? sent : fight.lookAt(plan.aim()));
      }

      return acted;
   }

   private boolean fire(class_310 mc, class_746 player, CrystalAuraModule.Fight fight, AnchorAuraModule aura, Combo.Shot shot) {
      if (shot.route() != -2) {
         this.select(player, shot.route());
      }

      if (!aura.ready(mc, player, shot)) {
         ServerRotation.request(fight.lookAt(shot.aim()));
         return false;
      } else {
         aura.fire(mc, player, shot);
         return true;
      }
   }

   private boolean strike(class_310 mc, class_746 player, CrystalAuraModule.Fight fight, class_1511 crystal) {
      if (this.mainAge < 2 && !Crystals.enchantsAddDamage(player.method_6047()) && !Crystals.dealsDamage(player, class_1799.field_8037)) {
         return false;
      }

      if (!Crystals.strike(mc, player, crystal)) {
         return false;
      }

      this.markBreak(crystal);
      this.breakAttempts.addTo(crystal.method_5628(), 1);
      this.ticksSinceBreak = 0;

      for (CrystalDamage.Victim enemy : fight.side(CrystalAuraModule.Step.BREAK).enemies) {
         float raw = enemy.raw(crystal.method_73189(), false);
         if (raw > 0.0F) {
            HurtClock.shot(player, enemy.entity(), raw, crystal.method_5628(), fight.rtt);
         }
      }

      class_238 blast = crystal.method_5829().method_1014(12.0);

      for (class_1511 other : fight.level.method_18467(class_1511.class, blast)) {
         if (other != crystal && other.method_5858(crystal) <= 144.0) {
            this.doomed.put(other.method_5628(), player.field_6012);
         }
      }

      return true;
   }

   private boolean place(class_310 mc, class_746 player, CrystalAuraModule.Plan plan, class_3965 hit) {
      if (!Crystals.rightClick(mc, player, hit)) {
         return false;
      }

      if (plan.step() == CrystalAuraModule.Step.CRYSTAL) {
         Crystals.notePlace(player, plan.target());
      } else {
         this.pendingObsidian = plan.target();
         this.pendingObsidianAge = 0;
      }

      this.ticksSincePlace = 0;
      return true;
   }

   private static boolean sameLook(Rotation a, Rotation b) {
      return a != null && Math.abs(RotationUtils.yawDelta(a.yaw(), b.yaw())) <= 2.0F && Math.abs(a.pitch() - b.pitch()) <= 2.0F;
   }

   private boolean due(CrystalAuraModule.Plan plan, int ahead) {
      return plan.step() == CrystalAuraModule.Step.BREAK
         ? this.ticksSinceBreak + ahead > this.breakDelay.getInt()
         : this.ticksSincePlace + ahead > this.placeDelay.getInt();
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
      this.ticksSinceBreak = Math.min(this.ticksSinceBreak + 1, 1000);
      this.ticksSincePlace = Math.min(this.ticksSincePlace + 1, 1000);
      this.mainAge = Math.min(this.mainAge + 1, 1000);
      this.pendingBreakAge = Math.min(this.pendingBreakAge + 1, 1000);
      this.pendingObsidianAge = Math.min(this.pendingObsidianAge + 1, 1000);
   }

   @Environment(EnvType.CLIENT)
   private record Candidate(CrystalAuraModule.Plan plan, float bound) {
   }

   @Environment(EnvType.CLIENT)
   private final class Fight {
      final class_746 player;
      final class_638 level;
      final class_243 eye;
      final class_243 aimEye;
      final List<class_1309> enemies;
      final List<class_1309> friends;
      final int window;
      final int rtt;
      final Rotation fixedLook;
      final Rotation serverLook;
      final float lookBonus;
      final int arriveLead;
      private final int placeLead;
      private final int selfPlaceLead;
      final boolean forced;
      private CrystalAuraModule.Side breakSide;
      private CrystalAuraModule.Side placeSide;

      Fight(
         class_746 player,
         class_638 level,
         List<class_1309> enemies,
         List<class_1309> friends,
         int window,
         int rtt,
         Rotation fixedLook,
         float lookBonus,
         boolean forced
      ) {
         this.player = player;
         this.level = level;
         this.eye = player.method_33571();
         this.aimEye = fixedLook != null ? this.eye : this.eye.method_1019(player.method_18798());
         this.enemies = enemies;
         this.friends = friends;
         this.window = window;
         this.rtt = rtt;
         this.fixedLook = fixedLook;
         this.serverLook = ServerRotation.sent();
         this.lookBonus = lookBonus;
         this.arriveLead = Math.min(rtt, 5);
         this.placeLead = Math.min(2 * rtt, 5);
         this.selfPlaceLead = Math.min(rtt, 5);
         this.forced = forced;
      }

      CrystalAuraModule.Side side(CrystalAuraModule.Step step) {
         if (step == CrystalAuraModule.Step.BREAK) {
            if (this.breakSide == null) {
               this.breakSide = new CrystalAuraModule.Side(this.player, this.enemies, this.friends, this.arriveLead, 0);
            }

            return this.breakSide;
         } else {
            if (this.placeSide == null) {
               this.placeSide = new CrystalAuraModule.Side(this.player, this.enemies, this.friends, this.placeLead, this.selfPlaceLead);
            }

            return this.placeSide;
         }
      }

      Rotation lookAt(class_243 aim) {
         return this.fixedLook != null ? this.fixedLook : RotationUtils.fromTo(this.aimEye, aim);
      }

      boolean holds(CrystalAuraModule.Plan plan) {
         return CrystalAuraModule.hitAlong(this.player, plan, this.eye, this.serverLook) != null;
      }

      float lookCredit(CrystalAuraModule.Plan plan) {
         return this.lookBonus > 0.0F && this.holds(plan) ? this.lookBonus : 0.0F;
      }
   }

   @Environment(EnvType.CLIENT)
   private enum Look {
      SILENT,
      CROSSHAIR;
   }

   @Environment(EnvType.CLIENT)
   private record Plan(CrystalAuraModule.Step step, class_1511 crystal, class_2338 clicked, class_2338 target, class_243 aim, int route) {
      CrystalAuraModule.Plan withRoute(int newRoute) {
         return new CrystalAuraModule.Plan(this.step, this.crystal, this.clicked, this.target, this.aim, newRoute);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Side {
      final List<CrystalDamage.Victim> enemies = new ArrayList<>();
      final List<CrystalDamage.Victim> friends = new ArrayList<>();
      final CrystalDamage.Victim self;

      Side(class_746 player, List<class_1309> targets, List<class_1309> friends, int lead, int selfLead) {
         for (class_1309 target : targets) {
            this.enemies.add(new CrystalDamage.Victim(target, Combo.lead(target, lead)));
         }

         for (class_1309 friend : friends) {
            this.friends.add(new CrystalDamage.Victim(friend, Combo.lead(friend, lead)));
         }

         this.self = new CrystalDamage.Victim(player, Combo.lead(player, selfLead));
      }
   }

   @Environment(EnvType.CLIENT)
   private enum Step {
      BREAK,
      CRYSTAL,
      OBSIDIAN;
   }
}
