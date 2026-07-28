package net.scruffy.dermicraft.event;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.fluid.ModFluidTypes;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.main.Dermicraft;

/**
 * Ambient chime for Molten Amethyst -- reuses vanilla's own Amethyst Cluster chime, the same
 * "calming" identity already expressed in the fluid's dampened motion scale (see
 * dermicraft-crafting-notes.md -- Molten Amethyst). Triggers near a world fluid block, a tank
 * block entity holding it, or a carried container holding it (bucket, Beaker, Glass Flask,
 * Syringe).
 */
@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class MoltenAmethystChimeEvent {

    private static final int CHECK_INTERVAL_TICKS = 100; // 5 seconds
    private static final int WORLD_SCAN_RADIUS = 5;
    private static final float CHIME_CHANCE = 0.35f;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) return;
        if (level.getGameTime() % CHECK_INTERVAL_TICKS != 0) return;
        if (level.random.nextFloat() > CHIME_CHANCE) return;

        if (isCarryingMoltenAmethyst(player.getInventory()) || isNearMoltenAmethystFluid(level, player.blockPosition())) {
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.AMBIENT, 0.6f, 0.8f + level.random.nextFloat() * 0.4f);
        }
    }

    private static boolean isCarryingMoltenAmethyst(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (stackHoldsMoltenAmethyst(inventory.getItem(slot))) return true;
        }
        return false;
    }

    private static boolean stackHoldsMoltenAmethyst(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() == ModFluids.MOLTEN_AMETHYST_BUCKET.get()) return true;

        FluidData data = stack.getOrDefault(ModDataComponentTypes.FLUID_DATA.get(), FluidData.EMPTY);
        return !data.getFluidStack().isEmpty() && data.getFluid() == ModFluids.SOURCE_MOLTEN_AMETHYST.get();
    }

    private static boolean isNearMoltenAmethystFluid(Level level, BlockPos center) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -WORLD_SCAN_RADIUS; x <= WORLD_SCAN_RADIUS; x++) {
            for (int y = -WORLD_SCAN_RADIUS; y <= WORLD_SCAN_RADIUS; y++) {
                for (int z = -WORLD_SCAN_RADIUS; z <= WORLD_SCAN_RADIUS; z++) {
                    pos.setWithOffset(center, x, y, z);

                    if (level.getFluidState(pos).getFluidType() == ModFluidTypes.MOLTEN_AMETHYST_FLUID_TYPE.get()) {
                        return true;
                    }

                    if (tankHoldsMoltenAmethyst(level, pos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Placed fluid blocks are covered by the fluid-state check above -- this only needs to look at
     * block entities exposing a fluid tank (Skin/Chitin Tank, machine input/result tanks, etc.). */
    private static boolean tankHoldsMoltenAmethyst(Level level, BlockPos pos) {
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        if (handler == null) return false;

        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack stack = handler.getFluidInTank(tank);
            if (!stack.isEmpty() && stack.getFluid() == ModFluids.SOURCE_MOLTEN_AMETHYST.get()) {
                return true;
            }
        }
        return false;
    }
}
