package net.anubis.client.module.impl.movement;

import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.misc.FreecamModule;
import net.anubis.client.module.impl.render.FreelookModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1301;
import net.minecraft.class_1675;
import net.minecraft.class_1747;
import net.minecraft.class_1839;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_238;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_265;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_3966;
import net.minecraft.class_4184;
import net.minecraft.class_638;
import net.minecraft.class_746;
import net.minecraft.class_2350.class_2351;
import net.minecraft.class_239.class_240;
import net.minecraft.class_2828.class_2829;
import net.minecraft.class_2828.class_5911;
import net.minecraft.class_3959.class_242;
import net.minecraft.class_3959.class_3960;

@Environment(EnvType.CLIENT)
public final class ClickTpModule extends ClientModule {
   private static final double RAYCAST_DISTANCE = 210.0;
   private static final double BLOCKS_PER_PACKET = 10.0;
   private static final int MAX_PADDING_PACKETS = 19;

   public ClickTpModule() {
      super("click-tp", "Click TP", "Teleports you to the block you click on.", ModuleCategory.MOVEMENT);
   }

   @Override
   protected void onEnable() {
      this.on(TickEvent.TYPE, Stage.POST, this::onPostTick);
   }

   private void onPostTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      class_638 level = mc.field_1687;
      if (player != null && level != null) {
         if (player.method_31548().method_7391().method_7976() == class_1839.field_8952) {
            if (mc.field_1690.field_1904.method_1434()) {
               class_4184 camera = mc.field_1773.method_19418();
               class_243 cameraPos = camera.method_71156();
               class_243 direction = class_243.method_1030(camera.method_19329(), camera.method_19330());
               FreecamModule freecam = ModuleRegistry.get(FreecamModule.class);
               boolean freecamOn = freecam != null && freecam.isEnabled();
               class_239 crosshair = freecamOn ? pickFromCamera(player, level, cameraPos, direction) : mc.field_1765;
               if (crosshair != null) {
                  if (crosshair.method_17783() == class_240.field_1331
                     && player.method_7287(((class_3966)crosshair).method_17782(), class_1268.field_5808) != class_1269.field_5811) {
                     return;
                  }

                  if (crosshair.method_17783() == class_240.field_1332 && player.method_6047().method_7909() instanceof class_1747) {
                     return;
                  }
               }

               class_3965 hit = level.method_17742(
                  new class_3959(cameraPos, cameraPos.method_1019(direction.method_1021(210.0)), class_3960.field_17559, class_242.field_1348, player)
               );
               if (hit.method_17783() == class_240.field_1332) {
                  if (hit.method_17781() && !freecamOn) {
                     FreelookModule freelook = ModuleRegistry.get(FreelookModule.class);
                     if (freelook != null && freelook.isEnabled()) {
                        return;
                     }
                  }

                  class_2338 pos = hit.method_17777();
                  class_2350 side = hit.method_17780();
                  if (level.method_8320(pos).method_55781(level, player, hit) == class_1269.field_5811) {
                     class_2680 state = level.method_8320(pos);
                     class_265 shape = state.method_26220(level, pos);
                     if (shape.method_1110()) {
                        shape = state.method_26218(level, pos);
                     }

                     double height = shape.method_1110() ? 1.0 : shape.method_1105(class_2351.field_11052);
                     class_243 newPos = new class_243(
                        pos.method_10263() + 0.5 + side.method_10148(), pos.method_10264() + height, pos.method_10260() + 0.5 + side.method_10165()
                     );
                     int packetsRequired = (int)Math.ceil(player.method_73189().method_1022(newPos) / 10.0) - 1;
                     if (packetsRequired > 19) {
                        packetsRequired = 0;
                     }

                     for (int i = 0; i < packetsRequired; i++) {
                        player.field_3944.method_52787(new class_5911(true, true));
                     }

                     player.field_3944.method_52787(new class_2829(newPos.field_1352, newPos.field_1351, newPos.field_1350, true, true));
                     player.method_33574(newPos);
                  }
               }
            }
         }
      }
   }

   private static class_239 pickFromCamera(class_746 player, class_638 level, class_243 eye, class_243 dir) {
      double blockRange = player.method_55754();
      double entityRange = player.method_55755();
      double reach = Math.max(blockRange, entityRange);
      double reachSq = reach * reach;
      class_3965 blockHit = level.method_17742(
         new class_3959(eye, eye.method_1019(dir.method_1021(reach)), class_3960.field_17559, class_242.field_1348, player)
      );
      double blockDistSq = blockHit.method_17784().method_1025(eye);
      if (blockHit.method_17783() != class_240.field_1333) {
         reachSq = blockDistSq;
         reach = Math.sqrt(blockDistSq);
      }

      class_243 end = eye.method_1019(dir.method_1021(reach));
      class_238 searchBox = new class_238(eye, end).method_1014(1.0);
      class_3966 entityHit = class_1675.method_18075(player, eye, end, searchBox, class_1301.field_52443, reachSq);
      if (entityHit != null && entityHit.method_17784().method_1025(eye) < blockDistSq) {
         return entityHit.method_17784().method_24802(eye, entityRange) ? entityHit : null;
      } else {
         return blockHit.method_17784().method_24802(eye, blockRange) ? blockHit : null;
      }
   }
}
