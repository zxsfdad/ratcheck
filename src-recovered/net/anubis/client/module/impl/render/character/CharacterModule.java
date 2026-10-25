package net.anubis.client.module.impl.render.character;

import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.anubis.client.chat.ModuleChat;
import net.anubis.client.config.FriendStore;
import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.misc.FreecamModule;
import net.anubis.client.module.impl.render.PharaohHeaddressLayer;
import net.anubis.client.module.impl.render.PharaohHeaddressModule;
import net.anubis.client.module.impl.render.character.kit.CharacterDesign;
import net.anubis.client.module.impl.render.character.kit.CharacterModel;
import net.anubis.client.module.impl.render.character.kit.Detail;
import net.anubis.client.module.setting.ActionSetting;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10017;
import net.minecraft.class_10055;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_156;
import net.minecraft.class_1657;
import net.minecraft.class_310;
import net.minecraft.class_3887;
import net.minecraft.class_4507;
import net.minecraft.class_638;
import net.minecraft.class_746;
import net.minecraft.class_970;
import net.minecraft.class_972;
import net.minecraft.class_976;
import net.minecraft.class_978;
import net.minecraft.class_979;
import net.minecraft.class_983;
import net.minecraft.class_989;

@Environment(EnvType.CLIENT)
public final class CharacterModule extends ClientModule {
   private static final long FRIEND_REFRESH_NANOS = 1000000000L;
   private static final float MODEL_PIXEL = 0.05859375F;
   private static volatile CharacterModule active;
   private final ModeSetting character = this.addSetting("character", characterSetting());
   private final BooleanSetting friends = this.addSetting("friends", new BooleanSetting("On Friends", false));
   private final ModeSetting detail = this.addSetting(
      "detail", new ModeSetting("Detail", Detail.MEDIUM.label, Detail.LOW.label, Detail.MEDIUM.label, Detail.HIGH.label)
   );
   private final BooleanSetting heldItems = this.addSetting("held-items", new BooleanSetting("Held Items", false));
   private final Int2BooleanOpenHashMap friendCache = new Int2BooleanOpenHashMap();
   private long friendsCheckedAt;
   private int scanTicks;
   private WeakReference<class_638> level = new WeakReference<>(null);

   public CharacterModule() {
      super("character-3d", "3D Character", "Replaces your player model with a 3D character.", ModuleCategory.VISUALS);
      this.addSetting("characters-folder", new ActionSetting("Open Characters Folder", this::openFolder));
      this.character.onChange(this::prepare);
      this.detail.onChange(this::prepare);
   }

   private static ModeSetting characterSetting() {
      CharacterCatalog.refresh();
      List<String> names = CharacterCatalog.names();
      return new ModeSetting("Character", names.get(0), names.toArray(new String[0]));
   }

   private void refreshCharacters() {
      if (CharacterCatalog.refresh()) {
         this.character.setModes(CharacterCatalog.names());
      }
   }

   private void openFolder() {
      Path dir = CustomCharacters.folder();

      try {
         Files.createDirectories(dir);
      } catch (IOException e) {
         ModuleChat.send(this, ModuleChat.body().danger("Couldn't create ").text(dir.toString()));
         return;
      }

      this.refreshCharacters();
      class_156.method_668().method_60932(dir);
   }

   private void prepare() {
      if (this.isEnabled()) {
         CharacterCache.prepare(CharacterCatalog.byName(this.character.get()), this.effectiveDetail());
      }
   }

   private Detail effectiveDetail() {
      return Detail.byLabel(this.detail.get());
   }

   @Override
   protected void onEnable() {
      PharaohHeaddressModule headdress = ModuleRegistry.get(PharaohHeaddressModule.class);
      if (headdress != null) {
         headdress.setEnabled(false);
      }

      CharacterCache.listen();
      this.reset();
      this.refreshCharacters();
      this.prepare();
      active = this;
      this.on(TickEvent.TYPE, Stage.POST, event -> {
         this.watchLevel();
         if (++this.scanTicks % 40 == 0) {
            this.refreshCharacters();
         }
      });
   }

   @Override
   protected void onDisable() {
      if (active == this) {
         active = null;
      }

      this.reset();
      CharacterCache.clear();
   }

   private void reset() {
      this.friendCache.clear();
      CharacterMotion.clear();
      this.level = new WeakReference<>(class_310.method_1551().field_1687);
   }

   private void watchLevel() {
      class_638 current = class_310.method_1551().field_1687;
      if (this.level.get() != current) {
         this.reset();
         CharacterCache.releaseTextures();
      }
   }

   public static void extract(class_1297 entity, class_10055 state) {
      CharacterModule module = active;
      WornCharacter worn = module == null ? null : module.resolve(state);
      CharacterHolder holder = (CharacterHolder)state;
      holder.anubis$setCharacter(worn);
      if (worn != null) {
         class_1297 vehicle = entity.method_5854();
         holder.anubis$setSeat(vehicle == null ? 0.0F : (vehicle instanceof class_1309 ? 1.35F : 0.3F));
         if (state.field_53338 != null) {
            CharacterModel model = worn.model;
            float over = model.maxY * model.scale - 32.0F;
            if (over > 0.0F) {
               state.field_53338 = state.field_53338.method_1031(0.0, over * 0.05859375F * state.field_53453, 0.0);
            }
         }
      }
   }

   static void baking(String file) {
      CharacterModule module = active;
      if (module != null) {
         ModuleChat.send(module, ModuleChat.body().text("Preparing ").name(file).text(", big models take a few seconds the first time"));
      }
   }

   static void notice(String file, List<String> limbs) {
      CharacterModule module = active;
      if (module != null) {
         ModuleChat.Body body = ModuleChat.body().name(file);
         if (limbs.isEmpty()) {
            body.text(" has no skeleton the client recognises, so it moves as one piece");
         } else {
            body.text(" only moves its ").value(String.join(" and ", limbs)).text(", the rest of its skeleton wasn't recognised");
         }

         ModuleChat.send(module, body);
      }
   }

   static void failed(CharacterDesign design, Throwable failure) {
      CharacterModule module = active;
      if (module != null) {
         Throwable cause = failure instanceof UncheckedIOException && failure.getCause() != null ? failure.getCause() : failure;
         String reason = !(cause instanceof IOException) && !(cause instanceof IllegalStateException)
            ? "the file couldn't be read, see the log"
            : cause.getMessage();
         ModuleChat.send(module, ModuleChat.body().text("Couldn't load ").name(design.name()).text(": ").danger(reason));
      }
   }

   static WornCharacter worn(class_10017 state) {
      return state instanceof CharacterHolder holder ? holder.anubis$character() : null;
   }

   static float seat(class_10017 state) {
      return state instanceof CharacterHolder holder ? holder.anubis$seat() : 0.0F;
   }

   public static boolean hidesModel(class_10017 state) {
      return worn(state) != null;
   }

   public static boolean hidesLayer(class_3887<?, ?> layer, class_10017 state) {
      return worn(state) == null
         ? false
         : layer instanceof class_970
            || layer instanceof class_972
            || layer instanceof class_979
            || layer instanceof class_4507
            || layer instanceof class_976
            || layer instanceof class_983
            || layer instanceof class_978
            || layer instanceof PharaohHeaddressLayer
            || layer instanceof class_989 && !showsHeldItems();
   }

   private static boolean showsHeldItems() {
      CharacterModule module = active;
      return module != null && module.heldItems.get();
   }

   static WornCharacter firstPerson() {
      CharacterModule module = active;
      if (module == null) {
         return null;
      } else {
         class_746 self = class_310.method_1551().field_1724;
         if (self != null && !self.method_7325()) {
            WornCharacter worn = CharacterCache.ready(CharacterCatalog.byName(module.character.get()), module.effectiveDetail());
            return worn != null && worn.model.firstPersonArms ? worn : null;
         } else {
            return null;
         }
      }
   }

   private WornCharacter resolve(class_10055 state) {
      if (state.field_53542) {
         return null;
      }

      class_310 mc = class_310.method_1551();
      return !this.wears(mc, state) ? null : CharacterCache.ready(CharacterCatalog.byName(this.character.get()), this.effectiveDetail());
   }

   private boolean wears(class_310 mc, class_10055 state) {
      class_746 self = mc.field_1724;
      if (self != null && mc.field_1687 != null) {
         if (state.field_53528 != self.method_5628()) {
            return this.friends.get() && this.isFriend(mc, state.field_53528);
         } else {
            return FreecamModule.active == null && mc.field_1690.method_31044().method_31034() ? mc.field_1755 != null && !self.method_6113() : true;
         }
      } else {
         return false;
      }
   }

   private boolean isFriend(class_310 mc, int id) {
      long now = System.nanoTime();
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
}
