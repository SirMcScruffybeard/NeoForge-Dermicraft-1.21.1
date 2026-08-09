package net.scruffy.dermicraft.screen.custom.workbench;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.WorkbenchBlockEntity;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.item.custom.SunderItem;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class WorkbenchMenu extends AbstractModMenu {

    // Storage strip's on-screen anchor -- sits in its own zone below the main panel and the
    // player hotbar, not embedded inside the main background. See WorkbenchScreen for the
    // matching background art and MAIN_PANEL_HEIGHT/STRIP_GAP that derive this Y.
    public static final int STRIP_X = 7;
    public static final int STRIP_Y = 177;

    // Mod page (Swap page -- "Mod" is just this tab's display label): working-item slot top-left,
    // Sunder's chain slot + fuel fill slot beside it. Only Sunder's panel is concrete right now --
    // see IWorkbenchSwappable's javadoc for how a second gadget's panel would extend this.
    public static final int WORK_SLOT_X = 8;
    public static final int WORK_SLOT_Y = 8;
    public static final int CHAIN_SLOT_X = 44;
    public static final int CHAIN_SLOT_Y = 8;
    // Combined gauge+fill-slot asset (tank_and_slot.png, 18x66) -- same asset/shape ScrenchScreen
    // already uses for this exact pairing. The slot itself sits at the bottom 18px of that asset,
    // not at its own top -- see WorkbenchScreen.
    public static final int FUEL_TANK_X = 80;
    public static final int FUEL_TANK_Y = 8;
    public static final int FUEL_TANK_HEIGHT = 66;
    public static final int FUEL_FILL_SLOT_X = FUEL_TANK_X;
    public static final int FUEL_FILL_SLOT_Y = FUEL_TANK_Y + FUEL_TANK_HEIGHT - 18;

    // Slot indices for WorkbenchScreen's tooltip lookups -- matches AbstractModMenu's own
    // assumption (vanilla hotbar+inventory first, TE-specific slots after), since addPlayerInventory/
    // addPlayerHotbar run before the storage strip/Mod-page slots in the constructor below.
    private static final int VANILLA_SLOT_COUNT = 36;
    public static final int WORK_SLOT_INDEX = VANILLA_SLOT_COUNT + StorageStripSlot.COLUMNS;
    public static final int CHAIN_SLOT_INDEX = WORK_SLOT_INDEX + 1;
    public static final int FUEL_FILL_SLOT_INDEX = CHAIN_SLOT_INDEX + 1;

    // clickMenuButton ids, mirrors vanilla's loom/enchant-table button convention (already used
    // in this codebase by MrShepardMenu's population-cap buttons). Absolute row-set ids and the
    // fill-from-pool id are offset well past the two fixed scroll ids so they can't collide --
    // row ids grow with capacity (see setRowButtonId), so fill-from-pool gets a fixed high id
    // instead of sitting right after them.
    public static final int BUTTON_SCROLL_UP = 0;
    public static final int BUTTON_SCROLL_DOWN = 1;
    public static final int BUTTON_SET_ROW_BASE = 2;
    public static final int BUTTON_FILL_FROM_POOL = 100;

    public static int setRowButtonId(int row) {
        return BUTTON_SET_ROW_BASE + row;
    }

    public final WorkbenchBlockEntity be;
    private final Level level;
    private final SimpleContainer fuelFillContainer = new SimpleContainer(1);

    // Client-only rendering concern (which right-strip page is selected) -- not synced to the
    // server. Slot.x/y are final in this version, so hiding the Mod page's real slots (work/chain/
    // fuel-fill) while Fabrication is showing goes through Slot#isActive() instead of repositioning
    // them off-screen; see WorkbenchScreen#setPage.
    private boolean modPageActive = true;

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
        this.addSlot(new SunderChainSlot(be.WORK_ITEM, CHAIN_SLOT_X + 1, CHAIN_SLOT_Y + 1, () -> modPageActive));
        this.addSlot(new SunderFuelFillSlot(be.WORK_ITEM, fuelFillContainer, FUEL_FILL_SLOT_X + 1, FUEL_FILL_SLOT_Y + 1, () -> modPageActive));
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.WORKBENCH, be);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (level.isClientSide) return true;
        if (id == BUTTON_SCROLL_UP) {
            be.changeScrollRow(-1);
        } else if (id == BUTTON_SCROLL_DOWN) {
            be.changeScrollRow(1);
        } else if (id == BUTTON_FILL_FROM_POOL) {
            // TODO: no shared Gear Station fluid pool exists yet (Floor-network multiblock, not
            // built). Wired up client-side (button renders, clicks send this id) so the Fabrication
            // page's own future pool draws can follow the same pattern -- currently a no-op.
        } else if (id >= BUTTON_SET_ROW_BASE) {
            be.setScrollRow(id - BUTTON_SET_ROW_BASE);
        } else {
            return false;
        }
        return true;
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

    /** Reads straight off the current work item's own stack -- fully known client-side via the
     * normal slot-sync path, no extra menu-level sync needed (mirrors ScrenchMenu's own getter). */
    public FluidStack getSunderFluid() {
        return getWorkItemStack().getOrDefault(ModDataComponentTypes.FLUID_DATA.get(), FluidData.EMPTY).getFluidStack();
    }

    public int getSunderFuelCapacity() {
        return SunderItem.FUEL_CAPACITY;
    }
}
