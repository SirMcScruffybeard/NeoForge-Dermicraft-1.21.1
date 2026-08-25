package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.interfaces.IPreserveContentsOnPickup;
import net.scruffy.dermicraft.property.EvolutionModuleProperties;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.early_incubating.EarlyIncubatingRecipe;
import net.scruffy.dermicraft.recipe.early_incubating.EarlyIncubatingRecipeInput;
import net.scruffy.dermicraft.screen.custom.craw.CrawMenu;
import net.scruffy.dermicraft.util.ModItemUtil;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

public class CrawBlockEntity extends MachineBaseBlockEntity
        implements MenuProvider, IHaveInventory, IPreserveContentsOnPickup, IHasChannels {

    public static final int STACKS = 10;
    public static final int CAPACITY = 64 * STACKS; // 640 items of one type

    public static final int STORAGE_SLOT = 0;
    public static final int INPUT_SLOT = 0; // index within the separate INPUT handler
    public static final int MODULE_SLOT = 0; // index within the separate MODULE handler

    private static final int PUSH_INTERVAL_SECONDS = 1;
    private static final String INCUBATING_RECIPE_KEY = "incubating_recipe_id";

    // Early Incubating recipe cache -- re-evaluated whenever the storage slot's contents
    // change (see INVENTORY.onContentsChanged below), same lazy-id/resolve-on-load pattern
    // as EarlySurgeryTumorBlockEntity, adapted for Craw's single bulk stack instead of a
    // multi-slot Tumor inventory.
    private @Nullable RecipeHolder<EarlyIncubatingRecipe> cachedRecipeHolder = null;
    private @Nullable ResourceLocation lazyRecipeId = null;

    // Single locked storage slot. Locks to whichever item is first inserted and
    // unlocks once it empties -- the item-side twin of SkinTank's fluid-type lock.
    public final ItemStackHandler INVENTORY = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return capacity();
        }

        // ItemStackHandler.getStackLimit() normally clamps to the item's max stack size (64),
        // which would cap bulk storage at one stack -- override it so the slot honours CAPACITY.
        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            ItemStack stored = getStackInSlot(slot);
            return stored.isEmpty() || ItemStack.isSameItemSameComponents(stored, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (level != null && !level.isClientSide) {
                setChanged();
                updateBlock();
                updateRecipeCache();
            }
        }
    };

    // GUI-only staging slot: whatever's dropped in here drains into storage on the next tick,
    // so players can shift-click/drag a whole stack in without the vanilla 64-per-click cap
    // applying to the storage slot itself. Doesn't affect the block's direct drawer interaction.
    //
    // The drain happens on tick rather than synchronously in onContentsChanged: vanilla's
    // SlotItemHandler.getMaxStackSize(ItemStack) probes a slot's capacity by calling
    // setStackInSlot(EMPTY) then setStackInSlot(currentStack) to simulate-and-restore -- both
    // calls fire onContentsChanged. Reacting to those with a real (non-simulated) transfer
    // corrupted state whenever storage already held a compatible stack, since the "restore"
    // call re-triggered a genuine insert as a side effect of what should be a read-only probe.
    public final ItemStackHandler INPUT = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            if (level != null && !level.isClientSide) {
                setChanged();
                updateBlock();
            }
        }
    };

    // Module slot -- same tab-gated pattern as every other Module-tab machine, but standalone
    // (like INPUT above) rather than sharing INVENTORY, since INVENTORY's own getSlotLimit/
    // getStackLimit/isItemValid are all bulk-storage-specific and shouldn't apply to a Module.
    public final ItemStackHandler MODULE = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            if (level != null && !level.isClientSide) {
                onModuleChanged();
                setChanged();
                updateBlock();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModTags.Items.MODULES);
        }
    };

    // ---- Evolution (installed Evolution Module -> eventual Charred Craw) ---------------------
    // Simpler than Masticator/Metastasizer/Effluentcer/Mutator's gradual-progress mechanic, same
    // shape as Skin Tank -> Charred Tank: Craw holds items, not fluids, so there's no hazard
    // tolerance to grant here at all -- the Module slot exists purely to hold the Evolution Module
    // that triggers a flat 5-second countdown straight to the flourish, no progress render.
    private int flourishTicksRemaining = -1;
    private static final int FLOURISH_DURATION_TICKS = 100; // 5s

    /** Whether this instance can still evolve at all -- true for the base Craw, overridden to false
     * by {@code CharredCrawBlockEntity} (already evolved; installing an Evolution Module there does
     * nothing, since capacity/throughput are permanently doubled regardless of any Module, and
     * there's nothing further for it to transform into). */
    protected boolean canEvolve() {
        return true;
    }

    /** How many items of one type this instance can hold -- {@link #CAPACITY} for the base Craw,
     * overridden to double that by Charred Craw. */
    protected int capacity() {
        return CAPACITY;
    }

    /** Called whenever the Module slot's contents change at all -- cancels any countdown already
     * running (covers the Module being pulled back out mid-countdown), then starts a fresh one if
     * the slot now holds a real Evolution Module. Mirrors SkinTankBlockEntity's identical rule. */
    private void onModuleChanged() {
        flourishTicksRemaining = -1;
        if (!canEvolve()) return;
        ItemStack module = MODULE.getStackInSlot(MODULE_SLOT);
        if (module.isEmpty()) return;
        EvolutionModuleProperties evoProps = BuiltInRegistries.ITEM.wrapAsHolder(module.getItem())
                .getData(ModDataMaps.EVOLUTION_MODULE_PROPERTIES);
        if (evoProps != null) startEvolutionFlourish();
    }

    /** Runs every raw tick -- Craw's own {@link #tick} already runs every tick. Returns true once
     * the countdown completes and the block swap has fired, so {@link #tick} can stop touching this
     * now-stale instance. */
    private boolean tickEvolutionFlourish() {
        if (flourishTicksRemaining < 0) return false;

        if (level instanceof ServerLevel serverLevel) {
            spawnFlourishParticles(serverLevel, flourishTicksRemaining);
        }

        flourishTicksRemaining--;
        if (flourishTicksRemaining < 0) {
            completeEvolution(level);
            return true;
        }
        return false;
    }

    private void startEvolutionFlourish() {
        flourishTicksRemaining = FLOURISH_DURATION_TICKS;
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 1.0F, 0.6F);
        }
    }

    private void spawnFlourishParticles(ServerLevel serverLevel, int ticksRemaining) {
        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 0.5;
        double cz = worldPosition.getZ() + 0.5;

        float progress = 1f - (ticksRemaining / (float) FLOURISH_DURATION_TICKS);
        int dustCount = 4 + Math.round(progress * 10);
        // Lava-orange, matches every other Charred family flourish's own fallback tint (no target
        // fluid here to derive a real tint from -- Craw has no tank at all).
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(1.0F, 70 / 255.0F, 20 / 255.0F), 1.4F);
        serverLevel.sendParticles(dust, cx, cy, cz, dustCount, 0.3, 0.3, 0.3, 0.03);

        if (ticksRemaining % 4 == 0) {
            int smokeCount = 2 + Math.round(progress * 6);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy, cz, smokeCount, 0.35, 0.35, 0.35, 0.02);
        }

        if (ticksRemaining == 0) {
            serverLevel.sendParticles(dust, cx, cy, cz, 30, 0.5, 0.5, 0.5, 0.06);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy, cz, 16, 0.5, 0.5, 0.5, 0.04);
            serverLevel.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /**
     * Transforms this block into a Charred Craw in place, carrying over the storage and input
     * staging contents -- the Module itself is not carried over, this transform is what consumes
     * it. No FACING (or any other block state) to preserve -- Craw has none.
     */
    private void completeEvolution(Level level) {
        ItemStack storedItem = getStoredStack().copy();
        ItemStack inputItem = INPUT.getStackInSlot(INPUT_SLOT);
        @Nullable RecipeHolder<EarlyIncubatingRecipe> recipeHolder = this.cachedRecipeHolder;

        INVENTORY.setStackInSlot(STORAGE_SLOT, ItemStack.EMPTY);
        INPUT.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY);
        MODULE.setStackInSlot(MODULE_SLOT, ItemStack.EMPTY);

        level.setBlock(worldPosition, ModBlocks.CHARRED_CRAW.get().defaultBlockState(), Block.UPDATE_ALL);

        if (level.getBlockEntity(worldPosition) instanceof CrawBlockEntity charred) {
            charred.INVENTORY.setStackInSlot(STORAGE_SLOT, storedItem);
            charred.INPUT.setStackInSlot(INPUT_SLOT, inputItem);
            charred.cachedRecipeHolder = recipeHolder;
            charred.setChanged();
            charred.updateBlock();
        }
    }

    public void tick(Level level) {
        if (!level.isClientSide) {
            // Early-return on a completed swap -- same as SkinTankBlockEntity#tick -- this instance
            // is now stale, its own storage already cleared by completeEvolution.
            if (tickEvolutionFlourish()) return;

            transferInputToStorage();
            // Cadence-throttled (once per second) rather than amount-throttled by itself -- storage
            // has no crafting cycle to piggyback on. Each push is ALSO capped to stacksPerPush() full
            // stacks (see pushStack's own maxAmount param) -- pushing the entire 640-capacity slot in
            // one shot was a burst most duct networks can't actually absorb at once; smaller, more
            // frequent pushes are both more realistic and easier to reason about. Charred Craw
            // overrides stacksPerPush() for its own doubled throughput -- see that class's javadoc.
            if (autoPushEnabled && ModMath.Time.hasSecondsPassed(level, PUSH_INTERVAL_SECONDS)) {
                ItemStack stored = getStoredStack();
                if (!stored.isEmpty()) {
                    int maxAmount = stacksPerPush() * stored.getMaxStackSize();
                    ModItemUtil.pushItemToBelowTransport(level, worldPosition, INVENTORY, STORAGE_SLOT, maxAmount);
                }
            }
        }
    }

    /** How many full stacks (of whatever item is currently stored) a single auto-push cycle may
     * move -- 1 for the base Craw, overridden to 2 by Charred Craw. */
    protected int stacksPerPush() {
        return 1;
    }

    private void transferInputToStorage() {
        ItemStack input = INPUT.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return;
        }
        ItemStack remainder = INVENTORY.insertItem(STORAGE_SLOT, input, false);
        // Only write back if something actually moved -- prevents re-triggering this same
        // listener when storage is full/type-mismatched and the remainder is unchanged.
        if (remainder.getCount() != input.getCount()) {
            INPUT.setStackInSlot(INPUT_SLOT, remainder);
        }
    }

    public CrawBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.CRAW_BE.get(), pos, blockState);
    }

    // Lets a capability-leap subclass (Charred Craw) register under its own BlockEntityType while
    // reusing everything else this class provides -- see MachineTier's own javadoc on why a genuine
    // capability leap (here: doubled capacity/throughput) is a hook override, not a new MachineTier
    // constant (Craw has no MachineTier concept at all, but the same split applies).
    protected CrawBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    // Auto-push (pass-down) toggle -- default ON, matching the existing auto-push-when-a-valid-
    // transport-target-is-present behavior exactly. Off simply skips the tick()'s own push attempt
    // entirely; direct player deposit/withdraw and the GUI-only INPUT staging drain are unaffected.
    private boolean autoPushEnabled = true;

    public boolean isAutoPushEnabled() {
        return autoPushEnabled;
    }

    public void toggleAutoPush() {
        autoPushEnabled = !autoPushEnabled;
        setChanged();
        updateBlock();
    }

    // Which screen tab was last open -- same pattern as every other Module-tab machine, so
    // reopening the screen returns to the tab last viewed.
    private boolean moduleTabActive = false;

    public boolean isModuleTabActive() {
        return moduleTabActive;
    }

    public void setModuleTabActive(boolean active) {
        this.moduleTabActive = active;
    }

    public IItemHandler getItemHandler(@Nullable Direction face) {
        return INVENTORY; // any-face access -- ignore the face
    }

    /** See {@link IHasChannels#describeFace} -- getItemHandler ignores the face entirely. */
    @Override
    public Component describeFace(Direction face) {
        return Component.translatable("tooltip.dermicraft.idep.face.craw_storage");
    }

    /**
     * Self-described channel list for the Gate multiblock -- see {@link IHasChannels}.
     * Like Skin Tank, {@code getItemHandler(Direction)} already ignores the face -- Craw is bulk
     * storage, not a machine with distinct crafting slots, so {@link Channel.IO#BOTH} matches its
     * existing deposit/withdraw behaviour. INPUT is a GUI-only staging slot (see its own doc comment
     * above) with known simulate-probe corruption risk if touched by real automation -- deliberately
     * not exposed as a channel, only the real STORAGE_SLOT via INVENTORY is. Native faces are all 6.
     */
    @Override
    public List<Channel> getChannels() {
        if (level != null && isFaceServiced(level, worldPosition, Channel.Kind.ITEM, Direction.values())) {
            return List.of();
        }
        return List.of(
                new Channel.ItemChannel("storage", Component.literal("Storage"), Channel.IO.BOTH, INVENTORY)
        );
    }

    public ItemStack getStoredStack() {
        return INVENTORY.getStackInSlot(STORAGE_SLOT);
    }

    public int getStoredCount() {
        return getStoredStack().getCount();
    }

    public @Nullable EarlyIncubatingRecipe getCachedRecipe() {
        return this.cachedRecipeHolder != null ? this.cachedRecipeHolder.value() : null;
    }

    public void updateRecipeCache() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        EarlyIncubatingRecipeInput input = new EarlyIncubatingRecipeInput(getBlockState().getBlock(), getStoredStack());
        this.cachedRecipeHolder = this.level.getRecipeManager()
                .getRecipeFor(ModRecipes.EARLY_INCUBATING_TYPE.get(), input, this.level).orElse(null);
    }

    @SuppressWarnings("unchecked")
    private void resolveLazyRecipeHolder() {
        if (this.level == null || this.level.isClientSide || this.lazyRecipeId == null) {
            return;
        }
        this.level.getRecipeManager().byKey(this.lazyRecipeId).ifPresent(holder -> {
            if (holder.value() instanceof EarlyIncubatingRecipe) {
                this.cachedRecipeHolder = (RecipeHolder<EarlyIncubatingRecipe>) holder;
            }
        });
        this.lazyRecipeId = null;
    }

    /**
     * Debits the recipe's exact required item count from storage and drops the result next
     * to the Craw -- the block itself is never consumed and stays loaded for the next
     * injection (see [[project_proto_brain_synapse_catalyst_chain]] design notes). Works
     * identically whether {@code result} is a plain item or a block's {@code BlockItem}.
     */
    public void completeIncubation(EarlyIncubatingRecipe recipe) {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        INVENTORY.extractItem(STORAGE_SLOT, recipe.ingredient().getCount(), false);
        Containers.dropItemStack(this.level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), recipe.result().copy());
        this.level.playSound(null, worldPosition, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 1.0F, 0.6F);
    }

    /**
     * Deposit the whole held stack (as much as fits). Shrinks and returns the held stack.
     *
     * <p>Deliberately not crouch-gated: vanilla's {@code ServerPlayerGameMode.useItemOn} skips
     * {@code Block.useItemOn} entirely when the player is crouching with an item in hand, so a
     * crouch-to-insert-a-stack variant is unreachable from the block side. The Craw is bulk storage
     * (single item type, 640 capacity) rather than a precision inserter, so always taking the full
     * stack is the better default -- a player wanting to deposit less splits the stack first.
     * Withdrawal is unaffected and still uses crouch, since an empty hand does reach the block.
     */
    public ItemStack deposit(ItemStack held) {
        if (held.isEmpty()) {
            return held;
        }
        int amount = held.getCount();
        ItemStack toInsert = held.copyWithCount(amount);
        ItemStack remainder = INVENTORY.insertItem(STORAGE_SLOT, toInsert, false);
        int inserted = amount - remainder.getCount();
        held.shrink(inserted);
        return held;
    }

    /**
     * Withdraw from storage. Regular use pulls one item; crouch use pulls a full stack
     * (up to the item's max stack size, or the remainder if less).
     */
    public ItemStack withdraw(boolean fullStack) {
        ItemStack stored = getStoredStack();
        if (stored.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int amount = fullStack ? stored.getMaxStackSize() : 1;
        return INVENTORY.extractItem(STORAGE_SLOT, amount, false);
    }

    @Override
    public void drops() {
        if (level == null) {
            return;
        }
        // Storage can hold more than a vanilla stack; split into <=maxStackSize drops so no single
        // ItemEntity carries an over-99 count (which vanilla's item NBT codec rejects on save).
        ItemStack stored = getStoredStack();
        while (!stored.isEmpty()) {
            ItemStack chunk = stored.split(Math.min(stored.getCount(), stored.getMaxStackSize()));
            Containers.dropItemStack(level,
                    worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), chunk);
        }
        INVENTORY.setStackInSlot(STORAGE_SLOT, ItemStack.EMPTY);

        dropItems(level, INPUT, worldPosition); // input never exceeds one stack, safe as-is
        dropItems(level, MODULE, worldPosition);
    }

    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.CRAW);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CrawMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        // The storage slot can hold up to 640 of one item, but ItemStack's data codec caps a
        // stack's saved count at 99. So persist the item type from a single-count copy (which the
        // codec accepts) and store the real bulk amount separately as a plain int.
        ItemStack stored = getStoredStack();
        if (!stored.isEmpty()) {
            CompoundTag storageTag = new CompoundTag();
            storageTag.put("item", stored.copyWithCount(1).save(registries));
            storageTag.putInt("count", stored.getCount());
            tag.put("craw_storage", storageTag);
        }
        tag.put("craw_input_inv", INPUT.serializeNBT(registries));
        tag.put("craw_module_inv", MODULE.serializeNBT(registries));
        tag.putBoolean("module_tab_active", moduleTabActive);
        tag.putBoolean("auto_push_enabled", autoPushEnabled);
        tag.putInt("evolution_flourish_ticks", flourishTicksRemaining);
        if (this.cachedRecipeHolder != null) {
            tag.putString(INCUBATING_RECIPE_KEY, this.cachedRecipeHolder.id().toString());
        }
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ItemStack stored = ItemStack.EMPTY;
        if (tag.contains("craw_storage")) {
            CompoundTag storageTag = tag.getCompound("craw_storage");
            stored = ItemStack.parse(registries, storageTag.getCompound("item")).orElse(ItemStack.EMPTY);
            if (!stored.isEmpty()) {
                stored.setCount(storageTag.getInt("count"));
            }
        }
        INVENTORY.setStackInSlot(STORAGE_SLOT, stored);
        INPUT.deserializeNBT(registries, tag.getCompound("craw_input_inv"));
        if (tag.contains("craw_module_inv")) MODULE.deserializeNBT(registries, tag.getCompound("craw_module_inv"));
        moduleTabActive = tag.getBoolean("module_tab_active");
        // Absent on a world saved before this toggle existed -- default ON so pre-existing setups
        // keep auto-pushing exactly as before, matching the field's own default.
        autoPushEnabled = !tag.contains("auto_push_enabled") || tag.getBoolean("auto_push_enabled");
        flourishTicksRemaining = tag.contains("evolution_flourish_ticks")
                ? tag.getInt("evolution_flourish_ticks") : -1;
        if (tag.contains(INCUBATING_RECIPE_KEY, CompoundTag.TAG_STRING)) {
            this.lazyRecipeId = ResourceLocation.parse(tag.getString(INCUBATING_RECIPE_KEY));
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Re-bind the cached recipe instance once fully bound to the world, or derive it fresh
        // if none was persisted (e.g. a freshly-placed Craw with items inserted before this
        // logic existed).
        if (this.level != null && !this.level.isClientSide) {
            if (this.lazyRecipeId != null) {
                resolveLazyRecipeHolder();
            } else {
                updateRecipeCache();
            }
        }
    }
}
