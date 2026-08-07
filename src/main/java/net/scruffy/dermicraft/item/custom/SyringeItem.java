package net.scruffy.dermicraft.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.interfaces.IHaveFluidData;
import net.scruffy.dermicraft.interfaces.IInject;
import net.scruffy.dermicraft.main.Dermicraft;

public class SyringeItem extends Item implements IInject, IHaveFluidData {

    public static final int CAPACITY = 100;
    public static final int PRIMITIVE_DURABILITY = 10;
    private static final int USE_WEAR = 1;

    // Iron Syringe: no durability call at all -- genuinely unbreakable, same as the iron Forceps.
    public SyringeItem() {
        super(new Item.Properties());
    }

    /**
     * @param durability Primitive (Bone Meal + Bone + Glass) alternate only -- the iron Syringe
     *                    stays unbreakable via the no-arg constructor above. Mirrors ForcepsItem's
     *                    parameterized-class convention.
     */
    public SyringeItem(int durability) {
        super(new Item.Properties().durability(durability));
    }

    /**
     * Wear applies on injection (emptying), not on draw -- injection is the tool's real "use"
     * moment, mirroring the Scalpel/Suture Kit/Forceps pattern of damaging on the action that
     * matters rather than on fill-up. No-op on the undamageable iron Syringe (hurtAndBreak silently
     * ignores non-damageable items); depletes durability on the Primitive alternate.
     */
    @Override
    public void emptyDataFluidIfSurvival(ItemStack stack, Player player) {
        if (!player.isCreative()) {
            emptyDataFluid(stack);
            stack.hurtAndBreak(USE_WEAR, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        }
    }

    /**
     * Keyed off this item's own registry path (not hardcoded "syringe") so the Primitive Syringe --
     * same class, separate registration -- gets its own ".filled" translation. Mirrors BladderItem.
     */
    @Override
    public Component getName(ItemStack stack) {
        FluidData data = stack.getOrDefault(getFluidDataType(), FluidData.EMPTY);
        if (!data.isFluidEmpty()) {
            String path = BuiltInRegistries.ITEM.getKey(this).getPath();
            return Component.translatable("item." + Dermicraft.MOD_ID + "." + path + ".filled", data.getFluidString());
        }
        return super.getName(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!isServerSide(level)) return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        ItemStack stack = context.getItemInHand();

        if (isValidFluidHandler(getTargetFluidHandler(level, pos, face))) {
            draw(level, pos, face, stack);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private void draw(Level level, BlockPos pos, Direction face, ItemStack stack) {
        IFluidHandler handler = getTargetFluidHandler(level, pos, face);

        if (isValidFluidHandler(handler) && targetHasEnough(CAPACITY, handler)) {
            FluidData data = stack.getOrDefault(getFluidDataType(), FluidData.EMPTY);
            if (data.isFluidEmpty()) {
                stack.set(getFluidDataType(), FluidData.createData(handler.drain(CAPACITY, IFluidHandler.FluidAction.EXECUTE)));
            }
        }
    }
}
