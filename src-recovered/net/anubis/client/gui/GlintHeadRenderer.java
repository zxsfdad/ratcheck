package net.anubis.client.gui;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10508;
import net.minecraft.class_10515;
import net.minecraft.class_11659;
import net.minecraft.class_11786;
import net.minecraft.class_12249;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2960;
import net.minecraft.class_4587;
import net.minecraft.class_4608;
import net.minecraft.class_5598;
import net.minecraft.class_811;
import net.minecraft.class_836;
import net.minecraft.class_9334;
import net.minecraft.class_10515.class_10516;
import net.minecraft.class_10515.class_11695;
import net.minecraft.class_2484.class_2486;
import net.minecraft.class_5598.class_11652;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public final class GlintHeadRenderer implements class_10508 {
   public static final class_2960 ID = class_2960.method_60655("anubis", "glint_head");
   private final class_5598 model;

   private GlintHeadRenderer(class_5598 model) {
      this.model = model;
   }

   public static class_1799 stack() {
      class_1799 head = new class_1799(class_1802.field_8575);
      head.method_57379(class_9334.field_54199, ID);
      head.method_57379(class_9334.field_49641, true);
      return head;
   }

   public void method_65699(class_811 context, class_4587 pose, class_11659 collector, int light, int overlay, boolean foil, int outline) {
      class_836.method_72958(null, 180.0F, 0.0F, pose, collector, light, this.model, class_11786.field_62215, outline, null);
      if (foil) {
         pose.method_22903();
         pose.method_46416(0.5F, 0.0F, 0.5F);
         pose.method_22905(-1.0F, -1.0F, 1.0F);
         class_11652 state = new class_11652();
         state.field_61682 = 180.0F;
         collector.method_73529(1).method_73489(this.model, state, pose, class_12249.method_75997(), light, class_4608.field_21444, 0, null);
         pose.method_22909();
      }
   }

   public void method_72175(Consumer<Vector3fc> output) {
      class_4587 pose = new class_4587();
      pose.method_46416(0.5F, 0.0F, 0.5F);
      pose.method_22905(-1.0F, -1.0F, 1.0F);
      this.model.method_63512().method_72152(pose, output);
   }

   @Environment(EnvType.CLIENT)
   public record Unbaked() implements class_10516 {
      public static final MapCodec<GlintHeadRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new GlintHeadRenderer.Unbaked());

      public MapCodec<GlintHeadRenderer.Unbaked> method_65696() {
         return MAP_CODEC;
      }

      public class_10515<?> method_65698(class_11695 context) {
         class_5598 model = class_836.method_32160(context.comp_3391(), class_2486.field_11510);
         return model == null ? null : new GlintHeadRenderer(model);
      }
   }
}
