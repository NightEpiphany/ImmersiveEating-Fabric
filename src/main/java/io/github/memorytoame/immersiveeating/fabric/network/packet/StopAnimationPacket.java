package io.github.memorytoame.immersiveeating.fabric.network.packet;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record StopAnimationPacket(UUID sessionId) {
   public static final ResourceLocation CHANNEL = new ResourceLocation("food", "stop_animation");

   public static StopAnimationPacket read(FriendlyByteBuf buf) {
      return new StopAnimationPacket(buf.readUUID());
   }

   public void write(FriendlyByteBuf buf) {
      buf.writeUUID(this.sessionId);
   }
}
