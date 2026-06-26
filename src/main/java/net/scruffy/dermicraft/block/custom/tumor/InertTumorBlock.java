package net.scruffy.dermicraft.block.custom.tumor;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.util.ModItemUtil;

import java.util.ArrayList;
import java.util.List;

public class InertTumorBlock extends TumorBlock {
    public InertTumorBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(.05f)
                .explosionResistance(15f)
                .sound(SoundType.SLIME_BLOCK)
                .friction(0.6f)
                .ignitedByLava()
        );
    }

    @Override
    public List<ItemStack> harvest(Level level, Player player, ItemStack stack, BlockPos pos) {

        List<ItemStack> drops = new ArrayList<>();

        if (!level.isClientSide) {

            RandomSource random = level.getRandom();

            int count = getDropCount(random, 2, 5);

            if (random.nextFloat() < 0.20f) {
                count++;
            }

            for (int i = 0; i < count; i++) {
                int chance = random.nextInt(3);

                ItemStack drop = switch (chance) {
                    case 0 -> new ItemStack(ModItems.DENSE_MUSCLE.get());
                    case 1 -> new ItemStack(ModItems.NERVE_CLUSTER.get());
                    default -> new ItemStack(ModItems.EYE.get());
                };
                drops.add(drop);
            }

            ModItemUtil.giveItems(player, drops);

        }

        return drops;
    }
}
