package net.scruffy.dermicraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.custom.duct.AbstractInnardsDuctBlock;
import net.scruffy.dermicraft.block.custom.duct.AbstractNodeBlock;
import net.scruffy.dermicraft.block.custom.duct.DuctRunResolver;
import net.scruffy.dermicraft.block.custom.gate.GatePortBlock;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.item.custom.BeakerItem;
import net.scruffy.dermicraft.item.custom.GlassFlaskItem;
import net.scruffy.dermicraft.item.custom.SyringeItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    //////////External Transfer Methods\\\\\\\\\\
    // Mirrors ModFluidUtil.pushFluidToBelowNeighbour/pushFluidToNeighbour exactly -- same Node
    // bypass guard, same machine-direct duct-drain path (bounded to the 3x3 footprint below).
    public static void pushItemToBelowNeighbour(Level level, BlockPos worldPosition, ItemStackHandler inventory, int slot) {
        pushItemToNeighbour(level, worldPosition, inventory, slot, Direction.DOWN);
    }

    /**
     * Restricted variant of {@link #pushItemToBelowNeighbour} for bulk storage: only auto-ejects
     * into an Innards Duct or a Gate Port, never into arbitrary containers.
     *
     * <p>Storage differs from a machine here. A machine's result slot should hand off to whatever
     * is below it, but a storage block that drains into any container underneath stops being
     * storage -- a Craw couldn't sit on a chest without emptying itself. Limiting the target to
     * deliberate transport blocks keeps auto-output opt-in: you get it by plumbing for it.
     */
    public static void pushItemToBelowTransport(Level level, BlockPos worldPosition, ItemStackHandler inventory, int slot) {
        Block below = level.getBlockState(worldPosition.below()).getBlock();
        if (!(below instanceof AbstractInnardsDuctBlock) && !(below instanceof GatePortBlock)) return;
        pushItemToNeighbour(level, worldPosition, inventory, slot, Direction.DOWN);
    }

    public static void pushItemToNeighbour(Level level, BlockPos worldPosition, ItemStackHandler inventory, int slot, Direction direction) {
        BlockPos neighborPos = worldPosition.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);

        // A Node is never a passive auto-push target -- see ModFluidUtil.pushFluidToNeighbour for
        // the full rationale. Applies identically to items: a Node's buffer slot is only ever
        // filled by its own IN leg + Item toggle, never by an adjacent block pushing into it.
        if (neighborState.getBlock() instanceof AbstractNodeBlock) return;

        if (direction == Direction.DOWN && neighborState.getBlock() instanceof AbstractInnardsDuctBlock) {
            pushItemThroughDuct(level, worldPosition, inventory, slot);
            return;
        }

        Direction hitSide = direction.getOpposite();
        IItemHandler neighborHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, hitSide);
        if (neighborHandler == null) return;
        pushStack(inventory, slot, neighborHandler);
    }

    private static void pushItemThroughDuct(Level level, BlockPos worldPosition, ItemStackHandler inventory, int slot) {
        Optional<DuctRunResolver.Endpoint> endpointOpt =
                DuctRunResolver.resolveWithinFootprint(level, worldPosition, Direction.DOWN, DuctRunResolver.DRAIN_MAX_HOPS);
        if (endpointOpt.isEmpty()) return;
        DuctRunResolver.Endpoint endpoint = endpointOpt.get();

        // The run may end at a Node -- never push into one, it must pull deliberately via its own
        // leg config (items aren't tier-gated, so no fluidFilter check needed here).
        if (level.getBlockState(endpoint.pos()).getBlock() instanceof AbstractNodeBlock) return;

        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, endpoint.pos(), endpoint.accessDirection());
        if (handler == null) return;
        pushStack(inventory, slot, handler);
    }

    private static void pushStack(ItemStackHandler inventory, int slot, IItemHandler target) {
        ItemStack current = inventory.getStackInSlot(slot);
        if (current.isEmpty()) return;
        ItemStack leftover = ItemHandlerHelper.insertItemStacked(target, current, false);
        inventory.setStackInSlot(slot, leftover);
    }
}
