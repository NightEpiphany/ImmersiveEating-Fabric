package io.github.memorytoame.immersiveeating.fabric.init;

import io.github.memorytoame.immersiveeating.fabric.KaleidoscopeImmersiveEating;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class ModTags {
   public static final TagKey<EntityType<?>> ALLOW_INTERACTIONS = TagKey.create(
      Registries.ENTITY_TYPE, KaleidoscopeImmersiveEating.id("allow_interactions")
   );

   private ModTags() {
   }
}
