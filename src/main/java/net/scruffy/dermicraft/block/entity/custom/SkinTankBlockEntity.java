package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.fluid.BaseFluidType;
import net.scruffy.dermicraft.hazard.HazardProfile;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.interfaces.IHaveModules;
import net.scruffy.dermicraft.interfaces.IPreserveContentsOnPickup;
import net.scruffy.dermicraft.property.EvolutionModuleProperties;
import net.scruffy.dermicraft.screen.custom.skin_tank.SkinTankMenu;
import net.scruffy.dermicraft.tank.VulnerableTank;
import net.scruffy.dermicraft.util.ModMath;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.List;

/**
 * Pilot for machine Module slots (dermicraft-progression-notes.md, Decision Point #2 -> sequencing
 * step 4) -- chosen as the simplest case: one plain tank, no crafting, nothing else competing for
 * screen space. The Module slot lives as an ordinary slot in the existing INVENTORY handler rather
 * than a separate gadget-style {@code BulkItemData} component (there's no {@link ItemStack} for a
 * machine to carry one on) -- see {@link IHaveModules#installedHazardProfile(HazardProfile, ItemStack)},
 * the single-slot overload added specifically for this case.
 */
public class SkinTankBlockEntity extends MachineBaseBlockEntity
        implements MenuProvider, IHaveInventory, IHasChannels, IPreserveContentsOnPickup {

    public static final int CAPACITY = FluidType.BUCKET_VOLUME * 10;

    public static final int INPUT = 0;
    public static final int OUTPUT = 1;
    public static final int MODULE = 2;

    /** Single source of truth for the handler's slot count -- also re-asserted after
     * {@code deserializeNBT}, see {@link #loadAdditional} for why that's load-bearing. */
    public static final int INVENTORY_SIZE = 3;

    public final ItemStackHandler INVENTORY = createInventory();

    private final VulnerableTank TANK = createTank();

    /** Capability leap hook for a Tier 2 evolution (Charred Tank) to override -- higher capacity,
     * unconditional hazard tolerance -- same "hook override, not a new tier stat" split Masticator/
     * Metastasizer's own {@code createIngredientTank()}/{@code createReagentTank()} use. */
    protected VulnerableTank createTank() {
        return createVulnerableTank(CAPACITY, -1, this::installedHazardProfile);
    }

    /** Tier 1 base, plus whatever the Module slot's currently-installed Safety Module grants, or (if
     * this instance {@link #canEvolve()}) an Evolution Module's own hazards -- same "union, never
     * reset" rule as every gadget's identical method (DrinkerItem, SippingItem), extended with the
     * Evolution Module read Masticator/Metastasizer's own {@code installedHazardProfile} already
     * uses ({@link IHaveModules} only knows about Safety Modules). Public so a future screen/tooltip
     * can read it without duplicating the union logic. */
    public HazardProfile installedHazardProfile() {
        ItemStack module = INVENTORY.getStackInSlot(MODULE);
        HazardProfile profile = IHaveModules.installedHazardProfile(HazardProfile.TIER_1, module);

        if (canEvolve() && !module.isEmpty()) {
            EvolutionModuleProperties evoProps = BuiltInRegistries.ITEM.wrapAsHolder(module.getItem())
                    .getData(ModDataMaps.EVOLUTION_MODULE_PROPERTIES);
            if (evoProps != null) {
                for (var hazard : evoProps.hazards()) {
                    profile = profile.plus(hazard);
                }
            }
        }
        return profile;
    }

    // ---- Evolution (installed Evolution Module -> eventual Charred Tank) ---------------------
    // Simpler than Masticator/Metastasizer/Cauldron's gradual-progress mechanic -- Skin Tank has no
    // crafting cycle and no visible fluid pool geometry that a creeping overlay can sit outside of
    // without either fusing with the block's own translucent surface (tiny margin) or hiding the
    // texture behind an oversized one (large margin). Instead: installing an Evolution Module starts
    // a flat 5-second countdown straight to the flourish, no progress render at all -- just enough
    // time for the player to notice and pull the Module back out if they didn't mean to commit.
    private int flourishTicksRemaining = -1;
    private static final int FLOURISH_DURATION_TICKS = 100; // 5s

    /** Whether this instance can still evolve at all -- true for the base Skin Tank, overridden to
     * false by {@link CharredTankBlockEntity} (already evolved; installing an Evolution Module there
     * does nothing, since its tank is permanently hazard-tolerant regardless of any Module, and
     * there's nothing further for it to transform into). */
    protected boolean canEvolve() {
        return true;
    }

    /** Empty unless the Module slot holds an item with real {@code EvolutionModuleProperties} data
     * AND {@link #canEvolve()} -- a plain {@code MODULES}-tagged item with no such data (or any
     * Module at all once this is already a Charred Tank) is inert here. */
    private Optional<EvolutionModuleProperties> installedEvolutionProperties() {
        if (!canEvolve()) return Optional.empty();
        ItemStack module = INVENTORY.getStackInSlot(MODULE);
        if (module.isEmpty()) return Optional.empty();
        return Optional.ofNullable(
                BuiltInRegistries.ITEM.wrapAsHolder(module.getItem()).getData(ModDataMaps.EVOLUTION_MODULE_PROPERTIES));
    }

    /** Called whenever the Module slot's contents change at all -- cancels any countdown already
     * running (covers the Module being pulled back out mid-countdown), then starts a fresh one if
     * the slot now holds a real Evolution Module. Insert, remove, and swap-for-a-different-item all
     * fall out of this same rule. */
    private void onModuleChanged() {
        flourishTicksRemaining = -1;
        installedEvolutionProperties().ifPresent(props -> startEvolutionFlourish());
    }

    /** Runs every raw tick -- Skin Tank's own {@link #tick} already runs every tick with no coarser
     * cadence to hook into. Returns true once the countdown completes and the block swap has fired,
     * so {@link #tick} can stop touching this now-stale instance. */
    private boolean tickEvolutionFlourish() {
        if (flourishTicksRemaining < 0) return false;

        if (level instanceof ServerLevel serverLevel) {
            spawnFlourishParticles(serverLevel, flourishTicksRemaining);
        }

        flourishTicksRemaining--;
        if (flourishTicksRemaining < 0) {
            completeEvolution(level);
            return true;
        }
        return false;
    }

    private void startEvolutionFlourish() {
        flourishTicksRemaining = FLOURISH_DURATION_TICKS;
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 1.0F, 0.6F);
        }
    }

    private void spawnFlourishParticles(ServerLevel serverLevel, int ticksRemaining) {
        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 0.5;
        double cz = worldPosition.getZ() + 0.5;

        float progress = 1f - (ticksRemaining / (float) FLOURISH_DURATION_TICKS);
        int dustCount = 4 + Math.round(progress * 10);
        DustParticleOptions dust = tintedDust();
        serverLevel.sendParticles(dust, cx, cy, cz, dustCount, 0.3, 0.3, 0.3, 0.03);

        if (ticksRemaining % 4 == 0) {
            int smokeCount = 2 + Math.round(progress * 6);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy, cz, smokeCount, 0.35, 0.35, 0.35, 0.02);
        }

        if (ticksRemaining == 0) {
            serverLevel.sendParticles(dust, cx, cy, cz, 30, 0.5, 0.5, 0.5, 0.06);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy, cz, 16, 0.5, 0.5, 0.5, 0.04);
            serverLevel.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private DustParticleOptions tintedDust() {
        int tint = 0xFFCF4B12; // fallback: lava-orange, matches Thermal's own target fluid
        Optional<net.minecraft.world.level.material.Fluid> targetFluid = installedEvolutionProperties()
                .flatMap(EvolutionModuleProperties::targetFluid);
        if (targetFluid.isPresent() && targetFluid.get().getFluidType() instanceof BaseFluidType baseType) {
            tint = baseType.getTintColor();
        }
        return new DustParticleOptions(new Vector3f(
                ((tint >> 16) & 0xFF) / 255.0F,
                ((tint >> 8) & 0xFF) / 255.0F,
                (tint & 0xFF) / 255.0F), 1.4F);
    }

    /**
     * Transforms this block into a Charred Tank in place, carrying over the input/output items and
     * tank contents -- the Module itself is not carried over, this transform is what consumes it.
     * No FACING (or any other block state) to preserve -- Skin Tank has none, unlike Masticator.
     */
    private void completeEvolution(Level level) {
        ItemStack inputItem = INVENTORY.getStackInSlot(INPUT);
        ItemStack outputItem = INVENTORY.getStackInSlot(OUTPUT);
        FluidStack tankContents = TANK.getFluid().copy();

        // Clear this instance's own contents BEFORE the block swap -- the block's own onRemove drops
        // whatever's still in INVENTORY when the block itself changes, which would otherwise
        // duplicate everything captured above once it's handed to the new instance.
        INVENTORY.setStackInSlot(INPUT, ItemStack.EMPTY);
        INVENTORY.setStackInSlot(OUTPUT, ItemStack.EMPTY);
        INVENTORY.setStackInSlot(MODULE, ItemStack.EMPTY);
        if (!tankContents.isEmpty()) TANK.drain(tankContents.getAmount(), IFluidHandler.FluidAction.EXECUTE);

        level.setBlock(worldPosition, ModBlocks.CHARRED_TANK.get().defaultBlockState(), Block.UPDATE_ALL);

        if (level.getBlockEntity(worldPosition) instanceof SkinTankBlockEntity charred) {
            charred.INVENTORY.setStackInSlot(INPUT, inputItem);
            charred.INVENTORY.setStackInSlot(OUTPUT, outputItem);
            if (!tankContents.isEmpty()) charred.getTank(null).fill(tankContents, IFluidHandler.FluidAction.EXECUTE);
            charred.setChanged();
            charred.updateBlock();
        }
    }

    // Which screen tab was last open -- mirrors WorkbenchBlockEntity#isFabricationPageActive so
    // reopening this Skin Tank's screen returns to the tab last viewed instead of always resetting.
    private boolean moduleTabActive = false;

    public boolean isModuleTabActive() {
        return moduleTabActive;
    }

    public void setModuleTabActive(boolean active) {
        this.moduleTabActive = active;
    }

    public SkinTankBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.SKIN_TANK_BE.get(), pos, blockState);
    }

    // Lets a capability-leap subclass (Charred Tank) register under its own BlockEntityType while
    // reusing everything else this class provides -- see MachineTier's own javadoc on why a genuine
    // capability leap (here: higher capacity + hazard-tolerant tank) is a hook override, not a new
    // MachineTier constant (Skin Tank has no MachineTier concept at all, but the same split applies).
    protected SkinTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    public FluidStack getFluid() {
        return TANK.getFluid();
    }

    /** See {@link IHasChannels#describeFluidFace} -- mirrors {@link #getTank} literally (one tank,
     * every face); describeFace names the fill/drain bucket slots alongside it. */
    @Override
    public Component describeFluidFace(Direction face) {
        return Component.translatable("tooltip.dermicraft.tank.storage");
    }

    public IFluidHandler getTank(@Nullable Direction face) {
        return TANK;
    }

    /** See {@link IHasChannels#describeFace} -- getTank/getItemHandler ignore the face entirely. */
    @Override
    public Component describeFace(Direction face) {
        return Component.translatable("tooltip.dermicraft.idep.face.skin_tank_storage");
    }

    public IFluidHandler getTank() {
        return getTank(null);
    }

    IItemHandler getItemHandler(@Nullable Direction face) {
        return INVENTORY;
    }

    /**
     * Self-described channel list for the Gate multiblock -- see {@link IHasChannels}.
     * Unlike every other machine here, {@code getTank(Direction)} already ignores the face entirely
     * -- Skin Tank is a plain storage tank, not a machine with distinct crafting input/output tanks,
     * so it was never face-locked to begin with. Modelled as {@link Channel.IO#BOTH} rather than
     * IN or OUT, matching that existing bidirectional behaviour. Native faces are all 6; like the
     * Drooling Cauldron's result tank, this is rarely the actual bottleneck given how permissive it
     * already is, but the self-filter still applies correctly to the fully-isolated case.
     */
    @Override
    public List<Channel> getChannels() {
        if (level != null && isFaceServiced(level, worldPosition, Channel.Kind.FLUID, Direction.values())) {
            return List.of();
        }
        return List.of(
                new Channel.FluidChannel("storage", Component.literal("Storage"), Channel.IO.BOTH, TANK)
        );
    }

    public void drops(){
        dropItems(level, INVENTORY, worldPosition);
    }

    public void tick(Level sLevel) {
        if (!sLevel.isClientSide) {
            // Early-return on a completed swap -- same as DroolingMachineBlockEntity#onTickStart --
            // this instance is now stale, its own TANK already drained by completeEvolution.
            if (tickEvolutionFlourish()) return;

            if (autoDrainEnabled && ModMath.Time.hasTicksPassed(sLevel, 20)) {
                TANK.pushFluidToBelowNeighbour(level, worldPosition);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.SKIN_TANK);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SkinTankMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("skin_tank_inv", INVENTORY.serializeNBT(registries));
        tag = TANK.writeToNBT(registries, tag);
        tag.putBoolean("module_tab_active", moduleTabActive);
        tag.putInt("evolution_flourish_ticks", flourishTicksRemaining);
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // NOT a plain INVENTORY.deserializeNBT -- a Skin Tank saved before the Module slot existed
        // carries Size=2 and would shrink this handler back to 2 slots, crashing on world load when
        // the menu adds its Module slot at index 2. See MachineBaseBlockEntity#loadItemHandler.
        loadItemHandler(INVENTORY, INVENTORY_SIZE, registries, tag.getCompound("skin_tank_inv"));
        TANK.readFromNBT(registries, tag);
        moduleTabActive = tag.getBoolean("module_tab_active");
        flourishTicksRemaining = tag.contains("evolution_flourish_ticks")
                ? tag.getInt("evolution_flourish_ticks") : -1;
    }

    private ItemStackHandler createInventory() {
        return new ItemStackHandler(INVENTORY_SIZE) {
           @Override
           protected void onContentsChanged(int slot) {
                if (level != null && !level.isClientSide) {

                    if (slot == MODULE) {
                        onModuleChanged();
                    }

                    if (TANK.hasFluidHandlerInSlot(this, INPUT)) {
                        TANK.transferFluidToTank(this, INPUT);
                    }

                    if (TANK.hasEmptyFluidHandlerInSlot(this, OUTPUT)) {
                        TANK.transferFluidFromTankToHandler(this, OUTPUT);
                    }

                    setChanged();
                    updateBlock();
                }
            }

            @Override
            public int getSlotLimit(int slot) {
                return (slot == OUTPUT || slot == MODULE) ? 1 : super.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                // Same tag every gadget's Module slot filters to (ModTags.Items.MODULES) -- a
                // machine's Module slot is exactly the same "scarce, tag-identified" convention,
                // not a bespoke allowlist. INPUT/OUTPUT stay unrestricted, matching every other
                // machine's own fluid-container-only-in-practice slots.
                return slot != MODULE || stack.is(ModTags.Items.MODULES);
            }
        };
    }

}
