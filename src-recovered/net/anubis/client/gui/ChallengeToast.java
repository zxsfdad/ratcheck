package net.anubis.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_3532;
import net.minecraft.class_368;
import net.minecraft.class_374;
import net.minecraft.class_368.class_369;

@Environment(EnvType.CLIENT)
public class ChallengeToast implements class_368 {
   private static final int BACKGROUND = -266921698;
   private static final int TITLE_COLOR = -986891;
   private static final int MESSAGE_COLOR = -5656902;
   private static final int COUNT_COLOR = -6643540;
   private static final int TIMER_TRACK = 654311423;
   private static final int TIMER_FILL = -1711276033;
   private static final int TEXT_X = 30;
   private static final int TEXT_PAD = 7;
   private static final int RIGHT_GAP = 3;
   private static final int MAX_WIDTH = 240;
   private static final long DISPLAY_MILLIS = 5000L;
   private final Object token;
   private final class_1799 icon;
   private final boolean sound;
   private class_2561 title;
   private class_2561 message;
   private int width;
   private int count = 1;
   private boolean changed;
   private boolean hiding;
   private long changedAt;
   private class_369 wanted = class_369.field_2209;

   public ChallengeToast(class_1799 icon, class_2561 title, class_2561 message, boolean sound) {
      this(field_2208, icon, title, message, sound);
   }

   private ChallengeToast(Object token, class_1799 icon, class_2561 title, class_2561 message, boolean sound) {
      this.token = token;
      this.icon = icon.method_7972();
      this.title = title;
      this.message = message;
      this.sound = sound;
      this.fit();
   }

   public static void addOrUpdate(class_1799 icon, String title, class_2561 message, boolean sound) {
      class_310 mc = class_310.method_1551();
      if (!mc.method_18854()) {
         mc.execute(() -> addOrUpdate(icon, title, message, sound));
      } else {
         class_374 toasts = mc.method_1566();
         ChallengeToast toast = (ChallengeToast)toasts.method_1997(ChallengeToast.class, title);
         if (toast == null) {
            toasts.method_1999(new ChallengeToast(title, icon, class_2561.method_43470(title), message, sound));
         } else {
            toast.count++;
            toast.message = message;
            toast.changed = true;
            toast.fit();
         }
      }
   }

   private String countLabel() {
      return this.count > 1 ? " x" + this.count : "";
   }

   private void fit() {
      class_327 font = class_310.method_1551().field_1772;
      int text = Math.max(font.method_27525(this.title) + font.method_1727(this.countLabel()), font.method_27525(this.message));
      this.width = class_3532.method_15340(30 + text + 7, 160, 240);
   }

   public Object method_1987() {
      return this.token;
   }

   public class_3414 method_66530() {
      return this.sound ? (class_3414)class_3417.field_14793.comp_349() : null;
   }

   public class_369 method_61988() {
      return this.wanted;
   }

   public int method_29049() {
      return this.width;
   }

   public void method_61989(class_374 toasts, long visibleFor) {
      if (!this.hiding) {
         if (this.changed) {
            this.changedAt = visibleFor;
            this.changed = false;
         }

         this.hiding = visibleFor - this.changedAt >= 5000.0 * toasts.method_48221();
         this.wanted = this.hiding ? class_369.field_2209 : class_369.field_2210;
      }
   }

   public void method_71995() {
      if (this.changed) {
         this.hiding = false;
         class_310.method_1551().method_1566().method_1999(this);
      }
   }

   public void method_1986(class_332 graphics, class_327 font, long visibleFor) {
      int right = this.width - 3;
      int bottom = 31;
      rounded(graphics, 0, 1, right, bottom, -266921698);
      this.drawTimer(graphics, visibleFor, right, bottom);
      boolean single = this.message.getString().isEmpty();
      this.drawText(graphics, font, this.title, this.countLabel(), single ? 12 : 7, -986891);
      if (!single) {
         this.drawText(graphics, font, this.message, "", 18, -5656902);
      }

      graphics.method_51445(this.icon, 8, 8);
   }

   private void drawTimer(class_332 graphics, long visibleFor, int right, int bottom) {
      double duration = 5000.0 * class_310.method_1551().method_1566().method_48221();
      float left = 1.0F - (float)class_3532.method_15350((visibleFor - this.changedAt) / duration, 0.0, 1.0);
      int x0 = 4;
      int x1 = right - 4;
      int y = bottom - 2;
      graphics.method_25294(x0, y, x1, y + 1, 654311423);
      graphics.method_25294(x0, y, x0 + Math.round((x1 - x0) * left), y + 1, -1711276033);
   }

   private static void rounded(class_332 graphics, int x0, int y0, int x1, int y1, int color) {
      graphics.method_25294(x0 + 2, y0, x1 - 2, y0 + 1, color);
      graphics.method_25294(x0 + 1, y0 + 1, x1 - 1, y0 + 2, color);
      graphics.method_25294(x0, y0 + 2, x1, y1 - 2, color);
      graphics.method_25294(x0 + 1, y1 - 2, x1 - 1, y1 - 1, color);
      graphics.method_25294(x0 + 2, y1 - 1, x1 - 2, y1, color);
   }

   private void drawText(class_332 graphics, class_327 font, class_2561 text, String suffix, int y, int color) {
      int room = this.width - 30 - 7;
      int textWidth = font.method_27525(text) + font.method_1727(suffix);
      graphics.method_51448().pushMatrix();
      graphics.method_51448().translate(30.0F, y);
      if (textWidth > room) {
         graphics.method_51448().scale((float)room / textWidth, 1.0F);
      }

      graphics.method_51439(font, text, 0, 0, color, true);
      if (!suffix.isEmpty()) {
         graphics.method_51433(font, suffix, font.method_27525(text), 0, -6643540, true);
      }

      graphics.method_51448().popMatrix();
   }
}
