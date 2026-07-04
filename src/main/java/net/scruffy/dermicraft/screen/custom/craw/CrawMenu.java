package net.scruffy.dermicraft.screen.custom.craw;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.CrawBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class CrawMenu extends AbstractModMenu {

    public final CrawBlockEntity be;
    private final Level level;

    public CrawMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public CrawMenu(int containerId, Inventory inventory, BlockEntity blockEntity) {
        super(ModMenuTypes.CRAW_MENU.get(), containerId, 2);

        this.be = (CrawBlockEntity) blockEntity;
        this.level = inventory.player.level();

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);

        this.addSlot(new SlotItemHandler(be.INPUT, CrawBlockEntity.INPUT_SLOT, 51, 35));
        this.addSlot(new SlotItemHandler(be.INVENTORY, CrawBlockEntity.STORAGE_SLOT, 110, 35));
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.CRAW, be);
    }

    public int getStoredCount() {
        return be.getStoredCount();
    }
}
