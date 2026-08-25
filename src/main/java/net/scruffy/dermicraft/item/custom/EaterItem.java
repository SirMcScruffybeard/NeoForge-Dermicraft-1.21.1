package net.scruffy.dermicraft.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scruffy.dermicraft.component.DrinkerModeData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.hazard.HazardProfile;
import net.scruffy.dermicraft.interfaces.IGadget;
import net.scruffy.dermicraft.interfaces.IHaveItemData;
import net.scruffy.dermicraft.interfaces.IHaveModules;
import net.scruffy.dermicraft.interfaces.IWorkbenchSwappable;
import net.scruffy.dermicraft.screen.custom.scrench.ScrenchMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * E.A.T.E.R. -- held item vacuum, the item-side counterpart to D.R.I.N.K.E.R.'s fluid vacuum.
 * Base tier only: hold right-click to pull loose dropped items within a forward cone into a
 * 4-slot internal buffer, or push them straight to the player/void them depending on mode.
 *
 * <p>No atomic-pickup ghost buffer the way DRINKER needs for a fluid source block -- a dropped
 * item stack is already divisible, so every tick just routes whatever it can reach straight to
 * its destination. That's the whole reason this item's tick loop is simpler than DRINKER's
 * despite sharing the same held-trigger identity and mode cycle.
 */
public class EaterItem extends Item implements GeoItem, IGadget, IHaveItemData, IHaveModules, IWorkbenchSwappable {

    /** Gadget Module loadout size -- see dermicraft-gadget-notes.md -> Gadget upgrade points ->
     * Modules direction note's worked Eater example ("3 general-purpose slots, no type
     * reservation"). Backed by {@link ModDataComponentTypes#MODULE_DATA}, independent of the item
     * buffer above (same stack, two separate components). */
    public static final int MODULE_SLOT_COUNT = 3;
    /** Exactly one Module per slot -- these aren't stackable resources. */
    public static final int MODULE_SLOT_CAPACITY = 1;

    /** Brief "can't use Eater again yet" cooldown applied after a completed field (Scrench) Module
     * swap -- Eater's own flavor of Scrench field cost, deliberately not Sunder's movement penalty
     * (see the design discussion: a harvesting tool doesn't have the same "off-balance from
     * wrenching on a weapon mid-combat" stakes a weapon does). Vanilla's own per-item cooldown
     * system is a clean fit -- no custom effect class needed. Placeholder magnitude, same "short
     * debuff-length" ballpark as Sunder's own SWAP_PENALTY_DURATION_TICKS.
     */
    private static final int SWAP_RECALIBRATION_COOLDOWN_TICKS = 40;

    /** Pickup range around the player, in blocks. Originally a flat 360-degree radius (see the
     * base-tier design notes in dermicraft-gadget-notes.md); narrowed to a forward cone (see
     * {@link #CONE_HALF_ANGLE_DEGREES}) after in-game testing showed the full sphere felt
     * untargeted, picking up items well outside where the player was actually aiming. */
    public static final double RADIUS = 4.0;

    /** Half-angle of the forward cone, measured from the player's look vector -- an item outside
     * this angle is ignored even if within {@link #RADIUS}. 60 degrees is a generous cone (120
     * degrees total), not a narrow reticle-style aim; tune down for something more deliberate. */
    private static final double CONE_HALF_ANGLE_DEGREES = 60.0;
    private static final double CONE_COS_THRESHOLD = Math.cos(Math.toRadians(CONE_HALF_ANGLE_DEGREES));

    public static final int SLOT_COUNT = 4;
    /** Deliberately a normal stack, not a bulk multiplier -- Eater's base tier has no need to
     * exceed it. Shares the bulk-capable backing store anyway; see IHaveItemData's javadoc. */
    public static final int SLOT_CAPACITY = 64;

    /** Gadget health, expressed as vanilla durability -- see {@link IGadget}. */
    public static final int MAX_HP = 10;

    /** Effectively "until released" -- same trick DRINKER's held pose uses. */
    private static final int HELD_INDEFINITELY = 72000;

    /** Minimum ticks after arming before a confirm counts -- blocks accidental double-clicks. */
    private static final long MIN_CONFIRM_DELAY_TICKS = 20;
    /** Total ticks the Disposal confirm window stays open. */
    private static final long ARM_WINDOW_TICKS = 60;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public EaterItem(Properties properties) {
        super(properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    ////////////////////Held pose\\\\\\\\\\\\\\\\\\\\

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return HELD_INDEFINITELY;
    }

    /** See DrinkerItem's identical override -- EATER_VACUUMING rewrites every tick while active,
     * and the default equals-check would otherwise thrash the equip animation. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    ////////////////////Gestures\\\\\\\\\\\\\\\\\\\\

    /**
     * Mirrors DRINKER's actual branching: a real target always wins regardless of crouch/stand,
     * and crouch only decides what a targetless click does. Previously this checked crouch first,
     * which meant standing with nothing in range still started the (useless) held vacuum instead
     * of falling through to a mode cycle.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Checks the other hand for a paired Scrench first, deferring to the Module-swap GUI if
        // found -- same pattern as SunderItem's own matching check (see that class for why this has
        // to happen before any of Eater's own click handling below, not after).
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (player.getItemInHand(otherHand).getItem() instanceof ScrenchItem) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                ScrenchMenu.open(serverPlayer, hand);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (hasVacuumTarget(level, player, stack)) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        if (!level.isClientSide) {
            if (player.isCrouching()) {
                processBuffer(level, player, stack);
            } else {
                cycleMode(player, stack);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Existence-only check, same scans the real per-tick loops use -- cheap enough to run on every
     * right-click since it's just an AABB scan plus a dot-product filter (loose items) or a single
     * raycast (an Aggregate target), no routing/mutation either way. Without the Aggregate half of
     * this check, a click with nothing to vacuum but a valid Aggregate block in view fell through to
     * the mode-cycle branch below instead of ever starting the held vacuum that would have let
     * {@link #aggregateTick} run at all. */
    private boolean hasVacuumTarget(Level level, Player player, ItemStack stack) {
        if (!nearbyVacuumTargets(level, player, stack).isEmpty()) return true;
        if (!hasModule(stack, ModTags.Items.MODULE_AGGREGATE) && !hasModule(stack, ModTags.Items.MODULE_BEAM)) return false;
        return aggregateTarget(level, player, stack) != null;
    }

    /** Items within {@link #RADIUS}, inside the forward cone, AND whose straight-line path from the
     * player's eye is hazard-tolerated -- the single filter both the targetless-click check and the
     * real vacuum tick scan through, so they can never disagree about what counts as reachable.
     * Gated the same way {@link #aggregateTarget}'s Fluid Bypass path is: an item resting past a
     * lava flow isn't reachable at all without the gadget's installed Safety Modules ({@link
     * #installedHazardProfile}) tolerating what's in the way, regardless of Fluid Bypass -- Bypass
     * only concerns Aggregate's block-targeting raycast, loose items were never fluid-obstructed to
     * begin with, this just adds the hazard half of that check. */
    private static List<ItemEntity> nearbyVacuumTargets(Level level, Player player, ItemStack stack) {
        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();
        HazardProfile profile = installedHazardProfile(stack);

        List<ItemEntity> all = level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(RADIUS),
                ItemEntity::isAlive);
        List<ItemEntity> targets = new java.util.ArrayList<>();
        for (ItemEntity candidate : all) {
            // Already close enough to be mid-pull (or resting at the player's feet) -- stays a
            // target regardless of look direction. Without this, an item nearing the player drops
            // below eye level, the eye-to-item vector points steeply down, fails the cone test
            // below, and the item gets stranded requiring the player to look back down at its new
            // position -- the cone should only gate STARTING a pull, not continuing one already
            // underway.
            if (candidate.distanceToSqr(player) <= CAPTURE_BUBBLE * CAPTURE_BUBBLE) {
                targets.add(candidate);
                continue;
            }

            Vec3 toTarget = candidate.position().subtract(eyePos);
            double lengthSq = toTarget.lengthSqr();
            boolean inCone = lengthSq < 1.0E-4 || toTarget.normalize().dot(look) >= CONE_COS_THRESHOLD;
            if (!inCone) continue;
            if (!fluidPathTolerated(level, eyePos, candidate.position(), profile)) continue;
            targets.add(candidate);
        }
        return targets;
    }

    private void cycleMode(Player player, ItemStack stack) {
        DrinkerModeData next = modeData(stack).next();
        stack.set(ModDataComponentTypes.EATER_MODE_DATA.get(), next);
        player.displayClientMessage(Component.translatable("tooltip.dermicraft.eater.mode",
                Component.translatable(modeKey(next.mode()))), true);
    }

    /**
     * Crouch-click with nothing to vacuum acts on whatever's already banked -- mirrors DRINKER's
     * split exactly: crouch is always "act on the buffer", standing (see {@link #use}) is always
     * "change what happens next". Storage has nothing to actively do with its own contents (same
     * as DRINKER's "storage_inert" message), so only Transfer/Disposal move anything.
     */
    private void processBuffer(Level level, Player player, ItemStack stack) {
        switch (modeData(stack).mode()) {
            case STORAGE -> player.displayClientMessage(
                    Component.translatable("tooltip.dermicraft.eater.storage_inert"), true);
            case TRANSFER -> transferBuffer(player, stack);
            case DISPOSAL -> voidBuffer(level, player, stack);
        }
    }

    /** Pushes every banked slot into the player's inventory; whatever doesn't fit stays banked. */
    private void transferBuffer(Player player, ItemStack stack) {
        IItemHandlerModifiable handler = new IHaveItemData.BulkItemHandler(stack, SLOT_COUNT, SLOT_CAPACITY);
        boolean movedAny = false;

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack held = handler.getStackInSlot(slot);
            if (held.isEmpty()) continue;

            ItemStack remainder = held.copy();
            player.getInventory().add(remainder);
            int moved = held.getCount() - remainder.getCount();
            if (moved <= 0) continue;

            movedAny = true;
            handler.extractItem(slot, moved, false);
        }

        player.displayClientMessage(Component.translatable(movedAny
                ? "tooltip.dermicraft.eater.transferred"
                : "tooltip.dermicraft.eater.nothing_held"), true);
    }

    /**
     * Voids every banked slot, behind the same arm/confirm shape DRINKER uses for its own
     * Disposal action: crouch-click once to arm (with a warning), crouch-click again inside the
     * window to confirm, or let it expire and nothing happens. The confirm guards the ACT of
     * voiding, not the mode switch -- Storage/Transfer stay freely reachable, only actually
     * destroying banked contents needs a second click.
     */
    private void voidBuffer(Level level, Player player, ItemStack stack) {
        boolean hadContents = !net.scruffy.dermicraft.component.BulkItemData.empty(SLOT_COUNT)
                .equals(bufferData(stack));
        if (!hadContents) {
            player.displayClientMessage(Component.translatable("tooltip.dermicraft.eater.nothing_held")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }

        DrinkerModeData data = modeData(stack);
        long now = level.getGameTime();
        if (data.armed()) {
            long elapsed = now - data.armedAtGameTime();
            // Inside the dead zone: swallowed silently, arm state untouched -- a second click that
            // fast is far more likely to be a slip than a decision.
            if (elapsed < MIN_CONFIRM_DELAY_TICKS) return;

            if (elapsed <= ARM_WINDOW_TICKS) {
                stack.remove(ModDataComponentTypes.BULK_ITEM_DATA.get());
                stack.set(ModDataComponentTypes.EATER_MODE_DATA.get(), data.disarmed());
                player.displayClientMessage(Component.translatable("tooltip.dermicraft.eater.voided")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
        }

        stack.set(ModDataComponentTypes.EATER_MODE_DATA.get(), data.armedAt(now));
        player.displayClientMessage(Component.translatable("tooltip.dermicraft.eater.void_warning")
                .withStyle(ChatFormatting.RED), true);
    }

    /** A pending void confirm never outlives its window, nor the player's grip on the item --
     * same shape as DRINKER's identical guard. */
    private void expireArm(ItemStack stack, Level level, Player player) {
        DrinkerModeData data = modeData(stack);
        if (!data.armed()) return;

        boolean stillHeld = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        if (!stillHeld || level.getGameTime() - data.armedAtGameTime() > ARM_WINDOW_TICKS) {
            stack.set(ModDataComponentTypes.EATER_MODE_DATA.get(), data.disarmed());
        }
    }

    ////////////////////Vacuum\\\\\\\\\\\\\\\\\\\\

    /** Ticks the trigger must be held before the vacuum actually starts pulling/consuming --
     * gives the mouth-cover bloom animation (which starts immediately on holding, see below) time
     * to finish opening before anything visibly moves toward it. */
    private static final int VACUUM_WINDUP_TICKS = 20;

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;

        expireArm(stack, level, player);

        boolean holdingTrigger = player.isUsingItem() && player.getUseItem() == stack;
        // Candidates need protecting from vanilla's own walk-over-item pickup for the WHOLE hold,
        // not just once the windup ends -- otherwise vanilla grabs them during the windup, before
        // Eater ever touches them, which looks identical to Eater doing nothing at all.
        if (holdingTrigger) protectVacuumCandidates(level, player, stack);

        boolean pastWindup = holdingTrigger && player.getTicksUsingItem() >= VACUUM_WINDUP_TICKS;
        if (pastWindup) {
            vacuumTick(level, player, stack);
            aggregateTick(level, player, stack);
        } else {
            // Trigger released (or still mid-windup) -- don't leave a stale crack overlay/progress
            // entry sitting on whatever block was last targeted.
            clearAggregateProgress(player);
        }

        // Broadcasts the mining beam's target to everyone ELSE tracking this player, so third parties
        // see it too -- the holder's own client renders its own beam off a local, zero-latency
        // raycast instead (see GadgetBeamTargetPayload's javadoc for the full reasoning).
        //
        // Requires Beam Module specifically -- no beam at all without it, matching the emitter bone's
        // own visibility gate. With Beam installed, ANY block aggregateTarget accepts counts: a
        // STONE_ORE hit needs only Beam, but an AGGREGATE/AGGREGATE_HOT hit is only ever returned by
        // aggregateTarget when Aggregate is ALSO installed (that gate lives inside aggregateTarget
        // itself) -- so this single check already implements "beam shows for Aggregate targets too,
        // but only with both Modules installed" without needing to test for Aggregate again here.
        // Never active for loose-item vacuuming (see vacuumTick above): this only ever asks
        // aggregateTarget, which is block-only, so plain item pickup can't produce a beam.
        if (player instanceof ServerPlayer serverPlayer) {
            boolean canBeamMine = pastWindup && hasModule(stack, ModTags.Items.MODULE_BEAM);
            BlockHitResult beamHit = canBeamMine ? aggregateTarget(level, player, stack) : null;
            net.scruffy.dermicraft.util.GadgetBeamSync.tick(serverPlayer, beamHit != null,
                    beamHit != null ? beamHit.getLocation() : null);
        }

        // Drives the mouth-bloom animation off "trigger held", not "actually pulling something" --
        // it should be mid-open through the whole windup, not just once the windup ends.
        if (stack.getOrDefault(ModDataComponentTypes.EATER_VACUUMING.get(), false) != holdingTrigger) {
            stack.set(ModDataComponentTypes.EATER_VACUUMING.get(), holdingTrigger);
        }
    }

    /** Radius around the player that bypasses the forward-cone check entirely -- bigger than
     * {@link #CONSUME_DISTANCE} so it covers an item's whole final stretch of the pull, not just
     * the instant it's consumed. See {@link #nearbyVacuumTargets} for why this exists. */
    private static final double CAPTURE_BUBBLE = 1.5;

    /** Entities farther than this are pulled toward the player instead of being consumed outright
     * -- a short cosmetic tail instead of vanishing the instant they enter range. */
    private static final double CONSUME_DISTANCE = 0.75;
    /** Fraction of the remaining distance closed per tick -- eases out as the item nears the
     * player instead of a constant speed, which would either crawl in from range or overshoot up
     * close. */
    private static final double PULL_FRACTION = 0.4;
    /** Refreshed every tick an item is being pulled, so vanilla's own walk-over-item pickup never
     * gets a chance to grab it out from under Eater's own routing -- see {@link #pullToward}. */
    private static final int PULL_PICKUP_DELAY_TICKS = 40;

    /** Refreshes every candidate's pickup delay for the whole windup, before Eater has any other
     * business with them -- see the windup comment in {@link #inventoryTick}. Also refreshes lava
     * protection (see {@link #markLavaProtected}) for any candidate currently sitting in lava while
     * Heat Safety is installed -- without this, an item only reachable in the first place because
     * its path was heat-tolerated (see {@link #nearbyVacuumTargets}) would still burn to nothing
     * mid-pull, defeating the point of tolerating the path at all. */
    private static void protectVacuumCandidates(Level level, Player player, ItemStack stack) {
        boolean heatTolerant = installedHazardProfile(stack).tolerated().contains(ModTags.Fluids.THERMAL);
        for (ItemEntity candidate : nearbyVacuumTargets(level, player, stack)) {
            candidate.setPickUpDelay(PULL_PICKUP_DELAY_TICKS);
            if (heatTolerant && candidate.isInLava()) {
                markLavaProtected(candidate);
            }
        }
    }

    /** Item entities Heat Safety is currently shielding from lava's own damage/ignite while Eater
     * pulls them through it -- refreshed every tick a candidate qualifies (see
     * {@link #protectVacuumCandidates}), read by {@link GadgetEvents}'s invulnerability hook. Short
     * expiry rather than persistent, so a released trigger or an item that leaves lava on its own
     * stops being protected almost immediately -- same "harmless stale entry" reasoning as
     * {@link #AGGREGATE_PROGRESS}. */
    private static final Map<UUID, Long> LAVA_PROTECTED_UNTIL = new HashMap<>();
    private static final int LAVA_PROTECTION_GRACE_TICKS = 5;

    private static void markLavaProtected(ItemEntity entity) {
        LAVA_PROTECTED_UNTIL.put(entity.getUUID(), entity.level().getGameTime() + LAVA_PROTECTION_GRACE_TICKS);
    }

    /** Whether Heat Safety is currently shielding {@code entity} from fire/lava damage -- see
     * {@link #markLavaProtected}. */
    public static boolean isLavaProtected(Entity entity) {
        Long until = LAVA_PROTECTED_UNTIL.get(entity.getUUID());
        return until != null && entity.level().getGameTime() <= until;
    }

    /** The point the pull actually eases items toward -- roughly chest height, not the feet.
     * Shared by the pull itself and the consume-distance check below so the two can't disagree
     * about "close enough": comparing the consume threshold against {@code player.position()}
     * (feet level) while pulling toward a point ~0.9 blocks above it meant an item would approach
     * equilibrium just outside the threshold and stall there forever, never actually consumed. */
    private static Vec3 pullTarget(Player player) {
        return player.position().add(0, player.getBbHeight() * 0.5, 0);
    }

    private void vacuumTick(Level level, Player player, ItemStack stack) {
        List<ItemEntity> nearby = nearbyVacuumTargets(level, player, stack);
        Vec3 target = pullTarget(player);

        for (ItemEntity entity : nearby) {
            if (entity.position().distanceToSqr(target) > CONSUME_DISTANCE * CONSUME_DISTANCE) {
                pullToward(entity, target);
                continue;
            }

            ItemStack ground = entity.getItem();
            if (ground.isEmpty()) continue;

            int consumed = routeIncoming(stack, player, ground);
            if (consumed <= 0) continue;

            if (consumed >= ground.getCount()) {
                entity.discard();
            } else {
                ground.shrink(consumed);
                entity.setItem(ground);
            }
        }
    }

    /** Cosmetic pull -- eases the item toward the player rather than fighting vanilla physics or
     * teleporting outright, so it reads as being drawn in rather than snapping. Refreshes the
     * pickup delay every tick (rather than clearing it) so vanilla's own walk-over-item pickup
     * can't grab it out from under Eater's own routing while it's mid-pull. */
    private static void pullToward(ItemEntity entity, Vec3 target) {
        Vec3 eased = entity.position().lerp(target, PULL_FRACTION);

        entity.setPos(eased.x, eased.y, eased.z);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hasImpulse = true;
        entity.setPickUpDelay(PULL_PICKUP_DELAY_TICKS);
    }

    /**
     * Routes an incoming ground stack per the current mode, without mutating {@code ground} --
     * the caller decides how to shrink/remove the source entity from the returned amount. Mirrors
     * DRINKER's {@code route}, but items have no hazard profile to gate against, so every mode
     * always accepts everything it touches.
     */
    private static int routeIncoming(ItemStack self, Player player, ItemStack ground) {
        return switch (modeData(self).mode()) {
            case DISPOSAL -> ground.getCount();
            case STORAGE -> fillBuffer(self, ground);
            case TRANSFER -> {
                ItemStack remainder = ground.copy();
                player.getInventory().add(remainder);
                int intoInventory = ground.getCount() - remainder.getCount();

                if (remainder.isEmpty()) yield intoInventory;
                yield intoInventory + fillBuffer(self, remainder);
            }
        };
    }

    private static int fillBuffer(ItemStack self, ItemStack toStore) {
        if (toStore.isEmpty()) return 0;

        IItemHandlerModifiable handler = new IHaveItemData.BulkItemHandler(self, SLOT_COUNT, SLOT_CAPACITY);
        ItemStack remaining = toStore.copy();
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, false);
        }
        return toStore.getCount() - remaining.getCount();
    }

    ////////////////////Aggregate/Beam mining (Aggregate Module, Beam Module)\\\\\\\\\\\\\\\\\\\\

    /** Cadence for block mining specifically, distinct from the per-tick item-pull loop -- each
     * "hit" lands once per this many ticks, not every tick. */
    private static final int AGGREGATE_STEP_TICKS = 5;

    /** What each Module simulates holding for break SPEED purposes -- fixed per Module regardless of
     * the specific target block (unlike {@link #minimumToolFor}, which varies per Beam target for
     * drop-gating purposes; Aggregate targets never gate drops on a tool at all, so Aggregate only
     * ever needs a speed stand-in, no per-block minimum). Both user-decided tiers -- Aggregate at
     * iron shovel, Beam upgraded from its original stone-pickaxe baseline to iron. Plain, unenchanted
     * stand-ins. */
    private static final ItemStack AGGREGATE_SPEED_TOOL_STAND_IN = new ItemStack(net.minecraft.world.item.Items.IRON_SHOVEL);
    private static final ItemStack BEAM_SPEED_TOOL_STAND_IN = new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE);

    /** What Beam mining simulates holding for drop-tier purposes: the block always breaks, but drops
     * are computed as if using EXACTLY the minimum pickaxe tier that block actually requires,
     * unenchanted -- coal/iron/redstone/etc. each get their own real requirement rather than one flat
     * assumed tier, and nothing needing better than that minimum (there isn't one, by construction)
     * ever fails to drop. A fresh plain stack per call, not player-visible or stored anywhere --
     * purely a stand-in passed to vanilla's own tool-aware APIs. Aggregate targets never need this:
     * none of dirt/sand/gravel/clay/snow/netherrack require a correct tool to drop at all. */
    private static ItemStack minimumToolFor(BlockState state) {
        if (state.is(net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL)) return new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        if (state.is(net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL)) return new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE);
        if (state.is(net.minecraft.tags.BlockTags.NEEDS_STONE_TOOL)) return new ItemStack(net.minecraft.world.item.Items.STONE_PICKAXE);
        return new ItemStack(net.minecraft.world.item.Items.WOODEN_PICKAXE);
    }

    /** Transient, server-only mining progress, keyed per player -- deliberately NOT gadget item
     * data (unlike everything else this session): breaking progress is momentary interaction state,
     * not something meaningful to persist/sync with the stack, the same reason vanilla's own block-
     * breaking progress lives on {@code ServerPlayerGameMode} rather than on the tool. A stale entry
     * left behind by a player who disconnects mid-swing is harmless (just a few bytes, overwritten
     * or ignored on their next swing) -- not worth a logout-listener for a first pass.
     *
     * <p>{@code progress} is a 0..1 fraction -- both Modules now share the same hardness-scaled
     * formula ({@link #progressPerStep}), just with different simulated tools, so one accumulator
     * shape covers both. */
    private static final Map<UUID, AggregateProgress> AGGREGATE_PROGRESS = new HashMap<>();

    private record AggregateProgress(BlockPos pos, float progress) {}

    /**
     * Damages the block Eater is looking at, if -- and only if -- an Aggregate or Beam Module is
     * installed (see {@link #hasModule}) and {@link #aggregateTarget} accepts it, breaking (and
     * vacuuming) it only once progress reaches 1.0 on the SAME position consecutively -- aiming away
     * and back, or switching targets entirely, restarts progress from zero, same as vanilla's own
     * mining. {@link Level#destroyBlockProgress} drives the real crack overlay so this reads as
     * actual mining, not a silent timer.
     *
     * <p>On break: routes the real vanilla drops (gravel's flint chance, clay's 4-ball yield, an
     * ore's raw resource, etc. -- reusing {@link Block#getDrops}, not a hardcoded "drops itself"
     * assumption) through the exact same {@link #routeIncoming} every other vacuum target already
     * goes through, so Storage/Transfer/Disposal modes apply identically. Whatever doesn't fit drops
     * on the ground at the mined position, same as a buffer-full loose item is simply left
     * uncollected rather than lost. Beam targets pass {@link #minimumToolFor} as the drop-loot tool
     * instead of {@link ItemStack#EMPTY}, so ore loot tables gate on it same as a real pickaxe would.
     */
    private void aggregateTick(Level level, Player player, ItemStack stack) {
        if (!hasModule(stack, ModTags.Items.MODULE_AGGREGATE) && !hasModule(stack, ModTags.Items.MODULE_BEAM)) return;
        if (level.getGameTime() % AGGREGATE_STEP_TICKS != 0) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockHitResult hit = aggregateTarget(level, player, stack);
        if (hit == null) {
            clearAggregateProgress(player);
            return;
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        boolean beamTarget = state.is(ModTags.Blocks.STONE_ORE);
        // Netherrack is Aggregate roster, but a shovel isn't its correct tool in vanilla (pickaxe is)
        // -- special-cased to the Beam pickaxe stand-in for both speed and drops rather than the
        // shared Aggregate shovel stand-in, so it mines as if the correct tool were actually used.
        boolean netherrackTarget = !beamTarget && state.is(net.minecraft.world.level.block.Blocks.NETHERRACK);

        ItemStack minimumTool = beamTarget ? minimumToolFor(state)
                : netherrackTarget ? BEAM_SPEED_TOOL_STAND_IN : ItemStack.EMPTY;
        ItemStack speedTool = beamTarget || netherrackTarget ? BEAM_SPEED_TOOL_STAND_IN : AGGREGATE_SPEED_TOOL_STAND_IN;
        float increment = progressPerStep(state, level, pos, speedTool);

        AggregateProgress existing = AGGREGATE_PROGRESS.get(player.getUUID());
        float progress = (existing != null && existing.pos().equals(pos) ? existing.progress() : 0F) + increment;

        if (progress < 1F) {
            AGGREGATE_PROGRESS.put(player.getUUID(), new AggregateProgress(pos, progress));
            level.destroyBlockProgress(player.getId(), pos, Math.min(9, (int) (progress * 10F)));
            return;
        }

        clearAggregateProgress(player);

        List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, level.getBlockEntity(pos), player, minimumTool);
        // destroyBlock (not removeBlock) so this plays the same break particle burst + sound vanilla
        // mining always does -- that event fires independently of the drop flag, which stays false
        // since drops are already computed and routed through the buffer ourselves below.
        level.destroyBlock(pos, false);

        for (ItemStack drop : drops) {
            int consumed = routeIncoming(stack, player, drop);
            if (consumed >= drop.getCount()) continue;

            ItemStack leftover = drop.copyWithCount(drop.getCount() - consumed);
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), leftover);
        }
    }

    /** Vanilla's own per-tick destroy-progress formula (speed / hardness / (30 or 100)), scaled up
     * by {@link #AGGREGATE_STEP_TICKS} since this only actually evaluates once every that-many
     * ticks rather than every tick the way real mining does -- keeps the accumulated progress
     * equivalent to "as if evaluated every tick" despite the coarser cadence. Unbreakable blocks
     * (negative hardness) never progress, same as vanilla. Shared by both Modules; {@code speedTool}
     * is whichever of {@link #AGGREGATE_SPEED_TOOL_STAND_IN}/{@link #BEAM_SPEED_TOOL_STAND_IN} the
     * caller passes. */
    private static float progressPerStep(BlockState state, Level level, BlockPos pos, ItemStack speedTool) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0) return 0F;

        float speed = speedTool.getDestroySpeed(state);
        boolean correctTool = !state.requiresCorrectToolForDrops() || speedTool.isCorrectToolForDrops(state);
        float perTick = speed / hardness / (correctTool ? 30F : 100F);
        return perTick * AGGREGATE_STEP_TICKS;
    }

    /** Clears this player's mining progress and the crack overlay it was driving -- called both on
     * a completed break and whenever aiming stops landing on a valid target at all (see
     * {@link #inventoryTick}). */
    private static void clearAggregateProgress(Player player) {
        AggregateProgress removed = AGGREGATE_PROGRESS.remove(player.getUUID());
        if (removed != null) {
            player.level().destroyBlockProgress(player.getId(), removed.pos(), -1);
        }
    }

    /**
     * The block Eater's crosshair is on, within {@link #RADIUS} -- a straightforward reach-style
     * raycast, not the item vacuum's forward-cone-plus-AABB scan (blocks sit at fixed positions, so
     * "what am I actually looking at" is the natural targeting model, same as vanilla's own
     * block-breaking reach).
     *
     * <p>Fluid Bypass flips whether fluid blocks the ray: Eater deliberately treats fluid as an
     * obstruction by DEFAULT (the opposite of vanilla's own block-interaction raycast, which always
     * ignores fluid) -- see the Fluid Bypass Module's own design note ("Fluids block Eater's
     * block-targeting raycast by default, deliberately, not a bug to fix"). With Bypass installed,
     * the ray passes through fluid like vanilla's does, but only fluid the gadget's installed Safety
     * Modules actually tolerate ({@link #installedHazardProfile}) -- Bypass alone reaches a
     * submerged Aggregate deposit through plain water, but not through lava, unless a Heat Safety
     * Module is ALSO installed (see {@link #fluidPathTolerated}). Without Bypass, fluid blocks the
     * ray outright regardless of hazard tolerance, same as before.
     *
     * <p>The target block itself also needs a valid reason to be minable, gated per-Module
     * independently (see {@link ModTags.Items#MODULE_BEAM}'s own comment -- Beam and Aggregate are
     * unrelated capabilities, neither implies the other): Aggregate covers plain
     * {@link ModTags.Blocks#AGGREGATE} membership, or {@link ModTags.Blocks#AGGREGATE_HOT}
     * membership (currently just Magma Block) gated on the installed hazard tolerance covering
     * {@code THERMAL} -- the Thermal Safety Module's other half of its behavior. Beam covers
     * {@link ModTags.Blocks#STONE_ORE}, with no hazard gating of its own (see that tag's comment).
     *
     * <p>Filters here (not left to each caller) so {@link #hasVacuumTarget} and
     * {@link #aggregateTick} can never disagree about what counts as a real target -- the same "one
     * filter, two callers" rule {@link #nearbyVacuumTargets} already follows for loose items.
     */
    @Nullable
    public static BlockHitResult aggregateTarget(Level level, Player player, ItemStack stack) {
        boolean fluidBypass = hasModule(stack, ModTags.Items.MODULE_FLUID_BYPASS);
        Vec3 eyePos = player.getEyePosition();
        Vec3 endPos = eyePos.add(player.getLookAngle().scale(RADIUS));

        ClipContext.Fluid fluidMode = fluidBypass ? ClipContext.Fluid.NONE : ClipContext.Fluid.ANY;
        BlockHitResult hit = level.clip(new ClipContext(eyePos, endPos,
                ClipContext.Block.COLLIDER, fluidMode, player));
        if (hit.getType() != HitResult.Type.BLOCK) return null;

        HazardProfile profile = installedHazardProfile(stack);
        if (fluidBypass && !fluidPathTolerated(level, eyePos, hit.getLocation(), profile)) return null;

        BlockState targetState = level.getBlockState(hit.getBlockPos());
        if (hasModule(stack, ModTags.Items.MODULE_AGGREGATE)) {
            if (targetState.is(ModTags.Blocks.AGGREGATE)) return hit;
            if (targetState.is(ModTags.Blocks.AGGREGATE_HOT)
                    && profile.tolerated().contains(ModTags.Fluids.THERMAL)) return hit;
            if (targetState.is(ModTags.Blocks.AGGREGATE_METAPHYSICAL)
                    && profile.tolerated().contains(ModTags.Fluids.METAPHYSICAL_MILD)) return hit;
        }
        if (hasModule(stack, ModTags.Items.MODULE_BEAM) && targetState.is(ModTags.Blocks.STONE_ORE)) return hit;
        return null;
    }

    /** Distance between fluid samples along the raycast when Fluid Bypass is checking what it's
     * passing through -- fine enough to not skip a thin fluid layer, coarse enough to stay cheap on
     * a per-tick held-trigger check. */
    private static final double FLUID_PATH_SAMPLE_STEP = 0.2;

    /** Whether every fluid block between {@code from} and {@code to} is tolerated by
     * {@code profile} -- Fluid Bypass alone (an empty, {@link HazardProfile#TIER_1}-shaped profile)
     * passes through non-hazardous fluid like water, but a hazardous one (lava, tagged
     * {@code THERMAL}) still blocks the ray unless a Safety Module granting that hazard is also
     * installed. Samples along the segment rather than reusing a single {@code ClipContext} call
     * since vanilla's fluid clipping has no per-hazard granularity to ask for directly. */
    private static boolean fluidPathTolerated(Level level, Vec3 from, Vec3 to, HazardProfile profile) {
        double length = from.distanceTo(to);
        int steps = Math.max(1, (int) Math.ceil(length / FLUID_PATH_SAMPLE_STEP));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.lerp(to, (double) i / steps);
            FluidState fluidState = level.getBlockState(BlockPos.containing(point)).getFluidState();
            if (fluidState.isEmpty()) continue;
            if (!profile.accepts(new FluidStack(fluidState.getType(), 1))) return false;
        }
        return true;
    }

    /** Union of {@link HazardProfile#TIER_1} (Eater has no permanent per-hazard tier profile yet,
     * see the design notes' open questions) with every hazard kind {@code eaterStack}'s
     * currently-installed Safety Modules grant. Thin wrapper over the shared
     * {@link IHaveModules#installedHazardProfile} (generalized 2026-08-19, see
     * dermicraft-progression-notes.md step 2) so every existing call site in this class keeps
     * calling a bare, Eater-specific {@code installedHazardProfile(stack)}. */
    private static HazardProfile installedHazardProfile(ItemStack eaterStack) {
        return IHaveModules.installedHazardProfile(eaterStack, ModDataComponentTypes.MODULE_DATA.get(),
                MODULE_SLOT_COUNT, HazardProfile.TIER_1);
    }

    /** Whether {@code eaterStack} currently has a Module tagged {@code moduleTag} installed in any
     * of its 3 slots -- the generic capability-query dispatch the Modules direction note describes.
     * Reads {@link ModDataComponentTypes#MODULE_DATA} directly rather than through the swap panel,
     * which only exists while a menu actually has this stack's panel open. Thin wrapper over the
     * shared {@link IHaveModules#hasModule}, same reasoning as {@link #installedHazardProfile}. */
    public static boolean hasModule(ItemStack eaterStack, TagKey<Item> moduleTag) {
        return IHaveModules.hasModule(eaterStack, ModDataComponentTypes.MODULE_DATA.get(), MODULE_SLOT_COUNT, moduleTag);
    }

    ////////////////////IWorkbenchSwappable (Scrench field / Workbench station swap panel)\\\\\\\\\\\\\\\\\\\\

    // Panel layout -- public so ScrenchScreen/WorkbenchScreen can draw the matching backgrounds
    // under exactly the slots EaterSwapPanel builds, without either screen needing to know Eater's
    // internal panel shape beyond these coordinates.
    public static final int MODULE_SLOT_X = 8;
    public static final int MODULE_SLOT_Y = 27;
    public static final int MODULE_SLOT_SPACING = 20;

    // A horizontal row directly above the player's own inventory grid (which starts at y=83, see
    // AbstractModScreen#PLAYER_INVENTORY_Y), not a vertical column -- a column of 4 starting at
    // y=20 was bleeding into the inventory art by the last slot (20+3*20=80, right against 83).
    // X is anchored so the row's LAST slot lines up with the inventory's own last (9th) column, and
    // the pitch matches the inventory's own 18px column spacing -- reads as a continuation of the
    // same grid rather than an unrelated strip. A dividing line renders just below this row and
    // above the inventory (see ScrenchScreen/WorkbenchScreen) to keep the two visually separate
    // despite sharing a column rhythm.
    public static final int BUFFER_SLOT_Y = 60;
    public static final int BUFFER_SLOT_SPACING = 18;
    public static final int BUFFER_SLOT_X = 151 - (SLOT_COUNT - 1) * BUFFER_SLOT_SPACING;

    @Override
    public SwapPanel openSwapPanel(java.util.function.Supplier<ItemStack> gadgetStackSupplier, Player player, boolean fieldHosted) {
        return new EaterSwapPanel(gadgetStackSupplier, fieldHosted);
    }

    /**
     * Eater's Module + buffer panel. Unlike Sunder's copy-out/copy-back panel, this is a pure live
     * view -- both the 3 Module slots and the item buffer slots read/write straight through to the
     * Eater stack's own data components ({@link ModDataComponentTypes#MODULE_DATA}/
     * {@link ModDataComponentTypes#BULK_ITEM_DATA} via {@link IHaveItemData.BulkItemHandler}), so
     * there's nothing to materialize or write back on close -- no "what if the GUI closes weird"
     * cases to design around at all, since nothing is ever out of sync with the real stack.
     *
     * <p>Re-resolves a fresh {@code BulkItemHandler} against {@code gadgetStackSupplier.get()} on
     * every access (see {@link IHaveItemData#liveHandler}) rather than binding to one captured
     * stack -- required for the Workbench host, whose working-item slot can be swapped out entirely
     * while the menu stays open (see {@link IWorkbenchSwappable#openSwapPanel}'s own javadoc for why).
     *
     * <p>{@code onClosed} only has one job: apply the recalibration cooldown, and only if a Module
     * slot's contents actually changed THIS session (tracked via {@link #moduleSlotChanged}) and
     * this session was field-hosted (the Workbench stays costless).
     *
     * <p>Layout coordinates below are first-pass placeholders -- exact positions get finalized once
     * the actual screen art is built (see the Eater GUI design discussion); functionally correct
     * either way since slot behavior doesn't depend on where they're drawn.
     */
    private final class EaterSwapPanel implements SwapPanel {

        private final java.util.function.Supplier<ItemStack> gadgetStackSupplier;
        private final boolean fieldHosted;
        private final IItemHandlerModifiable moduleHandler;
        private final IItemHandlerModifiable bufferHandler;
        private boolean moduleSlotChanged = false;

        private EaterSwapPanel(java.util.function.Supplier<ItemStack> gadgetStackSupplier, boolean fieldHosted) {
            this.gadgetStackSupplier = gadgetStackSupplier;
            this.fieldHosted = fieldHosted;
            this.moduleHandler = IHaveItemData.liveHandler(() -> new IHaveItemData.BulkItemHandler(gadgetStackSupplier.get(),
                    ModDataComponentTypes.MODULE_DATA.get(), MODULE_SLOT_COUNT, MODULE_SLOT_CAPACITY,
                    stack -> stack.is(ModTags.Items.MODULES)));
            this.bufferHandler = IHaveItemData.liveHandler(() -> new IHaveItemData.BulkItemHandler(gadgetStackSupplier.get(), SLOT_COUNT, SLOT_CAPACITY));
        }

        @Override
        public List<Slot> slots(int panelX, int panelY, java.util.function.BooleanSupplier active) {
            List<Slot> slots = new java.util.ArrayList<>(IHaveModules.buildModuleSlots(moduleHandler, MODULE_SLOT_COUNT,
                    panelX + MODULE_SLOT_X + 1, panelY + MODULE_SLOT_Y + 1, MODULE_SLOT_SPACING,
                    active, () -> moduleSlotChanged = true));

            for (int i = 0; i < SLOT_COUNT; i++) {
                int x = panelX + BUFFER_SLOT_X + 1 + i * BUFFER_SLOT_SPACING;
                int y = panelY + BUFFER_SLOT_Y + 1;
                slots.add(new SlotItemHandler(bufferHandler, i, x, y) {
                    @Override
                    public boolean isActive() {
                        return active.getAsBoolean();
                    }
                });
            }

            return slots;
        }

        @Override
        public void onClosed(Player player) {
            if (fieldHosted && moduleSlotChanged) {
                player.getCooldowns().addCooldown(EaterItem.this, SWAP_RECALIBRATION_COOLDOWN_TICKS);
            }
        }
    }

    ////////////////////Gadget health\\\\\\\\\\\\\\\\\\\\

    @Override
    public void onGadgetDeath(ServerLevel level, ItemEntity entity, ItemStack stack) {
        IGadget.deathFlourish(level, entity, ParticleTypes.POOF, 20, 0.2,
                SoundEvents.GHAST_HURT, 0.9F, 1.1F);
    }

    ////////////////////Tooltip\\\\\\\\\\\\\\\\\\\\

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dermicraft.eater.mode",
                        Component.translatable(modeKey(modeData(stack).mode())))
                .withStyle(ChatFormatting.GRAY));
    }

    ////////////////////Helpers\\\\\\\\\\\\\\\\\\\\

    public static DrinkerModeData modeData(ItemStack stack) {
        return stack.getOrDefault(ModDataComponentTypes.EATER_MODE_DATA.get(), DrinkerModeData.DEFAULT);
    }

    /** Always {@link #SLOT_COUNT} long, regardless of what's actually stored -- lets the screen
     * controllers below index straight into it without a bounds check of their own. */
    public static net.scruffy.dermicraft.component.BulkItemData bufferData(ItemStack stack) {
        return stack.getOrDefault(ModDataComponentTypes.BULK_ITEM_DATA.get(),
                net.scruffy.dermicraft.component.BulkItemData.empty(SLOT_COUNT)).withSize(SLOT_COUNT);
    }

    private static String modeKey(DrinkerModeData.Mode mode) {
        return switch (mode) {
            case DISPOSAL -> "tooltip.dermicraft.eater.mode.disposal";
            case STORAGE -> "tooltip.dermicraft.eater.mode.storage";
            case TRANSFER -> "tooltip.dermicraft.eater.mode.transfer";
        };
    }

    ////////////////////Animation\\\\\\\\\\\\\\\\\\\\

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Same shape as DRINKER's own Body controller: play "activate" once into a held
        // "active_hold" loop while vacuuming, otherwise fall back to "idle" (which also drives the
        // rib flutter). Only one direction is authored -- no "deactivate" clip -- so coming off
        // active_hold back to idle relies on transitionLength to blend the mouth covers back down,
        // same as the screen bones' retract.
        controllers.add(new AnimationController<>(this, "Body", 0, state -> {
            ItemStack stack = state.getData(software.bernie.geckolib.constant.DataTickets.ITEMSTACK);
            boolean vacuuming = stack != null
                    && stack.getOrDefault(ModDataComponentTypes.EATER_VACUUMING.get(), false);
            return state.setAndContinue(vacuuming
                    ? RawAnimation.begin().thenPlay("activate").thenLoop("active_hold")
                    : RawAnimation.begin().thenLoop("idle"));
        }).transitionLength(4));

        // One controller per screen bone ("1".."4"), each independently toggled by whether its
        // matching buffer slot holds an item. Only ever plays its own "screen_N" clip once and
        // holds the flush pose -- retracting back to recessed is deliberately NOT a second
        // authored clip (see the design discussion: the slide is too small to be worth two clips),
        // it's just the controller switching to a no-op animation and letting transitionLength
        // blend the bone back toward its rest/recessed position.
        //
        // Deliberately "none", NOT "idle": idle carries the mouth-cover/rib flutter keyframes, and
        // since GeckoLib doesn't blend bone writes across separate controllers, every empty
        // screen's controller falling back to "idle" was silently re-applying idle's bl/tl/br/tr
        // pose AFTER the Body controller's own pass each frame -- permanently masking the
        // activate/active_hold animation. "none" touches no bones at all, so it can't collide.
        for (int i = 1; i <= SLOT_COUNT; i++) {
            int slotIndex = i - 1;
            String clip = "screen_" + i;
            controllers.add(new AnimationController<>(this, "Screen" + i, 5, state -> {
                ItemStack stack = state.getData(software.bernie.geckolib.constant.DataTickets.ITEMSTACK);
                boolean occupied = stack != null && !bufferData(stack).slot(slotIndex).isEmpty();
                return state.setAndContinue(occupied
                        ? RawAnimation.begin().thenPlayAndHold(clip)
                        : RawAnimation.begin().thenLoop("none"));
            }).transitionLength(4));
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
