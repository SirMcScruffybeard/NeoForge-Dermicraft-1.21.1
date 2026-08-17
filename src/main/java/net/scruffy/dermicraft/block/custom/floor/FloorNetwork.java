package net.scruffy.dermicraft.block.custom.floor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.hazard.HazardProfile;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves a Gear Station's shared resource pool by walking the Lab Floor network it sits on (see
 * dermicraft-gear-stations-notes.md -> Overview, and dermicraft-machine-notes.md -> Flesh Lab ->
 * Lab Floor). Flood-fills across touching Lab Floor blocks from the station, then collects every
 * fluid/item handler adjacent to any floor tile reached -- the floor itself never stores anything,
 * it's purely the connector that decides which containers count as "the pool".
 *
 * <p><b>Tier gating (built 2026-08-09):</b> reads each visited tile's {@link TieredFloor#hazardProfile()}
 * and {@link TieredFloor#reach()} straight off its blockstate -- no per-tile storage, still no block
 * entity. Two of the design's three tier consequences are implemented:
 * <ul>
 *   <li><b>Fluid-hazard capability</b> -- weakest-link across the WHOLE connected network (every
 *   visited tile's profile intersected together), not a true per-container shortest-path calculation.
 *   That's a deliberate simplification for this flat "one shared pool" architecture (there's no
 *   per-consumer routing concept anywhere else in the Gear Station system to hang a per-path
 *   calculation off of) -- the more precise per-path version belongs to the FL Core's own multiblock,
 *   which doesn't exist in code yet and can revisit this if/when it does.
 *   <li><b>Reach</b> -- genuinely per-floor (not weakest-link): each tile's own {@code reach()} decides
 *   how far a connection may skip past it in {@link #walkFloor} (floor-to-floor) and {@link #neighborsOf}
 *   (floor-to-container). No obstruction check on what occupies the skipped block(s) -- consistent with
 *   the rest of this mod's floor/duct systems not doing line-of-sight checks either.
 * </ul>
 * <b>Throughput, the design's third tier consequence, was scrapped</b> (2026-08-09) rather than built.
 *
 * <p><b>Deliberately stateless and block-entity-free.</b> Lab Floor blocks stay plain inert blocks;
 * this reads blockstates via the {@link ModTags.Blocks#LAB_FLOOR} tag on demand. Originally justified
 * as "the caller is a button press, not a tick loop" -- no longer strictly true (Workbench's Duty 2/3
 * and Fabrication now all walk this every {@code CRAFT_TICKS}), but the walk is still a bounded
 * (≤{@link #MAX_TILES}) flood-fill of cheap blockstate reads, not capability lookups, so it remains
 * affordable without the cached, change-triggered "Knitting" recompute the full design calls for.
 * Revisit if a real hot path shows up.
 */
public final class FloorNetwork {

    /**
     * Defensive bound on how many floor tiles one walk will visit. A 5x5 footprint is 25 tiles and
     * a multi-station platform is a small multiple of that, so this is generous -- it exists so a
     * pathologically large (or griefed) floor can't stall the server thread, not as a design limit.
     */
    private static final int MAX_TILES = 256;

    private FloorNetwork() {
    }

    /**
     * A position adjacent to the floor network, and the direction to access it from (i.e. the face
     * touching the floor tile, from that neighbor's own perspective). Still correct even when the
     * connection bridges a Tier 3-4 gap -- the direction is which face of the neighbor block is being
     * queried, independent of how many blocks of empty space sit between it and the floor tile.
     */
    private record Neighbor(BlockPos pos, Direction accessDirection) {
    }

    /** The connected floor region reached from a station, plus its weakest-link hazard profile. */
    private record Region(Set<BlockPos> tiles, HazardProfile hazardProfile) {
    }

    /**
     * Every fluid handler reachable from {@code origin} across the Lab Floor network, gated by the
     * network's weakest-link {@link HazardProfile} -- a fluid the network can't conduct is invisible
     * through the returned handlers (see {@link HazardGatedFluidHandler}), not merely excluded from
     * some separate check callers have to remember to run.
     *
     * <p>{@code origin} is the station's own position and is never itself collected -- a station
     * doesn't count as part of its own pool. Order is walk order (breadth-first from the station),
     * which callers should treat as arbitrary rather than a priority.
     */
    public static List<IFluidHandler> fluidHandlers(Level level, BlockPos origin) {
        Region region = walkFloor(level, origin);
        List<IFluidHandler> handlers = new ArrayList<>();
        for (Neighbor neighbor : neighborsOf(level, origin, region.tiles())) {
            FluidUtil.getFluidHandler(level, neighbor.pos(), neighbor.accessDirection())
                    .map(handler -> (IFluidHandler) new HazardGatedFluidHandler(handler, region.hazardProfile()))
                    .ifPresent(handlers::add);
        }
        return handlers;
    }

    /**
     * Every item handler reachable from {@code origin} across the Lab Floor network -- the item
     * counterpart to {@link #fluidHandlers}, and Fabrication's intended item-ingredient source (see
     * dermicraft-gear-stations-notes.md -> Fabrication page, "Item ingredients... connected to the
     * Workbench's own Gear Station Floor network"), not the Storage strip. Items aren't hazard-gated,
     * so this skips the wrapping step entirely.
     */
    public static List<IItemHandler> itemHandlers(Level level, BlockPos origin) {
        Region region = walkFloor(level, origin);
        List<IItemHandler> handlers = new ArrayList<>();
        for (Neighbor neighbor : neighborsOf(level, origin, region.tiles())) {
            // A double chest's RIGHT half exposes the same merged (both-halves) capability as its
            // LEFT half -- querying both positions would count every item in the chest twice.
            // Skipping RIGHT (keeping LEFT/SINGLE) still reaches the whole double chest through the
            // one handler, just without the duplicate.
            if (isRedundantChestHalf(level, neighbor.pos())) continue;

            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, neighbor.pos(), neighbor.accessDirection());
            if (handler != null) handlers.add(handler);
        }
        return handlers;
    }

    private static boolean isRedundantChestHalf(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) == ChestType.RIGHT;
    }

    /**
     * Every position touching {@code floorTiles} (within each tile's own reach), paired with the
     * direction to access it from -- shared by {@link #fluidHandlers} and {@link #itemHandlers} so
     * both read the exact same network membership from a single walk.
     */
    private static List<Neighbor> neighborsOf(Level level, BlockPos origin, Set<BlockPos> floorTiles) {
        if (floorTiles.isEmpty()) return List.of();

        List<Neighbor> neighbors = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();

        for (BlockPos tile : floorTiles) {
            int reach = reachAt(level, tile);
            for (Direction direction : Direction.values()) {
                for (int distance = 1; distance <= reach + 1; distance++) {
                    BlockPos candidate = tile.relative(direction, distance);
                    // Another floor tile marks this tile's own territory ending -- whatever's past
                    // it is that floor's own reach to worry about, not double-claimed from here.
                    if (floorTiles.contains(candidate)) break;
                    // The station asking is not part of its own pool, wherever it happens to sit.
                    if (candidate.equals(origin)) continue;
                    if (seen.add(candidate)) {
                        neighbors.add(new Neighbor(candidate, direction.getOpposite()));
                    }
                }
            }
        }
        return neighbors;
    }

    /**
     * Breadth-first flood-fill of the connected Lab Floor region touching {@code origin}, respecting
     * each tile's own {@link TieredFloor#reach()} to bridge gaps, and intersecting every visited
     * tile's {@link TieredFloor#hazardProfile()} into one network-wide weakest-link profile.
     *
     * <p>"Touching" is any-face contact in all six directions (per the Lab Floor design), seeded
     * from the six neighbors of the station rather than the station's own position -- the station
     * is not a floor tile itself, it just has to be directly against one (no reach for this specific
     * bootstrap connection).
     */
    private static Region walkFloor(Level level, BlockPos origin) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        HazardProfile profile = HazardProfile.ALL;

        for (Direction direction : Direction.values()) {
            BlockPos seed = origin.relative(direction);
            if (isFloor(level, seed) && visited.add(seed)) {
                queue.add(seed);
            }
        }

        while (!queue.isEmpty() && visited.size() < MAX_TILES) {
            BlockPos tile = queue.removeFirst();
            profile = profile.intersect(hazardProfileAt(level, tile));
            int reach = reachAt(level, tile);

            for (Direction direction : Direction.values()) {
                if (visited.size() >= MAX_TILES) break;
                BlockPos found = nextFloorWithinReach(level, tile, direction, reach);
                if (found != null && visited.add(found)) {
                    queue.add(found);
                }
            }
        }
        return new Region(visited, profile);
    }

    /** The first floor tile within {@code reach + 1} blocks of {@code tile} in {@code direction},
     * or null if none -- {@code reach == 0} is exactly the old adjacent-only behavior. */
    @Nullable
    private static BlockPos nextFloorWithinReach(Level level, BlockPos tile, Direction direction, int reach) {
        for (int distance = 1; distance <= reach + 1; distance++) {
            BlockPos candidate = tile.relative(direction, distance);
            if (isFloor(level, candidate)) return candidate;
        }
        return null;
    }

    /** Defaults to 0 (adjacent-only) for a floor block that, unexpectedly, isn't a {@link TieredFloor}
     * -- shouldn't happen now that every Lab Floor variant is a {@link LabFloorBlock}, but conservative
     * either way (a missing tier should never grant MORE reach than intended). */
    private static int reachAt(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof TieredFloor tiered ? tiered.reach() : 0;
    }

    /** Defaults to Tier 1 (safe fluids only) for the same unexpected case above -- conservative in
     * the same direction, never granting more hazard tolerance than intended. */
    private static HazardProfile hazardProfileAt(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof TieredFloor tiered ? tiered.hazardProfile() : HazardProfile.TIER_1;
    }

    private static boolean isFloor(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ModTags.Blocks.LAB_FLOOR);
    }
}
