package net.scruffy.dermicraft.block.custom.duct;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Shared base for the Innards Duct family (the dumb transport segment).
 *
 * <p>Holds all the connection/rendering logic: six per-face {@link DuctConnection} properties
 * computed from neighbours on placement and neighbour-change, exactly like a fence/pane. There is
 * <b>no block entity</b> - a duct is pure geometry; all routing/flow state lives on the Node.
 *
 * <p>Kept abstract so upgrade tiers can subclass and override the small tier-specific hooks
 * (currently just {@link #canConnectToPipe}) without duplicating the connection machinery.
 */
public abstract class AbstractInnardsDuctBlock extends Block {

    public static final EnumProperty<DuctConnection> NORTH = EnumProperty.create("north", DuctConnection.class);
    public static final EnumProperty<DuctConnection> EAST = EnumProperty.create("east", DuctConnection.class);
    public static final EnumProperty<DuctConnection> SOUTH = EnumProperty.create("south", DuctConnection.class);
    public static final EnumProperty<DuctConnection> WEST = EnumProperty.create("west", DuctConnection.class);
    public static final EnumProperty<DuctConnection> UP = EnumProperty.create("up", DuctConnection.class);
    public static final EnumProperty<DuctConnection> DOWN = EnumProperty.create("down", DuctConnection.class);

    public static final Map<Direction, EnumProperty<DuctConnection>> PROPERTY_BY_DIRECTION;

    static {
        EnumMap<Direction, EnumProperty<DuctConnection>> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, NORTH);
        map.put(Direction.EAST, EAST);
        map.put(Direction.SOUTH, SOUTH);
        map.put(Direction.WEST, WEST);
        map.put(Direction.UP, UP);
        map.put(Direction.DOWN, DOWN);
        PROPERTY_BY_DIRECTION = Map.copyOf(map);
    }

    // Collision box: a cube matching the node-hub (the core, 2-14). The thin arm/end stubs are left
    // non-solid on purpose - you bump the hub, not the stubs.
    private static final VoxelShape SHAPE = Block.box(2, 2, 2, 14, 14, 14);

    protected AbstractInnardsDuctBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, DuctConnection.NONE)
                .setValue(EAST, DuctConnection.NONE)
                .setValue(SOUTH, DuctConnection.NONE)
                .setValue(WEST, DuctConnection.NONE)
                .setValue(UP, DuctConnection.NONE)
                .setValue(DOWN, DuctConnection.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // Persistent slot-commit model: a freshly placed duct commits (in a fixed direction order)
        // to at most maxConnections() neighbours, but ONLY to a duct neighbour that still has a free
        // slot. An already-complete run's segments read as full, so a corner placed alongside an
        // existing leg won't steal a slot from -- or attach to -- that leg. Nodes/machines always
        // accept (they aren't slot-limited on their side), still counting toward our own two.
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = this.defaultBlockState();
        for (Direction dir : Direction.values()) {
            if (countConnections(state) >= maxConnections()) break;
            DuctConnection conn = placementConnection(level, pos, dir);
            if (conn != DuctConnection.NONE) {
                state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), conn);
            }
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Only the face toward the changed neighbour can change -- existing commitments on the other
        // faces are preserved (that persistence is exactly what lets a corner keep its two real
        // connections instead of being recomputed away). Connections are symmetric: a duct face
        // only turns PIPE if the neighbouring duct already connects back, so no one-sided arms.
        EnumProperty<DuctConnection> property = PROPERTY_BY_DIRECTION.get(direction);
        DuctConnection current = state.getValue(property);
        DuctConnection reconciled = reconcileFace(state, direction, neighborState, level, neighborPos, current);
        return reconciled == current ? state : state.setValue(property, reconciled);
    }

    /** Ducts are non-branching: exactly two ends. Overridable in case a tier ever needs otherwise. */
    protected int maxConnections() {
        return 2;
    }

    /**
     * The hazard tier of this duct: which fluids it will ever carry, regardless of which path
     * (Node-routed or the direct machine-to-machine drain) they're travelling. Mirrors
     * {@link net.scruffy.dermicraft.tank.VulnerableTank} exactly, since a duct is otherwise a
     * tankless conduit — this is the one place fluid tier is enforced for a run that never passes
     * through a Node's tank. {@link DuctRunResolver} accumulates every duct's filter along a run
     * (weakest-link semantics), so upgrade tiers only need to override this to accept more.
     */
    public abstract Predicate<FluidStack> fluidFilter();

    /** How many faces of {@code state} currently hold a connection (PIPE or END). */
    protected int countConnections(BlockState state) {
        int count = 0;
        for (Direction dir : Direction.values()) {
            if (state.getValue(PROPERTY_BY_DIRECTION.get(dir)) != DuctConnection.NONE) count++;
        }
        return count;
    }

    /** True if {@code state} has a slot left to commit (used only when the face in question is NONE). */
    private boolean hasFreeSlot(BlockState state) {
        return countConnections(state) < maxConnections();
    }

    /**
     * The connection a newly placed duct commits toward {@code dir}. Node -> PIPE, machine -> END
     * (both always accept), duct -> PIPE only if that duct still has a free slot, else NONE.
     */
    private DuctConnection placementConnection(LevelReader level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighbor = level.getBlockState(neighborPos);

        if (neighbor.getBlock() instanceof AbstractNodeBlock) {
            return DuctConnection.PIPE;
        }
        if (neighbor.getBlock() instanceof AbstractInnardsDuctBlock) {
            return hasFreeSlot(neighbor) ? DuctConnection.PIPE : DuctConnection.NONE;
        }
        if (level instanceof Level realLevel && exposesCapability(realLevel, neighborPos, dir.getOpposite())) {
            return DuctConnection.END;
        }
        return DuctConnection.NONE;
    }

    /**
     * Recompute a single face after the neighbour on {@code direction} changed, honouring existing
     * commitments and slot budget. Only ever turns a NONE face into a connection when there's room;
     * only ever clears a face when the neighbour is positively gone / no longer connecting back.
     */
    protected DuctConnection reconcileFace(BlockState state, Direction direction, BlockState neighborState,
                                           LevelAccessor level, BlockPos neighborPos, DuctConnection current) {
        // Node: always a valid endpoint. Keep an existing commitment; otherwise attach if we have room.
        if (neighborState.getBlock() instanceof AbstractNodeBlock) {
            if (current != DuctConnection.NONE) return current;
            return hasFreeSlot(state) ? DuctConnection.PIPE : DuctConnection.NONE;
        }

        // Duct: symmetric. Attach only if the neighbour already points back at us and we have room;
        // drop our arm if the neighbour has stopped pointing back.
        if (neighborState.getBlock() instanceof AbstractInnardsDuctBlock) {
            boolean neighborConnectsBack =
                    neighborState.getValue(PROPERTY_BY_DIRECTION.get(direction.getOpposite())) == DuctConnection.PIPE;
            if (neighborConnectsBack) {
                if (current == DuctConnection.PIPE) return current;
                return hasFreeSlot(state) ? DuctConnection.PIPE : DuctConnection.NONE;
            }
            return current == DuctConnection.PIPE ? DuctConnection.NONE : current;
        }

        // Anything else needs a full Level to classify (capability query). Without one we can't tell
        // a machine from a bare block, so we preserve whatever commitment this face already had
        // rather than risk clearing a live machine connection on a partial-level update.
        if (!(level instanceof Level realLevel)) {
            return current;
        }
        if (exposesCapability(realLevel, neighborPos, direction.getOpposite())) {
            if (current == DuctConnection.END) return current;
            return hasFreeSlot(state) ? DuctConnection.END : DuctConnection.NONE;
        }
        // Neighbour is air / a non-capability block (e.g. the previous duct or machine was removed):
        // clear this face and free the slot.
        return DuctConnection.NONE;
    }

    /** Whether the block at {@code pos} exposes an item or fluid handler on {@code face}. */
    private boolean exposesCapability(Level level, BlockPos pos, Direction face) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, face) != null
                || level.getCapability(Capabilities.FluidHandler.BLOCK, pos, face) != null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
