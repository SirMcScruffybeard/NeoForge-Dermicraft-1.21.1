package net.scruffy.dermicraft.screen;

/**
 * Where things sit on a farm machine's screen, shared by the menu (which places the slots) and the screen (which draws
 * the backdrops). Pure constants, so the server can load the menu without ever touching a client class. Coordinates are
 * the backdrop's top-left inside the 176 x 166 main panel; a slot sits one pixel in from its backdrop.
 */
public final class FarmLayout {

    private FarmLayout() {}

    public static final int SLOT_SIZE = 18;

    // ---- Home tab ----
    /** The "Field: W x L" readout at the top. */
    public static final int READOUT_X = 7;
    public static final int READOUT_Y = 8;

    /** The fuel row: the container slot, then the horizontal gauge just right of it. */
    public static final int FUEL_SLOT_X = 7;
    public static final int FUEL_SLOT_Y = 44;
    public static final int FUEL_GAUGE_X = 26;
    public static final int FUEL_GAUGE_Y = 44;

    /** The input row (seeds, saplings, feed): up to three rows of four slots right of the fuel gauge. The first row is level with
     * the fuel gauge and each later row sits above it, so a tier's new slots appear on top. */
    public static final int INPUT_ROW_X = 97;
    public static final int INPUT_ROW_Y = 44;
    public static final int INPUT_COLUMNS = 4;

    /** The backdrop's left edge for input slot {@code index} (0 is the first slot of the bottom row). */
    public static int getInputSlotX(int index) {
        return INPUT_ROW_X + (index % INPUT_COLUMNS) * SLOT_SIZE;
    }

    /** The backdrop's top edge for input slot {@code index}. */
    public static int getInputSlotY(int index) {
        return INPUT_ROW_Y - (index / INPUT_COLUMNS) * SLOT_SIZE;
    }

    /** The output buffer row, nine slots across. */
    public static final int BUFFER_ROW_X = 7;
    public static final int BUFFER_ROW_Y = 64;

    // ---- Field tab ----
    /** The three stepper rows: a label on the left, a minus and a plus button on the right. */
    public static final int FIELD_LABEL_X = 7;
    public static final int FIELD_MINUS_X = 62;
    public static final int FIELD_PLUS_X = 82;
    public static final int FIELD_WIDTH_ROW_Y = 16;
    public static final int FIELD_LENGTH_ROW_Y = 38;
    public static final int FIELD_DISTANCE_ROW_Y = 60;

    /** The preview grid: a box 54 pixels across (9 cells of 6 pixels at the smallest tier), each cell shrinking as the tier's
     * largest field grows so the same box always fits it. */
    public static final int FIELD_GRID_PIXELS = 54;
    public static final int FIELD_GRID_X = 108;
    public static final int FIELD_GRID_Y = 14;
}
