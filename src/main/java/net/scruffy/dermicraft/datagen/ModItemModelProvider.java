package net.scruffy.dermicraft.datagen;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;

import java.util.Objects;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Dermicraft.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {

        ////////////////////Basic Tools\\\\\\\\\\\\\\\\\\\\
        handheldItem(ModItems.SCALPEL.get());
        handheldItem(ModItems.SUTURE_KIT.get());
        handheldItem(ModItems.FORCEPS.get());


        ////////////////////Basic Tools\\\\\\\\\\\\\\\\\\\\
        handheldItem(ModItems.EYE.get());
        basicItem(ModItems.NERVE_CLUSTER.get());
        basicItem(ModItems.DENSE_MUSCLE.get());
        basicItem(ModItems.MRE.get());
        basicItem(ModItems.MEAT_FLAVORED_MEAT.get());


        ////////////////////Buckets\\\\\\\\\\\\\\\\\\\\
        chunkyBucketItem(ModFluids.CALCIUM_BLEND_BUCKET.get());
        chunkyBucketItem(ModFluids.CARBON_BLEND_BUCKET.get());
        chunkyBucketItem(ModFluids.PROTEIN_BLEND_BUCKET.get());

        chunkyBucketItem(ModFluids.CRUDE_SLURRY_BUCKET.get());
        chunkyBucketItem(ModFluids.CONCENTRATED_SLURRY_BUCKET.get());

        thinBucketItem(ModFluids.PRIMITIVE_CATALYST_BUCKET.get());

        chunkyBucketItem(ModFluids.STONE_BLEND_BUCKET.get());
        chunkyBucketItem(ModFluids.SILICA_BLEND_BUCKET.get());
        chunkyBucketItem(ModFluids.CLAY_BLEND_BUCKET.get());

        chunkyBucketItem(ModFluids.FERROUS_BLEND_BUCKET.get());
        chunkyBucketItem(ModFluids.CUPROUS_BLEND_BUCKET.get());
        chunkyBucketItem(ModFluids.AUROUS_BLEND_BUCKET.get());
        chunkyBucketItem(ModFluids.PULP_BLEND_BUCKET.get());

        thinBucketItem(ModFluids.F_STUFF_BUCKET.get());
        thinBucketItem(ModFluids.C_STUFF_BUCKET.get());

        chunkyBucketItem(ModFluids.MOLTEN_REDSTONE_BUCKET.get());
        chunkyBucketItem(ModFluids.MOLTEN_QUARTZ_BUCKET.get());
        chunkyBucketItem(ModFluids.MOLTEN_GLOWSTONE_BUCKET.get());
        chunkyBucketItem(ModFluids.MOLTEN_AMETHYST_BUCKET.get());
        chunkyBucketItem(ModFluids.MOLTEN_DIAMOND_BUCKET.get());
        chunkyBucketItem(ModFluids.MOLTEN_OBSIDIAN_BUCKET.get());
        chunkyBucketItem(ModFluids.MOLTEN_LAPIS_BUCKET.get());
    }

    // Chunky fluids (blends/slurries) use the chunky fill overlay; layer1 gets tinted at runtime per-fluid.
    private ItemModelBuilder chunkyBucketItem(Item item) {
        return bucketItem(item, "item/bucket/bucket_chunky_fluid");
    }

    // Thin fluids (catalysts, F/C-Stuff) use the thin, water-like fill overlay.
    private ItemModelBuilder thinBucketItem(Item item) {
        return bucketItem(item, "item/bucket/bucket_fluid_thin");
    }

    // layer0 is the untinted vanilla bucket body; layer1 is the fill overlay, tinted per-fluid via item color handlers.
    private ItemModelBuilder bucketItem(Item item, String fillTexturePath) {
        ResourceLocation id = Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item));
        return getBuilder(id.toString())
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", ResourceLocation.withDefaultNamespace("item/bucket"))
                .texture("layer1", ResourceLocation.fromNamespaceAndPath(id.getNamespace(), fillTexturePath));
    }
}
