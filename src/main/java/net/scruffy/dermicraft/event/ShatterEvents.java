package net.scruffy.dermicraft.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.scruffy.dermicraft.item.custom.ShatterItem;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.property.ShatterHeadProperties;
import net.scruffy.dermicraft.util.AutoSmeltUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shatter's left-click mining AoE -- flat 3x3 face, 1 block deep, oriented to whichever axis the
 * player was facing when the origin block broke (see the Shatter design notes in
 * {@code dermicraft-gadget-notes.md}). Scoped to the block-mining half only for now: hitting a mob
 * with a regular left-click does NOT spread to nearby mobs -- the design notes' old "AoE on a landed
 * mob hit" branch is dropped, not carried forward, since that needs real target-detection work
 * (planned separately, see the design notes' own open questions).
 *
 * <p>Hooks {@link BlockEvent.BreakEvent} rather than a custom mining path -- the origin block's own
 * break has already gone through every normal vanilla permission/gamemode/reach check by the time
 * this fires, so the design notes' "flying out of reach does nothing" exception falls out for free
 * (the origin break simply couldn't have happened). The 8 surrounding blocks are broken directly via
 * {@code level.destroyBlock}, which does NOT re-run those same checks -- a known gap for
 * protection-mod compatibility (claims, WorldGuard-alikes), not solved here.
 *
 * <p><b>Mining-tier mismatch (2026-08-12, revised again).</b> The struck block itself goes through
 * the normal vanilla mining pipeline untouched by this class -- real vanilla behavior for free (an
 * insufficient tool still breaks it, just with no drops, since {@link ShatterItem#isCorrectToolForDrops}
 * is what that pipeline consults). The 8 <em>surrounding</em> AoE blocks are different: an
 * insufficient tier leaves them completely untouched (not broken at all, not even for empty drops) --
 * only blocks the mounted head can actually mine get swept up in the AoE.
 */
@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class ShatterEvents {

    /** Thin wiring only -- the actual crater/burst advancement logic (and their own pending-state
     * maps) lives on {@link ShatterItem#tickCraters}/{@link ShatterItem#tickBursts}, not here;
     * GeckoLib items aren't event-bus subscribers themselves, so this class is just where the tick
     * hook has to live. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ShatterItem.tickCraters(event.getServer());
        ShatterItem.tickBursts(event.getServer());
    }

    /** Placeholder "heavy bonus" magnitude vs skeletons/wither skeletons -- flat additive, not a
     * multiplier, so it stacks the same way regardless of head material. Not tuned. */
    private static final float SKELETON_BONUS_DAMAGE = 3.0F;

    /** Placeholder "heavy bonus" durability damage per equipped armor piece -- on top of vanilla's
     * own normal 1-2 points of wear per hit taken, not replacing it. Not tuned. */
    private static final int ARMOR_BONUS_DURABILITY_DAMAGE = 3;

    private static final EquipmentSlot[] ARMOR_SLOTS =
            {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /**
     * Skeleton/wither bonus damage -- {@code AbstractSkeleton} covers Skeleton, Wither Skeleton, AND
     * Stray for free (a skeleton variant, matches the design notes' spirit even though not named
     * explicitly). Modifies the damage value itself via {@code LivingDamageEvent.Pre} (modern
     * NeoForge's own "adjust damage before it lands" hook, not the older {@code LivingHurtEvent}) --
     * a flat add, not a multiplier, so a weak/no-head swing still gets the same bonus a strong one
     * does. Extra drops/status effect explicitly scoped OUT of this pass (design notes still call
     * both "possibly", left for later).
     */
    @SubscribeEvent
    public static void onSkeletonBonusDamage(LivingDamageEvent.Pre event) {
        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon == null || !(weapon.getItem() instanceof ShatterItem)) return;
        if (!(event.getEntity() instanceof AbstractSkeleton)) return;

        event.setNewDamage(event.getNewDamage() + SKELETON_BONUS_DAMAGE);
    }

    /**
     * Heavy bonus armor durability damage, applied uniformly across every equipped armor piece --
     * see the design notes' "no chainmail exception" decision (dropped 2026-08-11, the old
     * piercing-logic reasoning for exempting chainmail doesn't hold for a blunt weapon). Fires on
     * {@code LivingDamageEvent.Post} (after the hit is finalized) rather than {@code .Pre}, so this
     * only wears armor on a hit that actually landed, not one that got fully blocked/no-opped.
     */
    @SubscribeEvent
    public static void onArmorBonusWear(LivingDamageEvent.Post event) {
        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon == null || !(weapon.getItem() instanceof ShatterItem)) return;
        if (event.getNewDamage() <= 0) return;

        LivingEntity target = event.getEntity();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = target.getItemBySlot(slot);
            if (!armor.isEmpty()) {
                armor.hurtAndBreak(ARMOR_BONUS_DURABILITY_DAMAGE, target, slot);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null || !(player.level() instanceof ServerLevel level)) return;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ShatterItem)) return;

        BlockPos origin = event.getPos();
        Direction.Axis axis = faceStruck(player, level, origin).getAxis();

        for (BlockPos pos : facePositions(origin, axis)) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            if (state.getDestroySpeed(level, pos) < 0) continue; // unbreakable (bedrock, etc.)
            if (!state.getFluidState().isEmpty()) continue; // leave fluid blocks (source or flowing) alone

            // Below the mounted head's tier -- left completely untouched, not swept into the AoE at
            // all (see the class javadoc). Only the struck block itself ever gets vanilla's own
            // "still breaks, just no drops" treatment; the surrounding blocks don't.
            if (!ShatterItem.meetsMiningTier(stack, state)) continue;

            // level.destroyBlock(pos, true, entity) does NOT actually use the entity's held item for
            // drop computation -- it unconditionally passes ItemStack.EMPTY internally (confirmed
            // against vanilla's own source, not assumed), so relying on it for drops would silently
            // produce nothing here even though the tier check above just confirmed Shatter CAN mine
            // this block. Computing drops manually with the real stack (same technique EaterItem's
            // Aggregate Module already uses, see its own comment) is what makes normal drops actually
            // happen.
            List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, stack);

            level.destroyBlock(pos, false, player);
            for (ItemStack drop : drops) {
                Block.popResource(level, pos, drop);
            }

            // The origin block's own wear happens in ShatterItem#mineBlock (vanilla's normal mining
            // hook) -- this is the AoE blocks' equivalent, same per-block magnitude. Reads the
            // mounted head fresh each iteration, so a head that breaks mid-swing correctly stops
            // contributing tier to the remaining blocks in this same AoE (meetsMiningTier reads
            // "no head" from that point on) rather than needing separate handling here.
            ShatterItem.wearHead(stack, ShatterItem.HEAD_WEAR_PER_BLOCK);
        }
    }

    /** The actual face the player struck to break {@code origin} -- a fresh raycast, since
     * {@code BlockEvent.BreakEvent} doesn't carry the {@code BlockHitResult} itself. Falls back to
     * the look vector's nearest axis (the old, less precise approximation) only if the raycast
     * somehow doesn't land back on the same block that just broke -- shouldn't normally happen, since
     * the player was looking straight at it to break it in the first place. */
    private static Direction faceStruck(Player player, ServerLevel level, BlockPos origin) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        double range = player.blockInteractionRange();
        Vec3 endPos = eyePos.add(lookVec.scale(range));

        BlockHitResult hit = level.clip(new ClipContext(eyePos, endPos,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(origin)) {
            return hit.getDirection();
        }

        return Direction.getNearest(lookVec.x, lookVec.y, lookVec.z);
    }

    /** The 8 positions surrounding {@code origin} in the plane perpendicular to {@code axis} --
     * the flat 3x3 face (no 2-deep center, that was retired with the old drill head -- see the
     * design notes). */
    private static List<BlockPos> facePositions(BlockPos origin, Direction.Axis axis) {
        List<BlockPos> positions = new ArrayList<>(8);
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;
                positions.add(switch (axis) {
                    case X -> origin.offset(0, a, b);
                    case Y -> origin.offset(a, 0, b);
                    case Z -> origin.offset(a, b, 0);
                });
            }
        }
        return positions;
    }

    /**
     * Gold's signature trait -- a mining-side "weak Fortune/Looting" bonus, mirroring
     * {@code SunderEvents#onLivingDropsLootBonus}'s exact mechanic (a per-drop chance to duplicate
     * each item), but restricted to {@code Tags.Blocks.ORES} -- see {@link ShatterHeadProperties}'
     * own javadoc for why. Fires for both the origin block (normal vanilla mining) and the AoE's
     * surrounding blocks ({@code level.destroyBlock} in {@link #onBlockBroken} still routes through
     * this same NeoForge-patched drops event either way). Rolls against a snapshot of the drops
     * list (via {@code List.copyOf}), same reason as Sunder's version: adding to a collection while
     * iterating it throws {@code ConcurrentModificationException}.
     */
    @SubscribeEvent
    public static void onBlockDropsLootBonus(net.neoforged.neoforge.event.level.BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (!(tool.getItem() instanceof ShatterItem)) return;

        if (!event.getState().is(net.neoforged.neoforge.common.Tags.Blocks.ORES)) return;

        net.scruffy.dermicraft.property.ShatterHeadProperties head = ShatterItem.headProperties(tool);
        if (head == null || head.lootBonusChance() <= 0.0f) return;

        var random = event.getLevel().getRandom();
        for (var drop : List.copyOf(event.getDrops())) {
            if (random.nextFloat() < head.lootBonusChance()) {
                event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(
                        event.getLevel(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy()));
            }
        }
    }

    /**
     * Blaze Essence's ignite-on-hit trait -- fires on {@code LivingDamageEvent.Post} (a hit that
     * actually landed), same shape as {@link #onArmorBonusWear}. Shatter has no sustained-attack
     * mode the way Sunder's SAWING does, so this always just rolls the plain chance -- see
     * {@link ShatterHeadProperties}'s own javadoc.
     */
    @SubscribeEvent
    public static void onIgniteOnHit(LivingDamageEvent.Post event) {
        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon == null || !(weapon.getItem() instanceof ShatterItem)) return;
        if (event.getNewDamage() <= 0) return;

        ShatterHeadProperties head = ShatterItem.headProperties(weapon);
        if (head == null || head.igniteChance() <= 0.0f) return;

        LivingEntity target = event.getEntity();
        if (target.getRandom().nextFloat() < head.igniteChance()) {
            target.igniteForSeconds(head.igniteFireSeconds());
        }
    }

    /**
     * Blaze Essence's other trait -- universal auto-smelt, a built-in "furnace enchantment": every
     * drop from a block this head mines gets substituted for its real {@link
     * net.minecraft.world.item.crafting.SmeltingRecipe} result (if one exists at all -- a block
     * with no smelting recipe just drops normally), with the recipe's own XP awarded too, same as
     * smelting it in a real Furnace. Deliberately NOT restricted to {@code Tags.Blocks.ORES} the
     * way Gold's loot bonus is -- see {@link ShatterHeadProperties}'s own javadoc for why this one
     * is meant to be universal. Same origin-block-and-AoE coverage as {@link #onBlockDropsLootBonus}
     * (both fire from the same {@link BlockDropsEvent}).
     */
    @SubscribeEvent
    public static void onBlockDropsAutoSmelt(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (!(tool.getItem() instanceof ShatterItem)) return;

        ShatterHeadProperties head = ShatterItem.headProperties(tool);
        if (head == null || !head.autoSmelt()) return;

        ServerLevel level = event.getLevel();
        float totalXp = 0f;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack original = drop.getItem();
            Optional<AutoSmeltUtil.SmeltResult> smelted = AutoSmeltUtil.smeltOne(level, original);
            if (smelted.isEmpty()) continue;

            AutoSmeltUtil.SmeltResult result = smelted.get();
            drop.setItem(result.result().copyWithCount(result.result().getCount() * original.getCount()));
            totalXp += result.experience() * original.getCount();
        }

        if (totalXp > 0) {
            AutoSmeltUtil.awardExperience(level, event.getPos(), totalXp);
        }
    }
}
