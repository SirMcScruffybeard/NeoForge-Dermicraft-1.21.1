package net.scruffy.dermicraft.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.scruffy.dermicraft.item.custom.ShatterItem;
import net.scruffy.dermicraft.main.Dermicraft;

import java.util.ArrayList;
import java.util.List;

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
}
