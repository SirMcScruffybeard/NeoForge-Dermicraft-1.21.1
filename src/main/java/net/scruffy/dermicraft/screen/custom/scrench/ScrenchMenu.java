package net.scruffy.dermicraft.screen.custom.scrench;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.HeldItemData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.effect.ModEffects;
import net.scruffy.dermicraft.item.custom.ScrenchItem;
import net.scruffy.dermicraft.item.custom.SunderChainItem;
import net.scruffy.dermicraft.item.custom.SunderItem;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

/**
 * Sunder's field maintenance screen, opened by the Scrench -- see the Scrench design notes in
 * {@code dermicraft-gadget-notes.md}. Chain slot (materialize/install round-trip), fuel
 * gauge/fill slot, and the completed-swap costs (movement penalty, Scrench durability wear) --
 * see {@link #applyCompletedSwapCosts}.
 *
 * <p>Not block-entity-backed like every other menu in the mod -- the first item-triggered one.
 * {@code sunderHand} is captured at open time and re-read fresh every {@link #stillValid} check
 * and on close, since the source of truth (the player's held Sunder) can change slot/hand or
 * disappear entirely while the screen is open.
 */
public class ScrenchMenu extends AbstractModMenu {

    private static final int CHAIN_SLOT_X = 45;
    private static final int CHAIN_SLOT_Y = 35;
    private static final int FUEL_TANK_X = 113;
    private static final int FUEL_TANK_Y = 12;
    private static final int FUEL_SLOT_X = FUEL_TANK_X;
    private static final int FUEL_SLOT_Y = 60;

    private final Player player;
    private final InteractionHand sunderHand;
    private final SimpleContainer chainContainer = new SimpleContainer(1);
    private final SimpleContainer fuelSlotContainer = new SimpleContainer(1);

    public ScrenchMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(containerId, inv, extraData.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    /**
     * Shared trigger, callable from either item's {@code use()} -- {@code ScrenchItem} (Scrench in
     * the acting hand, Sunder in the other) and {@code SunderItem} (Sunder in the acting hand,
     * Scrench in the other) both need to open the exact same menu, just with the hand roles
     * reversed. Extracted here specifically so that logic exists once, not duplicated per item --
     * see the hand-order fix in the Scrench design notes for why both directions need to work.
     */
    public static void open(ServerPlayer player, InteractionHand sunderHand) {
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.dermicraft.scrench");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player p) {
                return new ScrenchMenu(containerId, inv, sunderHand);
            }
        }, buf -> buf.writeBoolean(sunderHand == InteractionHand.OFF_HAND));
    }

    public ScrenchMenu(int containerId, Inventory inv, InteractionHand sunderHand) {
        super(ModMenuTypes.SCRENCH_MENU.get(), containerId, 2);
        this.player = inv.player;
        this.sunderHand = sunderHand;

        // Materialize the mounted chain and pull it out -- Sunder is chain-less for as long as this
        // screen has it. If the GUI closes with the slot still non-empty, removed() re-mounts it
        // (the same chain if untouched, a swapped-in spare otherwise) -- see the class javadoc.
        ItemStack sunder = player.getItemInHand(sunderHand);
        HeldItemData mounted = SunderItem.mountedChain(sunder);
        if (!mounted.isEmpty()) {
            chainContainer.setItem(0, mounted.itemStack().copy());
            SunderItem.clearMountedChain(sunder);
        }

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        addSlot(new Slot(chainContainer, 0, CHAIN_SLOT_X + 1, CHAIN_SLOT_Y + 1) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof SunderChainItem;
            }
        });

        addSlot(new FuelFillSlot());
    }

    /** Coordinates the screen needs for the fuel tank+slot pairing and the chain slot -- kept here
     * rather than duplicated as separate constants in the screen. */
    public static int chainSlotX() { return CHAIN_SLOT_X; }
    public static int chainSlotY() { return CHAIN_SLOT_Y; }
    public static int fuelTankX() { return FUEL_TANK_X; }
    public static int fuelTankY() { return FUEL_TANK_Y; }

    /** Reads straight off the player's currently-held Sunder stack -- the local player's own
     * inventory is always fully known client-side, no menu-level sync needed for this. */
    public FluidStack getSunderFluid() {
        return player.getItemInHand(sunderHand).getOrDefault(ModDataComponentTypes.FLUID_DATA.get(), FluidData.EMPTY).getFluidStack();
    }

    public int getSunderFuelCapacity() {
        return SunderItem.FUEL_CAPACITY;
    }

    @Override
    public boolean stillValid(Player p) {
        if (p != player) return false;
        ItemStack sunder = player.getItemInHand(sunderHand);
        if (!(sunder.getItem() instanceof SunderItem)) return false;

        return player.getMainHandItem().getItem() instanceof ScrenchItem
                || player.getOffhandItem().getItem() instanceof ScrenchItem;
    }

    /** Placeholder -- exact magnitude/duration are open design questions (see the Scrench design
     * notes' Open questions). 5 seconds, matching the mod's other short debuff-style durations. */
    private static final int SWAP_PENALTY_DURATION_TICKS = 100;

    /**
     * Completed-swap detection: non-empty chain slot at close = install it, full stop, no
     * distinction for "the same chain the player never touched" -- matches what was actually
     * decided (only "empty" and "cursor-held" are called out as the no-penalty/drop cases).
     * Cursor-held-item-drops-on-close is already vanilla's own default {@code
     * AbstractContainerMenu} behavior, nothing to add for that case.
     */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide) return;

        ItemStack chain = chainContainer.getItem(0);
        if (!chain.isEmpty() && chain.getItem() instanceof SunderChainItem) {
            ItemStack sunder = player.getItemInHand(sunderHand);
            if (sunder.getItem() instanceof SunderItem) {
                SunderItem.setMountedChain(sunder, chain);
                applyCompletedSwapCosts(player);
            }
        }
        chainContainer.setItem(0, ItemStack.EMPTY);
    }

    /** Movement penalty (see {@link net.scruffy.dermicraft.effect.ScrenchOffBalanceEffect} for why
     * it's a raw attribute modifier, not vanilla Slowness) plus 1 point of Scrench durability wear
     * -- both only on a completed chain swap, per the design notes (a refuel-only session costs
     * nothing). Checks both hands for the Scrench the same way {@link #stillValid} does, since this
     * menu only tracks {@code sunderHand}, not which hand holds the Scrench that opened it. */
    private void applyCompletedSwapCosts(Player player) {
        player.addEffect(new MobEffectInstance(ModEffects.SCRENCH_OFF_BALANCE,
                SWAP_PENALTY_DURATION_TICKS, 0, false, false, false));

        if (player.getMainHandItem().getItem() instanceof ScrenchItem) {
            player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        } else if (player.getOffhandItem().getItem() instanceof ScrenchItem) {
            player.getOffhandItem().hurtAndBreak(1, player, EquipmentSlot.OFFHAND);
        }
    }

    /**
     * Drains a filled fluid container immediately into Sunder's own fuel tank on contact -- same
     * "immediate, like everything else" rule as every other fluid transfer in the mod, reusing
     * {@code SippingItem.tryHandTransfer}'s {@code FluidUtil.tryFluidTransfer} pattern rather than
     * a new mechanism. Placed via {@code Slot#set} (fires on every insert) rather than requiring
     * the player to also click it a second time.
     */
    private class FuelFillSlot extends Slot {
        FuelFillSlot() {
            super(fuelSlotContainer, 0, FUEL_SLOT_X + 1, FUEL_SLOT_Y + 1);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getCapability(Capabilities.FluidHandler.ITEM, null) != null;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            ItemStack held = getItem();
            if (held.isEmpty()) return;

            ItemStack sunder = player.getItemInHand(sunderHand);
            IFluidHandlerItem sunderTank = sunder.getCapability(Capabilities.FluidHandler.ITEM, null);
            IFluidHandlerItem containerHandler = held.getCapability(Capabilities.FluidHandler.ITEM, null);
            if (sunderTank == null || containerHandler == null) return;

            if (FluidUtil.tryFluidTransfer(sunderTank, containerHandler, Integer.MAX_VALUE, true).isEmpty()) return;
            set(containerHandler.getContainer());
        }
    }
}
