package net.anubis.client.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import net.anubis.client.module.impl.client.PerformanceModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2487;
import net.minecraft.class_2591;
import net.minecraft.class_2596;
import net.minecraft.class_2622;
import net.minecraft.class_2672;
import net.minecraft.class_2678;
import net.minecraft.class_2724;
import net.minecraft.class_2960;
import net.minecraft.class_6603;
import net.minecraft.class_6880;
import net.minecraft.class_7923;

@Environment(EnvType.CLIENT)
public final class BlockEntityLeakTracker {
   private static final Map<class_2338, BlockEntityLeakTracker.Entry> FOUND = new ConcurrentHashMap<>();
   private static final ConcurrentLinkedQueue<class_2338> ADDED = new ConcurrentLinkedQueue<>();
   private static final AtomicInteger GENERATION = new AtomicInteger();
   private static volatile boolean recordAdditions;

   private BlockEntityLeakTracker() {
   }

   public static Map<class_2338, BlockEntityLeakTracker.Entry> view() {
      return FOUND;
   }

   public static boolean contains(class_2338 pos) {
      return FOUND.containsKey(pos);
   }

   public static void clear() {
      FOUND.clear();
      ADDED.clear();
      GENERATION.incrementAndGet();
   }

   public static int generation() {
      return GENERATION.get();
   }

   public static void recordAdditions(boolean on) {
      recordAdditions = on;
      if (!on) {
         ADDED.clear();
      }
   }

   public static class_2338 pollAdded() {
      return ADDED.poll();
   }

   public static void ingest(class_2596<?> packet) {
      if (packet instanceof class_2672 chunkPacket) {
         class_6603 data = chunkPacket.method_38598();

         try {
            data.method_38587(chunkPacket.method_11523(), chunkPacket.method_11524()).accept((pos, type, tag) -> {
               if (type != null) {
                  put(pos, type, tag);
               }
            });
         } catch (Throwable var5) {
         }
      } else if (packet instanceof class_2622 bePacket) {
         put(bePacket.method_11293(), bePacket.method_11291(), bePacket.method_11290());
      } else if (packet instanceof class_2678 || packet instanceof class_2724) {
         clear();
      }
   }

   private static void put(class_2338 pos, class_2591<?> type, class_2487 tag) {
      if (!PerformanceModule.optimizationsEnabled() || !FOUND.containsKey(pos)) {
         class_2338 key = pos.method_10062();
         class_2487 data = tag != null && !tag.method_33133() ? tag : null;
         BlockEntityLeakTracker.Entry old = FOUND.putIfAbsent(key, new BlockEntityLeakTracker.Entry(blockFromType(type), data));
         if (old == null) {
            if (recordAdditions) {
               ADDED.add(key);
            }
         } else if (data != null && !data.equals(old.tag())) {
            FOUND.replace(key, old, new BlockEntityLeakTracker.Entry(old.block(), data));
         }
      }
   }

   public static class_2248 blockFromType(class_2591<?> type) {
      class_2960 id = class_7923.field_41181.method_10221(type);
      if (id == null) {
         return class_2246.field_10124;
      }

      String path = id.method_12832();

      class_2248 mapped = switch (path) {
         case "mob_spawner" -> class_2246.field_10260;
         case "piston" -> class_2246.field_10008;
         case "sign" -> class_2246.field_10121;
         case "hanging_sign" -> class_2246.field_40262;
         case "skull" -> class_2246.field_10481;
         case "banner" -> class_2246.field_10154;
         case "bed" -> class_2246.field_10069;
         case "brushable_block" -> class_2246.field_42728;
         default -> null;
      };
      return mapped != null ? mapped : class_7923.field_41175.method_10223(id).<class_2248>map(class_6880::comp_349).orElse(class_2246.field_10124);
   }

   @Environment(EnvType.CLIENT)
   public record Entry(class_2248 block, class_2487 tag) {
   }
}
