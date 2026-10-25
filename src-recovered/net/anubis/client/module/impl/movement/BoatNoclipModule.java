package net.anubis.client.module.impl.movement;

import java.util.ArrayList;
import java.util.List;
import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.PacketSendEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.misc.FreecamModule;
import net.anubis.client.module.impl.movement.entitycontrol.EntityControlVehicleHook;
import net.anubis.client.module.impl.movement.entitycontrol.VehicleAntiKick;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10255;
import net.minecraft.class_1297;
import net.minecraft.class_1301;
import net.minecraft.class_1657;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_259;
import net.minecraft.class_265;
import net.minecraft.class_2680;
import net.minecraft.class_2833;
import net.minecraft.class_2836;
import net.minecraft.class_310;
import net.minecraft.class_3486;
import net.minecraft.class_3532;
import net.minecraft.class_3726;
import net.minecraft.class_4076;
import net.minecraft.class_634;
import net.minecraft.class_638;
import net.minecraft.class_746;
import net.minecraft.class_2338.class_2339;

@Environment(EnvType.CLIENT)
public final class BoatNoclipModule extends ClientModule {
   public static volatile BoatNoclipModule active;
   private static final double MAX_MISS = 0.24;
   private static final double MAX_PACKET_LENGTH = 1.0;
   private static final int MAX_PACKETS_PER_TICK = 16;
   private static final double MAX_SERVER_LEAD = 19.0;
   private static final double MAX_UNCONFIRMED = 28.5;
   private static final int STALE_SERVER_TICKS = 40;
   private static final double MIN_BURST = 2.0;
   private static final double MOUNT_BURST = 4.0;
   private static final double BURST_RECOVERY = 0.025;
   private static final double CAUTION_STEP = 0.5;
   private static final double CAUTION_GROWTH = 1.25;
   private static final double LAVA_MARGIN = 3.0;
   private final NumberSetting speed = this.addSetting("speed", new NumberSetting("Speed", 10.0F, 0.0F, 320.0F).step(0.5F));
   private final NumberSetting speedInsideBlocks = this.addSetting(
      "speed-inside-blocks", new NumberSetting("Speed Inside Blocks", 5.0F, 0.0F, 320.0F).step(0.5F)
   );
   private final NumberSetting verticalSpeed = this.addSetting("vertical-speed", new NumberSetting("Vertical Speed", 6.0F, 0.0F, 320.0F).step(0.5F));
   private final NumberSetting fallSpeed = this.addSetting("fall-speed", new NumberSetting("Fall Speed", 0.0F, 0.0F, 10.0F).step(0.1F));
   private final BooleanSetting lavaProtection = this.addSetting("lava-protection", new BooleanSetting("Lava Protection", true));
   private final BooleanSetting antiKick = this.addSetting("anti-fly-kick", new BooleanSetting("Anti Fly Kick", true));
   private final NumberSetting delay = this.addSetting("delay", new NumberSetting("Delay", 40.0F, 1.0F, 80.0F).step(1.0F));
   private final VehicleAntiKick antiKicker = new VehicleAntiKick();
   private class_10255 noclipBoat;
   private class_243 serverPos;
   private class_243 lastBase;
   private int staleTicks;
   private double cautionCap = Double.POSITIVE_INFINITY;
   private boolean sendingStops;
   private int sinceBase;
   private double burst = 4.0;
   private double lastMoveLength;

   public BoatNoclipModule() {
      super("boat-noclip", "Boat Noclip", "Fly your boat through blocks and entities.", ModuleCategory.MOVEMENT);
      this.delay.visibleWhen(this.antiKick::get);
      this.antiKick.onChange(this.antiKicker::forget);
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      BoatFlyModule fly = ModuleRegistry.get(BoatFlyModule.class);
      if (fly != null) {
         fly.setEnabled(false);
      }

      this.antiKicker.start(this.delay.getInt());
      active = this;
      EntityControlVehicleHook.refresh();
      this.on(TickEvent.TYPE, Stage.PRE, this::onPreTick);
      this.on(PacketSendEvent.TYPE, this::onPacketSend);
   }

   @Override
   protected void onDisable() {
      active = null;
      EntityControlVehicleHook.refresh();
      if (FreecamModule.active == null) {
         class_310.method_1551().field_1730 = true;
      }

      this.antiKicker.stop();
      this.releaseBoat();
   }

   private void onPreTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (this.noclipBoat != null && (player == null || player.method_5854() != this.noclipBoat)) {
         this.releaseBoat();
      }

      this.antiKicker.tick();
      this.updateCulling(mc);
   }

   private void updateCulling(class_310 mc) {
      if (FreecamModule.active == null && mc.field_1687 != null) {
         boolean sealed = this.noclipBoat != null && mc.field_1687.method_8320(mc.field_1773.method_19418().method_19328()).method_26216();
         mc.field_1730 = !sealed;
      }
   }

   private void onPacketSend(PacketSendEvent event) {
      if (this.antiKick.get() && event.packet instanceof class_2833 packet) {
         if (this.sendingStops) {
            this.antiKicker.observe(packet);
         } else if (this.antiKicker.onPacketSend(packet, this.delay.getInt())) {
            event.setCancelled(true);
         }
      }
   }

   public void onVehicleSetback() {
      this.antiKicker.forget();
      if (this.noclipBoat != null) {
         this.serverPos = this.noclipBoat.method_73189();
      }

      this.cautionCap = 0.5;
   }

   private void releaseBoat() {
      if (this.noclipBoat != null) {
         this.noclipBoat.field_5960 = false;
      }

      this.noclipBoat = null;
      this.serverPos = null;
      this.lastBase = null;
      this.sinceBase = 0;
      this.burst = 4.0;
   }

   public class_243 boatMovement(class_10255 boat, class_746 player) {
      if (this.noclipBoat != boat) {
         this.releaseBoat();
         this.noclipBoat = boat;
      }

      boat.field_5960 = true;
      forceServerBroadcast(boat);
      double budget = this.serverBudget(boat);
      if (this.antiKick.get() && this.antiKicker.isHolding()) {
         return class_243.field_1353;
      }

      budget = Math.min(budget, this.cautionCap);
      this.cautionCap *= 1.25;
      double bps = isInsideBlock(boat, player) ? this.speedInsideBlocks.get() : this.speed.get();
      class_243 horizontal = EntityControlVehicleHook.horizontalVelocity(player, bps);
      class_310 mc = class_310.method_1551();
      double velY = 0.0;
      if (EntityControlVehicleHook.isPhysicallyDown(mc.field_1690.field_1903)) {
         velY += this.verticalSpeed.get() / 20.0;
      }

      if (EntityControlVehicleHook.isPhysicallyDown(mc.field_1690.field_1867)) {
         velY -= this.verticalSpeed.get() / 20.0;
      } else {
         velY -= this.fallSpeed.get() / 20.0;
      }

      class_243 move = new class_243(horizontal.field_1352, velY, horizontal.field_1350);
      double length = move.method_1033();
      if (length > budget) {
         move = move.method_1021(budget / length);
      }

      class_243 planned = this.planMove(boat, move);
      this.lastMoveLength = planned.method_1033();
      return planned;
   }

   private static void forceServerBroadcast(class_10255 boat) {
      class_634 connection = class_310.method_1551().method_1562();
      if (connection != null) {
         connection.method_52787(new class_2836(!boat.method_64491(0), boat.method_64491(1)));
      }
   }

   private double serverBudget(class_10255 boat) {
      class_243 base = boat.method_43389().method_60933();
      if (!base.equals(this.lastBase)) {
         this.burst = Math.max(this.burst, this.sinceBase + 1);
         this.sinceBase = 0;
         this.lastBase = base;
         this.serverPos = base;
         this.staleTicks = 0;
      } else {
         if (this.lastMoveLength > 0.01) {
            this.sinceBase++;
         }

         if (++this.staleTicks >= 40) {
            this.serverPos = boat.method_73189();
         }
      }

      this.burst = Math.max(2.0, this.burst - 0.025);
      if (this.serverPos == null) {
         this.serverPos = boat.method_73189();
      }

      double lead = boat.method_73189().method_1022(this.serverPos);
      return Math.max(0.0, Math.min(19.0 / this.burst, 28.5 - lead));
   }

   private class_243 planMove(class_10255 boat, class_243 move) {
      class_243 start = boat.method_73189();
      class_243 pos = start;
      class_243 left = move;
      List<class_238> lava = this.lavaProtection.get() ? nearbyLava(boat, move) : List.of();
      List<class_243> stops = new ArrayList<>();

      while (stops.size() < 16 && left.method_1027() > 1.0E-8) {
         class_243 hop = left.method_1021(Math.min(1.0, 1.0 / left.method_1033()));
         double fraction = allowedFraction(boat, pos, hop);
         if (fraction <= 0.0) {
            break;
         }

         class_243 step = hop.method_1021(fraction);
         if (reachesUnloadedChunk(boat, pos, step) || !lava.isEmpty() && approachesLava(boat, pos, step, lava)) {
            break;
         }

         pos = pos.method_1019(step);
         left = left.method_1020(step);
         stops.add(pos);
      }

      class_634 connection = class_310.method_1551().method_1562();
      if (connection != null) {
         this.sendingStops = true;

         try {
            for (int i = 0; i < stops.size() - 1; i++) {
               connection.method_52787(new class_2833(stops.get(i), boat.method_36454(), boat.method_36455(), false));
            }
         } finally {
            this.sendingStops = false;
         }
      }

      return pos.method_1020(start);
   }

   private static List<class_238> nearbyLava(class_10255 boat, class_243 move) {
      class_238 area = boat.method_5829().method_18804(move).method_1014(4.0);
      List<class_238> lava = new ArrayList<>();

      for (class_2338 pos : class_2338.method_10094(
         class_3532.method_15357(area.field_1323),
         class_3532.method_15357(area.field_1322),
         class_3532.method_15357(area.field_1321),
         class_3532.method_15357(area.field_1320),
         class_3532.method_15357(area.field_1325),
         class_3532.method_15357(area.field_1324)
      )) {
         if (boat.method_73183().method_8316(pos).method_15767(class_3486.field_15518)) {
            lava.add(new class_238(pos));
         }
      }

      return lava;
   }

   private static boolean approachesLava(class_10255 boat, class_243 from, class_243 step, List<class_238> lava) {
      class_238 box = boat.method_5829().method_997(from.method_1020(boat.method_73189()));
      double after = distanceTo(box.method_997(step), lava);
      return after < 3.0 && after < distanceTo(box, lava);
   }

   private static double distanceTo(class_238 box, List<class_238> blocks) {
      double best = Double.MAX_VALUE;

      for (class_238 block : blocks) {
         double dx = Math.max(0.0, Math.max(block.field_1323 - box.field_1320, box.field_1323 - block.field_1320));
         double dy = Math.max(0.0, Math.max(block.field_1322 - box.field_1325, box.field_1322 - block.field_1325));
         double dz = Math.max(0.0, Math.max(block.field_1321 - box.field_1324, box.field_1321 - block.field_1324));
         best = Math.min(best, dx * dx + dy * dy + dz * dz);
      }

      return Math.sqrt(best);
   }

   private static boolean reachesUnloadedChunk(class_10255 boat, class_243 from, class_243 step) {
      class_238 box = boat.method_5829().method_997(from.method_1020(boat.method_73189())).method_18804(step);

      for (int x = class_4076.method_42615(box.field_1323); x <= class_4076.method_42615(box.field_1320); x++) {
         for (int z = class_4076.method_42615(box.field_1321); z <= class_4076.method_42615(box.field_1324); z++) {
            if (!boat.method_73183().method_8398().method_12123(x, z)) {
               return true;
            }
         }
      }

      return false;
   }

   private static double allowedFraction(class_10255 boat, class_243 from, class_243 move) {
      class_238 box = boat.method_5829().method_997(from.method_1020(boat.method_73189()));
      List<class_265> entities = serverEntityShapes(boat, box.method_18804(move));
      if (replayMiss(boat, box, move, entities) <= 0.24) {
         return 1.0;
      }

      double lo = 0.0;
      double hi = 1.0;

      for (int i = 0; i < 12; i++) {
         double mid = (lo + hi) / 2.0;
         if (replayMiss(boat, box, move.method_1021(mid), entities) <= 0.24) {
            lo = mid;
         } else {
            hi = mid;
         }
      }

      return lo;
   }

   private static double replayMiss(class_10255 boat, class_238 box, class_243 move, List<class_265> entities) {
      class_243 replay = class_1297.method_20736(boat, move, box, boat.method_73183(), entities);
      return Math.hypot(move.field_1352 - replay.field_1352, move.field_1350 - replay.field_1350);
   }

   private static List<class_265> serverEntityShapes(class_10255 boat, class_238 area) {
      List<class_265> shapes = new ArrayList<>();

      for (class_1297 other : boat.method_73183().method_8333(boat, area.method_1014(1.0E-7), class_1301.field_6155)) {
         if (class_10255.method_64486(boat, other)) {
            shapes.add(class_259.method_1078(other.method_5829()));
         }
      }

      return shapes;
   }

   public static boolean hidesWallOverlay(class_1657 player) {
      return active != null && player.method_5854() instanceof class_10255;
   }

   private static boolean isInsideBlock(class_10255 boat, class_746 player) {
      class_638 level = class_310.method_1551().field_1687;
      if (level == null) {
         return false;
      }

      class_238 boatBox = boat.method_5829();
      class_238 box = boatBox.method_1002(0.0, 0.05, 0.0).method_1009(0.5, 0.0, 0.5);
      int maxX = class_3532.method_15357(box.field_1320);
      int maxY = class_3532.method_15357(box.field_1325);
      int maxZ = class_3532.method_15357(box.field_1324);
      class_2339 pos = new class_2339();

      for (int x = class_3532.method_15357(box.field_1323); x <= maxX; x++) {
         for (int y = class_3532.method_15357(box.field_1322); y <= maxY; y++) {
            for (int z = class_3532.method_15357(box.field_1321); z <= maxZ; z++) {
               class_2680 state = level.method_8320(pos.method_10103(x, y, z));
               if (!state.method_26215()) {
                  class_265 shape = state.method_26194(level, pos, class_3726.method_16194());
                  if (!shape.method_1110() && boatBox.method_994(shape.method_1107().method_996(pos))) {
                     return true;
                  }
               }
            }
         }
      }

      for (class_1297 other : level.method_8335(boat, box)) {
         if (other != player && !other.method_5626(boat) && !boat.method_5626(other) && other.method_5805() && other.method_5829().method_994(box)) {
            return true;
         }
      }

      return false;
   }
}
