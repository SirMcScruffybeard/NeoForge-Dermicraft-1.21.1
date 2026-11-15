package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.util.SlotRangeItemHandler;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.screen.custom.mr_shepherd.MrShepherdMenu;
import net.scruffy.dermicraft.tank.DroolingTank;
import net.scruffy.dermicraft.tank.FuelTank;
import net.scruffy.dermicraft.tank.ModFluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.scruffy.dermicraft.block.entity.custom.base.AbstractFarmBlockEntity;

/**
 * Livestock counterpart to Mr. Farmer -- see dermicraft-machine-notes.md -> Mr. Shepherd.
 * Unlike Farmer's block-position wave (crops don't move), Shepherd scans a live AABB each work
 * cycle since animals move freely -- no wave-stepping needed for a first pass. The shared farm
 * machinery (fuel, field size, buffer, preview timer, automation, channels, saving) is in
 * {@link AbstractFarmBlockEntity}; an unfueled Shepherd does nothing.
 */
public class MrShepherdBlockEntity extends AbstractFarmBlockEntity {


    private static final int VERTICAL_REACH = 4; // AABB extends this far above/below the machine's plane
    private static final int PICKUP_MARGIN = 1;  // pickup-only overreach on each horizontal side

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
    private static final int PREVIEW_EMIT_INTERVAL = 10;   // re-emit twice a second
    private static final int PREVIEW_PARTICLES_PER_CELL = 2;

    private static final int FOOD_SLOT_COUNT = 4;
    private static final int BUFFER_SLOT_COUNT = 9;

    // XP-gathering tank -- locked to Knowledge Colloid, same fixed-target-fluid lock the Knowledge
    // Vat uses (DroolingTank with a constant supplier, since this tank's target never changes).
    // 1000mB (10 levels at the Vat's own 100mB/level rate) -- a compact catch-tray, not a Vat-sized
    // reservoir; this machine is meant to gather in passing, not act as long-term storage.
    private static final int XP_TANK_CAPACITY = ModFluidTank.BUCKET_VOLUME;
    private static final int XP_SLOT_INDEX = 1 + FOOD_SLOT_COUNT + BUFFER_SLOT_COUNT;
    private static final int INVENTORY_SIZE = XP_SLOT_INDEX + 1;

    private final DroolingTank XP_TANK = createDroolingTank(XP_TANK_CAPACITY, XP_SLOT_INDEX, ModFluids.SOURCE_KNOWLEDGE_COLLOID::get);
    // slot 0 = fuel container, 1..FOOD_SLOT_COUNT = breeding food, next BUFFER_SLOT_COUNT =
    // pickup/wool buffer, last slot = XP tank's own fill/drain container.

    // Per-channel views for the Gate (see getChannels). Feed only accepts pushes, the buffer is
    // drain-only -- the fuel container slot is deliberately in neither, staying player-only.
    private final IItemHandler FEED_CHANNEL =
            new SlotRangeItemHandler(INVENTORY, 1, FOOD_SLOT_COUNT, true, false);

    private int waveTimer = 0;
    private int populationCap = DEFAULT_POPULATION_CAP;

    // Last real fuel's heal value, and which fluid it came from. FuelTank.getHeal() returns 0.0 on an
    // EMPTY tank, which would collapse the max cap to the minimum and destructively clamp the player's
    // selection every single time the tank ran dry. So an empty tank deliberately keeps the last fuel's
    // numbers; the value only changes when a genuinely DIFFERENT fluid is loaded. (FuelTank rejects a
    // non-matching biofuel while non-empty, so a fuel change can only ever happen from empty.)
    private float rememberedHeal = 1.0f;
    private Fluid rememberedFuel = Fluids.EMPTY;

    public MrShepherdBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MR_SHEPHERD_BE.get(), pos, blockState,
                INVENTORY_SIZE, 1 + FOOD_SLOT_COUNT, BUFFER_SLOT_COUNT);
    }

    @Override
    public DeferredBlock<Block> getFarmBlock() {
        return ModBlocks.MR_SHEPHERD;
    }

    @Override
    protected String getMachineName() {
        return "mr_shepherd";
    }

    /** Picks up a fuel swap; an empty tank keeps the last fuel's numbers. */
    @Override
    protected void onFuelContentsChanged() {
        refreshFuelMemory();
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

    // ---- Work ---------------------------------------------------------------------------------

    @Override
    protected void tickFarmWork(Level level) {
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
     * The working volume, the player's field. Horizontal mounts cover the machine's OWN row through {@code length}
     * blocks ahead and {@code width} blocks across, centered on the machine; vertical mounts still use the older
     * offset-centre box pending their rework (width along X, length along Z).
     */
    private AABB workArea() {
        int halfWidth = getHalfWidth();
        int halfLength = getHalfLength();
        Direction facing = getFacing();

        if (facing.getAxis().isVertical()) {
            // Unchanged pending the vertical-facing rework -- this box bleeds past the machine's own
            // plane (see the divergence note in dermicraft-machine-notes.md).
            BlockPos center = worldPosition.relative(facing, Math.max(halfWidth, halfLength) + 1);
            return new AABB(
                    center.getX() - halfWidth, center.getY() - VERTICAL_REACH, center.getZ() - halfLength,
                    center.getX() + halfWidth + 1, center.getY() + VERTICAL_REACH + 1, center.getZ() + halfLength + 1);
        }

        // Depth starts at 0 -- the machine's own row -- not 1 block ahead like Mr. Farmer's. A Shepherd
        // set into a fence or wall line is flush with the pen, so items dropped directly beside it sit
        // in the machine's own plane; starting a block ahead would leave them permanently uncollected.
        Direction side = facing.getClockWise();
        AABB box = new AABB(worldPosition.relative(side, -halfWidth))
                .minmax(new AABB(worldPosition.relative(facing, getFieldLength()).relative(side, halfWidth)));
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
            // drops -- so this is what actually stops Shepherd hoovering up your inventory when you die
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

    // 1:1 with XP_TANK's own real point-cost convention (KnowledgeVatBlockEntity) -- must match
    // exactly, or fluid collected here would be worth a different amount of real XP than the same
    // fluid deposited by hand through the Vat, letting orb collection silently inflate or deflate a
    // player's XP the moment it's converted back.
    private static final int MB_PER_XP_POINT = 1;

    private boolean collectXp(ServerLevel level, AABB area) {
        boolean any = false;
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, area, ExperienceOrb::isAlive)) {
            int value = orb.getValue();
            if (value <= 0) continue;

            // All-or-nothing per orb, same as a real player pickup -- a nearly-full tank shouldn't
            // split one orb's worth across two work cycles, so this simulates first and skips the
            // orb entirely (leaving it for a later cycle, or a player, to take) if it doesn't fully fit.
            FluidStack offer = new FluidStack(ModFluids.SOURCE_KNOWLEDGE_COLLOID.get(), value * MB_PER_XP_POINT);
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

    @Override
    protected void tickPreview(Level level) {
        if (getPreviewTicksRemaining() % PREVIEW_EMIT_INTERVAL != 0) return;
        emitPreviewParticles(level, footprintPerimeter(), previewParticleCount(PREVIEW_PARTICLES_PER_CELL));
    }

    /** The edge cells of the working footprint -- mirrors {@link #workArea()}'s placement exactly. */
    private List<BlockPos> footprintPerimeter() {
        int halfWidth = getHalfWidth();
        int halfLength = getHalfLength();
        Direction facing = getFacing();
        List<BlockPos> cells = new ArrayList<>();

        if (facing.getAxis().isVertical()) {
            BlockPos center = worldPosition.relative(facing, Math.max(halfWidth, halfLength) + 1);
            if (halfWidth == 0 && halfLength == 0) { // a 1 x 1 field -- the machine only reaches its own spot
                cells.add(center);
                return cells;
            }
            for (int dx = -halfWidth; dx <= halfWidth; dx++) {
                for (int dz = -halfLength; dz <= halfLength; dz++) {
                    if (Math.abs(dx) != halfWidth && Math.abs(dz) != halfLength) continue; // interior, skip
                    cells.add(center.offset(dx, 0, dz));
                }
            }
            return cells;
        }

        // Horizontal: outline the (length+1) deep x width wide field, starting at the machine's own row
        // so the guide shows the pen wall running through the machine rather than in front of it.
        Direction side = facing.getClockWise();
        int length = getFieldLength();
        for (int depth = 0; depth <= length; depth++) {
            for (int lateral = -halfWidth; lateral <= halfWidth; lateral++) {
                boolean edge = depth == 0 || depth == length || lateral == -halfWidth || lateral == halfWidth;
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

    // ---- Slots, automation and Gate integration -----------------------------------------------

    // The XP tank's own container slot is a second fluid-container slot beside the fuel one.
    @Nullable
    @Override
    protected ModFluidTank getTankForSlot(int slot) {
        return slot == XP_TANK.SLOT ? XP_TANK : super.getTankForSlot(slot);
    }

    /** Automation: only the food slots take input (the feed channel); the buffer is extract-only, and the fuel and XP
     * container slots are player-only. */
    @Override
    protected boolean canAutomationInsert(int slot, ItemStack stack) {
        return slot >= 1 && slot <= FOOD_SLOT_COUNT;
    }

    /**
     * Self-described channels for the Gate. All are exposed on all six faces, since both capabilities are registered
     * face-agnostically. Order is the Gate's priority order, so the convention is starvation-risk-first: fuel (machine
     * stops dead without it), then feed (breeding stalls without it), then the output buffer (only backs up). The fuel
     * *container* slot is never a channel -- it stays player-only, exactly as the automation handler already enforces.
     */
    @Override
    protected void addChannels(List<Channel> channels, boolean exposeFluid, boolean exposeItem) {
        if (exposeFluid) {
            channels.add(fuelChannel());
        }
        if (exposeItem) {
            channels.add(new Channel.ItemChannel("feed", Component.literal("Animal Feed"), Channel.IO.IN, FEED_CHANNEL));
            channels.add(new Channel.ItemChannel("output", Component.literal("Output Buffer"), Channel.IO.OUT, BUFFER_CHANNEL));
        }
        if (exposeFluid) {
            // Last in priority order -- a passive gather buffer, nothing stalls without it, unlike
            // fuel starving the whole machine.
            channels.add(new Channel.FluidChannel("xp", Component.literal("Knowledge Colloid"), Channel.IO.OUT, XP_TANK));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("xp_tank", XP_TANK.writeToNBT(registries, new CompoundTag()));
        tag.putInt("populationCap", populationCap);
        // Persisted so an empty tank still remembers its last fuel's cap across a reload.
        tag.putFloat("rememberedHeal", rememberedHeal);
        tag.putString("rememberedFuel", BuiltInRegistries.FLUID.getKey(rememberedFuel).toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("xp_tank")) XP_TANK.readFromNBT(registries, tag.getCompound("xp_tank"));
        if (tag.contains("populationCap")) populationCap = tag.getInt("populationCap");
        if (tag.contains("rememberedHeal")) rememberedHeal = tag.getFloat("rememberedHeal");
        if (tag.contains("rememberedFuel")) {
            rememberedFuel = BuiltInRegistries.FLUID.get(ResourceLocation.parse(tag.getString("rememberedFuel")));
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MrShepherdMenu(containerId, playerInventory, this);
    }
}
