package io.github.memorytoame.immersiveeating.fabric.common.content.item;

import io.github.memorytoame.immersiveeating.fabric.common.content.definition.FoodDefinitionManager;
import io.github.memorytoame.immersiveeating.fabric.client.render.CustomRenderer;
import io.github.memorytoame.immersiveeating.fabric.init.ModItems;
import io.github.memorytoame.immersiveeating.fabric.unique.UniqueItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Empty extends Item implements GeoItem {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
   private static Consumer<ItemStack> clientUseReequipHandler = ignored -> {};
   private static Runnable clientCameraShakeHandler = () -> {};
   private static Supplier<ItemStack> clientAnimationStackSupplier = () -> ItemStack.EMPTY;
   private static Runnable clientAnimationStackClearHandler = () -> {};
   private static Runnable clientHotbarUnlockHandler = () -> {};
   private static Supplier<Item> clientGetMainHandItem = () -> null;
   private static Supplier<UUID> clientAnimationSessionSupplier = () -> null;
   private static Supplier<Long> clientAnimationControllerSupplier = () -> Long.MAX_VALUE;
   private static Consumer<ResourceLocation> clientSoundHandler = ignored -> {};
   private static Runnable clientSoundStopHandler = () -> {};
   private static BiConsumer<InteractionHand, Item> clientCustomUseHandler = (ignoredHand, ignoredItem) -> {};
   private static BiConsumer<InteractionHand, UUID> clientFinishUseHandler = (ignoredHand, ignoredSession) -> {};
   private static final Map<String, ClientAnimationContext> clientAnimationStacks = new HashMap<>(3);
   private final List<AnimationController<Empty>> reg_controllers = new ArrayList<>(3);

   public Empty(Properties pProperties) {
      super(pProperties);
      GeoItem.registerSyncedAnimatable(this);
   }

   @SuppressWarnings("unused")
   public static void setClientHooks(
      Consumer<ItemStack> useReequipHandler,
      Runnable cameraShakeHandler,
      Supplier<ItemStack> animationStackSupplier,
      Runnable animationStackClearHandler,
      Runnable hotbarUnlockHandler,
      Supplier<Item> mainHandItem,
      Supplier<UUID> animationSessionSupplier,
      Supplier<Long> animationControllerSupplier
   ) {
      clientUseReequipHandler = useReequipHandler;
      clientCameraShakeHandler = cameraShakeHandler;
      clientAnimationStackSupplier = animationStackSupplier;
      clientAnimationStackClearHandler = animationStackClearHandler;
      clientHotbarUnlockHandler = hotbarUnlockHandler;
      clientGetMainHandItem = mainHandItem;
      clientAnimationSessionSupplier = animationSessionSupplier;
      clientAnimationControllerSupplier = animationControllerSupplier;
   }

   public static void setClientSoundHooks(Consumer<ResourceLocation> soundHandler, Runnable soundStopHandler) {
      clientSoundHandler = soundHandler;
      clientSoundStopHandler = soundStopHandler;
   }

   public static void setClientNetworkHooks(
      BiConsumer<InteractionHand, Item> customUseHandler,
      BiConsumer<InteractionHand, UUID> finishUseHandler
   ) {
      clientCustomUseHandler = customUseHandler;
      clientFinishUseHandler = finishUseHandler;
   }

   public static void stopEatAnimation(long instanceId) {
      clientSoundStopHandler.run();
      Empty empty = ModItems.EMPTY;
      AnimationController<?> controller = empty.getAnimatableInstanceCache()
         .getManagerForId(instanceId)
         .getAnimationControllers()
         .get("eat");
      if (controller != null) {
         controller.stop();
      }
      clientAnimationStacks.remove("eat");
   }

   public static boolean isEatAnimationPlaying() {
      return isClientAnimationPlaying(clientAnimationStackSupplier.get(), "eat");
   }

   public static boolean isAnimationPlaying() {
      return isClientAnimationPlaying(clientAnimationStackSupplier.get(), "eat")
         || isStoredAnimationPlaying("switchin")
         || isStoredAnimationPlaying("switchout");
   }

   public static boolean isClientAnimationPlaying(ItemStack stack, String controllerName) {
      if (stack != null && !stack.isEmpty()) {
         ClientAnimationContext context = clientAnimationStacks.get(controllerName);
         if (context != null && ItemStack.isSameItemSameTags(context.stack(), stack)) {
            return isControllerPlaying(context.instanceId(), controllerName);
         }

         return isControllerPlaying(resolveControllerId(stack), controllerName);
      } else {
         return false;
      }
   }

   public static void stopClientAnimation(ItemStack stack, String controllerName) {
      if (stack != null && !stack.isEmpty()) {
         long instanceId = resolveControllerId(stack);
         AnimationController<?> controller = ModItems.EMPTY.getAnimatableInstanceCache()
            .getManagerForId(instanceId)
            .getAnimationControllers()
            .get(controllerName);
         if (controller != null) {
            controller.stop();
         }
         clientAnimationStacks.remove(controllerName);
      }
   }

   public static void triggerClientAnimation(ItemStack stack, String controllerName) {
      if (stack != null && !stack.isEmpty()) {
         Empty empty = ModItems.EMPTY;
         long instanceId = resolveControllerId(stack);
         AnimationController<?> controller = empty.getAnimatableInstanceCache()
            .getManagerForId(instanceId)
            .getAnimationControllers()
            .get(controllerName);
         if (controller != null) {
            clientSoundStopHandler.run();
            controller.forceAnimationReset();
            if (controller.tryTriggerAnimation(controllerName)) {
               clientAnimationStacks.put(controllerName, new ClientAnimationContext(stack.copy(), instanceId));
            }
         }
      }
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      this.reg_controllers.add(this.createControl("eat"));
      this.reg_controllers.add(this.createControl("switchin"));
      this.reg_controllers.add(this.createControl("switchout"));

      for (AnimationController<Empty> controller : this.reg_controllers) {
         controller.setSoundKeyframeHandler(
            keyFrames -> {
               String controllerName = keyFrames.getController().getName();
               ClientAnimationContext context = clientAnimationStacks.get(controllerName);
               ItemStack animationStack = context == null ? clientAnimationStackSupplier.get() : context.stack();
               if (animationStack != null && !animationStack.isEmpty()) {
                  ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(animationStack.getItem());
                  String soundKey = keyFrames.getKeyframeData().getSound();
                  if ("pickup".equals(soundKey)) {
                     clientSoundHandler.accept(new ResourceLocation("minecraft", "entity.item.pickup"));
                  }

                  clientCameraShakeHandler.run();
                  ResourceLocation soundId = FoodDefinitionManager.getSound(itemId.toString(), soundKey);
                  if (soundId != null) {
                     Item mainHandItem = clientGetMainHandItem.get();
                     ResourceLocation mainHandId = mainHandItem == null ? null : BuiltInRegistries.ITEM.getKey(mainHandItem);
                     if ("eat".equals(controllerName)
                        && mainHandId != null
                        && UniqueItem.UNIQUE_ITEM_MAP.get(mainHandId) != null) {
                        Item child_item = BuiltInRegistries.ITEM
                           .get(UniqueItem.UNIQUE_ITEM_MAP.get(mainHandId));
                        if (child_item != null) {
                           clientCustomUseHandler.accept(InteractionHand.MAIN_HAND, child_item);
                        }
                     } else {
                        clientSoundHandler.accept(soundId);
                     }
                  }
               }
            }
         );
         controller.setCustomInstructionKeyframeHandler(keyFrames -> {
            if (keyFrames.getKeyframeData().getInstructions().contains("finished")) {
               ClientAnimationContext context = clientAnimationStacks.get(controller.getName());
               ItemStack animationStack = context == null ? clientAnimationStackSupplier.get() : context.stack();
               controller.stop();
               clientAnimationStacks.remove(controller.getName());
               if (animationStack != null && !animationStack.isEmpty()) {
                  clientAnimationStackClearHandler.run();
                  UUID sessionId = clientAnimationSessionSupplier.get();
                  if (sessionId != null) {
                     clientFinishUseHandler.accept(InteractionHand.MAIN_HAND, sessionId);
                  }

                  clientUseReequipHandler.accept(animationStack.copy());
               }

               clientHotbarUnlockHandler.run();
            }
         });
         controllers.add(controller);
      }
   }

   @Override
   public void createRenderer(Consumer<Object> consumer) {
      consumer.accept(new CustomRenderer("empty"));
   }

   @Override
   public Supplier<Object> getRenderProvider() {
      return this.renderProvider;
   }

   private AnimationController<Empty> createControl(String name) {
      AnimationController<Empty> controller = new AnimationController<>(this, name, state -> PlayState.STOP);
      return controller.triggerableAnim(name, RawAnimation.begin().thenPlay(name));
   }

   private static boolean isStoredAnimationPlaying(String controllerName) {
      ClientAnimationContext context = clientAnimationStacks.get(controllerName);
      if (context == null || context.stack().isEmpty()) {
         return false;
      }

      boolean playing = isControllerPlaying(context.instanceId(), controllerName);
      if (!playing) {
         clientAnimationStacks.remove(controllerName);
      }
      return playing;
   }

   private static boolean isControllerPlaying(long instanceId, String controllerName) {
      AnimationController<?> controller = ModItems.EMPTY.getAnimatableInstanceCache()
         .getManagerForId(instanceId)
         .getAnimationControllers()
         .get(controllerName);
      return controller != null && controller.isPlayingTriggeredAnimation();
   }

   private static long resolveControllerId(ItemStack stack) {
      long stackId = stack != null && !stack.isEmpty() ? GeoItem.getId(stack) : Long.MAX_VALUE;
      return stackId == Long.MAX_VALUE ? clientAnimationControllerSupplier.get() : stackId;
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   public double getBoneResetTime() {
      return 0.0;
   }

   private record ClientAnimationContext(ItemStack stack, long instanceId) {
   }
}

