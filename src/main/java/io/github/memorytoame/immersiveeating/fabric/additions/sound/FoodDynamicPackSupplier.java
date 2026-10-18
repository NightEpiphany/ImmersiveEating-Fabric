package io.github.memorytoame.immersiveeating.fabric.additions.sound;

import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.Pack.Metadata;
import org.jspecify.annotations.NonNull;

public final class FoodDynamicPackSupplier implements Pack.ResourcesSupplier {

   @Override
   public @NonNull PackResources openPrimary(@NonNull PackLocationInfo location) {
      return new FoodDynamicPack();
   }

   @Override
   public @NonNull PackResources openFull(@NonNull PackLocationInfo location, @NonNull Metadata metadata) {
      return new FoodDynamicPack();
   }
}
