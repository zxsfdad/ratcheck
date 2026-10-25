package net.anubis.client.module.impl.misc;

import java.util.Locale;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterTick;
import net.minecraft.class_156;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_412;
import net.minecraft.class_4185;
import net.minecraft.class_419;
import net.minecraft.class_437;
import net.minecraft.class_442;
import net.minecraft.class_500;
import net.minecraft.class_639;
import net.minecraft.class_642;

@Environment(EnvType.CLIENT)
public final class AutoReconnectModule extends ClientModule {
   private static final long HOLD_MS = 15000L;
   private static volatile long holdUntil;
   private final NumberSetting delay = this.addSetting("delay", new NumberSetting("Delay", 5.0F, 3.0F, 60.0F).step(1.0F));
   private class_642 lastServer;
   private class_437 activeScreen;
   private class_4185 reconnectButton;
   private int ticksLeft;
   private boolean heldScreen;

   public static void holdOff() {
      holdUntil = class_156.method_658() + 15000L;
   }

   public AutoReconnectModule() {
      super("auto-reconnect", "Auto Reconnect", "Reconnects to the last server after an unexpected disconnect.", ModuleCategory.MISC);
      TickEvent.TYPE.listen(this, this::trackServer);
      ScreenEvents.AFTER_INIT.register(this::onScreenInit);
   }

   private void trackServer(TickEvent event) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1724 != null && mc.field_1687 != null) {
         class_642 server = mc.method_1542() ? null : mc.method_1558();
         this.lastServer = server != null && !server.method_2994() && !server.method_52811() ? server : null;
      }
   }

   private void onScreenInit(class_310 client, class_437 screen, int scaledWidth, int scaledHeight) {
      if (screen instanceof class_442 || screen instanceof class_500) {
         this.lastServer = null;
      } else if (screen instanceof class_419 && this.lastServer != null) {
         class_642 server = this.lastServer;
         if (screen == this.activeScreen) {
            Screens.getButtons(screen).remove(this.reconnectButton);
         } else {
            this.activeScreen = screen;
            this.ticksLeft = Math.round(this.delay.get() * 20.0F);
            this.heldScreen = class_156.method_658() < holdUntil || isBan(screen);
            holdUntil = 0L;
         }

         boolean auto = this.isEnabled() && !this.heldScreen;
         class_4185 button = class_4185.method_46430(reconnectLabel(auto, this.ticksLeft), btn -> {
            btn.field_22763 = false;
            this.reconnect(client, server);
         }).method_46434(scaledWidth - 150 - 10, scaledHeight - 20 - 10, 150, 20).method_46431();
         this.reconnectButton = button;
         Screens.getButtons(screen).add(button);
         if (auto) {
            ScreenEvents.afterTick(screen).register((AfterTick)s -> {
               this.ticksLeft--;
               if (this.ticksLeft <= 0) {
                  button.field_22763 = false;
                  this.reconnect(client, server);
               } else {
                  button.method_25355(reconnectLabel(true, this.ticksLeft));
               }
            });
         }
      }
   }

   private static boolean isBan(class_437 screen) {
      String reason = screen.method_25435().getString().toLowerCase(Locale.ROOT);
      return reason.contains("banned") || reason.contains("blacklisted");
   }

   private void reconnect(class_310 client, class_642 server) {
      class_639 address = class_639.method_2950(server.field_3761);
      class_412.method_36877(new class_500(new class_442()), client, address, server, false, null);
   }

   private static class_2561 reconnectLabel(boolean auto, int ticksLeft) {
      return auto ? class_2561.method_43470(String.format(Locale.ROOT, "Reconnect (%.1f)", ticksLeft / 20.0)) : class_2561.method_43470("Reconnect");
   }
}
