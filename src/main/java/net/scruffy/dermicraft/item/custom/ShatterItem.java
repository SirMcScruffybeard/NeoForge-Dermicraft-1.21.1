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
import net.scruffy.dermicraft.interfaces.IHaveItemData;
import net.scruffy.dermicraft.interfaces.IHaveModules;
import net.scruffy.dermicraft.interfaces.IWorkbenchSwappable;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
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
public class ShatterItem extends Item implements GeoItem, IGadget, IWorkbenchSwappable, IHaveFluidData, IHaveModules {

    /** Placeholder, same as every other gadget's starting point -- see {@link IGadget}. Not tuned. */
    public static final int MAX_HP = 10;

    /** Same shape and same round default as Sunder's own FUEL_CAPACITY -- placeholder, nothing
     * reads/gates on it yet (charge speed/damage scaling per fuel grade is a separate, later step --
     * see the Shatter design notes' Fuel section). The capability exists now so the Scrench
     * maintenance GUI's fuel gauge/fill slot has a real tank to display and fill. */
    public static final int FUEL_CAPACITY = 1000;

    /** Real, current fuel capacity: {@link #FUEL_CAPACITY} plus whatever Capacity Module(s) are
     * installed -- see {@code IHaveModules#capacityBonus}. */
    public static int effectiveCapacity(ItemStack shatterStack) {
        return FUEL_CAPACITY + IHaveModules.capacityBonus(shatterStack, ModDataComponentTypes.SHATTER_MODULE_DATA.get(), MODULE_SLOT_COUNT);
    }

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
                    // Released before reaching full charge -- no special, same as a plain right-click
                    // with nothing to trigger (2026-08-18, decided: charging must be held its full
                    // second, not just used as a same-tick fire-on-release shortcut).
                    yield elapsed >= CHARGE_TICKS ? release(stack, level, player) : cancelCharge(stack, level, player);
                }
                yield elapsed >= CHARGE_TICKS ? ShatterModeData.of(ShatterModeData.State.CHARGED, now) : null;
            }
            case CHARGED -> !holdingTrigger ? release(stack, level, player) : null;
        };

        if (next != null) stack.set(ModDataComponentTypes.SHATTER_MODE_DATA.get(), next);
    }

    /**
     * Trigger released -- fires the special against whatever's aimed at (a mob gets a single-target
     * burst, a block starts a crater), paying the fuel/hunger cost, or plays the {@code release}
     * animation and costs nothing if nothing's aimed at all, OR if no head is mounted -- "no head
     * installed = no special available at all" (same shape as Sunder's own no-chain state, see the
     * design notes' Heads section) applies here too, not just to the attribute-modifier penalty.
     * Mobs take precedence over blocks, same ordering the old design notes' left-click branching
     * used. Either way resets back to IDLE.
     */
    /**
     * Trigger released before {@link #CHARGE_TICKS} elapsed -- no special, no fuel/hunger cost, just
     * the same {@code stopUsingItem()}/{@code release} anim reset {@link #release} plays on its own
     * no-target branch. Kept separate from {@link #release} rather than folding into it with a flag,
     * since this path never touches target-finding, fuel, or head wear at all.
     */
    private ShatterModeData cancelCharge(ItemStack stack, Level level, Player player) {
        player.stopUsingItem();
        long id = GeoItem.getOrAssignId(stack, (net.minecraft.server.level.ServerLevel) level);
        triggerAnim(player, id, "Body", "release");
        return ShatterModeData.of(ShatterModeData.State.IDLE, level.getGameTime());
    }

    private ShatterModeData release(ItemStack stack, Level level, Player player) {
        LivingEntity target = hasMountedHead(stack) ? findTarget(level, player) : null;
        boolean fired;
        if (target != null && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            payForAttack(stack, player);
            wearHead(stack, HEAD_WEAR_PER_ATTACK);
            scheduleMobBurst(stack, serverLevel, player, target);
            fired = true;
        } else if (target == null && hasMountedHead(stack) && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.world.phys.BlockHitResult blockHit = findBlockHit(level, player);
            if (blockHit != null) {
                payForAttack(stack, player);
                startCrater(stack, serverLevel, player, blockHit);
                fired = true;
            } else {
                fired = false;
            }
        } else {
            fired = false;
        }

        // Explicit stopUsingItem() before swinging -- see SunderItem#tickSawing's own comment on
        // this exact limitation: ItemInHandRenderer renders the static held-item pose (never the
        // swing pose) for as long as isUsingItem() reads true, and vanilla only flips that false via
        // a real stopUsingItem() call. holdingTrigger going false in inventoryTick means the CLIENT
        // has already released the input, but nothing forces isUsingItem() itself false server-side
        // before this point -- without this call there's a race where the swing fires while
        // isUsingItem() is technically still true, and the static pose silently wins.
        player.stopUsingItem();

        // Normal vanilla arm-swing on a real hit, same as any ordinary attack -- GeckoLib's own item
        // model animation ("attack") carries Shatter's own visual, but the player's ARM never swings
        // for it on its own since this fires from startUsingItem's held-trigger path, not a left-click
        // attack. player.swing(hand) is the same call/network sync vanilla's own attack path uses.
        // Determined by which hand currently holds this exact stack, not player.getUsedItemHand() --
        // a foolproof check either way.
        if (fired) {
            player.swing(player.getMainHandItem() == stack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        }

        long id = GeoItem.getOrAssignId(stack, (net.minecraft.server.level.ServerLevel) level);
        triggerAnim(player, id, "Body", fired ? "attack" : "release");
        return ShatterModeData.of(ShatterModeData.State.IDLE, level.getGameTime());
    }

    /** Base multiplier at the worst (Crude) fuel grade -- "2x with an Iron head mounted" was the
     * explicit target (2026-08-12), chosen so the special reads as clearly stronger than a standard
     * swing even on the worst fuel, rather than only becoming special once a better grade is used. */
    private static final float SPECIAL_BASE_MULTIPLIER = 2.0F;

    /** 0.5 seconds -- the block-break/damage moment of the special was landing visibly before the
     * swing animation reached its own impact point; this defers BOTH the mob burst and the crater's
     * first wave to line up with it instead, rather than firing the world effect synchronously with
     * the release itself. */
    private static final long SPECIAL_IMPACT_DELAY_TICKS = 10;

    /** Pending single-target bursts, keyed by attacking player UUID -- same transient in-memory,
     * player-UUID-keyed shape {@link #PENDING_CRATERS} uses, advanced by {@link #tickBursts}. */
    private static final java.util.Map<java.util.UUID, PendingBurst> PENDING_BURSTS = new java.util.HashMap<>();

    private record PendingBurst(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> levelKey,
                                 java.util.UUID targetId, float damage, long fireTick) {}

    /** Computes the burst amount NOW (reading the live {@code ATTACK_DAMAGE} attribute, which
     * already includes Shatter's own base + the mounted head's damageShift, see
     * {@link #getDefaultAttributeModifiers}) but doesn't apply it until {@link #tickBursts} finds it
     * due -- see {@link #SPECIAL_IMPACT_DELAY_TICKS}. Computing the damage at release time rather
     * than at impact time is deliberate: it's what the player was actually charging up to deal,
     * unaffected by anything that changes about their gear in the half-second before it lands. */
    private void scheduleMobBurst(ItemStack stack, net.minecraft.server.level.ServerLevel level, Player player, LivingEntity target) {
        double totalDamage = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        float burst = (float) (totalDamage * SPECIAL_BASE_MULTIPLIER * speedRatio(stack));
        PENDING_BURSTS.put(player.getUUID(), new PendingBurst(level.dimension(), target.getUUID(), burst,
                level.getGameTime() + SPECIAL_IMPACT_DELAY_TICKS));
    }

    /** Advances every pending burst whose impact moment has arrived -- called once per server tick
     * from {@code ShatterEvents#onServerTick}, same wiring shape as {@link #tickCraters}. Re-resolves
     * both the attacking player and the target fresh by UUID rather than holding live references
     * across ticks; a target that died or unloaded in the meantime is simply skipped, not errored. */
    public static void tickBursts(net.minecraft.server.MinecraftServer server) {
        if (PENDING_BURSTS.isEmpty()) return;

        java.util.Iterator<java.util.Map.Entry<java.util.UUID, PendingBurst>> it = PENDING_BURSTS.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<java.util.UUID, PendingBurst> entry = it.next();
            PendingBurst pending = entry.getValue();

            net.minecraft.server.level.ServerLevel level = server.getLevel(pending.levelKey());
            Player player = level != null ? level.getPlayerByUUID(entry.getKey()) : null;
            if (level == null || player == null) {
                it.remove();
                continue;
            }
            if (level.getGameTime() < pending.fireTick()) continue;

            it.remove();
            if (level.getEntity(pending.targetId()) instanceof LivingEntity target && target.isAlive()) {
                target.hurt(player.damageSources().playerAttack(player), pending.damage());
            }
        }
    }

    /** Fuel grade's speed value relative to Crude's own -- the single axis driving both the mob
     * burst's damage multiplier and the crater's wave count (see the design notes' Fuel section:
     * "speed drives charge speed and damage"). 1.0 at Crude itself; empty/unrecognized fuel also
     * falls back to 1.0 rather than erroring, since charging is already gated on having SOME fuel
     * (see {@link #hasFuel}) -- this is a defensive fallback, not an expected path. */
    private float speedRatio(ItemStack stack) {
        FluidData data = stack.getOrDefault(getDataType(), FluidData.EMPTY);
        float crudeSpeed = biofuelSpeed(net.scruffy.dermicraft.fluid.ModFluids.SOURCE_CRUDE_SLURRY.get());
        if (data.isFluidEmpty() || crudeSpeed <= 0.0F) return 1.0F;

        float fuelSpeed = biofuelSpeed(data.getFluid());
        return fuelSpeed / crudeSpeed;
    }

    private static float biofuelSpeed(net.minecraft.world.level.material.Fluid fluid) {
        net.scruffy.dermicraft.property.BiofuelProperties props =
                BuiltInRegistries.FLUID.wrapAsHolder(fluid).getData(ModDataMaps.BIOFUELS);
        return props != null ? props.speed() : 0.1F; // Crude's own real baseline value as the ultimate fallback
    }

    /** Raycast along the player's standard block-interaction reach -- purely a "what's the player
     * aiming at" check (no state mutation), used to decide whether the release should start a crater
     * at all. Distinct from {@code faceStruck} in {@code ShatterEvents} (which re-derives the same
     * kind of hit for the left-click AoE) since that one runs from a `BlockEvent.BreakEvent` context
     * this class has no access to. */
    @Nullable
    private net.minecraft.world.phys.BlockHitResult findBlockHit(Level level, Player player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        double range = player.blockInteractionRange();
        Vec3 endPos = eyePos.add(lookVec.scale(range));

        net.minecraft.world.phys.BlockHitResult hit = level.clip(new net.minecraft.world.level.ClipContext(eyePos, endPos,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));

        return hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK ? hit : null;
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

    /** Real per-material speed, mirroring {@code DiggerItem}'s own shape: the mounted head's
     * {@code miningSpeed} only applies against pickaxe-appropriate blocks the head can actually
     * harvest ({@link #meetsMiningTier}) -- same scoping {@code isCorrectToolForDrops} uses, so a
     * too-low-tier head (or no head at all) falls back to vanilla's own default (bare-hand) speed
     * rather than digging fast with nothing to show for it. */
    @Override
    public float getDestroySpeed(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        if (!state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)) return super.getDestroySpeed(stack, state);
        if (!meetsMiningTier(stack, state)) return super.getDestroySpeed(stack, state);

        ShatterHeadProperties head = headProperties(stack);
        return head != null ? head.miningSpeed() : super.getDestroySpeed(stack, state);
    }

    ////////////////////Special: block crater\\\\\\\\\\\\\\\\\\\\

    /** Base wave count at Crude (worst grade) -- see the design notes' crater section. */
    private static final int CRATER_BASE_WAVES = 2;

    /** How much {@link #speedRatio} has to grow past 1.0 before the crater gains another wave --
     * calibrated off Concentrated Slurry's REAL speed value relative to Crude's (0.125 / 0.1 = 1.25,
     * a +0.25 edge), not an invented number, so "+1 wave per grade" holds exactly for that one known
     * step and scales proportionally for anything stronger. */
    private static final double CRATER_WAVE_STEP_RATIO = 0.25;

    /** Circular-mask radius for a brand-new layer's very first wave -- 1.5 is deliberate, not
     * rounded: every cell of the existing flat 3x3 AoE face sits at Euclidean distance <= sqrt(2) =
     * 1.414 from its center, so radius 1.5 reproduces that exact 9-cell shape ("the standard 3x3
     * pattern," per the design discussion) as wave 0's starting layer, while every ring added after
     * it is genuinely round (not another square ring) -- see {@link #craterWaveCells}. */
    private static final double CRATER_BASE_RADIUS = 1.5;

    /** 1 second -- "enough for the player to notice," the explicit reasoning given for this pacing,
     * and also a server-load safety valve (spreads a big crater's block-breaking across several
     * ticks instead of one). */
    private static final long CRATER_WAVE_INTERVAL_TICKS = 20;

    /** Waves in progress, keyed by player UUID -- transient server memory only (lost on
     * server-restart/reload, same as {@code EaterItem}'s own {@code AGGREGATE_PROGRESS} map uses for
     * its similarly time-spread mining action), advanced by {@link #tickCraters}. A player starting a
     * second crater before their first finishes simply overwrites the pending entry -- the earlier
     * one is abandoned mid-sequence, a known simplification, not handled specially. */
    private static final java.util.Map<java.util.UUID, PendingCrater> PENDING_CRATERS = new java.util.HashMap<>();

    private record PendingCrater(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> levelKey,
                                  BlockPos origin, net.minecraft.core.Direction intoDirection,
                                  int totalWaves, int wavesFired, long nextWaveTick) {}

    /** Schedules wave 0 through the SAME {@link #PENDING_CRATERS}/{@link #tickCraters} path every
     * later wave already uses, rather than firing it synchronously with the release -- wave 0 used
     * to fire instantly, which landed visibly before the swing animation's own impact point; now
     * every wave, including the first, waits out {@link #SPECIAL_IMPACT_DELAY_TICKS} from release. */
    private void startCrater(ItemStack stack, net.minecraft.server.level.ServerLevel level, Player player,
                              net.minecraft.world.phys.BlockHitResult hit) {
        int totalWaves = craterWaveCount(stack);
        net.minecraft.core.Direction intoDirection = hit.getDirection().getOpposite();
        BlockPos origin = hit.getBlockPos();

        PENDING_CRATERS.put(player.getUUID(), new PendingCrater(level.dimension(), origin, intoDirection,
                totalWaves, 0, level.getGameTime() + SPECIAL_IMPACT_DELAY_TICKS));
    }

    /** {@code CRATER_BASE_WAVES + floor((speedRatio - 1.0) / CRATER_WAVE_STEP_RATIO)} -- driven by
     * the fuel's actual numeric speed value rather than a per-grade lookup table, so a future fluid
     * added specifically to maximize crater size scales the wave count proportionally with no new
     * case needed here. No cap, deliberately -- "the only way to make a bigger crater is a better
     * fuel" (2026-08-12 decision), so there's no reason to clamp it. */
    private int craterWaveCount(ItemStack stack) {
        double ratio = speedRatio(stack);
        return CRATER_BASE_WAVES + (int) Math.floor((ratio - 1.0) / CRATER_WAVE_STEP_RATIO);
    }

    /**
     * One wave: breaks every newly-revealed cell (see {@link #craterWaveCells}), always drops
     * normally regardless of tier (unlike the left-click AoE's "leave untouched" rule -- see the
     * design notes for why the special is deliberately different here), wears the head per block at
     * the normal mining rate, and destroys the head outright -- ignoring remaining durability -- if
     * this wave touched even one block above the head's tier. The wave still finishes breaking
     * everything in it before that happens; the destruction only prevents FUTURE waves.
     *
     * <p>{@code Block.getDrops} is called with the real {@code stack} regardless of tier -- it
     * doesn't itself consult {@code isCorrectToolForDrops} (that gating happens externally, in the
     * normal player-mining pipeline this method deliberately bypasses), so passing the real tool
     * here is what makes "always drops normally" actually true rather than needing a fake stand-in
     * tool the way {@code ShatterEvents}' left-click AoE avoids for the opposite reason.
     *
     * @return true if the head was destroyed (an over-tier block was hit) -- the caller uses this to
     * stop scheduling further waves.
     */
    private static boolean fireCraterWave(ItemStack stack, net.minecraft.server.level.ServerLevel level, Player player,
                                           BlockPos origin, net.minecraft.core.Direction intoDirection, int waveIndex) {
        boolean overTier = false;

        for (BlockPos pos : craterWaveCells(origin, intoDirection, waveIndex)) {
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            if (state.getDestroySpeed(level, pos) < 0) continue; // unbreakable (bedrock, etc.)
            if (!state.getFluidState().isEmpty()) continue; // leave fluid blocks (source or flowing) alone

            if (!meetsMiningTier(stack, state)) overTier = true;

            List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
                    state, level, pos, level.getBlockEntity(pos), player, stack);
            level.destroyBlock(pos, false, player);
            for (ItemStack drop : drops) {
                net.minecraft.world.level.block.Block.popResource(level, pos, drop);
            }

            wearHead(stack, HEAD_WEAR_PER_BLOCK);
        }

        if (overTier) {
            clearMountedHead(stack);
            return true;
        }
        return false;
    }

    /** Every NEW cell wave {@code waveIndex} (0-based) adds -- prior waves' cells are already broken,
     * so this is a delta, not the full crater shape. For each depth layer 0..waveIndex (depth 0 =
     * the struck face itself), the layer's radius grows by 1 block of round radius per wave past its
     * own birth wave ({@code radius = CRATER_BASE_RADIUS + (waveIndex - depth)}); a layer's very
     * first appearance (its birth wave, {@code depth == waveIndex}) contributes its whole disk, every
     * later wave only contributes the newly-added ring past its previous radius -- this is what
     * makes each wave visibly "grow deeper and wider" (a fresh disk at the new deepest point, plus a
     * wider ring on every shallower layer) rather than recomputing overlapping geometry. Depth 0 ends
     * up with the largest radius and each deeper layer is strictly narrower -- an inverted cone, a
     * regular crater silhouette, not a flared one. */
    private static List<BlockPos> craterWaveCells(BlockPos origin, net.minecraft.core.Direction intoDirection, int waveIndex) {
        List<BlockPos> cells = new java.util.ArrayList<>();

        for (int depth = 0; depth <= waveIndex; depth++) {
            boolean brandNewLayer = depth == waveIndex;
            double newRadius = CRATER_BASE_RADIUS + (waveIndex - depth);
            double oldRadiusSq = brandNewLayer ? -1.0 : Math.pow(CRATER_BASE_RADIUS + (waveIndex - 1 - depth), 2);
            double newRadiusSq = newRadius * newRadius;
            int maxOffset = (int) Math.ceil(newRadius);

            for (int a = -maxOffset; a <= maxOffset; a++) {
                for (int b = -maxOffset; b <= maxOffset; b++) {
                    double distSq = (double) a * a + (double) b * b;
                    if (distSq > newRadiusSq) continue;
                    if (!brandNewLayer && distSq <= oldRadiusSq) continue;

                    cells.add(craterCellPos(origin, intoDirection, depth, a, b));
                }
            }
        }

        return cells;
    }

    /** The two perpendicular offsets (a, b) live in the plane orthogonal to {@code intoDirection}'s
     * axis -- same per-axis offset shape {@code ShatterEvents#facePositions} already uses for the
     * flat left-click AoE, just parameterized by depth here instead of being fixed at 0. */
    private static BlockPos craterCellPos(BlockPos origin, net.minecraft.core.Direction intoDirection, int depth, int a, int b) {
        BlockPos depthPos = origin.relative(intoDirection, depth);
        return switch (intoDirection.getAxis()) {
            case X -> depthPos.offset(0, a, b);
            case Y -> depthPos.offset(a, 0, b);
            case Z -> depthPos.offset(a, b, 0);
        };
    }

    /**
     * Advances every pending crater whose next wave is due -- called once per server tick from
     * {@code ShatterEvents#onServerTick}, not subscribed here directly, since GeckoLib items aren't
     * otherwise event-bus subscribers and adding one just for this would be an odd asymmetry.
     *
     * <p>Re-locates the player's currently-held Shatter stack fresh each wave rather than caching an
     * {@code ItemStack} reference in {@link PendingCrater} -- component writes (mode data, fuel,
     * mounted head) regularly produce a different object even for "the same" logical item, the same
     * lesson {@code WorkbenchMenu}'s own supplier-not-reference pattern is built around. A player no
     * longer holding Shatter in either hand (dropped it, switched away) simply abandons that pending
     * crater rather than erroring.
     */
    public static void tickCraters(net.minecraft.server.MinecraftServer server) {
        if (PENDING_CRATERS.isEmpty()) return;

        java.util.Iterator<java.util.Map.Entry<java.util.UUID, PendingCrater>> it = PENDING_CRATERS.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<java.util.UUID, PendingCrater> entry = it.next();
            PendingCrater pending = entry.getValue();

            net.minecraft.server.level.ServerLevel level = server.getLevel(pending.levelKey());
            Player player = level != null ? level.getPlayerByUUID(entry.getKey()) : null;
            if (level == null || player == null) {
                it.remove();
                continue;
            }
            if (level.getGameTime() < pending.nextWaveTick()) continue;

            ItemStack stack = findShatterStack(player);
            if (stack == null) {
                it.remove();
                continue;
            }

            boolean destroyed = fireCraterWave(stack, level, player, pending.origin(), pending.intoDirection(), pending.wavesFired());
            int nextWaveIndex = pending.wavesFired() + 1;
            if (destroyed || nextWaveIndex >= pending.totalWaves()) {
                it.remove();
            } else {
                entry.setValue(new PendingCrater(pending.levelKey(), pending.origin(), pending.intoDirection(),
                        pending.totalWaves(), nextWaveIndex, level.getGameTime() + CRATER_WAVE_INTERVAL_TICKS));
            }
        }
    }

    @Nullable
    private static ItemStack findShatterStack(Player player) {
        if (player.getMainHandItem().getItem() instanceof ShatterItem) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof ShatterItem) return player.getOffhandItem();
        return null;
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

    /** Shatter's Gadget Module loadout -- same shared Module system as Eater/Drinker/Sipping/Sunder
     * (see {@link ModDataComponentTypes#SHATTER_MODULE_DATA}). 1 general-purpose slot, same size as
     * Sunder's own. */
    public static final int MODULE_SLOT_COUNT = 1;
    public static final int MODULE_SLOT_CAPACITY = IHaveModules.DEFAULT_MODULE_SLOT_CAPACITY;
    public static final int MODULE_SLOT_X = SunderItem.MODULE_SLOT_X;
    public static final int MODULE_SLOT_Y = SunderItem.MODULE_SLOT_Y;

    /** Whether {@code shatterStack} currently has a Module tagged {@code moduleTag} installed --
     * same shape as SunderItem's own {@code hasModule}. */
    public static boolean hasModule(ItemStack shatterStack, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> moduleTag) {
        return IHaveModules.hasModule(shatterStack, ModDataComponentTypes.SHATTER_MODULE_DATA.get(), MODULE_SLOT_COUNT, moduleTag);
    }

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
        private final IItemHandlerModifiable moduleHandler;
        private boolean moduleSlotChanged = false;

        private ShatterSwapPanel(Supplier<ItemStack> gadgetStackSupplier, boolean fieldHosted) {
            this.gadgetStackSupplier = gadgetStackSupplier;
            this.fieldHosted = fieldHosted;
            this.moduleHandler = IHaveItemData.liveHandler(() -> new IHaveItemData.BulkItemHandler(gadgetStackSupplier.get(),
                    ModDataComponentTypes.SHATTER_MODULE_DATA.get(), MODULE_SLOT_COUNT, MODULE_SLOT_CAPACITY,
                    candidate -> candidate.is(net.scruffy.dermicraft.datagen.tag.ModTags.Items.MODULES)));
        }

        @Override
        public List<Slot> slots(int panelX, int panelY, BooleanSupplier active) {
            List<Slot> slots = new java.util.ArrayList<>(IHaveModules.buildModuleSlots(moduleHandler, MODULE_SLOT_COUNT,
                    panelX + MODULE_SLOT_X + 1, panelY + MODULE_SLOT_Y + 1, 0, active, () -> moduleSlotChanged = true,
                    slot -> IHaveModules.mayRemoveCapacityModule(gadgetStackSupplier.get(), ModDataComponentTypes.SHATTER_MODULE_DATA.get(),
                            MODULE_SLOT_COUNT, slot, FUEL_CAPACITY,
                            gadgetStackSupplier.get().getOrDefault(ModDataComponentTypes.FLUID_DATA.get(), FluidData.EMPTY).getFluidAmount())));
            slots.add(new HeadSlot(panelX + HEAD_SLOT_X + 1, panelY + HEAD_SLOT_Y + 1, active));
            slots.add(new FuelFillSlot(panelX + FUEL_SLOT_X + 1, panelY + FUEL_SLOT_Y + 1, active));
            return slots;
        }

        /** Same "a mounted part present at close counts as a completed swap" detection as Sunder's
         * own panel -- see that class's javadoc. */
        @Override
        public void onClosed(Player player) {
            if (fieldHosted && (hasMountedHead(gadgetStackSupplier.get()) || moduleSlotChanged)) {
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
                    data.getFluidAmount(), effectiveCapacity(stack)).withStyle(net.minecraft.ChatFormatting.GRAY));
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
