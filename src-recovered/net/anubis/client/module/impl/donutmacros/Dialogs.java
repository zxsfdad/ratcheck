package net.anubis.client.module.impl.donutmacros;

import com.example.client.mixin.AccessorOrderDialogScreen;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11414;
import net.minecraft.class_11419;
import net.minecraft.class_11424;
import net.minecraft.class_11426;
import net.minecraft.class_11430;
import net.minecraft.class_11432;
import net.minecraft.class_11434;
import net.minecraft.class_11435;
import net.minecraft.class_11437;
import net.minecraft.class_11444;
import net.minecraft.class_11473;
import net.minecraft.class_11519;
import net.minecraft.class_11722;
import net.minecraft.class_1799;
import net.minecraft.class_2481;
import net.minecraft.class_2520;
import net.minecraft.class_2558;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_310;
import net.minecraft.class_5251;
import net.minecraft.class_11521.class_11522;

@Environment(EnvType.CLIENT)
final class Dialogs {
   private Dialogs() {
   }

   static Dialogs.View current(class_310 mc) {
      if (!(mc.field_1755 instanceof class_11473<?> screen)) {
         return null;
      } else {
         class_11419 dialog = ((AccessorOrderDialogScreen)screen).anubis$dialog();
         if (dialog == null) {
            return null;
         }

         List<class_11519> raw = new ArrayList<>();
         if (dialog instanceof class_11426 multi) {
            raw.addAll(multi.comp_4317());
         }

         if (dialog instanceof class_11430 simple) {
            raw.addAll(simple.method_71698());
         }

         if (dialog instanceof class_11414 list) {
            list.comp_4395().ifPresent(raw::add);
         }

         List<Dialogs.Button> buttons = new ArrayList<>(raw.size());

         for (class_11519 button : raw) {
            class_2561 label = button.comp_4390().comp_4298();
            String text = visibleText(label);
            buttons.add(new Dialogs.Button(button, ServerText.normalize(text), ServerText.key(text), label));
         }

         List<String> body = new ArrayList<>();
         List<class_1799> items = new ArrayList<>();

         for (class_11432 part : dialog.comp_4305().comp_4304()) {
            if (part instanceof class_11435 message) {
               body.add(ServerText.normalize(visibleText(message.comp_4330())));
            }

            if (part instanceof class_11434 item) {
               items.add(item.comp_4324());
               item.comp_4325().ifPresent(message -> body.add(ServerText.normalize(visibleText(message.comp_4330()))));
            }
         }

         List<String> textInputs = new ArrayList<>();

         for (class_11424 input : dialog.comp_4305().comp_4394()) {
            if (input.comp_4313() instanceof class_11444) {
               textInputs.add(input.comp_4312());
            }
         }

         return new Dialogs.View(dialog, ServerText.normalize(visibleText(dialog.comp_4305().comp_4301())), buttons, body, textInputs, items);
      }
   }

   static String visibleText(class_2561 component) {
      StringBuilder out = new StringBuilder();
      appendVisible(component, out);
      return out.toString();
   }

   private static void appendVisible(class_2561 component, StringBuilder out) {
      if (!(component.method_10851() instanceof class_11722)) {
         component.method_10851().method_27659(text -> {
            out.append(text);
            return Optional.empty();
         });
      }

      for (class_2561 sibling : component.method_10855()) {
         appendVisible(sibling, out);
      }
   }

   static boolean press(class_310 mc, Dialogs.View view, Dialogs.Button button, Map<String, String> text) {
      if (mc.field_1755 instanceof class_11473<?> screen) {
         if (((AccessorOrderDialogScreen)screen).anubis$dialog() != view.dialog()) {
            return false;
         }

         Map<String, class_11522> values = new HashMap<>();

         for (class_11424 input : view.dialog().comp_4305().comp_4394()) {
            if (input.comp_4313() instanceof class_11444 textInput) {
               values.put(input.comp_4312(), class_11522.method_72070(text.getOrDefault(input.comp_4312(), textInput.comp_4354())));
            } else if (input.comp_4313() instanceof class_11437 bool) {
               values.put(input.comp_4312(), booleanValue(bool));
            }
         }

         Optional<class_2558> event = button.raw().comp_4391().flatMap(action -> action.method_72068(values));
         screen.method_72141(event);
         return true;
      } else {
         return false;
      }
   }

   static void close(class_310 mc) {
      if (mc.field_1755 instanceof class_11473<?> screen) {
         screen.method_25419();
      }
   }

   private static class_11522 booleanValue(class_11437 input) {
      final boolean value = input.comp_4333();
      return new class_11522() {
         public String method_72069() {
            return value ? input.comp_4334() : input.comp_4335();
         }

         public class_2520 method_72073() {
            return class_2481.method_23234(value);
         }
      };
   }

   private static boolean isGreen(int rgb) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      return g >= 192 && r < 144 && b < 144;
   }

   @Environment(EnvType.CLIENT)
   record Button(class_11519 raw, String label, String key, class_2561 component) {
      boolean green() {
         boolean[] found = new boolean[]{false};
         this.component.method_27658((style, text) -> {
            class_5251 color = style.method_10973();
            if (!text.isBlank() && color != null && Dialogs.isGreen(color.method_27716())) {
               found[0] = true;
            }

            return Optional.empty();
         }, class_2583.field_24360);
         return found[0];
      }
   }

   @Environment(EnvType.CLIENT)
   record View(class_11419 dialog, String title, List<Dialogs.Button> buttons, List<String> body, List<String> textInputs, List<class_1799> items) {
      Dialogs.Button find(Predicate<Dialogs.Button> test) {
         for (Dialogs.Button button : this.buttons) {
            if (test.test(button)) {
               return button;
            }
         }

         return null;
      }

      Dialogs.Button labelled(String... words) {
         for (String word : words) {
            String wanted = ServerText.key(word);
            Dialogs.Button exact = this.find(button -> button.key().equals(wanted));
            if (exact != null) {
               return exact;
            }
         }

         for (String word : words) {
            String wanted = ServerText.key(word);
            Dialogs.Button partial = this.find(button -> button.key().contains(wanted));
            if (partial != null) {
               return partial;
            }
         }

         return null;
      }

      boolean titled(String text) {
         return this.title.contains(text);
      }

      String describe() {
         List<String> labels = new ArrayList<>();

         for (Dialogs.Button button : this.buttons) {
            if (!button.label().isBlank()) {
               labels.add(button.label());
            }

            if (labels.size() >= 12) {
               break;
            }
         }

         return "'" + this.title + "' buttons: " + (labels.isEmpty() ? "(none)" : String.join(", ", labels));
      }
   }
}
