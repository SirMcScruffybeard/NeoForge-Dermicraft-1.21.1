package net.scruffy.dermicraft.datagen.datamaps;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.property.BiofuelProperties;

@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class ModDataMaps {
    public static final DataMapType<Fluid, BiofuelProperties> BIOFUELS =
            DataMapType.builder(
                            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "biofuels"),
                            Registries.FLUID,
                            BiofuelProperties.CODEC
                    )
                    .build();




    @SubscribeEvent
    public static void register(RegisterDataMapTypesEvent event) {
        event.register(BIOFUELS);
    }
}
