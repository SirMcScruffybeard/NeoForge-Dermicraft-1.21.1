package net.scruffy.dermicraft.screen;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.scruffy.dermicraft.block.entity.custom.AbstractFueledMachineBlockEntity;
import net.scruffy.dermicraft.interfaces.IModuleTabMachine;

/**
 * The menu shared by every machine that has moved onto the evolution engine: two tabs (Main and Module), the general
 * Module slots and the dedicated Evolution slot on the Module tab, and the readouts the screen draws (progress, HP,
 * auto-drain). A machine's menu adds its own Main-tab slots with {@link #addMainSlot}, then calls
 * {@link #addModuleTabSlots()}, sets its quick-move range and calls {@link #restoreActiveTab()}, in that order, so
 * slot indices stay: player inventory, the machine's Main-tab slots, Module slots, the Evolution slot last.
 *
 * @param <B> the machine's block entity
 */
public abstract class AbstractEvolvingMachineMenu<B extends AbstractFueledMachineBlockEntity<?> & IModuleTabMachine>
        extends AbstractModMenu {

    public static final int MAIN_TAB = 0;
    public static final int MODULE_TAB = 1;

    // Every machine's Module tab uses the same screen coordinates, one consistent mod-wide GUI convention.
    public static final int MODULE_SLOT_X = 79;
    public static final int MODULE_SLOT_Y = 34;
    public static final int MODULE_SLOT_SPACING = 20;

    /** The Evolution slot sits right of the health bar (which takes the left edge, x 8 to 26, on both tabs). */
    public static final int EVOLUTION_SLOT_X = 30;

    public final B BE;
    private final Level level;
    private final DeferredBlock<Block> block;

    /**
     * @param mainSlotCount how many slots the machine's own Main tab adds (the base adds the Module slots and the
     *                      Evolution slot to the count)
     */
    @SuppressWarnings("unchecked")
    protected AbstractEvolvingMachineMenu(MenuType<?> type, int containerId, Inventory inv, BlockEntity blockEntity,
                                          int mainSlotCount, DeferredBlock<Block> block) {
        super(type, containerId, mainSlotCount + ((B) blockEntity).getModuleSlotCount() + 1);
        this.BE = (B) blockEntity;
        this.level = inv.player.level();
        this.block = block;
        checkContainerSize(inv, 2);

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    /** A Main-tab slot, active only while the Main tab is showing. */
    protected void addMainSlot(IItemHandler handler, int index, int x, int y) {
        this.addSlot(new SlotItemHandler(handler, index, x, y) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MAIN_TAB;
            }
        });
    }

    /** The general Module slots, then the Evolution slot last, both active only on the Module tab. */
    protected void addModuleTabSlots() {
        for (int i = 0; i < BE.getModuleSlotCount(); i++) {
            this.addSlot(new SlotItemHandler(BE.getModuleInventory(), i,
                    MODULE_SLOT_X + 1 + i * MODULE_SLOT_SPACING, MODULE_SLOT_Y + 1) {
                @Override
                public boolean isActive() {
                    return getActiveTab() == MODULE_TAB;
                }
            });
        }

        this.addSlot(new SlotItemHandler(BE.EVOLUTION_INVENTORY, 0,
                EVOLUTION_SLOT_X + 1, EvolutionPanel.SLOT_Y + 1) {
            @Override
            public boolean isActive() {
                return getActiveTab() == MODULE_TAB;
            }
        });
    }

    /** Opens on the tab last viewed. */
    protected void restoreActiveTab() {
        setActiveTab(BE.isModuleTabActive() ? MODULE_TAB : MAIN_TAB);
    }

    @Override
    protected void onTabChanged(int index) {
        BE.setModuleTabActive(index == MODULE_TAB);
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(level, player, block, BE);
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
