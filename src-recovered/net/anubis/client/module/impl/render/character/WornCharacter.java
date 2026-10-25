package net.anubis.client.module.impl.render.character;

import net.anubis.client.module.impl.render.character.kit.CharacterModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12249;
import net.minecraft.class_1921;
import net.minecraft.class_2960;

@Environment(EnvType.CLIENT)
public final class WornCharacter {
   final CharacterModel model;
   final class_1921 solid;
   final class_1921 decal;
   final class_1921 ghost;

   WornCharacter(CharacterModel model, class_2960 texture) {
      this.model = model;
      this.solid = class_12249.method_75982(texture);
      this.decal = class_12249.method_76000(texture);
      this.ghost = class_12249.method_75998(texture);
   }
}
