package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Workbench's top half -- purely visual/animated this pass (see dermicraft-gear-stations-notes.md
 * -> Construction), no GUI/menu of its own; that stays on the bottom half. Just holds a live-loop
 * "active" animation for now so the two GeckoLib models can be checked for alignment in-world --
 * no pairing/activate-deactivate triggering with the bottom yet.
 */
public class WorkbenchTopBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public WorkbenchTopBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WORKBENCH_TOP_BE.get(), pos, state);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("active"))));
    }
}
