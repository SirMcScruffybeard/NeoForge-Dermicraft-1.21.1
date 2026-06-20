package net.scruffy.dermicraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.puddle_crafting.PuddleCraftingRecipe;
import net.scruffy.dermicraft.recipe.puddle_crafting.PuddleCraftingRecipeInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PuddleCraftHelper {

    /**
     * Scans a specific block coordinate for fluids and floating items to generate a recipe input snapshot.
     */
    public static PuddleCraftingRecipeInput getInput(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Fluid fluid = state.getFluidState().getType();

        AABB searchBox = new AABB(pos).inflate(0.25, 0.0, 0.25).expandTowards(0, 0.5, 0);

        List<ItemEntity> itemEntities = level.getEntitiesOfClass(ItemEntity.class, searchBox);

        List<ItemStack> itemPool = itemEntities.stream()
                .map(ItemEntity::getItem)
                .filter(stack -> !stack.isEmpty())
                .toList();

        return new PuddleCraftingRecipeInput(itemPool, fluid);
    }

    /**
     * Surgically shrinks only the items required by the recipe, leaving extra items untouched.
     */
    public static void consumeRequiredItems(Level level, BlockPos pos, PuddleCraftingRecipe recipe) {

        AABB searchBox = new AABB(pos).inflate(0.25, 0.0, 0.25).expandTowards(0, 0.5, 0);

        List<ItemEntity> worldEntities = level.getEntitiesOfClass(ItemEntity.class, searchBox);
        List<ItemStack> availablePool = new ArrayList<>();

        for (ItemEntity entity : worldEntities) {
            if (!entity.getItem().isEmpty()) {
                availablePool.add(entity.getItem());
            }
        }

        for (Ingredient ingredient : recipe.ingredients()) {
            boolean ingredientSatisfied = false;

            for (ItemStack poolStack : availablePool) {
                if (!poolStack.isEmpty() && ingredient.test(poolStack)) {
                    poolStack.shrink(1);
                    ingredientSatisfied = true;
                    break;
                }
            }
        }

        for (ItemEntity entity : worldEntities) {
            if (entity.getItem().isEmpty()) {
                entity.discard();
            } else {
                entity.setItem(entity.getItem());
            }
        }
    }

    public static RecipeHolder<PuddleCraftingRecipe> getRecipe(ServerLevel level, PuddleCraftingRecipeInput input) {
        Optional<RecipeHolder<PuddleCraftingRecipe>> match = level.getRecipeManager()
                .getRecipeFor(ModRecipes.PUDDLE_FLUID_CRAFTING_TYPE.get(), input, level);

        if (match.isPresent()) {
            return  match.get();
        }
        return null;
    }

    public static boolean isRecipeValid(RecipeHolder<PuddleCraftingRecipe> recipe) {
        return RecipeUtil.isRecipeValid(recipe);
    }
}
