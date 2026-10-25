package net.anubis.client.module.impl.donutmacros;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.anubis.client.chat.ModuleChat;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.PacketReceiveNettyEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.StringSetting;
import net.anubis.client.security.PayGuard;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1661;
import net.minecraft.class_1713;
import net.minecraft.class_1735;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1935;
import net.minecraft.class_310;
import net.minecraft.class_465;
import net.minecraft.class_7439;
import net.minecraft.class_746;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class PanicPayModule extends ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final Pattern VALID_NAME = Pattern.compile("\\.?[A-Za-z0-9_]{1,16}");
   private static final Pattern RATE_LIMIT = Pattern.compile("^you need to wait another ([\\d.]+) seconds?");
   private static final double MAX_WAIT_SECONDS = 30.0;
   private static final int INVENTORY_SLOTS = 36;
   private static final int CONFIRM_SLOT = 53;
   private static final int MAX_STRIKES = 2;
   private static final int MAX_SELL_ROUNDS = 6;
   private static final int[] ARMOR_MENU_SLOTS = new int[]{5, 6, 7, 8};
   private static final int OFFHAND_MENU_SLOT = 45;
   private static final long COMMAND_GAP_MS = 350L;
   private static final long OPEN_TIMEOUT_MS = 8000L;
   private static final long REPLY_TIMEOUT_MS = 6000L;
   private static final long MENU_SETTLE_MS = 50L;
   private static final long CONFIRM_WAIT_MS = 700L;
   private static final long RECONFIRM_WAIT_MS = 900L;
   private static final long RETURN_WAIT_MS = 600L;
   private final StringSetting payTo = this.addSetting("pay-to", new StringSetting("Pay To", "").hint("Player name"));
   private final BooleanSetting sellArmor = this.addSetting("sell-armor", new BooleanSetting("Sell Armor", true));
   private final BooleanSetting sellOffhand = this.addSetting("sell-offhand", new BooleanSetting("Sell Offhand", true));
   private final MacroWorker worker = new MacroWorker("Anubis Panic Pay");
   private volatile boolean running;
   private volatile long paidAt;
   private volatile String paidText = "";
   private volatile long poorAt;
   private volatile long errorAt;
   private volatile String errorText = "";
   private volatile long holdUntil;
   private volatile long limitedAt;
   private volatile long balanceAt;
   private volatile double balance;
   private volatile String balanceText = "";
   private long lastCommandAt;

   public PanicPayModule() {
      super("panic-pay", "Panic Pay", "Sells your whole inventory and armor with /sell, then pays all your money to one player.", ModuleCategory.DONUT_MACROS);
      this.usesNotifications();
      this.payTo.sessionOnly();
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   public boolean inConfigs() {
      return false;
   }

   @Override
   protected void onEnable() {
      class_310 mc = class_310.method_1551();
      String target = this.payTo.get().trim();
      if (mc.field_1724 == null || mc.method_1562() == null) {
         this.say(ModuleChat.body().warn("Join a server first."));
         this.requestDisable();
      } else if (!VALID_NAME.matcher(target).matches()) {
         this.say(ModuleChat.body().warn("Set Pay To to a player name first."));
         this.requestDisable();
      } else if (target.equalsIgnoreCase(mc.method_1548().method_1676())) {
         this.say(ModuleChat.body().warn("Pay To is your own name."));
         this.requestDisable();
      } else {
         boolean armor = this.sellArmor.get();
         boolean offhand = this.sellOffhand.get();
         this.running = true;
         this.lastCommandAt = 0L;
         this.say(
            ModuleChat.body().danger("Panic Pay started").text(": selling everything, then paying ").name(target).text(". Press the key again to cancel.")
         );
         this.worker.start(() -> {
            try {
               this.run(new Gui(this.worker), target, armor, offhand);
            } catch (AuctionHouse.Problem problem) {
               this.say(ModuleChat.body().warn(problem.getMessage()).text(", stopped."));
            } finally {
               this.finish(mc);
            }
         }, failure -> {
            LOGGER.error("[Anubis] Panic Pay stopped", failure);
            this.say(ModuleChat.body().danger("Stopped").text(": " + failure));
            this.finish(mc);
         });
      }
   }

   private void finish(class_310 mc) {
      mc.execute(() -> {
         if (this.running && this.isEnabled()) {
            this.setEnabled(false);
         }
      });
   }

   @Override
   protected void onDisable() {
      boolean wasRunning = this.running;
      this.running = false;
      this.worker.stop();
      class_746 player = class_310.method_1551().field_1724;
      if (wasRunning && player != null && player.field_7512 != player.field_7498) {
         player.method_7346();
      }
   }

   @EventHandler
   public void onPacket(PacketReceiveNettyEvent event) {
      if (this.running && event.packet instanceof class_7439 chat && chat.comp_763() != null) {
         String raw = chat.comp_763().getString();
         String line = ServerText.normalize(raw);
         long now = System.currentTimeMillis();
         Matcher wait = RATE_LIMIT.matcher(line);
         if (wait.find()) {
            try {
               this.holdUntil = now + (long)Math.ceil(Math.min(30.0, Double.parseDouble(wait.group(1))) * 1000.0) + 100L;
               this.limitedAt = now;
            } catch (NumberFormatException var11) {
            }
         } else {
            if (line.startsWith("you paid ")) {
               this.paidText = line;
               this.paidAt = now;
            } else if (line.contains("enough money")) {
               this.poorAt = now;
            } else if (!line.startsWith("you have $") && (!line.startsWith("balance") || !line.contains("$"))) {
               if (line.contains("not online")
                  || line.contains("not found")
                  || line.contains("cannot pay")
                  || line.contains("can't pay")
                  || line.contains("not a valid")
                  || line.contains("invalid")
                  || line.contains("doesn't exist")
                  || line.contains("does not exist")) {
                  this.errorText = raw;
                  this.errorAt = now;
               }
            } else {
               String shown = line.substring(line.indexOf(36));
               List<Double> values = ServerText.moneyAll(shown);
               String token = ServerText.moneyToken(shown);
               if (!values.isEmpty() && token != null) {
                  this.balance = values.get(0);
                  this.balanceText = token;
                  this.balanceAt = now;
               }
            }
         }
      }
   }

   private void run(Gui gui, String target, boolean armor, boolean offhand) {
      gui.closeAll();
      Map<class_1792, Integer> strikes = new HashMap<>();
      int sold = 0;

      try {
         if (armor || offhand) {
            this.takeOff(gui, armor, offhand, true);
         }

         sold = this.sellRounds(gui, strikes);
         if ((armor || offhand) && this.takeOff(gui, armor, offhand, false)) {
            sold += this.sellRounds(gui, strikes);
         }
      } catch (AuctionHouse.Problem problem) {
         this.say(ModuleChat.body().warn(problem.getMessage()).text(", paying what you have."));
         gui.closeAll();
      }

      this.say(ModuleChat.body().text("Sold ").value(sold).text(sold == 1 ? " item" : " items").text(", paying ").name(target).text("."));
      this.payBalance(target);
   }

   private int sellRounds(Gui gui, Map<class_1792, Integer> strikes) {
      int total = 0;

      for (int round = 0; round < 6; round++) {
         Map<class_1792, Integer> before = this.worker.call(() -> inventoryCounts(strikes));
         if (before.isEmpty()) {
            break;
         }

         this.openSell(gui);
         int moved = this.worker
            .call(
               () -> {
                  class_746 player = Gui.player();
                  List<Integer> slots = new ArrayList<>();

                  for (class_1735 slot : player.field_7512.field_7761) {
                     if (slot.field_7871 instanceof class_1661
                        && slot.method_7681()
                        && slot.method_34266() < 36
                        && strikes.getOrDefault(slot.method_7677().method_7909(), 0) < 2) {
                        slots.add(slot.field_7874);
                     }
                  }

                  for (int index : slots) {
                     Gui.mc().field_1761.method_2906(player.field_7512.field_7763, index, 0, class_1713.field_7794, player);
                  }

                  return slots.size();
               }
            );
         this.worker.ticks(2);
         Map<class_1792, Integer> offered = moved > 0 ? this.worker.call(PanicPayModule::waitingItems) : Map.of();
         Map<class_1792, Integer> left = offered.isEmpty() ? offered : this.confirm(gui, offered);
         gui.closeAll();
         if (left == null) {
            this.worker.sleep(600L);
         } else if (!left.isEmpty()) {
            int expected = total(before) - total(offered) + total(left);
            this.worker.waitFor(() -> total(inventoryCounts(strikes)) >= expected, 600L);
         }

         Map<class_1792, Integer> after = this.worker.call(() -> inventoryCounts(strikes));
         int soldNow = 0;

         for (Entry<class_1792, Integer> entry : before.entrySet()) {
            soldNow += Math.max(0, entry.getValue() - after.getOrDefault(entry.getKey(), 0));
         }

         total += soldNow;

         for (Entry<class_1792, Integer> entry : before.entrySet()) {
            if (after.getOrDefault(entry.getKey(), 0) >= entry.getValue()) {
               int strike = soldNow > 0 ? 2 : strikes.getOrDefault(entry.getKey(), 0) + 1;
               strikes.put(entry.getKey(), strike);
               if (strike == 2) {
                  this.say(
                     ModuleChat.body()
                        .warn("Could not sell ")
                        .name(new class_1799((class_1935)entry.getKey()).method_7964().getString())
                        .text(", skipping it.")
                  );
               }
            }
         }
      }

      return total;
   }

   private Map<class_1792, Integer> confirm(Gui gui, Map<class_1792, Integer> offered) {
      Map<class_1792, Integer> left = offered;

      for (int attempt = 0; attempt < 2 && left.equals(offered); attempt++) {
         if (!this.worker.call(PanicPayModule::sellMenuOpen)) {
            return null;
         }

         gui.click(53, 0, class_1713.field_7790);
         Map<class_1792, Integer> shown = left;
         this.worker.waitFor(() -> !sellMenuOpen() || !waitingItems().equals(shown), attempt == 0 ? 700L : 900L);
         left = this.worker.call(() -> sellMenuOpen() ? waitingItems() : null);
         if (left == null) {
            return null;
         }
      }

      return left;
   }

   private static Map<class_1792, Integer> waitingItems() {
      Map<class_1792, Integer> items = new HashMap<>();

      for (class_1735 slot : Gui.player().field_7512.field_7761) {
         if (!(slot.field_7871 instanceof class_1661) && slot.field_7874 != 53 && slot.method_7681()) {
            items.merge(slot.method_7677().method_7909(), slot.method_7677().method_7947(), Integer::sum);
         }
      }

      return items;
   }

   private static int total(Map<class_1792, Integer> counts) {
      int sum = 0;

      for (int count : counts.values()) {
         sum += count;
      }

      return sum;
   }

   private static Map<class_1792, Integer> inventoryCounts(Map<class_1792, Integer> strikes) {
      Map<class_1792, Integer> counts = new HashMap<>();
      class_1661 inventory = Gui.player().method_31548();

      for (int index = 0; index < 36; index++) {
         class_1799 stack = inventory.method_5438(index);
         if (!stack.method_7960() && strikes.getOrDefault(stack.method_7909(), 0) < 2) {
            counts.merge(stack.method_7909(), stack.method_7947(), Integer::sum);
         }
      }

      return counts;
   }

   private void openSell(Gui gui) {
      for (int attempt = 0; attempt < 3; attempt++) {
         long sent = this.command("sell");
         this.worker.waitFor(() -> sellMenuOpen() || this.limitedAt >= sent, 8000L);
         if (this.worker.call(PanicPayModule::sellMenuOpen)) {
            this.worker.sleep(50L);
            return;
         }

         gui.closeAll();
      }

      throw new AuctionHouse.Problem("The sell menu did not open");
   }

   private static boolean sellMenuOpen() {
      class_746 player = Gui.mc().field_1724;
      if (player != null && player.field_7512 != player.field_7498 && Gui.mc().field_1755 instanceof class_465) {
         int container = 0;

         for (class_1735 slot : player.field_7512.field_7761) {
            if (!(slot.field_7871 instanceof class_1661)) {
               container++;
            }
         }

         return container > 53;
      } else {
         return false;
      }
   }

   private boolean takeOff(Gui gui, boolean armor, boolean offhand, boolean onlyIntoFreeSlots) {
      gui.closeAll();
      boolean moved = this.worker.call(() -> {
         class_746 player = Gui.player();
         List<Integer> worn = new ArrayList<>();
         if (armor) {
            for (int slot : ARMOR_MENU_SLOTS) {
               if (player.field_7498.method_7611(slot).method_7681()) {
                  worn.add(slot);
               }
            }
         }

         if (offhand && player.field_7498.method_7611(45).method_7681()) {
            worn.add(45);
         }

         int free = 0;

         for (int index = 0; index < 36; index++) {
            if (player.method_31548().method_5438(index).method_7960()) {
               free++;
            }
         }

         int count = onlyIntoFreeSlots ? Math.min(free, worn.size()) : worn.size();

         for (int i = 0; i < count; i++) {
            Gui.mc().field_1761.method_2906(player.field_7498.field_7763, worn.get(i), 0, class_1713.field_7794, player);
         }

         return count > 0;
      });
      if (!moved) {
         return false;
      }

      gui.closeInventory();
      this.worker.ticks(2);
      return true;
   }

   private void payBalance(String target) {
      String amount = this.readBalance();
      if (this.balance < 1.0) {
         this.say(ModuleChat.body().warn("You have no money to pay."));
      } else {
         double step = ServerText.displayStep(this.balanceText);
         int lowered = 0;

         for (int attempt = 0; attempt < 4; attempt++) {
            PanicPayModule.Payment result = this.pay(target, amount);
            if (result == PanicPayModule.Payment.PAID) {
               this.say(ModuleChat.body().good("Paid $" + amount).text(" to ").name(target).text("."));
               if (!mentions(this.paidText, target)) {
                  this.say(ModuleChat.body().danger("Check the payment").text(": the server said \"" + this.paidText + "\"."));
               }

               return;
            }

            if (result == PanicPayModule.Payment.POOR) {
               lowered++;
               long lower = (long)Math.floor(this.balance - step * lowered);
               if (lowered > 2 || lower < 1L) {
                  throw new AuctionHouse.Problem("/pay " + target + " " + amount + " was refused: not enough money");
               }

               amount = Long.toString(lower);
            } else if (result == PanicPayModule.Payment.FAILED) {
               throw new AuctionHouse.Problem("/pay failed: " + this.errorText);
            }
         }

         throw new AuctionHouse.Problem("No answer to /pay " + target + " " + amount);
      }
   }

   private String readBalance() {
      for (int attempt = 0; attempt < 3; attempt++) {
         long sent = this.command("bal");
         this.worker.waitFor(() -> this.balanceAt >= sent || this.limitedAt >= sent, 6000L);
         if (this.balanceAt >= sent) {
            return this.balanceText;
         }
      }

      throw new AuctionHouse.Problem("Could not read your balance from /bal");
   }

   private PanicPayModule.Payment pay(String target, String amount) {
      long mark = this.command("pay " + target + " " + amount);
      Object[] pressed = new Object[]{null};
      long end = mark + 6000L;

      while (System.currentTimeMillis() < end) {
         if (this.paidAt >= mark) {
            return PanicPayModule.Payment.PAID;
         }

         if (this.poorAt >= mark) {
            return PanicPayModule.Payment.POOR;
         }

         if (this.errorAt >= mark) {
            return PanicPayModule.Payment.FAILED;
         }

         if (this.limitedAt >= mark) {
            return PanicPayModule.Payment.RETRY;
         }

         boolean otherPlayer = this.worker.call(() -> {
            Dialogs.View view = Dialogs.current(Gui.mc());
            if (view != null && view.dialog() != pressed[0]) {
               Dialogs.Button confirm = view.labelled("confirm", "yes", "pay");
               if (confirm == null) {
                  return false;
               }

               if (namesOtherPlayer(view, target)) {
                  Dialogs.close(Gui.mc());
                  pressed[0] = view.dialog();
                  return true;
               }

               if (Dialogs.press(Gui.mc(), view, confirm, Map.of())) {
                  pressed[0] = view.dialog();
               }

               return false;
            } else {
               return false;
            }
         });
         if (otherPlayer) {
            this.errorText = "the confirmation named a different player than " + target;
            return PanicPayModule.Payment.FAILED;
         }

         this.worker.sleep(50L);
      }

      return PanicPayModule.Payment.RETRY;
   }

   private static boolean mentions(String line, String target) {
      String wanted = target.toLowerCase(Locale.ROOT);
      if (wanted.startsWith(".")) {
         wanted = wanted.substring(1);
      }

      for (String word : line.split("[^a-z0-9_]+")) {
         if (word.equals(wanted)) {
            return true;
         }
      }

      return false;
   }

   private static boolean namesOtherPlayer(Dialogs.View view, String target) {
      String wanted = target.toLowerCase(Locale.ROOT);
      if (wanted.startsWith(".")) {
         wanted = wanted.substring(1);
      }

      List<String> text = new ArrayList<>(view.body());
      text.add(view.title());

      for (String line : text) {
         for (String word : line.split("[^a-z0-9_]+")) {
            if (word.length() > wanted.length() && word.startsWith(wanted)) {
               return true;
            }
         }
      }

      return false;
   }

   private long command(String command) {
      long wait = Math.max(this.holdUntil, this.lastCommandAt + 350L) - System.currentTimeMillis();
      if (wait > 0L) {
         this.worker.sleep(wait);
      }

      long sent = System.currentTimeMillis();
      this.worker.run(() -> {
         PayGuard.allow(command);
         Gui.player().field_3944.method_45730(command);
      });
      this.lastCommandAt = sent;
      return sent;
   }

   private void say(ModuleChat.Body body) {
      ModuleChat.send(this, body);
   }

   @Environment(EnvType.CLIENT)
   private enum Payment {
      PAID,
      POOR,
      RETRY,
      FAILED;
   }
}
