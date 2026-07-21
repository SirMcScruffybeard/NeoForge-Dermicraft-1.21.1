package net.scruffy.dermicraft.datagen.datamaps;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.property.BiofuelProperties;
import net.scruffy.dermicraft.property.EdibleFluidProperties;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ModDataMapProvider extends DataMapProvider {
    /**
     * Create a new provider.
     *
     * @param packOutput     the output location
     * @param lookupProvider a {@linkplain CompletableFuture} supplying the registries
     */
    public ModDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {

        this.builder(ModDataMaps.BIOFUELS)
                .add(getResourceLocation(ModFluids.SOURCE_CRUDE_SLURRY),
                        new BiofuelProperties(.1f, 1f, .1f, 1), false)
                .add(getResourceLocation(ModFluids.SOURCE_CONCENTRATED_SLURRY),
                        new BiofuelProperties(.125f, .90f, .125f, 1), false)

                ;

        this.builder(ModDataMaps.EDIBLE_FLUID)
                .add(getResourceLocation(ModFluids.SOURCE_CRUDE_SLURRY),
                        new EdibleFluidProperties(250, 3, 0.1f), false)
                .add(getResourceLocation(ModFluids.SOURCE_PROTEIN_BLEND),
                        new EdibleFluidProperties(250, 5, 0.3f), false)
                .add(getResourceLocation(ModFluids.SOURCE_F_STUFF),
                        new EdibleFluidProperties(250, 4, 0.5f), false)

                ;

    }

    private static ResourceLocation getResourceLocation(Supplier<FlowingFluid> fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid.get());
    }
}
