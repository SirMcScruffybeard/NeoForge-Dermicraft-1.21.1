package net.scruffy.dermicraft.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.puddle_crafting.PuddleCraftingRecipe;
import net.scruffy.dermicraft.recipe.puddle_crafting.PuddleCraftingRecipeInput;
import net.scruffy.dermicraft.util.ModMath;
import net.scruffy.dermicraft.util.PuddleCraftHelper;

import java.util.*;

@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class PuddleCraftEvent {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();

        //Performance Gate: Server-side only, every 10-ticks
        if (level.isClientSide || !ModMath.Time.hasTicksPassed(level, 10)) {
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            // Track blocks processed during this tick to avoid duplicate processing if multiple players are nearby
            Set<BlockPos> processedPositions = new java.util.HashSet<>();

            for (Player player : serverLevel.players()) {
                BlockPos playerPos = player.blockPosition();
                AABB searchArea = new AABB(playerPos).inflate(16);

                //Gather ALL item entities near the player
                List<ItemEntity> items = serverLevel.getEntitiesOfClass(ItemEntity.class, searchArea);

                //Group the items by the exact block coordinate they are currently bobbing in
                Map<BlockPos, List<ItemEntity>> itemsByPos = items.stream()
                        .filter(item -> !item.getItem().isEmpty())
                        .collect(java.util.stream.Collectors.groupingBy(item -> item.blockPosition()));

                //Process each puddle coordinate found
                itemsByPos.forEach((pos, itemList) -> {
                    if (processedPositions.contains(pos)) return;


                    PuddleCraftingRecipeInput input = PuddleCraftHelper.getInput(serverLevel, pos);
                    var recipeOptional = PuddleCraftHelper.getRecipe(serverLevel, input);

                    if (PuddleCraftHelper.isRecipeValid(recipeOptional)) {
                        PuddleCraftingRecipe recipe = recipeOptional.value();


                        // Find the "youngest" item in the puddle to ensure ALL items have marinated long enough
                        int youngestItemAge = itemList.stream()
                                .mapToInt(item -> item.tickCount)
                                .min()
                                .orElse(0);

                        // If the items haven't been in the world long enough yet, skip processing this tick
                        if (youngestItemAge < recipe.ticks()) {
                            // Optional: Spawn some ambient particles here to show it's cooking!
                            return;
                        }

                        processedPositions.add(pos);

                        // --- SUCCESS & TRANSFORMATION PHASE ---

                        // 1. Morph the MakeFluids block
                        serverLevel.setBlock(pos, recipe.resultFluid().defaultFluidState().createLegacyBlock(), 3);

                        // 2. Spawn the output item if it isn't AIR
                        Item outputItem = recipe.resultItem();
                        if (outputItem != Items.AIR) {

                            Block blockFromResult = Block.byItem(outputItem);

                            if (blockFromResult != Blocks.AIR) {
                                // SUCCESS: The result is a block!
                                // We place it at 'pos', which replaces the resulting MakeFluids block we just set.
                                serverLevel.setBlock(pos, blockFromResult.defaultBlockState(), 3);

                            } else {
                                // FALLBACK: The result is just a regular item (spawns as entity)
                                double spawnX = pos.getX() + 0.5;
                                double spawnY = pos.getY() + 0.2;
                                double spawnZ = pos.getZ() + 0.5;

                                ItemEntity resultEntity = new ItemEntity(serverLevel, spawnX, spawnY, spawnZ, new ItemStack(outputItem));
                                resultEntity.setDefaultPickUpDelay();
                                serverLevel.addFreshEntity(resultEntity);
                            }
                        }

                        // 3. Surgically consume only the required recipe components
                        PuddleCraftHelper.consumeRequiredItems(serverLevel, pos, recipe);

                        // 4. Play standard block breaking audio/visual feedback
                        serverLevel.levelEvent(2001, pos, Block.getId(serverLevel.getBlockState(pos)));
                    }
                });
            }
        }
    }
}
