package net.scruffy.dermicraft.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.recipe.dipping.DippingRecipe;
import net.scruffy.dermicraft.recipe.drooling.VagueDroolingRecipe;
import net.scruffy.dermicraft.recipe.early_implant.EarlyImplantRecipe;
import net.scruffy.dermicraft.recipe.effluencing.EffluencingRecipe;
import net.scruffy.dermicraft.recipe.masticating.MasticatingRecipe;
import net.scruffy.dermicraft.recipe.metastasizing.MetastasizingRecipe;
import net.scruffy.dermicraft.recipe.mutating.MutatingRecipe;
import net.scruffy.dermicraft.recipe.puddle_crafting.PuddleCraftingRecipe;
import net.scruffy.dermicraft.recipe.rendering.RenderingRecipe;

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

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MetastasizingRecipe>> METASTASIZING_SERIALIZER =
            SERIALIZERS.register("metastasizing", MetastasizingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<MetastasizingRecipe>> METASTASIZING_TYPE =
            TYPES.register("metastasizing", () -> new RecipeType<MetastasizingRecipe>() {
                @Override
                public String toString() {
                    return "metastasizing";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MutatingRecipe>> MUTATING_SERIALIZER =
            SERIALIZERS.register("mutating", MutatingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<MutatingRecipe>> MUTATING_TYPE =
            TYPES.register("mutating", () -> new RecipeType<MutatingRecipe>() {
                @Override
                public String toString() {
                    return "mutating";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EffluencingRecipe>> EFFLUENCING_SERIALIZER =
            SERIALIZERS.register("effluencing", EffluencingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<EffluencingRecipe>> EFFLUENCING_TYPE =
            TYPES.register("effluencing", () -> new RecipeType<EffluencingRecipe>() {
                @Override
                public String toString() {
                    return "effluencing";
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




    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DippingRecipe>> DIPPING_SERIALIZER =
            SERIALIZERS.register("dipping", DippingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<DippingRecipe>> DIPPING_TYPE =
            TYPES.register("dipping", () -> new RecipeType<DippingRecipe>() {
                @Override
                public String toString() {
                    return "dipping";
                }
            });


    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RenderingRecipe>> RENDERING_SERIALIZER =
            SERIALIZERS.register("rendering", RenderingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<RenderingRecipe>> RENDERING_TYPE =
            TYPES.register("rendering", () -> new RecipeType<RenderingRecipe>() {
                @Override
                public String toString() {
                    return "rendering";
                }
            });


    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }
}
