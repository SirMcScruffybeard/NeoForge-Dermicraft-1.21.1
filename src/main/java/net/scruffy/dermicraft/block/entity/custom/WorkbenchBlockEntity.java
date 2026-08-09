package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.custom.WorkbenchBlock;
import net.scruffy.dermicraft.block.custom.floor.GearStationPool;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.interfaces.IGadget;
import net.scruffy.dermicraft.interfaces.IPreserveContentsOnPickup;
import net.scruffy.dermicraft.interfaces.IWorkbenchSwappable;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.recipe.gadget_fabricating.GadgetFabricatingRecipe;
import net.scruffy.dermicraft.screen.custom.workbench.StorageStripSlot;
import net.scruffy.dermicraft.screen.custom.workbench.WorkbenchMenu;
import net.scruffy.dermicraft.tank.FuelTank;
import net.scruffy.dermicraft.util.ModFluidUtil;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Workbench (see dermicraft-gear-stations-notes.md -> Workbench). This slice builds the Storage
 * strip (Duty 1, restricted to gadgets/Frames/exchangeable parts/Damageable items, scrollable one
 * row of 9 at a time), the Mod/Swap page, and the Fabrication display. Implements
 * IPreserveContentsOnPickup so Forceps recovery (COLLECTIBLE tag, bottom only) carries Storage/
 * Work-item contents onto the recovered item instead of spilling them -- same as the Craw.
 */
public class WorkbenchBlockEntity extends MachineBaseBlockEntity implements MenuProvider, GeoBlockEntity, IPreserveContentsOnPickup {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // Tier 1 fixed for now -- doubles per station tier (9 -> 18 -> 36 -> 72) once station tiering
    // exists; see the Storage strip's own design notes for the growth curve.
    public static final int STORAGE_CAPACITY = 9;

    public final ItemStackHandler STORAGE = new ItemStackHandler(STORAGE_CAPACITY) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isStorageEligible(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            updateBlock();
        }

        // ItemStackHandler#deserializeNBT resizes itself to whatever "Size" was saved in NBT
        // *before* loading items, overriding whatever size the constructor requested. A save made
        // at a smaller capacity (an earlier tier, or this session's pre-bump test data) would
        // otherwise silently shrink STORAGE back down on load, desyncing it from
        // StorageStripSlot's index math (which assumes STORAGE_CAPACITY slots always exist) --
        // this restores the real capacity afterward, preserving whatever loaded. Matters for real
        // tier-ups too, not just this test value.
        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            super.deserializeNBT(provider, nbt);
            if (getSlots() != STORAGE_CAPACITY) {
                NonNullList<ItemStack> loaded = NonNullList.withSize(getSlots(), ItemStack.EMPTY);
                for (int i = 0; i < getSlots(); i++) {
                    loaded.set(i, getStackInSlot(i));
                }
                setSize(STORAGE_CAPACITY);
                for (int i = 0; i < Math.min(loaded.size(), STORAGE_CAPACITY); i++) {
                    setStackInSlot(i, loaded.get(i));
                }
            }
        }
    };

    // Saved/loaded purely to ride the existing BE-sync pathway (see saveAdditional), same "no
    // DataSlot" convention MrShepardBlockEntity's population cap already uses in this codebase --
    // not because a UI scroll position is meaningful long-term world state.
    private int scrollRow = 0;

    // Mod page's working-item slot (Duties 4+5, see the Swap page design notes) -- separate from
    // STORAGE, persists across menu close so a gadget left mid-service stays put.
    public final ItemStackHandler WORK_ITEM = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.isEmpty() || stack.getItem() instanceof IWorkbenchSwappable;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            updateBlock();
        }
    };

    // Fabrication's dedicated output slot (Duty 6, see the Fabrication page design notes) --
    // machine-produced only, holds the finished gadget until the player manually takes it. Not
    // insert-restricted at the GUI layer, same convention RenderKilnMenu's own output slot follows
    // (a plain SlotItemHandler; the "no automation inserts" rule lives at the capability layer,
    // which the Workbench doesn't expose externally at all yet).
    public final ItemStackHandler OUTPUT = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            updateBlock();
        }
    };

    // The single active Fabrication job, if any -- a recipe id rather than a RecipeHolder so it
    // survives NBT round-trips the same way AbstractFueledMachineBlockEntity's own
    // pendingRecipeId/saved_recipe does. progress/maxProgress (inherited from MachineBaseBlockEntity)
    // track the job itself; this is just "which recipe."
    @Nullable
    private ResourceLocation activeFabricationRecipeId = null;

    // Duty 3 (general-purpose durability repair) -- see dermicraft-gear-stations-notes.md ->
    // Workbench, "General-purpose durability repair". Single active job, cycling through STORAGE
    // one Damageable item at a time. repairSlot is -1 when nothing is being repaired. Deliberately
    // separate from the Fabrication job's own progress/maxProgress (inherited from
    // MachineBaseBlockEntity) so the two duties can run independently.
    private static final int BASE_REPAIR_POINTS = 1; // durability points repaired/cycle at Heal modifier 1.0 (Crude Slurry)

    private int repairSlot = -1;
    private int repairProgress = 0;
    private int repairMaxProgress = 0;

    /**
     * Advances Duty 3. Every {@link #CRAFT_TICKS}, picks the next damaged Damageable item in Storage
     * if nothing is currently targeted, then spends one cycle's worth of shared-pool fuel to chip
     * away at it -- repair magnitude scales with the loaded fuel's Heal modifier (same "BASE * heal"
     * shape {@code AbstractFueledMachineBlockEntity#getHealAmount} uses for machine healing), cost
     * scales with its Use-rate modifier (mod-wide cost convention, see
     * {@code MrShepardBlockEntity#fuelPerStep}). {@link IGadget} items are included: they already
     * reuse vanilla damage as their own HP stat and heal for free over time while carried (see
     * {@link net.scruffy.dermicraft.event.GadgetEvents}) -- Duty 3 just trades that idle time for
     * material, healing a stored gadget faster at the cost of fuel instead of waiting it out in a
     * pocket. If Storage has nothing to repair, or the pool has no fuel (or not enough for this
     * cycle's cost), nothing is drawn from the pool at all -- "no items being repaired" means zero
     * fuel use, and a mid-repair shortfall halts with progress preserved, same Gear Station rule
     * every fuel-driven process in this mod follows.
     */
    public void tickRepair(Level level) {
        if (!ModMath.Time.hasTicksPassed(level, CRAFT_TICKS)) return;

        if (repairSlot < 0 || !isRepairTarget(STORAGE.getStackInSlot(repairSlot))) {
            if (!pickNextRepairTarget()) return;
        }

        FluidStack fuel = GearStationPool.bestFuel(level, worldPosition);
        if (fuel.isEmpty()) return;

        float heal = ModFluidUtil.getHeal(fuel) / FuelTank.BASE_SPEED_MODIFIER;
        int points = Math.max(1, Math.round(BASE_REPAIR_POINTS * heal));
        int cost = Math.max(1, Math.round(ModFluidUtil.getUseRate(fuel) * CRAFT_TICKS));
        if (fuel.getAmount() < cost) return;

        GearStationPool.drainFuel(level, worldPosition, fuel, cost);

        ItemStack target = STORAGE.getStackInSlot(repairSlot);
        repairProgress = Math.min(repairMaxProgress, repairProgress + points);
        if (repairProgress >= repairMaxProgress) {
            target.setDamageValue(Math.max(0, target.getDamageValue() - repairMaxProgress));
            STORAGE.setStackInSlot(repairSlot, target);
            repairSlot = -1;
            repairProgress = 0;
            repairMaxProgress = 0;
        }
        setChanged();
        updateBlock();
    }

    private boolean pickNextRepairTarget() {
        for (int slot = 0; slot < STORAGE.getSlots(); slot++) {
            ItemStack candidate = STORAGE.getStackInSlot(slot);
            if (isRepairTarget(candidate)) {
                repairSlot = slot;
                repairMaxProgress = candidate.getDamageValue();
                repairProgress = 0;
                return true;
            }
        }
        return false;
    }

    private static boolean isRepairTarget(ItemStack stack) {
        return !stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged();
    }

    /**
     * Advances Duty 2 -- passively tops off every eligible stored gadget's own onboard fuel tank
     * from the shared pool, every cycle, independent of Duty 3's repair queue above and of whether
     * anyone has this Workbench's screen open (a block-entity tick concern, not a GUI one, same
     * "works while nobody's looking" model the design calls for). Eligibility is
     * {@link ModTags.Items#FUEL_CONSUMING_GADGETS}, deliberately not "anything in Storage exposing a
     * fluid handler" -- Sipping/Drinker's tanks hold payload fluid (drink content, vacuumed liquid),
     * not fuel, and would otherwise get silently topped off too, draining the shared pool for a tank
     * that was never meant to burn fuel at all (see the tag's own javadoc for the full reasoning).
     */
    public void tickRecharge(Level level) {
        if (!ModMath.Time.hasTicksPassed(level, CRAFT_TICKS)) return;

        for (int slot = 0; slot < STORAGE.getSlots(); slot++) {
            ItemStack stack = STORAGE.getStackInSlot(slot);
            if (stack.isEmpty() || !stack.is(ModTags.Items.FUEL_CONSUMING_GADGETS)) continue;

            IFluidHandlerItem tank = stack.getCapability(Capabilities.FluidHandler.ITEM, null);
            if (tank == null) continue;

            if (GearStationPool.fillFromPool(level, worldPosition, tank) <= 0) continue;
            // The handler may hand back a different stack rather than mutating in place, same
            // write-back rule WorkbenchMenu#fillWorkItemFromPool follows for the manual path.
            STORAGE.setStackInSlot(slot, tank.getContainer());
        }
    }

    // GUI convenience state -- which right-strip page and which Fabrication recipe were last
    // selected, purely so reopening THIS Workbench's screen returns to where the player left off,
    // rather than always resetting to the Mod page with nothing selected. Persisted per-block (same
    // save/sync pathway as scrollRow, "not because it's meaningful world state" but because that's
    // how the client mirror stays in sync) rather than as a client-only preference, so it's tied to
    // this specific block rather than shared/remembered across every Workbench.
    private boolean fabricationPageActive = false;
    @Nullable
    private ResourceLocation selectedFabricationRecipeId = null;

    public boolean isFabricationPageActive() {
        return fabricationPageActive;
    }

    public void setFabricationPageActive(boolean active) {
        if (fabricationPageActive != active) {
            fabricationPageActive = active;
            setChanged();
            updateBlock();
            if (active) broadcastPoolSync();
        }
    }

    @Nullable
    public ResourceLocation getSelectedFabricationRecipeId() {
        return selectedFabricationRecipeId;
    }

    public void setSelectedFabricationRecipeId(@Nullable ResourceLocation recipeId) {
        if (!java.util.Objects.equals(selectedFabricationRecipeId, recipeId)) {
            selectedFabricationRecipeId = recipeId;
            setChanged();
            updateBlock();
        }
    }

    public WorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(net.scruffy.dermicraft.block.entity.ModBlockEntities.WORKBENCH_BE.get(), pos, state);
    }

    public boolean isFabricating() {
        return activeFabricationRecipeId != null;
    }

    @Nullable
    public ResourceLocation getActiveFabricationRecipeId() {
        return activeFabricationRecipeId;
    }

    /** Hardcoded to Tier 1 until station tiering exists -- same convention as STORAGE_CAPACITY
     * above. Every currently-authored GadgetFabricatingRecipe is tier 1, so this isn't blocking
     * anything yet; it's just what GadgetFabricatingRecipe#meetsTier reads against. */
    public int getStationTier() {
        return 1;
    }

    /**
     * Starts a Fabrication job for {@code recipeId}, following the design's ordered checks: single
     * active job, station tier, output-slot conflict (vanilla-furnace convention), then pool
     * availability -- ingredients are consumed atomically here, at start, not gradually over the
     * craft (see Fabrication page notes, "Timed, single active job"). Returns whether the job
     * actually started; server-only.
     */
    public boolean startFabrication(ResourceLocation recipeId) {
        if (level == null || isFabricating()) return false;

        var recipeHolder = level.getRecipeManager().byKey(recipeId);
        if (recipeHolder.isEmpty() || !(recipeHolder.get().value() instanceof GadgetFabricatingRecipe recipe)) {
            return false;
        }
        if (!recipe.meetsTier(getStationTier())) return false;
        if (!OUTPUT.insertItem(0, recipe.getResult(), true).isEmpty()) return false;
        if (!GearStationPool.consumeForRecipe(level, worldPosition, recipe)) return false;

        activeFabricationRecipeId = recipeId;
        resetProgress();
        maxProgress = recipe.getCraftingTime();
        setChanged();
        updateBlock();
        return true;
    }

    /**
     * Advances the active Fabrication job, if any -- called from {@link net.scruffy.dermicraft.block.custom.WorkbenchBlock}'s
     * ticker. Draws shared-pool fuel each cycle the same way Duty 3's repair does, but with the
     * *other* half of that split: crafting uses the **Speed** modifier for pacing (`progress +=
     * round(CRAFT_TICKS * speed)`, same shape every fuel-driven Machine's own
     * {@code AbstractFueledMachineBlockEntity#incrementProgress} uses) while Duty 3 uses Heal for its
     * healing-shaped repair magnitude -- see dermicraft-gear-stations-notes.md -> Workbench, Duty 3,
     * for the reasoning behind that split. Cost per cycle scales with Use-rate, the mod-wide cost
     * convention. No fuel at all, or not enough for this cycle's cost, halts with progress preserved
     * (Gear Station rule -- no unfueled reduced-speed fallback the way a "dumb" Machine gets).
     */
    public void tickFabrication(Level level) {
        if (!isFabricating() || !ModMath.Time.hasTicksPassed(level, CRAFT_TICKS)) return;

        if (isStillCrafting()) {
            FluidStack fuel = GearStationPool.bestFuel(level, worldPosition);
            if (fuel.isEmpty()) return;

            float speed = ModFluidUtil.getSpeed(fuel) / FuelTank.BASE_SPEED_MODIFIER;
            int cost = Math.max(1, Math.round(ModFluidUtil.getUseRate(fuel) * CRAFT_TICKS));
            if (fuel.getAmount() < cost) return;

            GearStationPool.drainFuel(level, worldPosition, fuel, cost);
            progress += Math.max(1, Math.round(CRAFT_TICKS * speed));
        } else {
            completeFabrication(level);
        }
        setChanged();
        updateBlock();
    }

    /**
     * Keeps the client's Fabrication pool display fresh while it's actually looking at it -- see
     * {@link net.scruffy.dermicraft.network.WorkbenchPoolSyncPayload}'s own javadoc for why a vanilla
     * container (e.g. a chest connected via the Lab Floor network) needs this at all: unlike this
     * mod's own machines, it won't proactively sync its contents to a client that hasn't opened it,
     * so the client's own live capability query would otherwise show it as empty/unavailable forever
     * even though the real (server-side) pool, and therefore actual crafting, is correct. Gated to
     * "someone has this Workbench's menu open AND is on the Fabrication page" so an idle/Mod-page
     * Workbench doesn't broadcast for no reason.
     */
    public void tickPoolSync(Level level) {
        if (openCount == 0 || !fabricationPageActive || !ModMath.Time.hasTicksPassed(level, CRAFT_TICKS)) return;
        broadcastPoolSync();
    }

    private void broadcastPoolSync() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        GearStationPool.Snapshot snapshot = GearStationPool.snapshot(level, worldPosition);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(serverLevel,
                new net.minecraft.world.level.ChunkPos(worldPosition),
                new net.scruffy.dermicraft.network.WorkbenchPoolSyncPayload(worldPosition, snapshot.items(), snapshot.fluids()));
    }

    private void completeFabrication(Level level) {
        ResourceLocation recipeId = activeFabricationRecipeId;
        activeFabricationRecipeId = null;
        resetProgress();
        resetMaxProgress();
        if (recipeId == null) return;

        level.getRecipeManager().byKey(recipeId).ifPresent(holder -> {
            if (!(holder.value() instanceof GadgetFabricatingRecipe recipe)) return;

            ItemStack result = recipe.getResult();
            ItemStack leftover = OUTPUT.insertItem(0, result, false);
            // Shouldn't happen -- startFabrication already confirmed the output slot would accept
            // this exact result -- but drop rather than silently discard if something else changed
            // the slot in between (e.g. NBT-loaded external interference).
            if (!leftover.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), leftover);
            }
        });
    }

    // "active" is the persistent, independently-toggled power state -- right-clicking the bottom
    // (see WorkbenchBlock#useWithoutItem) flips it directly, with no GUI involved at all. "open" is
    // the actual visual/animation-driving flag (synced/persisted below, same pathway as scrollRow):
    // it's forced true for as long as the GUI (opened only via the top, see WorkbenchTopBlock) is
    // being viewed -- regardless of what active is -- and snaps back to whatever active is the
    // moment the last viewer closes it. Since active can only change via a bottom right-click, and
    // the GUI captures input while open, active can never actually change mid-view -- so this
    // reduces to "the models return to the state they were in when the GUI was opened."
    private int openCount = 0;
    private boolean active = false;
    private boolean open = false;

    public void toggleActive() {
        active = !active;
        if (openCount == 0) {
            setOpen(active);
        }
    }

    public void onMenuOpened() {
        openCount++;
        if (openCount == 1) {
            setOpen(true);
        }
        // Covers reopening the GUI already sitting on the Fabrication page -- otherwise the client
        // shows whatever it last cached (possibly stale, or nothing at all) until the next periodic
        // tickPoolSync fires.
        if (fabricationPageActive) broadcastPoolSync();
    }

    public void onMenuClosed() {
        openCount = Math.max(0, openCount - 1);
        if (openCount == 0) {
            setOpen(active);
        }
    }

    // Client-side read by WorkbenchScreenGlowLayer to gate the screen-on glow overlay to exactly
    // the activate/active window (not deactivate) -- see that class.
    public boolean isOpen() {
        return open;
    }

    private void setOpen(boolean open) {
        this.open = open;
        setChanged();
        updateBlock();
        if (level != null) {
            BlockState state = getBlockState();
            if (state.hasProperty(WorkbenchBlock.LIT) && state.getValue(WorkbenchBlock.LIT) != open) {
                level.setBlock(worldPosition, state.setValue(WorkbenchBlock.LIT, open), 3);
            }
            if (level.getBlockEntity(worldPosition.above()) instanceof WorkbenchTopBlockEntity top) {
                top.setOpen(open);
            }
        }
    }

    public static boolean isStorageEligible(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (stack.getItem() instanceof IGadget) return true;
        if (stack.isDamageableItem()) return true;
        return stack.is(ModItems.CHASSIS.get()) || stack.is(ModItems.ADDON_FRAME.get());
    }

    public int getMaxScrollRow() {
        return Math.max(0, (STORAGE_CAPACITY - 1) / StorageStripSlot.COLUMNS);
    }

    public int getScrollRow() {
        return scrollRow;
    }

    public void changeScrollRow(int delta) {
        setScrollRow(scrollRow + delta);
    }

    public void setScrollRow(int row) {
        int clamped = Math.max(0, Math.min(getMaxScrollRow(), row));
        if (clamped != scrollRow) {
            scrollRow = clamped;
            setChanged();
            updateBlock();
        }
    }

    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.WORKBENCH);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new WorkbenchMenu(containerId, inventory, this);
    }

    public void drops() {
        drops(STORAGE);
        drops(WORK_ITEM);
        drops(OUTPUT);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "screen", 0, state -> open
                ? state.setAndContinue(RawAnimation.begin().thenPlay("activate").thenLoop("active"))
                : state.setAndContinue(RawAnimation.begin().thenPlay("deactivate"))));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("storage", STORAGE.serializeNBT(registries));
        tag.put("work_item", WORK_ITEM.serializeNBT(registries));
        tag.put("output", OUTPUT.serializeNBT(registries));
        // scrollRow isn't meaningfully "world state" (it's a UI scroll position), but this
        // save/load pathway IS how the client-side BE mirror gets kept in sync in this codebase
        // (see MrShepardBlockEntity's populationCap) -- skipping it here means the server value
        // changes but the client never finds out, so the thumb/visible slots never update.
        tag.putInt("scrollRow", scrollRow);
        // Same rationale as scrollRow above -- keeps a late-joining client's "screen" bone in the
        // right pose instead of always starting closed regardless of current viewer count.
        tag.putBoolean("open", open);
        // The actual power-toggle state -- needs its own persistence separate from "open" so a
        // reload doesn't lose what the bottom's right-click was last set to.
        tag.putBoolean("active", active);
        // Fabrication job state -- progress/maxProgress aren't persisted by MachineBaseBlockEntity
        // itself (unlike AbstractFueledMachineBlockEntity's fuel-driven machines, which save their
        // own), so the Workbench owns that here alongside which recipe is running.
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        if (activeFabricationRecipeId != null) {
            tag.putString("activeFabricationRecipe", activeFabricationRecipeId.toString());
        }
        // GUI convenience state -- see the fields' own javadoc.
        tag.putBoolean("fabricationPageActive", fabricationPageActive);
        if (selectedFabricationRecipeId != null) {
            tag.putString("selectedFabricationRecipe", selectedFabricationRecipeId.toString());
        }
        // Duty 3 repair job state -- see the fields' own javadoc.
        tag.putInt("repairSlot", repairSlot);
        tag.putInt("repairProgress", repairProgress);
        tag.putInt("repairMaxProgress", repairMaxProgress);
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        STORAGE.deserializeNBT(registries, tag.getCompound("storage"));
        WORK_ITEM.deserializeNBT(registries, tag.getCompound("work_item"));
        OUTPUT.deserializeNBT(registries, tag.getCompound("output"));
        // Re-clamped against the CURRENT capacity, not just loaded verbatim -- a row saved while
        // capacity was higher (e.g. this session's now-reverted test bump) would otherwise still
        // compute an out-of-range slot index once capacity shrinks back down. Same category of bug
        // as STORAGE's own deserializeNBT-resize fix above, just on the derived row instead of the
        // handler's size.
        if (tag.contains("scrollRow")) {
            scrollRow = Math.max(0, Math.min(getMaxScrollRow(), tag.getInt("scrollRow")));
        }
        open = tag.getBoolean("open");
        active = tag.getBoolean("active");
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("maxProgress");
        activeFabricationRecipeId = tag.contains("activeFabricationRecipe", CompoundTag.TAG_STRING)
                ? ResourceLocation.parse(tag.getString("activeFabricationRecipe"))
                : null;
        fabricationPageActive = tag.getBoolean("fabricationPageActive");
        selectedFabricationRecipeId = tag.contains("selectedFabricationRecipe", CompoundTag.TAG_STRING)
                ? ResourceLocation.parse(tag.getString("selectedFabricationRecipe"))
                : null;
        repairSlot = tag.contains("repairSlot") ? tag.getInt("repairSlot") : -1;
        repairProgress = tag.getInt("repairProgress");
        repairMaxProgress = tag.getInt("repairMaxProgress");
    }
}
