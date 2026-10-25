package net.anubis.client.module.impl.render.character;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10055;
import net.minecraft.class_11659;
import net.minecraft.class_1306;
import net.minecraft.class_3883;
import net.minecraft.class_3887;
import net.minecraft.class_4587;
import net.minecraft.class_591;
import net.minecraft.class_630;

@Environment(EnvType.CLIENT)
public final class CharacterLayer extends class_3887<class_10055, class_591> {
   public CharacterLayer(class_3883<class_10055, class_591> parent) {
      super(parent);
   }

   public void submit(class_4587 poseStack, class_11659 collector, int light, class_10055 state, float yRot, float xRot) {
      WornCharacter worn = CharacterModule.worn(state);
      if (worn != null && (!state.field_53333 || !state.field_53461)) {
         CharacterRenderer.submit(worn, (class_591)this.method_17165(), poseStack, collector, light, state);
      }
   }

   public static boolean firstPersonHand(class_591 model, class_4587 poseStack, class_11659 collector, int light, class_630 arm) {
      return CharacterRenderer.firstPersonHand(model, poseStack, collector, light, arm);
   }

   public static boolean translateToHand(class_591 model, class_10055 state, class_1306 arm, class_4587 poseStack) {
      return CharacterRenderer.translateToHand(model, state, arm, poseStack);
   }
}
