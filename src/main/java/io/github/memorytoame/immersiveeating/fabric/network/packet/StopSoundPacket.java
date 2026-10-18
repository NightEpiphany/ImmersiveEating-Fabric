package io.github.memorytoame.immersiveeating.fabric.network.packet;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record StopSoundPacket(UUID sessionId, ResourceLocation soundId) {
   public static final ResourceLocation CHANNEL = new ResourceLocation("food", "stop_sound");

   public static StopSoundPacket read(FriendlyByteBuf buf) {
      return new StopSoundPacket(buf.readUUID(), buf.readResourceLocation());
   }

   public void write(FriendlyByteBuf buf) {
      buf.writeUUID(this.sessionId);
      buf.writeResourceLocation(this.soundId);
   }
}
