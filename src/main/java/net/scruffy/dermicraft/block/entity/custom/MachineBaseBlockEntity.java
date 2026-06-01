package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.tank.FuelTank;
import net.scruffy.dermicraft.tank.VulnerableTank;
import net.scruffy.dermicraft.tank.WaterTank;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public abstract class MachineBaseBlockEntity extends BlockEntity {
    public MachineBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    protected ItemStackHandler createItemHandler(int size, int limitedSlot) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                updateBlock();
            }

            @Override
            public int getSlotLimit(int slot) {
                return slot == limitedSlot ? 1 : super.getSlotLimit(slot);
            }
        };
    }

    protected VulnerableTank createVulnerableTank(int capacity) {
        return new VulnerableTank(capacity) {
            @Override
            protected void onContentsChanged()
            {
                setChanged();
                updateBlock();
            }
        };
    }

    protected FuelTank createFuelTank(int capacity) {
        return new FuelTank(capacity) {
            @Override
            protected void onContentsChanged() {
                if (!level.isClientSide()) {
                    setChanged();
                    updateBlock();
                }
            }
        };
    }

    protected WaterTank createWaterTank(int capacity) {
        return new WaterTank(capacity) {
            @Override
            protected void onContentsChanged() {
                setChanged();
                updateBlock();
            }
        };
    }


    public void drops(IItemHandler itemHandler) {
        if (level != null) {
            SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());

            for (int i = 0; i < itemHandler.getSlots(); i++) {
                inventory.setItem(i, itemHandler.getStackInSlot(i));
            }
            Containers.dropContents(this.level, this.worldPosition, inventory);
        }
    }

    protected boolean isRecipeValid(RecipeHolder<?> recipe) {
        return recipe != null;
    }

    public int getScaledProgress(int scale, int progress, int maxProgress) {
        if (maxProgress <= 0 || progress <= 0) {
            return 0;
        }
        return (progress * scale) / maxProgress;
    }


    public Component getDisplayName(DeferredBlock<Block> block) {
        return Component.translatable("blockentity." + Dermicraft.MOD_ID + "." + block.getId());
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

    protected void updateBlock() {
        if (level != null) {
            updateBlock(level);
        }

    }

    protected void updateBlock(Level level) {
        if (!level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
}
