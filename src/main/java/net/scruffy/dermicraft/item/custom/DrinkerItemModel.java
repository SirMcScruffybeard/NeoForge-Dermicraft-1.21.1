package net.scruffy.dermicraft.item.custom;

import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.model.GeoModel;

/** Placeholder -- no animation controllers/game logic yet, see DrinkerItem's class javadoc. */
public class DrinkerItemModel extends GeoModel<DrinkerItem> {
    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(DrinkerItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "geo/item/drinker.geo.json");
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(DrinkerItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/drinker/drinker.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DrinkerItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "animations/item/drinker.animation.json");
    }
}
