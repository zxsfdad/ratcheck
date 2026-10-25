package net.anubis.client.module.impl.misc.noinvleak;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;

@Environment(EnvType.CLIENT)
public final class FakeLoadout {
   public static final int UNKNOWN = 41;
   private static final class_1792[] HOTBAR = new class_1792[]{
      class_1802.field_8371,
      class_1802.field_8802,
      class_1802.field_22022,
      class_1802.field_8403,
      class_1802.field_8377,
      class_1802.field_22024,
      class_1802.field_8475,
      class_1802.field_8556,
      class_1802.field_22025,
      class_1802.field_8699,
      class_1802.field_8250,
      class_1802.field_22023,
      class_1802.field_8102,
      class_1802.field_8705,
      class_1802.field_20412,
      class_1802.field_8810,
      class_1802.field_8176,
      class_1802.field_8071,
      class_1802.field_8229,
      class_1802.field_8634,
      class_1802.field_8118,
      class_1802.field_8831,
      class_1802.field_8884
   };
   private static final class_1792[] MAIN = new class_1792[]{
      class_1802.field_20412,
      class_1802.field_20391,
      class_1802.field_8831,
      class_1802.field_8583,
      class_1802.field_8118,
      class_1802.field_8600,
      class_1802.field_8713,
      class_1802.field_8620,
      class_1802.field_8695,
      class_1802.field_8477,
      class_1802.field_8725,
      class_1802.field_8759,
      class_1802.field_8107,
      class_1802.field_8276,
      class_1802.field_8606,
      class_1802.field_8511,
      class_1802.field_8054,
      class_1802.field_8858,
      class_1802.field_8110,
      class_1802.field_8317,
      class_1802.field_8861,
      class_1802.field_8121,
      class_1802.field_8281,
      class_1802.field_8280,
      class_1802.field_8745,
      class_1802.field_8153,
      class_1802.field_8145,
      class_1802.field_8680,
      class_1802.field_8279,
      class_1802.field_8261,
      class_1802.field_8550,
      class_1802.field_8529,
      class_1802.field_8407,
      class_1802.field_17531,
      class_1802.field_8465,
      class_1802.field_8106,
      class_1802.field_8732
   };
   private static final class_1792[] OFFHAND = new class_1792[]{
      class_1802.field_8255, class_1802.field_8810, class_1802.field_8705, class_1802.field_8107, class_1802.field_8176, class_1802.field_8463
   };
   private static final class_1792[][] ARMOR = new class_1792[][]{
      {class_1802.field_8660, class_1802.field_8313, class_1802.field_8285, class_1802.field_22030},
      {class_1802.field_8396, class_1802.field_8218, class_1802.field_8348, class_1802.field_22029},
      {class_1802.field_8523, class_1802.field_8873, class_1802.field_8058, class_1802.field_22028},
      {class_1802.field_8743, class_1802.field_8283, class_1802.field_8805, class_1802.field_22027}
   };
   private static final int FIRST_ARMOR = 36;
   private final class_1799[] stacks = new class_1799[42];
   private long seed;

   public void reseed() {
      this.seed = ThreadLocalRandom.current().nextLong();
      Arrays.fill(this.stacks, null);
   }

   public class_1799 get(int slot) {
      if (slot < 0 || slot > 41) {
         slot = 41;
      }

      class_1799 stack = this.stacks[slot];
      if (stack == null) {
         this.stacks[slot] = stack = this.roll(slot);
      }

      return stack;
   }

   private class_1799 roll(int slot) {
      class_1792 item;
      if (slot < 9) {
         item = this.pick(HOTBAR, slot);
      } else if (slot < 36 || slot == 41) {
         item = this.pick(MAIN, slot);
      } else if (slot == 40) {
         item = this.pick(OFFHAND, slot);
      } else {
         item = ARMOR[slot - 36][(int)Long.remainderUnsigned(mix(this.seed ^ 167L), ARMOR[0].length)];
      }

      class_1799 stack = new class_1799(item);
      if (stack.method_7914() > 1) {
         stack.method_7939(1 + (int)Long.remainderUnsigned(mix(this.seed ^ slot ^ 81L), stack.method_7914()));
      } else if (stack.method_7963()) {
         stack.method_7974((int)Long.remainderUnsigned(mix(this.seed ^ slot ^ 82L), stack.method_7936() / 2));
      }

      return stack;
   }

   private class_1792 pick(class_1792[] pool, int slot) {
      return pool[(int)Long.remainderUnsigned(mix(this.seed ^ slot * -7046029254386353131L), pool.length)];
   }

   private static long mix(long z) {
      z = (z ^ z >>> 30) * -4658895280553007687L;
      z = (z ^ z >>> 27) * -7723592293110705685L;
      return z ^ z >>> 31;
   }
}
