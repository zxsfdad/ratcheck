package net.anubis.client.event.impl;

import net.anubis.client.event.Event;
import net.anubis.client.event.EventType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;

@Environment(EnvType.CLIENT)
public final class WorldEntityAddEvent extends Event {
   public static final EventType<WorldEntityAddEvent> TYPE = new EventType<>("WorldEntityAddEvent");
   public class_1297 entity;
   private static final WorldEntityAddEvent INSTANCE = new WorldEntityAddEvent();

   private WorldEntityAddEvent() {
   }

   public static void dispatch(class_1297 entity) {
      if (TYPE.hasPreListeners()) {
         INSTANCE.entity = entity;
         TYPE.dispatch(INSTANCE);
         INSTANCE.entity = null;
      }
   }
}
