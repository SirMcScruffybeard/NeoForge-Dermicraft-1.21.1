package net.scruffy.dermicraft.screen.custom.mr_logger;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scruffy.dermicraft.block.entity.custom.MrLoggerBlockEntity;
import net.scruffy.dermicraft.screen.AbstractFarmMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

/** Mr. Logger's menu: the shared farm menu's Home slots (fuel container, output buffer, sapling row) and tabs, plus the leaf toggle. */
public class MrLoggerMenu extends AbstractFarmMenu<MrLoggerBlockEntity> {

    /** The Home tab's leaf toggle button, below the tab buttons' base id like every other button. */
    public static final int BUTTON_LEAVES = 0;

    /** Where the toggle sits on Home (its top-left), above the fuel row. */
    public static final int LEAF_TOGGLE_X = 7;
    public static final int LEAF_TOGGLE_Y = 24;

    public MrLoggerMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public MrLoggerMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.MR_LOGGER_MENU.get(), containerId, inv, blockEntity,
                ((MrLoggerBlockEntity) blockEntity).getInventorySize());

        addFuelSlot();
        addBufferSlots();
        addInputSlots();
        addModuleTabSlots();
    }

    @Override
    protected boolean clickExtraButton(int id) {
        if (id != BUTTON_LEAVES) return false;
        BE.toggleBreakLeaves();
        return true;
    }

    public boolean isBreakingLeaves() {
        return BE.isBreakingLeaves();
    }
}
