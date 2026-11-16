package net.scruffy.dermicraft.screen;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.entity.custom.base.AbstractFarmBlockEntity;

/**
 * The menu shared by the farm machines (Mr. Farmer, Mr. Shepherd and the ones that follow): two tabs, Home (the fuel
 * slot, the machine's own slots and the output buffer) and Field (the width, length and distance steppers), plus any tabs a machine
 * adds after them. Home slots are only active while Home is showing. The field buttons change the block entity on the
 * server, which clamps them; the screen reads the result back from its own copy of the block entity.
 *
 * <p>A machine's menu registers its Home slots in order (fuel, its own, then the buffer, as shift-click walks them in
 * that order), and handles its own extra buttons by overriding {@link #clickExtraButton}. Every button id here and in a
 * subclass must stay below {@link AbstractModMenu#TAB_BUTTON_BASE}, which tab clicks use.
 */
public abstract class AbstractFarmMenu<B extends AbstractFarmBlockEntity> extends AbstractModMenu {

    public static final int HOME_TAB = 0;
    public static final int FIELD_TAB = 1;

    public static final int BUTTON_WIDTH_UP = 10;
    public static final int BUTTON_WIDTH_DOWN = 11;
    public static final int BUTTON_LENGTH_UP = 12;
    public static final int BUTTON_LENGTH_DOWN = 13;
    public static final int BUTTON_DISTANCE_UP = 14;
    public static final int BUTTON_DISTANCE_DOWN = 15;

    private static final int PLAYER_SLOT_COUNT = 36;

    public final B BE;
    protected final Level level;

    @SuppressWarnings("unchecked")
    protected AbstractFarmMenu(MenuType<?> menuType, int containerId, Inventory inv, BlockEntity blockEntity, int blockEntitySlots) {
        super(menuType, containerId, blockEntitySlots);
        this.BE = (B) blockEntity;
        this.level = inv.player.level();

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    /** A Home-tab slot for inventory slot {@code index}, at a backdrop position (the slot sits one pixel in). */
    protected void addHomeSlot(int index, int backdropX, int backdropY) {
        this.addSlot(new SlotItemHandler(BE.getItemHandler(null), index, backdropX + 1, backdropY + 1) {
            @Override
            public boolean isActive() {
                return getActiveTab() == HOME_TAB;
            }
        });
    }

    protected void addFuelSlot() {
        addHomeSlot(BE.getFuelTank().SLOT, FarmLayout.FUEL_SLOT_X, FarmLayout.FUEL_SLOT_Y);
    }

    protected void addBufferSlots() {
        for (int i = 0; i < BE.getBufferSlotCount(); i++) {
            addHomeSlot(BE.getBufferStartSlot() + i, FarmLayout.BUFFER_ROW_X + i * FarmLayout.SLOT_SIZE, FarmLayout.BUFFER_ROW_Y);
        }
    }

    // An inactive slot is invisible and can't be clicked, but quickMoveStack ignores that, so shift-clicking from the
    // player's inventory while another tab shows would drop items into slots nobody can see.
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (getActiveTab() != HOME_TAB && index < PLAYER_SLOT_COUNT) return ItemStack.EMPTY;
        return super.quickMoveStack(player, index);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (super.clickMenuButton(player, id)) return true; // tab selection
        if (level.isClientSide) return true;
        switch (id) {
            case BUTTON_WIDTH_UP -> BE.stepFieldWidth(1);
            case BUTTON_WIDTH_DOWN -> BE.stepFieldWidth(-1);
            case BUTTON_LENGTH_UP -> BE.stepFieldLength(1);
            case BUTTON_LENGTH_DOWN -> BE.stepFieldLength(-1);
            case BUTTON_DISTANCE_UP -> BE.stepFieldDistance(1);
            case BUTTON_DISTANCE_DOWN -> BE.stepFieldDistance(-1);
            default -> {
                return clickExtraButton(id);
            }
        }
        return true;
    }

    /** A machine's own buttons (server side). Return whether the id was one of them. */
    protected boolean clickExtraButton(int id) {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, BE.getFarmBlock(), BE);
    }

    // Closing the GUI kicks off the ~30s range-preview visualization (server-side only).
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            BE.startRangePreview();
        }
    }
}
