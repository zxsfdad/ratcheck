package net.anubis.client.util.seedscan;

import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2680;

@Environment(EnvType.CLIENT)
public final class VBlock {
   public static final byte AIR = 0;
   public static final byte WATER = 1;
   public static final byte LAVA = 2;
   public static final byte STONE = 3;
   public static final byte GRANITE = 4;
   public static final byte DIORITE = 5;
   public static final byte ANDESITE = 6;
   public static final byte TUFF = 7;
   public static final byte GRAVEL = 8;
   public static final byte DIRT = 9;
   public static final byte CLAY = 10;
   public static final byte ORE = 11;
   public static final byte OTHER = 12;
   public static final byte UNKNOWN = 15;
   public static final int FAMILY_GRANITE = 1;
   public static final int FAMILY_DIORITE = 2;
   public static final int FAMILY_ANDESITE = 4;
   public static final int FAMILY_GRAVEL = 8;
   private static volatile Map<class_2248, Byte> kinds;

   private VBlock() {
   }

   public static boolean isBase(byte kind) {
      return kind >= 3 && kind <= 7;
   }

   public static boolean isOpen(byte kind) {
      return kind == 0 || kind == 1 || kind == 2;
   }

   public static int familyBit(byte kind) {
      return switch (kind) {
         case 4 -> 1;
         case 5 -> 2;
         case 6 -> 4;
         default -> 0;
         case 8 -> 8;
      };
   }

   public static class_2248 familyBlock(byte kind) {
      return switch (kind) {
         case 4 -> class_2246.field_10474;
         case 5 -> class_2246.field_10508;
         case 6 -> class_2246.field_10115;
         default -> throw new IllegalArgumentException("not a blob kind: " + kind);
         case 8 -> class_2246.field_10255;
      };
   }

   public static byte of(class_2680 state) {
      if (state.method_26215()) {
         return 0;
      }

      Byte kind = kinds().get(state.method_26204());
      return kind != null ? kind : 12;
   }

   private static Map<class_2248, Byte> kinds() {
      Map<class_2248, Byte> map = kinds;
      if (map != null) {
         return map;
      }

      map = new IdentityHashMap<>();
      map.put(class_2246.field_10340, (byte)3);
      map.put(class_2246.field_28888, (byte)3);
      map.put(class_2246.field_10474, (byte)4);
      map.put(class_2246.field_10508, (byte)5);
      map.put(class_2246.field_10115, (byte)6);
      map.put(class_2246.field_27165, (byte)7);
      map.put(class_2246.field_10255, (byte)8);
      map.put(class_2246.field_10566, (byte)9);
      map.put(class_2246.field_10460, (byte)10);
      map.put(class_2246.field_10382, (byte)1);
      map.put(class_2246.field_10164, (byte)2);

      for (class_2248 ore : new class_2248[]{
         class_2246.field_10418,
         class_2246.field_29219,
         class_2246.field_10212,
         class_2246.field_29027,
         class_2246.field_27120,
         class_2246.field_29221,
         class_2246.field_10571,
         class_2246.field_29026,
         class_2246.field_10080,
         class_2246.field_29030,
         class_2246.field_10090,
         class_2246.field_29028,
         class_2246.field_10442,
         class_2246.field_29029,
         class_2246.field_10013,
         class_2246.field_29220,
         class_2246.field_33508,
         class_2246.field_33509,
         class_2246.field_10277,
         class_2246.field_29224
      }) {
         map.put(ore, (byte)11);
      }

      kinds = map;
      return map;
   }
}
