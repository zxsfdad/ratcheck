package net.anubis.client.esp;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.LongPredicate;
import java.util.function.Supplier;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.module.impl.client.PerformanceModule;
import net.anubis.client.util.BlockBoxHelper;
import net.anubis.client.util.BlockClusterGeometry;
import net.anubis.client.util.TracerEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12179;
import net.minecraft.class_12180;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_4604;

@Environment(EnvType.CLIENT)
public final class BlockLayer {
   private static final long REVEAL_NANOS = 380000000L;
   private static final double FADE_START = 64.0;
   private static final double FADE_END = 256.0;
   private static final float FADE_FLOOR = 0.45F;
   private final BlockLayer.Palette palette;
   private final Supplier<Object> dimension;
   private final Map<Object, BlockLayer.World> worlds = new HashMap<>();

   public BlockLayer(BlockLayer.Palette palette, Supplier<Object> dimension) {
      this.palette = palette;
      this.dimension = dimension;
   }

   public void put(long pos, Object kind) {
      BlockLayer.World world = this.world(true);
      if (world != null) {
         BlockLayer.Found existing = (BlockLayer.Found)world.positions.get(pos);
         if (existing == null || !Objects.equals(existing.kind(), kind)) {
            world.positions.put(pos, new BlockLayer.Found(kind, System.nanoTime()));
            ((LongOpenHashSet)world.byChunk.computeIfAbsent(chunkOf(pos), k -> new LongOpenHashSet())).add(pos);
            markDirty(world, pos);
         }
      }
   }

   public void remove(long pos) {
      BlockLayer.World world = this.world(false);
      if (world != null && world.positions.remove(pos) != null) {
         long chunk = chunkOf(pos);
         LongOpenHashSet set = (LongOpenHashSet)world.byChunk.get(chunk);
         if (set != null && set.remove(pos) && set.isEmpty()) {
            world.byChunk.remove(chunk);
         }

         markDirty(world, pos);
      }
   }

   public void clear() {
      this.worlds.clear();
   }

   public void retainChunk(long chunkKey, LongPredicate keep) {
      BlockLayer.World world = this.world(false);
      if (world != null) {
         LongOpenHashSet set = (LongOpenHashSet)world.byChunk.get(chunkKey);
         if (set != null) {
            for (long pos : set.toLongArray()) {
               if (!keep.test(pos)) {
                  this.remove(pos);
               }
            }
         }
      }
   }

   public List<BlockLayer.Vein> veins() {
      BlockLayer.World world = this.world(false);
      if (world == null) {
         return List.of();
      }

      this.rebuildDirty(world);
      if (world.veinsDirty) {
         world.veins = joinVeins(world);
         world.veinsDirty = false;
      }

      return world.veins;
   }

   private BlockLayer.World world(boolean create) {
      Object key = this.dimension.get();
      if (key == null) {
         return null;
      } else {
         return create ? this.worlds.computeIfAbsent(key, k -> new BlockLayer.World()) : this.worlds.get(key);
      }
   }

   private static long chunkKey(int cx, int cz) {
      return cx & 4294967295L | (long)cz << 32;
   }

   private static long chunkOf(long pos) {
      return chunkKey(class_2338.method_10061(pos) >> 4, class_2338.method_10083(pos) >> 4);
   }

   private static long regionOf(long chunk) {
      return chunkKey((int)chunk >> 4, (int)(chunk >> 32) >> 4);
   }

   private static void markDirty(BlockLayer.World world, long pos) {
      ChunkScanner.ScanBatch batch = ChunkScanner.deliveryBatch();
      LongOpenHashSet dirty = world.dirty;
      if (batch != null && !batch.finished) {
         if (world.scanBatch != batch) {
            flushScanDirty(world);
         }

         world.scanBatch = batch;
         dirty = world.scanDirty;
      }

      int x = class_2338.method_10061(pos);
      int z = class_2338.method_10083(pos);
      int cx = x >> 4;
      int cz = z >> 4;
      dirty.add(chunkKey(cx, cz));
      if ((x & 15) == 0) {
         dirty.add(chunkKey(cx - 1, cz));
      }

      if ((x & 15) == 15) {
         dirty.add(chunkKey(cx + 1, cz));
      }

      if ((z & 15) == 0) {
         dirty.add(chunkKey(cx, cz - 1));
      }

      if ((z & 15) == 15) {
         dirty.add(chunkKey(cx, cz + 1));
      }
   }

   private static void flushScanDirty(BlockLayer.World world) {
      world.dirty.addAll(world.scanDirty);
      world.scanDirty.clear();
      world.scanBatch = null;
   }

   private void rebuildDirty(BlockLayer.World world) {
      if (world.scanBatch != null && (world.scanBatch.finished || !PerformanceModule.optimizationsEnabled())) {
         flushScanDirty(world);
      }

      if (!world.dirty.isEmpty()) {
         LongIterator var2 = world.dirty.iterator();

         while (var2.hasNext()) {
            long chunk = (Long)var2.next();
            this.rebuildChunk(world, chunk);
         }

         world.dirty.clear();
         world.veinsDirty = true;
      }
   }

   private void rebuildChunk(BlockLayer.World world, long chunkKey) {
      LongOpenHashSet set = (LongOpenHashSet)world.byChunk.get(chunkKey);
      if (set == null) {
         if (world.shapes.remove(chunkKey) != null) {
            LongOpenHashSet region = (LongOpenHashSet)world.shapeRegions.get(regionOf(chunkKey));
            if (region != null && region.remove(chunkKey) && region.isEmpty()) {
               world.shapeRegions.remove(regionOf(chunkKey));
            }

            world.shapeRevision++;
         }
      } else {
         Map<Object, List<class_2338>> byKind = new LinkedHashMap<>();
         int minX = Integer.MAX_VALUE;
         int minY = Integer.MAX_VALUE;
         int minZ = Integer.MAX_VALUE;
         int maxX = Integer.MIN_VALUE;
         int maxY = Integer.MIN_VALUE;
         int maxZ = Integer.MIN_VALUE;
         LongIterator cx = set.iterator();

         while (cx.hasNext()) {
            long pos = (Long)cx.next();
            class_2338 block = class_2338.method_10092(pos);
            byKind.computeIfAbsent(((BlockLayer.Found)world.positions.get(pos)).kind(), k -> new ArrayList<>()).add(block);
            minX = Math.min(minX, block.method_10263());
            maxX = Math.max(maxX, block.method_10263());
            minY = Math.min(minY, block.method_10264());
            maxY = Math.max(maxY, block.method_10264());
            minZ = Math.min(minZ, block.method_10260());
            maxZ = Math.max(maxZ, block.method_10260());
         }

         int cxx = (int)chunkKey;
         int cz = (int)(chunkKey >> 32);
         List<BlockLayer.Cluster> clusters = new ArrayList<>();

         for (Entry<Object, List<class_2338>> entry : byKind.entrySet()) {
            Object kind = entry.getKey();
            BlockClusterGeometry.Membership same = (x, y, z) -> {
               BlockLayer.Found found = (BlockLayer.Found)world.positions.get(class_2338.method_10064(x, y, z));
               return found != null && Objects.equals(found.kind(), kind);
            };

            for (List<class_2338> component : BlockClusterGeometry.connectedComponents(entry.getValue())) {
               clusters.add(buildCluster(world, kind, component, same, cxx, cz));
            }
         }

         world.shapes.put(chunkKey, new BlockLayer.Shapes(clusters, new class_238(minX - 1, minY - 1, minZ - 1, maxX + 2, maxY + 2, maxZ + 2)));
         ((LongOpenHashSet)world.shapeRegions.computeIfAbsent(regionOf(chunkKey), k -> new LongOpenHashSet())).add(chunkKey);
         world.shapeRevision++;
      }
   }

   private static Iterable<BlockLayer.Shapes> nearbyShapes(BlockLayer.World world, class_243 eye, double range) {
      if (!PerformanceModule.optimizationsEnabled()) {
         return world.shapes.values();
      }

      int cx = (int)Math.floor(eye.field_1352) >> 4;
      int cz = (int)Math.floor(eye.field_1350) >> 4;
      if (world.candidateRevision == world.shapeRevision && world.candidateX == cx && world.candidateZ == cz && world.candidateRange == range) {
         return world.candidates;
      }

      world.candidates.clear();
      int radius = (int)Math.ceil(range / 16.0) + 1;

      for (int rx = cx - radius >> 4; rx <= cx + radius >> 4; rx++) {
         for (int rz = cz - radius >> 4; rz <= cz + radius >> 4; rz++) {
            LongOpenHashSet region = (LongOpenHashSet)world.shapeRegions.get(chunkKey(rx, rz));
            if (region != null) {
               LongIterator var10 = region.iterator();

               while (var10.hasNext()) {
                  long key = (Long)var10.next();
                  if (Math.abs((long)((int)key) - cx) <= radius && Math.abs((long)((int)(key >> 32)) - cz) <= radius) {
                     BlockLayer.Shapes shape = (BlockLayer.Shapes)world.shapes.get(key);
                     if (shape != null) {
                        world.candidates.add(shape);
                     }
                  }
               }
            }
         }
      }

      world.candidateRevision = world.shapeRevision;
      world.candidateX = cx;
      world.candidateZ = cz;
      world.candidateRange = range;
      return world.candidates;
   }

   private static BlockLayer.Cluster buildCluster(
      BlockLayer.World world, Object kind, List<class_2338> component, BlockClusterGeometry.Membership same, int cx, int cz
   ) {
      long[] positions = new long[component.size()];
      List<Long> links = new ArrayList<>();
      double sx = 0.0;
      double sy = 0.0;
      double sz = 0.0;
      long bornAt = Long.MAX_VALUE;

      for (int i = 0; i < positions.length; i++) {
         class_2338 pos = component.get(i);
         positions[i] = pos.method_10063();
         sx += pos.method_10263() + 0.5;
         sy += pos.method_10264() + 0.5;
         sz += pos.method_10260() + 0.5;
         bornAt = Math.min(bornAt, ((BlockLayer.Found)world.positions.get(positions[i])).bornAt());

         for (class_2338 next : new class_2338[]{pos.method_10078(), pos.method_10067(), pos.method_10072(), pos.method_10095()}) {
            if ((next.method_10263() >> 4 != cx || next.method_10260() >> 4 != cz)
               && same.contains(next.method_10263(), next.method_10264(), next.method_10260())) {
               links.add(next.method_10063());
            }
         }
      }

      int n = positions.length;
      return new BlockLayer.Cluster(
         kind,
         positions,
         new class_243(sx / n, sy / n, sz / n),
         bornAt,
         BlockClusterGeometry.build(component, same),
         links.stream().mapToLong(Long::longValue).toArray()
      );
   }

   private static List<BlockLayer.Vein> joinVeins(BlockLayer.World world) {
      List<BlockLayer.Cluster> all = new ArrayList<>();
      Long2ObjectOpenHashMap<BlockLayer.Cluster> owner = new Long2ObjectOpenHashMap();
      ObjectIterator parent = world.shapes.values().iterator();

      while (parent.hasNext()) {
         BlockLayer.Shapes shapes = (BlockLayer.Shapes)parent.next();

         for (BlockLayer.Cluster cluster : shapes.clusters()) {
            all.add(cluster);
            if (cluster.links().length != 0) {
               for (long pos : cluster.positions()) {
                  int lx = class_2338.method_10061(pos) & 15;
                  int lz = class_2338.method_10083(pos) & 15;
                  if (lx == 0 || lx == 15 || lz == 0 || lz == 15) {
                     owner.put(pos, cluster);
                  }
               }
            }
         }
      }

      Map<BlockLayer.Cluster, BlockLayer.Cluster> parentx = new IdentityHashMap<>();

      for (BlockLayer.Cluster cluster : all) {
         parentx.put(cluster, cluster);
      }

      for (BlockLayer.Cluster cluster : all) {
         for (long link : cluster.links()) {
            BlockLayer.Cluster other = (BlockLayer.Cluster)owner.get(link);
            if (other != null) {
               parentx.put(find(parentx, cluster), find(parentx, other));
            }
         }
      }

      Map<BlockLayer.Cluster, double[]> sums = new IdentityHashMap<>();

      for (BlockLayer.Cluster cluster : all) {
         double[] sum = sums.computeIfAbsent(find(parentx, cluster), r -> new double[4]);
         int n = cluster.positions().length;
         sum[0] += cluster.center().field_1352 * n;
         sum[1] += cluster.center().field_1351 * n;
         sum[2] += cluster.center().field_1350 * n;
         sum[3] += n;
      }

      List<BlockLayer.Vein> veins = new ArrayList<>(sums.size());

      for (Entry<BlockLayer.Cluster, double[]> entry : sums.entrySet()) {
         double[] s = entry.getValue();
         veins.add(new BlockLayer.Vein(entry.getKey().kind(), new class_243(s[0] / s[3], s[1] / s[3], s[2] / s[3])));
      }

      return veins;
   }

   private static BlockLayer.Cluster find(Map<BlockLayer.Cluster, BlockLayer.Cluster> parent, BlockLayer.Cluster cluster) {
      BlockLayer.Cluster root = cluster;

      while (parent.get(root) != root) {
         root = parent.get(root);
      }

      while (parent.get(cluster) != root) {
         BlockLayer.Cluster next = parent.get(cluster);
         parent.put(cluster, root);
         cluster = next;
      }

      return root;
   }

   public void render(Render3DEvent event, EspStyle style) {
      class_310 mc = class_310.method_1551();
      BlockLayer.World world = this.world(false);
      if (mc.field_1724 != null && world != null) {
         this.rebuildDirty(world);
         class_4604 frustum = PerformanceModule.frustumCulling() ? event.frustum : null;
         long now = System.nanoTime();
         class_243 eye = mc.field_1724.method_5836(event.partialTick);
         boolean outline = style.outline();
         boolean glow = style.glow.get();
         boolean fade = style.fade.get();
         boolean reveal = style.reveal.get();
         float baseWidth = style.lineWidth.get();
         float fillAlpha = style.fillAlpha.get();
         float boxSize = style.boxSize.get();
         double range = style.range.get();

         for (BlockLayer.Shapes shapes : nearbyShapes(world, eye, range)) {
            if (frustum == null || frustum.method_23093(shapes.bounds())) {
               for (BlockLayer.Cluster cluster : shapes.clusters()) {
                  double dist = eye.method_1022(cluster.center());
                  if (!(dist > range)) {
                     float alpha = fade ? distanceFade(dist) : 1.0F;
                     float t = reveal ? Math.min(1.0F, (float)(now - cluster.bornAt()) / 3.8E8F) : 1.0F;
                     alpha *= easeOutCubic(t);
                     if (!(alpha < 0.02F)) {
                        float scale = t < 1.0F ? 0.55F + 0.45F * easeOutBack(t) : 1.0F;
                        float width = fade ? baseWidth * (1.0F - 0.3F * (float)Math.min(1.0, dist / 256.0)) : baseWidth;
                        int color = this.palette.color(cluster.kind());
                        int stroke = scaleAlpha(color, alpha);
                        int fill = fillAlpha > 0.0F ? withAlpha(color, fillAlpha * alpha) : 0;
                        if (outline) {
                           drawOutline(cluster, stroke, fill, width, glow, scale);
                        } else {
                           drawBoxes(cluster, stroke, fill, width, boxSize * scale);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void drawOutline(BlockLayer.Cluster cluster, int stroke, int fill, float width, boolean glow, float scale) {
      class_243 c = cluster.center();
      BlockClusterGeometry.Mesh mesh = cluster.mesh();
      if (fill >>> 24 > 0) {
         class_12179 style = class_12179.method_75539(fill);

         for (BlockClusterGeometry.Face face : mesh.faces()) {
            class_12180.method_75547(grow(face.a(), c, scale), grow(face.b(), c, scale), grow(face.c(), c, scale), grow(face.d(), c, scale), style)
               .method_75533();
         }
      }

      int halo = glow ? scaleAlpha(stroke, 0.18F) : 0;

      for (BlockClusterGeometry.Segment edge : mesh.edges()) {
         class_243 a = grow(edge.a(), c, scale);
         class_243 b = grow(edge.b(), c, scale);
         if (glow) {
            class_12180.method_75546(a, b, halo, width * 3.0F).method_75533();
         }

         class_12180.method_75546(a, b, stroke, width).method_75533();
      }
   }

   private static void drawBoxes(BlockLayer.Cluster cluster, int stroke, int fill, float width, float size) {
      class_12179 style = fill >>> 24 > 0 ? class_12179.method_75537(stroke, width, fill) : class_12179.method_75536(stroke, width);

      for (long pos : cluster.positions()) {
         class_238 bounds = BlockBoxHelper.scaledBlockBox(class_2338.method_10092(pos), size);
         class_12180.method_75541(bounds, style).method_75533();
      }
   }

   private static class_243 grow(class_243 p, class_243 c, float scale) {
      return scale == 1.0F ? p : c.method_1019(p.method_1020(c).method_1021(scale));
   }

   private static float distanceFade(double dist) {
      if (dist <= 64.0) {
         return 1.0F;
      }

      float t = (float)Math.min(1.0, (dist - 64.0) / 192.0);
      return 1.0F - 0.55F * t * t;
   }

   public void renderTracers(EspStyle style) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1724 != null) {
         class_243 eye = mc.field_1724.method_5836(1.0F);
         double rangeSq = (double)style.range.get() * style.range.get();

         for (BlockLayer.Vein vein : this.veins()) {
            if (!(eye.method_1025(vein.center()) > rangeSq)) {
               int color = this.palette.tracerColor(vein.kind());
               if (color >>> 24 != 0) {
                  TracerEngine.draw(vein.center(), color);
               }
            }
         }
      }
   }

   private static int withAlpha(int argb, float alpha) {
      return Math.round(255.0F * Math.max(0.0F, Math.min(1.0F, alpha))) << 24 | argb & 16777215;
   }

   private static int scaleAlpha(int argb, float mul) {
      int alpha = Math.round((argb >>> 24) * Math.max(0.0F, Math.min(1.0F, mul)));
      return alpha << 24 | argb & 16777215;
   }

   private static float easeOutCubic(float t) {
      float inv = 1.0F - t;
      return 1.0F - inv * inv * inv;
   }

   private static float easeOutBack(float t) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float u = t - 1.0F;
      return 1.0F + c3 * u * u * u + c1 * u * u;
   }

   @Environment(EnvType.CLIENT)
   private record Cluster(Object kind, long[] positions, class_243 center, long bornAt, BlockClusterGeometry.Mesh mesh, long[] links) {
   }

   @Environment(EnvType.CLIENT)
   private record Found(Object kind, long bornAt) {
   }

   @Environment(EnvType.CLIENT)
   public interface Palette {
      int color(Object var1);

      int tracerColor(Object var1);
   }

   @Environment(EnvType.CLIENT)
   private record Shapes(List<BlockLayer.Cluster> clusters, class_238 bounds) {
   }

   @Environment(EnvType.CLIENT)
   public record Vein(Object kind, class_243 center) {
   }

   @Environment(EnvType.CLIENT)
   private static final class World {
      final Long2ObjectOpenHashMap<BlockLayer.Found> positions = new Long2ObjectOpenHashMap();
      final Long2ObjectOpenHashMap<LongOpenHashSet> byChunk = new Long2ObjectOpenHashMap();
      final Long2ObjectOpenHashMap<BlockLayer.Shapes> shapes = new Long2ObjectOpenHashMap();
      final Long2ObjectOpenHashMap<LongOpenHashSet> shapeRegions = new Long2ObjectOpenHashMap();
      final LongOpenHashSet dirty = new LongOpenHashSet();
      final LongOpenHashSet scanDirty = new LongOpenHashSet();
      ChunkScanner.ScanBatch scanBatch;
      List<BlockLayer.Vein> veins = List.of();
      boolean veinsDirty;
      long shapeRevision;
      long candidateRevision = -1L;
      int candidateX;
      int candidateZ;
      double candidateRange;
      final List<BlockLayer.Shapes> candidates = new ArrayList<>();
   }
}
