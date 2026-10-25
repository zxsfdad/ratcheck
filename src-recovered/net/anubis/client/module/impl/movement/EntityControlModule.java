package net.anubis.client.module.impl.movement;

import net.anubis.client.event.impl.PacketReceiveNettyEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.movement.entitycontrol.EntityControlVehicleHook;
import net.anubis.client.module.setting.BooleanSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10255;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_2692;

@Environment(EnvType.CLIENT)
public final class EntityControlModule extends ClientModule {
   public static volatile EntityControlModule active;
   private final BooleanSetting boats = this.addSetting("boats", new BooleanSetting("Boats", true));
   private final BooleanSetting horses = this.addSetting("horses", new BooleanSetting("Horses", true));
   private final BooleanSetting camels = this.addSetting("camels", new BooleanSetting("Camels", true));
   private final BooleanSetting pigs = this.addSetting("pigs", new BooleanSetting("Pigs", true));
   private final BooleanSetting striders = this.addSetting("striders", new BooleanSetting("Striders", true));
   private final BooleanSetting nautiluses = this.addSetting("nautiluses", new BooleanSetting("Nautiluses", true));
   private final BooleanSetting happyGhasts = this.addSetting("happy-ghasts", new BooleanSetting("Happy Ghasts", true));
   private final BooleanSetting spoofSaddle = this.addSetting("spoof-saddle", new BooleanSetting("Spoof Saddle", false));
   private final BooleanSetting maxJump = this.addSetting("max-jump", new BooleanSetting("Max Jump", true));
   private final BooleanSetting lockYaw = this.addSetting("lock-yaw", new BooleanSetting("Lock Yaw", true));
   private final BooleanSetting cancelServerPackets = this.addSetting("cancel-server-packets", new BooleanSetting("Cancel Server Packets", false));

   public EntityControlModule() {
      super("entity-control", "Entity Control", "Lets you control rideable entities without a saddle.", ModuleCategory.MOVEMENT);
   }

   @Override
   protected void onEnable() {
      active = this;
      EntityControlVehicleHook.refresh();
      this.on(PacketReceiveNettyEvent.TYPE, this::onPacketReceive);
   }

   @Override
   protected void onDisable() {
      active = null;
      EntityControlVehicleHook.refresh();
   }

   private void onPacketReceive(PacketReceiveNettyEvent event) {
      if (this.cancelServerPackets.get() && event.packet instanceof class_2692) {
         event.setCancelled(true);
      }
   }

   public static boolean spoofSaddle() {
      EntityControlModule module = active;
      return module != null && module.spoofSaddle.get();
   }

   public static boolean maxJump() {
      EntityControlModule module = active;
      return module != null && module.maxJump.get();
   }

   public boolean locksYaw(class_1297 vehicle) {
      return this.lockYaw.get() && this.isTarget(vehicle);
   }

   private boolean isTarget(class_1297 entity) {
      if (entity instanceof class_10255) {
         return this.boats.get();
      } else {
         class_1299<?> type = entity.method_5864();
         if (type == class_1299.field_6139
            || type == class_1299.field_6067
            || type == class_1299.field_6057
            || type == class_1299.field_6075
            || type == class_1299.field_6048) {
            return this.horses.get();
         } else if (type == class_1299.field_40116 || type == class_1299.field_64132) {
            return this.camels.get();
         } else if (type == class_1299.field_6093) {
            return this.pigs.get();
         } else if (type == class_1299.field_23214) {
            return this.striders.get();
         } else if (type == class_1299.field_63289 || type == class_1299.field_63290) {
            return this.nautiluses.get();
         } else {
            return type == class_1299.field_59668 ? this.happyGhasts.get() : false;
         }
      }
   }
}
