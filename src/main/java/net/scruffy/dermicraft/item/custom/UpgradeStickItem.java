package net.scruffy.dermicraft.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.scruffy.dermicraft.component.EvolutionStateData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.machine.StatConfig;
import net.scruffy.dermicraft.screen.custom.upgrade_stick.UpgradeStickMenu;

import java.util.List;

/**
 * Creative-only (unless the server config allows survival) tool that sets a machine's tier, Overgrowth level and hazard tolerances outright. The selection lives
 * on the stick as an {@link EvolutionStateData} (L1 with no hazards by default). Right-clicking air or a block that is
 * not an Upgrade Stick target opens the selection screen; right-clicking a target machine applies the selection to it,
 * and crouching copies the machine's state into the stick instead (both in {@code UpgradeStickEvents}, which also
 * decides what counts as a target).
 */
public class UpgradeStickItem extends Item {

    public UpgradeStickItem(Properties properties) {
        super(properties);
    }

    /** Whether {@code player} may use an Upgrade Stick: creative players always, anyone else only when the synced server
     * config allows it. Every part of the stick goes through this one check. */
    public static boolean canUse(Player player) {
        return player.isCreative() || StatConfig.isUpgradeStickAllowedInSurvival();
    }

    /** The level and tolerances this stick would give a machine. */
    public static EvolutionStateData getTarget(ItemStack stack) {
        return stack.getOrDefault(ModDataComponentTypes.UPGRADE_STICK_TARGET.get(), EvolutionStateData.DEFAULT);
    }

    public static void setTarget(ItemStack stack, EvolutionStateData state) {
        stack.set(ModDataComponentTypes.UPGRADE_STICK_TARGET.get(), state);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canUse(player)) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            UpgradeStickMenu.open(serverPlayer, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** A block that is not a target: same as clicking air. (A target machine never gets here; the event handles it.) */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !canUse(player)) return InteractionResult.PASS;
        Level level = context.getLevel();
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            UpgradeStickMenu.open(serverPlayer, context.getHand());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dermicraft.upgrade_stick.creative").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.dermicraft.upgrade_stick.selected", getTarget(stack).label())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.dermicraft.upgrade_stick.use").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.dermicraft.upgrade_stick.copy").withStyle(ChatFormatting.DARK_GRAY));
    }
}
