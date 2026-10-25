package net.anubis.client.module.impl.misc;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.anubis.client.esp.ChunkScanner;
import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.gui.ChallengeToast;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BlockColorListSetting;
import net.anubis.client.module.setting.BooleanSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1937;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_5321;

@Environment(EnvType.CLIENT)
public final class BlockNotifierModule extends ClientModule implements ChunkScanner.Consumer {
   private static final int MAX_KNOWN = 50000;
   private final BlockColorListSetting blocks = this.addSetting("blocks", BlockColorListSetting.blockList("Blocks", List.of("minecraft:spawner")));
   private final BooleanSetting sound = this.addSetting("sound", new BooleanSetting("Sound", true));
   private final LongLinkedOpenHashSet known = new LongLinkedOpenHashSet();
   private final Map<class_2248, BlockNotifierModule.Finds> pending = new LinkedHashMap<>();
   private Set<String> scannedIds = Set.of();
   private class_5321<class_1937> dimension;

   public BlockNotifierModule() {
      super(
         "block-notifier", "Block Notifier", "Shows a toast with the block and its coordinates when one of your chosen blocks shows up.", ModuleCategory.MISC
      );
      this.blocks.onChange(this::rescan);
   }

   @Override
   protected void onEnable() {
      this.known.clear();
      this.pending.clear();
      this.dimension = null;
      this.scannedIds = this.blocks.enabledIds();
      ChunkScanner.register(this);
      this.on(TickEvent.TYPE, Stage.POST, this::onTick);
   }

   @Override
   protected void onDisable() {
      ChunkScanner.unregister(this);
      this.known.clear();
      this.pending.clear();
   }

   private void rescan() {
      if (this.isEnabled()) {
         Set<String> ids = this.blocks.enabledIds();
         if (!ids.equals(this.scannedIds)) {
            this.scannedIds = ids;
            ChunkScanner.rescan();
         }
      }
   }

   @Override
   public boolean wants(class_2680 state) {
      return this.blocks.isEnabled(state.method_26204());
   }

   @Override
   public void found(long pos, class_2680 state) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1687 != null) {
         this.syncDimension(mc);
         if (this.known.add(pos)) {
            if (this.known.size() > 50000) {
               this.known.removeFirstLong();
            }

            class_2338 at = class_2338.method_10092(pos);
            BlockNotifierModule.Finds finds = this.pending.computeIfAbsent(state.method_26204(), block -> new BlockNotifierModule.Finds());
            finds.count++;
            double distanceSq = mc.field_1724 == null ? 0.0 : mc.field_1724.method_5707(class_243.method_24953(at));
            if (distanceSq < finds.nearestSq) {
               finds.nearestSq = distanceSq;
               finds.nearest = at;
            }
         }
      }
   }

   @Override
   public void removed(long pos) {
      this.known.remove(pos);
   }

   @Override
   public void reset() {
      this.known.clear();
      this.pending.clear();
   }

   private void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1687 != null) {
         this.syncDimension(mc);
         if (!this.pending.isEmpty()) {
            for (Entry<class_2248, BlockNotifierModule.Finds> entry : this.pending.entrySet()) {
               this.notify(entry.getKey(), entry.getValue());
            }

            this.pending.clear();
         }
      }
   }

   private void syncDimension(class_310 mc) {
      class_5321<class_1937> current = mc.field_1687.method_27983();
      if (!current.equals(this.dimension)) {
         this.dimension = current;
         this.known.clear();
         this.pending.clear();
      }
   }

   private void notify(class_2248 block, BlockNotifierModule.Finds finds) {
      class_1792 item = block.method_8389();
      class_1799 icon = new class_1799(item == class_1802.field_8162 ? class_1802.field_8251 : item);
      String title = block.method_9518().getString();
      class_2338 at = finds.nearest;
      class_2561 message = class_2561.method_43470("at " + at.method_10263() + ", " + at.method_10264() + ", " + at.method_10260());

      for (int i = 0; i < finds.count; i++) {
         ChallengeToast.addOrUpdate(icon, title, message, this.sound.get());
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Finds {
      int count;
      class_2338 nearest;
      double nearestSq = Double.MAX_VALUE;
   }
}
