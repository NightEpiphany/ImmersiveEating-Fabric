package io.github.memorytoame.immersiveeating.fabric.client.input;

import io.github.memorytoame.immersiveeating.fabric.Food;
import io.github.memorytoame.immersiveeating.fabric.KaleidoscopeImmersiveEating;
import io.github.memorytoame.immersiveeating.fabric.client.events.GameplayEvents;
import io.github.memorytoame.immersiveeating.fabric.common.config.Config;
import io.github.memorytoame.immersiveeating.fabric.common.content.definition.FoodDefinitionManager;
import io.github.memorytoame.immersiveeating.fabric.common.content.item.Empty;
import io.github.memorytoame.immersiveeating.fabric.mixin.accessor.InventoryAccessor;
import io.github.memorytoame.immersiveeating.fabric.utils.AnimationUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;

@Environment(EnvType.CLIENT)
public final class InputEvents {
   public static float debugX = -1.5F;
   public static float debugY = 0.05F;
   public static float debugZ = -0.3F;
   public static float debugRo = 0.0F;
   private static boolean rightClickHandled;

   private InputEvents() {
   }

   public static void register() {
      GameplayEvents.register();
      ClientTickEvents.START_CLIENT_TICK.register(InputEvents::onClientTickStart);
      ClientTickEvents.END_CLIENT_TICK.register(InputEvents::onClientTick);
      UseItemCallback.EVENT.register((player, level, hand) -> {
         if (!level.isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
         }
         Minecraft mc = Minecraft.getInstance();
         if (player == mc.player && handleRightClick(mc)) {
            // CONSUME 处理本次点击，同时避免发送原版物品使用数据包。
            return InteractionResult.CONSUME;
         }
         return InteractionResult.PASS;
      });
   }

   public static boolean handleRightClick(Minecraft mc) {
      if (mc == null || !mc.options.keyUse.isDown()) {
         rightClickHandled = false;
      }

      if (mc == null || mc.player == null || mc.player.isSpectator() || !mc.options.getCameraType().isFirstPerson()) {
         return false;
      }

      AnimationUtils.cancelIfHeldItemChanged(mc);
      ItemStack stack = mc.player.getMainHandItem();
      if (!FoodDefinitionManager.init_ItemList.contains(stack.getItem())) {
         return false;
      }

      if (AnimationUtils.isInputLocked() || Empty.isAnimationPlaying()) {
         return true;
      }

      if (!Config.ENABLE_RIGHT_BUTTON.get() || mc.player.isCrouching() || isAnimationGui(mc.gui.screen())
         || GameplayEvents.shouldAllowTargetInteraction(mc)) {
         return false;
      }

      if (!rightClickHandled) {
         rightClickHandled = GameplayEvents.startRightClickAnimation(stack);
      }
      return rightClickHandled;
   }

   private static void onClientTickStart(Minecraft mc) {
      if (!mc.options.getCameraType().isFirstPerson() && AnimationUtils.isInputLocked()) {
         AnimationUtils.stopAnimationControl();
      }
      AnimationUtils.updateSwitchInAnimation();
      if (mc.player == null || !mc.options.keyUse.isDown()) {
         rightClickHandled = false;
      }
      if (mc.player == null || isAnimationGui(mc.gui.screen()) || !AnimationUtils.isInputLocked()) {
         return;
      }

      if (mc.options.keyAttack.consumeClick()) {
         AnimationUtils.stopAnimationControl();
         mc.player.swing(InteractionHand.MAIN_HAND, false);
      }
   }

   private static void onClientTick(Minecraft mc) {
      if (mc.player == null) {
         AnimationUtils.clearAnimationState();
         return;
      }

      if (ModKeyMappings.ANIMATION.consumeClick()) {
         startKeyboardAnimation(mc);
      }

      AnimationUtils.updateCustomUseState(mc);
      if (!isAnimationGui(mc.gui.screen())) {
         AnimationUtils.cancelIfHeldItemChanged(mc);
      }

      if ((mc.gui.screen() == null || isAnimationGui(mc.gui.screen())) && mc.options.getCameraType().isFirstPerson()) {
         AnimationUtils.checkEatAnimationState();
         if (AnimationUtils.isAnimationLocked()) {
            var accessToInventory = (InventoryAccessor) mc.player.getInventory();
            accessToInventory.food$setSelected(AnimationUtils.lockedHotbarSlot);
         } else {
            AnimationUtils.lockedHotbarSlot = -1;
         }
      } else if (AnimationUtils.isInputLocked()) {
         AnimationUtils.stopAnimationControl();
      }

      if (!AnimationUtils.isInputLocked() && Food.DEBUG) {
         updateDebugKeys(mc);
      }
   }

   private static void startKeyboardAnimation(Minecraft mc) {
      if (mc.player == null || GameplayEvents.shouldAllowTargetInteraction(mc)) {
         return;
      }

      GameplayEvents.startRightClickAnimation(mc.player.getMainHandItem());
   }

   private static void updateDebugKeys(Minecraft mc) {
      if (mc.options.keyInventory.consumeClick()) {
         debugX += 0.05F;
      }
      if (mc.options.keyAdvancements.consumeClick()) {
         debugX -= 0.05F;
      }
      if (mc.options.keyDrop.consumeClick()) {
         debugY -= 0.05F;
      }
      if (mc.options.keySwapOffhand.consumeClick()) {
         debugZ -= 0.05F;
      }
      if (mc.options.keyChat.consumeClick()) {
         debugRo += 0.01F;
      }
      if (mc.options.keyCommand.consumeClick()) {
         debugRo -= 0.01F;
      }
      if (mc.options.keyPlayerList.consumeClick()) {
         debugY += 0.05F;
      }
      if (mc.options.keyScreenshot.consumeClick()) {
         debugZ += 0.05F;
      }

      KaleidoscopeImmersiveEating.LOGGER.debug("translate = {}, {}, {}",debugX, debugY, debugZ);
   }

   private static boolean isAnimationGui(Screen screen) {
      return screen instanceof ChatScreen || screen instanceof AbstractContainerScreen<?>;
   }
}
