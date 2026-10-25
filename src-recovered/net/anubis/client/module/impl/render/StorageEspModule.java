package net.anubis.client.module.impl.render;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.HashMap;
import java.util.Map;
import net.anubis.client.esp.BlockLayer;
import net.anubis.client.esp.EspStyle;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.PacketAppliedEvent;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1923;
import net.minecraft.class_1937;
import net.minecraft.class_2248;
import net.minecraft.class_2281;
import net.minecraft.class_2331;
import net.minecraft.class_2336;
import net.minecraft.class_2338;
import net.minecraft.class_2363;
import net.minecraft.class_2377;
import net.minecraft.class_2480;
import net.minecraft.class_2487;
import net.minecraft.class_2496;
import net.minecraft.class_2586;
import net.minecraft.class_2591;
import net.minecraft.class_2622;
import net.minecraft.class_2626;
import net.minecraft.class_2637;
import net.minecraft.class_2672;
import net.minecraft.class_2678;
import net.minecraft.class_2680;
import net.minecraft.class_2818;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3708;
import net.minecraft.class_5321;
import net.minecraft.class_638;
import net.minecraft.class_7923;

@Environment(EnvType.CLIENT)
public final class StorageEspModule extends ClientModule {
   private final EspStyle style = new EspStyle((x$0, x$1) -> (S)this.addSetting(x$0, x$1), "Outline", 0.25F, 256.0F);
   private final BooleanSetting sTracers = this.addSetting("tracers", new BooleanSetting("Tracers", false));
   private final StorageEspModule.Type chest = new StorageEspModule.Type(
      "Chest", true, -6530304, "chests", "chest-color", "chest-tracers", "chest-tracer-color"
   );
   private final StorageEspModule.Type enderChest = new StorageEspModule.Type(
      "Ender Chest", true, -9109249, "ender-chests", "ender-chest-color", "ender-chest-tracers", "ender-chest-tracer-color"
   );
   private final StorageEspModule.Type shulker = new StorageEspModule.Type(
      "Shulker Box", true, -7995234, "shulker-boxes", "shulker-box-color", "shulker-tracers", "shulker-tracer-color"
   );
   private final StorageEspModule.Type spawner = new StorageEspModule.Type(
      "Spawner", true, -7700826, "spawners", "spawner-color", "spawner-tracers", "spawner-tracer-color"
   );
   private final StorageEspModule.Type furnace = new StorageEspModule.Type(
      "Furnace", false, -10197916, "furnaces", "furnace-color", "furnace-tracers", "furnace-tracer-color"
   );
   private final StorageEspModule.Type barrel = new StorageEspModule.Type(
      "Barrel", true, -10204366, "barrels", "barrel-color", "barrel-tracers", "barrel-tracer-color"
   );
   private final StorageEspModule.Type enchanting = new StorageEspModule.Type(
      "Enchanting Table", true, -3670016, "enchanting-tables", "enchanting-table-color", "enchanting-tracers", "enchanting-tracer-color"
   );
   private final StorageEspModule.Type hopper = new StorageEspModule.Type(
      "Hopper", false, -11513776, "hoppers", "hopper-color", "hopper-tracers", "hopper-tracer-color"
   );
   private final StorageEspModule.Type[] types = new StorageEspModule.Type[]{
      this.chest, this.enderChest, this.shulker, this.spawner, this.furnace, this.barrel, this.enchanting, this.hopper
   };
   private final BlockLayer layer = new BlockLayer(new BlockLayer.Palette() {
      @Override
      public int color(Object kind) {
         return ((StorageEspModule.Type)kind).color.argb();
      }

      @Override
      public int tracerColor(Object kind) {
         StorageEspModule.Type t = (StorageEspModule.Type)kind;
         return t.tracers.get() ? t.tracerColor.argb() : 0;
      }
   }, () -> class_310.method_1551().field_1687 == null ? null : class_310.method_1551().field_1687.method_27983());
   private final Map<class_5321<class_1937>, Long2ObjectOpenHashMap<StorageEspModule.Type>> detections = new HashMap<>();
   private class_5321<class_1937> shownDimension;

   public StorageEspModule() {
      super("storage-esp", "Storage ESP", "Highlights storage blocks and spawners with per-type colours.", ModuleCategory.RENDER);
   }

   private StorageEspModule.Type typeOf(class_2248 block) {
      if (block instanceof class_2281) {
         return this.chest;
      } else if (block instanceof class_2336) {
         return this.enderChest;
      } else if (block instanceof class_2480) {
         return this.shulker;
      } else if (block instanceof class_2496) {
         return this.spawner;
      } else if (block instanceof class_2363) {
         return this.furnace;
      } else if (block instanceof class_3708) {
         return this.barrel;
      } else if (block instanceof class_2331) {
         return this.enchanting;
      } else {
         return block instanceof class_2377 ? this.hopper : null;
      }
   }

   private StorageEspModule.Type typeOf(class_2591<?> type) {
      if (type == class_2591.field_11914 || type == class_2591.field_11891) {
         return this.chest;
      } else if (type == class_2591.field_11901) {
         return this.enderChest;
      } else if (type == class_2591.field_11896) {
         return this.shulker;
      } else if (type == class_2591.field_11889) {
         return this.spawner;
      } else if (type == class_2591.field_11903 || type == class_2591.field_16415 || type == class_2591.field_16414) {
         return this.furnace;
      } else if (type == class_2591.field_16411) {
         return this.barrel;
      } else if (type == class_2591.field_11912) {
         return this.enchanting;
      } else {
         return type == class_2591.field_11888 ? this.hopper : null;
      }
   }

   private StorageEspModule.Type typeOf(class_2591<?> type, class_2487 tag) {
      StorageEspModule.Type known = this.typeOf(type);
      if (known == null && tag != null) {
         class_2960 id = tag.method_10558("id").<class_2960>map(class_2960::method_12829).orElse(null);
         return id == null ? null : this.typeOf((class_2591<?>)class_7923.field_41181.method_63535(id));
      } else {
         return known;
      }
   }

   private void rescan() {
      if (this.isEnabled()) {
         this.layer.clear();
         class_638 level = class_310.method_1551().field_1687;
         this.shownDimension = level == null ? null : level.method_27983();
         Long2ObjectOpenHashMap<StorageEspModule.Type> found = this.shownDimension == null ? null : this.detections.get(this.shownDimension);
         if (found != null) {
            ObjectIterator var3 = found.long2ObjectEntrySet().iterator();

            while (var3.hasNext()) {
               Entry<StorageEspModule.Type> entry = (Entry<StorageEspModule.Type>)var3.next();
               if (((StorageEspModule.Type)entry.getValue()).enabled.get()) {
                  this.layer.put(entry.getLongKey(), entry.getValue());
               }
            }
         }
      }
   }

   private boolean anyTracer() {
      if (!this.sTracers.get()) {
         return false;
      }

      for (StorageEspModule.Type type : this.types) {
         if (type.enabled.get() && type.tracers.get()) {
            return true;
         }
      }

      return false;
   }

   @Override
   protected void onEnable() {
      this.detections.clear();
      this.layer.clear();
      class_638 level = class_310.method_1551().field_1687;
      this.shownDimension = level == null ? null : level.method_27983();
      if (level != null && class_310.method_1551().field_1724 != null) {
         class_1923 center = class_310.method_1551().field_1724.method_31476();
         int radius = class_310.method_1551().field_1690.method_38521() + 1;

         for (int x = center.field_9181 - radius; x <= center.field_9181 + radius; x++) {
            for (int z = center.field_9180 - radius; z <= center.field_9180 + radius; z++) {
               class_2818 chunk = level.method_2935().method_21730(x, z);
               if (chunk != null) {
                  this.scanBlockEntities(chunk);
               }
            }
         }
      }
   }

   @Override
   protected void onDisable() {
      this.detections.clear();
      this.layer.clear();
      this.shownDimension = null;
   }

   private void found(class_2338 pos, StorageEspModule.Type type) {
      class_638 level = class_310.method_1551().field_1687;
      if (type != null && level != null) {
         long key = pos.method_10063();
         StorageEspModule.Type previous = (StorageEspModule.Type)this.detections
            .computeIfAbsent(level.method_27983(), k -> new Long2ObjectOpenHashMap())
            .put(key, type);
         if (previous != type) {
            if (previous != null) {
               this.layer.remove(key);
            }

            if (type.enabled.get()) {
               this.layer.put(key, type);
            }
         }
      }
   }

   private void gone(class_2338 pos) {
      class_638 level = class_310.method_1551().field_1687;
      if (level != null) {
         Long2ObjectOpenHashMap<StorageEspModule.Type> found = this.detections.get(level.method_27983());
         if (found != null && found.remove(pos.method_10063()) != null) {
            this.layer.remove(pos.method_10063());
         }
      }
   }

   private void scanBlockEntities(class_2818 chunk) {
      for (class_2586 blockEntity : chunk.method_12214().values()) {
         this.found(blockEntity.method_11016(), this.typeOf(blockEntity.method_11017()));
      }
   }

   private void blockChanged(class_2338 pos, class_2680 state) {
      StorageEspModule.Type type = this.typeOf(state.method_26204());
      if (type != null) {
         this.found(pos, type);
      } else if (state.method_26215()) {
         this.gone(pos);
      }
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent event) {
      class_638 level = class_310.method_1551().field_1687;
      if (level != null) {
         if (level.method_27983() != this.shownDimension) {
            this.rescan();
         }

         if (event.packet instanceof class_2672 packet) {
            packet.method_38598()
               .method_38587(packet.method_11523(), packet.method_11524())
               .accept((pos, type, tag) -> this.found(pos, this.typeOf(type, tag)));
            class_2818 chunk = level.method_2935().method_21730(packet.method_11523(), packet.method_11524());
            if (chunk != null) {
               this.scanBlockEntities(chunk);
            }
         } else if (event.packet instanceof class_2622 packet) {
            this.found(packet.method_11293(), this.typeOf(packet.method_11291(), packet.method_11290()));
         } else if (event.packet instanceof class_2626 packet) {
            this.blockChanged(packet.method_11309(), packet.method_11308());
         } else if (event.packet instanceof class_2637 packet) {
            packet.method_30621(this::blockChanged);
         } else if (event.packet instanceof class_2678) {
            this.detections.clear();
            this.layer.clear();
         }
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      class_638 level = class_310.method_1551().field_1687;
      if (level != null && level.method_27983() != this.shownDimension) {
         this.rescan();
      }

      this.layer.render(event, this.style);
      if (this.anyTracer()) {
         this.layer.renderTracers(this.style);
      }
   }

   @Environment(EnvType.CLIENT)
   private final class Type {
      final BooleanSetting enabled;
      final BooleanSetting tracers;
      final ColorSetting color;
      final ColorSetting tracerColor;

      Type(String label, boolean on, int argb, String idOn, String idColor, String idTracers, String idTracerColor) {
         this.enabled = StorageEspModule.this.addSetting(idOn, new BooleanSetting(label, on));
         this.color = StorageEspModule.this.addSetting(idColor, new ColorSetting(label + " Color", argb));
         this.tracers = StorageEspModule.this.addSetting(idTracers, new BooleanSetting(label + " Tracers", false));
         this.tracerColor = StorageEspModule.this.addSetting(idTracerColor, new ColorSetting(label + " Tracer Color", argb));
         this.color.visibleWhen(this.enabled::get);
         this.tracers.visibleWhen(() -> StorageEspModule.this.sTracers.get() && this.enabled.get());
         this.tracerColor.visibleWhen(() -> StorageEspModule.this.sTracers.get() && this.enabled.get() && this.tracers.get());
         this.enabled.onChange(StorageEspModule.this::rescan);
      }
   }
}
