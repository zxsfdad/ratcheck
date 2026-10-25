package net.anubis.client.gui;

import com.mojang.blaze3d.textures.GpuTexture;
import imgui.ImDrawList;
import imgui.ImFont;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.anubis.client.config.Surface;
import net.anubis.client.imgui.ImGuiManager;
import net.anubis.client.module.impl.client.HudModule;
import net.anubis.client.module.impl.misc.NameProtectModule;
import net.anubis.client.util.combat.CombatTarget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10868;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1531;
import net.minecraft.class_156;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1826;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_742;
import net.minecraft.class_8685;
import org.lwjgl.opengl.GL11;

@Environment(EnvType.CLIENT)
final class TargetCard {
   private static final long COMBAT_MEMORY_MS = 4000L;
   private static final float LINGER = 0.8F;
   private static final float SHOW_TIME = 0.2F;
   private static final float HIDE_TIME = 0.34F;
   private static final float ENTER_LIFT = 7.0F;
   private static final float HEALTH_RATE = 13.0F;
   private static final float TRAIL_HOLD = 0.4F;
   private static final float TRAIL_RATE = 4.5F;
   private static final float STRIKE_TIME = 0.45F;
   private static final long NAME_REFRESH_MS = 1000L;
   private static final float PAD = 8.0F;
   private static final float PORTRAIT = 40.0F;
   private static final float FACE = 32.0F;
   private static final float CONTENT_H = 46.0F;
   private static final float COLUMN_GAP = 10.0F;
   private static final float COLUMN_W = 178.0F;
   private static final float NAME_ROW = 18.0F;
   private static final float BAR_TOP = 22.0F;
   private static final float BAR_H = 6.0F;
   private static final float GEAR = 16.0F;
   private static final float GEAR_GAP = 3.0F;
   private static final float ANKH_H = 11.0F;
   private static final float ANKH_GAP = 4.0F;
   private static final float CLUSTER_GAP = 8.0F;
   private static final int MAT = 1493172224;
   private static final int NOTCH = 1191182336;
   private static final String ELLIPSIS = "...";
   private static final float SKIN_FACE_U = 0.125F;
   private static final float SKIN_HAT_U = 0.625F;
   private static final float SKIN_TOP_V = 0.125F;
   private static final float SKIN_PART = 0.125F;
   private static final class_1304[] GEAR_ORDER = new class_1304[]{
      class_1304.field_6173, class_1304.field_6171, class_1304.field_6169, class_1304.field_6174, class_1304.field_6172, class_1304.field_6166
   };
   private class_1309 target;
   private float presence;
   private float lost;
   private float health;
   private float trail;
   private float trailHold;
   private float absorb;
   private float lastHealth;
   private int lastHurt;
   private float strike;
   private float clock;
   private class_1309 namedFor;
   private int nameGeneration = Integer.MIN_VALUE;
   private long namedAt;
   private String rawName = "";
   private String fittedName = "";
   private int fittedWidth = -1;
   private int fittedPx = -1;
   private int distanceTenths = Integer.MIN_VALUE;
   private String distanceText = "";
   private int healthTenths = Integer.MIN_VALUE;
   private String healthText = "";
   private int absorbWhole = Integer.MIN_VALUE;
   private String absorbText = "";
   private final Set<GpuTexture> crispSkins = Collections.newSetFromMap(new WeakHashMap<>());

   void reset() {
      this.target = null;
      this.namedFor = null;
      this.presence = 0.0F;
      this.lost = 0.0F;
   }

   boolean visible() {
      return this.target != null && this.presence > 0.0F;
   }

   float planWidth(float s) {
      return this.visible() ? Math.round(244.0F * s) : 1.0F;
   }

   float planHeight(float s) {
      return Math.round(62.0F * s);
   }

   void update(class_310 mc, HudModule hud, boolean active, boolean editMode, float dt) {
      this.clock += dt;
      if (this.target != null && (this.target.method_73183() != mc.field_1687 || !editMode && this.target == mc.field_1724)) {
         this.reset();
      }

      class_1309 next = active ? pick(mc, hud) : null;
      if (next == null && editMode) {
         next = mc.field_1724;
      }

      if (next != null && next != this.target) {
         this.adopt(next);
      }

      this.lost = next != null ? 0.0F : this.lost + dt;
      boolean wanted = this.target != null && (next != null || this.lost < 0.8F);
      if (editMode && wanted) {
         this.presence = 1.0F;
      } else {
         this.presence = class_3532.method_15363(this.presence + (wanted ? dt / 0.2F : -dt / 0.34F), 0.0F, 1.0F);
      }

      if (this.presence <= 0.0F && !wanted) {
         this.target = null;
         this.namedFor = null;
      } else {
         if (this.target != null) {
            this.follow(dt);
         }
      }
   }

   private static class_1309 pick(class_310 mc, HudModule hud) {
      class_1309 entity = hud.targetOnLook() ? CombatTarget.current(4000L) : CombatTarget.lastHit(4000L);
      if (entity == null || entity == mc.field_1724 || entity instanceof class_1531) {
         return null;
      } else {
         return hud.targetPlayersOnly() && !(entity instanceof class_1657) ? null : entity;
      }
   }

   private void adopt(class_1309 next) {
      this.target = next;
      float max = Math.max(1.0F, next.method_6063());
      this.health = this.trail = this.lastHealth = class_3532.method_15363(next.method_6032(), 0.0F, max);
      this.absorb = Math.max(0.0F, next.method_6067());
      this.trailHold = 0.0F;
      this.strike = 0.0F;
      this.lastHurt = next.field_6235;
   }

   private void follow(float dt) {
      float max = Math.max(1.0F, this.target.method_6063());
      float hp = class_3532.method_15363(this.target.method_6032(), 0.0F, max);
      if (hp < this.lastHealth - 0.01F) {
         this.trailHold = 0.4F;
         this.strike = 1.0F;
      }

      if (this.target.field_6235 > this.lastHurt) {
         this.strike = 1.0F;
      }

      this.lastHurt = this.target.field_6235;
      this.lastHealth = hp;
      this.health = HudGlyphs.approach(this.health, hp, 13.0F, dt);
      if (this.trailHold > 0.0F) {
         this.trailHold -= dt;
      } else {
         this.trail = HudGlyphs.approach(this.trail, this.health, 4.5F, dt);
      }

      if (this.trail < this.health) {
         this.trail = this.health;
      }

      this.absorb = HudGlyphs.approach(this.absorb, Math.max(0.0F, this.target.method_6067()), 13.0F, dt);
      this.strike = Math.max(0.0F, this.strike - dt / 0.45F);
   }

   void draw(ImDrawList dl, HudLayoutStore.HudElementState state, float x, float y, float alpha, boolean shadow, HudModule hud) {
      if (this.target != null) {
         float shown = HudGlyphs.easeOutQuint(this.presence);
         float a = alpha * shown;
         if (!(a <= 0.004F)) {
            class_310 mc = class_310.method_1551();
            float s = HudRenderer.scaleOf(state);
            y = Math.round(y + 7.0F * s * (1.0F - shown));
            float w = state.width;
            float h = state.height;
            Surface surface = HudRenderer.hudSurface(state);
            boolean guard = HudChrome.needsTextGuard(surface);
            HudChrome.card(dl, surface, x, y, w, h, HudRenderer.hudRounding() * s, a, shadow, s);
            ImGuiManager fonts = ImGuiManager.getInstance();
            ImFont textFont = fonts.hudTextFont();
            ImFont labelFont = fonts.hudLabelFont();
            int textPx = HudChrome.px(17.0F, s);
            int labelPx = HudChrome.px(14.0F, s);
            float left = x + Math.round(8.0F * s);
            float top = y + Math.round(8.0F * s);
            float frame = Math.round(40.0F * s);
            this.drawPortrait(dl, mc, left, top + Math.round((46.0F * s - frame) * 0.5F), frame, s, a);
            float colLeft = left + frame + Math.round(10.0F * s);
            float colRight = x + w - Math.round(8.0F * s);
            float max = Math.max(1.0F, this.target.method_6063());
            float fraction = class_3532.method_15363(this.health / max, 0.0F, 1.0F);
            float textTop = top + (float)Math.floor((18.0F * s - textPx) * 0.5F);
            float labelTop = textTop + HudChrome.baselineOffset(textFont, textPx, labelFont, labelPx);
            this.refreshNumbers(mc);
            float absorbW = this.absorbText.isEmpty() ? 0.0F : HudChrome.width(labelFont, labelPx, this.absorbText);
            float healthW = HudChrome.tabularWidth(textFont, textPx, this.healthText);
            float ankhH = 11.0F * s;
            float clusterW = ankhH * 0.62F + 4.0F * s + healthW + (absorbW > 0.0F ? 4.0F * s + absorbW : 0.0F);
            float cursor = colRight - clusterW;
            HudGlyphs.ankh(dl, cursor + ankhH * 0.31F, textTop + textPx * 0.52F, ankhH, Math.max(1.0F, 1.3F * s), HudGlyphs.ink(-871878, a));
            cursor += ankhH * 0.62F + 4.0F * s;
            HudChrome.tabular(dl, textFont, textPx, cursor, textTop, HudGlyphs.ink(HudGlyphs.vitality(fraction), a), this.healthText, guard);
            if (absorbW > 0.0F) {
               HudChrome.text(dl, labelFont, labelPx, cursor + healthW + 4.0F * s, labelTop, HudGlyphs.ink(-12750874, a), this.absorbText, guard);
            }

            String name = this.fittedName(textFont, textPx, colRight - clusterW - 8.0F * s - colLeft);
            HudChrome.text(dl, textFont, textPx, colLeft, textTop, HudChrome.scaleAlpha(HudChrome.TEXT_NAME, a), name, guard);
            float barTop = top + Math.round(22.0F * s);
            this.drawBar(dl, colLeft, barTop, colRight, Math.max(3.0F, Math.round(6.0F * s)), max, s, a);
            int gearPx = (int)ItemIcons.pixels(16.0F * s);
            float gearTop = top + Math.round(46.0F * s) - gearPx;
            float distanceW = HudChrome.width(labelFont, labelPx, this.distanceText);
            float distanceX = colRight - distanceW;
            HudChrome.text(
               dl,
               labelFont,
               labelPx,
               distanceX,
               gearTop + Math.round((gearPx - labelPx) * 0.5F),
               HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a),
               this.distanceText,
               guard
            );
            if (hud.targetGear()) {
               this.drawGear(dl, colLeft, gearTop, gearPx, distanceX - 6.0F * s, s, a);
            }

            float rule = Math.max(1.0F, Math.round(s));
            HudGlyphs.register(dl, x + w * 0.12F, x + w * 0.88F, y + h - rule - Math.max(1.0F, Math.round(2.0F * s)), rule, HudGlyphs.ink(-871878, a * 0.35F));
         }
      }
   }

   private void drawPortrait(ImDrawList dl, class_310 mc, float fx, float fy, float size, float s, float a) {
      float r = Math.min(HudRenderer.hudRounding(), 6.0F) * s;
      dl.addRectFilled(fx, fy, fx + size, fy + size, HudChrome.scaleAlpha(1493172224, a), r);
      float face = Math.max(8.0F, Math.round(32.0F * s / 8.0F) * 8.0F);
      if (face > size - 4.0F) {
         face = Math.max(8.0F, face - 8.0F);
      }

      float ix = Math.round(fx + (size - face) * 0.5F);
      float iy = Math.round(fy + (size - face) * 0.5F);
      boolean drawn = false;
      if (this.target instanceof class_742 player) {
         int texture = this.skinTexture(mc, player);
         if (texture != 0) {
            float fr = Math.max(1.0F, Math.round(2.0F * s));
            int tint = HudChrome.scaleAlpha(-1, a);
            dl.addImageRounded(texture, ix, iy, ix + face, iy + face, 0.125F, 0.125F, 0.25F, 0.25F, tint, fr, 240);
            dl.addImageRounded(texture, ix, iy, ix + face, iy + face, 0.625F, 0.125F, 0.75F, 0.25F, tint, fr, 240);
            drawn = true;
         }
      } else {
         class_1826 egg = class_1826.method_8019(this.target.method_5864());
         if (egg != null) {
            float shown = ItemIcons.pixels(face);
            float ex = Math.round(fx + (size - shown) * 0.5F);
            float ey = Math.round(fy + (size - shown) * 0.5F);
            ItemIcons.draw(dl, egg, ex, ey, shown, a);
            drawn = true;
         }
      }

      if (!drawn) {
         HudGlyphs.horusEye(dl, fx + size * 0.5F, fy + size * 0.46F, size * 0.3F, Math.max(1.0F, 1.4F * s), HudGlyphs.ink(-871878, a * 0.85F));
      }

      int edge = HudGlyphs.ink(EgyptDraw.mix(-871878, -2537681, this.strike), a * (0.7F + 0.3F * this.strike));
      dl.addRect(fx, fy, fx + size, fy + size, edge, r, 0, Math.max(1.0F, s));
      float tie = Math.max(1.0F, Math.round(1.5F * s));
      float tieY = fy + size + Math.max(1.0F, Math.round(s));
      dl.addRectFilled(fx + size * 0.22F, tieY, fx + size * 0.78F, tieY + tie, HudGlyphs.ink(-871878, a * 0.8F), tie * 0.5F);
      if (this.strike > 0.0F) {
         float grow = (1.0F - this.strike) * 6.0F * s;
         dl.addRect(
            fx - grow, fy - grow, fx + size + grow, fy + size + grow, HudGlyphs.ink(-2537681, a * this.strike * 0.7F), r + grow, 0, Math.max(1.0F, 1.5F * s)
         );
      }
   }

   private void drawBar(ImDrawList dl, float l, float t, float r, float bh, float max, float s, float a) {
      float bw = r - l;
      float round = bh * 0.5F;
      float fraction = class_3532.method_15363(this.health / max, 0.0F, 1.0F);
      float trailFraction = class_3532.method_15363(this.trail / max, 0.0F, 1.0F);
      dl.addRectFilled(l, t, r, t + bh, HudChrome.scaleAlpha(452984831, a), round);
      if (trailFraction > fraction + 0.002F) {
         float tr = l + bw * trailFraction;
         dl.addRectFilled(l, t, tr, t + bh, HudGlyphs.ink(-11654, a * 0.55F), Math.min(round, (tr - l) * 0.5F));
      }

      if (fraction > 0.0F) {
         float fr = l + bw * fraction;
         float fill = a;
         if (fraction < 0.25F) {
            fill *= 0.72F + 0.28F * (0.5F + 0.5F * (float)Math.sin(this.clock * 7.5F));
         }

         float corner = Math.min(round, (fr - l) * 0.5F);
         dl.addRectFilled(l, t, fr, t + bh, HudGlyphs.ink(HudGlyphs.vitality(fraction), fill), corner);
         dl.addRectFilled(l, t, fr, t + bh * 0.45F, HudGlyphs.ink(-1, fill * 0.16F), corner, 48);
      }

      float step = max <= 40.0F ? 2.0F : (max <= 200.0F ? 10.0F : max / 20.0F);
      float notch = Math.max(1.0F, Math.round(0.75F * s));
      int notchInk = HudChrome.scaleAlpha(1191182336, a);

      for (float mark = step; mark < max - 0.01F; mark += step) {
         float nx = Math.round(l + bw * mark / max);
         dl.addRectFilled(nx, t, nx + notch, t + bh, notchInk);
      }

      float absorbFraction = class_3532.method_15363(this.absorb / max, 0.0F, 1.0F);
      if (absorbFraction > 0.002F) {
         float ar = l + bw * absorbFraction;
         dl.addRectFilled(l, t, ar, t + Math.max(1.5F, bh * 0.45F), HudGlyphs.ink(-12750874, a * 0.95F), Math.min(round, (ar - l) * 0.5F), 48);
      }
   }

   private void drawGear(ImDrawList dl, float gx, float gy, int gearPx, float limit, float s, float a) {
      float step = gearPx + Math.round(3.0F * s);

      for (class_1304 slot : GEAR_ORDER) {
         class_1799 stack = this.target.method_6118(slot);
         if (!stack.method_7960()) {
            if (gx + gearPx > limit) {
               break;
            }

            ItemIcons.draw(dl, stack, gx, gy, gearPx, a);
            if (stack.method_7958()) {
               HudGlyphs.sparkle(dl, gx + gearPx - 1.5F * s, gy + 1.5F * s, 2.6F * s, HudGlyphs.ink(-11654, a * 0.9F));
            }

            if (stack.method_7963() && stack.method_7936() > 0) {
               float left = 1.0F - (float)stack.method_7919() / stack.method_7936();
               if (left < 0.25F) {
                  float line = Math.max(1.0F, Math.round(1.5F * s));
                  dl.addRectFilled(gx + 1.0F, gy + gearPx + line * 0.5F, gx + gearPx - 1.0F, gy + gearPx + line * 1.5F, HudGlyphs.ink(-2537681, a), line * 0.5F);
               }
            }

            gx += step;
         }
      }
   }

   private void refreshNumbers(class_310 mc) {
      float max = Math.max(1.0F, this.target.method_6063());
      int tenths = Math.round(class_3532.method_15363(this.target.method_6032(), 0.0F, max) * 10.0F);
      if (tenths != this.healthTenths) {
         this.healthTenths = tenths;
         this.healthText = tenths % 10 == 0 ? Integer.toString(tenths / 10) : tenths / 10 + "." + tenths % 10;
      }

      int whole = Math.round(Math.max(0.0F, this.target.method_6067()));
      if (whole != this.absorbWhole) {
         this.absorbWhole = whole;
         this.absorbText = whole > 0 ? "+" + whole : "";
      }

      int distance = mc.field_1724 == null ? 0 : Math.round(mc.field_1724.method_5739(this.target) * 10.0F);
      if (distance != this.distanceTenths) {
         this.distanceTenths = distance;
         this.distanceText = distance / 10 + "." + distance % 10 + "m";
      }
   }

   private String fittedName(ImFont font, int fontPx, float maxW) {
      int generation = NameProtectModule.generation();
      long now = class_156.method_658();
      if (this.target != this.namedFor || generation != this.nameGeneration || now - this.namedAt > 1000L) {
         this.namedFor = this.target;
         this.nameGeneration = generation;
         this.namedAt = now;
         String fresh = NameProtectModule.protect(this.target.method_5477()).getString();
         if (!fresh.equals(this.rawName)) {
            this.rawName = fresh;
            this.fittedWidth = -1;
         }
      }

      int budget = Math.max(0, (int)maxW);
      if (budget == this.fittedWidth && fontPx == this.fittedPx) {
         return this.fittedName;
      }

      this.fittedWidth = budget;
      this.fittedPx = fontPx;
      this.fittedName = fit(font, fontPx, this.rawName, budget);
      return this.fittedName;
   }

   private static String fit(ImFont font, int fontPx, String name, float maxW) {
      if (HudChrome.width(font, fontPx, name) <= maxW) {
         return name;
      }

      float dots = HudChrome.width(font, fontPx, "...");
      int lo = 0;
      int hi = name.length();

      while (lo < hi) {
         int mid = lo + hi + 1 >>> 1;
         if (HudChrome.width(font, fontPx, name.substring(0, mid)) + dots <= maxW) {
            lo = mid;
         } else {
            hi = mid - 1;
         }
      }

      return lo == 0 ? "" : name.substring(0, lo).stripTrailing() + "...";
   }

   private int skinTexture(class_310 mc, class_742 player) {
      class_8685 skin = player.method_52814();
      if (NameProtectModule.hidesName(player.method_7334().name())) {
         skin = NameProtectModule.hiddenHead(player.method_5667(), skin);
      }

      class_2960 path = skin.comp_1626().comp_3627();

      GpuTexture texture;
      try {
         texture = mc.method_1531().method_4619(path).method_68004();
      } catch (IllegalStateException notUploaded) {
         return 0;
      }

      if (texture instanceof class_10868 gl && !gl.isClosed()) {
         if (this.crispSkins.add(texture)) {
            int bound = GL11.glGetInteger(32873);
            GL11.glBindTexture(3553, gl.method_68427());
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            GL11.glBindTexture(3553, bound);
         }

         return gl.method_68427();
      } else {
         return 0;
      }
   }
}
