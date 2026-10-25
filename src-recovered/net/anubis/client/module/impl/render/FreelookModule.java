package net.anubis.client.module.impl.render;

import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.input.AnubisKeyPolling;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.misc.FreecamModule;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_5498;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class FreelookModule extends ClientModule {
   private static final String PLAYER = "Player";
   private static final String CAMERA = "Camera";
   private static final float MIN_SENSITIVITY = 0.1F;
   private final ModeSetting mode = this.addSetting("mode", new ModeSetting("Mode", "Player", "Player", "Camera"));
   private final BooleanSetting togglePerspective = this.addSetting("toggle-perspective", new BooleanSetting("Toggle Perspective", true));
   private final NumberSetting sensitivity = this.addSetting("camera-sensitivity", new NumberSetting("Camera Sensitivity", 8.0F, 0.0F, 10.0F).step(0.1F));
   private final BooleanSetting arrows = this.addSetting("arrows-control-opposite", new BooleanSetting("Arrows Control Opposite", true));
   private final NumberSetting arrowSpeed = this.addSetting("arrow-speed", new NumberSetting("Arrow Speed", 4.0F, 0.0F, 10.0F).step(0.5F));
   private float cameraYaw;
   private float cameraPitch;
   private class_5498 previousView;

   public FreelookModule() {
      super("freelook", "Freelook", "Turns the camera and the player separately.", ModuleCategory.RENDER);
      this.sensitivity.visibleWhen(() -> this.mode.is("Camera"));
      this.arrowSpeed.visibleWhen(this.arrows::get);
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      class_310 mc = class_310.method_1551();
      if (mc.field_1724 == null) {
         this.requestDisable();
      } else {
         this.cameraYaw = mc.field_1724.method_36454();
         this.cameraPitch = mc.field_1724.method_36455();
         this.previousView = mc.field_1690.method_31044();
         if (this.previousView != class_5498.field_26665 && this.togglePerspective.get()) {
            mc.field_1690.method_31043(class_5498.field_26665);
         }

         this.on(TickEvent.TYPE, Stage.POST, this::onTick);
      }
   }

   @Override
   protected void onDisable() {
      class_310 mc = class_310.method_1551();
      if (this.previousView != null && mc.field_1690.method_31044() != this.previousView && this.togglePerspective.get()) {
         mc.field_1690.method_31043(this.previousView);
      }

      this.previousView = null;
   }

   private void onTick(TickEvent e) {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (player != null) {
         if (this.arrows.get() && mc.field_1755 == null) {
            float turn = this.arrowSpeed.get();
            float yawTurn = (held(mc, 262) ? turn : 0.0F) - (held(mc, 263) ? turn : 0.0F);
            float pitchTurn = (held(mc, 264) ? turn : 0.0F) - (held(mc, 265) ? turn : 0.0F);
            if (this.mode.is("Player")) {
               this.cameraYaw += yawTurn;
               this.cameraPitch += pitchTurn;
            } else if (yawTurn != 0.0F || pitchTurn != 0.0F) {
               player.method_36456(player.method_36454() + yawTurn);
               player.method_36457(player.method_36455() + pitchTurn);
            }
         }

         this.cameraPitch = class_3532.method_15363(this.cameraPitch, -90.0F, 90.0F);
      }
   }

   private static boolean held(class_310 mc, int key) {
      return AnubisKeyPolling.isKeyDown(mc.method_22683(), key);
   }

   public boolean cameraMode() {
      return this.isEnabled() && this.mode.is("Camera");
   }

   public static boolean throughBlocks() {
      FreelookModule freelook = ModuleRegistry.get(FreelookModule.class);
      return freelook != null && freelook.isEnabled() && FreecamModule.active == null;
   }

   public void turnCamera(double yRot, double xRot) {
      float divisor = Math.max(this.sensitivity.get(), 0.1F);
      this.cameraYaw += (float)(yRot / divisor);
      this.cameraPitch += (float)(xRot / divisor);
      if (Math.abs(this.cameraPitch) > 90.0F) {
         this.cameraPitch = this.cameraPitch > 0.0F ? 90.0F : -90.0F;
      }
   }

   public float cameraYaw() {
      return this.cameraYaw;
   }

   public float cameraPitch() {
      return this.cameraPitch;
   }
}
