package net.anubis.client.module.impl.donutmacros;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1661;
import net.minecraft.class_1703;
import net.minecraft.class_1713;
import net.minecraft.class_1735;
import net.minecraft.class_1799;
import net.minecraft.class_2815;
import net.minecraft.class_310;
import net.minecraft.class_418;
import net.minecraft.class_465;
import net.minecraft.class_746;
import net.minecraft.class_7923;

@Environment(EnvType.CLIENT)
final class Gui {
   private static final int COMMAND_GAP_MS = 200;
   final MacroWorker worker;

   Gui(MacroWorker worker) {
      this.worker = worker;
   }

   static class_310 mc() {
      return class_310.method_1551();
   }

   static class_746 player() {
      class_746 player = mc().field_1724;
      if (player == null) {
         throw new IllegalStateException("Not in a world");
      } else {
         return player;
      }
   }

   String title() {
      return this.worker.call(() -> mc().field_1755 == null ? "" : ServerText.normalize(mc().field_1755.method_25440()));
   }

   static boolean containerOpenNow() {
      class_746 player = mc().field_1724;
      return player != null && player.field_7512 != player.field_7498 && mc().field_1755 instanceof class_465;
   }

   static List<class_1799> containerNow() {
      List<class_1799> out = new ArrayList<>();
      class_746 player = mc().field_1724;
      if (player != null && player.field_7512 != player.field_7498) {
         for (class_1735 slot : player.field_7512.field_7761) {
            if (slot.field_7871 instanceof class_1661) {
               break;
            }

            out.add(slot.method_7677().method_7972());
         }

         return out;
      } else {
         return out;
      }
   }

   List<class_1799> container() {
      return this.worker.call(Gui::containerNow);
   }

   static String fingerprintNow() {
      StringBuilder text = new StringBuilder();
      class_746 player = mc().field_1724;
      if (player != null) {
         text.append(player.field_7512.field_7763).append('#');
      }

      for (class_1799 stack : containerNow()) {
         text.append(class_7923.field_41178.method_10221(stack.method_7909()))
            .append(':')
            .append(stack.method_7947())
            .append(':')
            .append(stack.method_7964().getString())
            .append('|');
      }

      return text.toString();
   }

   String fingerprint() {
      return this.worker.call(Gui::fingerprintNow);
   }

   static class_1799 slotNow(int index) {
      List<class_1799> stacks = containerNow();
      return index >= 0 && index < stacks.size() ? stacks.get(index) : class_1799.field_8037;
   }

   static int findNow(Predicate<class_1799> test, int limit) {
      List<class_1799> stacks = containerNow();

      for (int i = 0; i < stacks.size() && i < limit; i++) {
         if (!stacks.get(i).method_7960() && test.test(stacks.get(i))) {
            return i;
         }
      }

      return -1;
   }

   static String nameOf(class_1799 stack) {
      return ServerText.normalize(stack.method_7964());
   }

   void click(int slot, int button, class_1713 type) {
      this.worker.run(() -> {
         class_746 player = player();
         class_1703 menu = player.field_7512;
         mc().field_1761.method_2906(menu.field_7763, slot, button, type, player);
      });
   }

   void clickInventory(int slot, int button, class_1713 type) {
      this.worker.run(() -> {
         class_746 player = player();
         mc().field_1761.method_2906(player.field_7498.field_7763, slot, button, type, player);
      });
   }

   void closeInventory() {
      this.worker.run(() -> {
         class_746 player = player();
         if (player.field_7512 == player.field_7498) {
            player.field_3944.method_52787(new class_2815(player.field_7498.field_7763));
         }
      });
   }

   void command(String command) {
      this.worker.run(() -> player().field_3944.method_45730(command));
      this.worker.sleep(200L);
   }

   void closeAll() {
      for (int i = 0; i < 4; i++) {
         boolean open = this.worker.call(() -> {
            class_746 player = mc().field_1724;
            if (player == null) {
               return false;
            } else if (player.field_7512 != player.field_7498) {
               player.method_7346();
               return true;
            } else if (mc().field_1755 != null && !(mc().field_1755 instanceof class_418)) {
               mc().field_1755.method_25419();
               return true;
            } else {
               return false;
            }
         });
         if (!open) {
            return;
         }

         this.worker.ticks(2);
      }
   }

   boolean waitChange(String before, long timeoutMs) {
      return this.worker.waitFor(() -> !fingerprintNow().equals(before), timeoutMs);
   }
}
