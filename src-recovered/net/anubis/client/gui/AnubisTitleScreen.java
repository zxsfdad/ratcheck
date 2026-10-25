package net.anubis.client.gui;

import net.anubis.client.config.ThemeStore;
import net.anubis.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_332;
import net.minecraft.class_442;

@Environment(EnvType.CLIENT)
public final class AnubisTitleScreen extends class_442 {
   private final MainMenuRenderer menu = new MainMenuRenderer(this);

   public static boolean enabled() {
      return ThemeStore.get().customMainMenu.get();
   }

   protected void method_25426() {
      ImGuiManager.getInstance().activate(this.menu::render);
   }

   public void method_25393() {
   }

   public boolean method_25404(class_11908 event) {
      return this.menu.keyPressed(event.comp_4795()) || super.method_25404(event);
   }

   public boolean method_25402(class_11909 event, boolean doubleClick) {
      return false;
   }

   public void method_25432() {
      super.method_25432();
      ImGuiManager.getInstance().deactivate();
   }

   public void method_25420(class_332 graphics, int mouseX, int mouseY, float partialTick) {
   }

   public void method_25394(class_332 graphics, int mouseX, int mouseY, float partialTick) {
      ImGuiManager.getInstance().requestCursor(graphics);
      graphics.method_73199();
   }

   public boolean method_25422() {
      return false;
   }

   public boolean method_25421() {
      return false;
   }
}
