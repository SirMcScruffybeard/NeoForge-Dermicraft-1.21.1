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

    /** Backing store for {@code IHaveItemData}'s bulk item handlers -- see {@link BulkItemData}
     * for why this exists instead of vanilla's {@code DataComponents.CONTAINER}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BulkItemData>> BULK_ITEM_DATA =
            register("bulk_item_data", builder -> builder
                    .persistent(BulkItemData.CODEC)
                    .networkSynchronized(BulkItemData.STREAM_CODEC));

    /** Eater's Gadget Module loadout -- see dermicraft-gadget-notes.md -> Gadget upgrade points ->
     * Modules direction note. Reuses {@link BulkItemData}'s shape (a fixed-size list of item slots)
     * rather than a dedicated record -- same "shared shape, separate registration per consumer"
     * pattern as {@link #EATER_MODE_DATA} above -- registered separately from {@link #BULK_ITEM_DATA}
     * so a gadget's Module loadout is never confused with its own item buffer (both live on the same
     * Eater stack, as two independent components). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BulkItemData>> MODULE_DATA =
            register("module_data", builder -> builder
                    .persistent(BulkItemData.CODEC)
                    .networkSynchronized(BulkItemData.STREAM_CODEC));

    /** Eater's mode state -- same shape as D.R.I.N.K.E.R.'s, registered separately so a mode set on
     * one item is never confused with the other's. Reuses {@link DrinkerModeData}'s codecs rather
     * than a duplicate record; the Storage/Transfer/Disposal cycle-step shape is identical. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DrinkerModeData>> EATER_MODE_DATA =
            register("eater_mode_data", builder -> builder
                    .persistent(DrinkerModeData.CODEC)
                    .networkSynchronized(DrinkerModeData.STREAM_CODEC));

    /** Whether Eater is currently held-down/vacuuming -- same role as {@link #DRINKER_SIPHONING},
     * derived server-side each tick and synced for the client's animation controller. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> EATER_VACUUMING =
            register("eater_vacuuming", builder -> builder
                    .persistent(com.mojang.serialization.Codec.BOOL)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL));

    /** Sunder's basic rev/dig-in state machine -- see {@link SunderModeData}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SunderModeData>> SUNDER_MODE_DATA =
            register("sunder_mode_data", builder -> builder
                    .persistent(SunderModeData.CODEC)
                    .networkSynchronized(SunderModeData.STREAM_CODEC));

    /** The chain currently mounted on Sunder, held as a real (nested) {@link ItemStack} --
     * reuses {@link HeldItemData}'s shape/codecs (same role as I.D.E.P.'s use of it) rather than a
     * dedicated record, registered separately from {@link #HELD_ITEM_DATA} so a chain is never
     * confused with I.D.E.P.'s held item (same "shared shape, separate registration per consumer"
     * pattern as EATER_MODE_DATA reusing DrinkerModeData's codecs).
     *
     * <p>Storing the mounted chain as a real ItemStack (vanilla damage value and all) rather than a
     * bespoke int stat is deliberate: nothing about vanilla's break-on-zero-durability behavior
     * fires just because a nested ItemStack's damage value happens to reach its max -- that only
     * happens through code paths that actively call {@code hurtAndBreak} on a stack sitting in a
     * real slot. A chain stored here is inert data Sunder's own code fully controls, so "reaches 0
     * without destroying Sunder" falls out for free rather than needing a separate int-based stat
     * converted to/from vanilla durability at the Scrench GUI boundary (see the Chain durability
     * design notes in {@code dermicraft-gadget-notes.md} -- this refines, rather than contradicts,
     * that "dual representation" language: it's the same ItemStack representation throughout,
     * relocated between a real inventory slot and this nested data value, not two different ones). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HeldItemData>> SUNDER_MOUNTED_CHAIN =
            register("sunder_mounted_chain", builder -> builder
                    .persistent(HeldItemData.CODEC)
                    .networkSynchronized(HeldItemData.STREAM_CODEC));

    /** Shatter's charge/release state -- see {@link ShatterModeData}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ShatterModeData>> SHATTER_MODE_DATA =
            register("shatter_mode_data", builder -> builder
                    .persistent(ShatterModeData.CODEC)
                    .networkSynchronized(ShatterModeData.STREAM_CODEC));

    /** The head currently mounted on Shatter -- same real-nested-ItemStack shape as
     * {@link #SUNDER_MOUNTED_CHAIN} and the same reasoning for why (see that field's javadoc);
     * registered separately so a mounted head is never confused with Sunder's mounted chain. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HeldItemData>> SHATTER_MOUNTED_HEAD =
            register("shatter_mounted_head", builder -> builder
                    .persistent(HeldItemData.CODEC)
                    .networkSynchronized(HeldItemData.STREAM_CODEC));




    ///////////////////////Helper Methods\\\\\\\\\\\\\\\\\\\\\\\
    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }

    private static <T>DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
            String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return DATA_COMPONENT_TYPES.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }



}
