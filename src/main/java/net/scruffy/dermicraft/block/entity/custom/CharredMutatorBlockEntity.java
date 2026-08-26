package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.hazard.HazardProfile;
import net.scruffy.dermicraft.screen.custom.charred_mutator.CharredMutatorMenu;
import net.scruffy.dermicraft.tank.VulnerableTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Charred Mutator -- the Mutator family's hazard-gated Tier 2 evolution. Everything about the base
 * engine (fuel/health, both MUTATE/FILL modes, visual state, item/fluid IO layout) is inherited from
 * {@link MutatorBlockEntity} unchanged; the only real difference is the capability leap this class
 * exists to provide: {@link #createReagentTank()} is overridden to accept and hold
 * {@code HazardProfile.TIER_2} fluids (thermal-hazard fluids -- lava-class), which the base
 * Mutator's plain hazard-gated tank (locked to TIER_1 plus whatever a Module grants) doesn't
 * tolerate permanently. Per {@code MachineTier}'s own javadoc, a genuine capability leap is a hook
 * override on the family's block entity, not a new stat-only tier constant -- this class is exactly
 * that hook.
 */
public class CharredMutatorBlockEntity extends MutatorBlockEntity {

    public CharredMutatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CHARRED_MUTATOR_BE.get(), pos, blockState);
    }

    // Already evolved -- installing an Evolution Module here does nothing: this class's own tank is
    // unconditionally TIER_2 regardless of any Module (see createReagentTank below), and there's no
    // further Tier for it to accumulate progress toward.
    @Override
    protected boolean canEvolve() {
        return false;
    }

    // The "Charred machines get an extra Module slot" upgrade -- see CharredTankBlockEntity's
    // identical override for the pilot.
    @Override
    public int moduleSlotCount() {
        return 2;
    }

    // Matches createReagentTank() below -- without this override, the inherited
    // MutatorBlockEntity#installedHazardProfile() would report TIER_1 (+ any Safety Module)
    // instead of this class's real, unconditional TIER_2 tolerance.
    @Override
    protected HazardProfile installedHazardProfile() {
        return HazardProfile.TIER_2;
    }

    @Override
    protected VulnerableTank createReagentTank() {
        return new VulnerableTank(getTier().tankCapacity(), 1, () -> HazardProfile.TIER_2) {
            @Override
            protected void onContentsChanged() {
                if (level != null && !level.isClientSide()) {
                    if (getMode() == Mode.MUTATE) resolveRecipe();
                    setChanged();
                }
            }
        };
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.CHARRED_MUTATOR);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CharredMutatorMenu(containerId, playerInventory, this);
    }
}
