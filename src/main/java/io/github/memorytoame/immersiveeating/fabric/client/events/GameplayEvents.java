package io.github.memorytoame.immersiveeating.fabric.client.events;

import io.github.memorytoame.immersiveeating.fabric.common.content.definition.FoodDefinitionManager;
import io.github.memorytoame.immersiveeating.fabric.common.content.item.Empty;
import io.github.memorytoame.immersiveeating.fabric.client.network.ClientNetwork;
import io.github.memorytoame.immersiveeating.fabric.utils.AnimationUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

@Environment(EnvType.CLIENT)
public final class GameplayEvents {
   private GameplayEvents() {
   }

   public static void register() {
      ItemTooltipCallback.EVENT.register((stack, context, flags, tooltip) -> {
         if (FoodDefinitionManager.init_ItemList.contains(stack.getItem())) {
            tooltip.add(Component.translatable("tooltip.food.eating_animation").withStyle(ChatFormatting.GRAY));
         }
      });
   }

   public static boolean shouldAllowTargetInteraction(Minecraft mc) {
      if (mc.hitResult instanceof EntityHitResult hit) {
         return ContainerTarget.allowsInteraction(hit.getEntity());
      }

      return mc.level != null
         && mc.hitResult instanceof BlockHitResult hit
         && hit.getType() == HitResult.Type.BLOCK
         && ContainerTarget.allowsInteraction(mc.level, hit.getBlockPos());
   }

   public static boolean startRightClickAnimation() {
      Minecraft mc = Minecraft.getInstance();
      return mc.player != null && startRightClickAnimation(mc.player.getMainHandItem());
   }

   public static boolean startRightClickAnimation(ItemStack stack) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.options.getCameraType().isFirstPerson() && !Empty.isAnimationPlaying()
         && !AnimationUtils.isAnimationLocked() && !AnimationUtils.isAnimationRequestPending()
         && !Empty.isClientAnimationPlaying(stack, "switchin") && FoodDefinitionManager.init_ItemList.contains(stack.getItem())) {
         AnimationUtils.beginAnimationSession(mc);
         ClientNetwork.sendAnimation(AnimationUtils.getAnimationSessionId(mc));
         return true;
      }
      return false;
   }
}

