package net.scruffy.dermicraft.datagen.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import org.jetbrains.annotations.Nullable;
import net.scruffy.dermicraft.block.custom.EvolutionBlockProperties;

import java.util.List;

import static net.scruffy.dermicraft.datagen.machine.Dirs.*;

/**
 * Builds a farm machine's multipart blockstate and item model in the style of the simple machines. The stacking order is:
 * the plate for the machine's Overgrowth stage (the same plates the other machines use), the machine's own faces over it
 * (front, back, left, right and top, each a 16 x 16 texture that may have transparent parts), the light mount (by tier)
 * and the light (off, or lit and emissive while the machine runs) on the front, and the port ring (by tier) and center on
 * the bottom. Every part but the plate is authored with the front on the north face and turned to the machine's facing by
 * the blockstate, which uses the six-way facing of the farm machines (up and down tip the front to look up or down).
 *
 * <p>The machine's own textures live in {@code textures/block/farm_machine/<machine>/} as {@code <machine>_front},
 * {@code _back}, {@code _left}, {@code _right} and {@code _top}. Left and right are as seen looking at the front, so on the
 * north face the left texture is on the east side and the right on the west side.
 */
public class FarmMachineModels {

    /** The front: the light mount centered across the top with its top edge 4 pixels down (x 5 to 10, y 6 to 11). */
    public static final MachineFaceLayout FRONT = new MachineFaceLayout(Direction.NORTH, 5, 6, List.of());

    /** Mr. Shepherd's front: the mount 4 pixels down and 2 in from the right edge (x 8 to 13, y 6 to 11). */
    public static final MachineFaceLayout SHEPHERD_FRONT = new MachineFaceLayout(Direction.NORTH, 8, 6, List.of());

    /** Mr. Logger's front: the mount centered both ways (x 5 to 10, y 5 to 10). */
    public static final MachineFaceLayout LOGGER_FRONT = new MachineFaceLayout(Direction.NORTH, 5, 5, List.of());

    /** Mr. Hunter's front: the mount 4 pixels down and 1 in from the right edge (x 9 to 14, y 6 to 11). */
    public static final MachineFaceLayout HUNTER_FRONT = new MachineFaceLayout(Direction.NORTH, 9, 6, List.of());

    /** A pixel of the machine's own front texture that glows while the machine is lit, as the light does. Position in pixels, as
     * the player sees the face (origin lower left). */
    public record GlowPixel(int x, int y) {}

    /** Mr. Hunter's glow pixel: 1 pixel up from the bottom and 5 in from the right edge (x 10, y 1). */
    public static final GlowPixel HUNTER_GLOW = new GlowPixel(10, 1);

    private static final String FARM_MACHINE_DIR = BLOCK_DIR + "/farm_machine";
    private static final String PLATE = SIMPLE_MACHINE_DIR + "/plate/l_plate";
    private static final String LIGHT_OFF = SIMPLE_MACHINE_DIR + LIGHT_DIR + "/light_off";
    private static final String LIGHT_ON = SIMPLE_MACHINE_DIR + LIGHT_DIR + "/light_on";
    private static final String PORT_CENTER = SIMPLE_MACHINE_DIR + PORT_DIR + "/port_center";

    /** How far the machine's faces sit off the plate, half a decal layer, so the light and ports (a full layer) draw over them. */
    private static final float FACE_OFFSET = 0.025f;

    private final BlockStateProvider provider;
    private final PartModels parts;

    public FarmMachineModels(BlockStateProvider provider) {
        this.provider = provider;
        this.parts = new PartModels(provider);
    }

    /**
     * Registers the machine's blockstate and item model.
     *
     * @param machine the model folder name and the item's name, for example "mr_farmer"
     * @param front   where the light mount sits on the front
     */
    public void register(Block block, String machine, MachineFaceLayout front, DirectionProperty facing, BooleanProperty lit) {
        register(block, machine, front, null, facing, lit);
    }

    /** As {@link #register(Block, String, MachineFaceLayout, DirectionProperty, BooleanProperty)}, with a pixel of the front that
     * glows while the machine is lit ({@code glow} null for none). */
    public void register(Block block, String machine, MachineFaceLayout front, @Nullable GlowPixel glow,
                         DirectionProperty facing, BooleanProperty lit) {
        register(block, machine, front, glow, null, facing, lit);
    }

    /**
     * As above, with a tier accent: a 16 x 16 texture per tier laid over the whole front face, just above the machine's own faces
     * and below the light mount, named {@code <machine>_<accent>_l<tier>} in the machine's texture folder (Mr. Hunter's is
     * {@code scar}: {@code mr_hunter_scar_l1} to {@code l4}). {@code accent} null for none.
     */
    public void register(Block block, String machine, MachineFaceLayout front, @Nullable GlowPixel glow, @Nullable String accent,
                         DirectionProperty facing, BooleanProperty lit) {
        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);

        // The plate: one full-block model per Overgrowth stage, shared by every machine.
        for (int stage = 0; stage <= 3; stage++) {
            builder.part().modelFile(parts.buildPlate(stage)).addModel()
                    .condition(EvolutionBlockProperties.OVERGROWTH, stage).end();
        }

        BlockModelBuilder faces = buildFaces(machine);
        BlockModelBuilder lightOff = parts.buildLight(machine, front, PartModels.Lamp.OFF);
        BlockModelBuilder lightOn = parts.buildLight(machine, front, PartModels.Lamp.ON);
        BlockModelBuilder[] accents = new BlockModelBuilder[5]; // indexed by tier, 1 to 4; null entries without an accent
        BlockModelBuilder[] mounts = new BlockModelBuilder[5];  // indexed by tier, 1 to 4
        BlockModelBuilder[] rings = new BlockModelBuilder[5];
        for (int tier = 1; tier <= 4; tier++) {
            if (accent != null) accents[tier] = buildAccent(machine, accent, tier);
            mounts[tier] = parts.buildLightMount(machine, front, tier);
            rings[tier] = buildRing(machine, tier);
        }
        BlockModelBuilder center = buildCenter(machine);
        BlockModelBuilder glowModel = glow == null ? null
                : parts.buildGlowPixel(machine, front, glow.x(), glow.y(), FARM_MACHINE_DIR + "/" + machine + "/" + machine + "_front");

        for (Direction direction : Direction.values()) {
            int rotX = getRotationX(direction);
            int rotY = getRotationY(direction);

            builder.part().modelFile(faces).rotationX(rotX).rotationY(rotY).addModel()
                    .condition(facing, direction).end();
            for (int tier = 1; tier <= 4 && accent != null; tier++) {
                builder.part().modelFile(accents[tier]).rotationX(rotX).rotationY(rotY).addModel()
                        .condition(facing, direction).condition(EvolutionBlockProperties.TIER, tier).end();
            }
            for (int tier = 1; tier <= 4; tier++) {
                builder.part().modelFile(mounts[tier]).rotationX(rotX).rotationY(rotY).addModel()
                        .condition(facing, direction).condition(EvolutionBlockProperties.TIER, tier).end();
            }
            builder.part().modelFile(lightOff).rotationX(rotX).rotationY(rotY).addModel()
                    .condition(facing, direction).condition(lit, false).end();
            builder.part().modelFile(lightOn).rotationX(rotX).rotationY(rotY).addModel()
                    .condition(facing, direction).condition(lit, true).end();
            if (glowModel != null) {
                builder.part().modelFile(glowModel).rotationX(rotX).rotationY(rotY).addModel()
                        .condition(facing, direction).condition(lit, true).end();
            }
            builder.part().modelFile(center).rotationX(rotX).rotationY(rotY).addModel()
                    .condition(facing, direction).end();
            for (int tier = 1; tier <= 4; tier++) {
                builder.part().modelFile(rings[tier]).rotationX(rotX).rotationY(rotY).addModel()
                        .condition(facing, direction).condition(EvolutionBlockProperties.TIER, tier).end();
            }
        }

        registerItemModels(machine, front, accent);
    }

    /** How far the tier accent sits off the front, between the faces (0.025) and the light mount and port ring (0.05). */
    private static final float ACCENT_OFFSET = 0.0375f;

    /** The tier accent for one tier: {@code block/<machine>/<accent>_l<tier>}, a 16 x 16 quad over the front face. */
    private BlockModelBuilder buildAccent(String machine, String accent, int tier) {
        BlockModelBuilder model = provider.models().getBuilder(MODEL_DIR + machine + "/" + accent + "_l" + tier)
                .renderType("cutout")
                .texture("decal", provider.modLoc(accentTexture(machine, accent, tier)));
        addAccentElement(model, "decal");
        return model;
    }

    private static String accentTexture(String machine, String accent, int tier) {
        return FARM_MACHINE_DIR + "/" + machine + "/" + machine + "_" + accent + "_l" + tier;
    }

    private static <T extends ModelBuilder<T>> void addAccentElement(T model, String textureKey) {
        model.element().from(0, 0, -ACCENT_OFFSET).to(16, 16, -ACCENT_OFFSET)
                .face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#" + textureKey).end().end();
    }

    /** The blockstate X rotation that turns a model authored facing north to look up or down; 0 for the sideways ones. */
    private static int getRotationX(Direction direction) {
        return switch (direction) {
            case UP -> 270;
            case DOWN -> 90;
            default -> 0;
        };
    }

    /** The blockstate Y rotation that turns a model authored facing north to face a sideways direction. */
    private static int getRotationY(Direction direction) {
        return switch (direction) {
            case SOUTH -> 180;
            case EAST -> 90;
            case WEST -> 270;
            default -> 0;
        };
    }

    /** The machine's own faces as a slightly oversize cube over the plate: {@code block/<machine>/faces}. */
    private BlockModelBuilder buildFaces(String machine) {
        BlockModelBuilder model = provider.models().getBuilder(MODEL_DIR + machine + "/faces")
                .parent(provider.models().getExistingFile(provider.mcLoc("block/block")))
                .renderType("cutout")
                .texture("particle", provider.modLoc(PLATE));
        addFacesTextures(model, machine);
        addFaceElements(model);
        return model;
    }

    private <T extends ModelBuilder<T>> void addFacesTextures(T model, String machine) {
        String textures = FARM_MACHINE_DIR + "/" + machine + "/" + machine;
        model.texture("front", provider.modLoc(textures + "_front"))
                .texture("back", provider.modLoc(textures + "_back"))
                .texture("left", provider.modLoc(textures + "_left"))
                .texture("right", provider.modLoc(textures + "_right"))
                .texture("top", provider.modLoc(textures + "_top"));
    }

    /** Seen from outside the north face the player's left is east, so the left texture is on the east side and the right on
     * the west. The bottom has none: it carries the ports. */
    private static <T extends ModelBuilder<T>> void addFaceElements(T model) {
        var faces = model.element().from(-FACE_OFFSET, -FACE_OFFSET, -FACE_OFFSET)
                .to(16 + FACE_OFFSET, 16 + FACE_OFFSET, 16 + FACE_OFFSET);
        faces.face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#front").cullface(Direction.NORTH).end();
        faces.face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#back").cullface(Direction.SOUTH).end();
        faces.face(Direction.EAST).uvs(0, 0, 16, 16).texture("#left").cullface(Direction.EAST).end();
        faces.face(Direction.WEST).uvs(0, 0, 16, 16).texture("#right").cullface(Direction.WEST).end();
        faces.face(Direction.UP).uvs(0, 0, 16, 16).texture("#top").cullface(Direction.UP).end();
        faces.end();
    }

    /** The port ring for one tier on the bottom only: {@code block/<machine>/ports_l<tier>}. */
    private BlockModelBuilder buildRing(String machine, int tier) {
        BlockModelBuilder model = provider.models().getBuilder(MODEL_DIR + machine + "/ports_l" + tier)
                .parent(provider.models().getExistingFile(provider.mcLoc("block/block")))
                .renderType("cutout")
                .texture("particle", provider.modLoc(PLATE))
                .texture("ring", provider.modLoc(SIMPLE_MACHINE_DIR + PORT_DIR + "/l" + tier + "_port"));
        parts.addPortSides(model, "ring", side -> side == Direction.DOWN);
        return model;
    }

    /** The center that fills the ring's hole, the same at every tier: {@code block/<machine>/ports_center}. */
    private BlockModelBuilder buildCenter(String machine) {
        BlockModelBuilder model = provider.models().getBuilder(MODEL_DIR + machine + "/ports_center")
                .parent(provider.models().getExistingFile(provider.mcLoc("block/block")))
                .renderType("cutout")
                .texture("particle", provider.modLoc(PLATE))
                .texture("center", provider.modLoc(PORT_CENTER));
        parts.addPortSides(model, "center", side -> side == Direction.DOWN);
        return model;
    }

    /**
     * The machine's item: tier 1 as the base model, with overrides that swap in the model for a higher tier and, at tier 4,
     * for each Overgrowth stage. Vanilla resolves overrides to the LAST matching entry, so they go in ascending order.
     */
    private void registerItemModels(String machine, MachineFaceLayout front, @Nullable String accent) {
        ItemModelBuilder base = buildItemModel(machine, machine, front, accent, 1, 0);

        for (int tier = 2; tier <= 4; tier++) {
            base.override()
                    .predicate(EvolutionBlockProperties.ITEM_TIER, tier)
                    .model(buildItemModel(machine + "_l" + tier, machine, front, accent, tier, 0))
                    .end();
        }
        for (int stage = 1; stage <= 3; stage++) {
            base.override()
                    .predicate(EvolutionBlockProperties.ITEM_OVERGROWTH, stage)
                    .model(buildItemModel(machine + "_l4_o" + stage, machine, front, accent, 4, stage))
                    .end();
        }
    }

    /** The inventory model at a tier and Overgrowth stage: the plate, the machine's faces, the mount, the unlit light and the
     * bottom ports, all in one model, since an item can only have the one. */
    private ItemModelBuilder buildItemModel(String itemName, String machine, MachineFaceLayout front, @Nullable String accent,
                                            int tier, int stage) {
        ItemModelBuilder model = provider.itemModels().withExistingParent(itemName, "block/block")
                .renderType("cutout")
                .texture("particle", provider.modLoc(PLATE))
                .texture("plate", provider.modLoc(SIMPLE_MACHINE_DIR + "/plate/" + (stage == 0 ? "l" : "o" + stage) + "_plate"))
                .texture("mount", provider.modLoc(SIMPLE_MACHINE_DIR + LIGHT_MOUNT_DIR + "/l" + tier))
                .texture("light", provider.modLoc(LIGHT_OFF))
                .texture("ring", provider.modLoc(SIMPLE_MACHINE_DIR + PORT_DIR + "/l" + tier + "_port"))
                .texture("center", provider.modLoc(PORT_CENTER));
        addFacesTextures(model, machine);

        model.element().from(0, 0, 0).to(16, 16, 16).textureAll("#plate").end();
        addFaceElements(model);
        if (accent != null) {
            model.texture("accent", provider.modLoc(accentTexture(machine, accent, tier)));
            addAccentElement(model, "accent");
        }
        parts.addLightMountAndLight(model, front, "mount", "light", false);
        parts.addPortSides(model, "ring", side -> side == Direction.DOWN);
        parts.addPortSides(model, "center", side -> side == Direction.DOWN);
        return model;
    }
}
