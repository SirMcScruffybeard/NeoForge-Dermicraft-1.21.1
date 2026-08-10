package net.scruffy.dermicraft.screen.custom.scrench;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.interfaces.IWorkbenchSwappable;
import net.scruffy.dermicraft.item.custom.ScrenchItem;
import net.scruffy.dermicraft.item.custom.SunderItem;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

/**
 * Generic field maintenance screen, opened by the Scrench -- see the Scrench design notes in
 * {@code dermicraft-gadget-notes.md}. Generalized 2026-08-09: this menu no longer knows anything
 * gadget-specific itself -- whichever {@link IWorkbenchSwappable} gadget is held hands back its own
 * {@link IWorkbenchSwappable.SwapPanel} (see that interface), and this menu just adds whatever slots
 * come back and calls {@code onClosed} when it closes. Sunder is the first implementation (its own
 * chain-swap + fuel-fill panel lives on {@code SunderItem} now, not here); a second Scrench-eligible
 * gadget needs no changes to this class at all.
 *
 * <p>Not block-entity-backed like every other menu in the mod -- the first item-triggered one.
 * {@code gadgetHand} is captured at open time and re-read fresh every {@link #stillValid} check,
 * since the source of truth (the player's held gadget) can change slot/hand or disappear entirely
 * while the screen is open.
 */
public class ScrenchMenu extends AbstractModMenu {

    private final Player player;
    private final InteractionHand gadgetHand;
    private final IWorkbenchSwappable.SwapPanel panel;

    public ScrenchMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(containerId, inv, extraData.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    /**
     * Shared trigger, callable from any Scrench-eligible gadget's own {@code use()} and from
     * {@code ScrenchItem} itself -- both directions need to open the exact same menu, just with the
     * hand roles reversed. Extracted here specifically so that logic exists once, not duplicated per
     * gadget -- see the hand-order fix in the Scrench design notes for why both directions need to
     * work.
     */
    public static void open(ServerPlayer player, InteractionHand gadgetHand) {
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.dermicraft.scrench");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player p) {
                return new ScrenchMenu(containerId, inv, gadgetHand);
            }
        }, buf -> buf.writeBoolean(gadgetHand == InteractionHand.OFF_HAND));
    }

    public ScrenchMenu(int containerId, Inventory inv, InteractionHand gadgetHand) {
        super(ModMenuTypes.SCRENCH_MENU.get(), containerId, 2);
        this.player = inv.player;
        this.gadgetHand = gadgetHand;

        ItemStack gadget = player.getItemInHand(gadgetHand);
        this.panel = gadget.getItem() instanceof IWorkbenchSwappable swappable
                ? swappable.openSwapPanel(() -> player.getItemInHand(gadgetHand), player, true)
                : null;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        if (panel != null) {
            panel.slots(0, 0, () -> true).forEach(this::addSlot);
        }
    }

    /** Coordinates the screen needs for the fuel tank+slot pairing and the chain slot -- Sunder-only
     * convenience accessors, delegating straight to SunderItem's own public panel constants (the
     * source of truth for where SunderSwapPanel actually builds its slots) rather than duplicating
     * the numbers here. */
    public static int chainSlotX() { return SunderItem.CHAIN_SLOT_X; }
    public static int chainSlotY() { return SunderItem.CHAIN_SLOT_Y; }
    public static int fuelTankX() { return SunderItem.FUEL_TANK_X; }
    public static int fuelTankY() { return SunderItem.FUEL_TANK_Y; }

    /** The gadget currently held in {@code gadgetHand}, read fresh -- lets the screen decide which
     * gadget-specific chrome to draw (fuel gauge, Module slots, ...) without this menu needing to
     * know what any of that looks like. */
    public ItemStack getGadgetStack() {
        return player.getItemInHand(gadgetHand);
    }

    /** Reads straight off the player's currently-held gadget -- the local player's own inventory is
     * always fully known client-side, no menu-level sync needed for this. Sunder-only convenience,
     * same caveat as the coordinate accessors above. */
    public FluidStack getSunderFluid() {
        return player.getItemInHand(gadgetHand).getOrDefault(ModDataComponentTypes.FLUID_DATA.get(), FluidData.EMPTY).getFluidStack();
    }

    public int getSunderFuelCapacity() {
        return SunderItem.FUEL_CAPACITY;
    }

    @Override
    public boolean stillValid(Player p) {
        if (p != player) return false;
        if (!(player.getItemInHand(gadgetHand).getItem() instanceof IWorkbenchSwappable)) return false;

        return player.getMainHandItem().getItem() instanceof ScrenchItem
                || player.getOffhandItem().getItem() instanceof ScrenchItem;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide || panel == null) return;

        panel.onClosed(player);
    }
}
