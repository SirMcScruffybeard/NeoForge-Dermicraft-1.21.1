package net.scruffy.dermicraft.item.custom;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.fluid.BaseFluidType;
import org.joml.Vector3f;

/**
 * Visible feedback for an active siphon, spawned SERVER-side so bystanders see it too -- a running
 * D.R.I.N.K.E.R. should be legible from across a base, not just to its holder.
 *
 * <p>Two distinct effects, because the two draw types don't look alike in fiction:
 * <ul>
 *   <li><b>Stream</b> for a world source -- particles fly from the block into the mouth, across a
 *       real gap, which is what actually sells "vacuum".
 *   <li><b>Drip</b> for a machine tank -- drawn point-blank against a face, so there's no gap to
 *       cross; a little runoff falling from the mouth reads better than a stream over nothing.
 * </ul>
 *
 * <p>Both are tinted from the fluid's own colour, so every fluid (including ones added later) is
 * covered with no per-fluid assets, and the tint doubles as information: you can tell what someone
 * is siphoning at a glance.
 */
final class DrinkerParticles {

    private DrinkerParticles() {
    }

    /** How far along the look vector the mouth sits. The rig is long, so aiming at the player's
     * centre would visibly undershoot -- particles would stop short and vanish in mid-air. */
    private static final double MOUTH_FORWARD = 0.95;
    private static final double MOUTH_SIDE = 0.28;
    private static final double MOUTH_DROP = 0.18;

    private static final float PARTICLE_SCALE = 0.9F;
    /** Every other tick. Over a 200-tick siphon a per-tick stream is dense enough to fog up the
     * crosshair in first person, which is exactly what the player is trying to aim with. */
    private static final int STREAM_INTERVAL = 2;
    private static final int DRIP_INTERVAL = 6;

    /** Fallback tint for fluids that aren't ours -- vanilla water, mostly. */
    private static final int DEFAULT_TINT = 0xFF3F76E4;

    static void streamFromSource(ServerLevel level, Player player, Vec3 source, FluidStack fluid) {
        if (level.getGameTime() % STREAM_INTERVAL != 0) return;

        Vec3 mouth = mouthPosition(player);
        Vec3 travel = mouth.subtract(source);
        double distance = travel.length();
        if (distance < 0.05) return;

        Vec3 heading = travel.scale(1.0 / distance);
        // count 0 makes the delta arguments a VELOCITY rather than a spread, which is the only way
        // to get particles that actually travel toward a point.
        level.sendParticles(tintOf(fluid), source.x, source.y, source.z, 0,
                heading.x, heading.y, heading.z, Math.min(distance * 0.35, 0.6));
    }

    static void dripFromMouth(ServerLevel level, Player player, FluidStack fluid) {
        if (level.getGameTime() % DRIP_INTERVAL != 0) return;

        Vec3 mouth = mouthPosition(player);
        level.sendParticles(tintOf(fluid), mouth.x, mouth.y, mouth.z, 0, 0.0, -0.08, 0.0, 1.0);
    }

    /**
     * Approximates where the rig's mouth is from the player's eye and facing.
     *
     * <p>Deliberately not read off the model's actual bone: that only exists during rendering, on
     * the client, for whoever is looking at it -- useless for a server-side spawn meant to be seen
     * by everyone. Eye plus facing plus a hand offset lands close enough that the stream visibly
     * terminates at the rig.
     */
    private static Vec3 mouthPosition(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        // Straight up or down collapses the usual cross product, so fall back to the facing yaw.
        Vec3 side = look.cross(new Vec3(0.0, 1.0, 0.0));
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();

        boolean heldInRightArm = (player.getUsedItemHand() == InteractionHand.MAIN_HAND)
                == (player.getMainArm() == HumanoidArm.RIGHT);

        return eye.add(look.scale(MOUTH_FORWARD))
                .add(side.scale(heldInRightArm ? MOUTH_SIDE : -MOUTH_SIDE))
                .add(0.0, -MOUTH_DROP, 0.0);
    }

    private static DustParticleOptions tintOf(FluidStack fluid) {
        int tint = DEFAULT_TINT;
        if (fluid.getFluid().getFluidType() instanceof BaseFluidType baseType) {
            tint = baseType.getTintColor();
        } else if (fluid.getFluid() == Fluids.LAVA) {
            tint = 0xFFCF4B12;
        }

        return new DustParticleOptions(new Vector3f(
                ((tint >> 16) & 0xFF) / 255.0F,
                ((tint >> 8) & 0xFF) / 255.0F,
                (tint & 0xFF) / 255.0F), PARTICLE_SCALE);
    }
}
