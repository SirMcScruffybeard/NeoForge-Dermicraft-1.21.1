package net.scruffy.dermicraft.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.scruffy.dermicraft.component.HeldItemData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.component.ShatterModeData;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.effect.ModEffects;
import net.scruffy.dermicraft.interfaces.IGadget;
import net.scruffy.dermicraft.interfaces.IHaveFluidData;
import net.scruffy.dermicraft.interfaces.IWorkbenchSwappable;
import net.scruffy.dermicraft.property.ShatterHeadProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.scruffy.dermicraft.component.FluidData;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Hold-right-click-to-charge, release-to-fire-or-reset -- the first real mechanic pass on Shatter
 * (formerly Drill Hammer), after {@code ShatterItem}'s original registration/visibility-only commit.
 * See the Shatter design notes in {@code dermicraft-gadget-notes.md} for the full mechanic. Fuel now
 * gates charging (same "requires fuel to start" rule as Sunder's rev-up) and is spent -- fuel first,
 * hunger for any shortfall -- only on a fired attack, never on a safely reset charge; the per-grade
 * "use rate"/"speed" scaling from the design notes' Fuel section is still open (only a flat
 * placeholder cost exists so far). Still no actual special effect on a successful release (just the
 * {@code attack} animation trigger, per the current design pass), and no left-click 3x3 mining swing
 * yet either.
 *
 * <p>Deliberately NOT modeled on Sunder's rev/auto-lock shape (hold-and-sustain, locks on landing a
 * hit) -- this is closer to a drawn bow: charge while held, and the outcome is decided at the moment
 * of release rather than continuously while holding. See {@code ShatterModeData} for the smaller
 * three-state machine this needs as a result.
 */
public class ShatterItem extends Item implements GeoItem, IGadget, IWorkbenchSwappable, IHaveFluidData {

    /** Placeholder, same as every other gadget's starting point -- see {@link IGadget}. Not tuned. */
    public static final int MAX_HP = 10;

    /** Same shape and same round default as Sunder's own FUEL_CAPACITY -- placeholder, nothing
     * reads/gates on it yet (charge speed/damage scaling per fuel grade is a separate, later step --
     * see the Shatter design notes' Fuel section). The capability exists now so the Scrench
     * maintenance GUI's fuel gauge/fill slot has a real tank to display and fill. */
    public static final int FUEL_CAPACITY = 1000;

    /** Ticks to reach full charge -- matches {@code build_charge}'s own authored length (1 second)
     * in the animation file, so the mechanical charge time and the animation stay in lockstep
     * without a second number to keep in sync. */
    private static final long CHARGE_TICKS = 20;

    /** Effectively "until released" -- same trick every other held-trigger Gadget uses. */
    private static final int HELD_INDEFINITELY = 72000;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ShatterItem(Properties properties) {
        super(properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    ////////////////////Combat\\\\\\\\\\\\\\\\\\\\

    /** Iron is "the base" (2026-08-12, decided) -- 5.0F ADD_VALUE, combined with the player's own
     * implicit 1.0 base attack damage gives 6.0 total with an Iron head mounted (or none at all
     * beyond this, since Iron's own {@code damageShift} is 0.0F -- see {@link ShatterHeadProperties}).
     * That's a flat +2 margin over Iron Pickaxe's own real total damage (4.0) -- every other head's
     * {@code damageShift} is picked to keep that exact same +2 margin over ITS OWN vanilla pickaxe
     * equivalent (Stone Pickaxe totals 3 -&gt; Bone shift -1.0F -&gt; Shatter totals 5; Gold Pickaxe
     * totals 2 -&gt; Gold shift -2.0F -&gt; Shatter totals 4; Diamond Pickaxe totals 5 -&gt; Diamond
     * shift +1.0F -&gt; Shatter totals 7). The margin itself (+2) is a placeholder choice, not a
     * design-notes-committed number -- easy to retune later since every head's shift derives from it
     * the same way. */
    private static final float BASE_ATTACK_DAMAGE = 5.0F;

    /** Matches every vanilla pickaxe's own flat attack-speed modifier exactly (`PickaxeItem`'s own
     * constant passed into `DiggerItem`'s constructor -- NOT tier-dependent, real pickaxes don't vary
     * speed by material either) rather than inventing a new number -- no per-head speed variance for
     * the same reason. */
    private static final float BASE_ATTACK_SPEED = -2.8F;

    /** No head mounted -- same magnitude/mechanism as Sunder's own no-chain penalty (a 60% cut via
     * ADD_MULTIPLIED_BASE, scaling {@link #BASE_ATTACK_DAMAGE} down rather than an absolute number),
     * not yet a design-notes-committed value for Shatter specifically. */
    private static final float NO_HEAD_DAMAGE_PENALTY = -0.6F;

    private static final net.minecraft.resources.ResourceLocation BASE_DAMAGE_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(net.scruffy.dermicraft.main.Dermicraft.MOD_ID, "base_attack_damage");
    private static final net.minecraft.resources.ResourceLocation BASE_SPEED_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(net.scruffy.dermicraft.main.Dermicraft.MOD_ID, "base_attack_speed");
    private static final net.minecraft.resources.ResourceLocation HEAD_DAMAGE_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(net.scruffy.dermicraft.main.Dermicraft.MOD_ID, "head_damage");

    /** Per-stack combat stats: Shatter's own base, plus the mounted head's flat damage shift (or the
     * no-head penalty if nothing's mounted). Overriding the {@code ItemStack}-sensitive form, not
     * {@code Item.Properties#attributes}, for the exact same reason {@code SunderItem}'s own override
     * does -- see that method's javadoc, the whole explanation (NeoForge/ATTRIBUTE_MODIFIERS-component
     * shadowing) applies identically here. */
    @Override
    public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        net.minecraft.world.item.component.ItemAttributeModifiers.Builder builder =
                net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                        .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_DAMAGE_ID, BASE_ATTACK_DAMAGE,
                                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                        .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_SPEED_ID, BASE_ATTACK_SPEED,
                                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);

        ShatterHeadProperties head = headProperties(stack);
        if (head == null) {
            builder.add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(HEAD_DAMAGE_ID, NO_HEAD_DAMAGE_PENALTY,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
        } else if (head.damageShift() != 0.0F) {
            // Iron's own shift is 0.0F (the baseline) -- skipped so it doesn't show as a meaningless
            // "+0" tooltip line, same as Sunder's own Iron-chain skip.
            builder.add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(HEAD_DAMAGE_ID, head.damageShift(),
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
        }

        return builder.build();
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

    /** See SunderItem/DrinkerItem/EaterItem's identical override -- SHATTER_MODE_DATA rewrites on
     * every state transition while charging, and the default equals-check would otherwise thrash
     * the equip animation. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    /**
     * Overrides vanilla's default {@code Item#mineBlock}, which would otherwise call
     * {@code stack.hurtAndBreak(1, ...)} on THIS stack -- Shatter's own durability is Gadget HP (see
     * {@link IGadget}, drop-damage-only by convention), not mining wear, so left unoverridden this
     * would have been silently chipping away at Shatter's HP on every block mined. Mining wear
     * belongs to the mounted HEAD instead (mirrors Sunder's chain wear, see {@link #wearHead}) --
     * this is where the origin block's own wear happens; {@code ShatterEvents#onBlockBroken} handles
     * the AoE blocks' wear separately, since that loop runs outside this hook entirely.
     */
    @Override
    public boolean mineBlock(ItemStack stack, Level level, net.minecraft.world.level.block.state.BlockState state,
                              BlockPos pos, LivingEntity miningEntity) {
        if (!level.isClientSide) {
            wearHead(stack, HEAD_WEAR_PER_BLOCK);
        }
        return true;
    }

    /** Checks the other hand for a paired Scrench first, same reverse-hand-ordering check as
     * {@code SunderItem#use} -- without it, a Shatter held in the main hand would swallow every
     * right-click and an off-hand Scrench would never get a chance to open its swap menu. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (player.getItemInHand(otherHand).getItem() instanceof ScrenchItem) {
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                net.scruffy.dermicraft.screen.custom.scrench.ScrenchMenu.open(serverPlayer, hand);
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
        ShatterModeData mode = stack.getOrDefault(ModDataComponentTypes.SHATTER_MODE_DATA.get(), ShatterModeData.DEFAULT);
        long now = level.getGameTime();
        long elapsed = now - mode.since();

        ShatterModeData next = switch (mode.stateEnum()) {
            // "Requires fuel to start" -- same gate Sunder's own rev-up uses -- but only for the
            // INITIAL trigger; once charging, sustaining an already-started charge doesn't re-check
            // here (see release()/payForAttack() for where the actual cost gets paid, and where a
            // future fuel-bypass Safety Module -- shared with Sunder's own -- will hook in).
            case IDLE -> holdingTrigger && hasFuel(stack) ? ShatterModeData.of(ShatterModeData.State.CHARGING, now) : null;
            case CHARGING -> {
                if (!holdingTrigger) {
                    yield release(stack, level, player);
                }
                yield elapsed >= CHARGE_TICKS ? ShatterModeData.of(ShatterModeData.State.CHARGED, now) : null;
            }
            case CHARGED -> !holdingTrigger ? release(stack, level, player) : null;
        };

        if (next != null) stack.set(ModDataComponentTypes.SHATTER_MODE_DATA.get(), next);
    }

    /**
     * Trigger released -- fires the {@code attack} animation against a valid target (no world effect
     * yet, per the current design pass) and pays the fuel/hunger cost, or plays the {@code release}
     * animation and costs nothing if nothing's aimed at -- "a safely released charge spends no fuel"
     * (see the class javadoc's Fuel discussion). Either way resets back to IDLE.
     */
    private ShatterModeData release(ItemStack stack, Level level, Player player) {
        LivingEntity target = findTarget(level, player);
        if (target != null) {
            payForAttack(stack, player);
            wearHead(stack, HEAD_WEAR_PER_ATTACK);
        }

        long id = GeoItem.getOrAssignId(stack, (net.minecraft.server.level.ServerLevel) level);
        triggerAnim(player, id, "Body", target != null ? "attack" : "release");
        return ShatterModeData.of(ShatterModeData.State.IDLE, level.getGameTime());
    }

    /** Non-empty, not any specific amount -- gate for starting a charge, same shape as Sunder's own
     * {@code hasFuel}. */
    private boolean hasFuel(ItemStack stack) {
        return !stack.getOrDefault(getDataType(), FluidData.EMPTY).isFluidEmpty();
    }

    /** Placeholder cost per fired attack -- not tuned, and not yet scaled by fuel grade (the "use
     * rate" axis from the Shatter design notes' Fuel section is still an open number). */
    private static final int FUEL_PER_ATTACK = 25;

    /** Fuel-then-hunger, same fallback shape as Sunder's {@code payForPulse} -- drains what fuel is
     * available toward the cost first, then takes the remainder out of hunger (floored at 0, never
     * blocking the attack outright since there's no real world effect yet to gate -- see release()).
     * Unlike Sunder's per-pulse version, this always runs exactly once per fired attack, not in a
     * loop -- "spent on the attack, not on a safely released charge" (see the class javadoc). */
    private void payForAttack(ItemStack stack, Player player) {
        int remaining = FUEL_PER_ATTACK;

        IFluidHandlerItem fuelHandler = stack.getCapability(Capabilities.FluidHandler.ITEM, null);
        if (fuelHandler != null) {
            FluidStack drained = fuelHandler.drain(remaining, IFluidHandler.FluidAction.EXECUTE);
            remaining -= drained.getAmount();
        }

        if (remaining <= 0) return;

        var food = player.getFoodData();
        food.setFoodLevel(Math.max(0, food.getFoodLevel() - remaining));
    }

    /** Raycast along the player's look vector out to the player's own standard tool/melee reach
     * ({@code entityInteractionRange()} -- vanilla's own attribute, gamemode-sensitive) rather than a
     * fixed distance. Deliberately different from Sunder's {@code findSawTarget} (which avoids this
     * attribute specifically because creative's longer reach reads as "grabbing mobs from way too
     * far away" during a sustained lock-on) -- Shatter's release check is a one-off point-in-time
     * test, not a sustained multi-second lock, so the same objection doesn't apply here. */
    @Nullable
    private LivingEntity findTarget(Level level, Player player) {
        double range = player.entityInteractionRange();
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(range));
        AABB searchArea = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(0.5);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eyePos, endPos, searchArea,
                candidate -> candidate instanceof LivingEntity living && living.isAlive() && candidate != player);

        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    ////////////////////Head\\\\\\\\\\\\\\\\\\\\

    /** The head currently mounted on Shatter, or {@link HeldItemData#EMPTY} if none -- same nested-
     * ItemStack shape as {@code SunderItem#mountedChain}, see SHATTER_MOUNTED_HEAD's own javadoc in
     * {@link ModDataComponentTypes} for why. */
    public static HeldItemData mountedHead(ItemStack shatterStack) {
        return shatterStack.getOrDefault(ModDataComponentTypes.SHATTER_MOUNTED_HEAD.get(), HeldItemData.EMPTY);
    }

    public static boolean hasMountedHead(ItemStack shatterStack) {
        return !mountedHead(shatterStack).isEmpty();
    }

    public static void setMountedHead(ItemStack shatterStack, ItemStack head) {
        shatterStack.set(ModDataComponentTypes.SHATTER_MOUNTED_HEAD.get(), new HeldItemData(head.copy()));
    }

    public static void clearMountedHead(ItemStack shatterStack) {
        shatterStack.set(ModDataComponentTypes.SHATTER_MOUNTED_HEAD.get(), HeldItemData.EMPTY);
    }

    /** Placeholder magnitude, matching vanilla's own "1 durability per block mined" model now that
     * head durability is set to match its corresponding pickaxe's real max damage -- see
     * {@link #mineBlock} (origin block) and {@code ShatterEvents#onBlockBroken} (the AoE blocks).
     * Public, not private -- {@code ShatterEvents} needs the same magnitude for the AoE blocks'
     * own wear, and duplicating the number there would risk the two drifting apart. */
    public static final int HEAD_WEAR_PER_BLOCK = 1;

    /** Same placeholder magnitude, worn once per fired charge attack -- see {@link #release}. */
    private static final int HEAD_WEAR_PER_ATTACK = 1;

    /** Wears the mounted head by {@code amount} -- breaking outright (lost, not materialized) if
     * this pushes it to its max damage, same "the part disappears off the model" convention Sunder's
     * own {@code wearChain} uses for its chain. No-ops if nothing's mounted. */
    public static void wearHead(ItemStack shatterStack, int amount) {
        HeldItemData mounted = mountedHead(shatterStack);
        if (mounted.isEmpty()) return;

        ItemStack head = mounted.itemStack();
        int newDamage = head.getDamageValue() + amount;
        if (newDamage >= head.getMaxDamage()) {
            clearMountedHead(shatterStack);
        } else {
            head.setDamageValue(newDamage);
            setMountedHead(shatterStack, head);
        }
    }

    /** The mounted head's material data (currently just tint/mining tier -- see
     * {@link ShatterHeadProperties}), or {@code null} when nothing's mounted. Mirrors
     * {@code SunderItem#chainProperties}. */
    @Nullable
    public static ShatterHeadProperties headProperties(ItemStack shatterStack) {
        ItemStack head = mountedHead(shatterStack).itemStack();
        if (head.isEmpty()) return null;
        return BuiltInRegistries.ITEM.wrapAsHolder(head.getItem()).getData(ModDataMaps.SHATTER_HEAD_PROPERTIES);
    }

    /** No head mounted -- treated as below every real tier (can't touch any tool-gated block at
     * all), same "much weaker than a real material" flavor Sunder's own no-chain state carries. */
    private static final int NO_HEAD_MINING_TIER = -1;

    /**
     * Whether the mounted head's material tier meets {@code state}'s tool requirement -- mirrors
     * {@code DiggerItem#isCorrectToolForDrops}'s own three-tag check (NEEDS_STONE_TOOL/
     * NEEDS_IRON_TOOL/NEEDS_DIAMOND_TOOL against the tier's numeric level), plus scoping to
     * pickaxe-appropriate blocks specifically ({@code BlockTags.MINEABLE_WITH_PICKAXE}) -- Shatter
     * is a hammer, not a general-purpose tool, so this doesn't extend to axe/shovel-gated blocks.
     * Blocks that don't require a tool at all (dirt, sand, ...) always pass, regardless of head.
     *
     * <p>Real vanilla behavior (2026-08-12, revised from an earlier hard-block pass) -- this is a
     * drops/speed gate only, same as vanilla's own tool tiers, NOT something that blocks the swing
     * outright. {@code ShatterEvents#onBlockBroken} always breaks the targeted blocks; this just
     * decides whether drops actually come out of it.
     */
    public static boolean meetsMiningTier(ItemStack shatterStack, net.minecraft.world.level.block.state.BlockState state) {
        if (!state.requiresCorrectToolForDrops()) return true;
        if (!state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)) return true;

        ShatterHeadProperties head = headProperties(shatterStack);
        int level = head != null ? head.miningTier() : NO_HEAD_MINING_TIER;

        if (level < 3 && state.is(net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL)) return false;
        if (level < 2 && state.is(net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL)) return false;
        if (level < 1 && state.is(net.minecraft.tags.BlockTags.NEEDS_STONE_TOOL)) return false;
        return true;
    }

    /** Vanilla's own drops-gate hook -- delegates to {@link #meetsMiningTier}, so anything reading
     * this directly (tooltips, other mods, vanilla's own normal mining pipeline for the origin
     * block) sees the same answer {@code ShatterEvents}' manual drop computation uses. */
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        return meetsMiningTier(stack, state);
    }

    ////////////////////IWorkbenchSwappable (Scrench field / Workbench station swap panel)\\\\\\\\\\\\\\\\\\\\

    // Panel layout -- public so ScrenchScreen (and, later, the Workbench's own Swap page) can draw
    // matching backgrounds under exactly the slot ShatterSwapPanel builds, same convention SunderItem's
    // own panel constants use. Reuses Sunder's own CHAIN_SLOT_X/Y position -- there's no fuel slot
    // beside it yet (see the class javadoc), so there's no layout reason to place it anywhere else.
    public static final int HEAD_SLOT_X = SunderItem.CHAIN_SLOT_X;
    public static final int HEAD_SLOT_Y = SunderItem.CHAIN_SLOT_Y;
    // Same fuel tank/slot position as Sunder's own panel -- "same layout and pattern" per the design
    // decision, not an independent layout choice.
    public static final int FUEL_SLOT_X = SunderItem.FUEL_SLOT_X;
    public static final int FUEL_SLOT_Y = SunderItem.FUEL_SLOT_Y;
    public static final int FUEL_TANK_X = FUEL_SLOT_X;
    public static final int FUEL_TANK_Y = FUEL_SLOT_Y - 48; // tank asset's own top, 48px above its bottom-anchored fill slot

    @Override
    public SwapPanel openSwapPanel(Supplier<ItemStack> gadgetStackSupplier, Player player, boolean fieldHosted) {
        return new ShatterSwapPanel(gadgetStackSupplier, fieldHosted);
    }

    /**
     * Shatter's own head-swap panel -- mirrors {@code SunderItem.SunderSwapPanel} exactly (same
     * live-view technique, same completed-swap cost shape, same fuel-fill slot).
     */
    private static final class ShatterSwapPanel implements SwapPanel {

        /** Same magnitude as Sunder's own swap penalty -- see {@code SunderItem.SunderSwapPanel}'s
         * javadoc for why this exact number is still a placeholder. */
        private static final int SWAP_PENALTY_DURATION_TICKS = 100;

        private static final net.minecraft.world.SimpleContainer DUMMY = new net.minecraft.world.SimpleContainer(0);

        private final Supplier<ItemStack> gadgetStackSupplier;
        private final boolean fieldHosted;

        private ShatterSwapPanel(Supplier<ItemStack> gadgetStackSupplier, boolean fieldHosted) {
            this.gadgetStackSupplier = gadgetStackSupplier;
            this.fieldHosted = fieldHosted;
        }

        @Override
        public List<Slot> slots(int panelX, int panelY, BooleanSupplier active) {
            return List.of(
                    new HeadSlot(panelX + HEAD_SLOT_X + 1, panelY + HEAD_SLOT_Y + 1, active),
                    new FuelFillSlot(panelX + FUEL_SLOT_X + 1, panelY + FUEL_SLOT_Y + 1, active));
        }

        /** Same "a mounted part present at close counts as a completed swap" detection as Sunder's
         * own panel -- see that class's javadoc. */
        @Override
        public void onClosed(Player player) {
            if (fieldHosted && hasMountedHead(gadgetStackSupplier.get())) {
                applyCompletedSwapCosts(player);
            }
        }

        /** Identical cost shape to Sunder's own completed swap -- movement penalty plus 1 point of
         * Scrench durability wear, checking both hands for the Scrench. */
        private void applyCompletedSwapCosts(Player player) {
            player.addEffect(new MobEffectInstance(ModEffects.SCRENCH_OFF_BALANCE,
                    SWAP_PENALTY_DURATION_TICKS, 0, false, false, false));

            if (player.getMainHandItem().getItem() instanceof ScrenchItem) {
                player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            } else if (player.getOffhandItem().getItem() instanceof ScrenchItem) {
                player.getOffhandItem().hurtAndBreak(1, player, EquipmentSlot.OFFHAND);
            }
        }

        /** Live view straight into {@link #mountedHead}/{@link #setMountedHead}/
         * {@link #clearMountedHead} on whatever the supplier currently points at -- same technique
         * as Sunder's own {@code ChainSlot}. */
        private final class HeadSlot extends Slot {
            private final BooleanSupplier active;

            HeadSlot(int x, int y, BooleanSupplier active) {
                super(DUMMY, 0, x, y);
                this.active = active;
            }

            @Override
            public boolean isActive() {
                return active.getAsBoolean();
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ShatterHeadItem;
            }

            @Override
            public ItemStack getItem() {
                return mountedHead(gadgetStackSupplier.get()).itemStack();
            }

            @Override
            public void set(ItemStack stack) {
                if (stack.isEmpty()) {
                    clearMountedHead(gadgetStackSupplier.get());
                } else {
                    setMountedHead(gadgetStackSupplier.get(), stack);
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
                return hasMountedHead(gadgetStackSupplier.get());
            }

            @Override
            public ItemStack remove(int amount) {
                ItemStack head = mountedHead(gadgetStackSupplier.get()).itemStack();
                if (!head.isEmpty()) clearMountedHead(gadgetStackSupplier.get());
                return head;
            }
        }

        /** Drains a filled fluid container immediately into Shatter's own fuel tank on contact --
         * identical to Sunder's own {@code FuelFillSlot}, same "immediate, like everything else"
         * rule as every other fluid transfer in the mod. */
        private final class FuelFillSlot extends Slot {
            private final BooleanSupplier active;

            FuelFillSlot(int x, int y, BooleanSupplier active) {
                super(new net.minecraft.world.SimpleContainer(1), 0, x, y);
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

                ItemStack shatterStack = gadgetStackSupplier.get();
                IFluidHandlerItem shatterTank = shatterStack.getCapability(Capabilities.FluidHandler.ITEM, null);
                IFluidHandlerItem containerHandler = held.getCapability(Capabilities.FluidHandler.ITEM, null);
                if (shatterTank == null || containerHandler == null) return;

                if (FluidUtil.tryFluidTransfer(shatterTank, containerHandler, Integer.MAX_VALUE, true).isEmpty()) return;
                set(containerHandler.getContainer());
            }
        }
    }

    ////////////////////Tooltip\\\\\\\\\\\\\\\\\\\\

    /** Same shape as {@code SunderItem#appendHoverText} -- fuel and head durability always show (no
     * shift-gate, both are "can I keep using this right now" reads); computed damage and mining tier
     * are shift-gated (no per-head *special* stat exists yet beyond these two, see the design
     * notes). */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag);

        FluidData data = stack.getOrDefault(getDataType(), FluidData.EMPTY);
        if (!data.isFluidEmpty()) {
            tooltip.add(data.getFluidComponent());
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.dermicraft.liquid.amount.with.capacity",
                    data.getFluidAmount(), FUEL_CAPACITY).withStyle(net.minecraft.ChatFormatting.GRAY));
        }

        ItemStack headStack = mountedHead(stack).itemStack();
        if (headStack.isEmpty()) {
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.dermicraft.shatter.head_none")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        } else {
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.dermicraft.shatter.head_durability",
                    headStack.getHoverName(), headStack.getMaxDamage() - headStack.getDamageValue(), headStack.getMaxDamage())
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }

        if (!net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.dermicraft.hold_shift_for_stats"));
            return;
        }

        ShatterHeadProperties head = headProperties(stack);
        float damageShift = head != null ? head.damageShift() : NO_HEAD_DAMAGE_PENALTY;
        boolean multiplied = head == null; // no-head penalty is ADD_MULTIPLIED_BASE, a head's own shift is ADD_VALUE
        float effectiveDamage = multiplied ? BASE_ATTACK_DAMAGE * (1.0F + damageShift) : BASE_ATTACK_DAMAGE + damageShift;
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.dermicraft.shatter.damage",
                String.format("%.1f", effectiveDamage)).withStyle(net.minecraft.ChatFormatting.GRAY));

        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.dermicraft.shatter.mining_tier",
                head != null ? head.miningTier() : NO_HEAD_MINING_TIER).withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    ////////////////////Animation\\\\\\\\\\\\\\\\\\\\

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "Body", 5, state -> {
            // Idle only plays while actually held (first/third-person hand) -- same
            // ITEM_RENDER_PERSPECTIVE check DrinkerScreenGlowLayer#isHeldInHand uses to tell a held
            // item from one sitting in a GUI slot, on the ground, or in an item frame. Charging never
            // reaches this branch outside a genuine hold anyway (inventoryTick only advances the mode
            // while a player is actively using the item), so this only ever actually changes idle's
            // own behavior.
            net.minecraft.world.item.ItemDisplayContext perspective = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
            boolean heldInHand = perspective != null && switch (perspective) {
                case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND,
                     THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> true;
                default -> false;
            };
            if (!heldInHand) return software.bernie.geckolib.animation.PlayState.STOP;

            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            ShatterModeData mode = stack != null
                    ? stack.getOrDefault(ModDataComponentTypes.SHATTER_MODE_DATA.get(), ShatterModeData.DEFAULT)
                    : ShatterModeData.DEFAULT;

            boolean charging = mode.stateEnum() == ShatterModeData.State.CHARGING
                    || mode.stateEnum() == ShatterModeData.State.CHARGED;

            return state.setAndContinue(charging
                    ? RawAnimation.begin().thenPlay("build_charge").thenLoop("hold_charge")
                    : RawAnimation.begin().thenLoop("idle"));
        })
                // One-shot release-time triggers -- interrupt whatever the state predicate above is
                // showing, play once, then fall back to the predicate (which by then already reads
                // IDLE again, since release() sets the mode data before triggering -- see release()),
                // same "trigger on top of predicate" shape Sunder's own Body controller uses for its
                // "saw" pulse trigger.
                .triggerableAnim("release", RawAnimation.begin().thenPlay("release"))
                .triggerableAnim("attack", RawAnimation.begin().thenPlay("attack")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
