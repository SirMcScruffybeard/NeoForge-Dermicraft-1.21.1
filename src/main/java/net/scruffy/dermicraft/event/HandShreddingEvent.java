package net.scruffy.dermicraft.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.scruffy.dermicraft.component.AidModeData;
import net.scruffy.dermicraft.item.custom.AidItem;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.hand_shredding.HandShreddingRecipe;
import net.scruffy.dermicraft.recipe.hand_shredding.HandShreddingRecipeInput;
import software.bernie.geckolib.animatable.GeoItem;

import java.util.Optional;

/**
 * Generic trigger for {@link HandShreddingRecipe}: right-click in air with a matching tool in one
 * hand and the matching input in the other (either hand order), no block/machine involved. See
 * the recipe class doc for why this is a standalone recipe type rather than a one-off item hook.
 */
@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class HandShreddingEvent {

    @SubscribeEvent
    public static void onRightClickAir(PlayerInteractEvent.RightClickItem event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;

        // Vanilla tries main hand first, then off hand, for a single right-click if main hand's
        // result doesn't consume the action -- both stacks are guaranteed non-empty for a valid
        // combo here, so only the main-hand firing is needed. Without this filter, the off-hand
        // firing of the SAME click re-runs this logic and double-consumes the input.
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Player player = event.getEntity();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        if (mainHand.isEmpty() || offHand.isEmpty()) return;

        // A.I.D. matches the Ingredient-based recipes below purely by item identity, with no notion
        // of its own mode -- isModeReady rejects a structurally-matching A.I.D. candidate that isn't
        // currently in Scalpel mode, falling through to try the reverse hand order instead of
        // silently shredding wool/pumpkin while in a different mode (same gating concern as
        // AidItem's own useScalpel, which keeps A.I.D. OFF the EXTRACTION_TOOLS tag entirely).
        HandShreddingRecipe recipe = findRecipe(level, mainHand, offHand);
        InteractionHand toolHand;
        ItemStack tool, input;
        if (recipe != null && isModeReady(mainHand)) {
            toolHand = InteractionHand.MAIN_HAND;
            tool = mainHand;
            input = offHand;
        } else {
            recipe = findRecipe(level, offHand, mainHand);
            if (recipe == null || !isModeReady(offHand)) return;
            toolHand = InteractionHand.OFF_HAND;
            tool = offHand;
            input = mainHand;
        }

        input.shrink(1);

        if (recipe.consumeTool()) {
            tool.shrink(1);
        } else if (recipe.toolDamage() > 0) {
            tool.hurtAndBreak(recipe.toolDamage(), player, toolHand == InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND
                    : EquipmentSlot.OFFHAND);
        }

        ItemStack result = recipe.getResultItem(level.registryAccess());
        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }

        // A.I.D. plays its own "cut" trigger in place of the vanilla arm swing -- CONSUME (rather
        // than SUCCESS) is what cancels that swing, same trick as AidItem's own onItemUseFirst uses
        // for Forceps' "grab" and Scalpel's harvest/duct-cycle "cut".
        boolean isAid = tool.getItem() instanceof AidItem;
        if (isAid && level instanceof ServerLevel serverLevel) {
            ((GeoItem) tool.getItem()).triggerAnim(player, GeoItem.getOrAssignId(tool, serverLevel), "Scalpel", "cut");
        }

        event.setCanceled(true);
        event.setCancellationResult(isAid ? InteractionResult.CONSUME : InteractionResult.SUCCESS);
    }

    /** True for any tool that isn't A.I.D.; for A.I.D., true only while in Scalpel mode. */
    private static boolean isModeReady(ItemStack tool) {
        return !(tool.getItem() instanceof AidItem) || AidItem.modeData(tool).modeEnum() == AidModeData.Mode.SCALPEL;
    }

    private static HandShreddingRecipe findRecipe(Level level, ItemStack tool, ItemStack input) {
        HandShreddingRecipeInput recipeInput = new HandShreddingRecipeInput(tool, input);
        Optional<RecipeHolder<HandShreddingRecipe>> holder =
                level.getRecipeManager().getRecipeFor(ModRecipes.HAND_SHREDDING_TYPE.get(), recipeInput, level);
        return holder.map(RecipeHolder::value).orElse(null);
    }
}
