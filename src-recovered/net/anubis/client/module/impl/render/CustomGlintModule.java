package net.anubis.client.module.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1011;
import net.minecraft.class_1044;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3298;
import net.minecraft.class_918;
import net.minecraft.class_9848;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class CustomGlintModule extends ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   public static volatile CustomGlintModule active;
   private static final String COLOR_CUSTOM = "Custom";
   private static final String COLOR_ACCENT = "Accent";
   private final ModeSetting colorMode = this.addSetting("color-mode", new ModeSetting("Color", "Custom", "Custom", "Accent"));
   private final ColorSetting itemColor = this.addSetting("item-color", new ColorSetting("Item Color", -9524994));
   private final BooleanSetting separateArmor = this.addSetting("separate-armor", new BooleanSetting("Separate Armor", false));
   private final ColorSetting armorColor = this.addSetting("armor-color", new ColorSetting("Armor Color", -3697942));
   private final NumberSetting intensity = this.addSetting("intensity", new NumberSetting("Intensity", 1.0F, 0.05F, 1.0F).step(0.05F));
   private final NumberSetting speed = this.addSetting("speed", new NumberSetting("Speed", 1.0F, 0.1F, 4.0F).step(0.1F));
   private final NumberSetting patternSize = this.addSetting("pattern-size", new NumberSetting("Pattern Size", 1.0F, 0.25F, 4.0F).step(0.25F));
   private final CustomGlintModule.GlintTexture items = new CustomGlintModule.GlintTexture(class_918.field_43087);
   private final CustomGlintModule.GlintTexture armor = new CustomGlintModule.GlintTexture(class_918.field_43086);
   private double glintClock = Double.NaN;
   private long glintClockReal;

   public CustomGlintModule() {
      super("custom-glint", "Custom Glint", "Recolours the enchantment glint and changes its speed and pattern size.", ModuleCategory.VISUALS);
      this.itemColor.visibleWhen(() -> this.colorMode.is("Custom"));
      this.separateArmor.visibleWhen(() -> this.colorMode.is("Custom"));
      this.armorColor.visibleWhen(() -> this.colorMode.is("Custom") && this.separateArmor.get());
   }

   @Override
   protected void onEnable() {
      this.glintClock = Double.NaN;
      active = this;
      this.on(TickEvent.TYPE, event -> this.refresh());
   }

   @Override
   protected void onDisable() {
      active = null;
      class_310 mc = class_310.method_1551();
      if (mc.method_18854()) {
         this.restore();
      } else {
         mc.execute(() -> {
            if (active == null) {
               this.restore();
            }
         });
      }
   }

   public long glintMillis(long realMillis) {
      if (Double.isNaN(this.glintClock)) {
         this.glintClock = realMillis;
      } else {
         this.glintClock = this.glintClock + (double)Math.max(0L, realMillis - this.glintClockReal) * this.speed.get();
      }

      this.glintClockReal = realMillis;
      return (long)this.glintClock;
   }

   public float uvScaleMultiplier() {
      return 1.0F / this.patternSize.get();
   }

   private void refresh() {
      class_310 mc = class_310.method_1551();
      if (mc.field_1687 != null && mc.method_18506() == null && RenderSystem.tryGetDevice() != null) {
         int itemRgb = this.colorMode.is("Accent") ? CosmeticAccent.argb() & 16777215 : this.itemColor.rgb();
         int armorRgb = this.colorMode.is("Custom") && this.separateArmor.get() ? this.armorColor.rgb() : itemRgb;
         float strength = this.intensity.get();
         this.items.tint(mc, itemRgb, strength);
         this.armor.tint(mc, armorRgb, strength);
      }
   }

   private void restore() {
      class_310 mc = class_310.method_1551();
      this.items.restore(mc);
      this.armor.restore(mc);
   }

   @Environment(EnvType.CLIENT)
   private static final class GlintTexture {
      private final class_2960 id;
      private int width;
      private int height;
      private int[] original;
      private int[] pattern;
      private GpuTexture written;
      private long writtenKey;
      private GpuTexture unusable;

      GlintTexture(class_2960 id) {
         this.id = id;
      }

      void tint(class_310 mc, int rgb, float intensity) {
         GpuTexture gpu = this.gpuTexture(mc);
         if (gpu != null && !gpu.isClosed() && gpu != this.unusable) {
            long key = (long)(rgb & 16777215) << 8 | Math.round(intensity * 255.0F);
            if (gpu != this.written || key != this.writtenKey) {
               if (gpu != this.written) {
                  this.written = null;
                  if ((gpu.usage() & 1) == 0 || !this.readShipped(mc, gpu)) {
                     this.unusable = gpu;
                     return;
                  }
               }

               if (!this.upload(gpu, this.tinted(rgb, intensity))) {
                  this.unusable = gpu;
               } else {
                  this.written = gpu;
                  this.writtenKey = key;
               }
            }
         }
      }

      void restore(class_310 mc) {
         GpuTexture gpu = this.written != null ? this.gpuTexture(mc) : null;
         if (gpu != null && gpu == this.written && !gpu.isClosed() && this.original != null) {
            this.upload(gpu, this.original);
         }

         this.written = null;
         this.unusable = null;
         this.original = null;
         this.pattern = null;
      }

      private GpuTexture gpuTexture(class_310 mc) {
         try {
            class_1044 texture = mc.method_1531().method_4619(this.id);
            return texture == null ? null : texture.method_68004();
         } catch (IllegalStateException notLoaded) {
            return null;
         }
      }

      private boolean readShipped(class_310 mc, GpuTexture gpu) {
         Optional<class_3298> resource = mc.method_1478().method_14486(this.id);
         if (resource.isEmpty()) {
            return false;
         }

         try (InputStream in = resource.get().method_14482()) {
            class_1011 image = class_1011.method_4309(in);

            boolean var19;
            label117: {
               int y;
               try {
                  int w = image.method_4307();
                  int h = image.method_4323();
                  if (w != gpu.getWidth(0) || h != gpu.getHeight(0)) {
                     var19 = false;
                     break label117;
                  }

                  int[] pixels = new int[w * h];
                  int[] luma = new int[w * h];
                  y = 0;
                  int i = 0;

                  while (y < h) {
                     for (int x = 0; x < w; i++) {
                        int argb = image.method_61940(x, y);
                        pixels[i] = argb;
                        luma[i] = Math.max(class_9848.method_61327(argb), Math.max(class_9848.method_61329(argb), class_9848.method_61331(argb)));
                        x++;
                     }

                     y++;
                  }

                  this.width = w;
                  this.height = h;
                  this.original = pixels;
                  this.pattern = luma;
                  y = 1;
               } catch (Throwable var16) {
                  if (image != null) {
                     try {
                        image.close();
                     } catch (Throwable var15) {
                        var16.addSuppressed(var15);
                     }
                  }

                  throw var16;
               }

               if (image != null) {
                  image.close();
               }

               return (boolean)y;
            }

            if (image != null) {
               image.close();
            }

            return var19;
         } catch (IOException | RuntimeException e) {
            CustomGlintModule.LOGGER.warn("[Anubis] Custom Glint could not read {}", this.id, e);
            return false;
         }
      }

      private int[] tinted(int rgb, float intensity) {
         float r = class_9848.method_61327(rgb) * intensity / 255.0F;
         float g = class_9848.method_61329(rgb) * intensity / 255.0F;
         float b = class_9848.method_61331(rgb) * intensity / 255.0F;
         int[] out = new int[this.pattern.length];

         for (int i = 0; i < out.length; i++) {
            int l = this.pattern[i];
            out[i] = class_9848.method_61324(class_9848.method_61320(this.original[i]), Math.round(l * r), Math.round(l * g), Math.round(l * b));
         }

         return out;
      }

      private boolean upload(GpuTexture gpu, int[] argb) {
         try {
            class_1011 image = new class_1011(this.width, this.height, false);

            int y;
            try {
               y = 0;
               int i = 0;

               while (y < this.height) {
                  for (int x = 0; x < this.width; i++) {
                     image.method_61941(x, y, argb[i]);
                     x++;
                  }

                  y++;
               }

               RenderSystem.getDevice().createCommandEncoder().writeToTexture(gpu, image);
               y = 1;
            } catch (Throwable var8) {
               try {
                  image.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }

               throw var8;
            }

            image.close();
            return (boolean)y;
         } catch (RuntimeException e) {
            CustomGlintModule.LOGGER.warn("[Anubis] Custom Glint could not write {}", this.id, e);
            return false;
         }
      }
   }
}
