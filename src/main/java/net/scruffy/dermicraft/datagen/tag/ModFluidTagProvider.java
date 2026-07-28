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
                .add(ModFluids.SOURCE_CONCENTRATED_SLURRY.get())
        ;

        // --- Hazard kinds ---
        tag(ModTags.Fluids.EXTREME_HEAT)
                .addTag(FluidTags.LAVA)
                .add(ModFluids.SOURCE_MOLTEN_REDSTONE.get())
                .add(ModFluids.SOURCE_MOLTEN_QUARTZ.get())
                .add(ModFluids.SOURCE_MOLTEN_GLOWSTONE.get())
                .add(ModFluids.SOURCE_MOLTEN_AMETHYST.get())
                .add(ModFluids.SOURCE_MOLTEN_DIAMOND.get())
                .add(ModFluids.SOURCE_MOLTEN_OBSIDIAN.get())
                .add(ModFluids.SOURCE_MOLTEN_LAPIS.get())
        ;

        tag(ModTags.Fluids.RADIATION_MILD)
                .add(ModFluids.SOURCE_MOLTEN_GLOWSTONE.get())
        ;

        tag(ModTags.Fluids.RADIATION_SEVERE)
        ;

        tag(ModTags.Fluids.BIOHAZARD)
        ;

        tag(ModTags.Fluids.METAPHYSICAL_MILD)
        ;

        tag(ModTags.Fluids.METAPHYSICAL_SEVERE)
        ;

        // Union of every hazard kind above.
        tag(ModTags.Fluids.HAZARDOUS)
                .addTag(ModTags.Fluids.EXTREME_HEAT)
                .addTag(ModTags.Fluids.RADIATION_MILD)
                .addTag(ModTags.Fluids.RADIATION_SEVERE)
                .addTag(ModTags.Fluids.BIOHAZARD)
                .addTag(ModTags.Fluids.METAPHYSICAL_MILD)
                .addTag(ModTags.Fluids.METAPHYSICAL_SEVERE)
        ;

        tag(ModTags.Fluids.THICK)
                .add(ModFluids.SOURCE_CALCIUM_BLEND.get())
                .add(ModFluids.SOURCE_CARBON_BLEND.get())
                .add(ModFluids.SOURCE_PROTEIN_BLEND.get())

                .add(ModFluids.SOURCE_CRUDE_SLURRY.get())
                .add(ModFluids.SOURCE_CONCENTRATED_SLURRY.get())

                .add(ModFluids.SOURCE_STONE_BLEND.get())
                .add(ModFluids.SOURCE_SILICA_BLEND.get())
                .add(ModFluids.SOURCE_CLAY_BLEND.get())

                .add(ModFluids.SOURCE_FERROUS_BLEND.get())
                .add(ModFluids.SOURCE_CUPROUS_BLEND.get())
                .add(ModFluids.SOURCE_AUROUS_BLEND.get())

                .add(ModFluids.SOURCE_PULP_BLEND.get())

                .add(ModFluids.SOURCE_MOLTEN_REDSTONE.get())
                .add(ModFluids.SOURCE_MOLTEN_QUARTZ.get())
                .add(ModFluids.SOURCE_MOLTEN_GLOWSTONE.get())
                .add(ModFluids.SOURCE_MOLTEN_AMETHYST.get())
                .add(ModFluids.SOURCE_MOLTEN_DIAMOND.get())
                .add(ModFluids.SOURCE_MOLTEN_OBSIDIAN.get())
                .add(ModFluids.SOURCE_MOLTEN_LAPIS.get())
        ;

        tag(ModTags.Fluids.THIN)
                .add(Fluids.WATER);
        ;
    }

}