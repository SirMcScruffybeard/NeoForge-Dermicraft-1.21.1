package net.scruffy.dermicraft.screen.custom.drooling_crucible;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.DroolingCrucibleBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.DroolingMachineBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class DroolingCrucibleMenu extends AbstractModMenu {

    // Tab indices -- same shared tab-bar mechanism SkinTankMenu piloted (dermicraft-progression-notes.md
    // Decision Point #2). Only two tabs, so no enum -- see that class's own identical note.
    public static final int MAIN_TAB = 0;
    public static final int MODULE_TAB = 1;

    // Module slot position -- public so DroolingCrucibleScreen can draw the matching background,
    // same convention as every other machine's screen/menu coordinate split.
    public static final int MODULE_SLOT_X = 79;
    public static final int MODULE_SLOT_Y = 34;

    public final DroolingCrucibleBlockEntity BE;
    private final Level level;

    public DroolingCrucibleMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public DroolingCrucibleMenu(int pContainerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.DROOLING_CRUCIBLE_MENU.get(), pContainerId, DroolingMachineBlockEntity.INVENTORY_SIZE);
        checkContainerSize(inv, DroolingMachineBlockEntity.INVENTORY_SIZE);
        this.BE = ((DroolingCrucibleBlockEntity) blockEntity);
        this.level = inv.player.level();

        super.addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(this.BE.INVENTORY, 0, 43, 34) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MAIN_TAB;
            }
        });
        this.addSlot(new SlotItemHandler(this.BE.INVENTORY, 1, 117, 34) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean isActive() {
                return getActiveTab() == MAIN_TAB;
            }
        });
        this.addSlot(new SlotItemHandler(this.BE.INVENTORY, DroolingMachineBlockEntity.MODULE,
                MODULE_SLOT_X + 1, MODULE_SLOT_Y + 1) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MODULE_TAB;
            }
        });

        setQuickMoveInputSlots(0, 1); // INPUT only -- skip OUTPUT and the Module slot

        // Restores whichever tab was open last, same reasoning as SkinTankMenu's own identical line.
        setActiveTab(BE.isModuleTabActive() ? MODULE_TAB : MAIN_TAB);
    }

    @Override
    protected void onTabChanged(int index) {
        BE.setModuleTabActive(index == MODULE_TAB);
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

    public boolean isAutoDrainEnabled() {
        return BE.isAutoDrainEnabled();
    }
}
