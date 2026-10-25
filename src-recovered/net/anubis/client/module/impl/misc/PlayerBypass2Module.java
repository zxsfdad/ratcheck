package net.anubis.client.module.impl.misc;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.anubis.client.chat.ModuleChat;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.gui.ChallengeToast;
import net.anubis.client.gui.GlintHeadRenderer;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.FoglessLines;
import net.anubis.client.util.playerbypass2.SimulationSignals;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12179;
import net.minecraft.class_12180;
import net.minecraft.class_1923;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_5321;
import net.minecraft.class_7134;
import net.minecraft.class_746;
import net.minecraft.class_9848;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class PlayerBypass2Module extends ClientModule {
   private static final float LINE_WIDTH = 2.0F;
   private static final int MARK_COLOR = -11205;
   private static final int MARK_FILL_ALPHA = 34;
   private static final int MEMORY_SECONDS = 45;
   private static final float FALLOFF = 0.3F;
   private static final float MIN_SOURCE = 0.5F;
   private static final int MAX_FOUND = 256;
   private static final int ALERT_SPACING = 4;
   private static final int PIN_SPACING = 1;
   private static final float FROZEN_NEIGHBOUR = 0.5F;
   private static final int REPORT_SOURCES = 4;
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private final NumberSetting sensitivity = this.addSetting("sens", new NumberSetting("Sensitivity", 5.0F, 1.0F, 10.0F).step(1.0F));
   private final LongOpenHashSet peaks = new LongOpenHashSet();
   private final Map<class_5321<class_1937>, LongLinkedOpenHashSet> foundByDimension = new HashMap<>();
   private LongLinkedOpenHashSet found = new LongLinkedOpenHashSet();
   private int session = -1;
   private final LongOpenHashSet shown = new LongOpenHashSet();
   private final Long2FloatOpenHashMap field = new Long2FloatOpenHashMap();
   private final Long2FloatOpenHashMap raw = new Long2FloatOpenHashMap();
   private int ticks;

   public PlayerBypass2Module() {
      super(
         "player-bypass-2", "Player Bypass 2", "Finds hidden players from chunks that keep ticking outside your simulation distance.", ModuleCategory.BASE_RAID
      );
   }

   @Override
   protected void onEnable() {
      this.foundByDimension.clear();
      this.found = new LongLinkedOpenHashSet();
      this.session = SimulationSignals.session();
      SimulationSignals.clear();
      this.configure();
      this.on(TickEvent.TYPE, Stage.POST, this::onTick);
   }

   @Override
   protected void onDisable() {
      SimulationSignals.configure(false, 0, 0, true, 30);
      this.peaks.clear();
      this.foundByDimension.clear();
      this.found = new LongLinkedOpenHashSet();
      this.shown.clear();
      this.field.clear();
      this.raw.clear();
   }

   private void configure() {
      SimulationSignals.configure(true, 0, 0, true, 45);
   }

   private static boolean netherOrEnd(class_1937 level) {
      class_5321<class_1937> dimension = level.method_27983();
      return dimension.equals(class_1937.field_25180)
         || dimension.equals(class_1937.field_25181)
         || level.method_40134().method_40225(class_7134.field_37667)
         || level.method_40134().method_40225(class_7134.field_37668)
         || !level.method_8597().comp_642();
   }

   private float threshold() {
      return 8.0F * (float)Math.pow(2.0, (5.0 - this.sensitivity.get()) / 2.5);
   }

   private void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1687 != null && mc.field_1724 != null) {
         if (this.session != SimulationSignals.session()) {
            this.session = SimulationSignals.session();
            this.foundByDimension.clear();
            this.peaks.clear();
            this.shown.clear();
         }

         this.found = this.foundByDimension.computeIfAbsent(mc.field_1687.method_27983(), key -> new LongLinkedOpenHashSet());
         this.configure();
         SimulationSignals.tick();
         if (netherOrEnd(mc.field_1687)) {
            SimulationSignals.dropPins();
            this.shown.clear();
         } else {
            this.drainPins();
            if (++this.ticks % 5 == 0) {
               this.collect(mc.field_1724);
               this.alert();
               this.refreshShown(mc.field_1724);
            }
         }
      }
   }

   private void collect(class_746 self) {
      this.peaks.clear();
      this.field.clear();
      this.raw.clear();
      long now = SimulationSignals.now();
      int sim = SimulationSignals.simulation();
      int pcx = self.method_24515().method_10263() >> 4;
      int pcz = self.method_24515().method_10260() >> 4;
      int start = SimulationSignals.ringStart();
      int view = SimulationSignals.view();
      int side = 2 * sim + 1;
      float[] weights = new float[side * side];
      float span = sim * 1.4143F + 1.0F;

      for (int dx = -sim; dx <= sim; dx++) {
         for (int dz = -sim; dz <= sim; dz++) {
            weights[(dx + sim) * side + dz + sim] = 1.0F - 0.3F * (float)Math.sqrt(dx * dx + dz * dz) / span;
         }
      }

      ObjectIterator var24 = SimulationSignals.chunks().long2ObjectEntrySet().iterator();

      while (var24.hasNext()) {
         Entry<SimulationSignals.ChunkSignal> entry = (Entry<SimulationSignals.ChunkSignal>)var24.next();
         long key = entry.getLongKey();
         int cx = class_1923.method_8325(key);
         int cz = class_1923.method_8332(key);
         int d = SimulationSignals.distance(cx, cz, pcx, pcz);
         if (d >= start && d <= view) {
            float score = ((SimulationSignals.ChunkSignal)entry.getValue()).score(now);
            if (!(score < 0.5F)) {
               for (int dx = -sim; dx <= sim; dx++) {
                  for (int dz = -sim; dz <= sim; dz++) {
                     long area = class_1923.method_8331(cx + dx, cz + dz);
                     this.field.addTo(area, score * weights[(dx + sim) * side + dz + sim]);
                     this.raw.addTo(area, score);
                  }
               }
            }
         }
      }

      LongIterator var25 = SimulationSignals.frozen().iterator();

      while (var25.hasNext()) {
         long frozen = (Long)var25.next();
         this.field.remove(frozen);
         this.raw.remove(frozen);
         int fx = class_1923.method_8325(frozen);
         int fz = class_1923.method_8332(frozen);

         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               long near = class_1923.method_8331(fx + dx, fz + dz);
               if (this.field.containsKey(near)) {
                  this.field.put(near, this.field.get(near) * 0.5F);
                  this.raw.put(near, this.raw.get(near) * 0.5F);
               }
            }
         }
      }

      float threshold = this.threshold();
      ObjectIterator var29 = this.field.long2FloatEntrySet().iterator();

      while (var29.hasNext()) {
         it.unimi.dsi.fastutil.longs.Long2FloatMap.Entry entry = (it.unimi.dsi.fastutil.longs.Long2FloatMap.Entry)var29.next();
         long key = entry.getLongKey();
         if (!(this.raw.get(key) < threshold) && this.isPeak(key, entry.getFloatValue(), sim)) {
            this.peaks.add(key);
         }
      }
   }

   private boolean isPeak(long key, float value, int reach) {
      int cx = class_1923.method_8325(key);
      int cz = class_1923.method_8332(key);

      for (int dx = -reach; dx <= reach; dx++) {
         for (int dz = -reach; dz <= reach; dz++) {
            if (dx != 0 || dz != 0) {
               long other = class_1923.method_8331(cx + dx, cz + dz);
               float near = this.field.get(other);
               if (near > value || near == value && other < key) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private void drainPins() {
      SimulationSignals.Pin pin;
      while ((pin = SimulationSignals.pollPin()) != null) {
         class_2338 pos = pin.pos();
         long key = class_1923.method_8331(pos.method_10263() >> 4, pos.method_10260() >> 4);
         LOGGER.info("[Player Bypass 2] {}", pin.detail());
         boolean known = false;
         LongListIterator var6 = this.found.iterator();

         while (var6.hasNext()) {
            long old = (Long)var6.next();
            if (SimulationSignals.distance(class_1923.method_8325(old), class_1923.method_8332(old), class_1923.method_8325(key), class_1923.method_8332(key))
               <= 1) {
               known = true;
               break;
            }
         }

         if (!known) {
            this.found.add(key);

            while (this.found.size() > 256) {
               this.found.removeFirstLong();
            }

            ModuleChat.send(this, pin.detail());
            ChallengeToast.addOrUpdate(
               GlintHeadRenderer.stack(),
               "Player Activity Detected",
               class_2561.method_43470("at " + pos.method_10263() + ", " + pos.method_10264() + ", " + pos.method_10260()),
               true
            );
         }
      }
   }

   private void alert() {
      long now = SimulationSignals.now();
      int spacing = Math.max(4, 2 * SimulationSignals.simulation());
      LongIterator var4 = this.peaks.iterator();

      while (var4.hasNext()) {
         long key = (Long)var4.next();
         boolean known = false;
         LongListIterator x = this.found.iterator();

         while (x.hasNext()) {
            long old = (Long)x.next();
            if (SimulationSignals.distance(class_1923.method_8325(old), class_1923.method_8332(old), class_1923.method_8325(key), class_1923.method_8332(key))
               <= spacing) {
               known = true;
               break;
            }
         }

         if (!known) {
            this.found.add(key);

            while (this.found.size() > 256) {
               this.found.removeFirstLong();
            }

            int xx = (class_1923.method_8325(key) << 4) + 8;
            int z = (class_1923.method_8332(key) << 4) + 8;
            this.report(key, xx, z, now);
            ChallengeToast.addOrUpdate(GlintHeadRenderer.stack(), "Player Activity Detected", class_2561.method_43470("at " + xx + ", " + z), true);
         }
      }
   }

   private void refreshShown(class_746 self) {
      this.shown.clear();
      int pcx = self.method_24515().method_10263() >> 4;
      int pcz = self.method_24515().method_10260() >> 4;
      int reach = SimulationSignals.view() + SimulationSignals.simulation() + 2;
      LongListIterator var5 = this.found.iterator();

      while (var5.hasNext()) {
         long key = (Long)var5.next();
         if (SimulationSignals.distance(class_1923.method_8325(key), class_1923.method_8332(key), pcx, pcz) <= reach) {
            this.shown.add(key);
         }
      }
   }

   private void report(long found, int x, int z, long now) {
      int sim = SimulationSignals.simulation();
      int fx = class_1923.method_8325(found);
      int fz = class_1923.method_8332(found);
      List<Entry<SimulationSignals.ChunkSignal>> sources = new ArrayList<>();
      ObjectIterator lines = SimulationSignals.chunks().long2ObjectEntrySet().iterator();

      while (lines.hasNext()) {
         Entry<SimulationSignals.ChunkSignal> entry = (Entry<SimulationSignals.ChunkSignal>)lines.next();
         long key = entry.getLongKey();
         if (SimulationSignals.distance(class_1923.method_8325(key), class_1923.method_8332(key), fx, fz) <= sim
            && !(((SimulationSignals.ChunkSignal)entry.getValue()).score(now) < 0.5F)) {
            sources.add(entry);
         }
      }

      sources.sort((a, b) -> Float.compare(((SimulationSignals.ChunkSignal)b.getValue()).score(now), ((SimulationSignals.ChunkSignal)a.getValue()).score(now)));
      List<String> linesx = new ArrayList<>();
      linesx.add(
         "Found "
            + x
            + ", "
            + z
            + " from "
            + sources.size()
            + " chunks (sim "
            + sim
            + ", ring "
            + SimulationSignals.ringStart()
            + "-"
            + SimulationSignals.view()
            + ")"
      );

      for (int i = 0; i < Math.min(4, sources.size()); i++) {
         SimulationSignals.ChunkSignal signal = (SimulationSignals.ChunkSignal)sources.get(i).getValue();
         long key = sources.get(i).getLongKey();
         StringBuilder kinds = new StringBuilder();

         for (SimulationSignals.Kind kind : SimulationSignals.Kind.values()) {
            if (signal.count(kind) > 0) {
               kinds.append(kinds.isEmpty() ? "" : ", ").append(kind.title).append(" x").append(signal.count(kind));
            }
         }

         linesx.add(
            " chunk "
               + ((class_1923.method_8325(key) << 4) + 8)
               + ", "
               + ((class_1923.method_8332(key) << 4) + 8)
               + String.format(Locale.ROOT, " score %.1f: ", signal.score(now))
               + kinds
         );

         for (String recent : signal.recent()) {
            linesx.add("   " + recent);
         }
      }

      for (String line : linesx) {
         LOGGER.info("[Player Bypass 2] {}", line);
         ModuleChat.send(this, line);
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1687 != null && mc.field_1724 != null && !netherOrEnd(mc.field_1687)) {
         if (!this.shown.isEmpty()) {
            FoglessLines lines = FoglessLines.forFrame(event.partialTick);
            double bottom = mc.field_1687.method_31607();
            double top = mc.field_1687.method_31600() + 1;
            class_12179 fill = class_12179.method_75539(class_9848.method_61330(34, -11205));
            LongIterator var9 = this.shown.iterator();

            while (var9.hasNext()) {
               long key = (Long)var9.next();
               double x0 = class_1923.method_8325(key) << 4;
               double z0 = class_1923.method_8332(key) << 4;
               double x1 = x0 + 16.0;
               double z1 = z0 + 16.0;
               class_12180.method_75541(new class_238(x0, bottom, z0, x1, top, z1), fill).method_75533();
               column(lines, x0, z0, x1, z1, bottom, top);
            }
         }
      }
   }

   private static void column(FoglessLines lines, double x0, double z0, double x1, double z1, double bottom, double top) {
      double[][] corners = new double[][]{{x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}};

      for (int i = 0; i < corners.length; i++) {
         double[] a = corners[i];
         double[] b = corners[(i + 1) % corners.length];
         lines.line(new class_243(a[0], bottom, a[1]), new class_243(a[0], top, a[1]), -11205, 2.0F);
         lines.line(new class_243(a[0], bottom, a[1]), new class_243(b[0], bottom, b[1]), -11205, 2.0F);
         lines.line(new class_243(a[0], top, a[1]), new class_243(b[0], top, b[1]), -11205, 2.0F);
      }
   }
}
