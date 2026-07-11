package net.scruffy.dermicraft.block.custom.duct;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Walks a duct run out from an origin (a Node leg, or a machine's direct drain face) to find what's
 * on the other end. A duct is strictly two-ended (see {@link AbstractInnardsDuctBlock#maxConnections()}),
 * so a run is always a straight walk: at each duct hop, find the one connected face that isn't where
 * we came from and step through it, until we hit something that isn't a duct.
 *
 * <p>A run ending at a Node is a valid endpoint (Node-to-Node chaining): the caller treats it like
 * any other capability-exposing block via the standard item/fluid capability lookups on
 * {@code endpoint.pos()}/{@code endpoint.accessDirection()} -- {@link NodeBlockEntity} gates
 * whether the transfer is actually allowed based on the target Node's own leg mode. The only case
 * resolved as inert here is a run that loops straight back to its own origin Node.
 *
 * <p>Every duct hop's {@link AbstractInnardsDuctBlock#fluidFilter()} is accumulated along the walk
 * (weakest-link AND) and returned on the {@link Endpoint} -- the one place fluid hazard tier is
 * enforced for a run that never passes through a Node's tank (e.g. the direct machine-to-machine
 * drain). A zero-hop endpoint (no duct at all between origin and target) carries an always-true
 * filter, since no duct's tier ever applies.
 */
public class DuctRunResolver {

    // Defensive cap matching the duct run size limit (64 blocks) -- the 2-connection invariant
    // already prevents loops, this just guards against ever spinning on a corrupted state.
    private static final int MAX_HOPS = 64;

    // Shared hop cap for machine-direct drains (fluid and item) -- paired with the 3x3-below
    // footprint bound so the drain stays a local drop rather than a base-spanning backbone.
    public static final int DRAIN_MAX_HOPS = 8;

    /** Where a resolved run ends: the block to interact with, the face touching the duct, and the
     * combined fluid-hazard filter of every duct hop crossed to get there. */
    public record Endpoint(BlockPos pos, Direction accessDirection, Predicate<FluidStack> fluidFilter) {
    }

    private DuctRunResolver() {
    }

    /** Standard resolve, used by Node legs: unbounded (aside from the defensive hop cap), no footprint restriction. */
    public static Optional<Endpoint> resolve(Level level, BlockPos originPos, Direction leg) {
        return walk(level, originPos, leg, MAX_HOPS, null);
    }

    /**
     * Resolve bounded to a 3x3xN column strictly below {@code originPos} -- every hop (duct or
     * final endpoint) must stay within one block horizontally of {@code originPos} and strictly
     * below its Y. Used by the machine-direct drain so it stays a local drop rather than a
     * base-spanning backbone; a plain {@link #resolve} run has no such restriction.
     */
    public static Optional<Endpoint> resolveWithinFootprint(Level level, BlockPos originPos, Direction leg, int maxHops) {
        Predicate<BlockPos> withinFootprint = pos ->
                Math.abs(pos.getX() - originPos.getX()) <= 1
                        && Math.abs(pos.getZ() - originPos.getZ()) <= 1
                        && pos.getY() < originPos.getY();
        return walk(level, originPos, leg, maxHops, withinFootprint);
    }

    private static Optional<Endpoint> walk(Level level, BlockPos originPos, Direction leg, int maxHops,
                                            Predicate<BlockPos> withinBounds) {
        BlockPos currentPos = originPos;
        Direction currentDir = leg;
        Predicate<FluidStack> combinedFilter = fluidStack -> true;

        for (int hop = 0; hop < maxHops; hop++) {
            BlockPos nextPos = currentPos.relative(currentDir);
            if (withinBounds != null && !withinBounds.test(nextPos)) return Optional.empty();

            BlockState nextState = level.getBlockState(nextPos);

            if (nextState.getBlock() instanceof AbstractNodeBlock) {
                // A run that loops straight back to its own origin isn't a real destination --
                // guard it here rather than let a Node try to push/pull into itself.
                if (nextPos.equals(originPos)) return Optional.empty();
                return Optional.of(new Endpoint(nextPos, currentDir.getOpposite(), combinedFilter));
            }

            if (!(nextState.getBlock() instanceof AbstractInnardsDuctBlock duct)) {
                return Optional.of(new Endpoint(nextPos, currentDir.getOpposite(), combinedFilter));
            }

            // Hopped into a duct -- confirm it actually connects back to us (mutual connection),
            // fold in its hazard filter, then find its other connected face to continue the walk.
            Direction incoming = currentDir.getOpposite();
            EnumProperty<DuctConnection> incomingProperty = AbstractInnardsDuctBlock.PROPERTY_BY_DIRECTION.get(incoming);
            if (nextState.getValue(incomingProperty) == DuctConnection.NONE) {
                return Optional.empty();
            }
            combinedFilter = combinedFilter.and(duct.fluidFilter());

            Direction far = null;
            for (Direction dir : Direction.values()) {
                if (dir == incoming) continue;
                EnumProperty<DuctConnection> property = AbstractInnardsDuctBlock.PROPERTY_BY_DIRECTION.get(dir);
                if (nextState.getValue(property) != DuctConnection.NONE) {
                    far = dir;
                    break;
                }
            }
            if (far == null) {
                return Optional.empty(); // dead-end duct, only one connection
            }

            currentPos = nextPos;
            currentDir = far;
        }

        return Optional.empty();
    }
}
