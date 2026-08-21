package net.scruffy.dermicraft.screen.custom.drooling_crucible;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.DroolingCrucibleBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class DroolingCrucibleMenu extends AbstractModMenu {

    public final DroolingCrucibleBlockEntity BE;
    private final Level level;

    public DroolingCrucibleMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public DroolingCrucibleMenu(int pContainerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.DROOLING_CRUCIBLE_MENU.get(), pContainerId, 2);
        checkContainerSize(inv, 2);
        this.BE = ((DroolingCrucibleBlockEntity) blockEntity);
        this.level = inv.player.level();

        super.addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(this.BE.INVENTORY, 0, 43, 34));
        this.addSlot(new SlotItemHandler(this.BE.INVENTORY, 1, 117, 34) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        setQuickMoveInputSlots(0, 1); // INPUT only -- skip OUTPUT
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.DROOLING_CRUCIBLE, BE);
    }

    public boolean isCrafting() {
        return BE.isStillCrafting();
    }

    public int getScaledArrowProgress() {
        int arrowPixels = 15;
        return BE.getScaledProgress(arrowPixels);
    }
}
