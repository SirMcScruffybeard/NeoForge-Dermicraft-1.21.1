package net.scruffy.dermicraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.item.custom.BeakerItem;
import net.scruffy.dermicraft.item.custom.GlassFlaskItem;
import net.scruffy.dermicraft.item.custom.SyringeItem;

import java.util.ArrayList;
import java.util.List;

public class ModItemUtil {
    public static void giveItem(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public static void giveItems(Player player, List<ItemStack> stackList) {
        for (ItemStack dropItem : stackList) {
            giveItem(player, dropItem);
        }
    }

    /*********************************************************************************************************
     *
     * @param level
     * @param player
     * @param pos
     * @param state
     * @param tool
     * Gives the player the items dropped from a block entity instead of dropping it on the ground
     *********************************************************************************************************/
    public static void giveDrops(Level level, Player player, BlockPos pos, BlockState state, ItemStack tool) {
        List<ItemStack> drops = Block.getDrops(state, (ServerLevel) level, pos, level.getBlockEntity(pos), player, tool);

        giveItems(player, drops);
    }

    public static ItemStack buildFlaskStack(Fluid fluid) {
        ItemStack stack = new ItemStack(ModItems.GLASS_FLASK.get());
        stack.set(ModDataComponentTypes.FLUID_DATA.get(), FluidData.createData(fluid, GlassFlaskItem.CAPACITY));
        return stack;
    }

    public static ItemStack buildSyringeStack(Fluid fluid) {
        ItemStack stack = new ItemStack(ModItems.SYRINGE.get());
        stack.set(ModDataComponentTypes.FLUID_DATA.get(), FluidData.createData(fluid, SyringeItem.CAPACITY));
        return stack;
    }

    public static ItemStack buildBeakerStack(Fluid fluid) {
        ItemStack stack = new ItemStack(ModBlocks.BEAKER_ITEM.get());
        stack.set(ModDataComponentTypes.FLUID_DATA.get(), FluidData.createData(fluid, BeakerItem.CAPACITY));
        return stack;
    }

    public static List<ItemStack> snapshotInventory(ItemStackHandler inventory) {
        List<ItemStack> snapshot = new ArrayList<>();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                snapshot.add(stack.copy());
            }
        }
        return snapshot;
    }

    public static void restoreInventory(ItemStackHandler inventory, List<ItemStack> snapshot) {
        for (int i = 0; i < snapshot.size(); i++) {
            inventory.setStackInSlot(i, snapshot.get(i));
        }
    }
}
