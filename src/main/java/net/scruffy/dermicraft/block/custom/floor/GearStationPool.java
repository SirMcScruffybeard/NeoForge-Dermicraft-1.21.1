package net.scruffy.dermicraft.block.custom.floor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scruffy.dermicraft.util.ModFluidUtil;

import java.util.List;

/**
 * Fluid-side operations against a Gear Station's shared pool, on top of {@link FloorNetwork}'s raw
 * handler list. Keeps pool <i>policy</i> (which fuel to pick, how much to move) separate from pool
 * <i>membership</i> (what counts as connected).
 *
 * <p>Currently only the "Fill from pool" path is built -- see dermicraft-gear-stations-notes.md ->
 * Workbench -> Swap page. Fabrication's own item+fluid availability checks will layer on here.
 */
public final class GearStationPool {

    private GearStationPool() {
    }

    /**
     * Tops {@code target} off from the station's shared pool, following the design's fuel-type
     * priority (see the Storage strip notes -> "Fuel-type selection priority"): if the target
     * already holds fuel, only more of that same fluid is pulled; if it's empty, the best-graded
     * biofuel currently in the pool is chosen. The pool is only ever debited for what actually
     * transfers.
     *
     * @return total mB moved into {@code target} (0 if nothing was available or it was already full)
     */
    public static int fillFromPool(Level level, BlockPos origin, IFluidHandler target) {
        List<IFluidHandler> sources = FloorNetwork.fluidHandlers(level, origin);
        if (sources.isEmpty()) return 0;

        FluidStack wanted = chooseFluid(sources, target);
        if (wanted.isEmpty()) return 0;

        int totalMoved = 0;
        for (IFluidHandler source : sources) {
            // Re-checked every source rather than once up front -- the target's remaining room
            // shrinks as we go, and a full target should stop the loop, not keep draining.
            int room = target.fill(new FluidStack(wanted.getFluid(), Integer.MAX_VALUE), IFluidHandler.FluidAction.SIMULATE);
            if (room <= 0) break;

            FluidStack request = new FluidStack(wanted.getFluid(), room);
            if (source.drain(request, IFluidHandler.FluidAction.SIMULATE).isEmpty()) continue;

            // Drain for real, then fill what we actually got. Sized by the target's room (not the
            // source's contents) so nothing is ever pulled out that the target can't take in.
            FluidStack drained = source.drain(request, IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()) continue;
            totalMoved += target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        }
        return totalMoved;
    }

    /**
     * Which fluid this fill should move: the target's existing contents if it holds any (top-off
     * only, never mixed), otherwise the best-graded biofuel in the pool that the target will accept.
     * Grade is Biofuel tier first, Speed as the tiebreak -- the same ranking the fuel data map
     * already defines, rather than a bespoke ordering.
     */
    private static FluidStack chooseFluid(List<IFluidHandler> sources, IFluidHandler target) {
        for (int tank = 0; tank < target.getTanks(); tank++) {
            FluidStack existing = target.getFluidInTank(tank);
            if (!existing.isEmpty()) return existing;
        }

        FluidStack best = FluidStack.EMPTY;
        for (IFluidHandler source : sources) {
            for (int tank = 0; tank < source.getTanks(); tank++) {
                FluidStack candidate = source.getFluidInTank(tank);
                if (candidate.isEmpty() || !ModFluidUtil.isBiofuel(candidate)) continue;
                // Only worth ranking if the target would actually take it -- skips fuels this
                // particular gadget's tank rejects outright.
                if (target.fill(candidate, IFluidHandler.FluidAction.SIMULATE) <= 0) continue;
                if (best.isEmpty() || isBetterGrade(candidate, best)) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    private static boolean isBetterGrade(FluidStack candidate, FluidStack incumbent) {
        int candidateTier = ModFluidUtil.getTier(candidate);
        int incumbentTier = ModFluidUtil.getTier(incumbent);
        if (candidateTier != incumbentTier) return candidateTier > incumbentTier;
        return ModFluidUtil.getSpeed(candidate) > ModFluidUtil.getSpeed(incumbent);
    }
}
