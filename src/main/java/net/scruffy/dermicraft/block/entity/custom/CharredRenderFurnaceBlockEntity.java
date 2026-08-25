package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.screen.custom.charred_render_furnace.CharredRenderFurnaceMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Charred Render Furnace -- the Render Furnace family's Tier 2 evolution, same shape as
 * {@link CharredMetastasizerBlockEntity}. Everything about the base engine (fuel, recipe lookup,
 * visual state, item/fluid IO layout, Module tab, evolution mechanic) is inherited from
 * {@link RenderFurnaceBlockEntity} unchanged; the only real difference is {@link #canEvolve()}
 * (already evolved, nothing further to do). Unlike the Masticator/Metastasizer/Render Kiln
 * family, there's no hazard-gated tank to unconditionally widen here -- see
 * {@link RenderFurnaceBlockEntity}'s own javadoc for why this evolution is a pure stat upgrade
 * (double fuel capacity, 1.25x speed, via {@code MachineTier.CHARRED_NO_HEALTH}).
 */
public class CharredRenderFurnaceBlockEntity extends RenderFurnaceBlockEntity {

    public CharredRenderFurnaceBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CHARRED_RENDER_FURNACE_BE.get(), pos, blockState);
    }

    // Already evolved -- installing a Module here does nothing further to accumulate toward.
    @Override
    protected boolean canEvolve() {
        return false;
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.CHARRED_RENDER_FURNACE);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CharredRenderFurnaceMenu(containerId, playerInventory, this);
    }
}
