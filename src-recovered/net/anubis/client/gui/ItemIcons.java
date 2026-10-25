package net.anubis.client.gui;

import com.example.client.mixin.AccessorGameRenderer;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.buffers.GpuBuffer.MappedView;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import imgui.ImDrawList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.anubis.client.imgui.ImGuiManager;
import net.anubis.client.module.impl.client.PerformanceModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10366;
import net.minecraft.class_10444;
import net.minecraft.class_10725;
import net.minecraft.class_10868;
import net.minecraft.class_11278;
import net.minecraft.class_11515;
import net.minecraft.class_11661;
import net.minecraft.class_11684;
import net.minecraft.class_1747;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_4696;
import net.minecraft.class_811;
import net.minecraft.class_9799;
import net.minecraft.class_1011.class_1012;
import net.minecraft.class_308.class_11274;
import net.minecraft.class_4597.class_4598;
import net.minecraft.class_758.class_4596;
import org.joml.Matrix4fStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class ItemIcons {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final int SHEET_SIDE = 512;
   private static final int MIN_PX = 8;
   private static final int MAX_PX = 96;
   private static final int MAX_SHEETS = 8;
   private static final int BUSY_FRAMES = 2;
   private static final int IDLE_FRAMES = 600;
   private static final int PAINTS_PER_FRAME = 6;
   private static final int CHECK_SIDE = 32;
   static final int AUDIT_FRAMES = 30;
   private static final ItemIcons.Cell BLANK = new ItemIcons.Cell();
   private static final Int2ObjectOpenHashMap<ItemIcons.Sheet> SHEETS = new Int2ObjectOpenHashMap();
   private static final List<ItemIcons.Sheet> RETIRED = new ArrayList<>();
   private static final Reference2ObjectOpenHashMap<class_1792, class_1799> PLAIN = new Reference2ObjectOpenHashMap();
   private static final ReferenceOpenHashSet<class_1792> BROKEN = new ReferenceOpenHashSet();
   private static final ItemLook MODEL = new ItemLook();
   private static final int[] VIEWPORT = new int[4];
   private static GpuTexture depth;
   private static GpuTextureView depthView;
   private static class_11278 projection;
   private static class_11661 submits;
   private static class_11684 features;
   private static class_4598 buffers;
   private static GpuBuffer readback;
   private static ByteBuffer straightened;
   private static boolean straightenFailed;
   private static int verdict;
   private static GpuTexture itemAtlas;
   private static int frame = Integer.MIN_VALUE;
   private static int retiredFrame;
   private static int auditedFrame;
   private static int paintsLeft;
   private static boolean disabled;

   private ItemIcons() {
   }

   static float pixels(float size) {
      float snapped = size < 16.0F ? Math.round(size) : 16.0F * (float)Math.floor(size / 16.0F + 0.25F);
      return Math.max(8.0F, Math.min(96.0F, snapped));
   }

   static void draw(ImDrawList dl, class_1792 item, float x, float y, float size, float alpha) {
      if (item != null && item != class_1802.field_8162) {
         class_1799 plain = (class_1799)PLAIN.get(item);
         if (plain == null) {
            plain = item.method_7854();
            PLAIN.put(item, plain);
         }

         draw(dl, plain, x, y, size, alpha);
      }
   }

   public static void draw(ImDrawList dl, class_1799 stack, float x, float y, float size, float alpha) {
      if (stack != null && !stack.method_7960() && !(alpha <= 0.004F)) {
         startFrame();
         int px = (int)pixels(size);
         float x0 = Math.round(x);
         float y0 = Math.round(y);

         ItemIcons.Cell cell;
         try {
            cell = cell(stack, px);
         } finally {
            MODEL.method_65605();
         }

         if (cell != null) {
            if (cell.texture == 0) {
               float r = Math.max(1.0F, px * 0.18F);
               dl.addRectFilled(x0 + 1.0F, y0 + 1.0F, x0 + px - 1.0F, y0 + px - 1.0F, HudChrome.scaleAlpha(452984831, alpha), r);
            } else {
               dl.addImage(cell.texture, x0, y0, x0 + px, y0 + px, cell.u0, cell.v0, cell.u1, cell.v1, HudChrome.scaleAlpha(-1, alpha));
            }
         }
      }
   }

   private static void startFrame() {
      int now = ImGuiManager.getInstance().frameCount();
      if (now != frame) {
         frame = now;
         paintsLeft = PerformanceModule.optimizationsEnabled() ? 2 : 6;
         if (!RETIRED.isEmpty() && now != retiredFrame) {
            for (ItemIcons.Sheet sheet : RETIRED) {
               sheet.release();
            }

            RETIRED.clear();
         }

         GpuTexture atlas = currentAtlas();
         boolean lost = now - auditedFrame >= 30 && audit();
         if (atlas == itemAtlas && !lost) {
            Iterator<ItemIcons.Sheet> it = SHEETS.values().iterator();

            while (it.hasNext()) {
               ItemIcons.Sheet sheet = it.next();
               if (now - sheet.lastUsed > 600) {
                  sheet.release();
                  it.remove();
               }
            }
         } else {
            itemAtlas = atlas;
            ObjectIterator it = SHEETS.values().iterator();

            while (it.hasNext()) {
               ItemIcons.Sheet sheet = (ItemIcons.Sheet)it.next();
               sheet.release();
            }

            SHEETS.clear();
            BROKEN.clear();
         }
      }
   }

   private static boolean audit() {
      auditedFrame = frame;
      boolean lost = false;
      if (depth != null && !alive(depth)) {
         depthView.close();
         depth = null;
         depthView = null;
         lost = true;
      }

      ObjectIterator var1 = SHEETS.values().iterator();

      while (var1.hasNext()) {
         ItemIcons.Sheet sheet = (ItemIcons.Sheet)var1.next();
         lost |= sheet.color != null && !alive(sheet.color);
      }

      if (lost) {
         LOGGER.warn("HUD item icon textures were lost; drawing them again");
      }

      return lost;
   }

   private static GpuTexture currentAtlas() {
      try {
         return class_310.method_1551().method_72703().method_73025(class_10725.field_64477).method_68004();
      } catch (RuntimeException notReady) {
         return null;
      }
   }

   private static ItemIcons.Cell cell(class_1799 stack, int px) {
      ItemIcons.Sheet sheet = sheet(px);
      if (sheet == null) {
         return null;
      }

      sheet.lastUsed = frame;
      if (!resolve(stack)) {
         return BLANK;
      }

      ItemIcons.Cell cell = (ItemIcons.Cell)sheet.cells.get(MODEL.identity);
      if (cell != null) {
         return cell;
      }

      if (disabled) {
         return BLANK;
      }

      if (paintsLeft <= 0) {
         return null;
      }

      paintsLeft--;
      if (sheet.full()) {
         retire(sheet);
         sheet = new ItemIcons.Sheet(px);
         sheet.lastUsed = frame;
         SHEETS.put(px, sheet);
      }

      cell = paint(sheet, seeThrough(stack.method_7909()));
      sheet.cells.put(Arrays.asList(MODEL.identity.toArray()), cell);
      return cell;
   }

   private static ItemIcons.Sheet sheet(int px) {
      ItemIcons.Sheet sheet = (ItemIcons.Sheet)SHEETS.get(px);
      if (sheet != null) {
         return sheet;
      }

      if (SHEETS.size() >= 8) {
         ItemIcons.Sheet oldest = stalest();
         if (oldest == null || frame - oldest.lastUsed < 2) {
            return null;
         }

         retire(oldest);
      }

      sheet = new ItemIcons.Sheet(px);
      SHEETS.put(px, sheet);
      return sheet;
   }

   private static ItemIcons.Sheet stalest() {
      ItemIcons.Sheet oldest = null;
      ObjectIterator var1 = SHEETS.values().iterator();

      while (var1.hasNext()) {
         ItemIcons.Sheet sheet = (ItemIcons.Sheet)var1.next();
         if (oldest == null || sheet.lastUsed < oldest.lastUsed) {
            oldest = sheet;
         }
      }

      return oldest;
   }

   private static void retire(ItemIcons.Sheet sheet) {
      SHEETS.remove(sheet.px);
      RETIRED.add(sheet);
      retiredFrame = frame;
   }

   private static boolean resolve(class_1799 stack) {
      if (BROKEN.contains(stack.method_7909())) {
         return false;
      }

      if (MODEL.resolve(stack)) {
         return true;
      }

      BROKEN.add(stack.method_7909());
      return false;
   }

   private static boolean seeThrough(class_1792 item) {
      return item instanceof class_1747 block ? class_4696.method_23679(block.method_7711().method_9564()) == class_11515.field_60926 : false;
   }

   private static ItemIcons.Cell paint(ItemIcons.Sheet sheet, boolean seeThrough) {
      if (MODEL.method_65606()) {
         return BLANK;
      }

      try {
         if (sheet.color == null) {
            allocate(sheet);
         }

         int px = sheet.px;
         int perRow = 512 / px;
         int index = sheet.filled++;
         int left = index % perRow * px;
         int top = index / perRow * px;
         int glTop = 512 - top - px;
         render(MODEL, sheet.color, sheet.colorView, depth, depthView, left, top, px);
         if (seeThrough) {
            straighten(sheet.color, left, glTop, px);
         }

         ItemIcons.Cell cell = new ItemIcons.Cell();
         cell.texture = ((class_10868)sheet.color).method_68427();
         cell.u0 = left / 512.0F;
         cell.u1 = (left + px) / 512.0F;
         cell.v0 = (512 - top) / 512.0F;
         cell.v1 = glTop / 512.0F;
         return cell;
      } catch (RuntimeException failure) {
         disabled = true;
         LOGGER.warn("HUD item icons could not be drawn; showing plain slots instead", failure);
         return BLANK;
      }
   }

   private static void allocate(ItemIcons.Sheet sheet) {
      GpuDevice device = RenderSystem.getDevice();
      if (depth == null) {
         depth = create("Anubis HUD icon depth", 8, TextureFormat.DEPTH32, 512);
         depthView = device.createTextureView(depth);
      }

      sheet.color = create("Anubis HUD icons " + sheet.px, 15, TextureFormat.RGBA8, 512);
      sheet.colorView = device.createTextureView(sheet.color);
      device.createCommandEncoder().clearColorAndDepthTextures(sheet.color, 0, depth, 1.0);
   }

   public static void render(
      class_10444 model, GpuTexture target, GpuTextureView targetView, GpuTexture targetDepth, GpuTextureView targetDepthView, int x, int y, int cell
   ) {
      class_310 mc = class_310.method_1551();
      int side = target.getWidth(0);
      int glY = side - y - cell;
      RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target, 0, targetDepth, 1.0, x, glY, cell, cell);
      if (features == null) {
         buffers = class_4597.method_22991(new class_9799(262144));
         submits = new class_11661();
         features = new class_11684(
            submits, mc.method_1541(), buffers, mc.method_72703(), mc.method_22940().method_23003(), mc.method_22940().method_23001(), mc.field_1772
         );
      }

      GpuTextureView colorBefore = RenderSystem.outputColorTextureOverride;
      GpuTextureView depthBefore = RenderSystem.outputDepthTextureOverride;
      GpuBufferSlice lightsBefore = RenderSystem.getShaderLights();
      GpuBufferSlice fogBefore = RenderSystem.getShaderFog();
      GpuBufferSlice projectionBefore = RenderSystem.getProjectionMatrixBuffer();
      class_10366 projectionTypeBefore = RenderSystem.getProjectionType();
      GL11.glGetIntegerv(2978, VIEWPORT);
      GlStateManager._viewport(VIEWPORT[0], VIEWPORT[1], VIEWPORT[2], VIEWPORT[3]);
      Matrix4fStack modelView = RenderSystem.getModelViewStack();
      modelView.pushMatrix();
      modelView.identity();

      try {
         RenderSystem.outputColorTextureOverride = targetView;
         RenderSystem.outputDepthTextureOverride = targetDepthView;
         if (projection == null) {
            projection = new class_11278("Anubis icons", -1000.0F, 1000.0F, true);
         }

         RenderSystem.setProjectionMatrix(projection.method_71092(side, side), class_10366.field_54954);
         class_11274 light = model.method_65608() ? class_11274.field_60027 : class_11274.field_60026;
         mc.field_1773.method_71114().method_71034(light);
         RenderSystem.setShaderFog(((AccessorGameRenderer)mc.field_1773).anubis$getFogRenderer().method_71109(class_4596.field_60101));
         RenderSystem.enableScissorForRenderTypeDraws(x, glY, cell, cell);
         class_4587 pose = new class_4587();
         pose.method_46416(x + cell * 0.5F, y + cell * 0.5F, 0.0F);
         pose.method_22905(cell, -cell, cell);

         try {
            model.method_65604(pose, submits, 15728880, class_4608.field_21444, 0);
            features.method_73002();
         } finally {
            submits.method_72953();
            buffers.method_22993();
         }
      } finally {
         RenderSystem.disableScissorForRenderTypeDraws();
         RenderSystem.outputColorTextureOverride = colorBefore;
         RenderSystem.outputDepthTextureOverride = depthBefore;
         RenderSystem.setShaderLights(lightsBefore);
         RenderSystem.setShaderFog(fogBefore);
         RenderSystem.setProjectionMatrix(projectionBefore, projectionTypeBefore);
         modelView.popMatrix();
         GlStateManager._viewport(VIEWPORT[0], VIEWPORT[1], VIEWPORT[2], VIEWPORT[3]);
      }
   }

   static void straighten(GpuTexture page, int x, int y, int cell) {
      if (!straightenFailed) {
         int bytes = cell * cell * 4;
         long whole = (long)page.getWidth(0) * page.getHeight(0) * 4L;

         try {
            GpuDevice device = RenderSystem.getDevice();
            if (readback == null || readback.size() < whole) {
               if (readback != null) {
                  readback.close();
               }

               readback = device.createBuffer(() -> "Anubis icon readback", 9, whole);
            }

            if (straightened == null || straightened.capacity() < bytes) {
               straightened = BufferUtils.createByteBuffer(bytes);
            }

            CommandEncoder encoder = device.createCommandEncoder();
            encoder.copyTextureToBuffer(page, readback, 0L, () -> {}, 0, x, y, cell, cell);
            GpuFence copied = encoder.createFence();

            try {
               GL11.glFlush();
               if (!copied.awaitCompletion(1000000000L)) {
                  throw new IllegalStateException("icon read-back timed out");
               }
            } catch (Throwable var25) {
               if (copied != null) {
                  try {
                     copied.close();
                  } catch (Throwable var23) {
                     var25.addSuppressed(var23);
                  }
               }

               throw var25;
            }

            if (copied != null) {
               copied.close();
            }

            straightened.clear().limit(bytes);
            MappedView view = encoder.mapBuffer(readback.slice(0L, bytes), true, false);

            try {
               ByteBuffer read = view.data();

               for (int i = 0; i < bytes; i += 4) {
                  int a = read.get(i + 3) & 255;

                  for (int ch = 0; ch < 3; ch++) {
                     int v = a == 0 ? 0 : Math.min(255, ((read.get(i + ch) & 255) * 255 + a / 2) / a);
                     straightened.put(i + ch, (byte)v);
                  }

                  straightened.put(i + 3, (byte)a);
               }
            } catch (Throwable var24) {
               if (view != null) {
                  try {
                     view.close();
                  } catch (Throwable var22) {
                     var24.addSuppressed(var22);
                  }
               }

               throw var24;
            }

            if (view != null) {
               view.close();
            }

            encoder.writeToTexture(page, straightened, class_1012.field_4997, 0, 0, x, y, cell, cell);
         } catch (RuntimeException e) {
            straightenFailed = true;
            LOGGER.warn("Couldn't read icons back; see-through ones stay darker", e);
         } finally {
            GlStateManager._pixelStore(3330, 0);
         }
      }
   }

   static void forgetVerdict() {
      verdict = 0;
   }

   static boolean works() {
      if (verdict == 0) {
         verdict = check() ? 1 : -1;
      }

      return verdict > 0;
   }

   private static boolean check() {
      class_310 mc = class_310.method_1551();
      GpuDevice device = RenderSystem.getDevice();
      class_10444 state = new class_10444();
      GpuTexture color = null;
      GpuTexture checkDepth = null;
      GpuTextureView colorView = null;
      GpuTextureView checkDepthView = null;
      GpuBuffer pixels = null;

      boolean var30;
      try {
         mc.method_65386().method_65598(state, new class_1799(class_1802.field_20391), class_811.field_4317, mc.field_1687, null, 0);
         if (state.method_65606()) {
            return true;
         }

         color = create("Anubis icon check", 10, TextureFormat.RGBA8, 32);
         checkDepth = create("Anubis icon check depth", 8, TextureFormat.DEPTH32, 32);
         colorView = device.createTextureView(color);
         checkDepthView = device.createTextureView(checkDepth);
         render(state, color, colorView, checkDepth, checkDepthView, 0, 0, 32);
         pixels = device.createBuffer(() -> "Anubis icon check", 9, 4096L);
         CommandEncoder encoder = device.createCommandEncoder();
         encoder.copyTextureToBuffer(color, pixels, 0L, () -> {}, 0, 0, 0, 32, 32);
         GpuFence copied = encoder.createFence();

         label313: {
            try {
               GL11.glFlush();
               if (!copied.awaitCompletion(1000000000L)) {
                  var30 = true;
                  break label313;
               }
            } catch (Throwable var26) {
               if (copied != null) {
                  try {
                     copied.close();
                  } catch (Throwable var24) {
                     var26.addSuppressed(var24);
                  }
               }

               throw var26;
            }

            if (copied != null) {
               copied.close();
            }

            int covered = 0;
            long brightness = 0L;
            MappedView view = encoder.mapBuffer(pixels, true, false);

            try {
               ByteBuffer data = view.data();

               for (int i = 0; i < 4096; i += 4) {
                  if ((data.get(i + 3) & 255) >= 128) {
                     covered++;
                     brightness += (data.get(i) & 255) + (data.get(i + 1) & 255) + (data.get(i + 2) & 255);
                  }
               }
            } catch (Throwable var25) {
               if (view != null) {
                  try {
                     view.close();
                  } catch (Throwable var23) {
                     var25.addSuppressed(var23);
                  }
               }

               throw var25;
            }

            if (view != null) {
               view.close();
            }

            long average = covered == 0 ? 0L : brightness / covered / 3L;
            boolean fine = covered >= 128 && average >= 24L;
            if (!fine) {
               LOGGER.warn("Item icons come out blank or black here (covered {}, brightness {}); block lists use flat textures", covered, average);
            }

            return fine;
         }

         if (copied != null) {
            copied.close();
         }
      } catch (RuntimeException e) {
         LOGGER.warn("Couldn't check item icons", e);
         return true;
      } finally {
         state.method_65605();
         GlStateManager._pixelStore(3330, 0);
         if (pixels != null) {
            pixels.close();
         }

         if (colorView != null) {
            colorView.close();
         }

         if (checkDepthView != null) {
            checkDepthView.close();
         }

         if (color != null) {
            color.close();
         }

         if (checkDepth != null) {
            checkDepth.close();
         }
      }

      return var30;
   }

   static GpuTexture create(String label, int usage, TextureFormat format, int side) {
      GlStateManager._activeTexture(GL11.glGetInteger(34016));
      GlStateManager._bindTexture(GL11.glGetInteger(32873));
      GpuTexture texture = RenderSystem.getDevice().createTexture(label, usage, format, side, side, 1, 1);
      if (texture instanceof class_10868 gl && !prepare(gl)) {
         texture.close();
         throw new IllegalStateException(label + " got no storage");
      } else {
         return texture;
      }
   }

   private static boolean prepare(class_10868 gl) {
      int bound = GL11.glGetInteger(32873);
      GL11.glBindTexture(3553, gl.method_68427());

      try {
         if (!holds(gl)) {
            int unpack = GL11.glGetInteger(35055);
            GL15.glBindBuffer(35052, 0);
            TextureFormat format = gl.getFormat();
            GL11.glTexImage2D(
               3553,
               0,
               GlConst.toGlInternalId(format),
               gl.getWidth(0),
               gl.getHeight(0),
               0,
               GlConst.toGlExternalId(format),
               GlConst.toGlType(format),
               (ByteBuffer)null
            );
            GL15.glBindBuffer(35052, unpack);
         }

         GL11.glTexParameteri(3553, 33085, 0);
         GL11.glTexParameteri(3553, 10241, 9728);
         GL11.glTexParameteri(3553, 10240, 9728);
         return holds(gl);
      } finally {
         GL11.glBindTexture(3553, bound);
      }
   }

   static boolean alive(GpuTexture texture) {
      if (texture.isClosed()) {
         return false;
      }

      if (texture instanceof class_10868 gl) {
         if (!GL11.glIsTexture(gl.method_68427())) {
            return false;
         }

         int bound = GL11.glGetInteger(32873);
         GL11.glBindTexture(3553, gl.method_68427());
         boolean alive = GL11.glGetInteger(32873) == gl.method_68427() && holds(gl);
         GL11.glBindTexture(3553, bound);
         if (!alive) {
            GlStateManager.clearGlErrors();
         }

         return alive;
      } else {
         return true;
      }
   }

   private static boolean holds(GpuTexture texture) {
      return GL11.glGetTexLevelParameteri(3553, 0, 4096) == texture.getWidth(0) && GL11.glGetTexLevelParameteri(3553, 0, 4097) == texture.getHeight(0);
   }

   static void dispose(GpuTexture texture) {
      if (texture != null && alive(texture)) {
         texture.close();
      }
   }

   static void crisp(class_10868 gl) {
      int bound = GL11.glGetInteger(32873);
      GL11.glBindTexture(3553, gl.method_68427());
      GL11.glTexParameteri(3553, 10240, 9728);
      GL11.glBindTexture(3553, bound);
   }

   @Environment(EnvType.CLIENT)
   private static final class Cell {
      int texture;
      float u0;
      float v0;
      float u1;
      float v1;
   }

   @Environment(EnvType.CLIENT)
   private static final class Sheet {
      final int px;
      final Object2ObjectOpenHashMap<Object, ItemIcons.Cell> cells = new Object2ObjectOpenHashMap();
      GpuTexture color;
      GpuTextureView colorView;
      int filled;
      int lastUsed;

      Sheet(int px) {
         this.px = px;
      }

      boolean full() {
         int perRow = 512 / this.px;
         return this.filled >= perRow * perRow;
      }

      void release() {
         if (this.colorView != null) {
            this.colorView.close();
         }

         ItemIcons.dispose(this.color);
         this.colorView = null;
         this.color = null;
         this.cells.clear();
      }
   }
}
