package io.github.memorytoame.immersiveeating.fabric.server;

import io.github.memorytoame.immersiveeating.fabric.common.config.Config;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;

public final class ServerGameplayEvents {
   private ServerGameplayEvents() {
   }

   public static void register() {
      ServerAnimationEvents.register();
      UseItemCallback.EVENT.register((player, level, hand) -> {
         if (shouldBlockInteraction(player, hand)) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
         }
         return InteractionResultHolder.pass(player.getItemInHand(hand));
      });
      UseBlockCallback.EVENT.register((player, level, hand, hitResult) ->
         shouldBlockInteraction(player, hand) && !ContainerTarget.allowsInteraction(level, hitResult.getBlockPos())
            ? InteractionResult.FAIL : InteractionResult.PASS);
      UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
         shouldBlockInteraction(player, hand) && !ContainerTarget.allowsInteraction(entity)
            ? InteractionResult.FAIL : InteractionResult.PASS);
   }

   private static boolean shouldBlockInteraction(Player player, InteractionHand hand) {
      return player instanceof ServerPlayer serverPlayer
         && Config.ENABLE_RIGHT_BUTTON.get()
         && !player.isCrouching()
         && hand == InteractionHand.MAIN_HAND
         && ServerAnimationManager.matchesHeldStack(serverPlayer);
   }
}

