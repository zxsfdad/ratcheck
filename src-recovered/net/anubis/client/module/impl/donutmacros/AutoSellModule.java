package net.anubis.client.module.impl.donutmacros;

import net.anubis.client.chat.ModuleChat;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.StringListSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1661;
import net.minecraft.class_1703;
import net.minecraft.class_1713;
import net.minecraft.class_1735;
import net.minecraft.class_1799;
import net.minecraft.class_310;
import net.minecraft.class_465;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class AutoSellModule extends ClientModule {
   private static final int INVENTORY_SLOTS = 36;
   private static final int CONFIRM_SLOT = 53;
   private static final long POLL_MS = 350L;
   private static final long COMMAND_COOLDOWN_MS = 4000L;
   private static final long OPEN_TIMEOUT_MS = 10000L;
   private static final long CONFIRM_COOLDOWN_MS = 900L;
   private static final long POST_CONFIRM_MS = 250L;
   private static final int STUCK_CONFIRMS = 3;
   private final StringListSetting neverSell = this.addSetting(
      "never-sell", new StringListSetting("Never Sell", "minecraft:netherite_sword", "golden_apple", "minecraft:totem_of_undying").itemPicker()
   );
   private int menuId = -1;
   private long openingUntil;
   private long lastCommandAt;
   private long lastConfirmAt;
   private long nextPollAt;
   private boolean confirmPending;
   private String lastFingerprint = "";
   private int sameAfterConfirm;
   private int sold;
   private int confirmedCount;
   private boolean warnedNoOpen;

   public AutoSellModule() {
      super(
         "auto-sell", "Auto Sell", "Keeps the /sell menu open and keeps selling everything that is not on your Never Sell list.", ModuleCategory.DONUT_MACROS
      );
      this.usesNotifications();
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      this.menuId = -1;
      this.openingUntil = 0L;
      this.lastCommandAt = 0L;
      this.lastConfirmAt = 0L;
      this.nextPollAt = 0L;
      this.confirmPending = false;
      this.lastFingerprint = "";
      this.sameAfterConfirm = 0;
      this.sold = 0;
      this.confirmedCount = 0;
      this.warnedNoOpen = false;
   }

   @Override
   protected void onDisable() {
      class_746 player = class_310.method_1551().field_1724;
      if (player != null && this.ours(player)) {
         player.method_7346();
      }

      this.menuId = -1;
      if (this.sold > 0) {
         ModuleChat.send(this, ModuleChat.body().text("Sold ").value(this.sold).text(this.sold == 1 ? " item." : " items."));
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (player != null && mc.method_1562() != null && mc.field_1761 != null && !player.method_29504()) {
         long now = System.currentTimeMillis();
         if (this.menuId < 0 && this.openingUntil > 0L) {
            if (player.field_7512 != player.field_7498 && mc.field_1755 instanceof class_465 && containerSlots(player.field_7512) > 53) {
               this.menuId = player.field_7512.field_7763;
               this.openingUntil = 0L;
               this.nextPollAt = now + 150L;
               this.warnedNoOpen = false;
            } else if (now > this.openingUntil) {
               this.openingUntil = 0L;
               if (!this.warnedNoOpen) {
                  ModuleChat.send(this, ModuleChat.body().warn("The sell menu did not open").text(", retrying."));
               }

               this.warnedNoOpen = true;
            }
         } else if (!this.ours(player)) {
            this.menuId = -1;
            if (mc.field_1755 == null && now - this.lastCommandAt >= 4000L) {
               player.field_3944.method_45730("sell");
               this.lastCommandAt = now;
               this.openingUntil = now + 10000L;
            }
         } else if (now >= this.nextPollAt) {
            this.nextPollAt = now + 350L;
            class_1703 menu = player.field_7512;
            if (this.confirmedCount > 0) {
               int gone = this.confirmedCount - countWaiting(menu);
               this.confirmedCount = 0;
               if (gone > 0) {
                  this.sold += gone;
                  ModuleChat.actionBar(this, ModuleChat.body().text("Sold ").value(this.sold).text(" items this session"));
               }
            }

            if (this.confirmPending) {
               this.confirmPending = false;
               if (waiting(menu) && now - this.lastConfirmAt >= 900L && this.sameAfterConfirm < 3) {
                  int before = countWaiting(menu);
                  mc.field_1761.method_2906(menu.field_7763, 53, 0, class_1713.field_7790, player);
                  this.lastConfirmAt = now;
                  this.lastFingerprint = fingerprint(menu);
                  this.confirmedCount = before;
                  this.nextPollAt = now + 250L;
               }
            } else {
               int moved = 0;

               for (class_1735 slot : menu.field_7761) {
                  if (slot.field_7871 instanceof class_1661
                     && slot.method_7681()
                     && slot.method_34266() < 36
                     && !SellRoutine.neverSell(slot.method_7677(), this.neverSell.get())) {
                     mc.field_1761.method_2906(menu.field_7763, slot.field_7874, 0, class_1713.field_7794, player);
                     moved++;
                  }
               }

               String fingerprint = fingerprint(menu);
               if (moved > 0 || !fingerprint.equals(this.lastFingerprint)) {
                  this.sameAfterConfirm = 0;
               } else if (waiting(menu)) {
                  this.sameAfterConfirm++;
               }

               if (moved > 0 || waiting(menu)) {
                  this.confirmPending = true;
                  this.nextPollAt = now + 50L;
               }
            }
         }
      } else {
         this.menuId = -1;
      }
   }

   private boolean ours(class_746 player) {
      return this.menuId >= 0 && player.field_7512 != player.field_7498 && player.field_7512.field_7763 == this.menuId;
   }

   private static boolean waiting(class_1703 menu) {
      return countWaiting(menu) > 0;
   }

   private static int countWaiting(class_1703 menu) {
      int count = 0;

      for (class_1735 slot : menu.field_7761) {
         if (!(slot.field_7871 instanceof class_1661) && slot.field_7874 != 53 && slot.method_7681()) {
            count += slot.method_7677().method_7947();
         }
      }

      return count;
   }

   private static int containerSlots(class_1703 menu) {
      int count = 0;

      for (class_1735 slot : menu.field_7761) {
         if (!(slot.field_7871 instanceof class_1661)) {
            count++;
         }
      }

      return count;
   }

   private static String fingerprint(class_1703 menu) {
      StringBuilder text = new StringBuilder();

      for (class_1735 slot : menu.field_7761) {
         if (slot.field_7871 instanceof class_1661) {
            break;
         }

         class_1799 stack = slot.method_7677();
         if (!stack.method_7960()) {
            text.append(slot.field_7874).append(':').append(stack.method_7909()).append('x').append(stack.method_7947()).append(';');
         }
      }

      return text.toString();
   }
}
