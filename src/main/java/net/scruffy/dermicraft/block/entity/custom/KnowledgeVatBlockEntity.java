package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
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
 * Essence fluid, 1mB per real XP point (see {@link #xpCostForLevel} -- the same piecewise formula
 * vanilla uses for {@code Player#getXpNeededForNextLevel}, duplicated here since vanilla only
 * exposes it for the player's own current level, not an arbitrary one). A flat mB-per-LEVEL rate
 * would value a cheap early level and an expensive late level identically, which is the opposite of
 * "vanilla-accurate" -- pricing by real point cost means a level's fluid cost actually scales the
 * way vanilla's own XP curve does. {@link MrShepardBlockEntity}'s own orb-collecting tank fills the
 * same fluid at the same 1mB/point rate -- they must match, or fluid moved between the two would
 * silently inflate or deflate a player's real XP on conversion.
 *
 * <p>Right-click (empty hand) deposits levels; crouch + right-click withdraws them back -- see
 * {@code KnowledgeVatBlock#useItemOn}. A plain press always moves exactly one level, priced at that
 * level's real point cost; holding ramps the number of levels moved per pulse (see
 * {@link #pulseLevels}), each still priced individually and summed -- so a held pulse crossing a
 * cost tier (e.g. level 14 into 15) correctly charges each level's own real cost, not one flat
 * average. A held fluid container instead gets the standard fill/drain interaction, same as every
 * other tank machine.
 * Locked to Knowledge Essence only (via {@link DroolingTank}'s fixed-target-fluid lock, reused here
 * with a constant supplier since this tank's target never changes), 10-bucket capacity -- at 1mB/point
 * that comfortably covers a level-60+ player's entire lifetime XP bank, deliberately left generous
 * (unlike the old flat rate, capacity is no longer the interesting constraint here) so gating the
 * Knowledge Shatter Head/Sunder Chain behind it stays reachable -- and pushes to a neighbour below
 * same as every other machine's output tank.
 *
 * <p>No item inventory, no Module slot, no GUI -- deliberately the simplest possible machine shape:
 * one locked tank plus two player-facing interactions. There's nothing here for a menu to show.
 */
public class KnowledgeVatBlockEntity extends MachineBaseBlockEntity implements IHasChannels, IPreserveContentsOnPickup {

    public static final int CAPACITY = ModFluidTank.BUCKET_VOLUME * 10;

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

    /** Right-click, empty hand, not sneaking -- removes 1-{@link #RAMP_CAP_LEVELS} whole levels
     * (ramping with how long this player has been holding, see {@link #pulseLevels}) off the TOP of
     * the player's current level, priced at those specific levels' real point cost (see
     * {@link #costOfTopLevels}), and deposits that many mB into the tank. Shrinks the level count
     * first against however many levels the player actually has, then against tank room (real costs
     * aren't a flat multiple, so room is checked by re-pricing the smaller count rather than dividing
     * a simulated fill amount). No-op (returns false) if nothing is left to move either way. */
    public boolean depositLevel(ServerPlayer player) {
        if (level == null || player.experienceLevel <= 0) return false;

        int levels = Math.min(pulseLevels(player), player.experienceLevel);
        int cost = costOfTopLevels(player, levels);
        while (levels > 0 && TANK.fill(new FluidStack(ModFluids.SOURCE_KNOWLEDGE_ESSENCE.get(), cost),
                IFluidHandler.FluidAction.SIMULATE) < cost) {
            levels--;
            cost = costOfTopLevels(player, levels);
        }
        if (levels <= 0) return false;

        TANK.fill(new FluidStack(ModFluids.SOURCE_KNOWLEDGE_ESSENCE.get(), cost), IFluidHandler.FluidAction.EXECUTE);
        setTotalXpPoints(player, totalXpPoints(player) - cost);
        level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0F, 0.7F);
        return true;
    }

    /** Crouch + right-click, empty hand -- the reverse of {@link #depositLevel}: adds 1-
     * {@link #RAMP_CAP_LEVELS} levels ABOVE the player's current level, priced at those specific
     * levels' real point cost (see {@link #costOfNextLevels}), draining that many mB from the tank.
     * No-op if the tank doesn't hold even the cheapest single next level's worth. */
    public boolean withdrawLevel(ServerPlayer player) {
        if (level == null) return false;

        int levels = pulseLevels(player);
        int cost = costOfNextLevels(player, levels);
        while (levels > 0 && TANK.getFluid().getAmount() < cost) {
            levels--;
            cost = costOfNextLevels(player, levels);
        }
        if (levels <= 0) return false;

        TANK.drain(cost, IFluidHandler.FluidAction.EXECUTE);
        setTotalXpPoints(player, totalXpPoints(player) + cost);
        level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0F, 1.3F);
        return true;
    }

    /** Real point cost of removing the top {@code count} whole levels off the player's current
     * level -- e.g. count=2 at level 20 sums levels 19 and 18's own costs, NOT 2x a flat average. */
    private int costOfTopLevels(ServerPlayer player, int count) {
        int cost = 0;
        for (int i = 1; i <= count; i++) cost += xpCostForLevel(player.experienceLevel - i);
        return cost;
    }

    /** Real point cost of adding {@code count} whole levels above the player's current level. */
    private int costOfNextLevels(ServerPlayer player, int count) {
        int cost = 0;
        for (int i = 0; i < count; i++) cost += xpCostForLevel(player.experienceLevel + i);
        return cost;
    }

    /** Vanilla's own piecewise cost-to-advance-one-level formula (mirrors the real, decompiled
     * {@code Player#getXpNeededForNextLevel} body exactly), duplicated here because vanilla only
     * exposes it for the player's own CURRENT level -- pricing an arbitrary level (e.g. the one
     * below a player's current level, for withdrawal math) needs the same formula applied to any
     * {@code level} value, not just whatever the player happens to be standing on right now. */
    public static int xpCostForLevel(int level) {
        if (level >= 30) return 112 + (level - 30) * 9;
        if (level >= 15) return 37 + (level - 15) * 5;
        return 7 + level * 2;
    }

    /** How many whole levels {@code points} raw XP is worth, starting from level 0 -- used by
     * {@link net.scruffy.dermicraft.item.custom.KnowledgeVatBlockItem}'s tooltip to show what a
     * banked amount of fluid is actually "worth" in levels, since points no longer convert to
     * levels at one fixed rate. */
    public static int levelForPoints(int points) {
        int lvl = 0;
        int remaining = points;
        while (remaining >= xpCostForLevel(lvl)) {
            remaining -= xpCostForLevel(lvl);
            lvl++;
        }
        return lvl;
    }

    /** Derives the player's true total accumulated XP points from their current level + progress
     * fraction, rather than trusting {@code Player#totalExperience} directly -- that field is never
     * touched by {@code giveExperienceLevels} (what the OLD flat-rate version of this class used),
     * so on a world that's had the old Vat used already, totalExperience may already be stale/out of
     * sync with the player's real level and progress. Deriving from level+progress (the fields that
     * actually govern gameplay -- enchanting cost, the XP bar, etc.) is always correct regardless of
     * that history. */
    private static int totalXpPoints(Player player) {
        int total = 0;
        for (int l = 0; l < player.experienceLevel; l++) total += xpCostForLevel(l);
        total += Math.round(player.experienceProgress * xpCostForLevel(player.experienceLevel));
        return total;
    }

    /** Resets the player to level 0 and re-grants {@code newTotal} points from scratch, so vanilla's
     * own {@code giveExperiencePoints} recomputes a consistent level + progress pair -- the standard
     * trick for setting a player's XP to an arbitrary absolute total (vanilla has no direct "set/
     * remove points" API, only relative add). A negative/zero {@code newTotal} just leaves the
     * player empty. Note: if the player's progress was mid-level at a cost-tier boundary (level 15
     * or 30, where the per-level cost formula changes), the reconstructed progress fraction can read
     * slightly differently than before the transaction -- the POINT total is always exact, only the
     * cosmetic progress-bar percentage can shift, and only right at those two boundaries. */
    private static void setTotalXpPoints(Player player, int newTotal) {
        player.experienceLevel = 0;
        player.totalExperience = 0;
        player.experienceProgress = 0f;
        if (newTotal > 0) player.giveExperiencePoints(newTotal);
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
