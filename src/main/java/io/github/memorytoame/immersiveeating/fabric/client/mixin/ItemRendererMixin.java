package io.github.memorytoame.immersiveeating.fabric.client.mixin;

import com.geckolib.GeckoLibConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.memorytoame.immersiveeating.fabric.client.animation.ClientAnimationManager;
import io.github.memorytoame.immersiveeating.fabric.client.render.CustomRenderer;
import io.github.memorytoame.immersiveeating.fabric.client.render.TransformationMatrixProperties;
import io.github.memorytoame.immersiveeating.fabric.common.content.definition.FoodDefinitionManager;
import io.github.memorytoame.immersiveeating.fabric.init.ModItems;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Submits GeckoLib's animated food model in the 26.3 first-person renderer. */
@Environment(EnvType.CLIENT)
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class ItemRendererMixin {
   /**
    * Vanilla builds the first-person item transform before submitting the item
    * render state. Replacing that submission keeps all vanilla hand, swing,
    * use-animation, and equip-height transforms in the pose stack.
    */
   @Inject(
      method = "submitArmWithItem",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
         ordinal = 1
      ),
      cancellable = true
   )
   private void food$renderCustomMainHand(
      PlayerRenderState playerState,
      FirstPersonHandsAndItemsRenderState state,
      float partialTicks,
      float xRot,
      InteractionHand hand,
      float attack,
      ItemStack itemStack,
      float inverseArmHeight,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int lightCoords,
      CallbackInfo ci
   ) {
      if (hand != InteractionHand.MAIN_HAND || !FoodDefinitionManager.init_ItemList.contains(itemStack.getItem())) {
         return;
      }

      // A normal right-click food use is rendered by vanilla. The custom
      // model is reserved for the explicit GeckoLib eating session, which
      // leaves the player's vanilla isUsingItem flag unset.
      if (food$isVanillaUse(playerState, hand, itemStack)) {
         return;
      }

      HumanoidArm mainArm = playerState.avatarRenderState == null
         ? HumanoidArm.RIGHT
         : playerState.avatarRenderState.mainArm;
      ItemDisplayContext displayContext = mainArm == HumanoidArm.LEFT
         ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND
         : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
      Identifier id = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
      poseStack.pushPose();
      if (mainArm == HumanoidArm.LEFT) {
         poseStack.translate(
            -1.0F - TransformationMatrixProperties.NORMAL.getTranslateX(),
            TransformationMatrixProperties.NORMAL.getTranslateY(),
            TransformationMatrixProperties.NORMAL.getTranslateZ()
         );
      } else {
         poseStack.translate(
            TransformationMatrixProperties.NORMAL.getTranslateX(),
            TransformationMatrixProperties.NORMAL.getTranslateY(),
            TransformationMatrixProperties.NORMAL.getTranslateZ()
         );
      }

      CustomRenderer customRenderer = CustomRenderer.rendererMap.computeIfAbsent(id.getPath(), _ -> new CustomRenderer(id.toString()));
      ItemStack proxy = new ItemStack(ModItems.EMPTY);
      Long stackId = itemStack.get(GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT.get());
      long instanceId = stackId == null ? Long.MAX_VALUE : stackId;
      if (instanceId == Long.MAX_VALUE && playerState.avatarRenderState != null) {
         instanceId = ClientAnimationManager.getOrCreateControllerId(Minecraft.getInstance().player);
      }

      if (instanceId != Long.MAX_VALUE) {
         proxy.set(GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT.get(), instanceId);
      }

      customRenderer.renderByItem(
         proxy,
         displayContext,
         poseStack,
         submitNodeCollector,
         lightCoords,
         OverlayTexture.NO_OVERLAY,
         playerState
      );
      poseStack.popPose();
      // The injected call is inside vanilla's method-level pushPose scope.
      poseStack.popPose();
      ci.cancel();
   }

   @Unique
   private static boolean food$isVanillaUse(PlayerRenderState playerState, InteractionHand hand, ItemStack stack) {
      if (playerState.avatarRenderState == null
         || !playerState.avatarRenderState.isUsingItem
         || playerState.avatarRenderState.useItemHand != hand) {
         return false;
      }

      Minecraft mc = Minecraft.getInstance();
      return mc.player != null
         && mc.player.isUsingItem()
         && mc.player.getUsedItemHand() == hand
         && ItemStack.isSameItemSameComponents(mc.player.getUseItem(), stack);
   }

}
