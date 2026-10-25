package net.anubis.client.gui;

import net.anubis.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_437;

@Environment(EnvType.CLIENT)
public final class HudEditorScreen extends class_437 {
   public HudEditorScreen() {
      super(class_2561.method_43470("ANUBIS HUD Layout"));
   }

   public void method_25432() {
      super.method_25432();
      HudLayoutStore.getInstance().save();
   }

   public void method_25419() {
      HudLayoutStore.getInstance().save();
      class_310.method_1551().method_1507(null);
   }

   public void method_25420(class_332 guiGraphics, int mouseX, int mouseY, float partialTick) {
   }

   public void method_25394(class_332 graphics, int mouseX, int mouseY, float tickDelta) {
      ImGuiManager.getInstance().requestCursor(graphics);
   }

   public boolean method_25421() {
      return false;
   }
}
