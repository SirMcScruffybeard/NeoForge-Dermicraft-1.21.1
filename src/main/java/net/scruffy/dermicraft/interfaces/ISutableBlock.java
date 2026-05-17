package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.scruffy.dermicraft.datagen.tag.ModTags;

public interface ISutableBlock {

    void suture(Level level, Player player, BlockPos pos);
    void changeState(Level level, BlockPos pos, Block block);

    default boolean isSutureTool(ItemStack stack) {
        return stack.is(ModTags.Items.SUTURE_TOOLS);
    }

    default void setState(Level level, BlockPos pos, BlockState targetState) {
        level.setBlock(pos, targetState, 3);
    }

    default void playSutureSound(Level level, Player player, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, volume, pitch);
    }

    default void playDefaultSutureSound(Level level, Player player) {
        playSutureSound(level, player, SoundEvents.LEASH_KNOT_PLACE, 1f, 0.8f);
    }


}
