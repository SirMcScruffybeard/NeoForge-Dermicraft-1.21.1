package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.base.EvolvingMachineBlockEntity;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.interfaces.IModuleTabMachine;
import net.scruffy.dermicraft.interfaces.IPreserveContentsOnPickup;
import net.scruffy.dermicraft.machine.CrawStats;
import net.scruffy.dermicraft.main.Config;
import net.scruffy.dermicraft.machine.EvolutionEffects;
import net.scruffy.dermicraft.machine.EvolutionResult;
import net.scruffy.dermicraft.machine.EvolutionStats;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.early_incubating.EarlyIncubatingRecipe;
import net.scruffy.dermicraft.recipe.early_incubating.EarlyIncubatingRecipeInput;
import net.scruffy.dermicraft.screen.custom.craw.CrawMenu;
import net.scruffy.dermicraft.util.ModItemUtil;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CrawBlockEntity extends EvolvingMachineBlockEntity
        implements MenuProvider, IHaveInventory, IPreserveContentsOnPickup, IHasChannels, IModuleTabMachine {

    public static final int STORAGE_SLOT = 0;
    public static final int INPUT_SLOT = 0; // index within the separate INPUT handler

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
        // What is held (or, when empty, a full 64 stack) sets the limit: capacity is counted in stacks.
        @Override
        public int getSlotLimit(int slot) {
            ItemStack stored = getStackInSlot(slot);
            return capacityFor(stored.isEmpty() ? 64 : stored.getMaxStackSize());
        }

        // ItemStackHandler.getStackLimit() normally clamps to the item's max stack size (64),
        // which would cap bulk storage at one stack -- override it so the slot honours the capacity,
        // measured in stacks of the item being inserted.
        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return capacityFor(stack.getMaxStackSize());
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

    /** General Module slots, 1 to 4 from the tier and resized when the evolution state changes, see
     * {@link #onEvolutionStateChanged()}. Kept apart from INVENTORY, whose limit and validity rules are all
     * bulk-storage-specific. Evolution Modules have their own dedicated slot, so they are refused here. */
    public final ItemStackHandler MODULE_INVENTORY = createModuleInventory(getModuleSlotCount(),
            stack -> stack.is(ModTags.Items.MODULES) && !isEvolutionModule(stack));

    /** General Module slots, from {@link CrawStats} (the dedicated Evolution slot is separate). */
    @Override
    public int getModuleSlotCount() {
        return CrawStats.getModuleSlots(getEvolutionState());
    }

    @Override
    protected void onEvolutionStateChanged() {
        resizeModuleInventory(MODULE_INVENTORY, getModuleSlotCount());
    }

    @Override
    public ItemStackHandler getModuleInventory() {
        return MODULE_INVENTORY;
    }

    /** The Craw evolves through its own countdown, not the shared gradual progress, so the shared slot fill reads it. */
    @Override
    protected boolean usesEvolutionState() {
        return true;
    }

    /** How many items of {@code maxStackSize}-sized stacks this Craw holds: its stack count from {@link CrawStats}. */
    private int capacityFor(int maxStackSize) {
        return CrawStats.getStacks(getEvolutionState()) * maxStackSize;
    }

    // ---- Evolution (an Evolution Module in the Evolution slot raises this Craw in place) -------
    // Storage never processes, so there is no progress to accumulate: installing an Evolution Module that applies
    // starts a short countdown (EvolutionStats.getCountdownTicks()) with the shared EvolutionEffects particles, and
    // when it ends the new evolution state is applied in place. The window exists only so the player can pull the
    // module back out if they did not mean to commit; pulling it cancels the countdown. Same rule as the Skin Tank.
    private int flourishTicksRemaining = -1;
    /** The countdown's full length when it started, saved so the progress fraction stays right on the client. */
    private int flourishTotalTicks = 1;

    @Override
    public float getGradualEvolutionFraction() {
        if (flourishTicksRemaining < 0) return 0f;
        return EvolutionEffects.getProgress(flourishTicksRemaining, flourishTotalTicks);
    }

    /** Called whenever the Evolution slot's contents change -- cancels any countdown already running (covers the
     * module being pulled back out), then starts a fresh one if the slot now holds a module that would apply. */
    @Override
    protected void onEvolutionSlotChanged() {
        super.onEvolutionSlotChanged();
        flourishTicksRemaining = -1;
        if (getPendingUpgrade().isPresent()) {
            flourishTotalTicks = EvolutionStats.getCountdownTicks();
            flourishTicksRemaining = flourishTotalTicks;
            if (level != null) EvolutionEffects.playDefaultStartSound(level, worldPosition);
        }
    }

    /** Runs every tick: the cancel window counts down, then the upgrade is applied. */
    private void tickEvolutionCountdown() {
        if (flourishTicksRemaining < 0) return;

        if (level instanceof ServerLevel serverLevel) {
            EvolutionEffects.spawnParticles(serverLevel, worldPosition, getGradualEvolutionFraction(),
                    flourishTicksRemaining == 0, EvolutionEffects.DEFAULT_TINT, EvolutionEffects.Cadence.TICK);
        }

        flourishTicksRemaining--;
        if (flourishTicksRemaining < 0) {
            completeEvolution();
        } else {
            updateBlock(); // the Evolution slot's fill reads the progress on the client
        }
    }

    /** Applies the module's upgrade in place and consumes it. Evaluated again in case the state changed during the
     * window; if it no longer applies, the module stays in the slot. The contents are untouched. */
    private void completeEvolution() {
        Optional<EvolutionResult.Upgrade> upgrade = getPendingUpgrade();
        if (upgrade.isEmpty()) return;

        EVOLUTION_INVENTORY.setStackInSlot(0, ItemStack.EMPTY);
        setEvolutionState(upgrade.get().newState());
        setChanged();
        updateBlock();
    }

    /** Items the auto-push may still send this second; fractional rates carry over. Not saved. */
    private double pushBudget = 0;

    public void tick(Level level) {
        if (!level.isClientSide) {
            syncEvolutionBlockState();

            tickEvolutionCountdown();

            transferInputToStorage();
            // Cadence-throttled (once per second), with the stat table's items-per-second rate as a running budget
            // so a fractional rate still averages out. Pushing the entire slot in one shot was a burst most duct
            // networks can't absorb at once; smaller, more frequent pushes are more realistic.
            if (autoDrainEnabled && ModMath.Time.hasSecondsPassed(level, PUSH_INTERVAL_SECONDS)) {
                double rate = CrawStats.getItemTransferPerSecond(getEvolutionState());
                pushBudget = Math.min(pushBudget + rate, rate * 2);
                ItemStack stored = getStoredStack();
                int maxAmount = (int) pushBudget;
                if (!stored.isEmpty() && maxAmount > 0) {
                    int before = stored.getCount();
                    ModItemUtil.pushItemToBelowTransport(level, worldPosition, INVENTORY, STORAGE_SLOT, maxAmount);
                    pushBudget -= before - getStoredCount();
                }
            }
        }
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
        super(ModBlockEntities.CRAW_BE.get(), pos, blockState);
    }

    // Which screen tab was last open -- same pattern as every other Module-tab machine, so
    // reopening the screen returns to the tab last viewed.
    private boolean moduleTabActive = false;

    @Override
    public boolean isModuleTabActive() {
        return moduleTabActive;
    }

    @Override
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

    // ---- Click gestures ---------------------------------------------------------------------------------
    // Standing, holding an item: one click deposits one item; a second click with the same item inside the
    // configured window (Config.CRAW_DOUBLE_CLICK_TICKS) deposits the rest of the held stack. Crouching deposits every
    // matching stack from the player's inventory (see CrawClickEvents, which catches the crouch click). A held item that
    // exposes an item handler -- the Eater's buffer, another mod's backpack -- is a source: its contents are deposited
    // and the item itself never is. Matching is item plus components; an empty Craw takes the type from what is held.

    /** A player's last standing deposit click: when, and a one-count copy of what they held. */
    private record LastClick(long tick, ItemStack template, boolean source) {}

    private final Map<UUID, LastClick> lastClicks = new HashMap<>();

    /** The held item's item handler if it has one (a source to deposit from), otherwise null. */
    private static @Nullable IItemHandler getSource(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getCapability(Capabilities.ItemHandler.ITEM);
    }

    /** Whether this click follows the player's last one, within the window, with the same item. A source is compared by
     * item only, since its contents (and so its components) change with every deposit. */
    private boolean isDoubleClick(Player player, ItemStack template, boolean source) {
        LastClick last = lastClicks.get(player.getUUID());
        if (last == null || level == null || last.source() != source) return false;
        if (level.getGameTime() - last.tick() > Config.CRAW_DOUBLE_CLICK_TICKS.get()) return false;
        return source ? last.template().is(template.getItem())
                : ItemStack.isSameItemSameComponents(last.template(), template);
    }

    /**
     * Whether an empty-handed standing click is the second half of a double click that began with an item: the player's
     * last click deposited their last item (a single item, or the rest of a stack) inside the window, so this click finds
     * an empty hand. It is swallowed rather than opening the GUI, so the gesture finishes the same whatever was held. A
     * double click that starts empty-handed never gets here, since its first click already opened the GUI.
     */
    public boolean consumeEmptyHandDoubleClick(Player player) {
        LastClick last = lastClicks.get(player.getUUID());
        if (last == null || level == null) return false;
        lastClicks.remove(player.getUUID());
        return level.getGameTime() - last.tick() <= Config.CRAW_DOUBLE_CLICK_TICKS.get();
    }

    /**
     * A standing right click holding {@code held}: deposits one item, or the held stack on a double click. For a source,
     * one item (or one slot's worth) from its contents. Returns false when nothing moved, so the click falls through to
     * the GUI.
     */
    public boolean handleStandingClick(Player player, ItemStack held) {
        if (level == null || level.isClientSide || held.isEmpty()) return false;

        IItemHandler source = getSource(held);
        // A component belongs to the whole stack, so taking from a stack of several gadgets would not stick.
        if (source != null && held.getCount() > 1) return false;

        ItemStack template = held.copyWithCount(1);
        boolean isDouble = isDoubleClick(player, template, source != null);

        int moved;
        if (source != null) {
            int slot = findSourceSlot(source);
            moved = slot < 0 ? 0 : moveFromSlot(source, slot, isDouble ? Integer.MAX_VALUE : 1);
        } else {
            int amount = isDouble ? held.getCount() : 1;
            ItemStack leftover = INVENTORY.insertItem(STORAGE_SLOT, held.copyWithCount(amount), false);
            moved = amount - leftover.getCount();
            held.shrink(moved);
        }
        if (moved <= 0) return false;

        if (isDouble) {
            lastClicks.remove(player.getUUID());
        } else {
            lastClicks.put(player.getUUID(), new LastClick(level.getGameTime(), template, source != null));
        }
        return true;
    }

    /**
     * A crouching right click: deposits everything matching from a held source (if there is one), then every matching
     * stack in the player's inventory. The type to match is what the Craw holds, or, when it is empty, the held item.
     * Returns false when there was nothing to match against (an empty Craw and nothing useful in hand), so the caller
     * opens the GUI instead.
     */
    public boolean handleCrouchClick(Player player, ItemStack held) {
        if (level == null || level.isClientSide) return true;
        lastClicks.remove(player.getUUID());

        IItemHandler source = getSource(held);
        if (source != null && held.getCount() == 1) pullAllFromSource(source);

        ItemStack type = getStoredStack();
        if (type.isEmpty() && source == null && !held.isEmpty()) type = held.copyWithCount(1);
        if (type.isEmpty()) return false;

        vacuumFromInventory(player, type);
        return true;
    }

    /** The first slot of {@code source} holding the Craw's type, or, when the Craw is empty, the first non-empty slot. */
    private int findSourceSlot(IItemHandler source) {
        ItemStack stored = getStoredStack();
        for (int i = 0; i < source.getSlots(); i++) {
            ItemStack inSlot = source.getStackInSlot(i);
            if (inSlot.isEmpty()) continue;
            if (stored.isEmpty() || ItemStack.isSameItemSameComponents(inSlot, stored)) return i;
        }
        return -1;
    }

    /** Moves up to {@code amount} from one slot of {@code source} into storage, as much as fits. Returns how many moved. */
    private int moveFromSlot(IItemHandler source, int slot, int amount) {
        ItemStack probe = source.extractItem(slot, amount, true);
        if (probe.isEmpty()) return 0;
        int fits = probe.getCount() - INVENTORY.insertItem(STORAGE_SLOT, probe, true).getCount();
        if (fits <= 0) return 0;

        ItemStack taken = source.extractItem(slot, fits, false);
        if (taken.isEmpty()) return 0;
        ItemStack leftover = INVENTORY.insertItem(STORAGE_SLOT, taken, false);
        if (!leftover.isEmpty()) source.insertItem(slot, leftover, false); // not expected after the probe above
        return taken.getCount() - leftover.getCount();
    }

    /** Moves everything of the Craw's type out of {@code source}, slot by slot. Returns how many moved. */
    private int pullAllFromSource(IItemHandler source) {
        int total = 0;
        for (int guard = 0; guard < source.getSlots() * 4; guard++) {
            int slot = findSourceSlot(source);
            if (slot < 0) break;
            int moved = moveFromSlot(source, slot, Integer.MAX_VALUE);
            if (moved <= 0) break;
            total += moved;
        }
        return total;
    }

    /**
     * Deposits every stack matching {@code type} out of the player's main inventory (36 slots; armor and offhand are not
     * scanned). {@code INVENTORY.insertItem}'s slot-limit override (see {@link #capacityFor(int)}) caps each insertion at
     * the Craw's remaining room, so whichever runs out first, the player's matching stock or the room, stops it.
     */
    private void vacuumFromInventory(Player player, ItemStack type) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack slotStack = inv.items.get(i);
            if (slotStack.isEmpty() || !ItemStack.isSameItemSameComponents(slotStack, type)) continue;

            ItemStack remainder = INVENTORY.insertItem(STORAGE_SLOT, slotStack.copy(), false);
            int inserted = slotStack.getCount() - remainder.getCount();
            if (inserted <= 0) continue;
            slotStack.shrink(inserted);
        }
        inv.setChanged();
    }

    // ---- Left click: withdrawal -------------------------------------------------------------------------
    // The mirror of the right-click gestures, and a left click never opens the GUI. Standing: one click takes one
    // item, a second click inside the double-click window takes a full stack; both go to the player's inventory.
    // Crouching: fills the held item's container (an item with an item handler, such as the Eater) or, with none in
    // hand, the player's inventory, until that is full or the Craw is empty. Holding the click for
    // Config.CRAW_BREAK_HOLD_TICKS breaks the Craw, in survival (through the block's destroy progress, see
    // CrawBlock#getDestroyProgress) and in creative (see onCreativeLeftClick).

    /** Player -> game tick of their last standing withdraw click, for the double click. */
    private final Map<UUID, Long> lastWithdrawClicks = new HashMap<>();

    /** A creative player's run of left-click events on this Craw: the last one and the first. */
    private record HoldChain(long lastTick, long startTick) {}

    private final Map<UUID, HoldChain> holdChains = new HashMap<>();

    // A held attack key in creative re-sends a left click every 6 ticks (the client's destroy delay is 5), so events
    // that far apart, give or take network jitter, are one hold rather than separate clicks.
    private static final int HOLD_GAP_MIN_TICKS = 5;
    private static final int HOLD_GAP_MAX_TICKS = 8;

    /** A left click that starts a withdraw gesture; the caller handles the break hold. Server side only. */
    public void handleLeftClick(Player player, ItemStack held) {
        if (level == null || level.isClientSide) return;
        if (getStoredStack().isEmpty()) {
            lastWithdrawClicks.remove(player.getUUID());
            return;
        }

        if (player.isShiftKeyDown()) {
            lastWithdrawClicks.remove(player.getUUID());
            IItemHandler container = getSource(held);
            if (container != null && held.getCount() == 1) {
                fillContainerFromStorage(container);
            } else {
                fillInventoryFromStorage(player);
            }
            return;
        }

        long now = level.getGameTime();
        Long last = lastWithdrawClicks.get(player.getUUID());
        boolean isDouble = last != null && now - last <= Config.CRAW_DOUBLE_CLICK_TICKS.get();

        int amount = isDouble ? getStoredStack().getMaxStackSize() : 1;
        IItemHandler container = held.getCount() == 1 ? getSource(held) : null;
        if (container != null) {
            // A held container (the Eater) takes the withdrawal, and only as much as it can hold: the rest stays in
            // the Craw. A container that is full or refuses the item moves nothing.
            ItemStack probe = INVENTORY.extractItem(STORAGE_SLOT, amount, true);
            ItemStack unplaced = probe.copy();
            for (int slot = 0; slot < container.getSlots() && !unplaced.isEmpty(); slot++) {
                unplaced = container.insertItem(slot, unplaced, true);
            }
            int fits = probe.getCount() - unplaced.getCount();
            if (fits <= 0) return;

            ItemStack taken = INVENTORY.extractItem(STORAGE_SLOT, fits, false);
            for (int slot = 0; slot < container.getSlots() && !taken.isEmpty(); slot++) {
                taken = container.insertItem(slot, taken, false);
            }
            if (!taken.isEmpty()) giveToHandFirst(player, taken); // not expected after the probe above
        } else {
            ItemStack taken = INVENTORY.extractItem(STORAGE_SLOT, amount, false);
            if (taken.isEmpty()) return;
            giveToHandFirst(player, taken);
        }

        if (isDouble) {
            lastWithdrawClicks.remove(player.getUUID());
        } else {
            lastWithdrawClicks.put(player.getUUID(), now);
        }
    }

    /** Gives {@code stack} to the player's main hand if it is empty or holds the same item with room, and whatever does
     * not fit there to their inventory (dropping it at their feet if that is full too). */
    private static void giveToHandFirst(Player player, ItemStack stack) {
        ItemStack inHand = player.getMainHandItem();
        if (inHand.isEmpty()) {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
            return;
        }
        if (ItemStack.isSameItemSameComponents(inHand, stack)) {
            int room = inHand.getMaxStackSize() - inHand.getCount();
            int moved = Math.min(room, stack.getCount());
            if (moved > 0) {
                inHand.grow(moved);
                stack.shrink(moved);
            }
        }
        if (!stack.isEmpty()) player.getInventory().placeItemBackInInventory(stack);
    }

    /**
     * A creative player's left click. The click is cancelled before vanilla can break the block instantly (see
     * CrawClickEvents), so this runs the gesture and breaks the Craw once the click has been held long enough: the
     * server sees a run of events a few ticks apart while the key is down, and the run's length is the hold. Server
     * side only.
     */
    public void onCreativeLeftClick(net.minecraft.server.level.ServerPlayer player, ItemStack held) {
        if (level == null) return;
        long now = level.getGameTime();
        HoldChain chain = holdChains.get(player.getUUID());
        long gap = chain == null ? Long.MAX_VALUE : now - chain.lastTick();
        boolean holding = gap >= HOLD_GAP_MIN_TICKS && gap <= HOLD_GAP_MAX_TICKS;

        if (holding) {
            chain = new HoldChain(now, chain.startTick());
        } else {
            handleLeftClick(player, held); // a fresh click, not the held key repeating
            chain = new HoldChain(now, now);
        }
        holdChains.put(player.getUUID(), chain);

        if (now - chain.startTick() >= Config.CRAW_BREAK_HOLD_TICKS.get()) {
            holdChains.remove(player.getUUID());
            player.gameMode.destroyBlock(worldPosition);
        }
    }

    /** Moves stored items into {@code container} until it is full or the Craw is empty. */
    private void fillContainerFromStorage(IItemHandler container) {
        for (int guard = 0; guard < 4096 && !getStoredStack().isEmpty(); guard++) {
            ItemStack probe = INVENTORY.extractItem(STORAGE_SLOT, getStoredStack().getMaxStackSize(), true);
            if (probe.isEmpty()) return;

            ItemStack leftover = probe.copy();
            for (int slot = 0; slot < container.getSlots() && !leftover.isEmpty(); slot++) {
                leftover = container.insertItem(slot, leftover, false);
            }
            int moved = probe.getCount() - leftover.getCount();
            if (moved <= 0) return;

            INVENTORY.extractItem(STORAGE_SLOT, moved, false);
            if (moved < probe.getCount()) return; // the container is full
        }
    }

    /** Moves stored items into the player's inventory until it is full or the Craw is empty. */
    private void fillInventoryFromStorage(Player player) {
        for (int guard = 0; guard < 4096 && !getStoredStack().isEmpty(); guard++) {
            ItemStack probe = INVENTORY.extractItem(STORAGE_SLOT, getStoredStack().getMaxStackSize(), true);
            if (probe.isEmpty()) return;

            ItemStack leftover = probe.copy();
            player.getInventory().add(leftover); // leaves whatever did not fit in leftover
            int moved = probe.getCount() - leftover.getCount();
            if (moved <= 0) return;

            INVENTORY.extractItem(STORAGE_SLOT, moved, false);
            if (moved < probe.getCount()) return; // the inventory is full
        }
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
        dropItems(level, MODULE_INVENTORY, worldPosition);
        dropItems(level, EVOLUTION_INVENTORY, worldPosition);
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
        tag.put("craw_module_inv", MODULE_INVENTORY.serializeNBT(registries));
        tag.putBoolean("module_tab_active", moduleTabActive);
        tag.putInt("evolution_flourish_ticks", flourishTicksRemaining);
        tag.putInt("evolution_flourish_total", flourishTotalTicks);
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
        if (tag.contains("craw_module_inv")) {
            loadItemHandler(MODULE_INVENTORY, getModuleSlotCount(), registries, tag.getCompound("craw_module_inv"));
        }
        moduleTabActive = tag.getBoolean("module_tab_active");
        flourishTicksRemaining = tag.contains("evolution_flourish_ticks")
                ? tag.getInt("evolution_flourish_ticks") : -1;
        flourishTotalTicks = Math.max(1, tag.getInt("evolution_flourish_total"));
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
