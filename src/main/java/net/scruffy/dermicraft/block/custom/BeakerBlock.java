package net.scruffy.dermicraft.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scruffy.dermicraft.block.entity.custom.BeakerBlockEntity;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.item.custom.ForcepsItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BeakerBlock extends ModBaseEntityBlock {

    public static final MapCodec<BeakerBlock> CODEC = simpleCodec(BeakerBlock::new);

    public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("light_level", 0, 15);

    private static final int FULL_THRESHOLD = 950;

    public BeakerBlock(Properties properties) {
        super(properties
                .noLootTable()
                .noOcclusion()
                .strength(0.3F)
                .sound(SoundType.GLASS)
                .lightLevel(state -> state.getValue(LIGHT_LEVEL))
        );

        this.registerDefaultState(this.stateDefinition.any().setValue(LIGHT_LEVEL, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIGHT_LEVEL);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new BeakerBlockEntity(blockPos, blockState);
    }

    @NotNull
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {

        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        if (stack.getItem() instanceof ForcepsItem) {
            pickUpBeaker(level, pos, player);
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof BeakerBlockEntity beakerBlockEntity) {
            if (FluidUtil.interactWithFluidHandler(player, hand, beakerBlockEntity.getTank(null))) {
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @NotNull
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        pickUpBeaker(level, pos, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof BeakerBlockEntity beakerBlockEntity) {
                FluidStack fluid = beakerBlockEntity.getFluid();

                if (fluid.getAmount() >= FULL_THRESHOLD && hasFluidBlock(fluid.getFluid())) {
                    level.setBlockAndUpdate(pos, fluid.getFluid().defaultFluidState().createLegacyBlock());
                    level.playSound(null, pos, getSpillSound(fluid), SoundSource.BLOCKS, 1f, 1f);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private boolean hasFluidBlock(Fluid fluid) {
        return !fluid.defaultFluidState().createLegacyBlock().isAir();
    }

    private SoundEvent getSpillSound(FluidStack fluid) {
        if (fluid.is(Fluids.LAVA)) {
            return SoundEvents.BUCKET_EMPTY_LAVA;
        }
        return SoundEvents.BUCKET_EMPTY;
    }

    private void pickUpBeaker(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof BeakerBlockEntity beakerBlockEntity)) return;

        ItemStack beakerStack = new ItemStack(this.asItem());
        FluidStack fluid = beakerBlockEntity.getFluid();

        if (!fluid.isEmpty()) {
            beakerStack.set(ModDataComponentTypes.FLUID_DATA.get(), FluidData.createData(fluid.copy()));
            beakerBlockEntity.getTank(null).drain(fluid.getAmount(), IFluidHandler.FluidAction.EXECUTE);
        }

        level.removeBlock(pos, false);

        if (!player.getInventory().add(beakerStack)) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), beakerStack);
        }
    }
}
