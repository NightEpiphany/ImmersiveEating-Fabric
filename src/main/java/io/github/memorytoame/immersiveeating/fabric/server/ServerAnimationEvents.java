package io.github.memorytoame.immersiveeating.fabric.server;

import io.github.memorytoame.immersiveeating.fabric.network.Network;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

public final class ServerAnimationEvents {
   private ServerAnimationEvents() {
   }

   public static void register() {
      ServerTickEvents.END_SERVER_TICK.register(server -> {
         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerAnimationManager.AnimationState state = ServerAnimationManager.get(player);
            if (state != null && !ServerAnimationManager.matchesHeldStack(player)) {
               ServerAnimationManager.clear(player);
               Network.sendAnimationStatus(player, state.sessionId(), false);
            }
         }
      });
      ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ServerAnimationManager.clear(handler.getPlayer()));
      ServerLifecycleEvents.SERVER_STOPPING.register(server -> ServerAnimationManager.clearAll());
   }
}

