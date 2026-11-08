package io.github.memorytoame.immersiveeating.fabric.client.mixin;

import io.github.memorytoame.immersiveeating.fabric.client.animation.AnimationResourceChecker;
import io.github.memorytoame.immersiveeating.fabric.common.content.definition.FoodDefinitionManager;
import io.github.memorytoame.immersiveeating.fabric.common.content.item.Empty;
import io.github.memorytoame.immersiveeating.fabric.utils.AnimationUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(FirstPersonHandsAndItems.class)
public abstract class FirstPersonHandsAndItemsMixin {
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
   private void food$clearThirdPersonAnimations(LocalPlayer player, CallbackInfo ci) {
      if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
         this.food$stopSwitchAnimations();
         this.food$resetTracking();
      }
   }

   @Inject(
      method = "extractRenderState",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;clear()V",
         ordinal = 0
      )
   )
   private void food$prepareFirstPersonState(
      LocalPlayer player,
      float partialTicks,
      FirstPersonHandsAndItemsRenderState state,
      CallbackInfo ci
   ) {
      if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
         this.food$stopSwitchAnimations();
         this.food$resetTracking();
         return;
      }

      ItemStack selectedMainHandItem = state.mainHandItem;
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
            state.mainHandItem = this.food$switchOutItem.copy();
            state.mainHandHeight = 1.0F;
            state.oldMainHandHeight = 1.0F;
         } else {
            Empty.stopClientAnimation(this.food$switchOutItem, "switchout");
            state.mainHandItem = this.food$pendingMainHandItem.copy();
            state.mainHandHeight = 0.0F;
            state.oldMainHandHeight = 0.0F;
            this.food$renderedMainHandItem = state.mainHandItem.copy();
            this.food$waitingForSwitchOut = false;
            this.food$switchOutItem = ItemStack.EMPTY;
            this.food$pendingMainHandItem = ItemStack.EMPTY;
            food$trigger(state.mainHandItem, "switchin");
         }
      } else if (!this.food$hasTrackedRenderedItem) {
         this.food$renderedMainHandItem = state.mainHandItem.copy();
         this.food$hasTrackedRenderedItem = true;
      } else if (!ItemStack.isSameItemSameComponents(this.food$renderedMainHandItem, state.mainHandItem)) {
         food$trigger(state.mainHandItem, "switchin");
         this.food$renderedMainHandItem = state.mainHandItem.copy();
      }

      if ((this.food$isCustomMainHand(state.mainHandItem) || this.food$isCustomMainHand(this.food$renderedMainHandItem))
         && !player.isUsingItem()) {
         state.handRenderSelection = FirstPersonHandsAndItemsRenderState.HandRenderSelection.RENDER_MAIN_HAND_ONLY;
      }
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
