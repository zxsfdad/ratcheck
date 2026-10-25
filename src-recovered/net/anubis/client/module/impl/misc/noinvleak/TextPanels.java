package net.anubis.client.module.impl.misc.noinvleak;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1041;
import net.minecraft.class_124;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_437;
import net.minecraft.class_9334;

@Environment(EnvType.CLIENT)
final class TextPanels {
   private static final int TOOLTIP_MARGIN = 12;
   private static final int LINE = 10;
   private static final Canvas canvas = new Canvas();
   private static OverlayWindow tooltipWindow;
   private static OverlayWindow nameWindow;
   private static class_1799 tooltipStack;
   private static int tooltipMouseX;
   private static int tooltipMouseY;
   private static class_1799 nameStack;
   private static int nameAlpha;
   private static int nameY;
   private static class_1799 shownTooltip;
   private static int shownTooltipCount;
   private static int shownTooltipDamage;
   private static int shownTooltipScale;
   private static int tooltipWidth;
   private static int tooltipHeight;
   private static int tooltipScreenX;
   private static int tooltipScreenY;
   private static class_1799 shownName;
   private static int shownNameAlpha;
   private static int shownNameScale;
   private static int nameScreenX;
   private static int nameScreenY;

   private TextPanels() {
   }

   static void tooltip(class_1799 stack, int mouseX, int mouseY) {
      tooltipStack = stack;
      tooltipMouseX = mouseX;
      tooltipMouseY = mouseY;
   }

   static void itemName(class_1799 stack, int alpha, int y) {
      nameStack = stack;
      nameAlpha = alpha;
      nameY = y;
   }

   static boolean started() {
      return tooltipWindow != null || nameWindow != null;
   }

   static void frameDone() {
      tooltipStack = null;
      nameStack = null;
   }

   static void update(class_1041 win, int scale, int windowX, int windowY) {
      class_310 mc = class_310.method_1551();
      int guiW = win.method_4486();
      int guiH = win.method_4502();
      updateTooltip(mc, win, scale, windowX, windowY, guiW, guiH);
      updateName(mc, win, scale, windowX, windowY, guiW);
   }

   private static void updateTooltip(class_310 mc, class_1041 win, int scale, int windowX, int windowY, int guiW, int guiH) {
      class_1799 stack = tooltipStack;
      if (stack == null) {
         if (tooltipWindow != null) {
            tooltipWindow.hide();
         }

         shownTooltip = null;
      } else {
         if (tooltipWindow == null) {
            tooltipWindow = new OverlayWindow(win.method_4490());
         }

         boolean changed = stack != shownTooltip
            || stack.method_7947() != shownTooltipCount
            || stack.method_7919() != shownTooltipDamage
            || scale != shownTooltipScale;
         if (changed) {
            List<class_2561> lines = class_437.method_25408(mc, stack);
            int w = 0;
            int h = lines.size() == 1 ? -2 : 0;

            for (class_2561 line : lines) {
               w = Math.max(w, mc.field_1772.method_27525(line));
               h += 10;
            }

            tooltipWidth = w;
            tooltipHeight = h;
            int boxW = w + 24;
            int boxH = h + 24;
            canvas.wrap(tooltipWindow.pixels(boxW * scale, boxH * scale), tooltipWindow.stride(), boxW * scale, boxH * scale);
            OverlayArt.slice(canvas, OverlayArt.tooltipBackground(), 0, 0, boxW, boxH, scale);
            OverlayArt.slice(canvas, OverlayArt.tooltipFrame(), 0, 0, boxW, boxH, scale);
            int y = 12;

            for (int i = 0; i < lines.size(); i++) {
               OverlayArt.text(canvas, lines.get(i), 12, y, scale, -1, true);
               y += 10 + (i == 0 ? 2 : 0);
            }

            shownTooltip = stack;
            shownTooltipCount = stack.method_7947();
            shownTooltipDamage = stack.method_7919();
            shownTooltipScale = scale;
         }

         int x = tooltipMouseX + 12;
         int y = tooltipMouseY - 12;
         if (x + tooltipWidth > guiW) {
            x = Math.max(x - 24 - tooltipWidth, 4);
         }

         if (y + tooltipHeight + 3 > guiH) {
            y = guiH - tooltipHeight - 3;
         }

         int screenX = windowX + (x - 12) * scale;
         int screenY = windowY + (y - 12) * scale;
         if (changed) {
            tooltipWindow.present(screenX, screenY, (tooltipWidth + 24) * scale, (tooltipHeight + 24) * scale);
         } else if (screenX != tooltipScreenX || screenY != tooltipScreenY) {
            tooltipWindow.move(screenX, screenY);
         }

         tooltipScreenX = screenX;
         tooltipScreenY = screenY;
      }
   }

   private static void updateName(class_310 mc, class_1041 win, int scale, int windowX, int windowY, int guiW) {
      class_1799 stack = nameStack;
      if (stack != null && nameAlpha > 0) {
         if (nameWindow == null) {
            nameWindow = new OverlayWindow(win.method_4490());
         }

         class_2561 name = class_2561.method_43473().method_10852(stack.method_7964()).method_27692(stack.method_7932().method_58413());
         if (stack.method_57826(class_9334.field_49631)) {
            name = name.method_27661().method_27692(class_124.field_1056);
         }

         int width = mc.field_1772.method_27525(name);
         int x = (guiW - width) / 2;
         int screenX = windowX + (x - 2) * scale;
         int screenY = windowY + (nameY - 2) * scale;
         boolean changed = stack != shownName || nameAlpha != shownNameAlpha || scale != shownNameScale;
         if (changed) {
            int boxW = width + 4;
            int boxH = 9 + 4;
            canvas.wrap(nameWindow.pixels(boxW * scale, boxH * scale), nameWindow.stride(), boxW * scale, boxH * scale);
            int backdrop = mc.field_1690.method_19345(0.0F);
            if (backdrop != 0) {
               int a = (backdrop >>> 24) * nameAlpha / 255;
               canvas.rect(0, 0, boxW * scale, boxH * scale, a << 24 | backdrop & 16777215);
            }

            OverlayArt.text(canvas, name, 2, 2, scale, nameAlpha << 24 | 16777215, true);
            nameWindow.present(screenX, screenY, boxW * scale, boxH * scale);
            shownName = stack;
            shownNameAlpha = nameAlpha;
            shownNameScale = scale;
         } else if (screenX != nameScreenX || screenY != nameScreenY) {
            nameWindow.move(screenX, screenY);
         }

         nameScreenX = screenX;
         nameScreenY = screenY;
      } else {
         if (nameWindow != null) {
            nameWindow.hide();
         }

         shownName = null;
      }
   }

   static void hide() {
      if (tooltipWindow != null) {
         tooltipWindow.hide();
      }

      if (nameWindow != null) {
         nameWindow.hide();
      }

      shownTooltip = null;
      shownName = null;
   }

   static void release() {
      if (tooltipWindow != null) {
         tooltipWindow.destroy();
         tooltipWindow = null;
      }

      if (nameWindow != null) {
         nameWindow.destroy();
         nameWindow = null;
      }

      shownTooltip = null;
      shownName = null;
      tooltipStack = null;
      nameStack = null;
   }
}
