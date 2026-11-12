package net.scruffy.dermicraft.screen.custom.upgrade_stick;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.EvolutionStateData;
import net.scruffy.dermicraft.hazard.HazardFamily;
import net.scruffy.dermicraft.item.custom.UpgradeStickItem;
import net.scruffy.dermicraft.machine.UpgradeStickRules;
import net.scruffy.dermicraft.screen.AbstractModMenu;
import net.scruffy.dermicraft.screen.ModMenuTypes;

/**
 * The Upgrade Stick's selection screen: no slots, only buttons. The selection is the stick's own
 * {@link EvolutionStateData}; the server changes it on a button click (through {@link UpgradeStickRules}) and the six
 * values are mirrored to the client as data slots, so the screen never depends on the held item having synced.
 */
public class UpgradeStickMenu extends AbstractModMenu {

    // Button ids, all below AbstractModMenu.TAB_BUTTON_BASE.
    public static final int BUTTON_TIER_BASE = 1;        // 1 to 4: L1 to L4
    public static final int BUTTON_OVERGROWTH = 5;
    public static final int BUTTON_OVERGROWTH_MINUS_5 = 6;
    public static final int BUTTON_OVERGROWTH_MINUS_1 = 7;
    public static final int BUTTON_OVERGROWTH_PLUS_1 = 8;
    public static final int BUTTON_OVERGROWTH_PLUS_5 = 9;
    public static final int BUTTON_HAZARD_BASE = 10;     // 10 plus the family's ordinal

    private final InteractionHand hand;
    private final DataSlot tier = DataSlot.standalone();
    private final DataSlot overgrowth = DataSlot.standalone();
    private final DataSlot[] hazards = new DataSlot[HazardFamily.values().length];

    public UpgradeStickMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(containerId, inv, extraData.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    public static void open(ServerPlayer player, InteractionHand hand) {
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.dermicraft.upgrade_stick");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player p) {
                return new UpgradeStickMenu(containerId, inv, hand);
            }
        }, buf -> buf.writeBoolean(hand == InteractionHand.OFF_HAND));
    }

    public UpgradeStickMenu(int containerId, Inventory inv, InteractionHand hand) {
        super(ModMenuTypes.UPGRADE_STICK_MENU.get(), containerId, 0);
        this.hand = hand;

        addDataSlot(tier);
        addDataSlot(overgrowth);
        for (int i = 0; i < hazards.length; i++) {
            hazards[i] = DataSlot.standalone();
            addDataSlot(hazards[i]);
        }
        setState(UpgradeStickItem.getTarget(inv.player.getItemInHand(hand)));
    }

    /** The selection, as the screen should draw it. */
    public EvolutionStateData getState() {
        EvolutionStateData state = new EvolutionStateData(tier.get(), overgrowth.get(), 0, 0, 0, 0);
        for (HazardFamily family : HazardFamily.values()) {
            state = state.withLevel(family, hazards[family.ordinal()].get());
        }
        return state;
    }

    private void setState(EvolutionStateData state) {
        tier.set(state.tier());
        overgrowth.set(state.overgrowth());
        for (HazardFamily family : HazardFamily.values()) {
            hazards[family.ordinal()].set(state.level(family));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!UpgradeStickItem.canUse(player)) return false;
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof UpgradeStickItem)) return false;

        EvolutionStateData current = UpgradeStickItem.getTarget(stack);
        EvolutionStateData next = current;
        if (id >= BUTTON_TIER_BASE && id < BUTTON_TIER_BASE + EvolutionStateData.MAX_TIER) {
            next = UpgradeStickRules.setTier(current, id - BUTTON_TIER_BASE + 1);
        } else if (id == BUTTON_OVERGROWTH) {
            next = UpgradeStickRules.selectOvergrowth(current);
        } else if (id == BUTTON_OVERGROWTH_MINUS_5) {
            next = UpgradeStickRules.adjustOvergrowth(current, -5);
        } else if (id == BUTTON_OVERGROWTH_MINUS_1) {
            next = UpgradeStickRules.adjustOvergrowth(current, -1);
        } else if (id == BUTTON_OVERGROWTH_PLUS_1) {
            next = UpgradeStickRules.adjustOvergrowth(current, 1);
        } else if (id == BUTTON_OVERGROWTH_PLUS_5) {
            next = UpgradeStickRules.adjustOvergrowth(current, 5);
        } else if (id >= BUTTON_HAZARD_BASE && id < BUTTON_HAZARD_BASE + HazardFamily.values().length) {
            next = UpgradeStickRules.cycleHazard(current, HazardFamily.values()[id - BUTTON_HAZARD_BASE]);
        } else {
            return false;
        }

        if (!next.equals(current)) {
            UpgradeStickItem.setTarget(stack, next);
            setState(next);
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // no slots
    }

    @Override
    public boolean stillValid(Player player) {
        return UpgradeStickItem.canUse(player) && player.getItemInHand(hand).getItem() instanceof UpgradeStickItem;
    }
}
