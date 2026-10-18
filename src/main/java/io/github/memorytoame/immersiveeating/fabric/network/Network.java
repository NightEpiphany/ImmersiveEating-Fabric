package io.github.memorytoame.immersiveeating.fabric.network;

import io.github.memorytoame.immersiveeating.fabric.init.ModItems;
import io.github.memorytoame.immersiveeating.fabric.network.packet.AnimationPacket;
import io.github.memorytoame.immersiveeating.fabric.network.packet.CustomUsingPacket;
import io.github.memorytoame.immersiveeating.fabric.network.packet.FinishUsePacket;
import io.github.memorytoame.immersiveeating.fabric.network.packet.StopAnimationPacket;
import io.github.memorytoame.immersiveeating.fabric.network.packet.StopSoundPacket;
import io.github.memorytoame.immersiveeating.fabric.common.content.definition.FoodDefinitionManager;
import io.github.memorytoame.immersiveeating.fabric.server.ServerAnimationManager;
import io.github.memorytoame.immersiveeating.fabric.unique.UniqueItem;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;

public final class Network {
   private static boolean registered;

   private Network() {
   }

   public static void register() {
      if (registered) {
         return;
      }
      registered = true;
      ServerPlayNetworking.registerGlobalReceiver(AnimationPacket.CHANNEL, (server, player, handler, buf, responseSender) -> {
         AnimationPacket packet = AnimationPacket.read(buf);
         server.execute(() -> beginAnimation(player, packet.sessionId()));
      });
      ServerPlayNetworking.registerGlobalReceiver(FinishUsePacket.CHANNEL, (server, player, handler, buf, responseSender) -> {
         FinishUsePacket packet = FinishUsePacket.read(buf);
         server.execute(() -> finishUse(player, packet.hand(), packet.sessionId()));
      });
      ServerPlayNetworking.registerGlobalReceiver(CustomUsingPacket.CHANNEL, (server, player, handler, buf, responseSender) -> {
         CustomUsingPacket packet = CustomUsingPacket.read(buf);
         server.execute(() -> customUsing(player, packet.hand(), packet.customItem(), packet.count()));
      });
      ServerPlayNetworking.registerGlobalReceiver(StopAnimationPacket.CHANNEL, (server, player, handler, buf, responseSender) -> {
         StopAnimationPacket packet = StopAnimationPacket.read(buf);
         server.execute(() -> stopAnimation(player, packet.sessionId()));
      });
   }

   public static void sendStopSound(ServerPlayer player, UUID sessionId, ResourceLocation soundId) {
      FriendlyByteBuf buf = PacketByteBufs.create();
      new StopSoundPacket(sessionId, soundId).write(buf);
      ServerPlayNetworking.send(player, StopSoundPacket.CHANNEL, buf);
   }

   private static void beginAnimation(ServerPlayer player, UUID sessionId) {
      if (sessionId == null || player == null) {
         return;
      }
      ItemStack stack = player.getMainHandItem();
      if (stack.isEmpty() || stack.is(ModItems.EMPTY) || !FoodDefinitionManager.init_ItemList.contains(stack.getItem())) {
         return;
      }
      long id = GeoItem.getOrAssignId(stack, player.serverLevel());
      AnimationController<GeoAnimatable> controller = ModItems.EMPTY.getAnimatableInstanceCache()
         .getManagerForId(id).getAnimationControllers().get("eat");
      if ((controller == null || !controller.isPlayingTriggeredAnimation())
         && ServerAnimationManager.begin(player, sessionId, InteractionHand.MAIN_HAND, stack, id)) {
         ModItems.EMPTY.triggerAnim(player, id, "eat", "eat");
      }
   }

   private static void stopAnimation(ServerPlayer player, UUID sessionId) {
      if (ServerAnimationManager.matchesSession(player, sessionId, InteractionHand.MAIN_HAND)) {
         ServerAnimationManager.AnimationState state = ServerAnimationManager.get(player);
         if (state != null) {
            ModItems.EMPTY.stopTriggeredAnim(player, state.controllerId(), "eat", "eat");
         }
         ServerAnimationManager.clear(player);
      }
   }

   private static void finishUse(ServerPlayer player, InteractionHand hand, UUID sessionId) {
      if (player == null || sessionId == null || !ServerAnimationManager.matchesSession(player, sessionId, hand)
         || !ServerAnimationManager.matchesHeldStack(player)) {
         return;
      }
      ItemStack stack = player.getItemInHand(hand);
      ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (UniqueItem.UNIQUE_ITEM_MAP.containsKey(itemId)) {
         stack.shrink(1);
         player.setItemInHand(hand, stack);
         player.getInventory().add(new ItemStack(Items.BOWL));
      } else if (!stack.isEmpty()) {
         player.setItemInHand(hand, stack.finishUsingItem(player.level(), player));
      }
      ServerAnimationManager.clear(player);
   }

   private static void customUsing(ServerPlayer player, InteractionHand hand, Item item, int count) {
      if (player == null || item == null || item == Items.AIR) {
         return;
      }
      ItemStack stack = new ItemStack(item, Math.max(1, count));
      if (stack.isEdible() && stack.getUseDuration() > 0) {
         stack.finishUsingItem(player.level(), player);
      }
   }
}
