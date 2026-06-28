package net.scruffy.dermicraft.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scruffy.dermicraft.block.entity.custom.MachineBaseBlockEntity;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.recipe.ModRecipes;
import net.scruffy.dermicraft.recipe.OneFluidOneItemRecipeInput;
import net.scruffy.dermicraft.recipe.dipping.DippingRecipe;

import java.util.Optional;

@EventBusSubscriber(modid = Dermicraft.MOD_ID)
public class DippingEvent {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        BlockPos pos = event.getPos();
        if (!level.getBlockState(pos).is(ModTags.Blocks.DIPPING_TANKS)) return; //Check Tag

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MachineBaseBlockEntity machine && !machine.hasTank()) return;

        IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, event.getFace());
        if (tank == null) return;

        for (int i = 0; i < tank.getTanks(); i++) {
            FluidStack fluidInTank = tank.getFluidInTank(i);
            if (fluidInTank.isEmpty()) continue;

            OneFluidOneItemRecipeInput input = new OneFluidOneItemRecipeInput(stack, fluidInTank);
            Optional<RecipeHolder<DippingRecipe>> recipeOptional =
                    level.getRecipeManager().getRecipeFor(ModRecipes.DIPPING_TYPE.get(), input, level);

            if (recipeOptional.isEmpty()) continue;

            DippingRecipe recipe = recipeOptional.get().value();

            Player player = event.getEntity();
            int crafts;
            if (player.isShiftKeyDown()) {
                int craftsByItem = stack.getCount() / recipe.itemAmount();
                int craftsByFluid = recipe.ingredientFluidAmount() < 0
                        ? craftsByItem
                        : fluidInTank.getAmount() / recipe.ingredientFluidAmount();
                crafts = Math.min(craftsByItem, craftsByFluid);
            } else {
                crafts = 1;
            }
            if (crafts <= 0) continue;

            tank.drain(new FluidStack(fluidInTank.getFluid(), recipe.ingredientFluidAmount() * crafts), IFluidHandler.FluidAction.EXECUTE);
            stack.shrink(recipe.itemAmount() * crafts);

            ItemStack result = recipe.assemble(input, level.registryAccess());
            result.setCount(result.getCount() * crafts);
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
    }
}
