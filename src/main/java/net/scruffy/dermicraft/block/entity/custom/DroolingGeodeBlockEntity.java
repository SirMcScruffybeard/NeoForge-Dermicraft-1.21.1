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
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.drooling.VagueDroolingGeodeRecipe;
import net.scruffy.dermicraft.screen.custom.drooling_geode.DroolingGeodeMenu;
import net.scruffy.dermicraft.tank.ModFluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Drooling Geode -- Stone Blend, per {@link #currentTargetFluid} below. Everything else lives on
 * {@link DroolingMachineBlockEntity}, the shared Drooling-family base this,
 * {@link DroolingCauldronBlockEntity}, and {@link DroolingCrucibleBlockEntity} all extend.
 *
 * <p>Standalone Stage 1 machine, same tier as Drooling Cauldron (same Early Implant recipe shape,
 * a vanilla Cauldron plus a seed Stone block) -- no evolution mechanic, matching Crucible's own
 * simplicity rather than Cauldron's Evolution Module complexity. Passive rate and tank capacity
 * match Cauldron's own numbers exactly, since Stone Blend isn't meant to outperform mining plus a
 * real Masticator -- this is a convenience trickle, not a power play (see the design discussion).
 */
public class DroolingGeodeBlockEntity extends DroolingMachineBlockEntity<VagueDroolingGeodeRecipe> implements MenuProvider {

    /** Same 5 buckets Cauldron's own tank uses -- same tier, same convenience-not-power framing. */
    public static final int CAPACITY = ModFluidTank.BUCKET_VOLUME * 5;
    /** Same 4 mB/s Cauldron's own water always produces. */
    public static final int PASSIVE_YIELD = 4;

    public DroolingGeodeBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DROOLING_GEODE_BE.get(), pos, blockState);
    }

    @Override
    protected Fluid currentTargetFluid() {
        return ModFluids.SOURCE_STONE_BLEND.get();
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
    protected RecipeType<VagueDroolingGeodeRecipe> recipeType() {
        return ModRecipes.VAGUE_DROOLING_GEODE_TYPE.get();
    }

    @Override
    @NotNull
    public Component getDisplayName() {
        return super.getDisplayName(ModBlocks.DROOLING_GEODE);
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DroolingGeodeMenu(containerId, inventory, this);
    }
}
