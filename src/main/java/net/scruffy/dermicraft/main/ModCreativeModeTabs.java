package net.scruffy.dermicraft.main;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scruffy.dermicraft.block.ModBlocks;

import java.util.function.Supplier;

public class ModCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Dermicraft.MOD_ID);

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }

    public static final Supplier<CreativeModeTab> DERMICRAFT_BLOCKS_TAB = CREATIVE_MODE_TAB.register(Dermicraft.MOD_ID +"_blocks_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModBlocks.INERT_TUMOR.get()))
                    .title(Component.translatable("creativetab.dermicraft.dermicraft_blocks"))
                    .displayItems((itemDisplayParameters, output) -> {

                        ////////////////////Tumors\\\\\\\\\\\\\\\\\\\\
                        output.accept(ModBlocks.INERT_TUMOR);

                    }).build());
}
