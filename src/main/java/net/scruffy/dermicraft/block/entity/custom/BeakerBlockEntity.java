package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scruffy.dermicraft.block.custom.BeakerBlock;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.tank.ModFluidTank;
import org.jetbrains.annotations.Nullable;

public class BeakerBlockEntity extends MachineBaseBlockEntity {

    public static final int CAPACITY = FluidType.BUCKET_VOLUME;

    private final ModFluidTank TANK = new ModFluidTank(CAPACITY, -1) {
        @Override
        protected void onContentsChanged() {
            if (!level.isClientSide) {
                setChanged();
                updateBlock();
                updateLightLevel();
            }
        }
    };

    public BeakerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BEAKER_BE.get(), pos, blockState);
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    public FluidStack getFluid() {
        return TANK.getFluid();
    }

    public IFluidHandler getTank(@Nullable Direction face) {
        return TANK;
    }

    public IFluidHandler getTank() {
        return getTank(null);
    }

    private void updateLightLevel() {
        if (level == null) return;

        FluidStack fluid = TANK.getFluid();
        int lightLevel = fluid.isEmpty() ? 0 : fluid.getFluid().getFluidType().getLightLevel(fluid);

        BlockState state = getBlockState();
        if (state.getValue(BeakerBlock.LIGHT_LEVEL) != lightLevel) {
            level.setBlock(worldPosition, state.setValue(BeakerBlock.LIGHT_LEVEL, lightLevel), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        TANK.writeToNBT(registries, tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        TANK.readFromNBT(registries, tag);
    }
}
