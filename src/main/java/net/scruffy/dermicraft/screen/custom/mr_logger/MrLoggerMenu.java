package net.scruffy.dermicraft.screen.custom.mr_logger;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scruffy.dermicraft.block.entity.custom.base.AbstractFarmBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.MrLoggerBlockEntity;
import net.scruffy.dermicraft.screen.AbstractFarmMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

/** Mr. Logger's menu: the shared farm menu's Home slots (fuel container, output buffer, sapling row) and tabs, plus the leaf toggle. */
public class MrLoggerMenu extends AbstractFarmMenu<MrLoggerBlockEntity> {

    /** The Home tab's leaf toggle button, below the tab buttons' base id like every other button. */
    public static final int BUTTON_LEAVES = 0;

    /** The growth toggle button. */
    public static final int BUTTON_GROWTH = 1;

    /** Where the toggles sit on Home (their top-left), above the fuel row: the leaf toggle, then the growth toggle beside it. */
    public static final int LEAF_TOGGLE_X = 7;
    public static final int LEAF_TOGGLE_Y = 24;
    public static final int GROWTH_TOGGLE_X = 27;
    public static final int GROWTH_TOGGLE_Y = 24;

    public MrLoggerMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), extraData.readVarInt());
    }

    /** Opens on the tab the block asked for (the Module tab after a Module is right-clicked). */
    public MrLoggerMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        this(containerId, inv, blockEntity, ((AbstractFarmBlockEntity) blockEntity).consumeRequestedTab());
    }

    public MrLoggerMenu(int containerId, Inventory inv, BlockEntity blockEntity, int initialTab) {
        super(ModMenuTypes.MR_LOGGER_MENU.get(), containerId, inv, blockEntity,
                ((MrLoggerBlockEntity) blockEntity).getInventorySize(), initialTab);

        addFuelSlot();
        addBufferSlots();
        addInputSlots();
        addModuleTabSlots();
    }

    @Override
    protected boolean clickExtraButton(int id) {
        switch (id) {
            case BUTTON_LEAVES -> BE.toggleHarvestLeaves();
            case BUTTON_GROWTH -> BE.toggleAccelerateGrowth();
            default -> {
                return false;
            }
        }
        return true;
    }

    public boolean isAcceleratingGrowth() {
        return BE.isAcceleratingGrowth();
    }

    public boolean isHarvestingLeaves() {
        return BE.isHarvestingLeaves();
    }
}
