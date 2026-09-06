package net.scruffy.dermicraft.screen.custom.aid;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.item.custom.AidItem;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

/**
 * A.I.D.'s own GUI -- crouch-right-click opens it (see {@code AidItem#use}). Not block-entity-backed,
 * same shape as {@code ScrenchMenu} (the mod's first item-triggered menu): {@code aidHand} is
 * captured at open time and re-read fresh every {@link #stillValid} check and every fluid/slot
 * access, since the source of truth (the player's held A.I.D.) can change slot/hand or disappear
 * entirely while the screen is open.
 *
 * <p>Two slots: a string-only item slot (Suture mode's ammo, backed by
 * {@link AidItem.StringSlotHandler}) and a bidirectional fill/drain slot for the Syringe tank gauge,
 * same {@code FluidUtil.tryFluidTransfer} mechanics {@code SippingItem}'s own {@code FillDrainSlot}
 * uses. Placing a filled container fills A.I.D.; placing an empty (or A.I.D.-full) one drains it.
 */
public class AidMenu extends AbstractModMenu {

    private static final int SLOT_COUNT = 2;
    public static final int STRING_SLOT_INDEX = 0;
    public static final int TANK_FILL_SLOT_INDEX = 1;

    // Slot coordinates live here, not on AidScreen: this menu is constructed server-side too, and
    // AidScreen extends a client-only AbstractContainerScreen -- referencing its statics from here
    // would drag a client-only class onto the dedicated server. AidScreen reads these instead.
    public static final int STRING_SLOT_X = 62;
    public static final int STRING_SLOT_Y = 20;

    // Bottom-anchored on the 66px-tall tank_and_slot texture, same 48px offset Sunder's own
    // FUEL_SLOT_Y uses relative to its tank's top.
    public static final int TANK_FILL_SLOT_X = 98;
    public static final int TANK_FILL_SLOT_Y = 56;

    private final Player player;
    private final InteractionHand aidHand;

    public AidMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(containerId, inv, extraData.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    /** Shared trigger -- mirrors {@code ScrenchMenu.open}. */
    public static void open(ServerPlayer player, InteractionHand aidHand) {
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.dermicraft.aid");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player p) {
                return new AidMenu(containerId, inv, aidHand);
            }
        }, buf -> buf.writeBoolean(aidHand == InteractionHand.OFF_HAND));
    }

    public AidMenu(int containerId, Inventory inv, InteractionHand aidHand) {
        super(ModMenuTypes.AID_MENU.get(), containerId, SLOT_COUNT);
        this.player = inv.player;
        this.aidHand = aidHand;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(
                new AidItem.StringSlotHandler(() -> player.getItemInHand(aidHand)),
                0, STRING_SLOT_X, STRING_SLOT_Y));
        this.addSlot(new TankFillDrainSlot(TANK_FILL_SLOT_X + 1, TANK_FILL_SLOT_Y + 1));

        setQuickMoveInputSlots(0, 1); // String slot only -- skip the fill/drain slot.
    }

    /** Reads straight off the player's currently-held A.I.D. -- the local player's own inventory is
     * always fully known client-side, no menu-level sync needed, same reasoning as ScrenchMenu's
     * per-gadget fluid accessors. */
    public FluidStack getFluid() {
        return player.getItemInHand(aidHand).getOrDefault(ModDataComponentTypes.FLUID_DATA.get(), FluidData.EMPTY).getFluidStack();
    }

    public ItemStack getAidStack() {
        return player.getItemInHand(aidHand);
    }

    @Override
    public boolean stillValid(Player p) {
        return p == player && player.getItemInHand(aidHand).getItem() instanceof AidItem;
    }

    /**
     * Bidirectional, same shape and same {@code FluidUtil.tryFluidTransfer} mechanics as
     * {@code SippingItem.FillDrainSlot}: drains A.I.D.'s own tank into the placed container first,
     * falling back to filling A.I.D. from it. All-or-nothing either way -- {@code tryFluidTransfer}
     * only executes when the destination's simulated fill reports it can take the whole amount.
     *
     * <p>{@code processed} is the actual fix for the toggle bug that took several rounds to pin
     * down: {@code Slot#setChanged} fires for reasons beyond a genuine player action (confirmed via
     * two separate diagnostic log captures showing it firing from ordinary client/server resync
     * echoes), and a container that's still sitting in the slot after a transfer is a perfectly
     * valid target in the OPPOSITE direction too -- so re-running the transfer on every fire toggled
     * the fluid back and forth indefinitely. Tracking "have I already transferred for whatever's
     * currently occupying this slot" (reset only when the slot goes genuinely empty) is robust
     * against that regardless of which thread or how many times setChanged fires, unlike comparing
     * item equality (which a mismatched client/server capability re-construction slipped past once
     * already).
     */
    private final class TankFillDrainSlot extends Slot {

        private boolean processed = false;

        TankFillDrainSlot(int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getCapability(Capabilities.FluidHandler.ITEM, null) != null;
        }

        @Override
        public void setChanged() {
            super.setChanged();

            ItemStack held = getItem();
            if (held.isEmpty()) {
                processed = false;
                return;
            }
            if (processed) return;

            IFluidHandlerItem containerHandler = held.getCapability(Capabilities.FluidHandler.ITEM, null);
            ItemStack aidStack = player.getItemInHand(aidHand);
            IFluidHandlerItem aidHandler = aidStack.getCapability(Capabilities.FluidHandler.ITEM, null);
            if (containerHandler == null || aidHandler == null) return;

            // Marked before transferring, not after: set() below re-invokes this method, and marking
            // first is what makes that re-entry (and any later resync echo) a no-op.
            processed = true;

            FluidStack moved = FluidUtil.tryFluidTransfer(containerHandler, aidHandler, Integer.MAX_VALUE, true);
            if (moved.isEmpty()) {
                moved = FluidUtil.tryFluidTransfer(aidHandler, containerHandler, Integer.MAX_VALUE, true);
            }
            if (moved.isEmpty()) return;

            set(containerHandler.getContainer());
        }
    }
}
