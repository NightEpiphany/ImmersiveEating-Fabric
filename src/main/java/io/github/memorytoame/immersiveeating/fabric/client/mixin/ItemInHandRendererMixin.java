package io.github.memorytoame.immersiveeating.fabric.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.memorytoame.immersiveeating.fabric.client.animation.AnimationResourceChecker;
import io.github.memorytoame.immersiveeating.fabric.common.content.definition.FoodDefinitionManager;
import io.github.memorytoame.immersiveeating.fabric.common.content.item.Empty;
import io.github.memorytoame.immersiveeating.fabric.utils.AnimationUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
   @Shadow
   private ItemStack mainHandItem;

   @Shadow
   private float mainHandHeight;

   @Shadow
   private float oMainHandHeight;

   @Unique
   private ItemStack food$selectedMainHandItem = ItemStack.EMPTY;
   @Unique
   private ItemStack food$renderedMainHandItem = ItemStack.EMPTY;
   @Unique
   private ItemStack food$pendingMainHandItem = ItemStack.EMPTY;
   @Unique
   private ItemStack food$switchOutItem = ItemStack.EMPTY;
   @Unique
   private boolean food$hasTrackedSelectedItem;
   @Unique
   private boolean food$hasTrackedRenderedItem;
   @Unique
   private boolean food$waitingForSwitchOut;
   @Unique
   private boolean food$switchOutStarted;
   @Unique
   private int food$switchOutStartTick;

   @Inject(method = "tick", at = @At("HEAD"))
   private void food$clearThirdPersonAnimations(CallbackInfo ci) {
      if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
         this.food$stopSwitchAnimations();
         this.food$resetTracking();
      }
   }

   @Inject(method = "submitHandsWithItems", at = @At("HEAD"))
   private void food$prepareFirstPersonState(
      float frameInterp,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      LocalPlayer player,
      int lightCoords,
      CallbackInfo ci
   ) {
      if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
         this.food$stopSwitchAnimations();
         this.food$resetTracking();
         return;
      }

      ItemStack selectedMainHandItem = player.getMainHandItem();
      if (!this.food$hasTrackedSelectedItem) {
         this.food$selectedMainHandItem = selectedMainHandItem.copy();
         this.food$hasTrackedSelectedItem = true;
      } else if (!ItemStack.isSameItemSameComponents(this.food$selectedMainHandItem, selectedMainHandItem)) {
         this.food$selectedMainHandItem = selectedMainHandItem.copy();
         if (this.food$waitingForSwitchOut) {
            this.food$pendingMainHandItem = selectedMainHandItem.copy();
         } else if (food$trigger(this.food$renderedMainHandItem, "switchout")) {
            this.food$switchOutItem = this.food$renderedMainHandItem.copy();
            this.food$pendingMainHandItem = selectedMainHandItem.copy();
            this.food$waitingForSwitchOut = true;
            this.food$switchOutStarted = false;
            this.food$switchOutStartTick = player.tickCount;
         }
      }

      if (this.food$waitingForSwitchOut) {
         boolean playing = Empty.isClientAnimationPlaying(this.food$switchOutItem, "switchout");
         this.food$switchOutStarted |= playing;
         boolean awaitingFirstFrame = !this.food$switchOutStarted && player.tickCount - this.food$switchOutStartTick <= 40;
         if (playing || awaitingFirstFrame) {
            this.mainHandItem = this.food$switchOutItem.copy();
            this.mainHandHeight = 1.0F;
            this.oMainHandHeight = 1.0F;
         } else {
            Empty.stopClientAnimation(this.food$switchOutItem, "switchout");
            this.mainHandItem = this.food$pendingMainHandItem.copy();
            this.mainHandHeight = 0.0F;
            this.oMainHandHeight = 0.0F;
            this.food$renderedMainHandItem = this.mainHandItem.copy();
            this.food$waitingForSwitchOut = false;
            this.food$switchOutItem = ItemStack.EMPTY;
            this.food$pendingMainHandItem = ItemStack.EMPTY;
            food$trigger(this.mainHandItem, "switchin");
         }
      } else if (!this.food$hasTrackedRenderedItem) {
         this.food$renderedMainHandItem = this.mainHandItem.copy();
         this.food$hasTrackedRenderedItem = true;
      } else if (!ItemStack.isSameItemSameComponents(this.food$renderedMainHandItem, this.mainHandItem)) {
         food$trigger(this.mainHandItem, "switchin");
         this.food$renderedMainHandItem = this.mainHandItem.copy();
      }
   }

   @WrapOperation(
      method = "submitHandsWithItems",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;submitArmWithItem("
            + "Lnet/minecraft/client/player/AbstractClientPlayer;"
            + "FFLnet/minecraft/world/InteractionHand;F"
            + "Lnet/minecraft/world/item/ItemStack;F"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
         ordinal = 1
      )
   )
   private void food$skipOffHandForCustomMainHand(
      ItemInHandRenderer renderer,
      AbstractClientPlayer player,
      float frameInterp,
      float xRot,
      InteractionHand hand,
      float attack,
      ItemStack itemStack,
      float inverseArmHeight,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int lightCoords,
      Operation<Void> original
   ) {
      if (hand == InteractionHand.OFF_HAND
         && (this.food$isCustomMainHand(this.food$selectedMainHandItem)
            || this.food$isCustomMainHand(this.food$renderedMainHandItem))
         && !player.isUsingItem()) {
         return;
      }

      original.call(
         renderer,
         player,
         frameInterp,
         xRot,
         hand,
         attack,
         itemStack,
         inverseArmHeight,
         poseStack,
         submitNodeCollector,
         lightCoords
      );
   }

   @Unique
   private boolean food$isCustomMainHand(ItemStack stack) {
      return stack != null && !stack.isEmpty() && FoodDefinitionManager.init_ItemList.contains(stack.getItem());
   }

   @Unique
   private static boolean food$trigger(ItemStack stack, String controllerName) {
      if (Minecraft.getInstance().options.getCameraType().isFirstPerson()
         && !stack.isEmpty() && FoodDefinitionManager.init_ItemList.contains(stack.getItem())
         && AnimationResourceChecker.hasAnimation(stack, controllerName)) {
         if ("switchin".equals(controllerName)) {
            AnimationUtils.beginSwitchInAnimation(stack);
         }
         Empty.triggerClientAnimation(stack, controllerName);
         return true;
      }

      return false;
   }

   @Unique
   private void food$stopSwitchAnimations() {
      if (this.food$isCustomMainHand(this.food$renderedMainHandItem)) {
         Empty.stopClientAnimation(this.food$renderedMainHandItem, "switchin");
      }
      if (!this.food$switchOutItem.isEmpty()) {
         Empty.stopClientAnimation(this.food$switchOutItem, "switchout");
      }
   }

   @Unique
   private void food$resetTracking() {
      this.food$selectedMainHandItem = ItemStack.EMPTY;
      this.food$renderedMainHandItem = ItemStack.EMPTY;
      this.food$pendingMainHandItem = ItemStack.EMPTY;
      this.food$switchOutItem = ItemStack.EMPTY;
      this.food$hasTrackedSelectedItem = false;
      this.food$hasTrackedRenderedItem = false;
      this.food$waitingForSwitchOut = false;
      this.food$switchOutStarted = false;
      this.food$switchOutStartTick = 0;
   }
}
