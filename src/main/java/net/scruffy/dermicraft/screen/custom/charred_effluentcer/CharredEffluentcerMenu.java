package net.scruffy.dermicraft.screen.custom.charred_effluentcer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.CharredEffluentcerBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.EffluentcerBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

/** Charred Effluentcer's menu -- identical layout/slots to {@code EffluentcerMenu} (Module tab
 * included), just typed to {@link CharredEffluentcerBlockEntity} and checked against
 * {@link ModBlocks#CHARRED_EFFLUENTCER} in {@link #stillValid}. A distinct class rather than reusing
 * EffluentcerMenu because stillValid needs to match the actual block at this position -- same split
 * the Drooling family/Charred Masticator use. */
public class CharredEffluentcerMenu extends AbstractModMenu {

    public static final int MAIN_TAB = 0;
    public static final int MODULE_TAB = 1;

    public static final int MODULE_SLOT_X = 79;
    public static final int MODULE_SLOT_Y = 34;

    public final CharredEffluentcerBlockEntity BE;
    private Level level;

    public CharredEffluentcerMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public CharredEffluentcerMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.CHARRED_EFFLUENTCER_MENU.get(), containerId, 5);
        checkContainerSize(inv, 2);
        this.BE = ((CharredEffluentcerBlockEntity) blockEntity);
        this.level = inv.player.level();

        this.BE.setInteractingPlayer(inv.player);

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), BE.getFuelTank().SLOT, 151, 60) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MAIN_TAB;
            }
        });
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), BE.getInputATank().SLOT, 41, 60) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MAIN_TAB;
            }
        });
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), BE.getInputBTank().SLOT, 61, 60) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MAIN_TAB;
            }
        });
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), BE.getResultTank().SLOT, 113, 60) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MAIN_TAB;
            }
        });
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), EffluentcerBlockEntity.MODULE, MODULE_SLOT_X + 1, MODULE_SLOT_Y + 1) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MODULE_TAB;
            }
        });

        setQuickMoveInputSlots(0, 0);

        setActiveTab(BE.isModuleTabActive() ? MODULE_TAB : MAIN_TAB);
    }

    @Override
    protected void onTabChanged(int index) {
        BE.setModuleTabActive(index == MODULE_TAB);
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.CHARRED_EFFLUENTCER, BE);
    }

    public boolean isCrafting() {
        return BE.isStillCrafting();
    }

    public int getScaledArrowProgress() {
        int arrowPixels = 15;
        return BE.getScaledProgress(arrowPixels);
    }

    public int getHealth() {
        return BE.getHealth();
    }

    public int getMaxHealth() {
        return BE.getMaxHealth();
    }

    public boolean isAutoDrainEnabled() {
        return BE.isAutoDrainEnabled();
    }
}
