package net.anubis.client.module.impl.misc;

import com.example.client.mixin.AccessorElytraFlyMultiPlayerGameMode;
import com.example.client.mixin.AccessorLocalPlayer;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.EventPriority;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.combat.AutoInvTotemModule;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.service.humanize.HumanInventoryTiming;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1703;
import net.minecraft.class_1713;
import net.minecraft.class_1723;
import net.minecraft.class_1735;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_465;
import net.minecraft.class_490;
import net.minecraft.class_746;
import net.minecraft.class_9334;

@Environment(EnvType.CLIENT)
public final class AutoReplenishModule extends ClientModule {
   private static final int TRACKED_SLOTS = 10;
   private static final int OFFHAND_TRACKED_SLOT = 9;
   private static final int OFFHAND_SWAP_BUTTON = 40;
   private static final int MAIN_INVENTORY_FIRST = 9;
   private static final int MAIN_INVENTORY_LAST = 35;
   private static final double FAST_MOVEMENT_THRESHOLD_SQ = 0.06;
   private static final String MODE_INSTANT = "Instant";
   private static final String MODE_LEGIT = "Legit";
   private final ModeSetting mode = this.addSetting("mode", new ModeSetting("Mode", "Instant", "Instant", "Legit"));
   private final NumberSetting delay = this.addSetting("delay", new NumberSetting("Delay", 1.0F, 0.0F, 20.0F).step(1.0F));
   private final NumberSetting threshold = this.addSetting("threshold", new NumberSetting("Threshold", 8.0F, 1.0F, 64.0F).step(1.0F));
   private final BooleanSetting offhand = this.addSetting("offhand", new BooleanSetting("Offhand", true));
   private final class_1799[] lastKnown = new class_1799[10];
   private final class_1799[] pendingRefill = new class_1799[10];
   private static final int QUIET_TICKS_BEFORE_OPEN = 10;
   private static final int MAX_WAIT_TICKS = 10;
   private class_746 trackedPlayer;
   private int stage;
   private int delayTimer;
   private int quietTicks;
   private int waitTicks;
   private int lastSelectedSlot = -1;
   private int activeSlot = -1;
   private class_1799 activeReference = class_1799.field_8037;
   private boolean wasScreenOpen;

   public AutoReplenishModule() {
      super("auto-replenish", "Auto Replenish", "Refills a hotbar or offhand slot from the main inventory once it runs low.", ModuleCategory.MISC);
      this.delay.visibleWhen(() -> this.mode.is("Instant"));
      this.mode.onChange(this::resetJob);
   }

   @Override
   protected void onEnable() {
      this.resetJob();
   }

   @Override
   protected void onDisable() {
      this.resetJob();
      this.trackedPlayer = null;
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (player != null && mc.field_1761 != null) {
         if (player != this.trackedPlayer) {
            this.resync(player);
         }

         this.updateTracking(mc, player);
         if (this.delayTimer > 0) {
            this.delayTimer--;
         } else if (this.mode.is("Instant")) {
            this.refillNow(mc, player);
         } else {
            for (int transitions = 0; transitions < 3; transitions++) {
               switch (this.stage) {
                  case 0:
                     if (mc.field_1755 != null && !(mc.field_1755 instanceof class_490)) {
                        return;
                     }

                     if (player.method_56992() || AutoInvTotemModule.busy()) {
                        return;
                     }

                     if (this.isMovingFast(player) || ((AccessorLocalPlayer)player).anubis$lastSentInput().comp_3164()) {
                        return;
                     }

                     if (player.method_6115() || mc.field_1761.method_2923()) {
                        return;
                     }

                     if (this.quietTicks < 10) {
                        return;
                     }

                     int slot = this.nextPending(player);
                     if (slot < 0) {
                        return;
                     }

                     this.activeSlot = slot;
                     this.activeReference = this.pendingRefill[slot];
                     this.pendingRefill[slot] = class_1799.field_8037;
                     this.wasScreenOpen = mc.field_1755 instanceof class_490;
                     this.stage = this.wasScreenOpen ? 2 : 1;
                     break;
                  case 1:
                     mc.method_1507(new class_490(player));
                     this.stage = 2;
                     this.delayTimer = HumanInventoryTiming.openGuiDelayTicks();
                     return;
                  case 2:
                     if (!(mc.field_1755 instanceof class_490 screen)) {
                        if (this.activeSlot >= 0 && !this.activeReference.method_7960() && this.pendingRefill[this.activeSlot].method_7960()) {
                           this.pendingRefill[this.activeSlot] = this.activeReference;
                        }

                        this.resetJob();
                        return;
                     }

                     boolean safe = AutoInvTotemModule.clickSafe(player) && !AutoInvTotemModule.busy();
                     if (!safe && ++this.waitTicks <= 10) {
                        return;
                     }

                     if (!safe && this.activeSlot >= 0 && this.pendingRefill[this.activeSlot].method_7960()) {
                        this.pendingRefill[this.activeSlot] = this.activeReference;
                     }

                     if (safe && this.stillNeedsRefill(player)) {
                        class_1735 source = this.findSourceSlot(screen.method_17577(), player);
                        if (source != null) {
                           int button = this.activeSlot == 9 ? 40 : this.activeSlot;
                           ((AccessorElytraFlyMultiPlayerGameMode)mc.field_1761).anubis$elytraFlySyncSelected();
                           mc.field_1761.method_2906(((class_1723)screen.method_17577()).field_7763, source.field_7874, button, class_1713.field_7791, player);
                        }
                     }

                     this.stage = 3;
                     this.delayTimer = HumanInventoryTiming.closeGuiDelayTicks();
                     return;
                  case 3:
                     if (!this.wasScreenOpen && mc.field_1755 instanceof class_490) {
                        mc.field_1724.method_7346();
                     }

                     this.resetJob();
                     return;
                  default:
                     this.resetJob();
               }
            }
         }
      }
   }

   private void refillNow(class_310 mc, class_746 player) {
      if (mc.field_1755 == null && !player.method_56992() && !player.method_6115()) {
         if (AutoInvTotemModule.clickSafe(player) && !AutoInvTotemModule.busy()) {
            int slot = this.nextPending(player);
            if (slot >= 0) {
               this.activeSlot = slot;
               this.activeReference = this.pendingRefill[slot];
               this.pendingRefill[slot] = class_1799.field_8037;
               class_1703 menu = player.field_7498;
               class_1735 source = this.findSourceSlot(menu, player);
               if (source != null && menu.method_34255().method_7960()) {
                  int button = this.activeSlot == 9 ? 40 : this.activeSlot;
                  ((AccessorElytraFlyMultiPlayerGameMode)mc.field_1761).anubis$elytraFlySyncSelected();
                  mc.field_1761.method_2906(menu.field_7763, source.field_7874, button, class_1713.field_7791, player);
                  this.delayTimer = this.delay.getInt();
               }

               this.activeSlot = -1;
               this.activeReference = class_1799.field_8037;
            }
         }
      }
   }

   private void resync(class_746 player) {
      this.trackedPlayer = player;

      for (int i = 0; i < 10; i++) {
         class_1799 current = i == 9 ? player.method_6079() : player.method_31548().method_5438(i);
         this.lastKnown[i] = current.method_7972();
         this.pendingRefill[i] = class_1799.field_8037;
      }

      this.resetJob();
   }

   private void updateTracking(class_310 mc, class_746 player) {
      boolean foreignScreenOpen = this.stage == 0 && mc.field_1755 instanceof class_465;
      boolean infinite = player.method_56992();
      boolean changed = false;
      int selectedSlot = player.method_31548().method_67532();
      if (selectedSlot != this.lastSelectedSlot) {
         this.lastSelectedSlot = selectedSlot;
         changed = true;
      }

      for (int i = 0; i < 10; i++) {
         if (i == 9 && !this.offhand.get()) {
            this.lastKnown[i] = player.method_6079().method_7972();
            this.pendingRefill[i] = class_1799.field_8037;
         } else {
            class_1799 current = i == 9 ? player.method_6079() : player.method_31548().method_5438(i);
            class_1799 previous = this.lastKnown[i];
            if (!class_1799.method_7973(previous, current)) {
               changed = true;
            }

            this.lastKnown[i] = current.method_7972();
            if (current.method_31574(class_1802.field_8288) || previous.method_31574(class_1802.field_8288)) {
               this.pendingRefill[i] = class_1799.field_8037;
            } else if (infinite) {
               this.pendingRefill[i] = class_1799.field_8037;
            } else if (!foreignScreenOpen) {
               boolean sameItem = previous.method_7960() || current.method_7909() == previous.method_7909();
               if (current.method_7960() && !previous.method_7960()) {
                  this.pendingRefill[i] = previous.method_7972();
               } else if (!previous.method_7960() && !previous.method_7946() && !sameItem) {
                  this.pendingRefill[i] = previous.method_7972();
               } else if (!current.method_7946()
                  || current.method_7960()
                  || !sameItem
                  || current.method_7947() > this.threshold.getInt()
                  || !this.pendingRefill[i].method_7960() && this.pendingRefill[i].method_7909() != current.method_7909()) {
                  if (!current.method_7960() && current.method_7947() > this.threshold.getInt()) {
                     this.pendingRefill[i] = class_1799.field_8037;
                  }
               } else {
                  this.pendingRefill[i] = current.method_7972();
               }
            }
         }
      }

      this.quietTicks = changed ? 0 : Math.min(this.quietTicks + 1, 1000);
   }

   private int nextPending(class_746 player) {
      for (int i = 0; i < 10; i++) {
         if (!this.pendingRefill[i].method_7960() && this.hasSource(player, i, this.pendingRefill[i])) {
            return i;
         }
      }

      return -1;
   }

   private boolean stillNeedsRefill(class_746 player) {
      class_1799 current = this.activeSlot == 9 ? player.method_6079() : player.method_31548().method_5438(this.activeSlot);
      if (current.method_7960()) {
         return true;
      } else {
         return current.method_7909() != this.activeReference.method_7909() ? true : current.method_7946() && current.method_7947() <= this.threshold.getInt();
      }
   }

   private boolean hasSource(class_746 player, int trackedSlot, class_1799 reference) {
      class_1799 targetNow = trackedSlot == 9 ? player.method_6079() : player.method_31548().method_5438(trackedSlot);

      for (int i = 9; i <= 35; i++) {
         if (isViableSource(player.method_31548().method_5438(i), targetNow, reference)) {
            return true;
         }
      }

      return false;
   }

   private class_1735 findSourceSlot(class_1703 menu, class_746 player) {
      class_1799 targetNow = this.activeSlot == 9 ? player.method_6079() : player.method_31548().method_5438(this.activeSlot);
      int bestCount = class_1799.method_7984(targetNow, this.activeReference) ? targetNow.method_7947() : 0;
      class_1735 best = null;

      for (class_1735 slot : menu.field_7761) {
         if (slot.field_7874 >= 9 && slot.field_7874 <= 35) {
            class_1799 candidate = slot.method_7677();
            if (matchesReference(candidate, this.activeReference) && candidate.method_7947() > bestCount) {
               best = slot;
               bestCount = candidate.method_7947();
            }
         }
      }

      return best;
   }

   private static boolean isViableSource(class_1799 candidate, class_1799 targetNow, class_1799 reference) {
      int have = class_1799.method_7984(targetNow, reference) ? targetNow.method_7947() : 0;
      return matchesReference(candidate, reference) && candidate.method_7947() > have;
   }

   private static boolean matchesReference(class_1799 candidate, class_1799 reference) {
      if (candidate.method_7960()) {
         return false;
      } else {
         return reference.method_7946()
            ? class_1799.method_31577(candidate, reference)
            : class_1799.method_75222(candidate, reference, type -> type == class_9334.field_49629);
      }
   }

   private boolean isMovingFast(class_746 player) {
      if (player.method_5624()) {
         return true;
      } else if (player.method_5765()) {
         class_1297 vehicle = player.method_5668();
         double dx = vehicle.method_23317() - vehicle.field_6014;
         double dz = vehicle.method_23321() - vehicle.field_5969;
         return dx * dx + dz * dz > 0.06;
      } else {
         class_243 velocity = player.method_18798();
         return velocity.field_1352 * velocity.field_1352 + velocity.field_1350 * velocity.field_1350 > 0.06;
      }
   }

   private void resetJob() {
      this.stage = 0;
      this.delayTimer = 0;
      this.waitTicks = 0;
      this.activeSlot = -1;
      this.activeReference = class_1799.field_8037;
      this.wasScreenOpen = false;
   }
}
