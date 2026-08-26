package net.scruffy.dermicraft.screen.custom.charred_masticator;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.CharredMasticatorBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.MasticatorBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

/** Charred Masticator's menu -- identical layout/slots to {@code MasticatorMenu} (Module tab
 * included), just typed to {@link CharredMasticatorBlockEntity} and checked against
 * {@link ModBlocks#CHARRED_MASTICATOR} in {@link #stillValid}. A distinct class rather than reusing
 * MasticatorMenu because stillValid needs to match the actual block at this position -- same split
 * the Drooling family uses for Cauldron/Crucible. */
public class CharredMasticatorMenu extends AbstractModMenu {

    public static final int MAIN_TAB = 0;
    public static final int MODULE_TAB = 1;

    // Matches Drooling Cauldron's/base Masticator's own Module slot position -- one consistent
    // mod-wide GUI convention.
    public static final int MODULE_SLOT_X = 79;
    public static final int MODULE_SLOT_Y = 34;
    public static final int MODULE_SLOT_SPACING = 20;

    public final CharredMasticatorBlockEntity BE;
    private Level level;

    public CharredMasticatorMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public CharredMasticatorMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.CHARRED_MASTICATOR_MENU.get(), containerId,
                MasticatorBlockEntity.INVENTORY_SIZE + ((CharredMasticatorBlockEntity) blockEntity).moduleSlotCount());
        this.BE = ((CharredMasticatorBlockEntity) blockEntity);
        checkContainerSize(inv, 2);
        this.level = inv.player.level();

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), BE.getFuelTank().SLOT, 151, 60) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MAIN_TAB;
            }
        });
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), BE.getIngredientTank().SLOT, 67, 60) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MAIN_TAB;
            }
        });
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), BE.getResultTank().SLOT, 123, 60) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MAIN_TAB;
            }
        });
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), MasticatorBlockEntity.INGREDIENT_ITEM_SLOT, 38, 35) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MAIN_TAB;
            }
        });
        for (int i = 0; i < BE.moduleSlotCount(); i++) {
            this.addSlot(new SlotItemHandler(BE.MODULE_INVENTORY, i,
                    MODULE_SLOT_X + 1 + i * MODULE_SLOT_SPACING, MODULE_SLOT_Y + 1) {
                @Override
                public boolean isActive() {
                    return getActiveTab() == MODULE_TAB;
                }
            });
        }

        setQuickMoveInputSlots(3, 1); // INGREDIENT_ITEM_SLOT only -- skip fuel/ingredient/result tank slots and Module

        setActiveTab(BE.isModuleTabActive() ? MODULE_TAB : MAIN_TAB);
    }

    @Override
    protected void onTabChanged(int index) {
        BE.setModuleTabActive(index == MODULE_TAB);
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.CHARRED_MASTICATOR, BE);
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
