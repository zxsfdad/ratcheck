package net.anubis.client.module.impl.combat;

import net.anubis.client.event.EventHandler;
import net.anubis.client.event.EventPriority;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.pvp.InputControl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12125;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_310;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class WTapRecodeModule extends ClientModule {
   private static final String W_TAP = "W Tap";
   private static final String S_TAP = "S Tap";
   private final ModeSetting mode = this.addSetting("mode", new ModeSetting("Mode", "W Tap", "W Tap", "S Tap"));
   private final NumberSetting hold = this.addSetting("hold", new NumberSetting("Hold", 1.0F, 1.0F, 3.0F).step(1.0F));
   private boolean sprintHit;
   private int taps;
   private int tapTick;

   public WTapRecodeModule() {
      super("w-tap-recode", "W Tap", "Resets your sprint right after a sprint hit so the next hit knocks back too.", ModuleCategory.COMBAT);
   }

   @Override
   protected void onDisable() {
      this.taps = 0;
   }

   public void beforeAttack(class_1657 player, class_1297 target) {
      class_746 self = class_310.method_1551().field_1724;
      this.sprintHit = player == self
         && self.method_5624()
         && MeleeAttacks.charge(self) > 0.9F
         && target instanceof class_1309
         && self.field_3913.field_54155.comp_3159();
   }

   public void afterAttack() {
      if (this.sprintHit) {
         this.start(class_310.method_1551().field_1724);
      }
   }

   public void afterStab() {
      class_746 player = class_310.method_1551().field_1724;
      if (player.method_5624() && player.field_3913.field_54155.comp_3159()) {
         if (MeleeAttacks.stabTarget(player, entity -> class_12125.method_75244(player, entity)) != null) {
            this.start(player);
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGH)
   public void onTick(TickEvent event) {
      class_746 player = class_310.method_1551().field_1724;
      if (player == null) {
         this.taps = 0;
      } else {
         if (this.taps > 0 && player.field_6012 != this.tapTick) {
            this.apply();
         }
      }
   }

   private void start(class_746 player) {
      this.taps = this.hold.getInt();
      this.tapTick = player.field_6012;
      this.apply();
   }

   private void apply() {
      InputControl.releaseForward(this.mode.is("S Tap"));
      this.taps--;
   }
}
