package net.scruffy.dermicraft.datagen.machine;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;

import java.util.List;

import static net.scruffy.dermicraft.datagen.machine.Dirs.*;

/**
 * Builds a farm machine's block model, one piece per state (the light is off or on), in the style of the simple machines:
 * the L-tier plate as the base cube, the machine's own faces over it (front, back, left, right and top, each a 16 x 16
 * texture that may have transparent parts), the status light and its mount on the front, and the port ring and center on
 * the bottom. The model is authored with its front on the north face; the blockstate turns it to the machine's facing.
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

    private static final String FARM_MACHINE_DIR = BLOCK_DIR + "/farm_machine";
    private static final String PLATE = SIMPLE_MACHINE_DIR + "/plate/l_plate";
    private static final String MOUNT = SIMPLE_MACHINE_DIR + LIGHT_MOUNT_DIR + "/l1";
    private static final String LIGHT_OFF = SIMPLE_MACHINE_DIR + LIGHT_DIR + "/light_off";
    private static final String LIGHT_ON = SIMPLE_MACHINE_DIR + LIGHT_DIR + "/light_on";
    private static final String PORT_RING = SIMPLE_MACHINE_DIR + PORT_DIR + "/l1_port";
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
     * The model for one state: {@code block/<machine>} with the light off, {@code block/<machine>_on} with it lit. Call
     * once per state: the provider hands back the same builder for a name it already has.
     */
    public BlockModelBuilder build(String machine, boolean lit) {
        return build(machine, lit, FRONT);
    }

    /** As {@link #build(String, boolean)}, with the light mount placed by {@code front}. */
    public BlockModelBuilder build(String machine, boolean lit, MachineFaceLayout front) {
        String textures = FARM_MACHINE_DIR + "/" + machine + "/" + machine;
        BlockModelBuilder model = provider.models().getBuilder(MODEL_DIR + machine + (lit ? "_on" : ""))
                .parent(provider.models().getExistingFile(provider.mcLoc("block/block")))
                .renderType("cutout")
                .texture("particle", provider.modLoc(PLATE))
                .texture("plate", provider.modLoc(PLATE))
                .texture("front", provider.modLoc(textures + "_front"))
                .texture("back", provider.modLoc(textures + "_back"))
                .texture("left", provider.modLoc(textures + "_left"))
                .texture("right", provider.modLoc(textures + "_right"))
                .texture("top", provider.modLoc(textures + "_top"))
                .texture("mount", provider.modLoc(MOUNT))
                .texture("light", provider.modLoc(lit ? LIGHT_ON : LIGHT_OFF))
                .texture("ring", provider.modLoc(PORT_RING))
                .texture("center", provider.modLoc(PORT_CENTER));

        // The plate, the base cube on every side.
        model.element().from(0, 0, 0).to(16, 16, 16)
                .allFaces((side, face) -> face.texture("#plate").cullface(side)).end();

        // The machine's own faces, over the plate. Seen from outside the north face the player's left is east, so the
        // left texture is on the east side and the right on the west. The bottom has none: it carries the ports.
        var faces = model.element().from(-FACE_OFFSET, -FACE_OFFSET, -FACE_OFFSET)
                .to(16 + FACE_OFFSET, 16 + FACE_OFFSET, 16 + FACE_OFFSET);
        faces.face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#front").cullface(Direction.NORTH).end();
        faces.face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#back").cullface(Direction.SOUTH).end();
        faces.face(Direction.EAST).uvs(0, 0, 16, 16).texture("#left").cullface(Direction.EAST).end();
        faces.face(Direction.WEST).uvs(0, 0, 16, 16).texture("#right").cullface(Direction.WEST).end();
        faces.face(Direction.UP).uvs(0, 0, 16, 16).texture("#top").cullface(Direction.UP).end();
        faces.end();

        parts.addLightMountAndLight(model, front, "mount", "light", lit);
        parts.addPortSides(model, "ring", side -> side == Direction.DOWN);
        parts.addPortSides(model, "center", side -> side == Direction.DOWN);
        return model;
    }
}
