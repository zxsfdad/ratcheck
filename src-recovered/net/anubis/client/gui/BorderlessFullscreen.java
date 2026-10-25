package net.anubis.client.gui;

import com.sun.jna.Platform;
import net.anubis.client.module.impl.misc.NoInvLeakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1041;
import net.minecraft.class_310;

@Environment(EnvType.CLIENT)
public final class BorderlessFullscreen {
   private BorderlessFullscreen() {
   }

   public static boolean wanted() {
      return Platform.isWindows() && NoInvLeakModule.wantsBorderless();
   }

   public static void tick() {
      class_1041 window = class_310.method_1551().method_22683();
      if (window.method_4498()) {
         BorderlessFullscreen.Switchable switchable = (BorderlessFullscreen.Switchable)window;
         if (wanted() != switchable.anubis$isBorderless()) {
            switchable.anubis$reapplyMode();
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public interface Switchable {
      boolean anubis$isBorderless();

      void anubis$reapplyMode();
   }
}
