package net.anubis.client.module.impl.render;

import imgui.ImDrawList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.gui.WorldLabel;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.HudOverlayProvider;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.misc.NameProtectModule;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.PearlTrajectorySimulator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12179;
import net.minecraft.class_12180;
import net.minecraft.class_1297;
import net.minecraft.class_1684;
import net.minecraft.class_2350;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3965;

@Environment(EnvType.CLIENT)
public final class PearlTrajectoryModule extends ClientModule implements HudOverlayProvider {
   private static final int MAX_TRAIL_POINTS = 200;
   private static final double MIN_TRAIL_DISTANCE_SQ = 1.0E-4;
   private static final double PIECE_LENGTH = 0.45;
   private static final double FADE_IN_DISTANCE = 1.8;
   private static final int RING_SEGMENTS = 48;
   private static final float POP_IN_SECONDS = 0.22F;
   private static final float WAVE_PERIOD_SECONDS = 1.4F;
   private static final long LABEL_MAX_AGE_NANOS = 250000000L;
   private static final long EPOCH = System.nanoTime();
   private static final String LANDING_RING = "Ring";
   private static final String LANDING_BOX = "Box";
   private static final String LANDING_NONE = "None";
   private final Map<Integer, List<class_243>> trails = new HashMap<>();
   private final List<PearlTrajectoryModule.Label> labels = new ArrayList<>();
   private long labelsAt;
   private long predictionShownAt = -1L;
   private final BooleanSetting sPredict = this.addSetting("predict", new BooleanSetting("Predict", true));
   private final BooleanSetting sTrail = this.addSetting("trail", new BooleanSetting("Trail", true));
   private final BooleanSetting sOthers = this.addSetting("others-pearls", new BooleanSetting("Others' Pearls", false));
   private final ModeSetting sLanding = this.addSetting("landing", new ModeSetting("Landing", "Ring", "Ring", "Box", "None"));
   private final BooleanSetting sInfo = this.addSetting("landing-info", new BooleanSetting("Landing Info", true));
   private final BooleanSetting sGlow = this.addSetting("glow", new BooleanSetting("Glow", true));
   private final BooleanSetting sGradient = this.addSetting("gradient", new BooleanSetting("Gradient", true));
   private final ColorSetting sColor = this.addSetting("color", new ColorSetting("Color", -9524994));
   private final ColorSetting sEndColor = this.addSetting("end-color", new ColorSetting("End Color", -3697942));
   private final ColorSetting sHitColor = this.addSetting("hit-color", new ColorSetting("Hit Color", -42406));
   private final NumberSetting sLineWidth = this.addSetting("line-width", new NumberSetting("Line Width", 2.0F, 0.5F, 5.0F).step(0.5F));

   public PearlTrajectoryModule() {
      super("pearl-trajectory", "Pearl Trajectory", "Predicts where a held pearl lands and previews the flight of thrown pearls.", ModuleCategory.RENDER);
      this.sEndColor.visibleWhen(this.sGradient::get);
   }

   @Override
   protected void onDisable() {
      this.trails.clear();
      this.labels.clear();
      this.predictionShownAt = -1L;
   }

   @EventHandler
   public void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1687 != null && mc.field_1724 != null) {
         Set<Integer> active = new HashSet<>();

         for (class_1297 entity : mc.field_1687.method_18112()) {
            if (entity instanceof class_1684 pearl && this.isTracked(pearl, mc)) {
               active.add(pearl.method_5628());
               List<class_243> points = this.trails.computeIfAbsent(pearl.method_5628(), ignored -> new ArrayList<>());
               class_243 position = pearl.method_73189();
               if (points.isEmpty() || !(points.getLast().method_1025(position) < 1.0E-4)) {
                  points.add(position);
                  if (points.size() > 200) {
                     points.removeFirst();
                  }
               }
            }
         }

         this.trails.keySet().retainAll(active);
      } else {
         this.trails.clear();
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      this.labels.clear();
      class_310 mc = class_310.method_1551();
      if (mc.field_1724 != null && mc.field_1687 != null) {
         long now = System.nanoTime();
         this.labelsAt = now;
         float time = (float)(now - EPOCH) / 1.0E9F;
         float width = this.sLineWidth.get();
         class_243 eye = mc.field_1724.method_5836(event.partialTick);
         if (this.sPredict.get() && PearlTrajectorySimulator.isHoldingPearl(mc.field_1724)) {
            if (this.predictionShownAt < 0L) {
               this.predictionShownAt = now;
            }

            float pop = easeOutBack(Math.min(1.0F, (float)(now - this.predictionShownAt) / 1.0E9F / 0.22F));
            PearlTrajectorySimulator.SimulationResult result = PearlTrajectorySimulator.simulate(mc.field_1687, mc.field_1724, event.partialTick);
            this.drawPath(result.points(), width, true, false, 1.0F);
            this.drawLanding(result, pop, time, width, 1.0F);
            this.addLabel(result, eye, null);
         } else {
            this.predictionShownAt = -1L;
         }

         for (class_1297 entity : mc.field_1687.method_18112()) {
            if (entity instanceof class_1684 pearl && this.isTracked(pearl, mc)) {
               class_243 head = pearl.method_30950(event.partialTick);
               if (this.sTrail.get()) {
                  this.drawTrail(this.trails.get(pearl.method_5628()), head, width);
               }

               PearlTrajectorySimulator.SimulationResult rest = PearlTrajectorySimulator.simulate(mc.field_1687, pearl);
               List<class_243> ahead = new ArrayList<>(rest.points());
               ahead.set(0, head);
               this.drawPath(ahead, width * 0.8F, false, true, 0.7F);
               this.drawLanding(rest, 1.0F, time, width, 0.75F);
               class_1297 owner = pearl.method_24921();
               boolean own = owner == null || owner == mc.field_1724;
               this.addLabel(rest, eye, own ? null : NameProtectModule.protect(owner.method_5477()).getString());
            }
         }
      }
   }

   private void drawPath(List<class_243> points, float width, boolean fadeIn, boolean dashed, float alphaMul) {
      if (points.size() >= 2) {
         double total = 0.0;

         for (int i = 1; i < points.size(); i++) {
            total += points.get(i - 1).method_1022(points.get(i));
         }

         if (!(total <= 1.0E-6)) {
            int start = this.sColor.argb();
            int end = this.sGradient.get() ? this.sEndColor.argb() : start;
            boolean glow = this.sGlow.get();
            double travelled = 0.0;
            int piece = 0;

            for (int i = 1; i < points.size(); i++) {
               class_243 a = points.get(i - 1);
               class_243 b = points.get(i);
               double length = a.method_1022(b);
               int pieces = Math.max(1, (int)Math.ceil(length / 0.45));

               for (int p = 0; p < pieces; piece++) {
                  if (!dashed || (piece & 1) != 1) {
                     double t0 = (double)p / pieces;
                     double t1 = (double)(p + 1) / pieces;
                     double mid = travelled + length * (t0 + t1) * 0.5;
                     float alpha = alphaMul;
                     if (fadeIn) {
                        alpha *= smoothstep((float)(mid / 1.8));
                     }

                     if (!(alpha < 0.02F)) {
                        int color = scaleAlpha(lerpArgb(start, end, (float)(mid / total)), alpha);
                        class_243 from = a.method_35590(b, t0);
                        class_243 to = a.method_35590(b, t1);
                        if (glow) {
                           class_12180.method_75546(from, to, scaleAlpha(color, 0.2F), width * 3.2F).method_75533();
                        }

                        class_12180.method_75546(from, to, color, width).method_75533();
                     }
                  }

                  p++;
               }

               travelled += length;
            }
         }
      }
   }

   private void drawTrail(List<class_243> points, class_243 head, float width) {
      if (points != null && !points.isEmpty()) {
         int color = this.sColor.argb();
         int count = points.size() + 1;

         for (int i = 1; i < count; i++) {
            class_243 from = points.get(i - 1);
            class_243 to = i < points.size() ? points.get(i) : head;
            float alpha = 0.15F + 0.85F * i / (count - 1.0F);
            class_12180.method_75546(from, to, scaleAlpha(color, alpha), width).method_75533();
         }
      }
   }

   private void drawLanding(PearlTrajectorySimulator.SimulationResult result, float scale, float time, float width, float alphaMul) {
      int hitColor = scaleAlpha(this.sHitColor.argb(), alphaMul);
      if (result.entityHit() != null) {
         class_238 box = result.entityHit().method_5829().method_1014(0.05);
         class_12180.method_75541(box, class_12179.method_75537(hitColor, width, scaleAlpha(hitColor, 0.14F))).method_75533();
      } else {
         class_3965 hit = result.blockHit();
         String mode = this.sLanding.get();
         if (hit != null && !"None".equals(mode)) {
            int color = scaleAlpha(this.sGradient.get() ? this.sEndColor.argb() : this.sColor.argb(), alphaMul);
            class_2350 face = hit.method_17780();
            class_243 normal = new class_243(face.method_10148(), face.method_10164(), face.method_10165());
            if ("Box".equals(mode)) {
               class_238 box = new class_238(hit.method_17777()).method_1014(0.002);
               class_12180.method_75548(box.method_61125(), box.method_61126(), face, class_12179.method_75537(color, width, scaleAlpha(color, 0.22F)))
                  .method_75533();
            } else {
               class_243 centre = result.landing().method_1019(normal.method_1021(0.02));
               float pulse = 1.0F + 0.05F * (float)Math.sin(time * 5.0F);
               drawRing(centre, normal, 0.38F * scale * pulse, color, width);
               drawCross(centre, normal, 0.09F * scale, color, width);
               float phase = time % 1.4F / 1.4F;
               float waveRadius = lerp(0.3F, 0.95F, easeOutCubic(phase)) * scale;
               drawRing(centre, normal, waveRadius, scaleAlpha(color, 0.75F * (1.0F - phase)), width * 0.8F);
            }
         }
      }
   }

   private static void drawRing(class_243 centre, class_243 normal, float radius, int color, float width) {
      if ((color >>> 24 & 0xFF) >= 4 && !(radius <= 0.0F)) {
         class_243[] basis = planeBasis(normal);
         class_243 previous = null;

         for (int i = 0; i <= 48; i++) {
            double angle = (Math.PI * 2) * i / 48.0;
            class_243 point = centre.method_1019(basis[0].method_1021(Math.cos(angle) * radius)).method_1019(basis[1].method_1021(Math.sin(angle) * radius));
            if (previous != null) {
               class_12180.method_75546(previous, point, color, width).method_75533();
            }

            previous = point;
         }
      }
   }

   private static void drawCross(class_243 centre, class_243 normal, float half, int color, float width) {
      class_243[] basis = planeBasis(normal);
      class_12180.method_75546(centre.method_1019(basis[0].method_1021(-half)), centre.method_1019(basis[0].method_1021(half)), color, width).method_75533();
      class_12180.method_75546(centre.method_1019(basis[1].method_1021(-half)), centre.method_1019(basis[1].method_1021(half)), color, width).method_75533();
   }

   private static class_243[] planeBasis(class_243 normal) {
      class_243 u = Math.abs(normal.field_1351) > 0.9 ? new class_243(1.0, 0.0, 0.0) : normal.method_1036(new class_243(0.0, 1.0, 0.0)).method_1029();
      return new class_243[]{u, normal.method_1036(u).method_1029()};
   }

   private void addLabel(PearlTrajectorySimulator.SimulationResult result, class_243 eye, String owner) {
      if (this.sInfo.get() && result.landed()) {
         String time = String.format(Locale.ROOT, "%.1fs", result.ticks() / 20.0F);
         String primary = String.format(Locale.ROOT, "%.1fm", eye.method_1022(result.landing()));
         String secondary;
         if (result.entityHit() != null) {
            secondary = NameProtectModule.protect(result.entityHit().method_5477()).getString() + "  ·  " + time;
         } else {
            secondary = owner != null ? owner + "  ·  " + time : time;
         }

         this.labels.add(new PearlTrajectoryModule.Label(result.landing(), primary, secondary));
      }
   }

   private boolean isTracked(class_1684 pearl, class_310 mc) {
      if (pearl.method_31481()) {
         return false;
      }

      class_1297 owner = pearl.method_24921();
      return owner == null || owner == mc.field_1724 || this.sOthers.get();
   }

   @Override
   public boolean isHudOverlayEnabled() {
      return this.isEnabled() && this.sInfo.get();
   }

   @Override
   public void renderHudOverlay(ImDrawList dl, float screenW, float screenH) {
      if (!this.labels.isEmpty() && System.nanoTime() - this.labelsAt <= 250000000L) {
         class_310 mc = class_310.method_1551();

         for (PearlTrajectoryModule.Label label : this.labels) {
            float[] screen = WorldLabel.project(mc, label.pos().method_1031(0.0, 0.35, 0.0), screenW, screenH);
            if (screen != null) {
               WorldLabel.drawText(dl, screen[0], screen[1], label.primary(), label.secondary(), 1.0F);
            }
         }
      }
   }

   private static int lerpArgb(int a, int b, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      int aa = a >>> 24;
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int ba = b >>> 24;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      return Math.round(aa + (ba - aa) * t) << 24 | Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
   }

   private static int scaleAlpha(int argb, float mul) {
      int alpha = Math.round((argb >>> 24) * Math.max(0.0F, Math.min(1.0F, mul)));
      return alpha << 24 | argb & 16777215;
   }

   private static float lerp(float a, float b, float t) {
      return a + (b - a) * t;
   }

   private static float smoothstep(float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      return t * t * (3.0F - 2.0F * t);
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
   private record Label(class_243 pos, String primary, String secondary) {
   }
}
