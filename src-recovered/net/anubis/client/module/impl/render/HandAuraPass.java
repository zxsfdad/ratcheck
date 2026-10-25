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
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.anubis.client.util.ShaderOverlay;
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
final class HandAuraPass {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final RenderPipeline PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(class_2960.method_60655("anubis", "pipeline/hand_aura"))
      .withVertexShader("core/screenquad")
      .withFragmentShader(class_2960.method_60655("anubis", "core/hand_aura"))
      .withSampler("CaptureSampler")
      .withSampler("HaloSampler")
      .withUniform("HandAuraConfig", class_10789.field_60031)
      .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
      .withColorWrite(true, false)
      .withDepthWrite(false)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withVertexFormat(class_290.field_60033, class_5596.field_27379)
      .build();
   private static final Supplier<String> PASS_LABEL = () -> "Anubis hand aura";
   private static final Supplier<String> CONFIG_LABEL = () -> "Anubis hand aura config";
   private static final int CONFIG_SIZE = 48;
   private static class_6367 hand;
   private static AuraBlur blur;
   private static GpuBuffer config;
   private static GpuBufferSlice configSlice;
   private static ByteBuffer configData;
   private static boolean capturing;
   private static boolean invalidShaderLogged;

   private HandAuraPass() {
   }

   static boolean begin() {
      if (!capturing && RenderSystem.outputColorTextureOverride == null && RenderSystem.outputDepthTextureOverride == null) {
         class_310 mc = class_310.method_1551();
         class_276 main = mc.method_1522();
         if (mc.field_1687 != null
            && !mc.field_1773.method_35765()
            && main.method_30277() != null
            && main.field_1482 > 0
            && main.field_1481 > 0
            && !ShaderOverlay.shaderPackInUse()) {
            GpuDevice device = RenderSystem.getDevice();
            if (!shaderReady(device)) {
               return false;
            }

            if (hand == null) {
               allocate(main.field_1482, main.field_1481);
            } else if (hand.field_1482 != main.field_1482 || hand.field_1481 != main.field_1481) {
               hand.method_1234(main.field_1482, main.field_1481);
            }

            device.createCommandEncoder().clearColorAndDepthTextures(hand.method_30277(), 0, hand.method_30278(), 1.0);
            RenderSystem.outputColorTextureOverride = hand.method_71639();
            RenderSystem.outputDepthTextureOverride = hand.method_71640();
            capturing = true;
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   static boolean end() {
      if (!capturing) {
         return false;
      }

      capturing = false;
      RenderSystem.outputColorTextureOverride = null;
      RenderSystem.outputDepthTextureOverride = null;
      return true;
   }

   static void compose(int tintArgb, float aura, float reachPx, float rim, float rimPx, int sheenArgb, float sheen, float phase) {
      class_276 main = class_310.method_1551().method_1522();
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      GpuTextureView halo = aura > 0.0F ? blur.spread(encoder, hand.method_71639(), hand.field_1482, hand.field_1481, reachPx) : hand.method_71639();
      putColor(0, tintArgb, aura);
      putColor(16, sheenArgb, sheen);
      configData.putFloat(32, rim);
      configData.putFloat(36, rimPx);
      configData.putFloat(40, phase);
      configData.putFloat(44, 0.0F);
      encoder.writeToBuffer(configSlice, configData);
      RenderPass pass = encoder.createRenderPass(PASS_LABEL, main.method_71639(), OptionalInt.empty());

      try {
         pass.setPipeline(PIPELINE);
         pass.setUniform("HandAuraConfig", configSlice);
         pass.bindTexture("CaptureSampler", hand.method_71639(), RenderSystem.getSamplerCache().method_75294(FilterMode.LINEAR));
         pass.bindTexture("HaloSampler", halo, RenderSystem.getSamplerCache().method_75294(FilterMode.LINEAR));
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

   private static void putColor(int offset, int argb, float weight) {
      configData.putFloat(offset, (argb >> 16 & 0xFF) / 255.0F);
      configData.putFloat(offset + 4, (argb >> 8 & 0xFF) / 255.0F);
      configData.putFloat(offset + 8, (argb & 0xFF) / 255.0F);
      configData.putFloat(offset + 12, weight);
   }

   private static boolean shaderReady(GpuDevice device) {
      if (device.precompilePipeline(PIPELINE).isValid() && AuraBlur.compiled(device)) {
         invalidShaderLogged = false;
         return true;
      }

      if (!invalidShaderLogged) {
         invalidShaderLogged = true;
         LOGGER.warn("Hand Aura shaders did not compile; the hand is drawn as usual");
      }

      return false;
   }

   private static void allocate(int width, int height) {
      hand = new class_6367("Anubis hand aura", width, height, true);
      blur = new AuraBlur("Anubis hand aura");
      config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 48L);
      configSlice = config.slice();
      configData = BufferUtils.createByteBuffer(48);
   }

   static void release() {
      if (capturing) {
         end();
      }

      if (hand != null) {
         hand.method_1238();
         hand = null;
      }

      if (blur != null) {
         blur.release();
         blur = null;
      }

      if (config != null) {
         config.close();
         config = null;
      }

      configSlice = null;
      configData = null;
   }
}
