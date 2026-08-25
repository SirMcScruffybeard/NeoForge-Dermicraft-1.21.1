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
import net.scruffy.dermicraft.screen.custom.charred_render_kiln.CharredRenderKilnMenu;
import net.scruffy.dermicraft.tank.VulnerableTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Charred Render Kiln -- the Render Kiln family's hazard-gated Tier 2 evolution. Everything about
 * the base engine (fuel/health, recipe lookup, visual state, item/fluid IO layout) is inherited from
 * {@link RenderKilnBlockEntity} unchanged; the only real difference is the capability leap this class
 * exists to provide: {@link #createInputTank()} is overridden to accept and hold
 * {@code HazardProfile.TIER_2} fluids (thermal-hazard fluids -- lava-class), which the base Kiln's
 * plain {@code VulnerableTank(capacity, slot)} tank (locked to TIER_1) rejects outright.
 */
public class CharredRenderKilnBlockEntity extends RenderKilnBlockEntity {

    public CharredRenderKilnBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CHARRED_RENDER_KILN_BE.get(), pos, blockState);
    }

    // Already evolved -- installing a Module here does nothing: this class's own tank is
    // unconditionally TIER_2 regardless of any Module, and there's no further Tier to accumulate
    // progress toward.
    @Override
    protected boolean canEvolve() {
        return false;
    }

    @Override
    protected VulnerableTank createInputTank() {
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
        return getDisplayName(ModBlocks.CHARRED_RENDER_KILN);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CharredRenderKilnMenu(containerId, playerInventory, this);
    }
}
