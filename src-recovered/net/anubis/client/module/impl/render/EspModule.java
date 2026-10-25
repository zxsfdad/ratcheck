package net.anubis.client.module.impl.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.anubis.client.config.FriendStore;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.TracerEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12179;
import net.minecraft.class_12180;
import net.minecraft.class_1297;
import net.minecraft.class_1308;
import net.minecraft.class_1309;
import net.minecraft.class_1569;
import net.minecraft.class_1657;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_638;

@Environment(EnvType.CLIENT)
public final class EspModule extends ClientModule {
   private static EspModule instance;
   private static final int ENTITY_CAP = 96;
   private static final Comparator<EspModule.Target> PRIORITY = Comparator.<EspModule.Target, Boolean>comparing(
         target -> target.group() != EspModule.Group.PLAYER
      )
      .thenComparingDouble(EspModule.Target::distanceSq);
   private final BooleanSetting sPlayers = this.addSetting("players", new BooleanSetting("Players", true));
   private final BooleanSetting sMobs = this.addSetting("mobs", new BooleanSetting("Hostile Mobs", true));
   private final BooleanSetting sAnimals = this.addSetting("animals", new BooleanSetting("Passive Mobs", false));
   private final BooleanSetting sIgnoreSelf = this.addSetting("ignore-self", new BooleanSetting("Ignore Self", true));
   private final BooleanSetting sBox = this.addSetting("box", new BooleanSetting("Box", true));
   private final BooleanSetting sTracers = this.addSetting("tracers", new BooleanSetting("Tracers", true));
   private final BooleanSetting sCustomTracerColor = this.addSetting("custom-tracer-color", new BooleanSetting("Custom Tracer Color", false));
   private final ColorSetting sTracerColor = this.addSetting("tracer-color", new ColorSetting("Tracer Color", -1));
   private final ModeSetting sShape = this.addSetting("shape", new ModeSetting("Shape", "Glow", "Glow", "Box", "Fill", "Both"));
   private final NumberSetting sRange = this.addSetting("range", new NumberSetting("Range", 96.0F, 16.0F, 256.0F).step(1.0F));
   private final NumberSetting sMaxEntities = this.addSetting("max-entities", new NumberSetting("Max Entities", 48.0F, 8.0F, 96.0F).step(1.0F));
   private final ColorSetting sPlayerColor = this.addSetting("player-color", new ColorSetting("Player Color", -1));
   private final BooleanSetting sFriends = this.addSetting("friends", new BooleanSetting("Highlight Friends", true));
   private final ColorSetting sFriendColor = this.addSetting("friend-color", new ColorSetting("Friend Color", -10169601));
   private final ColorSetting sMobColor = this.addSetting("mob-color", new ColorSetting("Hostile Color", -47814));
   private final ColorSetting sAnimalColor = this.addSetting("animal-color", new ColorSetting("Passive Color", -13577896));
   private final NumberSetting sFillAlpha = this.addSetting("fill-alpha", new NumberSetting("Fill Alpha", 0.3F, 0.0F, 1.0F).step(0.05F));
   private final NumberSetting sLineWidth = this.addSetting("line-width", new NumberSetting("Line Width", 1.5F, 0.5F, 5.0F).step(0.5F));
   private final Map<class_1297, Integer> glowing = new IdentityHashMap<>();
   private long glowFrame = -1L;
   private class_638 glowLevel;

   public EspModule() {
      super("esp", "ESP", "Highlights players, hostile and passive mobs with outlines, boxes and tracers.", ModuleCategory.RENDER);
      instance = this;
      this.sBox.visibleWhen(() -> !this.sShape.is("Glow"));
      this.sFillAlpha.visibleWhen(() -> this.sShape.is("Fill") || this.sShape.is("Both"));
      this.sLineWidth.visibleWhen(() -> this.sShape.is("Box") || this.sShape.is("Both"));
      this.sCustomTracerColor.visibleWhen(this.sTracers::get);
      this.sTracerColor.visibleWhen(() -> this.sTracers.get() && this.sCustomTracerColor.get());
      this.sPlayerColor.visibleWhen(this.sPlayers::get);
      this.sFriends.visibleWhen(this.sPlayers::get);
      this.sFriendColor.visibleWhen(() -> this.sPlayers.get() && this.sFriends.get());
      this.sMobColor.visibleWhen(this.sMobs::get);
      this.sAnimalColor.visibleWhen(this.sAnimals::get);
   }

   @Override
   protected void onDisable() {
      this.resetGlow();
   }

   public static boolean shouldGlow(class_1297 entity) {
      EspModule module = instance;
      return module != null && module.isEnabled() && module.sShape.is("Glow") ? module.glowing().containsKey(entity) : false;
   }

   public static int glowColor(class_1297 entity) {
      EspModule module = instance;
      Integer color = module == null ? null : module.glowing().get(entity);
      return (color == null ? 16777215 : color) & 16777215;
   }

   private Map<class_1297, Integer> glowing() {
      long frame = Render3DEvent.frame();
      if (frame != this.glowFrame) {
         this.glowFrame = frame;
         this.glowing.clear();

         for (EspModule.Target target : this.targets(1.0F)) {
            this.glowing.put(target.entity(), this.colorFor(target));
         }
      }

      return this.glowing;
   }

   @EventHandler
   public void onTick(TickEvent event) {
      class_638 level = class_310.method_1551().field_1687;
      if (level != this.glowLevel || !this.sShape.is("Glow")) {
         this.resetGlow();
      }

      this.glowLevel = level;
   }

   private void resetGlow() {
      this.glowing.clear();
      this.glowFrame = -1L;
      this.glowLevel = null;
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      boolean box = this.sBox.get() && !this.sShape.is("Glow");
      boolean tracers = this.sTracers.get();
      if (box || tracers) {
         String shape = this.sShape.get();
         float lineWidth = this.sLineWidth.get();
         int fillAlpha = Math.round(this.sFillAlpha.get() * 255.0F);

         for (EspModule.Target target : this.targets(event.partialTick)) {
            int color = this.colorFor(target);
            class_238 bounds = interpolatedBox(target.entity(), event.partialTick);
            if (box) {
               int fill = color & 16777215 | fillAlpha << 24;

               class_12179 style = switch (shape) {
                  case "Fill" -> class_12179.method_75539(fill);
                  case "Both" -> class_12179.method_75537(color, lineWidth, fill);
                  default -> class_12179.method_75536(color, lineWidth);
               };
               class_12180.method_75541(bounds, style).method_75533();
            }

            if (tracers) {
               TracerEngine.draw(bounds.method_1005(), this.sCustomTracerColor.get() ? this.sTracerColor.argb() : color);
            }
         }
      }
   }

   private List<EspModule.Target> targets(float partialTick) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1687 != null && mc.field_1724 != null) {
         int max = Math.max(1, Math.min(96, this.sMaxEntities.getInt()));
         float range = Math.max(1.0F, this.sRange.get());
         double rangeSq = (double)range * range;
         class_243 eye = mc.field_1724.method_5836(partialTick);
         List<EspModule.Target> result = new ArrayList<>();

         for (class_1297 entity : mc.field_1687.method_18112()) {
            EspModule.Group group = groupOf(entity);
            if (group != null
               && this.shown(group)
               && (entity != mc.field_1724 || !this.sIgnoreSelf.get())
               && !isGone(entity)
               && !(entity instanceof class_1657 player && player.method_7325())) {
               class_243 interpolated = entity.method_30950(partialTick);
               double distanceSq = interpolated.method_1025(eye);
               if (!(distanceSq > rangeSq)) {
                  result.add(new EspModule.Target(entity, group, distanceSq));
               }
            }
         }

         if (result.size() <= max) {
            return result;
         }

         result.sort(PRIORITY);
         return result.subList(0, max);
      } else {
         return List.of();
      }
   }

   private static boolean isGone(class_1297 entity) {
      return entity.method_31481() || entity instanceof class_1309 living && living.method_29504();
   }

   private static EspModule.Group groupOf(class_1297 entity) {
      if (entity instanceof class_1657) {
         return EspModule.Group.PLAYER;
      } else if (entity instanceof class_1569) {
         return EspModule.Group.HOSTILE;
      } else {
         return entity instanceof class_1308 ? EspModule.Group.PASSIVE : null;
      }
   }

   private boolean shown(EspModule.Group group) {
      return switch (group) {
         case PLAYER -> this.sPlayers.get();
         case HOSTILE -> this.sMobs.get();
         case PASSIVE -> this.sAnimals.get();
      };
   }

   private int colorFor(EspModule.Target target) {
      return switch (target.group()) {
         case PLAYER -> {
            if (this.sFriends.get() && target.entity() instanceof class_1657 player && FriendStore.getInstance().isFriend(player.method_7334().name())) {
               yield this.sFriendColor.argb();
            }

            yield this.sPlayerColor.argb();
         }
         case HOSTILE -> this.sMobColor.argb();
         case PASSIVE -> this.sAnimalColor.argb();
      };
   }

   private static class_238 interpolatedBox(class_1297 entity, float partialTick) {
      class_243 current = entity.method_73189();
      class_243 interpolated = entity.method_30950(partialTick);
      return entity.method_5829()
         .method_989(interpolated.field_1352 - current.field_1352, interpolated.field_1351 - current.field_1351, interpolated.field_1350 - current.field_1350);
   }

   @Environment(EnvType.CLIENT)
   private enum Group {
      PLAYER,
      HOSTILE,
      PASSIVE;
   }

   @Environment(EnvType.CLIENT)
   private record Target(class_1297 entity, EspModule.Group group, double distanceSq) {
   }
}
