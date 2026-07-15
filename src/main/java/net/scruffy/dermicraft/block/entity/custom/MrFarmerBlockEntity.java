package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.custom.MrFarmerBlock;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.machine.MachineTier;
import net.scruffy.dermicraft.screen.custom.mr_farmer.MrFarmerMenu;
import net.scruffy.dermicraft.tank.FuelTank;
import net.scruffy.dermicraft.util.ModFluidUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MrFarmerBlockEntity extends MachineBaseBlockEntity implements MenuProvider, IHaveInventory {

    // Shared per-tier ring formula (see Shared mechanics in dermicraft-machine-notes.md):
    // unfueled = 1 block, fuel tier 1..4 = 3x3..9x9, capped at tier 4.
    private static final int MAX_RANGE_TIER = 4;

    // --- Range/wave geometry (see the "Range shape & wave" spec in dermicraft-machine-notes.md) ---
    private static final int VERTICAL_Y_REACH = 5;       // up/down: scan this many blocks along the facing axis
    private static final int HORIZONTAL_Y_TOLERANCE = 2; // forward: ±this many Y around the machine's own plane

    private static final int MAX_MOISTURE = 7; // FarmBlock.MOISTURE range is 0-7; 7 = fully hydrated
    // Extra random-ticks applied per immature crop per wave visit, per unit of fuel heal rate
    // (getHeal() is ~1.0 for Crude Slurry). Tuning knob for how strongly heal accelerates growth.
    private static final int GROWTH_ATTEMPTS_PER_HEAL = 2;
    private static final int LIGHT_LEVEL = 15;  // invisible Light block brightness placed over the field
    // Block light is LIGHT_LEVEL at the source and drops 1/block; crops need >=9 to grow, so one light
    // covers every crop within this many blocks. Used to place a minimal covering set, not one-per-cell.
    private static final int LIGHT_REACH = LIGHT_LEVEL - 9; // = 6

    // --- Wave sweep pacing ---
    private static final int WAVE_STEP_TICKS = 6;      // delay between rows/rings ("noticeable, not a bottleneck")
    private static final int WAVE_COOLDOWN_TICKS = 40; // pause between full sweeps

    // --- Range preview (particle visualization, triggered on GUI close) ---
    private static final int PREVIEW_DURATION_TICKS = 600; // ~30s the preview stays active after closing the GUI
    private static final int PREVIEW_FADE_TICKS = 100;     // over the last ~5s, taper particle density to fade out
    private static final int PREVIEW_PARTICLES_PER_TARGET = 3;

    private final FuelTank FUEL_TANK = createFuelTank();
    private final ItemStackHandler INVENTORY = createItemHandler(10); // slot 0 = fuel container, 1-9 = output buffer

    private boolean isTransferringFluids = false;

    // Transient preview state -- a sweep is a list of ordered steps (rows or rings); not persisted.
    // Runs only while previewTicksRemaining > 0 (set by startRangePreview() on GUI close), looping
    // the sweep to show the working area, then fading out.
    private int previewTicksRemaining = 0;
    private List<List<BlockPos>> previewWaveSteps = null;
    private int previewWaveCursor = 0;
    private int previewWaveTimer = 0;

    // Transient farming-wave state -- the real work sweep (fuel-gated), separate from the free
    // preview. Not persisted; a reload just restarts the sweep from scratch.
    private List<List<BlockPos>> farmWaveSteps = null;
    private int farmWaveCursor = 0;
    private int farmWaveTimer = 0;

    // Invisible Light blocks this machine has placed over its field. Persisted so they can always be
    // cleaned up (on unfuel, field change, or removal) even across a reload.
    private final Set<BlockPos> placedLights = new HashSet<>();

    // Hidden seed reserve: crop block -> banked seed count. Harvest feeds it, replant draws from it,
    // surplus above the per-type cap flows to the public buffer (held here if the buffer is full).
    // Persisted; dropped on block break. Same-crop replant means the crop block IS the key.
    private final Map<Block, Integer> seedReserve = new HashMap<>();

    public MrFarmerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MR_FARMER_BE.get(), pos, blockState);
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    public FuelTank getFuelTank() {
        return FUEL_TANK;
    }

    /** Working range as a side length (3, 5, 7, or 9), derived live from the fuel tank's tier. Unfueled = 1 (just this block). */
    public int getRange() {
        int tier = FUEL_TANK.getFuelTier();
        if (tier <= 0) return 1;
        int cappedTier = Math.min(tier, MAX_RANGE_TIER);
        return 1 + 2 * cappedTier;
    }

    private Direction getFacing() {
        return getBlockState().getValue(MrFarmerBlock.FACING);
    }

    /** Flips the ACTIVE blockstate (on/off texture) only when it actually changes; keeps the same BE. */
    private void setActive(Level level, boolean active) {
        BlockState state = getBlockState();
        if (state.getValue(MrFarmerBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(MrFarmerBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Tick -------------------------------------------------------------------------------

    public void tick(Level level) {
        if (level.isClientSide) return;

        tickRangePreview(level);
        tickFarming(level);
    }

    // ---- Farming wave (fuel-gated real work; slice 1 = skeleton, no block work yet) ----------

    private int fuelPerStep() {
        return Math.max(1, FUEL_TANK.getUseRate());
    }

    private void tickFarming(Level level) {
        // No fuel -> idle, forget the sweep, extinguish lights, and show the "off" texture.
        if (!FUEL_TANK.hasEnoughFuel(fuelPerStep())) {
            farmWaveSteps = null;
            clearAllLights(level);
            setActive(level, false);
            return;
        }
        setActive(level, true); // fueled -> "on" texture

        if (farmWaveTimer > 0) {
            farmWaveTimer--;
            return;
        }

        // Start a new sweep if we don't have one (or the previous one finished). Farming uses the
        // strict farmland test -- only actual farmland is worked, unlike the permissive preview.
        if (farmWaveSteps == null || farmWaveCursor >= farmWaveSteps.size()) {
            farmWaveSteps = resolveWaveSteps(level, this::isFarmland);
            farmWaveCursor = 0;
            if (isAllEmpty(farmWaveSteps)) {
                farmWaveSteps = null;
                clearAllLights(level);              // no farmland in range -> no lights
                farmWaveTimer = WAVE_COOLDOWN_TICKS;
                return;
            }
            // Recompute a minimal covering set of lights for the current field, then reconcile the
            // world to it (prune stale, place new). Done at sweep start rather than per-step so the
            // covering greedy sees the whole field.
            Set<BlockPos> desiredLights = computeLightPositions(farmWaveSteps);
            reconcileLights(level, desiredLights);
            for (BlockPos lp : desiredLights) placeLight(level, lp);
        }

        List<BlockPos> step = farmWaveSteps.get(farmWaveCursor);
        if (!step.isEmpty()) {
            processFarmStep(level, step);
            FUEL_TANK.useFuel(fuelPerStep()); // only burn fuel on steps that had real work
        }
        farmWaveCursor++;

        if (farmWaveCursor >= farmWaveSteps.size()) {
            farmWaveSteps = null;
            farmWaveTimer = WAVE_COOLDOWN_TICKS;
        } else {
            farmWaveTimer = WAVE_STEP_TICKS;
        }
        setChanged();
    }

    /**
     * Does the actual per-step farming work on one ring/row of crop positions (farmland is the block
     * directly below each). Slice 2 = hydration in; lighting/harvest/replant still to come.
     */
    private void processFarmStep(Level level, List<BlockPos> step) {
        if (!(level instanceof ServerLevel serverLevel)) return; // farming work is server-only
        for (BlockPos cropPos : step) {
            hydrate(level, cropPos.below());

            BlockState state = level.getBlockState(cropPos);
            if (isMatureCrop(state)) {
                harvestAndReplant(serverLevel, cropPos, state);
            } else if (state.isAir()) {
                replantRandom(serverLevel, cropPos); // fill genuinely-empty tilled ground
            } else {
                accelerateGrowth(serverLevel, cropPos, state); // immature crop -> fuel-heal-boosted growth
            }
        }
        // Push any seeds now over the cap toward the buffer (in case the buffer freed up since last pass).
        drainAllExcessSeeds(level);
        // Lighting isn't per-step -- it's a minimal covering set recomputed at sweep start (see tickFarming).
    }

    // ---- Harvest + replant ------------------------------------------------------------------

    /**
     * Harvests a mature crop and immediately replants the SAME crop from the seed reserve (falling back
     * to a random reserved seed, else leaving the cell empty). Seeds go to the reserve, products to the
     * buffer -- so no persistent crop-per-cell tracking is needed, the crop is known right here.
     */
    private void harvestAndReplant(ServerLevel level, BlockPos cropPos, BlockState state) {
        Block cropBlock = state.getBlock();
        Item seedItem = getSeedItem(level, state);

        List<ItemStack> drops = Block.getDrops(state, level, cropPos, null);
        level.destroyBlock(cropPos, false); // false = we route the drops ourselves

        for (ItemStack drop : drops) {
            if (drop.is(seedItem)) {
                bankSeeds(level, cropBlock, drop.getCount()); // seeds -> reserve (overflow to buffer)
            } else {
                overflowToBufferOrDrop(level, cropPos, drop); // products -> buffer, spill if full
            }
        }

        if (takeSeed(cropBlock)) {
            plantCrop(level, cropPos, cropBlock);
        } else {
            replantRandom(level, cropPos); // same-crop seed exhausted -> random fallback
        }
    }

    // Mature-crop test: covers vanilla wheat/carrot/potato/beetroot and modded crops that extend
    // CropBlock. TODO: broaden to non-CropBlock crops (nether wart, modded stems, etc.) if needed.
    private boolean isMatureCrop(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    // ---- Growth acceleration (fuel heal rate) -----------------------------------------------

    /**
     * Boosts an immature crop by applying extra vanilla random-ticks, count scaled by the fuel's heal
     * rate. Random-ticking respects light/moisture/fertility -- all of which the machine already maxes
     * -- so it reads as faster natural growth rather than a forced age jump. No-op on non-crops / at max age.
     */
    private void accelerateGrowth(ServerLevel level, BlockPos cropPos, BlockState state) {
        if (!(state.getBlock() instanceof CropBlock)) return;
        int attempts = Math.round(FUEL_TANK.getHeal() * GROWTH_ATTEMPTS_PER_HEAL);
        for (int i = 0; i < attempts; i++) {
            BlockState current = level.getBlockState(cropPos);
            if (!(current.getBlock() instanceof CropBlock crop) || crop.isMaxAge(current)) break;
            current.randomTick(level, cropPos, level.getRandom());
        }
    }

    // ---- Seed reserve -----------------------------------------------------------------------

    /** Per-crop-type reserve cap = ceil(field cells * 1.1) -- a full replant plus a 10% bumper. */
    private int seedCap() {
        int cells = getRange() * getRange();
        return (int) Math.ceil(cells * 1.1);
    }

    /** Banks harvested seeds; anything over the cap is pushed to the buffer (held here if buffer full). */
    private void bankSeeds(Level level, Block cropBlock, int count) {
        seedReserve.merge(cropBlock, count, Integer::sum);
        drainExcessSeeds(level, cropBlock);
        setChanged();
    }

    /** Moves seeds above the cap for one crop type into the buffer; leftovers stay held in the reserve. */
    private void drainExcessSeeds(Level level, Block cropBlock) {
        int count = seedReserve.getOrDefault(cropBlock, 0);
        int excess = count - seedCap();
        if (excess <= 0) return;
        Item seed = getSeedItem(level, cropBlock.defaultBlockState());
        int moved = insertCountIntoBuffer(seed, excess);
        if (moved > 0) {
            seedReserve.put(cropBlock, count - moved);
            setChanged();
        }
    }

    private void drainAllExcessSeeds(Level level) {
        for (Block b : new ArrayList<>(seedReserve.keySet())) {
            drainExcessSeeds(level, b);
        }
    }

    /** Consumes one seed of the given crop from the reserve; returns false if none banked. */
    private boolean takeSeed(Block cropBlock) {
        int count = seedReserve.getOrDefault(cropBlock, 0);
        if (count <= 0) return false;
        if (count == 1) seedReserve.remove(cropBlock);
        else seedReserve.put(cropBlock, count - 1);
        setChanged();
        return true;
    }

    /** Plants an age-0 crop of the given type at the cell. */
    private void plantCrop(Level level, BlockPos cropPos, Block cropBlock) {
        level.setBlock(cropPos, cropBlock.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    /** Plants a random reserved crop at an empty cell (no-op if the reserve is empty). */
    private void replantRandom(Level level, BlockPos cropPos) {
        List<Block> available = new ArrayList<>();
        for (Map.Entry<Block, Integer> e : seedReserve.entrySet()) {
            if (e.getValue() > 0) available.add(e.getKey());
        }
        if (available.isEmpty()) return;
        Block chosen = available.get(level.getRandom().nextInt(available.size()));
        if (takeSeed(chosen)) plantCrop(level, cropPos, chosen);
    }

    /** The plantable seed item for a crop (wheat->seeds, carrot->carrot, etc.). */
    private Item getSeedItem(Level level, BlockState state) {
        return state.getBlock().getCloneItemStack(level, worldPosition, state).getItem();
    }

    // ---- Buffer helpers ---------------------------------------------------------------------

    /** Inserts a stack into the buffer slots (1-9); returns whatever didn't fit. */
    private ItemStack insertIntoBuffer(ItemStack stack) {
        ItemStack remaining = stack;
        for (int slot = FUEL_TANK.SLOT + 1; slot < INVENTORY.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = INVENTORY.insertItem(slot, remaining, false);
        }
        return remaining;
    }

    /** Inserts up to `count` of an item into the buffer; returns how many were actually inserted. */
    private int insertCountIntoBuffer(Item item, int count) {
        int remaining = count;
        int maxStack = new ItemStack(item).getMaxStackSize();
        while (remaining > 0) {
            int size = Math.min(remaining, maxStack);
            ItemStack leftover = insertIntoBuffer(new ItemStack(item, size));
            int inserted = size - leftover.getCount();
            remaining -= inserted;
            if (inserted == 0) break; // buffer full
        }
        return count - remaining;
    }

    /** Buffer-full rule for products: into the buffer, spilling on the ground if there's no room. */
    private void overflowToBufferOrDrop(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemStack leftover = insertIntoBuffer(stack);
        if (!leftover.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), leftover);
        }
    }

    /** Forces farmland to max moisture -- no water source required (re-applied every sweep, so it stays wet). */
    private void hydrate(Level level, BlockPos farmlandPos) {
        BlockState state = level.getBlockState(farmlandPos);
        if (state.is(Blocks.FARMLAND) && state.getValue(FarmBlock.MOISTURE) < MAX_MOISTURE) {
            level.setBlock(farmlandPos, state.setValue(FarmBlock.MOISTURE, MAX_MOISTURE), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Lighting (invisible Light blocks, tracked for cleanup) ------------------------------

    /** Places an invisible Light block in the given air position and tracks it for later cleanup. */
    private void placeLight(Level level, BlockPos pos) {
        if (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LIGHT_LEVEL),
                    Block.UPDATE_CLIENTS);
            placedLights.add(pos.immutable());
        }
    }

    /** Removes one tracked Light block from the world if it's still ours (leaves other blocks alone). */
    private void removeLight(Level level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.LIGHT)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Extinguishes and forgets every placed Light block. No-op if none are placed. */
    private void clearAllLights(Level level) {
        if (placedLights.isEmpty()) return;
        for (BlockPos p : placedLights) removeLight(level, p);
        placedLights.clear();
        setChanged();
    }

    /**
     * Greedy minimal covering set: walk the field's crop cells and only choose a new light where the
     * cell isn't already lit >=9 by an already-chosen one. Lights are placed one block ABOVE the chosen
     * crop cell (so no crop cell is blocked). Returns the light-block positions to maintain.
     */
    private Set<BlockPos> computeLightPositions(List<List<BlockPos>> steps) {
        List<BlockPos> chosen = new ArrayList<>(); // crop cells picked to host an above-light
        for (List<BlockPos> step : steps) {
            for (BlockPos cell : step) {
                boolean covered = false;
                for (BlockPos c : chosen) {
                    if (c.above().distManhattan(cell) <= LIGHT_REACH) { covered = true; break; }
                }
                if (!covered) chosen.add(cell);
            }
        }
        Set<BlockPos> lights = new HashSet<>();
        for (BlockPos c : chosen) lights.add(c.above().immutable());
        return lights;
    }

    /** Removes any placed lights not in the desired covering set (e.g. after the range shrank). */
    private void reconcileLights(Level level, Set<BlockPos> desired) {
        Iterator<BlockPos> it = placedLights.iterator();
        boolean removedAny = false;
        while (it.hasNext()) {
            BlockPos p = it.next();
            if (!desired.contains(p)) {
                removeLight(level, p);
                it.remove();
                removedAny = true;
            }
        }
        if (removedAny) setChanged();
    }

    /** Called from the block on removal so the field doesn't keep our lights after the machine is gone. */
    public void extinguishLights() {
        if (level != null) clearAllLights(level);
    }

    private boolean isAllEmpty(List<List<BlockPos>> steps) {
        for (List<BlockPos> s : steps) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    // Strict farmland test used by the farming wave (vs. the preview's permissive surface test).
    // TODO: broaden to modded farmland (no vanilla farmland tag exists; revisit when needed).
    private boolean isFarmland(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.FARMLAND);
    }

    /** Starts (or restarts) the ~30s range-preview visualization. Called on GUI close; see MrFarmerMenu. */
    public void startRangePreview() {
        previewTicksRemaining = PREVIEW_DURATION_TICKS;
        previewWaveSteps = null;   // recompute against the current fuel-tier range
        previewWaveCursor = 0;
        previewWaveTimer = 0;      // begin sweeping immediately
        setChanged();
    }

    // ---- Range preview (particle-only; loops the sweep, then fades) --------------------------

    private void tickRangePreview(Level level) {
        if (previewTicksRemaining <= 0) return;
        previewTicksRemaining--;

        if (previewWaveTimer > 0) {
            previewWaveTimer--;
            return;
        }

        // (Re)start the sweep, looping it for the duration of the preview window.
        if (previewWaveSteps == null || previewWaveCursor >= previewWaveSteps.size()) {
            // Preview marks any ground surface (not just farmland) so the player can see which
            // cells still need tilling. Farming logic will pass a strict farmland test instead.
            previewWaveSteps = resolveWaveSteps(level, this::isGroundSurface);
            previewWaveCursor = 0;
            if (previewWaveSteps.isEmpty()) {
                previewWaveSteps = null;
                previewWaveTimer = WAVE_COOLDOWN_TICKS; // nothing farmable in range; check again shortly
                return;
            }
        }

        emitPreviewParticles(level, previewWaveSteps.get(previewWaveCursor));
        previewWaveCursor++;

        if (previewWaveCursor >= previewWaveSteps.size()) {
            previewWaveSteps = null;
            previewWaveTimer = WAVE_COOLDOWN_TICKS; // brief pause, then loop again
        } else {
            previewWaveTimer = WAVE_STEP_TICKS;
        }
    }

    private void emitPreviewParticles(Level level, List<BlockPos> step) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        int count = previewParticleCount();
        if (count <= 0) return;
        for (BlockPos p : step) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                    count, 0.25, 0.25, 0.25, 0.0);
        }
    }

    /** Full density until the fade window, then a linear taper to 0 so the preview visibly fades out. */
    private int previewParticleCount() {
        if (previewTicksRemaining >= PREVIEW_FADE_TICKS) return PREVIEW_PARTICLES_PER_TARGET;
        return Math.round((float) PREVIEW_PARTICLES_PER_TARGET * previewTicksRemaining / PREVIEW_FADE_TICKS);
    }

    // ---- Target resolution -------------------------------------------------------------------
    // Returns the sweep as an ordered list of steps; each step is the set of crop positions (the
    // block directly above a qualifying ground block) processed together in one wave tick. The
    // ground test decides what counts as a valid base -- any surface for the preview, farmland
    // for actual farming.

    @FunctionalInterface
    private interface GroundTest {
        boolean test(Level level, BlockPos groundPos);
    }

    private List<List<BlockPos>> resolveWaveSteps(Level level, GroundTest ground) {
        Direction facing = getFacing();
        int range = getRange();
        if (facing.getAxis().isVertical()) {
            return resolveVerticalRings(level, facing, range, ground);
        }
        return resolveHorizontalRows(level, facing, range, ground);
    }

    // Horizontal facing: N wide x N deep field projected in front, swept row by row (nearest first).
    // Each column scans ±HORIZONTAL_Y_TOLERANCE around the machine's own plane for the topmost ground.
    private List<List<BlockPos>> resolveHorizontalRows(Level level, Direction facing, int range, GroundTest ground) {
        int half = (range - 1) / 2;
        Direction side = facing.getClockWise();
        List<List<BlockPos>> steps = new ArrayList<>();

        for (int depth = 1; depth <= range; depth++) {
            List<BlockPos> row = new ArrayList<>();
            for (int s = -half; s <= half; s++) {
                BlockPos columnBase = worldPosition.relative(facing, depth).relative(side, s);
                BlockPos target = scanColumnForGround(level, columnBase, HORIZONTAL_Y_TOLERANCE, ground);
                if (target != null) row.add(target);
            }
            steps.add(row); // keep empty rows so the wave keeps a uniform cadence
        }
        return steps;
    }

    // Vertical facing: N x N field centered under/over the machine on the first qualifying plane found
    // within VERTICAL_Y_REACH along the facing axis, swept as concentric square rings (center first).
    private List<List<BlockPos>> resolveVerticalRings(Level level, Direction facing, int range, GroundTest ground) {
        int half = (range - 1) / 2;

        BlockPos center = null;
        for (int d = 1; d <= VERTICAL_Y_REACH; d++) {
            BlockPos probe = worldPosition.relative(facing, d);
            if (ground.test(level, probe)) {
                center = probe.above(); // crop position on that plane
                break;
            }
        }
        if (center == null) return List.of();

        List<List<BlockPos>> steps = new ArrayList<>();
        for (int r = 0; r <= half; r++) {
            List<BlockPos> ring = new ArrayList<>();
            for (BlockPos p : ringPositions(center, r)) {
                if (ground.test(level, p.below())) ring.add(p);
            }
            steps.add(ring);
        }
        return steps;
    }

    /** Scans a column ±yTolerance (topmost first) for qualifying ground; returns the crop position above it, or null. */
    @Nullable
    private BlockPos scanColumnForGround(Level level, BlockPos base, int yTolerance, GroundTest ground) {
        for (int dy = yTolerance; dy >= -yTolerance; dy--) {
            BlockPos groundPos = base.offset(0, dy, 0);
            if (ground.test(level, groundPos)) {
                return groundPos.above();
            }
        }
        return null;
    }

    /** Crop positions on the horizontal plane at exactly Chebyshev distance r from center (r=0 is the center). */
    private List<BlockPos> ringPositions(BlockPos center, int r) {
        List<BlockPos> ring = new ArrayList<>();
        if (r == 0) {
            ring.add(center);
            return ring;
        }
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) == r) {
                    ring.add(center.offset(x, 0, z));
                }
            }
        }
        return ring;
    }

    // Preview ground test: any solid top a crop could sit on (dirt, grass, path, stone, farmland...),
    // so the preview lights up the whole reachable footprint and the player can see what still needs
    // tilling. The top-down column scan naturally passes over vegetation/crops to the real ground.
    private boolean isGroundSurface(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty()) return false;
        return state.isFaceSturdy(level, pos, Direction.UP) || state.is(Blocks.FARMLAND);
    }

    public IFluidHandler getTank(@Nullable Direction direction) {
        return FUEL_TANK;
    }

    /** Full-access handler used by the GUI (both slots, insert + extract). */
    public IItemHandler getItemHandler(@Nullable Direction direction) {
        return INVENTORY;
    }

    /**
     * Automation-facing item handler (all faces identical): buffer slots (1-9) are extract-only so
     * harvest can be piped out but nothing can be pushed in. The fuel slot (0) is player-only -- no
     * automated insert or extract -- so fuel containers can't be shuttled in/out by pipes; automated
     * fueling goes through the FluidHandler capability instead.
     */
    public IItemHandler getAutomationItemHandler(@Nullable Direction direction) {
        return AUTOMATION_HANDLER;
    }

    private final IItemHandler AUTOMATION_HANDLER = new IItemHandler() {
        @Override public int getSlots() { return INVENTORY.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return INVENTORY.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return INVENTORY.getSlotLimit(slot); }

        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; } // automation never inserts

        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack; // buffer is output-only; fuel slot is player-only
        }

        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == FUEL_TANK.SLOT) return ItemStack.EMPTY; // fuel slot player-only
            return INVENTORY.extractItem(slot, amount, simulate); // harvest out
        }
    };


    @Override
    public void drops() {
        super.drops(INVENTORY);
        dropSeedReserve();
    }

    /** Spills the hidden seed reserve as items so breaking the machine never silently eats banked seeds. */
    private void dropSeedReserve() {
        if (level == null) return;
        for (Map.Entry<Block, Integer> e : seedReserve.entrySet()) {
            Item seed = getSeedItem(level, e.getKey().defaultBlockState());
            int count = e.getValue();
            int maxStack = new ItemStack(seed).getMaxStackSize();
            while (count > 0) {
                int size = Math.min(count, maxStack);
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                        new ItemStack(seed, size));
                count -= size;
            }
        }
        seedReserve.clear();
    }

    private FuelTank createFuelTank() {
        return new FuelTank(MachineTier.BASIC.tankCapacity(), 0) {
            @Override
            protected void onContentsChanged() {
                if (level != null && !level.isClientSide) {
                    setChanged();
                    updateBlock();
                }
            }
        };
    }

    private ItemStackHandler createItemHandler(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                if (level != null && !level.isClientSide()) {
                    if (slot == FUEL_TANK.SLOT && !isTransferringFluids) {
                        biDirectionalFluidTransfer();
                        isTransferringFluids = false;
                    }
                    setChanged();
                    updateBlock();
                }
            }

            private void biDirectionalFluidTransfer() {
                if (FUEL_TANK.hasFluidHandlerInSlot(this, FUEL_TANK.SLOT)) {
                    isTransferringFluids = true;
                    FUEL_TANK.transferFluidToTank(this, FUEL_TANK.SLOT);
                } else {
                    transferToHandler();
                }
            }

            private void transferToHandler() {
                if (FUEL_TANK.hasEmptyFluidHandlerInSlot(this, FUEL_TANK.SLOT)) {
                    isTransferringFluids = true;

                    ItemStack stack = getStackInSlot(FUEL_TANK.SLOT);
                    if (stack.isEmpty()) return;

                    if (ModFluidUtil.hasEmptyFluidHandlerInSlot(this, FUEL_TANK.SLOT) && stack.getCount() > 1) {
                        ItemStack extra = stack.copy();
                        extra.shrink(1);

                        stack.setCount(1);
                        setStackInSlot(FUEL_TANK.SLOT, stack);

                        Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), extra);
                    } else {
                        setStackInSlot(FUEL_TANK.SLOT, stack);
                    }
                    FUEL_TANK.transferFluidFromTankToHandler(this, FUEL_TANK.SLOT);
                }
            }

            @Override
            public int getSlotLimit(int slot) {
                return slot == FUEL_TANK.SLOT && ModFluidUtil.hasEmptyFluidHandlerInSlot(this, slot)
                        ? 1 : super.getSlotLimit(slot);
            }

            @Override
            @NotNull
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (slot == FUEL_TANK.SLOT && ModFluidUtil.hasFluidHandlerInSlot(this, slot)) {
                    if (!getStackInSlot(slot).isEmpty()) {
                        return stack;
                    }
                    if (stack.getCount() > 1) {
                        if (simulate) {
                            ItemStack remainder = stack.copy();
                            remainder.shrink(1);
                            return remainder;
                        } else {
                            ItemStack singleInsert = stack.copyWithCount(1);
                            super.insertItem(slot, singleInsert, false);
                            ItemStack remainder = stack.copy();
                            remainder.shrink(1);
                            return remainder;
                        }
                    }
                }
                return super.insertItem(slot, stack, simulate);
            }
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", INVENTORY.serializeNBT(registries));
        tag.put("fuel", FUEL_TANK.writeToNBT(registries, new CompoundTag()));
        long[] lights = new long[placedLights.size()];
        int i = 0;
        for (BlockPos p : placedLights) lights[i++] = p.asLong();
        tag.putLongArray("lights", lights);

        ListTag reserve = new ListTag();
        for (Map.Entry<Block, Integer> e : seedReserve.entrySet()) {
            if (e.getValue() <= 0) continue;
            CompoundTag entry = new CompoundTag();
            entry.putString("block", BuiltInRegistries.BLOCK.getKey(e.getKey()).toString());
            entry.putInt("count", e.getValue());
            reserve.add(entry);
        }
        tag.put("seedReserve", reserve);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) INVENTORY.deserializeNBT(registries, tag.getCompound("inventory"));
        if (tag.contains("fuel")) FUEL_TANK.readFromNBT(registries, tag.getCompound("fuel"));
        placedLights.clear();
        for (long l : tag.getLongArray("lights")) placedLights.add(BlockPos.of(l));

        seedReserve.clear();
        ListTag reserve = tag.getList("seedReserve", Tag.TAG_COMPOUND);
        for (int i = 0; i < reserve.size(); i++) {
            CompoundTag entry = reserve.getCompound(i);
            Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.getString("block")));
            if (b != Blocks.AIR) seedReserve.put(b, entry.getInt("count"));
        }
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.MR_FARMER);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MrFarmerMenu(containerId, playerInventory, this);
    }
}
