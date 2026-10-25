package net.anubis.client.module.impl.misc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import net.anubis.client.event.EventPriority;
import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.PacketReceiveNettyEvent;
import net.anubis.client.event.impl.PacketSendEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.movement.BoatFlyModule;
import net.anubis.client.module.impl.movement.BoatNoclipModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_10264;
import net.minecraft.class_1297;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2596;
import net.minecraft.class_2661;
import net.minecraft.class_2692;
import net.minecraft.class_2708;
import net.minecraft.class_2752;
import net.minecraft.class_2777;
import net.minecraft.class_2833;
import net.minecraft.class_310;
import net.minecraft.class_4076;
import net.minecraft.class_746;
import net.minecraft.class_8042;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class VehicleLogModule extends ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
   private static final double SERVER_BOX_DEFLATE = 0.0625;
   private BufferedWriter out;
   private long startNanos;
   private volatile int tick;
   private volatile int vehicleId = -1;
   private volatile class_243 lastSent;

   public VehicleLogModule() {
      super("vehicle-log", "Vehicle Log", "Writes vehicle packets and server corrections to a CSV file.", ModuleCategory.MISC);
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      Path dir = FabricLoader.getInstance().getConfigDir().resolve("anubis").resolve("vehicle-logs");

      try {
         Files.createDirectories(dir);
         Path file = dir.resolve("vehicle-" + STAMP.format(LocalDateTime.now()) + ".csv");
         this.out = Files.newBufferedWriter(file);
         this.out.write("t_ms,tick,dir,type,x,y,z,dx,dy,dz,info\n");
         LOGGER.info("Vehicle log: {}", file);
      } catch (IOException e) {
         LOGGER.error("Could not open vehicle log", e);
         this.closeLog();
         this.requestDisable();
         return;
      }

      this.startNanos = System.nanoTime();
      this.tick = 0;
      this.vehicleId = -1;
      this.lastSent = null;
      this.on(TickEvent.TYPE, Stage.PRE, this::onTick);
      this.on(PacketSendEvent.TYPE, Stage.PRE, EventPriority.LOWEST, this::onPacketSend);
      this.on(PacketReceiveNettyEvent.TYPE, this::onPacketReceive);
   }

   @Override
   protected void onDisable() {
      this.closeLog();
   }

   private void onTick(TickEvent event) {
      this.tick++;
      class_746 player = class_310.method_1551().field_1724;
      class_1297 vehicle = player != null ? player.method_5854() : null;
      int id = vehicle != null ? vehicle.method_5628() : -1;
      if (id != this.vehicleId) {
         this.vehicleId = id;
         this.lastSent = null;
         if (vehicle != null) {
            this.row("local", "mount", vehicle.method_73189(), "id=" + id + " " + vehicle.method_5864().method_5882());
         } else {
            this.row("local", "dismount", null, "");
         }
      }
   }

   private void onPacketSend(PacketSendEvent event) {
      if (!event.isCancelled() && event.packet instanceof class_2833 packet) {
         class_310 mc = class_310.method_1551();
         class_1297 vehicle = mc.field_1724 != null ? mc.field_1724.method_5854() : null;
         StringBuilder info = new StringBuilder("mode=").append(mode()).append(" ground=").append(packet.comp_3353());
         if (vehicle != null && mc.field_1687 != null) {
            class_238 box = vehicle.method_5829().method_997(packet.comp_3350().method_1020(vehicle.method_73189()));
            info.append(" colliding=").append(!mc.field_1687.method_8587(vehicle, box.method_1011(0.0625))).append(" noPhysics=").append(vehicle.field_5960);
            class_243 pos = packet.comp_3350();
            info.append(" loaded=")
               .append(mc.field_1687.method_2935().method_12123(class_4076.method_42615(pos.field_1352), class_4076.method_42615(pos.field_1350)));
         }

         this.row("out", "move_vehicle", packet.comp_3350(), info.toString());
         this.lastSent = packet.comp_3350();
      }
   }

   private void onPacketReceive(PacketReceiveNettyEvent event) {
      this.logIncoming(event.packet);
   }

   private void logIncoming(class_2596<?> packet) {
      if (packet instanceof class_8042 bundle) {
         for (class_2596<?> sub : bundle.method_48324()) {
            this.logIncoming(sub);
         }
      } else if (packet instanceof class_2692 move) {
         this.row("in", "move_vehicle", move.comp_3347(), "");
      } else if (packet instanceof class_2708 teleport) {
         this.row(
            "in", "player_position", teleport.comp_3228().comp_3148(), "teleport_id=" + teleport.comp_3133() + " relative=" + !teleport.comp_3229().isEmpty()
         );
      } else if (packet instanceof class_2777 teleport) {
         if (teleport.comp_3237() == this.vehicleId) {
            this.row("in", "teleport_entity", teleport.comp_3238().comp_3148(), "");
         }
      } else if (packet instanceof class_10264 sync) {
         if (sync.comp_3223() == this.vehicleId) {
            this.row("in", "position_sync", sync.comp_3224().comp_3148(), "");
         }
      } else if (packet instanceof class_2752 passengers) {
         if (passengers.method_11841() == this.vehicleId) {
            this.row("in", "set_passengers", null, "passengers=" + Arrays.toString(passengers.method_11840()));
         }
      } else if (packet instanceof class_2661 disconnect) {
         this.row("in", "disconnect", null, disconnect.comp_2325().getString().replace(',', ';').replace('\n', ' '));
      }
   }

   private static String mode() {
      if (BoatFlyModule.active != null) {
         return "fly";
      } else {
         return BoatNoclipModule.active != null ? "noclip" : "none";
      }
   }

   private synchronized void row(String dir, String type, class_243 pos, String info) {
      if (this.out != null) {
         StringBuilder line = new StringBuilder();
         line.append((System.nanoTime() - this.startNanos) / 1000000L).append(',').append(this.tick).append(',').append(dir).append(',').append(type);
         class_243 previous = this.lastSent;
         appendVec(line, pos);
         appendVec(line, pos != null && previous != null ? pos.method_1020(previous) : null);
         line.append(',').append(info).append('\n');

         try {
            this.out.write(line.toString());
            this.out.flush();
         } catch (IOException e) {
            LOGGER.error("Vehicle log write failed, closing it", e);
            this.closeLog();
         }
      }
   }

   private static void appendVec(StringBuilder line, class_243 vec) {
      if (vec == null) {
         line.append(",,,");
      } else {
         line.append(String.format(Locale.ROOT, ",%.5f,%.5f,%.5f", vec.field_1352, vec.field_1351, vec.field_1350));
      }
   }

   private synchronized void closeLog() {
      if (this.out != null) {
         try {
            this.out.close();
         } catch (IOException var2) {
         }

         this.out = null;
      }
   }
}
