package net.scruffy.dermicraft.machine;

/**
 * Implemented by a machine's {@code Block} to declare which {@link MachineTier} it is. The family's
 * block entity reads the tier off its block state at runtime, so the tier never has to be stored in
 * NBT and one block-entity class can serve every stat-only tier of a family.
 */
public interface TieredMachine {
    MachineTier getTier();
}
