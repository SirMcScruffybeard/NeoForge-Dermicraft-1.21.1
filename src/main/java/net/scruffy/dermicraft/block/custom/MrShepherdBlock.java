package net.scruffy.dermicraft.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.MrShepherdBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.base.AbstractFarmBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Mr. Shepherd's block. Facing, ACTIVE, ticking, breaking and the interactions are all in {@link AbstractFarmBlock};
 * this adds the raised collision shape that lets it sit in a pen's fence line. */
public class MrShepherdBlock extends AbstractFarmBlock {

    public static final MapCodec<MrShepherdBlock> CODEC = simpleCodec(MrShepherdBlock::new);

    // Collision extends a full 24px up -- deliberately matching a vanilla fence rather than the
    // model's own ~22px hat. A Shepherd is set INTO the pen's fence line, so a plain full-cube block
    // would be a 1-block-tall gap in an otherwise 1.5-tall wall: an escape hatch animals could jump.
    // Blocks can still be placed on top; placement only checks the target position for replaceability
    // and entities, never a neighbour's collision shape (same as placing a block atop a fence).
    // The block above will visually clip the hat -- accepted, pending a new model.
    //
    // X AND Z both extend .25 block (4px) past each side (-4 to 20 instead of 0 to 16) to close the
    // sliver gap animals could otherwise squeeze through at the seam with the neighbouring fence
    // blocks. Both axes rather than just the fence-line axis -- this shape isn't rotated by FACING,
    // so it can't tell which way a given pen's wall actually runs.
    private static final VoxelShape COLLISION_SHAPE = Block.box(-4, 0, -4, 20, 24, 20);

    public MrShepherdBlock(Properties properties) {
        super(properties);
    }

    /**
     * Only the COLLISION shape is raised, not {@link #getShape} -- the selection outline stays a
     * normal cube so the outline doesn't overlap and fight with whatever block sits above.
     */
    @NotNull
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends AbstractFarmBlockEntity> getFarmBlockEntityType() {
        return ModBlockEntities.MR_SHEPHERD_BE.get();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new MrShepherdBlockEntity(blockPos, blockState);
    }
}
