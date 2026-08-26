package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.custom.DroolingMachineBlock;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.datagen.datamaps.ModDataMaps;
import net.scruffy.dermicraft.fluid.BaseFluidType;
import net.scruffy.dermicraft.interfaces.IEvolvingMachine;
import net.scruffy.dermicraft.property.EvolutionModuleProperties;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.drooling.VagueDroolingRecipe;
import net.scruffy.dermicraft.screen.custom.drooling_cauldron.DroolingCauldronMenu;
import net.scruffy.dermicraft.tank.ModFluidTank;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Optional;

/**
 * Drooling Cauldron -- water, per {@link #currentTargetFluid} below. Everything else lives on
 * {@link DroolingMachineBlockEntity}, the shared Drooling-family base this and
 * {@link DroolingCrucibleBlockEntity} both extend.
 *
 * <p>Selector + gradual evolution -- see dermicraft-machine-notes.md's "Evolution Module family".
 * An {@link EvolutionModuleProperties} entry in the Module slot both switches {@link #currentTargetFluid}
 * immediately and accumulates {@link #evolutionProgress} toward transforming this block into an
 * actual {@link DroolingCrucibleBlockEntity} in place.
 */
public class DroolingCauldronBlockEntity extends DroolingMachineBlockEntity<VagueDroolingRecipe> implements MenuProvider, IEvolvingMachine {

    /** Same 5 buckets the original hardcoded-water version always had. */
    public static final int CAPACITY = ModFluidTank.BUCKET_VOLUME * 5;
    /** Same 4 mB/s the original hardcoded-water version always had -- also the "fully evolved"
     * rate; see {@link #passiveYieldAmount} for the halved rate while evolving. */
    public static final int PASSIVE_YIELD = 4;

    /** Ticks of genuinely active production accumulated toward evolution -- only advances on a
     * cycle where {@link #onPassiveFillResult} actually added fluid, per direction (not merely
     * because a Module sits in the slot; not during the halt-while-old-fluid-drains period). */
    private int evolutionProgress = 0;

    /** Ticks remaining in the evolution flourish -- a burst of particles/sound that visibly covers
     * the block swap so it doesn't read as an instant flip. -1 means not flourishing. Set when
     * {@link #evolutionProgress} reaches threshold (start of the NEXT tick via {@link #onTickStart},
     * not synchronously from inside {@link #onPassiveFillResult}, since that runs mid-way through
     * this instance's own tick() and swapping the block right then would leave the rest of that
     * tick() call running against a now-stale block entity); counts down once per tick, and the
     * actual block swap ({@link #completeEvolution}) only fires once it reaches 0 -- by then the
     * particle cloud built up over {@link #FLOURISH_DURATION_TICKS} is already covering the block. */
    private int flourishTicksRemaining = -1;

    private static final int FLOURISH_DURATION_TICKS = 40;

    public DroolingCauldronBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DROOLING_CAULDRON_BE.get(), pos, blockState);
    }

    @Override
    protected Fluid currentTargetFluid() {
        return installedEvolutionProperties()
                .flatMap(EvolutionModuleProperties::targetFluid)
                .orElse(Fluids.WATER);
    }

    @Override
    protected int passiveYieldAmount() {
        return installedEvolutionProperties().isPresent() ? Math.round(PASSIVE_YIELD * 0.5f) : PASSIVE_YIELD;
    }

    @Override
    protected int tankCapacity() {
        return CAPACITY;
    }

    @Override
    protected RecipeType<VagueDroolingRecipe> recipeType() {
        return ModRecipes.VAGUE_DROOLING_TYPE.get();
    }

    /** 0 when not evolving at all (no Module, or one with no real Evolution properties); otherwise
     * how far {@link #evolutionProgress} is toward {@code evolutionThreshold}, 0-1. Public purely
     * for {@code EvolutionOverlayBlockEntityRenderer}'s creeping overlay -- no other consumer needs
     * this, evolution completion itself reads the raw fields directly. */
    @Override
    public float getEvolutionProgressFraction() {
        return installedEvolutionProperties()
                .map(props -> Math.min(1f, evolutionProgress / (float) props.evolutionThreshold()))
                .orElse(0f);
    }

    /** Empty unless the Module slot holds an item with real {@code EvolutionModuleProperties} data
     * -- a plain {@code MODULES}-tagged item with no such data (Aggregate Module, Beam Module, etc.
     * left in this slot) is inert here, same as it would be doing nothing useful in any other
     * machine's Module slot it doesn't apply to. */
    private Optional<EvolutionModuleProperties> installedEvolutionProperties() {
        for (int i = 0; i < MODULE_INVENTORY.getSlots(); i++) {
            ItemStack module = MODULE_INVENTORY.getStackInSlot(i);
            if (module.isEmpty()) continue;
            EvolutionModuleProperties props = BuiltInRegistries.ITEM.wrapAsHolder(module.getItem())
                    .getData(ModDataMaps.EVOLUTION_MODULE_PROPERTIES);
            if (props != null) return Optional.of(props);
        }
        return Optional.empty();
    }

    @Override
    protected void onModuleSlotChanged(int slot) {
        // Full reset on ANY change to the slot -- installed, removed, or swapped for a different
        // Module -- matching "pulling the Module wipes all progress" from the design doc, extended
        // to swaps for the same reason (a fresh commitment, not a continuation).
        evolutionProgress = 0;
    }

    @Override
    protected void onPassiveFillResult(int filledAmount) {
        if (filledAmount <= 0) return; // no real production this cycle -- see the doc's own rule

        installedEvolutionProperties().ifPresent(props -> {
            evolutionProgress += ModMath.Time.getSecondsToTicks(1);
            if (evolutionProgress >= props.evolutionThreshold() && flourishTicksRemaining < 0) {
                startEvolutionFlourish();
            }
        });
    }

    @Override
    protected boolean onTickStart(Level level) {
        if (flourishTicksRemaining < 0) return false;

        if (level instanceof ServerLevel serverLevel) {
            spawnFlourishParticles(serverLevel, flourishTicksRemaining);
        }

        flourishTicksRemaining--;
        if (flourishTicksRemaining < 0) {
            completeEvolution(level);
        }
        return true;
    }

    /** Kicks off the flourish: a rising hum immediately, then {@link #FLOURISH_DURATION_TICKS} of
     * particles via {@link #onTickStart} before the block actually swaps. */
    private void startEvolutionFlourish() {
        flourishTicksRemaining = FLOURISH_DURATION_TICKS;
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 1.0F, 0.6F);
        }
    }

    /** Dense, growing burst tinted to the target fluid's own colour, so it reads as "this fluid is
     * consuming the machine" rather than a generic effect -- plus smoke for raw visual bulk, since
     * tinted dust alone is too sparse to actually hide the model underneath. Density ramps up as
     * {@code ticksRemaining} counts down toward 0, so the block is fully obscured right as the swap
     * happens instead of the cloud being front-loaded and thinning out by the time it matters. */
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
            // Completion burst, right as the block swap fires -- masks the actual moment of change.
            serverLevel.sendParticles(dust, cx, cy, cz, 30, 0.5, 0.5, 0.5, 0.06);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy, cz, 16, 0.5, 0.5, 0.5, 0.04);
            serverLevel.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private DustParticleOptions tintedDust() {
        int tint = 0xFFCF4B12; // fallback: lava-orange, matches Heat's own target fluid
        if (currentTargetFluid().getFluidType() instanceof BaseFluidType baseType) {
            tint = baseType.getTintColor();
        }
        return new DustParticleOptions(new Vector3f(
                ((tint >> 16) & 0xFF) / 255.0F,
                ((tint >> 8) & 0xFF) / 255.0F,
                (tint & 0xFF) / 255.0F), 1.4F);
    }

    /**
     * Transforms this block into a Drooling Crucible in place, carrying over the ingredient/output
     * items and whatever's in the tank (by construction, always the Module's target fluid by the
     * time evolution completes) -- but deliberately NOT the recipe-in-progress bookkeeping
     * (active recipe/item, craft progress). That state is keyed to THIS class's own
     * {@code VAGUE_DROOLING_TYPE}; blindly copying it into a Crucible whose recipe type is
     * different could leave stale, type-mismatched data sitting in the new instance for no benefit
     * -- a half-finished food-boost cycle just restarts cleanly on the new block instead. The
     * Module itself is not carried over either: this transform is what consumes it.
     */
    private void completeEvolution(Level level) {
        ItemStack inputItem = INVENTORY.getStackInSlot(INPUT);
        ItemStack outputItem = INVENTORY.getStackInSlot(OUTPUT);
        FluidStack tankContents = TANK.getFluid().copy();
        BlockState oldState = getBlockState();

        // Clear this instance's own contents BEFORE the block swap -- DroolingMachineBlock#onRemove
        // drops whatever's still in INVENTORY when the block itself changes, which would otherwise
        // duplicate everything captured above once it's handed to the new Crucible instance.
        INVENTORY.setStackInSlot(INPUT, ItemStack.EMPTY);
        INVENTORY.setStackInSlot(OUTPUT, ItemStack.EMPTY);
        for (int i = 0; i < MODULE_INVENTORY.getSlots(); i++) {
            MODULE_INVENTORY.setStackInSlot(i, ItemStack.EMPTY);
        }
        if (!tankContents.isEmpty()) {
            TANK.drain(tankContents.getAmount(), IFluidHandler.FluidAction.EXECUTE);
        }

        BlockState newState = ModBlocks.DROOLING_CRUCIBLE.get().defaultBlockState()
                .setValue(DroolingMachineBlock.FACING, oldState.getValue(DroolingMachineBlock.FACING));
        level.setBlock(worldPosition, newState, Block.UPDATE_ALL);

        if (level.getBlockEntity(worldPosition) instanceof DroolingCrucibleBlockEntity crucible) {
            crucible.INVENTORY.setStackInSlot(DroolingCrucibleBlockEntity.INPUT, inputItem);
            crucible.INVENTORY.setStackInSlot(DroolingCrucibleBlockEntity.OUTPUT, outputItem);
            if (!tankContents.isEmpty()) {
                crucible.TANK.fill(tankContents, IFluidHandler.FluidAction.EXECUTE);
            }
            crucible.setChanged();
            crucible.updateBlock();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("cauldron_evolution_progress", evolutionProgress);
        tag.putInt("cauldron_evolution_flourish_ticks", flourishTicksRemaining);
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        evolutionProgress = tag.getInt("cauldron_evolution_progress");
        flourishTicksRemaining = tag.contains("cauldron_evolution_flourish_ticks")
                ? tag.getInt("cauldron_evolution_flourish_ticks") : -1;
    }

    @Override
    @NotNull
    public Component getDisplayName() {
        return super.getDisplayName(ModBlocks.DROOLING_CAULDRON);
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DroolingCauldronMenu(containerId, inventory, this);
    }
}
