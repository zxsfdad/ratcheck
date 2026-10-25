package net.anubis.client.module.impl.misc;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.anubis.client.chat.ModuleChat;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.gui.ChallengeToast;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.client.PerformanceModule;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.module.setting.Setting;
import net.anubis.client.module.setting.StringSetting;
import net.anubis.client.util.ChunkAreas;
import net.anubis.client.util.FoglessLines;
import net.anubis.client.util.playerbypass.ChunkLightSnapshot;
import net.anubis.client.util.playerbypass.PlayerBypassLightTracker;
import net.anubis.client.util.playerbypass.VisibleChunkCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12179;
import net.minecraft.class_12180;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1923;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_634;
import net.minecraft.class_9848;

@Environment(EnvType.CLIENT)
public final class PlayerBypassModule extends ClientModule {
   private static final int RENDER_DISTANCE_CHUNKS = 16;
   private static final int ZONE_RADIUS = 5;
   private static final double MARKER_Y = 64.0;
   private static final float LINE_WIDTH = 1.5F;
   private static final float CENTER_FILL = 0.45F;
   private static final float RIM_FILL = 0.25F;
   private static final float EDGE_SHADE = 0.8F;
   private static final int BASE_COLOR = -2516480;
   private static final int SUSPECT_COLOR = -5766;
   private final NumberSetting opacity = this.addSetting("opacity", new NumberSetting("Opacity", 140.0F, 10.0F, 255.0F).step(1.0F));
   private final BooleanSetting tracers = this.addSetting("tracers", new BooleanSetting("Tracers", false));
   private final ChunkAreas chunkAreas = new ChunkAreas();
   private final ChunkAreas suspectAreas = new ChunkAreas();
   private final Long2FloatOpenHashMap baseWeight = new Long2FloatOpenHashMap();
   private final Long2FloatOpenHashMap suspectWeight = new Long2FloatOpenHashMap();
   private final LongOpenHashSet zone = new LongOpenHashSet();
   private final VisibleChunkCache visibleChunks = new VisibleChunkCache(16);
   private boolean zoneDirty = true;
   private final BooleanSetting sound = this.addSetting("sound", new BooleanSetting("Sound", true));
   private final BooleanSetting showCoordinates = this.addSetting("show-coordinates", new BooleanSetting("Show Coordinates", true));
   private final BooleanSetting disconnect = this.addSetting("disconnect-on-spotted", new BooleanSetting("Auto Disconnect", false));
   private final BooleanSetting autoRtp = this.addSetting("auto-rtp", new BooleanSetting("Auto RTP", false));
   private final StringSetting rtpCommand = this.addSetting("rtp-command", new StringSetting("RTP Command", "rtp").hint("Without the slash, e.g. rtp"));
   private final NumberSetting rtpScanTime = this.addSetting("rtp-scan-time", new NumberSetting("Scan Time (s)", 5.0F, 2.0F, 30.0F).step(0.5F));
   private final NumberSetting rtpDelay = this.addSetting("rtp-delay", new NumberSetting("RTP Delay (s)", 6.0F, 3.0F, 120.0F).step(0.5F));
   private final NumberSetting rtpTimeout = this.addSetting("rtp-max-timeout", new NumberSetting("RTP Timeout (s)", 10.0F, 3.0F, 60.0F).step(1.0F));
   private final NumberSetting rtpMax = this.addSetting("rtp-max", new NumberSetting("Max RTPs (0 = unlimited)", 100.0F, 0.0F, 500.0F).step(1.0F));
   private final BooleanSetting rtpStopOnSuspect = this.addSetting("rtp-stop-on-suspect", new BooleanSetting("Stop On Suspect", false));
   private final StringSetting rtpMenuItem = this.addSetting(
      "rtp-menu-item", new StringSetting("Menu Item", "").hint("Clicked if the command opens a menu, e.g. overworld")
   );
   private final PlayerBypassAutoRtp autoRtpRunner = new PlayerBypassAutoRtp(
      this,
      this.autoRtp,
      this.rtpCommand,
      this.rtpScanTime,
      this.rtpDelay,
      this.rtpTimeout,
      this.rtpMax,
      this.rtpStopOnSuspect,
      this.rtpMenuItem,
      this.sound,
      this.showCoordinates
   );

   public PlayerBypassModule() {
      super("player-bypass", "Player Bypass", "Flags chunks with signs of underground player bases from light data.", ModuleCategory.BASE_RAID);
      BooleanSetting notifications = this.usesNotifications();
      this.sound.visibleWhen(() -> notifications.get() || this.autoRtp.get());
      this.showCoordinates.visibleWhen(() -> notifications.get() || this.disconnect.get() || this.autoRtp.get());
      this.autoRtp.sessionOnly();
      Setting[] rtpSettings = new Setting[]{
         this.rtpCommand, this.rtpScanTime, this.rtpDelay, this.rtpTimeout, this.rtpMax, this.rtpStopOnSuspect, this.rtpMenuItem
      };

      for (Setting setting : rtpSettings) {
         setting.visibleWhen(this.autoRtp::get);
      }

      this.autoRtp.onChange(() -> {
         if (this.isEnabled()) {
            if (this.autoRtp.get()) {
               this.autoRtpRunner.start();
            } else {
               this.autoRtpRunner.stopByUser();
            }
         }
      });
   }

   @Override
   protected void onEnable() {
      this.visibleChunks.invalidate();
      PlayerBypassLightTracker.setSpottedListener(this::onChunkSpotted);
      if (this.autoRtp.get()) {
         this.autoRtpRunner.start();
      }
   }

   @Override
   protected void onDisable() {
      this.visibleChunks.invalidate();
      PlayerBypassLightTracker.setSpottedListener(null);
      this.autoRtpRunner.stopByUser();
      this.autoRtpRunner.forgetCooldown();
      this.autoRtp.set(false);
   }

   private void onChunkSpotted(int chunkX, int chunkZ) {
      class_310 mc = class_310.method_1551();
      int x = (chunkX << 4) + 8;
      int z = (chunkZ << 4) + 8;
      boolean showPos = this.showCoordinates.get();
      if (!this.autoRtpRunner.baseSpotted(x, z, this.disconnect.get()) && this.notificationsOn()) {
         ChallengeToast.addOrUpdate(
            new class_1799(class_1802.field_8575),
            "Player Chunk Detected",
            showPos ? class_2561.method_43470("at " + x + ", " + z) : class_2561.method_43473(),
            this.sound.get()
         );
      }

      if (this.disconnect.get()) {
         class_634 connection = mc.method_1562();
         if (connection != null && connection.method_48296().method_10758()) {
            ModuleChat.send(this, ModuleChat.body().text("Auto Disconnect ").warn("triggered"));
            AutoReconnectModule.holdOff();
            connection.method_48296().method_10747(class_2561.method_43470("[Player Bypass] Base detected" + (showPos ? " at " + x + ", " + z : "")));
         }
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1687 != null && mc.field_1724 != null) {
         Long2ObjectMap<ChunkLightSnapshot> snapshots = PlayerBypassLightTracker.snapshots();
         if (!snapshots.isEmpty()) {
            int centerX = mc.field_1724.method_24515().method_10263() >> 4;
            int centerZ = mc.field_1724.method_24515().method_10260() >> 4;
            LongOpenHashSet bases;
            LongOpenHashSet suspects;
            boolean changed;
            if (PerformanceModule.optimizationsEnabled()) {
               changed = this.visibleChunks
                  .update(
                     PlayerBypassLightTracker.visibilityRevision(),
                     centerX,
                     centerZ,
                     snapshots,
                     PlayerBypassLightTracker::isFlagged,
                     PlayerBypassLightTracker::isSuspect
                  );
               bases = this.visibleChunks.bases();
               suspects = this.visibleChunks.suspects();
            } else {
               this.visibleChunks.invalidate();
               changed = true;
               bases = new LongOpenHashSet();
               suspects = new LongOpenHashSet();
               ObjectIterator base = snapshots.long2ObjectEntrySet().iterator();

               while (base.hasNext()) {
                  Entry<ChunkLightSnapshot> entry = (Entry<ChunkLightSnapshot>)base.next();
                  long key = entry.getLongKey();
                  if (Math.abs(class_1923.method_8325(key) - centerX) <= 16
                     && Math.abs(class_1923.method_8332(key) - centerZ) <= 16
                     && PlayerBypassLightTracker.isFlagged(key, (ChunkLightSnapshot)entry.getValue())) {
                     (PlayerBypassLightTracker.isSuspect(key) ? suspects : bases).add(key);
                  }
               }
            }

            if (!bases.isEmpty() || !suspects.isEmpty()) {
               int base = -2516480;
               int suspect = -5766;
               if (changed) {
                  spread(bases, this.baseWeight);
                  spread(suspects, this.suspectWeight);
                  this.zoneDirty = true;
               }

               FoglessLines lines = FoglessLines.forFrame(event.partialTick);
               this.drawZones(lines, base, suspect);
               if (this.tracers.get()) {
                  this.chunkAreas.drawTracers(bases, 64.0, class_9848.method_61334(base));
                  this.suspectAreas.drawTracers(suspects, 64.0, class_9848.method_61334(suspect));
               }
            }
         }
      }
   }

   private static void spread(LongOpenHashSet sources, Long2FloatOpenHashMap into) {
      into.clear();
      LongIterator var2 = sources.iterator();

      while (var2.hasNext()) {
         long key = (Long)var2.next();
         int cx = class_1923.method_8325(key);
         int cz = class_1923.method_8332(key);

         for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
               float weight = falloff(Math.max(Math.abs(dx), Math.abs(dz)));
               long area = class_1923.method_8331(cx + dx, cz + dz);
               if (weight > into.get(area)) {
                  into.put(area, weight);
               }
            }
         }
      }
   }

   private static float falloff(int ring) {
      float t = ring / 6.0F;
      return 1.0F - t * t * (3.0F - 2.0F * t);
   }

   private void drawZones(FoglessLines lines, int base, int suspect) {
      if (this.zoneDirty) {
         this.zone.clear();
         this.zone.addAll(this.baseWeight.keySet());
         this.zone.addAll(this.suspectWeight.keySet());
         this.zoneDirty = false;
      }

      float maxAlpha = Math.max(10, Math.min(255, this.opacity.getInt()));
      LongIterator var5 = this.zone.iterator();

      while (var5.hasNext()) {
         long key = (Long)var5.next();
         float b = this.baseWeight.get(key);
         float s = this.suspectWeight.get(key);
         float mix = b * b / (b * b + s * s);
         int color = class_9848.method_61319(mix, suspect, base);
         float strength = 0.25F + 0.75F * Math.max(b, s);
         int fill = class_9848.method_61330(Math.round(maxAlpha * 0.45F * strength * class_9848.method_65100(color)), color);
         int cx = class_1923.method_8325(key);
         int cz = class_1923.method_8332(key);
         double x0 = cx << 4;
         double z0 = cz << 4;
         double x1 = x0 + 16.0;
         double z1 = z0 + 16.0;
         class_12180.method_75541(new class_238(x0, 64.0, z0, x1, 64.0, z1), class_12179.method_75539(fill)).method_75533();
         int edge = class_9848.method_61330(Math.round(maxAlpha * class_9848.method_65100(color)), class_9848.method_61321(color, 0.8F));
         if (!this.zone.contains(class_1923.method_8331(cx, cz - 1))) {
            edge(lines, x0, z0, x1, z0, edge);
         }

         if (!this.zone.contains(class_1923.method_8331(cx, cz + 1))) {
            edge(lines, x0, z1, x1, z1, edge);
         }

         if (!this.zone.contains(class_1923.method_8331(cx - 1, cz))) {
            edge(lines, x0, z0, x0, z1, edge);
         }

         if (!this.zone.contains(class_1923.method_8331(cx + 1, cz))) {
            edge(lines, x1, z0, x1, z1, edge);
         }
      }
   }

   private static void edge(FoglessLines lines, double ax, double az, double bx, double bz, int color) {
      lines.line(new class_243(ax, 64.0, az), new class_243(bx, 64.0, bz), color, 1.5F);
   }
}
