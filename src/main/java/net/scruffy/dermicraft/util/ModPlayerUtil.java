package net.scruffy.dermicraft.util;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ModPlayerUtil {

    public static void emptyHand(Player player, InteractionHand hand) {
        if (player != null && hand != null) {
            if (!player.level().isClientSide) {
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
        }
    }

    public static boolean isFullHealth(Player player) {
        return (player.getHealth() >= player.getMaxHealth());
    }
}
