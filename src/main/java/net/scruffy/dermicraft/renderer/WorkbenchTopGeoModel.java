package net.scruffy.dermicraft.renderer;

import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.block.entity.custom.WorkbenchTopBlockEntity;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.model.GeoModel;

public class WorkbenchTopGeoModel extends GeoModel<WorkbenchTopBlockEntity> {

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(WorkbenchTopBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "geo/block/workbench_top.geo.json");
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(WorkbenchTopBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/block/workbench/workbench_top.png");
    }

    @Override
    public ResourceLocation getAnimationResource(WorkbenchTopBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "animations/block/workbench/workbench_top.animation.json");
    }
}
