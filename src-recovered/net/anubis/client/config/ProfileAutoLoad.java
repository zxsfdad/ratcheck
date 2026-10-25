package net.anubis.client.config;

import java.lang.ref.WeakReference;
import java.util.List;
import net.anubis.client.event.impl.PacketAppliedEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.gui.ChallengeToast;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2535;
import net.minecraft.class_2561;
import net.minecraft.class_2678;
import net.minecraft.class_310;
import net.minecraft.class_3928;
import net.minecraft.class_634;
import net.minecraft.class_642;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class ProfileAutoLoad {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final Object OWNER = new Object();
   private static final long TOAST_WAIT_NANOS = 15000000000L;
   private static WeakReference<class_2535> handled = new WeakReference<>(null);
   private static String pendingTitle;
   private static String pendingMessage;
   private static long pendingSince;

   private ProfileAutoLoad() {
   }

   public static void install() {
      PacketAppliedEvent.TYPE.listen(OWNER, event -> {
         if (event.packet instanceof class_2678) {
            onJoin();
         }
      });
      TickEvent.TYPE.listen(OWNER, event -> showPendingToast());
   }

   private static void onJoin() {
      class_310 mc = class_310.method_1551();
      if (!mc.method_1542()) {
         class_634 listener = mc.method_1562();
         class_642 server = listener == null ? null : listener.method_45734();
         if (server != null && server.field_3761 != null) {
            class_2535 connection = listener.method_48296();
            if (connection != handled.get()) {
               handled = new WeakReference<>(connection);
               List<String> loaded = ConfigProfiles.getInstance().loadForServer(server.field_3761);
               if (!loaded.isEmpty()) {
                  LOGGER.info("[Anubis] Joined {} - loaded profile {}", server.field_3761, String.join(", ", loaded));
                  pendingTitle = loaded.size() == 1 ? "Profile loaded" : "Profiles loaded";
                  pendingMessage = String.join(", ", loaded);
                  pendingSince = System.nanoTime();
               }
            }
         }
      }
   }

   private static void showPendingToast() {
      if (pendingTitle != null) {
         class_310 mc = class_310.method_1551();
         boolean expired = System.nanoTime() - pendingSince > 15000000000L;
         if (expired || mc.field_1724 != null && !(mc.field_1755 instanceof class_3928)) {
            String title = pendingTitle;
            String message = pendingMessage;
            pendingTitle = null;
            pendingMessage = null;
            if (mc.field_1687 != null) {
               ChallengeToast.addOrUpdate(new class_1799(class_1802.field_8674), title, class_2561.method_43470(message), false);
            }
         }
      }
   }
}
