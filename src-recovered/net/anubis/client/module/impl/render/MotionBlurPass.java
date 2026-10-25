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
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat.class_5596;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10789;
import net.minecraft.class_276;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_6367;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class MotionBlurPass {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final RenderPipeline PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(class_2960.method_60655("anubis", "pipeline/motion_blur"))
      .withVertexShader("core/screenquad")
      .withFragmentShader(class_2960.method_60655("anubis", "core/motion_blur"))
      .withSampler("InSampler")
      .withUniform("MotionBlurConfig", class_10789.field_60031)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withColorWrite(true, false)
      .withDepthWrite(false)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withVertexFormat(class_290.field_60033, class_5596.field_27379)
      .build();
   private static final Supplier<String> PASS_LABEL = () -> "Anubis motion blur";
   private static final Supplier<String> CONFIG_LABEL = () -> "Anubis motion blur config";
   private static final int CONFIG_SIZE = 16;
   private static final long STALE_NANOS = 1000000000L;
   private static final float MIN_RETENTION = 0.003921569F;
   private static class_6367 history;
   private static GpuBuffer config;
   private static GpuBufferSlice configSlice;
   private static ByteBuffer configData;
   private static boolean primed;
   private static long lastFrameNanos;
   private static int levelKey;
   private static int seed;
   private static boolean invalidShaderLogged;

   private MotionBlurPass() {
   }

   static void apply(boolean worldRendered, float retention60) {
      class_310 mc = class_310.method_1551();
      class_276 main = mc.method_1522();
      GpuTexture mainColor = main.method_30277();
      if (worldRendered
         && mc.field_1687 != null
         && mc.method_53466()
         && !mc.field_1773.method_35765()
         && mainColor != null
         && main.field_1482 > 0
         && main.field_1481 > 0) {
         long now = System.nanoTime();
         long gap = now - lastFrameNanos;
         lastFrameNanos = now;
         int level = System.identityHashCode(mc.field_1687);
         if (level != levelKey) {
            levelKey = level;
            primed = false;
         }

         if (history == null) {
            allocate(main.field_1482, main.field_1481);
            primed = false;
         } else if (history.field_1482 != main.field_1482 || history.field_1481 != main.field_1481) {
            history.method_1234(main.field_1482, main.field_1481);
            primed = false;
         }

         GpuDevice device = RenderSystem.getDevice();
         CommandEncoder encoder = device.createCommandEncoder();
         if (primed && gap > 0L && gap < 1000000000L) {
            float retention = (float)Math.pow(retention60, gap * 6.0E-8);
            if (retention >= 0.003921569F && shaderReady(device)) {
               blend(encoder, main, retention);
            }
         }

         encoder.copyTextureToTexture(mainColor, history.method_30277(), 0, 0, 0, 0, 0, main.field_1482, main.field_1481);
         primed = true;
      } else {
         primed = false;
      }
   }

   private static void blend(CommandEncoder encoder, class_276 main, float retention) {
      seed = seed + 1 & 63;
      configData.putFloat(0, retention);
      configData.putFloat(4, seed * 7.0F);
      encoder.writeToBuffer(configSlice, configData);
      RenderPass pass = encoder.createRenderPass(PASS_LABEL, main.method_71639(), OptionalInt.empty());

      try {
         pass.setPipeline(PIPELINE);
         pass.setUniform("MotionBlurConfig", configSlice);
         pass.bindTexture("InSampler", history.method_71639(), RenderSystem.getSamplerCache().method_75294(FilterMode.NEAREST));
         pass.draw(0, 3);
      } catch (Throwable var7) {
         if (pass != null) {
            try {
               pass.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (pass != null) {
         pass.close();
      }
   }

   private static boolean shaderReady(GpuDevice device) {
      if (device.precompilePipeline(PIPELINE).isValid()) {
         invalidShaderLogged = false;
         return true;
      }

      if (!invalidShaderLogged) {
         invalidShaderLogged = true;
         LOGGER.warn("Motion Blur shader {} did not compile; frames are passed through unblurred", PIPELINE.getFragmentShader());
      }

      return false;
   }

   private static void allocate(int width, int height) {
      history = new class_6367("Anubis motion blur history", width, height, false);
      config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 16L);
      configSlice = config.slice();
      configData = BufferUtils.createByteBuffer(16);
   }

   static void release() {
      if (history != null) {
         history.method_1238();
         history = null;
      }

      if (config != null) {
         config.close();
         config = null;
      }

      configSlice = null;
      configData = null;
      primed = false;
      lastFrameNanos = 0L;
      levelKey = 0;
   }
}
