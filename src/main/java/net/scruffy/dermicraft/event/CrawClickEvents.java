package net.scruffy.dermicraft.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.scruffy.dermicraft.block.entity.custom.CrawBlockEntity;
import net.scruffy.dermicraft.main.Dermicraft;

/**
 * Catches the Craw's crouching right click. Vanilla skips a block's {@code useItemOn} when the player crouches with
 * something in hand, so the gesture cannot live in the block; this event fires first, on both sides, and is cancelled
 * with SUCCESS on both so neither the client nor the server goes on to use the held item. The client's cancelled click
 * is still sent to the server, where the work is done.
 *
 * <p>A crouching click deposits every stack in the player's inventory that matches the Craw (or the held item, when the
 * Craw is empty); with nothing to match against it opens the GUI, as an empty hand does when standing.
 */
@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class CrawClickEvents {

    /**
     * A left click on a Craw withdraws items (see {@code CrawBlockEntity#handleLeftClick}) and never opens the GUI.
     * In survival the click is left alone and vanilla mining carries on, finishing after the hold time set by
     * {@code CrawBlock#getDestroyProgress}. In creative vanilla would break the block outright, so the click is cancelled
     * on both sides and the server breaks it after the hold instead. Only the start of a click counts.
     */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;

        Level level = event.getLevel();
        if (!(level.getBlockEntity(event.getPos()) instanceof CrawBlockEntity craw)) return;

        Player player = event.getEntity();
        boolean creative = player.isCreative();
        if (creative) event.setCanceled(true);

        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) return;
        if (creative) {
            craw.onCreativeLeftClick(serverPlayer, event.getItemStack());
        } else {
            craw.handleLeftClick(player, event.getItemStack());
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) return;

        Level level = event.getLevel();
        if (!(level.getBlockEntity(event.getPos()) instanceof CrawBlockEntity craw)) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) return;
        if (!craw.handleCrouchClick(player, event.getItemStack())) {
            serverPlayer.openMenu(craw, buf -> buf.writeBlockPos(event.getPos()));
        }
    }
}
