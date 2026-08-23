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
 * lookup, visual state, item/fluid IO layout) is inherited from {@link MetastasizerBlockEntity}
 * unchanged; the only real difference is {@link #createReagentTank()}, overridden to accept and
 * hold {@code HazardProfile.TIER_2} fluids instead of the base's TIER_1-locked reagent tank. No
 * Module tab/slot exists on the Metastasizer family yet (unlike Masticator), so this is scoped to
 * the same starting point Charred Masticator had before that follow-up work.
 */
public class CharredMetastasizerBlockEntity extends MetastasizerBlockEntity {

    public CharredMetastasizerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CHARRED_METASTASIZER_BE.get(), pos, blockState);
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
