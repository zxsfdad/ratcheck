package net.anubis.client.gui.mainmenu;

import imgui.ImDrawList;
import imgui.ImFont;
import java.util.ArrayList;
import java.util.List;
import net.anubis.client.gui.AnubisTitleScreen;
import net.anubis.client.security.OfficialBuild;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterInit;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterRender;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_442;
import net.minecraft.class_5481;

@Environment(EnvType.CLIENT)
public final class OfficialBuildBanner {
   private static final String TITLE = "This Anubis file has been modified";
   private static final String[] LINES = new String[]{
      "It differs from the signed official release, so its code may have been changed.",
      "Get Anubis only from the official Discord.",
      "If you did not change it yourself, delete it and scan your PC."
   };
   private static final int RED = -2537681;
   private static final int WHITE = -921103;
   private static final int PANEL = -16053492;

   private OfficialBuildBanner() {
   }

   public static void draw(MenuFrame frame) {
      if (OfficialBuild.state() == OfficialBuild.State.MODIFIED) {
         ImDrawList dl = frame.fg();
         ImFont titleFont = frame.bodyFont();
         ImFont bodyFont = frame.tagFont();
         float pad = frame.px(14.0F);
         float gap = frame.px(6.0F);
         float titleHeight = frame.textHeight(titleFont);
         float bodyHeight = frame.textHeight(bodyFont);
         float width = frame.textWidth(titleFont, "This Anubis file has been modified");

         for (String line : LINES) {
            width = Math.max(width, frame.textWidth(bodyFont, line));
         }

         width += pad * 2.0F;
         float height = pad * 2.0F + titleHeight + (gap + bodyHeight) * LINES.length;
         float x = frame.px(24.0F);
         float y = frame.height() - height - frame.px(24.0F);
         dl.addRectFilled(x, y, x + width, y + height, MenuFrame.col(-16053492, 0.95F), frame.px(4.0F));
         dl.addRect(x, y, x + width, y + height, MenuFrame.col(-2537681, 0.95F), frame.px(4.0F), 0, Math.max(1.5F, frame.scale() * 2.0F));
         float lineY = y + pad;
         frame.text(dl, titleFont, x + pad, lineY, -2537681, 1.0F, "This Anubis file has been modified");
         lineY += titleHeight;

         for (String line : LINES) {
            lineY += gap;
            frame.text(dl, bodyFont, x + pad, lineY, -921103, 1.0F, line);
            lineY += bodyHeight;
         }
      }
   }

   public static void installVanillaTitleScreen() {
      ScreenEvents.AFTER_INIT.register((AfterInit)(client, screen, width, height) -> {
         if (screen instanceof class_442 && !(screen instanceof AnubisTitleScreen)) {
            ScreenEvents.afterRender(screen).register((AfterRender)(shown, graphics, mouseX, mouseY, delta) -> drawVanilla(graphics));
         }
      });
   }

   private static void drawVanilla(class_332 graphics) {
      if (OfficialBuild.state() == OfficialBuild.State.MODIFIED) {
         class_327 font = class_310.method_1551().field_1772;
         int pad = 6;
         int lineHeight = 9 + 2;
         int x = 4;
         int y = 4;
         int maxText = Math.max(60, graphics.method_51421() - x * 2 - pad * 2);
         List<class_5481> title = font.method_1728(class_2561.method_43470("This Anubis file has been modified"), maxText);
         List<class_5481> body = new ArrayList<>();

         for (String line : LINES) {
            body.addAll(font.method_1728(class_2561.method_43470(line), maxText));
         }

         int width = 0;

         for (class_5481 row : title) {
            width = Math.max(width, font.method_30880(row));
         }

         for (class_5481 row : body) {
            width = Math.max(width, font.method_30880(row));
         }

         int right = x + width + pad * 2;
         int bottom = y + pad * 2 + lineHeight * (title.size() + body.size());
         graphics.method_25294(x, y, right, bottom, -401929460);
         graphics.method_25294(x, y, right, y + 1, -2537681);
         graphics.method_25294(x, bottom - 1, right, bottom, -2537681);
         graphics.method_25294(x, y, x + 1, bottom, -2537681);
         graphics.method_25294(right - 1, y, right, bottom, -2537681);
         int rowY = y + pad;

         for (class_5481 row : title) {
            graphics.method_51430(font, row, x + pad, rowY, -2537681, true);
            rowY += lineHeight;
         }

         for (class_5481 row : body) {
            graphics.method_51430(font, row, x + pad, rowY, -921103, true);
            rowY += lineHeight;
         }
      }
   }
}
