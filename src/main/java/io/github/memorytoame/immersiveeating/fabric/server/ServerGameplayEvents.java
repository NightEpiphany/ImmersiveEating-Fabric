package io.github.memorytoame.immersiveeating.fabric.server;

import io.github.memorytoame.immersiveeating.fabric.common.config.Config;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;

public final class ServerGameplayEvents {
   private ServerGameplayEvents() {
   }

   public static void register() {
      ServerAnimationEvents.register();
      UseItemCallback.EVENT.register((player, level, hand) -> {
         if (player instanceof ServerPlayer serverPlayer && Config.ENABLE_RIGHT_BUTTON.get() && !player.isCrouching()) {
            ServerAnimationManager.AnimationState state = ServerAnimationManager.get(serverPlayer);
            if (state != null && state.hand() == hand && ServerAnimationManager.matchesHeldStack(serverPlayer)) {
               return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
         }
         return InteractionResultHolder.pass(player.getItemInHand(hand));
      });
   }
}

