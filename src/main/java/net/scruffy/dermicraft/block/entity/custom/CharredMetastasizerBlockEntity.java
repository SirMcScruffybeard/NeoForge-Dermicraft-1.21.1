package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.hazard.HazardProfile;
import net.scruffy.dermicraft.interfaces.IHaveModules;
import net.scruffy.dermicraft.screen.custom.charred_metastasizer.CharredMetastasizerMenu;
import net.scruffy.dermicraft.tank.VulnerableTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Charred Metastasizer -- the Metastasizer family's hazard-gated Tier 2 evolution, same shape as
 * {@link CharredMasticatorBlockEntity}. Everything about the base engine (fuel/health, recipe
 * lookup, visual state, item/fluid IO layout, Module tab, evolution mechanic) is inherited from
 * {@link MetastasizerBlockEntity} unchanged; the only real differences are
 * {@link #createReagentTank()} (unconditional {@code HazardProfile.TIER_2} instead of the base's
 * Module-dependent one) and {@link #canEvolve()} (already evolved, nothing further to do).
 */
public class CharredMetastasizerBlockEntity extends MetastasizerBlockEntity {

    public CharredMetastasizerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CHARRED_METASTASIZER_BE.get(), pos, blockState);
    }

    // Already evolved -- installing an Evolution Module here does nothing: this class's tanks are
    // permanently at least TIER_2 regardless of any Module (see installedHazardProfile() below), and
    // there's no further Tier to accumulate progress toward. A Safety Module still layers extra
    // hazard tolerance on top, same as it would for the base Metastasizer.
    @Override
    protected boolean canEvolve() {
        return false;
    }

    // The "Charred machines get an extra Module slot" upgrade -- see CharredTankBlockEntity's
    // identical override for the pilot. Both slots count toward installedHazardProfile()'s union
    // below.
    @Override
    public int moduleSlotCount() {
        return 2;
    }

    // Union of this class's permanent TIER_2 floor with whatever Safety Module(s) sit in its 2
    // Module slots -- same rule the base MetastasizerBlockEntity#installedHazardProfile() applies
    // from TIER_1, just starting one rung higher since Charred's TIER_2 is unconditional (never
    // drops below it, only adds on top). Previously hardcoded to a flat TIER_2 that ignored the
    // Module slots entirely, stranding e.g. a Radiation Safety Module dropped in here.
    @Override
    protected HazardProfile installedHazardProfile() {
        HazardProfile profile = HazardProfile.TIER_2;
        for (int slot = 0; slot < MODULE_INVENTORY.getSlots(); slot++) {
            profile = IHaveModules.installedHazardProfile(profile, MODULE_INVENTORY.getStackInSlot(slot));
        }
        return profile;
    }

    @Override
    protected VulnerableTank createReagentTank() {
        return new VulnerableTank(getTier().tankCapacity(), 1, this::installedHazardProfile) {
            @Override
            protected void onContentsChanged() {
                if (level != null && !level.isClientSide()) {
                    resolveRecipe();
                    setChanged();
                }
            }
        };
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.CHARRED_METASTASIZER);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CharredMetastasizerMenu(containerId, playerInventory, this);
    }
}
