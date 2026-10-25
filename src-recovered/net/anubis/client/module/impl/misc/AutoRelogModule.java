package net.anubis.client.module.impl.misc;

import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.LevelUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_3928;
import net.minecraft.class_412;
import net.minecraft.class_437;
import net.minecraft.class_500;
import net.minecraft.class_638;
import net.minecraft.class_639;
import net.minecraft.class_642;
import net.minecraft.class_746;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class AutoRelogModule extends ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final int REARM_HEIGHT = 4;
   private static final int REARM_TICKS = 40;
   private final NumberSetting yLevel = this.addSetting("y-level", new NumberSetting("Y Level", 0.0F, -60.0F, 16.0F).step(1.0F));
   private final NumberSetting delay = this.addSetting("delay", new NumberSetting("Delay", 3.0F, 1.0F, 30.0F).step(0.5F));
   private class_638 seenLevel;
   private class_746 seenPlayer;
   private int aboveTicks;
   private class_642 rejoin;
   private boolean disconnected;
   private int rejoinTicks;

   public AutoRelogModule() {
      super("auto-relog", "Auto Relog", "Relogs once when you go down to deepslate level.", ModuleCategory.DONUT);
   }

   @Override
   protected void onEnable() {
      this.forget();
      this.on(TickEvent.TYPE, event -> this.onTick());
   }

   @Override
   protected void onDisable() {
      this.forget();
   }

   private void forget() {
      this.seenLevel = null;
      this.seenPlayer = null;
      this.aboveTicks = 0;
      this.rejoin = null;
      this.disconnected = false;
   }

   private void onTick() {
      class_310 mc = class_310.method_1551();
      if (this.rejoin != null) {
         this.waitToRejoin(mc);
      } else {
         class_746 player = mc.field_1724;
         class_638 level = mc.field_1687;
         if (player != null && level != null && !(mc.field_1755 instanceof class_3928)) {
            if (level != this.seenLevel || player != this.seenPlayer) {
               this.seenLevel = level;
               this.seenPlayer = player;
               this.aboveTicks = 0;
            }

            if (!LevelUtil.isOverworld(level)) {
               this.aboveTicks = 0;
            } else {
               double y = player.method_23318();
               float line = this.yLevel.get();
               if (y >= line + 4.0F) {
                  if (this.aboveTicks < 40) {
                     this.aboveTicks++;
                  }
               } else if (y < line) {
                  boolean armed = this.aboveTicks >= 40;
                  this.aboveTicks = 0;
                  if (armed) {
                     this.relog(mc, y);
                  }
               }
            }
         } else {
            this.aboveTicks = 0;
         }
      }
   }

   private void relog(class_310 mc, double y) {
      class_642 server = mc.method_1542() ? null : mc.method_1558();
      if (server != null && !server.method_52811()) {
         LOGGER.info("[Anubis] Auto Relog: reached Y {}, relogging", (int)Math.floor(y));
         this.rejoin = server;
         this.disconnected = false;
         this.rejoinTicks = Math.round(this.delay.get() * 20.0F);
         mc.method_63588(() -> {
            if (this.rejoin == server && mc.field_1687 != null) {
               mc.method_73360(class_638.field_61021);
               this.disconnected = true;
            }
         });
      }
   }

   private void waitToRejoin(class_310 mc) {
      if (this.disconnected) {
         if (mc.field_1687 == null && mc.field_1755 instanceof class_500) {
            if (--this.rejoinTicks <= 0) {
               class_642 server = this.rejoin;
               class_437 serverList = mc.field_1755;
               this.rejoin = null;
               mc.method_63588(() -> {
                  if (mc.field_1755 == serverList && mc.field_1687 == null) {
                     class_412.method_36877(serverList, mc, class_639.method_2950(server.field_3761), server, false, null);
                  }
               });
            }
         } else {
            this.rejoin = null;
         }
      }
   }
}
