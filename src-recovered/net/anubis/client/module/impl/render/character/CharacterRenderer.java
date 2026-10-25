package net.anubis.client.module.impl.render.character;

import net.anubis.client.module.impl.render.character.kit.Bone;
import net.anubis.client.module.impl.render.character.kit.CharacterModel;
import net.anubis.client.module.impl.render.character.kit.Lights;
import net.anubis.client.module.impl.render.character.kit.PartMesh;
import net.anubis.client.module.impl.render.character.kit.Surface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10055;
import net.minecraft.class_11659;
import net.minecraft.class_1306;
import net.minecraft.class_3532;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4608;
import net.minecraft.class_5603;
import net.minecraft.class_591;
import net.minecraft.class_630;
import net.minecraft.class_922;
import net.minecraft.class_4587.class_4665;
import net.minecraft.class_572.class_573;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
final class CharacterRenderer {
   private static final int GHOST_ALPHA = 38;
   private static final Bone[] BONES = Bone.values();
   private static final Quaternionf ROTATION = new Quaternionf();
   private static final CharacterRotationTable LEVELS = new CharacterRotationTable();
   private static final float[] PITCH = new float[BONES.length];
   private static final float[] JOINT = new float[BONES.length];
   private static final CharacterRotationTable JOINT_LEVELS = new CharacterRotationTable();
   private static float[] scratch = new float[24576];

   private CharacterRenderer() {
   }

   static void submit(WornCharacter worn, class_591 pm, class_4587 stack, class_11659 collector, int light, class_10055 state) {
      CharacterModel model = worn.model;
      PartMesh[] parts = model.parts;
      boolean ghost = state.field_53333;
      int overlay = class_922.method_23622(state, 0.0F);
      float[] pitch = pitches(state, model, pm);
      float[] joint = joints(state, model);
      float lookPitch = pm.field_3398.field_3654;
      float lookYaw = -pm.field_3398.field_3675;
      float[] angles = CharacterMotion.update(state, model, pm, pitch);
      stack.method_22903();
      pm.method_63512().method_22703(stack);
      lift(stack, state, model);

      for (int i = 0; i < parts.length; i++) {
         PartMesh part = parts[i];
         if (part.base != null || part.outline != null || part.decals != null) {
            boolean bends = part.bends() && angles != null;
            float bendPitch = bends ? angles[part.motionSlot * 3] : 0.0F;
            float yaw = bends ? angles[part.motionSlot * 3 + 1] : 0.0F;
            float roll = bends ? angles[part.motionSlot * 3 + 2] : 0.0F;
            float bent = 0.0F;
            float turn = 0.0F;
            if (part.neck) {
               bent = look(part.lookPitch * lookPitch, part.lookLimit);
               turn = look(part.lookYaw * lookYaw, part.lookLimit);
            } else if (part.jointed) {
               bent = joint[part.bone.ordinal()];
            }

            stack.method_22903();
            chain(stack, pm, model, i, angles, !bends, null, pitch);
            submitPart(worn, part, stack, collector, light, overlay, ghost, bends, bendPitch, yaw, roll, bent, turn);
            stack.method_22909();
         }
      }

      stack.method_22909();
   }

   static boolean firstPersonHand(class_591 pm, class_4587 stack, class_11659 collector, int light, class_630 arm) {
      WornCharacter worn = CharacterModule.firstPerson();
      if (worn == null) {
         return false;
      }

      Bone bone = arm == pm.field_3401 ? Bone.RIGHT_ARM : Bone.LEFT_ARM;
      CharacterModel model = worn.model;
      PartMesh main = model.part(bone);
      if (main != null && main.base != null) {
         arm.method_41923();
         arm.field_3674 = bone == Bone.RIGHT_ARM ? 0.1F : -0.1F;
         PartMesh[] parts = model.parts;

         for (int i = 0; i < parts.length; i++) {
            PartMesh part = parts[i];
            if (part.bone == bone && (part.base != null || part.outline != null || part.decals != null)) {
               stack.method_22903();
               chain(stack, pm, model, i, null, true, arm, null);
               submitPart(worn, part, stack, collector, light, class_4608.field_21444, false, false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
               stack.method_22909();
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static void submitPart(
      WornCharacter worn,
      PartMesh part,
      class_4587 stack,
      class_11659 collector,
      int light,
      int overlay,
      boolean ghost,
      boolean bends,
      float pitch,
      float yaw,
      float roll,
      float bent,
      float turn
   ) {
      if (ghost) {
         if (part.base != null) {
            collector.method_73529(1)
               .method_73483(stack, worn.ghost, (pose, out) -> emit(pose, out, part.base, light, overlay, 38, bends, pitch, yaw, roll, part, bent, turn));
         }
      } else {
         if (part.base != null || part.outline != null) {
            collector.method_73483(stack, worn.solid, (pose, out) -> {
               if (part.base != null) {
                  emit(pose, out, part.base, light, overlay, 255, bends, pitch, yaw, roll, part, bent, turn);
               }

               if (part.outline != null) {
                  emit(pose, out, part.outline, light, overlay, 255, bends, pitch, yaw, roll, part, bent, turn);
               }
            });
         }

         if (part.decals != null) {
            collector.method_73529(1)
               .method_73483(stack, worn.decal, (pose, out) -> emit(pose, out, part.decals, light, overlay, 255, bends, pitch, yaw, roll, part, bent, turn));
         }
      }
   }

   static boolean translateToHand(class_591 pm, class_10055 state, class_1306 arm, class_4587 stack) {
      WornCharacter worn = CharacterModule.worn(state);
      if (worn == null) {
         return false;
      }

      CharacterModel model = worn.model;
      Bone bone = arm == class_1306.field_6183 ? Bone.RIGHT_ARM : Bone.LEFT_ARM;
      PartMesh part = model.part(bone);
      if (part == null) {
         return false;
      }

      float s = model.scale;
      class_630 vp = vanilla(pm, bone);
      pm.method_63512().method_22703(stack);
      lift(stack, state, model);
      pivot(stack, vp, part, s, pitch(state, part, vp.field_3654));
      float bent = part.jointed ? CharacterJoints.angle(state, bone, walk(state, bone) * weight(state, bone)) : 0.0F;
      if (bent != 0.0F) {
         float jy = part.jointY * s / 16.0F;
         float jz = part.jointZ * s / 16.0F;
         stack.method_46416(0.0F, -jy, -jz);
         stack.method_22907(ROTATION.rotationX(bent));
         stack.method_46416(0.0F, jy, jz);
      }

      stack.method_46416((part.gripX - part.pivotX) * s / 16.0F, -(part.gripY - part.pivotY) * s / 16.0F, -(part.gripZ - part.pivotZ) * s / 16.0F);
      stack.method_22905(s, s, s);
      stack.method_46416((arm == class_1306.field_6183 ? 1.0F : -1.0F) / 16.0F, -0.625F, 0.125F);
      return true;
   }

   private static float[] pitches(class_10055 state, CharacterModel model, class_591 pm) {
      float[] out = PITCH;

      for (Bone bone : BONES) {
         float xRot = vanilla(pm, bone).field_3654;
         PartMesh part = !bone.arm() && !bone.leg() ? null : model.part(bone);
         out[bone.ordinal()] = part == null ? xRot : pitch(state, part, xRot);
      }

      return out;
   }

   private static float pitch(class_10055 state, PartMesh part, float xRot) {
      float out = part.swingPitch(xRot, walk(state, part.bone), weight(state, part.bone));
      if (part.jointed && part.bone.leg() && state.field_53410 && !state.field_53413) {
         out -= 0.5F * (1.0F - state.field_53403);
      }

      return out;
   }

   private static float[] joints(class_10055 state, CharacterModel model) {
      float[] out = JOINT;

      for (Bone bone : BONES) {
         PartMesh part = !bone.arm() && !bone.leg() ? null : model.part(bone);
         out[bone.ordinal()] = part != null && part.jointed ? CharacterJoints.angle(state, bone, walk(state, bone) * weight(state, bone)) : 0.0F;
      }

      return out;
   }

   private static float look(float angle, float limitDegrees) {
      float max = limitDegrees * (float) (Math.PI / 180.0);
      return class_3532.method_15363(angle, -max, max);
   }

   private static float walk(class_10055 state, Bone bone) {
      float phase = state.field_53450 * 0.6662F;
      float speed = state.field_53451 / state.field_53405;

      return switch (bone) {
         case RIGHT_LEG -> class_3532.method_15362(phase) * 1.4F * speed;
         case LEFT_LEG -> class_3532.method_15362(phase + (float) Math.PI) * 1.4F * speed;
         case RIGHT_ARM -> class_3532.method_15362(phase + (float) Math.PI) * speed;
         case LEFT_ARM -> class_3532.method_15362(phase) * speed;
         default -> 0.0F;
      };
   }

   private static float weight(class_10055 state, Bone bone) {
      if (bone.leg()) {
         return state.field_53413 ? 0.0F : 1.0F - state.field_53403;
      }

      if (!bone.arm()) {
         return 0.0F;
      }

      class_573 pose = bone == Bone.RIGHT_ARM ? state.field_55304 : state.field_55306;
      class_573 other = bone == Bone.RIGHT_ARM ? state.field_55306 : state.field_55304;
      if (other.method_30156()) {
         return 0.0F;
      }

      float share = switch (pose) {
         case field_3409 -> 1.0F;
         case field_3410, field_3406, field_42877, field_63542 -> 0.5F;
         default -> 0.0F;
      };
      return state.field_53414 ? share : share * (1.0F - state.field_53403);
   }

   private static void lift(class_4587 stack, class_10055 state, CharacterModel model) {
      if (state.field_53413) {
         float rise = 12.0F - model.scale * model.hipY;
         if (rise != 0.0F) {
            stack.method_46416(0.0F, -rise / 16.0F, 0.0F);
         }
      }
   }

   private static void chain(class_4587 stack, class_591 pm, CharacterModel model, int index, float[] angles, boolean rotate, class_630 hand, float[] pitch) {
      PartMesh part = model.parts[index];
      float s = model.scale;
      if (!part.extra) {
         if (hand == null) {
            class_630 vp = vanilla(pm, part.bone);
            pivot(stack, vp, part, s, pitch == null ? vp.field_3654 : pitch[part.bone.ordinal()]);
         } else {
            stack.method_46416(hand.field_3657 / 16.0F, hand.field_3656 / 16.0F, hand.field_3655 / 16.0F);
            stack.method_22907(ROTATION.rotationZYX(hand.field_3674, hand.field_3675, hand.field_3654));
            float side = part.bone == Bone.RIGHT_ARM ? -1.0F : 1.0F;
            stack.method_46416(
               (side - (part.gripX - part.pivotX) * s) / 16.0F,
               (10.0F + (part.gripY - part.pivotY) * s) / 16.0F,
               (-2.0F + (part.gripZ - part.pivotZ) * s) / 16.0F
            );
         }

         float k = s / 16.0F;
         stack.method_22905(k, -k, -k);
      } else {
         chain(stack, pm, model, part.parent, angles, true, hand, pitch);
         PartMesh parent = model.parts[part.parent];
         stack.method_46416(part.pivotX - parent.pivotX, part.pivotY - parent.pivotY, part.pivotZ - parent.pivotZ);
         if (rotate && angles != null && part.motionSlot >= 0) {
            int o = part.motionSlot * 3;
            if (angles[o] != 0.0F || angles[o + 1] != 0.0F || angles[o + 2] != 0.0F) {
               stack.method_22907(ROTATION.rotationZYX(angles[o + 2], angles[o + 1], angles[o]));
            }
         }
      }
   }

   private static void pivot(class_4587 stack, class_630 vp, PartMesh part, float s, float xRot) {
      class_5603 rest = vp.method_41921();
      stack.method_46416(
         (s * part.pivotX + (vp.field_3657 - rest.comp_2997()) * s) / 16.0F,
         (24.0F - s * part.pivotY + (vp.field_3656 - rest.comp_2998()) * s) / 16.0F,
         (-s * part.pivotZ + (vp.field_3655 - rest.comp_2999()) * s) / 16.0F
      );
      if (xRot != 0.0F || vp.field_3675 != 0.0F || vp.field_3674 != 0.0F) {
         stack.method_22907(ROTATION.rotationZYX(vp.field_3674, vp.field_3675, xRot));
      }

      if (vp.field_37938 != 1.0F || vp.field_37939 != 1.0F || vp.field_37940 != 1.0F) {
         stack.method_22905(vp.field_37938, vp.field_37939, vp.field_37940);
      }
   }

   private static class_630 vanilla(class_591 pm, Bone bone) {
      return switch (bone) {
         case RIGHT_LEG -> pm.field_3392;
         case LEFT_LEG -> pm.field_3397;
         case RIGHT_ARM -> pm.field_3401;
         case LEFT_ARM -> pm.field_27433;
         case HEAD -> pm.field_3398;
         case BODY -> pm.field_3391;
      };
   }

   private static void emit(
      class_4665 pose,
      class_4588 out,
      Surface s,
      int light,
      int overlay,
      int alpha,
      boolean bends,
      float pitch,
      float yaw,
      float roll,
      PartMesh part,
      float bent,
      float turn
   ) {
      int n = s.vertexCount();
      if (scratch.length < n * 6) {
         scratch = new float[Math.max(n * 6, scratch.length * 2)];
      }

      float[] buf = scratch;
      Matrix4f m = pose.method_23761();
      Matrix3f nm = pose.method_23762();
      float m00 = m.m00();
      float m01 = m.m10();
      float m02 = m.m20();
      float m03 = m.m30();
      float m10 = m.m01();
      float m11 = m.m11();
      float m12 = m.m21();
      float m13 = m.m31();
      float m20 = m.m02();
      float m21 = m.m12();
      float m22 = m.m22();
      float m23 = m.m32();
      float n00 = nm.m00();
      float n01 = nm.m10();
      float n02 = nm.m20();
      float n10 = nm.m01();
      float n11 = nm.m11();
      float n12 = nm.m21();
      float n20 = nm.m02();
      float n21 = nm.m12();
      float n22 = nm.m22();
      float[] pos = s.pos;
      float[] nrm = s.nrm;
      float[] weight = bends ? s.weight : null;
      float[] flat = s.flat;
      Lights lights = flat != null ? CharacterLighting.current() : null;
      if (weight != null) {
         LEVELS.update(pitch, yaw, roll);
      }

      float[] joint = bent == 0.0F && turn == 0.0F ? null : s.joint;
      float jx = part.jointX;
      float jy = part.jointY;
      float jz = part.jointZ;
      if (joint != null) {
         JOINT_LEVELS.update(bent, turn, 0.0F);
      }

      for (int v = 0; v < n; v++) {
         float x = pos[v * 3];
         float y = pos[v * 3 + 1];
         float z = pos[v * 3 + 2];
         float nx = nrm[v * 3];
         float ny = nrm[v * 3 + 1];
         float nz = nrm[v * 3 + 2];
         if (joint != null) {
            int k = Math.round(joint[v] * 16.0F);
            if (k > 0) {
               float[] r = JOINT_LEVELS.values;
               int o = k * 9;
               float dx = x - jx;
               float dy = y - jy;
               float dz = z - jz;
               x = jx + r[o] * dx + r[o + 1] * dy + r[o + 2] * dz;
               y = jy + r[o + 3] * dx + r[o + 4] * dy + r[o + 5] * dz;
               z = jz + r[o + 6] * dx + r[o + 7] * dy + r[o + 8] * dz;
               float rx = r[o] * nx + r[o + 1] * ny + r[o + 2] * nz;
               float ry = r[o + 3] * nx + r[o + 4] * ny + r[o + 5] * nz;
               nz = r[o + 6] * nx + r[o + 7] * ny + r[o + 8] * nz;
               nx = rx;
               ny = ry;
            }
         }

         if (weight != null) {
            int k = Math.round(weight[v] * 16.0F) * 9;
            float[] r = LEVELS.values;
            float bx = r[k] * x + r[k + 1] * y + r[k + 2] * z;
            float by = r[k + 3] * x + r[k + 4] * y + r[k + 5] * z;
            float bz = r[k + 6] * x + r[k + 7] * y + r[k + 8] * z;
            float cx = r[k] * nx + r[k + 1] * ny + r[k + 2] * nz;
            float cy = r[k + 3] * nx + r[k + 4] * ny + r[k + 5] * nz;
            float cz = r[k + 6] * nx + r[k + 7] * ny + r[k + 8] * nz;
            x = bx;
            y = by;
            z = bz;
            nx = cx;
            ny = cy;
            nz = cz;
         }

         int o = v * 6;
         buf[o] = m00 * x + m01 * y + m02 * z + m03;
         buf[o + 1] = m10 * x + m11 * y + m12 * z + m13;
         buf[o + 2] = m20 * x + m21 * y + m22 * z + m23;
         float tx = n00 * nx + n01 * ny + n02 * nz;
         float ty = n10 * nx + n11 * ny + n12 * nz;
         float tz = n20 * nx + n21 * ny + n22 * nz;
         float l = tx * tx + ty * ty + tz * tz;
         if (l > 1.0E-12F && Math.abs(l - 1.0F) > 0.001F) {
            l = 1.0F / (float)Math.sqrt(l);
            tx *= l;
            ty *= l;
            tz *= l;
         }

         buf[o + 3] = tx;
         buf[o + 4] = ty;
         buf[o + 5] = tz;
         if (lights != null && flat[v] > 0.0F) {
            lights.flatten(buf, o + 3, flat[v]);
         }
      }

      int[] quads = s.quads;
      int[] color = s.color;
      float[] uv = s.uv;
      byte[] glow = s.glow;

      for (int q = 0; q < quads.length; q++) {
         int v = quads[q];
         int o = v * 6;
         int c = alpha == 255 ? color[v] : color[v] & 16777215 | alpha << 24;
         int l = glow != null && glow[v] != 0 ? 15728880 : light;
         out.method_23919(buf[o], buf[o + 1], buf[o + 2], c, uv[v * 2], uv[v * 2 + 1], overlay, l, buf[o + 3], buf[o + 4], buf[o + 5]);
      }
   }
}
