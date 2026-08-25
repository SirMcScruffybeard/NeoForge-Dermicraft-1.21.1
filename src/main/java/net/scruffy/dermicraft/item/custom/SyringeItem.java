package net.scruffy.dermicraft.item.custom;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    // Tank draw/inject used to live here (useOn -> draw), but every machine block's
    // useWithoutItem now unconditionally opens its GUI on a fallen-through click, which consumes
    // the interaction before vanilla ever reaches an item's own useOn (see
    // ServerPlayerGameMode#useItemOn: block's useItemOn -> block's useWithoutItem -> ONLY THEN the
    // item's useOn). That logic moved to SyringeTankEvent (PlayerInteractEvent.RightClickBlock,
    // which fires before all of that), so it stays effective regardless of any given block's own
    // interaction shape. See that class for the real draw/inject implementation.
}
