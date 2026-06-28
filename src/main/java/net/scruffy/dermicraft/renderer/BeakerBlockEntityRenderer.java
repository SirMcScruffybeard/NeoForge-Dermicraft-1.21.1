package net.scruffy.dermicraft.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.scruffy.dermicraft.block.entity.custom.BeakerBlockEntity;


// Credits to TurtyWurty
// Under MIT-License: https://github.com/DaRealTurtyWurty/1.20-Tutorial-Mod?tab=MIT-1-ov-file#readme
public class BeakerBlockEntityRenderer extends TankBlockEntityRenderer implements BlockEntityRenderer<BeakerBlockEntity> {

    public BeakerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BeakerBlockEntity pBlockEntity, float partialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int packedOverlay) {
        FluidStack fluidStack = pBlockEntity.getFluid();
        if (fluidStack.isEmpty())
            return;

        Level level = pBlockEntity.getLevel();
        if (level == null)
            return;

        BlockPos pos = pBlockEntity.getBlockPos();

        IClientFluidTypeExtensions fluidTypeExtensions = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        ResourceLocation stillTexture = fluidTypeExtensions.getStillTexture(fluidStack);

        FluidState state = fluidStack.getFluid().defaultFluidState();

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
        int tintColor = fluidTypeExtensions.getTintColor(state, level, pos);

        float height = (((float) pBlockEntity.getTank(null).getFluidInTank(0).getAmount() / pBlockEntity.getTank(null).getTankCapacity(0)) * 0.625f) + 0.25f;

        VertexConsumer builder = pBuffer.getBuffer(ItemBlockRenderTypes.getRenderLayer(state));

        drawDefaultTop(builder, pPoseStack, height, sprite, pPackedLight, tintColor);

        drawDefaultBottom(builder, pPoseStack, height, sprite, pPackedLight, tintColor);

        drawDefaultSides(builder, pPoseStack, height, sprite, pPackedLight, tintColor);

    }

}
