package io.github.memorytoame.immersiveeating.fabric.client.mixin;

import io.github.memorytoame.immersiveeating.fabric.additions.sound.FoodDynamicPack;
import java.util.function.BiConsumer;
import java.util.function.Function;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.Pack.ResourcesSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ClientPackSource.class)
public class ClientPackSourceMixin {
   @Inject(method = "populatePackList", at = @At("TAIL"))
   private void food$addDynamicPack(BiConsumer<String, Function<String, Pack>> consumer, CallbackInfo ci) {
      ResourcesSupplier resources = ignored -> new FoodDynamicPack();
      consumer.accept(
         "food_dynamic",
         ignored -> Pack.readMetaAndCreate(
            "food_dynamic",
            Component.literal("Kaleidoscope ImmersiveEating Resources"),
            false,
            resources,
            PackType.CLIENT_RESOURCES,
            Pack.Position.TOP,
            PackSource.BUILT_IN
         )
      );
   }
}

