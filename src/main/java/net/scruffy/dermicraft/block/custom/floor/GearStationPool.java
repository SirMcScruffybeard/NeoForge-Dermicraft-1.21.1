package net.scruffy.dermicraft.block.custom.floor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.scruffy.dermicraft.recipe.gadget_fabricating.GadgetFabricatingRecipe;
import net.scruffy.dermicraft.util.ModFluidUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Item and fluid operations against a Gear Station's shared pool, on top of {@link FloorNetwork}'s
 * raw handler lists. Keeps pool <i>policy</i> (which fuel to pick, how much to move, how a recipe's
 * needs get checked/consumed) separate from pool <i>membership</i> (what counts as connected).
 */
public final class GearStationPool {

    private GearStationPool() {
    }

    /**
     * A read-only capture of every item and fluid currently in the pool, for repeated availability
     * checks against a single consistent view -- e.g. rendering several ingredient-count lines for
     * one selected recipe without re-walking the floor network once per line.
     */
    public record Snapshot(List<ItemStack> items, List<FluidStack> fluids) {

        /** Total count of {@code required}'s item across every matching stack in the snapshot. */
        public int itemCount(ItemStack required) {
            int total = 0;
            for (ItemStack candidate : items) {
                if (ItemStack.isSameItemSameComponents(candidate, required)) total += candidate.getCount();
            }
            return total;
        }

        /** Total amount of {@code required}'s fluid across every matching tank in the snapshot. */
        public int fluidAmount(FluidStack required) {
            int total = 0;
            for (FluidStack candidate : fluids) {
                if (FluidStack.isSameFluidSameComponents(candidate, required)) total += candidate.getAmount();
            }
            return total;
        }
    }

    /** Captures the pool's current contents once -- see {@link Snapshot}. Safe to call client-side
     * for GUI display; it only reads capabilities, never mutates. */
    public static Snapshot snapshot(Level level, BlockPos origin) {
        return new Snapshot(flattenItems(FloorNetwork.itemHandlers(level, origin)),
                flattenFluids(FloorNetwork.fluidHandlers(level, origin)));
    }

    /** Whether the pool currently holds enough of everything {@code recipe} needs -- read-only,
     * same safety as {@link #snapshot}. */
    public static boolean isAvailable(Level level, BlockPos origin, GadgetFabricatingRecipe recipe) {
        Snapshot pool = snapshot(level, origin);
        return recipe.testItems(pool.items()) && recipe.testFluids(pool.fluids());
    }

    /**
     * Atomically consumes {@code recipe}'s item and fluid costs from the pool -- re-verifies
     * availability against a fresh read immediately before draining anything, so a caller doesn't
     * need to snapshot first itself. Returns {@code false} (consuming nothing) if the pool can no
     * longer satisfy the recipe; server-only.
     */
    public static boolean consumeForRecipe(Level level, BlockPos origin, GadgetFabricatingRecipe recipe) {
        List<IItemHandler> itemSources = FloorNetwork.itemHandlers(level, origin);
        List<IFluidHandler> fluidSources = FloorNetwork.fluidHandlers(level, origin);

        if (!recipe.testItems(flattenItems(itemSources)) || !recipe.testFluids(flattenFluids(fluidSources))) {
            return false;
        }

        consumeItems(itemSources, recipe.items());
        consumeFluids(fluidSources, recipe.fluids());
        return true;
    }

    /**
     * Drains exactly {@code required}'s count of matching items from {@code sources}, sequentially
     * (fully draining one slot before moving to the next) -- same "walk order, not split evenly"
     * policy {@link #fillFromPool} already uses. Assumes the caller already verified total
     * availability (see {@link #consumeForRecipe}); this does not re-check or roll back.
     */
    private static void consumeItems(List<IItemHandler> sources, List<ItemStack> required) {
        for (ItemStack requirement : required) {
            int remaining = requirement.getCount();
            for (IItemHandler source : sources) {
                if (remaining <= 0) break;
                for (int slot = 0; slot < source.getSlots() && remaining > 0; slot++) {
                    ItemStack candidate = source.getStackInSlot(slot);
                    if (candidate.isEmpty() || !ItemStack.isSameItemSameComponents(candidate, requirement)) continue;

                    ItemStack extracted = source.extractItem(slot, remaining, false);
                    remaining -= extracted.getCount();
                }
            }
        }
    }

    /** Fluid counterpart to {@link #consumeItems}, same sequential drain policy. */
    private static void consumeFluids(List<IFluidHandler> sources, List<FluidStack> required) {
        for (FluidStack requirement : required) {
            drainAmount(sources, requirement, requirement.getAmount());
        }
    }

    /**
     * Drains up to {@code amount} mB of {@code fluidType}'s exact fluid/components from
     * {@code sources}, sequentially (fully draining one tank before moving to the next). Returns
     * how much was actually drained -- may be less than {@code amount} if the sources ran dry
     * mid-drain, so callers that need an atomic all-or-nothing debit must check availability
     * themselves first (see {@link #consumeForRecipe}'s pre-check, or Workbench Duty 3's own
     * pool-amount check via {@link #bestFuel}).
     */
    private static int drainAmount(List<IFluidHandler> sources, FluidStack fluidType, int amount) {
        int remaining = amount;
        for (IFluidHandler source : sources) {
            if (remaining <= 0) break;
            for (int tank = 0; tank < source.getTanks() && remaining > 0; tank++) {
                FluidStack candidate = source.getFluidInTank(tank);
                if (candidate.isEmpty() || !FluidStack.isSameFluidSameComponents(candidate, fluidType)) continue;

                FluidStack request = new FluidStack(candidate.getFluid(), Math.min(remaining, candidate.getAmount()));
                FluidStack drained = source.drain(request, IFluidHandler.FluidAction.EXECUTE);
                remaining -= drained.getAmount();
            }
        }
        return amount - remaining;
    }

    /**
     * Drains up to {@code amount} mB of {@code fluidType} from the station's shared pool -- the
     * general-purpose counterpart to {@link #consumeForRecipe} for callers that debit a computed
     * cost each cycle (e.g. Workbench Duty 3 repair) rather than a fixed recipe cost. Not atomic
     * against a shortfall on its own; pair with a {@link #bestFuel} amount check first.
     */
    public static int drainFuel(Level level, BlockPos origin, FluidStack fluidType, int amount) {
        return drainAmount(FloorNetwork.fluidHandlers(level, origin), fluidType, amount);
    }

    /**
     * Picks the best-graded biofuel currently in the pool (tier first, Speed tiebreak -- same
     * ranking {@link #chooseFluid} uses when filling a tank) and reports how much of it the pool
     * holds, packed into the returned stack's amount. Unlike {@link #chooseFluid} this has no
     * target tank to check room against, for callers that just need a quality+quantity reading
     * (e.g. Workbench Duty 3, which computes its own per-cycle repair/cost math from the modifiers
     * rather than transferring fluid into a tank). Returns {@link FluidStack#EMPTY} if the pool
     * holds no biofuel at all.
     */
    public static FluidStack bestFuel(Level level, BlockPos origin) {
        List<IFluidHandler> sources = FloorNetwork.fluidHandlers(level, origin);

        FluidStack best = FluidStack.EMPTY;
        for (IFluidHandler source : sources) {
            for (int tank = 0; tank < source.getTanks(); tank++) {
                FluidStack candidate = source.getFluidInTank(tank);
                if (candidate.isEmpty() || !ModFluidUtil.isBiofuel(candidate)) continue;
                if (best.isEmpty() || isBetterGrade(candidate, best)) best = candidate;
            }
        }
        if (best.isEmpty()) return FluidStack.EMPTY;

        int total = 0;
        for (IFluidHandler source : sources) {
            for (int tank = 0; tank < source.getTanks(); tank++) {
                FluidStack candidate = source.getFluidInTank(tank);
                if (!candidate.isEmpty() && FluidStack.isSameFluidSameComponents(candidate, best)) {
                    total += candidate.getAmount();
                }
            }
        }
        FluidStack result = best.copy();
        result.setAmount(total);
        return result;
    }

    private static List<ItemStack> flattenItems(List<IItemHandler> handlers) {
        List<ItemStack> items = new ArrayList<>();
        for (IItemHandler handler : handlers) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) items.add(stack);
            }
        }
        return items;
    }

    private static List<FluidStack> flattenFluids(List<IFluidHandler> handlers) {
        List<FluidStack> fluids = new ArrayList<>();
        for (IFluidHandler handler : handlers) {
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                FluidStack stack = handler.getFluidInTank(tank);
                if (!stack.isEmpty()) fluids.add(stack);
            }
        }
        return fluids;
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
