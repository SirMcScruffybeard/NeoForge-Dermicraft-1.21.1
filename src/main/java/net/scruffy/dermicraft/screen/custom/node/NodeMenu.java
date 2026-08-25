package net.scruffy.dermicraft.screen.custom.node;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.custom.duct.AbstractNodeBlock;
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
        // Can't use the shared single-Block stillValid helper here -- Tier 1 and Charred Node are
        // two different blocks sharing this menu, so a hardcoded ModBlocks.INNARDS_NODE identity
        // check would fail (and instant-close the screen) whenever opened on a Charred Node. Accept
        // any AbstractNodeBlock instead, replicating vanilla's own distance-gated logic.
        return ContainerLevelAccess.create(level, BE.getBlockPos()).evaluate(
                (lvl, pos) -> lvl.getBlockState(pos).getBlock() instanceof AbstractNodeBlock
                        && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0,
                true);
    }
}
