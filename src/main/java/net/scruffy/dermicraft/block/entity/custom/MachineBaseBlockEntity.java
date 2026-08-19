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

    /**
     * Deserializes a saved handler, then re-asserts {@code expectedSize}.
     *
     * <p>Use this instead of calling {@code handler.deserializeNBT} directly whenever a machine's
     * slot count has EVER changed, or might. NeoForge's {@code ItemStackHandler#deserializeNBT}
     * calls {@code setSize(tag.getInt("Size"))} whenever the saved NBT carries a Size, trusting the
     * saved value over the size the handler was just constructed with -- so a block saved before a
     * new slot was added silently shrinks its freshly-built handler back to the old count on load.
     * The menu then throws adding a slot past the end ("Slot N not in valid range - [0,N)"), which
     * surfaces as a crash on WORLD LOAD rather than on opening the screen, and only in worlds that
     * predate the change (never in a fresh test world -- which is exactly why it is easy to miss).
     *
     * <p><b>{@code setSize} does NOT preserve existing contents</b> -- it unconditionally replaces
     * the backing list with a fresh all-empty one (verified against the real NeoForge 21.1.228
     * bytecode, not assumed). Calling it straight after {@code deserializeNBT} would silently
     * DELETE whatever had just been loaded into the surviving slots, turning a crash into a quieter
     * and worse item-loss bug. This copies the loaded stacks out first and writes them back after
     * resizing, up to however many still fit -- the same technique
     * {@code WorkbenchBlockEntity}'s own hand-rolled STORAGE fix already used (this generalizes
     * that one, not the other way around).
     */
    protected static void loadItemHandler(ItemStackHandler handler, int expectedSize,
                                          HolderLookup.Provider registries, CompoundTag tag) {
        handler.deserializeNBT(registries, tag);
        if (handler.getSlots() == expectedSize) return;

        int loadedSlots = handler.getSlots();
        net.minecraft.world.item.ItemStack[] loaded = new net.minecraft.world.item.ItemStack[loadedSlots];
        for (int i = 0; i < loadedSlots; i++) {
            loaded[i] = handler.getStackInSlot(i);
        }

        handler.setSize(expectedSize);

        for (int i = 0; i < Math.min(loadedSlots, expectedSize); i++) {
            handler.setStackInSlot(i, loaded[i]);
        }
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

    /** Same as {@link #createVulnerableTank(int, int)}, but the hazard profile is read fresh from
     * {@code profileSupplier} on every fill/drain instead of fixed at Tier 1 forever -- see
     * {@link VulnerableTank}'s own supplier constructor. For a machine whose Module slot can grant
     * extra hazard tolerance (Decision Point #2, dermicraft-progression-notes.md). */
    protected VulnerableTank createVulnerableTank(int capacity, int slot, java.util.function.Supplier<net.scruffy.dermicraft.hazard.HazardProfile> profileSupplier) {
        return new VulnerableTank(capacity, slot, profileSupplier) {
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
