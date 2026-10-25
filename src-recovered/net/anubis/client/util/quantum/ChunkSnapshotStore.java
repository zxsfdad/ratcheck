package net.anubis.client.util.quantum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongPredicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2248;
import net.minecraft.class_2680;
import net.minecraft.class_2841;
import net.minecraft.class_6563;

@Environment(EnvType.CLIENT)
public final class ChunkSnapshotStore {
   public static final int MIN_SECTION_INDEX = 6;
   public static final int MAX_SECTION_INDEX = 23;
   private static final class_6563<class_2680> STRATEGY = class_6563.method_74162(class_2248.field_10651);
   private final Map<Long, class_2841<class_2680>[]> snapshots = new HashMap<>();
   private final class_2680 air;

   public ChunkSnapshotStore(class_2680 air) {
      this.air = air;
   }

   public static long chunkKey(int chunkX, int chunkZ) {
      return (long)chunkX << 32 | chunkZ & 4294967295L;
   }

   public void store(int chunkX, int chunkZ, class_2841<class_2680>[] sections) {
      this.snapshots.put(chunkKey(chunkX, chunkZ), sections);
   }

   public void retain(LongPredicate keep) {
      this.snapshots.keySet().removeIf(key -> !keep.test(key));
   }

   public void clear() {
      this.snapshots.clear();
   }

   public void update(int chunkX, int chunkZ, int sectionIndex, int localX, int localY, int localZ, class_2680 state) {
      if (sectionIndex >= 6 && sectionIndex <= 23) {
         class_2841<class_2680>[] sections = this.snapshots.get(chunkKey(chunkX, chunkZ));
         if (sections != null && sectionIndex < sections.length) {
            class_2841<class_2680> section = sections[sectionIndex];
            if (section == null) {
               if (state == this.air) {
                  return;
               }

               section = new class_2841(this.air, STRATEGY);
               sections[sectionIndex] = section;
            }

            section.method_35321(localX, localY, localZ, state);
         }
      }
   }

   public List<ChunkSnapshotStore.BlockChange> compare(int chunkX, int chunkZ, class_2841<class_2680>[] current, int minSectionY) {
      List<ChunkSnapshotStore.BlockChange> changes = new ArrayList<>();
      class_2841<class_2680>[] old = this.snapshots.get(chunkKey(chunkX, chunkZ));
      if (this.isAllAir(old)) {
         return changes;
      }

      for (int index = 6; index <= 23; index++) {
         class_2841<class_2680> before = index < old.length ? old[index] : null;
         class_2841<class_2680> after = index < current.length ? current[index] : null;
         if (before != null || after != null) {
            int baseY = (index + minSectionY) * 16;

            for (int y = 0; y < 16; y++) {
               for (int z = 0; z < 16; z++) {
                  for (int x = 0; x < 16; x++) {
                     class_2680 oldState = before != null ? (class_2680)before.method_12321(x, y, z) : this.air;
                     class_2680 newState = after != null ? (class_2680)after.method_12321(x, y, z) : this.air;
                     if (oldState != newState) {
                        changes.add(new ChunkSnapshotStore.BlockChange(chunkX * 16 + x, baseY + y, chunkZ * 16 + z, newState, oldState));
                     }
                  }
               }
            }
         }
      }

      return changes;
   }

   private boolean isAllAir(class_2841<class_2680>[] sections) {
      if (sections == null) {
         return true;
      }

      for (int index = 6; index <= 23 && index < sections.length; index++) {
         if (sections[index] != null) {
            return false;
         }
      }

      return true;
   }

   @Environment(EnvType.CLIENT)
   public record BlockChange(int worldX, int worldY, int worldZ, class_2680 newState, class_2680 oldState) {
   }
}
