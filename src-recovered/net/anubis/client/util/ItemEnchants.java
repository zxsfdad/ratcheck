package net.anubis.client.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1887;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_5455;
import net.minecraft.class_6880;
import net.minecraft.class_7923;
import net.minecraft.class_7924;
import net.minecraft.class_9334;

@Environment(EnvType.CLIENT)
public final class ItemEnchants {
   private static final String[] ROMAN = new String[]{"i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x"};
   private static class_5455 cachedFor;
   private static List<class_6880<class_1887>> cached = List.of();

   private ItemEnchants() {
   }

   public static class_1792 resolve(String raw) {
      String text = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
      if (text.isEmpty()) {
         return null;
      }

      if (text.indexOf(58) < 0) {
         text = "minecraft:" + text;
      }

      class_2960 id = class_2960.method_12829(text);
      if (id != null && class_7923.field_41178.method_10250(id)) {
         class_1792 item = (class_1792)class_7923.field_41178.method_63535(id);
         return item == class_1802.field_8162 ? null : item;
      } else {
         return null;
      }
   }

   public static String stored(class_1792 item) {
      class_2960 key = class_7923.field_41178.method_10221(item);
      return "minecraft".equals(key.method_12836()) ? key.method_12832() : key.toString();
   }

   public static boolean supports(class_1792 item) {
      if (item == null) {
         return false;
      }

      if (item == class_1802.field_8598) {
         return true;
      }

      List<class_6880<class_1887>> all = all();
      class_1799 stack = item.method_7854();
      if (all.isEmpty()) {
         return stack.method_57826(class_9334.field_53695);
      }

      for (class_6880<class_1887> holder : all) {
         if (((class_1887)holder.comp_349()).method_60046(stack)) {
            return true;
         }
      }

      return false;
   }

   public static List<class_6880<class_1887>> applicable(class_1792 item) {
      List<class_6880<class_1887>> out = new ArrayList<>();
      if (item == null) {
         return out;
      }

      class_1799 stack = item.method_7854();
      boolean book = item == class_1802.field_8598;

      for (class_6880<class_1887> holder : all()) {
         if (book || ((class_1887)holder.comp_349()).method_60046(stack)) {
            out.add(holder);
         }
      }

      out.sort(Comparator.comparing(holderx -> ((class_1887)holderx.comp_349()).comp_2686().getString(), String.CASE_INSENSITIVE_ORDER));
      return out;
   }

   public static List<class_6880<class_1887>> all() {
      class_310 mc = class_310.method_1551();
      if (mc.field_1687 == null) {
         return List.of();
      }

      class_5455 access = mc.field_1687.method_30349();
      if (access != cachedFor) {
         List<class_6880<class_1887>> found = new ArrayList<>();
         access.method_30530(class_7924.field_41265).method_42017().forEach(found::add);
         cached = List.copyOf(found);
         cachedFor = access;
      }

      return cached;
   }

   public static String path(class_6880<class_1887> holder) {
      return holder.method_40230().map(key -> key.method_29177().method_12832()).orElse("");
   }

   public static String key(String raw) {
      String text = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
      StringBuilder out = new StringBuilder(text.length());

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
            out.append(c);
         }
      }

      return out.toString();
   }

   public static Map<String, Integer> parse(String raw) {
      Map<String, Integer> out = new LinkedHashMap<>();
      if (raw == null) {
         return out;
      }

      for (String part : raw.split("[,;]")) {
         String text = part.trim();
         if (!text.isEmpty()) {
            int level = 1;
            int space = text.lastIndexOf(32);
            if (space > 0) {
               int parsed = level(text.substring(space + 1).trim());
               if (parsed > 0) {
                  level = parsed;
                  text = text.substring(0, space).trim();
               }
            }

            String key = key(text);
            if (!key.isEmpty()) {
               out.put(key, level);
            }
         }
      }

      return out;
   }

   private static int level(String raw) {
      try {
         int value = Integer.parseInt(raw);
         return value >= 1 && value <= 10 ? value : 0;
      } catch (NumberFormatException e) {
         String text = raw.toLowerCase(Locale.ROOT);

         for (int i = 0; i < ROMAN.length; i++) {
            if (ROMAN[i].equals(text)) {
               return i + 1;
            }
         }

         return 0;
      }
   }

   public static String roman(int level) {
      return level >= 1 && level <= ROMAN.length ? ROMAN[level - 1].toUpperCase(Locale.ROOT) : Integer.toString(level);
   }
}
