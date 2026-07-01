package net.scruffy.dermicraft.datagen.datamaps;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.property.BiofuelProperties;

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
                        new BiofuelProperties(.1f, 1f, .1f), false)

                ;

    }

    private static ResourceLocation getResourceLocation(Supplier<FlowingFluid> fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid.get());
    }
}
