package net.scruffy.dermicraft.screen;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.Nullable;

/********************************************************************
 * AbstractModMenu
 *  A base class for machine block menus.
 *  Contains methods for:
 *      quickMoveStack()
 *      addPlayerInventory()
 *      addPlayerHotbar()
 ********************************************************************/
public abstract class AbstractModMenu extends AbstractContainerMenu {

    private final int TE_INVENTORY_SLOT_COUNT;

    // BE-relative range (within this menu's own TE slots) that shift-click FROM the player's
    // inventory is allowed to target -- defaults to the full TE range (every existing menu's prior
    // behavior) unless a subclass narrows it via setQuickMoveInputSlots. Doesn't affect the reverse
    // direction: shift-clicking a BE slot back to the player still works from any BE slot.
    private int quickMoveInputStart;
    private int quickMoveInputCount;

    protected AbstractModMenu(@Nullable MenuType<?> menuType, int containerId, int invSlots) {
        super(menuType, containerId);
        TE_INVENTORY_SLOT_COUNT = invSlots;
        this.quickMoveInputStart = TE_INVENTORY_FIRST_SLOT_INDEX;
        this.quickMoveInputCount = invSlots;
    }

    /**
     * Narrows player-inventory shift-click to only the given BE-relative slots (0-based, within
     * this menu's own TE range) -- lets a machine's real item input slot(s) be targeted while
     * skipping fluid-container passthrough slots (fuel/reagent/result tank bucket slots), which
     * otherwise sit earlier in slot-registration order and would silently claim the click instead
     * (e.g. a shift-clicked bucket landing in the fuel tank rather than the intended input). Call
     * after registering this menu's TE slots. A count of 0 means no valid quick-move target exists
     * (e.g. an all-fluid machine like the Effluentcer) -- shift-click into the machine just no-ops.
     */
    protected void setQuickMoveInputSlots(int relativeStart, int count) {
        this.quickMoveInputStart = TE_INVENTORY_FIRST_SLOT_INDEX + relativeStart;
        this.quickMoveInputCount = count;
    }

    // Shared tab-bar state, generalized off WorkbenchScreen's own Page/tab-click handling per
    // dermicraft-progression-notes.md Decision Point #2 -- a machine opts into a Module tab (or any
    // other multi-page screen) by using this instead of re-deriving the Workbench's bespoke
    // mechanism per machine family. clickMenuButton IDs at or above TAB_BUTTON_BASE are reserved for
    // tab selection; a subclass's own buttons (Workbench's BUTTON_SET_MOD_PAGE=101, a Fill-from-pool
    // button, etc.) must stay below it to avoid collision -- 200 leaves comfortable headroom under
    // that ceiling for any menu with its own buttons.
    // Public, not protected: a screen (a different package, and not itself a subclass of this
    // class -- screens extend AbstractModScreen) needs this constant to build the
    // handleInventoryButtonClick id it sends on a tab click, same as WorkbenchMenu's own
    // BUTTON_SET_MOD_PAGE/BUTTON_SET_FABRICATION_PAGE constants are public for the identical reason.
    public static final int TAB_BUTTON_BASE = 200;

    private int activeTab = 0;

    public int getActiveTab() {
        return activeTab;
    }

    /** Selects a tab locally (for instant client-side feedback) -- does NOT itself send anything
     * over the network. The screen's click handler calls this immediately, then separately fires
     * {@code handleInventoryButtonClick} so the server (and this same call server-side, via
     * {@link #clickMenuButton}) agrees. Public, not protected, same reasoning as
     * {@link #TAB_BUTTON_BASE}: the screen calling it isn't a subclass of this menu. */
    public void setActiveTab(int index) {
        this.activeTab = index;
        onTabChanged(index);
    }

    /** Overridden by a subclass that persists the active tab somewhere durable (a block entity
     * field, the same way {@code WorkbenchBlockEntity#isFabricationPageActive} survives a reopen).
     * No-op by default -- plenty of tab bars are fine resetting to tab 0 every time the screen opens. */
    protected void onTabChanged(int index) {
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < TAB_BUTTON_BASE) return false;
        setActiveTab(id - TAB_BUTTON_BASE);
        return true;
    }

    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    // must assign a slot number to each of the slots used by the GUI.
    // For this container, we can see both the tile inventory's slots as well as the player inventory slots and the hotbar.
    // Each time we add a Slot to the container, it automatically increases the slotIndex, which means
    //  0 - 8 = hotbar slots (which will map to the InventoryPlayer slot numbers 0 - 8)
    //  9 - 35 = player inventory slots (which map to the InventoryPlayer slot numbers 9 - 35)
    //  36 - 44 = TileInventory slots, which map to our TileEntity slot numbers 0 - 8)
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    // THIS YOU HAVE TO DEFINE!
    // must be the number of slots you have!
    @Override
    public ItemStack quickMoveStack(Player playerIn, int pIndex) {
        Slot sourceSlot = slots.get(pIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;  //EMPTY_ITEM
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Check if the slot clicked is one of the vanilla container slots
        if (pIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory --
            // restricted to the real input slot(s) only, see setQuickMoveInputSlots.
            if (quickMoveInputCount <= 0
                    || !moveItemStackTo(sourceStack, quickMoveInputStart, quickMoveInputStart + quickMoveInputCount, false)) {
                return ItemStack.EMPTY;  // EMPTY_ITEM
            }
        } else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a TE slot so merge the stack into the players inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            System.out.println("Invalid slotIndex:" + pIndex);
            return ItemStack.EMPTY;
        }
        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public abstract boolean stillValid(Player player);

    protected boolean stillValid(Level level, Player player, DeferredBlock<Block> block, BlockEntity be) {
        return stillValid(ContainerLevelAccess.create(level, be.getBlockPos()), player, block.get());
    }

    protected void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    protected void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
