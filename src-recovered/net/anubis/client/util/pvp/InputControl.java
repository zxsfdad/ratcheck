package net.anubis.client.util.pvp;

import net.anubis.client.module.impl.movement.SprintModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10185;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class InputControl {
   private static boolean dropSprint;
   private static boolean releaseForward;
   private static boolean backward;
   private static boolean jump;
   private static boolean idle;

   private InputControl() {
   }

   public static void dropSprint() {
      dropSprint = true;
   }

   public static void releaseForward(boolean backward) {
      releaseForward = true;
      InputControl.backward |= backward;
   }

   public static void jump() {
      jump = true;
   }

   public static void idle() {
      idle = true;
   }

   public static class_10185 shape(class_746 player, class_10185 keys) {
      boolean forward = keys.comp_3159();
      boolean back = keys.comp_3160();
      boolean left = keys.comp_3161();
      boolean right = keys.comp_3162();
      boolean up = keys.comp_3163();
      boolean sprint = keys.comp_3165() || SprintModule.holding();
      if (idle) {
         sprint = false;
         up = false;
         right = false;
         left = false;
         back = false;
         forward = false;
      }

      if (releaseForward && !idle) {
         forward = false;
         back |= backward;
      }

      if (jump && !idle) {
         up = true;
      }

      if (dropSprint || idle) {
         sprint = false;
         player.method_5728(false);
      }

      idle = false;
      jump = false;
      backward = false;
      releaseForward = false;
      dropSprint = false;
      return forward == keys.comp_3159()
            && back == keys.comp_3160()
            && left == keys.comp_3161()
            && right == keys.comp_3162()
            && up == keys.comp_3163()
            && sprint == keys.comp_3165()
         ? keys
         : new class_10185(forward, back, left, right, up, keys.comp_3164(), sprint);
   }
}
