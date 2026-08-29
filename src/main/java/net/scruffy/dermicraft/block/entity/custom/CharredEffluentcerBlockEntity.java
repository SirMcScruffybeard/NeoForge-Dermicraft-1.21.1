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
import net.scruffy.dermicraft.screen.custom.charred_effluentcer.CharredEffluentcerMenu;
import net.scruffy.dermicraft.tank.VulnerableTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Charred Effluentcer -- the Effluentcer family's hazard-gated Tier 2 evolution. Everything about
 * the base engine (fuel/health, recipe lookup, visual state, item/fluid IO layout) is inherited from
 * {@link EffluentcerBlockEntity} unchanged; the only real difference is the capability leap this
 * class exists to provide: {@link #createInputTank(int)}/{@link #createResultTank()} are overridden
 * to accept and hold {@code HazardProfile.TIER_2} fluids (thermal-hazard fluids -- lava-class),
 * which the base Effluentcer's plain hazard-gated tanks (locked to TIER_1 plus whatever a Module
 * grants) don't tolerate permanently. Per {@code MachineTier}'s own javadoc, a genuine capability
 * leap is a hook override on the family's block entity, not a new stat-only tier constant -- this
 * class is exactly that hook.
 */
public class CharredEffluentcerBlockEntity extends EffluentcerBlockEntity {

    public CharredEffluentcerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CHARRED_EFFLUENTCER_BE.get(), pos, blockState);
    }

    // Already evolved -- installing an Evolution Module here does nothing: this class's tanks are
    // permanently at least TIER_2 regardless of any Module (see installedHazardProfile() below), and
    // there's no further Tier for it to accumulate progress toward. A Safety Module still layers
    // extra hazard tolerance on top, same as it would for the base Effluentcer.
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
    // Module slots -- same rule the base EffluentcerBlockEntity#installedHazardProfile() applies
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
    protected VulnerableTank createInputTank(int slot) {
        return new VulnerableTank(getTier().tankCapacity(), slot, this::installedHazardProfile) {
            @Override
            protected void onContentsChanged() {
                if (level != null && !level.isClientSide()) {
                    resolveRecipe();
                }
                setChanged();
            }
        };
    }

    @Override
    protected VulnerableTank createResultTank() {
        return new VulnerableTank(getTier().tankCapacity(), 3, this::installedHazardProfile) {
            @Override
            protected void onContentsChanged() {
                if (level != null && !level.isClientSide()) {
                    if (autoDrainEnabled) {
                        this.pushFluidToBelowNeighbour(level, worldPosition);
                    }
                    setChanged();
                }
            }
        };
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.CHARRED_EFFLUENTCER);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CharredEffluentcerMenu(containerId, playerInventory, this);
    }
}
