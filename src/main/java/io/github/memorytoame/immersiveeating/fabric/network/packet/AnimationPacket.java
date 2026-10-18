package io.github.memorytoame.immersiveeating.fabric.network.packet;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record AnimationPacket(UUID sessionId) {
   public static final ResourceLocation CHANNEL = new ResourceLocation("food", "animation");

   public static AnimationPacket read(FriendlyByteBuf buf) {
      return new AnimationPacket(buf.readUUID());
   }

   public void write(FriendlyByteBuf buf) {
      buf.writeUUID(this.sessionId);
   }
}
