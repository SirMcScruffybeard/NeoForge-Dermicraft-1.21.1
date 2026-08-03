package net.scruffy.dermicraft.event;

import net.minecraft.client.player.Input;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.scruffy.dermicraft.item.custom.SunderItem;
import net.scruffy.dermicraft.main.Dermicraft;

/**
 * Cancels out vanilla's hardcoded "using an item slows you to 20% movement" penalty
 * ({@code LocalPlayer.tick()}: {@code if (isUsingItem()) { input.leftImpulse *= 0.2F; ... }}, not
 * gated on {@code UseAnim} or anything overridable -- applies to any item mid-use) specifically
 * while revving Sunder, which relies on {@code isUsingItem()} purely for hold-tracking and was
 * never meant to carry the eating/drinking/bow-draw movement penalty that comes bundled with it.
 *
 * <p>{@link MovementInputUpdateEvent} fires (per NeoForge's own {@code ClientHooks}) immediately
 * before that hardcoded multiply, so pre-multiplying by the reciprocal (5.0, since 1/0.2 = 5) here
 * nets back to the original impulse once vanilla's own 0.2x runs right after -- same technique as
 * the earlier auto-swing fix: work with the extension point NeoForge actually provides, rather than
 * fighting client internals that aren't overridable at all (compare {@code SunderItem#hurtEnemy}'s
 * javadoc on the attack-key block, which has no such counter-multiply available).
 */
@EventBusSubscriber(modid = Dermicraft.MOD_ID, value = Dist.CLIENT)
public class SunderClientEvents {

    /** Reciprocal of vanilla's hardcoded 0.2F use-item movement multiplier. */
    private static final float COUNTER_SLOWDOWN_MULTIPLIER = 5.0F;

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (!player.isUsingItem() || !(player.getUseItem().getItem() instanceof SunderItem)) return;

        Input input = event.getInput();
        input.leftImpulse *= COUNTER_SLOWDOWN_MULTIPLIER;
        input.forwardImpulse *= COUNTER_SLOWDOWN_MULTIPLIER;
    }
}
