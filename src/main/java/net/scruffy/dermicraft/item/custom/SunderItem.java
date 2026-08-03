package net.scruffy.dermicraft.item.custom;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.HeldItemData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.component.SunderModeData;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.interfaces.IHaveFluidData;
import net.scruffy.dermicraft.property.ChainProperties;
import net.scruffy.dermicraft.screen.custom.scrench.ScrenchMenu;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Placeholder -- basic rev state machine only (hold right-click to rev/dig-in eventually). Has a
 * fuel tank ({@link IHaveFluidData}, capacity {@link #FUEL_CAPACITY}) but nothing reads or gates on
 * it yet -- revving isn't fuel-gated, dig-in doesn't drain it. See the Sunder design notes in
 * {@code dermicraft-gadget-notes.md}. Proves the input -> state -> animation loop before fuel and
 * combat get layered on top.
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
public class SunderItem extends Item implements GeoItem, IHaveFluidData {

    /** Placeholder capacity -- fuel-drain-rate-per-pulse and every other fuel-economy number are
     * still open design questions (see the notes), so this is a round default to build against,
     * not a tuned value. Matches Drinker/Sipping's own 1000mB default for consistency. */
    public static final int FUEL_CAPACITY = 1000;

    /** Base (pre-chain/tier/fuel/points) attack damage modifier -- 5.0, matching vanilla's own
     * Iron Sword exactly (3.0 base + Iron's 2.0 tier bonus, per {@code SwordItem.createAttributes}).
     * Deliberately pinned to IRON rather than stone: the Iron chain is the baseline every other
     * chain material is defined relative to (see the design notes), so Sunder's own base tracks the
     * same metal for consistency. */
    private static final float BASE_ATTACK_DAMAGE = 5.0F;

    /** Base attack speed modifier -- a bit more negative than a vanilla sword's shared -2.4 (every
     * sword tier uses the same speed), landing at 1.4 attacks/second instead of a sword's 1.6, per
     * "slightly slower swing speed." */
    private static final float BASE_ATTACK_SPEED = -2.6F;

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
     * idle lines up with roughly when the blend finishes. */
    private static final long UNREV_TICKS = 5;

    /** Effectively "until released" -- same trick DRINKER/EATER's held pose uses. */
    private static final int HELD_INDEFINITELY = 72000;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SunderItem(Properties properties) {
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

        SunderModeData next = switch (mode.stateEnum()) {
            case IDLE -> holdingTrigger ? SunderModeData.of(SunderModeData.State.ARM_DELAY, now) : null;
            case ARM_DELAY -> !holdingTrigger
                    ? SunderModeData.of(SunderModeData.State.IDLE, now) // released before the swap -- nothing to wind down
                    : elapsed >= ARM_DELAY_TICKS ? SunderModeData.of(SunderModeData.State.ACTIVE, now) : null;
            case ACTIVE -> !holdingTrigger ? SunderModeData.of(SunderModeData.State.RELEASE_DELAY, now) : null;
            // Re-holding during the wind-down is ignored for this basic pass -- always let it finish.
            case RELEASE_DELAY -> elapsed >= RELEASE_DELAY_TICKS
                    ? SunderModeData.of(SunderModeData.State.UNREVVING, now) : null;
            case UNREVVING -> elapsed >= UNREV_TICKS ? SunderModeData.of(SunderModeData.State.IDLE, now) : null;
        };

        if (next != null) stack.set(ModDataComponentTypes.SUNDER_MODE_DATA.get(), next);
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

    ////////////////////Tooltip\\\\\\\\\\\\\\\\\\\\

    /** Same shape as BladderItem's identical override -- shift-to-reveal exact amount. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag);

        FluidData data = stack.getOrDefault(getDataType(), FluidData.EMPTY);
        if (!data.isFluidEmpty()) {
            if (Screen.hasShiftDown()) {
                tooltip.add(Component.translatable("tooltip.dermicraft.liquid.amount", data.getFluidAmount()));
            } else {
                tooltip.add(Component.translatable("tooltip.dermicraft.hold_shift_for_amount"));
            }
        }

        // Testability only, not final tooltip copy -- lets chain mount/clear be verified in-game
        // before the Scrench GUI (the real player-facing path) exists.
        ItemStack chain = mountedChain(stack).itemStack();
        tooltip.add(Component.literal(chain.isEmpty()
                ? "Chain: none"
                : "Chain: " + chain.getHoverName().getString() + " (" + (chain.getMaxDamage() - chain.getDamageValue()) + "/" + chain.getMaxDamage() + ")"));
    }

    ////////////////////Animation\\\\\\\\\\\\\\\\\\\\

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "Body", 0, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            SunderModeData mode = stack != null
                    ? stack.getOrDefault(ModDataComponentTypes.SUNDER_MODE_DATA.get(), SunderModeData.DEFAULT)
                    : SunderModeData.DEFAULT;

            // Stays on the rev_up_down->running target through RELEASE_DELAY (nothing visibly
            // changes during that wait); UNREVVING switches back to idle and lets transitionLength
            // blend the pose down -- see the class javadoc for why there's no authored reverse clip.
            boolean revvedLook = mode.stateEnum() == SunderModeData.State.ACTIVE
                    || mode.stateEnum() == SunderModeData.State.RELEASE_DELAY;

            return state.setAndContinue(revvedLook
                    ? RawAnimation.begin().thenPlay("rev_up_down").thenLoop("running")
                    : RawAnimation.begin().thenLoop("idle"));
        }).transitionLength((int) UNREV_TICKS));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
