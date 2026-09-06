package net.scruffy.dermicraft.item.custom;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.scruffy.dermicraft.component.AidModeData;
import net.scruffy.dermicraft.event.AidTargetScanner;
import net.scruffy.dermicraft.main.Dermicraft;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * A.I.D.'s "screen" bone is shared across all four modes (one cube, not per-mode geometry like the
 * tool bones) -- so unlike {@code DrinkerScreenGlowLayer}'s single on-texture-or-nothing, this picks
 * a whole on/off PAIR per mode, close in color to that mode's cycle-message text ({@code AidItem
 * #modeColor}), and always renders one of the two: off is the idle color, on (per
 * {@link AidTargetScanner}) is shown while aiming at something that mode could actually act on.
 *
 * <p>Guarded to the actual held perspective for the ON texture specifically, same reasoning as
 * {@code DrinkerScreenGlowLayer}: {@link AidTargetScanner} only ever describes what the HELD A.I.D.
 * is aiming at, so an A.I.D. sitting in an inventory slot must not borrow that state.
 */
public class AidScreenGlowLayer extends AutoGlowingGeoLayer<AidItem> {

    private static final ResourceLocation FORCEPS_ON =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/aid/frocepts_light_on.png");
    private static final ResourceLocation FORCEPS_OFF =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/aid/frocepts_light_off.png");
    private static final ResourceLocation SCALPEL_ON =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/aid/scalpel_light_on.png");
    private static final ResourceLocation SCALPEL_OFF =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/aid/scalpel_light_off.png");
    private static final ResourceLocation SUTURE_ON =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/aid/suture_light_on.png");
    private static final ResourceLocation SUTURE_OFF =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/aid/suture_light_off.png");
    private static final ResourceLocation SYRINGE_ON =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/aid/syringe_light_on.png");
    private static final ResourceLocation SYRINGE_OFF =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/aid/syringe_light_off.png");

    public AidScreenGlowLayer(GeoRenderer<AidItem> renderer) {
        super(renderer);
    }

    @Override
    protected RenderType getRenderType(AidItem animatable, MultiBufferSource bufferSource) {
        return RenderType.entityTranslucentEmissive(screenTexture());
    }

    private ResourceLocation screenTexture() {
        AidModeData.Mode mode = currentMode();
        boolean on = isHeldInHand() && AidTargetScanner.hasValidTarget();

        return switch (mode) {
            case FORCEPS -> on ? FORCEPS_ON : FORCEPS_OFF;
            case SCALPEL -> on ? SCALPEL_ON : SCALPEL_OFF;
            case SUTURE -> on ? SUTURE_ON : SUTURE_OFF;
            case SYRINGE -> on ? SYRINGE_ON : SYRINGE_OFF;
        };
    }

    private AidModeData.Mode currentMode() {
        if (!(getRenderer() instanceof GeoItemRenderer<?> itemRenderer)) return AidModeData.Mode.FORCEPS;
        ItemStack stack = itemRenderer.getCurrentItemStack();
        if (stack == null || stack.isEmpty()) return AidModeData.Mode.FORCEPS;
        return AidItem.modeData(stack).modeEnum();
    }

    private boolean isHeldInHand() {
        if (!(getRenderer() instanceof AidItemRenderer renderer)) return false;
        return switch (renderer.currentPerspective()) {
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND,
                 THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> true;
            default -> false;
        };
    }
}
