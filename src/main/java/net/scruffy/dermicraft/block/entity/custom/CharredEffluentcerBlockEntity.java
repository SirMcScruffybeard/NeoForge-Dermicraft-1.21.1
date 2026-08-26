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
import net.scruffy.dermicraft.screen.custom.charred_effluentcer.CharredEffluentcerMenu;
import net.scruffy.dermicraft.tank.VulnerableTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Charred Effluentcer -- the Effluentcer family's hazard-gated Tier 2 evolution. Everything about
 * the base engine (fuel/health, recipe lookup, visual state, item/fluid IO layout) is inherited from
 * {@link EffluentcerBlockEntity} unchanged; the only real difference is the capability leap this
 * class exists to provide: {@link #createInputTank(int)}/{@link #createResultTank()} are overridden
 * to accept and hold {@code HazardProfile.TIER_2} fluids (thermal-hazard fluids -- lava-class),
 * which the base Effluentcer's plain hazard-gated tanks (locked to TIER_1 plus whatever a Module
 * grants) don't tolerate permanently. Per {@code MachineTier}'s own javadoc, a genuine capability
 * leap is a hook override on the family's block entity, not a new stat-only tier constant -- this
 * class is exactly that hook.
 */
public class CharredEffluentcerBlockEntity extends EffluentcerBlockEntity {

    public CharredEffluentcerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CHARRED_EFFLUENTCER_BE.get(), pos, blockState);
    }

    // Already evolved -- installing an Evolution Module here does nothing: this class's own tanks
    // are unconditionally TIER_2 regardless of any Module (see createInputTank/createResultTank
    // below), and there's no further Tier for it to accumulate progress toward.
    @Override
    protected boolean canEvolve() {
        return false;
    }

    // Matches createInputTank()/createResultTank() below -- without this override, the inherited
    // EffluentcerBlockEntity#installedHazardProfile() would report TIER_1 (+ any Safety Module)
    // instead of this class's real, unconditional TIER_2 tolerance.
    @Override
    protected HazardProfile installedHazardProfile() {
        return HazardProfile.TIER_2;
    }

    @Override
    protected VulnerableTank createInputTank(int slot) {
        return new VulnerableTank(getTier().tankCapacity(), slot, () -> HazardProfile.TIER_2) {
            @Override
            protected void onContentsChanged() {
                if (level != null && !level.isClientSide()) {
                    resolveRecipe();
                }
                setChanged();
            }
        };
    }

    @Override
    protected VulnerableTank createResultTank() {
        return new VulnerableTank(getTier().tankCapacity(), 3, () -> HazardProfile.TIER_2) {
            @Override
            protected void onContentsChanged() {
                if (level != null && !level.isClientSide()) {
                    if (autoDrainEnabled) {
                        this.pushFluidToBelowNeighbour(level, worldPosition);
                    }
                    setChanged();
                }
            }
        };
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.CHARRED_EFFLUENTCER);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CharredEffluentcerMenu(containerId, playerInventory, this);
    }
}
