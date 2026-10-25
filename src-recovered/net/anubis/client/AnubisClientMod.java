package net.anubis.client;

import net.anubis.client.addon.AddonManager;
import net.anubis.client.config.HudTheme;
import net.anubis.client.config.ModuleConfigStore;
import net.anubis.client.config.ProfileAutoLoad;
import net.anubis.client.config.ThemeStore;
import net.anubis.client.esp.ChunkScanner;
import net.anubis.client.event.impl.PacketAppliedEvent;
import net.anubis.client.event.impl.PacketReceiveNettyEvent;
import net.anubis.client.event.impl.Render2DEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.gui.BorderlessFullscreen;
import net.anubis.client.gui.HudLayoutStore;
import net.anubis.client.gui.HudRenderer;
import net.anubis.client.gui.ImGuiScreen;
import net.anubis.client.gui.LoadingScreens;
import net.anubis.client.gui.StaffGhost;
import net.anubis.client.gui.ToggleToasts;
import net.anubis.client.gui.mainmenu.OfficialBuildBanner;
import net.anubis.client.imgui.ImGuiManager;
import net.anubis.client.input.AnubisKeyPolling;
import net.anubis.client.input.ModuleKeybinds;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.misc.noinvleak.StreamOverlay;
import net.anubis.client.security.OfficialBuild;
import net.anubis.client.util.BlockEntityLeakTracker;
import net.anubis.client.util.CompletionChannel;
import net.anubis.client.util.combat.CombatTarget;
import net.anubis.client.util.netherite.NetheriteChunkTracker;
import net.anubis.client.util.playerbypass.PlayerBypassLightTracker;
import net.anubis.client.util.playerbypass.ProtectedChunkStore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.class_11908;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3414;
import net.minecraft.class_304.class_11900;
import net.minecraft.class_3675.class_307;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class AnubisClientMod implements ClientModInitializer {
   public static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   public static final class_2960 SPAWNER_NOTIFY_SOUND_ID = class_2960.method_60655("anubis", "spawner_notify");
   public static final class_3414 SPAWNER_NOTIFY_SOUND = class_3414.method_47908(SPAWNER_NOTIFY_SOUND_ID);
   public static final class_11900 KEY_CATEGORY = class_11900.method_74698(class_2960.method_60655("anubis", "controls"));
   private static class_304 openGuiKey;
   private final ModuleKeybinds keybinds = new ModuleKeybinds();
   private static final Object LEAK_TRACKER_OWNER = new Object();
   private static final Object PLAYER_BYPASS_TRACKER_OWNER = new Object();
   private static final Object NETHERITE_TRACKER_OWNER = new Object();

   public void onInitializeClient() {
      LOGGER.info("[Anubis] Initialising");
      AddonManager.init();
      ImGuiManager.getInstance().setHudRenderer(HudRenderer.getInstance()::render, HudRenderer.getInstance()::hasWork);
      ImGuiManager.getInstance().setStreamRenderer(StreamOverlay::drawFakes, StreamOverlay::hasFakes);
      ImGuiManager.getInstance().setToastRenderer(ToggleToasts::render, ToggleToasts::hasWork);
      ImGuiManager.getInstance().setOverlayRenderer(LoadingScreens::render, LoadingScreens::hasWork);
      ImGuiManager.getInstance().setAlertRenderer(StaffGhost::render, StaffGhost::hasWork);
      Render2DEvent.TYPE.listen(StaffGhost.class, StaffGhost::renderNote);
      ChunkScanner.init();
      PacketReceiveNettyEvent.TYPE.listen(LEAK_TRACKER_OWNER, event -> BlockEntityLeakTracker.ingest(event.packet));
      CompletionChannel.install();
      PacketAppliedEvent.TYPE.listen(PLAYER_BYPASS_TRACKER_OWNER, event -> PlayerBypassLightTracker.onPacketApplied(event.packet));
      TickEvent.TYPE.listen(PLAYER_BYPASS_TRACKER_OWNER, event -> PlayerBypassLightTracker.onTick());
      TickEvent.TYPE.listen(BorderlessFullscreen.class, event -> BorderlessFullscreen.tick());
      CombatTarget.install();
      PacketAppliedEvent.TYPE.listen(NETHERITE_TRACKER_OWNER, event -> NetheriteChunkTracker.onPacketApplied(event.packet));
      ToggleToasts.install();
      ProfileAutoLoad.install();
      ClientLifecycleEvents.CLIENT_STOPPING.register((ClientStopping)client -> {
         HudLayoutStore.getInstance().save();
         ModuleConfigStore.getInstance().saveAndWait();
         ThemeStore.get().save();
         HudTheme.get().save();
         ProtectedChunkStore.get().flushAndWait();
         StreamOverlay.stop();
      });
      openGuiKey = KeyBindingHelper.registerKeyBinding(new class_304("key.anubis.open_gui", class_307.field_1668, 344, KEY_CATEGORY));
      ModuleConfigStore.getInstance().load();
      OfficialBuild.enforceIfPending();
      OfficialBuildBanner.installVanillaTitleScreen();
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> OfficialBuild.enforceOnFirstTick());
      AddonManager.configLoaded();
      ClientTickEvents.END_CLIENT_TICK
         .register(
            (EndTick)client -> {
               ModuleConfigStore.getInstance().applyPendingEnabled();

               while (openGuiKey.method_1436()) {
                  if (client.field_1755 instanceof ImGuiScreen) {
                     client.method_1507(null);
                  } else {
                     client.method_1507(new ImGuiScreen());
                  }
               }

               this.keybinds
                  .update(ModuleRegistry.all(), AnubisClientMod::isBoundKeyDown, client.field_1755 == null && client.method_1569() && client.field_1724 != null);
               ModuleConfigStore.getInstance().tickAutosave();
            }
         );
   }

   public static boolean isGuiKey(long window, class_11908 event) {
      return openGuiKey != null && window == class_310.method_1551().method_22683().method_4490() && openGuiKey.method_1417(event);
   }

   public static boolean toggleGuiFromKey() {
      class_310 client = class_310.method_1551();
      if (client.field_1755 == null) {
         client.method_1507(new ImGuiScreen());
         return true;
      }

      if (client.field_1755 instanceof ImGuiScreen gui && !gui.capturesKeys()) {
         client.method_1507(null);
      }

      return false;
   }

   public static void dropGuiClicks() {
      while (openGuiKey.method_1436()) {
      }
   }

   private static boolean isBoundKeyDown(int key) {
      class_310 client = class_310.method_1551();
      return key >= 1000
         ? GLFW.glfwGetMouseButton(client.method_22683().method_4490(), key - 1000) == 1
         : AnubisKeyPolling.isKeyDown(client.method_22683(), key);
   }
}
