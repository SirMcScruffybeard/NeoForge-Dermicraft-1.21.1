package net.scruffy.dermicraft.event;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.scruffy.dermicraft.hazard.HazardProfile;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.item.custom.DrinkerItem;
import net.scruffy.dermicraft.main.Dermicraft;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side "what am I aiming at" scan for the D.R.I.N.K.E.R. -- drives both the model's
 * emissive target-scan screen and a live action-bar readout, so the player can see what they're
 * about to siphon BEFORE committing to a click (the old version's problem: misjudging which
 * face/tank a cramped multi-tank machine exposes).
 *
 * <p>Deliberately re-scans only every few ticks rather than every frame, per the design notes --
 * a raycast per frame is wasted work for something the player reads at human speed.
 *
 * <p>Show/no-show rules (from the design pass): valid fluid target -> name; fluid present but
 * beyond DRINKER's hazard tolerance -> name plus an invalid marker, so the player learns WHY it's
 * refusing rather than seeing nothing happen; anything that isn't a fluid source or fluid-handling
 * block -> nothing at all, so the action bar isn't spammed for irrelevant blocks.
 */
@EventBusSubscriber(modid = Dermicraft.MOD_ID, value = Dist.CLIENT)
public class DrinkerTargetScanner {

    /** Ticks between re-scans -- the "slight delay on the render tick" the design notes call for. */
    private static final int SCAN_INTERVAL_TICKS = 3;

    /** DRINKER's Tier 1 tolerance -- rejects any hazard tag at all. */
    private static final HazardProfile PROFILE = HazardProfile.TIER_1;
    /** One fluid source block, the indivisible unit DRINKER picks up. */
    private static final int SOURCE_BLOCK_AMOUNT = 1000;

    private static int tickCounter = 0;
    private static boolean validTarget = false;

    /** Whether the held DRINKER is currently aimed at something it could actually siphon.
     * Read by the model's screen render layer. */
    public static boolean hasValidTarget() {
        return validTarget;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level level = mc.level;

        ItemStack drinker = heldDrinker(player);
        if (player == null || level == null || drinker.isEmpty()) {
            validTarget = false;
            return;
        }

        if (++tickCounter < SCAN_INTERVAL_TICKS) return;
        tickCounter = 0;

        scan(player, level, drinker);
    }

    private static ItemStack heldDrinker(LocalPlayer player) {
        if (player == null) return ItemStack.EMPTY;
        if (player.getMainHandItem().is(ModItems.DRINKER.get())) return player.getMainHandItem();
        if (player.getOffhandItem().is(ModItems.DRINKER.get())) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    private static void scan(LocalPlayer player, Level level, ItemStack drinker) {
        // Deliberately the EXACT raycast DrinkerItem uses to pick its target, not a lookalike:
        // this shares the player's real block-interaction range and SOURCE_ONLY fluid clipping, so
        // the readout can never promise something the siphon then refuses to reach or act on.
        HitResult hit = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            clear();
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos pos = blockHit.getBlockPos();
        Direction face = blockHit.getDirection();

        // A block with a fluid-handler capability (machine/tank) takes priority over the raw
        // fluidstate check -- a machine is the more specific interpretation of the same position.
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, face);
        if (handler != null) {
            reportHandler(player, level, pos, face, handler, drinker);
            return;
        }

        FluidState fluidState = level.getFluidState(pos);
        if (!fluidState.isEmpty() && fluidState.isSource()) {
            // A source block is exactly one bucket's worth, matching DRINKER's atomic pickup rule.
            FluidStack source = new FluidStack(fluidState.getType(), SOURCE_BLOCK_AMOUNT);
            reportFluid(player, drinker, source, null, null, true);
            return;
        }

        clear();
    }

    /** Machine/tank face: name the fluid that would actually be pulled, plus which TANK that face
     * reaches. Uses {@link IHasChannels#describeFluidFace}, not {@code describeFace} -- DRINKER has
     * no concept of item slots, and the two genuinely name different things on several machines. */
    private static void reportHandler(LocalPlayer player, Level level, BlockPos pos, Direction face,
                                      IFluidHandler handler, ItemStack drinker) {
        BlockEntity be = level.getBlockEntity(pos);
        Component faceLabel = be instanceof IHasChannels hasChannels ? hasChannels.describeFluidFace(face) : null;

        FluidStack contained = FluidStack.EMPTY;
        int capacity = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack inTank = handler.getFluidInTank(tank);
            if (!inTank.isEmpty()) {
                contained = inTank;
                capacity = handler.getTankCapacity(tank);
                break;
            }
        }

        if (contained.isEmpty()) {
            // Nothing to take, but this IS a fluid target -- say so rather than going silent, so an
            // empty machine reads differently from "you're pointing at the wrong thing entirely".
            show(player, false, faceLabel != null
                    ? Component.translatable("tooltip.dermicraft.drinker.empty_named", faceLabel)
                    : Component.translatable("tooltip.dermicraft.drinker.empty"));
            return;
        }

        // Level readout is tank-only: a world source block is always exactly one bucket, so
        // printing its amount would be noise.
        Component amount = Component.translatable("tooltip.dermicraft.drinker.amount",
                contained.getAmount(), capacity);
        reportFluid(player, drinker, contained, faceLabel, amount, false);
    }

    /**
     * Shared readout for both target kinds. Rejection reasons are checked in order of how
     * fundamental they are -- hazard, then fluid mismatch, then capacity -- so the player always
     * gets the most explanatory message rather than whichever check happened to run first.
     *
     * @param atomic true for a world source block, which needs room for a WHOLE bucket's worth;
     *               false for a tank, which transfers partially and only needs room for something.
     */
    private static void reportFluid(LocalPlayer player, ItemStack drinker, FluidStack fluid,
                                    @Nullable Component faceLabel, @Nullable Component amount, boolean atomic) {
        MutableComponent subject = Component.empty();
        if (faceLabel != null) subject.append(faceLabel).append(" -- ");
        subject.append(Component.translatable(fluid.getFluid().getFluidType().getDescriptionId()));
        if (amount != null) subject.append(" (").append(amount).append(")");

        if (!PROFILE.accepts(fluid)) {
            show(player, false, Component.translatable("tooltip.dermicraft.drinker.blocked", subject)
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // The accept/reject decision comes from the item's OWN routing simulation, not a local
        // re-implementation -- destination depends on mode (Disposal is bottomless, Transfer can
        // reach inventory containers), and a second copy of those rules would inevitably drift.
        int room = DrinkerItem.route(drinker, player, fluid, IFluidHandler.FluidAction.SIMULATE);
        if (atomic ? room < SOURCE_BLOCK_AMOUNT : room <= 0) {
            // Message is a best-effort explanation of a decision already made above. A buffer
            // holding a different fluid also reports zero room, so name that specifically rather
            // than claiming "full", which would be actively misleading.
            FluidStack held = DrinkerItem.bufferContents(drinker);
            boolean mismatch = !held.isEmpty() && !FluidStack.isSameFluidSameComponents(held, fluid);

            Component reason = mismatch
                    ? Component.translatable("tooltip.dermicraft.drinker.fluid_mismatch",
                            Component.translatable(held.getFluid().getFluidType().getDescriptionId()))
                    : Component.translatable(atomic
                            ? "tooltip.dermicraft.drinker.no_room"
                            : "tooltip.dermicraft.drinker.buffer_full");
            show(player, false, reason.copy().withStyle(ChatFormatting.RED));
            return;
        }

        show(player, true, Component.translatable("tooltip.dermicraft.drinker.target", subject));
    }

    private static void show(LocalPlayer player, boolean valid, Component message) {
        validTarget = valid;
        player.displayClientMessage(message, true);
    }

    private static void clear() {
        validTarget = false;
    }
}
