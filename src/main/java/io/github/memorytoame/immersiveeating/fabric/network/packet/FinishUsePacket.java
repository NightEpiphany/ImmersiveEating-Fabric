package io.github.memorytoame.immersiveeating.fabric.network.packet;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

public record FinishUsePacket(InteractionHand hand, UUID sessionId) {
   public static final ResourceLocation CHANNEL = new ResourceLocation("food", "finish_use");

   public static FinishUsePacket read(FriendlyByteBuf buf) {
      return new FinishUsePacket(buf.readEnum(InteractionHand.class), buf.readUUID());
   }

   public void write(FriendlyByteBuf buf) {
      buf.writeEnum(this.hand);
      buf.writeUUID(this.sessionId);
   }
}
