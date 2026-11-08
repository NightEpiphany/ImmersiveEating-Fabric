package io.github.memorytoame.immersiveeating.fabric.client.events;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static io.github.memorytoame.immersiveeating.fabric.init.ModTags.ALLOW_INTERACTIONS;

final class ContainerTarget {
   private ContainerTarget() {
   }

   static boolean allowsInteraction(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return state.is(Blocks.CAKE)
         || state.is(BlockTags.BEDS)
         || state.is(BlockTags.DOORS)
         || state.getMenuProvider(level, pos) != null
         || level.getBlockEntity(pos) instanceof MenuProvider;
   }

   static boolean allowsInteraction(Entity entity) {
      return entity.getType().is(ALLOW_INTERACTIONS);
   }
}
