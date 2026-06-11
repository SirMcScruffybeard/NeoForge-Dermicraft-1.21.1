package net.scruffy.dermicraft.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;

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
        basicItem(ModFluids.CALCIUM_BLEND_BUCKET.get());
        basicItem(ModFluids.CARBON_BLEND_BUCKET.get());
        basicItem(ModFluids.PROTEIN_BLEND_BUCKET.get());

        basicItem(ModFluids.CRUDE_SLURRY_BUCKET.get());
    }
}
