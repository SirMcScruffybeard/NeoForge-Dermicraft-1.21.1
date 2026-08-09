package net.scruffy.dermicraft.renderer;

import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.block.entity.custom.WorkbenchBlockEntity;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.model.GeoModel;

public class WorkbenchBottomGeoModel extends GeoModel<WorkbenchBlockEntity> {

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(WorkbenchBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "geo/block/workbench_bottom.geo.json");
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(WorkbenchBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/block/workbench/workbench_bottom.png");
    }

    @Override
    public ResourceLocation getAnimationResource(WorkbenchBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "animations/block/workbench/workbench_bottom.animation.json");
    }
}
