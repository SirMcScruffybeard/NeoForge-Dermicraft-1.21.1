package net.scruffy.dermicraft.screen.custom.node;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.block.custom.duct.AbstractNodeBlock;
import net.scruffy.dermicraft.block.entity.custom.NodeBlockEntity;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

/**
 * Framework menu for the tabbed Node GUI rework (see project_node_gui_tab_overhaul memory).
 * Wires the buffer item slot, tank gauge, fill/drain slot, and the six-tab bar to the BE, plus the
 * per-leg item/fluid filter ghost slots (see {@link #clicked}) -- one real Slot per leg per type
 * (12 total), all sharing the same on-screen position and gated by {@code isActive()} so only the
 * currently selected leg's pair is ever visible/clickable, same pattern as WorkbenchMenu's
 * Mod/Fabrication page slots. Per-leg enable/direction/mode/NBT toggle BUTTONS are wired by
 * TabbedNodeScreen directly (existing click-payload pattern); only the filter slots' click gesture
 * needs a menu-level override since it isn't a normal deposit/withdraw.
 */
public class TabbedNodeMenu extends AbstractModMenu {

    public final NodeBlockEntity BE;
    private final Level level;
    private final int itemFilterSlotStart;
    private final int fluidFilterSlotStart;

    // The fluid filter slot never actually holds an item -- its identity lives on the BE as a plain
    // Fluid (see BE#getFluidFilter/setFluidFilter) and it renders as a fluid swatch, not a ghost item
    // (see TabbedNodeScreen). This backing handler exists only so the slot can be a real Slot (hover
    // highlight, click routing); it's always empty and never persisted, so it doesn't need to live on
    // the BE or be kept in sync with anything.
    private final ItemStackHandler fluidFilterSlotBacking = new ItemStackHandler(NodeBlockEntity.LEG_ORDER.length);

    public TabbedNodeMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public TabbedNodeMenu(int containerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.TABBED_INNARDS_NODE_MENU.get(), containerId, 2);
        this.BE = (NodeBlockEntity) blockEntity;
        this.level = inv.player.level();

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        // Positions tied to TabbedNodeScreen's side-column layout -- keep in sync if those move.
        // Item slot backdrop texture sits at (182, 28); the logical slot is nudged +1, +1 from that
        // (183, 29) so the item icon centers correctly in the slot art. Fluid slot offset (+1, +49)
        // from the gauge origin at (182, 50), matching tank_and_slot.png's own internal slot placement.
        this.addSlot(new SlotItemHandler(BE.getItemHandler(null), NodeBlockEntity.BUFFER_SLOT, 183, 29));
        this.addSlot(new SlotItemHandler(BE.getItemHandler(null), NodeBlockEntity.FLUID_ITEM_SLOT, 183, 99));

        // Per-leg filter slots -- position matches TabbedNodeScreen's FILTER_COL_X/ITEMS_ROW_Y and
        // FLUID_ROW_Y (+1, +1 nudge, same convention as the buffer/fluid slots above). All six
        // per-leg slots of a type share that one position; isActive() ensures only the selected
        // leg's slot ever renders/hovers/receives clicks, so there's no visual collision.
        itemFilterSlotStart = this.slots.size();
        for (int i = 0; i < NodeBlockEntity.LEG_ORDER.length; i++) {
            int legIndex = i;
            this.addSlot(new SlotItemHandler(BE.getItemFilters(), legIndex, 53, 21) {
                @Override
                public boolean isActive() {
                    return getEffectiveLeg() == legIndex;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }

        fluidFilterSlotStart = this.slots.size();
        for (int i = 0; i < NodeBlockEntity.LEG_ORDER.length; i++) {
            int legIndex = i;
            this.addSlot(new SlotItemHandler(fluidFilterSlotBacking, legIndex, 53, 43) {
                @Override
                public boolean isActive() {
                    return getEffectiveLeg() == legIndex;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }

        setActiveTab(BE.getActiveTab());
    }

    /** The leg the filter slots are actually active for -- the BE's persisted active tab if it's
     * currently connected, otherwise the first connected leg, matching TabbedNodeScreen's own
     * fallback so the slot a player can click always lines up with what the screen shows is
     * selected. -1 if the Node has no connections at all (every filter slot then reports inactive). */
    public int getEffectiveLeg() {
        if (BE.isConnected(NodeBlockEntity.LEG_ORDER[getActiveTab()])) return getActiveTab();
        for (int i = 0; i < NodeBlockEntity.LEG_ORDER.length; i++) {
            if (BE.isConnected(NodeBlockEntity.LEG_ORDER[i])) return i;
        }
        return -1;
    }

    /**
     * Filter slots are ghosts, not real storage -- a click always either sets the filter to
     * whatever's carried (leaving the cursor untouched, player keeps their item) or clears it (empty
     * cursor). Every other slot falls through to vanilla's normal click handling unchanged.
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        int itemLeg = slotId - itemFilterSlotStart;
        if (itemLeg >= 0 && itemLeg < NodeBlockEntity.LEG_ORDER.length && slots.get(slotId).isActive()) {
            BE.setItemFilter(NodeBlockEntity.LEG_ORDER[itemLeg], getCarried());
            return;
        }

        int fluidLeg = slotId - fluidFilterSlotStart;
        if (fluidLeg >= 0 && fluidLeg < NodeBlockEntity.LEG_ORDER.length && slots.get(slotId).isActive()) {
            Direction dir = NodeBlockEntity.LEG_ORDER[fluidLeg];
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                BE.setFluidFilter(dir, Fluids.EMPTY);
            } else {
                IFluidHandlerItem handler = carried.getCapability(Capabilities.FluidHandler.ITEM, null);
                if (handler != null) {
                    FluidStack content = handler.getFluidInTank(0);
                    if (!content.isEmpty()) BE.setFluidFilter(dir, content.getFluid());
                }
            }
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    protected void onTabChanged(int index) {
        BE.setActiveTab(index);
    }

    @Override
    public boolean stillValid(Player player) {
        return ContainerLevelAccess.create(level, BE.getBlockPos()).evaluate(
                (lvl, pos) -> lvl.getBlockState(pos).getBlock() instanceof AbstractNodeBlock
                        && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0,
                true);
    }
}
