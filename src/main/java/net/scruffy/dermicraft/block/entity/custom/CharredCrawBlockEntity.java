package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.screen.custom.charred_craw.CharredCrawMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Charred Craw -- Craw's hazard-gated Tier 2 evolution (in name only; Craw holds items, not
 * fluids, so there's no actual hazard tolerance to grant -- see {@link CrawBlockEntity}'s own
 * evolution-mechanic javadoc). Everything about the base engine (input staging, recipe cache,
 * pickup preservation) is inherited from {@link CrawBlockEntity} unchanged; the only real
 * differences are the two capability leaps this class exists to provide: double storage capacity
 * ({@link #capacity()}) and double auto-push throughput ({@link #stacksPerPush()}).
 */
public class CharredCrawBlockEntity extends CrawBlockEntity {

    public CharredCrawBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CHARRED_CRAW_BE.get(), pos, blockState);
    }

    // Already evolved -- installing an Evolution Module here does nothing: capacity/throughput are
    // unconditionally doubled regardless of any Module, and there's no further Tier for it to
    // accumulate progress toward.
    @Override
    protected boolean canEvolve() {
        return false;
    }

    @Override
    protected int capacity() {
        return CAPACITY * 2;
    }

    @Override
    protected int stacksPerPush() {
        return 2;
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.CHARRED_CRAW);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CharredCrawMenu(containerId, inventory, this);
    }
}
