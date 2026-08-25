package net.scruffy.dermicraft.screen.custom.charred_craw;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.CharredCrawBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.CrawBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

/** Charred Craw's menu -- identical layout/slots to {@code CrawMenu} (Module tab included), just
 * typed to {@link CharredCrawBlockEntity} and checked against {@link ModBlocks#CHARRED_CRAW} in
 * {@link #stillValid}. A distinct class rather than reusing CrawMenu because stillValid needs to
 * match the actual block at this position -- same split the Drooling family/Charred Masticator use. */
public class CharredCrawMenu extends AbstractModMenu {

    public static final int MAIN_TAB = 0;
    public static final int MODULE_TAB = 1;

    public static final int MODULE_SLOT_X = 79;
    public static final int MODULE_SLOT_Y = 34;

    public final CharredCrawBlockEntity be;
    private final Level level;

    public CharredCrawMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public CharredCrawMenu(int containerId, Inventory inventory, BlockEntity blockEntity) {
        super(ModMenuTypes.CHARRED_CRAW_MENU.get(), containerId, 3);

        this.be = (CharredCrawBlockEntity) blockEntity;
        this.level = inventory.player.level();

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);

        this.addSlot(new SlotItemHandler(be.INPUT, CrawBlockEntity.INPUT_SLOT, 51, 35) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MAIN_TAB;
            }
        });
        this.addSlot(new SlotItemHandler(be.INVENTORY, CrawBlockEntity.STORAGE_SLOT, 110, 35) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MAIN_TAB;
            }
        });
        this.addSlot(new SlotItemHandler(be.MODULE, CrawBlockEntity.MODULE_SLOT, MODULE_SLOT_X + 1, MODULE_SLOT_Y + 1) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MODULE_TAB;
            }
        });

        setActiveTab(be.isModuleTabActive() ? MODULE_TAB : MAIN_TAB);
    }

    @Override
    protected void onTabChanged(int index) {
        be.setModuleTabActive(index == MODULE_TAB);
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.CHARRED_CRAW, be);
    }

    public int getStoredCount() {
        return be.getStoredCount();
    }

    public boolean isAutoPushEnabled() {
        return be.isAutoPushEnabled();
    }
}
