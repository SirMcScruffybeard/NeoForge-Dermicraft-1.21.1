package net.scruffy.dermicraft.interfaces;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.effect.ModEffects;
import net.scruffy.dermicraft.util.ModPlayerUtil;

public interface ISuture {

    void suturePlayer(Level level, Player player, ItemStack stack);
    void useMaterials(Player player, InteractionHand hand);

    default boolean isSutable(BlockState state) {
        return state.is(ModTags.Blocks.SUTABLE);
    }

    default boolean isFullHealth(Player player) {
        return ModPlayerUtil.isFullHealth(player);
    }

    default void applySutureEffect(Player player, int ticks, int amplifier) {
        player.addEffect(new MobEffectInstance(ModEffects.SUTURED, ticks, amplifier));
    }

    default boolean consumeItem(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (invStack.is(item)) {
                invStack.shrink(1);
                return true;
            }
        }
        return false;
    }

    default boolean consumeItem(Player player, TagKey<Item> tag) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (invStack.is(tag)) {
                invStack.shrink(1);
                return true;
            }
        }
        return false;
    }

    default boolean consumeString(Player player) {
        return consumeItem(player, Tags.Items.STRINGS);
    }

    default void playSutureSound(Level level, Player player, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, volume, pitch);
    }

    default void playDefaultSutureSound(Level level, Player player) {
        playSutureSound(level, player, SoundEvents.LEASH_KNOT_PLACE, 1f, 0.8f);
    }
}
