package net.scruffy.dermicraft.item.custom;

import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.model.GeoModel;

public class EaterItemModel extends GeoModel<EaterItem> {
    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(EaterItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "geo/item/eater.geo.json");
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(EaterItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/eater/eater_base.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EaterItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "animations/item/eater.animation.json");
    }
}
