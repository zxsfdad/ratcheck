package net.anubis.client.module.impl.render;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongPredicate;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.BlockUpdateEvent;
import net.anubis.client.event.impl.PacketReceiveNettyEvent;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.gui.ChallengeToast;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.anubis.client.util.ChunkAreas;
import net.anubis.client.util.ChunkOverlay;
import net.anubis.client.util.LevelUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_156;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1923;
import net.minecraft.class_1944;
import net.minecraft.class_2231;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2269;
import net.minecraft.class_2309;
import net.minecraft.class_2312;
import net.minecraft.class_2315;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2377;
import net.minecraft.class_2401;
import net.minecraft.class_2426;
import net.minecraft.class_2428;
import net.minecraft.class_2436;
import net.minecraft.class_2453;
import net.minecraft.class_2457;
import net.minecraft.class_2459;
import net.minecraft.class_2537;
import net.minecraft.class_2561;
import net.minecraft.class_2665;
import net.minecraft.class_2667;
import net.minecraft.class_2671;
import net.minecraft.class_2672;
import net.minecraft.class_2676;
import net.minecraft.class_2678;
import net.minecraft.class_2680;
import net.minecraft.class_2804;
import net.minecraft.class_2806;
import net.minecraft.class_2818;
import net.minecraft.class_2826;
import net.minecraft.class_310;
import net.minecraft.class_3562;
import net.minecraft.class_4076;
import net.minecraft.class_4850;
import net.minecraft.class_5803;
import net.minecraft.class_631;
import net.minecraft.class_638;
import net.minecraft.class_6606;
import net.minecraft.class_8886;
import net.minecraft.class_9848;
import net.minecraft.class_2338.class_2339;

@Environment(EnvType.CLIENT)
public final class PrimeChunkFinderModule extends ClientModule {
   private static final int SECTION_BYTES = 2048;
   private static final int MAX_FLAGGED_CHUNKS = 256;
   private static final int AMETHYST_MAX_LIGHT = 5;
   private static final int AMETHYST_REACH = 5;
   private static final int ACTIVE_CHANGES = 5;
   private static final int NOTIFY_AREA = 3;
   private static final int ORE_LIGHT = 9;
   private static final long[] NO_ORES = new long[0];
   private static final int LAMP_LIGHT = 14;
   private static final int[] NO_LAMPS = new int[0];
   private static final long GONE_LAMP_MS = 5000L;
   private static final int MAX_TRACKED_CHUNKS = 5000;
   private static final int SNAPSHOT_RADIUS = 32;
   private static final byte[] ZERO_SECTION = new byte[2048];
   private static final int MAX_Y = 0;
   private final BooleanSetting skyLight = this.addSetting("sky-light", new BooleanSetting("Sky Light", true));
   private final ColorSetting chunkColor = this.addSetting("chunk-color", new ColorSetting("Chunk Color", 1342242650));
   private final BooleanSetting tracers = this.addSetting("tracers", new BooleanSetting("Tracers", true));
   private final BooleanSetting sound = this.addSetting("sound", new BooleanSetting("Sound", true));
   private final LinkedHashMap<Long, Map<Integer, byte[]>> chunkLight = new LinkedHashMap<>(256, 0.75F, true);
   private final LinkedHashMap<Long, Map<Integer, byte[]>> chunkSky = new LinkedHashMap<>(256, 0.75F, true);
   private final LongOpenHashSet passedChunks = new LongOpenHashSet();
   private final LongLinkedOpenHashSet flaggedChunks = new LongLinkedOpenHashSet();
   private final LongOpenHashSet shownChunks = new LongOpenHashSet();
   private final ChunkAreas chunkAreas = new ChunkAreas();
   private final ChunkOverlay chunkOverlay = new ChunkOverlay();
   private final LongOpenHashSet checkedSections = new LongOpenHashSet();
   private final LongOpenHashSet amethystSections = new LongOpenHashSet();
   private final Long2ObjectOpenHashMap<long[]> oreSections = new Long2ObjectOpenHashMap();
   private final Long2ObjectOpenHashMap<int[]> lampSections = new Long2ObjectOpenHashMap();
   private final Long2LongOpenHashMap goneLamps = new Long2LongOpenHashMap();
   private final class_2339 cursor = new class_2339();
   private final Long2IntOpenHashMap changes = new Long2IntOpenHashMap();

   public PrimeChunkFinderModule() {
      super("prime-chunk-finder", "Chunk Finder", "Flags chunks with light and redstone changes below Y 0.", ModuleCategory.BASE_RAID);
      BooleanSetting notifications = this.usesNotifications();
      this.sound.visibleWhen(notifications::get);
      this.skyLight.onChange(this::skyLightChanged);
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      this.reset();
      this.snapshotLoadedChunks(true);
      this.on(PacketReceiveNettyEvent.TYPE, this::onPacket);
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   private void onPacket(PacketReceiveNettyEvent event) {
      class_310 mc = class_310.method_1551();
      if (event.packet instanceof class_2678) {
         mc.execute(() -> {
            if (this.isEnabled()) {
               this.reset();
            }
         });
      } else if (event.packet instanceof class_2672 packet) {
         int chunkX = packet.method_11523();
         int chunkZ = packet.method_11524();
         class_6606 data = packet.method_38599();
         mc.execute(() -> {
            if (this.isEnabled() && mc.field_1687 != null) {
               this.baselineChunk(chunkX, chunkZ, data);
            }
         });
      } else if (event.packet instanceof class_2676 packet) {
         int chunkX = packet.method_11558();
         int chunkZ = packet.method_11554();
         class_6606 data = packet.method_38600();
         mc.execute(() -> {
            if (this.isEnabled() && mc.field_1687 != null) {
               this.detectChanges(chunkX, chunkZ, data);
            }
         });
      }
   }

   @EventHandler
   public void onBlockUpdate(BlockUpdateEvent event) {
      if (event.oldState != event.newState && class_310.method_1551().field_1687 != null) {
         this.rememberGoneLamp(event.pos, event.oldState, event.newState);
         this.detectRedstone(event.pos, event.oldState, event.newState);
      }
   }

   @EventHandler(stage = Stage.POST)
   public void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1687 == null) {
         this.reset();
      } else {
         if (mc.field_1724 != null && mc.field_1724.method_23318() < 0.0) {
            this.passedChunks.add(mc.field_1724.method_31476().method_8324());
         }

         this.forgetOldLamps();
         this.refreshShown(mc.field_1687);
      }
   }

   private void refreshShown(class_638 level) {
      this.shownChunks.clear();
      LongListIterator var2 = this.flaggedChunks.iterator();

      while (var2.hasNext()) {
         long key = (Long)var2.next();
         if (LevelUtil.isLoaded(level, class_1923.method_8325(key), class_1923.method_8332(key))) {
            this.shownChunks.add(key);
         }
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      if (!this.shownChunks.isEmpty() && class_310.method_1551().field_1687 != null) {
         int fill = this.chunkColor.argb();
         this.chunkOverlay.draw(this.shownChunks, fill, event.partialTick);
         if (this.tracers.get()) {
            this.chunkAreas.drawTracers(this.shownChunks, 63.0, class_9848.method_61334(fill));
         }
      }
   }

   private void flagChunk(long key) {
      this.flaggedChunks.addAndMoveToLast(key);
      this.shownChunks.add(key);

      while (this.flaggedChunks.size() > 256) {
         this.flaggedChunks.removeFirstLong();
      }
   }

   private void skyLightChanged() {
      this.chunkSky.clear();
      if (this.isEnabled() && this.skyLight.get()) {
         this.snapshotLoadedChunks(false);
      }
   }

   private void snapshotLoadedChunks(boolean block) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1687 != null && mc.field_1724 != null) {
         int centerX = mc.field_1724.method_31477() >> 4;
         int centerZ = mc.field_1724.method_31479() >> 4;

         for (int chunkX = centerX - 32; chunkX <= centerX + 32; chunkX++) {
            for (int chunkZ = centerZ - 32; chunkZ <= centerZ + 32; chunkZ++) {
               long key = class_1923.method_8331(chunkX, chunkZ);
               Map<Integer, byte[]> sections = block ? this.snapshotChunk(class_1944.field_9282, chunkX, chunkZ) : null;
               if (sections != null) {
                  this.chunkLight.put(key, sections);
               }

               Map<Integer, byte[]> sky = this.skyLight.get() ? this.snapshotChunk(class_1944.field_9284, chunkX, chunkZ) : null;
               if (sky != null) {
                  this.chunkSky.put(key, sky);
               }
            }
         }

         this.trimChunkCache();
      }
   }

   private Map<Integer, byte[]> snapshotChunk(class_1944 layer, int chunkX, int chunkZ) {
      class_638 level = class_310.method_1551().field_1687;
      if (level == null) {
         return null;
      }

      class_631 chunkSource = level.method_2935();
      if (!chunkSource.method_12123(chunkX, chunkZ)) {
         return null;
      }

      class_3562 light = chunkSource.method_12130().method_15562(layer);
      Map<Integer, byte[]> sections = layer == class_1944.field_9284 ? new HashMap<>() : this.createZeroedChunk();

      for (int sectionY = this.bottomSection(); sectionY <= this.topSection(); sectionY++) {
         class_2804 data = light.method_15544(class_4076.method_18676(chunkX, sectionY, chunkZ));
         if (data != null) {
            sections.put(sectionY, data.method_51379(0) ? ZERO_SECTION : Arrays.copyOf(data.method_12137(), 2048));
         }
      }

      return sections;
   }

   private void baselineChunk(int chunkX, int chunkZ, class_6606 data) {
      long key = class_1923.method_8331(chunkX, chunkZ);
      Map<Integer, byte[]> sections = this.createZeroedChunk();
      this.applySections(class_1944.field_9282, data, sections, chunkX, chunkZ, false, null);
      this.chunkLight.put(key, sections);
      if (this.skyLight.get()) {
         Map<Integer, byte[]> sky = new HashMap<>();
         this.applySections(class_1944.field_9284, data, sky, chunkX, chunkZ, false, null);
         this.chunkSky.put(key, sky);
      }

      this.trimChunkCache();
   }

   private void detectChanges(int chunkX, int chunkZ, class_6606 data) {
      long key = class_1923.method_8331(chunkX, chunkZ);
      this.checkedSections.clear();
      this.amethystSections.clear();
      this.oreSections.clear();
      this.lampSections.clear();
      Map<Integer, byte[]> sections = this.chunkLight.get(key);
      if (sections == null) {
         sections = this.snapshotChunk(class_1944.field_9282, chunkX, chunkZ);
      }

      boolean compare = sections != null && !this.isOwnChunk(key);
      if (sections == null) {
         sections = this.createZeroedChunk();
      }

      PrimeChunkFinderModule.ChangeSummary summary = new PrimeChunkFinderModule.ChangeSummary(false);
      this.applySections(class_1944.field_9282, data, sections, chunkX, chunkZ, compare, summary);
      this.chunkLight.put(key, sections);
      PrimeChunkFinderModule.ChangeSummary sky = this.skyLight.get() ? this.skyChanges(key, chunkX, chunkZ, data) : null;
      this.trimChunkCache();
      if (summary.count == 0 && sky != null) {
         summary = sky;
      }

      if (summary.count != 0) {
         this.activity(key, class_1802.field_8733, summary.sky ? "Sky " : "", summary.x, summary.y, summary.z);
      }
   }

   private PrimeChunkFinderModule.ChangeSummary skyChanges(long key, int chunkX, int chunkZ, class_6606 data) {
      Map<Integer, byte[]> sections = this.chunkSky.get(key);
      if (sections == null) {
         sections = this.snapshotChunk(class_1944.field_9284, chunkX, chunkZ);
      }

      boolean compare = sections != null && !this.nearOwnChunk(key);
      if (sections == null) {
         sections = new HashMap<>();
      }

      PrimeChunkFinderModule.ChangeSummary summary = new PrimeChunkFinderModule.ChangeSummary(true);
      this.applySections(class_1944.field_9284, data, sections, chunkX, chunkZ, compare, summary);
      this.chunkSky.put(key, sections);
      return summary;
   }

   private void rememberGoneLamp(class_2338 pos, class_2680 previous, class_2680 state) {
      if (isLamp(previous)) {
         int light = previous.method_26213();
         if (!isLamp(state) || state.method_26213() < light) {
            this.goneLamps.put(pos.method_10063(), class_156.method_658() + 5000L << 4 | light);
         }
      }
   }

   private void forgetOldLamps() {
      if (!this.goneLamps.isEmpty()) {
         long now = class_156.method_658();
         LongIterator it = this.goneLamps.values().iterator();

         while (it.hasNext()) {
            if (it.nextLong() >>> 4 < now) {
               it.remove();
            }
         }
      }
   }

   private void detectRedstone(class_2338 pos, class_2680 previous, class_2680 state) {
      if (pos.method_10264() < 0 && (isRedstone(state) || isRedstone(previous))) {
         long key = class_1923.method_8331(pos.method_10263() >> 4, pos.method_10260() >> 4);
         if (!this.isOwnChunk(key)) {
            this.activity(key, class_1802.field_8733, "", pos.method_10263(), pos.method_10264(), pos.method_10260());
         }
      }
   }

   private void activity(long key, class_1792 icon, String label, int x, int y, int z) {
      boolean fresh = !nearby(key, this.flaggedChunks::contains);
      this.flagChunk(key);
      int count = this.changes.addTo(key, 1) + 1;
      if (this.notificationsOn()) {
         String at = label + x + ", " + y + ", " + z;
         if (fresh) {
            ChallengeToast.addOrUpdate(new class_1799(icon), this.name(), class_2561.method_43470(at), this.sound.get());
         } else if (count == 5 && !nearby(key, other -> other != key && this.changes.get(other) >= 5)) {
            ChallengeToast.addOrUpdate(new class_1799(icon), this.name(), class_2561.method_43470("Active " + at), this.sound.get());
         }
      }
   }

   private static boolean nearby(long key, LongPredicate test) {
      int chunkX = class_1923.method_8325(key);
      int chunkZ = class_1923.method_8332(key);

      for (int dx = -3; dx <= 3; dx++) {
         for (int dz = -3; dz <= 3; dz++) {
            if (test.test(class_1923.method_8331(chunkX + dx, chunkZ + dz))) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean isRedstone(class_2680 state) {
      class_2248 block = state.method_26204();
      return block instanceof class_2457
         || block instanceof class_2312
         || block instanceof class_2459
         || block instanceof class_2453
         || block instanceof class_2426
         || block instanceof class_2665
         || block instanceof class_2671
         || block instanceof class_2667
         || block instanceof class_2315
         || block instanceof class_2377
         || block instanceof class_2428
         || block instanceof class_2309
         || block instanceof class_2401
         || block instanceof class_2269
         || block instanceof class_2231
         || block instanceof class_2537
         || block instanceof class_4850
         || block instanceof class_8886
         || block instanceof class_2436;
   }

   private boolean isOwnChunk(long key) {
      if (this.passedChunks.contains(key)) {
         return true;
      }

      class_310 mc = class_310.method_1551();
      return mc.field_1724 != null && mc.field_1724.method_31476().method_8324() == key;
   }

   private boolean nearOwnChunk(long key) {
      int chunkX = class_1923.method_8325(key);
      int chunkZ = class_1923.method_8332(key);

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            if (this.isOwnChunk(class_1923.method_8331(chunkX + dx, chunkZ + dz))) {
               return true;
            }
         }
      }

      return false;
   }

   private void applySections(
      class_1944 layer, class_6606 data, Map<Integer, byte[]> sections, int chunkX, int chunkZ, boolean compare, PrimeChunkFinderModule.ChangeSummary summary
   ) {
      boolean sky = layer == class_1944.field_9284;
      BitSet mask = sky ? data.method_38601() : data.method_38608();
      List<byte[]> updates = sky ? data.method_38606() : data.method_38610();
      int next = 0;

      for (int bit = mask.nextSetBit(0); bit >= 0 && next < updates.size(); bit = mask.nextSetBit(bit + 1)) {
         byte[] update = updates.get(next++);
         int sectionY = this.lightSectionY(bit);
         if (this.isTrackedSection(sectionY) && update.length == 2048) {
            this.updateSection(sections, chunkX, chunkZ, sectionY, Arrays.copyOf(update, 2048), compare, summary);
         }
      }

      BitSet empty = sky ? data.method_38604() : data.method_38609();

      for (int bit = empty.nextSetBit(0); bit >= 0; bit = empty.nextSetBit(bit + 1)) {
         int sectionY = this.lightSectionY(bit);
         if (this.isTrackedSection(sectionY)) {
            this.updateSection(sections, chunkX, chunkZ, sectionY, ZERO_SECTION, compare, summary);
         }
      }
   }

   private void updateSection(
      Map<Integer, byte[]> sections, int chunkX, int chunkZ, int sectionY, byte[] light, boolean compare, PrimeChunkFinderModule.ChangeSummary summary
   ) {
      byte[] previous = sections.get(sectionY);
      if (compare && previous != null && summary != null && !isDark(previous) && !isDark(light)) {
         this.compareSection(previous, light, chunkX, chunkZ, sectionY, summary);
      }

      sections.put(sectionY, light);
   }

   private static boolean isDark(byte[] light) {
      if (light == ZERO_SECTION) {
         return true;
      }

      for (byte value : light) {
         if (value != 0) {
            return false;
         }
      }

      return true;
   }

   private void compareSection(byte[] previous, byte[] light, int chunkX, int chunkZ, int sectionY, PrimeChunkFinderModule.ChangeSummary summary) {
      for (int i = 0; i < 2048; i++) {
         int was = previous[i] & 255;
         int now = light[i] & 255;
         int changed = was ^ now;
         if ((changed & 15) != 0) {
            this.recordChange(chunkX, chunkZ, sectionY, i << 1, was & 15, now & 15, summary);
         }

         if ((changed & 240) != 0) {
            this.recordChange(chunkX, chunkZ, sectionY, (i << 1) + 1, was >>> 4 & 15, now >>> 4 & 15, summary);
         }
      }
   }

   private void recordChange(int chunkX, int chunkZ, int sectionY, int index, int oldValue, int newValue, PrimeChunkFinderModule.ChangeSummary summary) {
      int x = (chunkX << 4) + (index & 15);
      int y = (sectionY << 4) + (index >>> 8 & 15);
      int z = (chunkZ << 4) + (index >>> 4 & 15);
      if (y < 0) {
         int value = Math.max(oldValue, newValue);
         if (summary.sky || !this.fromAmethyst(x, y, z, oldValue, newValue) && !this.fromRedstoneOre(x, y, z, value) && !this.fromLamp(x, y, z, value)) {
            summary.add(x, y, z, oldValue, newValue);
         }
      }
   }

   private boolean fromLamp(int x, int y, int z, int value) {
      if (value > 14) {
         return false;
      }

      class_638 level = class_310.method_1551().field_1687;
      if (level == null) {
         return false;
      }

      int reach = 14 - value;

      for (int sx = x - reach >> 4; sx <= x + reach >> 4; sx++) {
         for (int sy = y - reach >> 4; sy <= y + reach >> 4; sy++) {
            for (int sz = z - reach >> 4; sz <= z + reach >> 4; sz++) {
               for (int lamp : this.lamps(level, sx, sy, sz)) {
                  int index = lamp >>> 4;
                  int lx = (sx << 4) + (index & 15);
                  int ly = (sy << 4) + (index >>> 8);
                  int lz = (sz << 4) + (index >>> 4 & 15);
                  if (Math.abs(lx - x) + Math.abs(ly - y) + Math.abs(lz - z) <= (lamp & 15) - value) {
                     return true;
                  }
               }
            }
         }
      }

      long now = class_156.method_658();
      ObjectIterator var19 = this.goneLamps.long2LongEntrySet().iterator();

      while (var19.hasNext()) {
         Entry gone = (Entry)var19.next();
         long packed = gone.getLongValue();
         if (packed >>> 4 >= now) {
            long pos = gone.getLongKey();
            int distance = Math.abs(class_2338.method_10061(pos) - x) + Math.abs(class_2338.method_10071(pos) - y) + Math.abs(class_2338.method_10083(pos) - z);
            if (distance <= (int)(packed & 15L) - value) {
               return true;
            }
         }
      }

      return false;
   }

   private int[] lamps(class_638 level, int sectionX, int sectionY, int sectionZ) {
      long key = class_4076.method_18685(sectionX, sectionY, sectionZ);
      int[] cached = (int[])this.lampSections.get(key);
      if (cached != null) {
         return cached;
      }

      int[] found = scanLamps(level, sectionX, sectionY, sectionZ);
      this.lampSections.put(key, found);
      return found;
   }

   private static int[] scanLamps(class_638 level, int sectionX, int sectionY, int sectionZ) {
      if (sectionY >= level.method_32891() && sectionY <= level.method_31597()) {
         class_2818 chunk = level.method_2935().method_2857(sectionX, sectionZ, class_2806.field_12803, false);
         if (chunk == null) {
            return NO_LAMPS;
         }

         class_2826 section = chunk.method_38259(chunk.method_31603(sectionY));
         if (!section.method_38292() && section.method_12265().method_19526(PrimeChunkFinderModule::isLamp)) {
            IntArrayList lamps = new IntArrayList();

            for (int index = 0; index < 4096; index++) {
               class_2680 state = section.method_12254(index & 15, index >>> 8, index >>> 4 & 15);
               if (isLamp(state)) {
                  lamps.add(index << 4 | state.method_26213());
               }
            }

            return lamps.isEmpty() ? NO_LAMPS : lamps.toIntArray();
         } else {
            return NO_LAMPS;
         }
      } else {
         return NO_LAMPS;
      }
   }

   private static boolean isLamp(class_2680 state) {
      return state.method_27852(class_2246.field_10336)
         || state.method_27852(class_2246.field_10099)
         || state.method_27852(class_2246.field_22092)
         || state.method_27852(class_2246.field_22093)
         || state.method_27852(class_2246.field_61902)
         || state.method_27852(class_2246.field_61903)
         || class_5803.method_33618(state);
   }

   private boolean fromAmethyst(int x, int y, int z, int oldValue, int newValue) {
      if (Math.max(oldValue, newValue) > 5) {
         return false;
      }

      class_638 level = class_310.method_1551().field_1687;
      if (level != null && this.amethystSectionNear(level, x, y, z)) {
         for (int dx = -5; dx <= 5; dx++) {
            int restX = 5 - Math.abs(dx);

            for (int dy = -restX; dy <= restX; dy++) {
               int restY = restX - Math.abs(dy);

               for (int dz = -restY; dz <= restY; dz++) {
                  if (isAmethyst(level.method_8320(this.cursor.method_10103(x + dx, y + dy, z + dz)))) {
                     return true;
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean amethystSectionNear(class_638 level, int x, int y, int z) {
      for (int sx = x - 5 >> 4; sx <= x + 5 >> 4; sx++) {
         for (int sy = y - 5 >> 4; sy <= y + 5 >> 4; sy++) {
            for (int sz = z - 5 >> 4; sz <= z + 5 >> 4; sz++) {
               long key = class_4076.method_18685(sx, sy, sz);
               if (this.checkedSections.add(key) && sectionHasAmethyst(level, sx, sy, sz)) {
                  this.amethystSections.add(key);
               }

               if (this.amethystSections.contains(key)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private static boolean sectionHasAmethyst(class_638 level, int sectionX, int sectionY, int sectionZ) {
      if (sectionY >= level.method_32891() && sectionY <= level.method_31597()) {
         class_2818 chunk = level.method_2935().method_2857(sectionX, sectionZ, class_2806.field_12803, false);
         if (chunk == null) {
            return false;
         }

         class_2826 section = chunk.method_38259(chunk.method_31603(sectionY));
         return !section.method_38292() && section.method_12265().method_19526(PrimeChunkFinderModule::isAmethyst);
      } else {
         return false;
      }
   }

   private boolean fromRedstoneOre(int x, int y, int z, int value) {
      if (value > 9) {
         return false;
      }

      class_638 level = class_310.method_1551().field_1687;
      if (level == null) {
         return false;
      }

      int reach = 9 - value;

      for (int sx = x - reach >> 4; sx <= x + reach >> 4; sx++) {
         for (int sy = y - reach >> 4; sy <= y + reach >> 4; sy++) {
            for (int sz = z - reach >> 4; sz <= z + reach >> 4; sz++) {
               for (long ore : this.exposedOres(level, sx, sy, sz)) {
                  int distance = Math.abs(class_2338.method_10061(ore) - x)
                     + Math.abs(class_2338.method_10071(ore) - y)
                     + Math.abs(class_2338.method_10083(ore) - z);
                  if (distance <= reach) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private long[] exposedOres(class_638 level, int sectionX, int sectionY, int sectionZ) {
      long key = class_4076.method_18685(sectionX, sectionY, sectionZ);
      long[] cached = (long[])this.oreSections.get(key);
      if (cached != null) {
         return cached;
      }

      long[] found = this.scanExposedOres(level, sectionX, sectionY, sectionZ);
      this.oreSections.put(key, found);
      return found;
   }

   private long[] scanExposedOres(class_638 level, int sectionX, int sectionY, int sectionZ) {
      if (sectionY >= level.method_32891() && sectionY <= level.method_31597()) {
         class_2818 chunk = level.method_2935().method_2857(sectionX, sectionZ, class_2806.field_12803, false);
         if (chunk == null) {
            return NO_ORES;
         }

         class_2826 section = chunk.method_38259(chunk.method_31603(sectionY));
         if (!section.method_38292() && section.method_12265().method_19526(PrimeChunkFinderModule::isRedstoneOre)) {
            LongArrayList ores = new LongArrayList();

            for (int ly = 0; ly < 16; ly++) {
               for (int lz = 0; lz < 16; lz++) {
                  for (int lx = 0; lx < 16; lx++) {
                     if (isRedstoneOre(section.method_12254(lx, ly, lz))) {
                        int wx = (sectionX << 4) + lx;
                        int wy = (sectionY << 4) + ly;
                        int wz = (sectionZ << 4) + lz;
                        if (this.exposed(level, wx, wy, wz)) {
                           ores.add(class_2338.method_10064(wx, wy, wz));
                        }
                     }
                  }
               }
            }

            return ores.isEmpty() ? NO_ORES : ores.toLongArray();
         } else {
            return NO_ORES;
         }
      } else {
         return NO_ORES;
      }
   }

   private boolean exposed(class_638 level, int x, int y, int z) {
      for (class_2350 direction : class_2350.values()) {
         class_2680 neighbour = level.method_8320(
            this.cursor.method_10103(x + direction.method_10148(), y + direction.method_10164(), z + direction.method_10165())
         );
         if (neighbour.method_26215() || !neighbour.method_26225()) {
            return true;
         }
      }

      return false;
   }

   private static boolean isRedstoneOre(class_2680 state) {
      return state.method_27852(class_2246.field_10080) || state.method_27852(class_2246.field_29030);
   }

   private static boolean isAmethyst(class_2680 state) {
      return state.method_27852(class_2246.field_27161)
         || state.method_27852(class_2246.field_27162)
         || state.method_27852(class_2246.field_27163)
         || state.method_27852(class_2246.field_27164)
         || state.method_27852(class_2246.field_27160)
         || state.method_27852(class_2246.field_27159);
   }

   private Map<Integer, byte[]> createZeroedChunk() {
      Map<Integer, byte[]> sections = new HashMap<>();

      for (int sectionY = this.bottomSection(); sectionY <= this.topSection(); sectionY++) {
         sections.put(sectionY, ZERO_SECTION);
      }

      return sections;
   }

   private int lightSectionY(int index) {
      class_638 level = class_310.method_1551().field_1687;
      return level == null ? Integer.MIN_VALUE : level.method_2935().method_12130().method_31929() + index;
   }

   private int bottomSection() {
      return class_310.method_1551().field_1687.method_32891();
   }

   private int topSection() {
      return Math.min(class_310.method_1551().field_1687.method_31597(), class_4076.method_18675(-1));
   }

   private boolean isTrackedSection(int sectionY) {
      return sectionY >= this.bottomSection() && sectionY <= this.topSection();
   }

   private void trimChunkCache() {
      trimCache(this.chunkLight);
      trimCache(this.chunkSky);
   }

   private static void trimCache(LinkedHashMap<Long, Map<Integer, byte[]>> cache) {
      while (cache.size() > 5000) {
         Iterator<Long> keys = cache.keySet().iterator();
         keys.next();
         keys.remove();
      }
   }

   private void reset() {
      this.chunkLight.clear();
      this.chunkSky.clear();
      this.passedChunks.clear();
      this.flaggedChunks.clear();
      this.shownChunks.clear();
      this.changes.clear();
      this.lampSections.clear();
      this.goneLamps.clear();
   }

   @Environment(EnvType.CLIENT)
   private static final class ChangeSummary {
      final boolean sky;
      int count;
      int x;
      int y;
      int z;
      int oldValue;
      int newValue;

      ChangeSummary(boolean sky) {
         this.sky = sky;
      }

      void add(int x, int y, int z, int oldValue, int newValue) {
         if (this.count == 0 || Math.abs(newValue - oldValue) > Math.abs(this.newValue - this.oldValue)) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.oldValue = oldValue;
            this.newValue = newValue;
         }

         this.count++;
      }
   }
}
