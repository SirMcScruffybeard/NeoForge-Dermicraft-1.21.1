package net.scruffy.dermicraft.item.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.scruffy.dermicraft.component.DrinkerModeData;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Storage/Transfer/Disposal lit textures are mutually exclusive, same approach as SIPPING's
 * {@link SippingGlowLayer} -- points directly at whichever full lit variant matches the current
 * mode rather than the default "<texture>_glowmask" convention.
 *
 * <p>Reads each rendered stack's own mode, so a DRINKER in the inventory shows its real setting
 * rather than mirroring whatever the held one is doing.
 */
public class DrinkerGlowLayer extends AutoGlowingGeoLayer<DrinkerItem> {

    private static final ResourceLocation STORAGE_LIT =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/drinker/drinker_storage.png");
    private static final ResourceLocation TRANSFER_LIT =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/drinker/drinker_transfer.png");
    private static final ResourceLocation DISPOSAL_LIT =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/drinker/drinker_disposal.png");

    public DrinkerGlowLayer(GeoRenderer<DrinkerItem> renderer) {
        super(renderer);
    }

    @Override
    protected RenderType getRenderType(DrinkerItem animatable, MultiBufferSource bufferSource) {
        ResourceLocation lit = switch (currentMode()) {
            case DISPOSAL -> DISPOSAL_LIT;
            case TRANSFER -> TRANSFER_LIT;
            case STORAGE -> STORAGE_LIT;
        };
        return RenderType.entityTranslucentEmissive(lit);
    }

    /**
     * While a destructive action is armed, the current mode's lights blink instead of holding
     * steady -- a glanceable "something is pending" tell to go with the action-bar warning, and the
     * same idea S.I.P.P.I.N.G. uses for its own confirm window.
     */
    @Override
    public void render(PoseStack poseStack, DrinkerItem animatable, BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (isArmed() && !flashVisible()) return;
        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
    }

    private boolean isArmed() {
        if (!(getRenderer() instanceof GeoItemRenderer<?> itemRenderer)) return false;
        ItemStack stack = itemRenderer.getCurrentItemStack();
        return stack != null && !stack.isEmpty() && DrinkerItem.modeData(stack).armed();
    }

    /** Deliberately quick -- this is a warning, not an idle shimmer. */
    private static boolean flashVisible() {
        Level level = Minecraft.getInstance().level;
        return level == null || (level.getGameTime() / 3) % 2 == 0;
    }

    private DrinkerModeData.Mode currentMode() {
        if (!(getRenderer() instanceof GeoItemRenderer<?> itemRenderer)) return DrinkerModeData.Mode.STORAGE;
        ItemStack stack = itemRenderer.getCurrentItemStack();
        if (stack == null || stack.isEmpty()) return DrinkerModeData.Mode.STORAGE;
        return DrinkerItem.modeData(stack).mode();
    }
}
