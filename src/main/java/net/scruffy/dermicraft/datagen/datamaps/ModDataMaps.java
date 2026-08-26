package net.scruffy.dermicraft.datagen.datamaps;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
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
import net.scruffy.dermicraft.property.EvolutionModuleProperties;
import net.scruffy.dermicraft.property.SafetyModuleProperties;
import net.scruffy.dermicraft.property.ShatterHeadProperties;
import net.scruffy.dermicraft.property.WorkSpeedModuleProperties;

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

    /** Which hazard(s) a Safety Module grants while equipped -- see {@link SafetyModuleProperties}.
     * Kind (is this a Safety Module at all) lives on {@code ModTags.Items.MODULE_SAFETY}; this data
     * map is specifically WHICH hazard(s) a given one grants. */
    public static final DataMapType<Item, SafetyModuleProperties> SAFETY_MODULE_PROPERTIES =
            DataMapType.builder(
                            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "safety_module_properties"),
                            Registries.ITEM,
                            SafetyModuleProperties.CODEC
                    )
                    .build();

    /** Per-item Evolution Module data -- see {@link EvolutionModuleProperties}. Kind (is this an
     * Evolution Module at all) lives on {@code ModTags.Items.MODULES}; a future sub-tag can narrow
     * which machine's Module slot accepts it if that turns out to matter. This map is specifically
     * WHAT a given one does. */
    public static final DataMapType<Item, EvolutionModuleProperties> EVOLUTION_MODULE_PROPERTIES =
            DataMapType.builder(
                            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "evolution_module_properties"),
                            Registries.ITEM,
                            EvolutionModuleProperties.CODEC
                    )
                    .build();

    /** Which mobs can be mechanically decapitated -- scoped for now to mobs that already have a
     * real vanilla head/skull item (Skeleton, Wither Skeleton, Zombie, Creeper, Piglin), per the
     * design decision not to author brand-new head items for every mob type yet. Bare Item value
     * (no wrapper record) since "does this mob have a head to drop" is the whole question -- no
     * per-mob nuance needed beyond which item that head is. */
    public static final DataMapType<EntityType<?>, Item> DECAPITATION_HEADS =
            DataMapType.builder(
                            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "decapitation_heads"),
                            Registries.ENTITY_TYPE,
                            BuiltInRegistries.ITEM.byNameCodec()
                    )
                    .build();

    /** Per-material Shatter head stats, keyed on the head Item -- see {@link ShatterHeadProperties}. */
    public static final DataMapType<Item, ShatterHeadProperties> SHATTER_HEAD_PROPERTIES =
            DataMapType.builder(
                            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "shatter_head_properties"),
                            Registries.ITEM,
                            ShatterHeadProperties.CODEC
                    )
                    .build();

    /** Per-item speed multiplier for a Work Speed Module -- see {@link WorkSpeedModuleProperties}.
     * Kind (is this a Work Speed Module at all) lives on {@code ModTags.Items.MODULE_WORK_SPEED};
     * this data map is specifically WHAT multiplier a given one grants. */
    public static final DataMapType<Item, WorkSpeedModuleProperties> WORK_SPEED_MODULE_PROPERTIES =
            DataMapType.builder(
                            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "work_speed_module_properties"),
                            Registries.ITEM,
                            WorkSpeedModuleProperties.CODEC
                    )
                    .build();

    @SubscribeEvent
    public static void register(RegisterDataMapTypesEvent event) {
        event.register(BIOFUELS);
        event.register(EDIBLE_FLUID);
        event.register(SUNDER_CHAIN_PROPERTIES);
        event.register(SAFETY_MODULE_PROPERTIES);
        event.register(EVOLUTION_MODULE_PROPERTIES);
        event.register(DECAPITATION_HEADS);
        event.register(SHATTER_HEAD_PROPERTIES);
        event.register(WORK_SPEED_MODULE_PROPERTIES);
    }
}
