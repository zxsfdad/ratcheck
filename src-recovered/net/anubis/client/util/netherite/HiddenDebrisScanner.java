package net.anubis.client.util.netherite;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2540;
import net.minecraft.class_6603;

@Environment(EnvType.CLIENT)
public final class HiddenDebrisScanner {
   private static final int BLOCK_VALUES = 4096;
   private static final int BIOME_VALUES = 64;
   private static final int BLOCK_PALETTE_BITS = 8;
   private static final int BIOME_PALETTE_BITS = 3;

   private HiddenDebrisScanner() {
   }

   public static List<Integer> findHiddenSections(class_6603 data, int sectionCount, int minSectionY, int targetStateId) {
      Objects.requireNonNull(data, "data");
      if (targetStateId < 0) {
         throw new IllegalArgumentException("targetStateId must be non-negative");
      }

      class_2540 buf = data.method_38586();

      try {
         List<Integer> sections = scanSections(buf, sectionCount, minSectionY, targetStateId);
         return buf.readableBytes() == 0 ? sections : List.of();
      } catch (RuntimeException malformed) {
         return List.of();
      } finally {
         buf.release();
      }
   }

   static List<Integer> scanSections(class_2540 buf, int sectionCount, int minSectionY, int targetStateId) {
      Objects.requireNonNull(buf, "buf");
      if (sectionCount >= 0 && targetStateId >= 0) {
         List<Integer> hidden = new ArrayList<>();

         for (int i = 0; i < sectionCount; i++) {
            int nonEmptyBlocks = buf.readUnsignedShort();
            HiddenDebrisScanner.PaletteRead blocks = readPalette(buf, 4096, 8, 4, targetStateId);
            readPalette(buf, 64, 3, 1, -1);
            if (nonEmptyBlocks > 0 && blocks.hiddenTarget()) {
               hidden.add(minSectionY + i);
            }
         }

         return List.copyOf(hidden);
      } else {
         throw new IllegalArgumentException("invalid section scan arguments");
      }
   }

   private static HiddenDebrisScanner.PaletteRead readPalette(class_2540 buf, int valueCount, int maxPaletteBits, int minBits, int target) {
      int bits = buf.readUnsignedByte();
      int storageBits;
      int[] palette;
      if (bits == 0) {
         storageBits = 0;
         palette = new int[]{buf.method_10816()};
      } else if (bits <= maxPaletteBits) {
         storageBits = Math.max(minBits, bits);
         int size = buf.method_10816();
         if (size <= 0 || size > 1 << storageBits) {
            throw new IllegalArgumentException("invalid local palette size");
         }

         palette = new int[size];

         for (int i = 0; i < size; i++) {
            palette[i] = buf.method_10816();
         }
      } else {
         storageBits = bits;
         if (storageBits > 30) {
            throw new IllegalArgumentException("direct palette is too wide");
         }

         palette = null;
      }

      int valuesPerLong = storageBits == 0 ? 0 : 64 / storageBits;
      if (storageBits > 0 && valuesPerLong == 0) {
         throw new IllegalArgumentException("invalid palette storage width");
      }

      int longs = storageBits == 0 ? 0 : (valueCount + valuesPerLong - 1) / valuesPerLong;
      long bytes = longs * 8L;
      if (bytes > buf.readableBytes()) {
         throw new IndexOutOfBoundsException("truncated palette data");
      }

      boolean paletteHasTarget = palette != null && contains(palette, target);
      if (!paletteHasTarget) {
         buf.method_52994((int)bytes);
         return new HiddenDebrisScanner.PaletteRead(false, true);
      }

      int used = 0;
      if (storageBits == 0) {
         if (palette[0] == target) {
            used = valueCount;
         }
      } else {
         int start = buf.readerIndex();
         long mask = (1L << storageBits) - 1L;

         for (int i = 0; i < valueCount; i++) {
            int longIndex = i / valuesPerLong;
            int bitIndex = i - longIndex * valuesPerLong;
            int raw = (int)(buf.getLong(start + longIndex * 8) >>> bitIndex * storageBits & mask);
            int value = palette == null ? raw : paletteValue(palette, raw);
            if (value == target) {
               used++;
            }
         }
      }

      buf.method_52994((int)bytes);
      return new HiddenDebrisScanner.PaletteRead(paletteHasTarget, used == 0);
   }

   private static int paletteValue(int[] palette, int index) {
      if (index >= 0 && index < palette.length) {
         return palette[index];
      } else {
         throw new IllegalArgumentException("packed palette index is invalid");
      }
   }

   private static boolean contains(int[] values, int target) {
      for (int value : values) {
         if (value == target) {
            return true;
         }
      }

      return false;
   }

   @Environment(EnvType.CLIENT)
   private record PaletteRead(boolean paletteHasTarget, boolean targetUnused) {
      boolean hiddenTarget() {
         return this.paletteHasTarget && this.targetUnused;
      }
   }
}
