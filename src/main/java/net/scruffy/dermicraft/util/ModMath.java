package net.scruffy.dermicraft.util;

import net.minecraft.world.level.Level;

public class ModMath {

    public static class time {
        public static final int TICKS_PER_SECOND = 20;

        public static int getSecondsToTicks(int seconds) {
            return seconds * TICKS_PER_SECOND;
        }

        public static int getSecondsToTicks(float seconds) {
            return (int) (seconds * TICKS_PER_SECOND);
        }

        public static int getMinutesToTicks(int minutes) {
            return 60 * getSecondsToTicks(minutes);
        }

        public static boolean hasTicksPassed(Level level, int ticks) {
            return level.getGameTime() % ticks == 0;
        }
    }
}
