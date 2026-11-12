package net.scruffy.dermicraft.block.entity.custom.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.hazard.HazardFamily;
import net.scruffy.dermicraft.item.custom.module.EvolutionModuleItem;
import net.scruffy.dermicraft.machine.EvolutionEffects;
import net.scruffy.dermicraft.machine.EvolutionResult;
import net.scruffy.dermicraft.machine.MachineStatKey;
import net.scruffy.dermicraft.machine.SimpleMachineStats;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * The shared groundwork for every machine that evolves in place through an Evolution Module in its dedicated
 * Evolution slot: the fuel-powered crafting machines ({@code AbstractFueledMachineBlockEntity}) and the Drooling
 * family. It owns the Evolution slot, the gradual progress toward the pending upgrade, the end-of-evolution
 * flourish and the saving of all three, plus the Module slot count read from the stat table.
 *
 * <p>Evolution is gradual: ticks of real production accrue toward the module's upgrade time (from
 * {@code EvolutionRules}), and the machine decides what counts as production and when to add it with
 * {@link #tickGradualProgress(int)}. Progress resets only when the module comes out or the machine is picked up.
 * When the time is served a short flourish plays (one step per call of {@link #tickGradualFlourish()}), then the new
 * evolution state is applied in place and the module is consumed. A machine opts in with
 * {@link #usesEvolutionState()}.
 */
public abstract class EvolvingMachineBlockEntity extends MachineBaseBlockEntity {

    private static final int GRADUAL_FLOURISH_CYCLES = 4; // steps of the flourish: 4 cycles (about 2s) for a processing machine

    /** The dedicated Evolution Module slot: one slot, Evolution Modules only. Unused (and unexposed) on a machine
     * that has not opted in with {@link #usesEvolutionState()}. */
    public final ItemStackHandler EVOLUTION_INVENTORY = createEvolutionInventory();

    /** Ticks of real production accrued toward the pending upgrade. */
    private int gradualProgress = 0;
    /** The pending upgrade's time when progress last advanced, saved so a client (which has no server config) can
     * draw the progress fraction. */
    private int gradualTarget = 1;
    /** Steps left in the end-of-evolution flourish; negative when it is not running. */
    private int gradualFlourishCycles = -1;

    protected EvolvingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /** Whether this machine's stats and evolution come from its evolution state and Evolution slot, not from its
     * block's tier. False until a machine is migrated. */
    protected boolean usesEvolutionState() {
        return false;
    }

    /** The machine's key in the stat config (its own section of overrides), or null for the shared values. A machine
     * that uses the evolution state returns its {@link MachineStatKey}. */
    @Nullable
    protected MachineStatKey getStatKey() {
        return null;
    }

    /** General Module slots (the dedicated Evolution slot is separate): from the stat table once the machine uses the
     * evolution state, otherwise the base default. */
    @Override
    public int getModuleSlotCount() {
        return usesEvolutionState() ? SimpleMachineStats.getModuleSlots(getStatKey(), getEvolutionState()) : super.getModuleSlotCount();
    }

    /** The worst intensity of {@code family} among the fluids this machine is processing, for the screen's
     * missing-tolerance warning. 0 by default: a machine that processes no hazardous fluid never warns. */
    public int getHazardIntensity(HazardFamily family) {
        return 0;
    }

    /** Whether a stack is an Evolution Module (the frame or any loaded module). */
    public static boolean isEvolutionModule(ItemStack stack) {
        return stack.getItem() instanceof EvolutionModuleItem;
    }

    private ItemStackHandler createEvolutionInventory() {
        return new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return usesEvolutionState() && isEvolutionModule(stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                if (level != null && !level.isClientSide) {
                    onEvolutionSlotChanged();
                    setChanged();
                    updateBlock();
                }
            }
        };
    }

    /** Whatever was accrued belongs to the old module, so insert, removal and swap all start over. A machine with its
     * own evolution timing (the Skin Tank's countdown) overrides this, calling super first. */
    protected void onEvolutionSlotChanged() {
        gradualProgress = 0;
        gradualFlourishCycles = -1;
        gradualTarget = getPendingUpgrade().map(EvolutionResult.Upgrade::durationTicks).orElse(1);
    }

    /** What the Evolution slot's module would do on this machine right now: empty for an empty slot, the bare frame
     * or a machine that does not use the evolution state; otherwise the {@code EvolutionRules} answer. */
    public Optional<EvolutionResult> getEvolutionStatus() {
        if (!usesEvolutionState()) return Optional.empty();
        ItemStack module = EVOLUTION_INVENTORY.getStackInSlot(0);
        if (!(module.getItem() instanceof EvolutionModuleItem evolutionModule)) return Optional.empty();
        return evolutionModule.evaluate(getStatKey(), getEvolutionState(), module);
    }

    /** The upgrade the Evolution slot's module would apply, or empty if it is blocked, inert or absent. */
    protected Optional<EvolutionResult.Upgrade> getPendingUpgrade() {
        return getEvolutionStatus()
                .filter(EvolutionResult.Upgrade.class::isInstance)
                .map(EvolutionResult.Upgrade.class::cast);
    }

    /** Production is frozen while the flourish plays, so the swap never lands mid-craft. */
    protected boolean isEvolutionFlourishing() {
        return gradualFlourishCycles >= 0;
    }

    /** How far the pending evolution is, 0 to 1 (0 with no live module). Read by the evolution overlay renderer and
     * the Evolution slot's fill. */
    public float getGradualEvolutionFraction() {
        if (!usesEvolutionState() || getPendingUpgrade().isEmpty()) return 0f;
        return Math.clamp(gradualProgress / (float) Math.max(1, gradualTarget), 0f, 1f);
    }

    /** Adds {@code ticks} of real production to the pending evolution; called when the machine actually worked. */
    protected void tickGradualProgress(int ticks) {
        if (!usesEvolutionState() || isEvolutionFlourishing()) return;

        Optional<EvolutionResult.Upgrade> pending = getPendingUpgrade();
        if (pending.isEmpty()) {
            gradualProgress = 0;
            return;
        }
        gradualTarget = pending.get().durationTicks();
        gradualProgress += ticks;
        if (gradualProgress >= gradualTarget) {
            gradualFlourishCycles = GRADUAL_FLOURISH_CYCLES;
            if (level != null) EvolutionEffects.playDefaultStartSound(level, worldPosition);
        }
    }

    /** One step of the end-of-evolution flourish; the upgrade is applied on its last step. */
    protected void tickGradualFlourish() {
        if (!isEvolutionFlourishing()) return;

        if (level instanceof ServerLevel serverLevel) {
            EvolutionEffects.spawnParticles(serverLevel, worldPosition,
                    EvolutionEffects.getProgress(gradualFlourishCycles, GRADUAL_FLOURISH_CYCLES),
                    gradualFlourishCycles == 0, EvolutionEffects.DEFAULT_TINT, EvolutionEffects.Cadence.CYCLE);
        }

        gradualFlourishCycles--;
        if (gradualFlourishCycles < 0) completeGradualEvolution();
    }

    /** Applies the module's upgrade in place and consumes the module. It is evaluated again first in case the state
     * changed during the flourish; if it no longer applies the module stays in the slot. */
    private void completeGradualEvolution() {
        Optional<EvolutionResult.Upgrade> upgrade = getPendingUpgrade();
        gradualProgress = 0;
        gradualFlourishCycles = -1;
        if (upgrade.isEmpty()) return;

        EVOLUTION_INVENTORY.setStackInSlot(0, ItemStack.EMPTY);
        setEvolutionState(upgrade.get().newState());
        setChanged();
        updateBlock();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("evolution_inventory", EVOLUTION_INVENTORY.serializeNBT(registries));
        tag.putInt("gradual_progress", gradualProgress);
        tag.putInt("gradual_target", gradualTarget);
        tag.putInt("gradual_flourish_cycles", gradualFlourishCycles);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("evolution_inventory")) {
            loadItemHandler(EVOLUTION_INVENTORY, 1, registries, tag.getCompound("evolution_inventory"));
        }
        gradualProgress = tag.getInt("gradual_progress");
        gradualTarget = tag.contains("gradual_target") ? tag.getInt("gradual_target") : 1;
        gradualFlourishCycles = tag.contains("gradual_flourish_cycles") ? tag.getInt("gradual_flourish_cycles") : -1;
    }
}
