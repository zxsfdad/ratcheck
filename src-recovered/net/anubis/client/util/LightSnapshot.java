package net.anubis.client.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1923;
import net.minecraft.class_1944;
import net.minecraft.class_2338;
import net.minecraft.class_2804;
import net.minecraft.class_3562;
import net.minecraft.class_3568;
import net.minecraft.class_4076;
import net.minecraft.class_638;
import net.minecraft.class_2338.class_2339;

@Environment(EnvType.CLIENT)
public final class LightSnapshot {
   private static final int MAX_HITS = 250000;
   private final ArrayDeque<class_1923> chunks;
   private final Map<class_2338, Integer> hits = new HashMap<>();
   private final int minLight;

   public LightSnapshot(class_1923 center, int radius, int minLight) {
      this.minLight = minLight;
      List<class_1923> ordered = new ArrayList<>();

      for (int x = -radius; x <= radius; x++) {
         for (int z = -radius; z <= radius; z++) {
            ordered.add(new class_1923(center.field_9181 + x, center.field_9180 + z));
         }
      }

      ordered.sort(
         Comparator.comparingInt(
            p -> (p.field_9181 - center.field_9181) * (p.field_9181 - center.field_9181)
               + (p.field_9180 - center.field_9180) * (p.field_9180 - center.field_9180)
         )
      );
      this.chunks = new ArrayDeque<>(ordered);
   }

   public Map<class_2338, Integer> hits() {
      return this.hits;
   }

   public boolean collect(class_638 level) {
      long start = System.nanoTime();
      int processed = 0;

      while (!this.chunks.isEmpty() && processed++ < 16 && System.nanoTime() - start < 2000000L) {
         class_1923 pos = this.chunks.removeFirst();
         if (level.method_2935().method_12123(pos.field_9181, pos.field_9180)) {
            class_3568 engine = level.method_2935().method_12130();
            class_3562 blockLight = engine.method_15562(class_1944.field_9282);
            class_3562 skyLight = engine.method_15562(class_1944.field_9284);
            class_2339 cursor = new class_2339();

            for (int sy = level.method_31607() >> 4; sy < 0; sy++) {
               class_2804 block = blockLight.method_15544(class_4076.method_18676(pos.field_9181, sy, pos.field_9180));
               if (block != null && !block.method_12146()) {
                  for (int y = 0; y < 16; y++) {
                     for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                           int light = block.method_12139(x, y, z);
                           if (light >= this.minLight) {
                              cursor.method_10103((pos.field_9181 << 4) + x, (sy << 4) + y, (pos.field_9180 << 4) + z);
                              if (light > skyLight.method_15543(cursor)) {
                                 this.hits.put(cursor.method_10062(), light);
                              }

                              if (this.hits.size() >= 250000) {
                                 this.chunks.clear();
                                 return true;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return this.chunks.isEmpty();
   }
}
