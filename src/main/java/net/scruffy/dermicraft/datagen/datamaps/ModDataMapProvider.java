package net.scruffy.dermicraft.datagen.datamaps;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.property.BiofuelProperties;
import net.scruffy.dermicraft.property.ChainProperties;
import net.scruffy.dermicraft.property.EdibleFluidProperties;

import java.util.Optional;
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

        // Iron is the BASELINE material every other chain is defined relative to, not merely one
        // entry among equals -- its numbers are the reference point, so a later material reads as
        // "+15% damage over iron" rather than needing its own from-scratch justification.
        //
        // - damage_multiplier 1.0: baseline by definition.
        // - bleed_chance 0.50 / decap_chance 0.20: deliberately NOT equal. Standard-hit Bleed was
        //   specified as a "high chance," standard-hit decapitation as ~20-25% -- two different
        //   bars, so equal values would collapse a real distinction. These carry the WHOLE chance
        //   rather than a bonus over some Sunder-side base, which is what makes the decided
        //   "broken chain = flat 0%" behavior fall out for free instead of needing an override.
        // - durability 250: matches IRON_SUNDER_CHAIN's own standalone-item durability, itself
        //   anchored to vanilla's iron-tool durability.
        // - tint #D8D8D8: Ferrous Blend's own tint color (see ModFluidTypes). Tint is deliberately
        //   NOT part of the iron-is-baseline idea -- it's independent per chain, each taking the
        //   tint of its equivalent fluid material, so there's no "base tint" other chains deviate
        //   from. Adjustable toward a more iron-like shade later.
        // - no status effect: signature traits are reserved for special-case materials.
        //
        // Numbers are first-pass and untuned -- see the design notes' open questions.
        this.builder(ModDataMaps.SUNDER_CHAIN_PROPERTIES)
                .add(getResourceLocation(ModItems.IRON_SUNDER_CHAIN),
                        new ChainProperties(1.0f, 0.50f, 0.20f, 250,
                                TextColor.fromRgb(0xD8D8D8), Optional.empty()), false)

                ;

    }

    private static ResourceLocation getResourceLocation(Supplier<FlowingFluid> fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid.get());
    }

    private static ResourceLocation getResourceLocation(net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.Item> item) {
        return BuiltInRegistries.ITEM.getKey(item.get());
    }
}
