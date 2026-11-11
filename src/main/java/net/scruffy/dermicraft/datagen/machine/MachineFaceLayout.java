package net.scruffy.dermicraft.datagen.machine;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * Where the parts of one machine's face sit. All positions are in pixels (0 to 16) measured the way a player sees the
 * face: the origin is the face's lower-left corner and x grows to the right, y grows upward. Converting that to model
 * coordinates (the north face is mirrored in x, the top face has its own orientation) is PartModels' job, so a layout
 * never has to think about it.
 *
 * <p>The status light always sits inside its mount, centred, so one position covers both. The face is the side the
 * model is authored on: NORTH for the machines that rotate with their facing, UP for the Grafting Table.
 *
 * @param face     the authored face, NORTH or UP
 * @param lightMountX   left edge of the light mount
 * @param lightMountY   bottom edge of the light mount
 * @param features the machine's own parts on the face (the Masticator's mouth); empty for a light-only face
 */
public record MachineFaceLayout(Direction face, int lightMountX, int lightMountY, List<MachineFaceLayout.Feature> features) {

    public static final int FACE_SIZE = 16;

    /** The light mount texture is 6x6 and the light texture 4x4, so the light sits one pixel in from the mount's edge. */
    public static final int MOUNT_SIZE = 6;
    public static final int LIGHT_SIZE = 4;
    public static final int LIGHT_INSET = (MOUNT_SIZE - LIGHT_SIZE) / 2;

    public MachineFaceLayout {
        if (face != Direction.NORTH && face != Direction.UP) {
            throw new IllegalArgumentException("A machine face must be NORTH or UP, not " + face);
        }

        requireInside("light mount", lightMountX, lightMountY, MOUNT_SIZE, MOUNT_SIZE);
        features = List.copyOf(features);
        for (Feature feature : features) {
            requireInside(feature.name(), feature.x(), feature.y(), feature.width(), feature.height());
        }
    }

    /** Left edge of the light, inside the mount. */
    public int getLightX() {
        return lightMountX + LIGHT_INSET;
    }

    /** Bottom edge of the light, inside the mount. */
    public int getLightY() {
        return lightMountY + LIGHT_INSET;
    }

    private static void requireInside(String what, int x, int y, int width, int height) {
        if ((x < 0 || y < 0) || (x + width > FACE_SIZE || y + height > FACE_SIZE)) {
            throw new IllegalArgumentException(what + " does not fit on the face: x=" + x + " y=" + y
                    + " size=" + width + "x" + height);
        }
    }

    /**
     * One of the machine's own parts on its face, with a texture per tier. The texture fields are patterns with a
     * single {@code %d} that is filled in with the tier (1 to 4), relative to the mod's texture folder, for example
     * {@code "block/simple_machine/masticator/masticator_mouth_l%d"}. A part that never moves has no moving pattern.
     *
     * @param name          a short name used in the generated model names, for example "mouth"
     * @param x             left edge
     * @param y             bottom edge
     * @param width         width in pixels (matches the texture's width)
     * @param height        height in pixels (matches ONE frame of the animated texture)
     * @param stillPattern  the texture shown when the machine is not working
     * @param movingPattern the animated texture shown while it is working, or null if the part does not move
     */
    public record Feature(String name, int x, int y, int width, int height,
                          String stillPattern, @Nullable String movingPattern) {

        public String getStillTexture(int tier) {
            return stillPattern.formatted(tier);
        }

        public boolean hasMovingTexture() {
            return movingPattern != null;
        }

        /** The animated texture for a tier; only valid when {@link #hasMovingTexture()}. */
        public String getMovingTexture(int tier) {
            if (movingPattern == null) throw new IllegalStateException(name + " has no moving texture");
            return movingPattern.formatted(tier);
        }
    }
}
