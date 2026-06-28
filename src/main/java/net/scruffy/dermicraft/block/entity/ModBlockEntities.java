package net.scruffy.dermicraft.block.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.custom.*;
import net.scruffy.dermicraft.main.Dermicraft;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Dermicraft.MOD_ID);
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }

    public static final Supplier<BlockEntityType<MarredTumorBlockEntity>> MARRED_TUMOR_BE =
            BLOCK_ENTITIES.register("marred_tumor_be", () -> BlockEntityType.Builder.of(
                    MarredTumorBlockEntity::new, ModBlocks.MARRED_TUMOR.get()).build(null));

    public static final Supplier<BlockEntityType<StitchedTumorBlockEntity>> STITCHED_TUMOR_BE =
            BLOCK_ENTITIES.register("stitched_tumor_be", () -> BlockEntityType.Builder.of(
                    StitchedTumorBlockEntity::new, ModBlocks.STITCHED_TUMOR.get()).build(null));


    public static final Supplier<BlockEntityType<DroolingCauldronBlockEntity>> DROOLING_CAULDRON_BE =
            BLOCK_ENTITIES.register("drooling_cauldron_be", () -> BlockEntityType.Builder.of(
                    DroolingCauldronBlockEntity::new, ModBlocks.DROOLING_CAULDRON.get()).build(null));

    public static final Supplier<BlockEntityType<MasticatorBlockEntity>> MASTICATOR_BE =
            BLOCK_ENTITIES.register("masticator_be", () -> BlockEntityType.Builder.of(
                    MasticatorBlockEntity::new, ModBlocks.MASTICATOR.get()).build(null));

    public static final Supplier<BlockEntityType<SkinTankBlockEntity>> SKIN_TANK_BE =
            BLOCK_ENTITIES.register("skin_tank_be", () -> BlockEntityType.Builder.of(
                    SkinTankBlockEntity::new, ModBlocks.SKIN_TANK.get()).build(null));

    public static final Supplier<BlockEntityType<BeakerBlockEntity>> BEAKER_BE =
            BLOCK_ENTITIES.register("beaker_be", () -> BlockEntityType.Builder.of(
                    BeakerBlockEntity::new, ModBlocks.BEAKER.get()).build(null));

}
