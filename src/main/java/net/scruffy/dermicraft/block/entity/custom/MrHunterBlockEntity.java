package net.scruffy.dermicraft.block.entity.custom;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.block.entity.custom.base.AbstractFarmBlockEntity;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.event.HunterKillEvents;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.machine.MachineStatKey;
import net.scruffy.dermicraft.machine.SimpleMachineStats;
import net.scruffy.dermicraft.screen.custom.mr_hunter.MrHunterMenu;
import net.scruffy.dermicraft.tank.DroolingTank;
import net.scruffy.dermicraft.tank.ModFluidTank;
import net.scruffy.dermicraft.util.DrainOnlyFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Mr. Hunter: strikes the hostile mobs in its field. Each work cycle it picks the nearest hostile mobs in its box (as many as the
 * tier allows) and hurts each once, a hit that just happens (no swing, no projectile) and costs fuel. The hit is a player's
 * attack, made by a fake player, so a mob that dies of it dies the ordinary way: its own loot table, the player-only drops and
 * its XP. {@link HunterKillEvents} hands that loot to the output buffer and the XP to the XP tank (Knowledge Colloid, as Mr.
 * Shepherd's), so nothing lands on the ground unless the buffer is full.
 *
 * <p>It works a box, like Mr. Shepherd's pen (width, length, height; the box starts at the machine's own layer). It has no
 * input row yet and produces no light. It stops when its fuel runs out. It never hits a player, a boss or anything in the
 * {@link ModTags.EntityTypes#HUNTER_IGNORES} tag, and "hostile" means the game's own {@link Enemy} (monsters, slimes, ghasts,
 * phantoms, hoglins and the like), so animals, villagers and golems are safe.
 */
public class MrHunterBlockEntity extends AbstractFarmBlockEntity {

    private static final int BUFFER_SLOT_COUNT = 9;

    // The XP tank is Mr. Shepherd's, the same lock to Knowledge Colloid and the same one bucket: a catch-tray, not a store.
    private static final int XP_TANK_CAPACITY = ModFluidTank.BUCKET_VOLUME;
    private static final int XP_SLOT_INDEX = 1 + BUFFER_SLOT_COUNT;
    private static final int INVENTORY_SIZE = XP_SLOT_INDEX + 1;
    // 1:1 with the Knowledge Vat and Mr. Shepherd, so XP is worth the same fluid wherever it is gathered.
    private static final int MB_PER_XP_POINT = 1;

    /** The fake player whose attack every Hunter hit is, so a kill counts as a player kill. */
    private static final GameProfile HUNTER_PROFILE = new GameProfile(UUID.fromString("5d3c1f0e-7a41-4b8e-9c1b-6f0d2a8e4b17"), "[Mr. Hunter]");

    private final DroolingTank XP_TANK = createDroolingTank(XP_TANK_CAPACITY, XP_SLOT_INDEX, ModFluids.SOURCE_KNOWLEDGE_COLLOID::get);
    // The bottom face is an output: the XP tank as a drain-only view (see getTank).
    private final IFluidHandler XP_DRAIN = new DrainOnlyFluidHandler(XP_TANK);

    private int waveTimer = 0;

    public MrHunterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MR_HUNTER_BE.get(), pos, blockState, INVENTORY_SIZE, 1, BUFFER_SLOT_COUNT);
    }

    @Override
    public DeferredBlock<Block> getFarmBlock() {
        return ModBlocks.MR_HUNTER;
    }

    @Override
    protected String getMachineName() {
        return "mr_hunter";
    }

    @Override
    protected MachineStatKey getStatKey() {
        return MachineStatKey.MR_HUNTER;
    }

    /** A mob stands on the layer and is up to three blocks tall (an enderman), so the box starts that tall. */
    @Override
    protected int getDefaultFieldHeight() {
        return 3;
    }

    public DroolingTank getXpTank() {
        return XP_TANK;
    }

    // ---- No input row (yet) ---------------------------------------------------------------------------

    @Override
    protected boolean isInputItemValid(ItemStack stack) {
        return false;
    }

    // ---- Work -------------------------------------------------------------------------------------------

    @Override
    protected void tickFarmWork(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (waveTimer > 0) {
            waveTimer--;
            return;
        }
        waveTimer = currentWaveStepTicks();

        // Loot that spilled because the buffer was full is swept up again once there is room (it costs nothing).
        collectItems(serverLevel, getVolumeBox().inflate(1, 0, 1));

        List<LivingEntity> targets = findTargets(serverLevel);
        int hits = Math.min(targets.size(), SimpleMachineStats.getHunterHitsPerCycle(getEvolutionState()));
        float damage = SimpleMachineStats.getHunterDamage(getEvolutionState());
        float fuelPerHit = SimpleMachineStats.getHunterFuelStepsPerHit();

        int struck = 0;
        for (int i = 0; i < hits; i++) {
            if (!FUEL_TANK.hasEnoughFuel(fuelPerStep())) break; // out of fuel part way through a cycle: stop there
            strike(serverLevel, targets.get(i), damage);
            burnFuel(fuelPerHit);
            struck++;
        }

        if (struck > 0) {
            tickGradualProgress(currentWaveStepTicks()); // a cycle that struck is real production toward an evolution
            setChanged();
        }
    }

    /** The hostile mobs in the box, nearest to the machine first. */
    private List<LivingEntity> findTargets(ServerLevel level) {
        Vec3 from = Vec3.atCenterOf(worldPosition);
        List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, getVolumeBox(), MrHunterBlockEntity::isTarget);
        found.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(from)));
        return found;
    }

    /** Hostile (the game's {@link Enemy}), alive and hittable, not a player, not a boss, not in the ignore tag. */
    private static boolean isTarget(LivingEntity mob) {
        return mob instanceof Enemy
                && !(mob instanceof Player)
                && mob.isAlive()
                && !mob.isInvulnerable()
                && !mob.getType().is(ModTags.EntityTypes.HUNTER_IGNORES);
    }

    /**
     * One hit: the damage of a player's attack, so loot and XP are a player kill's. The fake player is moved onto the machine
     * first (a hit knocks the mob away from its attacker), and the mob's invulnerability from an earlier hit is cleared, so a
     * hit is never lost to the half-second grace a mob gets after being hurt.
     */
    private void strike(ServerLevel level, LivingEntity target, float damage) {
        FakePlayer hunter = FakePlayerFactory.get(level, HUNTER_PROFILE);
        hunter.setPos(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5);

        HunterKillEvents.begin(this, target);
        try {
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(hunter), damage);
        } finally {
            HunterKillEvents.end();
        }
    }

    // ---- What a kill hands the machine (called from HunterKillEvents) -------------------------------------

    /** The kill's drops go to the output buffer, and spill where the mob died if it is full. */
    public void acceptKillLoot(ServerLevel level, BlockPos at, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            overflowToBufferOrDrop(level, at, stack);
        }
        setChanged();
    }

    /** The kill's XP goes into the tank as Knowledge Colloid; what does not fit becomes orbs where the mob died. */
    public void acceptKillXp(ServerLevel level, Vec3 at, int xp) {
        if (xp <= 0) return;
        int taken = XP_TANK.fill(new FluidStack(ModFluids.SOURCE_KNOWLEDGE_COLLOID.get(), xp * MB_PER_XP_POINT),
                IFluidHandler.FluidAction.EXECUTE) / MB_PER_XP_POINT;
        if (taken < xp) ExperienceOrb.award(level, at, xp - taken);
        setChanged();
    }

    // ---- Range preview -----------------------------------------------------------------------------
    // The particle box is the shared one (AbstractFarmBlockEntity#tickPreview); only the volume is the Hunter's: its kill zone.

    @Override
    protected AABB getFieldBox(Level level) {
        return getVolumeBox();
    }

    // ---- Slots, automation and Gate integration -----------------------------------------------------

    // The XP tank's own container slot is a second fluid-container slot beside the fuel one.
    @Nullable
    @Override
    protected ModFluidTank getTankForSlot(int slot) {
        return slot == XP_TANK.SLOT ? XP_TANK : super.getTankForSlot(slot);
    }

    // The bottom face is an output only: Knowledge Colloid out of the XP tank and the output buffer out, nothing in.
    @Override
    public IFluidHandler getTank(@Nullable Direction direction) {
        return direction == Direction.DOWN ? XP_DRAIN : super.getTank(direction);
    }

    @Override
    public IItemHandler getAutomationItemHandler(@Nullable Direction direction) {
        return direction == Direction.DOWN ? BUFFER_CHANNEL : super.getAutomationItemHandler(direction);
    }

    // The Gate describes each face from these, so they mirror the capabilities above.
    @Override
    public Component describeFace(Direction face) {
        return face == Direction.DOWN
                ? Component.translatable("tooltip.dermicraft.idep.face.mr_hunter.bottom")
                : super.describeFace(face);
    }

    @Override
    public Component describeFluidFace(Direction face) {
        return face == Direction.DOWN
                ? Component.translatable("tooltip.dermicraft.gauge.xp")
                : super.describeFluidFace(face);
    }

    /** The output buffer as usual, plus the Knowledge Colloid into the block below. */
    @Override
    protected void drainOutputs(Level level) {
        super.drainOutputs(level);
        if (!XP_TANK.isEmpty()) XP_TANK.pushFluidToBelowNeighbour(level, worldPosition);
    }

    /** Fuel first (the machine stops dead without it), then the output buffer, then the XP (a passive catch-tray). */
    @Override
    protected void addChannels(List<Channel> channels, boolean exposeFluid, boolean exposeItem) {
        if (exposeFluid) {
            channels.add(fuelChannel());
        }
        if (exposeItem) {
            channels.add(new Channel.ItemChannel("output", Component.literal("Output Buffer"), Channel.IO.OUT, BUFFER_CHANNEL));
        }
        if (exposeFluid) {
            channels.add(new Channel.FluidChannel("xp", Component.literal("Knowledge Colloid"), Channel.IO.OUT, XP_TANK));
        }
    }

    // ---- Saving ---------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("xp_tank", XP_TANK.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("xp_tank")) XP_TANK.readFromNBT(registries, tag.getCompound("xp_tank"));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MrHunterMenu(containerId, playerInventory, this);
    }
}
