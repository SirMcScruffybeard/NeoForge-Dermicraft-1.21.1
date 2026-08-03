package net.scruffy.dermicraft.datagen.datamaps;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.property.BiofuelProperties;
import net.scruffy.dermicraft.property.ChainProperties;
import net.scruffy.dermicraft.property.EdibleFluidProperties;

@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class ModDataMaps {
    public static final DataMapType<Fluid, BiofuelProperties> BIOFUELS =
            DataMapType.builder(
                            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "biofuels"),
                            Registries.FLUID,
                            BiofuelProperties.CODEC
                    )
                    .build();

    public static final DataMapType<Fluid, EdibleFluidProperties> EDIBLE_FLUID =
            DataMapType.builder(
                            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "edible_fluid"),
                            Registries.FLUID,
                            EdibleFluidProperties.CODEC
                    )
                    .build();

    /** Per-material Sunder chain stats, keyed on the chain Item -- see {@link ChainProperties}. */
    public static final DataMapType<Item, ChainProperties> SUNDER_CHAIN_PROPERTIES =
            DataMapType.builder(
                            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "sunder_chain_properties"),
                            Registries.ITEM,
                            ChainProperties.CODEC
                    )
                    .build();


    @SubscribeEvent
    public static void register(RegisterDataMapTypesEvent event) {
        event.register(BIOFUELS);
        event.register(EDIBLE_FLUID);
        event.register(SUNDER_CHAIN_PROPERTIES);
    }
}
