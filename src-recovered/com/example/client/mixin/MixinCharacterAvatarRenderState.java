package com.example.client.mixin;

import net.anubis.client.module.impl.render.character.CharacterHolder;
import net.anubis.client.module.impl.render.character.WornCharacter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10055;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Environment(EnvType.CLIENT)
@Mixin(class_10055.class)
public abstract class MixinCharacterAvatarRenderState implements CharacterHolder {
   @Unique
   private WornCharacter anubis$character;
   @Unique
   private float anubis$seat;

   @Override
   public WornCharacter anubis$character() {
      return this.anubis$character;
   }

   @Override
   public void anubis$setCharacter(WornCharacter character) {
      this.anubis$character = character;
   }

   @Override
   public float anubis$seat() {
      return this.anubis$seat;
   }

   @Override
   public void anubis$setSeat(float knee) {
      this.anubis$seat = knee;
   }
}
