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

    public static final Supplier<BlockEntityType<EffluentcerBlockEntity>> EFFLUENTCER_BE =
            BLOCK_ENTITIES.register("effluentcer_be", () -> BlockEntityType.Builder.of(
                    EffluentcerBlockEntity::new, ModBlocks.EFFLUENTCER.get()).build(null));

    public static final Supplier<BlockEntityType<BeakerBlockEntity>> BEAKER_BE =
            BLOCK_ENTITIES.register("beaker_be", () -> BlockEntityType.Builder.of(
                    BeakerBlockEntity::new, ModBlocks.BEAKER.get()).build(null));

    public static final Supplier<BlockEntityType<MetastasizerBlockEntity>> METASTASIZER_BE =
            BLOCK_ENTITIES.register("metastasizer_be", () -> BlockEntityType.Builder.of(
                    MetastasizerBlockEntity::new, ModBlocks.METASTASIZER.get()).build(null));

    public static final Supplier<BlockEntityType<CrawBlockEntity>> CRAW_BE =
            BLOCK_ENTITIES.register("craw_be", () -> BlockEntityType.Builder.of(
                    CrawBlockEntity::new, ModBlocks.CRAW.get()).build(null));

    public static final Supplier<BlockEntityType<NodeBlockEntity>> INNARDS_NODE_BE =
            BLOCK_ENTITIES.register("innards_node_be", () -> BlockEntityType.Builder.of(
                    NodeBlockEntity::new, ModBlocks.INNARDS_NODE.get()).build(null));

    public static final Supplier<BlockEntityType<GateControllerBlockEntity>> INNARDS_GATE_CONTROLLER_BE =
            BLOCK_ENTITIES.register("innards_gate_controller_be", () -> BlockEntityType.Builder.of(
                    GateControllerBlockEntity::new, ModBlocks.INNARDS_GATE_CONTROLLER.get()).build(null));

    public static final Supplier<BlockEntityType<GateBufferBlockEntity>> INNARDS_GATE_BUFFER_BE =
            BLOCK_ENTITIES.register("innards_gate_buffer_be", () -> BlockEntityType.Builder.of(
                    GateBufferBlockEntity::new, ModBlocks.INNARDS_GATE_BUFFER.get()).build(null));

}
