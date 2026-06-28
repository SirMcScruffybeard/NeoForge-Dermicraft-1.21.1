package net.scruffy.dermicraft.datagen.tag;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.main.Dermicraft;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModFluidTagProvider extends FluidTagsProvider {
    public ModFluidTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, provider, Dermicraft.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

        tag(FluidTags.WATER)

        ;

        tag(FluidTags.LAVA)

        ;

        tag(ModTags.Fluids.BIOFUELS)
                .add(ModFluids.SOURCE_CRUDE_SLURRY.get())
        ;

        tag(ModTags.Fluids.HAZARDOUS)
                .addTag(FluidTags.LAVA)

        ;

        tag(ModTags.Fluids.THICK)
                .add(ModFluids.SOURCE_CALCIUM_BLEND.get())
                .add(ModFluids.SOURCE_CARBON_BLEND.get())
                .add(ModFluids.SOURCE_PROTEIN_BLEND.get())

                .add(ModFluids.SOURCE_CRUDE_SLURRY.get())

                .add(ModFluids.SOURCE_STONE_BLEND.get())
                .add(ModFluids.SOURCE_SILICA_BLEND.get())
                .add(ModFluids.SOURCE_CLAY_BLEND.get())

                .add(ModFluids.SOURCE_FERROUS_BLEND.get())
                .add(ModFluids.SOURCE_CUPROUS_BLEND.get())
                .add(ModFluids.SOURCE_AUROUS_BLEND.get())
        ;

        tag(ModTags.Fluids.THIN)
                .add(Fluids.WATER);
        ;
    }

}