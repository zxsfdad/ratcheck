package net.anubis.client.gui;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import imgui.ImDrawList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.anubis.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10444;
import net.minecraft.class_1047;
import net.minecraft.class_1058;
import net.minecraft.class_10725;
import net.minecraft.class_10868;
import net.minecraft.class_11515;
import net.minecraft.class_1747;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_4696;
import net.minecraft.class_811;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class BlockIcons {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final float SPRITE_TEXELS = 16.0F;
   private static final int PAGE_SIDE = 1024;
   private static final int RENDERS_PER_FRAME = 48;
   private static final int MAX_SHEETS = 4;
   private static final int MAX_LOSSES = 3;
   private static final int WATER_RGB = 4159204;
   private static final Int2ObjectOpenHashMap<BlockIcons.Sheet> SHEETS = new Int2ObjectOpenHashMap();
   private static final ReferenceOpenHashSet<GpuTexture> CRISP = new ReferenceOpenHashSet();
   private static GpuTexture atlas;
   private static final class_10444 ITEM = new class_10444();
   private static int budgetFrame = -1;
   private static int rendersLeft;
   private static boolean itemsFailed;
   private static int auditedFrame;
   private static int losses;

   private BlockIcons() {
   }

   static void validate() {
      GpuTexture current;
      try {
         current = class_310.method_1551().method_72703().method_73025(class_10725.field_56382).method_68004();
      } catch (RuntimeException notLoaded) {
         current = null;
      }

      if (current != atlas) {
         atlas = current;
         clearSheets();
      } else {
         int frame = ImGuiManager.getInstance().frameCount();
         if (frame - auditedFrame >= 30) {
            auditedFrame = frame;
            if (!intact()) {
               clearSheets();
               if (++losses < 3) {
                  LOGGER.warn("Block icon pages were lost; drawing them again");
               } else {
                  itemsFailed = true;
                  LOGGER.warn("Block icon pages keep getting lost; using block textures instead");
               }
            }
         }
      }
   }

   private static boolean intact() {
      ObjectIterator var0 = SHEETS.values().iterator();

      while (var0.hasNext()) {
         BlockIcons.Sheet sheet = (BlockIcons.Sheet)var0.next();
         if (sheet.depth != null && !ItemIcons.alive(sheet.depth)) {
            return false;
         }

         for (GpuTexture page : sheet.pages) {
            if (!ItemIcons.alive(page)) {
               return false;
            }
         }
      }

      return true;
   }

   static void forget() {
      clearSheets();
   }

   static float size(float target) {
      return Math.max(1, (int)(target / 16.0F)) * 16.0F;
   }

   static void draw(ImDrawList dl, class_2248 block, float x, float y, float size) {
      int px = Math.round(size);
      BlockIcons.Sheet sheet = sheet(px);
      BlockIcons.Icon icon = (BlockIcons.Icon)sheet.icons.get(block);
      if (icon == null) {
         icon = resolve(sheet, block);
         if (icon == null) {
            return;
         }

         sheet.icons.put(block, icon);
      }

      float x0 = Math.round(x);
      float y0 = Math.round(y);
      if (icon.texture != 0) {
         dl.addImage(icon.texture, x0, y0, x0 + px, y0 + px, icon.u0, icon.v0, icon.u1, icon.v1, AnubisWidgets.c(icon.tint));
      } else {
         float r = px * 0.2F;
         dl.addRectFilled(x0, y0, x0 + px, y0 + px, AnubisWidgets.c(-12240588), r);
         dl.addRect(x0, y0, x0 + px, y0 + px, AnubisWidgets.c(536870911), r, 0, 1.0F);
      }
   }

   private static BlockIcons.Sheet sheet(int px) {
      BlockIcons.Sheet sheet = (BlockIcons.Sheet)SHEETS.get(px);
      if (sheet == null) {
         if (SHEETS.size() >= 4) {
            clearSheets();
         }

         sheet = new BlockIcons.Sheet(px);
         SHEETS.put(px, sheet);
      }

      return sheet;
   }

   private static void clearSheets() {
      ObjectIterator var0 = SHEETS.values().iterator();

      while (var0.hasNext()) {
         BlockIcons.Sheet sheet = (BlockIcons.Sheet)var0.next();
         sheet.close();
      }

      SHEETS.clear();
      CRISP.clear();
      ItemIcons.forgetVerdict();
   }

   private static BlockIcons.Icon resolve(BlockIcons.Sheet sheet, class_2248 block) {
      class_1792 item = block.method_8389();
      class_2248 shown = block;
      if (item == class_1802.field_8162) {
         item = standIn(block);
         if (item instanceof class_1747 placed) {
            shown = placed.method_7711();
         }
      }

      if (item == class_1802.field_8162 || itemsFailed || sheet.cell > 256 || !ItemIcons.works()) {
         return sprite(block);
      }

      if (!takeRender()) {
         return null;
      }

      class_310 mc = class_310.method_1551();

      try {
         mc.method_65386().method_65598(ITEM, new class_1799(item), class_811.field_4317, mc.field_1687, null, 0);
      } catch (RuntimeException brokenModel) {
         ITEM.method_65605();
         return sprite(block);
      }

      if (ITEM.method_65606()) {
         return sprite(block);
      }

      BlockIcons.Icon icon = new BlockIcons.Icon();
      boolean seeThrough = class_4696.method_23679(shown.method_9564()) == class_11515.field_60926;

      try {
         renderItem(sheet, icon, seeThrough);
         return icon;
      } catch (RuntimeException e) {
         itemsFailed = true;
         LOGGER.warn("Block item icons failed, using block textures instead", e);
         return sprite(block);
      }
   }

   private static class_1792 standIn(class_2248 block) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1687 != null) {
         try {
            class_1799 picked = block.method_9564().method_65171(mc.field_1687, class_2338.field_10980, false);
            if (!picked.method_7960()) {
               return picked.method_7909();
            }
         } catch (RuntimeException var3) {
         }
      }

      if (block == class_2246.field_10008) {
         return class_1802.field_8249;
      } else if (block == class_2246.field_10027) {
         return class_1802.field_8449;
      } else {
         return block == class_2246.field_10613 ? class_1802.field_8634 : class_1802.field_8162;
      }
   }

   private static boolean takeRender() {
      int frame = ImGuiManager.getInstance().frameCount();
      if (frame != budgetFrame) {
         budgetFrame = frame;
         rendersLeft = 48;
      }

      if (rendersLeft <= 0) {
         return false;
      }

      rendersLeft--;
      return true;
   }

   private static void renderItem(BlockIcons.Sheet sheet, BlockIcons.Icon icon, boolean seeThrough) {
      int cell = sheet.cell;
      int perRow = 1024 / cell;
      if (sheet.pages.isEmpty() || sheet.used >= perRow * perRow) {
         addPage(sheet);
      }

      int slot = sheet.used++;
      int x = slot % perRow * cell;
      int y = slot / perRow * cell;
      int glY = 1024 - y - cell;
      GpuTexture page = sheet.pages.get(sheet.pages.size() - 1);
      GpuTextureView view = sheet.views.get(sheet.views.size() - 1);

      try {
         ItemIcons.render(ITEM, page, view, sheet.depth, sheet.depthView, x, y, cell);
      } finally {
         ITEM.method_65605();
      }

      if (seeThrough) {
         ItemIcons.straighten(page, x, glY, cell);
      }

      icon.texture = ((class_10868)page).method_68427();
      icon.u0 = x / 1024.0F;
      icon.u1 = (x + cell) / 1024.0F;
      icon.v0 = (1024 - y) / 1024.0F;
      icon.v1 = glY / 1024.0F;
   }

   private static void addPage(BlockIcons.Sheet sheet) {
      GpuDevice device = RenderSystem.getDevice();
      if (sheet.depth == null) {
         sheet.depth = ItemIcons.create("Anubis block icons depth", 8, TextureFormat.DEPTH32, 1024);
         sheet.depthView = device.createTextureView(sheet.depth);
      }

      GpuTexture page = ItemIcons.create("Anubis block icons " + sheet.cell + "px", 15, TextureFormat.RGBA8, 1024);
      sheet.pages.add(page);
      sheet.views.add(device.createTextureView(page));
      sheet.used = 0;
      device.createCommandEncoder().clearColorAndDepthTextures(page, 0, sheet.depth, 1.0);
   }

   private static BlockIcons.Icon sprite(class_2248 block) {
      BlockIcons.Icon icon = new BlockIcons.Icon();
      class_310 mc = class_310.method_1551();
      class_2680 state = block.method_9564();

      try {
         class_1058 sprite = mc.method_1554().method_4743().method_3339(state);
         if (class_1047.method_4539().equals(sprite.method_45851().method_45816())) {
            return icon;
         }

         if (!(mc.method_1531().method_4619(sprite.method_45852()).method_68004() instanceof class_10868 gl && !gl.isClosed())) {
            return icon;
         }

         if (CRISP.add(gl)) {
            ItemIcons.crisp(gl);
         }

         icon.texture = gl.method_68427();
         icon.u0 = sprite.method_4594();
         icon.v0 = sprite.method_4593();
         icon.u1 = sprite.method_4577();
         icon.v1 = sprite.method_4575();
      } catch (RuntimeException unavailable) {
         return icon;
      }

      if (!state.method_27852(class_2246.field_10219)) {
         int rgb = -1;

         try {
            rgb = mc.method_1505().method_1697(state, null, null, 0);
         } catch (RuntimeException var7) {
         }

         if (rgb == -1 && (state.method_27852(class_2246.field_10382) || state.method_27852(class_2246.field_10422))) {
            rgb = 4159204;
         }

         if (rgb != -1) {
            icon.tint = 0xFF000000 | (rgb & 0xFF) << 16 | rgb & 0xFF00 | rgb >> 16 & 0xFF;
         }
      }

      return icon;
   }

   @Environment(EnvType.CLIENT)
   private static final class Icon {
      int texture;
      float u0;
      float v0;
      float u1;
      float v1;
      int tint = -1;
   }

   @Environment(EnvType.CLIENT)
   private static final class Sheet {
      final int cell;
      final Reference2ObjectOpenHashMap<class_2248, BlockIcons.Icon> icons = new Reference2ObjectOpenHashMap();
      final List<GpuTexture> pages = new ArrayList<>();
      final List<GpuTextureView> views = new ArrayList<>();
      GpuTexture depth;
      GpuTextureView depthView;
      int used;

      Sheet(int cell) {
         this.cell = cell;
      }

      void close() {
         for (GpuTextureView view : this.views) {
            view.close();
         }

         for (GpuTexture page : this.pages) {
            ItemIcons.dispose(page);
         }

         if (this.depthView != null) {
            this.depthView.close();
         }

         ItemIcons.dispose(this.depth);
         this.views.clear();
         this.pages.clear();
         this.depth = null;
         this.depthView = null;
      }
   }
}
