package net.scruffy.dermicraft.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.block.entity.custom.SkinTankBlockEntity;


// Credits to TurtyWurty
// Under MIT-License: https://github.com/DaRealTurtyWurty/1.20-Tutorial-Mod?tab=MIT-1-ov-file#readme
public class TankBlockEntityRenderer {

    public TankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {

    }

    protected static void drawDefaultTop(VertexConsumer builder, PoseStack poseStack, float height, TextureAtlasSprite sprite, int packedLight, int tintColor) {
        drawQuad(builder, poseStack, 0.1f, height, 0.1f, 0.9f, height, 0.9f, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), packedLight, tintColor);
        drawQuad(builder, poseStack, 0.1f, 0, 0.1f, 0.9f, height, 0.1f, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), packedLight, tintColor);
    }

    protected static void drawDefaultBottom(VertexConsumer builder, PoseStack poseStack, float height, TextureAtlasSprite sprite, int packedLight, int tintColor) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        poseStack.translate(0, -0.9f, -1f);
        drawQuad(builder, poseStack, 0.1f, height, 0.1f, 0.9f, height, 0.9f, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), packedLight, tintColor);
        poseStack.popPose();
    }

    protected static void drawDefaultSides(VertexConsumer builder, PoseStack poseStack, float height, TextureAtlasSprite sprite, int packedLight, int tintColor) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        poseStack.translate(-1f, 0, -1.8f);
        drawQuad(builder, poseStack, 0.1f, 0, 0.9f, 0.9f, height, 0.9f, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), packedLight, tintColor);
        poseStack.popPose();


        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        poseStack.translate(-1f, 0, 0);
        drawQuad(builder, poseStack, 0.1f, 0, 0.1f, 0.9f, height, 0.1f, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), packedLight, tintColor);
        poseStack.popPose();


        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(90));
        poseStack.translate(0, 0, -1f);
        drawQuad(builder, poseStack, 0.1f, 0, 0.1f, 0.9f, height, 0.1f, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), packedLight, tintColor);
        poseStack.popPose();
    }


    protected static void drawVertex(VertexConsumer builder, PoseStack poseStack, float x, float y, float z, float u, float v, int packedLight, int color) {
        builder.addVertex(poseStack.last().pose(), x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setLight(packedLight)
                .setNormal(1, 0, 0);
    }

    protected static void drawQuad(VertexConsumer builder, PoseStack poseStack, float x0, float y0, float z0, float x1, float y1, float z1, float u0, float v0, float u1, float v1, int packedLight, int color) {
        drawVertex(builder, poseStack, x0, y0, z0, u0, v0, packedLight, color);
        drawVertex(builder, poseStack, x0, y1, z1, u0, v1, packedLight, color);
        drawVertex(builder, poseStack, x1, y1, z1, u1, v1, packedLight, color);
        drawVertex(builder, poseStack, x1, y0, z0, u1, v0, packedLight, color);
    }
}

