package net.scruffy.dermicraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Shared "substitute an item for its smelted result" lookup -- Blaze Essence's signature trait on
 * both weapons (Shatter's universal auto-smelt on any mined block, see {@code ShatterEvents
 * #onBlockDropsAutoSmelt}; Sunder's log-into-charcoal while sawing, see {@code SunderItem
 * #tickFelling}). Reuses the real registered {@link SmeltingRecipe} (result AND experience), same
 * as a player manually smelting the item in a Furnace, rather than hardcoding a result/XP pair --
 * stays correct if a datapack ever changes a smelting recipe, and naturally means a block with no
 * smelting recipe at all just isn't affected.
 */
public final class AutoSmeltUtil {

    private AutoSmeltUtil() {
    }

    public record SmeltResult(ItemStack result, float experience) {
    }

    /** The smelted result and XP for ONE unit of {@code input}, or empty if no SmeltingRecipe
     * matches it at all. Callers with a stacked input are responsible for scaling both the result
     * count and the experience by however many units they're substituting. */
    public static Optional<SmeltResult> smeltOne(Level level, ItemStack input) {
        if (input.isEmpty()) return Optional.empty();

        SingleRecipeInput recipeInput = new SingleRecipeInput(input.copyWithCount(1));
        Optional<RecipeHolder<SmeltingRecipe>> recipe =
                level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, level);

        return recipe.map(holder -> new SmeltResult(
                holder.value().assemble(recipeInput, level.registryAccess()),
                holder.value().getExperience()));
    }

    /** Awards {@code amount} XP as one orb, same fractional-remainder rounding vanilla's own
     * furnace uses when a player takes smelted items out of the result slot (a whole number of
     * orbs, plus a random chance at one more to cover the fractional remainder) -- so accumulating
     * a fractional per-item XP value (e.g. Charcoal's 0.15) across several items still averages out
     * correctly over time instead of always rounding the same direction. */
    public static void awardExperience(ServerLevel level, Vec3 pos, float amount) {
        int whole = (int) amount;
        float remainder = amount - whole;
        if (level.getRandom().nextFloat() < remainder) whole++;
        if (whole > 0) {
            ExperienceOrb.award(level, pos, whole);
        }
    }

    public static void awardExperience(ServerLevel level, BlockPos pos, float amount) {
        awardExperience(level, Vec3.atCenterOf(pos), amount);
    }
}
