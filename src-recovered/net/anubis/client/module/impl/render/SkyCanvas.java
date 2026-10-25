package net.anubis.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.class_5590;
import com.mojang.blaze3d.vertex.VertexFormat.class_5596;
import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10799;
import net.minecraft.class_276;
import net.minecraft.class_287;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_9799;
import net.minecraft.class_9801;
import net.minecraft.class_9848;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
final class SkyCanvas implements SkyPaint {
   private static final float RADIUS = 90.0F;
   private static final float WIDEST_HALF_ANGLE = 1.35F;
   private static final int HALO_SIZE = 128;
   private static final int START_BYTES = 4096 * class_290.field_1575.getVertexSize();
   private static final RenderPipeline BLEND = pipeline("sky_canvas", BlendFunction.TRANSLUCENT);
   private static final RenderPipeline ADD = pipeline("sky_canvas_add", BlendFunction.LIGHTNING);
   private static SkyTexture plain;
   private static SkyTexture halo;
   private SkyCanvas.Run[] runs = new SkyCanvas.Run[64];
   private int runCount;
   private class_9799 bytes;
   private class_287 builder;
   private int quads;
   private float forwardX;
   private float forwardY;
   private float forwardZ;
   private float rightX;
   private float rightZ;
   private float upX;
   private float upY;
   private float upZ;

   private static RenderPipeline pipeline(String name, BlendFunction blend) {
      return RenderPipeline.builder(new Snippet[]{class_10799.field_60125})
         .withLocation(class_2960.method_60655("anubis", "pipeline/" + name))
         .withVertexShader("core/position_tex_color")
         .withFragmentShader("core/position_tex_color")
         .withSampler("Sampler0")
         .withBlend(blend)
         .withDepthWrite(false)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withCull(false)
         .withVertexFormat(class_290.field_1575, class_5596.field_27382)
         .build();
   }

   @Override
   public SkyImage upload(String name, int width, int height, int[] argb) {
      return SkyTexture.upload(name, width, height, argb);
   }

   @Override
   public SkyImage upload(String name, int width, int height, int[][] levels) {
      return SkyTexture.upload(name, width, height, levels);
   }

   @Override
   public void gradient(boolean additive, float u0, float u1, float top, float bottom, int topColor, int bottomColor) {
      class_287 out = this.open(plain(), additive, 1);
      this.onSphere(out, u0, top, 0.0F, 0.0F, topColor);
      this.onSphere(out, u1, top, 1.0F, 0.0F, topColor);
      this.onSphere(out, u1, bottom, 1.0F, 1.0F, bottomColor);
      this.onSphere(out, u0, bottom, 0.0F, 1.0F, bottomColor);
   }

   @Override
   public void band(SkyImage texture, float u0, float u1, float top, float bottom, int color, boolean additive, int pieces) {
      class_287 out = this.open((SkyTexture)texture, additive, pieces);

      for (int i = 0; i < pieces; i++) {
         float from = (float)i / pieces;
         float to = (float)(i + 1) / pieces;
         float left = class_3532.method_16439(from, u0, u1);
         float right = class_3532.method_16439(to, u0, u1);
         this.onSphere(out, left, top, from, 0.0F, color);
         this.onSphere(out, right, top, to, 0.0F, color);
         this.onSphere(out, right, bottom, to, 1.0F, color);
         this.onSphere(out, left, bottom, from, 1.0F, color);
      }
   }

   @Override
   public void sprite(SkyImage texture, float u, float y, float width, float height, float spin, int color, boolean additive) {
      class_287 out = this.open((SkyTexture)texture, additive, 1);
      this.face(u, y);
      float halfWidth = 90.0F * (float)Math.tan(Math.min(width * (float) (Math.PI / 3) * 0.5F, 1.35F));
      float halfHeight = 90.0F * (float)Math.tan(Math.min(height * (float) (Math.PI / 3) * 0.5F, 1.35F));
      float cos = (float)Math.cos(spin);
      float sin = (float)Math.sin(spin);
      this.onPlane(out, -halfWidth, -halfHeight, cos, sin, 0.0F, 0.0F, color);
      this.onPlane(out, halfWidth, -halfHeight, cos, sin, 1.0F, 0.0F, color);
      this.onPlane(out, halfWidth, halfHeight, cos, sin, 1.0F, 1.0F, color);
      this.onPlane(out, -halfWidth, halfHeight, cos, sin, 0.0F, 1.0F, color);
   }

   @Override
   public void glow(float u, float y, float radius, int rgb, float alpha) {
      if (!(alpha <= 0.003F)) {
         this.sprite(halo(), u, y, radius * 2.0F, radius * 2.0F, 0.0F, class_9848.method_71346(class_3532.method_15363(alpha, 0.0F, 1.0F), rgb), true);
      }
   }

   void flush(float alpha) {
      if (this.builder != null) {
         class_287 done = this.builder;
         this.builder = null;

         try {
            class_9801 mesh = done.method_60794();

            label169: {
               try {
                  if (mesh != null && this.runCount != 0) {
                     GpuBuffer vertices = class_290.field_1575.uploadImmediateVertexBuffer(mesh.method_60818());
                     class_5590 indices = RenderSystem.getSequentialBuffer(class_5596.field_27382);
                     GpuBuffer indexBuffer = indices.method_68274(this.quads * 6);
                     GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                        .method_71106(RenderSystem.getModelViewStack(), new Vector4f(1.0F, 1.0F, 1.0F, alpha), new Vector3f(), new Matrix4f());
                     class_276 target = class_310.method_1551().method_1522();
                     RenderPass pass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "Anubis sky canvas", target.method_71639(), OptionalInt.empty(), target.method_71640(), OptionalDouble.empty());

                     try {
                        RenderPipeline active = null;

                        for (int i = 0; i < this.runCount; i++) {
                           SkyCanvas.Run run = this.runs[i];
                           RenderPipeline wanted = run.additive ? ADD : BLEND;
                           if (wanted != active) {
                              active = wanted;
                              pass.setPipeline(wanted);
                              RenderSystem.bindDefaultUniforms(pass);
                              pass.setUniform("DynamicTransforms", transforms);
                              pass.setVertexBuffer(0, vertices);
                              pass.setIndexBuffer(indexBuffer, indices.method_31924());
                           }

                           pass.bindTexture("Sampler0", run.texture.view(), run.texture.sampler());
                           pass.drawIndexed(0, run.first * 6, run.count * 6, 1);
                        }
                     } catch (Throwable var22) {
                        if (pass != null) {
                           try {
                              pass.close();
                           } catch (Throwable var21) {
                              var22.addSuppressed(var21);
                           }
                        }

                        throw var22;
                     }

                     if (pass != null) {
                        pass.close();
                     }
                     break label169;
                  }
               } catch (Throwable var23) {
                  if (mesh != null) {
                     try {
                        mesh.close();
                     } catch (Throwable var20) {
                        var23.addSuppressed(var20);
                     }
                  }

                  throw var23;
               }

               if (mesh != null) {
                  mesh.close();
               }

               return;
            }

            if (mesh != null) {
               mesh.close();
            }
         } finally {
            this.runCount = 0;
            this.quads = 0;
         }
      }
   }

   void clear() {
      class_287 dropped = this.builder;
      this.builder = null;
      this.runCount = 0;
      this.quads = 0;
      if (dropped != null) {
         class_9801 ignored = dropped.method_60794();
         if (ignored != null) {
            ignored.close();
         }
      }
   }

   private class_287 open(SkyTexture texture, boolean additive, int count) {
      if (this.builder == null) {
         if (this.bytes == null) {
            this.bytes = new class_9799(START_BYTES);
         }

         this.builder = new class_287(this.bytes, class_5596.field_27382, class_290.field_1575);
      }

      SkyCanvas.Run last = this.runCount == 0 ? null : this.runs[this.runCount - 1];
      if (last == null || last.texture != texture || last.additive != additive) {
         if (this.runCount == this.runs.length) {
            this.runs = Arrays.copyOf(this.runs, this.runCount * 2);
         }

         last = this.runs[this.runCount];
         if (last == null) {
            last = this.runs[this.runCount] = new SkyCanvas.Run();
         }

         last.texture = texture;
         last.additive = additive;
         last.first = this.quads;
         last.count = 0;
         this.runCount++;
      }

      last.count += count;
      this.quads += count;
      return this.builder;
   }

   private void face(float u, float y) {
      double azimuth = (u - 0.5) * (float) (Math.PI * 2);
      double elevation = (0.72F - y) * (float) (Math.PI / 3);
      float cosA = (float)Math.cos(azimuth);
      float sinA = (float)Math.sin(azimuth);
      float cosE = (float)Math.cos(elevation);
      float sinE = (float)Math.sin(elevation);
      this.forwardX = -cosA * cosE;
      this.forwardY = sinE;
      this.forwardZ = -sinA * cosE;
      this.rightX = sinA;
      this.rightZ = -cosA;
      this.upX = cosA * sinE;
      this.upY = cosE;
      this.upZ = sinA * sinE;
   }

   private void onSphere(class_287 out, float u, float y, float s, float t, int color) {
      this.face(u, y);
      out.method_22912(this.forwardX * 90.0F, this.forwardY * 90.0F, this.forwardZ * 90.0F).method_22913(s, t).method_39415(color);
   }

   private void onPlane(class_287 out, float x, float y, float cos, float sin, float s, float t, int color) {
      float across = x * cos - y * sin;
      float down = x * sin + y * cos;
      out.method_22912(
            this.forwardX * 90.0F + this.rightX * across - this.upX * down,
            this.forwardY * 90.0F - this.upY * down,
            this.forwardZ * 90.0F + this.rightZ * across - this.upZ * down
         )
         .method_22913(s, t)
         .method_39415(color);
   }

   private static SkyTexture plain() {
      if (plain == null) {
         int[] white = new int[4];
         Arrays.fill(white, -1);
         plain = SkyTexture.upload("plain", 2, 2, white);
      }

      return plain;
   }

   private static SkyTexture halo() {
      if (halo == null) {
         int[] pixels = new int[16384];
         float middle = 64.0F;

         for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
               float d = Math.min(1.0F, (float)Math.hypot(x + 0.5F - middle, y + 0.5F - middle) / middle);
               float rest = 1.0F - d;
               float rest2 = rest * rest;
               pixels[y * 128 + x] = class_9848.method_71346(class_3532.method_15363(0.85F * rest2 * rest2 + 0.15F * rest, 0.0F, 1.0F), 16777215);
            }
         }

         halo = SkyTexture.upload("halo", 128, 128, pixels);
      }

      return halo;
   }

   @Environment(EnvType.CLIENT)
   private static final class Run {
      SkyTexture texture;
      boolean additive;
      int first;
      int count;
   }
}
