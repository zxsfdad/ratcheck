package net.anubis.client.module.impl.render;

import imgui.ImDrawList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.gui.WorldLabel;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.HudOverlayProvider;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.misc.NameProtectModule;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.module.setting.Setting;
import net.anubis.client.util.Ballistics;
import net.anubis.client.util.combat.CombatTarget;
import net.anubis.client.util.glyph.GlyphCanvas;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12178;
import net.minecraft.class_12179;
import net.minecraft.class_12180;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1665;
import net.minecraft.class_1676;
import net.minecraft.class_1680;
import net.minecraft.class_1681;
import net.minecraft.class_1683;
import net.minecraft.class_1684;
import net.minecraft.class_1685;
import net.minecraft.class_1686;
import net.minecraft.class_1753;
import net.minecraft.class_1764;
import net.minecraft.class_1771;
import net.minecraft.class_1776;
import net.minecraft.class_1779;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1811;
import net.minecraft.class_1823;
import net.minecraft.class_1835;
import net.minecraft.class_1890;
import net.minecraft.class_2350;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_4537;
import net.minecraft.class_638;
import net.minecraft.class_746;
import net.minecraft.class_9278;
import net.minecraft.class_9284;
import net.minecraft.class_9334;
import net.minecraft.class_9701;
import net.minecraft.class_9848;
import net.minecraft.class_2350.class_2351;

@Environment(EnvType.CLIENT)
public final class TrajectoriesModule extends ClientModule implements HudOverlayProvider {
   private static final String PATH_BEADS = "Beads";
   private static final String PATH_DASHES = "Dashes";
   private static final String PATH_SOLID = "Solid";
   private static final String COLOR_THEME = "Theme";
   private static final String COLOR_MANUAL = "Manual";
   private static final String MARKER_ANKH = "Ankh";
   private static final String MARKER_SUN = "Sun Disk";
   private static final String MARKER_NONE = "None";
   private static final int WHITE = -1;
   private static final float HEAD_LIFT = 0.35F;
   private static final float SHEEN_LIFT = 0.8F;
   private static final float PLATE_LIFT = 0.55F;
   private static final float HIT_LIFT = 0.5F;
   private static final int VOLLEY_CAP = 3;
   private static final int TRACK_CAP = 24;
   private static final int TRACK_LIFETIME = 200;
   private static final double RESTING_SPEED_SQR = 1.0E-4;
   private static final double AIM_DRIFT = 0.001;
   private static final double AIM_SWAY = 5.0E-4;
   private static final float BOW_MIN_PULL = 0.1F;
   private static final float BOW_SPEED = 3.0F;
   private static final int TRIDENT_WINDUP = 10;
   private static final float CROSSBOW_ARROW_SPEED = 3.15F;
   private static final float CROSSBOW_ROCKET_SPEED = 1.6F;
   private static final float MULTISHOT_FAN = 10.0F;
   private static final float BOTTLE_SPEED = 0.7F;
   private static final float LOB_TILT = -20.0F;
   private static final double MUZZLE_DROP = 0.1;
   private static final double ROCKET_DROP = 0.15;
   private static final int ROCKET_FUSE_SLACK = 6;
   private static final double BEAD_PERIOD = 0.42;
   private static final double BEAD_ANGLE = 0.011;
   private static final double BEAD_MIN = 0.035;
   private static final double BEAD_MAX = 0.2;
   private static final double DASH_PERIOD = 0.72;
   private static final double DASH_LENGTH = 0.44;
   private static final double SOLID_PIECE = 0.6;
   private static final double FLOW_SPEED = 1.3;
   private static final double FADE_IN = 1.4;
   private static final double SHEEN_SPEED = 9.0;
   private static final double SHEEN_REACH = 1.6;
   private static final double SHEEN_REST = 14.0;
   private static final float HALO_ALPHA = 0.22F;
   private static final float HALO_WIDTH = 2.8F;
   private static final double HALO_NEAR = 24.0;
   private static final double HALO_FAR = 40.0;
   private static final int AIM_PIECES = 200;
   private static final int TRACK_PIECES = 110;
   private static final float GHOST_ALPHA = 0.38F;
   private static final float STRAY_ALPHA = 0.7F;
   private static final float FLYING_ALPHA = 0.85F;
   private static final float UNFURL_SECONDS = 0.32F;
   private static final double MARKER_UNIT = 0.5;
   private static final double MARKER_LIFT = 0.025;
   private static final int RIM_STEPS = 24;
   private static final int ORBIT_ARCS = 10;
   private static final int SUN_RAYS = 12;
   private static final long TARGET_MEMORY_MS = 6000L;
   private static final long LABEL_TTL_NANOS = 250000000L;
   private static final long EPOCH = System.nanoTime();
   private static final String PEARL_PREDICT_ID = "predict";
   private static final String PEARL_OTHERS_ID = "others-pearls";
   private static final double[] RIM_COS = new double[25];
   private static final double[] RIM_SIN = new double[25];
   private static final double DIAGONAL = 1.0 / Math.sqrt(3.0);
   private static final double[][] BURST_RAYS = new double[][]{
      {1.0, 0.0, 0.0},
      {-1.0, 0.0, 0.0},
      {0.0, 1.0, 0.0},
      {0.0, -1.0, 0.0},
      {0.0, 0.0, 1.0},
      {0.0, 0.0, -1.0},
      {DIAGONAL, DIAGONAL, DIAGONAL},
      {-DIAGONAL, DIAGONAL, DIAGONAL},
      {DIAGONAL, -DIAGONAL, DIAGONAL},
      {DIAGONAL, DIAGONAL, -DIAGONAL},
      {-DIAGONAL, -DIAGONAL, DIAGONAL},
      {-DIAGONAL, DIAGONAL, -DIAGONAL},
      {DIAGONAL, -DIAGONAL, -DIAGONAL},
      {-DIAGONAL, -DIAGONAL, -DIAGONAL}
   };
   private final BooleanSetting aimPreview = this.addSetting("aim-preview", new BooleanSetting("Aim Preview", true));
   private final BooleanSetting ghostArc = this.addSetting("ghost-arc", new BooleanSetting("Ghost Arc", true));
   private final BooleanSetting inFlight = this.addSetting("in-flight", new BooleanSetting("In Flight", true));
   private final BooleanSetting othersShots = this.addSetting("others-shots", new BooleanSetting("Others' Shots", true));
   private final NumberSetting range = this.addSetting("range", new NumberSetting("Range", 64.0F, 16.0F, 160.0F).step(8.0F));
   private final BooleanSetting bows = this.addSetting("bows", new BooleanSetting("Bows & Crossbows", true));
   private final BooleanSetting tridents = this.addSetting("tridents", new BooleanSetting("Tridents", true));
   private final BooleanSetting throwables = this.addSetting("throwables", new BooleanSetting("Throwables", true));
   private final BooleanSetting pearls = this.addSetting("pearls", new BooleanSetting("Ender Pearls", true));
   private final ModeSetting pathStyle = this.addSetting("path-style", new ModeSetting("Path", "Beads", "Beads", "Dashes", "Solid"));
   private final BooleanSetting flow = this.addSetting("flow", new BooleanSetting("Flow", true));
   private final ModeSetting colorMode = this.addSetting("color", new ModeSetting("Color", "Theme", "Theme", "Manual"));
   private final ColorSetting pathColor = this.addSetting("path-color", new ColorSetting("Path Color", -13338378));
   private final NumberSetting thickness = this.addSetting("thickness", new NumberSetting("Thickness", 2.25F, 0.5F, 5.0F).step(0.25F));
   private final BooleanSetting halo = this.addSetting("halo", new BooleanSetting("Halo", true));
   private final BooleanSetting throughWalls = this.addSetting("through-walls", new BooleanSetting("Through Walls", true));
   private final ModeSetting marker = this.addSetting("marker", new ModeSetting("Marker", "Ankh", "Ankh", "Sun Disk", "None"));
   private final NumberSetting markerScale = this.addSetting("marker-scale", new NumberSetting("Marker Scale", 1.0F, 0.5F, 2.0F).step(0.1F));
   private final BooleanSetting targetBox = this.addSetting("target-box", new BooleanSetting("Target Box", true));
   private final ColorSetting hitColor = this.addSetting("hit-color", new ColorSetting("Hit Color", -6635270));
   private final BooleanSetting flightTime = this.addSetting("flight-time", new BooleanSetting("Flight Time", true));
   private final TrajectoriesModule.Aim[] volley = new TrajectoriesModule.Aim[3];
   private int volleySize;
   private boolean volleyGhost;
   private class_1792 aimItem;
   private long aimSince = -1L;
   private long markerSince = -1L;
   private final Int2ObjectOpenHashMap<TrajectoriesModule.Track> tracks = new Int2ObjectOpenHashMap();
   private final ArrayDeque<TrajectoriesModule.Track> spareTracks = new ArrayDeque<>();
   private int sweep;
   private class_638 level;
   private final GlyphCanvas canvas = new GlyphCanvas();
   private final List<TrajectoriesModule.Label> labels = new ArrayList<>();
   private long labelsAt;
   private int headTint;
   private int tailTint;
   private int haloTint;
   private int markTint;
   private int ringTint;
   private int sheenTint;
   private int plateTint;
   private int hitTint;
   private boolean onTop;
   private class_243 camera = class_243.field_1353;
   private double strokeTotal;
   private double strokeSheen;
   private float strokeAlpha;
   private float strokeWidth;
   private boolean strokeFade;
   private boolean strokeHalo;
   private double gx;
   private double gy;
   private double gz;
   private double rx;
   private double ry;
   private double rz;
   private double ux;
   private double uy;
   private double uz;

   public TrajectoriesModule() {
      super("trajectories", "Trajectories", "Shows where your projectiles will land.", ModuleCategory.RENDER);

      for (int i = 0; i < 3; i++) {
         this.volley[i] = new TrajectoriesModule.Aim();
      }

      this.ghostArc.visibleWhen(this.aimPreview::get);
      this.othersShots.visibleWhen(this.inFlight::get);
      this.range.visibleWhen(this.inFlight::get);
      this.flow.visibleWhen(() -> !this.pathStyle.is("Solid"));
      this.pathColor.visibleWhen(() -> this.colorMode.is("Manual"));
      this.markerScale.visibleWhen(() -> !this.marker.is("None"));
      this.hitColor.visibleWhen(() -> this.targetBox.get() && this.colorMode.is("Manual"));
   }

   @Override
   protected void onEnable() {
      this.reset();
      this.on(TickEvent.TYPE, Stage.POST, this::onTick);
      this.on(Render3DEvent.TYPE, this::onRender);
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   private void reset() {
      this.forgetAim();
      this.dropTracks();
      this.labels.clear();
      this.level = null;
   }

   private void syncLevel(class_638 world) {
      if (world != this.level) {
         this.reset();
         this.level = world;
      }
   }

   private void forgetAim() {
      for (TrajectoriesModule.Aim aim : this.volley) {
         aim.forget();
      }

      this.volleySize = 0;
      this.volleyGhost = false;
      this.aimItem = null;
      this.aimSince = -1L;
      this.markerSince = -1L;
   }

   private void dropTracks() {
      if (!this.tracks.isEmpty()) {
         ObjectIterator var1 = this.tracks.values().iterator();

         while (var1.hasNext()) {
            TrajectoriesModule.Track track = (TrajectoriesModule.Track)var1.next();
            this.recycle(track);
         }

         this.tracks.clear();
      }
   }

   private void recycle(TrajectoriesModule.Track track) {
      track.shot = null;
      track.departed = false;
      track.forget();
      if (this.spareTracks.size() < 24) {
         this.spareTracks.push(track);
      }
   }

   private void onTick(TickEvent event) {
      this.sweep++;
      class_310 mc = class_310.method_1551();
      class_638 world = mc.field_1687;
      class_746 player = mc.field_1724;
      this.syncLevel(world);
      if (world != null && player != null && this.inFlight.get()) {
         double reach = this.range.get() * this.range.get();
         PearlTrajectoryModule pearlModule = activePearlModule();
         boolean otherPearlsTaken = pearlModule != null && pearlToggle(pearlModule, "others-pearls");
         boolean others = this.othersShots.get();
         boolean captions = this.flightTime.get();
         class_243 eye = player.method_33571();
         int budget = 24;

         for (class_1297 entity : world.method_18112()) {
            if (entity instanceof class_1676 shot && !shot.method_31481()) {
               class_1297 owner = shot.method_24921();
               boolean own = owner == player;
               if (own || others) {
                  boolean pearlTaken = pearlModule != null && (owner == null || own || otherPearlsTaken);
                  Ballistics.Body body = this.bodyOf(shot, pearlTaken);
                  if (body != null && !(shot.method_5858(player) > reach)) {
                     class_243 motion = shot.method_18798();
                     if (!(motion.method_1027() < 1.0E-4) && !resting(shot)) {
                        if (budget-- == 0) {
                           break;
                        }

                        TrajectoriesModule.Track track = (TrajectoriesModule.Track)this.tracks.get(shot.method_5628());
                        if (track == null) {
                           track = this.spareTracks.isEmpty() ? new TrajectoriesModule.Track() : this.spareTracks.pop();
                           this.tracks.put(shot.method_5628(), track);
                        }

                        track.shot = shot;
                        track.seen = this.sweep;
                        track.flight
                           .launch(body, shot.method_73189(), motion)
                           .bounds(shot.method_17681(), shot.method_17682())
                           .gravity(shot.method_5740() ? 0.0 : body.gravity())
                           .wet(shot.method_5799())
                           .age(shot.field_6012)
                           .lifetime(200)
                           .ignoring(owner, shot)
                           .leftShooter(track.departed)
                           .solve(world);
                        track.departed = track.flight.hasLeftShooter();
                        track.caption(eye, captions && (own || track.flight.outcome() == Ballistics.Outcome.ENTITY));
                     }
                  }
               }
            }
         }

         if (!this.tracks.isEmpty()) {
            this.tracks.values().removeIf(this::stale);
         }
      } else {
         this.dropTracks();
      }
   }

   private boolean stale(TrajectoriesModule.Track track) {
      if (track.seen == this.sweep) {
         return false;
      }

      this.recycle(track);
      return true;
   }

   private static boolean resting(class_1676 shot) {
      double dx = shot.method_23317() - shot.field_6014;
      double dy = shot.method_23318() - shot.field_6036;
      double dz = shot.method_23321() - shot.field_5969;
      return shot.field_6012 > 1 && dx * dx + dy * dy + dz * dz < 1.0E-4;
   }

   private Ballistics.Body bodyOf(class_1676 shot, boolean pearlsTaken) {
      if (shot instanceof class_1685 trident) {
         return this.tridents.get() && !trident.method_7441() ? Ballistics.Body.TRIDENT : null;
      } else if (shot instanceof class_1665 arrow) {
         return this.bows.get() && !arrow.method_7441() ? Ballistics.Body.ARROW : null;
      } else if (!(shot instanceof class_1684)) {
         if (!this.throwables.get()) {
            return null;
         } else if (shot instanceof class_1686) {
            return Ballistics.Body.FLASK;
         } else if (shot instanceof class_1683) {
            return Ballistics.Body.BOTTLE;
         } else {
            return !(shot instanceof class_1680) && !(shot instanceof class_1681) ? null : Ballistics.Body.ORB;
         }
      } else {
         return this.pearls.get() && !pearlsTaken ? Ballistics.Body.ORB : null;
      }
   }

   private static PearlTrajectoryModule activePearlModule() {
      PearlTrajectoryModule pearl = ModuleRegistry.get(PearlTrajectoryModule.class);
      return pearl != null && pearl.isEnabled() ? pearl : null;
   }

   private static boolean pearlToggle(PearlTrajectoryModule pearl, String id) {
      for (Setting setting : pearl.settings()) {
         if (id.equals(setting.id()) && setting instanceof BooleanSetting toggle) {
            return toggle.get();
         }
      }

      return true;
   }

   private void onRender(Render3DEvent event) {
      this.labels.clear();
      class_310 mc = class_310.method_1551();
      class_638 world = mc.field_1687;
      class_746 player = mc.field_1724;
      this.syncLevel(world);
      if (world != null && player != null) {
         long now = System.nanoTime();
         this.labelsAt = now;
         float time = (float)(now - EPOCH) / 1.0E9F;
         float partialTick = event.partialTick;
         this.choosePalette();
         this.onTop = this.throughWalls.get();
         this.canvas.begin(mc.field_1773.method_19418());
         this.camera = mc.field_1773.method_19418().method_71156();
         class_1309 focus = this.targetBox.get() ? CombatTarget.current(6000L) : null;
         if (this.aimPreview.get()) {
            this.drawAim(world, player, partialTick, now, time, focus);
         } else if (this.aimItem != null || this.volleySize > 0) {
            this.forgetAim();
         }

         if (this.inFlight.get()) {
            this.drawTracks(player, partialTick, time, focus);
         }

         this.canvas.submit(this.onTop);
      }
   }

   private void drawTracks(class_746 player, float partialTick, float time, class_1309 focus) {
      ObjectIterator var5 = this.tracks.values().iterator();

      while (var5.hasNext()) {
         TrajectoriesModule.Track track = (TrajectoriesModule.Track)var5.next();
         class_1676 shot = track.shot;
         if (shot != null && !shot.method_31481()) {
            boolean own = shot.method_24921() == player;
            float alpha = own ? 1.0F : 0.7F;
            this.drawPath(track.flight, shot.method_30950(partialTick), Double.MAX_VALUE, alpha * 0.85F, false, false, own, time);
            this.drawImpact(track.flight, 1.0F, alpha, time, partialTick, focus);
            this.addLabel(track);
         }
      }
   }

   private void choosePalette() {
      boolean manual = this.colorMode.is("Manual");
      int base = manual ? this.pathColor.argb() : CosmeticAccent.argb();
      this.headTint = class_9848.method_61319(0.35F, base, -1);
      this.tailTint = base;
      this.haloTint = base;
      this.markTint = base;
      this.ringTint = this.headTint;
      this.sheenTint = class_9848.method_61319(0.8F, base, -1);
      this.plateTint = class_9848.method_61319(0.55F, base, -1);
      this.hitTint = manual ? this.hitColor.argb() : class_9848.method_61319(0.5F, base, -1);
   }

   private void drawAim(class_638 world, class_746 player, float partialTick, long now, float time, class_1309 focus) {
      class_1799 stack = heldLauncher(player);
      this.volleySize = 0;
      this.volleyGhost = false;
      if (!stack.method_7960()) {
         this.loadVolley(world, player, stack, partialTick);
      }

      if (this.volleySize == 0) {
         if (this.aimItem != null) {
            this.forgetAim();
         }
      } else {
         class_1792 item = stack.method_7909();
         if (item != this.aimItem) {
            this.aimItem = item;
            this.aimSince = now;
            this.markerSince = -1L;
         }

         float unfurl = easeOutExpo((float)(now - this.aimSince) / 1.0E9F / 0.32F);
         Ballistics.Flight lead = this.volley[0].flight;
         if (unfurl < 1.0F || lead.outcome() == Ballistics.Outcome.OPEN) {
            this.markerSince = -1L;
         } else if (this.markerSince < 0L) {
            this.markerSince = now;
         }

         float grow = this.markerSince < 0L ? 0.0F : settle((float)(now - this.markerSince) / 1.0E9F);
         float alpha = this.volleyGhost ? 0.38F : 1.0F;

         for (int i = 0; i < this.volleySize; i++) {
            Ballistics.Flight flight = this.volley[i].flight;
            this.drawPath(flight, null, flight.length() * unfurl, alpha, true, !this.volleyGhost, true, time);
            if (grow > 0.0F) {
               this.drawImpact(flight, grow, alpha, time, partialTick, focus);
            }
         }

         if (!this.volleyGhost && unfurl >= 1.0F) {
            this.addLabel(this.volley[0]);
         }
      }
   }

   private static class_1799 heldLauncher(class_746 player) {
      if (player.method_6115()) {
         return player.method_6030();
      }

      class_1799 main = player.method_6047();
      return launches(main.method_7909()) ? main : player.method_6079();
   }

   private static boolean launches(class_1792 item) {
      return item instanceof class_1811
         || item instanceof class_1835
         || item instanceof class_1823
         || item instanceof class_1771
         || item instanceof class_1776
         || item instanceof class_4537
         || item instanceof class_1779;
   }

   private void loadVolley(class_638 world, class_746 player, class_1799 stack, float partialTick) {
      class_1792 item = stack.method_7909();
      boolean drawing = player.method_6115() && player.method_6030() == stack;
      if (item instanceof class_1753) {
         if (!this.bows.get() || player.method_18808(stack).method_7960() || !drawing && !this.ghostArc.get()) {
            return;
         }

         float pull = drawing ? bowPull(player.method_6048() + partialTick) : 1.0F;
         this.volleyGhost = !drawing || pull < 0.1F;
         this.aimed(world, player, partialTick, Ballistics.Body.ARROW, Math.max(pull, 0.1F) * 3.0F, 0.0F);
      } else if (item instanceof class_1764) {
         if (this.bows.get()) {
            this.crossbowVolley(world, player, partialTick, (class_9278)stack.method_58695(class_9334.field_49649, class_9278.field_49298));
         }
      } else if (item instanceof class_1835) {
         if (!this.tridents.get() || !drawing && !this.ghostArc.get()) {
            return;
         }

         if (class_1890.method_60142(stack, class_9701.field_51651)) {
            return;
         }

         this.volleyGhost = !drawing || player.method_6048() < 10;
         this.aimed(world, player, partialTick, Ballistics.Body.TRIDENT, 2.5F, 0.0F);
      } else if (item instanceof class_1776) {
         PearlTrajectoryModule pearlModule = activePearlModule();
         if (this.pearls.get() && (pearlModule == null || !pearlToggle(pearlModule, "predict"))) {
            this.aimed(world, player, partialTick, Ballistics.Body.ORB, class_1776.field_55033, 0.0F);
         }
      } else if (this.throwables.get()) {
         this.lob(world, player, partialTick, item);
      }
   }

   private void lob(class_638 world, class_746 player, float partialTick, class_1792 item) {
      if (item instanceof class_1823) {
         this.aimed(world, player, partialTick, Ballistics.Body.ORB, class_1823.field_55045, 0.0F);
      } else if (item instanceof class_1771) {
         this.aimed(world, player, partialTick, Ballistics.Body.ORB, 1.5F, 0.0F);
      } else if (item instanceof class_4537) {
         this.aimed(world, player, partialTick, Ballistics.Body.FLASK, class_4537.field_55046, -20.0F);
      } else if (item instanceof class_1779) {
         this.aimed(world, player, partialTick, Ballistics.Body.BOTTLE, 0.7F, -20.0F);
      }
   }

   private static float bowPull(float ticks) {
      float seconds = ticks / 20.0F;
      return Math.min(1.0F, (seconds * seconds + seconds * 2.0F) / 3.0F);
   }

   private void aimed(class_638 world, class_746 player, float partialTick, Ballistics.Body body, float speed, float tilt) {
      float pitchDeg = player.method_5695(partialTick);
      double pitch = Math.toRadians(pitchDeg);
      double yaw = Math.toRadians(player.method_5705(partialTick));
      double lift = Math.toRadians(pitchDeg + tilt);
      class_243 aim = new class_243(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(lift), Math.cos(yaw) * Math.cos(pitch)).method_1029().method_1021(speed);
      class_243 carried = player.method_60478();
      class_243 motion = aim.method_1031(carried.field_1352, player.method_24828() ? 0.0 : carried.field_1351, carried.field_1350);
      this.release(world, player, body, muzzle(player, partialTick, 0.1), motion, 240);
   }

   private void crossbowVolley(class_638 world, class_746 player, float partialTick, class_9278 loaded) {
      if (!loaded.method_57442()) {
         List<class_1799> rounds = loaded.method_57437();
         float speed = loaded.method_57438(class_1802.field_8639) ? 1.6F : 3.15F;
         int count = rounds.size();
         float gap = count > 1 ? 20.0F / (count - 1) : 0.0F;
         float centre = (count - 1) % 2 * gap / 2.0F;
         class_243 look = player.method_5828(partialTick);
         class_243 up = player.method_18864(partialTick);

         for (int i = 0; i < count; i++) {
            class_1799 round = rounds.get(i);
            if (!round.method_7960()) {
               float offset = centre + ((i & 1) == 1 ? -1.0F : 1.0F) * ((i + 1) / 2) * gap;
               class_243 motion = swing(look, up, offset).method_1029().method_1021(speed);
               if (round.method_31574(class_1802.field_8639)) {
                  this.release(world, player, Ballistics.Body.ROCKET, muzzle(player, partialTick, 0.15), motion, rocketFuse(round));
               } else {
                  this.release(world, player, Ballistics.Body.ARROW, muzzle(player, partialTick, 0.1), motion, 240);
               }
            }
         }
      }
   }

   private static int rocketFuse(class_1799 rocket) {
      class_9284 fireworks = (class_9284)rocket.method_58694(class_9334.field_49616);
      int flight = 1 + (fireworks != null ? fireworks.comp_2391() : 0);
      return 10 * flight + 6;
   }

   private static class_243 swing(class_243 vector, class_243 axis, double degrees) {
      if (degrees == 0.0) {
         return vector;
      }

      double angle = Math.toRadians(degrees);
      double cos = Math.cos(angle);
      double sin = Math.sin(angle);
      class_243 k = axis.method_1029();
      return vector.method_1021(cos).method_1019(k.method_1036(vector).method_1021(sin)).method_1019(k.method_1021(k.method_1026(vector) * (1.0 - cos)));
   }

   private static class_243 muzzle(class_746 player, float partialTick, double drop) {
      class_243 feet = player.method_30950(partialTick);
      return new class_243(feet.field_1352, feet.field_1351 + player.method_5751() - drop, feet.field_1350);
   }

   private void release(class_638 world, class_746 player, Ballistics.Body body, class_243 from, class_243 motion, int life) {
      if (this.volleySize != 3) {
         TrajectoriesModule.Aim aim = this.volley[this.volleySize++];
         if (!aim.matches(body, from, motion, life, this.sweep)) {
            aim.hold(body, from, motion, life, this.sweep);
            aim.flight.launch(body, from, motion).lifetime(life).ignoring(player, null).solve(world);
            aim.caption(from, this.flightTime.get());
         }
      }
   }

   private void drawPath(Ballistics.Flight flight, class_243 head, double reveal, float alpha, boolean aim, boolean shine, boolean glow, float time) {
      int count = flight.size();
      if (count >= 2 && !(alpha < 0.02F)) {
         double total = flight.length();
         if (head != null) {
            double hx = head.field_1352 - flight.x(0);
            double hy = head.field_1351 - flight.y(0);
            double hz = head.field_1350 - flight.z(0);
            total += Math.sqrt(hx * hx + hy * hy + hz * hz);
         }

         if (!(total < 1.0E-4)) {
            double limit = Math.min(reveal, total);
            boolean solid = this.pathStyle.is("Solid");
            boolean dashes = this.pathStyle.is("Dashes");
            double unit = solid ? 0.6 : (dashes ? 0.72 : 0.42);
            double spread = Math.max(1.0, total / (unit * (aim ? 200 : 110)));
            double period = (dashes ? 0.72 : 0.42) * spread;
            double dash = 0.44 * spread;
            double pieceLength = 0.6 * spread;
            double phase = !solid && this.flow.get() ? time * 1.3 % period : 0.0;
            this.strokeTotal = total;
            this.strokeAlpha = alpha;
            this.strokeFade = aim;
            this.strokeSheen = shine ? time * 9.0 % (total + 14.0) - 1.6 : Double.NaN;
            this.strokeWidth = this.thickness.get();
            this.strokeHalo = glow && this.halo.get();
            double ax;
            double ay;
            double az;
            int next;
            if (head != null) {
               ax = head.field_1352;
               ay = head.field_1351;
               az = head.field_1350;
               next = 0;
            } else {
               ax = flight.x(0);
               ay = flight.y(0);
               az = flight.z(0);
               next = 1;
            }

            double walked = 0.0;

            for (int i = next; i < count && walked < limit; i++) {
               double bx = flight.x(i);
               double by = flight.y(i);
               double bz = flight.z(i);
               double dx = bx - ax;
               double dy = by - ay;
               double dz = bz - az;
               double seg = Math.sqrt(dx * dx + dy * dy + dz * dz);
               if (seg > 1.0E-6) {
                  double end = Math.min(walked + seg, limit);
                  if (solid) {
                     int pieces = Math.max(1, (int)Math.ceil((end - walked) / pieceLength));
                     double piece = (end - walked) / pieces;

                     for (int p = 0; p < pieces; p++) {
                        double u0 = walked + p * piece;
                        this.stroke(ax, ay, az, dx, dy, dz, (u0 - walked) / seg, (u0 + piece - walked) / seg, u0 + piece * 0.5);
                     }
                  } else {
                     for (double d = Math.floor((walked - phase) / period) * period + phase; d < end; d += period) {
                        double u0 = Math.max(d, walked);
                        double length = dashes ? dash : this.beadLength(ax, ay, az, dx, dy, dz, (u0 - walked) / seg);
                        double u1 = Math.min(d + length, end);
                        if (u1 > u0) {
                           this.stroke(ax, ay, az, dx, dy, dz, (u0 - walked) / seg, (u1 - walked) / seg, (u0 + u1) * 0.5);
                        }
                     }
                  }
               }

               walked += seg;
               ax = bx;
               ay = by;
               az = bz;
            }
         }
      }
   }

   private double beadLength(double ax, double ay, double az, double dx, double dy, double dz, double t) {
      double px = ax + dx * t - this.camera.field_1352;
      double py = ay + dy * t - this.camera.field_1351;
      double pz = az + dz * t - this.camera.field_1350;
      return class_3532.method_15350(Math.sqrt(px * px + py * py + pz * pz) * 0.011, 0.035, 0.2);
   }

   private void stroke(double ax, double ay, double az, double dx, double dy, double dz, double t0, double t1, double along) {
      float alpha = this.strokeAlpha;
      if (this.strokeFade) {
         alpha *= smooth((float)(along / 1.4));
      }

      if (!(alpha < 0.02F)) {
         float progress = (float)(along / this.strokeTotal);
         int core = class_9848.method_61319(progress, this.headTint, this.tailTint);
         if (!Double.isNaN(this.strokeSheen)) {
            double glint = 1.0 - Math.abs(along - this.strokeSheen) / 1.6;
            if (glint > 0.0) {
               core = class_9848.method_61319((float)(glint * 0.7), core, this.sheenTint);
            }
         }

         double x0 = ax + dx * t0;
         double y0 = ay + dy * t0;
         double z0 = az + dz * t0;
         double x1 = ax + dx * t1;
         double y1 = ay + dy * t1;
         double z1 = az + dz * t1;
         if (this.strokeHalo) {
            float glow = alpha * 0.22F * this.haloFade(x0, y0, z0);
            if (glow > 0.0F) {
               this.canvas.line(x0, y0, z0, x1, y1, z1, class_9848.method_75601(this.haloTint, glow), this.strokeWidth * 2.8F);
            }
         }

         this.canvas.line(x0, y0, z0, x1, y1, z1, class_9848.method_75601(core, alpha), this.strokeWidth);
      }
   }

   private float haloFade(double x, double y, double z) {
      double px = x - this.camera.field_1352;
      double py = y - this.camera.field_1351;
      double pz = z - this.camera.field_1350;
      double distanceSqr = px * px + py * py + pz * pz;
      if (distanceSqr <= 576.0) {
         return 1.0F;
      } else {
         return distanceSqr >= 1600.0 ? 0.0F : 1.0F - smooth((float)((Math.sqrt(distanceSqr) - 24.0) / 16.0));
      }
   }

   private void drawImpact(Ballistics.Flight flight, float grow, float alpha, float time, float partialTick, class_1309 focus) {
      switch (flight.outcome()) {
         case ENTITY:
            if (this.targetBox.get()) {
               this.drawTargetBox(flight.struck(), alpha, time, partialTick, focus);
            }
            break;
         case BLOCK:
            this.drawMarker(flight, grow, alpha, time);
            break;
         case BURST:
            this.drawBurst(flight.impact(), grow, alpha);
      }
   }

   private void drawMarker(Ballistics.Flight flight, float grow, float alpha, float time) {
      if (!this.marker.is("None") && !(grow <= 0.01F)) {
         class_2350 face = flight.face();
         class_243 normal = new class_243(face.method_10148(), face.method_10164(), face.method_10165());
         class_243 up = new class_243(0.0, 1.0, 0.0);
         if (face.method_10166() == class_2351.field_11052) {
            class_243 heading = flight.heading();
            up = heading.field_1352 * heading.field_1352 + heading.field_1350 * heading.field_1350 < 1.0E-8
               ? new class_243(0.0, 0.0, 1.0)
               : new class_243(heading.field_1352, 0.0, heading.field_1350).method_1029();
         }

         class_243 right = up.method_1036(normal).method_1029();
         class_243 centre = flight.impact().method_1019(normal.method_1021(0.025));
         double scale = 0.5 * this.markerScale.get() * grow;
         this.gx = centre.field_1352;
         this.gy = centre.field_1351;
         this.gz = centre.field_1350;
         this.rx = right.field_1352 * scale;
         this.ry = right.field_1351 * scale;
         this.rz = right.field_1350 * scale;
         this.ux = up.field_1352 * scale;
         this.uy = up.field_1351 * scale;
         this.uz = up.field_1350 * scale;
         if (this.marker.is("Sun Disk")) {
            this.drawSunDisk(alpha, time);
         } else {
            this.drawAnkh(alpha, time);
         }
      }
   }

   private void drawAnkh(float alpha, float time) {
      int gold = class_9848.method_75601(this.markTint, alpha);
      this.disc(0.78, class_9848.method_75601(this.plateTint, alpha * 0.16F));
      this.orbit(0.9, time * 0.6, class_9848.method_75601(this.ringTint, alpha * 0.85F), this.thickness.get() * 0.75F);
      this.facet(this.at(-0.06, 0.02), this.at(0.06, 0.02), this.at(0.15, -0.62), this.at(-0.15, -0.62), gold);
      this.facet(this.at(-0.06, 0.09), this.at(-0.06, -0.03), this.at(-0.37, -0.08), this.at(-0.37, 0.14), gold);
      this.facet(this.at(0.06, 0.09), this.at(0.06, -0.03), this.at(0.37, -0.08), this.at(0.37, 0.14), gold);

      for (int k = 0; k < 24; k++) {
         double c0 = RIM_COS[k];
         double s0 = RIM_SIN[k];
         double c1 = RIM_COS[k + 1];
         double s1 = RIM_SIN[k + 1];
         this.facet(
            this.at(0.19 * c0, 0.34 + 0.27 * s0),
            this.at(0.19 * c1, 0.34 + 0.27 * s1),
            this.at(0.1 * c1, 0.34 + 0.17 * s1),
            this.at(0.1 * c0, 0.34 + 0.17 * s0),
            gold
         );
      }
   }

   private void drawSunDisk(float alpha, float time) {
      int gold = class_9848.method_75601(this.markTint, alpha);
      float width = this.thickness.get();
      this.disc(0.95, class_9848.method_75601(this.plateTint, alpha * 0.12F));
      this.disc(0.3, gold);
      int ring = class_9848.method_75601(this.ringTint, alpha);
      class_243 previous = this.at(0.42, 0.0);

      for (int k = 1; k <= 24; k++) {
         class_243 point = this.at(0.42 * RIM_COS[k], 0.42 * RIM_SIN[k]);
         this.line(previous, point, ring, width);
         previous = point;
      }

      double spin = time * 0.35;

      for (int k = 0; k < 12; k++) {
         double angle = spin + (Math.PI * 2) * k / 12.0;
         double cos = Math.cos(angle);
         double sin = Math.sin(angle);
         double tip = (k & 1) == 0 ? 0.8 : 0.66;
         this.line(this.at(0.52 * cos, 0.52 * sin), this.at(tip * cos, tip * sin), gold, width);
      }
   }

   private void drawBurst(class_243 centre, float grow, float alpha) {
      int color = class_9848.method_75601(this.markTint, alpha);
      double reach = 0.55 * this.markerScale.get() * grow;
      float width = this.thickness.get();

      for (double[] ray : BURST_RAYS) {
         class_243 dir = new class_243(ray[0], ray[1], ray[2]);
         this.line(centre.method_1019(dir.method_1021(reach * 0.25)), centre.method_1019(dir.method_1021(reach)), color, width);
      }
   }

   private void drawTargetBox(class_1297 entity, float alpha, float time, float partialTick, class_1309 focus) {
      if (entity != null && !entity.method_31481()) {
         class_243 shift = entity.method_30950(partialTick).method_1020(entity.method_73189());
         class_238 box = entity.method_5829().method_997(shift).method_1014(0.06);
         if (!box.method_1006(this.camera)) {
            boolean locked = entity == focus;
            float pulse = locked ? 0.5F + 0.5F * class_3532.method_15374(time * 6.5F) : 0.0F;
            int edge = class_9848.method_75601(locked ? class_9848.method_61319(0.35F * pulse, this.hitTint, -1) : this.hitTint, alpha);
            class_12178 fill = class_12180.method_75541(box, class_12179.method_75539(class_9848.method_75601(edge, locked ? 0.14F + 0.12F * pulse : 0.1F)));
            if (this.onTop) {
               fill.method_75533();
            }

            this.brackets(box, locked ? 0.34 + 0.06 * pulse : 0.26, edge, this.thickness.get() * (locked ? 1.2F : 1.0F));
         }
      }
   }

   private void brackets(class_238 box, double reach, int color, float width) {
      double lx = (box.field_1320 - box.field_1323) * reach;
      double ly = (box.field_1325 - box.field_1322) * reach;
      double lz = (box.field_1324 - box.field_1321) * reach;

      for (int corner = 0; corner < 8; corner++) {
         boolean east = (corner & 1) != 0;
         boolean top = (corner & 2) != 0;
         boolean south = (corner & 4) != 0;
         double x = east ? box.field_1320 : box.field_1323;
         double y = top ? box.field_1325 : box.field_1322;
         double z = south ? box.field_1324 : box.field_1321;
         class_243 tip = new class_243(x, y, z);
         this.line(tip, new class_243(east ? x - lx : x + lx, y, z), color, width);
         this.line(tip, new class_243(x, top ? y - ly : y + ly, z), color, width);
         this.line(tip, new class_243(x, y, south ? z - lz : z + lz), color, width);
      }
   }

   private void disc(double radius, int color) {
      class_243 centre = this.at(0.0, 0.0);

      for (int k = 0; k < 24; k++) {
         this.facet(centre, centre, this.at(radius * RIM_COS[k], radius * RIM_SIN[k]), this.at(radius * RIM_COS[k + 1], radius * RIM_SIN[k + 1]), color);
      }
   }

   private void orbit(double radius, double spin, int color, float width) {
      double slice = Math.PI / 5;
      double span = slice * 0.55;

      for (int k = 0; k < 10; k++) {
         double start = spin + k * slice;
         class_243 previous = this.at(radius * Math.cos(start), radius * Math.sin(start));

         for (int s = 1; s <= 3; s++) {
            double angle = start + span * s / 3.0;
            class_243 point = this.at(radius * Math.cos(angle), radius * Math.sin(angle));
            this.line(previous, point, color, width);
            previous = point;
         }
      }
   }

   private class_243 at(double x, double y) {
      return new class_243(this.gx + this.rx * x + this.ux * y, this.gy + this.ry * x + this.uy * y, this.gz + this.rz * x + this.uz * y);
   }

   private void facet(class_243 a, class_243 b, class_243 c, class_243 d, int color) {
      double ex = c.field_1352 - a.field_1352;
      double ey = c.field_1351 - a.field_1351;
      double ez = c.field_1350 - a.field_1350;
      double fx = d.field_1352 - b.field_1352;
      double fy = d.field_1351 - b.field_1351;
      double fz = d.field_1350 - b.field_1350;
      double nx = ey * fz - ez * fy;
      double ny = ez * fx - ex * fz;
      double nz = ex * fy - ey * fx;
      double facing = nx * (this.camera.field_1352 - a.field_1352)
         + ny * (this.camera.field_1351 - a.field_1351)
         + nz * (this.camera.field_1350 - a.field_1350);
      if (facing >= 0.0) {
         this.canvas
            .quad(
               a.field_1352,
               a.field_1351,
               a.field_1350,
               b.field_1352,
               b.field_1351,
               b.field_1350,
               c.field_1352,
               c.field_1351,
               c.field_1350,
               d.field_1352,
               d.field_1351,
               d.field_1350,
               color
            );
      } else {
         this.canvas
            .quad(
               d.field_1352,
               d.field_1351,
               d.field_1350,
               c.field_1352,
               c.field_1351,
               c.field_1350,
               b.field_1352,
               b.field_1351,
               b.field_1350,
               a.field_1352,
               a.field_1351,
               a.field_1350,
               color
            );
      }
   }

   private void line(class_243 from, class_243 to, int color, float width) {
      this.canvas.line(from.field_1352, from.field_1351, from.field_1350, to.field_1352, to.field_1351, to.field_1350, color, width);
   }

   private void addLabel(TrajectoriesModule.Plot plot) {
      if (this.flightTime.get() && plot.seconds != null) {
         Ballistics.Flight flight = plot.flight;
         class_243 impact = flight.impact();
         class_1297 struck = flight.struck();
         class_243 anchor = struck != null
            ? new class_243(impact.field_1352, struck.method_5829().field_1325 + 0.35, impact.field_1350)
            : impact.method_1031(0.0, 0.55, 0.0);
         this.labels.add(new TrajectoriesModule.Label(anchor, plot.seconds, plot.detail));
      }
   }

   @Override
   public boolean isHudOverlayEnabled() {
      return this.isEnabled() && this.flightTime.get();
   }

   @Override
   public void renderHudOverlay(ImDrawList dl, float screenW, float screenH) {
      if (!this.labels.isEmpty() && System.nanoTime() - this.labelsAt <= 250000000L) {
         class_310 mc = class_310.method_1551();

         for (TrajectoriesModule.Label label : this.labels) {
            float[] screen = WorldLabel.project(mc, label.pos(), screenW, screenH);
            if (screen != null) {
               WorldLabel.drawText(dl, screen[0], screen[1], label.primary(), label.secondary(), 1.0F);
            }
         }
      }
   }

   private static float smooth(float t) {
      t = class_3532.method_15363(t, 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   private static float easeOutExpo(float t) {
      return t >= 1.0F ? 1.0F : 1.0F - (float)Math.pow(2.0, -10.0 * Math.max(0.0F, t));
   }

   private static float settle(float seconds) {
      return 1.0F - (float)(Math.exp(-5.5 * seconds) * Math.cos(8.5 * seconds));
   }

   static {
      for (int i = 0; i <= 24; i++) {
         double angle = (Math.PI * 2) * i / 24.0;
         RIM_COS[i] = Math.cos(angle);
         RIM_SIN[i] = Math.sin(angle);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Aim extends TrajectoriesModule.Plot {
      Ballistics.Body body;
      double x;
      double y;
      double z;
      double vx;
      double vy;
      double vz;
      int life;
      int stamp;
      boolean held;

      boolean matches(Ballistics.Body body, class_243 from, class_243 motion, int life, int stamp) {
         return this.held
            && this.stamp == stamp
            && this.body == body
            && this.life == life
            && Math.abs(this.x - from.field_1352) < 0.001
            && Math.abs(this.y - from.field_1351) < 0.001
            && Math.abs(this.z - from.field_1350) < 0.001
            && Math.abs(this.vx - motion.field_1352) < 5.0E-4
            && Math.abs(this.vy - motion.field_1351) < 5.0E-4
            && Math.abs(this.vz - motion.field_1350) < 5.0E-4;
      }

      void hold(Ballistics.Body body, class_243 from, class_243 motion, int life, int stamp) {
         this.body = body;
         this.x = from.field_1352;
         this.y = from.field_1351;
         this.z = from.field_1350;
         this.vx = motion.field_1352;
         this.vy = motion.field_1351;
         this.vz = motion.field_1350;
         this.life = life;
         this.stamp = stamp;
         this.held = true;
      }

      @Override
      void forget() {
         super.forget();
         this.held = false;
      }
   }

   @Environment(EnvType.CLIENT)
   private record Label(class_243 pos, String primary, String secondary) {
   }

   @Environment(EnvType.CLIENT)
   private static class Plot {
      final Ballistics.Flight flight = new Ballistics.Flight();
      String seconds;
      String detail;

      void caption(class_243 from, boolean wanted) {
         this.seconds = null;
         this.detail = null;
         if (wanted && this.flight.outcome() != Ballistics.Outcome.OPEN && this.flight.size() >= 2) {
            class_1297 struck = this.flight.struck();
            this.seconds = String.format(Locale.ROOT, "%.2fs", this.flight.ticks() / 20.0F);
            this.detail = struck != null
               ? NameProtectModule.protect(struck.method_5477()).getString()
               : String.format(Locale.ROOT, "%.0fm", from.method_1022(this.flight.impact()));
         }
      }

      void forget() {
         this.flight.clear();
         this.seconds = null;
         this.detail = null;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Track extends TrajectoriesModule.Plot {
      class_1676 shot;
      int seen;
      boolean departed;
   }
}
