package net.scruffy.dermicraft.item.custom.test;

import net.minecraft.resources.ResourceLocation;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.model.GeoModel;

/**
 * See {@link TestRigItem} -- throwaway, delete once the GeckoLib pipeline is validated.
 *
 * <p>Overrides the single-arg getModelResource/getTextureResource despite them being flagged
 * deprecated in GeckoLib 4.8.4 -- they're still the only abstract methods GeoModel actually
 * requires; the two-arg (T, GeoRenderer) overloads are defaults that delegate back to these.
 */
public class TestRigItemModel extends GeoModel<TestRigItem> {
    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getModelResource(TestRigItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "geo/item/test_rig.geo.json");
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureResource(TestRigItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/test_rig.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TestRigItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "animations/item/test_rig.animation.json");
    }
}
