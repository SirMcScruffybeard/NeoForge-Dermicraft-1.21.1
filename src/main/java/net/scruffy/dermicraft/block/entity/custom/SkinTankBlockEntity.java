package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.interfaces.IFleshBlockEntity;
import net.scruffy.dermicraft.interfaces.IHaveTank;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.tank.VulnerableTank;
import org.jetbrains.annotations.Nullable;

public class SkinTankBlockEntity extends BlockEntity implements MenuProvider, IFleshBlockEntity, IHaveTank {

    public static final int CAPACITY = FluidType.BUCKET_VOLUME * 10;

    public static final int INPUT = 0;
    public static final int OUTPUT = 1;

    public final ItemStackHandler INVENTORY = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            updateBlock();
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == OUTPUT ? 1 : super.getSlotLimit(slot);
        }
    };

    private final VulnerableTank TANK = createTank();

    private VulnerableTank createTank() {
        return new VulnerableTank(CAPACITY) {
            @Override
            protected void onContentsChanged() {
                setChanged();
                updateBlock();
            }
        };
    }

    public SkinTankBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SKIN_TANK_BE.get(), pos, blockState);
    }

    public FluidStack getFluid() {
        return TANK.getFluid();
    }

    public IFluidHandler getTank(@Nullable Direction face) {
        return TANK;
    }

    IItemHandler getItemHandler(@Nullable Direction face) {
        return INVENTORY;
    }

    public void drops(){
        drops(level, INVENTORY, worldPosition);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide) {
            if (hasFluidHandlerInSlot(INVENTORY, INPUT)) {
                transferFluidToTank(INVENTORY, INPUT, TANK);
            }

            if (hasEmptyFluidHandlerInSlot(INVENTORY, OUTPUT, TANK)) {
                transferFluidFromTankToHandler(INVENTORY, OUTPUT, TANK);
            }

            pushFluidToBelowNeighbour(level, worldPosition, TANK);
        }
    }


    @Override
    public Component getDisplayName() {
        return Component.translatable("blockentity." + Dermicraft.MOD_ID + "." + ModBlocks.SKIN_TANK.getId());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return ;
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

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider pRegistries) {
        super.onDataPacket(net, pkt, pRegistries);
    }

    private void updateBlock() {
        if(isServerSide(level)){
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
}
