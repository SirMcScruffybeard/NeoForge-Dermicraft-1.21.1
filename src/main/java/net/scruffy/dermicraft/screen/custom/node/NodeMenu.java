package net.scruffy.dermicraft.screen.custom.node;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.NodeBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class NodeMenu extends AbstractModMenu {

    public final NodeBlockEntity BE;
    private final Level level;

    public NodeMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public NodeMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.INNARDS_NODE_MENU.get(), containerId, 2);
        this.BE = (NodeBlockEntity) blockEntity;
        this.level = inv.player.level();

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        // Transport item buffer (automation-facing later) + GUI-only fluid-handler slot for the
        // manual fill/drain / jam-clear interaction. Positions tied to NodeScreen's background
        // art (buffer slot icon, tank gauge origin) -- keep in sync if those move.
        this.addSlot(new SlotItemHandler(BE.getItemHandler(null), NodeBlockEntity.BUFFER_SLOT, 131, 33));
        this.addSlot(new SlotItemHandler(BE.getItemHandler(null), NodeBlockEntity.FLUID_ITEM_SLOT, 153, 60));
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.INNARDS_NODE, BE);
    }
}
