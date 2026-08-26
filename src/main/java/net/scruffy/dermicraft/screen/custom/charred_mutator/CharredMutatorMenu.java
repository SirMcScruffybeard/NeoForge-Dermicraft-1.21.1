package net.scruffy.dermicraft.screen.custom.charred_mutator;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.CharredMutatorBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.MutatorBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

/** Charred Mutator's menu -- identical layout/slots to {@code MutatorMenu} (Module tab included),
 * just typed to {@link CharredMutatorBlockEntity} and checked against
 * {@link ModBlocks#CHARRED_MUTATOR} in {@link #stillValid}. A distinct class rather than reusing
 * MutatorMenu because stillValid needs to match the actual block at this position -- same split the
 * Drooling family/Charred Masticator use. */
public class CharredMutatorMenu extends AbstractModMenu {

    public static final int MAIN_TAB = 0;
    public static final int MODULE_TAB = 1;

    public static final int MODULE_SLOT_X = 79;
    public static final int MODULE_SLOT_Y = 34;
    public static final int MODULE_SLOT_SPACING = 20;

    public final CharredMutatorBlockEntity BE;
    private final Level level;

    public CharredMutatorMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public CharredMutatorMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.CHARRED_MUTATOR_MENU.get(), containerId,
                MutatorBlockEntity.INVENTORY_SIZE + ((CharredMutatorBlockEntity) blockEntity).moduleSlotCount());
        this.BE = (CharredMutatorBlockEntity) blockEntity;
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
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), BE.getReagentTank().SLOT, 31, 60) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MAIN_TAB;
            }
        });
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), MutatorBlockEntity.INPUT_SLOT, 61, 35) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MAIN_TAB;
            }
        });
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), MutatorBlockEntity.OUTPUT_SLOT, 121, 35) {
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

        setQuickMoveInputSlots(2, 1);

        setActiveTab(BE.isModuleTabActive() ? MODULE_TAB : MAIN_TAB);
    }

    @Override
    protected void onTabChanged(int index) {
        BE.setModuleTabActive(index == MODULE_TAB);
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.CHARRED_MUTATOR, BE);
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

    public MutatorBlockEntity.Mode getMode() {
        return BE.getMode();
    }

    public boolean isAutoDrainEnabled() {
        return BE.isAutoDrainEnabled();
    }
}
