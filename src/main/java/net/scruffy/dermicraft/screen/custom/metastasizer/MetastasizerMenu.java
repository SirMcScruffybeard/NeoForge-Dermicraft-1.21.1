package net.scruffy.dermicraft.screen.custom.metastasizer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.MetastasizerBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class MetastasizerMenu extends AbstractModMenu {

    // Same tab pattern as Masticator/Drooling Cauldron.
    public static final int MAIN_TAB = 0;
    public static final int MODULE_TAB = 1;

    // Matches every other machine's Module slot position -- one consistent mod-wide GUI convention.
    public static final int MODULE_SLOT_X = 79;
    public static final int MODULE_SLOT_Y = 34;
    public static final int MODULE_SLOT_SPACING = 20;

    public final MetastasizerBlockEntity BE;
    private final Level level;

    public MetastasizerMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public MetastasizerMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.METASTASIZER_MENU.get(), containerId,
                MetastasizerBlockEntity.INVENTORY_SIZE + ((MetastasizerBlockEntity) blockEntity).moduleSlotCount());
        this.BE = (MetastasizerBlockEntity) blockEntity;
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
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), MetastasizerBlockEntity.PATTERN_SLOT, 61, 35) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MAIN_TAB;
            }
        });
        this.addSlot(new SlotItemHandler(this.BE.getItemHandler(null), MetastasizerBlockEntity.OUTPUT_SLOT, 121, 35) {
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

        setQuickMoveInputSlots(2, 1); // PATTERN_SLOT only -- skip fuel/reagent tank slots, OUTPUT_SLOT, and Module

        setActiveTab(BE.isModuleTabActive() ? MODULE_TAB : MAIN_TAB);
    }

    @Override
    protected void onTabChanged(int index) {
        BE.setModuleTabActive(index == MODULE_TAB);
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.METASTASIZER, BE);
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
