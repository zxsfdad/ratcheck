package net.anubis.client.module.impl.render;

import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.combat.CombatTarget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1569;
import net.minecraft.class_1657;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_4587;
import net.minecraft.class_7833;

@Environment(EnvType.CLIENT)
public final class SwingAnimationModule extends ClientModule {
   public static volatile SwingAnimationModule active;
   private static final String KHOPESH = "Slash";
   private static final String ANKH = "Chop";
   private static final String SICKLE = "Hook";
   private static final String SCARAB = "Spin";
   private static final String HORUS = "Double Tap";
   private static final String SPHINX = "Push";
   private static final long COMBAT_WINDOW_MS = 3000L;
   private static final float PI = (float) Math.PI;
   private static final float TAU = (float) (Math.PI * 2);
   private final ModeSetting style = this.addSetting("style", new ModeSetting("Style", "Slash", "Slash", "Chop", "Hook", "Spin", "Double Tap", "Push"));
   private final NumberSetting tempo = this.addSetting("tempo", new NumberSetting("Tempo", 1.0F, 0.4F, 2.5F).step(0.05F));
   private final NumberSetting intensity = this.addSetting("intensity", new NumberSetting("Intensity", 1.0F, 0.2F, 2.0F).step(0.05F));
   private final BooleanSetting offhand = this.addSetting("offhand", new BooleanSetting("Offhand Too", true));
   private final BooleanSetting combatOnly = this.addSetting("combat-only", new BooleanSetting("Combat Only", false));
   private int styleIndex;
   private boolean styled;

   public SwingAnimationModule() {
      super("swing-animation", "Swing Animation", "Changes your first-person swing animation.", ModuleCategory.VISUALS);
      this.style.onChange(this::cacheStyle);
      this.cacheStyle();
   }

   @Override
   protected void onEnable() {
      this.latch();
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   private void cacheStyle() {
      this.styleIndex = Math.max(0, this.style.modes().indexOf(this.style.get()));
   }

   public void latch() {
      this.styled = !this.combatOnly.get() || inCombat();
   }

   private static boolean inCombat() {
      if (CombatTarget.lastHit(3000L) != null) {
         return true;
      }

      class_1297 looked = class_310.method_1551().field_1692;
      return (looked instanceof class_1657 || looked instanceof class_1569) && looked.method_5805();
   }

   public boolean covers(boolean mainHand) {
      return (mainHand || this.offhand.get()) && this.styled;
   }

   public int swingTicks(int ticks) {
      return !this.styled ? ticks : Math.max(1, Math.round(ticks / this.tempo.get()));
   }

   public void pose(class_4587 pose, float progress, int side) {
      if (!(progress <= 0.0F) && !(progress >= 1.0F)) {
         float k = this.intensity.get();
         switch (this.styleIndex) {
            case 1:
               ankh(pose, progress, side, k);
               break;
            case 2:
               sickle(pose, progress, side, k);
               break;
            case 3:
               scarab(pose, progress, side, k);
               break;
            case 4:
               horus(pose, progress, side, k);
               break;
            case 5:
               sphinx(pose, progress, side, k);
               break;
            default:
               khopesh(pose, progress, side, k);
         }
      }
   }

   private static void khopesh(class_4587 pose, float t, int side, float k) {
      float a = crest(t, 0.3F) * k;
      pose.method_46416(side * -0.3F * a, 0.05F * class_3532.method_15374((float) Math.PI * t) * k, -0.1F * a);
      pose.method_22907(class_7833.field_40716.rotationDegrees(side * 64.0F * a));
      pose.method_22907(class_7833.field_40718.rotationDegrees(side * 48.0F * a));
      pose.method_22907(class_7833.field_40714.rotationDegrees(-14.0F * a));
   }

   private static void ankh(class_4587 pose, float t, int side, float k) {
      float raise = t < 0.28F ? class_3532.method_15374((float) Math.PI * t / 0.28F) * k : 0.0F;
      float strike = t <= 0.18F ? 0.0F : crest((t - 0.18F) / 0.82F, 0.28F) * k;
      pose.method_46416(side * -0.06F * strike, 0.16F * raise - 0.1F * strike, 0.06F * raise - 0.24F * strike);
      pose.method_22907(class_7833.field_40714.rotationDegrees(30.0F * raise - 105.0F * strike));
      pose.method_22907(class_7833.field_40718.rotationDegrees(side * 10.0F * strike));
   }

   private static void sickle(class_4587 pose, float t, int side, float k) {
      float a = crest(t, 0.42F);
      float hook = a * a * k;
      a *= k;
      pose.method_46416(side * -0.1F * a, -0.24F * a, -0.06F * a);
      pose.method_22907(class_7833.field_40718.rotationDegrees(side * -100.0F * a));
      pose.method_22907(class_7833.field_40716.rotationDegrees(side * 30.0F * hook));
      pose.method_22907(class_7833.field_40714.rotationDegrees(-25.0F * a));
   }

   private static void scarab(class_4587 pose, float t, int side, float k) {
      float turn = (float) (Math.PI * 2) * t;
      float cos = class_3532.method_15362(turn);
      float sin = class_3532.method_15374(turn);
      float body = class_3532.method_15374((float) Math.PI * t) * k;
      float r = 0.17F * k;
      pose.method_46416(side * r * 0.6F * (cos - 1.0F), r * sin, -0.12F * body);
      pose.method_22907(class_7833.field_40718.rotationDegrees(side * 40.0F * sin * k));
      pose.method_22907(class_7833.field_40714.rotationDegrees(-40.0F * body));
   }

   private static void horus(class_4587 pose, float t, int side, float k) {
      float beat = class_3532.method_15374((float) (Math.PI * 2) * t);
      float w = beat * beat * (t < 0.5F ? 1.0F : 0.65F) * k;
      pose.method_46416(side * -0.08F * w, 0.06F * w, -0.1F * w);
      pose.method_22907(class_7833.field_40716.rotationDegrees(side * 26.0F * w));
      pose.method_22907(class_7833.field_40714.rotationDegrees(-50.0F * w));
      pose.method_22907(class_7833.field_40718.rotationDegrees(side * -22.0F * w));
   }

   private static void sphinx(class_4587 pose, float t, int side, float k) {
      float a = (0.5F - 0.5F * class_3532.method_15362((float) (Math.PI * 2) * t)) * k;
      pose.method_46416(0.0F, -0.06F * a, -0.1F * a);
      pose.method_22907(class_7833.field_40714.rotationDegrees(-24.0F * a));
      pose.method_22907(class_7833.field_40716.rotationDegrees(side * 10.0F * a));
   }

   private static float crest(float t, float peak) {
      if (t <= 0.0F || t >= 1.0F) {
         return 0.0F;
      } else if (t < peak) {
         float u = 1.0F - t / peak;
         return 1.0F - u * u * u;
      } else {
         float u = (t - peak) / (1.0F - peak);
         return 1.0F - u * u * (3.0F - 2.0F * u);
      }
   }
}
