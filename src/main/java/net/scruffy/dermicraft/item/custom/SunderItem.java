package net.scruffy.dermicraft.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.effect.ModEffects;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.HeldItemData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.component.SunderModeData;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.interfaces.IGadget;
import net.scruffy.dermicraft.interfaces.IHaveFluidData;
import net.scruffy.dermicraft.interfaces.IHaveItemData;
import net.scruffy.dermicraft.interfaces.IHaveModules;
import net.scruffy.dermicraft.interfaces.IWorkbenchSwappable;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.scruffy.dermicraft.property.ChainProperties;
import net.scruffy.dermicraft.screen.custom.scrench.ScrenchMenu;
import net.scruffy.dermicraft.util.AutoSmeltUtil;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Rev state machine (hold right-click) plus SAWING -- a revved hit locks onto a valid target and
 * pulses damage/fuel-then-hunger drain/chain wear automatically until it dies, the time cap hits,
 * resources run dry, or the player is knocked back (see {@code SunderEvents}). See the Sunder
 * design notes in {@code dermicraft-gadget-notes.md} for the full mechanic; numbers here are a
 * first pass, not tuned.
 *
 * <p>Held-trigger shape (getUseAnimation/getUseDuration/shouldCauseReequipAnimation,
 * inventoryTick reading {@code player.isUsingItem()}) mirrors {@code EaterItem} exactly -- same
 * "hold right-click" identity, same equip-animation-thrash guard.
 *
 * <p>Rev-down has no authored reverse animation -- GeckoLib 4.8.4 has no true reverse playback (see
 * the design notes). {@code EaterItem}'s Body controller already proved the substitute: switch the
 * controller's target back to a rest-pose animation and let {@code transitionLength} blend the
 * bones back down, which reads the same as a reverse for a simple flex/extend clip like
 * {@code rev_up_down}.
 */
public class SunderItem extends Item implements GeoItem, IHaveFluidData, IGadget, IWorkbenchSwappable, IHaveModules {

    /** Placeholder capacity -- fuel-drain-rate-per-pulse and every other fuel-economy number are
     * still open design questions (see the notes), so this is a round default to build against,
     * not a tuned value. Matches Drinker/Sipping's own 1000mB default for consistency. */
    public static final int FUEL_CAPACITY = 1000;

    /** Gadget health, expressed as vanilla durability -- see {@link IGadget}. Registered via
     * {@code Item.Properties#durability}, which is the single source of truth for max HP. Purely
     * drop-damage-based, same as every other gadget -- entirely independent of chain durability
     * (which wears from combat use, not impact) and untouched by combat itself. */
    public static final int MAX_HP = 10;

    /** Base (pre-chain/tier/fuel/points) attack damage modifier -- 5.0, matching vanilla's own
     * Iron Sword exactly (3.0 base + Iron's 2.0 tier bonus, per {@code SwordItem.createAttributes}).
     * Deliberately pinned to IRON rather than stone: the Iron chain is the baseline every other
     * chain material is defined relative to (see the design notes), so Sunder's own base tracks the
     * same metal for consistency. */
    private static final float BASE_ATTACK_DAMAGE = 5.0F;

    /** Dropped from -2.6F to -3.0F (2026-08-12) -- 1.4 attacks/second read as too fast for a
     * chainsaw-style Weapon in practice; -3.0F lands at 1.0 attacks/second (base 4.0 - 3.0) instead. */
    private static final float BASE_ATTACK_SPEED = -3.0F;

    /** Damage penalty applied when no chain is mounted -- a fraction of total, so -0.6 means the
     * swing lands at 40% strength. Placeholder magnitude; the notes only commit to "a large
     * standard-hit damage reduction, still functions as a much weaker plain sword." */
    private static final float NO_CHAIN_DAMAGE_PENALTY = -0.6F;

    private static final ResourceLocation BASE_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "base_attack_damage");
    private static final ResourceLocation BASE_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "base_attack_speed");
    private static final ResourceLocation CHAIN_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "chain_damage");

    /**
     * Per-stack combat stats: Sunder's own base, plus the mounted chain's damage multiplier (or the
     * no-chain penalty if it's missing/broken).
     *
     * <p>Overriding the {@code ItemStack}-sensitive form rather than baking modifiers into
     * {@code Item.Properties#attributes} is required, not stylistic -- NeoForge only consults this
     * when the stack has no {@code ATTRIBUTE_MODIFIERS} component, and {@code Properties#attributes}
     * sets exactly that component as an item default, which would shadow this override entirely and
     * freeze the stats at their chainless values.
     *
     * <p>The chain's multiplier is emitted as {@code ADD_MULTIPLIED_BASE}, which is what the stat
     * layering model in the design notes calls for -- and it works here because vanilla's
     * {@code AttributeInstance} applies that operation against the base value <em>plus all
     * {@code ADD_VALUE} modifiers</em> (so it scales Sunder's own base damage, not just the
     * player's bare-fist 1.0). Tier and fuel are designed to join this same summed layer later, and
     * points the {@code ADD_MULTIPLIED_TOTAL} one -- vanilla does that combining itself, so there's
     * no hand-rolled damage formula to keep in sync.
     */
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                                BASE_DAMAGE_ID, BASE_ATTACK_DAMAGE, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(
                                BASE_SPEED_ID, BASE_ATTACK_SPEED, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND);

        ChainProperties chain = chainProperties(stack);
        // Iron (the baseline material) is exactly 1.0, which contributes nothing -- skip emitting a
        // no-op modifier so it doesn't show as a meaningless "+0%" tooltip line.
        float damageShift = chain == null ? NO_CHAIN_DAMAGE_PENALTY : chain.damageMultiplier() - 1.0F;
        if (damageShift != 0.0F) {
            builder.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                            CHAIN_DAMAGE_ID, damageShift, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    EquipmentSlotGroup.MAINHAND);
        }

        return builder.build();
    }

    /**
     * The mounted chain's material stats, or {@code null} when nothing is mounted -- which is the
     * same state a broken chain leaves behind, so callers get the chainless behavior for free
     * without a separate "is it broken" check.
     */
    @Nullable
    public static ChainProperties chainProperties(ItemStack sunderStack) {
        ItemStack chain = mountedChain(sunderStack).itemStack();
        if (chain.isEmpty()) return null;
        return BuiltInRegistries.ITEM.wrapAsHolder(chain.getItem()).getData(ModDataMaps.SUNDER_CHAIN_PROPERTIES);
    }

    /** Ticks the trigger must be held before anything visibly changes -- see SunderModeData.State.ARM_DELAY. */
    private static final long ARM_DELAY_TICKS = 5;
    /** Ticks after release before the wind-down starts -- see SunderModeData.State.RELEASE_DELAY. */
    private static final long RELEASE_DELAY_TICKS = 10;
    /** How long the transition-blend wind-down takes -- matches rev_up_down's own 0.25s (5-tick)
     * length, and is also the controller's transitionLength below, so the chain bone swap back to
     * idle lines up with roughly when the blend finishes. Package-visible (not private) because
     * {@code SunderGlowLayer} also needs rev_up_down's length -- to know when ACTIVE has moved past
     * the one-shot clip and settled into the "running" hold loop, rather than duplicating this
     * number in a second file where the two copies could drift apart. */
    static final long UNREV_TICKS = 5;

    /** Effectively "until released" -- same trick DRINKER/EATER's held pose uses. */
    private static final int HELD_INDEFINITELY = 72000;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** Placeholder duration -- not tuned, see the design notes' open questions. */
    private static final int BLEED_DURATION_TICKS = 100;

    ////////////////////Sawing\\\\\\\\\\\\\\\\\\\\ (placeholders, tune during testing)

    /** Ticks between pulses -- damage/fuel-then-hunger drain/chain wear all fire together on each. */
    private static final long SAW_PULSE_COOLDOWN_TICKS = 15;
    private static final float SAW_PULSE_DAMAGE = 3.0F;
    /** mB drained from the fuel tank per pulse. A clean fuel-then-hunger switchover, not a blend --
     * a pulse's full cost comes from fuel if there's enough, otherwise the whole cost shifts to
     * hunger instead, matching "continues on hunger" as a fallback resource, not a mixed one. */
    private static final int SAW_FUEL_PER_PULSE = 25;
    /** Hunger points drained per pulse once fuel can't cover it. */
    private static final int SAW_HUNGER_PER_PULSE = 1;
    /** Hard anti-grind cap on a single mob lock -- 10 seconds. */
    private static final long SAW_MOB_TIME_CAP_TICKS = 200;
    /** 1 second, per the design notes -- not a placeholder, the notes give this number directly. */
    private static final long BLEED_GUARANTEE_TICKS = 20;
    private static final int CHAIN_WEAR_PER_PULSE = 1;
    /** Blocks in front of the player the locked target is held at. */
    private static final double SAW_LOCK_DISTANCE = 2.0;
    /** Fraction of the remaining distance to the lock point closed per tick -- eased like Eater's
     * item-pull rather than a rigid teleport, so hit knockback settles back into place smoothly
     * instead of being fought with a hard snap every tick. */
    private static final double SAW_LOCK_PULL_STRENGTH = 0.5;
    /** Raycast distance for acquiring a SAWING target -- a fixed melee-length reach rather than
     * {@code player.entityInteractionRange()} (vanilla's own reach attribute, which is gamemode-
     * dependent -- creative's ~5 blocks read as "grabbing mobs from way too far away"). */
    private static final double SAW_TARGET_RANGE = 2.0;

    ////////////////////Tree Felling\\\\\\\\\\\\\\\\\\\\ (placeholders, tune during testing)

    /** Health cost per detected log -- total tree health is this times the flood-fill's log count.
     * Pulses deal the same {@link #SAW_PULSE_DAMAGE} combat uses, no separate tree damage stat. */
    private static final int TREE_LOG_HEALTH_COST = 15;
    /** Safety cap on the flood-fill's scanned-block count -- a performance/technical guard against a
     * touching-canopy chain reaction across a whole forest, not a gameplay number. Whatever's found
     * by the time this is hit is treated as "the whole tree." */
    private static final int TREE_SCAN_CAP = 64;

    public SunderItem(Properties properties) {
        super(properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    ////////////////////Gadget HP\\\\\\\\\\\\\\\\\\\\

    @Override
    public void onGadgetDeath(ServerLevel level, ItemEntity entity, ItemStack stack) {
        IGadget.deathFlourish(level, entity, ParticleTypes.CRIT, 20, 0.2,
                SoundEvents.GHAST_HURT, 0.8F, 0.6F);
        IGadget.deathFlourish(level, entity, ParticleTypes.SMOKE, 14, 0.2,
                SoundEvents.ANVIL_BREAK, 0.6F, 1.0F);
    }

    ////////////////////Combat\\\\\\\\\\\\\\\\\\\\

    /**
     * Standard-hit Bleed roll only -- SAWING starts via auto-targeting in {@code inventoryTick}
     * while ACTIVE, not from a left-click hit here. Vanilla's own client blocks the attack key
     * entirely while {@code isUsingItem()} is true ({@code Minecraft.java}'s input tick drains the
     * attack-click queue without calling {@code startAttack()} whenever the player is using an
     * item) -- since revving relies on {@code startUsingItem} for hold-tracking, left-click flat out
     * never reaches the server while revved, so a revved branch here would be dead code, not a
     * fallback. Decapitation is handled separately (see {@code SunderEvents}), since it only matters
     * on the killing blow, not every hit.
     *
     * <p>A missing/broken chain rolls a flat 0% here for free: {@link #chainProperties} returns
     * {@code null} in that case, and {@code chain == null} short-circuits before any roll happens.
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        ChainProperties chain = chainProperties(stack);
        if (chain != null && !target.getType().is(ModTags.EntityTypes.NOT_BLEEDABLE)
                && attacker.getRandom().nextFloat() < chain.bleedChance()) {
            target.addEffect(new MobEffectInstance(ModEffects.BLEED, BLEED_DURATION_TICKS));
        }

        // Blaze Essence's ignite-on-hit trait -- standard-hit roll only; SAWING's own guaranteed
        // version lives in tickSawing instead, see ChainProperties' own javadoc for why.
        if (chain != null && chain.igniteChance() > 0.0f
                && attacker.getRandom().nextFloat() < chain.igniteChance()) {
            target.igniteForSeconds(chain.igniteFireSeconds());
        }

        return super.hurtEnemy(stack, target, attacker);
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

    /** See DrinkerItem/EaterItem's identical override -- SUNDER_MODE_DATA rewrites on every state
     * transition while active, and the default equals-check would otherwise thrash the equip
     * animation. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    /** Checks the other hand for a paired Scrench first, deferring to the maintenance GUI if found
     * -- Sunder's own use() always consumes the click (never PASS), so without this check a Sunder
     * held in the main hand would silently swallow every right-click and the Scrench (off hand)
     * would never get a chance to open its menu. See {@link ScrenchItem}'s matching check for the
     * reverse hand ordering, and {@link ScrenchMenu#open} for the shared menu-opening logic both
     * directions call. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (player.getItemInHand(otherHand).getItem() instanceof ScrenchItem) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                ScrenchMenu.open(serverPlayer, hand);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // Reverse ordering of BladderItem's own refuel-shortcut check -- see that method's javadoc.
        if (player.getItemInHand(otherHand).getItem() instanceof BladderItem) {
            if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
            if (BladderItem.tryFillFuelGadget(player, otherHand, hand)) {
                return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), false);
            }
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    ////////////////////State machine\\\\\\\\\\\\\\\\\\\\

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;

        boolean holdingTrigger = player.isUsingItem() && player.getUseItem() == stack;
        SunderModeData mode = stack.getOrDefault(ModDataComponentTypes.SUNDER_MODE_DATA.get(), SunderModeData.DEFAULT);
        long now = level.getGameTime();
        long elapsed = now - mode.since();

        // SAWING has its own tick logic (pulses, resource draw, and either position lock or a
        // flood-filled log set depending on target type) instead of a single state-transition
        // expression -- handled separately rather than folded into the switch below. Dispatches on
        // which of target/treeOrigin is present -- see SunderModeData's own javadoc for why both
        // share the same SAWING ordinal instead of a separate state each.
        if (mode.stateEnum() == SunderModeData.State.SAWING) {
            if (mode.target().isPresent()) {
                tickSawing(stack, (ServerLevel) level, player, mode, now, elapsed, holdingTrigger);
            } else {
                tickFelling(stack, (ServerLevel) level, player, mode, now, elapsed, holdingTrigger);
            }
            return;
        }

        SunderModeData next = switch (mode.stateEnum()) {
            // "Requires fuel to start" (design notes) gates only the INITIAL trigger -- once armed,
            // sustaining an already-started rev doesn't re-check the tank here (SAWING's own fuel
            // drain is what would ever empty it mid-sequence).
            case IDLE -> holdingTrigger && hasFuel(stack) ? SunderModeData.of(SunderModeData.State.ARM_DELAY, now) : null;
            case ARM_DELAY -> !holdingTrigger
                    ? SunderModeData.of(SunderModeData.State.IDLE, now) // released before the swap -- nothing to wind down
                    : elapsed >= ARM_DELAY_TICKS ? SunderModeData.of(SunderModeData.State.ACTIVE, now) : null;
            // Auto-swing, not left-click: vanilla's client blocks the attack key entirely while
            // isUsingItem() is true (see the class javadoc), so left-click can never reach the
            // server while revved. No extra wait past ARM_DELAY here -- the mechanical attack is
            // meant to land 5 ticks after the initial right-click (ARM_DELAY_TICKS's own length),
            // not stacked behind a second delay for the rev-up animation too. Every ACTIVE tick
            // checks whether the player is aiming at a valid target within melee reach and starts
            // SAWING automatically if so -- this is what stands in for "landing a hit" in the design
            // notes now that a real hit can't be thrown here.
            case ACTIVE -> {
                if (!holdingTrigger) yield SunderModeData.of(SunderModeData.State.RELEASE_DELAY, now);

                // "attack" fires once here, on the mechanical attack actually starting -- not per
                // pulse (see tickSawing/tickFelling, which only fire "saw"). Blends "main" straight
                // into the held struck pose via the controller's transitionLength (see
                // registerControllers for why this is a coded blend rather than an authored swing-in
                // clip); "saw"'s per-pulse chain wobble is what carries the repeated-hit feel for the
                // rest of the lock-on, mob or tree alike.
                LivingEntity autoTarget = findSawTarget((ServerLevel) level, player);
                if (autoTarget != null) {
                    triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerLevel) level), "Swing", "attack");
                    yield SunderModeData.sawing(autoTarget.getUUID(), now);
                }

                // No mob in reach -- try a log next, same reach, same trigger shape. Mobs are
                // checked first so a mob standing in front of a tree still wins the auto-target.
                // Requires an actual chain to even start -- unlike a mob (a broken/missing chain
                // still swings as "a much weaker plain sword," per the design notes), there's no
                // bare-bar equivalent for cutting wood, so no chain means no felling at all.
                if (chainProperties(stack) != null) {
                    BlockPos fellTarget = findFellTarget((ServerLevel) level, player);
                    if (fellTarget != null) {
                        triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerLevel) level), "Swing", "attack");
                        yield SunderModeData.felling(fellTarget, now);
                    }
                }

                yield null;
            }
            // Re-holding during the wind-down is ignored for this basic pass -- always let it finish.
            case RELEASE_DELAY -> elapsed >= RELEASE_DELAY_TICKS
                    ? SunderModeData.of(SunderModeData.State.UNREVVING, now) : null;
            case UNREVVING -> elapsed >= UNREV_TICKS ? SunderModeData.of(SunderModeData.State.IDLE, now) : null;
            case SAWING -> null; // unreachable -- handled by the early return above
        };

        if (next != null) stack.set(ModDataComponentTypes.SUNDER_MODE_DATA.get(), next);
    }

    /** Non-empty, not any specific amount -- dig-in's exact fuel-cost-per-pulse is still an open
     * number (see the design notes), so there's nothing yet to require more than "some." */
    private boolean hasFuel(ItemStack stack) {
        return !stack.getOrDefault(getDataType(), FluidData.EMPTY).isFluidEmpty();
    }

    /** Raycast along the player's look vector out to {@link #SAW_TARGET_RANGE} -- deliberately not
     * {@code entityInteractionRange()} (see that constant's javadoc). {@code
     * ProjectileUtil.getEntityHitResult} is the standard vanilla helper for "what entity am I
     * looking at," the same shape vanilla's own attack-target detection uses internally. */
    @Nullable
    private LivingEntity findSawTarget(ServerLevel level, Player player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(SAW_TARGET_RANGE));
        AABB searchArea = player.getBoundingBox().expandTowards(lookVec.scale(SAW_TARGET_RANGE)).inflate(0.5);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eyePos, endPos, searchArea,
                candidate -> candidate instanceof LivingEntity living && living.isAlive() && candidate != player);

        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    /**
     * One SAWING tick: locks the target every tick regardless of whether this is a pulse tick, then
     * fires a pulse (damage + resource draw + chain wear) whenever {@code elapsed} lands on a pulse
     * boundary -- {@code elapsed % SAW_PULSE_COOLDOWN_TICKS == 0} is true at elapsed=0 too, which is
     * what delivers "damage at the start of the pulse" for free on the very first tick after
     * {@code hurtEnemy} starts SAWING, without hurtEnemy needing to apply a first pulse itself.
     *
     * <p>Ends back to ACTIVE (still holding) or RELEASE_DELAY (let go) rather than through a single
     * shared exit -- never straight to IDLE -- since exiting SAWING isn't the same event as choosing
     * to stop revving.
     */
    private void tickSawing(ItemStack stack, ServerLevel level, Player player, SunderModeData mode,
                             long now, long elapsed, boolean holdingTrigger) {
        LivingEntity target = resolveTarget(level, mode);

        if (!holdingTrigger || target == null || elapsed >= SAW_MOB_TIME_CAP_TICKS) {
            endSawing(stack, level, player, target, elapsed, holdingTrigger, now);
            return;
        }

        lockTarget(level, player, target);

        if (elapsed % SAW_PULSE_COOLDOWN_TICKS != 0) return;

        if (!payForPulse(stack, player)) {
            // Resources exhausted -- ends immediately, no damage for this pulse.
            endSawing(stack, level, player, target, elapsed, holdingTrigger, now);
            return;
        }

        target.hurt(player.damageSources().playerAttack(player), SAW_PULSE_DAMAGE);
        // Blaze Essence's ignite-on-hit trait -- guaranteed every pulse while sawing (not a chance
        // roll like the standard-hit version in hurtEnemy), which is also what gives "burns through
        // the whole attack plus igniteFireSeconds after it ends" for free -- see ChainProperties'
        // own javadoc for why no separate duration tracking is needed.
        ChainProperties sawChain = chainProperties(stack);
        if (sawChain != null && sawChain.igniteChance() > 0.0f) {
            target.igniteForSeconds(sawChain.igniteFireSeconds());
        }
        // LivingEntity#hurt applies a fixed 0.4-strength knockback itself for any damage source with
        // a position, entirely independent of Player#attack's own knockback logic -- since this
        // calls hurt() directly rather than going through the normal attack path, that vanilla
        // knockback still fires and can start moving the target before lockTarget's next tick ever
        // sees it. Canceling it immediately, same tick, keeps the position lock from getting a full
        // tick's head start to fight against.
        target.setDeltaMovement(Vec3.ZERO);
        wearChain(stack);
        // Vanilla's own arm-swing render path is unreachable here regardless -- ItemInHandRenderer
        // only ever applies the static held-item transform while isUsingItem() is true (with
        // UseAnim.NONE), never the attack-swing transform, so the per-pulse hit feedback has to come
        // entirely from the item's own GeckoLib model animation instead of the player arm. "swing"
        // itself already fired once when SAWING started (see the ACTIVE case above) and is holding
        // in "attack" for the whole locked-on duration -- only "saw"'s chain wobble repeats per pulse.
        triggerAnim(player, GeoItem.getOrAssignId(stack, level), "Body", "saw");
    }

    @Nullable
    private LivingEntity resolveTarget(ServerLevel level, SunderModeData mode) {
        if (mode.target().isEmpty()) return null;
        Entity resolved = level.getEntity(mode.target().get());
        return resolved instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    ////////////////////Tree Felling\\\\\\\\\\\\\\\\\\\\

    /** Block-raycast counterpart to {@link #findSawTarget} -- same {@link #SAW_TARGET_RANGE}, same
     * "log" identity check ({@code BlockTags.LOGS}, matching the flood-fill's own filter) as what
     * would actually get felled. Checked second in the ACTIVE case, after mobs, so a mob standing in
     * front of a tree doesn't get shadowed by the trunk behind it. */
    @Nullable
    private BlockPos findFellTarget(ServerLevel level, Player player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(SAW_TARGET_RANGE));

        BlockHitResult hit = level.clip(new ClipContext(eyePos, endPos,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) return null;

        return level.getBlockState(hit.getBlockPos()).is(BlockTags.LOGS) ? hit.getBlockPos() : null;
    }

    /**
     * Standard tree-capitator-style contiguous log detection from the hit log -- logs only, leaves
     * untouched. Widened past the vanilla-mod-convention 6-face adjacency to 26-neighbor (diagonal-
     * inclusive), plus a single-air-gap hop (one block past an immediate air neighbor, same
     * direction) specifically to catch detached/diagonal branches that don't directly touch the main
     * trunk. Capped at {@link #TREE_SCAN_CAP} scanned logs -- a performance guard against a touching-
     * canopy chain reaction across a whole forest, not a gameplay number; whatever's found by the cap
     * is treated as "the whole tree."
     *
     * <p>Returned sorted top-down (highest Y first) -- this is what lets {@link #endFelling} take
     * "the top N logs" for a partial harvest without needing any separate ordering data stored
     * anywhere. Recomputed fresh every call rather than cached anywhere (see SunderModeData's own
     * javadoc for why) -- bounded by the scan cap, so this is cheap enough to re-run every tick.
     */
    private List<BlockPos> floodFillLogs(ServerLevel level, BlockPos origin) {
        if (!level.getBlockState(origin).is(BlockTags.LOGS)) return List.of();

        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> found = new ArrayList<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(origin);
        queue.add(origin);

        while (!queue.isEmpty() && found.size() < TREE_SCAN_CAP) {
            BlockPos current = queue.poll();
            found.add(current);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        if (visited.size() >= TREE_SCAN_CAP) continue;

                        BlockPos neighbor = current.offset(dx, dy, dz);
                        if (visited.contains(neighbor)) continue;

                        if (level.getBlockState(neighbor).is(BlockTags.LOGS)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        } else if (level.getBlockState(neighbor).isAir()) {
                            BlockPos gapHop = neighbor.offset(dx, dy, dz);
                            if (!visited.contains(gapHop) && level.getBlockState(gapHop).is(BlockTags.LOGS)) {
                                visited.add(gapHop);
                                queue.add(gapHop);
                            }
                        }
                    }
                }
            }
        }

        found.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).reversed());
        return found;
    }

    /** Pulses needed to fully cut through one log -- derived from {@link #TREE_LOG_HEALTH_COST}
     * against the shared {@link #SAW_PULSE_DAMAGE} rather than a second independent number, so the
     * two stay in sync automatically if either is retuned. */
    private static final int TREE_PULSES_PER_LOG = Math.round(TREE_LOG_HEALTH_COST / SAW_PULSE_DAMAGE);

    /**
     * Tree-felling's counterpart to {@code tickSawing} -- same pulse cadence, resource draw, and
     * chain wear (no separate tree-specific numbers there), just no position lock (a tree doesn't
     * move). No mob time cap either -- trees have none, the fuel/hunger budget is the only limiter
     * (see the design notes).
     *
     * <p>Logs are consumed as cutting happens, not all at once at the end -- every {@link
     * #TREE_PULSES_PER_LOG}th pulse finishes cutting through the current top-most remaining log
     * (see {@code floodFillLogs}' top-down sort), which breaks immediately and drops at the player's
     * feet right then, same tree-feller-mod feedback loop rather than a silent damage counter with
     * nothing to show for it until the very end. This also means there's no separate "partial
     * harvest on interruption" case to handle -- whatever's already been cut is already in the
     * player's hands the moment SAWING stops for any reason (release, knockback, resources
     * exhausted, or the origin log itself gone -- {@code floodFillLogs} returning empty covers that
     * last one for free); nothing further needs doing on exit beyond the normal state transition.
     *
     * <p>Requires a mounted chain to even be reachable -- see the ACTIVE case's own gate -- but also
     * re-checked every tick here, not just at acquisition: if the chain breaks mid-cut (wear, same
     * as combat), cutting stops there rather than continuing on a bare bar that can't actually cut
     * wood.
     */
    private void tickFelling(ItemStack stack, ServerLevel level, Player player, SunderModeData mode,
                              long now, long elapsed, boolean holdingTrigger) {
        BlockPos origin = mode.treeOrigin().orElse(null);
        boolean hasChain = chainProperties(stack) != null;
        List<BlockPos> logs = (hasChain && origin != null) ? floodFillLogs(level, origin) : List.of();

        if (!holdingTrigger || logs.isEmpty()) {
            exitFellingState(stack, level, player, holdingTrigger, now);
            return;
        }

        if (elapsed % SAW_PULSE_COOLDOWN_TICKS != 0) return;

        if (!payForPulse(stack, player)) {
            exitFellingState(stack, level, player, holdingTrigger, now);
            return;
        }

        wearChain(stack);
        triggerAnim(player, GeoItem.getOrAssignId(stack, level), "Body", "saw");

        long pulsesLanded = elapsed / SAW_PULSE_COOLDOWN_TICKS + 1;
        if (pulsesLanded % TREE_PULSES_PER_LOG != 0) return; // still cutting through the current log

        BlockPos cutPos = logs.get(0);
        ItemStack rawDrop = new ItemStack(level.getBlockState(cutPos).getBlock());

        // Blaze Essence's other trait -- SAWING felling drops Charcoal (via a real SmeltingRecipe
        // lookup, same as Shatter's auto-smelt) instead of the raw Log, with the recipe's own XP
        // awarded too. Falls back to the raw log if no smelting recipe matches (shouldn't happen
        // for a real log, but safe regardless).
        ItemStack drop = rawDrop;
        ChainProperties fellingChain = chainProperties(stack);
        if (fellingChain != null && fellingChain.smeltsLogs()) {
            Optional<AutoSmeltUtil.SmeltResult> smelted = AutoSmeltUtil.smeltOne(level, rawDrop);
            if (smelted.isPresent()) {
                drop = smelted.get().result();
                AutoSmeltUtil.awardExperience(level, player.position(), smelted.get().experience());
            }
        }

        level.destroyBlock(cutPos, false, player);
        level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), drop));
        // If that was the last log, next tick's flood-fill from the (now-gone) origin comes back
        // empty and the early-return above ends SAWING cleanly -- no separate "tree complete" branch
        // needed.
    }

    /** Shared tail for every felling exit -- same "hand the Swing controller back to rest, then fall
     * back to ACTIVE/RELEASE_DELAY" shape {@code endSawing} uses, just without a Bleed-guarantee roll
     * (nothing to bleed on a tree) and without needing a partial-harvest payout (see {@code
     * tickFelling}'s own javadoc for why -- logs are already handed out as they're cut, not batched
     * for the end). Public, not private, since {@code SunderEvents}' knockback handler calls this
     * directly too. */
    public void exitFellingState(ItemStack stack, ServerLevel level, Player player, boolean holdingTrigger, long now) {
        triggerAnim(player, GeoItem.getOrAssignId(stack, level), "Swing", "swing_rest");
        SunderModeData.State nextState = holdingTrigger ? SunderModeData.State.ACTIVE : SunderModeData.State.RELEASE_DELAY;
        stack.set(ModDataComponentTypes.SUNDER_MODE_DATA.get(), SunderModeData.of(nextState, now));
    }

    /** Eased like Eater's item-pull, not a rigid teleport -- closes {@link #SAW_LOCK_PULL_STRENGTH}
     * of the remaining distance to the lock point each tick rather than snapping straight to it.
     * Hard-snapping was directly fighting hit knockback (canceling the whole displacement in one
     * frame reads as jarring); easing it back lets the knockback's own "bounce" read naturally while
     * still reeling the target back in over a couple ticks.
     *
     * <p>The move itself is skipped (leaves the target wherever it last validly was) if the eased
     * step would shove it into terrain -- {@code lockPos} is a raw offset from the player with no
     * regard for blocks in between, so without this check a player can walk/look the fixed point
     * straight into a wall or floor and pin the target there.
     *
     * <p>Velocity is zeroed unconditionally, separately from that check -- this is what keeps each
     * pulse's hit knockback from compounding pulse after pulse (see {@code tickSawing}'s {@code
     * target.hurt} call). Folding it into the same early-return as the move skip was a bug: the lock
     * point reads as "blocked" surprisingly easily on ordinary ground (it inherits the look vector's
     * pitch, so merely aiming slightly downward at a mob tips it a hair into the floor), and every
     * tick that skipped meant knockback went uncancelled entirely. */
    private void lockTarget(ServerLevel level, Player player, LivingEntity target) {
        Vec3 lockPos = player.position().add(player.getLookAngle().scale(SAW_LOCK_DISTANCE));
        Vec3 pulled = target.position().add(lockPos.subtract(target.position()).scale(SAW_LOCK_PULL_STRENGTH));
        AABB pulledBox = target.getBoundingBox().move(pulled.subtract(target.position()));
        if (level.noCollision(target, pulledBox)) {
            target.setPos(pulled.x, pulled.y, pulled.z);
        }

        target.setDeltaMovement(Vec3.ZERO);
    }

    /** Fuel-then-hunger as a clean switchover, not a blend -- a pulse's full cost comes from fuel if
     * there's enough, otherwise the whole cost shifts to hunger instead. Returns false (pulse can't
     * be paid for at all) only once both are insufficient. */
    private boolean payForPulse(ItemStack stack, Player player) {
        IFluidHandlerItem fuelHandler = stack.getCapability(Capabilities.FluidHandler.ITEM, null);
        if (fuelHandler != null) {
            FluidStack simulated = fuelHandler.drain(SAW_FUEL_PER_PULSE, IFluidHandler.FluidAction.SIMULATE);
            if (simulated.getAmount() >= SAW_FUEL_PER_PULSE) {
                fuelHandler.drain(SAW_FUEL_PER_PULSE, IFluidHandler.FluidAction.EXECUTE);
                return true;
            }
        }

        FoodData food = player.getFoodData();
        if (food.getFoodLevel() < SAW_HUNGER_PER_PULSE) return false;
        food.setFoodLevel(food.getFoodLevel() - SAW_HUNGER_PER_PULSE);
        return true;
    }

    /** Wears the mounted chain by one pulse's worth -- breaking outright (lost, not materialized)
     * if this pushes it to its max damage, matching "the chain disappears off the model" on break. */
    private void wearChain(ItemStack sunderStack) {
        HeldItemData mounted = mountedChain(sunderStack);
        if (mounted.isEmpty()) return;

        ItemStack chain = mounted.itemStack();
        int newDamage = chain.getDamageValue() + CHAIN_WEAR_PER_PULSE;
        if (newDamage >= chain.getMaxDamage()) {
            clearMountedChain(sunderStack);
        } else {
            chain.setDamageValue(newDamage);
            setMountedChain(sunderStack, chain);
        }
    }

    /**
     * Shared exit point for SAWING, callable both from {@code tickSawing}'s own exit conditions and
     * from an external interruption (knockback, see {@code SunderEvents}) -- one place decides the
     * Bleed guarantee-vs-fallback and the next state, so both paths behave identically regardless of
     * which one actually triggered the exit.
     */
    public void endSawing(ItemStack stack, ServerLevel level, Player player, @Nullable LivingEntity target, long elapsed, boolean holdingTrigger, long now) {
        if (target != null && target.isAlive()) {
            applyBleedOnSawEnd(stack, player, target, elapsed);
        }

        // See registerControllers -- "swing"'s held "attack" loop never finishes on its own, so this
        // is the only place that ever hands the Swing controller back to a rest pose.
        triggerAnim(player, GeoItem.getOrAssignId(stack, level), "Swing", "swing_rest");

        SunderModeData.State nextState = holdingTrigger ? SunderModeData.State.ACTIVE : SunderModeData.State.RELEASE_DELAY;
        stack.set(ModDataComponentTypes.SUNDER_MODE_DATA.get(), SunderModeData.of(nextState, now));
    }

    /** Guaranteed if the pulse sequence ran {@link #BLEED_GUARANTEE_TICKS} (1 second) or longer
     * before ending, kill/timeout/interruption all counting the same -- falls back to the standard
     * chance roll otherwise, per "interrupted before the 1-second mark falls back to the standard
     * hit's Bleed chance." */
    private void applyBleedOnSawEnd(ItemStack stack, Player player, LivingEntity target, long elapsed) {
        if (target.getType().is(ModTags.EntityTypes.NOT_BLEEDABLE)) return;

        ChainProperties chain = chainProperties(stack);
        boolean guaranteed = elapsed >= BLEED_GUARANTEE_TICKS;
        boolean applies = guaranteed || (chain != null && player.getRandom().nextFloat() < chain.bleedChance());
        if (applies) {
            target.addEffect(new MobEffectInstance(ModEffects.BLEED, BLEED_DURATION_TICKS));
        }
    }

    ////////////////////Chain\\\\\\\\\\\\\\\\\\\\

    /** The chain currently mounted on Sunder, or {@link HeldItemData#EMPTY} if none -- see
     * SUNDER_MOUNTED_CHAIN's javadoc in {@link ModDataComponentTypes} for why this is a real nested
     * ItemStack rather than a bespoke durability stat. Materializing it into a real slot item (the
     * Scrench GUI's job, not yet built) and clearing it here are two different steps -- this getter
     * doesn't mutate anything. */
    public static HeldItemData mountedChain(ItemStack sunderStack) {
        return sunderStack.getOrDefault(ModDataComponentTypes.SUNDER_MOUNTED_CHAIN.get(), HeldItemData.EMPTY);
    }

    public static boolean hasMountedChain(ItemStack sunderStack) {
        return !mountedChain(sunderStack).isEmpty();
    }

    /** Installs a chain -- the Scrench GUI's completed-swap case. Stores {@code chain} as-is
     * (whatever its current damage value is), so the caller is responsible for handing in the exact
     * stack that should end up mounted, copied rather than referenced live. */
    public static void setMountedChain(ItemStack sunderStack, ItemStack chain) {
        sunderStack.set(ModDataComponentTypes.SUNDER_MOUNTED_CHAIN.get(), new HeldItemData(chain.copy()));
    }

    /** Clears the mounted chain -- either the Scrench GUI pulling it out to materialize as a real
     * item, or it breaking outright from wear (see the Chain durability design notes). Doesn't
     * itself decide which case this is or what happens to the chain afterward -- that's the caller's
     * job (materialize a real ItemStack for the GUI, or just drop it entirely on break). */
    public static void clearMountedChain(ItemStack sunderStack) {
        sunderStack.set(ModDataComponentTypes.SUNDER_MOUNTED_CHAIN.get(), HeldItemData.EMPTY);
    }

    /** Whether {@code sunderStack} currently has a Module tagged {@code moduleTag} installed --
     * generic capability-query dispatch, same shape as EaterItem's own {@code hasModule}. Reads
     * {@link ModDataComponentTypes#SUNDER_MODULE_DATA} directly. */
    public static boolean hasModule(ItemStack sunderStack, TagKey<Item> moduleTag) {
        return IHaveModules.hasModule(sunderStack, ModDataComponentTypes.SUNDER_MODULE_DATA.get(), MODULE_SLOT_COUNT, moduleTag);
    }

    ////////////////////IWorkbenchSwappable (Scrench field / Workbench station swap panel)\\\\\\\\\\\\\\\\\\\\

    // Panel layout -- public so ScrenchScreen/WorkbenchScreen can draw matching backgrounds under
    // exactly the slots SunderSwapPanel builds, same convention EaterItem's own public panel
    // constants use.
    public static final int CHAIN_SLOT_X = 45;
    public static final int CHAIN_SLOT_Y = 35;
    public static final int FUEL_SLOT_X = 113;
    public static final int FUEL_SLOT_Y = 60;
    public static final int FUEL_TANK_X = FUEL_SLOT_X;
    public static final int FUEL_TANK_Y = FUEL_SLOT_Y - 48; // tank asset's own top, 48px above its bottom-anchored fill slot

    /** Sunder's Gadget Module loadout -- first Weapons-subsection gadget to get one, same shared
     * Module system as Eater/Drinker/Sipping (see {@link ModDataComponentTypes#SUNDER_MODULE_DATA}).
     * 1 general-purpose slot, same size as Drinker's own -- Sunder's Module catalog starts empty
     * (Safety Modules already apply generically; the Salvage/Anchor keep-on-death Modules are this
     * slot's actual motivating use case). */
    public static final int MODULE_SLOT_COUNT = 1;
    public static final int MODULE_SLOT_CAPACITY = IHaveModules.DEFAULT_MODULE_SLOT_CAPACITY;
    public static final int MODULE_SLOT_X = 8;
    public static final int MODULE_SLOT_Y = 27;

    @Override
    public SwapPanel openSwapPanel(java.util.function.Supplier<ItemStack> gadgetStackSupplier, Player player, boolean fieldHosted) {
        return new SunderSwapPanel(gadgetStackSupplier, fieldHosted);
    }

    /**
     * Sunder's own chain-swap + fuel-fill panel, extracted 2026-08-09 out of what used to be
     * {@code ScrenchMenu}'s own constructor/removed()/applyCompletedSwapCosts -- moved here so both
     * the Scrench and the Workbench's Swap page share one implementation instead of each hosting a
     * hand-written copy (the exact duplication the original {@code IWorkbenchSwappable} javadoc
     * flagged as its own future extension point).
     *
     * <p>Rewritten (2026-08-10) into a pure live view, no materialize-on-open/write-back-on-close --
     * both slots read/write {@link #mountedChain}/the fuel capability directly against whatever the
     * supplier currently points at, the same technique the pre-existing Workbench-only
     * {@code SunderChainSlot}/{@code SunderFuelFillSlot} already proved correct (and which the
     * Workbench host needs regardless, since its working-item slot can be swapped out entirely while
     * the menu stays open -- a captured single {@code ItemStack} reference would go stale there).
     */
    private static final class SunderSwapPanel implements SwapPanel {

        /** 5 seconds, matching the mod's other short debuff-style durations -- see the Scrench
         * design notes' open questions for why this exact magnitude is still a placeholder. */
        private static final int SWAP_PENALTY_DURATION_TICKS = 100;

        private static final SimpleContainer DUMMY = new SimpleContainer(0);

        private final java.util.function.Supplier<ItemStack> gadgetStackSupplier;
        private final boolean fieldHosted;
        private final IItemHandlerModifiable moduleHandler;
        private boolean moduleSlotChanged = false;

        private SunderSwapPanel(java.util.function.Supplier<ItemStack> gadgetStackSupplier, boolean fieldHosted) {
            this.gadgetStackSupplier = gadgetStackSupplier;
            this.fieldHosted = fieldHosted;
            this.moduleHandler = IHaveItemData.liveHandler(() -> new IHaveItemData.BulkItemHandler(gadgetStackSupplier.get(),
                    ModDataComponentTypes.SUNDER_MODULE_DATA.get(), MODULE_SLOT_COUNT, MODULE_SLOT_CAPACITY,
                    candidate -> candidate.is(ModTags.Items.MODULES)));
        }

        @Override
        public List<Slot> slots(int panelX, int panelY, java.util.function.BooleanSupplier active) {
            List<Slot> slots = new ArrayList<>(IHaveModules.buildModuleSlots(moduleHandler, MODULE_SLOT_COUNT,
                    panelX + MODULE_SLOT_X + 1, panelY + MODULE_SLOT_Y + 1, 0, active, () -> moduleSlotChanged = true));
            slots.add(new ChainSlot(panelX + CHAIN_SLOT_X + 1, panelY + CHAIN_SLOT_Y + 1, active));
            slots.add(new FuelFillSlot(panelX + FUEL_SLOT_X + 1, panelY + FUEL_SLOT_Y + 1, active));
            return slots;
        }

        /**
         * Completed-swap detection: a mounted chain present at close = penalty, matching what was
         * actually decided for the original copy-out/copy-back version -- no distinction for "the
         * same chain the player never touched," since that version applied the cost whenever the
         * chain slot was non-empty at close regardless of whether a swap actually happened. A live
         * view makes this simpler to read, not different in behavior: just check what's mounted
         * right now. Costs only apply when {@link #fieldHosted} -- the Workbench is the deliberately
         * safer/costless station alternative.
         */
        @Override
        public void onClosed(Player player) {
            if (fieldHosted && (hasMountedChain(gadgetStackSupplier.get()) || moduleSlotChanged)) {
                applyCompletedSwapCosts(player);
            }
        }

        /** Movement penalty (see {@link ModEffects#SCRENCH_OFF_BALANCE}) plus 1 point of Scrench
         * durability wear -- checks both hands for the Scrench, since a completed swap could have
         * come from either pairing direction. */
        private void applyCompletedSwapCosts(Player player) {
            player.addEffect(new MobEffectInstance(ModEffects.SCRENCH_OFF_BALANCE,
                    SWAP_PENALTY_DURATION_TICKS, 0, false, false, false));

            if (player.getMainHandItem().getItem() instanceof ScrenchItem) {
                player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            } else if (player.getOffhandItem().getItem() instanceof ScrenchItem) {
                player.getOffhandItem().hurtAndBreak(1, player, EquipmentSlot.OFFHAND);
            }
        }

        /** Live view straight into {@link #mountedChain}/{@link #setMountedChain}/
         * {@link #clearMountedChain} on whatever the supplier currently points at -- same technique
         * as the pre-existing {@code SunderChainSlot}, generalized to work for either host via the
         * supplier instead of a captured work-item handler. */
        private final class ChainSlot extends Slot {
            private final java.util.function.BooleanSupplier active;

            ChainSlot(int x, int y, java.util.function.BooleanSupplier active) {
                super(DUMMY, 0, x, y);
                this.active = active;
            }

            @Override
            public boolean isActive() {
                return active.getAsBoolean();
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof SunderChainItem;
            }

            @Override
            public ItemStack getItem() {
                return mountedChain(gadgetStackSupplier.get()).itemStack();
            }

            @Override
            public void set(ItemStack stack) {
                if (stack.isEmpty()) {
                    clearMountedChain(gadgetStackSupplier.get());
                } else {
                    setMountedChain(gadgetStackSupplier.get(), stack);
                }
            }

            @Override
            public void setChanged() {
                // No-op target container (DUMMY) -- the real mutation already happened directly on
                // the live gadget stack via set()/remove() above.
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPickup(Player player) {
                return hasMountedChain(gadgetStackSupplier.get());
            }

            @Override
            public ItemStack remove(int amount) {
                ItemStack chain = mountedChain(gadgetStackSupplier.get()).itemStack();
                if (!chain.isEmpty()) clearMountedChain(gadgetStackSupplier.get());
                return chain;
            }
        }

        /**
         * Drains a filled fluid container immediately into Sunder's own fuel tank on contact -- same
         * "immediate, like everything else" rule as every other fluid transfer in the mod.
         */
        private final class FuelFillSlot extends Slot {
            private final java.util.function.BooleanSupplier active;

            FuelFillSlot(int x, int y, java.util.function.BooleanSupplier active) {
                super(new SimpleContainer(1), 0, x, y);
                this.active = active;
            }

            @Override
            public boolean isActive() {
                return active.getAsBoolean();
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

                ItemStack sunderStack = gadgetStackSupplier.get();
                IFluidHandlerItem sunderTank = sunderStack.getCapability(Capabilities.FluidHandler.ITEM, null);
                IFluidHandlerItem containerHandler = held.getCapability(Capabilities.FluidHandler.ITEM, null);
                if (sunderTank == null || containerHandler == null) return;

                if (FluidUtil.tryFluidTransfer(sunderTank, containerHandler, Integer.MAX_VALUE, true).isEmpty()) return;
                set(containerHandler.getContainer());
            }
        }
    }

    ////////////////////Tooltip\\\\\\\\\\\\\\\\\\\\

    /** Fuel and chain durability always show (no shift-gate) -- both are "can I keep using this
     * right now" reads, not incidental detail. Fuel mirrors FluidTankRenderer#getTooltip's own
     * shape (name line + gray amount/capacity line); chain durability replaces the old
     * testability-only raw-literal line now that this is real tooltip copy.
     *
     * <p>Everything else -- calculated damage and the chain's Bleed/decapitation/bonus-loot chances
     * -- is shift-gated, same as {@link SunderChainItem}'s own stat tooltip, and reused from live
     * state every call: since {@link #chainProperties} reads the current mounted chain fresh each
     * time, this naturally reflects whatever chain is mounted right now with no extra wiring needed
     * for it to "update" after a Scrench swap -- there's no cached/stale copy anywhere to invalidate.
     * Damage is shown as the actual computed number (base + chain's shift combined), not a repeat of
     * the chain's own raw percent, which is what {@code SunderChainItem} already shows -- this is
     * meant to answer "what does Sunder hit for right now," not restate the chain's own stat line.
     *
     * <p>Note this doesn't suppress vanilla's own automatic Attack Damage/Attack Speed tooltip lines
     * from {@link #getDefaultAttributeModifiers} -- those still always show regardless of shift,
     * same as any other weapon; the computed Damage line here is a convenience restatement, not a
     * replacement for them. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag);

        FluidData data = stack.getOrDefault(getDataType(), FluidData.EMPTY);
        if (!data.isFluidEmpty()) {
            tooltip.add(data.getFluidComponent());
            tooltip.add(Component.translatable("tooltip.dermicraft.liquid.amount.with.capacity",
                    data.getFluidAmount(), FUEL_CAPACITY).withStyle(ChatFormatting.GRAY));
        }

        ItemStack chainStack = mountedChain(stack).itemStack();
        if (chainStack.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.dermicraft.sunder.chain_none").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.dermicraft.sunder.chain_durability", chainStack.getHoverName(),
                    chainStack.getMaxDamage() - chainStack.getDamageValue(), chainStack.getMaxDamage()).withStyle(ChatFormatting.GRAY));
        }

        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.dermicraft.hold_shift_for_stats"));
            return;
        }

        ChainProperties chain = chainProperties(stack);
        float damageShift = chain == null ? NO_CHAIN_DAMAGE_PENALTY : chain.damageMultiplier() - 1.0F;
        float effectiveDamage = BASE_ATTACK_DAMAGE * (1.0F + damageShift);
        tooltip.add(Component.translatable("tooltip.dermicraft.sunder.damage", String.format("%.2f", effectiveDamage))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.dermicraft.sunder_chain.bleed_chance",
                Math.round((chain == null ? 0.0F : chain.bleedChance()) * 100)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.dermicraft.sunder_chain.decap_chance",
                Math.round((chain == null ? 0.0F : chain.decapChance()) * 100)).withStyle(ChatFormatting.GRAY));
        if (chain != null && chain.lootBonusChance() > 0.0F) {
            tooltip.add(Component.translatable("tooltip.dermicraft.sunder_chain.loot_bonus",
                    Math.round(chain.lootBonusChance() * 100)).withStyle(ChatFormatting.GRAY));
        }
    }

    ////////////////////Animation\\\\\\\\\\\\\\\\\\\\

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "Body", 0, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            SunderModeData mode = stack != null
                    ? stack.getOrDefault(ModDataComponentTypes.SUNDER_MODE_DATA.get(), SunderModeData.DEFAULT)
                    : SunderModeData.DEFAULT;

            // Stays on the rev_up_down->running target through SAWING and RELEASE_DELAY (SAWING
            // looks identical to revved-idle -- "the chain will be the same," per the design notes
            // -- and nothing visibly changes during the release wait either); UNREVVING switches
            // back to idle and lets transitionLength blend the pose down -- see the class javadoc
            // for why there's no authored reverse clip.
            boolean revvedLook = mode.stateEnum() == SunderModeData.State.ACTIVE
                    || mode.stateEnum() == SunderModeData.State.SAWING
                    || mode.stateEnum() == SunderModeData.State.RELEASE_DELAY;

            return state.setAndContinue(revvedLook
                    ? RawAnimation.begin().thenPlay("rev_up_down").thenLoop("running")
                    : RawAnimation.begin().thenLoop("idle"));
        }).transitionLength((int) UNREV_TICKS)
                // One-shot hit reaction for a landed SAWING pulse (see tickSawing) -- triggered
                // animations take over the controller until they finish, then it falls back to
                // whatever the state handler above returns (the "running" loop stays targeted the
                // whole time SAWING is active), so this plays as a punch on top of the hold loop
                // rather than replacing it. Needed because a SAWING pulse damages the target
                // directly (see hurtEnemy's javadoc) without ever calling the vanilla attack path,
                // so the usual automatic arm-swing feedback never fires for it.
                .triggerableAnim("saw", RawAnimation.begin().thenPlay("saw")));

        // Separate controller, not another trigger on "Body" -- a controller can only hold one
        // triggered animation at a time, and "attack" fires in the same tick as "saw" (see the
        // ACTIVE case above), so sharing a controller would have the second trigger silently
        // overwrite the first. Safe as its own controller regardless, since "attack"/"swing_rest"
        // only touch the "main" bone, disjoint from everything "Body" ever animates.
        //
        // <p>Dormant (PlayState.STOP) baseline, same shape as SippingItem's "Fill" controller --
        // "main" is never touched by any other controller, so both transitions here have to be
        // driven explicitly by triggers rather than a state predicate. Both are single-stage
        // triggers (just "attack" or just "swing_rest"), deliberately not a chained
        // "swing"-then-"attack" -- an authored in-between arc was tried first, but GeckoLib's
        // internal stage-to-stage handoff (advancing a *single* triggered RawAnimation from one
        // stage to the next) takes a different, buggier code path than swapping to a different
        // triggered animation object entirely -- it re-polls an already-drained stage queue on the
        // following tick, producing a one-tick stutter right at the handoff. Triggering straight into
        // "attack" sidesteps that: it's a full swap to a new RawAnimation object, so it goes through
        // the controller's normal transitionLength blend instead -- the exact same path "swing_rest"
        // already used cleanly for the return. The tradeoff is losing the old clip's authored
        // easeInCirc arc/timing in favor of a flat, snappier interpolation over transitionLength.
        controllers.add(new AnimationController<>(this, "Swing", (int) UNREV_TICKS, state -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().thenLoop("attack"))
                .triggerableAnim("swing_rest", RawAnimation.begin().thenLoop("swing_rest")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
