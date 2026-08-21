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
import net.scruffy.dermicraft.tank.ModFluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Drooling Crucible -- lava, per {@link #currentTargetFluid} below. Everything else lives on
 * {@link DroolingMachineBlockEntity}, the shared Drooling-family base this and
 * {@link DroolingCauldronBlockEntity} both extend.
 *
 * <p>Standalone Tier 2 machine for now -- see dermicraft-machine-notes.md's Drooling Cauldron
 * entry ("Evolution Module family"): the evolution-FROM-Cauldron path (Evolution Catalyst
 * injection, or the gradual Evolution Module) is separate, not-yet-built work. This class's own
 * base passive rate matches Cauldron's exactly (4 mB/s), per direction -- not because the two are
 * required to match forever, just the confirmed starting number.
 */
public class DroolingCrucibleBlockEntity extends DroolingMachineBlockEntity<VagueDroolingCrucibleRecipe> implements MenuProvider {

    /** Same capacity as Cauldron's -- no reason given yet to differ. */
    public static final int CAPACITY = ModFluidTank.BUCKET_VOLUME * 5;
    /** Same rate as Cauldron's water, per direction. */
    public static final int PASSIVE_YIELD = 4;

    public DroolingCrucibleBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DROOLING_CRUCIBLE_BE.get(), pos, blockState);
    }

    @Override
    protected Fluid currentTargetFluid() {
        return Fluids.LAVA;
    }

    @Override
    protected int passiveYieldAmount() {
        return PASSIVE_YIELD;
    }

    @Override
    protected int tankCapacity() {
        return CAPACITY;
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
