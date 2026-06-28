package net.scruffy.dermicraft.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.recipe.drooling.VagueDroolingRecipe;
import net.scruffy.dermicraft.recipe.early_implant.EarlyImplantRecipe;
import net.scruffy.dermicraft.recipe.masticating.MasticatingRecipe;
import net.scruffy.dermicraft.recipe.puddle_crafting.PuddleCraftingRecipe;

public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Dermicraft.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Dermicraft.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<VagueDroolingRecipe>> VAGUE_DROOLING_SERIALIZER =
            SERIALIZERS.register("vague_drooling", VagueDroolingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<VagueDroolingRecipe>> VAGUE_DROOLING_TYPE =
            TYPES.register("vague_drooling", () -> new RecipeType<VagueDroolingRecipe>() {
                @Override
                public String toString() {
                    return "vague_drooling";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EarlyImplantRecipe>> EARLY_IMPLANT_SERIALIZER =
            SERIALIZERS.register("early_implant", EarlyImplantRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<EarlyImplantRecipe>> EARLY_IMPLANT_TYPE =
            TYPES.register("early_implant", () -> new RecipeType<EarlyImplantRecipe>() {
                @Override
                public String toString() {
                    return "early_implant";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MasticatingRecipe>> MASTICATING_SERIALIZER =
            SERIALIZERS.register("masticating", MasticatingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<MasticatingRecipe>> MASTICATING_TYPE =
            TYPES.register("masticating", () -> new RecipeType<MasticatingRecipe>() {
                @Override
                public String toString() {
                    return "masticating";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PuddleCraftingRecipe>> PUDDLE_FLUID_CRAFTING_SERIALIZER =
            SERIALIZERS.register("puddle_fluid_crafting", PuddleCraftingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<PuddleCraftingRecipe>> PUDDLE_FLUID_CRAFTING_TYPE =
            TYPES.register("puddle_fluid_crafting", () -> new RecipeType<PuddleCraftingRecipe>() {
                @Override
                public String toString() {
                    return "puddle_fluid_crafting";
                }
            });





    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }
}
