package net.scruffy.dermicraft.item.custom.test;

import software.bernie.geckolib.renderer.GeoItemRenderer;

/** See {@link TestRigItem} -- throwaway, delete once the GeckoLib pipeline is validated. */
public class TestRigItemRenderer extends GeoItemRenderer<TestRigItem> {
    public TestRigItemRenderer() {
        super(new TestRigItemModel());
    }
}
