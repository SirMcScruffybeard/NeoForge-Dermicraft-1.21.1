package net.scruffy.dermicraft.item.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.scruffy.dermicraft.client.GadgetBeamTracker;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.main.Dermicraft;
import org.jetbrains.annotations.Nullable;

import org.joml.Matrix4f;

/**
 * Draws Eater's mining beam in world space, entirely outside GeckoLib's own item-rendering
 * pipeline -- see the beam design discussion for why: {@link software.bernie.geckolib.renderer.GeoItemRenderer}'s
 * render call chain never hands the layer the holding entity, for anyone, local or remote, so
 * there's no way to know "whose beam is this" from inside a {@code GeoRenderLayer} without adding
 * mixin infrastructure this project doesn't have. Hooking {@link RenderLevelStageEvent} sidesteps
 * that entirely -- it hands over the level and every renderable player directly.
 *
 * <p>Beam origin is a deliberate APPROXIMATION: eye position offset forward/down along the look
 * vector, not the modeled {@code emitter} bone's true live position -- getting the real bone
 * transform would need the same per-entity render context this approach exists to avoid needing.
 * Traded precision (no idle-animation wobble on the beam) for correctness (no mixins, works for
 * every player, not just the render-call's mystery holder). See the design discussion -- the user
 * explicitly signed off on this trade.
 *
 * <p>Beam visibility condition mirrors {@link EaterItem#aggregateTick} exactly: only shows while
 * there's a real Aggregate-tagged block being mined, not just "trigger held" -- an idle vacuum
 * pulling loose items shouldn't sprout a beam.
 */
@EventBusSubscriber(modid = Dermicraft.MOD_ID, value = Dist.CLIENT)
public class EaterBeamRenderer {

    private static final ResourceLocation FRAME_1 =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/eater/eater_mining_beam_0001.png");
    private static final ResourceLocation FRAME_2 =
            ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "textures/item/eater/eater_mining_beam_0002.png");

    /** Ticks each flipbook frame holds before swapping to the other -- purely cosmetic pacing, see
     * the design discussion ("the timing of the pattern doesn't matter"). */
    private static final int FRAME_HOLD_TICKS = 4;

    private static final float BEAM_HALF_WIDTH = 0.06F;

    /** Offsets from eye position approximating the modeled emitter's position -- tuned by eye
     * against the model, not read from the live bone (see class javadoc). {@code RIGHT} is signed:
     * positive is the player's right (held item's typical side), flip the sign if it lands on the
     * wrong side instead of re-deriving it. */
    private static final double EMITTER_FORWARD_OFFSET = 0.5;
    private static final double EMITTER_DOWN_OFFSET = 0.04;
    private static final double EMITTER_RIGHT_OFFSET = 0.3;

    private static final int FULL_BRIGHT = LightTexture.pack(15, 15);

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        boolean framePrimary = (level.getGameTime() / FRAME_HOLD_TICKS) % 2 == 0;
        // entityTranslucentEmissive is texture-parameterized (unlike RenderType.translucent(), which
        // is a fixed instance bound to the block atlas) -- same RenderType family already proven
        // working in this codebase for a standalone PNG, see EaterGlowLayer/WorkbenchLightsGlowLayer.
        RenderType renderType = RenderType.entityTranslucentEmissive(framePrimary ? FRAME_1 : FRAME_2);

        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(renderType);

        for (Player player : level.players()) {
            Vec3 target = beamTargetFor(mc, level, player);
            if (target == null) continue;

            Vec3 eye = player.getEyePosition(partialTick);
            Vec3 look = player.getViewVector(partialTick);
            // Player's right-hand direction: perpendicular to both look and world-up. Without this
            // term the beam sat dead-center on the look vector regardless of offset tuning, which is
            // why it was reading as "bottom center of view" instead of coming from the held item.
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 start = eye.add(look.scale(EMITTER_FORWARD_OFFSET))
                    .subtract(0, EMITTER_DOWN_OFFSET, 0)
                    .add(right.scale(EMITTER_RIGHT_OFFSET));

            poseStack.pushPose();
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
            drawBeamQuad(buffer, poseStack, start, target, camPos);
            poseStack.popPose();
        }

        bufferSource.endBatch(renderType);
    }

    @Nullable
    private static Vec3 beamTargetFor(Minecraft mc, Level level, Player player) {
        ItemStack stack = beamStack(player);
        if (stack == null) return null;

        if (player == mc.player) {
            // Local player -- predict instantly via our own raycast, no packet round-trip (see class
            // javadoc / design discussion for why this differs from every other player below).
            // Requires Beam Module; any block aggregateTarget then accepts counts (mirrors
            // EaterItem#inventoryTick's server-side gate -- an Aggregate-tagged hit only ever comes
            // back when Aggregate is ALSO installed, so this single check already covers "beam shows
            // for Aggregate targets too, but only with both Modules").
            if (!EaterItem.hasModule(stack, ModTags.Items.MODULE_BEAM)) return null;
            BlockHitResult hit = EaterItem.aggregateTarget(level, player, stack);
            return hit == null ? null : hit.getLocation();
        }

        // Everyone else -- whatever the server last told us about this entity's beam (see
        // GadgetBeamTracker).
        return GadgetBeamTracker.targetFor(player.getId());
    }

    /** The stack actually driving the beam right now: whichever hand holds an actively-vacuuming
     * Eater. Both hands checked since either could theoretically hold one, though only one is ever
     * realistically active at a time. */
    @Nullable
    private static ItemStack beamStack(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof EaterItem && main.getOrDefault(ModDataComponentTypes.EATER_VACUUMING.get(), false)) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof EaterItem && off.getOrDefault(ModDataComponentTypes.EATER_VACUUMING.get(), false)) {
            return off;
        }
        return null;
    }

    /** Camera-facing strip from {@code start} to {@code end} -- perpendicular to both the beam
     * direction and the direction toward the camera, so the quad always presents its face to the
     * viewer regardless of beam orientation (the standard "billboarded beam" technique). One texture
     * repeat per block of length, so the flipbook stripe pattern reads at a consistent scale
     * regardless of how far the target block is. */
    private static void drawBeamQuad(VertexConsumer buffer, PoseStack poseStack, Vec3 start, Vec3 end, Vec3 camPos) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-4) return;
        Vec3 dir = delta.scale(1.0 / length);

        Vec3 towardCamera = camPos.subtract(start.add(end).scale(0.5));
        Vec3 right = dir.cross(towardCamera);
        if (right.lengthSqr() < 1.0E-6) right = dir.cross(new Vec3(0, 1, 0));
        right = right.normalize().scale(BEAM_HALF_WIDTH);

        Vec3 a = start.subtract(right);
        Vec3 b = start.add(right);
        Vec3 c = end.add(right);
        Vec3 d = end.subtract(right);

        float uLength = (float) length;
        int color = 0xFFFFFFFF;
        Matrix4f pose = poseStack.last().pose();

        // entityTranslucentEmissive uses the NEW_ENTITY vertex format (adds overlay + normal on top
        // of color/uv/light) -- setOverlay/setNormal are always safe to call even against a format
        // that doesn't need them (VertexConsumer's builder setters no-op for elements the bound
        // format lacks), so set them explicitly rather than relying on undefined defaults.
        buffer.addVertex(pose, (float) a.x, (float) a.y, (float) a.z).setColor(color).setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0, 1, 0);
        buffer.addVertex(pose, (float) b.x, (float) b.y, (float) b.z).setColor(color).setUv(0, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0, 1, 0);
        buffer.addVertex(pose, (float) c.x, (float) c.y, (float) c.z).setColor(color).setUv(uLength, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0, 1, 0);
        buffer.addVertex(pose, (float) d.x, (float) d.y, (float) d.z).setColor(color).setUv(uLength, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
