package net.scruffy.dermicraft.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import org.jetbrains.annotations.Nullable;

public class OuterfaceBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    // status: 0 = OFF (valid machine, idle), 1 = ON (valid machine, active player), 2 = ERROR (no valid machine)
    public static final IntegerProperty STATUS = IntegerProperty.create("status", 0, 2);

    public final int OFF = 0;
    public final int ON = 1;
    public final int ERROR = 2;

    private static final VoxelShape SHAPE_NORTH = Block.box(0.0, 0.0, 14.0, 16.0, 16.0, 16.0);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 2.0);
    private static final VoxelShape SHAPE_EAST = Block.box(0.0, 0.0, 0.0, 2.0, 16.0, 16.0);
    private static final VoxelShape SHAPE_WEST = Block.box(14.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public OuterfaceBlock() {
        super(BlockBehaviour.Properties.of()
                .noOcclusion()
                .noCollission()
                .lightLevel(state -> {
                    int status = state.getValue(STATUS);

                    return getLightLevel(status);
                }));

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(STATUS, ERROR));

    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos machinePos = getBlockBehind(context.getClickedPos(), facing);
        int status = getStatus(context.getLevel(), machinePos);
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(STATUS, status);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STATUS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        switch (state.getValue(FACING)) {
            case SOUTH -> {
                return SHAPE_SOUTH;
            }
            case EAST -> {
                return SHAPE_EAST;
            }
            case WEST -> {
                return SHAPE_WEST;
            }
            default -> {
                return SHAPE_NORTH;
            }
        }
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0; // Does not block light like a solid block would
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        int currentStatus = state.getValue(STATUS);
        Direction facing = state.getValue(FACING);
        BlockPos machinePos = getBlockBehind(pos, facing);

        if (currentStatus == ERROR) {
            currentStatus = getStatus(level, machinePos);
            state.setValue(STATUS, currentStatus);

            if (currentStatus == ERROR) {
                return InteractionResult.FAIL;
            }
        }

        if (!level.isClientSide) {
            BlockState machineState = level.getBlockState(machinePos);

            if (isValidMachine(level, machinePos)) {
                turnOn(level, pos, state);

                //Open the machine's GUI
                if (machineState.hasBlockEntity()) {
                    BlockEntity blockEntity = level.getBlockEntity(machinePos);

                    if (blockEntity instanceof MenuProvider mp) {
                        if (player instanceof ServerPlayer sp) {
                            sp.openMenu(mp, buf -> {
                                buf.writeBlockPos(machinePos);
                            });
                        }
                    }
                }

                return InteractionResult.CONSUME;
            } else {
                showError(level, pos, state);
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction targetDirection = state.getValue(FACING).getOpposite();

        // If the block that changed is directly behind the Outerface
        if (direction == targetDirection) {
            boolean valid = isValidMachine((Level) level, neighborPos);
            int currentStatus = state.getValue(STATUS);

            if (!valid) {
                return state.setValue(STATUS, ERROR); // Machine broke -> ERROR
            } else if (currentStatus == ERROR) {
                return state.setValue(STATUS, OFF); // Machine returned -> OFF (Idle)
            }
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    private BlockPos getBlockBehind(BlockPos pos, Direction facing) {
        return pos.relative(facing.getOpposite());
    }

    private boolean isValidMachine(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ModTags.Blocks.HAS_SCREEN);
    }

    private int getStatus(Level level, BlockPos pos) {
        return isValidMachine(level, pos) ? OFF : ERROR;
    }

    public void turnOff(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(STATUS, OFF), 3);
    }

    public void turnOn(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(STATUS, ON), 3);
    }

    public void showError(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(STATUS, ERROR), 3);
    }

    private static int getLightLevel(int status) {
        if (status == 2) {
            return 8;  // ERROR state: Emits a dim, eerie warning glow (Level 8)
        } else if (status == 1) {
            return 5;  // ON state: Emits a faint processing indicator light (Level 5)
        }

        return 0;
    }
}
