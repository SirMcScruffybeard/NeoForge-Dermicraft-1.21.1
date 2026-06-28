package net.scruffy.dermicraft.property;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;

public class ModItemProperties {
    public static void addCustomItemProperties() {

        ItemProperties.register(ModBlocks.OUTERFACE.asItem(),
                getResourceLocation("see"),
                (stack, level, entity, seed) -> {
                    if (entity instanceof Player player) {
                        HitResult hit = player.pick(5.0D, 0.0F, false);
                        if (hit.getType() == HitResult.Type.BLOCK) {
                            BlockHitResult blockHit = (BlockHitResult) hit;
                            if (level.getBlockState(blockHit.getBlockPos()).is(ModTags.Blocks.HAS_SCREEN)) {
                                return 1.0f;
                            }
                        }
                    }
                    return 0.0f;
                });

        ItemProperties.register(ModItems.SYRINGE.get(),
                getResourceLocation("full"),
                (stack, level, entity, seed) -> {
                    FluidData data = stack.getOrDefault(getFluidDataType(), FluidData.EMPTY);
                    if (data.getFluidType() == Fluids.LAVA.getFluidType()) return 3;
                    if (data.getFluidStack().is(ModTags.Fluids.THIN)) return 1;
                    if (data.getFluidStack().is(ModTags.Fluids.THICK)) return 2;
                    if (!data.isFluidEmpty()) return 1;
                    return 0;
                });

        ItemProperties.register(ModItems.GLASS_FLASK.get(),
                getResourceLocation("full"),
                (stack, level, entity, seed) -> {
                    FluidData data = stack.getOrDefault(getFluidDataType(), FluidData.EMPTY);
                    if (data.getFluidType() == Fluids.LAVA.getFluidType()) return 3;
                    if (data.getFluidStack().is(ModTags.Fluids.THIN)) return 1;
                    if (data.getFluidStack().is(ModTags.Fluids.THICK)) return 2;
                    if (!data.isFluidEmpty()) return 1;
                    return 0;
                });

        ItemProperties.register(ModBlocks.BEAKER_ITEM.get(),
                getResourceLocation("full"),
                (stack, level, entity, seed) -> {
                    FluidData data = stack.getOrDefault(getFluidDataType(), FluidData.EMPTY);
                    if (data.getFluidType() == Fluids.LAVA.getFluidType()) return 3;
                    if (data.getFluidStack().is(ModTags.Fluids.THIN)) return 1;
                    if (data.getFluidStack().is(ModTags.Fluids.THICK)) return 2;
                    if (!data.isFluidEmpty()) return 1;
                    return 0;
                });
    }

    //////////////HelperMethods\\\\\\\\\\\\\\
    private static ResourceLocation getResourceLocation(String path) {
        return ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, path);
    }

    private static DataComponentType<FluidData> getFluidDataType() {
        return ModDataComponentTypes.FLUID_DATA.get();
    }

}
