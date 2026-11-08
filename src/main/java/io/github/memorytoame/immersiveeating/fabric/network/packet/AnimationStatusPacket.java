package io.github.memorytoame.immersiveeating.fabric.network.packet;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record AnimationStatusPacket(UUID sessionId, boolean accepted) implements CustomPacketPayload {
   public static final Type<AnimationStatusPacket> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("food", "animation_status")
   );
   public static final StreamCodec<FriendlyByteBuf, AnimationStatusPacket> STREAM_CODEC = new StreamCodec<>() {
      @Override
      public @NotNull AnimationStatusPacket decode(FriendlyByteBuf buf) {
         return new AnimationStatusPacket(buf.readUUID(), buf.readBoolean());
      }

      @Override
      public void encode(FriendlyByteBuf buf, AnimationStatusPacket packet) {
         buf.writeUUID(packet.sessionId());
         buf.writeBoolean(packet.accepted());
      }
   };

   @Override
   public @NotNull Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
