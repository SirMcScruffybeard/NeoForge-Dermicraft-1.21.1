package net.scruffy.dermicraft.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * Brain's selection/collision box is tucked in from a full cube to hug the model -- 1 pixel in on
 * every horizontal side, top brought down 2 pixels.
 */
public class BrainBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 12.0, 15.0);

    public BrainBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
