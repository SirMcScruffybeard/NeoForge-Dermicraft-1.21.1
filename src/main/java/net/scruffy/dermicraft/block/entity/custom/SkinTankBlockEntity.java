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
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.screen.custom.skin_tank.SkinTankMenu;
import net.scruffy.dermicraft.tank.VulnerableTank;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SkinTankBlockEntity extends MachineBaseBlockEntity implements MenuProvider, IHaveInventory, IHasChannels {

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

    /** See {@link IHasChannels#describeFace} -- getTank/getItemHandler ignore the face entirely. */
    @Override
    public Component describeFace(Direction face) {
        return Component.translatable("tooltip.dermicraft.idep.face.skin_tank_storage");
    }

    public IFluidHandler getTank() {
        return getTank(null);
    }

    IItemHandler getItemHandler(@Nullable Direction face) {
        return INVENTORY;
    }

    /**
     * Self-described channel list for the Gate multiblock -- see {@link IHasChannels}.
     * Unlike every other machine here, {@code getTank(Direction)} already ignores the face entirely
     * -- Skin Tank is a plain storage tank, not a machine with distinct crafting input/output tanks,
     * so it was never face-locked to begin with. Modelled as {@link Channel.IO#BOTH} rather than
     * IN or OUT, matching that existing bidirectional behaviour. Native faces are all 6; like the
     * Drooling Cauldron's result tank, this is rarely the actual bottleneck given how permissive it
     * already is, but the self-filter still applies correctly to the fully-isolated case.
     */
    @Override
    public List<Channel> getChannels() {
        if (level != null && isFaceServiced(level, worldPosition, Channel.Kind.FLUID, Direction.values())) {
            return List.of();
        }
        return List.of(
                new Channel.FluidChannel("storage", Component.literal("Storage"), Channel.IO.BOTH, TANK)
        );
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
