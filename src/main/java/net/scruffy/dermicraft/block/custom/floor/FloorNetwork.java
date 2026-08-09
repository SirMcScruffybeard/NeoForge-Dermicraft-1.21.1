package net.scruffy.dermicraft.block.custom.floor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.scruffy.dermicraft.datagen.tag.ModTags;

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
 * fluid handler adjacent to any floor tile reached -- the floor itself never stores anything, it's
 * purely the connector that decides which containers count as "the pool".
 *
 * <p><b>Deliberately stateless and block-entity-free.</b> Lab Floor blocks stay plain inert blocks;
 * this reads blockstates via the {@link ModTags.Blocks#LAB_FLOOR} tag on demand. That's affordable
 * because the current caller is a button press, not a tick loop. The full design's cached,
 * change-triggered "Knitting" recompute is deliberately NOT built here -- it becomes worth adding
 * when a per-tick consumer (passive refuel/repair) exists, and can be layered on without changing
 * this class's contract.
 *
 * <p><b>Not yet implemented from the design:</b> floor tier gating (reach-per-tier, weakest-link
 * fluid-hazard capability and throughput). Tier is intrinsic to the floor block TYPE, so it can be
 * resolved from the blockstates this walk already visits -- no per-tile storage, and therefore
 * still no block entity, when that lands.
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
     * touching the floor tile, from that neighbor's own perspective).
     */
    private record Neighbor(BlockPos pos, Direction accessDirection) {
    }

    /**
     * Every fluid handler reachable from {@code origin} across the Lab Floor network.
     *
     * <p>{@code origin} is the station's own position and is never itself collected -- a station
     * doesn't count as part of its own pool. Order is walk order (breadth-first from the station),
     * which callers should treat as arbitrary rather than a priority.
     */
    public static List<IFluidHandler> fluidHandlers(Level level, BlockPos origin) {
        List<IFluidHandler> handlers = new ArrayList<>();
        for (Neighbor neighbor : neighbors(level, origin)) {
            FluidUtil.getFluidHandler(level, neighbor.pos(), neighbor.accessDirection()).ifPresent(handlers::add);
        }
        return handlers;
    }

    /**
     * Every item handler reachable from {@code origin} across the Lab Floor network -- the item
     * counterpart to {@link #fluidHandlers}, and Fabrication's intended item-ingredient source (see
     * dermicraft-gear-stations-notes.md -> Fabrication page, "Item ingredients... connected to the
     * Workbench's own Gear Station Floor network"), not the Storage strip.
     */
    public static List<IItemHandler> itemHandlers(Level level, BlockPos origin) {
        List<IItemHandler> handlers = new ArrayList<>();
        for (Neighbor neighbor : neighbors(level, origin)) {
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, neighbor.pos(), neighbor.accessDirection());
            if (handler != null) handlers.add(handler);
        }
        return handlers;
    }

    /**
     * Every position touching the Lab Floor network reachable from {@code origin}, paired with the
     * direction to access it from -- the walk and neighbor-enumeration shared by {@link #fluidHandlers}
     * and {@link #itemHandlers} so both read the exact same network membership from a single pass.
     */
    private static List<Neighbor> neighbors(Level level, BlockPos origin) {
        Set<BlockPos> floorTiles = walkFloor(level, origin);
        if (floorTiles.isEmpty()) return List.of();

        List<Neighbor> neighbors = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();

        for (BlockPos tile : floorTiles) {
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = tile.relative(direction);
                // A floor tile is a connector, never a container; and the station asking is not part
                // of its own pool.
                if (floorTiles.contains(neighborPos) || neighborPos.equals(origin)) continue;
                if (!seen.add(neighborPos)) continue;

                neighbors.add(new Neighbor(neighborPos, direction.getOpposite()));
            }
        }
        return neighbors;
    }

    /**
     * Breadth-first flood-fill of the connected Lab Floor region touching {@code origin}.
     *
     * <p>"Touching" is any-face contact in all six directions (per the Lab Floor design), seeded
     * from the six neighbors of the station rather than the station's own position -- the station
     * is not a floor tile itself, it just has to be against one.
     */
    private static Set<BlockPos> walkFloor(Level level, BlockPos origin) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        for (Direction direction : Direction.values()) {
            BlockPos seed = origin.relative(direction);
            if (isFloor(level, seed) && visited.add(seed)) {
                queue.add(seed);
            }
        }

        while (!queue.isEmpty() && visited.size() < MAX_TILES) {
            BlockPos tile = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = tile.relative(direction);
                if (!isFloor(level, next)) continue;
                if (visited.size() >= MAX_TILES) break;
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }
        return visited;
    }

    private static boolean isFloor(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ModTags.Blocks.LAB_FLOOR);
    }
}
