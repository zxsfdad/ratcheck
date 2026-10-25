package net.anubis.client.module.impl.render;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.nio.IntBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1011;
import net.minecraft.class_12137;
import net.minecraft.class_9848;
import org.lwjgl.system.MemoryUtil;

@Environment(EnvType.CLIENT)
final class SkyTexture implements SkyImage {
   private final int width;
   private final int height;
   private boolean mipmapped;
   private GpuTexture texture;
   private GpuTextureView view;

   private SkyTexture(int width, int height, GpuTexture texture, GpuTextureView view) {
      this.width = width;
      this.height = height;
      this.texture = texture;
      this.view = view;
   }

   static SkyTexture upload(String name, int width, int height, int[][] levels) {
      if (levels.length <= 1) {
         return upload(name, width, height, levels.length == 0 ? new int[0] : levels[0]);
      }

      for (int level = 0; level < levels.length; level++) {
         if (levels[level].length < SkyMips.size(width, level) * SkyMips.size(height, level)) {
            throw new IllegalArgumentException("Sky texture " + name + " is missing pixels at mip level " + level);
         }
      }

      String label = "anubis:sky/" + name;
      GpuDevice device = RenderSystem.getDevice();
      GpuTexture texture = device.createTexture(() -> label, 5, TextureFormat.RGBA8, width, height, 1, levels.length);

      try {
         for (int level = 0; level < levels.length; level++) {
            int w = SkyMips.size(width, level);
            int h = SkyMips.size(height, level);
            int[] argb = levels[level];
            class_1011 image = new class_1011(w, h, false);

            try {
               IntBuffer pixels = MemoryUtil.memIntBuffer(image.method_67769(), w * h);

               for (int i = 0; i < w * h; i++) {
                  pixels.put(i, class_9848.method_61337(argb[i]));
               }

               device.createCommandEncoder().writeToTexture(texture, image, level, 0, 0, 0, w, h, 0, 0);
            } catch (Throwable var15) {
               try {
                  image.close();
               } catch (Throwable var14) {
                  var15.addSuppressed(var14);
               }

               throw var15;
            }

            image.close();
         }

         SkyTexture made = new SkyTexture(width, height, texture, device.createTextureView(texture));
         made.mipmapped = true;
         return made;
      } catch (RuntimeException e) {
         texture.close();
         throw e;
      }
   }

   static SkyTexture upload(String name, int width, int height, int[] argb) {
      if (argb.length < width * height) {
         throw new IllegalArgumentException("Sky texture " + name + " is missing pixels");
      }

      String label = "anubis:sky/" + name;
      GpuDevice device = RenderSystem.getDevice();
      class_1011 image = new class_1011(width, height, false);

      SkyTexture e;
      try {
         IntBuffer pixels = MemoryUtil.memIntBuffer(image.method_67769(), width * height);

         for (int i = 0; i < width * height; i++) {
            pixels.put(i, class_9848.method_61337(argb[i]));
         }

         GpuTexture texture = device.createTexture(() -> label, 5, TextureFormat.RGBA8, width, height, 1, 1);

         try {
            device.createCommandEncoder().writeToTexture(texture, image);
            e = new SkyTexture(width, height, texture, device.createTextureView(texture));
         } catch (RuntimeException ex) {
            texture.close();
            throw ex;
         }
      } catch (Throwable var12) {
         try {
            image.close();
         } catch (Throwable var10) {
            var12.addSuppressed(var10);
         }

         throw var12;
      }

      image.close();
      return e;
   }

   @Override
   public int width() {
      return this.width;
   }

   @Override
   public int height() {
      return this.height;
   }

   GpuTextureView view() {
      return this.view;
   }

   class_12137 sampler() {
      return RenderSystem.getSamplerCache().method_76520(FilterMode.LINEAR, this.mipmapped);
   }

   @Override
   public void close() {
      if (this.view != null) {
         this.view.close();
      }

      if (this.texture != null) {
         this.texture.close();
      }

      this.view = null;
      this.texture = null;
   }
}
