package net.scruffy.dermicraft.recipe.early_implant;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.recipe.ModRecipes;

import java.util.ArrayList;
import java.util.List;

public class EarlyImplantRecipe implements Recipe<EarlyImplantRecipeInput> {

    private final List<Ingredient> ingredients;
    private final Ingredient sutureTool;
    private final FluidStack fluidIngredient;
    private final ItemStack result;

    public EarlyImplantRecipe(List<Ingredient> ingredients, Ingredient sutureItem, FluidStack fluidIngredient, ItemStack result) {
        this.ingredients = ingredients;
        this.sutureTool = sutureItem;
        this.fluidIngredient = fluidIngredient;
        this.result = result;
    }

    // Quantity-aware subtraction checking loop
    @Override
    public boolean matches(EarlyImplantRecipeInput input, Level level) {
        if (this.ingredients.isEmpty()) {
            return input.getItems().isEmpty();
        }


        if (input.getItems().size() != this.ingredients.size()) {
            return false;
        }

        // Create a modifiable pool copy of the items inside the tumor block
        List<ItemStack> inputPool = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            inputPool.add(input.getItem(i).copy());
        }

        // Loop over every ingredient requirement listed in the JSON file
        for (Ingredient ingredient : this.ingredients) {
            boolean matchedIngredient = false;

            for (ItemStack poolStack : inputPool) {
                if (!poolStack.isEmpty() && ingredient.test(poolStack)) {
                    poolStack.shrink(1); // Subtract 1 from the counted stack
                    matchedIngredient = true;
                    break;
                }
            }

            // An ingredient failed to find any matching item inside the pool
            if (!matchedIngredient) {
                return false;
            }
        }

        // Ensure everything left over in our working list has completely shrunk to 0
        return inputPool.stream().allMatch(ItemStack::isEmpty);
    }

    // Helper checks to keep BlockEntity interaction logic elegant
    public boolean testSuture(ItemStack stack) {
        return this.sutureTool.test(stack);
    }

    public boolean testFluid(FluidStack stack) {
        return FluidStack.isSameFluidSameComponents(this.fluidIngredient, stack)
                && stack.getAmount() >= this.fluidIngredient.getAmount();
    }

    // Standard Recipe Overrides
    @Override
    public ItemStack assemble(EarlyImplantRecipeInput input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true; // Shapeless world interaction handler
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.EARLY_IMPLANT_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        // Route this to wherever your custom recipe types are registered
        return ModRecipes.EARLY_IMPLANT_TYPE.get();
    }

    // Getters for the Codec structure
    public List<Ingredient> getIngredientList() { return this.ingredients; }
    public Ingredient getSutureTool() { return this.sutureTool; }
    public FluidStack getFluidIngredient() { return this.fluidIngredient; }
    public ItemStack getResult() { return this.result; }

    public static class Serializer implements RecipeSerializer<EarlyImplantRecipe> {
        @Override
        public MapCodec<EarlyImplantRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EarlyImplantRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        public static final MapCodec<EarlyImplantRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(EarlyImplantRecipe::getIngredientList),
                        Ingredient.CODEC.fieldOf("suture_tool").forGetter(EarlyImplantRecipe::getSutureTool),
                        FluidStack.CODEC.fieldOf("fluid_ingredient").forGetter(EarlyImplantRecipe::getFluidIngredient),
                        ItemStack.CODEC.fieldOf("result").forGetter(EarlyImplantRecipe::getResult)
                ).apply(instance, EarlyImplantRecipe::new)
        );


        public static final StreamCodec<RegistryFriendlyByteBuf, EarlyImplantRecipe> STREAM_CODEC = StreamCodec.of(
                (buf, recipe) -> {
                    buf.writeInt(recipe.ingredients.size());
                    for (Ingredient ing : recipe.ingredients) {
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing);
                    }
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.sutureTool);
                    FluidStack.STREAM_CODEC.encode(buf, recipe.fluidIngredient);
                    ItemStack.STREAM_CODEC.encode(buf, recipe.result);
                },
                buf -> {
                    int size = buf.readInt();
                    List<Ingredient> ingredients = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                    }
                    Ingredient sutureItem = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                    FluidStack fluidIngredient = FluidStack.STREAM_CODEC.decode(buf);
                    ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
                    return new EarlyImplantRecipe(ingredients, sutureItem, fluidIngredient, result);
                }
        );
    }
}
