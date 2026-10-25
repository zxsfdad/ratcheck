package net.anubis.client.module.impl.render;

import com.example.client.mixin.AccessorHandItemLayer;
import com.example.client.mixin.AccessorHandItemRenderState;
import com.example.client.mixin.AccessorSkinChangeEntityRenderDispatcher;
import java.util.function.Consumer;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.misc.SkinChangeModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1007;
import net.minecraft.class_10444;
import net.minecraft.class_11659;
import net.minecraft.class_1309;
import net.minecraft.class_1664;
import net.minecraft.class_1747;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_742;
import net.minecraft.class_746;
import net.minecraft.class_7833;
import net.minecraft.class_7920;
import net.minecraft.class_804;
import net.minecraft.class_811;
import net.minecraft.class_8685;
import net.minecraft.class_898;
import net.minecraft.class_4587.class_4665;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
final class FirstPersonArm {
   static final Vector3f FOREARM = new Vector3f(0.75F, -0.45F, 0.5F).normalize();
   static final float ELBOW = 0.6F;
   private static final Vector3f ARM_UP = new Vector3f(0.0F, 1.0F, 0.0F);
   private static final float ARM_FIST = 0.5F;
   private static final float ARM_SHOULDER = -0.625F;
   private static final float SHOULDER_STRETCH = 3.0F;
   private static final float HANDLE_INSET = 0.1875F;
   private static final float BLOCK_BELOW = 0.17F;
   private static final Vector3f BLOCK_SHIFT = new Vector3f(0.09F, 0.0F, 0.06F);
   private static final class_10444 MODEL = new class_10444();

   private FirstPersonArm() {
   }

   static FirstPersonArm.Arm arm() {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (player != null && !player.method_5767()) {
         class_898 dispatcher = mc.method_1561();
         class_1007<class_742> renderer = dispatcher.method_74405(player);
         class_8685 skin = player.method_52814();
         SkinChangeModule change = ModuleRegistry.get(SkinChangeModule.class);
         if (change != null && change.isEnabled()) {
            class_8685 swapped = change.skinFor(player.method_5628(), skin);
            if (swapped != skin) {
               class_1007<class_742> byModel = ((AccessorSkinChangeEntityRenderDispatcher)dispatcher)
                  .anubis$skinChangePlayerRenderers()
                  .get(swapped.comp_1629());
               if (byModel != null) {
                  renderer = byModel;
               }

               skin = swapped;
            }
         }

         return new FirstPersonArm.Arm(
            renderer,
            skin.comp_1626().comp_3627(),
            player.method_74091(class_1664.field_7570),
            player.method_74091(class_1664.field_7568),
            skin.comp_1629() == class_7920.field_41122
         );
      } else {
         return null;
      }
   }

   static void hold(
      class_4587 pose,
      FirstPersonArm.Arm arm,
      class_1799 stack,
      class_811 context,
      class_1309 holder,
      class_11659 collector,
      int light,
      float dx,
      float dy,
      float dz
   ) {
      Vector3f grip = grip(stack, context, holder);
      if (grip != null) {
         Vector3f elbow = new Vector3f(grip).fma(0.6F, FOREARM);
         grip.add(dx, dy, dz);
         boolean left = context.method_67675();
         pose.method_22903();
         if (left) {
            pose.method_22905(-1.0F, 1.0F, 1.0F);
         }

         place(pose, grip, elbow, arm.slim(), posed -> arm.draw(posed, left, collector, light));
         pose.method_22909();
      }
   }

   static Vector3f grip(class_1799 stack, class_811 context, class_1309 holder) {
      if (!stack.method_7960()
         && !stack.method_31574(class_1802.field_8102)
         && !stack.method_31574(class_1802.field_8399)
         && !stack.method_31574(class_1802.field_8255)
         && !stack.method_31574(class_1802.field_8547)
         && !stack.method_31574(class_1802.field_27070)) {
         class_310.method_1551().method_65386().method_65598(MODEL, stack, context, holder.method_73183(), holder, holder.method_5628() + context.ordinal());
         AccessorHandItemRenderState state = (AccessorHandItemRenderState)MODEL;
         if (state.anubis$handLayerCount() <= 0) {
            return null;
         }

         AccessorHandItemLayer layer = (AccessorHandItemLayer)state.anubis$handLayers()[0];
         class_804 transform = layer.anubis$handTransform();
         class_4665 display = new class_4665();
         if (transform != null) {
            transform.method_23075(context.method_67675(), display);
         }

         Matrix4f m = display.method_23761();
         if (!MODEL.method_65608()) {
            float[] handle = handle(layer.anubis$handExtents() != null ? layer.anubis$handExtents().get() : null);
            Vector3f grip = m.transformPosition(handle[0] - 0.5F, handle[1] - 0.5F, 0.0F, new Vector3f());
            if (context.method_67675()) {
               grip.x = -grip.x;
            }

            return grip;
         } else {
            if (!(stack.method_7909() instanceof class_1747)) {
               return null;
            }

            Vector3f center = m.transformPosition(0.0F, 0.0F, 0.0F, new Vector3f());
            if (context.method_67675()) {
               center.x = -center.x;
            }

            return blockGrip(center, transform != null ? transform.comp_3749().y() : 1.0F);
         }
      } else {
         return null;
      }
   }

   static Vector3f blockGrip(Vector3f center, float scale) {
      return new Vector3f(center).add(BLOCK_SHIFT).sub(0.0F, 0.5F * scale + 0.17F, 0.0F);
   }

   static float[] handle(Vector3fc[] extents) {
      float lowest = Float.MAX_VALUE;
      if (extents != null) {
         for (Vector3fc p : extents) {
            if (!squareCorner(p)) {
               lowest = Math.min(lowest, p.x() + p.y());
            }
         }
      }

      if (lowest == Float.MAX_VALUE) {
         return new float[]{0.1875F, 0.1875F};
      }

      float x = 0.0F;
      float y = 0.0F;
      int tied = 0;

      for (Vector3fc p : extents) {
         if (!squareCorner(p) && !(p.x() + p.y() > lowest + 1.0E-4F)) {
            x += p.x();
            y += p.y();
            tied++;
         }
      }

      return new float[]{x / tied + 0.1875F, y / tied + 0.1875F};
   }

   private static boolean squareCorner(Vector3fc p) {
      return (p.x() < 1.0E-4F || p.x() > 0.9999F) && (p.y() < 1.0E-4F || p.y() > 0.9999F);
   }

   static void place(class_4587 pose, Vector3f hand, Vector3f elbow, boolean slim, Consumer<class_4587> draw) {
      Vector3f along = new Vector3f(hand).sub(elbow).normalize();
      Vector3f front = new Vector3f(ARM_UP).fma(-ARM_UP.dot(along), along).normalize();
      Vector3f side = new Vector3f(along).cross(front);
      pose.method_22903();
      pose.method_46416(hand.x, hand.y, hand.z);
      pose.method_22907(new Quaternionf().setFromNormalized(new Matrix3f(side, along, front)));
      drawPart(pose, slim, draw);
      pose.method_46416(0.0F, -0.625F, 0.0F);
      pose.method_22905(-1.0F, -3.0F, 1.0F);
      pose.method_46416(0.0F, 0.625F, 0.0F);
      drawPart(pose, slim, draw);
      pose.method_22909();
   }

   private static void drawPart(class_4587 pose, boolean slim, Consumer<class_4587> draw) {
      pose.method_22903();
      pose.method_46416(slim ? 0.03125F : 0.0625F, -0.5F, 0.0F);
      pose.method_22907(class_7833.field_40718.rotation(-0.1F));
      pose.method_46416(0.3125F, -0.125F, 0.0F);
      draw.accept(pose);
      pose.method_22909();
   }

   static void shoulder(class_4587 pose, boolean right, boolean slim) {
      float pivotX = right ? -0.3125F : 0.3125F;
      float pivotY = 0.125F;
      float tilt = right ? 0.1F : -0.1F;
      float middle = (right ? -1.0F : 1.0F) * (slim ? 0.5F : 1.0F) / 16.0F;
      float top = -0.125F;
      pose.method_46416(pivotX, pivotY, 0.0F);
      pose.method_22907(class_7833.field_40718.rotation(tilt));
      pose.method_46416(middle, top, 0.0F);
      pose.method_22905(-1.0F, -3.0F, 1.0F);
      pose.method_46416(-middle, -top, 0.0F);
      pose.method_22907(class_7833.field_40718.rotation(-tilt));
      pose.method_46416(-pivotX, -pivotY, 0.0F);
   }

   @Environment(EnvType.CLIENT)
   record Arm(class_1007<class_742> renderer, class_2960 texture, boolean rightSleeve, boolean leftSleeve, boolean slim) {
      void draw(class_4587 pose, boolean left, class_11659 collector, int light) {
         if (left) {
            pose.method_22905(-1.0F, 1.0F, 1.0F);
            this.renderer.method_4221(pose, collector, light, this.texture, this.leftSleeve);
         } else {
            this.renderer.method_4220(pose, collector, light, this.texture, this.rightSleeve);
         }
      }
   }
}
