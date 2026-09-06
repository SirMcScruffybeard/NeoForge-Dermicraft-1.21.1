package net.scruffy.dermicraft.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scruffy.dermicraft.block.custom.duct.AbstractInnardsDuctBlock;
import net.scruffy.dermicraft.block.entity.custom.MarredTumorBlockEntity;
import net.scruffy.dermicraft.block.entity.custom.StitchedTumorBlockEntity;
import net.scruffy.dermicraft.component.AidModeData;
import net.scruffy.dermicraft.interfaces.ICutStitches;
import net.scruffy.dermicraft.interfaces.IInjectableBlock;
import net.scruffy.dermicraft.interfaces.IHarvestableBlock;
import net.scruffy.dermicraft.interfaces.ISutableBlock;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.item.custom.AidItem;
import net.scruffy.dermicraft.main.Dermicraft;

/**
 * Client-side "what am I aiming at" scan for A.I.D.'s held-mode screen, same idea as
 * {@link DrinkerTargetScanner} but boolean-only -- there's no readout to build, just a per-mode
 * validity check the screen glow layer swaps textures on. Cheap enough (a single raycast plus one
 * side-effect-free tag/interface check) to run every tick rather than throttled like Drinker's scan,
 * which also has to do fluid-handler capability lookups and build an action-bar message.
 *
 * <p>Each mode's check mirrors the real gate {@code AidItem}'s matching {@code use<Mode>} branch
 * uses, minus anything with a side effect -- Scalpel's duct case in particular doesn't simulate
 * {@code AbstractInnardsDuctBlock#cycleConnections} (that call mutates state), so it reads as valid
 * for any duct rather than only one with a free connection to cycle to, same tolerance
 * {@code useSyringe}'s own {@code isValidFluidHandler} pre-check already accepts.
 */
@EventBusSubscriber(modid = Dermicraft.MOD_ID, value = Dist.CLIENT)
public class AidTargetScanner {

    private static boolean validTarget = false;

    /** Whether the held A.I.D. is currently aimed at something valid for its current mode. Read by
     * the model's screen render layer. */
    public static boolean hasValidTarget() {
        return validTarget;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level level = mc.level;

        ItemStack aid = heldAid(player);
        if (player == null || level == null || aid.isEmpty()) {
            validTarget = false;
            return;
        }

        validTarget = scan(player, level, aid);
    }

    private static ItemStack heldAid(LocalPlayer player) {
        if (player == null) return ItemStack.EMPTY;
        if (player.getMainHandItem().is(ModItems.AID.get())) return player.getMainHandItem();
        if (player.getOffhandItem().is(ModItems.AID.get())) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    private static boolean scan(LocalPlayer player, Level level, ItemStack aid) {
        // Same block-interaction raycast onItemUseFirst's target ultimately comes from -- no fluid
        // clipping needed, none of A.I.D.'s modes target a bare fluid source.
        HitResult hit = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) return false;

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        AidItem item = (AidItem) ModItems.AID.get();
        AidModeData.Mode mode = AidItem.modeData(aid).modeEnum();

        return switch (mode) {
            case FORCEPS -> item.canCollect(level, pos);
            case SCALPEL -> isScalpelTarget(item, level, pos, state);
            case SUTURE -> isSutureTarget(item, level, pos, state);
            case SYRINGE -> isSyringeTarget(item, level, pos, state, blockHit.getDirection());
        };
    }

    private static boolean isScalpelTarget(AidItem item, Level level, BlockPos pos, BlockState state) {
        if (item.isHarvestable(state) && state.getBlock() instanceof IHarvestableBlock) return true;

        if (state.getBlock() instanceof ICutStitches
                && level.getBlockEntity(pos) instanceof StitchedTumorBlockEntity) {
            return true;
        }

        return state.getBlock() instanceof AbstractInnardsDuctBlock;
    }

    private static boolean isSutureTarget(AidItem item, Level level, BlockPos pos, BlockState state) {
        if (!item.isSutable(state) || !(state.getBlock() instanceof ISutableBlock)) return false;
        return level.getBlockEntity(pos) instanceof MarredTumorBlockEntity;
    }

    private static boolean isSyringeTarget(AidItem item, Level level, BlockPos pos, BlockState state, Direction face) {
        if (state.getBlock() instanceof IInjectableBlock
                && level.getBlockEntity(pos) instanceof StitchedTumorBlockEntity) {
            return true;
        }

        IFluidHandler handler = item.getTargetFluidHandler(level, pos, face);
        return item.isValidFluidHandler(handler);
    }
}
