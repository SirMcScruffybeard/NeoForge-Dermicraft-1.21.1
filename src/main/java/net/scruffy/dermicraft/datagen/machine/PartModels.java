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
            BLOCK_DIR + "/tumor/tumor_base",   // stage 0: the plain plate, used by every tier
            BLOCK_DIR + "/tumor/tumor_base",   // stage 1 (O1): replace with the O1 plate
            BLOCK_DIR + "/tumor/tumor_base",   // stage 2 (O5)
            BLOCK_DIR + "/tumor/tumor_base"    // stage 3 (O10)
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
        float offset = LAYER_OFFSET * PORT_LAYER;
        var element = model.element()
                .from(-offset, -offset, -offset)
                .to(16 + offset, 16 + offset, 16 + offset);
        for (Direction side : Direction.values()) {
            if (side == layout.face()) continue;
            element.face(side).uvs(0, 0, 16, 16).texture("#" + textureKey).cullface(side).end();
        }
        element.end();
    }
}
