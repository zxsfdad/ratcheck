package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.function.Supplier;
import net.anubis.client.util.pvp.Rotation;
import net.anubis.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12392;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_238;
import net.minecraft.class_310;
import net.minecraft.class_3965;
import net.minecraft.class_3966;
import net.minecraft.class_636;
import net.minecraft.class_746;
import net.minecraft.class_9334;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(class_636.class)
public abstract class MixinRotationMultiPlayerGameMode {
   @Shadow
   @Final
   private class_310 field_3712;

   @WrapMethod(method = "method_2919")
   private class_1269 anubis$useItemRotation(class_1657 player, class_1268 hand, Operation<class_1269> original) {
      return this.anubis$asSent(player, () -> (class_1269)original.call(new Object[]{player, hand}));
   }

   @WrapMethod(method = "method_2897")
   private void anubis$releaseRotation(class_1657 player, Operation<Void> original) {
      this.anubis$asSent(player, () -> (Void)original.call(new Object[]{player}));
   }

   private <T> T anubis$asSent(class_1657 player, Supplier<T> call) {
      class_746 self = this.field_3712.field_1724;
      if (player == self && !ServerRotation.isSwapped()) {
         Rotation rotation = ServerRotation.commit(self);
         float yaw = self.method_36454();
         float pitch = self.method_36455();
         if (rotation.yaw() == yaw && rotation.pitch() == pitch) {
            return call.get();
         }

         self.method_36456(rotation.yaw());
         self.method_36457(rotation.pitch());

         try {
            return call.get();
         } finally {
            self.method_36456(yaw);
            self.method_36457(pitch);
         }
      } else {
         return call.get();
      }
   }

   @Inject(method = "method_2918", at = @At("HEAD"), cancellable = true, order = 950)
   private void anubis$attackRotationGate(class_1657 player, class_1297 target, CallbackInfo ci) {
      class_746 self = this.field_3712.field_1724;
      if (player == self) {
         class_238 box = target.method_5829();
         double reach = self.method_55755();
         class_12392 range = (class_12392)self.method_6047().method_58694(class_9334.field_64680);
         if (range != null) {
            box = box.method_1014(range.comp_5262());
            reach = range.method_76739(self);
         }

         if (ServerRotation.misses(self, box, reach)) {
            player.method_7350();
            ci.cancel();
         }
      }
   }

   @Inject(method = "method_2905", at = @At("HEAD"), cancellable = true, order = 950)
   private void anubis$interactRotationGate(class_1657 player, class_1297 target, class_1268 hand, CallbackInfoReturnable<class_1269> cir) {
      class_746 self = this.field_3712.field_1724;
      if (player == self && ServerRotation.misses(self, target.method_5829(), self.method_55755())) {
         cir.setReturnValue(class_1269.field_5811);
      }
   }

   @Inject(method = "method_2917", at = @At("HEAD"), cancellable = true, order = 950)
   private void anubis$interactAtRotationGate(class_1657 player, class_1297 target, class_3966 hit, class_1268 hand, CallbackInfoReturnable<class_1269> cir) {
      class_746 self = this.field_3712.field_1724;
      if (player == self && ServerRotation.misses(self, target.method_5829(), self.method_55755())) {
         cir.setReturnValue(class_1269.field_5811);
      }
   }

   @Inject(method = "method_2896", at = @At("HEAD"), cancellable = true, order = 950)
   private void anubis$useItemOnRotationGate(class_746 player, class_1268 hand, class_3965 hit, CallbackInfoReturnable<class_1269> cir) {
      if (player == this.field_3712.field_1724 && ServerRotation.missesBlock(player, new class_238(hit.method_17777()), player.method_55754())) {
         cir.setReturnValue(class_1269.field_5814);
      }
   }

   @Inject(method = "method_2910", at = @At("HEAD"), cancellable = true, order = 950)
   private void anubis$startDestroyRotationGate(class_2338 pos, class_2350 face, CallbackInfoReturnable<Boolean> cir) {
      class_746 self = this.field_3712.field_1724;
      if (self != null && ServerRotation.missesBlock(self, new class_238(pos), self.method_55754())) {
         cir.setReturnValue(false);
      }
   }
}
