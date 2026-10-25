package net.anubis.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.class_5596;
import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10017;
import net.minecraft.class_10055;
import net.minecraft.class_10366;
import net.minecraft.class_10789;
import net.minecraft.class_11286;
import net.minecraft.class_11661;
import net.minecraft.class_11684;
import net.minecraft.class_12075;
import net.minecraft.class_1297;
import net.minecraft.class_1921;
import net.minecraft.class_243;
import net.minecraft.class_276;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4618;
import net.minecraft.class_6367;
import net.minecraft.class_777;
import net.minecraft.class_8921;
import net.minecraft.class_898;
import net.minecraft.class_9779;
import net.minecraft.class_9799;
import net.minecraft.class_4587.class_4665;
import net.minecraft.class_4597.class_4598;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class ShaderEspPass {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final RenderPipeline PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(class_2960.method_60655("anubis", "pipeline/glow_esp"))
      .withVertexShader("core/screenquad")
      .withFragmentShader(class_2960.method_60655("anubis", "core/glow_esp"))
      .withSampler("SilhouetteSampler")
      .withSampler("HaloSampler")
      .withUniform("GlowEspConfig", class_10789.field_60031)
      .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
      .withColorWrite(true, false)
      .withDepthWrite(false)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withVertexFormat(class_290.field_60033, class_5596.field_27379)
      .build();
   private static final Supplier<String> PASS_LABEL = () -> "Anubis glow esp";
   private static final Supplier<String> CONFIG_LABEL = () -> "Anubis glow esp config";
   private static final int CONFIG_SIZE = 32;
   private static final class_12075 CAMERA = new class_12075();
   private static final class_4587 POSE = new class_4587();
   private static final Map<class_1297, class_10017> KEPT = new IdentityHashMap<>();
   private static ShaderEspPass.Rig rig;
   private static class_11286 projectionBuffer;
   private static class_6367 mask;
   private static AuraBlur blur;
   private static GpuBuffer config;
   private static GpuBufferSlice configSlice;
   private static ByteBuffer configData;
   private static long keptFrame = Long.MIN_VALUE;
   private static boolean invalidShaderLogged;

   private ShaderEspPass() {
   }

   static void keep(class_1297 entity, class_10017 state, long frame) {
      if (keptFrame != frame) {
         KEPT.clear();
         keptFrame = frame;
      }

      KEPT.put(entity, state);
   }

   static void dropKept() {
      KEPT.clear();
      keptFrame = Long.MIN_VALUE;
   }

   static boolean paint(class_4184 camera, Matrix4f viewRotation, Matrix4f projection, class_1297[] entities, int[] tints, int count, long frame) {
      class_310 mc = class_310.method_1551();
      class_276 main = mc.method_1522();
      if (count > 0
         && mc.field_1687 != null
         && mc.method_53466()
         && !mc.field_1773.method_35765()
         && main.method_30277() != null
         && main.field_1482 > 0
         && main.field_1481 > 0) {
         GpuDevice device = RenderSystem.getDevice();
         if (!shaderReady(device)) {
            return false;
         }

         if (mask == null) {
            allocate(main.field_1482, main.field_1481);
         } else if (mask.field_1482 != main.field_1482 || mask.field_1481 != main.field_1481) {
            mask.method_1234(main.field_1482, main.field_1481);
         }

         if (rig == null) {
            rig = new ShaderEspPass.Rig(mc);
         }

         if (projectionBuffer == null) {
            projectionBuffer = new class_11286("anubis glow esp");
         }

         device.createCommandEncoder().clearColorTexture(mask.method_30277(), 0);
         submit(mc, camera, entities, tints, count, frame);
         flush(viewRotation, projection);
         return true;
      } else {
         return false;
      }
   }

   private static void submit(class_310 mc, class_4184 camera, class_1297[] entities, int[] tints, int count, long frame) {
      class_898 dispatcher = mc.method_1561();
      class_9779 delta = mc.method_61966();
      class_8921 ticks = mc.field_1687.method_54719();
      class_243 eye = camera.method_71156();
      class_1297 viewer = camera.method_19331();
      boolean reuse = keptFrame == frame;
      CAMERA.field_63079 = camera.method_19332();
      CAMERA.field_63078 = eye;
      CAMERA.field_63077 = camera.method_19328();
      CAMERA.field_63080 = viewer == null ? eye : viewer.method_30950(delta.method_60637(true));
      CAMERA.field_63081.set(camera.method_23767());

      for (int i = 0; i < count; i++) {
         class_1297 entity = entities[i];
         if (entity != null && !entity.method_31481()) {
            class_10017 state = reuse ? KEPT.get(entity) : null;
            if (state == null) {
               state = dispatcher.method_72977(entity, delta.method_60637(!ticks.method_54746(entity)));
            }

            silhouetteOnly(state);
            state.field_61821 = tints[i] | 0xFF000000;
            dispatcher.method_72976(
               state, CAMERA, state.field_53325 - eye.field_1352, state.field_53326 - eye.field_1351, state.field_53327 - eye.field_1350, POSE, rig.storage
            );
         }
      }
   }

   private static void silhouetteOnly(class_10017 state) {
      state.field_53337 = null;
      state.field_60160 = null;
      state.field_53335 = false;
      state.field_61823.clear();
      if (state instanceof class_10055 avatar) {
         avatar.field_53525 = null;
      }
   }

   private static void flush(Matrix4f viewRotation, Matrix4f projection) {
      GpuTextureView previousColor = RenderSystem.outputColorTextureOverride;
      GpuTextureView previousDepth = RenderSystem.outputDepthTextureOverride;
      Matrix4fStack modelView = RenderSystem.getModelViewStack();
      RenderSystem.backupProjectionMatrix();
      RenderSystem.setProjectionMatrix(projectionBuffer.method_71123(projection), class_10366.field_54953);
      modelView.pushMatrix();
      modelView.mul(viewRotation);
      RenderSystem.outputColorTextureOverride = mask.method_71639();
      RenderSystem.outputDepthTextureOverride = null;

      try {
         rig.features.method_73002();
         rig.outlines.method_23285();
      } finally {
         RenderSystem.outputColorTextureOverride = previousColor;
         RenderSystem.outputDepthTextureOverride = previousDepth;
         modelView.popMatrix();
         RenderSystem.restoreProjectionMatrix();
         rig.storage.method_72953();
      }

      rig.storage.method_72954();
   }

   static void compose(float strokePx, float aura, float reachPx, float fill, float phase, int sheenArgb, float sheen) {
      class_276 main = class_310.method_1551().method_1522();
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      GpuTextureView halo = blur.spread(encoder, mask.method_71639(), mask.field_1482, mask.field_1481, Math.max(reachPx, strokePx + 2.0F));
      configData.putFloat(0, strokePx);
      configData.putFloat(4, aura);
      configData.putFloat(8, fill);
      configData.putFloat(12, phase);
      configData.putFloat(16, (sheenArgb >> 16 & 0xFF) / 255.0F);
      configData.putFloat(20, (sheenArgb >> 8 & 0xFF) / 255.0F);
      configData.putFloat(24, (sheenArgb & 0xFF) / 255.0F);
      configData.putFloat(28, sheen);
      encoder.writeToBuffer(configSlice, configData);
      RenderPass pass = encoder.createRenderPass(PASS_LABEL, main.method_71639(), OptionalInt.empty());

      try {
         pass.setPipeline(PIPELINE);
         pass.setUniform("GlowEspConfig", configSlice);
         pass.bindTexture("SilhouetteSampler", mask.method_71639(), RenderSystem.getSamplerCache().method_75294(FilterMode.LINEAR));
         pass.bindTexture("HaloSampler", halo, RenderSystem.getSamplerCache().method_75294(FilterMode.LINEAR));
         pass.draw(0, 3);
      } catch (Throwable var14) {
         if (pass != null) {
            try {
               pass.close();
            } catch (Throwable var13) {
               var14.addSuppressed(var13);
            }
         }

         throw var14;
      }

      if (pass != null) {
         pass.close();
      }
   }

   private static boolean shaderReady(GpuDevice device) {
      if (device.precompilePipeline(PIPELINE).isValid() && AuraBlur.compiled(device)) {
         invalidShaderLogged = false;
         return true;
      }

      if (!invalidShaderLogged) {
         invalidShaderLogged = true;
         LOGGER.warn("Glow ESP shaders did not compile; entities are left without outlines");
      }

      return false;
   }

   private static void allocate(int width, int height) {
      mask = new class_6367("Anubis glow esp mask", width, height, false);
      blur = new AuraBlur("Anubis glow esp aura");
      config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 32L);
      configSlice = config.slice();
      configData = BufferUtils.createByteBuffer(32);
   }

   static void release() {
      dropKept();
      if (rig != null) {
         rig.close();
         rig = null;
      }

      if (mask != null) {
         mask.method_1238();
         mask = null;
      }

      if (blur != null) {
         blur.release();
         blur = null;
      }

      if (config != null) {
         config.close();
         config = null;
      }

      if (projectionBuffer != null) {
         projectionBuffer.close();
         projectionBuffer = null;
      }

      configSlice = null;
      configData = null;
   }

   @Environment(EnvType.CLIENT)
   private static final class Drain extends class_4598 {
      private final Map<class_1921, class_4588> sinks = new IdentityHashMap<>();

      Drain() {
         super(new class_9799(64), new LinkedHashMap());
      }

      void close() {
         this.field_52156.close();
      }

      public class_4588 method_73477(class_1921 renderType) {
         return this.sinks.computeIfAbsent(renderType, type -> new ShaderEspPass.Nowhere());
      }

      public void method_37104() {
      }

      public void method_22993() {
      }

      public void method_22994(class_1921 renderType) {
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Nowhere implements class_4588 {
      public class_4588 method_22912(float x, float y, float z) {
         return this;
      }

      public void method_23919(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
      }

      public void method_22919(class_4665 pose, class_777 quad, float red, float green, float blue, float alpha, int light, int overlay) {
      }

      public void method_22920(class_4665 pose, class_777 quad, float[] brightness, float red, float green, float blue, float alpha, int[] lights, int overlay) {
      }

      public class_4588 method_1336(int red, int green, int blue, int alpha) {
         return this;
      }

      public class_4588 method_39415(int argb) {
         return this;
      }

      public class_4588 method_22913(float u, float v) {
         return this;
      }

      public class_4588 method_60796(int u, int v) {
         return this;
      }

      public class_4588 method_22921(int u, int v) {
         return this;
      }

      public class_4588 method_22914(float x, float y, float z) {
         return this;
      }

      public class_4588 method_75298(float width) {
         return this;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Rig {
      final class_11661 storage = new class_11661();
      final ShaderEspPass.Drain drain = new ShaderEspPass.Drain();
      final class_4618 outlines;
      final class_11684 features;

      Rig(class_310 mc) {
         this.outlines = mc.method_22940().method_23003();
         this.features = new class_11684(this.storage, mc.method_1541(), this.drain, mc.method_72703(), this.outlines, this.drain, mc.field_1772);
      }

      void close() {
         this.features.close();
         this.drain.close();
      }
   }
}
