package net.anubis.client.module.impl.donutmacros;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.anubis.client.chat.ModuleChat;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.PacketReceiveNettyEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_7439;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class TpMacroModule extends ClientModule {
   private static final Pattern RATE_LIMIT = Pattern.compile("^you need to wait another ([\\d.]+) seconds? to execute a command");
   private static final Pattern VALID_NAME = Pattern.compile("\\.?[A-Za-z0-9_]{1,16}");
   private final ModeSetting command = this.addSetting("command", new ModeSetting("Command", "tpa", "tpa", "tpahere"));
   private final StringSetting target = this.addSetting("player", new StringSetting("Player", "").hint("Player name"));
   private final NumberSetting interval = this.addSetting("interval", new NumberSetting("Interval (s)", 1.0F, 0.25F, 60.0F).step(0.25F));
   private final TeleportConfirm confirm = new TeleportConfirm(this);
   private volatile long holdUntil;
   private long nextAt;
   private int sent;

   public TpMacroModule() {
      super("tp-macro", "Tp Macro", "Sends /tpa or /tpahere to one player on a timer.", ModuleCategory.DONUT_MACROS);
      this.usesNotifications();
      this.target.privateValue();
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      String name = this.target.get().trim();
      if (!VALID_NAME.matcher(name).matches()) {
         ModuleChat.send(this, ModuleChat.body().warn("Set a valid Player name first."));
         this.requestDisable();
      } else {
         this.confirm.reset();
         this.holdUntil = 0L;
         this.nextAt = 0L;
         this.sent = 0;
         ModuleChat.send(
            this,
            ModuleChat.body().text("Sending ").value("/" + this.command.get() + " " + name).text(" every ").value(this.interval.formatted() + "s").text(".")
         );
      }
   }

   @Override
   protected void onDisable() {
      if (this.sent > 0) {
         ModuleChat.send(this, ModuleChat.body().text("Sent ").value(this.sent).text(this.sent == 1 ? " request." : " requests."));
      }
   }

   @EventHandler
   public void onPacket(PacketReceiveNettyEvent event) {
      if (event.packet instanceof class_7439 chat && chat.comp_763() != null) {
         Matcher wait = RATE_LIMIT.matcher(ServerText.normalize(chat.comp_763().getString()));
         if (wait.find()) {
            try {
               this.holdUntil = System.currentTimeMillis() + (long)Math.ceil(Math.min(30.0, Double.parseDouble(wait.group(1))) * 1000.0) + 80L;
            } catch (NumberFormatException var5) {
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (player != null && mc.method_1562() != null && !player.method_29504()) {
         this.confirm.tick(mc);
         long now = System.currentTimeMillis();
         if (now >= this.nextAt && now >= this.holdUntil) {
            player.field_3944.method_45730(this.command.get() + " " + this.target.get().trim());
            this.confirm.sent();
            this.sent++;
            this.nextAt = now + (long)(this.interval.get() * 1000.0F);
         }
      }
   }
}
