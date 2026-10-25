package net.anubis.client.module.impl.combat;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Optional;
import net.anubis.client.chat.ModuleChat;
import net.anubis.client.event.EventPriority;
import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.KeyInputEvent;
import net.anubis.client.event.impl.MouseInputEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.input.ModuleKeybinds;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.CombatMacroLock;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.anubis.client.module.impl.combat.melee.SlotReturn;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.KeySetting;
import net.anubis.client.util.pvp.Rotation;
import net.anubis.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12125;
import net.minecraft.class_1268;
import net.minecraft.class_1297;
import net.minecraft.class_1661;
import net.minecraft.class_1799;
import net.minecraft.class_1887;
import net.minecraft.class_1893;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_6880;
import net.minecraft.class_746;
import net.minecraft.class_9334;

@Environment(EnvType.CLIENT)
public final class SpearLungeModule extends ClientModule {
   private static final int BUFFER_TICKS = 10;
   private static final double POKE_RANGE = 4.4;
   private static final double POKE_MARGIN = 0.125;
   private final KeySetting key = this.addSetting("lunge-key", new KeySetting("Lunge Key", -1));
   private final BooleanSetting reachPoke = this.addSetting("reach-poke", new BooleanSetting("Reach Poke", false));
   private final BooleanSetting switchBack = this.addSetting("switch-back", new BooleanSetting("Switch Back", true));
   private final SlotReturn slots = new SlotReturn(this);
   private boolean pending;
   private int pendingTicks;
   private class_746 owner;

   public SpearLungeModule() {
      super("spear-swap", "Spear Swap", "Swaps to your Lunge spear and lunges. Reach Poke stabs with a spear past sword reach.", ModuleCategory.COMBAT);
   }

   @Override
   protected void onEnable() {
      this.pending = false;
      this.owner = class_310.method_1551().field_1724;
      this.on(KeyInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onKey);
      this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onMouse);
      this.on(TickEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onTick);
   }

   @Override
   protected void onDisable() {
      this.pending = false;
      this.owner = null;
      this.slots.drop(class_310.method_1551(), this.switchBack.get());
   }

   private void onKey(KeyInputEvent event) {
      if (event.action == 1) {
         this.onPress(event.key);
      }
   }

   private void onMouse(MouseInputEvent event) {
      if (event.action == 1) {
         this.onPress(1000 + event.button);
      }
   }

   private void onPress(int pressed) {
      int bound = this.key.value();
      if (ModuleKeybinds.isBindable(bound) && pressed == bound) {
         class_310 mc = class_310.method_1551();
         if (mc.field_1755 == null && mc.field_1724 != null && mc.field_1687 != null) {
            if (bestSpear(mc.field_1724.method_31548(), true) < 0) {
               ModuleChat.actionBar(this, ModuleChat.body().warn("No Lunge spear").text(" in your hotbar"));
            } else {
               this.pending = true;
               this.pendingTicks = 0;
            }
         }
      }
   }

   private void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      this.slots.tick(mc, this.switchBack.get());
      class_746 player = mc.field_1724;
      if (player != this.owner) {
         this.owner = player;
         this.pending = false;
      }

      if (player != null && mc.field_1687 != null && mc.field_1761 != null) {
         if (this.pending) {
            this.tryLunge(mc, player);
         }

         if (this.reachPoke.get() && MeleeAttacks.clickPending(mc)) {
            this.tryPoke(mc, player);
         }
      }
   }

   private void tryLunge(class_310 mc, class_746 player) {
      if (++this.pendingTicks > 10) {
         this.pending = false;
      } else if (this.canStab(mc, player) && !player.method_5799() && !player.method_6128() && !player.method_5765()) {
         if (player.method_7344().method_75882() || player.method_31549().field_7478) {
            class_1661 inventory = player.method_31548();
            int slot = bestSpear(inventory, true);
            if (slot < 0) {
               this.pending = false;
            } else {
               class_1799 spear = inventory.method_5438(slot);
               if (canJab(mc, player, spear) && this.slots.take(player, slot)) {
                  this.pending = false;
                  stab(mc, player, (class_12125)spear.method_58694(class_9334.field_63631));
               }
            }
         }
      }
   }

   private void tryPoke(class_310 mc, class_746 player) {
      if (this.canStab(mc, player) && !player.method_6047().method_57826(class_9334.field_63631)) {
         if (MeleeAttacks.crosshairHit(mc) == null) {
            class_1661 inventory = player.method_31548();
            int slot = bestSpear(inventory, false);
            if (slot >= 0) {
               class_1799 spear = inventory.method_5438(slot);
               if (canJab(mc, player, spear) && enemyPastReach(player) && this.slots.take(player, slot)) {
                  stab(mc, player, (class_12125)spear.method_58694(class_9334.field_63631));
               }
            }
         }
      }
   }

   private static void stab(class_310 mc, class_746 player, class_12125 weapon) {
      mc.field_1761.method_75407(weapon);
      if (MeleeAttacks.actedThisTick(player)) {
         player.method_6104(class_1268.field_5808);
         MeleeAttacks.claimTick(mc);
      }
   }

   private boolean canStab(class_310 mc, class_746 player) {
      return MeleeAttacks.canAct(mc) && MeleeAttacks.handsFree(mc, player) && !MeleeAttacks.actedThisTick(player)
         ? !CombatMacroLock.heldByOther(this) && !AutoInvTotemModule.wantsHotbar()
         : false;
   }

   private static boolean canJab(class_310 mc, class_746 player, class_1799 spear) {
      return mc.field_1771 <= 0 && !mc.field_1761.method_2928() ? spear.method_45435(mc.field_1687.method_45162()) && !player.method_75202(spear, 0) : false;
   }

   private static boolean enemyPastReach(class_746 player) {
      Rotation sent = ServerRotation.sent();
      class_243 look = sent != null ? class_243.method_1030(sent.pitch(), sent.yaw()) : player.method_5828(1.0F);
      class_243 eye = player.method_33571();
      class_243 end = eye.method_1019(look.method_1021(4.4));
      double reach = player.method_55755();
      class_238 area = player.method_5829().method_18804(look.method_1021(4.4)).method_1014(1.0);

      for (class_1297 entity : player.method_73183().method_8335(player, area)) {
         if (MeleeAttacks.isEnemy(player, entity)) {
            Optional<class_243> at = entity.method_5829().method_1014(0.125).method_992(eye, end);
            if (at.isPresent() && eye.method_1022(at.get()) > reach && MeleeAttacks.lineIsClear(player, eye, at.get())) {
               return true;
            }
         }
      }

      return false;
   }

   private static int bestSpear(class_1661 inventory, boolean lunge) {
      int selected = inventory.method_67532();
      int best = -1;
      int bestLevel = 0;

      for (int slot = 0; slot < class_1661.method_7368(); slot++) {
         class_1799 stack = inventory.method_5438(slot);
         if (stack.method_57826(class_9334.field_63631) && !stack.method_63692()) {
            int level = lungeLevel(stack);
            boolean better = !lunge ? best < 0 || level < bestLevel : level > bestLevel || level > 0 && level == bestLevel && slot == selected;
            if (better) {
               best = slot;
               bestLevel = level;
            }
         }
      }

      return best;
   }

   private static int lungeLevel(class_1799 stack) {
      for (Entry<class_6880<class_1887>> entry : stack.method_58657().method_57539()) {
         if (((class_6880)entry.getKey()).method_40225(class_1893.field_63420)) {
            return entry.getIntValue();
         }
      }

      return 0;
   }
}
