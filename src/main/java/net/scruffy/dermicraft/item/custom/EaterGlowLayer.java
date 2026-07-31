package net.scruffy.dermicraft.item.custom;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.DrinkerModeData;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Storage/Transfer/Disposal mode lights -- same mutually-exclusive full-texture-swap shape as
 * {@link DrinkerGlowLayer}. No arm/confirm flash here: unlike DRINKER's Disposal (a single
 * destructive action on banked contents that needs a confirm window), Eater's Disposal is just a
 * continuous per-tick routing choice, nothing to arm.
 */
public class EaterGlowLayer extends AutoGlowingGeoLayer<EaterItem> {

    private static final ResourceLocation STORAGE_LIT =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/eater/eater_storage.png");
    private static final ResourceLocation TRANSFER_LIT =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/eater/eater_transfer.png");
    private static final ResourceLocation DISPOSAL_LIT =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/eater/eater_disposal.png");

    public EaterGlowLayer(GeoRenderer<EaterItem> renderer) {
        super(renderer);
    }

    @Override
    protected RenderType getRenderType(EaterItem animatable, MultiBufferSource bufferSource) {
        ResourceLocation lit = switch (currentMode()) {
            case DISPOSAL -> DISPOSAL_LIT;
            case TRANSFER -> TRANSFER_LIT;
            case STORAGE -> STORAGE_LIT;
        };
        return RenderType.entityTranslucentEmissive(lit);
    }

    private DrinkerModeData.Mode currentMode() {
        if (!(getRenderer() instanceof GeoItemRenderer<?> itemRenderer)) return DrinkerModeData.Mode.STORAGE;
        ItemStack stack = itemRenderer.getCurrentItemStack();
        if (stack == null || stack.isEmpty()) return DrinkerModeData.Mode.STORAGE;
        return EaterItem.modeData(stack).mode();
    }
}
