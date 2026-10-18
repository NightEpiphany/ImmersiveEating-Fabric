package io.github.memorytoame.immersiveeating.fabric.network.packet;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;

public record CustomUsingPacket(InteractionHand hand, Item customItem, int count) {
   public static final ResourceLocation CHANNEL = new ResourceLocation("food", "custom_using");

   public static CustomUsingPacket read(FriendlyByteBuf buf) {
      InteractionHand hand = buf.readEnum(InteractionHand.class);
      Item item = BuiltInRegistries.ITEM.get(buf.readResourceLocation());
      return new CustomUsingPacket(hand, item, buf.readVarInt());
   }

   public void write(FriendlyByteBuf buf) {
      buf.writeEnum(this.hand);
      buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(this.customItem));
      buf.writeVarInt(this.count);
   }
}
