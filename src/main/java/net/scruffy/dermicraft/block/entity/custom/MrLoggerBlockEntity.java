package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.base.AbstractFarmBlockEntity;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.machine.MachineStatKey;
import net.scruffy.dermicraft.machine.SimpleMachineStats;
import net.scruffy.dermicraft.screen.custom.mr_logger.MrLoggerMenu;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mr. Logger: fells the trees in its field, plants saplings on every cell it can, and speeds the saplings along. Each work
 * step takes one row (or ring) of the field: for each cell, any log in the column above it (up to {@link #TREE_REACH}
 * blocks) is a tree, and the whole connected cluster of logs is felled, then the cell is replanted with a sapling of the
 * species it just cut (or any sapling in the input row). The leaves are cleared too unless the player turned that off.
 * Whatever the machine breaks or picks up in its field goes to the output buffer, except saplings: the greater half of them
 * goes to the buffer and the rest to the input row, the same split Mr. Farmer makes for carrots and potatoes.
 *
 * <p>It lights its field with invisible Light blocks like Mr. Farmer (saplings need light to grow); a tree grows through them,
 * since {@code minecraft:light} is in the replaceable-by-trees tag, and the next sweep puts the light back.
 *
 * <p>Fuel: felling costs one fuel step per {@code farm.logsPerFuelStep} logs (a config number); planting costs one step per
 * three cells planted, as the other machines' per-cell baseline.
 */
public class MrLoggerBlockEntity extends AbstractFarmBlockEntity {

    private static final int BUFFER_SLOT_COUNT = 9; // slot 0 = fuel container, 1-9 = output buffer
    /** How far above a field cell the machine looks for a trunk (a giant tree is about 30 blocks). */
    private static final int TREE_REACH = 40;
    /** The most logs one felling takes, so a forest of touching trees cannot stall a tick. */
    private static final int MAX_LOGS_PER_TREE = 256;
    /** Leaves within this many blocks (a cube) of a felled log are cleared with it. */
    private static final int LEAF_RADIUS = 5;
    private static final int MAX_LEAVES_PER_TREE = 1024;
    /** Cells planted that one fuel step pays for: the 3 x 3 field's row, the other machines' per-cell baseline. */
    private static final int CELLS_PER_PLANTING_STEP = 3;

    /** Whether a felled tree's leaves are harvested like shears (true, the leaf blocks themselves drop) or broken by hand (false, saplings, sticks and apples). */
    private boolean harvestLeaves = true;
    /** Whether the saplings get extra random ticks (faster growth), which costs fuel. */
    private boolean accelerateGrowth = true;

    // Transient sweep state: a sweep is a list of ordered steps (rows or rings), one per wave tick.
    private List<List<BlockPos>> waveSteps = null;
    private int waveCursor = 0;
    private int waveTimer = 0;
    // Every cell of the current sweep, for checking that a 2 x 2 lies wholly inside the field.
    private Set<BlockPos> fieldCells = Set.of();

    public MrLoggerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MR_LOGGER_BE.get(), pos, blockState, 1 + BUFFER_SLOT_COUNT, 1, BUFFER_SLOT_COUNT);
    }

    @Override
    public DeferredBlock<Block> getFarmBlock() {
        return ModBlocks.MR_LOGGER;
    }

    @Override
    protected String getMachineName() {
        return "mr_logger";
    }

    @Override
    protected MachineStatKey getStatKey() {
        return MachineStatKey.MR_LOGGER;
    }

    // ---- The leaf toggle --------------------------------------------------------------------------

    public boolean isHarvestingLeaves() {
        return harvestLeaves;
    }

    public void toggleHarvestLeaves() {
        harvestLeaves = !harvestLeaves;
        setChanged();
        updateBlock();
    }

    public boolean isAcceleratingGrowth() {
        return accelerateGrowth;
    }

    public void toggleAccelerateGrowth() {
        accelerateGrowth = !accelerateGrowth;
        setChanged();
        updateBlock();
    }

    // ---- Saplings (the input row) -----------------------------------------------------------------

    private static boolean isSapling(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SaplingBlock;
    }

    @Override
    protected boolean isInputItemValid(ItemStack stack) {
        return isSapling(stack);
    }

    /** Picked-up saplings are split like harvested ones: the greater half to the buffer, the rest to the input row. */
    @Override
    protected ItemStack routePickedUp(ItemStack stack) {
        if (!isSapling(stack)) return insertIntoBuffer(stack);
        int total = stack.getCount();
        int toBuffer = (total + 1) / 2;
        ItemStack leftBuffer = insertIntoBuffer(stack.copyWithCount(toBuffer));
        ItemStack leftInput = total - toBuffer > 0 ? insertIntoInput(stack.copyWithCount(total - toBuffer)) : ItemStack.EMPTY;
        int left = leftBuffer.getCount() + leftInput.getCount();
        return left <= 0 ? ItemStack.EMPTY : stack.copyWithCount(left);
    }

    /** What the machine breaks goes to the buffer (spilled if full), saplings split as above. */
    private void routeProduct(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemStack leftover = routePickedUp(stack);
        if (!leftover.isEmpty()) overflowToBufferOrDrop(level, pos, leftover);
    }

    // ---- Work ------------------------------------------------------------------------------------

    @Override
    protected void onFieldChanged() {
        waveSteps = null;
    }

    @Override
    protected void onOutOfFuel(Level level) {
        waveSteps = null;
        clearAllLights(level);
    }

    /** Solid ground a sapling could stand on (not a log or leaves, which a tree itself is made of). */
    private boolean isSoil(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty()) return false;
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) return false;
        return state.isFaceSturdy(level, pos, Direction.UP);
    }

    @Override
    protected void tickFarmWork(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (waveTimer > 0) {
            waveTimer--;
            return;
        }

        if (waveSteps == null || waveCursor >= waveSteps.size()) {
            waveSteps = resolveWaveSteps(level, this::isSoil, true);
            waveCursor = 0;
            fieldCells = new HashSet<>();
            for (List<BlockPos> row : waveSteps) fieldCells.addAll(row);
            // The lights go in at the start of a sweep as a minimal covering set of the whole field, so the saplings can grow.
            Set<BlockPos> desiredLights = computeLightPositions(waveSteps);
            reconcileLights(level, desiredLights);
            for (BlockPos light : desiredLights) placeLight(level, light);
        }

        List<BlockPos> step = waveSteps.get(waveCursor);
        boolean worked = false;
        int logs = 0;
        int planted = 0;
        int boosted = 0;
        for (BlockPos cell : step) {
            Block species = null;
            int felled = fellTree(serverLevel, cell);
            if (felled > 0) {
                logs += felled;
                worked = true;
                species = lastFelledSpecies;
            }
            BlockState state = level.getBlockState(cell);
            if (state.getBlock() instanceof SaplingBlock) {
                if (accelerateGrowth) {
                    accelerateSapling(serverLevel, cell, state);
                    boosted++;
                }
            } else {
                int put = plantSapling(serverLevel, cell, species);
                if (put > 0) {
                    planted += put;
                    worked = true;
                }
            }
        }
        if (collectItems(serverLevel, getPickupArea())) worked = true;

        if (logs > 0) burnFuel(logs / (float) SimpleMachineStats.getLogsPerFuelStep());
        if (planted > 0) burnFuel(planted / (float) CELLS_PER_PLANTING_STEP);
        if (boosted > 0) burnFuel(boosted / (float) CELLS_PER_PLANTING_STEP); // speeding saplings along costs fuel, per sapling, like planting
        if (worked) tickGradualProgress(currentWaveStepTicks()); // real production toward an evolution

        waveCursor++;
        if (waveCursor >= waveSteps.size()) {
            waveSteps = null;
            waveTimer = WAVE_COOLDOWN_TICKS;
        } else {
            waveTimer = currentWaveStepTicks();
        }
        setChanged();
    }

    /** The field's footprint, a block wider all round (a drop settles outside the field) and as tall as a tree. */
    private AABB getPickupArea() {
        List<List<BlockPos>> steps = waveSteps;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        if (steps != null) {
            for (List<BlockPos> step : steps) {
                for (BlockPos p : step) {
                    minX = Math.min(minX, p.getX());
                    minY = Math.min(minY, p.getY());
                    minZ = Math.min(minZ, p.getZ());
                    maxX = Math.max(maxX, p.getX());
                    maxY = Math.max(maxY, p.getY());
                    maxZ = Math.max(maxZ, p.getZ());
                }
            }
        }
        if (minX == Integer.MAX_VALUE) return new AABB(worldPosition);
        return new AABB(minX - 1, minY - 1, minZ - 1, maxX + 2, maxY + TREE_REACH + 1, maxZ + 2);
    }

    // ---- Felling -----------------------------------------------------------------------------------

    /** The block of the first log of the tree felled last, for choosing the sapling to replant. */
    @Nullable
    private Block lastFelledSpecies = null;

    /**
     * Fells the tree above the cell: the first log in the column (up to {@link #TREE_REACH} blocks), then every log connected
     * to it (corners count, since branches touch diagonally) up to {@link #MAX_LOGS_PER_TREE}. Leaves near the cut are broken
     * too when the toggle is on. Returns how many logs were felled; {@link #lastFelledSpecies} is set when it is not zero.
     */
    private int fellTree(ServerLevel level, BlockPos cell) {
        BlockPos first = null;
        for (int dy = 0; dy <= TREE_REACH; dy++) {
            BlockPos probe = cell.above(dy);
            if (level.getBlockState(probe).is(BlockTags.LOGS)) {
                first = probe;
                break;
            }
        }
        if (first == null) return 0;

        Set<BlockPos> logs = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        logs.add(first);
        queue.add(first);
        while (!queue.isEmpty() && logs.size() < MAX_LOGS_PER_TREE) {
            BlockPos at = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos next = at.offset(dx, dy, dz);
                        if (logs.size() >= MAX_LOGS_PER_TREE) break;
                        if (logs.contains(next) || !level.getBlockState(next).is(BlockTags.LOGS)) continue;
                        logs.add(next.immutable());
                        queue.add(next.immutable());
                    }
                }
            }
        }

        lastFelledSpecies = level.getBlockState(first).getBlock();
        List<ItemStack> drops = new ArrayList<>();
        for (BlockPos pos : logs) {
            breakInto(level, pos, drops, ItemStack.EMPTY);
        }
        {
            // The leaves always come down with the tree; the toggle only decides what they give (shears or bare hands).
            ItemStack leafTool = harvestLeaves ? new ItemStack(Items.SHEARS) : ItemStack.EMPTY;
            Set<BlockPos> leaves = new HashSet<>();
            for (BlockPos log : logs) {
                for (BlockPos near : BlockPos.betweenClosed(log.offset(-LEAF_RADIUS, -LEAF_RADIUS, -LEAF_RADIUS),
                        log.offset(LEAF_RADIUS, LEAF_RADIUS, LEAF_RADIUS))) {
                    if (leaves.size() >= MAX_LEAVES_PER_TREE) break;
                    BlockState state = level.getBlockState(near);
                    if (state.is(BlockTags.LEAVES)) leaves.add(near.immutable());
                }
            }
            for (BlockPos pos : leaves) {
                breakInto(level, pos, drops, leafTool);
            }
        }
        for (ItemStack drop : drops) {
            routeProduct(level, cell, drop);
        }
        return logs.size();
    }

    /** Breaks the block, adding what it drops to {@code drops} (merged into matching stacks, so a big felling stays a few stacks). */
    private static void breakInto(ServerLevel level, BlockPos pos, List<ItemStack> drops, ItemStack tool) {
        BlockState state = level.getBlockState(pos);
        for (ItemStack drop : Block.getDrops(state, level, pos, null, null, tool)) {
            boolean merged = false;
            for (ItemStack have : drops) {
                if (ItemStack.isSameItemSameComponents(have, drop) && have.getCount() < have.getMaxStackSize()) {
                    int move = Math.min(drop.getCount(), have.getMaxStackSize() - have.getCount());
                    have.grow(move);
                    drop = drop.copyWithCount(drop.getCount() - move);
                    if (drop.isEmpty()) {
                        merged = true;
                        break;
                    }
                }
            }
            if (!merged) drops.add(drop.copy());
        }
        level.destroyBlock(pos, false); // false = the drops are routed above, not spawned
    }

    // ---- Planting and growth -----------------------------------------------------------------------

    /** The sapling block a felled log's species grows from: {@code oak_log} to {@code oak_sapling}, {@code mangrove_log} to
     * its propagule, and the same for any mod wood named the same way; null if there is none. */
    @Nullable
    private static Block saplingFor(Block log) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(log);
        String name = id.getPath();
        if (name.startsWith("stripped_")) name = name.substring("stripped_".length());
        for (String suffix : new String[]{"_log", "_wood", "_stem", "_hyphae"}) {
            if (name.endsWith(suffix)) {
                name = name.substring(0, name.length() - suffix.length());
                break;
            }
        }
        for (String grown : new String[]{"_sapling", "_propagule"}) {
            Block candidate = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), name + grown))
                    .orElse(null);
            if (candidate instanceof SaplingBlock) return candidate;
        }
        return null;
    }

    /**
     * Plants at the cell if it is empty: the species just felled here if the input row has it and it can be planted, otherwise a
     * random species that can be. Returns how many saplings went in (0, 1, or 4 for a 2 x 2).
     *
     * <p>A species in {@link ModTags.Items#SAPLINGS_2X2} (dark oak) only grows as a 2 x 2 of four, so it is planted only as one:
     * the cell is the north-west corner of the square (the cells east and south of it complete it), all four must be field cells that
     * are empty with ground the sapling survives on, and the row must hold four of it. Otherwise that species is skipped here, never
     * planted alone. Every other species goes in one per cell, as before.
     */
    private int plantSapling(ServerLevel level, BlockPos cell, @Nullable Block felledLog) {
        BlockState there = level.getBlockState(cell);
        if (!(there.isAir() || there.canBeReplaced()) || !there.getFluidState().isEmpty()) return 0;

        Block preferred = felledLog == null ? null : saplingFor(felledLog);
        List<Integer> feasible = new ArrayList<>();
        int preferredSlot = -1;
        for (int i = 0; i < INPUT_INVENTORY.getSlots(); i++) {
            ItemStack stack = INPUT_INVENTORY.getStackInSlot(i);
            if (!isSapling(stack)) continue;
            Block sapling = ((BlockItem) stack.getItem()).getBlock();
            boolean fits = stack.is(ModTags.Items.SAPLINGS_2X2)
                    ? countInInput(stack) >= 4 && canPlantSquare(level, cell, sapling)
                    : sapling.defaultBlockState().canSurvive(level, cell);
            if (!fits) continue;
            feasible.add(i);
            if (sapling == preferred && preferredSlot < 0) preferredSlot = i;
        }
        if (feasible.isEmpty()) return 0;

        int slot = preferredSlot >= 0 ? preferredSlot : feasible.get(level.getRandom().nextInt(feasible.size()));
        ItemStack chosen = INPUT_INVENTORY.getStackInSlot(slot).copy();
        Block sapling = ((BlockItem) chosen.getItem()).getBlock();
        BlockState plant = sapling.defaultBlockState();

        if (chosen.is(ModTags.Items.SAPLINGS_2X2)) {
            takeFromInput(chosen, 4);
            for (BlockPos spot : squareAt(cell)) {
                level.setBlock(spot, plant, Block.UPDATE_ALL);
            }
            return 4;
        }
        INPUT_INVENTORY.extractItem(slot, 1, false);
        level.setBlock(cell, plant, Block.UPDATE_ALL);
        return 1;
    }

    /** The four cells of the 2 x 2 whose north-west corner is {@code corner} (east and south in world axes, so it never depends
     * on which way the machine faces). */
    private static BlockPos[] squareAt(BlockPos corner) {
        return new BlockPos[]{corner, corner.east(), corner.south(), corner.south().east()};
    }

    /** Whether all four cells of the 2 x 2 at {@code corner} are field cells, empty, and suit the sapling. */
    private boolean canPlantSquare(ServerLevel level, BlockPos corner, Block sapling) {
        BlockState plant = sapling.defaultBlockState();
        for (BlockPos spot : squareAt(corner)) {
            if (!fieldCells.contains(spot)) return false;
            BlockState state = level.getBlockState(spot);
            if (!(state.isAir() || state.canBeReplaced()) || !state.getFluidState().isEmpty()) return false;
            if (!plant.canSurvive(level, spot)) return false;
        }
        return true;
    }

    /** How many of this item the input row holds in all. */
    private int countInInput(ItemStack like) {
        int total = 0;
        for (int i = 0; i < INPUT_INVENTORY.getSlots(); i++) {
            ItemStack stack = INPUT_INVENTORY.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(stack, like)) total += stack.getCount();
        }
        return total;
    }

    /** Takes {@code count} of this item out of the input row, across its slots. */
    private void takeFromInput(ItemStack like, int count) {
        for (int i = 0; i < INPUT_INVENTORY.getSlots() && count > 0; i++) {
            if (!ItemStack.isSameItemSameComponents(INPUT_INVENTORY.getStackInSlot(i), like)) continue;
            count -= INPUT_INVENTORY.extractItem(i, count, false).getCount();
        }
    }

    /** Extra random ticks for a sapling, scaled by the fuel's heal rate (see Mr. Farmer's crop acceleration). */
    private void accelerateSapling(ServerLevel level, BlockPos pos, BlockState state) {
        float expected = FUEL_TANK.getHeal() * GROWTH_ATTEMPTS_PER_HEAL;
        int attempts = (int) expected;
        if (level.getRandom().nextFloat() < expected - attempts) attempts++;
        for (int i = 0; i < attempts; i++) {
            BlockState current = level.getBlockState(pos);
            if (!(current.getBlock() instanceof SaplingBlock)) break; // it grew into a tree
            current.randomTick(level, pos, level.getRandom());
        }
    }

    // ---- Range preview ---------------------------------------------------------------------------
    // The outline is the shared one (AbstractFarmBlockEntity#tickPreview); only the ground it is placed on is the Logger's.

    @Override
    protected GroundTest getPreviewGround() {
        return this::isSoil;
    }

    // ---- Faces and the Gate ---------------------------------------------------------------------------

    // The bottom face is an output only: the output buffer out, nothing in (no fluid access at all).
    @Nullable
    @Override
    public IFluidHandler getTank(@Nullable Direction direction) {
        return direction == Direction.DOWN ? null : super.getTank(direction);
    }

    @Override
    public IItemHandler getAutomationItemHandler(@Nullable Direction direction) {
        return direction == Direction.DOWN ? BUFFER_CHANNEL : super.getAutomationItemHandler(direction);
    }

    @Override
    public Component describeFace(Direction face) {
        return face == Direction.DOWN
                ? Component.translatable("tooltip.dermicraft.idep.face.mr_logger.bottom")
                : super.describeFace(face);
    }

    @Override
    public Component describeFluidFace(Direction face) {
        return face == Direction.DOWN
                ? Component.translatable("tooltip.dermicraft.idep.face.no_fluid")
                : super.describeFluidFace(face);
    }

    /** Fuel first (the machine stops dead without it), then the sapling row (fill only), then the output buffer. */
    @Override
    protected void addChannels(List<Channel> channels, boolean exposeFluid, boolean exposeItem) {
        if (exposeFluid) {
            channels.add(fuelChannel());
        }
        if (exposeItem) {
            channels.add(inputChannel("saplings", "Saplings"));
            channels.add(new Channel.ItemChannel("output", Component.literal("Output Buffer"), Channel.IO.OUT, BUFFER_CHANNEL));
        }
    }

    // ---- Saving ---------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("harvestLeaves", harvestLeaves);
        tag.putBoolean("accelerateGrowth", accelerateGrowth);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        harvestLeaves = !tag.contains("harvestLeaves") || tag.getBoolean("harvestLeaves");
        accelerateGrowth = !tag.contains("accelerateGrowth") || tag.getBoolean("accelerateGrowth");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MrLoggerMenu(containerId, playerInventory, this);
    }
}
