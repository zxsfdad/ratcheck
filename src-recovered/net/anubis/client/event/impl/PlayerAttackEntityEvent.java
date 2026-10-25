package net.anubis.client.event.impl;

import net.anubis.client.event.CancellableEvent;
import net.anubis.client.event.CancellableEventType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;

@Environment(EnvType.CLIENT)
public final class PlayerAttackEntityEvent extends CancellableEvent {
   public static final CancellableEventType<PlayerAttackEntityEvent> TYPE = new CancellableEventType<>("PlayerAttackEntityEvent");
   public class_1297 target;
   private static final PlayerAttackEntityEvent INSTANCE = new PlayerAttackEntityEvent();

   private PlayerAttackEntityEvent() {
   }

   public static boolean dispatch(class_1297 target) {
      if (!TYPE.hasPreListeners()) {
         return false;
      }

      INSTANCE.target = target;
      INSTANCE.resetCancellation();
      TYPE.dispatch(INSTANCE);
      boolean cancelled = INSTANCE.isCancelled();
      INSTANCE.target = null;
      return cancelled;
   }
}
