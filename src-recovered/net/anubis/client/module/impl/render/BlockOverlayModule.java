package net.anubis.client.module.impl.render;

import com.example.client.mixin.AccessorBlockOverlayMultiPlayerGameMode;
import com.example.client.mixin.AccessorGameRenderer;
import java.util.ArrayList;
import java.util.List;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.gui.ThemeTint;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12178;
import net.minecraft.class_12179;
import net.minecraft.class_12180;
import net.minecraft.class_1297;
import net.minecraft.class_2244;
import net.minecraft.class_2281;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_259;
import net.minecraft.class_265;
import net.minecraft.class_2680;
import net.minecraft.class_2741;
import net.minecraft.class_2745;
import net.minecraft.class_2756;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_3726;
import net.minecraft.class_3965;
import net.minecraft.class_636;
import net.minecraft.class_638;
import net.minecraft.class_9848;
import net.minecraft.class_2350.class_2351;
import net.minecraft.class_239.class_240;

@Environment(EnvType.CLIENT)
public final class BlockOverlayModule extends ClientModule {
   private static final String BOTH = "Both";
   private static final String OUTLINE = "Outline";
   private static final String FILL = "Fill";
   private static final String THEME = "Theme";
   private static final String GRADIENT = "Gradient";
   private static final String SOLID = "Solid";
   private static final String RAINBOW = "Rainbow";
   private static final float THEME_LIGHTEN = 0.55F;
   private static final float FADE_IN = 0.12F;
   private static final float FADE_OUT = 0.2F;
   private static final double SNAP_DISTANCE = 6.0;
   private static final float FLOW_SPEED = 0.35F;
   private static final double SEGMENT = 0.125;
   private static final double FILL_INFLATE = 0.002;
   private static final double LINE_LIFT = 0.004;
   private static final double GLOW_LIFT = 0.002;
   private final ModeSetting style = this.addSetting("style", new ModeSetting("Style", "Both", "Both", "Outline", "Fill"));
   private final ModeSetting colorMode = this.addSetting("color-mode", new ModeSetting("Color Mode", "Theme", "Theme", "Gradient", "Solid", "Rainbow"));
   private final ColorSetting color = this.addSetting("color", new ColorSetting("Color", -871878));
   private final ColorSetting secondColor = this.addSetting("second-color", new ColorSetting("Second Color", -6480));
   private final BooleanSetting flow = this.addSetting("flow", new BooleanSetting("Flow", true));
   private final NumberSetting fillOpacity = this.addSetting("fill-opacity", new NumberSetting("Fill Opacity", 20.0F, 2.0F, 60.0F).step(1.0F));
   private final NumberSetting width = this.addSetting("width", new NumberSetting("Width", 2.0F, 1.0F, 5.0F).step(0.5F));
   private final BooleanSetting glow = this.addSetting("glow", new BooleanSetting("Glow", true));
   private final BooleanSetting smooth = this.addSetting("smooth", new BooleanSetting("Smooth", true));
   private final NumberSetting smoothSpeed = this.addSetting("smooth-speed", new NumberSetting("Smooth Speed", 16.0F, 4.0F, 40.0F).step(1.0F));
   private final BooleanSetting miningProgress = this.addSetting("mining-progress", new BooleanSetting("Mining Progress", true));
   private final BooleanSetting faceOnly = this.addSetting("face-only", new BooleanSetting("Face Only", false));
   private final BooleanSetting seeThrough = this.addSetting("see-through", new BooleanSetting("See Through", false));
   private class_638 level;
   private class_2338 pos;
   private class_2680 state;
   private class_2338 partner;
   private class_2680 partnerState;
   private final List<double[]> edges = new ArrayList<>();
   private List<class_238> boxes = List.of();
   private class_238 localBounds;
   private class_238 goal;
   private class_238 box;
   private class_2350 face;
   private class_238 faceBox;
   private float presence;
   private float progress;
   private long lastFrame;
   private class_243 eye;
   private double time;
   private float fade;
   private double ax;
   private double bx;
   private double ay;
   private double by;
   private double az;
   private double bz;

   public BlockOverlayModule() {
      super("block-overlay", "Block Overlay", "A smooth, animated outline and fill on the block you look at.", ModuleCategory.VISUALS);
      this.color.visibleWhen(() -> !this.colorMode.is("Theme"));
      this.secondColor.visibleWhen(() -> this.colorMode.is("Gradient"));
      this.flow.visibleWhen(() -> !this.colorMode.is("Solid"));
      this.fillOpacity.visibleWhen(() -> !this.style.is("Outline"));
      this.width.visibleWhen(() -> !this.style.is("Fill"));
      this.glow.visibleWhen(() -> !this.style.is("Fill"));
      this.smoothSpeed.visibleWhen(this.smooth::get);
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1687 != null && mc.field_1724 != null) {
         if (mc.field_1687 != this.level) {
            this.reset();
            this.level = mc.field_1687;
         }

         long now = System.nanoTime();
         float dt = this.lastFrame == 0L ? 0.0F : Math.min((float)(now - this.lastFrame) / 1.0E9F, 0.1F);
         this.lastFrame = now;
         class_3965 hit = target(mc);
         if (hit != null && this.adopt(mc, hit)) {
            if (this.box != null && !(this.presence < 0.02F) && this.smooth.get() && !(this.box.method_1005().method_1022(this.goal.method_1005()) > 6.0)) {
               this.box = lerp(this.box, this.goal, 1.0F - (float)Math.exp(-this.smoothSpeed.get() * dt));
            } else {
               this.box = this.goal;
            }

            this.presence = Math.min(1.0F, this.presence + dt / 0.12F);
         } else {
            hit = null;
            this.presence = Math.max(0.0F, this.presence - dt / 0.2F);
            if (this.presence == 0.0F) {
               this.reset();
               this.level = mc.field_1687;
               return;
            }
         }

         if (this.box != null) {
            this.updateProgress(mc, dt, hit != null);
            this.eye = mc.field_1773.method_19418().method_71156();
            this.time = now % 3600000000000L / 1.0E9;
            this.draw();
         }
      } else {
         this.reset();
      }
   }

   private static class_3965 target(class_310 mc) {
      if (!(mc.field_1765 instanceof class_3965 hit && hit.method_17783() == class_240.field_1332 && !hit.method_17781())) {
         return null;
      } else {
         if (!((AccessorGameRenderer)mc.field_1773).anubis$shouldRenderBlockOutline()) {
            return null;
         }

         class_2338 at = hit.method_17777();
         return !mc.field_1687.method_8320(at).method_26215() && mc.field_1687.method_8621().method_11952(at) ? hit : null;
      }
   }

   private boolean adopt(class_310 mc, class_3965 hit) {
      class_638 world = mc.field_1687;
      class_2338 at = hit.method_17777();
      class_2338 other = partner(world, at, world.method_8320(at));
      class_2338 ref = other != null && other.method_10265(at) < 0 ? other : at;
      class_2338 second = other == null ? null : (ref.equals(at) ? other : at);
      class_2680 refState = world.method_8320(ref);
      class_2680 secondState = second == null ? null : world.method_8320(second);
      if (!ref.equals(this.pos) || refState != this.state || secondState != this.partnerState) {
         class_1297 viewer = mc.method_1560();
         class_3726 context = viewer == null ? class_3726.method_16194() : class_3726.method_16195(viewer);
         class_265 shape = refState.method_26172(world, ref, context);
         if (second != null) {
            shape = class_259.method_1084(shape, secondState.method_26172(world, second, context).method_66507(second.method_10059(ref)));
         }

         shape = shape.method_1097();
         if (shape.method_1110()) {
            return false;
         }

         if (!ref.equals(this.pos)) {
            this.progress = 0.0F;
         }

         this.pos = ref;
         this.state = refState;
         this.partner = second;
         this.partnerState = secondState;
         this.edges.clear();
         shape.method_1104((x0, y0, z0, x1, y1, z1) -> this.edges.add(new double[]{x0, y0, z0, x1, y1, z1}));
         this.boxes = shape.method_1090();
         this.localBounds = shape.method_1107();
         this.goal = this.localBounds.method_996(ref);
      }

      this.face = hit.method_17780();
      this.faceBox = this.boxOnFace(hit.method_17784().method_1023(this.pos.method_10263(), this.pos.method_10264(), this.pos.method_10260()));
      return true;
   }

   private static class_2338 partner(class_638 world, class_2338 at, class_2680 state) {
      class_2338 other = null;
      if (state.method_28498(class_2741.field_12533)) {
         other = state.method_11654(class_2741.field_12533) == class_2756.field_12607 ? at.method_10084() : at.method_10074();
      } else if (state.method_26204() instanceof class_2244) {
         other = at.method_10093(class_2244.method_24163(state));
      } else if (state.method_26204() instanceof class_2281 && state.method_11654(class_2281.field_10770) != class_2745.field_12569) {
         other = class_2281.method_72581(at, state);
      }

      return other != null && world.method_8320(other).method_27852(state.method_26204()) ? other : null;
   }

   private class_238 boxOnFace(class_243 local) {
      double e = 0.001;

      for (class_238 b : this.boxes) {
         boolean onPlane = switch (this.face) {
            case field_11034 -> Math.abs(local.field_1352 - b.field_1320) < e;
            case field_11039 -> Math.abs(local.field_1352 - b.field_1323) < e;
            case field_11036 -> Math.abs(local.field_1351 - b.field_1325) < e;
            case field_11033 -> Math.abs(local.field_1351 - b.field_1322) < e;
            case field_11035 -> Math.abs(local.field_1350 - b.field_1324) < e;
            case field_11043 -> Math.abs(local.field_1350 - b.field_1321) < e;
            default -> throw new MatchException(null, null);
         };
         if (onPlane && b.method_1014(e).method_1006(local)) {
            return b;
         }
      }

      return this.localBounds;
   }

   private void updateProgress(class_310 mc, float dt, boolean targeted) {
      float want = 0.0F;
      class_636 gameMode = mc.field_1761;
      if (targeted && this.miningProgress.get() && gameMode != null && gameMode.method_2923()) {
         AccessorBlockOverlayMultiPlayerGameMode mining = (AccessorBlockOverlayMultiPlayerGameMode)gameMode;
         class_2338 breaking = mining.anubis$blockOverlayDestroyPos();
         if (breaking != null && (breaking.equals(this.pos) || breaking.equals(this.partner))) {
            want = class_3532.method_15363(mining.anubis$blockOverlayDestroyProgress(), 0.0F, 1.0F);
         }
      }

      this.progress = this.progress + (want - this.progress) * (1.0F - (float)Math.exp(-18.0F * dt));
   }

   private void reset() {
      this.level = null;
      this.pos = null;
      this.state = null;
      this.partner = null;
      this.partnerState = null;
      this.edges.clear();
      this.boxes = List.of();
      this.localBounds = null;
      this.goal = null;
      this.box = null;
      this.face = null;
      this.faceBox = null;
      this.presence = 0.0F;
      this.progress = 0.0F;
      this.lastFrame = 0L;
   }

   private void draw() {
      this.fade = this.presence * this.presence * (3.0F - 2.0F * this.presence);
      double pop = 0.94 + 0.06 * this.fade;
      double[] x = axis(
         this.box.field_1323, this.box.field_1320, this.goal.field_1323, this.goal.field_1320, this.pos.method_10263() - this.goal.field_1323, pop
      );
      double[] y = axis(
         this.box.field_1322, this.box.field_1325, this.goal.field_1322, this.goal.field_1325, this.pos.method_10264() - this.goal.field_1322, pop
      );
      double[] z = axis(
         this.box.field_1321, this.box.field_1324, this.goal.field_1321, this.goal.field_1324, this.pos.method_10260() - this.goal.field_1321, pop
      );
      this.ax = x[0];
      this.bx = x[1];
      this.ay = y[0];
      this.by = y[1];
      this.az = z[0];
      this.bz = z[1];
      boolean lines = !this.style.is("Fill");
      boolean fill = !this.style.is("Outline");
      float fillAlpha = this.fillOpacity.get() / 100.0F;
      if (fill) {
         if (this.faceOnly.get()) {
            this.faceFill(this.faceBox, this.face, fillAlpha, 0.002);
         } else {
            for (class_238 b : this.boxes) {
               this.boxFill(b, fillAlpha, 0.002);
            }
         }
      }

      if (this.progress > 0.01F) {
         this.progressFill(Math.min(0.45F, fillAlpha * 1.4F + 0.16F));
      }

      if (lines) {
         List<double[]> outlineEdges = this.edges;
         if (this.faceOnly.get()) {
            outlineEdges = faceEdges(this.faceBox, this.face);
         }

         this.outline(outlineEdges);
      }
   }

   private static double[] axis(double boxMin, double boxMax, double goalMin, double goalMax, double offset, double pop) {
      double goalSize = goalMax - goalMin;
      double scale = goalSize > 1.0E-6 ? (boxMax - boxMin) / goalSize : 1.0;
      double centre = (boxMin + boxMax) * 0.5;
      double a = centre * (1.0 - pop) + (boxMin + offset * scale) * pop;
      return new double[]{a, scale * pop};
   }

   private class_243 world(double lx, double ly, double lz) {
      return new class_243(this.ax + this.bx * lx, this.ay + this.by * ly, this.az + this.bz * lz);
   }

   private void outline(List<double[]> list) {
      boolean varies = !this.colorMode.is("Solid");
      List<double[]> pieces = new ArrayList<>(list.size() * 4);
      List<Integer> colors = new ArrayList<>(list.size() * 4);

      for (double[] e : list) {
         double length = Math.sqrt(sq(e[3] - e[0]) + sq(e[4] - e[1]) + sq(e[5] - e[2]));
         int n = varies ? class_3532.method_15340((int)Math.ceil(length / 0.125), 1, 16) : 1;

         for (int i = 0; i < n; i++) {
            double t0 = (double)i / n;
            double t1 = (double)(i + 1) / n;
            double tm = (t0 + t1) * 0.5;
            pieces.add(
               new double[]{
                  class_3532.method_16436(t0, e[0], e[3]),
                  class_3532.method_16436(t0, e[1], e[4]),
                  class_3532.method_16436(t0, e[2], e[5]),
                  class_3532.method_16436(t1, e[0], e[3]),
                  class_3532.method_16436(t1, e[1], e[4]),
                  class_3532.method_16436(t1, e[2], e[5])
               }
            );
            colors.add(this.colorAt(class_3532.method_16436(tm, e[0], e[3]), class_3532.method_16436(tm, e[1], e[4]), class_3532.method_16436(tm, e[2], e[5])));
         }
      }

      float w = this.width.get();
      if (this.glow.get()) {
         for (int i = 0; i < pieces.size(); i++) {
            double[] p = pieces.get(i);
            int c = colors.get(i);
            this.line(p, class_9848.method_75601(c, 0.34F * this.fade), w + 3.0F, 0.002);
            this.line(p, class_9848.method_75601(c, 0.13F * this.fade), w + 8.0F, 0.002);
         }
      }

      for (int i = 0; i < pieces.size(); i++) {
         this.line(pieces.get(i), class_9848.method_75601(colors.get(i), this.fade), w, 0.004);
      }
   }

   private void line(double[] p, int argb, float w, double lift) {
      class_12178 drawn = class_12180.method_75546(this.lift(this.world(p[0], p[1], p[2]), lift), this.lift(this.world(p[3], p[4], p[5]), lift), argb, w);
      if (this.seeThrough.get()) {
         drawn.method_75533();
      }
   }

   private class_243 lift(class_243 point, double by) {
      class_243 toEye = this.eye.method_1020(point);
      double length = toEye.method_1033();
      return length < 1.0E-6 ? point : point.method_1019(toEye.method_1021(by / length));
   }

   private void boxFill(class_238 b, float alpha, double inflate) {
      for (class_2350 d : class_2350.values()) {
         this.faceFill(b, d, alpha, inflate);
      }
   }

   private void faceFill(class_238 b, class_2350 d, float alpha, double inflate) {
      class_243 min = this.world(b.field_1323, b.field_1322, b.field_1321).method_1023(inflate, inflate, inflate);
      class_243 max = this.world(b.field_1320, b.field_1325, b.field_1324).method_1031(inflate, inflate, inflate);
      class_243 centre = faceCentre(b, d);
      int c = this.colorAt(centre.field_1352, centre.field_1351, centre.field_1350);
      int argb = class_3532.method_15340(Math.round(alpha * this.fade * 255.0F), 0, 255) << 24 | c & 16777215;
      class_12178 drawn = class_12180.method_75548(min, max, d, class_12179.method_75539(argb));
      if (this.seeThrough.get()) {
         drawn.method_75533();
      }
   }

   private void progressFill(float alpha) {
      double inflate = 0.004;
      if (this.faceOnly.get()) {
         class_238 b = this.faceBox;
         class_238 part = this.face.method_10166() == class_2351.field_11052
            ? b.method_35580(b.method_17939() * (1.0F - this.progress) * 0.5, 0.0, b.method_17941() * (1.0F - this.progress) * 0.5)
            : new class_238(b.field_1323, b.field_1322, b.field_1321, b.field_1320, b.field_1322 + b.method_17940() * this.progress, b.field_1324);
         this.faceFill(part, this.face, alpha, inflate);
      } else {
         double top = this.localBounds.field_1322 + this.localBounds.method_17940() * this.progress;

         for (class_238 b : this.boxes) {
            if (!(b.field_1322 >= top)) {
               this.boxFill(new class_238(b.field_1323, b.field_1322, b.field_1321, b.field_1320, Math.min(b.field_1325, top), b.field_1324), alpha, inflate);
            }
         }
      }
   }

   private int colorAt(double lx, double ly, double lz) {
      boolean themed = this.colorMode.is("Theme");
      int base = themed ? CosmeticAccent.argb() : ThemeTint.world(this.color.argb());
      if (this.colorMode.is("Solid")) {
         return base;
      } else {
         double u = (
               norm(lx, this.localBounds.field_1323, this.localBounds.field_1320)
                  + norm(ly, this.localBounds.field_1322, this.localBounds.field_1325)
                  + norm(lz, this.localBounds.field_1321, this.localBounds.field_1324)
            )
            / 3.0;
         double shift = this.flow.get() ? this.time * 0.35F : 0.0;
         if (this.colorMode.is("Rainbow")) {
            double hue = u * 0.5 - shift;
            return ThemeTint.world(base & 0xFF000000 | class_3532.method_15369((float)(hue - Math.floor(hue)), 0.65F, 1.0F) & 16777215);
         } else {
            double k = this.flow.get() ? 0.5 - 0.5 * Math.cos((Math.PI * 2) * (u - shift)) : u;
            int second = themed ? class_9848.method_61319(0.55F, base, -1) : ThemeTint.world(this.secondColor.argb());
            return class_9848.method_61319((float)k, base, second);
         }
      }
   }

   private static double norm(double v, double min, double max) {
      return max - min < 1.0E-6 ? 0.5 : (v - min) / (max - min);
   }

   private static class_243 faceCentre(class_238 b, class_2350 d) {
      class_243 c = b.method_1005();

      return switch (d) {
         case field_11034 -> new class_243(b.field_1320, c.field_1351, c.field_1350);
         case field_11039 -> new class_243(b.field_1323, c.field_1351, c.field_1350);
         case field_11036 -> new class_243(c.field_1352, b.field_1325, c.field_1350);
         case field_11033 -> new class_243(c.field_1352, b.field_1322, c.field_1350);
         case field_11035 -> new class_243(c.field_1352, c.field_1351, b.field_1324);
         case field_11043 -> new class_243(c.field_1352, c.field_1351, b.field_1321);
         default -> throw new MatchException(null, null);
      };
   }

   private static List<double[]> faceEdges(class_238 b, class_2350 d) {
      double[][] c = switch (d.method_10166()) {
         case field_11048 -> {
            double x = d == class_2350.field_11034 ? b.field_1320 : b.field_1323;
            yield new double[][]{
               {x, b.field_1322, b.field_1321}, {x, b.field_1325, b.field_1321}, {x, b.field_1325, b.field_1324}, {x, b.field_1322, b.field_1324}
            };
         }
         case field_11052 -> {
            double y = d == class_2350.field_11036 ? b.field_1325 : b.field_1322;
            yield new double[][]{
               {b.field_1323, y, b.field_1321}, {b.field_1320, y, b.field_1321}, {b.field_1320, y, b.field_1324}, {b.field_1323, y, b.field_1324}
            };
         }
         case field_11051 -> {
            double z = d == class_2350.field_11035 ? b.field_1324 : b.field_1321;
            yield new double[][]{
               {b.field_1323, b.field_1322, z}, {b.field_1320, b.field_1322, z}, {b.field_1320, b.field_1325, z}, {b.field_1323, b.field_1325, z}
            };
         }
         default -> throw new MatchException(null, null);
      };
      List<double[]> edges = new ArrayList<>(4);

      for (int i = 0; i < 4; i++) {
         double[] p = c[i];
         double[] q = c[(i + 1) % 4];
         edges.add(new double[]{p[0], p[1], p[2], q[0], q[1], q[2]});
      }

      return edges;
   }

   private static class_238 lerp(class_238 from, class_238 to, float k) {
      return new class_238(
         class_3532.method_16436(k, from.field_1323, to.field_1323),
         class_3532.method_16436(k, from.field_1322, to.field_1322),
         class_3532.method_16436(k, from.field_1321, to.field_1321),
         class_3532.method_16436(k, from.field_1320, to.field_1320),
         class_3532.method_16436(k, from.field_1325, to.field_1325),
         class_3532.method_16436(k, from.field_1324, to.field_1324)
      );
   }

   private static double sq(double v) {
      return v * v;
   }
}
