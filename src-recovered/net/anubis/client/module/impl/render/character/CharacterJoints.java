package net.anubis.client.module.impl.render.character;

import net.anubis.client.module.impl.render.character.kit.Bone;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10055;
import net.minecraft.class_12122;
import net.minecraft.class_1306;
import net.minecraft.class_3532;
import net.minecraft.class_572.class_573;

@Environment(EnvType.CLIENT)
final class CharacterJoints {
   static final float CROUCH_HIP = 0.5F;
   static final float SADDLE_KNEE = 1.35F;
   static final float SEAT_KNEE = 0.3F;
   private static final float CROUCH_KNEE = 0.62F;
   private static final float CROUCH_ELBOW = 0.2F;
   private static final float MAX_KNEE = 1.7F;
   private static final float MAX_ELBOW = 1.6F;

   private CharacterJoints() {
   }

   static float angle(class_10055 state, Bone bone, float swing) {
      float fade = !state.field_53411 && !state.field_53459 ? 1.0F - state.field_53403 * 0.8F : 0.0F;
      if (fade <= 0.0F) {
         return 0.0F;
      } else if (bone.leg()) {
         return knee(state, bone) * fade;
      } else {
         return bone.arm() ? -elbow(state, bone, swing) * fade : 0.0F;
      }
   }

   private static float knee(class_10055 state, Bone bone) {
      if (state.field_53413) {
         return CharacterModule.seat(state);
      }

      float speed = Math.min(1.0F, state.field_53451 / state.field_53405);
      float phase = state.field_53450 * 0.6662F + (bone == Bone.LEFT_LEG ? (float) Math.PI : 0.0F);
      float flex = speed * (0.1F + 0.85F * Math.max(0.0F, class_3532.method_15374(phase)));
      if (state.field_53410) {
         flex += 0.62F;
      }

      return Math.min(1.7F, flex);
   }

   private static float elbow(class_10055 state, Bone bone, float swing) {
      boolean right = bone == Bone.RIGHT_ARM;
      class_573 pose = right ? state.field_55304 : state.field_55306;
      class_573 other = right ? state.field_55306 : state.field_55304;
      if (other.method_30156()) {
         return 0.0F;
      }

      float speed = Math.min(1.0F, state.field_53451 / state.field_53405);

      float flex = switch (pose) {
         case field_3409 -> 0.16F + 0.45F * Math.max(0.0F, -swing) + 0.3F * speed;
         case field_3410 -> 0.3F + 0.2F * speed;
         case field_3406 -> 0.55F;
         case field_42877 -> 0.4F;
         default -> 0.0F;
      };
      if (flex <= 0.0F) {
         return 0.0F;
      }

      if (state.field_53410) {
         flex += 0.2F;
      }

      class_1306 arm = right ? class_1306.field_6183 : class_1306.field_6182;
      if (state.field_63604 > 0.0F && state.field_53408 == arm && state.field_63603 == class_12122.field_63399) {
         flex += 0.6F * class_3532.method_15374(state.field_63604 * (float) Math.PI);
      }

      return Math.min(1.6F, flex);
   }
}
