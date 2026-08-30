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

import java.util.List;

/**
 * Knowledge Vat -- Flesh Lab control block that stores the player's OWN experience as Knowledge
 * Essence fluid, at a fixed 100mB (Knowledge Vat's {@link #MB_PER_LEVEL}) per level. Right-click
 * (empty hand) deposits one level; crouch + right-click withdraws one level back -- see
 * {@code KnowledgeVatBlock#useWithoutItem}. A held fluid container instead gets the standard
 * fill/drain interaction ({@code KnowledgeVatBlock#useItemOn}), same as every other tank machine.
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

    private final DroolingTank TANK = createDroolingTank(CAPACITY, -1, ModFluids.SOURCE_KNOWLEDGE_ESSENCE::get);

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

    /** Right-click, empty hand, not sneaking -- pulls one level (100mB) out of the player and into
     * the tank. No-op (returns false) if the player has no level to give, or the tank has no room
     * for another 100mB of Knowledge Essence (full, or -- impossible in practice since nothing else
     * can ever be in this tank -- holding a different fluid). */
    public boolean depositLevel(ServerPlayer player) {
        if (level == null || player.experienceLevel <= 0) return false;

        FluidStack offer = new FluidStack(ModFluids.SOURCE_KNOWLEDGE_ESSENCE.get(), MB_PER_LEVEL);
        if (TANK.fill(offer, IFluidHandler.FluidAction.SIMULATE) < MB_PER_LEVEL) return false;

        TANK.fill(offer, IFluidHandler.FluidAction.EXECUTE);
        player.giveExperienceLevels(-1);
        level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0F, 0.7F);
        return true;
    }

    /** Crouch + right-click, empty hand -- the reverse of {@link #depositLevel}. No-op if the tank
     * holds less than one level's worth. */
    public boolean withdrawLevel(ServerPlayer player) {
        if (level == null || TANK.getFluid().getAmount() < MB_PER_LEVEL) return false;

        TANK.drain(MB_PER_LEVEL, IFluidHandler.FluidAction.EXECUTE);
        player.giveExperienceLevels(1);
        level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0F, 1.3F);
        return true;
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
