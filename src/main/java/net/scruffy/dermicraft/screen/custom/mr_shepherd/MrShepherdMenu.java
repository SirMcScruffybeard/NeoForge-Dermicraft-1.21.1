package net.scruffy.dermicraft.screen.custom.mr_shepherd;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scruffy.dermicraft.block.entity.custom.MrShepherdBlockEntity;
import net.scruffy.dermicraft.screen.AbstractFarmMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

/**
 * Mr. Shepherd's menu: the shared farm menu's Home and Field tabs, the feed row and the XP container on Home, and a
 * Population tab for the breeding-stock cap.
 */
public class MrShepherdMenu extends AbstractFarmMenu<MrShepherdBlockEntity> {

    public static final int POPULATION_TAB = FIRST_EXTRA_TAB;

    /** Backdrop position of the XP container, above the fuel one (the feed row is the shared input row). */
    public static final int XP_SLOT_X = 7;
    public static final int XP_SLOT_Y = 24;

    // clickMenuButton ids, mirrors vanilla's loom/enchant-table button convention.
    public static final int BUTTON_CAP_UP = 0;
    public static final int BUTTON_CAP_DOWN = 1;

    public MrShepherdMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public MrShepherdMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.MR_SHEPHERD_MENU.get(), containerId, inv, blockEntity,
                ((MrShepherdBlockEntity) blockEntity).getInventorySize());

        addFuelSlot();
        addBufferSlots();
        addHomeSlot(BE.getXpTank().SLOT, XP_SLOT_X, XP_SLOT_Y);
        addInputSlots();
        addModuleTabSlots();
    }

    @Override
    protected boolean clickExtraButton(int id) {
        switch (id) {
            case BUTTON_CAP_UP -> BE.changePopulationCap(1);
            case BUTTON_CAP_DOWN -> BE.changePopulationCap(-1);
            default -> {
                return false;
            }
        }
        return true;
    }

    public int getPopulationCap() {
        return BE.getPopulationCap();
    }

    public int getMaxPopulationCap() {
        return BE.getMaxPopulationCap();
    }
}
