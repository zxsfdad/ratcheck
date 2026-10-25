package net.anubis.client.module.impl.render;

import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import java.lang.ref.WeakReference;
import net.anubis.client.config.FriendStore;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.misc.FreecamModule;
import net.anubis.client.module.impl.render.character.CharacterModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10055;
import net.minecraft.class_1657;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_638;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
abstract class HeadCosmeticModule extends ClientModule {
   static volatile HeadCosmeticModule worn;
   static final String COLOR_THEME = "Theme";
   static final String COLOR_MANUAL = "Manual";
   private static final long EPOCH = System.nanoTime();
   private static final long LOOK_REFRESH_NANOS = 4000000L;
   private static final long FRIEND_REFRESH_NANOS = 1000000000L;
   private static final float GLINT_CYCLE_SECONDS = 3.6F;
   private static final float GLINT_STRENGTH = 0.6F;
   private PharaohMesh.Look look;
   private long lookAt;
   private final Int2BooleanOpenHashMap friendCache = new Int2BooleanOpenHashMap();
   private WeakReference<class_638> friendLevel = new WeakReference<>(null);
   private long friendsCheckedAt;

   HeadCosmeticModule(String id, String name, String description) {
      super(id, name, description, ModuleCategory.VISUALS);
   }

   abstract PharaohMesh headMesh(boolean var1);

   abstract PharaohMesh bodyMesh(boolean var1);

   abstract float scale();

   abstract int[] palette();

   abstract boolean glows();

   abstract boolean glints();

   abstract boolean onFriends();

   @Override
   protected void onEnable() {
      CharacterModule character = ModuleRegistry.get(CharacterModule.class);
      if (character != null) {
         character.setEnabled(false);
      }

      this.reset();
      worn = this;
   }

   @Override
   protected void onDisable() {
      if (worn == this) {
         worn = null;
      }

      this.reset();
   }

   void reset() {
      this.look = null;
      this.friendCache.clear();
      this.friendLevel = new WeakReference<>(null);
   }

   final boolean wears(class_10055 state) {
      class_310 mc = class_310.method_1551();
      class_746 self = mc.field_1724;
      if (self != null && mc.field_1687 != null) {
         return state.field_53528 == self.method_5628()
            ? !mc.field_1690.method_31044().method_31034() || mc.field_1755 != null || FreecamModule.active != null
            : this.onFriends() && this.isFriend(mc, state.field_53528);
      } else {
         return false;
      }
   }

   private boolean isFriend(class_310 mc, int id) {
      long now = System.nanoTime();
      if (this.friendLevel.get() != mc.field_1687) {
         this.friendLevel = new WeakReference<>(mc.field_1687);
         this.friendCache.clear();
      }

      if (now - this.friendsCheckedAt > 1000000000L) {
         this.friendCache.clear();
         this.friendsCheckedAt = now;
      }

      if (this.friendCache.containsKey(id)) {
         return this.friendCache.get(id);
      }

      boolean friend = mc.field_1687.method_8469(id) instanceof class_1657 player && FriendStore.getInstance().isFriend(player.method_7334().name());
      this.friendCache.put(id, friend);
      return friend;
   }

   final PharaohMesh.Look look() {
      long now = System.nanoTime();
      PharaohMesh.Look current = this.look;
      if (current != null && now - this.lookAt < 4000000L) {
         return current;
      }

      float seconds = (float)(now - EPOCH) / 1.0E9F;
      float phase = seconds % 3.6F / 3.6F;
      float sweep = (phase * 2.0F - 1.0F) * (float) Math.PI * 1.25F;
      float envelope = class_3532.method_15374(phase * (float) Math.PI);
      float gain = this.glints() ? 0.6F * envelope * envelope : 0.0F;
      float pulse = 0.9F + 0.1F * class_3532.method_15374(seconds * 2.2F);
      current = new PharaohMesh.Look(this.palette(), this.glows(), class_3532.method_15362(sweep), class_3532.method_15374(sweep), gain, pulse);
      this.look = current;
      this.lookAt = now;
      return current;
   }

   static int metal(int accent) {
      return shade(accent, 0.55F, 0.96F);
   }

   static int gem(int accent) {
      return shade(accent, 1.15F, 1.0F);
   }

   static int shade(int argb, float saturation, float value) {
      int r = argb >> 16 & 0xFF;
      int g = argb >> 8 & 0xFF;
      int b = argb & 0xFF;
      int max = Math.max(r, Math.max(g, b));
      int chroma = max - Math.min(r, Math.min(g, b));
      float hue = 0.0F;
      if (chroma > 0) {
         if (max == r) {
            hue = (float)(g - b) / chroma;
         } else if (max == g) {
            hue = 2.0F + (float)(b - r) / chroma;
         } else {
            hue = 4.0F + (float)(r - g) / chroma;
         }

         hue /= 6.0F;
         if (hue < 0.0F) {
            hue++;
         }
      }

      float s = max == 0 ? 0.0F : (float)chroma / max;
      return class_3532.method_60599(hue, Math.min(1.0F, s * saturation), Math.min(1.0F, value), 255);
   }
}
