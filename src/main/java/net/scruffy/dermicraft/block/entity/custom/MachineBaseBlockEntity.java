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
import org.jetbrains.annotations.Nullable;

public abstract class MachineBaseBlockEntity extends BlockEntity {

    protected static final int CRAFT_TICKS = 10;

    protected int progress = 0;
    protected int maxProgress = 0;

    // Health mechanic is opt-in: maxHealth stays 0 (disabled) unless a subclass sets it.
    protected int health = 0;
    protected int maxHealth = 0;

    // Auto-drain toggle -- mirrors CrawBlockEntity's own item auto-push toggle, but for the fluid
    // side and hoisted here since every machine with an output tank that pushes to its neighbour
    // (Masticator/Effluentcer/their Charred variants, Skin Tank/Charred Tank, Drooling
    // Cauldron/Crucible) already extends this class. Harmless no-op on machines with no auto-push
    // tank -- nothing ever reads it there.
    protected boolean autoDrainEnabled = true;

    public MachineBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public boolean isAutoDrainEnabled() {
        return autoDrainEnabled;
    }

    public void toggleAutoDrain() {
        autoDrainEnabled = !autoDrainEnabled;
        setChanged();
        updateBlock();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("auto_drain_enabled", autoDrainEnabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Defaults ON for worlds saved before this existed, same as Craw's auto-push flag.
        autoDrainEnabled = !tag.contains("auto_drain_enabled") || tag.getBoolean("auto_drain_enabled");
    }

    /** How many Module slots this machine instance has -- 1 by default. A Charred/evolved variant
     * overrides this to grant more (the "Charred machines get extra Module slots" upgrade) --
     * public so a menu can read it too, since the slot count varies per instance, not per class. */
    public int moduleSlotCount() {
        return 1;
    }

    /** Called whenever any Module slot's contents change at all -- no-op by default, mirrors
     * {@link #onTankContentsChanged()}'s "generic hook, opt-in override" shape. {@code slot} is
     * relative to the Module handler itself (0-based), not the machine's other slots. */
    protected void onModuleSlotChanged(int slot) {
    }

    /** Dedicated Module-only inventory, deliberately separate from a machine's own general
     * INVENTORY (input/output/fuel-passthrough slots) -- one handler, sized per
     * {@link #moduleSlotCount()}, shared by every Module-slot-bearing machine (Skin Tank today;
     * the fueled machines still keep their Module slot inside their own combined INVENTORY as of
     * this writing, not yet migrated). Validated to {@code ModTags.Items.MODULES} same as every
     * other Module slot in the mod. */
    protected ItemStackHandler createModuleInventory(int size) {
        return new ItemStackHandler(size) {
            @Override
            public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
                return stack.is(net.scruffy.dermicraft.datagen.tag.ModTags.Items.MODULES);
            }

            // Locks a Capacity Module in place while removing it would leave a tank over its
            // would-be-smaller capacity -- see #canRemoveModule. Blocked for simulate too, so a
            // shift-click preview correctly refuses rather than silently no-oping.
            @Override
            public net.minecraft.world.item.ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (!canRemoveModule(slot)) return net.minecraft.world.item.ItemStack.EMPTY;
                return super.extractItem(slot, amount, simulate);
            }

            @Override
            protected void onContentsChanged(int slot) {
                if (level != null && !level.isClientSide) {
                    onModuleSlotChanged(slot);
                    applyCapacityBonus();
                    setChanged();
                    updateBlock();
                }
            }
        };
    }

    /** Whether the Module currently in {@code slot} may be removed right now -- true by default
     * (every non-capacity Module, and any slot on a consumer with no fluid tanks at all). A
     * consumer with fluid tanks overrides this to refuse removing a Capacity Module while any tank
     * is still holding more than the capacity that would result once this specific Module's own
     * bonus is subtracted -- see {@link #capacityModuleBonus}/{@link #capacityBonus}. */
    protected boolean canRemoveModule(int slot) {
        return true;
    }

    /** Re-applies {@link #capacityBonus} to every tank a Capacity-Module-bearing consumer has --
     * no-op by default. Called whenever a Module slot's contents change and once after NBT load
     * (a saved consumer's tanks are freshly constructed at base capacity before load, so an
     * already-installed Capacity Module needs re-applying). */
    protected void applyCapacityBonus() {
    }

    /** Total mB every currently-installed Capacity Module grants, summed linearly (no diminishing
     * returns -- see the design discussion for why Capacity doesn't need Work Speed's curve) -- 0
     * by default. A consumer with fluid tanks and a Module slot overrides this to sum over its own
     * {@code MODULE_INVENTORY}. */
    protected int capacityBonus() {
        return 0;
    }

    /** Reads {@code ModDataMaps.CAPACITY_MODULE_PROPERTIES} off a single Module stack -- 0 if
     * empty or not a Capacity Module. Shared by every {@link #capacityBonus}/
     * {@link #canRemoveModule} override so the data-map lookup lives in exactly one place. */
    protected static int capacityModuleBonus(net.minecraft.world.item.ItemStack module) {
        if (module.isEmpty()) return 0;
        net.scruffy.dermicraft.property.CapacityModuleProperties props =
                net.minecraft.core.registries.BuiltInRegistries.ITEM.wrapAsHolder(module.getItem())
                        .getData(net.scruffy.dermicraft.datagen.datamaps.ModDataMaps.CAPACITY_MODULE_PROPERTIES);
        return props == null ? 0 : props.bonusAmount();
    }

    /** One-time migration helper for a machine moving its Module slot out of a combined INVENTORY
     * (where it used to be the trailing slot) into {@link #createModuleInventory}'s own dedicated
     * handler -- otherwise a world saved before the split silently loses whatever was in that slot
     * the moment the old combined handler shrinks (see {@link #loadItemHandler}'s own shrink
     * behavior, which only preserves slots that still fit in the new, smaller size). Deserializes
     * the OLD tag into a throwaway scratch handler (sized off its own saved Size, never the live
     * INVENTORY) purely to read back whatever sat at {@code legacySlot}, so this never disturbs
     * whichever handler is actually being loaded elsewhere in the same {@code loadAdditional} call. */
    protected static net.minecraft.world.item.ItemStack extractLegacyModuleStack(
            HolderLookup.Provider registries, CompoundTag oldInventoryTag, int legacySlot) {
        if (oldInventoryTag == null || oldInventoryTag.isEmpty()) return net.minecraft.world.item.ItemStack.EMPTY;

        ItemStackHandler scratch = new ItemStackHandler();
        scratch.deserializeNBT(registries, oldInventoryTag);
        if (legacySlot < 0 || legacySlot >= scratch.getSlots()) return net.minecraft.world.item.ItemStack.EMPTY;
        return scratch.getStackInSlot(legacySlot);
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

    /** For the Drooling machine family -- see {@link net.scruffy.dermicraft.tank.DroolingTank}'s
     * own javadoc for why this reads the target fluid fresh on every fill rather than fixing it at
     * construction. */
    protected net.scruffy.dermicraft.tank.DroolingTank createDroolingTank(int capacity, int slot, java.util.function.Supplier<net.minecraft.world.level.material.Fluid> currentTarget) {
        return new net.scruffy.dermicraft.tank.DroolingTank(capacity, slot, currentTarget) {
            @Override
            protected void onContentsChanged()
            {
                if (!level.isClientSide) {
                    // Restores the original Cauldron-only WaterTank's own onContentsChanged, lost
                    // when this factory was generalized out of it -- passive generation filling the
                    // tank should keep auto-pushing downward, same as before. Hazard-safe for free:
                    // pushFluidToBelowNeighbour's own FluidUtil.tryFluidTransfer simulates the fill
                    // against the destination first, so a hazard-gated tank below (e.g. a
                    // VulnerableTank) still correctly refuses lava on its own -- no extra gating
                    // needed here, the destination already decides.
                    if (autoDrainEnabled) {
                        this.pushFluidToBelowNeighbour(level, worldPosition);
                    }
                    setChanged();
                    updateBlock();
                    onTankContentsChanged();
                }
            }
        };
    }

    /** Called whenever a machine's own tank contents change -- no-op by default, generic across
     * every machine (not just the Drooling family), for a subclass that wants to react without
     * needing its own tank-factory override. Currently only wired into {@link #createDroolingTank},
     * for the Drooling family's dynamic light level -- see
     * {@code DroolingMachineBlockEntity#updateLightLevel}. */
    protected void onTankContentsChanged() {
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
