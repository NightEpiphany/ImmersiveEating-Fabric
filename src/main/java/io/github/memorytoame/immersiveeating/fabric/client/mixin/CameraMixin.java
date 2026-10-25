package io.github.memorytoame.immersiveeating.fabric.client.mixin;

import io.github.memorytoame.immersiveeating.fabric.client.animation.CameraRotationComposer;
import io.github.memorytoame.immersiveeating.fabric.client.animation.CameraShake;
import io.github.memorytoame.immersiveeating.fabric.common.content.item.Empty;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Camera.class)
public abstract class CameraMixin {
   @Unique
   private static final String FOOD$GRAVITY_CHANGER = "gravity_changer";

   @Shadow
   private boolean detached;

   @Inject(
      method = "alignWithEntity",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Camera;setPosition(DDD)V",
         shift = At.Shift.BEFORE,
         ordinal = 0
      )
   )
   private void food$applyAnimationRotation(
           float partialTicks, CallbackInfo ci
   ) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || this.detached || !mc.options.getCameraType().isFirstPerson()) {
         CameraRotationComposer.clearBlockbenchRotation();
         return;
      }

      boolean animationPlaying = Empty.isEatAnimationPlaying();
      if (!animationPlaying) {
         CameraRotationComposer.clearBlockbenchRotation();
      }

      CameraRotationComposer.applyCameraEffects(
         (Camera)(Object)this,
         FabricLoader.getInstance().isModLoaded(FOOD$GRAVITY_CHANGER),
         animationPlaying,
         CameraShake.interpolatedPitch(partialTicks),
         CameraShake.interpolatedYaw(partialTicks),
         CameraShake.interpolatedRoll(partialTicks)
      );
   }
}
