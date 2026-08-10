package net.scruffy.dermicraft.screen.custom.workbench;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.custom.floor.GearStationPool;
import net.scruffy.dermicraft.block.entity.custom.WorkbenchBlockEntity;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.interfaces.IWorkbenchSwappable;
import net.scruffy.dermicraft.item.custom.SunderItem;
import net.scruffy.dermicraft.recipe.gadget_fabricating.GadgetFabricatingRecipe;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

import java.util.List;
import org.jetbrains.annotations.Nullable;

public class WorkbenchMenu extends AbstractModMenu {

    // Storage strip's on-screen anchor -- sits in its own zone below the main panel and the
    // player hotbar, not embedded inside the main background. See WorkbenchScreen for the
    // matching background art and MAIN_PANEL_HEIGHT/STRIP_GAP that derive this Y.
    public static final int STRIP_X = 7;
    public static final int STRIP_Y = 177;

    // Mod page (Swap page -- "Mod" is just this tab's display label): working-item slot top-left,
    // whichever gadget's own IWorkbenchSwappable panel beside it (see #panel below) -- generalized
    // 2026-08-10, was hardcoded to Sunder's chain+fuel slots specifically before.
    public static final int WORK_SLOT_X = 8;
    public static final int WORK_SLOT_Y = 8;
    // The panel's own origin -- (0, 0) so a gadget's panel renders at exactly the same relative
    // position here as it does hosted by the Scrench (see ScrenchMenu), rather than each host
    // maintaining its own separately-tuned coordinates for the same panel content.
    public static final int PANEL_X = 0;
    public static final int PANEL_Y = 0;

    // Combined gauge+fill-slot asset (tank_and_slot.png, 18x66), Sunder-specific -- kept here since
    // WorkbenchScreen needs it and this class is common-side/that's client-only. The slot itself
    // sits at the bottom 18px of that asset, not at its own top -- see WorkbenchScreen.
    public static final int FUEL_TANK_HEIGHT = 66;
    public static final int FUEL_FILL_SLOT_Y = SunderItem.FUEL_TANK_Y + FUEL_TANK_HEIGHT - 18;

    // Fabrication page's output slot -- sits beside the progress arrow/Craft button with a 5px gap,
    // holds the finished gadget regardless of which recipe is currently selected in the browser.
    // Position duplicated rather than shared (this class is common-side, WorkbenchScreen is
    // client-only) -- mirrors WorkbenchScreen's FAB_DETAIL_X (8) + FAB_ACTION_WIDTH (18) + 5px gap,
    // and FAB_ACTION_Y (54).
    public static final int OUTPUT_SLOT_X = 31;
    public static final int OUTPUT_SLOT_Y = 54;

    // Slot indices for WorkbenchScreen's tooltip lookups -- matches AbstractModMenu's own
    // assumption (vanilla hotbar+inventory first, TE-specific slots after), since addPlayerInventory/
    // addPlayerHotbar run before the storage strip/Mod-page slots in the constructor below.
    private static final int VANILLA_SLOT_COUNT = 36;
    public static final int WORK_SLOT_INDEX = VANILLA_SLOT_COUNT + StorageStripSlot.COLUMNS;
    // The gadget panel's own slots (0-2 of them depending which gadget) start right after the work
    // slot -- WorkbenchScreen locates its own tooltip targets (e.g. Sunder's chain slot) by asking
    // this menu for the panel's slots directly (see #panelSlots) rather than a fixed index, since
    // slot count now varies per gadget.
    public static final int PANEL_SLOTS_START_INDEX = WORK_SLOT_INDEX + 1;
    // Sunder's panel is always exactly [chain slot, fuel-fill slot] in that order (see
    // SunderItem.SunderSwapPanel#slots) -- these stay valid convenience accessors for
    // WorkbenchScreen's Sunder-specific tooltip lookups regardless of which host built the panel.
    public static final int CHAIN_SLOT_INDEX = PANEL_SLOTS_START_INDEX;
    public static final int FUEL_FILL_SLOT_INDEX = PANEL_SLOTS_START_INDEX + 1;

    // clickMenuButton ids, mirrors vanilla's loom/enchant-table button convention (already used
    // in this codebase by MrShepardMenu's population-cap buttons). Absolute row-set ids and the
    // fill-from-pool id are offset well past the two fixed scroll ids so they can't collide --
    // row ids grow with capacity (see setRowButtonId), so fill-from-pool gets a fixed high id
    // instead of sitting right after them.
    public static final int BUTTON_SCROLL_UP = 0;
    public static final int BUTTON_SCROLL_DOWN = 1;
    public static final int BUTTON_SET_ROW_BASE = 2;
    public static final int BUTTON_FILL_FROM_POOL = 100;
    // GUI-state persistence (see WorkbenchBlockEntity's own fields) -- which page is open and which
    // recipe is selected, per-block. Well clear of BUTTON_SET_ROW_BASE's own growth range (row ids
    // grow with Storage capacity, currently maxing at 9 slots / a handful of rows).
    public static final int BUTTON_SET_MOD_PAGE = 101;
    public static final int BUTTON_SET_FABRICATION_PAGE = 102;
    public static final int BUTTON_SELECT_RECIPE_BASE = 150;
    // Well clear of BUTTON_SELECT_RECIPE_BASE's own growth range.
    public static final int BUTTON_START_FABRICATION_BASE = 200;

    public static int setRowButtonId(int row) {
        return BUTTON_SET_ROW_BASE + row;
    }

    public static int selectRecipeButtonId(int recipeIndex) {
        return BUTTON_SELECT_RECIPE_BASE + recipeIndex;
    }

    public static int startFabricationButtonId(int recipeIndex) {
        return BUTTON_START_FABRICATION_BASE + recipeIndex;
    }

    public final WorkbenchBlockEntity be;
    private final Level level;

    // The current working item's own IWorkbenchSwappable panel, if it has one -- built once at
    // menu-open against whatever's in WORK_ITEM at that moment (see #panel's own field javadoc for
    // the known limitation this carries: swapping to a DIFFERENT gadget type while this menu stays
    // open doesn't rebuild the panel, same limitation the pre-existing Sunder-only implementation
    // already had). Null if the work item is empty or not swappable.
    @Nullable
    private final IWorkbenchSwappable.SwapPanel panel;

    // Mirrors WorkbenchBlockEntity#isFabricationPageActive (initialized from it below) so slot
    // visibility is correct from the first frame, not just after the screen's own init() runs.
    // Slot.x/y are final in this version, so hiding the Mod page's real slots (work item + gadget
    // panel) while Fabrication is showing goes through Slot#isActive() instead of repositioning them
    // off-screen; see WorkbenchScreen#setPage.
    private boolean modPageActive;

    public void setModPageActive(boolean active) {
        this.modPageActive = active;
    }

    public WorkbenchMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public WorkbenchMenu(int containerId, Inventory inventory, BlockEntity blockEntity) {
        super(ModMenuTypes.WORKBENCH_MENU.get(), containerId, StorageStripSlot.COLUMNS);

        this.be = (WorkbenchBlockEntity) blockEntity;
        this.level = inventory.player.level();
        this.modPageActive = !be.isFabricationPageActive();

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);

        for (int column = 0; column < StorageStripSlot.COLUMNS; column++) {
            this.addSlot(new StorageStripSlot(be.STORAGE, be::getScrollRow, column,
                    STRIP_X + 1 + column * 18, STRIP_Y + 1));
        }

        this.addSlot(new net.neoforged.neoforge.items.SlotItemHandler(be.WORK_ITEM, 0, WORK_SLOT_X + 1, WORK_SLOT_Y + 1) {
            @Override
            public boolean isActive() {
                return modPageActive;
            }
        });

        ItemStack workStack = be.WORK_ITEM.getStackInSlot(0);
        this.panel = workStack.getItem() instanceof IWorkbenchSwappable swappable
                ? swappable.openSwapPanel(() -> be.WORK_ITEM.getStackInSlot(0), inventory.player, false)
                : null;
        if (panel != null) {
            panel.slots(PANEL_X, PANEL_Y, () -> modPageActive).forEach(this::addSlot);
        }

        this.addSlot(new SlotItemHandler(be.OUTPUT, 0, OUTPUT_SLOT_X + 1, OUTPUT_SLOT_Y + 1) {
            @Override
            public boolean isActive() {
                return !modPageActive;
            }
        });

        // Server-only: drives the paired top half's open/closed animation (see
        // WorkbenchBlockEntity#onMenuOpened). Menu constructors also run client-side to build the
        // local screen's mirror, so this must not double-count there.
        if (!level.isClientSide) {
            be.onMenuOpened();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.WORKBENCH, be);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!level.isClientSide) {
            be.onMenuClosed();
            if (panel != null) panel.onClosed(player);
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (level.isClientSide) return true;
        if (id == BUTTON_SCROLL_UP) {
            be.changeScrollRow(-1);
        } else if (id == BUTTON_SCROLL_DOWN) {
            be.changeScrollRow(1);
        } else if (id == BUTTON_FILL_FROM_POOL) {
            fillWorkItemFromPool();
        } else if (id == BUTTON_SET_MOD_PAGE) {
            be.setFabricationPageActive(false);
        } else if (id == BUTTON_SET_FABRICATION_PAGE) {
            be.setFabricationPageActive(true);
        } else if (id >= BUTTON_START_FABRICATION_BASE) {
            startFabrication(id - BUTTON_START_FABRICATION_BASE);
        } else if (id >= BUTTON_SELECT_RECIPE_BASE) {
            selectRecipe(id - BUTTON_SELECT_RECIPE_BASE);
        } else if (id >= BUTTON_SET_ROW_BASE) {
            be.setScrollRow(id - BUTTON_SET_ROW_BASE);
        } else {
            return false;
        }
        return true;
    }

    /** Resolves the same sorted recipe index the client's grid used (see
     * {@link GadgetFabricatingRecipe#allRecipes}) and starts that job -- the server never trusts the
     * client's own craftability read, {@link WorkbenchBlockEntity#startFabrication} re-checks
     * everything (tier, output room, pool availability) itself. */
    private void startFabrication(int recipeIndex) {
        List<RecipeHolder<GadgetFabricatingRecipe>> recipes = GadgetFabricatingRecipe.allRecipes(level);
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) return;

        be.startFabrication(recipes.get(recipeIndex).id());
    }

    /** Persists which recipe the player clicked in the Fabrication grid, purely so reopening this
     * same Workbench returns to the same selection -- see WorkbenchBlockEntity's own field javadoc. */
    private void selectRecipe(int recipeIndex) {
        List<RecipeHolder<GadgetFabricatingRecipe>> recipes = GadgetFabricatingRecipe.allRecipes(level);
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) return;

        be.setSelectedFabricationRecipeId(recipes.get(recipeIndex).id());
    }

    /**
     * Tops the Mod page's working item off from the station's shared Gear Station fluid pool -- the
     * station-only counterpart to {@code SunderItem.SunderSwapPanel}'s carried-fuel fill slot (see
     * dermicraft-gear-stations-notes.md -> Swap page, "Fuel: both refuel paths, not one").
     *
     * <p>Not Sunder-specific by design: anything in the working-item slot exposing a fluid handler
     * can be topped off, so a second fuel-carrying gadget needs no changes here.
     */
    private void fillWorkItemFromPool() {
        ItemStack workStack = be.WORK_ITEM.getStackInSlot(0);
        IFluidHandlerItem tank = workStack.getCapability(Capabilities.FluidHandler.ITEM, null);
        if (tank == null) return;

        if (GearStationPool.fillFromPool(level, be.getBlockPos(), tank) <= 0) return;

        // The handler may hand back a different stack rather than mutating in place, so the
        // container is written back explicitly rather than assumed updated.
        be.WORK_ITEM.setStackInSlot(0, tank.getContainer());
    }

    /** Read straight off the BE's own synced fields -- fully known client-side via the normal
     * full-BE-data sync path (same rationale as scrollRow/open/active), no menu-level sync needed. */
    public boolean isFabricating() {
        return be.isFabricating();
    }

    public boolean isFabricating(RecipeHolder<GadgetFabricatingRecipe> recipe) {
        return recipe.id().equals(be.getActiveFabricationRecipeId());
    }

    public int getScaledFabricationProgress(int scale) {
        return be.getScaledProgress(scale);
    }

    /**
     * On the server this is always correct (a live capability read). On the client, a live read
     * would be wrong for anything that doesn't proactively sync to nearby players (a vanilla chest
     * connected via the Lab Floor network, in particular) -- see WorkbenchPoolSyncPayload's own
     * javadoc -- so the client instead reads WorkbenchBlockEntity's own periodic broadcast via
     * {@link WorkbenchClientPool}.
     */
    public GearStationPool.Snapshot getPoolSnapshot() {
        return level.isClientSide
                ? WorkbenchClientPool.get(be.getBlockPos())
                : GearStationPool.snapshot(level, be.getBlockPos());
    }

    public int getStationTier() {
        return be.getStationTier();
    }

    /** Whether the Fabrication page was open the last time this Workbench's screen was open --
     * read once by WorkbenchScreen#init to restore it, see WorkbenchBlockEntity's own field. */
    public boolean isFabricationPageActive() {
        return be.isFabricationPageActive();
    }

    /** Resolves the persisted selected-recipe id back into an index into the current sorted recipe
     * list (see GadgetFabricatingRecipe#allRecipes) -- storage is by id, not index, so this stays
     * correct even if the registered recipe set changes between sessions. Returns -1 if nothing was
     * selected, or the previously-selected recipe no longer exists. */
    public int getSelectedFabricationIndex() {
        ResourceLocation selected = be.getSelectedFabricationRecipeId();
        if (selected == null) return -1;

        List<RecipeHolder<GadgetFabricatingRecipe>> recipes = GadgetFabricatingRecipe.allRecipes(level);
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).id().equals(selected)) return i;
        }
        return -1;
    }

    public int getScrollRow() {
        return be.getScrollRow();
    }

    public int getMaxScrollRow() {
        return be.getMaxScrollRow();
    }

    public ItemStack getWorkItemStack() {
        return be.WORK_ITEM.getStackInSlot(0);
    }

    public boolean isWorkItemSunder() {
        return getWorkItemStack().getItem() instanceof SunderItem;
    }

    public boolean isWorkItemEater() {
        return getWorkItemStack().getItem() instanceof net.scruffy.dermicraft.item.custom.EaterItem;
    }

    /** Reads straight off the current work item's own stack -- fully known client-side via the
     * normal slot-sync path, no extra menu-level sync needed (mirrors ScrenchMenu's own getter). */
    public FluidStack getSunderFluid() {
        return getWorkItemStack().getOrDefault(ModDataComponentTypes.FLUID_DATA.get(), FluidData.EMPTY).getFluidStack();
    }

    public int getSunderFuelCapacity() {
        return SunderItem.FUEL_CAPACITY;
    }
}
