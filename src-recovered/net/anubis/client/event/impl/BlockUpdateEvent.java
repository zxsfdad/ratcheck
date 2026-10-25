package net.anubis.client.event.impl;

import net.anubis.client.event.Event;
import net.anubis.client.event.EventType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2338;
import net.minecraft.class_2680;

@Environment(EnvType.CLIENT)
public final class BlockUpdateEvent extends Event {
   public static final EventType<BlockUpdateEvent> TYPE = new EventType<>("BlockUpdateEvent");
   public class_2338 pos;
   public class_2680 oldState;
   public class_2680 newState;
   private static final BlockUpdateEvent INSTANCE = new BlockUpdateEvent();

   private BlockUpdateEvent() {
   }

   public static void dispatch(class_2338 pos, class_2680 oldState, class_2680 newState) {
      if (TYPE.hasPreListeners()) {
         INSTANCE.pos = pos;
         INSTANCE.oldState = oldState;
         INSTANCE.newState = newState;
         TYPE.dispatch(INSTANCE);
         INSTANCE.pos = null;
         INSTANCE.oldState = null;
         INSTANCE.newState = null;
      }
   }
}
