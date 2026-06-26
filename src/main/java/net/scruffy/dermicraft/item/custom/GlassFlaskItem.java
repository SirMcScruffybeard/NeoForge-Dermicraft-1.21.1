package net.scruffy.dermicraft.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.fluid.ModFluidTypes;
import net.scruffy.dermicraft.interfaces.IHaveFluidData;
import net.scruffy.dermicraft.item.custom.base.ToolItem;
import net.scruffy.dermicraft.main.Dermicraft;
import org.jetbrains.annotations.NotNull;

public class GlassFlaskItem extends ToolItem implements IHaveFluidData {

    public static final int CAPACITY = 250;

    public GlassFlaskItem() {
        super(new Item.Properties());
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidData data = stack.getOrDefault(getDataType(), FluidData.EMPTY);
        if (!data.isFluidEmpty()) {
            //Return Flask with + ingredientFluid name
            return Component.translatable("item." + Dermicraft.MOD_ID + ".flask.filled", data.getFluidString());
        }
        return super.getName(stack);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return !stack.has(getDataType()) ? 16 : 1;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!isServerSide(level)) return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        FluidData data = stack.getOrDefault(getDataType(), FluidData.EMPTY);

        if (data.isFluidEmpty()) {
            return InteractionResult.PASS;
        }

        FluidStack fluidStack = data.fluidStack();

        if (fluidStack.is(ModFluidTypes.CALCIUM_BLEND_FLUID_TYPE.get())) {
            return new CalciumBlend().useOn(level, player, pos, stack);
        }

        if (fluidStack.is(ModFluidTypes.CRUDE_SLURRY_FLUID_TYPE.get())) {
            return new CrudeSlurry().useOn(level, pos, player, stack);
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (isServerSide(level)) {
            FluidData data = stack.getOrDefault(getDataType(), FluidData.EMPTY);

            if (data.isFluidEmpty()) {
                new Air().startUsing(player, usedHand);
                return InteractionResultHolder.consume(stack);
            }
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    @NotNull
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (isServerSide(level)) {
            FluidData data = stack.getOrDefault(getDataType(), FluidData.EMPTY);

            if (livingEntity instanceof Player player) {

                if (data.isFluidEmpty()) {
                    new Air().finishUsing(level, player, stack);
                    return stack;
                }
            }
        }

        return stack;
    }

    private void setFlaskIfSurvival(Player player, ItemStack stack, FluidData data) {
        if (!player.isCreative()) {
            stack.set(getDataType(), data);
        }
    }

    private void consumeFlaskIfSurvival(Player player, ItemStack stack) {
        if (!player.isCreative()) {
            stack.remove(getDataType());
        }
    }


    protected class Air {

        public void startUsing(Player player, InteractionHand usedHand) {
            if (player.getAirSupply() < player.getMaxAirSupply()) {
                player.startUsingItem(usedHand);
            }
        }

        private void finishUsing(Level level, Player player, ItemStack stack) {
            if (level.isClientSide) return;

            int airSupply = 90; //about 3 bubbles
            int playerAir = player.getAirSupply();

            if (playerAir <= 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_HURT_DROWN, SoundSource.PLAYERS, 1f, 0.8f + level.random.nextFloat() * 0.4f);
            } else {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.PLAYERS, 0.7F, 1.2F);
            }

            player.setAirSupply(Math.min(player.getMaxAirSupply(), playerAir + airSupply));

            setFlaskIfSurvival(player, stack, FluidData.createData(Fluids.WATER, 250));
        }
    }

    private class CalciumBlend {

        public InteractionResult useOn(Level level, Player player, BlockPos pos, ItemStack itemStack) {
            if (BoneMealItem.applyBonemeal(itemStack.copy(), level, pos, player)) {

                Effects.playPlantEffect(level, pos.above());
                consumeFlaskIfSurvival(player, itemStack);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        }
    }

    private class CrudeSlurry {

        public InteractionResult useOn(Level level, BlockPos pos, Player player, ItemStack itemStack) {
            RandomSource random = level.getRandom();

            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.DIRT)) {
                if (!level.isClientSide) {
                    BlockState newState = random.nextBoolean() ?
                            Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.MYCELIUM.defaultBlockState();

                    Effects.playPlantEffect(level, pos.above());
                    level.setBlockAndUpdate(pos, newState);
                    consumeFlaskIfSurvival(player, itemStack);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            if (state.is(Tags.Blocks.VILLAGER_FARMLANDS) && level.isEmptyBlock(pos.above())) {
                if (!level.isClientSide) {

                    var croptag = level.registryAccess().registryOrThrow(Registries.BLOCK).getTag(BlockTags.CROPS);

                    if (croptag.isPresent() && croptag.get().size() != 0) {
                        var randomCrop = croptag.get().getRandomElement(random);
                        randomCrop.ifPresent(holder -> {
                            BlockState cropState = holder.value().defaultBlockState();

                            if (cropState.canSurvive(level, pos.above())) {
                              Effects.playPlantEffect(level, pos.above());
                                level.setBlockAndUpdate(pos.above(), cropState);
                                consumeFlaskIfSurvival(player, itemStack);
                            }
                        });
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        }
    }

    public static class Effects {

        public static void playPlantEffect(Level level, BlockPos pos) {
            level.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);

            if (level instanceof ServerLevel serverLevel) {
                // This plays the "Bonemeal" particle manually from the server
                // HAPPY_VILLAGER is the exact green sparkle used by bonemeal
                serverLevel.sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, // Center of block
                        15,         // Count
                        0.5, 0.5, 0.5, // Spread (Oxygen/Box size)
                        0.05        // Speed/Motion
                );
            }
        }
    }
}
