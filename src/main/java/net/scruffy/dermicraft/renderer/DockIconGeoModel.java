package net.scruffy.dermicraft.renderer;

import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.block.entity.custom.DockBlockEntity;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.model.GeoModel;

/**
 * Dock's item-form (inventory/hand/ground) model -- a separate, much smaller geometry from the
 * full in-world dock.geo.json, since Blockbench refuses to generate a display file for a model
 * that large. Reuses DockBlockEntity as the animatable carrier purely so this model class has
 * something to key off of (same trick WorkbenchBottomItemRenderer uses with a throwaway BE), not
 * because the icon shares any geometry with the real block model.
 */
public class DockIconGeoModel extends GeoModel<DockBlockEntity> {

    @Override
    public ResourceLocation getModelResource(DockBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "geo/item/dock_icon.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DockBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/dock/dock_icon.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DockBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "animations/item/dock/dock_icon.animation.json");
    }
}
