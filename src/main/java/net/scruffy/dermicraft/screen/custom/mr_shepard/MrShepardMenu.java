package net.scruffy.dermicraft.screen.custom.mr_shepard;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.MrShepardBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class MrShepardMenu extends AbstractModMenu {

    public final MrShepardBlockEntity BE;
    private final Level level;

    private static final int FOOD_SLOT_COUNT = 4;
    private static final int FOOD_ROW_X = 98;
    private static final int FOOD_ROW_Y = 45;
    private static final int BUFFER_SLOT_COUNT = 9;
    private static final int BUFFER_ROW_X = 8;
    private static final int BUFFER_ROW_Y = 65;
    private static final int FUEL_SLOT_X = 8;
    private static final int FUEL_SLOT_Y = 45;

    // clickMenuButton ids, mirrors vanilla's loom/enchant-table button convention.
    public static final int BUTTON_CAP_UP = 0;
    public static final int BUTTON_CAP_DOWN = 1;

    public MrShepardMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public MrShepardMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.MR_SHEPARD_MENU.get(), containerId, 1 + FOOD_SLOT_COUNT + BUFFER_SLOT_COUNT);
        this.BE = (MrShepardBlockEntity) blockEntity;
        this.level = inv.player.level();

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(BE.getItemHandler(null), BE.getFuelTank().SLOT, FUEL_SLOT_X, FUEL_SLOT_Y));

        for (int i = 0; i < FOOD_SLOT_COUNT; i++) {
            this.addSlot(new SlotItemHandler(BE.getItemHandler(null), 1 + i, FOOD_ROW_X + i * 18, FOOD_ROW_Y));
        }

        for (int i = 0; i < BUFFER_SLOT_COUNT; i++) {
            this.addSlot(new SlotItemHandler(BE.getItemHandler(null), 1 + FOOD_SLOT_COUNT + i,
                    BUFFER_ROW_X + i * 18, BUFFER_ROW_Y));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.MR_SHEPARD, BE);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (level.isClientSide) return true;
        switch (id) {
            case BUTTON_CAP_UP -> BE.changePopulationCap(1);
            case BUTTON_CAP_DOWN -> BE.changePopulationCap(-1);
            default -> { return false; }
        }
        return true;
    }

    // Closing the GUI kicks off the ~30s footprint guide so the player can site their pen.
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            BE.startRangePreview();
        }
    }

    public int getRange() {
        return BE.getRange();
    }

    public int getPopulationCap() {
        return BE.getPopulationCap();
    }

    public int getMaxPopulationCap() {
        return BE.getMaxPopulationCap();
    }
}
