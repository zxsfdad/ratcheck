package net.anubis.client.util;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_10366;
import net.minecraft.class_11286;
import net.minecraft.class_12075;
import net.minecraft.class_12160;
import net.minecraft.class_12180;
import net.minecraft.class_12185;
import net.minecraft.class_156;
import net.minecraft.class_310;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_12180.class_12181;
import net.minecraft.class_12185.class_12186;
import net.minecraft.class_4597.class_4598;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public final class ShaderOverlay {
   private static final MethodHandle SHADER_PACK_IN_USE = irisProbe();
   private static final class_12185 COLLECTOR = new class_12185();
   private static class_11286 projectionBuffer;

   private ShaderOverlay() {
   }

   private static MethodHandle irisProbe() {
      if (!FabricLoader.getInstance().isModLoaded("iris")) {
         return null;
      }

      try {
         Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
         Object instance = api.getMethod("getInstance").invoke(null);
         return MethodHandles.publicLookup().findVirtual(api, "isShaderPackInUse", MethodType.methodType(boolean.class)).bindTo(instance);
      } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
         return null;
      }
   }

   public static boolean shaderPackInUse() {
      if (SHADER_PACK_IN_USE == null) {
         return false;
      }

      try {
         return (boolean)SHADER_PACK_IN_USE.invokeExact();
      } catch (Throwable t) {
         return false;
      }
   }

   public static class_12181 capture() {
      if (!shaderPackInUse()) {
         return null;
      }

      if (pending()) {
         COLLECTOR.method_75558();
      }

      return class_12180.method_75554(COLLECTOR);
   }

   public static boolean pending() {
      return !COLLECTOR.method_75560().isEmpty();
   }

   public static void render(class_4184 camera, Matrix4f viewRotation, Matrix4f projection, GpuBufferSlice fog, GpuBufferSlice onTopFog) {
      if (pending()) {
         class_12160 standard = new class_12160();
         class_12160 onTop = new class_12160();
         long now = class_156.method_658();

         for (class_12186 instance : COLLECTOR.method_75558()) {
            instance.method_75563().method_75531(instance.method_75562() ? onTop : standard, instance.method_75857(now));
         }

         if (!standard.method_76659() || !onTop.method_76659()) {
            class_12075 state = new class_12075();
            state.field_63079 = camera.method_19332();
            state.field_63078 = camera.method_71156();
            state.field_63077 = camera.method_19328();
            state.field_63081 = new Quaternionf(camera.method_23767());
            class_310 mc = class_310.method_1551();
            if (projectionBuffer == null) {
               projectionBuffer = new class_11286("anubis overlay");
            }

            class_4598 buffers = mc.method_22940().method_23000();
            class_4587 poseStack = new class_4587();
            Matrix4fStack modelView = RenderSystem.getModelViewStack();
            GpuBufferSlice previousFog = RenderSystem.getShaderFog();
            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(projectionBuffer.method_71123(projection), class_10366.field_54953);
            modelView.pushMatrix();
            modelView.mul(viewRotation);

            try {
               if (!standard.method_76659()) {
                  RenderSystem.setShaderFog(fog);
                  standard.method_75477(poseStack, buffers, state, viewRotation);
                  buffers.method_22993();
               }

               if (!onTop.method_76659()) {
                  RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mc.method_1522().method_30278(), 1.0);
                  RenderSystem.setShaderFog(onTopFog);
                  onTop.method_75477(poseStack, buffers, state, viewRotation);
                  buffers.method_22993();
               }
            } finally {
               modelView.popMatrix();
               RenderSystem.restoreProjectionMatrix();
               RenderSystem.setShaderFog(previousFog);
            }
         }
      }
   }
}
