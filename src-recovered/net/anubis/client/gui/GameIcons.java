package net.anubis.client.gui;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1291;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_5321;
import net.minecraft.class_6880;

@Environment(EnvType.CLIENT)
final class GameIcons {
   private static final Map<class_2960, ThemeImages.Texture> TEXTURES = new HashMap<>();
   private static final Set<class_2960> MISSING = new HashSet<>();

   private GameIcons() {
   }

   static ThemeImages.Texture effect(class_6880<class_1291> effect) {
      class_2960 id = effect.method_40230().<class_2960>map(class_5321::method_29177).orElse(null);
      return id == null ? null : texture(class_2960.method_60655(id.method_12836(), "textures/mob_effect/" + id.method_12832() + ".png"));
   }

   static ThemeImages.Texture block(String name) {
      return texture(class_2960.method_60656("textures/block/" + name + ".png"));
   }

   private static ThemeImages.Texture texture(class_2960 path) {
      ThemeImages.Texture texture = TEXTURES.get(path);
      if (texture == null && !MISSING.contains(path)) {
         try (InputStream in = class_310.method_1551().method_1478().open(path)) {
            texture = ThemeImages.decode(in.readAllBytes(), path.toString(), true);
         } catch (IOException | RuntimeException e) {
            texture = null;
         }

         if (texture == null) {
            MISSING.add(path);
         } else {
            TEXTURES.put(path, texture);
         }

         return texture;
      } else {
         return texture;
      }
   }
}
