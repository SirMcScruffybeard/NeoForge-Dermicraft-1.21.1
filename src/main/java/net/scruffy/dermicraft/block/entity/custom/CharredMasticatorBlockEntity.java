package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.hazard.HazardProfile;
import net.scruffy.dermicraft.recipe.masticating.MasticatingRecipe;
import net.scruffy.dermicraft.screen.custom.charred_masticator.CharredMasticatorMenu;
import net.scruffy.dermicraft.tank.VulnerableTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Charred Masticator -- the Masticator family's hazard-gated Tier 2 evolution. Everything about the
 * base engine (fuel/health, recipe lookup, visual state, item/fluid IO layout) is inherited from
 * {@link MasticatorBlockEntity} unchanged; the only real difference is the capability leap this class
 * exists to provide: {@link #createIngredientTank()}/{@link #createResultTank()} are overridden to
 * accept and hold {@code HazardProfile.TIER_2} fluids (thermal-hazard fluids -- lava-class), which
 * the base Masticator's plain {@code VulnerableTank(capacity, slot)} tanks (locked to TIER_1) reject
 * outright. Per {@code MachineTier}'s own javadoc, a genuine capability leap is a hook override on
 * the family's block entity, not a new stat-only tier constant -- this class is exactly that hook.
 */
public class CharredMasticatorBlockEntity extends MasticatorBlockEntity {

    public CharredMasticatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CHARRED_MASTICATOR_BE.get(), pos, blockState);
    }

    // Already evolved -- installing a Thermal Evolution Module here does nothing: this class's own
    // tanks are unconditionally TIER_2 regardless of any Module (see createIngredientTank/
    // createResultTank below), and there's no further Tier for it to accumulate progress toward.
    @Override
    protected boolean canEvolve() {
        return false;
    }

    @Override
    protected VulnerableTank createIngredientTank() {
        return new VulnerableTank(getTier().tankCapacity(), 1, () -> HazardProfile.TIER_2) {
            @Override
            protected void onContentsChanged() {
                if (level != null && !level.isClientSide()) {
                    // Bypass the item optimization check entirely because a puddle update occurred
                    Optional<RecipeHolder<MasticatingRecipe>> recipeOpt = getRecipeOptional();
                    setActiveRecipe(recipeOpt);

                    if (isRecipeValid(activeRecipe)) {
                        setMaxProgress();
                        setResultAmount();
                    } else {
                        resetActiveRecipe();
                        resetMaxProgress();
                        resetProgress();
                    }
                }
                setChanged();
            }
        };
    }

    @Override
    protected VulnerableTank createResultTank() {
        return new VulnerableTank(getTier().tankCapacity(), 2, () -> HazardProfile.TIER_2) {
            @Override
            protected void onContentsChanged() {
                if (level != null && !level.isClientSide()) {
                    this.pushFluidToBelowNeighbour(level, worldPosition);
                    setChanged();
                }
            }
        };
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.CHARRED_MASTICATOR);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CharredMasticatorMenu(containerId, playerInventory, this);
    }
}
