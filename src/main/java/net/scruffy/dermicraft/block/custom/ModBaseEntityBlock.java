package net.scruffy.dermicraft.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.scruffy.dermicraft.block.entity.custom.base.MachineBaseBlockEntity;
import net.scruffy.dermicraft.datagen.tag.ModTags;
import net.scruffy.dermicraft.interfaces.IModuleTabMachine;
import org.jetbrains.annotations.Nullable;

public abstract class ModBaseEntityBlock extends BaseEntityBlock {
    protected ModBaseEntityBlock(Properties properties) {
        super(properties);
    }
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected abstract MapCodec<? extends BaseEntityBlock> codec();

    @Override
    public abstract @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState);

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
        if (level.getBlockEntity(pos) instanceof MachineBaseBlockEntity machine) {
            stack.applyComponents(machine.collectComponents());
        }
        return stack;
    }

    /**
     * Right-clicking a machine that has a Module tab with a module in hand opens its screen on that tab. A machine's
     * {@code useItemOn} calls this first, before any fluid or ingredient handling, so a module is never mistaken for
     * an ingredient. Returns whether the click was handled; the caller then returns SUCCESS.
     *
     * <p>The tab is set on both sides on purpose: the screen the client opens reads the client's copy of the flag,
     * which is not synced, and the click always runs on the client first.
     */
    protected static boolean tryOpenModuleTab(ItemStack stack, Level level, BlockPos pos, Player player) {
        if (!stack.is(ModTags.Items.MODULES)) return false;
        if (!(level.getBlockEntity(pos) instanceof IModuleTabMachine machine)
                || !(level.getBlockEntity(pos) instanceof MenuProvider menuProvider)) {
            return false;
        }

        machine.setModuleTabActive(true);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buf -> buf.writeBlockPos(pos));
        }
        return true;
    }
}
