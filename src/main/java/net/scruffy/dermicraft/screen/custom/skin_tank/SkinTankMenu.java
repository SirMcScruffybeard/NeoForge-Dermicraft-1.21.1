package net.scruffy.dermicraft.screen.custom.skin_tank;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.SkinTankBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;


public class SkinTankMenu extends AbstractModMenu {

    // Tab indices -- pilot for the shared tab-bar helper (dermicraft-progression-notes.md Decision
    // Point #2). Only two tabs, so no enum; a third machine adopting this pattern with more tabs is
    // the point at which an enum (mirroring WorkbenchScreen's own Page, but through the shared
    // mechanism instead of a bespoke one) becomes worth it over bare indices.
    public static final int MAIN_TAB = 0;
    public static final int MODULE_TAB = 1;

    // Module slot position -- public so SkinTankScreen can draw the matching background under it,
    // same convention as every other machine's screen/menu coordinate split (e.g. ScrenchMenu's own
    // chainSlotX()/chainSlotY()). Centered under the tank, roughly where INPUT/OUTPUT sit
    // vertically on the Main tab, since the Module tab has nothing else competing for space.
    public static final int MODULE_SLOT_X = 79;
    public static final int MODULE_SLOT_Y = 34;

    public final SkinTankBlockEntity be;
    private Level level;

    public SkinTankMenu(int containerId, Inventory inventory, FriendlyByteBuf extendData) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(extendData.readBlockPos()));
    }

    public SkinTankMenu(int containerId, Inventory inventory, BlockEntity blockEntity) {
        super(ModMenuTypes.SKIN_TANK_MENU.get(), containerId, SkinTankBlockEntity.INVENTORY_SIZE);

        checkContainerSize(inventory, SkinTankBlockEntity.INVENTORY_SIZE);
        this.be = (SkinTankBlockEntity) blockEntity;
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

        // Restores whichever tab was open last, same reasoning as
        // WorkbenchScreen#currentPage/WorkbenchBlockEntity#isFabricationPageActive -- correct from
        // the first frame rather than only after the screen's own init() runs.
        setActiveTab(be.isModuleTabActive() ? MODULE_TAB : MAIN_TAB);
    }

    @Override
    protected void onTabChanged(int index) {
        be.setModuleTabActive(index == MODULE_TAB);
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.SKIN_TANK, be);
    }
}
