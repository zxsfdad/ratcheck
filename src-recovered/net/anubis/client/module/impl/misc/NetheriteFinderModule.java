package net.anubis.client.module.impl.misc;

import imgui.ImDrawList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.anubis.client.chat.ModuleChat;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.HudOverlayProvider;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.netherite.NetheriteChunkTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;

@Environment(EnvType.CLIENT)
public final class NetheriteFinderModule extends ClientModule implements HudOverlayProvider {
   private static final int MAX_RENDERED_SECTIONS = 128;
   private static final long NOTIFY_COOLDOWN_MS = 2000L;
   private final ColorSetting color = this.addSetting("color", new ColorSetting("Near Color", -34304));
   private final ColorSetting farColor = this.addSetting("far-color", new ColorSetting("Far Color", -11688961));
   private final NumberSetting fade = this.addSetting("fade-distance", new NumberSetting("Fade Distance", 192.0F, 32.0F, 512.0F).step(8.0F));
   private final NumberSetting range = this.addSetting("range", new NumberSetting("Range", 512.0F, 64.0F, 1024.0F).step(1.0F));
   private final BooleanSetting tracers = this.addSetting("tracers", new BooleanSetting("Chunk Tracers", true));
   private final NumberSetting tracerCount = this.addSetting("tracer-count", new NumberSetting("Tracer Count", 3.0F, 1.0F, 8.0F).step(1.0F));
   private final BooleanSetting advanced = this.addSetting("advanced-finder", new BooleanSetting("Advanced Finder", false));
   private final BooleanSetting showProbes = this.addSetting("show-probes", new BooleanSetting("Show Probes", true));
   private final ModeSetting probeStyle = this.addSetting("probe-style", new ModeSetting("Probe Style", "Dots", "Dots", "Cubes", "Pulses"));
   private final NumberSetting missingFrom = this.addSetting("missing-from", new NumberSetting("Mark Missing From %", 60.0F, 30.0F, 100.0F).step(5.0F));
   private final AdvancedFinder advancedFinder = new AdvancedFinder(this);
   private final NetheriteVisuals visuals = new NetheriteVisuals();
   private int pendingCount;
   private class_2338 nearestPending;
   private double nearestPendingDistanceSq;
   private long lastNotifyAt;

   public NetheriteFinderModule() {
      super("netherite-finder", "Netherite Finder", "Boxes Nether chunk sections hiding ancient debris.", ModuleCategory.DONUT);
      this.usesNotifications().set(false);
      this.tracerCount.visibleWhen(this.tracers::get);
      this.showProbes.visibleWhen(this.advanced::get);
      this.probeStyle.visibleWhen(() -> this.advanced.get() && this.showProbes.get());
      this.missingFrom.visibleWhen(() -> this.advanced.get() && this.showProbes.get());
   }

   @Override
   protected void onEnable() {
      this.clearPending();
      this.lastNotifyAt = 0L;
      NetheriteChunkTracker.setListener(this::onCandidate);
      NetheriteChunkTracker.setRevealListener(this::onRevealed);
   }

   @Override
   protected void onDisable() {
      NetheriteChunkTracker.setListener(null);
      NetheriteChunkTracker.setRevealListener(null);
      this.advancedFinder.reset(class_310.method_1551());
      this.visuals.clear();
   }

   private void onRevealed(class_2338 pos) {
      if (this.advancedFinder.onRevealed(pos)) {
         this.visuals.burst(pos, this.color.argb());
      }
   }

   private void onCandidate(NetheriteChunkTracker.Candidate candidate) {
      if (this.notificationsOn()) {
         class_2338 pos = new class_2338((candidate.chunkX() << 4) + 8, (candidate.sectionY() << 4) + 8, (candidate.chunkZ() << 4) + 8);
         this.pendingCount++;
         class_310 mc = class_310.method_1551();
         double distanceSq = mc.field_1724 != null ? mc.field_1724.method_5649(pos.method_10263(), pos.method_10264(), pos.method_10260()) : 0.0;
         if (this.nearestPending == null || distanceSq < this.nearestPendingDistanceSq) {
            this.nearestPending = pos;
            this.nearestPendingDistanceSq = distanceSq;
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      this.flushNotifications();
      class_310 mc = class_310.method_1551();
      this.advancedFinder.maintain(mc);
      if (this.advanced.get()) {
         if (!this.advancedFinder.armed()) {
            this.advancedFinder.arm();
         }

         this.advancedFinder.tick(mc);
      } else if (this.advancedFinder.armed()) {
         this.advancedFinder.disarm(mc);
      }
   }

   private void flushNotifications() {
      if (this.pendingCount != 0) {
         if (!this.notificationsOn()) {
            this.clearPending();
         } else {
            long now = System.currentTimeMillis();
            if (now - this.lastNotifyAt >= 2000L) {
               ModuleChat.send(
                  this,
                  ModuleChat.body()
                     .value(this.pendingCount)
                     .text(" new hidden-debris chunk" + (this.pendingCount == 1 ? " " : "s "))
                     .good("found")
                     .text(", nearest at ")
                     .coords(this.nearestPending.method_10263(), this.nearestPending.method_10264(), this.nearestPending.method_10260())
               );
               this.clearPending();
               this.lastNotifyAt = now;
            }
         }
      }
   }

   private void clearPending() {
      this.pendingCount = 0;
      this.nearestPending = null;
      this.nearestPendingDistanceSq = Double.MAX_VALUE;
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1687 != null && mc.field_1724 != null && NetheriteChunkTracker.isNether(mc.field_1687)) {
         class_243 eye = mc.field_1724.method_5836(event.partialTick);
         double maxDistance = this.range.get();
         List<NetheriteVisuals.Section> sections = new ArrayList<>();

         for (NetheriteChunkTracker.Candidate candidate : NetheriteChunkTracker.snapshot()) {
            if (!this.advancedFinder.isDone(candidate)) {
               double x = candidate.chunkX() * 16.0;
               double y = candidate.sectionY() * 16.0;
               double z = candidate.chunkZ() * 16.0;
               class_238 box = new class_238(x, y, z, x + 16.0, y + 16.0, z + 16.0);
               double distance = Math.sqrt(distanceSq(box, eye));
               if (distance <= maxDistance) {
                  sections.add(new NetheriteVisuals.Section(box, distance, this.advancedFinder.progress(candidate)));
               }
            }
         }

         sections.sort(Comparator.comparingDouble(NetheriteVisuals.Section::distance));
         if (sections.size() > 128) {
            sections = sections.subList(0, 128);
         }

         NetheriteVisuals.Options options = new NetheriteVisuals.Options(
            this.color.argb(),
            this.farColor.argb(),
            this.fade.get(),
            maxDistance,
            this.probeStyle.get(),
            this.missingFrom.get() / 100.0F,
            this.tracers.get() ? this.tracerCount.getInt() : 0,
            this.showProbes.get()
         );
         this.visuals.render(mc, event.partialTick, eye, sections, List.copyOf(this.advancedFinder.revealed()), this.advancedFinder, options);
      } else {
         this.visuals.clear();
      }
   }

   @Override
   public boolean isHudOverlayEnabled() {
      return this.isEnabled() && this.visuals.hasLabels();
   }

   @Override
   public void renderHudOverlay(ImDrawList dl, float screenW, float screenH) {
      this.visuals.overlay(dl, screenW, screenH);
   }

   private static double distanceSq(class_238 box, class_243 point) {
      double dx = Math.max(0.0, Math.max(box.field_1323 - point.field_1352, point.field_1352 - box.field_1320));
      double dy = Math.max(0.0, Math.max(box.field_1322 - point.field_1351, point.field_1351 - box.field_1325));
      double dz = Math.max(0.0, Math.max(box.field_1321 - point.field_1350, point.field_1350 - box.field_1324));
      return dx * dx + dy * dy + dz * dz;
   }
}
