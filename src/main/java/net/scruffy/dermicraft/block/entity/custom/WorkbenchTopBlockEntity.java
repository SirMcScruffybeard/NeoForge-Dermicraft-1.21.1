package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.block.custom.WorkbenchTopBlock;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Workbench's top half -- purely visual/animated (see dermicraft-gear-stations-notes.md ->
 * Construction), no GUI/menu of its own; that stays on the bottom half. Sits closed at rest and
 * only flips open while the bottom's GUI is actually being viewed -- see
 * WorkbenchBlockEntity#onMenuOpened/onMenuClosed, which push the "open" flag here directly since
 * the two halves are always vertically adjacent. Synced the same full-BE-data way
 * MachineBaseBlockEntity's subclasses do (see that class's getUpdatePacket/getUpdateTag), just
 * hand-rolled here since this BE doesn't extend it.
 */
public class WorkbenchTopBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Matches "activate"'s animation_length (1 second) in workbench_top.animation.json -- the item
    // display (see WorkbenchItemDisplayLayer) waits out this same window after opening so it only
    // appears once the swing has actually finished, instead of popping in mid-animation.
    public static final int ACTIVATE_TICKS = 20;

    private boolean open = false;
    // Game-time tick "open" last flipped true, so WorkbenchItemDisplayLayer can tell how far into
    // (or past) the activate animation the current frame is. Only meaningful while open.
    private long openSinceTick = 0;

    public WorkbenchTopBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WORKBENCH_TOP_BE.get(), pos, state);
    }

    public void setOpen(boolean open) {
        if (this.open != open) {
            this.open = open;
            setChanged();
            if (level != null && !level.isClientSide) {
                if (open) {
                    openSinceTick = level.getGameTime();
                }
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                BlockState state = getBlockState();
                if (state.hasProperty(WorkbenchTopBlock.LIT) && state.getValue(WorkbenchTopBlock.LIT) != open) {
                    level.setBlock(getBlockPos(), state.setValue(WorkbenchTopBlock.LIT, open), 3);
                }
            }
        }
    }

    // Client-side read by WorkbenchLightsGlowLayer to gate the top's light-strip glow overlay to
    // exactly the activate/active window -- same rationale as WorkbenchBlockEntity#isOpen.
    public boolean isOpen() {
        return open;
    }

    // Client-side read by WorkbenchItemDisplayLayer to hold the item back until the activate
    // animation has actually finished playing.
    public boolean isPastActivateAnimation() {
        return level != null && level.getGameTime() - openSinceTick >= ACTIVATE_TICKS;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> open
                ? state.setAndContinue(RawAnimation.begin().thenPlay("activate").thenLoop("active"))
                : state.setAndContinue(RawAnimation.begin().thenPlay("deactivate"))));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("open", open);
        tag.putLong("openSinceTick", openSinceTick);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        open = tag.getBoolean("open");
        openSinceTick = tag.getLong("openSinceTick");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        super.onDataPacket(net, pkt, registries);
    }
}
