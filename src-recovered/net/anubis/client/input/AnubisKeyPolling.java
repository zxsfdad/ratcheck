package net.anubis.client.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1041;
import net.minecraft.class_3675;

@Environment(EnvType.CLIENT)
public final class AnubisKeyPolling {
   private AnubisKeyPolling() {
   }

   public static boolean isKeyDown(class_1041 window, int key) {
      return class_3675.method_15987(window, key);
   }
}
