package net.scruffy.dermicraft.screen.custom.grafting_table;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.GraftingTableBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

/**
 * The ghost/pattern click gesture lives here, layered thinly on top of vanilla's own slot
 * mechanics rather than replacing them -- see {@link #clicked}. Everything the gesture DOESN'T
 * need to special-case (extracting real stock, depositing once a ghost already accepts the item,
 * shift-click quick-move) falls straight through to {@code super.clicked}, which already respects
 * {@link GraftingTableBlockEntity}'s {@code isItemValid} gate (a slot with no ghost, or an item
 * that doesn't match/substitute for the ghost, is rejected by vanilla's own mayPlace check).
 */
public class GraftingTableMenu extends AbstractModMenu {

    // Grid origin/step matches GraftingTableScreen's GRID_X/GRID_Y/GRID_STEP exactly -- the two
    // must stay in lockstep since the menu places the real slots and the screen only draws art.
    private static final int GRID_X = 8;
    private static final int GRID_Y = 11;
    private static final int GRID_STEP = 18;

    public final GraftingTableBlockEntity BE;
    private final Level level;
    private final int gridSlotStart;

    public GraftingTableMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GraftingTableMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.GRAFTING_TABLE_MENU.get(), containerId, GraftingTableBlockEntity.GRID_SIZE + 2);
        checkContainerSize(inv, 2);
        this.BE = (GraftingTableBlockEntity) blockEntity;
        this.level = inv.player.level();

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), BE.getFuelTank().SLOT, 151, 60));

        // +1 down-right vs. the background art's GRID_X/GRID_Y (see GraftingTableScreen, which draws
        // the item_slot texture at the un-shifted position) -- the real item render and the vanilla
        // hover-highlight square both key off the Slot's own coordinates, not the background blit,
        // so this is the only place that needed the 1px correction. Ghost rendering already matched.
        this.gridSlotStart = this.slots.size();
        for (int row = 0; row < GraftingTableBlockEntity.GRID_HEIGHT; row++) {
            for (int col = 0; col < GraftingTableBlockEntity.GRID_WIDTH; col++) {
                int gridIndex = row * GraftingTableBlockEntity.GRID_WIDTH + col;
                this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), GraftingTableBlockEntity.GRID_START + gridIndex,
                        GRID_X + col * GRID_STEP + 1, GRID_Y + row * GRID_STEP + 1));
            }
        }

        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), GraftingTableBlockEntity.OUTPUT_SLOT, 101, 30));

        setQuickMoveInputSlots(1, GraftingTableBlockEntity.GRID_SIZE); // grid slots only -- skip fuel tank slot and OUTPUT_SLOT
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        int gridIndex = slotId - gridSlotStart;
        if (gridIndex >= 0 && gridIndex < GraftingTableBlockEntity.GRID_SIZE) {
            ItemStack ghost = BE.getGhost(gridIndex);
            ItemStack carried = getCarried();

            if (ghost.isEmpty()) {
                // Only a plain left-click with something on the cursor can establish a pattern --
                // and even then, the cursor stack is untouched (imprinting is free). Every other
                // combination (right-click, shift-click, empty hand) is a no-op on an un-imprinted
                // slot, matching "no imprint = nothing happens" for those click types.
                if (clickType == ClickType.PICKUP && button == 0 && !carried.isEmpty()) {
                    BE.setGhost(gridIndex, carried);
                }
                return;
            }

            if (carried.isEmpty() && BE.getRealStock(gridIndex).isEmpty()) {
                // Empty hand, zero real stock behind the ghost -- clears the pattern. A ghost can
                // only ever be cleared once its backing stock is fully depleted.
                BE.clearGhost(gridIndex);
                return;
            }

            // Ghost exists and there's either real stock to extract or a carried stack to try
            // depositing -- fall through to vanilla's own slot logic, which already consults
            // GraftingTableBlockEntity's isItemValid (ghost + flexible-match gate) before allowing
            // any deposit, and handles extraction/shift-click normally with no special-casing needed.
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.GRAFTING_TABLE, BE);
    }

    public boolean isCrafting() {
        return BE.isStillCrafting();
    }

    public int getScaledArrowProgress() {
        int arrowPixels = 15;
        return BE.getScaledProgress(arrowPixels);
    }

    public boolean isAutoDrainEnabled() {
        return BE.isAutoDrainEnabled();
    }
}
