package net.scruffy.dermicraft.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.scruffy.dermicraft.component.DrinkerModeData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.interfaces.IGadget;
import net.scruffy.dermicraft.interfaces.IHaveItemData;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

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
public class EaterItem extends Item implements GeoItem, IGadget, IHaveItemData {

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

        if (hasVacuumTarget(level, player)) {
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

    /** Existence-only check, same scan as the real vacuum tick -- cheap enough to run on every
     * right-click since it's just an AABB scan plus a dot-product filter, no routing/mutation. */
    private boolean hasVacuumTarget(Level level, Player player) {
        return !nearbyVacuumTargets(level, player).isEmpty();
    }

    /** Items within {@link #RADIUS} AND inside the forward cone -- the single filter both the
     * targetless-click check and the real vacuum tick scan through, so they can never disagree
     * about what counts as reachable. */
    private static List<ItemEntity> nearbyVacuumTargets(Level level, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();

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
            if (lengthSq < 1.0E-4 || toTarget.normalize().dot(look) >= CONE_COS_THRESHOLD) {
                targets.add(candidate);
            }
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
        if (holdingTrigger) protectVacuumCandidates(level, player);

        boolean pastWindup = holdingTrigger && player.getTicksUsingItem() >= VACUUM_WINDUP_TICKS;
        if (pastWindup) vacuumTick(level, player, stack);

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
     * business with them -- see the windup comment in {@link #inventoryTick}. */
    private static void protectVacuumCandidates(Level level, Player player) {
        for (ItemEntity candidate : nearbyVacuumTargets(level, player)) {
            candidate.setPickUpDelay(PULL_PICKUP_DELAY_TICKS);
        }
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
        List<ItemEntity> nearby = nearbyVacuumTargets(level, player);
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
