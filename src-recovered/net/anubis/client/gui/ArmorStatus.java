package net.anubis.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import net.anubis.client.config.Surface;
import net.anubis.client.imgui.ImGuiManager;
import net.anubis.client.module.impl.client.HudModule;
import net.anubis.client.module.impl.misc.NoInvLeakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1304;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_310;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
final class ArmorStatus {
   private static final class_1304[] ARMOR = new class_1304[]{class_1304.field_6169, class_1304.field_6174, class_1304.field_6172, class_1304.field_6166};
   private static final class_1304[] ARMOR_AND_HANDS = new class_1304[]{
      class_1304.field_6169, class_1304.field_6174, class_1304.field_6172, class_1304.field_6166, class_1304.field_6173, class_1304.field_6171
   };
   private static final float PAD = 6.0F;
   private static final float ICON = 18.0F;
   private static final float TILE_GAP = 5.0F;
   private static final float HANDS_GAP = 11.0F;
   private static final float GAUGE_H = 3.0F;
   private static final float GAUGE_GAP = 3.0F;
   private static final float TEXT_GAP = 2.0F;
   private static final float ROW_GAP = 4.0F;
   private static final float SIDE_GAP = 7.0F;
   private static final float SIDE_GAUGE_W = 44.0F;
   private static final float CRITICAL = 0.12F;
   private static final float GAUGE_RATE = 10.0F;
   private static final float SHOW_TIME = 0.25F;
   private static final String WIDEST = "100%";
   private static final String[] PERCENT = new String[101];
   private static final String[] COUNT = new String[100];
   private final float[] wear = new float[ARMOR_AND_HANDS.length];
   private final class_1792[] seen = new class_1792[ARMOR_AND_HANDS.length];
   private float presence;
   private float clock;
   private boolean column;
   private boolean hands;
   private boolean gauge;
   private boolean percent;

   void reset() {
      this.presence = 0.0F;

      for (int i = 0; i < this.seen.length; i++) {
         this.seen[i] = null;
      }
   }

   boolean visible() {
      return this.presence > 0.0F;
   }

   void update(class_310 mc, HudModule hud, boolean active, boolean editMode, float dt) {
      this.clock += dt;
      this.column = hud.armorColumn();
      this.hands = hud.armorHands();
      HudModule.ArmorWear mode = hud.armorWear();
      this.gauge = mode != HudModule.ArmorWear.PERCENT;
      this.percent = mode != HudModule.ArmorWear.GAUGE;
      class_746 player = mc.field_1724;
      boolean worn = false;
      class_1304[] order = this.order();

      for (int i = 0; i < order.length; i++) {
         class_1799 stack = player == null ? class_1799.field_8037 : player.method_6118(order[i]);
         if (!stack.method_7960()) {
            worn = true;
         }

         float target = remaining(stack);
         if (stack.method_7909() != this.seen[i]) {
            this.seen[i] = stack.method_7909();
            this.wear[i] = target;
         } else {
            this.wear[i] = target < 0.0F ? target : HudGlyphs.approach(Math.max(0.0F, this.wear[i]), target, 10.0F, dt);
         }
      }

      boolean wanted = editMode || active && worn;
      if (editMode) {
         this.presence = 1.0F;
      } else {
         this.presence = Math.max(0.0F, Math.min(1.0F, this.presence + (wanted ? dt : -dt) / 0.25F));
      }
   }

   private class_1304[] order() {
      return this.hands ? ARMOR_AND_HANDS : ARMOR;
   }

   private static float remaining(class_1799 stack) {
      return !stack.method_7960() && stack.method_7963() && stack.method_7936() > 0
         ? Math.max(0.0F, 1.0F - (float)stack.method_7919() / stack.method_7936())
         : -1.0F;
   }

   float planWidth(float s) {
      if (!this.visible()) {
         return 1.0F;
      } else {
         int n = this.order().length;
         float icon = ItemIcons.pixels(18.0F * s);
         float pctW = this.percent ? (float)Math.ceil(percentWidth(s)) : 0.0F;
         if (this.column) {
            float side = (this.percent ? pctW : 0.0F) + (this.gauge ? 44.0F * s : 0.0F) + (this.percent && this.gauge ? 7.0F * s : 0.0F);
            return Math.round(6.0F * s * 2.0F + icon + 7.0F * s + side);
         } else {
            float tile = Math.max(icon, pctW);
            return Math.round(6.0F * s * 2.0F + n * tile + (n - 1) * 5.0F * s + (this.hands ? 11.0F * s - 5.0F * s : 0.0F));
         }
      }
   }

   float planHeight(float s) {
      int n = this.order().length;
      float icon = ItemIcons.pixels(18.0F * s);
      if (this.column) {
         return Math.round(6.0F * s * 2.0F + n * icon + (n - 1) * 4.0F * s + (this.hands ? 11.0F * s - 4.0F * s : 0.0F));
      }

      float below = (this.gauge ? 3.0F * s + gaugeHeight(s) : 0.0F) + (this.percent ? 2.0F * s + HudChrome.px(14.0F, s) : 0.0F);
      return Math.round(6.0F * s * 2.0F + icon + below);
   }

   private static float percentWidth(float s) {
      return HudChrome.tabularWidth(ImGuiManager.getInstance().hudLabelFont(), HudChrome.px(14.0F, s), "100%");
   }

   private static float gaugeHeight(float s) {
      return Math.max(2.0F, Math.round(3.0F * s));
   }

   void draw(ImDrawList dl, HudLayoutStore.HudElementState state, float x, float y, float alpha, boolean shadow, class_310 mc) {
      float a = alpha * HudGlyphs.easeOutQuint(this.presence);
      if (!(a <= 0.004F) && mc.field_1724 != null) {
         float s = HudRenderer.scaleOf(state);
         float w = state.width;
         float h = state.height;
         Surface surface = HudRenderer.hudSurface(state);
         boolean guard = HudChrome.needsTextGuard(surface);
         HudChrome.card(dl, surface, x, y, w, h, HudRenderer.hudRounding() * s, a, shadow, s);
         ImFont label = ImGuiManager.getInstance().hudLabelFont();
         int labelPx = HudChrome.px(14.0F, s);
         int icon = (int)ItemIcons.pixels(18.0F * s);
         float pad = Math.round(6.0F * s);
         float pctW = this.percent ? (float)Math.ceil(percentWidth(s)) : 0.0F;
         float tile = Math.max(icon, pctW);
         class_1304[] order = this.order();
         float cx = x + pad;
         float cy = y + pad;

         for (int i = 0; i < order.length; i++) {
            if (i == ARMOR.length) {
               this.drawHandsRule(dl, x, y, w, h, cx, cy, s, a);
               if (this.column) {
                  cy += 7.0F * s;
               } else {
                  cx += 6.0F * s;
               }
            }

            class_1799 worn = mc.field_1724.method_6118(order[i]);
            class_1799 stack = NoInvLeakModule.shownInHud(worn, order[i]);
            float left = stack == worn ? this.wear[i] : -1.0F;
            if (this.column) {
               this.drawSideways(dl, stack, left, cx, Math.round(cy), icon, pctW, label, labelPx, s, a, guard);
               cy += icon + 4.0F * s;
            } else {
               this.drawStacked(dl, stack, left, Math.round(cx), cy, icon, tile, label, labelPx, s, a, guard);
               cx += tile + 5.0F * s;
            }
         }
      }
   }

   private void drawHandsRule(ImDrawList dl, float x, float y, float w, float h, float cx, float cy, float s, float a) {
      int rule = HudGlyphs.ink(-871878, a * 0.45F);
      float t = Math.max(1.0F, Math.round(s));
      if (this.column) {
         float ry = Math.round(cy + (11.0F * s - 4.0F * s) * 0.5F - 4.0F * s * 0.5F);
         HudGlyphs.register(dl, x + w * 0.15F, x + w * 0.85F, ry, t, rule);
      } else {
         float rx = Math.round(cx + (11.0F * s - 5.0F * s) * 0.5F - 5.0F * s * 0.5F);
         int clear = rule & 16777215;
         dl.addRectFilledMultiColor(rx, y + h * 0.18F, rx + t, y + h * 0.5F, clear, clear, rule, rule);
         dl.addRectFilledMultiColor(rx, y + h * 0.5F, rx + t, y + h * 0.82F, rule, rule, clear, clear);
      }
   }

   private void drawStacked(
      ImDrawList dl, class_1799 stack, float left, float tx, float ty, int icon, float tile, ImFont label, int labelPx, float s, float a, boolean guard
   ) {
      float ix = Math.round(tx + (tile - icon) * 0.5F);
      this.drawIcon(dl, stack, left, ix, ty, icon, label, labelPx, s, a);
      float below = ty + icon;
      if (this.gauge) {
         below += 3.0F * s;
         this.drawGauge(dl, stack, left, tx, below, tile, s, a);
         below += gaugeHeight(s);
      }

      if (this.percent && left >= 0.0F) {
         String value = percentText(left);
         float vw = HudChrome.tabularWidth(label, labelPx, value);
         HudChrome.tabular(
            dl,
            label,
            labelPx,
            Math.round(tx + (tile - vw) * 0.5F),
            Math.round(below + 2.0F * s),
            HudGlyphs.ink(HudGlyphs.vitality(left), a * this.criticalPulse(left)),
            value,
            guard
         );
      }
   }

   private void drawSideways(
      ImDrawList dl, class_1799 stack, float left, float tx, float ty, int icon, float pctW, ImFont label, int labelPx, float s, float a, boolean guard
   ) {
      this.drawIcon(dl, stack, left, tx, ty, icon, label, labelPx, s, a);
      float cx = tx + icon + 7.0F * s;
      float mid = ty + icon * 0.5F;
      if (this.percent) {
         if (left >= 0.0F) {
            String value = percentText(left);
            float vw = HudChrome.tabularWidth(label, labelPx, value);
            HudChrome.tabular(
               dl,
               label,
               labelPx,
               Math.round(cx + pctW - vw),
               Math.round(mid - labelPx * 0.5F),
               HudGlyphs.ink(HudGlyphs.vitality(left), a * this.criticalPulse(left)),
               value,
               guard
            );
         }

         cx += pctW + 7.0F * s;
      }

      if (this.gauge) {
         this.drawGauge(dl, stack, left, cx, Math.round(mid - gaugeHeight(s) * 0.5F), 44.0F * s, s, a);
      }
   }

   private void drawIcon(ImDrawList dl, class_1799 stack, float left, float ix, float iy, int icon, ImFont label, int labelPx, float s, float a) {
      if (stack.method_7960()) {
         float inset = Math.max(1.0F, Math.round(2.0F * s));
         dl.addRect(ix + inset, iy + inset, ix + icon - inset, iy + icon - inset, HudChrome.scaleAlpha(536870911, a), Math.max(1.0F, 3.0F * s), 0, 1.0F);
         dl.addCircleFilled(ix + icon * 0.5F, iy + icon * 0.5F, Math.max(1.0F, 1.2F * s), HudChrome.scaleAlpha(536870911, a), 8);
      } else {
         if (left >= 0.0F && left < 0.12F) {
            float glow = 0.5F + 0.5F * (float)Math.sin(this.clock * 6.5F);
            dl.addCircleFilled(ix + icon * 0.5F, iy + icon * 0.5F, icon * 0.62F, HudGlyphs.ink(-2537681, a * (0.18F + 0.22F * glow)), 20);
         }

         ItemIcons.draw(dl, stack, ix, iy, icon, a);
         if (stack.method_7958()) {
            HudGlyphs.sparkle(dl, ix + icon - 1.5F * s, iy + 1.5F * s, 2.8F * s, HudGlyphs.ink(-11654, a * 0.9F));
         }

         int count = stack.method_7947();
         if (count > 1) {
            String value = countText(count);
            float vw = HudChrome.width(label, labelPx, value);
            HudChrome.text(
               dl, label, labelPx, Math.round(ix + icon - vw + 2.0F * s), Math.round(iy + icon - labelPx + 3.0F * s), HudChrome.scaleAlpha(-1, a), value, true
            );
         }
      }
   }

   private void drawGauge(ImDrawList dl, class_1799 stack, float left, float gx, float gy, float gw, float s, float a) {
      float gh = gaugeHeight(s);
      float r = gh * 0.5F;
      dl.addRectFilled(gx, gy, gx + gw, gy + gh, HudChrome.scaleAlpha(452984831, a), r);
      if (!stack.method_7960() && !(left < 0.0F)) {
         float fill = gw * Math.max(0.0F, Math.min(1.0F, left));
         if (!(fill < 0.5F)) {
            dl.addRectFilled(gx, gy, gx + fill, gy + gh, HudGlyphs.ink(HudGlyphs.vitality(left), a * this.criticalPulse(left)), Math.min(r, fill * 0.5F));
         }
      }
   }

   private float criticalPulse(float left) {
      return left >= 0.12F ? 1.0F : 0.6F + 0.4F * (0.5F + 0.5F * (float)Math.sin(this.clock * 6.5F));
   }

   private static String percentText(float left) {
      int value = Math.max(0, Math.min(100, Math.round(left * 100.0F)));
      String cached = PERCENT[value];
      if (cached == null) {
         cached = value + "%";
         PERCENT[value] = cached;
      }

      return cached;
   }

   private static String countText(int count) {
      if (count >= COUNT.length) {
         return Integer.toString(count);
      }

      String cached = COUNT[count];
      if (cached == null) {
         cached = Integer.toString(count);
         COUNT[count] = cached;
      }

      return cached;
   }
}
