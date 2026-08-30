package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.custom.MrShepardBlock;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.util.SlotRangeItemHandler;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.machine.MachineTier;
import net.scruffy.dermicraft.screen.custom.mr_shepard.MrShepardMenu;
import net.scruffy.dermicraft.tank.DroolingTank;
import net.scruffy.dermicraft.tank.FuelTank;
import net.scruffy.dermicraft.tank.ModFluidTank;
import net.scruffy.dermicraft.util.ModFluidUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.MenuProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Livestock counterpart to Mr. Farmer -- see dermicraft-machine-notes.md -> Mr. Shepard.
 * Unlike Farmer's block-position wave (crops don't move), Shepard scans a live AABB each work
 * cycle since animals move freely -- no wave-stepping needed for a first pass.
 */
public class MrShepardBlockEntity extends MachineBaseBlockEntity
        implements MenuProvider, IHaveInventory, IHasChannels {

    private static final int MAX_RANGE_TIER = 4; // shared tier->range formula, see Mr. Farmer

    private static final int VERTICAL_REACH = 4; // AABB extends this far above/below the machine's plane
    private static final int PICKUP_MARGIN = 1;  // pickup-only overreach on each horizontal side

    private static final int WAVE_STEP_TICKS_BASE = 40; // 2s at fuel speed 1.0, same idiom as Mr. Farmer
    private static final float MIN_SPEED_FOR_PACING = 0.1f;

    // --- Population cap ---
    // The cap is the BREEDING STOCK (adults maintained), not the total population -- a total-population
    // cap goes static by construction: you breed up to it, headroom hits zero, and nothing is ever
    // surplus to harvest. Babies live in a separate pipeline bounded by BABY_HEADROOM, and production
    // is the overflow as they mature past the cap and get culled.
    private static final int DEFAULT_POPULATION_CAP = 8;
    private static final int MIN_POPULATION_CAP = 2;   // always leave a breeding pair as seed stock
    private static final int MAX_POPULATION_CAP = 32;  // hard ceiling, mirrors the range formula's 9x9 cap
    private static final int BASE_POPULATION_CAP = 8;  // cap at Crude Slurry (heal 1.0); scales with heal
    private static final int BABY_HEADROOM = 4;        // babies allowed on top of the breeding stock

    // Extra seconds of maturation granted to each baby per work cycle, per unit of fuel heal. Kept
    // fractional (whole seconds + a probabilistic extra for the remainder) so weak fuel averages a
    // modest boost rather than rounding away to nothing -- same idiom as Mr. Farmer's crop growth.
    // First-pass anchor, open to tuning.
    private static final float GROWTH_SECONDS_PER_HEAL = 1.0f;

    // --- Range preview (static footprint guide, shown on GUI close) ---
    private static final int PREVIEW_DURATION_TICKS = 600; // ~30s
    private static final int PREVIEW_FADE_TICKS = 100;     // taper density over the last ~5s
    private static final int PREVIEW_EMIT_INTERVAL = 10;   // re-emit twice a second
    private static final int PREVIEW_PARTICLES_PER_CELL = 2;

    private static final int FOOD_SLOT_COUNT = 4;
    private static final int BUFFER_SLOT_COUNT = 9;

    // XP-gathering tank -- locked to Knowledge Essence, same fixed-target-fluid lock the Knowledge
    // Vat uses (DroolingTank with a constant supplier, since this tank's target never changes).
    // 1000mB (10 levels at the Vat's own 100mB/level rate) -- a compact catch-tray, not a Vat-sized
    // reservoir; this machine is meant to gather in passing, not act as long-term storage.
    private static final int XP_TANK_CAPACITY = ModFluidTank.BUCKET_VOLUME;
    private static final int XP_SLOT_INDEX = 1 + FOOD_SLOT_COUNT + BUFFER_SLOT_COUNT;
    private static final int INVENTORY_SIZE = XP_SLOT_INDEX + 1;

    private final FuelTank FUEL_TANK = createFuelTank();
    private final DroolingTank XP_TANK = createDroolingTank(XP_TANK_CAPACITY, XP_SLOT_INDEX, ModFluids.SOURCE_KNOWLEDGE_ESSENCE::get);
    // slot 0 = fuel container, 1..FOOD_SLOT_COUNT = breeding food, next BUFFER_SLOT_COUNT =
    // pickup/wool buffer, last slot = XP tank's own fill/drain container.
    private final ItemStackHandler INVENTORY = createItemHandler(INVENTORY_SIZE);

    // Per-channel views for the Gate (see getChannels). Feed only accepts pushes, the buffer is
    // drain-only -- the fuel container slot is deliberately in neither, staying player-only.
    private final IItemHandler FEED_CHANNEL =
            new SlotRangeItemHandler(INVENTORY, 1, FOOD_SLOT_COUNT, true, false);
    private final IItemHandler BUFFER_CHANNEL =
            new SlotRangeItemHandler(INVENTORY, 1 + FOOD_SLOT_COUNT, BUFFER_SLOT_COUNT, false, true);

    private boolean isTransferringFluids = false;
    private int waveTimer = 0;
    private int populationCap = DEFAULT_POPULATION_CAP;
    private int previewTicksRemaining = 0;

    // Last real fuel's heal value, and which fluid it came from. FuelTank.getHeal() returns 0.0 on an
    // EMPTY tank, which would collapse the max cap to the minimum and destructively clamp the player's
    // selection every single time the tank ran dry. So an empty tank deliberately keeps the last fuel's
    // numbers; the value only changes when a genuinely DIFFERENT fluid is loaded. (FuelTank rejects a
    // non-matching biofuel while non-empty, so a fuel change can only ever happen from empty.)
    private float rememberedHeal = 1.0f;
    private Fluid rememberedFuel = Fluids.EMPTY;

    public MrShepardBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MR_SHEPARD_BE.get(), pos, blockState);
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    public FuelTank getFuelTank() {
        return FUEL_TANK;
    }

    public DroolingTank getXpTank() {
        return XP_TANK;
    }

    public int getPopulationCap() {
        return populationCap;
    }

    /** Fuel-driven ceiling on the breeding stock the player is allowed to select. */
    public int getMaxPopulationCap() {
        int scaled = Math.round(BASE_POPULATION_CAP * rememberedHeal);
        return Math.max(MIN_POPULATION_CAP, Math.min(MAX_POPULATION_CAP, scaled));
    }

    /** Steps the player's selection. updateBlock() so the GUI readout changes live, not on reopen. */
    public void changePopulationCap(int delta) {
        int updated = Math.max(MIN_POPULATION_CAP, Math.min(getMaxPopulationCap(), populationCap + delta));
        if (updated == populationCap) return;
        populationCap = updated;
        setChanged();
        updateBlock();
    }

    /**
     * Recomputes the remembered fuel stats when a different fuel is loaded, and clamps the player's
     * selection down if the new fuel's ceiling is lower. The clamp is deliberately destructive --
     * going back to better fuel does NOT restore the old selection.
     */
    private void refreshFuelMemory() {
        if (FUEL_TANK.isEmpty()) return; // empty keeps the last fuel's numbers, by design
        Fluid current = FUEL_TANK.getFluid().getFluid();
        if (current == rememberedFuel) return; // same fuel topped up -- nothing to recompute

        rememberedFuel = current;
        rememberedHeal = FUEL_TANK.getHeal();

        int max = getMaxPopulationCap();
        if (populationCap > max) populationCap = max;
        setChanged();
    }

    public int getRange() {
        int tier = FUEL_TANK.getFuelTier();
        if (tier <= 0) return 1;
        int cappedTier = Math.min(tier, MAX_RANGE_TIER);
        return 1 + 2 * cappedTier;
    }

    private Direction getFacing() {
        return getBlockState().getValue(MrShepardBlock.FACING);
    }

    private void setActive(Level level, boolean active) {
        BlockState state = getBlockState();
        if (state.getValue(MrShepardBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(MrShepardBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
    }

    private float pacingSpeed() {
        return Math.max(MIN_SPEED_FOR_PACING, FUEL_TANK.getSpeed());
    }

    private int currentWaveStepTicks() {
        return Math.max(1, Math.round(WAVE_STEP_TICKS_BASE / pacingSpeed()));
    }

    private int fuelPerStep() {
        return Math.max(1, FUEL_TANK.getUseRate());
    }

    // ---- Tick ---------------------------------------------------------------------------------

    public void tick(Level level) {
        if (level.isClientSide) return;

        tickRangePreview(level); // free, runs unfueled -- it's a siting guide, not machine work

        if (!FUEL_TANK.hasEnoughFuel(fuelPerStep())) {
            setActive(level, false);
            return;
        }
        setActive(level, true);

        if (waveTimer > 0) {
            waveTimer--;
            return;
        }
        waveTimer = currentWaveStepTicks();

        if (!(level instanceof ServerLevel serverLevel)) return;

        // Shearing runs before culling so a sheep can never be harvested with its wool still on.
        // cullOverpopulation() also guards this explicitly, so the guarantee doesn't depend on order.
        AABB area = workArea();
        boolean didWork = false;
        didWork |= collectItems(serverLevel, pickupArea()); // wider than the rest -- see pickupArea()
        didWork |= collectXp(serverLevel, pickupArea()); // same area, same cadence as item pickup
        didWork |= shearSheep(serverLevel, area);
        didWork |= growBabies(serverLevel, area);
        didWork |= breedAnimals(serverLevel, area);
        didWork |= cullOverpopulation(serverLevel, area);

        if (didWork) {
            FUEL_TANK.useFuel(fuelPerStep());
            setChanged();
        }
    }

    // ---- Growth acceleration (fuel heal rate) --------------------------------------------------
    // Only babies maturing is accelerated -- NOT the adult post-breeding cooldown. Speeding up both
    // was judged overpowered; maturation is the longer timer and the one that actually gates
    // throughput, so the untouched parent cooldown stays a natural brake on herd turnover.

    private boolean growBabies(ServerLevel level, AABB area) {
        float heal = FUEL_TANK.getHeal();
        if (heal <= 0) return false;

        // Fractional average -> whole seconds plus a probabilistic extra for the remainder, so weak
        // fuel means "usually some, sometimes more" rather than always rounding down.
        float expected = heal * GROWTH_SECONDS_PER_HEAL;
        int seconds = (int) expected;
        if (level.getRandom().nextFloat() < expected - seconds) seconds++;
        if (seconds <= 0) return false;

        boolean any = false;
        for (Animal animal : level.getEntitiesOfClass(Animal.class, area, Animal::isAlive)) {
            if (!animal.isBaby()) continue;
            animal.ageUp(seconds); // vanilla takes seconds and converts to ticks internally
            any = true;
        }
        return any;
    }

    /**
     * The working volume. Horizontal mounts cover the machine's OWN row through {@code range} blocks
     * ahead; vertical mounts still use the older offset-centre box pending their rework.
     */
    private AABB workArea() {
        int range = getRange();
        int half = (range - 1) / 2;
        Direction facing = getFacing();

        if (facing.getAxis().isVertical()) {
            // Unchanged pending the vertical-facing rework -- this box bleeds past the machine's own
            // plane (see the divergence note in dermicraft-machine-notes.md).
            BlockPos center = worldPosition.relative(facing, half + 1);
            return new AABB(
                    center.getX() - half, center.getY() - VERTICAL_REACH, center.getZ() - half,
                    center.getX() + half + 1, center.getY() + VERTICAL_REACH + 1, center.getZ() + half + 1);
        }

        // Depth starts at 0 -- the machine's own row -- not 1 block ahead like Mr. Farmer's. A Shepard
        // set into a fence or wall line is flush with the pen, so items dropped directly beside it sit
        // in the machine's own plane; starting a block ahead would leave them permanently uncollected.
        Direction side = facing.getClockWise();
        AABB box = new AABB(worldPosition.relative(side, -half))
                .minmax(new AABB(worldPosition.relative(facing, range).relative(side, half)));
        return box.inflate(0, VERTICAL_REACH, 0);
    }

    /**
     * Item collection reaches one block further out than the working area on every horizontal side.
     * Drops physically drift and settle, so an item produced well inside the range can come to rest
     * against a fence post or pen wall just outside it -- and would then sit there permanently, since
     * nothing ever moves it back in. The margin applies to <em>pickup only</em>: breeding, shearing,
     * growth and culling all stay bounded by the real working area, so this widens what the machine
     * cleans up without widening what it manages.
     */
    private AABB pickupArea() {
        return workArea().inflate(PICKUP_MARGIN, 0, PICKUP_MARGIN);
    }

    // ---- Item pickup ----------------------------------------------------------------------------

    private boolean collectItems(ServerLevel level, AABB area) {
        boolean any = false;
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive)) {
            // Never take a player's own belongings. ItemEntity#getOwner() (TraceableEntity) returns
            // whoever threw the stack, and Player#drop sets it -- for both deliberate throws and death
            // drops -- so this is what actually stops Shepard hoovering up your inventory when you die
            // inside the pen. Machine output and mob loot both have a null thrower and pass through.
            if (itemEntity.getOwner() instanceof Player) continue;

            // Also honour the vanilla pickup grace period. On its own this is weak protection (only
            // ~10 ticks, well under one work cycle), but it costs nothing and covers other sources.
            if (itemEntity.hasPickUpDelay()) continue;

            ItemStack stack = itemEntity.getItem();
            ItemStack leftover = insertIntoBuffer(stack.copy());
            int taken = stack.getCount() - leftover.getCount();
            if (taken > 0) {
                stack.shrink(taken);
                if (stack.isEmpty()) itemEntity.discard();
                any = true;
            }
        }
        return any;
    }

    // ---- XP pickup ------------------------------------------------------------------------------
    // Same pickup area and work cycle as collectItems() -- called right alongside it in tick(), so
    // XP gathering happens exactly when/where item gathering does, not on its own separate timer.

    private static final int MB_PER_XP_POINT = 10; // felt number -- see XP_TANK's own 100mB/level convention

    private boolean collectXp(ServerLevel level, AABB area) {
        boolean any = false;
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, area, ExperienceOrb::isAlive)) {
            int value = orb.getValue();
            if (value <= 0) continue;

            // All-or-nothing per orb, same as a real player pickup -- a nearly-full tank shouldn't
            // split one orb's worth across two work cycles, so this simulates first and skips the
            // orb entirely (leaving it for a later cycle, or a player, to take) if it doesn't fully fit.
            FluidStack offer = new FluidStack(ModFluids.SOURCE_KNOWLEDGE_ESSENCE.get(), value * MB_PER_XP_POINT);
            if (XP_TANK.fill(offer, IFluidHandler.FluidAction.SIMULATE) < offer.getAmount()) continue;

            XP_TANK.fill(offer, IFluidHandler.FluidAction.EXECUTE);
            orb.discard();
            any = true;
        }
        return any;
    }

    // ---- Shearing ---------------------------------------------------------------------------------
    // No public Shearable API to hook in this NeoForge version, so shearing is done manually: mark
    // the sheep sheared and spawn 1-3 wool of its color as a world item entity, mirroring vanilla
    // ShearsItem's own behavior. Wool is swept into the buffer by the pickup pass on a later cycle.

    private boolean shearSheep(ServerLevel level, AABB area) {
        boolean any = false;
        for (Sheep sheep : level.getEntitiesOfClass(Sheep.class, area, Sheep::isAlive)) {
            if (sheep.isSheared() || sheep.isBaby()) continue;
            shearOne(level, sheep);
            any = true;
        }
        return any;
    }

    /** Shears one sheep: marks it sheared and drops 1-3 wool of its colour as a world item. */
    private void shearOne(ServerLevel level, Sheep sheep) {
        sheep.setSheared(true);
        int count = 1 + level.getRandom().nextInt(3);
        Item wool = BuiltInRegistries.ITEM.get(
                ResourceLocation.withDefaultNamespace(sheep.getColor().getName() + "_wool"));
        level.addFreshEntity(new ItemEntity(level, sheep.getX(), sheep.getY(), sheep.getZ(),
                new ItemStack(wool, count)));
        level.playSound(null, sheep.getX(), sheep.getY(), sheep.getZ(),
                SoundEvents.SHEEP_SHEAR, sheep.getSoundSource(), 1.0F, 1.0F);
    }

    // ---- Breeding -------------------------------------------------------------------------------
    // Breeding intentionally runs even when adults are AT the cap -- the breed->cull cycle IS the
    // product (that's the meat farm), not a bug to suppress. What has to be controlled is the RATE:
    // an earlier version set every eligible adult in love each cycle (8 cows -> 4 babies per ~2s)
    // while culling only one per cycle, a 4:1 imbalance that made the cap unenforceable. Hence:
    // strict pairing, ONE pair per species per work cycle, and a total ceiling of cap + BABY_HEADROOM
    // so the baby pipeline stays bounded.

    private boolean breedAnimals(ServerLevel level, AABB area) {
        Map<EntityType<?>, List<Animal>> bySpecies = groupBySpecies(level, area);
        boolean any = false;
        for (List<Animal> group : bySpecies.values()) {
            int inLove = 0;
            List<Animal> eligible = new ArrayList<>();
            for (Animal a : group) {
                if (a.isInLove()) {
                    inLove++;
                } else if (!a.isBaby() && a.getAge() == 0 && a.canFallInLove()) {
                    eligible.add(a);
                }
            }
            if (eligible.size() < 2) continue; // nothing to pair with

            // Total ceiling = breeding stock + baby pipeline, projected forward by the babies already
            // on the way from pairs put in love on an earlier cycle (two in-love parents = one baby).
            int projected = group.size() + inLove / 2;
            if (projected >= populationCap + BABY_HEADROOM) continue;

            // Exactly one pair per cycle -- the rate limit that keeps breeding from outrunning the
            // culler and letting the population run away.
            Animal first = eligible.get(0);
            Animal second = eligible.get(1);

            int slotA = findMatchingFood(first);
            if (slotA < 0) continue; // out of matching food for this species
            ItemStack takenA = INVENTORY.extractItem(slotA, 1, false);
            int slotB = findMatchingFood(second);
            if (slotB < 0) {
                INVENTORY.insertItem(slotA, takenA, false); // don't waste food on a half-pair
                continue;
            }
            INVENTORY.extractItem(slotB, 1, false);

            first.setInLove(null);
            second.setInLove(null);
            any = true;
        }
        return any;
    }

    private int findMatchingFood(Animal animal) {
        for (int slot = 1; slot <= FOOD_SLOT_COUNT; slot++) {
            ItemStack stack = INVENTORY.getStackInSlot(slot);
            if (!stack.isEmpty() && animal.isFood(stack)) return slot;
        }
        return -1;
    }

    // ---- Culling --------------------------------------------------------------------------------
    // Harvests surplus ADULTS -- the cap counts breeding stock, so babies are the pipeline feeding
    // this, not candidates for it (vanilla babies drop nothing, so culling one is pure waste).
    // One per work cycle, matching breeding's one-pair-per-cycle rate so the two stay balanced.
    // Target: the most-overpopulated species, then its oldest member -- culling worn-out breeders
    // and keeping the freshly matured. Drops land as world items and are swept up by collectItems().

    private boolean cullOverpopulation(ServerLevel level, AABB area) {
        Map<EntityType<?>, List<Animal>> bySpecies = groupBySpecies(level, area);

        List<Animal> worstAdults = null;
        for (List<Animal> group : bySpecies.values()) {
            List<Animal> adults = new ArrayList<>();
            for (Animal a : group) {
                if (!a.isBaby()) adults.add(a);
            }
            if (adults.size() > populationCap
                    && (worstAdults == null || adults.size() > worstAdults.size())) {
                worstAdults = adults;
            }
        }
        if (worstAdults == null) return false;

        Animal oldest = null;
        for (Animal a : worstAdults) {
            if (oldest == null || a.tickCount > oldest.tickCount) oldest = a;
        }
        if (oldest == null) return false;

        // Never destroy wool. shearSheep() runs earlier in the same cycle so this is normally already
        // true, but relying on tick order would mean a future reorder silently starts eating wool.
        if (oldest instanceof Sheep sheep && !sheep.isSheared() && !sheep.isBaby()) {
            shearOne(level, sheep);
        }

        oldest.hurt(level.damageSources().generic(), Float.MAX_VALUE);
        return true;
    }

    // ---- Range preview (static footprint guide) -------------------------------------------------
    // Deliberately NOT Mr. Farmer's swept preview: with no wave, a sweep would be communicating an
    // ordering that doesn't exist. This shows the working footprint's PERIMETER all at once, because
    // the question it answers is "where do I build the pen walls?" -- and at 9x9 that's 32 edge cells
    // instead of 81 filled ones, so it reads as a wall line rather than particle soup.

    /** Starts (or restarts) the ~30s footprint guide. Called on GUI close; see MrShepardMenu. */
    public void startRangePreview() {
        previewTicksRemaining = PREVIEW_DURATION_TICKS;
        setChanged();
    }

    private void tickRangePreview(Level level) {
        if (previewTicksRemaining <= 0) return;
        previewTicksRemaining--;
        if (previewTicksRemaining % PREVIEW_EMIT_INTERVAL != 0) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        int count = previewParticleCount();
        if (count <= 0) return;
        for (BlockPos p : footprintPerimeter()) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                    count, 0.25, 0.25, 0.25, 0.0);
        }
    }

    /** Full density until the fade window, then a linear taper so the guide visibly fades out. */
    private int previewParticleCount() {
        if (previewTicksRemaining >= PREVIEW_FADE_TICKS) return PREVIEW_PARTICLES_PER_CELL;
        return Math.round((float) PREVIEW_PARTICLES_PER_CELL * previewTicksRemaining / PREVIEW_FADE_TICKS);
    }

    /** The edge cells of the working footprint -- mirrors {@link #workArea()}'s placement exactly. */
    private List<BlockPos> footprintPerimeter() {
        int range = getRange();
        int half = (range - 1) / 2;
        Direction facing = getFacing();
        List<BlockPos> cells = new ArrayList<>();

        if (facing.getAxis().isVertical()) {
            BlockPos center = worldPosition.relative(facing, half + 1);
            if (range <= 1) { // unfueled -- the machine only reaches its own spot
                cells.add(center);
                return cells;
            }
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != half) continue; // interior, skip
                    cells.add(center.offset(dx, 0, dz));
                }
            }
            return cells;
        }

        // Horizontal: outline the (range+1) deep x range wide field, starting at the machine's own row
        // so the guide shows the pen wall running through the machine rather than in front of it.
        Direction side = facing.getClockWise();
        for (int depth = 0; depth <= range; depth++) {
            for (int lateral = -half; lateral <= half; lateral++) {
                boolean edge = depth == 0 || depth == range || lateral == -half || lateral == half;
                if (!edge) continue;
                BlockPos cell = worldPosition.relative(facing, depth).relative(side, lateral);
                if (cell.equals(worldPosition)) continue; // no particles inside the machine itself
                cells.add(cell);
            }
        }
        return cells;
    }

    private Map<EntityType<?>, List<Animal>> groupBySpecies(ServerLevel level, AABB area) {
        Map<EntityType<?>, List<Animal>> map = new HashMap<>();
        for (Animal a : level.getEntitiesOfClass(Animal.class, area, Animal::isAlive)) {
            map.computeIfAbsent(a.getType(), t -> new ArrayList<>()).add(a);
        }
        return map;
    }

    // ---- Buffer helpers ---------------------------------------------------------------------------

    private ItemStack insertIntoBuffer(ItemStack stack) {
        ItemStack remaining = stack;
        for (int slot = 1 + FOOD_SLOT_COUNT; slot < INVENTORY.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = INVENTORY.insertItem(slot, remaining, false);
        }
        return remaining;
    }

    // ---- Capabilities / GUI -------------------------------------------------------------------

    public IFluidHandler getTank(@Nullable Direction direction) {
        return FUEL_TANK;
    }

    public IItemHandler getItemHandler(@Nullable Direction direction) {
        return INVENTORY;
    }

    /** Automation: buffer slots extract-only, food slots insert-only, fuel slot player-only (mirrors Mr. Farmer). */
    public IItemHandler getAutomationItemHandler(@Nullable Direction direction) {
        return AUTOMATION_HANDLER;
    }

    private final IItemHandler AUTOMATION_HANDLER = new IItemHandler() {
        @Override public int getSlots() { return INVENTORY.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return INVENTORY.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return INVENTORY.getSlotLimit(slot); }

        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot >= 1 && slot <= FOOD_SLOT_COUNT && INVENTORY.isItemValid(slot, stack);
        }

        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot < 1 || slot > FOOD_SLOT_COUNT) return stack; // only food slots accept automated input
            return INVENTORY.insertItem(slot, stack, simulate);
        }

        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot <= FOOD_SLOT_COUNT) return ItemStack.EMPTY; // fuel + food slots not automation-extractable
            return INVENTORY.extractItem(slot, amount, simulate);
        }
    };

    // ---- Gate integration (IHasChannels) --------------------------------------------------------

    /** See {@link IHasChannels#describeFace} -- every face routes identically on this machine. */
    @Override
    public Component describeFace(Direction face) {
        return Component.translatable("tooltip.dermicraft.idep.face.mr_shepard");
    }

    /** See {@link IHasChannels#describeFluidFace} -- getTank ignores the face; it's always the fuel tank. */
    @Override
    public Component describeFluidFace(Direction face) {
        return Component.translatable("tooltip.dermicraft.tank.fuel");
    }

    /**
     * Self-described channels for the Gate. All three are exposed on all six faces, since both
     * capabilities are registered face-agnostically. Order is the Gate's priority order, so the
     * convention is starvation-risk-first: fuel (machine stops dead without it), then feed (breeding
     * stalls without it), then the output buffer (only backs up). The fuel *container* slot is never
     * a channel -- it stays player-only, exactly as the automation handler already enforces.
     */
    @Override
    public List<Channel> getChannels() {
        List<Channel> channels = new ArrayList<>();
        boolean fluidServiced = level != null
                && isFaceServiced(level, worldPosition, Channel.Kind.FLUID, Direction.values());
        boolean itemServiced = level != null
                && isFaceServiced(level, worldPosition, Channel.Kind.ITEM, Direction.values());

        if (!fluidServiced) {
            channels.add(new Channel.FluidChannel("fuel", Component.literal("Fuel"), Channel.IO.IN, FUEL_TANK));
        }
        if (!itemServiced) {
            channels.add(new Channel.ItemChannel("feed", Component.literal("Animal Feed"), Channel.IO.IN, FEED_CHANNEL));
            channels.add(new Channel.ItemChannel("output", Component.literal("Output Buffer"), Channel.IO.OUT, BUFFER_CHANNEL));
        }
        if (!fluidServiced) {
            // Last in priority order -- a passive gather buffer, nothing stalls without it, unlike
            // fuel starving the whole machine.
            channels.add(new Channel.FluidChannel("xp", Component.literal("Knowledge Essence"), Channel.IO.OUT, XP_TANK));
        }
        return channels;
    }

    @Override
    public void drops() {
        super.drops(INVENTORY);
    }

    private FuelTank createFuelTank() {
        return new FuelTank(MachineTier.BASIC.tankCapacity(), 0) {
            @Override
            protected void onContentsChanged() {
                if (level != null && !level.isClientSide) {
                    refreshFuelMemory(); // picks up a fuel swap; empty keeps the last fuel's numbers
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
                        biDirectionalFluidTransfer(FUEL_TANK, FUEL_TANK.SLOT);
                        isTransferringFluids = false;
                    } else if (slot == XP_TANK.SLOT && !isTransferringFluids) {
                        biDirectionalFluidTransfer(XP_TANK, XP_TANK.SLOT);
                        isTransferringFluids = false;
                    }
                    setChanged();
                    updateBlock();
                }
            }

            // Shared by both fuel and XP-tank slots -- same tank-agnostic ModFluidTank API either way.
            private void biDirectionalFluidTransfer(ModFluidTank tank, int slot) {
                if (tank.hasFluidHandlerInSlot(this, slot)) {
                    isTransferringFluids = true;
                    tank.transferFluidToTank(this, slot);
                } else {
                    transferToHandler(tank, slot);
                }
            }

            private void transferToHandler(ModFluidTank tank, int slot) {
                if (tank.hasEmptyFluidHandlerInSlot(this, slot)) {
                    isTransferringFluids = true;
                    ItemStack stack = getStackInSlot(slot);
                    if (stack.isEmpty()) return;
                    setStackInSlot(slot, stack);
                    tank.transferFluidFromTankToHandler(this, slot);
                }
            }

            @Override
            public int getSlotLimit(int slot) {
                return slot == FUEL_TANK.SLOT || slot == XP_TANK.SLOT ? 1 : super.getSlotLimit(slot);
            }

            // The fuel and XP-tank slots only take fluid containers. Without this, quickMoveStack
            // walks the machine's slots in registration order -- fuel first -- so shift-clicking feed
            // from the player's inventory would drop one wheat into the fuel slot before the rest
            // reached the feed row. Slot.mayPlace() routes through here, so this fixes insertion and
            // shift-click.
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot == FUEL_TANK.SLOT || slot == XP_TANK.SLOT) {
                    return stack.getCapability(Capabilities.FluidHandler.ITEM) != null;
                }
                return super.isItemValid(slot, stack);
            }
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", INVENTORY.serializeNBT(registries));
        tag.put("fuel", FUEL_TANK.writeToNBT(registries, new CompoundTag()));
        tag.put("xp_tank", XP_TANK.writeToNBT(registries, new CompoundTag()));
        tag.putInt("populationCap", populationCap);
        // Persisted so an empty tank still remembers its last fuel's cap across a reload.
        tag.putFloat("rememberedHeal", rememberedHeal);
        tag.putString("rememberedFuel", BuiltInRegistries.FLUID.getKey(rememberedFuel).toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // NOT a plain INVENTORY.deserializeNBT -- a Mr. Shepard saved before the XP tank's own slot
        // existed carries Size=14 and would shrink this handler back down, crashing on world load
        // (the Menu adds a slot at index 14 that would no longer exist). Same gotcha documented on
        // SkinTankBlockEntity/DroolingMachineBlockEntity's own loadAdditional.
        if (tag.contains("inventory")) loadItemHandler(INVENTORY, INVENTORY_SIZE, registries, tag.getCompound("inventory"));
        if (tag.contains("fuel")) FUEL_TANK.readFromNBT(registries, tag.getCompound("fuel"));
        if (tag.contains("xp_tank")) XP_TANK.readFromNBT(registries, tag.getCompound("xp_tank"));
        if (tag.contains("populationCap")) populationCap = tag.getInt("populationCap");
        if (tag.contains("rememberedHeal")) rememberedHeal = tag.getFloat("rememberedHeal");
        if (tag.contains("rememberedFuel")) {
            rememberedFuel = BuiltInRegistries.FLUID.get(ResourceLocation.parse(tag.getString("rememberedFuel")));
        }
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.MR_SHEPARD);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MrShepardMenu(containerId, playerInventory, this);
    }
}
