package net.anubis.client.module.impl.combat;

import net.anubis.client.chat.ModuleChat;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_310;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class FriendGuardModule extends ClientModule {
   private static final long HINT_COOLDOWN_MS = 1500L;
   private static volatile FriendGuardModule active;
   private long lastHintAt;

   public FriendGuardModule() {
      super("friend-guard", "Friend Guard", "Stops you from hitting players on your friends list.", ModuleCategory.COMBAT);
      this.usesNotifications();
   }

   @Override
   protected void onEnable() {
      this.lastHintAt = 0L;
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
      this.lastHintAt = 0L;
   }

   public static boolean intercepts(class_1657 attacker, class_1297 target) {
      FriendGuardModule guard = active;
      if (!(guard != null && target instanceof class_1657 friend) || friend == attacker) {
         return false;
      } else if (attacker == class_310.method_1551().field_1724 && MeleeAttacks.isFriend(friend)) {
         guard.hint(friend);
         return true;
      } else {
         return false;
      }
   }

   public static boolean interceptsStab(class_746 player) {
      FriendGuardModule guard = active;
      if (guard != null && player != null) {
         class_1297 friend = MeleeAttacks.stabTarget(player, entity -> entity instanceof class_1657 other && MeleeAttacks.isFriend(other));
         if (friend == null) {
            return false;
         }

         guard.hint((class_1657)friend);
         return true;
      } else {
         return false;
      }
   }

   private void hint(class_1657 friend) {
      if (this.notificationsOn()) {
         long now = System.currentTimeMillis();
         if (now - this.lastHintAt >= 1500L) {
            this.lastHintAt = now;
            ModuleChat.actionBar(this, ModuleChat.body().text("Held back your hit on ").name(friend.method_7334().name()));
         }
      }
   }
}
