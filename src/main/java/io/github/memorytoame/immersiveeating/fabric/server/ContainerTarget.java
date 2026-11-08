package io.github.memorytoame.immersiveeating.fabric.server;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

import static io.github.memorytoame.immersiveeating.fabric.init.ModTags.ALLOW_INTERACTIONS;

public final class ContainerTarget {
    private static final Predicate<BlockState> BLOCK_WHITELIST = state -> state.is(Blocks.CAKE) || state.is(BlockTags.BEDS) || state.is(BlockTags.DOORS);

    private ContainerTarget() {
    }

    public static boolean allowsInteraction(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return BLOCK_WHITELIST.test(state) || state.getMenuProvider(level, pos) != null || level.getBlockEntity(pos) instanceof MenuProvider;
    }

    public static boolean allowsInteraction(Entity entity) {
        return entity.getType().is(ALLOW_INTERACTIONS);
    }
}
