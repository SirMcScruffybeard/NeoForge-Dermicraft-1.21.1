package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Dock (see dermicraft-gear-worx-notes.md -> Dock). Placeholder pass only -- gets the GeckoLib
 * block model in place with a facing, no Duties/menu/tank wiring yet. The real Dock is planned as
 * a 3x3x3 multiblock with a shared interactive floor and a single model spanning the whole
 * structure (see memory: project_dock_model_plan); this single-block registration exists purely so
 * the in-progress model can be placed and viewed in-world while it's iterated on in Blockbench.
 */
public class DockBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public DockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DOCK_BE.get(), pos, state);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    // No animation yet -- intentionally empty, see class javadoc.
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }
}
