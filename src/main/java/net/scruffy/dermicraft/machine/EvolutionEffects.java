package net.scruffy.dermicraft.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * The sounds and particles every machine plays while it evolves: a conduit sound when the countdown
 * starts, a dust-and-smoke burst that grows with progress, and a larger burst with an anvil sound on the
 * finale. The machine owns the timing (when it starts, how far along it is, when it finishes); this only
 * draws it. The tint is a parameter because it comes from the module (or, for machines not yet migrated,
 * the target fluid).
 */
public class EvolutionEffects {

    /** Lava-orange, the Thermal family's color and the fallback when a module has none of its own. */
    public static final int DEFAULT_TINT = 0xFFCF4B12;

    /**
     * How often a machine calls {@link #spawnParticles}, which sets how dense each call is: a machine that
     * ticks the effect every game tick sends lighter bursts than one that does so once per craft cycle.
     * Smoke is thinned to every {@code smokeInterval} ticks so it does not smother a per-tick machine.
     */
    public enum Cadence {
        TICK(4, 10, 2, 6, 4),
        CYCLE(8, 16, 3, 8, 1);

        private final int dustBase;
        private final int dustGrowth;
        private final int smokeBase;
        private final int smokeGrowth;
        private final int smokeInterval;

        Cadence(int dustBase, int dustGrowth, int smokeBase, int smokeGrowth, int smokeInterval) {
            this.dustBase = dustBase;
            this.dustGrowth = dustGrowth;
            this.smokeBase = smokeBase;
            this.smokeGrowth = smokeGrowth;
            this.smokeInterval = smokeInterval;
        }
    }

    /**
     * One burst of dust and smoke around the block. {@code progress} (0 to 1, see {@link #getProgress})
     * makes it denser as the evolution nears completion; {@code finale} adds the big final burst and the
     * anvil sound.
     */
    public static void spawnParticles(ServerLevel level, BlockPos pos, float progress, boolean finale,
                                      int tint, Cadence cadence) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;
        DustParticleOptions dust = getDust(tint);

        level.sendParticles(dust, cx, cy, cz, cadence.dustBase + Math.round(progress * cadence.dustGrowth),
                0.3, 0.3, 0.3, 0.03);
        if (level.getGameTime() % cadence.smokeInterval == 0) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy, cz,
                    cadence.smokeBase + Math.round(progress * cadence.smokeGrowth), 0.35, 0.35, 0.35, 0.02);
        }

        if (finale) {
            level.sendParticles(dust, cx, cy, cz, 30, 0.5, 0.5, 0.5, 0.06);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy, cz, 16, 0.5, 0.5, 0.5, 0.04);
            level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /**
     * How far along a countdown is, 0 to 1. Clamped because the window length is config: a reload
     * mid-countdown can leave more ticks remaining than the new total.
     */
    public static float getProgress(int remaining, int total) {
        return Math.clamp(1f - (remaining / (float) Math.max(1, total)), 0f, 1f);
    }

    /** The dust particle for an ARGB tint (the alpha byte is ignored). */
    public static DustParticleOptions getDust(int tint) {
        return new DustParticleOptions(new Vector3f(
                ((tint >> 16) & 0xFF) / 255.0F,
                ((tint >> 8) & 0xFF) / 255.0F,
                (tint & 0xFF) / 255.0F), 1.4F);
    }

    /** Played once when an evolution countdown starts. */
    public static void playStartSound(Level level, BlockPos pos, float volume, float pitch) {
        level.playSound(null, pos, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, volume, pitch);
    }

    /** Played once when an evolution countdown starts. */
    public static void playDefaultStartSound(Level level, BlockPos pos) {
        playStartSound(level, pos, 1.0f, 0.6f);
    }
}
