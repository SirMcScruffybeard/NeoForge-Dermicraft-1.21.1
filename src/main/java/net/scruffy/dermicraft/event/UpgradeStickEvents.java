package net.scruffy.dermicraft.event;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.scruffy.dermicraft.block.entity.custom.base.EvolvingMachineBlockEntity;
import net.scruffy.dermicraft.component.EvolutionStateData;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.item.custom.UpgradeStickItem;
import net.scruffy.dermicraft.machine.EvolutionEffects;
import net.scruffy.dermicraft.machine.UpgradeStickRules;
import net.scruffy.dermicraft.main.Dermicraft;

/**
 * The Upgrade Stick on a machine. A target is a block in {@link ModTags.Blocks#UPGRADE_STICK_TARGETS} whose block
 * entity evolves ({@link EvolvingMachineBlockEntity}); add a machine to that tag once it is brought up to spec. The
 * click is cancelled on both sides so the machine's own interaction (its GUI, a held item's use) never runs, and the
 * server does the work: a standing click gives the machine the stick's selection, a crouching click copies the
 * machine's state into the stick. Creative only unless the server config allows survival; clicking anything else is left to {@link UpgradeStickItem}.
 */
@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class UpgradeStickEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Player player = event.getEntity();
        ItemStack stick = event.getItemStack();
        if (!(stick.getItem() instanceof UpgradeStickItem) || !UpgradeStickItem.canUse(player)) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!level.getBlockState(pos).is(ModTags.Blocks.UPGRADE_STICK_TARGETS)) return;
        if (!(level.getBlockEntity(pos) instanceof EvolvingMachineBlockEntity machine)) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level.isClientSide) return;

        if (player.isShiftKeyDown()) {
            copy(player, stick, machine);
        } else {
            apply(level, pos, player, stick, machine);
        }
    }

    private static void copy(Player player, ItemStack stick, EvolvingMachineBlockEntity machine) {
        EvolutionStateData state = machine.getEvolutionState();
        UpgradeStickItem.setTarget(stick, state);
        player.displayClientMessage(Component.translatable("message.dermicraft.upgrade_stick.copied", state.label()), true);
    }

    private static void apply(Level level, BlockPos pos, Player player, ItemStack stick, EvolvingMachineBlockEntity machine) {
        EvolutionStateData target = UpgradeStickItem.getTarget(stick);
        // A machine with no hazard concept (the Craw) takes the level only.
        if (!machine.handlesHazards()) target = UpgradeStickRules.withoutHazards(target);

        boolean changed = !target.equals(machine.getEvolutionState());
        // Sets the state (a no-op when it already matches) and resets any evolution progress either way.
        machine.applyEvolutionState(target);

        if (changed) {
            if (level instanceof ServerLevel serverLevel) {
                EvolutionEffects.playDefaultStartSound(level, pos);
                EvolutionEffects.spawnParticles(serverLevel, pos, 1.0F, true, EvolutionEffects.DEFAULT_TINT,
                        EvolutionEffects.Cadence.TICK);
            }
            player.displayClientMessage(Component.translatable("message.dermicraft.upgrade_stick.applied", target.label()), true);
        } else {
            player.displayClientMessage(Component.translatable("message.dermicraft.upgrade_stick.unchanged", target.label()), true);
        }
    }
}
