package net.scruffy.dermicraft.item.custom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.component.SippingModeData;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Storage/Disposal lights are mutually exclusive, so rather than swap the whole base texture per
 * mode (AutoGlowingGeoLayer's default "<texture>_glowmask" convention), this points directly at
 * whichever "_lit" mask matches the current mode -- only one light's pixels are unmasked per file.
 * While armed (Storage -> Disposal confirm window), the lights alternate instead of holding steady.
 */
public class SippingGlowLayer extends AutoGlowingGeoLayer<SippingItem> {

    private static final ResourceLocation STORAGE_LIT =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/sipping/sipping_storage_lit.png");
    private static final ResourceLocation DISPOSAL_LIT =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/sipping/sipping_disposal_lit.png");

    public SippingGlowLayer(GeoRenderer<SippingItem> renderer) {
        super(renderer);
    }

    @Override
    protected RenderType getRenderType(SippingItem animatable, MultiBufferSource bufferSource) {
        ItemStack stack = getRenderer() instanceof GeoItemRenderer<?> itemRenderer
                ? itemRenderer.getCurrentItemStack() : ItemStack.EMPTY;
        SippingModeData mode = stack.getOrDefault(ModDataComponentTypes.SIPPING_MODE_DATA.get(), SippingModeData.DEFAULT);

        boolean showDisposalLight;
        if (mode.armed()) {
            Level level = Minecraft.getInstance().level;
            long time = level != null ? level.getGameTime() : 0L;
            showDisposalLight = (time / 3) % 2 == 0;
        } else {
            showDisposalLight = mode.disposalMode();
        }

        return RenderType.entityTranslucentEmissive(showDisposalLight ? DISPOSAL_LIT : STORAGE_LIT);
    }
}
