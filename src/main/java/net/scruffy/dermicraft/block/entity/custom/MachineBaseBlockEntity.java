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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.tank.FuelTank;
import net.scruffy.dermicraft.tank.ModFluidTank;
import net.scruffy.dermicraft.tank.VulnerableTank;
import net.scruffy.dermicraft.tank.WaterTank;
import org.jetbrains.annotations.Nullable;

public abstract class MachineBaseBlockEntity extends BlockEntity {

    protected static final int CRAFT_TICKS = 10;

    protected int progress = 0;
    protected int maxProgress = 0;

    // Health mechanic is opt-in: maxHealth stays 0 (disabled) unless a subclass sets it.
    protected int health = 0;
    protected int maxHealth = 0;

    public MachineBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public boolean hasTank() {
        return false;
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

    protected VulnerableTank createVulnerableTank(int capacity, int slot) {
        return new VulnerableTank(capacity, slot) {
            @Override
            protected void onContentsChanged()
            {
                if (!level.isClientSide) {
                    setChanged();
                    updateBlock();
                }
            }
        };
    }

    protected ModFluidTank createFluidTank(int capacity, int slot) {
        return new ModFluidTank(capacity, slot) {
            @Override
            protected void onContentsChanged() {
                if (!level.isClientSide) {
                    setChanged();
                    updateBlock();
                }
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

    protected boolean isMaxProgressValid() {
        return maxProgress > 0;
    }

    public int getScaledProgress(int scale) {
        return getScaledProgress(scale, progress, maxProgress);
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

    protected void resetProgress() {
        progress = 0;
    }

    protected void resetMaxProgress() {
        maxProgress = 0;
    }

    public boolean isStillCrafting() {
        return progress < maxProgress;
    }

    protected void damageMachine(int amount) {
        health = Math.max(0, health - amount);
    }

    protected void healMachine(int amount) {
        health = Math.min(maxHealth, health + amount);
    }

    protected boolean isStarved() {
        return maxHealth > 0 && health <= 0;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
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

    public void updateBlock() {
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
