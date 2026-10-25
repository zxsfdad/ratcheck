package net.anubis.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
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
final class ColorGradingPass {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final RenderPipeline PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(class_2960.method_60655("anubis", "pipeline/color_grading"))
      .withVertexShader("core/screenquad")
      .withFragmentShader(class_2960.method_60655("anubis", "core/color_grading"))
      .withSampler("InSampler")
      .withUniform("ColorGradingConfig", class_10789.field_60031)
      .withoutBlend()
      .withColorWrite(true, false)
      .withDepthWrite(false)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withVertexFormat(class_290.field_60033, class_5596.field_27379)
      .build();
   private static final Supplier<String> PASS_LABEL = () -> "Anubis color grading";
   private static final Supplier<String> CONFIG_LABEL = () -> "Anubis color grading config";
   private static final int CONFIG_SIZE = 32;
   private static class_6367 copy;
   private static GpuBuffer config;
   private static GpuBufferSlice configSlice;
   private static ByteBuffer configData;
   private static boolean invalidShaderLogged;

   private ColorGradingPass() {
   }

   static void apply(boolean worldRendered, float vibrance, float contrast, float exposure, float warmth, float vignette) {
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
         if (copy == null) {
            allocate(main.field_1482, main.field_1481);
         } else if (copy.field_1482 != main.field_1482 || copy.field_1481 != main.field_1481) {
            copy.method_1234(main.field_1482, main.field_1481);
         }

         GpuDevice device = RenderSystem.getDevice();
         if (shaderReady(device)) {
            CommandEncoder encoder = device.createCommandEncoder();
            encoder.copyTextureToTexture(mainColor, copy.method_30277(), 0, 0, 0, 0, 0, main.field_1482, main.field_1481);
            configData.putFloat(0, vibrance);
            configData.putFloat(4, contrast);
            configData.putFloat(8, exposure);
            configData.putFloat(12, warmth);
            configData.putFloat(16, vignette);
            encoder.writeToBuffer(configSlice, configData);
            RenderPass pass = encoder.createRenderPass(PASS_LABEL, main.method_71639(), OptionalInt.empty());

            try {
               pass.setPipeline(PIPELINE);
               pass.setUniform("ColorGradingConfig", configSlice);
               pass.bindTexture("InSampler", copy.method_71639(), RenderSystem.getSamplerCache().method_75294(FilterMode.NEAREST));
               pass.draw(0, 3);
            } catch (Throwable var15) {
               if (pass != null) {
                  try {
                     pass.close();
                  } catch (Throwable var14) {
                     var15.addSuppressed(var14);
                  }
               }

               throw var15;
            }

            if (pass != null) {
               pass.close();
            }
         }
      }
   }

   private static boolean shaderReady(GpuDevice device) {
      if (device.precompilePipeline(PIPELINE).isValid()) {
         invalidShaderLogged = false;
         return true;
      }

      if (!invalidShaderLogged) {
         invalidShaderLogged = true;
         LOGGER.warn("Color Grading shader {} did not compile; the picture is left as it is", PIPELINE.getFragmentShader());
      }

      return false;
   }

   private static void allocate(int width, int height) {
      copy = new class_6367("Anubis color grading copy", width, height, false);
      config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 32L);
      configSlice = config.slice();
      configData = BufferUtils.createByteBuffer(32);
   }

   static void release() {
      if (copy != null) {
         copy.method_1238();
         copy = null;
      }

      if (config != null) {
         config.close();
         config = null;
      }

      configSlice = null;
      configData = null;
   }
}
