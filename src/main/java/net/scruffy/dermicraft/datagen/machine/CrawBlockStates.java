package net.scruffy.dermicraft.datagen.machine;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.scruffy.dermicraft.block.custom.EvolutionBlockProperties;

/**
 * Builds the multipart blockstate of the Craw: no facing, no face and no light. The stacking order is the plate for the
 * Overgrowth stage, then the port ring (by tier) and the port center on all six sides. See {@link PartModels} for the
 * models and {@link MachineBlockStates} for the face machines.
 */
public class CrawBlockStates {

    private final BlockStateProvider provider;
    private final PartModels parts;

    public CrawBlockStates(BlockStateProvider provider) {
        this.provider = provider;
        this.parts = new PartModels(provider);
    }

    /**
     * @param name the item's name and model folder, for example "craw"
     */
    public void register(Block block, String name) {
        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);

        for (int stage = 0; stage <= 3; stage++) {
            builder.part().modelFile(parts.buildPlate(stage)).addModel()
                    .condition(EvolutionBlockProperties.OVERGROWTH, stage).end();
        }

        builder.part().modelFile(parts.buildCubePortCenter(name)).addModel().end();
        for (int tier = 1; tier <= 4; tier++) {
            builder.part().modelFile(parts.buildCubePorts(name, tier)).addModel()
                    .condition(EvolutionBlockProperties.TIER, tier).end();
        }

        registerItemModels(name);
    }

    /** The item: tier 1 as the base model, with overrides for higher tiers and, at tier 4, each Overgrowth stage. Vanilla
     * resolves overrides to the LAST matching entry, so they go in ascending order. */
    private void registerItemModels(String name) {
        ItemModelBuilder base = parts.buildCubeItemModel(name, 1, 0);

        for (int tier = 2; tier <= 4; tier++) {
            base.override()
                    .predicate(EvolutionBlockProperties.ITEM_TIER, tier)
                    .model(parts.buildCubeItemModel(name + "_l" + tier, tier, 0))
                    .end();
        }
        for (int stage = 1; stage <= 3; stage++) {
            base.override()
                    .predicate(EvolutionBlockProperties.ITEM_OVERGROWTH, stage)
                    .model(parts.buildCubeItemModel(name + "_l4_o" + stage, 4, stage))
                    .end();
        }
    }
}
