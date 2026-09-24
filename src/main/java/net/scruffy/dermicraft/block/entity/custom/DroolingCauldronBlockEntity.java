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
import net.scruffy.dermicraft.recipe.drooling.VagueDroolingRecipe;
import net.scruffy.dermicraft.screen.custom.drooling_cauldron.DroolingCauldronMenu;
import net.scruffy.dermicraft.tank.ModFluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Drooling Cauldron -- water, per {@link #currentTargetFluid} below. Everything else lives on
 * {@link DroolingMachineBlockEntity}, the shared Drooling-family base this,
 * {@link DroolingCrucibleBlockEntity}, and {@link DroolingGeodeBlockEntity} all extend.
 *
 * <p>Standalone Tier 1 machine -- same simplicity as Crucible/Geode, a fixed single target fluid
 * with no Module-driven behavior. Previously had a Cauldron-evolves-into-Crucible mechanic
 * (Evolution Module in the Module slot both switched the target fluid and slowly transformed this
 * block into a {@link DroolingCrucibleBlockEntity} in place); removed 2026-09-21 in favor of
 * Crucible being its own standalone alternate path (its own Early Implant recipe now), the same
 * relationship Cauldron already has with Geode. A future Drooling-family evolution mechanic is
 * planned to increase production/add Module slots instead of changing what's produced -- not this.
 */
public class DroolingCauldronBlockEntity extends DroolingMachineBlockEntity<VagueDroolingRecipe> implements MenuProvider {

    /** Same 5 buckets the original hardcoded-water version always had. */
    public static final int CAPACITY = ModFluidTank.BUCKET_VOLUME * 5;
    /** Same 4 mB/s the original hardcoded-water version always had. */
    public static final int PASSIVE_YIELD = 4;

    public DroolingCauldronBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DROOLING_CAULDRON_BE.get(), pos, blockState);
    }

    @Override
    protected Fluid currentTargetFluid() {
        return Fluids.WATER;
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
    protected RecipeType<VagueDroolingRecipe> recipeType() {
        return ModRecipes.VAGUE_DROOLING_TYPE.get();
    }

    @Override
    @NotNull
    public Component getDisplayName() {
        return super.getDisplayName(ModBlocks.DROOLING_CAULDRON);
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DroolingCauldronMenu(containerId, inventory, this);
    }
}
