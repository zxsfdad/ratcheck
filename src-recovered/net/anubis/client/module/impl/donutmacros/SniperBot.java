package net.anubis.client.module.impl.donutmacros;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.anubis.client.chat.ModuleChat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1713;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1890;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_5251;
import net.minecraft.class_640;
import net.minecraft.class_746;
import net.minecraft.class_7923;
import net.minecraft.class_9290;
import net.minecraft.class_9334;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class SniperBot {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final Pattern RATE_LIMIT = Pattern.compile("^you need to wait another ([\\d.]+) seconds?");
   private static final double MAX_WAIT_SECONDS = 30.0;
   private static final Pattern BOUGHT_COUNT = Pattern.compile("^(\\d{1,4})\\s*x?\\s+");
   private static final Pattern TIME = Pattern.compile("\\d+\\s*[dhms]\\b");
   private static final int LISTING_SLOTS = 45;
   private static final int DEFAULT_FILTER = 47;
   private static final int DEFAULT_REFRESH = 49;
   private static final long OPEN_TIMEOUT_MS = 10000L;
   private static final long CONTENT_TIMEOUT_MS = 3000L;
   private static final long SORT_TIMEOUT_MS = 3000L;
   private static final long SETTLE_MS = 150L;
   private static final long SETTLE_LIMIT_MS = 1500L;
   private static final int SETTLE_TICKS = 2;
   private static final long ANSWER_DEFAULT_MS = 1000L;
   private static final long ANSWER_MIN_MS = 700L;
   private static final long ANSWER_MAX_MS = 3000L;
   private static final long ANSWER_SLACK_MS = 300L;
   private static final double CONFIRM_PRICE_SLACK = 1.02;
   private static final long BUY_TIMEOUT_MS = 4000L;
   private static final long SCREEN_GRACE_MS = 400L;
   private static final long REOPEN_WAIT_MS = 1000L;
   private static final long LATE_PURCHASE_MS = 15000L;
   private static final long FAILED_SKIP_MS = 60000L;
   private static final long WRONG_SKIP_MS = 600000L;
   private static final long BOUGHT_SKIP_MS = 15000L;
   private static final long RECOVERY_MS = 2000L;
   private static final long POLL_MS = 2L;
   private static final int TIMER_SLACK_MINUTES = 2;
   private static final int STRIKES_TO_SKIP = 2;
   private static final int UNREADABLE_TO_STOP = 4;
   private static final SniperBot.Hit CLOSED = new SniperBot.Hit(-1, class_1799.field_8037, 0L, 0L, -1, "", "");
   private final AhSniperModule module;
   private final MacroWorker worker;
   private final Gui gui;
   private final SnipeTarget target;
   private final long refreshMs;
   private final long settleMs;
   private final long budget;
   private final boolean requireConfirm;
   private final int maxBuys;
   private final AuctionHouse ah;
   private final AuctionHouse.Stock stock;
   private final List<AuctionHouse.Stock> stocks;
   private final Map<String, Long> skip = new HashMap<>();
   private final Map<String, Integer> strikes = new HashMap<>();
   private volatile SniperBot.Purchase purchase;
   private volatile long goneAt;
   private volatile long poorAt;
   private volatile long ownAt;
   private volatile long holdUntil;
   private long handledAt;
   private SniperBot.Hit lastHit;
   private long lastClickAt;
   private long pageKey = Long.MIN_VALUE;
   private long pageChangedAt;
   private int pageChangedTick;
   private boolean refreshPending;
   private long refreshSentAt;
   private long answerMs;
   private long nextRefresh;
   private volatile boolean confirmPressed;
   private boolean instantWarned;
   private int unreadable;
   private int bought;
   private int unsold;
   private long spent;
   private int wrong;
   private long wrongSpent;

   SniperBot(
      AhSniperModule module,
      MacroWorker worker,
      SnipeTarget target,
      long refreshMs,
      long settleMs,
      long budget,
      boolean requireConfirm,
      int maxBuys,
      long sellPrice,
      String ign
   ) {
      this.module = module;
      this.worker = worker;
      this.gui = new Gui(worker);
      this.target = target;
      this.refreshMs = refreshMs;
      this.settleMs = settleMs;
      this.budget = budget;
      this.requireConfirm = requireConfirm;
      this.maxBuys = maxBuys;
      if (sellPrice > 0L) {
         this.ah = new AuctionHouse(module, worker, new AuctionHouse.Config(0L, 60000L, 20000L, 1, 1000, 0L, 3.0, 20.0, 36, 15000L), ign);
         this.stock = new AuctionHouse.Stock(target.product(sellPrice), true);
         this.stocks = List.of(this.stock);
      } else {
         this.ah = null;
         this.stock = null;
         this.stocks = List.of();
      }
   }

   private void say(ModuleChat.Body body) {
      ModuleChat.send(this.module, body);
   }

   void run() {
      this.say(
         ModuleChat.body()
            .text("Sniping ")
            .name(this.target.describe())
            .text(" under $")
            .value(ServerText.shortMoney(this.target.maxPrice))
            .text(this.target.minMinutes > 0 ? " with " + formatMinutes(this.target.minMinutes) + "+ left" : "")
            .text(this.stock != null ? ", selling at $" + ServerText.shortMoney(this.stock.price) : "")
            .text(this.budget > 0L ? ", spending at most $" + ServerText.shortMoney(this.budget) : "")
            .text(".")
      );

      while (true) {
         this.worker.check();

         try {
            if (!this.worker.call(() -> Gui.mc().field_1724 != null && Gui.mc().method_1562() != null)) {
               this.worker.sleep(2000L);
            } else {
               if (this.stock != null && this.unsold > 0) {
                  this.sellStock();
               }

               this.snipe();
            }
         } catch (MacroWorker.Cancelled cancelled) {
            throw cancelled;
         } catch (RuntimeException failure) {
            String reason = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            this.say(ModuleChat.body().warn(reason).text(", trying again."));
            this.worker.check();
            this.gui.closeAll();
            this.worker.sleep(2000L);
         }
      }
   }

   private void snipe() {
      this.openAuction();
      this.ensureRecent();
      this.pageKey = Long.MIN_VALUE;
      this.refreshPending = false;
      this.nextRefresh = System.currentTimeMillis() + this.refreshMs;

      while (true) {
         this.worker.check();
         SniperBot.Purchase late = this.purchase;
         if (late != null && late.at() > this.handledAt) {
            if (this.lastHit != null && late.at() - this.lastClickAt <= 15000L) {
               if (this.bought(this.lastHit, late)) {
                  return;
               }
               continue;
            }

            this.handledAt = late.at();
         }

         long mark = System.currentTimeMillis();
         SniperBot.Hit hit = this.worker.call(this::pageTick);
         if (hit == CLOSED) {
            return;
         }

         if (hit != null) {
            this.lastHit = hit;
            this.lastClickAt = mark;
            if (this.handleBuy(hit, mark)) {
               return;
            }

            this.pageKey = Long.MIN_VALUE;
            this.nextRefresh = System.currentTimeMillis() + this.refreshMs;
         } else {
            this.worker.sleep(2L);
         }
      }
   }

   private void openAuction() {
      this.gui.closeAll();
      long wait = this.holdUntil - System.currentTimeMillis();
      if (wait > 0L) {
         this.worker.sleep(wait);
      }

      this.gui.command("ah " + this.target.search);
      if (!this.worker.waitFor(SniperBot::auctionOpenNow, 10000L)) {
         throw new AuctionHouse.Problem("The AH did not open");
      }

      this.worker.waitFor(() -> {
         if (!auctionOpenNow()) {
            return true;
         }

         List<class_1799> stacks = Gui.containerNow();
         return refreshSlot(stacks) >= 0 || filterSlot(stacks) >= 0;
      }, 3000L);
      this.settle();
   }

   private void settle() {
      long end = System.currentTimeMillis() + 1500L;
      int last = this.worker.call(SniperBot::stateNow);
      long stableSince = System.currentTimeMillis();

      while (System.currentTimeMillis() < end) {
         this.worker.sleep(25L);
         int state = this.worker.call(SniperBot::stateNow);
         long now = System.currentTimeMillis();
         if (state != last) {
            last = state;
            stableSince = now;
         } else if (now - stableSince >= 150L) {
            return;
         }
      }
   }

   private static int stateNow() {
      class_746 player = Gui.mc().field_1724;
      return player == null ? -1 : player.field_7512.method_37421();
   }

   private static boolean auctionOpenNow() {
      if (!Gui.containerOpenNow()) {
         return false;
      }

      String title = ServerText.normalize(Gui.mc().field_1755.method_25440());
      return title.contains("order") ? false : title.contains("auction") || refreshSlot(Gui.containerNow()) >= 0;
   }

   private void ensureRecent() {
      for (int attempt = 0; attempt < 4; attempt++) {
         int[] state = this.worker.call(() -> {
            List<class_1799> stacks = Gui.containerNow();
            int slot = filterSlot(stacks);
            return new int[]{slot, slot < 0 ? SniperBot.Sort.UNKNOWN.ordinal() : selectedSort(stacks.get(slot)).ordinal()};
         });
         if (state[0] < 0) {
            this.say(ModuleChat.body().warn("No Filter button found").text(", sniping with the current sorting."));
            return;
         }

         SniperBot.Sort sort = SniperBot.Sort.values()[state[1]];
         if (sort == SniperBot.Sort.RECENT) {
            return;
         }

         if (sort == SniperBot.Sort.UNKNOWN && attempt > 0) {
            this.say(ModuleChat.body().warn("Could not read the Filter").text(", make sure it is on Recently Listed."));
            return;
         }

         String before = this.gui.fingerprint();
         this.gui.click(state[0], 0, class_1713.field_7790);
         if (sort == SniperBot.Sort.UNKNOWN) {
            this.gui.waitChange(before, 3000L);
         } else {
            this.worker.waitFor(() -> {
               SniperBot.Sort now = sortNow();
               return now != SniperBot.Sort.UNKNOWN && now != sort;
            }, 3000L);
         }

         this.settle();
      }
   }

   private static SniperBot.Sort sortNow() {
      class_746 player = Gui.mc().field_1724;
      if (player != null && auctionOpenNow() && player.field_7512.method_34255().method_7960()) {
         List<class_1799> stacks = Gui.containerNow();
         int slot = filterSlot(stacks);
         return slot < 0 ? SniperBot.Sort.UNKNOWN : selectedSort(stacks.get(slot));
      } else {
         return SniperBot.Sort.UNKNOWN;
      }
   }

   private static int filterSlot(List<class_1799> stacks) {
      for (int i = 45; i < stacks.size(); i++) {
         class_1799 stack = stacks.get(i);
         if (!stack.method_7960() && (stack.method_31574(class_1802.field_8239) || Gui.nameOf(stack).contains("filter"))) {
            return i;
         }
      }

      return stacks.size() > 47 && !stacks.get(47).method_7960() ? 47 : -1;
   }

   private static int refreshSlot(List<class_1799> stacks) {
      for (int i = 45; i < stacks.size(); i++) {
         class_1799 stack = stacks.get(i);
         if (!stack.method_7960() && stack.method_31574(class_1802.field_8782)) {
            return i;
         }
      }

      return stacks.size() > 49 && stacks.get(49).method_31574(class_1802.field_8782) ? 49 : -1;
   }

   static SniperBot.Sort selectedSort(class_1799 filter) {
      List<SniperBot.Sort> options = new ArrayList<>();
      List<Integer> colors = new ArrayList<>();

      for (class_2561 line : ((class_9290)filter.method_58695(class_9334.field_49632, class_9290.field_49340)).comp_2400()) {
         String text = ServerText.normalize(Dialogs.visibleText(line));
         SniperBot.Sort option = text.contains("recent")
            ? SniperBot.Sort.RECENT
            : (text.contains("lowest") ? SniperBot.Sort.LOWEST : (text.contains("highest") ? SniperBot.Sort.HIGHEST : SniperBot.Sort.UNKNOWN));
         if (option != SniperBot.Sort.UNKNOWN) {
            options.add(option);
            colors.add(color(line));
         }
      }

      if (options.size() < 2) {
         return SniperBot.Sort.UNKNOWN;
      }

      for (int i = 0; i < options.size(); i++) {
         boolean unique = true;
         boolean othersAgree = true;
         int other = -2;

         for (int j = 0; j < options.size(); j++) {
            if (j != i) {
               if (colors.get(j).equals(colors.get(i))) {
                  unique = false;
               }

               if (other == -2) {
                  other = colors.get(j);
               } else if (other != colors.get(j)) {
                  othersAgree = false;
               }
            }
         }

         if (unique && othersAgree) {
            return options.get(i);
         }
      }

      int best = -1;
      SniperBot.Sort pick = SniperBot.Sort.UNKNOWN;

      for (int i = 0; i < options.size(); i++) {
         int rgb = colors.get(i);
         int brightness = (rgb >> 16 & 0xFF) + (rgb >> 8 & 0xFF) + (rgb & 0xFF);
         if (brightness > best) {
            best = brightness;
            pick = options.get(i);
         } else if (brightness == best) {
            pick = SniperBot.Sort.UNKNOWN;
         }
      }

      return pick;
   }

   private static int color(class_2561 line) {
      int[] value = new int[]{-1};
      line.method_27658((style, text) -> {
         if (value[0] < 0 && !text.isBlank() && !text.trim().equals("•") && !text.trim().equals("▪")) {
            class_5251 color = style.method_10973();
            value[0] = color == null ? 11141290 : color.method_27716();
            return Optional.empty();
         } else {
            return Optional.empty();
         }
      }, class_2583.field_24360);
      return value[0];
   }

   private SniperBot.Hit pageTick() {
      class_746 player = Gui.mc().field_1724;
      if (player != null && auctionOpenNow()) {
         long now = System.currentTimeMillis();
         long key = (long)player.field_7512.field_7763 << 32 | player.field_7512.method_37421() & 4294967295L;
         if (key != this.pageKey) {
            this.pageKey = key;
            this.pageChangedAt = now;
            this.pageChangedTick = player.field_6012;
            if (this.refreshPending) {
               this.refreshPending = false;
               this.learnAnswer(now - this.refreshSentAt);
            }

            return null;
         } else {
            if (this.refreshPending) {
               if (now - this.refreshSentAt < this.answerTimeout(player)) {
                  return null;
               }

               this.refreshPending = false;
            }

            if (now - this.pageChangedAt < this.settleMs || player.field_6012 - this.pageChangedTick < 2) {
               return null;
            }

            if (!player.field_7512.method_34255().method_7960()) {
               return null;
            }

            String title = ServerText.normalize(Gui.mc().field_1755.method_25440());
            if (title.contains("(page") && !title.contains("(page 1)")) {
               return null;
            }

            this.skip.values().removeIf(until -> until < now);
            List<class_1799> stacks = Gui.containerNow();

            for (int i = 0; i < 45 && i < stacks.size(); i++) {
               class_1799 stack = stacks.get(i);
               if (!stack.method_7960() && this.target.matches(stack)) {
                  double total = ServerText.askingPrice(stack);
                  if (!(total <= 0.0)) {
                     long unit = Math.round(total / Math.max(1, stack.method_7947()));
                     if (unit <= this.target.maxPrice && (this.budget <= 0L || this.spent + this.wrongSpent + Math.round(total) <= this.budget)) {
                        String fingerprint = fingerprint(stack, total);
                        if (!this.skip.containsKey(fingerprint)) {
                           this.confirmPressed = false;
                           Gui.mc().field_1761.method_2906(player.field_7512.field_7763, i, 0, class_1713.field_7790, player);
                           return new SniperBot.Hit(
                              i,
                              stack,
                              Math.round(total),
                              unit,
                              this.target.timed() ? SnipeTarget.minutesLeft(stack) : -1,
                              fingerprint,
                              stack.method_7964().getString()
                           );
                        }
                     }
                  }
               }
            }

            if (now >= this.nextRefresh) {
               int slot = refreshSlot(stacks);
               if (slot >= 0) {
                  Gui.mc().field_1761.method_2906(player.field_7512.field_7763, slot, 0, class_1713.field_7790, player);
                  this.refreshPending = true;
                  this.refreshSentAt = now;
               }

               this.nextRefresh = now + this.refreshMs;
            }

            return null;
         }
      } else {
         return CLOSED;
      }
   }

   private void learnAnswer(long sample) {
      if (sample > 0L && sample <= 3000L) {
         this.answerMs = this.answerMs <= 0L ? sample : (this.answerMs * 3L + sample) / 4L;
      }
   }

   private long answerTimeout(class_746 player) {
      long base = this.answerMs > 0L ? this.answerMs * 3L : 1000L;
      class_640 info = Gui.mc().method_1562() == null ? null : Gui.mc().method_1562().method_2871(player.method_5667());
      if (info != null) {
         base = Math.max(base, info.method_2959() * 2L);
      }

      return Math.clamp(base + 300L, 700L, 3000L);
   }

   private static String fingerprint(class_1799 stack, double total) {
      StringBuilder text = new StringBuilder();
      text.append(class_7923.field_41178.method_10221(stack.method_7909()))
         .append('|')
         .append(stack.method_7947())
         .append('|')
         .append(ServerText.key(stack.method_7964().getString()))
         .append('|')
         .append(Math.round(total))
         .append('|')
         .append(stack.method_7919())
         .append('|')
         .append(class_1890.method_57532(stack));

      for (String line : ServerText.lore(stack)) {
         String clean = ServerText.normalize(line);
         if (!TIME.matcher(clean).find()) {
            text.append('|').append(clean);
         }
      }

      return text.toString();
   }

   private boolean handleBuy(SniperBot.Hit hit, long mark) {
      Object[] pressedDialog = new Object[]{null};
      int[] pressedChest = new int[]{-1};
      long[] seen = new long[]{0L, Long.MIN_VALUE};
      boolean[] verified = new boolean[]{false};
      SniperBot.Problem[] problem = new SniperBot.Problem[]{null};
      long end = System.currentTimeMillis() + 4000L;

      while (System.currentTimeMillis() < end) {
         SniperBot.Purchase done = this.purchase;
         if (done != null && done.at() >= mark && done.at() > this.handledAt) {
            return this.bought(hit, done);
         }

         if (this.poorAt >= mark) {
            this.say(
               ModuleChat.body()
                  .danger("Not enough money")
                  .text(" for ")
                  .name(hit.name())
                  .text(" at $")
                  .value(ServerText.shortMoney(hit.total()))
                  .text(", stopping.")
            );
            this.module.stopFromBot(this);
            throw new MacroWorker.Cancelled();
         }

         if (this.goneAt >= mark || this.ownAt >= mark) {
            this.skip.put(hit.fingerprint(), System.currentTimeMillis() + 60000L);
            return false;
         }

         SniperBot.Step step = this.worker.call(() -> this.confirmStep(hit, pressedDialog, pressedChest, seen, verified, problem));
         if (step == SniperBot.Step.WRONG) {
            if (this.strikes.merge(hit.fingerprint(), 1, Integer::sum) >= 2) {
               this.skip.put(hit.fingerprint(), System.currentTimeMillis() + 600000L);
            }

            this.say(ModuleChat.body().warn("Did not buy").text(" ").name(hit.name()).text(": " + problem[0].reason() + "."));
            if (problem[0].fatal()) {
               this.unreadable = 0;
            } else if (++this.unreadable >= 4) {
               this.say(
                  ModuleChat.body()
                     .danger("The purchase menu could not be checked")
                     .text(" " + this.unreadable + " times in a row, so AH Sniper stopped without buying. Send a screenshot of the menu to the Anubis team.")
               );
               this.module.stopFromBot(this);
               throw new MacroWorker.Cancelled();
            }

            return !this.worker.waitFor(SniperBot::auctionOpenNow, 1000L);
         }

         if (step == SniperBot.Step.PRESSED) {
            end = Math.max(end, System.currentTimeMillis() + 4000L);
         }

         this.worker.sleep(2L);
      }

      this.skip.put(hit.fingerprint(), System.currentTimeMillis() + 60000L);
      return !this.worker.call(SniperBot::auctionOpenNow);
   }

   private SniperBot.Step confirmStep(
      SniperBot.Hit hit, Object[] pressedDialog, int[] pressedChest, long[] seen, boolean[] verified, SniperBot.Problem[] problem
   ) {
      class_746 player = Gui.mc().field_1724;
      if (player == null) {
         return SniperBot.Step.WAIT;
      }

      Dialogs.View view = Dialogs.current(Gui.mc());
      if (view != null) {
         if (view.dialog() == pressedDialog[0]) {
            return SniperBot.Step.WAIT;
         }

         if (!view.titled("buy") && !view.titled("sure") && !view.titled("purchase") && !view.titled("confirm")) {
            return SniperBot.Step.WAIT;
         }

         Dialogs.Button yes = view.labelled("yes", "confirm", "buy");
         if (yes == null) {
            return SniperBot.Step.WAIT;
         }

         SniperBot.Problem wrong = this.dialogProblem(view, hit, verified[0]);
         if (wrong != null) {
            LOGGER.info(
               "[Anubis] AH Sniper cancelled a purchase dialog ({}): {} body={} items={}",
               new Object[]{wrong.reason(), view.describe(), view.body(), describe(view.items())}
            );
            Dialogs.close(Gui.mc());
            problem[0] = wrong;
            return SniperBot.Step.WRONG;
         }

         if (!Dialogs.press(Gui.mc(), view, yes, Map.of())) {
            return SniperBot.Step.WAIT;
         }

         pressedDialog[0] = view.dialog();
         verified[0] = true;
         this.confirmPressed = true;
         return SniperBot.Step.PRESSED;
      } else if (Gui.containerOpenNow() && !auctionOpenNow()) {
         int sync = player.field_7512.field_7763;
         if (sync == pressedChest[0]) {
            return SniperBot.Step.WAIT;
         }

         List<class_1799> stacks = Gui.containerNow();
         int confirm = confirmSlot(stacks, hit);
         if (confirm < 0) {
            return SniperBot.Step.WAIT;
         }

         long now = System.currentTimeMillis();
         if (seen[1] != sync) {
            seen[0] = now;
            seen[1] = sync;
         }

         SniperBot.Problem wrong = this.chestProblem(stacks, confirm, hit, verified[0]);
         if (wrong != null) {
            if (!wrong.fatal() && now - seen[0] < 400L) {
               return SniperBot.Step.WAIT;
            }

            LOGGER.info(
               "[Anubis] AH Sniper cancelled a purchase menu ({}): '{}' {}",
               new Object[]{wrong.reason(), ServerText.normalize(Gui.mc().field_1755.method_25440()), describe(stacks)}
            );
            player.method_7346();
            problem[0] = wrong;
            return SniperBot.Step.WRONG;
         } else {
            Gui.mc().field_1761.method_2906(sync, confirm, 0, class_1713.field_7790, player);
            pressedChest[0] = sync;
            verified[0] = true;
            this.confirmPressed = true;
            return SniperBot.Step.PRESSED;
         }
      } else {
         return SniperBot.Step.WAIT;
      }
   }

   private static int confirmSlot(List<class_1799> stacks, SniperBot.Hit hit) {
      int fallback = -1;

      for (int i = 0; i < stacks.size(); i++) {
         class_1799 stack = stacks.get(i);
         if (!stack.method_7960()
            && !stack.method_31574(hit.stack().method_7909())
            && class_7923.field_41178.method_10221(stack.method_7909()).method_12832().contains("lime")) {
            String name = Gui.nameOf(stack);
            if (name.contains("confirm") || name.contains("buy") || name.contains("purchase") || name.contains("yes")) {
               return i;
            }

            if (fallback < 0) {
               fallback = i;
            }
         }
      }

      return fallback;
   }

   private SniperBot.Problem chestProblem(List<class_1799> stacks, int confirm, SniperBot.Hit hit, boolean verified) {
      boolean shown = false;
      double price = -1.0;

      for (int i = 0; i < stacks.size(); i++) {
         class_1799 stack = stacks.get(i);
         if (!stack.method_7960()) {
            double asked = ServerText.askingPrice(textOf(stack));
            if (asked > price) {
               price = asked;
            }

            if (i != confirm) {
               if (this.sameListing(stack, hit)) {
                  shown = true;
               } else if (!decoration(stack) && asked > 0.0) {
                  return new SniperBot.Problem("the menu was for " + stack.method_7964().getString(), true);
               }
            }
         }
      }

      return this.verdict(shown, price, hit, verified);
   }

   private SniperBot.Problem dialogProblem(Dialogs.View view, SniperBot.Hit hit, boolean verified) {
      List<String> text = new ArrayList<>();
      text.add(view.title());
      text.addAll(view.body());

      for (Dialogs.Button button : view.buttons()) {
         text.add(button.label());
      }

      double price = ServerText.askingPrice(text);
      boolean shown = false;

      for (class_1799 item : view.items()) {
         if (!item.method_7960()) {
            double asked = ServerText.askingPrice(textOf(item));
            if (asked > price) {
               price = asked;
            }

            if (this.sameListing(item, hit)) {
               shown = true;
            } else if (!decoration(item)) {
               return new SniperBot.Problem("the menu was for " + item.method_7964().getString(), true);
            }
         }
      }

      if (!shown && view.items().isEmpty()) {
         String want = ServerText.key(hit.name());
         shown = !want.isEmpty() && ServerText.key(String.join(" ", text)).contains(want);
      }

      return this.verdict(shown, price, hit, verified);
   }

   private SniperBot.Problem verdict(boolean shown, double price, SniperBot.Hit hit, boolean verified) {
      if (price > this.limit(hit)) {
         return new SniperBot.Problem("the menu asked $" + ServerText.shortMoney(price), true);
      } else if (verified) {
         return null;
      } else if (!shown) {
         return new SniperBot.Problem("the menu did not show this listing", false);
      } else {
         return price <= 0.0 ? new SniperBot.Problem("the menu did not show the price", false) : null;
      }
   }

   private double limit(SniperBot.Hit hit) {
      double max = (double)this.target.maxPrice * Math.max(1, hit.stack().method_7947());
      return Math.min(max, hit.total() * 1.02 + 1.0);
   }

   private boolean sameListing(class_1799 shown, SniperBot.Hit hit) {
      class_1799 want = hit.stack();
      if (shown.method_31574(want.method_7909()) && shown.method_7947() == want.method_7947() && shown.method_7919() == want.method_7919()) {
         String wantName = ServerText.key(want.method_7964().getString());
         if (!ServerText.key(shown.method_7964().getString()).contains(wantName)) {
            return false;
         }

         if (!class_1890.method_57532(shown).equals(class_1890.method_57532(want))) {
            return false;
         }

         if (!this.target.sameKind(shown)) {
            return false;
         }

         if (hit.minutes() < 0) {
            return true;
         }

         int minutes = SnipeTarget.minutesLeft(shown);
         return minutes >= 0 && Math.abs(minutes - hit.minutes()) <= 2;
      } else {
         return false;
      }
   }

   private static boolean decoration(class_1799 stack) {
      String path = class_7923.field_41178.method_10221(stack.method_7909()).method_12832();
      if (!path.contains("glass_pane")
         && !path.endsWith("stained_glass")
         && !path.equals("glass")
         && !path.equals("barrier")
         && !path.equals("structure_void")
         && !path.equals("light")
         && !path.endsWith("_head")
         && !path.endsWith("_skull")) {
         boolean colored = path.startsWith("lime_") || path.startsWith("green_") || path.startsWith("red_");
         if (!colored
            || !path.endsWith("_dye") && !path.endsWith("_wool") && !path.endsWith("_concrete") && !path.endsWith("_terracotta") && !path.endsWith("_carpet")) {
            String name = Gui.nameOf(stack);
            return name.contains("confirm") || name.contains("cancel") || name.contains("go back") || name.contains("close");
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static List<String> textOf(class_1799 stack) {
      List<String> out = new ArrayList<>();
      out.add(stack.method_7964().getString());
      out.addAll(ServerText.lore(stack));
      return out;
   }

   private static String describe(List<class_1799> stacks) {
      StringBuilder text = new StringBuilder();

      for (int i = 0; i < stacks.size() && text.length() < 2000; i++) {
         class_1799 stack = stacks.get(i);
         if (!stack.method_7960()) {
            text.append(" [")
               .append(i)
               .append(' ')
               .append(class_7923.field_41178.method_10221(stack.method_7909()).method_12832())
               .append(" x")
               .append(stack.method_7947())
               .append(" '")
               .append(stack.method_7964().getString())
               .append('\'');

            for (String line : ServerText.lore(stack)) {
               text.append(" / ").append(line);
            }

            text.append(']');
         }
      }

      return text.toString();
   }

   private boolean bought(SniperBot.Hit hit, SniperBot.Purchase done) {
      this.handledAt = done.at();
      int count = Math.max(1, done.count());
      long total = done.total() > 0L ? done.total() : hit.total();
      boolean rightItem = done.name().isEmpty() || namesMatch(done.name(), hit.stack());
      boolean rightPrice = total <= this.target.maxPrice * count;
      this.skip.put(hit.fingerprint(), System.currentTimeMillis() + 15000L);
      if (rightItem && rightPrice) {
         this.bought++;
         this.spent += total;
         this.unreadable = 0;
         this.unsold++;
         this.say(
            ModuleChat.body()
               .good("Sniped")
               .text(" ")
               .name(hit.name())
               .text(" for $")
               .value(ServerText.shortMoney(total))
               .text(" (")
               .value(this.bought)
               .text(this.bought == 1 ? " buy" : " buys")
               .text(", $")
               .value(ServerText.shortMoney(this.spent))
               .text(" spent).")
         );
         if (!this.confirmPressed) {
            if (this.requireConfirm) {
               this.say(
                  ModuleChat.body()
                     .danger("Your AH buy confirm menu is off")
                     .text(
                        ", so that click bought the listing right away. AH Sniper stopped because Only Buy With Confirm is on. Turn the buy confirm menu on in /settings."
                     )
               );
               this.module.stopFromBot(this);
               throw new MacroWorker.Cancelled();
            }

            if (!this.instantWarned) {
               this.instantWarned = true;
               this.say(
                  ModuleChat.body()
                     .warn("Your AH buy confirm menu is off")
                     .text(
                        ", so a click buys right away. AH Sniper only clicks once the page has been still for "
                           + this.settleMs
                           + "ms. Turn the confirm menu on in /settings to have every purchase checked before you pay."
                     )
               );
            }
         }

         if (this.budget > 0L && this.spent + this.wrongSpent >= this.budget) {
            if (this.stock != null) {
               this.worker.sleep(300L);
               this.sellStock();
            }

            this.say(
               ModuleChat.body()
                  .text("Spent $")
                  .value(ServerText.shortMoney(this.spent + this.wrongSpent))
                  .text(" of your $")
                  .value(ServerText.shortMoney(this.budget))
                  .text(" limit, stopping.")
            );
            this.module.stopFromBot(this);
            throw new MacroWorker.Cancelled();
         } else if (this.maxBuys > 0 && this.bought >= this.maxBuys) {
            if (this.stock != null) {
               this.worker.sleep(300L);
               this.sellStock();
            }

            this.say(ModuleChat.body().text("Reached ").value(this.maxBuys).text(this.maxBuys == 1 ? " buy" : " buys").text(", stopping."));
            this.module.stopFromBot(this);
            throw new MacroWorker.Cancelled();
         } else {
            if (this.stock == null) {
               return !this.worker.call(SniperBot::auctionOpenNow);
            }

            this.worker.sleep(300L);
            this.sellStock();
            return true;
         }
      } else {
         this.wrong++;
         this.wrongSpent += total;
         LOGGER.warn(
            "[Anubis] AH Sniper bought {}x '{}' for {} while sniping '{}' under {}", new Object[]{count, done.name(), total, hit.name(), this.target.maxPrice}
         );
         this.say(
            ModuleChat.body()
               .danger("Bought the wrong listing")
               .text(": " + count + "x ")
               .name(done.name().isEmpty() ? hit.name() : done.name())
               .text(" for $")
               .value(ServerText.shortMoney(total))
               .text(". AH Sniper stopped to keep your money safe.")
         );
         this.module.stopFromBot(this);
         throw new MacroWorker.Cancelled();
      }
   }

   private static boolean namesMatch(String said, class_1799 stack) {
      String text = ServerText.key(said);
      if (text.isEmpty()) {
         return true;
      }

      String shown = ServerText.key(stack.method_7964().getString());
      String plain = ServerText.key(new class_1799(stack.method_7909()).method_7964().getString());
      String id = ServerText.key(class_7923.field_41178.method_10221(stack.method_7909()).method_12832());
      return !shown.isEmpty() && (text.contains(shown) || shown.contains(text))
         || !plain.isEmpty() && text.contains(plain)
         || !id.isEmpty() && text.contains(id);
   }

   private void sellStock() {
      int count = Math.min(this.unsold, this.ah.countInventory(this.stock.product));
      if (count > 0) {
         this.unsold = this.unsold - this.ah.listSome(this.stock, Math.min(count, 9), new int[]{0});
      }
   }

   void onChat(String raw) {
      String line = ServerText.normalize(raw);
      if (!line.isEmpty()) {
         long now = System.currentTimeMillis();
         Matcher wait = RATE_LIMIT.matcher(line);
         if (wait.find()) {
            try {
               this.holdUntil = now + (long)Math.ceil(Math.min(30.0, Double.parseDouble(wait.group(1))) * 1000.0) + 100L;
            } catch (NumberFormatException var7) {
            }
         }

         if (line.startsWith("you bought")) {
            this.purchase = parsePurchase(line, now);
         }

         if (line.contains("already bought") || line.contains("no longer available") || line.contains("not available")) {
            this.goneAt = now;
         }

         if (line.contains("enough money")) {
            this.poorAt = now;
         }

         if (line.contains("your own")) {
            this.ownAt = now;
         }

         if (this.ah != null) {
            this.ah.onChat(line, this.stocks);
         }
      }
   }

   private static SniperBot.Purchase parsePurchase(String line, long now) {
      String rest = line.substring("you bought".length()).trim();
      int count = 1;
      Matcher amount = BOUGHT_COUNT.matcher(rest);
      if (amount.find()) {
         count = Math.max(1, Integer.parseInt(amount.group(1)));
         rest = rest.substring(amount.end());
      }

      int forAt = rest.lastIndexOf(" for ");
      String name = (forAt >= 0 ? rest.substring(0, forAt) : rest).trim();
      long total = -1L;
      if (forAt >= 0) {
         List<Double> money = ServerText.moneyAll(rest.substring(forAt + 5));
         if (!money.isEmpty()) {
            total = Math.round(money.get(0));
         }
      }

      return new SniperBot.Purchase(now, count, name, total);
   }

   String summary() {
      return "Sniped "
         + this.bought
         + (this.bought == 1 ? " item" : " items")
         + " for $"
         + ServerText.shortMoney(this.spent)
         + (
            this.wrong > 0
               ? ", plus " + this.wrong + " wrong " + (this.wrong == 1 ? "listing" : "listings") + " for $" + ServerText.shortMoney(this.wrongSpent)
               : ""
         )
         + (this.stock != null ? ". " + AuctionHouse.summary(this.stocks) : ".");
   }

   private static String formatMinutes(int minutes) {
      return minutes % 60 == 0 ? minutes / 60 + "h" : minutes / 60 + "h " + minutes % 60 + "m";
   }

   @Environment(EnvType.CLIENT)
   private record Hit(int slot, class_1799 stack, long total, long unit, int minutes, String fingerprint, String name) {
   }

   @Environment(EnvType.CLIENT)
   private record Problem(String reason, boolean fatal) {
   }

   @Environment(EnvType.CLIENT)
   private record Purchase(long at, int count, String name, long total) {
   }

   @Environment(EnvType.CLIENT)
   enum Sort {
      UNKNOWN,
      LOWEST,
      HIGHEST,
      RECENT;
   }

   @Environment(EnvType.CLIENT)
   private enum Step {
      WAIT,
      PRESSED,
      WRONG;
   }
}
