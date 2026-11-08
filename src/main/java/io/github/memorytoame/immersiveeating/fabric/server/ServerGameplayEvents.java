package io.github.memorytoame.immersiveeating.fabric.server;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public final class ServerGameplayEvents {
   private ServerGameplayEvents() {
   }

   public static void register() {
      ServerAnimationEvents.register();
      UseItemCallback.EVENT.register((player, level, hand) -> {
         if (player instanceof ServerPlayer serverPlayer) {
            ServerAnimationManager.AnimationState state = ServerAnimationManager.get(serverPlayer);
            if (state != null && state.hand() == hand && ServerAnimationManager.matchesHeldStack(serverPlayer)) {
               return InteractionResult.FAIL;
            }
         }
         return InteractionResult.PASS;
      });
   }
}

