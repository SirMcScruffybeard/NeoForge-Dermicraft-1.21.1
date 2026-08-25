package net.scruffy.dermicraft.property;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.HeldItemData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.item.custom.BladderItem;
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

        ItemProperties.register(ModItems.PRIMITIVE_SYRINGE.get(),
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

        // I.D.E.P.'s two independent boolean indicator lights -- not a fill-level gauge like the
        // fluid containers above, just held/not-held for each of its two internal stores.
        ItemProperties.register(ModItems.IDEP.get(),
                getResourceLocation("fluid_held"),
                (stack, level, entity, seed) -> {
                    FluidData data = stack.getOrDefault(getFluidDataType(), FluidData.EMPTY);
                    return data.isFluidEmpty() ? 0 : 1;
                });

        ItemProperties.register(ModItems.IDEP.get(),
                getResourceLocation("item_held"),
                (stack, level, entity, seed) -> {
                    HeldItemData data = stack.getOrDefault(ModDataComponentTypes.HELD_ITEM_DATA.get(), HeldItemData.EMPTY);
                    return data.isEmpty() ? 0 : 1;
                });

        // Bladder family: empty / half / full -- three fill-level textures, thresholds now expressed
        // as fractions of each item's OWN capacity (see registerBladderFillProperty) rather than fixed
        // mB, so the same registration works unchanged for Charred Bladder's larger capacity too.
        registerBladderFillProperty(ModItems.BLADDER.get());
        registerBladderFillProperty(ModItems.FUEL_BLADDER.get());
        registerBladderFillProperty(ModItems.FEEDER_BLADDER.get());
        registerBladderFillProperty(ModItems.CHARRED_BLADDER.get());
        registerBladderFillProperty(ModItems.CHARRED_FUEL_BLADDER.get());
        registerBladderFillProperty(ModItems.CHARRED_FEEDER_BLADDER.get());
    }

    /**
     * Empty below 25% of the item's own capacity, half below 75%, full at/above -- matches the base
     * Bladder's original fixed 500/1500 mB thresholds exactly at its 2000 mB capacity, but now scales
     * with whatever {@link BladderItem#getCapacity()} the specific item reports, so a Charred Bladder
     * (or any future capacity tier) reuses this one registration instead of needing its own copy with
     * hand-recalculated absolute thresholds.
     */
    private static void registerBladderFillProperty(Item item) {
        int capacity = item instanceof BladderItem bladder ? bladder.getCapacity() : BladderItem.CAPACITY;

        ItemProperties.register(item,
                getResourceLocation("full"),
                (stack, level, entity, seed) -> {
                    FluidData data = stack.getOrDefault(getFluidDataType(), FluidData.EMPTY);
                    if (data.isFluidEmpty() || data.getFluidAmount() < capacity * 0.25f) return 0;
                    if (data.getFluidAmount() < capacity * 0.75f) return 1;
                    return 2;
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
