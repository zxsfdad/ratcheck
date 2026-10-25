package net.anubis.client.util.playerbypass2;

import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10264;
import net.minecraft.class_10583;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1309;
import net.minecraft.class_1311;
import net.minecraft.class_1314;
import net.minecraft.class_1540;
import net.minecraft.class_1542;
import net.minecraft.class_1606;
import net.minecraft.class_1657;
import net.minecraft.class_1923;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2604;
import net.minecraft.class_2626;
import net.minecraft.class_2637;
import net.minecraft.class_2663;
import net.minecraft.class_2680;
import net.minecraft.class_2684;
import net.minecraft.class_2726;
import net.minecraft.class_2739;
import net.minecraft.class_2741;
import net.minecraft.class_2744;
import net.minecraft.class_2775;
import net.minecraft.class_310;
import net.minecraft.class_3481;
import net.minecraft.class_5321;
import net.minecraft.class_5955;
import net.minecraft.class_6025;
import net.minecraft.class_638;
import net.minecraft.class_6880;
import net.minecraft.class_746;
import net.minecraft.class_7923;
import net.minecraft.class_8110;
import net.minecraft.class_8111;
import net.minecraft.class_8143;

@Environment(EnvType.CLIENT)
public final class SimulationSignals {
   private static final int RECENT = 6;
   private static final long SECOND = 1000000000L;
   private static final long CHUNK_SETTLE = 3000000000L;
   private static final long FRESH_ENTITY = 2000000000L;
   private static final long WANDER_MEMORY = 60000000000L;
   private static final long TELEPORT_QUIET = 2000000000L;
   private static final long LIGHTNING_LIFE = 3000000000L;
   private static final long UNDERGROUND_HOLD = 3000000000L;
   private static final long FROZEN_AFTER = 40000000000L;
   private static final int MAX_PINS = 32;
   private static final double MIN_MOVE_SQR = 1.0E-4;
   private static final double TELEPORT_JUMP_SQR = 64.0;
   private static final double SAMPLE_JUMP_SQR = 900.0;
   private static final double TRACKING_INSET = 24.0;
   private static final double TRACKED_DECAY = 0.5;
   private static final float FORGET_SCORE = 0.05F;
   private static final float WEAK_CAP = 4.0F;
   private static final float STRONG_CAP = 10.0F;
   private static final int MIN_Y = 0;
   private static final int BLOCK_SPILL = 1;
   private static final Set<class_2248> GROWS_FROM_AIR = Set.of(
      class_2246.field_10424,
      class_2246.field_10029,
      class_2246.field_10211,
      class_2246.field_10108,
      class_2246.field_46283,
      class_2246.field_46282,
      class_2246.field_10477,
      class_2246.field_10036,
      class_2246.field_22089,
      class_2246.field_10251,
      class_2246.field_10559,
      class_2246.field_10021,
      class_2246.field_10528
   );
   private static final Set<class_2248> GONE_TO_AIR = Set.of(
      class_2246.field_10477, class_2246.field_10036, class_2246.field_22089, class_2246.field_10195, class_2246.field_43230, class_2246.field_37575
   );
   private static boolean active;
   private static int serverSim = -1;
   private static int serverView = -1;
   private static int simOverride;
   private static int margin;
   private static boolean ignoreVisiblePlayers = true;
   private static long memoryNanos = 30000000000L;
   private static int ticks;
   private static int session;
   private static class_5321<class_1937> dimension;
   private static long quietUntil;
   private static long undergroundUntil;
   private static double lastX;
   private static double lastZ;
   private static double sampleX;
   private static double sampleZ;
   private static boolean hasLast;
   private static boolean hasSample;
   private static long passedCenter = Long.MIN_VALUE;
   private static int passedRadius = -1;
   private static final Long2ObjectOpenHashMap<SimulationSignals.ChunkSignal> CHUNKS = new Long2ObjectOpenHashMap();
   private static final Int2ObjectOpenHashMap<SimulationSignals.Share> ENTITY_SHARES = new Int2ObjectOpenHashMap();
   private static final Long2ObjectOpenHashMap<SimulationSignals.Share> BLOCK_SHARES = new Long2ObjectOpenHashMap();
   private static final Long2LongOpenHashMap CHUNK_LOADED = new Long2LongOpenHashMap();
   private static final Int2LongOpenHashMap ENTITY_ADDED = new Int2LongOpenHashMap();
   private static final Int2LongOpenHashMap ENTITY_INSIDE = new Int2LongOpenHashMap();
   private static final Map<class_5321<class_1937>, LongOpenHashSet> PASSED_BY_DIMENSION = new HashMap<>();
   private static LongOpenHashSet passed = new LongOpenHashSet();
   private static final Int2DoubleOpenHashMap TRACKED_RANGE = new Int2DoubleOpenHashMap();
   private static final Int2ObjectOpenHashMap<long[]> LIGHTNING_PENDING = new Int2ObjectOpenHashMap();
   private static final Int2LongOpenHashMap LAST_ACTIVE = new Int2LongOpenHashMap();
   private static final LongOpenHashSet FROZEN = new LongOpenHashSet();
   private static final ArrayDeque<SimulationSignals.Pin> PINS = new ArrayDeque<>();

   private SimulationSignals() {
   }

   public static void configure(boolean on, int simulationOverride, int ringMargin, boolean ignorePlayers, int memorySeconds) {
      if (active && !on) {
         clear();
      }

      active = on;
      simOverride = simulationOverride;
      margin = ringMargin;
      ignoreVisiblePlayers = ignorePlayers;
      memoryNanos = Math.max(1, memorySeconds) * 1000000000L;
   }

   public static void clear() {
      resetLevel();
      PASSED_BY_DIMENSION.clear();
      passed = new LongOpenHashSet();
      if (dimension != null) {
         PASSED_BY_DIMENSION.put(dimension, passed);
      }
   }

   private static void resetLevel() {
      CHUNKS.clear();
      ENTITY_SHARES.clear();
      BLOCK_SHARES.clear();
      ENTITY_ADDED.clear();
      ENTITY_INSIDE.clear();
      TRACKED_RANGE.clear();
      LIGHTNING_PENDING.clear();
      LAST_ACTIVE.clear();
      FROZEN.clear();
      PINS.clear();
      hasLast = false;
      hasSample = false;
      passedCenter = Long.MIN_VALUE;
      passedRadius = -1;
   }

   public static int session() {
      return session;
   }

   public static int serverView() {
      return serverView;
   }

   public static int simulation() {
      int sim = simOverride > 0 ? simOverride : (serverSim > 0 ? serverSim : 5);
      return Math.max(1, Math.min(sim, view() - 3 - margin));
   }

   public static int view() {
      int option = class_310.method_1551().field_1690.method_38521();
      return serverView > 0 ? Math.min(serverView, option) : option;
   }

   public static int ringStart() {
      return simulation() + 2 + margin;
   }

   public static Long2ObjectMap<SimulationSignals.ChunkSignal> chunks() {
      return CHUNKS;
   }

   public static LongOpenHashSet frozen() {
      return FROZEN;
   }

   public static SimulationSignals.Pin pollPin() {
      return PINS.poll();
   }

   public static void dropPins() {
      PINS.clear();
   }

   public static boolean holding() {
      class_746 self = class_310.method_1551().field_1724;
      return now() < undergroundUntil || self != null && self.method_23318() < 0.0;
   }

   public static long now() {
      return System.nanoTime();
   }

   static float decay(long elapsed) {
      return (float)Math.exp(-elapsed / memoryNanos);
   }

   public static void onLogin(int simulationDistance, int chunkRadius, class_5321<class_1937> level) {
      serverSim = simulationDistance;
      serverView = chunkRadius;
      session++;
      dimension = level;
      CHUNK_LOADED.clear();
      clear();
      startQuiet();
   }

   public static void onRespawn(class_5321<class_1937> level) {
      startQuiet();
      if (!level.equals(dimension)) {
         dimension = level;
         CHUNK_LOADED.clear();
         resetLevel();
         passed = PASSED_BY_DIMENSION.computeIfAbsent(level, key -> new LongOpenHashSet());
      }
   }

   public static void onTeleport() {
      startQuiet();
   }

   public static void onSimulationDistance(int distance) {
      serverSim = distance;
   }

   public static void onViewDistance(int distance) {
      serverView = distance;
   }

   public static void onChunk(int x, int z) {
      if (active) {
         CHUNK_LOADED.put(class_1923.method_8331(x, z), now());
      }
   }

   public static void onMove(class_2684 packet) {
      if (active) {
         class_638 level = class_310.method_1551().field_1687;
         if (level != null) {
            class_1297 entity = packet.method_11645(level);
            if (entity != null) {
               boolean moved = packet.method_22826() && (packet.method_36150() != 0 || packet.method_36151() != 0 || packet.method_36152() != 0);
               if (moved || packet.method_11652()) {
                  LAST_ACTIVE.put(entity.method_5628(), now());
               }

               if (moved) {
                  entitySignal(entity, SimulationSignals.Kind.MOVE, 0);
               } else if (packet.method_11652()) {
                  entitySignal(entity, SimulationSignals.Kind.HEAD, 0);
               }
            }
         }
      }
   }

   public static void onPositionSync(class_10264 packet) {
      if (active) {
         class_638 level = class_310.method_1551().field_1687;
         if (level != null) {
            class_1297 entity = level.method_8469(packet.comp_3223());
            if (entity instanceof class_1309) {
               if (!(entity.method_73189().method_1025(packet.comp_3224().comp_3148()) < 1.0E-4)) {
                  LAST_ACTIVE.put(entity.method_5628(), now());
                  entitySignal(entity, SimulationSignals.Kind.MOVE, 0);
               }
            }
         }
      }
   }

   public static void onHeadRotation(class_2726 packet) {
      if (active) {
         class_638 level = class_310.method_1551().field_1687;
         if (level != null) {
            class_1297 entity = packet.method_11786(level);
            if (entity != null) {
               LAST_ACTIVE.put(entity.method_5628(), now());
               entitySignal(entity, SimulationSignals.Kind.HEAD, 0);
            }
         }
      }
   }

   public static void onEntityData(class_2739 packet) {
      if (active) {
         class_638 level = class_310.method_1551().field_1687;
         if (level != null) {
            if (now() - ENTITY_ADDED.getOrDefault(packet.comp_1127(), -4611686018427387904L) >= 2000000000L) {
               class_1297 entity = level.method_8469(packet.comp_1127());
               if (entity != null) {
                  LAST_ACTIVE.put(entity.method_5628(), now());
                  entitySignal(entity, SimulationSignals.Kind.STATE, 0);
               }
            }
         }
      }
   }

   public static void onEntityEvent(class_2663 packet) {
      if (active) {
         class_638 level = class_310.method_1551().field_1687;
         if (level != null) {
            class_1297 entity = packet.method_11469(level);
            if (entity != null) {
               LAST_ACTIVE.put(entity.method_5628(), now());
               entitySignal(entity, SimulationSignals.Kind.EVENT, 0);
            }
         }
      }
   }

   public static void onEquipment(class_2744 packet) {
      if (active) {
         class_638 level = class_310.method_1551().field_1687;
         if (level != null) {
            if (now() - ENTITY_ADDED.getOrDefault(packet.method_11820(), -4611686018427387904L) >= 2000000000L) {
               class_1297 entity = level.method_8469(packet.method_11820());
               if (entity != null) {
                  LAST_ACTIVE.put(entity.method_5628(), now());
                  entitySignal(entity, SimulationSignals.Kind.EQUIP, 0);
               }
            }
         }
      }
   }

   public static void onDamage(class_8143 packet) {
      if (active) {
         class_310 mc = class_310.method_1551();
         class_638 level = mc.field_1687;
         class_746 self = mc.field_1724;
         if (level != null && self != null) {
            class_6880<class_8110> type = packet.comp_1268();
            if (!type.method_40225(class_8111.field_42353) && !type.method_40225(class_8111.field_42355) && !type.method_40225(class_8111.field_42358)) {
               class_1297 victim = level.method_8469(packet.comp_1267());
               if (victim != null) {
                  int cause = packet.comp_1269();
                  if (type.method_40225(class_8111.field_42320) && cause >= 0 && cause != self.method_5628() && level.method_8469(cause) == null) {
                     pin(victim.method_24515(), "Hidden Attacker hit " + name(victim.method_5864()));
                  }

                  LAST_ACTIVE.put(victim.method_5628(), now());
                  entitySignal(victim, SimulationSignals.Kind.HURT, 0);
               }
            }
         }
      }
   }

   public static void onTakeItem(class_2775 packet) {
      if (active) {
         class_310 mc = class_310.method_1551();
         class_638 level = mc.field_1687;
         class_746 self = mc.field_1724;
         if (level != null && self != null) {
            int collector = packet.method_11912();
            if (collector != self.method_5628()) {
               class_1297 item = level.method_8469(packet.method_11915());
               if (item != null) {
                  class_1297 taker = level.method_8469(collector);
                  if (taker == null) {
                     String what = item instanceof class_1542 stack
                        ? class_7923.field_41178.method_10221(stack.method_6983().method_7909()).method_12832()
                        : name(item.method_5864());
                     pin(item.method_24515(), "Hidden Pickup " + what);
                  } else if (!(taker instanceof class_1657)) {
                     LAST_ACTIVE.put(taker.method_5628(), now());
                     entitySignal(taker, SimulationSignals.Kind.EVENT, 0);
                  }
               }
            }
         }
      }
   }

   public static void onAddEntity(class_2604 packet) {
      if (active) {
         long now = now();
         ENTITY_ADDED.put(packet.method_11167(), now);
         LAST_ACTIVE.put(packet.method_11167(), now);
         class_1299<?> type = packet.method_11169();
         if (type != class_1299.field_6097) {
            class_2338 pos = class_2338.method_49637(packet.method_11175(), packet.method_11174(), packet.method_11176());
            if (type == class_1299.field_6112) {
               LIGHTNING_PENDING.put(packet.method_11167(), new long[]{pos.method_10063(), now});
            } else if (spawnable(type) && !isQuiet()) {
               if (wellInsideTracking(packet.method_11175(), packet.method_11176(), type)) {
                  if (settled(pos)) {
                     int id = packet.method_11167();
                     signal(pos, SimulationSignals.Kind.SPAWN, spill(type), () -> entityShare(id), () -> name(type));
                  }
               }
            }
         }
      }
   }

   public static void onRemoveEntities(IntList ids) {
      if (active) {
         class_638 level = class_310.method_1551().field_1687;
         if (level != null) {
            long now = now();
            boolean quiet = isQuiet();

            for (int i = 0; i < ids.size(); i++) {
               int id = ids.getInt(i);
               long[] bolt = (long[])LIGHTNING_PENDING.remove(id);
               if (bolt != null) {
                  if (!quiet && now - bolt[1] < 3000000000L) {
                     signal(class_2338.method_10092(bolt[0]), SimulationSignals.Kind.LIGHTNING, 0, () -> entityShare(id), () -> "lightning_bolt");
                  }
               } else if (!quiet) {
                  class_1297 entity = level.method_8469(id);
                  if (entity != null
                     && !(entity instanceof class_1657)
                     && now - ENTITY_ADDED.getOrDefault(id, -4611686018427387904L) >= 2000000000L
                     && spawnable(entity.method_5864())
                     && wellInsideTracking(entity.method_23317(), entity.method_23321(), entity.method_5864())
                     && settled(entity.method_24515())) {
                     entitySignal(entity, SimulationSignals.Kind.REMOVE, spill(entity.method_5864()));
                  }
               }
            }
         }
      }
   }

   public static void onBlockUpdate(class_2626 packet) {
      if (active) {
         block(packet.method_11309(), packet.method_11308());
      }
   }

   public static void onSectionUpdate(class_2637 packet) {
      if (active) {
         packet.method_30621(SimulationSignals::block);
      }
   }

   private static void block(class_2338 pos, class_2680 state) {
      class_638 level = class_310.method_1551().field_1687;
      if (level != null) {
         class_2680 previous = level.method_8320(pos);
         if (previous != state) {
            if (!ignored(previous) && !ignored(state)) {
               if (settled(pos)) {
                  SimulationSignals.Kind kind = ticked(previous, state) ? SimulationSignals.Kind.TICK : SimulationSignals.Kind.BLOCK;
                  signal(pos, kind, 1, () -> blockShare(pos), () -> describe(previous) + " -> " + describe(state));
               }
            }
         }
      }
   }

   static boolean ticked(class_2680 previous, class_2680 state) {
      class_2248 from = previous.method_26204();
      class_2248 to = state.method_26204();
      if (from == to) {
         return true;
      }

      if (!previous.method_26227().method_15769() || !state.method_26227().method_15769()) {
         return true;
      }

      if (previous.method_26164(class_3481.field_15503) && state.method_26215()) {
         return true;
      }

      if (previous.method_26215() && state.method_26164(class_3481.field_15503) && !(Boolean)state.method_61767(class_2741.field_12514, true)) {
         return true;
      }

      if (!previous.method_26164(class_3481.field_15462) || !state.method_26164(class_3481.field_15475) && !state.method_26164(class_3481.field_15503)) {
         if (previous.method_26164(class_3481.field_20341) && state.method_26215()) {
            return true;
         }

         if (from != class_2246.field_10566 || to != class_2246.field_10219 && to != class_2246.field_10402) {
            if (from == class_2246.field_10362 && to == class_2246.field_10566) {
               return true;
            } else if (from == class_2246.field_46287 && to == class_2246.field_46285) {
               return true;
            } else if (from == class_2246.field_46286 && to == class_2246.field_46284) {
               return true;
            } else if (previous.method_26215() && GROWS_FROM_AIR.contains(to)) {
               return true;
            } else {
               return state.method_26215() && GONE_TO_AIR.contains(from) ? true : class_5955.method_34737(from).map(next -> next == to).orElse(false);
            }
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static boolean ignored(class_2680 state) {
      return state.method_27852(class_2246.field_28048)
         || state.method_27852(class_2246.field_28675)
         || state.method_27852(class_2246.field_28676)
         || state.method_27852(class_2246.field_16999)
         || state.method_27852(class_2246.field_20422)
         || state.method_27852(class_2246.field_20421)
         || state.method_27852(class_2246.field_10597)
         || state.method_27852(class_2246.field_22123)
         || state.method_27852(class_2246.field_22124)
         || state.method_27852(class_2246.field_23078)
         || state.method_27852(class_2246.field_23079);
   }

   public static void tick() {
      if (active) {
         class_310 mc = class_310.method_1551();
         class_746 self = mc.field_1724;
         if (mc.field_1687 != null && self != null) {
            long now = now();
            if (self.method_23318() < 0.0) {
               undergroundUntil = now + 3000000000L;
            }

            double x = self.method_23317();
            double z = self.method_23321();
            if (hasLast && square(x - lastX, z - lastZ) > 64.0) {
               startQuiet();
            }

            lastX = x;
            lastZ = z;
            hasLast = true;
            refreshPassed(self);
            if (++ticks % 10 == 0) {
               int pcx = self.method_24515().method_10263() >> 4;
               int pcz = self.method_24515().method_10260() >> 4;
               int start = ringStart();
               boolean sample = !isQuiet() && (!hasSample || square(x - sampleX, z - sampleZ) <= 900.0);
               sampleX = x;
               sampleZ = z;
               hasSample = true;
               Int2DoubleOpenHashMap reach = new Int2DoubleOpenHashMap();
               int view = view();
               FROZEN.clear();

               for (class_1297 entity : mc.field_1687.method_18112()) {
                  if (!(entity instanceof class_1657)) {
                     long seen = LAST_ACTIVE.get(entity.method_5628());
                     if (seen == 0L) {
                        LAST_ACTIVE.put(entity.method_5628(), now);
                     } else if (now - seen > 40000000000L && witness(entity)) {
                        int ex = entity.method_24515().method_10263() >> 4;
                        int ez = entity.method_24515().method_10260() >> 4;
                        int d = distance(ex, ez, pcx, pcz);
                        if (d >= start && d <= view && !nearVisiblePlayer(ex, ez, self, 0)) {
                           FROZEN.add(class_1923.method_8331(ex, ez));
                        }
                     }

                     if (sample && spawnable(entity.method_5864()) && !entity.method_5782() && !entity.method_5765()) {
                        int key = entity.method_5864().method_18387();
                        double distance = Math.hypot(entity.method_23317() - x, entity.method_23321() - z);
                        if (distance > reach.get(key)) {
                           reach.put(key, distance);
                        }
                     }

                     if (distance(entity.method_24515().method_10263() >> 4, entity.method_24515().method_10260() >> 4, pcx, pcz) < start) {
                        ENTITY_INSIDE.put(entity.method_5628(), now);
                     }
                  }
               }

               if (sample) {
                  for (int key : TRACKED_RANGE.keySet().toIntArray()) {
                     TRACKED_RANGE.put(key, Math.max(reach.get(key), TRACKED_RANGE.get(key) - 0.5));
                  }

                  ObjectIterator var22 = reach.int2DoubleEntrySet().iterator();

                  while (var22.hasNext()) {
                     it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry entry = (it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry)var22.next();
                     if (entry.getDoubleValue() > TRACKED_RANGE.get(entry.getIntKey())) {
                        TRACKED_RANGE.put(entry.getIntKey(), entry.getDoubleValue());
                     }
                  }
               }

               if (ticks % 200 == 0) {
                  Iterator<Entry<SimulationSignals.ChunkSignal>> chunks = CHUNKS.long2ObjectEntrySet().iterator();

                  while (chunks.hasNext()) {
                     if (((SimulationSignals.ChunkSignal)chunks.next().getValue()).score(now) < 0.05F) {
                        chunks.remove();
                     }
                  }

                  ENTITY_SHARES.values().removeIf(share -> share.amount(now) < 0.05F);
                  BLOCK_SHARES.values().removeIf(share -> share.amount(now) < 0.05F);
                  int keep = view() + 4;
                  CHUNK_LOADED.long2LongEntrySet()
                     .removeIf(e -> distance(class_1923.method_8325(e.getLongKey()), class_1923.method_8332(e.getLongKey()), pcx, pcz) > keep);
                  ENTITY_ADDED.int2LongEntrySet().removeIf(e -> now - e.getLongValue() > 60000000000L);
                  ENTITY_INSIDE.int2LongEntrySet().removeIf(e -> now - e.getLongValue() > 60000000000L);
                  LIGHTNING_PENDING.values().removeIf(bolt -> now - bolt[1] > 3000000000L);
                  LAST_ACTIVE.keySet().removeIf(id -> mc.field_1687.method_8469(id) == null);
               }
            }
         }
      }
   }

   private static boolean witness(class_1297 entity) {
      return entity instanceof class_1314 mob
         && !(mob instanceof class_1606)
         && !mob.method_5987()
         && !mob.method_5765()
         && !mob.method_5782()
         && !mob.method_6113();
   }

   private static void pin(class_2338 pos, String detail) {
      class_746 self = class_310.method_1551().field_1724;
      if (self != null && !holding() && pos.method_10264() >= 0) {
         if (PINS.size() >= 32) {
            PINS.poll();
         }

         PINS.add(new SimulationSignals.Pin(pos.method_10062(), detail + " @ " + pos.method_10263() + " " + pos.method_10264() + " " + pos.method_10260()));
      }
   }

   private static void refreshPassed(class_746 self) {
      int pcx = self.method_24515().method_10263() >> 4;
      int pcz = self.method_24515().method_10260() >> 4;
      long center = class_1923.method_8331(pcx, pcz);
      int radius = ringStart() - 1 + 1;
      if (center != passedCenter || radius != passedRadius) {
         passedCenter = center;
         passedRadius = radius;

         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               long key = class_1923.method_8331(pcx + dx, pcz + dz);
               if (passed.add(key)) {
                  CHUNKS.remove(key);
               }
            }
         }
      }
   }

   private static void entitySignal(class_1297 entity, SimulationSignals.Kind kind, int spill) {
      if (!(entity instanceof class_1657)) {
         if (!(entity.method_31483() instanceof class_1657)) {
            if (!(entity instanceof class_1540 falling && falling.method_6962().method_27852(class_2246.field_28048))) {
               class_746 self = class_310.method_1551().field_1724;
               if (self != null) {
                  if (entity instanceof class_6025 ownable) {
                     class_10583<?> owner = ownable.method_66287();
                     if (owner != null && self.method_5667().equals(owner.method_66263())) {
                        return;
                     }
                  }

                  if (now() - ENTITY_INSIDE.getOrDefault(entity.method_5628(), -4611686018427387904L) >= 60000000000L) {
                     signal(entity.method_24515(), kind, spill, () -> entityShare(entity.method_5628()), () -> name(entity.method_5864()));
                  }
               }
            }
         }
      }
   }

   private static SimulationSignals.Share entityShare(int id) {
      return (SimulationSignals.Share)ENTITY_SHARES.computeIfAbsent(id, key -> new SimulationSignals.Share());
   }

   private static SimulationSignals.Share blockShare(class_2338 pos) {
      return (SimulationSignals.Share)BLOCK_SHARES.computeIfAbsent(pos.method_10063(), key -> new SimulationSignals.Share());
   }

   private static String name(class_1299<?> type) {
      return class_7923.field_41177.method_10221(type).method_12832();
   }

   private static String describe(class_2680 state) {
      return state.toString().replace("Block{minecraft:", "").replace("}", "");
   }

   private static void signal(class_2338 pos, SimulationSignals.Kind kind, int spill, Supplier<SimulationSignals.Share> source, Supplier<String> what) {
      class_746 self = class_310.method_1551().field_1724;
      if (self != null) {
         if (pos.method_10264() >= 0 && !holding()) {
            int cx = pos.method_10263() >> 4;
            int cz = pos.method_10260() >> 4;
            int pcx = self.method_24515().method_10263() >> 4;
            int pcz = self.method_24515().method_10260() >> 4;
            int d = distance(cx, cz, pcx, pcz);
            if (d >= ringStart() + spill && d <= view()) {
               long key = class_1923.method_8331(cx, cz);
               if (!passed.contains(key)) {
                  if (!ignoreVisiblePlayers || !nearVisiblePlayer(cx, cz, self, spill)) {
                     long now = now();
                     float weight = source.get().take(kind.weight, kind.cap, now);
                     if (!(weight <= 0.0F)) {
                        String detail = kind.title + " " + what.get() + " @ " + pos.method_10263() + " " + pos.method_10264() + " " + pos.method_10260();
                        ((SimulationSignals.ChunkSignal)CHUNKS.computeIfAbsent(key, k -> new SimulationSignals.ChunkSignal())).add(kind, now, weight, detail);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean nearVisiblePlayer(int cx, int cz, class_746 self, int spill) {
      class_638 level = class_310.method_1551().field_1687;
      if (level == null) {
         return false;
      }

      int reach = simulation() + 1 + spill;

      for (class_1657 player : level.method_18456()) {
         if (player != self && distance(cx, cz, player.method_24515().method_10263() >> 4, player.method_24515().method_10260() >> 4) <= reach) {
            return true;
         }
      }

      return false;
   }

   private static boolean settled(class_2338 pos) {
      long loaded = CHUNK_LOADED.getOrDefault(class_1923.method_8331(pos.method_10263() >> 4, pos.method_10260() >> 4), Long.MIN_VALUE);
      return loaded == Long.MIN_VALUE || now() - loaded > 3000000000L;
   }

   private static boolean wellInsideTracking(double x, double z, class_1299<?> type) {
      class_746 self = class_310.method_1551().field_1724;
      if (self == null) {
         return false;
      }

      double range = Math.min(TRACKED_RANGE.get(type.method_18387()), view() * 16.0);
      return Math.hypot(x - self.method_23317(), z - self.method_23321()) < range - 24.0;
   }

   private static boolean spawnable(class_1299<?> type) {
      return type.method_5891() == class_1311.field_6302 || type == class_1299.field_6052 || type == class_1299.field_6044;
   }

   private static int spill(class_1299<?> type) {
      return type != class_1299.field_6052 && type != class_1299.field_6044 ? 0 : 1;
   }

   private static void startQuiet() {
      quietUntil = now() + 2000000000L;
   }

   private static boolean isQuiet() {
      return now() < quietUntil;
   }

   private static double square(double dx, double dz) {
      return dx * dx + dz * dz;
   }

   public static int distance(int ax, int az, int bx, int bz) {
      return Math.max(Math.abs(ax - bx), Math.abs(az - bz));
   }

   @Environment(EnvType.CLIENT)
   public static final class ChunkSignal {
      private float score;
      private long updated;
      private int kinds;
      private final int[] counts = new int[SimulationSignals.Kind.values().length];
      private final ArrayDeque<String> recent = new ArrayDeque<>();

      public float score(long now) {
         return this.score * SimulationSignals.decay(now - this.updated);
      }

      public boolean has(SimulationSignals.Kind kind) {
         return (this.kinds & 1 << kind.ordinal()) != 0;
      }

      public int count(SimulationSignals.Kind kind) {
         return this.counts[kind.ordinal()];
      }

      public List<String> recent() {
         return List.copyOf(this.recent);
      }

      void add(SimulationSignals.Kind kind, long now, float weight, String detail) {
         this.recent.addLast(detail);
         if (this.recent.size() > 6) {
            this.recent.removeFirst();
         }

         this.score = this.score(now) + weight;
         this.updated = now;
         this.kinds = this.kinds | 1 << kind.ordinal();
         this.counts[kind.ordinal()]++;
      }
   }

   @Environment(EnvType.CLIENT)
   public enum Kind {
      MOVE("Mob Move", 2.5F, true),
      HEAD("Head Turn", 2.5F, true),
      SPAWN("Spawn / Drop", 2.0F, false),
      REMOVE("Despawn / Pickup", 1.5F, false),
      STATE("Entity State", 0.5F, false),
      EVENT("Entity Event", 5.0F, true),
      BLOCK("Block Change", 1.6F, false),
      TICK("Tick Change", 10.0F, true),
      LIGHTNING("Lightning", 10.0F, true),
      HURT("Hurt", 5.0F, true),
      EQUIP("Equipment", 5.0F, true);

      public final String title;
      final float weight;
      final float cap;

      Kind(String title, float weight, boolean ticked) {
         this.title = title;
         this.weight = weight;
         this.cap = ticked ? 10.0F : 4.0F;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Pin(class_2338 pos, String detail) {
   }

   @Environment(EnvType.CLIENT)
   private static final class Share {
      private float amount;
      private long updated;

      float amount(long now) {
         return this.amount * SimulationSignals.decay(now - this.updated);
      }

      float take(float weight, float cap, long now) {
         float held = this.amount(now);
         float granted = Math.min(weight, Math.max(cap, weight) - held);
         if (granted <= 0.0F) {
            return 0.0F;
         }

         this.amount = held + granted;
         this.updated = now;
         return granted;
      }
   }
}
