package net.scruffy.dermicraft.item.custom;

import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.model.GeoModel;

public class SippingItemModel extends GeoModel<SippingItem> {
    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(SippingItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "geo/item/sipping.geo.json");
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(SippingItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/sipping/sipping_base.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SippingItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "animations/item/sipping/sipping.animation.json");
    }
}
