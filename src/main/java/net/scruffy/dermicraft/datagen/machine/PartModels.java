package net.scruffy.dermicraft.datagen.machine;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.scruffy.dermicraft.block.custom.MachineStatus;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import static net.scruffy.dermicraft.datagen.machine.Dirs.*;

/**
 * Builds the small layer models a machine's blockstate is stacked from, and the single combined model its item uses.
 * Every position comes in as the layout describes it (pixels, from the lower-left as the player sees the face); this is
 * the only class that turns that into model coordinates.
 */
public class PartModels {

    /** How far each layer sits off the face, in model pixels, so decals never share a plane. Tune by eye in game. */
    private static final float LAYER_OFFSET = 0.05f;
    private static final int MOUNT_LAYER = 1;
    /** The mount has a hole the light sits in, so they share a plane. */
    private static final int LIGHT_LAYER = MOUNT_LAYER;
    /** The ring has a hole the center sits in, so they share a plane. */
    private static final int PORT_LAYER = 1;
    /** A machine's own feature (the Masticator's mouth) sits on the plate beside the mount, never over it. */
    private static final int FEATURE_LAYER = 1;

    /** The plate texture for each Overgrowth stage (0 = none). All the tumor base until the O-stage plates exist. */
    private static final String[] PLATE_TEXTURES = {
            SIMPLE_MACHINE_DIR + "/plate/l_plate",    // stage 0: the plain plate, used by every tier
            SIMPLE_MACHINE_DIR + "/plate/o1_plate",   // stage 1 (O1)
            SIMPLE_MACHINE_DIR + "/plate/o2_plate",   // stage 2 (O5)
            SIMPLE_MACHINE_DIR + "/plate/o3_plate"    // stage 3 (O10)
    };

    private final BlockStateProvider provider;
    /** Every decal and port model built so far, by name. {@code getBuilder} hands back the same builder for a name it
     * already has, and adding an element to it again would duplicate the element, so each is built once. */
    private final Map<String, BlockModelBuilder> built = new HashMap<>();

    public PartModels(BlockStateProvider provider) {
        this.provider = provider;
    }

    /** The full-block plate for one Overgrowth stage: {@code plate_o<stage>}, shared by every machine. */
    public BlockModelBuilder buildPlate(int stage) {
        return provider.models().cubeAll(MODEL_DIR + "plate_o" + stage, provider.modLoc(PLATE_TEXTURES[stage]));
    }

    /** The four lamp textures, matching light_off / light_on / light_warning / light_error. */
    public enum Lamp {
        OFF, ON, WARNING, ERROR;

        public String getName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The one place a status becomes a lamp color. */
        public static Lamp forStatus(MachineStatus status) {
            return switch (status) {
                case IDLE -> OFF;
                case RUNNING -> ON;
                case STRAINED_IDLE, STRAINED_RUNNING -> WARNING;
                case BROKEN_DOWN -> ERROR;
            };
        }
    }

    /** The light mount for one tier: {@code <machine>/light_mount_l<tier>}. */
    public BlockModelBuilder buildLightMount(String machine, MachineFaceLayout layout, int tier) {
        return buildDecal(MODEL_DIR + machine + "/light_mount_l" + tier,
                layout.face(), layout.lightMountX(), layout.lightMountY(),
                MachineFaceLayout.MOUNT_SIZE, MachineFaceLayout.MOUNT_SIZE,
                SIMPLE_MACHINE_DIR + LIGHT_MOUNT_DIR + "/l" + tier, MOUNT_LAYER, 0);
    }

    /** The light in one color, centred in the mount: {@code <machine>/light_<color>}. */
    public BlockModelBuilder buildLight(String machine, MachineFaceLayout layout, Lamp lamp) {
        return buildDecal(MODEL_DIR + machine + "/light_" + lamp.getName(), layout.face(),
                layout.getLightX(), layout.getLightY(),
                MachineFaceLayout.LIGHT_SIZE, MachineFaceLayout.LIGHT_SIZE,
                SIMPLE_MACHINE_DIR + LIGHT_DIR + "/light_" + lamp.getName(), LIGHT_LAYER,
                lamp == Lamp.OFF ? 0 : 15);
    }

    /** One feature texture for a tier: {@code <machine>/<feature>_l<tier>}, or {@code ..._moving} for the animated one. */
    public BlockModelBuilder buildFeature(String machine, MachineFaceLayout layout,
                                          MachineFaceLayout.Feature feature, int tier, boolean moving) {
        return buildDecal(MODEL_DIR + machine + "/" + feature.name() + "_l" + tier + (moving ? "_moving" : ""),
                layout.face(), feature.x(), feature.y(), feature.width(), feature.height(),
                moving ? feature.getMovingTexture(tier) : feature.getStillTexture(tier),
                FEATURE_LAYER, 0);
    }

    /** The port ring for one tier on every side except the face: {@code <machine>/ports_l<tier>}. */
    public BlockModelBuilder buildPorts(String machine, MachineFaceLayout layout, int tier) {
        return buildPortSides(MODEL_DIR + machine + "/ports_l" + tier, layout,
                SIMPLE_MACHINE_DIR + PORT_DIR + "/l" + tier + "_port");
    }

    /** The center that fills the ring's hole, the same at every tier: {@code <machine>/ports_center}. */
    public BlockModelBuilder buildPortCenter(String machine, MachineFaceLayout layout) {
        return buildPortSides(MODEL_DIR + machine + "/ports_center", layout,
                SIMPLE_MACHINE_DIR + PORT_DIR + "/port_center");
    }

    /**
     * The inventory model of a machine at a tier and Overgrowth stage: the plate, light mount, unlit light, each
     * feature's still texture and the ports, all in one model, since an item can only have the one. Tier 1 stage 0 is
     * the item's base model; the others are its overrides (see {@code MachineBlockStates}).
     *
     * @param itemName the model's name under {@code models/item}
     * @param stage    the Overgrowth stage, 0 to 3
     */
    public ItemModelBuilder buildItemModel(String itemName, MachineFaceLayout layout, int tier, int stage) {
        ItemModelBuilder model = provider.itemModels().withExistingParent(itemName, "block/block")
                .renderType("cutout")
                .texture("plate", provider.modLoc(PLATE_TEXTURES[stage]))
                .texture("mount", provider.modLoc(SIMPLE_MACHINE_DIR + LIGHT_MOUNT_DIR + "/l" + tier))
                .texture("light", provider.modLoc(SIMPLE_MACHINE_DIR + LIGHT_DIR + "/light_" + Lamp.OFF.getName()))
                .texture("ring", provider.modLoc(SIMPLE_MACHINE_DIR + PORT_DIR + "/l" + tier + "_port"))
                .texture("center", provider.modLoc(SIMPLE_MACHINE_DIR + PORT_DIR + "/port_center"));

        model.element().from(0, 0, 0).to(16, 16, 16).textureAll("#plate").end();
        addDecalElement(model, "mount", layout.face(), layout.lightMountX(), layout.lightMountY(),
                MachineFaceLayout.MOUNT_SIZE, MachineFaceLayout.MOUNT_SIZE, MOUNT_LAYER, 0);
        addDecalElement(model, "light", layout.face(), layout.getLightX(), layout.getLightY(),
                MachineFaceLayout.LIGHT_SIZE, MachineFaceLayout.LIGHT_SIZE, LIGHT_LAYER, 0);
        for (int i = 0; i < layout.features().size(); i++) {
            MachineFaceLayout.Feature feature = layout.features().get(i);
            String key = "feature_" + i;
            model.texture(key, provider.modLoc(feature.getStillTexture(tier)));
            addDecalElement(model, key, layout.face(), feature.x(), feature.y(), feature.width(), feature.height(),
                    FEATURE_LAYER, 0);
        }
        addPortElement(model, "ring", layout);
        addPortElement(model, "center", layout);
        return model;
    }

    /** One flat quad on the face, as its own block model. */
    private BlockModelBuilder buildDecal(String modelName, Direction face, int x, int y, int width, int height,
                                         String texturePath, int layer, int emissivity) {
        return built.computeIfAbsent(modelName, name -> {
            BlockModelBuilder model = provider.models().getBuilder(name)
                    .renderType("cutout")
                    .texture("decal", provider.modLoc(texturePath));
            addDecalElement(model, "decal", face, x, y, width, height, layer, emissivity);
            return model;
        });
    }

    /**
     * Adds a flat quad to any model, converting the player's view of the face into model coordinates. The texture is
     * the model's {@code textureKey}.
     */
    private <T extends ModelBuilder<T>> void addDecalElement(T model, String textureKey, Direction face,
                                                             int x, int y, int width, int height,
                                                             int layer, int emissivity) {
        float offset = LAYER_OFFSET * layer;
        var element = model.element();
        if (face == Direction.NORTH) {
            // Seen from outside the north face, the player's left is +x, so x is mirrored.
            element.from(16 - (x + width), y, -offset)
                    .to(16 - x, y + height, -offset);
        } else {
            // The top face is read the way a player standing at the front sees it, looking at the back: the art's
            // left is the model's east (+x), its top is the model's south (+z). That is the texture turned half a
            // turn, so the pixel position below is mirrored on both axes and the face itself is rotated to match.
            element.from(16 - (x + width), 16 + offset, y)
                    .to(16 - x, 16 + offset, y + height);
        }

        var faceBuilder = element.face(face).uvs(0, 0, 16, 16).texture("#" + textureKey);
        if (face == Direction.UP) faceBuilder.rotation(ModelBuilder.FaceRotation.UPSIDE_DOWN);
        if (emissivity > 0) faceBuilder.emissivity(emissivity, emissivity);
        faceBuilder.end().end();
    }

    private BlockModelBuilder buildPortSides(String modelName, MachineFaceLayout layout, String texturePath) {
        return built.computeIfAbsent(modelName, name -> {
            BlockModelBuilder model = provider.models().getBuilder(name)
                    .renderType("cutout")
                    .texture("port", provider.modLoc(texturePath));
            addPortElement(model, "port", layout);
            return model;
        });
    }

    /** Adds a slightly oversize box carrying a port texture on every side except the face. */
    private <T extends ModelBuilder<T>> void addPortElement(T model, String textureKey, MachineFaceLayout layout) {
        addPortElement(model, textureKey, side -> side != layout.face());
    }

    /** Adds a slightly oversize box carrying a port texture on each side the predicate accepts. */
    private <T extends ModelBuilder<T>> void addPortElement(T model, String textureKey, Predicate<Direction> include) {
        float offset = LAYER_OFFSET * PORT_LAYER;
        var element = model.element()
                .from(-offset, -offset, -offset)
                .to(16 + offset, 16 + offset, 16 + offset);
        for (Direction side : Direction.values()) {
            if (!include.test(side)) continue;
            element.face(side).uvs(0, 0, 16, 16).texture("#" + textureKey).cullface(side).end();
        }
        element.end();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The tank (Skin Tank). No facing and no light: the four sides carry a windowed plate, a tier accent and glass,
    // the top and bottom carry the shared plate with the port ring and center, and an interior backing shows through
    // the windows. The side parts are authored once on the north face and placed four times by rotation.
    // ------------------------------------------------------------------------------------------------------------

    private static final String TANK_MODEL_DIR = MODEL_DIR + "skin_tank/";
    private static final String TANK_TEXTURE_DIR = SIMPLE_MACHINE_DIR + SKIN_TANK_DIR;

    /**
     * The windowed side plate for each Overgrowth stage (0 = none). Every stage uses the L-tier tank plate until the
     * O-stage tank plates exist: add them next to {@code plate.png} (say {@code plate_o1.png}) and point the matching
     * entry at it. The glass and accents do not change with Overgrowth.
     */
    private static final String[] TANK_SIDE_PLATE_TEXTURES = {
            TANK_TEXTURE_DIR + "/plate",   // stage 0: the plain plate, used by every tier
            TANK_TEXTURE_DIR + "/plate_o1",   // stage 1 (O1)
            TANK_TEXTURE_DIR + "/plate_o2",   // stage 2 (O5)
            TANK_TEXTURE_DIR + "/plate_o3"    // stage 3 (O10)
    };
    private static final String TANK_GLASS_TEXTURE = TANK_TEXTURE_DIR + "/glass";
    /** The accent ring for a tier is {@code l1} to {@code l4}. */
    private static final String TANK_ACCENT_PATTERN = TANK_TEXTURE_DIR + "/l%d";
    /** How far inside the block the interior backing sits, in model pixels: behind the fluid, which is drawn 1.6 in. */
    private static final float TANK_INTERIOR_INSET = 0.8f;

    private static boolean isTankEnd(Direction side) {
        return side == Direction.UP || side == Direction.DOWN;
    }

    /** The full-block plate for one Overgrowth stage: the windowed plate on the sides, the shared plate on the ends. */
    public BlockModelBuilder buildTankPlate(int stage) {
        return built.computeIfAbsent(TANK_MODEL_DIR + "plate_o" + stage, name -> {
            BlockModelBuilder model = provider.models().getBuilder(name)
                    .renderType("cutout")
                    .texture("side", provider.modLoc(TANK_SIDE_PLATE_TEXTURES[stage]))
                    .texture("end", provider.modLoc(PLATE_TEXTURES[stage]))
                    .texture("particle", provider.modLoc(TANK_SIDE_PLATE_TEXTURES[stage]));
            addTankPlateElement(model, "side", "end");
            return model;
        });
    }

    /** The accent ring for one tier, on the north face. */
    public BlockModelBuilder buildTankAccent(int tier) {
        return buildDecal(TANK_MODEL_DIR + "accent_l" + tier, Direction.NORTH, 0, 0, 16, 16,
                TANK_ACCENT_PATTERN.formatted(tier), 1, 0);
    }

    /** The glass over the window, on the north face. Translucent, unlike every other layer. */
    public BlockModelBuilder buildTankGlass() {
        return buildDecal(TANK_MODEL_DIR + "glass", Direction.NORTH, 0, 0, 16, 16, TANK_GLASS_TEXTURE, 1, 0)
                .renderType("translucent");
    }

    /**
     * The inside of the tank for one Overgrowth stage, in that stage's plates, seen through the windows: each of the six walls' inner side (four sides, floor and
     * ceiling) carries the plate with the glass in front of it, all facing in. Whatever the tank holds is drawn between these and the front windows.
     */
    public BlockModelBuilder buildTankInterior(int stage) {
        return built.computeIfAbsent(TANK_MODEL_DIR + "interior_o" + stage, name -> {
            // Cutout, so it is drawn before the fluid (a block entity render): a translucent wall behind the fluid
            // would be depth-rejected by it and vanish.
            BlockModelBuilder model = provider.models().getBuilder(name)
                    .renderType("cutout")
                    .texture("plate", provider.modLoc(TANK_SIDE_PLATE_TEXTURES[stage]))
                    .texture("particle", provider.modLoc(TANK_SIDE_PLATE_TEXTURES[stage]));
            // The floor and ceiling use the solid end plate: the side plate has the window cut out of it.
            model.texture("end", provider.modLoc(PLATE_TEXTURES[stage]));
            for (Direction inward : Direction.values()) {
                addInteriorWall(model, isTankEnd(inward) ? "end" : "plate", inward, 0);
            }
            return model;
        });
    }

    /** The glass over the interior plates. Translucent, so it only shows where no fluid is behind it. */
    public BlockModelBuilder buildTankInteriorGlass() {
        return built.computeIfAbsent(TANK_MODEL_DIR + "interior_glass", name -> {
            BlockModelBuilder model = provider.models().getBuilder(name)
                    .renderType("translucent")
                    .texture("glass", provider.modLoc(TANK_GLASS_TEXTURE))
                    .texture("particle", provider.modLoc(TANK_GLASS_TEXTURE));
            for (Direction inward : Direction.values()) {
                addInteriorWall(model, "glass", inward, 1);
            }
            return model;
        });
    }

    /** The port ring for one tier on the top and bottom only: {@code skin_tank/ports_l<tier>}. */
    public BlockModelBuilder buildTankPorts(int tier) {
        return buildTankEnds(TANK_MODEL_DIR + "ports_l" + tier, SIMPLE_MACHINE_DIR + PORT_DIR + "/l" + tier + "_port");
    }

    /** The center that fills the ring's hole, the same at every tier, top and bottom only. */
    public BlockModelBuilder buildTankPortCenter() {
        return buildTankEnds(TANK_MODEL_DIR + "ports_center", SIMPLE_MACHINE_DIR + PORT_DIR + "/port_center");
    }

    /** The tank's inventory model at a tier and Overgrowth stage: the plate, accent and glass on all four sides, the
     * ports on top and bottom. */
    public ItemModelBuilder buildTankItemModel(String itemName, int tier, int stage) {
        ItemModelBuilder model = provider.itemModels().withExistingParent(itemName, "block/block")
                .renderType("translucent")
                .texture("side", provider.modLoc(TANK_SIDE_PLATE_TEXTURES[stage]))
                .texture("end", provider.modLoc(PLATE_TEXTURES[stage]))
                .texture("accent", provider.modLoc(TANK_ACCENT_PATTERN.formatted(tier)))
                .texture("glass", provider.modLoc(TANK_GLASS_TEXTURE))
                .texture("ring", provider.modLoc(SIMPLE_MACHINE_DIR + PORT_DIR + "/l" + tier + "_port"))
                .texture("center", provider.modLoc(SIMPLE_MACHINE_DIR + PORT_DIR + "/port_center"));

        addTankPlateElement(model, "side", "end");
        for (Direction side : Direction.Plane.HORIZONTAL) {
            addWallDecal(model, "accent", side);
            addWallDecal(model, "glass", side);
        }
        addPortElement(model, "ring", PartModels::isTankEnd);
        addPortElement(model, "center", PartModels::isTankEnd);
        return model;
    }

    private BlockModelBuilder buildTankEnds(String modelName, String texturePath) {
        return built.computeIfAbsent(modelName, name -> {
            BlockModelBuilder model = provider.models().getBuilder(name)
                    .renderType("cutout")
                    .texture("port", provider.modLoc(texturePath));
            addPortElement(model, "port", PartModels::isTankEnd);
            // The same ports again on the inside of the floor and ceiling, in front of their glass (layer 2).
            addInteriorWall(model, "port", Direction.UP, 2);
            addInteriorWall(model, "port", Direction.DOWN, 2);
            return model;
        });
    }

    /** A full cube: {@code sideKey} on the four sides, {@code endKey} on the top and bottom. */
    private <T extends ModelBuilder<T>> void addTankPlateElement(T model, String sideKey, String endKey) {
        var element = model.element().from(0, 0, 0).to(16, 16, 16);
        for (Direction side : Direction.values()) {
            element.face(side).uvs(0, 0, 16, 16).texture("#" + (isTankEnd(side) ? endKey : sideKey))
                    .cullface(side).end();
        }
        element.end();
    }

    /** A flat full-face quad just outside one of the four sides, one layer off the surface. */
    private <T extends ModelBuilder<T>> void addWallDecal(T model, String textureKey, Direction side) {
        float offset = LAYER_OFFSET;
        var element = model.element();
        switch (side) {
            case NORTH -> element.from(0, 0, -offset).to(16, 16, -offset);
            case SOUTH -> element.from(0, 0, 16 + offset).to(16, 16, 16 + offset);
            case WEST -> element.from(-offset, 0, 0).to(-offset, 16, 16);
            default -> element.from(16 + offset, 0, 0).to(16 + offset, 16, 16);
        }
        element.face(side).uvs(0, 0, 16, 16).texture("#" + textureKey).end().end();
    }

    /**
     * A flat full-wall quad just inside one wall, facing {@code inward} (the direction it looks, into the tank).
     * {@code layer} 1 sits a hair further in than 0, so the glass draws in front of the plate.
     */
    private <T extends ModelBuilder<T>> void addInteriorWall(T model, String textureKey, Direction inward, int layer) {
        float in = TANK_INTERIOR_INSET + LAYER_OFFSET * layer;
        var element = model.element();
        switch (inward) {
            case NORTH -> element.from(0, 0, 16 - in).to(16, 16, 16 - in);   // the south wall, looking north
            case SOUTH -> element.from(0, 0, in).to(16, 16, in);             // the north wall, looking south
            case EAST -> element.from(in, 0, 0).to(in, 16, 16);              // the west wall, looking east
            case WEST -> element.from(16 - in, 0, 0).to(16 - in, 16, 16);    // the east wall, looking west
            case UP -> element.from(0, in, 0).to(16, in, 16);                // the floor, looking up
            default -> element.from(0, 16 - in, 0).to(16, 16 - in, 16);      // the ceiling, looking down
        }
        element.face(inward).uvs(0, 0, 16, 16).texture("#" + textureKey).end().end();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The port cube (Craw): no face, no facing and no light. The plate carries the port ring and center on all six
    // sides, the ring changing with tier.
    // ------------------------------------------------------------------------------------------------------------

    /** The port ring for one tier on all six sides: {@code <machine>/ports_l<tier>}. */
    public BlockModelBuilder buildCubePorts(String machine, int tier) {
        return buildCubePortModel(MODEL_DIR + machine + "/ports_l" + tier,
                SIMPLE_MACHINE_DIR + PORT_DIR + "/l" + tier + "_port");
    }

    /** The center that fills the ring's hole, the same at every tier: {@code <machine>/ports_center}. */
    public BlockModelBuilder buildCubePortCenter(String machine) {
        return buildCubePortModel(MODEL_DIR + machine + "/ports_center", SIMPLE_MACHINE_DIR + PORT_DIR + "/port_center");
    }

    /** The inventory model of a port cube at a tier and Overgrowth stage: the plate with the ring and center on every
     * side. Tier 1 stage 0 is the item's base model; the others are its overrides. */
    public ItemModelBuilder buildCubeItemModel(String itemName, int tier, int stage) {
        ItemModelBuilder model = provider.itemModels().withExistingParent(itemName, "block/block")
                .renderType("cutout")
                .texture("plate", provider.modLoc(PLATE_TEXTURES[stage]))
                .texture("ring", provider.modLoc(SIMPLE_MACHINE_DIR + PORT_DIR + "/l" + tier + "_port"))
                .texture("center", provider.modLoc(SIMPLE_MACHINE_DIR + PORT_DIR + "/port_center"));

        model.element().from(0, 0, 0).to(16, 16, 16).textureAll("#plate").end();
        addPortElement(model, "ring", side -> true);
        addPortElement(model, "center", side -> true);
        return model;
    }

    private BlockModelBuilder buildCubePortModel(String modelName, String texturePath) {
        return built.computeIfAbsent(modelName, name -> {
            BlockModelBuilder model = provider.models().getBuilder(name)
                    .renderType("cutout")
                    .texture("port", provider.modLoc(texturePath));
            addPortElement(model, "port", side -> true);
            return model;
        });
    }
}
