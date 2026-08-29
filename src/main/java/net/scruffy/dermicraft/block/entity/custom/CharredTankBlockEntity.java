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
import net.scruffy.dermicraft.screen.custom.charred_tank.CharredTankMenu;
import net.scruffy.dermicraft.tank.VulnerableTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Charred Tank -- Skin Tank's hazard-gated Tier 2 evolution. Everything about the base storage tank
 * (item handler, channels, drops) is inherited from {@link SkinTankBlockEntity} unchanged; the only
 * real difference is the capability leap this class exists to provide: double capacity (whole
 * buckets) and unconditional {@code HazardProfile.TIER_2} tolerance (thermal-hazard fluids), via
 * overriding {@link #createTank()}. Standalone Tier 2 machine for now, same as Drooling Crucible's
 * original state -- no evolution-FROM-Skin-Tank path built yet.
 */
public class CharredTankBlockEntity extends SkinTankBlockEntity {

    public static final int CAPACITY = SkinTankBlockEntity.CAPACITY * 2;

    public CharredTankBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CHARRED_TANK_BE.get(), pos, blockState);
    }

    // Already evolved -- installing an Evolution Module here does nothing: this class's tank is
    // permanently at least TIER_2 regardless of any Module (see installedHazardProfile() below), and
    // there's no further Tier for it to accumulate progress toward. A Safety Module still layers
    // extra hazard tolerance on top, same as it would for the base Skin Tank.
    @Override
    protected boolean canEvolve() {
        return false;
    }

    @Override
    protected VulnerableTank createTank() {
        return createVulnerableTank(CAPACITY, -1, this::installedHazardProfile);
    }

    // Matches createTank() above -- SkinTankBlockEntity#applyCapacityBonus()/canRemoveModule() call
    // baseCapacity() (virtual) rather than referencing CAPACITY directly, specifically so this
    // override makes them resolve to Charred Tank's own doubled capacity, not Skin Tank's.
    @Override
    protected int baseCapacity() {
        return CAPACITY;
    }

    // Union of this class's permanent TIER_2 floor with whatever Safety Module(s) sit in its 2
    // Module slots -- same rule the base SkinTankBlockEntity#installedHazardProfile() applies from
    // TIER_1, just starting one rung higher since Charred's TIER_2 is unconditional (never drops
    // below it, only adds on top). Previously hardcoded to a flat TIER_2 that ignored the Module
    // slots entirely, stranding e.g. a Radiation Safety Module dropped in here.
    @Override
    public HazardProfile installedHazardProfile() {
        HazardProfile profile = HazardProfile.TIER_2;
        for (int slot = 0; slot < MODULE_INVENTORY.getSlots(); slot++) {
            profile = IHaveModules.installedHazardProfile(profile, MODULE_INVENTORY.getStackInSlot(slot));
        }
        return profile;
    }

    // The "Charred machines get an extra Module slot" upgrade -- both slots now count toward
    // installedHazardProfile()'s union above.
    @Override
    public int moduleSlotCount() {
        return 2;
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.CHARRED_TANK);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CharredTankMenu(containerId, inventory, this);
    }
}
