package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scruffy.dermicraft.block.custom.KnowledgeVatBlock;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.interfaces.IPreserveContentsOnPickup;
import net.scruffy.dermicraft.tank.DroolingTank;
import net.scruffy.dermicraft.tank.ModFluidTank;
import net.scruffy.dermicraft.util.ModMath;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Knowledge Vat -- Flesh Lab control block that stores the player's OWN experience as Knowledge
 * Essence fluid, at a fixed 100mB (Knowledge Vat's {@link #MB_PER_LEVEL}) per level. Right-click
 * (empty hand) deposits levels; crouch + right-click withdraws them back -- see
 * {@code KnowledgeVatBlock#useItemOn}. Holding the click ramps the amount per pulse (see
 * {@link #pulseLevels}) rather than always moving a flat one level. A held fluid container instead
 * gets the standard fill/drain interaction, same as every other tank machine.
 * Locked to Knowledge Essence only (via {@link DroolingTank}'s fixed-target-fluid lock, reused here
 * with a constant supplier since this tank's target never changes), 10-bucket capacity (100 levels
 * at 100mB/level), and pushes to a neighbour below same as every other machine's output tank.
 *
 * <p>No item inventory, no Module slot, no GUI -- deliberately the simplest possible machine shape:
 * one locked tank plus two player-facing interactions. There's nothing here for a menu to show.
 */
public class KnowledgeVatBlockEntity extends MachineBaseBlockEntity implements IHasChannels, IPreserveContentsOnPickup {

    public static final int CAPACITY = ModFluidTank.BUCKET_VOLUME * 10;
    public static final int MB_PER_LEVEL = 100;

    /** How many consecutive ticks may pass between two pulses from the same player before this
     * treats it as a fresh press rather than a continued hold -- vanilla's own repeat-interaction
     * interval is 4 ticks, so this leaves a little slack for network jitter. */
    private static final int HOLD_RESET_TICKS = 6;
    /** Ceiling on levels moved per pulse, however long the hold. */
    private static final int RAMP_CAP_LEVELS = 5;
    /** Consecutive pulses needed to climb one more level, so the ramp reaches {@link #RAMP_CAP_LEVELS}
     * after roughly (RAMP_CAP_LEVELS - 1) * this many pulses -- 16 pulses at the ~4-tick vanilla
     * repeat rate is ~3.2 real seconds. */
    private static final int PULSES_PER_RAMP_STEP = 4;

    private final DroolingTank TANK = createDroolingTank(CAPACITY, -1, ModFluids.SOURCE_KNOWLEDGE_ESSENCE::get);

    // Per-player hold tracking -- purely transient interaction state, deliberately not saved/loaded
    // (a held-down click never survives a chunk unload anyway, and it's harmless to reset on reload).
    private final Map<UUID, Long> lastPulseTick = new HashMap<>();
    private final Map<UUID, Integer> pulseStreak = new HashMap<>();

    public KnowledgeVatBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.KNOWLEDGE_VAT_BE.get(), pos, blockState);
    }

    public IFluidHandler getTank(@Nullable Direction face) {
        return TANK;
    }

    public FluidStack getFluid() {
        return TANK.getFluid();
    }

    /** Keeps this block's actual world light emission in sync with the tank's contents -- XP is
     * "special", so a Vat actually holding some should glow, same pattern
     * {@code DroolingMachineBlockEntity}/{@code BeakerBlockEntity} already use for their own
     * fluid-holding blocks. Called automatically whenever {@link #TANK} fills or drains, via
     * {@code MachineBaseBlockEntity#createDroolingTank}'s own onContentsChanged hook. */
    @Override
    protected void onTankContentsChanged() {
        if (level == null) return;

        FluidStack fluid = TANK.getFluid();
        int lightLevel = fluid.isEmpty() ? 0 : fluid.getFluid().getFluidType().getLightLevel(fluid);

        BlockState state = getBlockState();
        if (state.getValue(KnowledgeVatBlock.LIGHT_LEVEL) != lightLevel) {
            level.setBlock(worldPosition, state.setValue(KnowledgeVatBlock.LIGHT_LEVEL, lightLevel), 3);
        }
    }

    @Override
    public Component describeFace(Direction face) {
        return Component.translatable("tooltip.dermicraft.idep.face.knowledge_vat_storage");
    }

    @Override
    public Component describeFluidFace(Direction face) {
        return Component.translatable("tooltip.dermicraft.tank.storage");
    }

    /** Bidirectional storage channel, same shape as Skin Tank's -- see {@link IHasChannels}. */
    @Override
    public List<Channel> getChannels() {
        if (level != null && isFaceServiced(level, worldPosition, Channel.Kind.FLUID, Direction.values())) {
            return List.of();
        }
        return List.of(
                new Channel.FluidChannel("storage", Component.literal("Storage"), Channel.IO.BOTH, TANK)
        );
    }

    /** Right-click, empty hand, not sneaking -- pulls 1-{@link #RAMP_CAP_LEVELS} levels (ramping
     * with how long this player has been holding, see {@link #pulseLevels}) out of the player and
     * into the tank, capped by however many levels they actually have and however much room the tank
     * has left. No-op (returns false) if neither cap leaves anything to move. */
    public boolean depositLevel(ServerPlayer player) {
        if (level == null || player.experienceLevel <= 0) return false;

        int wantLevels = pulseLevels(player);
        int roomLevels = TANK.fill(new FluidStack(ModFluids.SOURCE_KNOWLEDGE_ESSENCE.get(), wantLevels * MB_PER_LEVEL),
                IFluidHandler.FluidAction.SIMULATE) / MB_PER_LEVEL;
        int levels = Math.min(wantLevels, Math.min(player.experienceLevel, roomLevels));
        if (levels <= 0) return false;

        TANK.fill(new FluidStack(ModFluids.SOURCE_KNOWLEDGE_ESSENCE.get(), levels * MB_PER_LEVEL), IFluidHandler.FluidAction.EXECUTE);
        player.giveExperienceLevels(-levels);
        level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0F, 0.7F);
        return true;
    }

    /** Crouch + right-click, empty hand -- the reverse of {@link #depositLevel}, same ramp. No-op if
     * the tank holds less than one level's worth. */
    public boolean withdrawLevel(ServerPlayer player) {
        if (level == null) return false;

        int wantLevels = pulseLevels(player);
        int levels = Math.min(wantLevels, TANK.getFluid().getAmount() / MB_PER_LEVEL);
        if (levels <= 0) return false;

        TANK.drain(levels * MB_PER_LEVEL, IFluidHandler.FluidAction.EXECUTE);
        player.giveExperienceLevels(levels);
        level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0F, 1.3F);
        return true;
    }

    /** How many levels THIS pulse should move, ramping with how many consecutive pulses this same
     * player has landed within {@link #HOLD_RESET_TICKS} of each other -- 1 on a fresh press, up to
     * {@link #RAMP_CAP_LEVELS} after holding for a while, resetting to 1 the instant the gap between
     * pulses exceeds the reset window (release, look away, or just the first click after being idle). */
    private int pulseLevels(ServerPlayer player) {
        if (level == null) return 1;

        UUID id = player.getUUID();
        long now = level.getGameTime();
        Long last = lastPulseTick.get(id);
        int streak = (last != null && now - last <= HOLD_RESET_TICKS) ? pulseStreak.getOrDefault(id, 0) + 1 : 0;

        lastPulseTick.put(id, now);
        pulseStreak.put(id, streak);

        return Math.min(RAMP_CAP_LEVELS, 1 + streak / PULSES_PER_RAMP_STEP);
    }

    /** No item inventory to drop -- {@link IPreserveContentsOnPickup} carries the tank home via
     * {@code saveAdditional}/the vanilla BLOCK_ENTITY_DATA component on a Forceps pickup instead
     * (see {@code SkinTankBlockItem}, reused as-is for this block's item too). A normal break just
     * voids the fluid along with the block, same "destroyed on break" rule every other machine
     * follows. */
    public void drops() {
    }

    public void tick(Level level) {
        if (level.isClientSide) return;
        if (autoDrainEnabled && ModMath.Time.hasSecondsPassed(level, 5)) {
            TANK.pushFluidToBelowNeighbour(level, worldPosition);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag = TANK.writeToNBT(registries, tag);
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        TANK.readFromNBT(registries, tag);
    }
}
