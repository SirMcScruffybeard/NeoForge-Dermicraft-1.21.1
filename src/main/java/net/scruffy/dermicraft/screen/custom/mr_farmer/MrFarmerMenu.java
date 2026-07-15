package net.scruffy.dermicraft.screen.custom.mr_farmer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.MrFarmerBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class MrFarmerMenu extends AbstractModMenu {

    public final MrFarmerBlockEntity BE;
    private final Level level;

    private static final int BUFFER_SLOT_COUNT = 9;
    private static final int BUFFER_ROW_X = 8;
    private static final int BUFFER_ROW_Y = 65;
    private static final int FUEL_SLOT_X = 8;
    private static final int FUEL_SLOT_Y = 45;

    public MrFarmerMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public MrFarmerMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.MR_FARMER_MENU.get(), containerId, 1 + BUFFER_SLOT_COUNT);
        this.BE = (MrFarmerBlockEntity) blockEntity;
        this.level = inv.player.level();

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(BE.getItemHandler(null), BE.getFuelTank().SLOT, FUEL_SLOT_X, FUEL_SLOT_Y));

        for (int i = 0; i < BUFFER_SLOT_COUNT; i++) {
            this.addSlot(new SlotItemHandler(BE.getItemHandler(null), BE.getFuelTank().SLOT + 1 + i,
                    BUFFER_ROW_X + i * 18, BUFFER_ROW_Y));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.MR_FARMER, BE);
    }

    // Closing the GUI kicks off the ~30s range-preview particle visualization (server-side only).
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
}
