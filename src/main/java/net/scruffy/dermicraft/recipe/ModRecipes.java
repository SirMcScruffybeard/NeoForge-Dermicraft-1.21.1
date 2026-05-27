package net.scruffy.dermicraft.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.recipe.early_implant.EarlyImplantRecipe;

public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Dermicraft.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Dermicraft.MOD_ID);


    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EarlyImplantRecipe>> EARLY_IMPLANT_SERIALIZER =
            SERIALIZERS.register("early_implant", EarlyImplantRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<EarlyImplantRecipe>> EARLY_IMPLANT_TYPE =
            TYPES.register("early_implant", () -> new RecipeType<EarlyImplantRecipe>() {
                @Override
                public String toString() {
                    return "early_implant";
                }
            });





    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }
}
