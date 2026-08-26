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

    // Already evolved -- installing an Evolution Module here does nothing: this class's own tank is
    // unconditionally TIER_2 regardless of any Module (see createTank() below), and there's no
    // further Tier for it to accumulate progress toward.
    @Override
    protected boolean canEvolve() {
        return false;
    }

    @Override
    protected VulnerableTank createTank() {
        return createVulnerableTank(CAPACITY, -1, () -> HazardProfile.TIER_2);
    }

    // Matches createTank() above -- without this override, a future tooltip/screen reading the
    // inherited SkinTankBlockEntity#installedHazardProfile() would report TIER_1 (+ any Safety
    // Module) instead of this class's real, unconditional TIER_2 tolerance.
    @Override
    public HazardProfile installedHazardProfile() {
        return HazardProfile.TIER_2;
    }

    // The "Charred machines get an extra Module slot" upgrade -- this class's own hazard tolerance
    // stays fixed at TIER_2 regardless (see installedHazardProfile() above), so the second slot's
    // value here is future module kinds beyond hazard tolerance, not a bigger hazard union.
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
