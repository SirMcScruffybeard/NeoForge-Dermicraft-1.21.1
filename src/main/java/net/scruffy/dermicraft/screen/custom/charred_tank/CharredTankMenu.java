package net.scruffy.dermicraft.screen.custom.charred_tank;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.CharredTankBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.SkinTankBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

/** Charred Tank's menu -- identical layout/slots to {@code SkinTankMenu} (Module tab included), just
 * typed to {@link CharredTankBlockEntity} and checked against {@link ModBlocks#CHARRED_TANK} in
 * {@link #stillValid}. A distinct class rather than reusing SkinTankMenu because stillValid needs to
 * match the actual block at this position -- same split the Drooling family/Charred Masticator use. */
public class CharredTankMenu extends AbstractModMenu {

    public static final int MAIN_TAB = 0;
    public static final int MODULE_TAB = 1;

    // Matches Skin Tank's own Module slot position -- one consistent mod-wide GUI convention.
    public static final int MODULE_SLOT_X = 79;
    public static final int MODULE_SLOT_Y = 34;

    public final CharredTankBlockEntity be;
    private Level level;

    public CharredTankMenu(int containerId, Inventory inventory, FriendlyByteBuf extendData) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(extendData.readBlockPos()));
    }

    public CharredTankMenu(int containerId, Inventory inventory, BlockEntity blockEntity) {
        super(ModMenuTypes.CHARRED_TANK_MENU.get(), containerId, SkinTankBlockEntity.INVENTORY_SIZE);

        checkContainerSize(inventory, SkinTankBlockEntity.INVENTORY_SIZE);
        this.be = (CharredTankBlockEntity) blockEntity;
        this.level = inventory.player.level();

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);

        this.addSlot(new SlotItemHandler(be.INVENTORY, SkinTankBlockEntity.INPUT, 43, 34) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MAIN_TAB;
            }
        });
        this.addSlot(new SlotItemHandler(be.INVENTORY, SkinTankBlockEntity.OUTPUT, 117, 34) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean isActive() {
                return getActiveTab() == MAIN_TAB;
            }
        });
        this.addSlot(new SlotItemHandler(be.INVENTORY, SkinTankBlockEntity.MODULE,
                MODULE_SLOT_X + 1, MODULE_SLOT_Y + 1) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MODULE_TAB;
            }
        });

        setQuickMoveInputSlots(0, 1); // INPUT only -- skip OUTPUT and the Module slot

        setActiveTab(be.isModuleTabActive() ? MODULE_TAB : MAIN_TAB);
    }

    @Override
    protected void onTabChanged(int index) {
        be.setModuleTabActive(index == MODULE_TAB);
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.CHARRED_TANK, be);
    }
}
