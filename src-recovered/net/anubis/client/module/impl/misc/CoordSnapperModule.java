package net.anubis.client.module.impl.misc;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.anubis.client.chat.ModuleChat;
import net.anubis.client.event.EventPriority;
import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.KeyInputEvent;
import net.anubis.client.event.impl.MouseInputEvent;
import net.anubis.client.input.ModuleKeybinds;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.KeySetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.module.setting.StringSetting;
import net.anubis.client.webhook.WebhookConfig;
import net.anubis.client.webhook.WebhookEmbed;
import net.anubis.client.webhook.WebhookPayloadBuilder;
import net.anubis.client.webhook.WebhookService;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_265;
import net.minecraft.class_310;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_4184;
import net.minecraft.class_5321;
import net.minecraft.class_638;
import net.minecraft.class_642;
import net.minecraft.class_746;
import net.minecraft.class_239.class_240;
import net.minecraft.class_3959.class_242;
import net.minecraft.class_3959.class_3960;

@Environment(EnvType.CLIENT)
public final class CoordSnapperModule extends ClientModule {
   private static final String TARGET_LOOKED_AT = "Looked At";
   private static final String TARGET_OWN_POSITION = "Own Position";
   private static final String FORMAT_SPACES = "X Y Z";
   private static final String FORMAT_COMMAS = "X, Y, Z";
   private static final String FORMAT_TP = "/tp Command";
   private static final double STEP_PAST_BLOCK = 0.001;
   private static final String WEBHOOK_TITLE = "\ud83d\udccd Coordinates";
   private static final int WEBHOOK_COLOR = 3438838;
   private static final ExecutorService WEBHOOK_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "Anubis-CoordSnapper-Webhook");
      thread.setDaemon(true);
      return thread;
   });
   private final KeySetting key = this.addSetting("key", new KeySetting("Key", -1));
   private final ModeSetting target = this.addSetting("target", new ModeSetting("Target", "Looked At", "Looked At", "Own Position"));
   private final NumberSetting range = this.addSetting("range", new NumberSetting("Range", 256.0F, 16.0F, 512.0F).step(1.0F));
   private final BooleanSetting stopAtFluids = this.addSetting("stop-at-fluids", new BooleanSetting("Stop at Fluids", false));
   private final ModeSetting format = this.addSetting("format", new ModeSetting("Format", "X Y Z", "X Y Z", "X, Y, Z", "/tp Command"));
   private final BooleanSetting includeDimension = this.addSetting("include-dimension", new BooleanSetting("Include Dimension", true));
   private final BooleanSetting confirm = this.addSetting("confirm", new BooleanSetting("Chat Confirmation", true));
   private final BooleanSetting webhook = this.addSetting("webhook", new BooleanSetting("Webhook", false));
   private final StringSetting webhookUrl = this.addSetting("webhook-url", new StringSetting("Webhook URL", ""));

   public CoordSnapperModule() {
      super("coord-snapper", "Coord Snapper", "Copies the coordinates you look at or stand on with one key.", ModuleCategory.MISC);
      this.range.visibleWhen(() -> this.target.is("Looked At"));
      this.stopAtFluids.visibleWhen(() -> this.target.is("Looked At"));
      BooleanSetting notifications = this.usesNotifications();
      this.confirm.visibleWhen(notifications::get);
      this.webhookUrl.visibleWhen(this.webhook::get);
      this.webhookUrl.privateValue();
   }

   @Override
   protected void onEnable() {
      this.on(KeyInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onKey);
      this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onMouse);
   }

   private void onKey(KeyInputEvent event) {
      if (event.action == 1) {
         this.onPress(event.key);
      }
   }

   private void onMouse(MouseInputEvent event) {
      if (event.action == 1) {
         this.onPress(1000 + event.button);
      }
   }

   private void onPress(int pressed) {
      int bound = this.key.value();
      if (ModuleKeybinds.isBindable(bound) && pressed == bound) {
         class_310 mc = class_310.method_1551();
         if (mc.field_1755 == null && mc.field_1724 != null && mc.field_1687 != null) {
            this.snap(mc, mc.field_1724, mc.field_1687);
         }
      }
   }

   private void snap(class_310 mc, class_746 player, class_638 level) {
      class_2338 pos;
      if (this.target.is("Own Position")) {
         pos = player.method_24515();
      } else {
         pos = this.raycast(mc, player, level);
         if (pos == null) {
            ModuleChat.send(
               this, ModuleChat.body().danger("Nothing copied").text(": no loaded block within ").value(this.range.getInt()).text(" blocks of your aim")
            );
            return;
         }

         if (this.format.is("/tp Command")) {
            pos = pos.method_10084();
         }
      }

      String text = this.formatCoords(pos, level.method_27983());
      mc.field_1774.method_1455(text);
      if (this.confirm.get()) {
         ModuleChat.send(this, ModuleChat.body().good("Copied").text(" ").value(text));
      }

      if (this.webhook.get()) {
         this.sendWebhook(mc, player, pos, level.method_27983());
      }
   }

   private void sendWebhook(class_310 mc, class_746 player, class_2338 pos, class_5321<class_1937> dimension) {
      String url = this.webhookUrl.get().trim();
      if (url.isEmpty()) {
         ModuleChat.send(this, ModuleChat.body().text("Webhook URL is ").warn("not set"));
      } else if (!WebhookConfig.isValidUrl(url)) {
         ModuleChat.send(this, ModuleChat.body().text("Webhook URL is ").danger("invalid"));
      } else {
         class_642 server = mc.method_1558();
         String serverName = server != null && server.field_3752 != null ? server.field_3752 : "Singleplayer";
         String description = "**Coordinates:** `"
            + String.format(Locale.ROOT, "%d %d %d", pos.method_10263(), pos.method_10264(), pos.method_10260())
            + "`\n**Dimension:** "
            + dimensionName(dimension)
            + "\n**Server:** "
            + escapeMarkdown(serverName)
            + "\n**Player:** "
            + escapeMarkdown(player.method_5477().getString())
            + "\n**Time:** <t:"
            + System.currentTimeMillis() / 1000L
            + ":R>";
         WebhookPayloadBuilder payload = new WebhookPayloadBuilder()
            .addEmbed(new WebhookEmbed("\ud83d\udccd Coordinates", description, 3438838, null, null, "Sent by Anubis", null));
         WebhookConfig config = new WebhookConfig(url);
         boolean confirmSent = this.confirm.get();
         WEBHOOK_EXECUTOR.execute(() -> {
            boolean sent;
            try {
               sent = CoordSnapperModule.WebhookHolder.SERVICE.send(config, payload);
            } catch (RuntimeException | LinkageError failure) {
               sent = false;
            }

            if (!sent) {
               ModuleChat.send(this, ModuleChat.body().text("Webhook ").danger("failed").text(" (see the log for details)"));
            } else if (confirmSent) {
               ModuleChat.send(this, ModuleChat.body().text("Sent to ").good("webhook"));
            }
         });
      }
   }

   private static String escapeMarkdown(String text) {
      StringBuilder out = new StringBuilder(text.length() + 8);

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c == '\\' || c == '*' || c == '_' || c == '~' || c == '`' || c == '|') {
            out.append('\\');
         }

         out.append(c);
      }

      return out.toString();
   }

   private class_2338 raycast(class_310 mc, class_746 player, class_638 level) {
      FreecamModule freecam = ModuleRegistry.get(FreecamModule.class);
      class_243 origin;
      class_243 direction;
      if (freecam != null && freecam.isEnabled()) {
         class_4184 camera = mc.field_1773.method_19418();
         origin = camera.method_71156();
         direction = class_243.method_1030(camera.method_19329(), camera.method_19330());
      } else {
         origin = player.method_5836(1.0F);
         direction = player.method_5828(1.0F);
      }

      class_243 end = origin.method_1019(direction.method_1021(this.range.get()));
      class_242 fluids = this.stopAtFluids.get() ? class_242.field_1347 : class_242.field_1348;
      class_3959 context = new class_3959(origin, end, class_3960.field_17559, fluids, player);
      class_2338 viewBlock = class_2338.method_49638(origin);
      if (startsInside(context, level, viewBlock, origin)) {
         Optional<class_243> exit = new class_238(viewBlock).method_992(end, origin);
         if (exit.isEmpty()) {
            return null;
         }

         class_243 start = exit.get().method_1019(direction.method_1021(0.001));
         context = new class_3959(start, end, class_3960.field_17559, fluids, player);
      }

      class_3965 hit = level.method_17742(context);
      return hit.method_17783() == class_240.field_1332 ? hit.method_17777() : null;
   }

   private static boolean startsInside(class_3959 context, class_638 level, class_2338 pos, class_243 point) {
      return contains(context.method_17748(level.method_8320(pos), level, pos), pos, point)
         || contains(context.method_17749(level.method_8316(pos), level, pos), pos, point);
   }

   private static boolean contains(class_265 shape, class_2338 pos, class_243 point) {
      for (class_238 box : shape.method_1090()) {
         if (box.method_996(pos).method_1006(point)) {
            return true;
         }
      }

      return false;
   }

   private String formatCoords(class_2338 pos, class_5321<class_1937> dimension) {
      int x = pos.method_10263();
      int y = pos.method_10264();
      int z = pos.method_10260();
      boolean withDimension = this.includeDimension.get();

      return switch (this.format.get()) {
         case "/tp Command" -> withDimension
            ? String.format(Locale.ROOT, "/execute in %s run tp @s %d %d %d", dimension.method_29177(), x, y, z)
            : String.format(Locale.ROOT, "/tp %d %d %d", x, y, z);
         case "X, Y, Z" -> String.format(Locale.ROOT, "%d, %d, %d", x, y, z) + dimensionSuffix(dimension, withDimension);
         default -> String.format(Locale.ROOT, "%d %d %d", x, y, z) + dimensionSuffix(dimension, withDimension);
      };
   }

   private static String dimensionSuffix(class_5321<class_1937> dimension, boolean enabled) {
      return enabled ? " (" + dimensionName(dimension) + ")" : "";
   }

   private static String dimensionName(class_5321<class_1937> dimension) {
      if (class_1937.field_25179.equals(dimension)) {
         return "Overworld";
      } else if (class_1937.field_25180.equals(dimension)) {
         return "Nether";
      } else {
         return class_1937.field_25181.equals(dimension) ? "End" : dimension.method_29177().toString();
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class WebhookHolder {
      static final WebhookService SERVICE = new WebhookService();
   }
}
