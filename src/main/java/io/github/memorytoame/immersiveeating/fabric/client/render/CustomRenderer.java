package io.github.memorytoame.immersiveeating.fabric.client.render;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.DataTickets;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.memorytoame.immersiveeating.fabric.client.animation.CameraRotationComposer;
import io.github.memorytoame.immersiveeating.fabric.client.animation.GeckoLibAssetPaths;
import io.github.memorytoame.immersiveeating.fabric.client.animation.HeldItemMotion;
import io.github.memorytoame.immersiveeating.fabric.client.animation.VanillaArmRenderer;
import io.github.memorytoame.immersiveeating.fabric.common.content.definition.FoodDefinitionManager;
import io.github.memorytoame.immersiveeating.fabric.common.content.item.Empty;
import io.github.memorytoame.immersiveeating.fabric.init.ModItems;
import io.github.memorytoame.immersiveeating.fabric.utils.AnimationUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

/** 基于GeckoLib 5. */
@Environment(EnvType.CLIENT)
public class CustomRenderer extends GeoItemRenderer<Empty> {
   public static final Map<String, CustomRenderer> rendererMap = new HashMap<>(32);

   private final String itemId;
   private final List<VanillaArmRenderer.CapturedArm> capturedArms = new ArrayList<>(4);
   private boolean mirrorLeftHandModel;
   private boolean capturingCameraRotation;
   private PlayerRenderState playerRenderState;

   public CustomRenderer(String id) {
      super(new Model(getModelId(id)));
      this.itemId = getItemId(id);
   }


   public void renderByItem(
      ItemStack stack,
      ItemDisplayContext transformType,
      PoseStack poseStack,
      SubmitNodeCollector renderTasks,
      int packedLight,
      int packedOverlay
   ) {
      renderByItem(stack, transformType, poseStack, renderTasks, packedLight, packedOverlay, null);
   }


   public void renderByItem(
      ItemStack stack,
      ItemDisplayContext transformType,
      PoseStack poseStack,
      SubmitNodeCollector renderTasks,
      int packedLight,
      int packedOverlay,
      PlayerRenderState playerRenderState
   ) {
      Minecraft minecraft = Minecraft.getInstance();
      this.playerRenderState = playerRenderState;
      this.capturedArms.clear();
      GeoItemRenderer.RenderData renderData = new GeoItemRenderer.RenderData(
         stack,
         new ItemStackRenderState(),
         transformType,
         minecraft.level,
         minecraft.player
      );
      GeoRenderState renderState = fillRenderState(
         ModItems.EMPTY,
         renderData,
         createRenderState(ModItems.EMPTY, renderData),
         minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false)
      );
      renderState.addGeckolibData(DataTickets.PACKED_LIGHT, packedLight);
      renderState.addGeckolibData(DataTickets.PACKED_OVERLAY, packedOverlay);
      try {
         submit(renderState, poseStack, renderTasks, 0);
      } finally {
         this.playerRenderState = null;
         this.capturedArms.clear();
      }
   }


   @Override
   public void postRenderPass(@NonNull RenderPassInfo<GeoRenderState> renderPassInfo, @NonNull SubmitNodeCollector renderTasks) {
      if (this.playerRenderState == null || !isFirstPerson(renderPassInfo)) {
         return;
      }

      this.capturedArms.clear();
      renderPassInfo.renderPosed(() -> {
         PoseStack poseStack = renderPassInfo.poseStack();
         poseStack.pushPose();
         poseStack.last().set(renderPassInfo.getModelRenderMatrixPose());
         for (GeoBone bone : renderPassInfo.model().boneLookup().get().values()) {
            if (!VanillaArmRenderer.isArmBone(bone)) {
               continue;
            }

            VanillaArmRenderer.CapturedArm capturedArm = VanillaArmRenderer.captureArmPose(
               poseStack,
               bone,
               this.mirrorLeftHandModel
            );
            if (capturedArm != null) {
               this.capturedArms.add(capturedArm);
            }
         }
         poseStack.popPose();
      });

      VanillaArmRenderer.renderCapturedArms(
         this.capturedArms,
         renderTasks,
         renderPassInfo.packedLight(),
         this.playerRenderState
      );
   }

   private static boolean isFirstPerson(RenderPassInfo<GeoRenderState> renderPassInfo) {
      ItemDisplayContext transformType = renderPassInfo.getOrDefaultGeckolibData(
         DataTickets.ITEM_RENDER_PERSPECTIVE,
         ItemDisplayContext.NONE
      );
      return transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
         || transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
   }

   @Override
   public RenderType getRenderType(GeoRenderState renderState, @NonNull Identifier texture) {
      ItemDisplayContext transformType = renderState.getOrDefaultGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.NONE);
      return transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
         ? RenderTypes.entityTranslucent(texture)
         : RenderTypes.entityTranslucentCull(texture);
   }

   @Override
   public void adjustRenderPose(@NonNull RenderPassInfo<GeoRenderState> renderPassInfo) {
      super.adjustRenderPose(renderPassInfo);

      float partialTick = renderPassInfo.getOrDefaultGeckolibData(
         DataTickets.PARTIAL_TICK,
         Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)
      );
      ItemDisplayContext transformType = renderPassInfo.getOrDefaultGeckolibData(
         DataTickets.ITEM_RENDER_PERSPECTIVE,
         ItemDisplayContext.NONE
      );
      boolean firstPerson = transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
         || transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
      this.mirrorLeftHandModel = transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
      this.capturingCameraRotation = firstPerson && Empty.isAnimationPlaying();

      if (firstPerson) {
         CameraRotationComposer.clearBlockbenchRotation();
         HeldItemMotion.applyIdleMotion(renderPassInfo.poseStack(), partialTick);
         HeldItemMotion.applyWalkMotion(renderPassInfo.poseStack(), partialTick);
         HeldItemMotion.applyInertiaMotion(renderPassInfo.poseStack(), partialTick);
         HeldItemMotion.applyJumpMotion(renderPassInfo.poseStack(), partialTick);
         HeldItemMotion.applyCrouchMotion(renderPassInfo.poseStack(), partialTick);
      }

      if (this.mirrorLeftHandModel) {
         renderPassInfo.poseStack().scale(-1.0F, 1.0F, 1.0F);
      }
   }

   @Override
   public void adjustModelBonesForRender(RenderPassInfo<GeoRenderState> renderPassInfo, @NonNull BoneSnapshots snapshots) {
      float partialTick = renderPassInfo.getOrDefaultGeckolibData(
         DataTickets.PARTIAL_TICK,
         Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)
      );
      boolean hideInvisibleBones = shouldHideInvisibleBone(partialTick);

      for (GeoBone bone : renderPassInfo.model().boneLookup().get().values()) {
         if (this.capturingCameraRotation && "camera".equals(bone.name())) {
            snapshots.ifPresent(
               bone.name(),
               snapshot -> CameraRotationComposer.captureBlockbenchRotationRadians(
                  snapshot.getRotX(), snapshot.getRotY(), snapshot.getRotZ()
               )
            );
         }

         if (isFirstPerson(renderPassInfo) && VanillaArmRenderer.isArmBone(bone)) {
            snapshots.ifPresent(bone.name(), snapshot -> snapshot.skipRender(true));
         }

         if (hideInvisibleBones && isInvisibleBone(bone) && !hasInvisibleBoneParent(bone)) {
            snapshots.ifPresent(bone.name(), snapshot -> snapshot.setTranslateZ(-16000.0F));
         }
      }
   }

   private boolean shouldHideInvisibleBone(float partialTick) {
      Minecraft minecraft = Minecraft.getInstance();
      boolean armSwinging = minecraft.player != null
         && minecraft.player.getSwingAnimation(partialTick) > 1.0E-4F;
      boolean eatAnimationPlaying = AnimationUtils.isAnimationLocked() && Empty.isEatAnimationPlaying();
      return armSwinging || !eatAnimationPlaying;
   }

   private boolean isInvisibleBone(GeoBone bone) {
      return FoodDefinitionManager.isInvisible(this.itemId, bone.name());
   }

   private boolean hasInvisibleBoneParent(GeoBone bone) {
      for (GeoBone parent = bone.parent(); parent != null; parent = parent.parent()) {
         if (isInvisibleBone(parent)) {
            return true;
         }
      }

      return false;
   }

   private static String getModelId(String id) {
      Identifier identifier = Identifier.tryParse(id);
      return identifier == null ? id : identifier.getPath();
   }

   private static String getItemId(String id) {
      Identifier identifier = Identifier.tryParse(id);
      return identifier == null ? id : identifier.toString();
   }

   /**
    * GeckoLib 5 derives item resources from the cache id {@code food:item/<id>}.
    * The corresponding files live under {@code geckolib/models/item} and
    * {@code geckolib/animations/item}; DefaultedItemGeoModel keeps those paths
    * consistent for the model, texture, and animation lookups.
    */
   private static final class Model extends DefaultedItemGeoModel<Empty> {
      private Model(String id) {
         super(GeckoLibAssetPaths.itemBase(id));
      }
   }
}
