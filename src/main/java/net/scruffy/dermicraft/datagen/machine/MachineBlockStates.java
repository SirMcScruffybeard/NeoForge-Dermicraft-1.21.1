package net.scruffy.dermicraft.datagen.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.scruffy.dermicraft.block.custom.EvolutionBlockProperties;
import net.scruffy.dermicraft.block.custom.MachineStatus;

import java.util.Arrays;

/**
 * Builds a face machine's multipart blockstate from the layer models in {@link PartModels}. The stacking order is:
 * the plate for the machine's Overgrowth stage, then for each of the four facings the light mount (by tier), the
 * machine's own features (by tier, still or moving by status), the light (by status), the port center and the port
 * rings (by tier). The plate has no facing, since every side of it is the same texture. Later parts draw on top of
 * earlier ones.
 */
public class MachineBlockStates {

    /** Statuses in which the machine is doing its work, which is when a moving feature shows. */
    private static final MachineStatus[] WORKING = Arrays.stream(MachineStatus.values())
            .filter(MachineStatus::isWorking).toArray(MachineStatus[]::new);
    private static final MachineStatus[] NOT_WORKING = Arrays.stream(MachineStatus.values())
            .filter(status -> !status.isWorking()).toArray(MachineStatus[]::new);

    private final BlockStateProvider provider;
    private final PartModels parts;

    public MachineBlockStates(BlockStateProvider provider) {
        this.provider = provider;
        this.parts = new PartModels(provider);
    }

    /**
     * @param machine the model folder name, for example "masticator"
     */
    public void register(Block block, String machine, MachineFaceLayout layout, Property<Direction> facing) {
        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);

        // The plate is one full-block model per Overgrowth stage, shared by every machine.
        for (int stage = 0; stage <= 3; stage++) {
            builder.part().modelFile(parts.buildPlate(stage)).addModel()
                    .condition(EvolutionBlockProperties.OVERGROWTH, stage).end();
        }

        // Build each tiered layer once, then reuse it for all four facings.
        ModelFile[] mounts = new ModelFile[5];                  // indexed by tier, 1 to 4
        ModelFile[] ports = new ModelFile[5];
        for (int tier = 1; tier <= 4; tier++) {
            mounts[tier] = parts.buildLightMount(machine, layout, tier);
            ports[tier] = parts.buildPorts(machine, layout, tier);
        }
        ModelFile portCenter = parts.buildPortCenter(machine, layout);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            int rotation = getRotationY(direction);

            addTierParts(builder, mounts, rotation, facing, direction);
            addFeatureParts(builder, machine, layout, rotation, facing, direction);
            addLightParts(builder, machine, layout, rotation, facing, direction);

            builder.part().modelFile(portCenter).rotationY(rotation).addModel()
                    .condition(facing, direction).end();
            addTierParts(builder, ports, rotation, facing, direction);
        }

        registerItemModels(machine, layout);
    }

    /**
     * The machine's item: tier 1 as the base model, with overrides that swap in the model for a higher tier and, at
     * tier 4, for each Overgrowth stage. Vanilla resolves overrides to the LAST matching entry, so they go in
     * ascending order. The item's name is the machine's, which is also its model folder name.
     */
    private void registerItemModels(String machine, MachineFaceLayout layout) {
        ItemModelBuilder base = parts.buildItemModel(machine, layout, 1, 0);

        for (int tier = 2; tier <= 4; tier++) {
            base.override()
                    .predicate(EvolutionBlockProperties.ITEM_TIER, tier)
                    .model(parts.buildItemModel(machine + "_l" + tier, layout, tier, 0))
                    .end();
        }
        for (int stage = 1; stage <= 3; stage++) {
            base.override()
                    .predicate(EvolutionBlockProperties.ITEM_OVERGROWTH, stage)
                    .model(parts.buildItemModel(machine + "_l4_o" + stage, layout, 4, stage))
                    .end();
        }
    }

    /** The blockstate Y rotation that turns a model authored facing north to face {@code direction}. */
    private static int getRotationY(Direction direction) {
        return ((int) direction.toYRot() + 180) % 360;
    }

    /** One part per tier (1 to 4) for a facing, each using that tier's model. */
    private static void addTierParts(MultiPartBlockStateBuilder builder, ModelFile[] models, int rotation,
                                     Property<Direction> facing, Direction direction) {
        for (int tier = 1; tier <= 4; tier++) {
            builder.part().modelFile(models[tier]).rotationY(rotation).addModel()
                    .condition(facing, direction)
                    .condition(EvolutionBlockProperties.TIER, tier).end();
        }
    }

    /** The light in each color, shown for the statuses that map to it. */
    private void addLightParts(MultiPartBlockStateBuilder builder, String machine, MachineFaceLayout layout,
                               int rotation, Property<Direction> facing, Direction direction) {
        for (PartModels.Lamp lamp : PartModels.Lamp.values()) {
            MachineStatus[] statuses = Arrays.stream(MachineStatus.values())
                    .filter(status -> PartModels.Lamp.forStatus(status) == lamp)
                    .toArray(MachineStatus[]::new);
            builder.part().modelFile(parts.buildLight(machine, layout, lamp)).rotationY(rotation).addModel()
                    .condition(facing, direction)
                    .condition(MachineStatus.PROPERTY, statuses).end();
        }
    }

    /**
     * The machine's own parts on its face. A feature with a moving texture shows its still texture while the machine
     * is not working (broken down included) and its moving texture while it is; one without shows the still texture
     * always.
     */
    private void addFeatureParts(MultiPartBlockStateBuilder builder, String machine, MachineFaceLayout layout,
                                 int rotation, Property<Direction> facing, Direction direction) {
        for (MachineFaceLayout.Feature feature : layout.features()) {
            for (int tier = 1; tier <= 4; tier++) {
                var still = builder.part()
                        .modelFile(parts.buildFeature(machine, layout, feature, tier, false))
                        .rotationY(rotation).addModel()
                        .condition(facing, direction)
                        .condition(EvolutionBlockProperties.TIER, tier);
                if (feature.hasMovingTexture()) still.condition(MachineStatus.PROPERTY, NOT_WORKING);
                still.end();

                if (feature.hasMovingTexture()) {
                    builder.part()
                            .modelFile(parts.buildFeature(machine, layout, feature, tier, true))
                            .rotationY(rotation).addModel()
                            .condition(facing, direction)
                            .condition(EvolutionBlockProperties.TIER, tier)
                            .condition(MachineStatus.PROPERTY, WORKING).end();
                }
            }
        }
    }
}
