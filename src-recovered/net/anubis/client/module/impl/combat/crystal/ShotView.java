package net.anubis.client.module.impl.combat.crystal;

import imgui.ImDrawList;
import java.util.Locale;
import net.anubis.client.gui.WorldLabel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12179;
import net.minecraft.class_12180;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;

@Environment(EnvType.CLIENT)
public final class ShotView {
   private static final long SLIDE_MS = 120L;
   private static final long HOLD_MS = 150L;
   private static final long FADE_MS = 350L;
   private static final float FILL_ALPHA = 0.22F;
   private static final float LINE_WIDTH = 1.5F;
   private static final double LABEL_RISE = 0.25;
   private class_238 from;
   private class_238 to;
   private long movedAt;
   private long shownAt;
   private int color;
   private float damage;
   private float selfDamage;

   public void show(class_238 box, int color, float damage, float selfDamage) {
      long now = System.currentTimeMillis();
      if (!this.showing(now)) {
         this.from = box;
         this.movedAt = now;
      } else if (!box.equals(this.to)) {
         this.from = this.box(now);
         this.movedAt = now;
      }

      this.to = box;
      this.shownAt = now;
      this.color = color;
      this.damage = damage;
      this.selfDamage = selfDamage;
   }

   public void clear() {
      this.to = null;
   }

   public boolean showing() {
      return this.showing(System.currentTimeMillis());
   }

   public void draw() {
      long now = System.currentTimeMillis();
      if (this.showing(now)) {
         float alpha = this.alpha(now);
         class_12180.method_75541(this.box(now), class_12179.method_75537(fade(this.color, alpha), 1.5F, fade(this.color, alpha * 0.22F))).method_75533();
      }
   }

   public void label(ImDrawList dl, float screenW, float screenH) {
      long now = System.currentTimeMillis();
      if (this.showing(now)) {
         class_238 box = this.box(now);
         class_243 top = new class_243(box.method_1005().field_1352, box.field_1325 + 0.25, box.method_1005().field_1350);
         float[] screen = WorldLabel.project(class_310.method_1551(), top, screenW, screenH);
         if (screen != null) {
            WorldLabel.drawText(
               dl,
               screen[0],
               screen[1],
               String.format(Locale.ROOT, "%.1f", this.damage),
               String.format(Locale.ROOT, "self %.1f", this.selfDamage),
               this.alpha(now)
            );
         }
      }
   }

   private boolean showing(long now) {
      return this.to != null && this.alpha(now) > 0.0F;
   }

   private float alpha(long now) {
      long idle = now - this.shownAt - 150L;
      return idle <= 0L ? 1.0F : Math.max(0.0F, 1.0F - (float)idle / 350.0F);
   }

   private class_238 box(long now) {
      float t = Math.min(1.0F, (float)(now - this.movedAt) / 120.0F);
      float eased = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
      return new class_238(
         this.from.method_61125().method_35590(this.to.method_61125(), eased), this.from.method_61126().method_35590(this.to.method_61126(), eased)
      );
   }

   private static int fade(int argb, float alpha) {
      return Math.round((argb >>> 24) * alpha) << 24 | argb & 16777215;
   }
}
