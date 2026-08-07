package net.scruffy.dermicraft.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.scruffy.dermicraft.interfaces.ICollectBlocks;
import net.scruffy.dermicraft.item.custom.base.ToolItem;

public class ForcepsItem extends ToolItem implements ICollectBlocks {

    public static final int PRIMITIVE_DURABILITY = 10;
    private static final int USE_WEAR = 1;

    // Iron Forceps: no durability call at all -- genuinely unbreakable, not just high-durability.
    // Its use is narrow enough (pick up a collectible block intact) that this is deliberate.
    public ForcepsItem() {
        super(new Item.Properties());
    }

    /**
     * @param durability Primitive (Bone + Stick) alternate only -- the iron Forceps stays
     *                    unbreakable via the no-arg constructor above. Mirrors ScalpelItem's
     *                    parameterized-class convention.
     */
    public ForcepsItem(int durability) {
        super(new Item.Properties().durability(durability));
    }

    // onItemUseFirst runs before the clicked block's own useItemOn (and before the crouch/
    // secondary-use check), so the Forceps can pick up an interactive block — a machine, a
    // storage block — without the player having to crouch to bypass that block's right-click.
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (player == null || !canCollect(level, pos)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        collect(level, pos, player);
        // No-op on the iron Forceps (hurtAndBreak silently ignores non-damageable items); depletes
        // durability on the Primitive alternate.
        EquipmentSlot slot = context.getHand() == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        stack.hurtAndBreak(USE_WEAR, player, slot);
        return InteractionResult.SUCCESS;
    }
}
