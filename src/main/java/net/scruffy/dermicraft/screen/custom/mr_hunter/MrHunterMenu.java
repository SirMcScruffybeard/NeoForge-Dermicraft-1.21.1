package net.scruffy.dermicraft.screen.custom.mr_hunter;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scruffy.dermicraft.block.entity.custom.MrHunterBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.base.AbstractFarmBlockEntity;
import net.scruffy.dermicraft.screen.AbstractFarmMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

/** Mr. Hunter's menu: the shared farm menu's Home, Field and Module tabs, with the XP container on Home (no input row yet). */
public class MrHunterMenu extends AbstractFarmMenu<MrHunterBlockEntity> {

    /** Backdrop position of the XP container, above the fuel one. */
    public static final int XP_SLOT_X = 7;
    public static final int XP_SLOT_Y = 24;

    public MrHunterMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), extraData.readVarInt());
    }

    /** Opens on the tab the block asked for (the Module tab after a Module is right-clicked). */
    public MrHunterMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        this(containerId, inv, blockEntity, ((AbstractFarmBlockEntity) blockEntity).consumeRequestedTab());
    }

    public MrHunterMenu(int containerId, Inventory inv, BlockEntity blockEntity, int initialTab) {
        super(ModMenuTypes.MR_HUNTER_MENU.get(), containerId, inv, blockEntity,
                ((MrHunterBlockEntity) blockEntity).getInventorySize(), initialTab);

        addFuelSlot();
        addBufferSlots();
        addHomeSlot(BE.getXpTank().SLOT, XP_SLOT_X, XP_SLOT_Y);
        addInputSlots(); // none yet (the input row has no slots), kept so the extras pass can add them without touching this
        addModuleTabSlots();
    }
}
