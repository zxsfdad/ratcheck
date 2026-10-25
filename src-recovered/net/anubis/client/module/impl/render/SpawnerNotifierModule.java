package net.anubis.client.module.impl.render;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.function.LongPredicate;
import net.anubis.client.AnubisClientMod;
import net.anubis.client.chat.ModuleChat;
import net.anubis.client.esp.BlockLayer;
import net.anubis.client.esp.ChunkScanner;
import net.anubis.client.esp.EspStyle;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1109;
import net.minecraft.class_1297;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_2636;
import net.minecraft.class_2680;
import net.minecraft.class_310;

@Environment(EnvType.CLIENT)
public final class SpawnerNotifierModule extends ClientModule implements ChunkScanner.Consumer {
   private static final String SPAWNER = "Spawner";
   private final BooleanSetting sSound = this.addSetting("sound", new BooleanSetting("Sound", true));
   private final BooleanSetting sRender = this.addSetting("render", new BooleanSetting("Render", true));
   private final ColorSetting sColor = this.addSetting("color", new ColorSetting("Color", -587246251));
   private final EspStyle style = new EspStyle((x$0, x$1) -> (S)this.addSetting(x$0, x$1), "Outline", 0.1F, 256.0F);
   private final BlockLayer layer = new BlockLayer(new BlockLayer.Palette() {
      @Override
      public int color(Object kind) {
         return SpawnerNotifierModule.this.sColor.argb();
      }

      @Override
      public int tracerColor(Object kind) {
         return 0;
      }
   }, () -> class_310.method_1551().field_1687 == null ? null : class_310.method_1551().field_1687.method_27983());
   private final LongOpenHashSet notified = new LongOpenHashSet();

   public SpawnerNotifierModule() {
      super("spawner-notifier", "Spawner Notifier", "Scans chunks for mob spawners and highlights them.", ModuleCategory.DONUT);
      this.usesNotifications();
   }

   @Override
   protected void onEnable() {
      ChunkScanner.register(this);
   }

   @Override
   protected void onDisable() {
      ChunkScanner.unregister(this);
      this.layer.clear();
      this.notified.clear();
   }

   @Override
   public boolean wants(class_2680 state) {
      return state.method_27852(class_2246.field_10260);
   }

   @Override
   public void found(long pos, class_2680 state) {
      this.layer.put(pos, "Spawner");
      if (this.notified.add(pos)) {
         this.notify(class_2338.method_10092(pos));
      }
   }

   @Override
   public void removed(long pos) {
      this.layer.remove(pos);
   }

   @Override
   public void reset() {
      this.layer.clear();
      this.notified.clear();
   }

   @Override
   public void chunkScanned(long chunkKey, LongPredicate stillThere) {
      this.layer.retainChunk(chunkKey, stillThere);
   }

   private void notify(class_2338 pos) {
      class_310 mc = class_310.method_1551();
      if (this.notificationsOn()) {
         ModuleChat.send(this, foundMessage(mc, pos));
      }

      if (this.sSound.get() && mc.field_1724 != null && mc.field_1687 != null) {
         mc.method_1483().method_4873(class_1109.method_4757(AnubisClientMod.SPAWNER_NOTIFY_SOUND, 1.0F, 1.0F));
      }
   }

   private static ModuleChat.Body foundMessage(class_310 mc, class_2338 pos) {
      String mob = entityName(mc, pos);
      ModuleChat.Body body = mob == null ? ModuleChat.body().warn("Unknown") : ModuleChat.body().name(mob);
      body.text(" spawner ").good("found").text(" at ").coords(pos.method_10263(), pos.method_10264(), pos.method_10260());
      if (mc.field_1724 != null) {
         long distance = Math.round(mc.field_1724.method_73189().method_1022(class_243.method_24953(pos)));
         body.text(" (").value(distance + "m").text(")");
      }

      return body;
   }

   private static String entityName(class_310 mc, class_2338 pos) {
      if (mc.field_1687 != null && mc.field_1687.method_8321(pos) instanceof class_2636 spawner) {
         class_1297 entity = spawner.method_11390().method_8283(mc.field_1687, pos);
         return entity == null ? null : entity.method_5864().method_5897().getString();
      } else {
         return null;
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      if (this.sRender.get()) {
         this.layer.render(event, this.style);
      }
   }
}
