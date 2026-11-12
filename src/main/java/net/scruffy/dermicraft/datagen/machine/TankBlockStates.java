package net.scruffy.dermicraft.datagen.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.scruffy.dermicraft.block.custom.EvolutionBlockProperties;

/**
 * Builds the multipart blockstate of a tank (the Skin Tank): no facing and no light. The stacking order is the plate for
 * the Overgrowth stage, the interior backing, then on each of the four sides the tier accent and the glass, and on the
 * top and bottom the port ring (by tier) and center. The side parts are authored once, facing north, and placed four
 * times by rotation. See {@link PartModels} for the models and {@link MachineBlockStates} for the face machines.
 */
public class TankBlockStates {

    private final BlockStateProvider provider;
    private final PartModels parts;

    public TankBlockStates(BlockStateProvider provider) {
        this.provider = provider;
        this.parts = new PartModels(provider);
    }

    /**
     * @param name the item's name, for example "skin_tank"
     */
    public void register(Block block, String name) {
        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);

        for (int stage = 0; stage <= 3; stage++) {
            builder.part().modelFile(parts.buildTankPlate(stage)).addModel()
                    .condition(EvolutionBlockProperties.OVERGROWTH, stage).end();
        }

        // The interior follows the stage like the outer plate; its glass is the same at every stage.
        for (int stage = 0; stage <= 3; stage++) {
            builder.part().modelFile(parts.buildTankInterior(stage)).addModel()
                    .condition(EvolutionBlockProperties.OVERGROWTH, stage).end();
        }
        builder.part().modelFile(parts.buildTankInteriorGlass()).addModel().end();

        for (Direction side : Direction.Plane.HORIZONTAL) {
            int rotation = getRotationY(side);
            for (int tier = 1; tier <= 4; tier++) {
                builder.part().modelFile(parts.buildTankAccent(tier)).rotationY(rotation).addModel()
                        .condition(EvolutionBlockProperties.TIER, tier).end();
            }
            builder.part().modelFile(parts.buildTankGlass()).rotationY(rotation).addModel().end();
        }

        for (int tier = 1; tier <= 4; tier++) {
            builder.part().modelFile(parts.buildTankPorts(tier)).addModel()
                    .condition(EvolutionBlockProperties.TIER, tier).end();
        }
        builder.part().modelFile(parts.buildTankPortCenter()).addModel().end();

        registerItemModels(name);
    }

    /** The item: tier 1 as the base model, with overrides for higher tiers and, at tier 4, each Overgrowth stage. Vanilla
     * resolves overrides to the LAST matching entry, so they go in ascending order. */
    private void registerItemModels(String name) {
        ItemModelBuilder base = parts.buildTankItemModel(name, 1, 0);

        for (int tier = 2; tier <= 4; tier++) {
            base.override()
                    .predicate(EvolutionBlockProperties.ITEM_TIER, tier)
                    .model(parts.buildTankItemModel(name + "_l" + tier, tier, 0))
                    .end();
        }
        for (int stage = 1; stage <= 3; stage++) {
            base.override()
                    .predicate(EvolutionBlockProperties.ITEM_OVERGROWTH, stage)
                    .model(parts.buildTankItemModel(name + "_l4_o" + stage, 4, stage))
                    .end();
        }
    }

    /** The blockstate Y rotation that turns a model authored facing north to face {@code direction}. */
    private static int getRotationY(Direction direction) {
        return ((int) direction.toYRot() + 180) % 360;
    }
}