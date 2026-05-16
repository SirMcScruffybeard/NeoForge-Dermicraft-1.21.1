package net.scruffy.dermicraft.event;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.util.ModMath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class TumorSynthesisEvent {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();

        if (level.isClientSide || !ModMath.time.hasTicksPassed(level, ModMath.time.getSecondsToTicks(10))) return;

        for (Player player : level.players()) {
            BlockPos playerPos = player.blockPosition();

            AABB searchArea = new AABB(playerPos).inflate(16);

            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, searchArea);

            // Group items by the block they are currently in
            Map<BlockPos, List<ItemEntity>> itemsByPos = items.stream()
                    .filter(item -> isValidFlesh(item.getItem()))
                    .collect(Collectors.groupingBy(item -> item.blockPosition()));

            itemsByPos.forEach(((pos, itemList) -> {
                if (level.getBlockState(pos).is(ModFluids.NUTRIENT_SLURRY_BLOCK)) {

                    //Get total of items with the tag
                    int totalFleshItems = itemList.stream()
                            .mapToInt(item -> item.getItem().getCount())
                            .sum();

                    if (totalFleshItems >= 4) {
                        processSynthesis(level, pos, itemList);
                    }
                }
            }));
        }
    }

    private static void processSynthesis(Level level, BlockPos pos, List<ItemEntity> items) {

        // 1. Map the items for easy checking
        Map<Item, Integer> itemCounts = new HashMap<>();
        for (ItemEntity entity : items) {
            ItemStack stack = entity.getItem();
            itemCounts.put(stack.getItem(), itemCounts.getOrDefault(stack.getItem(), 0) + stack.getCount());
        }

        int totalFlesh = items.stream()
                .filter(e -> isValidFlesh(e.getItem()))
                .mapToInt(e -> e.getItem().getCount())
                .sum();

        if (totalFlesh >= 4) {

            BlockState targetState = getTargetState(items);

            consumeIngredients(items, targetState);
            items.forEach(entity -> entity.setPos(pos.getX() + .5, pos.getY() + 1.2, pos.getZ() + .5));
            level.setBlock(pos, targetState, 3);

            playTumorSound(level, pos);
        }
    }

    private static void consumeIngredients(List<ItemEntity> items, BlockState state) {
        int fleshNeeded = 4;

        Item catalyst = getCatalyst(state);

        int catalystNeeded = (catalyst != null) ? 1 : 0;

        for (ItemEntity entity : items) {
            ItemStack stack = entity.getItem();

            if (catalystNeeded > 0 && stack.is(catalyst)) {
                int take = 1;
                stack.shrink(take);
                catalystNeeded = -take;

                entity.setItem(stack);
            }

            if (fleshNeeded > 0 && !stack.isEmpty() && stack.is(ModTags.Items.ANIMAL_MEATS)) {
                int take = Math.min(stack.getCount(), fleshNeeded);
                stack.shrink(take);
                fleshNeeded -= take;

                entity.setItem(stack);
            }

            if (state.isEmpty()) entity.discard();

            if (fleshNeeded <= 0 && catalystNeeded <= 0) {
                break;
            }

        }

    }


    private static void playTumorSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 0.5F);
        level.playSound(null, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 1.2F, 0.6F);
    }

    private static boolean isValidFlesh(ItemStack stack) {
        return stack.is(ModTags.Items.ANIMAL_MEATS) || stack.is(ModTags.Items.PART_ITEMS);
    }

    private static BlockState getTargetState(List<ItemEntity> items) {

        int numEye = countItems(items, ModItems.EYE.get());
        int numMuscle = countItems(items, ModItems.DENSE_MUSCLE.get());
        int numNerve = countItems(items, ModItems.NERVE_CLUSTER.get());

        if (numEye > numMuscle && numEye > numNerve) return ModBlocks.EYE_TUMOR.get().defaultBlockState();

        if (numMuscle > numEye && numMuscle > numNerve) return ModBlocks.MUSCLE_TUMOR.get().defaultBlockState();

        if (numNerve > numEye && numNerve > numMuscle) return ModBlocks.NERVE_TUMOR.get().defaultBlockState();

        int max = Math.max(numEye, Math.max(numMuscle, numNerve));

        if (numEye == 0 && numMuscle == 0 && numNerve == 0) return ModBlocks.INERT_TUMOR.get().defaultBlockState();

        List<BlockState> candidates = new ArrayList<>();

        if (numEye == max) candidates.add(ModBlocks.EYE_TUMOR.get().defaultBlockState());
        if (numMuscle == max) candidates.add(ModBlocks.MUSCLE_TUMOR.get().defaultBlockState());
        if (numNerve == max) candidates.add(ModBlocks.NERVE_TUMOR.get().defaultBlockState());

        RandomSource random = RandomSource.create();

        return candidates.get(random.nextInt(candidates.size()));
    }

    private static int countItems(List<ItemEntity> items, Item check) {
        int count = 0;

        for (ItemEntity entity : items) {
            if (entity.getItem().is(check)) count++;
        }

        return count;
    }

    private static Item getCatalyst(BlockState state) {
        if (state.is(ModBlocks.EYE_TUMOR.get())) {
            return ModItems.EYE.get();
        }

        if (state.is(ModBlocks.MUSCLE_TUMOR.get())) {
            return ModItems.DENSE_MUSCLE.get();
        }

        if (state.is(ModBlocks.NERVE_TUMOR.get())) {
            return ModItems.NERVE_CLUSTER.get();
        }

        return null;
    }

}
