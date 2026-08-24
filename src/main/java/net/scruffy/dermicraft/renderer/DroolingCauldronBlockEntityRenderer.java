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
import net.scruffy.dermicraft.block.entity.custom.DroolingCauldronBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.DroolingMachineBlockEntity;


// Credits to TurtyWurty
// Under MIT-License: https://github.com/DaRealTurtyWurty/1.20-Tutorial-Mod?tab=MIT-1-ov-file#readme
// Generalized (2026-08-20) to the shared Drooling-family base so Drooling Crucible can reuse this
// same renderer instead of a near-duplicate class -- nothing here ever depended on which specific
// fluid or machine, only on getFluid()/getTank(), both on DroolingMachineBlockEntity itself.
public class DroolingCauldronBlockEntityRenderer extends TankBlockEntityRenderer implements BlockEntityRenderer<DroolingMachineBlockEntity<?>> {

    public DroolingCauldronBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(DroolingMachineBlockEntity<?> pBlockEntity, float partialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int packedOverlay) {
        Level level = pBlockEntity.getLevel();
        if (level == null) return;

        FluidStack fluidStack = pBlockEntity.getFluid();
        if (!fluidStack.isEmpty()) {
            renderFluidPool(pBlockEntity, fluidStack, level, pPoseStack, pBuffer, pPackedLight);
        }

        // Evolution overlay is Drooling Cauldron-only -- Crucible is already the end state, it never
        // has anything to creep toward. Independent of whether the tank currently has fluid in it
        // (the halt-while-draining dead period still counts as "evolving" from the player's POV).
        // Geometry itself lives on TankBlockEntityRenderer -- shared with
        // EvolutionOverlayBlockEntityRenderer (Masticator/Metastasizer) rather than duplicated per machine.
        if (pBlockEntity instanceof DroolingCauldronBlockEntity cauldron) {
            float progress = cauldron.getEvolutionProgressFraction();
            if (progress > 0) {
                renderEvolutionOverlay(progress, pPoseStack, pBuffer, pPackedLight);
            }
        }
    }

    private void renderFluidPool(DroolingMachineBlockEntity<?> pBlockEntity, FluidStack fluidStack, Level level,
                                  PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
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
