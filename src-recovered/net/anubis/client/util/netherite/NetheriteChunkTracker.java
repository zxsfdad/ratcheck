package net.anubis.client.util.netherite;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2596;
import net.minecraft.class_2626;
import net.minecraft.class_2637;
import net.minecraft.class_2672;
import net.minecraft.class_2680;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_5321;
import net.minecraft.class_638;
import net.minecraft.class_7134;
import net.minecraft.class_7924;

@Environment(EnvType.CLIENT)
public final class NetheriteChunkTracker {
   static final int MAXIMUM_CANDIDATES = 512;
   private static final int ANCIENT_STATE_ID = class_2248.method_9507(class_2246.field_22109.method_9564());
   private static final class_5321<class_1937> DONUT_NETHER = class_5321.method_29179(class_7924.field_41223, class_2960.method_60655("worlds", "smp_nether"));
   private static final Set<NetheriteChunkTracker.Candidate> CANDIDATES = new LinkedHashSet<>();
   private static WeakReference<class_638> lastLevel = new WeakReference<>(null);
   private static class_5321<class_1937> lastDimension;
   private static NetheriteChunkTracker.CandidateListener listener;
   private static NetheriteChunkTracker.RevealListener revealListener;

   private NetheriteChunkTracker() {
   }

   public static void onPacketApplied(class_2596<?> packet) {
      class_638 level = class_310.method_1551().field_1687;
      if (level != null && isNether(level)) {
         if (packet instanceof class_2672 chunk) {
            trackLevel(level);

            for (int sectionY : HiddenDebrisScanner.findHiddenSections(chunk.method_38598(), level.method_32890(), level.method_32891(), ANCIENT_STATE_ID)) {
               add(new NetheriteChunkTracker.Candidate(chunk.method_11523(), chunk.method_11524(), sectionY));
            }
         } else if (packet instanceof class_2626 update) {
            onBlockUpdate(update.method_11309(), update.method_11308());
         } else if (packet instanceof class_2637 sectionUpdate) {
            sectionUpdate.method_30621(NetheriteChunkTracker::onBlockUpdate);
         }
      }
   }

   private static void onBlockUpdate(class_2338 pos, class_2680 state) {
      if (class_2248.method_9507(state) == ANCIENT_STATE_ID) {
         if (revealListener != null) {
            revealListener.onRevealed(pos.method_10062());
         }

         CANDIDATES.remove(new NetheriteChunkTracker.Candidate(pos.method_10263() >> 4, pos.method_10260() >> 4, pos.method_10264() >> 4));
      }
   }

   private static void add(NetheriteChunkTracker.Candidate candidate) {
      if (!CANDIDATES.contains(candidate)) {
         if (CANDIDATES.size() >= 512) {
            Iterator<NetheriteChunkTracker.Candidate> oldest = CANDIDATES.iterator();
            oldest.next();
            oldest.remove();
         }

         CANDIDATES.add(candidate);
         if (listener != null) {
            listener.onCandidate(candidate);
         }
      }
   }

   private static void trackLevel(class_638 level) {
      class_5321<class_1937> dimension = level.method_27983();
      if (level != lastLevel.get() || !dimension.equals(lastDimension)) {
         CANDIDATES.clear();
         lastLevel = new WeakReference<>(level);
         lastDimension = dimension;
      }
   }

   public static boolean isNether(class_638 level) {
      class_5321<class_1937> dimension = level.method_27983();
      return dimension == class_1937.field_25180 || DONUT_NETHER.equals(dimension) || level.method_40134().method_40225(class_7134.field_37667);
   }

   public static List<NetheriteChunkTracker.Candidate> snapshot() {
      return class_310.method_1551().field_1687 != lastLevel.get() ? List.of() : List.copyOf(CANDIDATES);
   }

   public static void setListener(NetheriteChunkTracker.CandidateListener candidateListener) {
      listener = candidateListener;
   }

   public static void setRevealListener(NetheriteChunkTracker.RevealListener debrisListener) {
      revealListener = debrisListener;
   }

   @Environment(EnvType.CLIENT)
   public record Candidate(int chunkX, int chunkZ, int sectionY) {
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface CandidateListener {
      void onCandidate(NetheriteChunkTracker.Candidate var1);
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface RevealListener {
      void onRevealed(class_2338 var1);
   }
}
