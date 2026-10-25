package net.anubis.client.util.pvp;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_3532;

@Environment(EnvType.CLIENT)
public final class TargetingUtils {
   private TargetingUtils() {
   }

   public static class_243 closestPoint(class_243 point, class_238 box) {
      return new class_243(
         class_3532.method_15350(point.field_1352, box.field_1323, box.field_1320),
         class_3532.method_15350(point.field_1351, box.field_1322, box.field_1325),
         class_3532.method_15350(point.field_1350, box.field_1321, box.field_1324)
      );
   }
}
