package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.screen.custom.skin_tank.SkinTankMenu;
import net.scruffy.dermicraft.tank.VulnerableTank;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.Nullable;

public class SkinTankBlockEntity extends MachineBaseBlockEntity implements MenuProvider, IHaveInventory {

    public static final int CAPACITY = FluidType.BUCKET_VOLUME * 10;

    public static final int INPUT = 0;
    public static final int OUTPUT = 1;

    public final ItemStackHandler INVENTORY = createInventory();

    private final VulnerableTank TANK =  createVulnerableTank(FluidType.BUCKET_VOLUME * 10, -1);

    public SkinTankBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SKIN_TANK_BE.get(), pos, blockState);
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

    IItemHandler getItemHandler(@Nullable Direction face) {
        return INVENTORY;
    }

    public void drops(){
        dropItems(level, INVENTORY, worldPosition);
    }

    public void tick(Level sLevel) {
        if (!sLevel.isClientSide) {

            if (ModMath.Time.hasTicksPassed(sLevel, 20)) {
                TANK.pushFluidToBelowNeighbour(level, worldPosition);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.SKIN_TANK);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SkinTankMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("skin_tank_inv", INVENTORY.serializeNBT(registries));
        tag = TANK.writeToNBT(registries, tag);
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        INVENTORY.deserializeNBT(registries, tag.getCompound("skin_tank_inv"));
        TANK.readFromNBT(registries, tag);
    }

    private ItemStackHandler createInventory() {
        return new ItemStackHandler(2) {
           @Override
           protected void onContentsChanged(int slot) {
                if (level != null && !level.isClientSide) {

                    if (TANK.hasFluidHandlerInSlot(this, INPUT)) {
                        TANK.transferFluidToTank(this, INPUT);
                    }

                    if (TANK.hasEmptyFluidHandlerInSlot(this, OUTPUT)) {
                        TANK.transferFluidFromTankToHandler(this, OUTPUT);
                    }

                    setChanged();
                    updateBlock();
                }
            }

            @Override
            public int getSlotLimit(int slot) {
                return slot == OUTPUT ? 1 : super.getSlotLimit(slot);
            }
        };
    }

}
