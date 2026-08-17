package net.scruffy.dermicraft.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.scruffy.dermicraft.item.custom.ShatterItem;
import net.scruffy.dermicraft.item.custom.SunderItem;
import net.scruffy.dermicraft.main.Dermicraft;

/**
 * Bottom-up fill bar over Sunder/Shatter in the player's inventory and hotbar, showing their
 * mounted chain/head's remaining durability -- not the gadget's own HP (that's still vanilla's
 * durability bar, per {@code IGadget}'s javadoc; this is a second, separate reading for the
 * chain/head, which previously only showed as tooltip text). Green >=75%, yellow 25-75%, red below
 * that; nothing renders when no chain/head is mounted at all.
 *
 * <p>Two different techniques for two different render surfaces, because they offer different
 * hooks:
 * <ul>
 *   <li><b>Any container screen showing the player's own inventory</b> (the inventory screen
 *   itself, or the player-inventory row at the bottom of a chest/machine GUI) --
 *   {@link ContainerScreenEvent.Render.Background} fires after the screen's background texture but
 *   BEFORE item icons render, so the bar draws as a true background, fully hidden behind the icon
 *   except for the sliver that pokes out at the top of the missing portion.</li>
 *   <li><b>The live gameplay HUD hotbar</b> -- there is no such gap in vanilla's hotbar rendering
 *   (background sprite and item icons are drawn together in one pass with no event between them),
 *   so this instead draws AFTER the layer ({@link RenderGuiLayerEvent.Post}) as a semi-transparent
 *   overlay on top of the icon. Less clean than a true background, but still reads at a glance
 *   without a mixin, and this is the more important of the two surfaces per the design ask.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Dermicraft.MOD_ID, value = Dist.CLIENT)
public class GadgetPartDurabilityOverlay {

    private static final int ICON_SIZE = 16;

    private static final int COLOR_GREEN = 0x3DDC3D;
    private static final int COLOR_YELLOW = 0xE6D732;
    private static final int COLOR_RED = 0xE0403D;

    @SubscribeEvent
    public static void onInventoryBackground(ContainerScreenEvent.Render.Background event) {
        GuiGraphics graphics = event.getGuiGraphics();
        int left = event.getContainerScreen().getGuiLeft();
        int top = event.getContainerScreen().getGuiTop();

        for (Slot slot : event.getContainerScreen().getMenu().slots) {
            if (!(slot.container instanceof Inventory)) continue; // only the player's own inv/hotbar

            Float fraction = partDurabilityFraction(slot.getItem());
            if (fraction == null) continue;

            drawBar(graphics, left + slot.x, top + slot.y, fraction, 0xFF);
        }
    }

    @SubscribeEvent
    public static void onHotbarLayer(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.HOTBAR.equals(event.getName())) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int centerX = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() - ICON_SIZE - 3;

        for (int i = 0; i < 9; i++) {
            Float fraction = partDurabilityFraction(player.getInventory().items.get(i));
            if (fraction == null) continue;

            int x = centerX - 90 + i * 20 + 2;
            // Semi-transparent (0x99) since this draws ON TOP of the already-rendered icon --
            // see the class javadoc for why the hotbar can't get a true background like the
            // inventory screen does.
            drawBar(graphics, x, y, fraction, 0x99);
        }
    }

    /** Fills the bottom {@code fraction} of one 16x16 icon cell at ({@code x}, {@code y}). */
    private static void drawBar(GuiGraphics graphics, int x, int y, float fraction, int alpha) {
        int barHeight = Math.max(0, Math.min(ICON_SIZE, Math.round(ICON_SIZE * fraction)));
        if (barHeight <= 0) return;

        int color = (alpha << 24) | colorFor(fraction);
        graphics.fill(x, y + (ICON_SIZE - barHeight), x + ICON_SIZE, y + ICON_SIZE, color);
    }

    private static int colorFor(float fraction) {
        if (fraction >= 0.75f) return COLOR_GREEN;
        if (fraction >= 0.25f) return COLOR_YELLOW;
        return COLOR_RED;
    }

    /** Remaining fraction (1.0 = full) of Sunder's mounted chain or Shatter's mounted head, or
     * {@code null} if the stack isn't one of those two gadgets or has nothing mounted right now. */
    private static Float partDurabilityFraction(ItemStack stack) {
        ItemStack part;
        if (stack.getItem() instanceof SunderItem) {
            part = SunderItem.mountedChain(stack).itemStack();
        } else if (stack.getItem() instanceof ShatterItem) {
            part = ShatterItem.mountedHead(stack).itemStack();
        } else {
            return null;
        }

        if (part.isEmpty()) return null;
        int maxDamage = part.getMaxDamage();
        if (maxDamage <= 0) return 1.0f; // unbreakable/no-durability part -- treat as always full
        return 1.0f - (float) part.getDamageValue() / maxDamage;
    }
}
