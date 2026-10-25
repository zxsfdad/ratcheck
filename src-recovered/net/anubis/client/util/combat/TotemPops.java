package net.anubis.client.util.combat;

import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1309;
import net.minecraft.class_2596;
import net.minecraft.class_2663;
import net.minecraft.class_310;
import net.minecraft.class_638;
import net.minecraft.class_8042;

@Environment(EnvType.CLIENT)
public final class TotemPops {
   private TotemPops() {
   }

   public static void scan(class_2596<?> packet, Consumer<class_1309> sink) {
      if (packet instanceof class_2663 event) {
         check(event, sink);
      } else if (packet instanceof class_8042 bundle) {
         for (class_2596<?> inner : bundle.method_48324()) {
            if (inner instanceof class_2663 event) {
               check(event, sink);
            }
         }
      }
   }

   private static void check(class_2663 event, Consumer<class_1309> sink) {
      if (event.method_11470() == 35) {
         class_638 level = class_310.method_1551().field_1687;
         if (level != null) {
            if (event.method_11469(level) instanceof class_1309 living && !living.method_31481()) {
               sink.accept(living);
            }
         }
      }
   }
}
