package net.scruffy.dermicraft.renderer;

import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.block.entity.custom.DockBlockEntity;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.model.GeoModel;

public class DockGeoModel extends GeoModel<DockBlockEntity> {

    @Override
    public ResourceLocation getModelResource(DockBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "geo/block/dock.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DockBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/block/dock/dock.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DockBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "animations/block/dock/dock.animation.json");
    }
}
