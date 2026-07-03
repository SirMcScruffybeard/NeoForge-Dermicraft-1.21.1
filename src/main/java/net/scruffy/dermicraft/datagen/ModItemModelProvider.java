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


        ////////////////////Buckets\\\\\\\\\\\\\\\\\\\\
        bucketItem(ModFluids.CALCIUM_BLEND_BUCKET.get());
        bucketItem(ModFluids.CARBON_BLEND_BUCKET.get());
        bucketItem(ModFluids.PROTEIN_BLEND_BUCKET.get());

        bucketItem(ModFluids.CRUDE_SLURRY_BUCKET.get());

        bucketItem(ModFluids.PRIMITIVE_CATALYST_BUCKET.get());

        bucketItem(ModFluids.STONE_BLEND_BUCKET.get());
        bucketItem(ModFluids.SILICA_BLEND_BUCKET.get());
        bucketItem(ModFluids.CLAY_BLEND_BUCKET.get());

        bucketItem(ModFluids.FERROUS_BLEND_BUCKET.get());
        bucketItem(ModFluids.CUPROUS_BLEND_BUCKET.get());
        bucketItem(ModFluids.AUROUS_BLEND_BUCKET.get());

        bucketItem(ModFluids.F_STUFF_BUCKET.get());
        bucketItem(ModFluids.C_STUFF_BUCKET.get());
    }

    // Bucket textures live under textures/bucket/ rather than the default textures/item/.
    private ItemModelBuilder bucketItem(Item item) {
        ResourceLocation id = Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item));
        return getBuilder(id.toString())
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "item/bucket/" + id.getPath()));
    }
}
