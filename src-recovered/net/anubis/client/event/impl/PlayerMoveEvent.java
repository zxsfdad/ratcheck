package net.anubis.client.event.impl;

import java.util.function.Consumer;
import net.anubis.client.event.CancellableEvent;
import net.anubis.client.event.PooledCancellableEventType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;

@Environment(EnvType.CLIENT)
public final class PlayerMoveEvent extends CancellableEvent {
   public static final PooledCancellableEventType<PlayerMoveEvent> TYPE = new PooledCancellableEventType<>("PlayerMoveEvent", PlayerMoveEvent::new, 16);
   public double x;
   public double y;
   public double z;

   private PlayerMoveEvent() {
   }

   public static void move(class_243 movement, Consumer<class_243> original) {
      if (!TYPE.hasListeners()) {
         original.accept(movement);
      } else {
         PlayerMoveEvent event = TYPE.pool.acquire();
         event.x = movement.field_1352;
         event.y = movement.field_1351;
         event.z = movement.field_1350;
         event.resetCancellation();

         try {
            TYPE.dispatchPre(event);
            if (event.isCancelled()) {
               return;
            }

            if (Double.isFinite(event.x) && Double.isFinite(event.y) && Double.isFinite(event.z)) {
               class_243 changed = event.x == movement.field_1352 && event.y == movement.field_1351 && event.z == movement.field_1350
                  ? movement
                  : new class_243(event.x, event.y, event.z);
               original.accept(changed);
               TYPE.dispatchPost(event);
               return;
            }
         } finally {
            TYPE.pool.release(event);
         }
      }
   }
}
