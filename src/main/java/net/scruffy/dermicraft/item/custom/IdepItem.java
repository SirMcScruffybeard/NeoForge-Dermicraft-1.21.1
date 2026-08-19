package net.scruffy.dermicraft.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import net.scruffy.dermicraft.block.entity.custom.GateBufferBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.GateControllerBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.NodeBlockEntity;
import net.scruffy.dermicraft.component.HeldItemData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.interfaces.Channel;
import net.scruffy.dermicraft.interfaces.IHasChannels;
import net.scruffy.dermicraft.interfaces.IHaveFluidData;
import net.scruffy.dermicraft.item.custom.base.ToolItem;
import net.scruffy.dermicraft.util.ModFluidUtil;

import java.util.List;

/**
 * I.D.E.P. -- Innards Diagnostic and Extraction Probe. A GUI-less maintenance tool for the Innards
 * Gate system: read status on Gate blocks and machine faces, and force-clear a jammed Buffer by
 * pulling its contents into the tool's own small internal storage.
 *
 * <p>Holds one item stack ({@link HeldItemData}) and fluid (the same {@link net.scruffy.dermicraft.component.FluidData}
 * mechanism Beaker/Flask/Syringe already use), each capacity-matched to a Gate Buffer's own capacity
 * so a single jam-clear always fits in one go.
 *
 * <p>Interactions:
 * <ul>
 *   <li>Right-click a Buffer -- short status (assigned channel, empty/full).
 *   <li>Sneak + right-click a Buffer -- jam-clear: drains its contents into the tool. Does NOT touch
 *       the Buffer's channel assignment -- it just resumes servicing the same channel once empty.
 *   <li>Sneak + right-click a Node -- same jam-clear, against its transport buffer slot + tank (never
 *       the GUI-only fluid-handler slot, same as duct/Node automation already respects). Does NOT
 *       touch its per-leg direction/toggle config. A plain (non-sneak) right-click on a Node is left
 *       alone -- PASS lets its own menu open exactly as it would without the tool in hand.
 *   <li>Right-click a Controller -- which channels are currently unserviced (waiting on a Buffer).
 *   <li>Right-click any {@link IHasChannels} machine face -- what that face actually connects to
 *       ({@link IHasChannels#describeFace}), independent of Gate/self-filter state.
 *   <li>Right-click any other fluid-handler block (a tank) -- deposit the tool's stored fluid into it.
 *   <li>Right-click on nothing -- dump the held item into the player's inventory (or drop it), and try
 *       to auto-fill the first compatible EMPTY fluid container already in the player's inventory.
 *       Uses the standard simulate-first pattern, so a fixed-capacity container (bucket, Glass Flask)
 *       is only ever attempted when the tool holds enough to fill it completely in one shot.
 * </ul>
 */
public class IdepItem extends ToolItem implements IHaveFluidData {

    public static final int ITEM_CAPACITY = GateBufferBlockEntity.ITEM_CAPACITY;
    public static final int FLUID_CAPACITY = GateBufferBlockEntity.FLUID_CAPACITY;

    /** Stack size 1: this carries per-item contents (a held item stack and fluid), and a data
     * component belongs to the whole ItemStack -- so a stacked I.D.E.P. would share one payload
     * across every copy. Structurally prevents that rather than relying on fill-side guards. */
    public IdepItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof GateBufferBlockEntity buffer) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (player.isCrouching()) {
                jamClear(stack, buffer, player);
            } else {
                reportBuffer(buffer, player);
            }
            return InteractionResult.SUCCESS;
        }

        if (be instanceof GateControllerBlockEntity controller) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            reportController(controller, player);
            return InteractionResult.SUCCESS;
        }

        if (be instanceof NodeBlockEntity node) {
            if (!player.isCrouching()) return InteractionResult.PASS; // let the Node's own menu open
            if (level.isClientSide) return InteractionResult.SUCCESS;
            jamClearNode(stack, node, player);
            return InteractionResult.SUCCESS;
        }

        // Fluid deposit takes priority over describeFace whenever the tool is actually holding
        // fluid -- otherwise an IHasChannels machine (e.g. Skin Tank, a genuine plain storage tank)
        // could NEVER be deposited into, since describeFace would always intercept the click first.
        // Only falls through to describeFace/PASS below if there's nothing to deposit, or the target
        // doesn't accept it.
        IFluidHandler targetTank = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, face);
        if (targetTank != null && holdsFluid(stack)) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (depositFluidInto(stack, targetTank, player)) return InteractionResult.SUCCESS;
        }

        if (be instanceof IHasChannels hasChannels) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            send(player, hasChannels.describeFace(face));
            return InteractionResult.SUCCESS;
        }

        if (targetTank != null) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            depositFluidInto(stack, targetTank, player);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            dumpHeldItem(stack, player);
            tryAutoFillInventoryContainer(stack, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void reportBuffer(GateBufferBlockEntity buffer, Player player) {
        String channelId = buffer.getAssignedChannelId();
        Component assignment = channelId == null
                ? Component.translatable("tooltip.dermicraft.idep.buffer_unassigned")
                : Component.translatable("tooltip.dermicraft.idep.buffer_servicing", humanize(channelId));
        Component fullness = Component.translatable(
                buffer.isEmpty() ? "tooltip.dermicraft.idep.buffer_empty" : "tooltip.dermicraft.idep.buffer_holding");
        send(player, assignment.copy().append(", ").append(fullness));
    }

    private void reportController(GateControllerBlockEntity controller, Player player) {
        if (controller.getTargetMachinePos() == null) {
            send(player, Component.translatable("tooltip.dermicraft.idep.controller_no_target"));
            return;
        }

        List<Channel> unserviced = controller.getUnservicedChannels();
        if (unserviced.isEmpty()) {
            send(player, Component.translatable("tooltip.dermicraft.idep.controller_all_serviced"));
            return;
        }

        MutableComponent joined = Component.empty();
        for (int i = 0; i < unserviced.size(); i++) {
            if (i > 0) joined.append(", ");
            joined.append(unserviced.get(i).label());
        }
        send(player, Component.translatable("tooltip.dermicraft.idep.controller_waiting", joined));
    }

    /** "ingredient_fluid" -> "Ingredient Fluid" -- a Buffer only persists the raw channel id (not
     * its Channel record/label), so this is a lightweight display fallback rather than a full label
     * lookup (which would need finding this Buffer's Controller and re-fetching the target's current
     * channel list just to render one word nicely). */
    private static Component humanize(String id) {
        StringBuilder result = new StringBuilder();
        for (String word : id.split("_")) {
            if (word.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return Component.literal(result.toString());
    }

    /** Styled "[I.D.E.P.] <message>" chat line -- keeps every diagnostic message visually consistent
     * and distinguishable from ordinary chat/system messages. */
    private void send(Player player, Component message) {
        player.sendSystemMessage(Component.literal("[").withStyle(ChatFormatting.GRAY)
                .append(Component.translatable("tooltip.dermicraft.idep.prefix").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD))
                .append(Component.literal("] ").withStyle(ChatFormatting.GRAY))
                .append(message.copy().withStyle(ChatFormatting.WHITE)));
    }

    /** Drains a jammed Buffer's contents into the tool's own storage. Does NOT clear the Buffer's
     * channel assignment -- only empties it, same "unstick, don't reset" behaviour discussed with
     * the user. */
    private void jamClear(ItemStack stack, GateBufferBlockEntity buffer, Player player) {
        jamClearGeneric(stack, buffer.getItemStore(), 0, buffer.getFluidStore(), player);
    }

    /** Same jam-clear, against a Node's transport buffer slot (via {@code getItemHandler(nonNull)},
     * which is already restricted to that one slot -- the GUI-only fluid-handler slot is untouched,
     * same as duct/Node automation already respects) and its tank. Does NOT touch the Node's per-leg
     * direction/toggle config. */
    private void jamClearNode(ItemStack stack, NodeBlockEntity node, Player player) {
        jamClearGeneric(stack, node.getItemHandler(Direction.UP), 0, node.getTank(Direction.UP), player);
    }

    /** Shared jam-clear core: drains one item slot and one fluid handler into the tool's own storage.
     * Silently skips whichever side (item/fluid) doesn't fit (different item already held, or the
     * tool's fluid capability rejects it) rather than failing the whole clear.
     *
     * <p>The fluid capability handler is fetched ONCE, up front, before any item-side mutation --
     * fetching it AFTER a {@code stack.set(...)} for the item component was silently returning a
     * handler that no longer wrote through to the same stack (only the item light would come on
     * when clearing both at once; found 2026-07-14). Doing both sides against handles/values grabbed
     * before any mutation avoids that ordering hazard entirely. */
    private void jamClearGeneric(ItemStack stack, IItemHandler itemSource, int itemSlot, IFluidHandler fluidSource, Player player) {
        boolean movedSomething = false;

        IFluidHandlerItem idepHandler = internalTank(stack);

        ItemStack sourceItem = itemSource.getStackInSlot(itemSlot);
        if (!sourceItem.isEmpty()) {
            HeldItemData current = stack.getOrDefault(ModDataComponentTypes.HELD_ITEM_DATA.get(), HeldItemData.EMPTY);
            boolean canMerge = current.isEmpty()
                    || (ItemStack.isSameItemSameComponents(current.itemStack(), sourceItem)
                        && current.itemStack().getCount() + sourceItem.getCount() <= ITEM_CAPACITY);
            if (canMerge) {
                ItemStack merged = current.isEmpty() ? sourceItem.copy() : current.itemStack().copy();
                if (!current.isEmpty()) merged.grow(sourceItem.getCount());
                stack.set(ModDataComponentTypes.HELD_ITEM_DATA.get(), new HeldItemData(merged));
                itemSource.extractItem(itemSlot, sourceItem.getCount(), false);
                movedSomething = true;
            }
        }

        FluidStack sourceFluid = fluidSource.getFluidInTank(0);
        if (!sourceFluid.isEmpty()) {
            int accepted = idepHandler.fill(sourceFluid, IFluidHandler.FluidAction.SIMULATE);
            if (accepted > 0) {
                FluidStack drained = fluidSource.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                idepHandler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                movedSomething = true;
            }
        }

        send(player, Component.translatable(movedSomething ? "tooltip.dermicraft.idep.cleared" : "tooltip.dermicraft.idep.nothing_cleared"));
    }

    /**
     * I.D.E.P.'s own tank, unsealed -- the registered capability
     * ({@link IHaveFluidData.SealedFillFluidDataFluidHandler}) refuses every fill so that nothing
     * external can load this tool, which would refuse the tool's own jam-clearing intake too. Both
     * write to the same {@code FluidData} component on the same stack, so this is the identical tank
     * seen from the inside rather than a second one.
     */
    private static IFluidHandlerItem internalTank(ItemStack stack) {
        return new IHaveFluidData.FlexibleFluidDataFluidHandler(stack, FLUID_CAPACITY);
    }

    private boolean holdsFluid(ItemStack stack) {
        return !internalTank(stack).getFluidInTank(0).isEmpty();
    }

    /** Right-click a tank block: push the tool's stored fluid into it. Standard simulate-first.
     * Returns whether anything actually moved, so the caller can fall back to describeFace when the
     * target doesn't accept it (e.g. wrong fluid already in an IHasChannels machine's tank). */
    private boolean depositFluidInto(ItemStack stack, IFluidHandler target, Player player) {
        IFluidHandlerItem idepHandler = internalTank(stack);

        FluidStack held = idepHandler.getFluidInTank(0);
        if (held.isEmpty()) return false;

        int accepted = target.fill(held, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return false;

        FluidStack drained = idepHandler.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        send(player, Component.translatable("tooltip.dermicraft.idep.deposited_fluid"));
        return true;
    }

    private void dumpHeldItem(ItemStack stack, Player player) {
        HeldItemData data = stack.getOrDefault(ModDataComponentTypes.HELD_ITEM_DATA.get(), HeldItemData.EMPTY);
        if (data.isEmpty()) return;

        // Lands at the player's feet rather than being flung forward -- player.drop(stack, boolean)
        // takes includeThrowerName, not dropAround, so it hits the Q-toss branch. See
        // IHaveFluidData#giveOrDrop for why the three-arg overload isn't the fix either.
        IHaveFluidData.giveOrDrop(player, data.itemStack().copy());
        stack.set(ModDataComponentTypes.HELD_ITEM_DATA.get(), HeldItemData.EMPTY);
    }

    /** Fills the FIRST empty, compatible fluid container found in the player's inventory (main +
     * offhand). A fixed-capacity container (bucket, Glass Flask) only accepts a fill for the FULL
     * amount it needs, so the standard simulate-first check here naturally skips it unless the tool
     * holds enough -- no special-casing needed between rigid and flexible containers. */
    private void tryAutoFillInventoryContainer(ItemStack stack, Player player) {
        IFluidHandlerItem idepHandler = internalTank(stack);

        FluidStack available = idepHandler.getFluidInTank(0);
        if (available.isEmpty()) return;

        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack candidate = inventory.getItem(i);
            if (candidate.isEmpty() || candidate == stack) continue;
            // A data component belongs to the whole stack, so filling a stacked container in place
            // would fill every item in it -- see IHaveFluidData#isSingleContainer.
            if (!IHaveFluidData.isSingleContainer(candidate)) continue;

            // Handles the destination check and the container-swap write-back both -- see
            // ModFluidUtil's "container-swap write-back" section for why hand-rolling this loses
            // fluid into a vanilla bucket.
            if (ModFluidUtil.fillPlayerContainer(player, candidate, idepHandler, available.getAmount()) > 0) return;
        }
    }
}
