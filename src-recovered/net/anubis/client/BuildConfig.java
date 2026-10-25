package net.anubis.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class BuildConfig {
   public static final boolean DEBUG = false;
   public static final String[] SIGN_CERT_PARTS = new String[]{"c0f62a59d9785", "f4d2f026cdd71", "f4ac272676dbd", "149e32473c262", "4711f3470e79"};

   private BuildConfig() {
   }
}
