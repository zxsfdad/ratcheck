package net.anubis.client.esp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongPredicate;
import net.anubis.client.event.impl.BlockUpdateEvent;
import net.anubis.client.event.impl.PacketReceiveNettyEvent;
import net.anubis.client.module.impl.client.PerformanceModule;
import net.anubis.client.util.PerformanceBudget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.Load;
import net.minecraft.class_1923;
import net.minecraft.class_2338;
import net.minecraft.class_2678;
import net.minecraft.class_2680;
import net.minecraft.class_2818;
import net.minecraft.class_2826;
import net.minecraft.class_310;
import net.minecraft.class_638;
import net.minecraft.class_2338.class_2339;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class ChunkScanner {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final Object OWNER = new Object();
   private static final ChunkScanner.Consumer[] NONE = new ChunkScanner.Consumer[0];
   private static final List<ChunkScanner.Consumer> consumers = new ArrayList<>();
   private static ChunkScanner.Consumer[] snapshot = NONE;
   private static final ArrayDeque<Long> queue = new ArrayDeque<>();
   private static final Set<Long> queued = new HashSet<>();
   private static volatile boolean resetPending;
   private static ChunkScanner.ScanCursor scanning;
   private static class_638 scanLevel;
   private static ChunkScanner.ScanBatch deliveryBatch;

   static ChunkScanner.ScanBatch deliveryBatch() {
      return deliveryBatch;
   }

   private ChunkScanner() {
   }

   public static void init() {
      ClientChunkEvents.CHUNK_LOAD.register((Load)(level, chunk) -> enqueue(chunk.method_12004().method_8324()));
      ClientTickEvents.END_CLIENT_TICK.register(ChunkScanner::tick);
      BlockUpdateEvent.TYPE.listen(OWNER, ChunkScanner::onBlockUpdate);
      PacketReceiveNettyEvent.TYPE.listen(OWNER, event -> {
         if (event.packet instanceof class_2678) {
            resetPending = true;
         }
      });
   }

   public static void register(ChunkScanner.Consumer consumer) {
      if (!consumers.contains(consumer)) {
         consumers.add(consumer);
         snapshot = consumers.toArray(NONE);
         restartScan();
         queueLoadedChunks();
      }
   }

   public static void unregister(ChunkScanner.Consumer consumer) {
      consumers.remove(consumer);
      snapshot = consumers.toArray(NONE);
      restartScan();
      if (consumers.isEmpty()) {
         queue.clear();
         queued.clear();
      }
   }

   public static int pending() {
      return queue.size() + (scanning == null ? 0 : 1);
   }

   public static void rescan() {
      restartScan();
      queueLoadedChunks();
   }

   private static void restartScan() {
      if (scanning != null) {
         long key = scanning.chunk.method_12004().method_8324();
         scanning.batch.finished = true;
         scanning = null;
         enqueue(key);
      }
   }

   private static void queueLoadedChunks() {
      class_310 mc = class_310.method_1551();
      if (mc.field_1687 != null && mc.field_1724 != null) {
         int cx = mc.field_1724.method_31477() >> 4;
         int cz = mc.field_1724.method_31479() >> 4;
         int radius = (Integer)mc.field_1690.method_42503().method_41753() + 1;
         List<long[]> loaded = new ArrayList<>();

         for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
               if (mc.field_1687.method_2935().method_12123(x, z)) {
                  int dx = x - cx;
                  int dz = z - cz;
                  loaded.add(new long[]{class_1923.method_8331(x, z), dx * dx + dz * dz});
               }
            }
         }

         loaded.sort(Comparator.comparingLong(a -> a[1]));

         for (long[] entry : loaded) {
            enqueue(entry[0]);
         }
      }
   }

   private static void enqueue(long chunkKey) {
      if (!consumers.isEmpty() && queued.add(chunkKey)) {
         queue.add(chunkKey);
      }
   }

   private static void tick(class_310 mc) {
      if (scanLevel != mc.field_1687) {
         if (scanning != null) {
            scanning.batch.finished = true;
         }

         scanning = null;
         scanLevel = mc.field_1687;
      }

      if (resetPending) {
         resetPending = false;
         queue.clear();
         queued.clear();
         if (scanning != null) {
            scanning.batch.finished = true;
         }

         scanning = null;

         for (ChunkScanner.Consumer consumer : snapshot) {
            call(consumer, ChunkScanner.Consumer::reset);
         }

         queueLoadedChunks();
      }

      if (mc.field_1687 != null && !consumers.isEmpty()) {
         if (PerformanceModule.optimizationsEnabled()) {
            tickIncremental(mc);
         } else {
            restartScan();
            long deadline = System.nanoTime() + PerformanceModule.scanBudgetNanos();

            Long key;
            while ((key = queue.poll()) != null) {
               queued.remove(key);
               class_2818 chunk = mc.field_1687.method_2935().method_21730(class_1923.method_8325(key), class_1923.method_8332(key));
               if (chunk != null) {
                  scan(mc, chunk);
               }

               if (System.nanoTime() >= deadline) {
                  break;
               }
            }
         }
      }
   }

   private static void tickIncremental(class_310 mc) {
      long deadline = PerformanceBudget.acquire(PerformanceModule.scanBudgetNanos());
      long started = System.nanoTime();
      if (started < deadline) {
         try {
            while (System.nanoTime() < deadline) {
               if (scanning == null) {
                  Long key = queue.poll();
                  if (key == null) {
                     break;
                  }

                  queued.remove(key);
                  class_2818 chunk = mc.field_1687.method_2935().method_21730(class_1923.method_8325(key), class_1923.method_8332(key));
                  if (chunk == null) {
                     continue;
                  }

                  scanning = new ChunkScanner.ScanCursor(chunk, mc.field_1687.method_8597().comp_651() >> 4, snapshot);
               }

               ChunkScanner.ScanCursor cursor = scanning;
               if (mc.field_1687.method_2935().method_21730(cursor.chunk.method_12004().field_9181, cursor.chunk.method_12004().field_9180) != cursor.chunk) {
                  cursor.batch.finished = true;
                  scanning = null;
               } else {
                  boolean complete = cursor.step();
                  if (scanning == cursor && complete) {
                     scanning = null;
                     finishScan(cursor.chunk);
                  }
               }
            }
         } finally {
            PerformanceBudget.record(started);
         }
      }
   }

   private static void scan(class_310 mc, class_2818 chunk) {
      int minX = chunk.method_12004().method_8326();
      int minZ = chunk.method_12004().method_8328();
      int minSection = mc.field_1687.method_8597().comp_651() >> 4;
      class_2826[] sections = chunk.method_12006();
      List<ChunkScanner.Consumer> interested = new ArrayList<>(consumers.size());

      for (int si = 0; si < sections.length; si++) {
         class_2826 section = sections[si];
         if (section != null && !section.method_38292()) {
            interested.clear();

            for (ChunkScanner.Consumer consumer : consumers) {
               if (section.method_19523(consumer::wants)) {
                  interested.add(consumer);
               }
            }

            if (!interested.isEmpty()) {
               int baseY = minSection + si << 4;

               for (int x = 0; x < 16; x++) {
                  for (int z = 0; z < 16; z++) {
                     for (int y = 0; y < 16; y++) {
                        class_2680 state = section.method_12254(x, y, z);
                        if (!state.method_26215()) {
                           long pos = class_2338.method_10064(minX + x, baseY + y, minZ + z);

                           for (ChunkScanner.Consumer consumer : interested) {
                              if (consumer.wants(state)) {
                                 found(consumer, pos, state);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      finishScan(chunk);
   }

   private static void finishScan(class_2818 chunk) {
      long chunkKey = chunk.method_12004().method_8324();
      class_2339 probe = new class_2339();

      for (ChunkScanner.Consumer consumer : snapshot) {
         call(consumer, c -> c.chunkScanned(chunkKey, pos -> {
            class_2680 state = chunk.method_8320(probe.method_16363(pos));
            return !state.method_26215() && c.wants(state);
         }));
      }
   }

   private static void onBlockUpdate(BlockUpdateEvent event) {
      ChunkScanner.Consumer[] current = snapshot;
      if (current.length != 0) {
         long pos = event.pos.method_10063();
         class_2680 state = event.newState;
         ChunkScanner.ScanBatch previous = deliveryBatch;
         deliveryBatch = null;

         try {
            for (ChunkScanner.Consumer consumer : current) {
               if (!state.method_26215() && consumer.wants(state)) {
                  found(consumer, pos, state);
               } else {
                  removed(consumer, pos);
               }
            }
         } finally {
            deliveryBatch = previous;
         }
      }
   }

   private static void found(ChunkScanner.Consumer consumer, long pos, class_2680 state) {
      try {
         consumer.found(pos, state);
      } catch (RuntimeException e) {
         drop(consumer, e);
      }
   }

   private static void removed(ChunkScanner.Consumer consumer, long pos) {
      try {
         consumer.removed(pos);
      } catch (RuntimeException e) {
         drop(consumer, e);
      }
   }

   private static void call(ChunkScanner.Consumer consumer, java.util.function.Consumer<ChunkScanner.Consumer> action) {
      try {
         action.accept(consumer);
      } catch (RuntimeException e) {
         drop(consumer, e);
      }
   }

   private static void drop(ChunkScanner.Consumer consumer, RuntimeException e) {
      LOGGER.error("[Anubis] Chunk scan consumer {} failed; disabling it", consumer.getClass().getSimpleName(), e);
      consumers.remove(consumer);
      snapshot = consumers.toArray(NONE);
      restartScan();
   }

   @Environment(EnvType.CLIENT)
   public interface Consumer {
      boolean wants(class_2680 var1);

      void found(long var1, class_2680 var3);

      void removed(long var1);

      void reset();

      default void chunkScanned(long chunkKey, LongPredicate stillThere) {
      }
   }

   @Environment(EnvType.CLIENT)
   static final class ScanBatch {
      boolean finished;
   }

   @Environment(EnvType.CLIENT)
   private static final class ScanCursor {
      final class_2818 chunk;
      final int minSection;
      final ChunkScanner.Consumer[] listeners;
      final ChunkScanner.ScanBatch batch = new ChunkScanner.ScanBatch();
      final List<ChunkScanner.Consumer> interested = new ArrayList<>();
      int sectionIndex;
      int blockIndex;

      ScanCursor(class_2818 chunk, int minSection, ChunkScanner.Consumer[] listeners) {
         this.chunk = chunk;
         this.minSection = minSection;
         this.listeners = listeners;
      }

      boolean step() {
         if (this.batch.finished) {
            return true;
         }

         ChunkScanner.ScanBatch previous = ChunkScanner.deliveryBatch;
         ChunkScanner.deliveryBatch = this.batch;

         try {
            boolean complete = this.stepBlocks();
            if (complete) {
               this.batch.finished = true;
            }

            return complete;
         } finally {
            ChunkScanner.deliveryBatch = previous;
         }
      }

      private boolean stepBlocks() {
         class_2826[] sections = this.chunk.method_12006();
         if (this.sectionIndex >= sections.length) {
            return true;
         }

         class_2826 section = sections[this.sectionIndex];
         if (this.blockIndex == 0) {
            this.interested.clear();
            if (section != null && !section.method_38292()) {
               for (ChunkScanner.Consumer consumer : this.listeners) {
                  if (ChunkScanner.consumers.contains(consumer) && section.method_19523(consumer::wants)) {
                     this.interested.add(consumer);
                  }
               }
            }

            if (this.interested.isEmpty()) {
               this.sectionIndex++;
               return this.sectionIndex >= sections.length;
            }
         }

         int end = Math.min(4096, this.blockIndex + 256);
         int minX = this.chunk.method_12004().method_8326();
         int minZ = this.chunk.method_12004().method_8328();
         int baseY = this.minSection + this.sectionIndex << 4;

         while (this.blockIndex < end && !this.batch.finished) {
            int index = this.blockIndex++;
            int x = index >> 8;
            int z = index >> 4 & 15;
            int y = index & 15;
            class_2680 state = section.method_12254(x, y, z);
            if (!state.method_26215()) {
               long pos = class_2338.method_10064(minX + x, baseY + y, minZ + z);

               for (ChunkScanner.Consumer consumer : this.interested) {
                  if (consumer.wants(state)) {
                     ChunkScanner.found(consumer, pos, state);
                  }
               }
            }
         }

         if (this.blockIndex == 4096) {
            this.blockIndex = 0;
            this.sectionIndex++;
         }

         return this.sectionIndex >= sections.length;
      }
   }
}
