package net.scruffy.dermicraft.datagen.machine;

import net.minecraft.core.Direction;

import java.util.List;

import static net.scruffy.dermicraft.datagen.machine.Dirs.*;

public class MachineLayouts {
    public static final MachineFaceLayout MASTICATOR = new MachineFaceLayout(
            Direction.NORTH, 9, 9,                       // mount: lower-left at (1, 10)
            List.of(new MachineFaceLayout.Feature(
                    "mouth", 2, 1, 12, 9,                 // 12x9 mouth at (2, 1)
                    SIMPLE_MACHINE_DIR + MASTICATOR_DIR + "/masticator_mouth_l%d",
                    SIMPLE_MACHINE_DIR + MASTICATOR_DIR + "/masticator_mouth_l%d_moving")));

    /**
     * The printer is a full-face 16x16 texture whose top six rows are transparent at every tier and frame, so the
     * mount sits there, top-right (x 9-14, y 10-15).
     */
    public static final MachineFaceLayout METASTASIZER = new MachineFaceLayout(
            Direction.NORTH, 1, 9,
            List.of(new MachineFaceLayout.Feature(
                    "printer", 0, 0, 16, 16,
                    SIMPLE_MACHINE_DIR + METASTASIZER_DIR + "/metastasizer_printer_l%d",
                    SIMPLE_MACHINE_DIR + METASTASIZER_DIR + "/metastasizer_printer_l%d_moving")));
}
