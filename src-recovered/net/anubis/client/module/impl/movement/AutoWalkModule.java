package net.anubis.client.module.impl.movement;

import net.anubis.client.event.EventPriority;
import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.KeyInputEvent;
import net.anubis.client.event.impl.MouseInputEvent;
import net.anubis.client.event.impl.PlayerMoveEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.input.AnubisKeyPolling;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.misc.FreecamModule;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_11910;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_315;
import net.minecraft.class_3675;
import net.minecraft.class_4076;
import net.minecraft.class_638;
import net.minecraft.class_746;
import net.minecraft.class_3675.class_306;
import net.minecraft.class_3675.class_307;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class AutoWalkModule extends ClientModule {
   private final ModeSetting direction = this.addSetting(
      "simple-direction", new ModeSetting("Simple Direction", "Forwards", "Forwards", "Backwards", "Left", "Right")
   );
   private final BooleanSetting disableOnInput = this.addSetting("disable-on-input", new BooleanSetting("Disable On Input", false));
   private final BooleanSetting disableOnY = this.addSetting("disable-on-y-change", new BooleanSetting("Disable On Y Change", false));
   private final BooleanSetting waitForChunks = this.addSetting("no-unloaded-chunks", new BooleanSetting("No Unloaded Chunks", true));

   public AutoWalkModule() {
      super("auto-walk", "Auto Walk", "Automatically walks forward.", ModuleCategory.MOVEMENT);
      this.direction.onChange(() -> {
         if (this.isEnabled()) {
            releaseKeys();
         }
      });
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      this.on(TickEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onPreTick);
      this.on(KeyInputEvent.TYPE, this::onKey);
      this.on(MouseInputEvent.TYPE, this::onMouseButton);
      this.on(PlayerMoveEvent.TYPE, this::onPlayerMove);
   }

   @Override
   protected void onDisable() {
      releaseKeys();
   }

   private void onPreTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (player != null && mc.field_1687 != null) {
         if (this.disableOnY.get() && player.field_6036 != player.method_23318()) {
            this.requestDisable();
         } else {
            this.keyFor(mc.field_1690).method_23481(true);
         }
      }
   }

   private void onKey(KeyInputEvent event) {
      if (event.action == 1 && this.disableOnInput.get()) {
         if (event.key != -1) {
            class_315 options = class_310.method_1551().field_1690;
            class_11908 key = new class_11908(event.key, event.scancode, event.mods);
            if (options.field_1894.method_1417(key)
               || options.field_1881.method_1417(key)
               || options.field_1913.method_1417(key)
               || options.field_1849.method_1417(key)
               || options.field_1832.method_1417(key)
               || options.field_1903.method_1417(key)) {
               this.onMovementInput();
            }
         }
      }
   }

   private void onMouseButton(MouseInputEvent event) {
      if (event.action == 1 && this.disableOnInput.get()) {
         class_315 options = class_310.method_1551().field_1690;
         class_11909 click = new class_11909(0.0, 0.0, new class_11910(event.button, event.mods));
         if (options.field_1894.method_1433(click)
            || options.field_1881.method_1433(click)
            || options.field_1913.method_1433(click)
            || options.field_1849.method_1433(click)
            || options.field_1832.method_1433(click)
            || options.field_1903.method_1433(click)) {
            this.onMovementInput();
         }
      }
   }

   private void onMovementInput() {
      if (class_310.method_1551().field_1755 == null) {
         this.requestDisable();
      }
   }

   private void onPlayerMove(PlayerMoveEvent event) {
      if (this.waitForChunks.get()) {
         class_310 mc = class_310.method_1551();
         class_746 player = mc.field_1724;
         class_638 level = mc.field_1687;
         if (player != null && level != null) {
            int chunkX = class_4076.method_42615(player.method_23317() + event.x * 2.0);
            int chunkZ = class_4076.method_42615(player.method_23321() + event.z * 2.0);
            if (!level.method_2935().method_12123(chunkX, chunkZ)) {
               event.x = 0.0;
               event.z = 0.0;
            }
         }
      }
   }

   private class_304 keyFor(class_315 options) {
      return switch (this.direction.get()) {
         case "Backwards" -> options.field_1881;
         case "Left" -> options.field_1913;
         case "Right" -> options.field_1849;
         default -> options.field_1894;
      };
   }

   private static void releaseKeys() {
      class_310 mc = class_310.method_1551();
      class_315 options = mc.field_1690;
      if (options != null) {
         boolean inGame = mc.field_1755 == null && !isFreecamActive();
         restore(mc, options.field_1894, inGame);
         restore(mc, options.field_1881, inGame);
         restore(mc, options.field_1913, inGame);
         restore(mc, options.field_1849, inGame);
      }
   }

   private static boolean isFreecamActive() {
      FreecamModule freecam = ModuleRegistry.get(FreecamModule.class);
      return freecam != null && freecam.isEnabled();
   }

   private static void restore(class_310 mc, class_304 mapping, boolean inGame) {
      mapping.method_23481(inGame && isPhysicallyHeld(mc, mapping));
   }

   private static boolean isPhysicallyHeld(class_310 mc, class_304 mapping) {
      if (mapping.method_1415()) {
         return false;
      } else {
         class_306 key = class_3675.method_15981(mapping.method_1428());
         class_307 type = key.method_1442();
         if (type == class_307.field_1668) {
            return AnubisKeyPolling.isKeyDown(mc.method_22683(), key.method_1444());
         } else {
            return type == class_307.field_1672 ? GLFW.glfwGetMouseButton(mc.method_22683().method_4490(), key.method_1444()) == 1 : false;
         }
      }
   }
}
