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
import net.scruffy.dermicraft.screen.custom.charred_metastasizer.CharredMetastasizerMenu;
import net.scruffy.dermicraft.tank.VulnerableTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Charred Metastasizer -- the Metastasizer family's hazard-gated Tier 2 evolution, same shape as
 * {@link CharredMasticatorBlockEntity}. Everything about the base engine (fuel/health, recipe
 * lookup, visual state, item/fluid IO layout, Module tab, evolution mechanic) is inherited from
 * {@link MetastasizerBlockEntity} unchanged; the only real differences are
 * {@link #createReagentTank()} (unconditional {@code HazardProfile.TIER_2} instead of the base's
 * Module-dependent one) and {@link #canEvolve()} (already evolved, nothing further to do).
 */
public class CharredMetastasizerBlockEntity extends MetastasizerBlockEntity {

    public CharredMetastasizerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CHARRED_METASTASIZER_BE.get(), pos, blockState);
    }

    // Already evolved -- installing a Module here does nothing: this class's own tanks are
    // unconditionally TIER_2 regardless of any Module, and there's no further Tier to accumulate
    // progress toward.
    @Override
    protected boolean canEvolve() {
        return false;
    }

    // Matches createReagentTank() below -- without this override, the inherited
    // MetastasizerBlockEntity#installedHazardProfile() would report TIER_1 (+ any Safety Module)
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
                    resolveRecipe();
                    setChanged();
                }
            }
        };
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.CHARRED_METASTASIZER);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CharredMetastasizerMenu(containerId, playerInventory, this);
    }
}
