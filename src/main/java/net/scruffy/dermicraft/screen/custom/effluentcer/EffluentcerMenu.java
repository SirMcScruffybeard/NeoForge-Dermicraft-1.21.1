package net.scruffy.dermicraft.screen.custom.effluentcer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.EffluentcerBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

public class EffluentcerMenu extends AbstractModMenu {

    // Same tab pattern as every other Module-tab machine.
    public static final int MAIN_TAB = 0;
    public static final int MODULE_TAB = 1;

    // Matches every other machine's own Module slot position -- one consistent mod-wide GUI
    // convention.
    public static final int MODULE_SLOT_X = 79;
    public static final int MODULE_SLOT_Y = 34;

    public final EffluentcerBlockEntity BE;
    private Level level;

    public EffluentcerMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public EffluentcerMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.EFFLUENTCER_MENU.get(), containerId, 5);
        checkContainerSize(inv, 2);
        this.BE = ((EffluentcerBlockEntity) blockEntity);
        this.level = inv.player.level();

        this.BE.setInteractingPlayer(inv.player);

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        // Coordinates follow the composited "inline pair" screen layout (see the machine
        // notes): HP far-left, fuel far-right, input A+B together left of center, result
        // beyond the arrow -- slot x tracks each tank column's blit x (+1), slot y fixed at 60.
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

        // Every Main-tab slot here is a fluid-container passthrough (fuel/inputA/inputB/result) --
        // there's no real solid item input, so shift-click from the player's inventory has nothing
        // to target and just no-ops rather than dumping into a tank slot.
        setQuickMoveInputSlots(0, 0);

        setActiveTab(BE.isModuleTabActive() ? MODULE_TAB : MAIN_TAB);
    }

    @Override
    protected void onTabChanged(int index) {
        BE.setModuleTabActive(index == MODULE_TAB);
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, ModBlocks.EFFLUENTCER, BE);
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
