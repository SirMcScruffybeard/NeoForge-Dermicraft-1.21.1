package net.scruffy.dermicraft.component;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scruffy.dermicraft.main.Dermicraft;

import java.util.function.UnaryOperator;

public class ModDataComponentTypes {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Dermicraft.MOD_ID);


    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FluidData>> FLUID_DATA =
            register("contained_fluid_data", builder -> builder
                    .persistent(FluidData.CODEC)
                    .networkSynchronized(FluidData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HeldItemData>> HELD_ITEM_DATA =
            register("held_item_data", builder -> builder
                    .persistent(HeldItemData.CODEC)
                    .networkSynchronized(HeldItemData.STREAM_CODEC));

    /** D.R.I.N.K.E.R.'s "ghost buffer" -- siphon progress toward a full source block. Deliberately
     * separate from FLUID_DATA (the real buffer): nothing is banked until this reaches a full
     * 1000mB, since a fluid source block can't be partially picked up. Reuses FluidData's codecs
     * rather than defining a new record -- it's the same fluid+amount shape. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FluidData>> DRINKER_SIPHON_PROGRESS =
            register("drinker_siphon_progress", builder -> builder
                    .persistent(FluidData.CODEC)
                    .networkSynchronized(FluidData.STREAM_CODEC));

    /** Whether D.R.I.N.K.E.R. is currently held-down/siphoning -- derived server-side from vanilla's
     * use state each tick and synced so the client's animation controller can read it without
     * touching client-only classes from common code. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> DRINKER_SIPHONING =
            register("drinker_siphoning", builder -> builder
                    .persistent(com.mojang.serialization.Codec.BOOL)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DrinkerModeData>> DRINKER_MODE_DATA =
            register("drinker_mode_data", builder -> builder
                    .persistent(DrinkerModeData.CODEC)
                    .networkSynchronized(DrinkerModeData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SippingModeData>> SIPPING_MODE_DATA =
            register("sipping_mode_data", builder -> builder
                    .persistent(SippingModeData.CODEC)
                    .networkSynchronized(SippingModeData.STREAM_CODEC));




    ///////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\\\\
    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }

    private static <T>DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
            String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return DATA_COMPONENT_TYPES.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }



}
