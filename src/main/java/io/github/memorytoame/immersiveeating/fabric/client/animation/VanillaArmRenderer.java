package io.github.memorytoame.immersiveeating.fabric.client.animation;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoQuad;
import com.geckolib.cache.model.GeoVertex;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.memorytoame.immersiveeating.fabric.client.render.ArmModelOffset;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Extracts the transform of an arm bone from an animated GeckoLib model.
 *
 * <p>Minecraft 26.3 submits first-person hands through
 * {@code FirstPersonHandsAndItemsRenderer} and no longer exposes the old
 * {@code PlayerRenderer#renderLeftHand/renderRightHand} methods. The
 * extraction helpers remain useful to renderers that submit their own hand
 * geometry; the old direct buffer rendering entry point was removed.</p>
 */
@Environment(EnvType.CLIENT)
public final class VanillaArmRenderer {
   private static final float PIXEL = 0.0625F;

   private VanillaArmRenderer() {
   }

   public static boolean isArmBone(GeoBone bone) {
      return isDedicatedAnchor(bone) || getArm(bone) != null && !hasDedicatedAnchorChild(bone);
   }

   /** Captures a model-space arm transform before the arm bone is skipped. */
   public static CapturedArm captureArmPose(PoseStack poseStack, GeoBone bone, boolean mirrorModel) {
      HumanoidArm arm = getArm(bone);
      GeoCube armCube = findArmCube(bone);
      if (arm == null || armCube == null) {
         return null;
      }

      PoseStack bonePose = new PoseStack();
      bonePose.last().pose().set(poseStack.last().pose());
      bonePose.last().normal().set(poseStack.last().normal());
      applyBoneHierarchy(bonePose, bone);
      bonePose.pushPose();
      try {
         HumanoidArm renderedArm = mirrorModel ? opposite(arm) : arm;
         Matrix4f armPose = createPreviewCubeArmPose(
            bonePose,
            renderedArm,
            armCube,
            ArmModelOffset.forPlayer(Minecraft.getInstance().player).matrixFor(renderedArm)
         );
         return new CapturedArm(renderedArm, armPose, new Matrix3f(armPose).normal());
      } finally {
         bonePose.popPose();
      }
   }

   private static void applyBoneHierarchy(PoseStack poseStack, GeoBone bone) {
      if (bone.parent() != null) {
         applyBoneHierarchy(poseStack, bone.parent());
      }

      RenderUtil.prepMatrixForBone(poseStack, bone);
   }

   /** Submits vanilla player hands at the animated arm bone transforms. */
   public static void renderCapturedArms(
      List<CapturedArm> capturedArms,
      SubmitNodeCollector renderTasks,
      int packedLight,
      AbstractClientPlayer playerState
   ) {
      if (capturedArms.isEmpty() || playerState == null) {
         return;
      }

      if (playerState.isInvisible() || playerState.getSkin() == null || playerState.getSkin().body() == null) {
         return;
      }

      EntityRenderer<? super AbstractClientPlayer, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(playerState);
      Identifier skinTexture = playerState.getSkin().body().texturePath();
      for (CapturedArm capturedArm : capturedArms) {
         PoseStack armPose = new PoseStack();
         armPose.last().pose().set(capturedArm.pose());
         armPose.last().normal().set(capturedArm.normal());
         boolean hasSleeve = capturedArm.arm() == HumanoidArm.RIGHT
            ? playerState.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE)
            : playerState.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
         if (renderer instanceof AvatarRenderer avatarRenderer) {
            if (capturedArm.arm() == HumanoidArm.RIGHT) {
               avatarRenderer.renderRightHand(armPose, renderTasks, packedLight, skinTexture, hasSleeve);
            } else {
               avatarRenderer.renderLeftHand(armPose, renderTasks, packedLight, skinTexture, hasSleeve);
            }
         }
      }
   }

   private static HumanoidArm opposite(HumanoidArm arm) {
      return arm == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
   }

   private static GeoCube findArmCube(GeoBone bone) {
      if (!(bone instanceof CuboidGeoBone cuboid) || cuboid.cubes.length == 0) {
         return null;
      }

      return cuboid.cubes[0];
   }

   private static HumanoidArm getArm(GeoBone bone) {
      return switch (normalize(bone.name())) {
         case "rightarm", "righthandpos" -> HumanoidArm.RIGHT;
         case "leftarm", "lefthandpos" -> HumanoidArm.LEFT;
         default -> null;
      };
   }

   private static boolean isDedicatedAnchor(GeoBone bone) {
      String normalizedName = normalize(bone.name());
      return normalizedName.equals("lefthandpos") || normalizedName.equals("righthandpos");
   }

   private static boolean hasDedicatedAnchorChild(GeoBone bone) {
      for (GeoBone child : bone.children()) {
         if (isDedicatedAnchor(child)) {
            return true;
         }
      }

      return false;
   }

   private static String normalize(String name) {
      return name.replace("_", "").replace(" ", "").toLowerCase(Locale.ROOT);
   }

   private static Matrix4f createPreviewCubeArmPose(PoseStack poseStack, HumanoidArm arm, GeoCube cube, Matrix4f localOffset) {
      Bounds bounds = findBounds(cube);
      Vector3f previewCenter = new Vector3f(
         (bounds.minX() + bounds.maxX()) * 0.5F,
         (bounds.minY() + bounds.maxY()) * 0.5F,
         (bounds.minZ() + bounds.maxZ()) * 0.5F
      );

      Matrix4f cubeTransform = new Matrix4f().identity();
      cubeTransform.translate((float)cube.pivot().x() * PIXEL, (float)cube.pivot().y() * PIXEL, (float)cube.pivot().z() * PIXEL);
      cubeTransform.rotateZ((float)cube.rotation().z());
      cubeTransform.rotateY((float)cube.rotation().y());
      cubeTransform.rotateX((float)cube.rotation().x());
      cubeTransform.translate(
         -(float)cube.pivot().x() * PIXEL,
         -(float)cube.pivot().y() * PIXEL,
         -(float)cube.pivot().z() * PIXEL
      );
      Vector3f targetCenter = cubeTransform.transformPosition(previewCenter);

      Vector3f vanillaCenter = arm == HumanoidArm.RIGHT
         ? new Vector3f(-6.0F, 6.0F, 0.0F).mul(PIXEL)
         : new Vector3f(6.0F, 6.0F, 0.0F).mul(PIXEL);
      Matrix4f vanillaToPreview = new Matrix4f().identity();
      vanillaToPreview.translate(targetCenter);
      vanillaToPreview.rotateZ((float)cube.rotation().z());
      vanillaToPreview.rotateY((float)cube.rotation().y());
      vanillaToPreview.rotateX((float)cube.rotation().x());
      vanillaToPreview.rotateZ((float)Math.PI);

      float sizeX = (float)cube.size().x();
      float sizeY = (float)cube.size().y();
      float sizeZ = (float)cube.size().z();
      if (sizeX <= 0.0F || sizeY <= 0.0F || sizeZ <= 0.0F) {
         sizeX = (bounds.maxX() - bounds.minX()) / PIXEL;
         sizeY = (bounds.maxY() - bounds.minY()) / PIXEL;
         sizeZ = (bounds.maxZ() - bounds.minZ()) / PIXEL;
      }

      vanillaToPreview.scale(safeScale(sizeX, 4.0F), safeScale(sizeY, 12.0F), safeScale(sizeZ, 4.0F));
      vanillaToPreview.translate(-vanillaCenter.x(), -vanillaCenter.y(), -vanillaCenter.z());
      return new Matrix4f(poseStack.last().pose()).mul(localOffset).mul(vanillaToPreview);
   }

   private static float safeScale(float size, float vanillaSize) {
      return size > 0.0F ? size / vanillaSize : 1.0F;
   }

   private static Bounds findBounds(GeoCube cube) {
      float minX = Float.POSITIVE_INFINITY;
      float minY = Float.POSITIVE_INFINITY;
      float minZ = Float.POSITIVE_INFINITY;
      float maxX = Float.NEGATIVE_INFINITY;
      float maxY = Float.NEGATIVE_INFINITY;
      float maxZ = Float.NEGATIVE_INFINITY;

      GeoQuad[] quads = cube.quads();
      if (quads != null) {
         for (GeoQuad quad : quads) {
            if (quad == null) {
               continue;
            }

            for (GeoVertex vertex : quad.vertices()) {
               minX = Math.min(minX, vertex.posX());
               minY = Math.min(minY, vertex.posY());
               minZ = Math.min(minZ, vertex.posZ());
               maxX = Math.max(maxX, vertex.posX());
               maxY = Math.max(maxY, vertex.posY());
               maxZ = Math.max(maxZ, vertex.posZ());
            }
         }
      }

      return Float.isInfinite(minX)
         ? new Bounds(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
         : new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
   }

   private record Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
   }

   public record CapturedArm(HumanoidArm arm, Matrix4f pose, Matrix3f normal) {
   }
}
