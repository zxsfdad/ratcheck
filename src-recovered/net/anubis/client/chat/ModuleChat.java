package net.anubis.client.chat;

import net.anubis.client.config.ThemeStore;
import net.anubis.client.gui.ThemeTint;
import net.anubis.client.module.ClientModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_5250;

@Environment(EnvType.CLIENT)
public final class ModuleChat {
   private static final String HEADER = "Anubis Client";
   private static final int DARK_GREY = 5592405;
   private static final int GREY = 11184810;
   private static final int WHITE = 16777215;
   private static final int DANGER = 16733525;
   private static final int GOOD = 5635925;
   private static final int WARN = 16755200;
   private static final int VALUE = 5636095;

   private ModuleChat() {
   }

   public static ModuleChat.Body body() {
      return new ModuleChat.Body();
   }

   public static void send(ClientModule module, class_2561 body) {
      onClientThread(() -> {
         if (module.notificationsOn()) {
            addLine(module, body);
         }
      });
   }

   public static void send(ClientModule module, ModuleChat.Body body) {
      send(module, body.build());
   }

   public static void send(ClientModule module, String text) {
      send(module, class_2561.method_43470(text));
   }

   public static void actionBar(ClientModule module, ModuleChat.Body body) {
      class_2561 built = body.build();
      onClientThread(() -> {
         class_310 mc = class_310.method_1551();
         if (module.notificationsOn() && mc.field_1705 != null) {
            mc.field_1705.method_1758(line(module, built), false);
         }
      });
   }

   public static void sendAlways(ClientModule module, ModuleChat.Body body) {
      class_2561 built = body.build();
      onClientThread(() -> addLine(module, built));
   }

   public static void actionBarAlways(ClientModule module, ModuleChat.Body body) {
      class_2561 built = body.build();
      onClientThread(() -> {
         class_310 mc = class_310.method_1551();
         if (mc.field_1705 != null) {
            mc.field_1705.method_1758(line(module, built), false);
         }
      });
   }

   private static void addLine(ClientModule module, class_2561 body) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1705 != null) {
         mc.field_1705.method_1743().method_1812(line(module, body));
      }
   }

   private static class_5250 line(ClientModule module, class_2561 body) {
      return class_2561.method_43473()
         .method_10852(class_2561.method_43470("[").method_54663(5592405))
         .method_10852(class_2561.method_43470("Anubis Client").method_54663(ThemeTint.accent(ThemeStore.get().accent.argb()) & 16777215))
         .method_10852(class_2561.method_43470("] ").method_54663(5592405))
         .method_10852(class_2561.method_43470(module.name()).method_54663(16777215))
         .method_10852(class_2561.method_43470(" : ").method_54663(5592405))
         .method_10852(class_2561.method_43473().method_54663(11184810).method_10852(body));
   }

   private static void onClientThread(Runnable task) {
      class_310 mc = class_310.method_1551();
      if (mc.method_18854()) {
         task.run();
      } else {
         mc.execute(task);
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class Body {
      private final class_5250 root = class_2561.method_43473().method_54663(11184810);

      private Body() {
      }

      public ModuleChat.Body text(String text) {
         this.root.method_10852(class_2561.method_43470(text));
         return this;
      }

      public ModuleChat.Body name(String name) {
         return this.part(name, 16777215);
      }

      public ModuleChat.Body danger(String text) {
         return this.part(text, 16733525);
      }

      public ModuleChat.Body good(String text) {
         return this.part(text, 5635925);
      }

      public ModuleChat.Body warn(String text) {
         return this.part(text, 16755200);
      }

      public ModuleChat.Body value(Object value) {
         return this.part(String.valueOf(value), 5636095);
      }

      public ModuleChat.Body coords(int x, int y, int z) {
         return this.part(x + ", " + y + ", " + z, 5636095);
      }

      public ModuleChat.Body append(class_2561 component) {
         this.root.method_10852(component);
         return this;
      }

      public class_2561 build() {
         return this.root.method_27661();
      }

      private ModuleChat.Body part(String text, int rgb) {
         this.root.method_10852(class_2561.method_43470(text).method_54663(rgb));
         return this;
      }
   }
}
