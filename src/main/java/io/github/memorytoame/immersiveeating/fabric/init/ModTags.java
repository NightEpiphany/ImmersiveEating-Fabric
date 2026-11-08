package io.github.memorytoame.immersiveeating.fabric.init;

import io.github.memorytoame.immersiveeating.fabric.KaleidoscopeImmersiveEating;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class ModTags {

    public static TagKey<EntityType<?>> ALLOW_INTERACTIONS = entityTag("allow_interactions");

    static TagKey<EntityType<?>> entityTag(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(KaleidoscopeImmersiveEating.MOD_ID, name));
    }

    public static void init() {}
}
