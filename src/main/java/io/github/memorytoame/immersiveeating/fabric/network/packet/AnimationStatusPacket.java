package io.github.memorytoame.immersiveeating.fabric.network.packet;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record AnimationStatusPacket(UUID sessionId, boolean accepted) {
   public static final ResourceLocation CHANNEL = new ResourceLocation("food", "animation_status");

   public static AnimationStatusPacket read(FriendlyByteBuf buf) {
      return new AnimationStatusPacket(buf.readUUID(), buf.readBoolean());
   }

   public void write(FriendlyByteBuf buf) {
      buf.writeUUID(this.sessionId);
      buf.writeBoolean(this.accepted);
   }
}
