package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.drooling.VagueDroolingCrucibleRecipe;
import net.scruffy.dermicraft.screen.custom.drooling_crucible.DroolingCrucibleMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Drooling Crucible -- lava, per {@link #currentTargetFluid} below. Everything else lives on
 * {@link DroolingMachineBlockEntity}, the shared Drooling-family base the Cauldron, Crucible and Geode all extend.
 *
 * <p>Evolves in place like the fueled machines: an Evolution Module in its Evolution slot raises its tier after
 * enough seconds of real production, which raises its passive yield (its own config table) and craft speed (the stat table's production
 * multiplier), its tank, and its Module slots. It handles no hazards, so tolerances do not apply to it.
 */
public class DroolingCrucibleBlockEntity extends DroolingMachineBlockEntity<VagueDroolingCrucibleRecipe> implements MenuProvider {

    public DroolingCrucibleBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DROOLING_CRUCIBLE_BE.get(), pos, blockState);
    }

    @Override
    protected Fluid currentTargetFluid() {
        return Fluids.LAVA;
    }

    @Override
    protected net.scruffy.dermicraft.machine.MachineStatKey getStatKey() {
        return net.scruffy.dermicraft.machine.MachineStatKey.DROOLING_CRUCIBLE;
    }

    @Override
    protected RecipeType<VagueDroolingCrucibleRecipe> recipeType() {
        return ModRecipes.VAGUE_DROOLING_CRUCIBLE_TYPE.get();
    }

    @Override
    @NotNull
    public Component getDisplayName() {
        return super.getDisplayName(ModBlocks.DROOLING_CRUCIBLE);
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DroolingCrucibleMenu(containerId, inventory, this);
    }
}
