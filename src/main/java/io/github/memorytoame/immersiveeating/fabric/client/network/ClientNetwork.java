package io.github.memorytoame.immersiveeating.fabric.client.network;

import io.github.memorytoame.immersiveeating.fabric.client.ClientSoundHandler;
import io.github.memorytoame.immersiveeating.fabric.network.packet.AnimationPacket;
import io.github.memorytoame.immersiveeating.fabric.network.packet.CustomUsingPacket;
import io.github.memorytoame.immersiveeating.fabric.network.packet.FinishUsePacket;
import io.github.memorytoame.immersiveeating.fabric.network.packet.StopAnimationPacket;
import io.github.memorytoame.immersiveeating.fabric.network.packet.StopSoundPacket;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import java.util.function.Consumer;

public final class ClientNetwork {
   private static boolean registered;

   private ClientNetwork() {
   }

   public static void register() {
      if (registered) {
         return;
      }
      registered = true;
      ClientPlayNetworking.registerGlobalReceiver(StopSoundPacket.CHANNEL, (client, handler, buf, responseSender) -> {
         StopSoundPacket packet = StopSoundPacket.read(buf);
         client.execute(() -> ClientSoundHandler.stop(packet));
      });
   }

   public static void sendAnimation(UUID sessionId) {
      send(AnimationPacket.CHANNEL, new AnimationPacket(sessionId)::write);
   }

   public static void sendFinishUse(InteractionHand hand, UUID sessionId) {
      send(FinishUsePacket.CHANNEL, new FinishUsePacket(hand, sessionId)::write);
   }

   public static void sendCustomUsing(InteractionHand hand, Item item, int count) {
      send(CustomUsingPacket.CHANNEL, new CustomUsingPacket(hand, item, count)::write);
   }

   public static void sendStopAnimation(UUID sessionId) {
      send(StopAnimationPacket.CHANNEL, new StopAnimationPacket(sessionId)::write);
   }

   private static void send(ResourceLocation channel, Consumer<FriendlyByteBuf> encoder) {
      FriendlyByteBuf buf = PacketByteBufs.create();
      encoder.accept(buf);
      ClientPlayNetworking.send(channel, buf);
   }
}
