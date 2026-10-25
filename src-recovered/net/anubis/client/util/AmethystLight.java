package net.anubis.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1923;
import net.minecraft.class_1944;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2680;
import net.minecraft.class_2804;
import net.minecraft.class_2818;
import net.minecraft.class_2826;
import net.minecraft.class_3562;
import net.minecraft.class_4076;
import net.minecraft.class_638;
import net.minecraft.class_2338.class_2339;

@Environment(EnvType.CLIENT)
public final class AmethystLight {
   public static final int SMALL_BUD = 1;
   public static final int MEDIUM_BUD = 2;
   public static final int LARGE_BUD = 4;
   public static final int CLUSTER = 5;
   private static final class_2350[] SIDES = class_2350.values();

   private AmethystLight() {
   }

   public static void scan(class_638 level, class_2818 chunk, AmethystLight.Sink out) {
      class_2826[] sections = chunk.method_12006();
      int minSectionY = chunk.method_32891();
      class_1923 pos = chunk.method_12004();
      class_2339 cursor = new class_2339();

      for (int i = 0; i < sections.length; i++) {
         class_2826 section = sections[i];
         if (section != null && !section.method_38292() && section.method_12265().method_19526(AmethystLight::isLining)) {
            int baseY = (minSectionY + i) * 16;

            for (int y = 0; y < 16; y++) {
               for (int z = 0; z < 16; z++) {
                  for (int x = 0; x < 16; x++) {
                     if (section.method_12254(x, y, z).method_26213() <= 0) {
                        cursor.method_10103(pos.method_8326() + x, baseY + y, pos.method_8328() + z);
                        int light = level.method_8314(class_1944.field_9282, cursor);
                        if (isGrowthLight(light) && touchesLining(level, cursor) && isLocalMaximum(level, cursor, light)) {
                           out.accept(cursor.method_10063(), light);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void scanSources(class_638 level, class_2818 chunk, AmethystLight.Sink out) {
      class_2826[] sections = chunk.method_12006();
      int minSectionY = chunk.method_32891();
      class_1923 pos = chunk.method_12004();
      AmethystLight.LightCache light = new AmethystLight.LightCache(level, pos.field_9181, pos.field_9180, minSectionY, sections.length);
      class_2339 cursor = new class_2339();

      for (int i = 0; i < sections.length; i++) {
         class_2826 section = sections[i];
         if (section != null && !section.method_38292() && !light.quiet(i) && section.method_12265().method_19526(AmethystLight::isLining)) {
            int baseY = (minSectionY + i) * 16;

            for (int y = 0; y < 16; y++) {
               for (int z = 0; z < 16; z++) {
                  for (int x = 0; x < 16; x++) {
                     class_2680 state = section.method_12254(x, y, z);
                     if (state.method_26213() <= 0) {
                        int wx = pos.method_8326() + x;
                        int wy = baseY + y;
                        int wz = pos.method_8328() + z;
                        int value = light.get(wx, wy, wz);
                        int brightest = light.brightestAround(wx, wy, wz);
                        int source;
                        if (value == 0) {
                           if (brightest != 4 && brightest != 3 || !isOpen(state)) {
                              continue;
                           }

                           cursor.method_10103(wx, wy, wz);
                           if (!touchesLining(level, cursor) || !litOpening(level, light, cursor, brightest)) {
                              continue;
                           }

                           source = brightest == 4 ? 5 : 4;
                        } else {
                           if (!isGrowthLight(value) || brightest >= value) {
                              continue;
                           }

                           cursor.method_10103(wx, wy, wz);
                           if (!touchesLining(level, cursor) || value == 4 && besideDarkOpening(level, light, cursor)) {
                              continue;
                           }

                           source = value;
                        }

                        out.accept(cursor.method_10063(), source);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean litOpening(class_638 level, AmethystLight.LightCache light, class_2338 pos, int brightest) {
      class_2339 next = new class_2339();

      for (class_2350 direction : SIDES) {
         next.method_25505(pos, direction);
         if (light.get(next.method_10263(), next.method_10264(), next.method_10260()) == brightest && isOpen(level.method_8320(next))) {
            return true;
         }
      }

      return false;
   }

   private static boolean besideDarkOpening(class_638 level, AmethystLight.LightCache light, class_2338 pos) {
      class_2339 next = new class_2339();

      for (class_2350 direction : SIDES) {
         next.method_25505(pos, direction);
         if (light.get(next.method_10263(), next.method_10264(), next.method_10260()) == 0) {
            class_2680 state = level.method_8320(next);
            if (isOpen(state) && !state.method_27852(class_2246.field_10243)) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean isOpen(class_2680 state) {
      return state.method_26215() || state.method_27852(class_2246.field_27161) || state.method_27852(class_2246.field_10382);
   }

   public static boolean isGrowthLight(int light) {
      return light == 1 || light == 2 || light == 4 || light == 5;
   }

   public static boolean touchesLining(class_638 level, class_2338 pos) {
      for (class_2350 direction : SIDES) {
         if (isLining(level.method_8320(pos.method_10093(direction)))) {
            return true;
         }
      }

      return false;
   }

   public static boolean isLining(class_2680 state) {
      return state.method_27852(class_2246.field_27159) || state.method_27852(class_2246.field_27160);
   }

   private static boolean isLocalMaximum(class_638 level, class_2338 pos, int light) {
      for (class_2350 direction : SIDES) {
         if (level.method_8314(class_1944.field_9282, pos.method_10093(direction)) >= light) {
            return false;
         }
      }

      return true;
   }

   @Environment(EnvType.CLIENT)
   private static final class LightCache {
      private final class_3562 listener;
      private final int chunkX;
      private final int chunkZ;
      private final int minSectionY;
      private final int sectionCount;
      private final class_2804[] layers;
      private final boolean[] fetched;

      private LightCache(class_638 level, int chunkX, int chunkZ, int minSectionY, int sectionCount) {
         this.listener = level.method_22336().method_15562(class_1944.field_9282);
         this.chunkX = chunkX;
         this.chunkZ = chunkZ;
         this.minSectionY = minSectionY - 1;
         this.sectionCount = sectionCount + 2;
         this.layers = new class_2804[9 * this.sectionCount];
         this.fetched = new boolean[this.layers.length];
      }

      private class_2804 layer(int dx, int dz, int section) {
         int slot = ((dx + 1) * 3 + dz + 1) * this.sectionCount + section;
         if (!this.fetched[slot]) {
            this.fetched[slot] = true;
            class_2804 data = this.listener.method_15544(class_4076.method_18676(this.chunkX + dx, this.minSectionY + section, this.chunkZ + dz));
            this.layers[slot] = data != null && !data.method_12146() ? data : null;
         }

         return this.layers[slot];
      }

      int get(int x, int y, int z) {
         int dx = (x >> 4) - this.chunkX;
         int dz = (z >> 4) - this.chunkZ;
         int section = (y >> 4) - this.minSectionY;
         if (dx >= -1 && dx <= 1 && dz >= -1 && dz <= 1 && section >= 0 && section < this.sectionCount) {
            class_2804 data = this.layer(dx, dz, section);
            return data == null ? 0 : data.method_12139(x & 15, y & 15, z & 15);
         } else {
            return 0;
         }
      }

      int brightestAround(int x, int y, int z) {
         int best = this.get(x + 1, y, z);
         best = Math.max(best, this.get(x - 1, y, z));
         best = Math.max(best, this.get(x, y + 1, z));
         best = Math.max(best, this.get(x, y - 1, z));
         best = Math.max(best, this.get(x, y, z + 1));
         return Math.max(best, this.get(x, y, z - 1));
      }

      boolean quiet(int sectionIndex) {
         int section = sectionIndex + 1;
         return this.layer(0, 0, section) == null
            && this.layer(0, 0, section - 1) == null
            && this.layer(0, 0, section + 1) == null
            && this.layer(-1, 0, section) == null
            && this.layer(1, 0, section) == null
            && this.layer(0, -1, section) == null
            && this.layer(0, 1, section) == null;
      }
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface Sink {
      void accept(long var1, int var3);
   }
}
