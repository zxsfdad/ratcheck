package net.anubis.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1937;
import net.minecraft.class_638;
import net.minecraft.class_7134;

@Environment(EnvType.CLIENT)
public final class LevelUtil {
   private LevelUtil() {
   }

   public static boolean isOverworld(class_638 level) {
      return level.method_27983() == class_1937.field_25179 || level.method_40134().method_40225(class_7134.field_37666);
   }

   public static boolean isLoaded(class_638 level, int chunkX, int chunkZ) {
      return level.method_2935().method_12123(chunkX, chunkZ);
   }
}
