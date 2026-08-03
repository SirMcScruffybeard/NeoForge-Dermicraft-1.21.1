package net.scruffy.dermicraft.item.custom;

import net.minecraft.world.item.Item;

/**
 * A spare chain material for Sunder, standalone/unmounted form -- plain vanilla durability (a
 * spare wearing out and being consumed at 0 is normal tool behavior, unlike the mounted-on-Sunder
 * representation, which can't use vanilla durability -- see the Chain durability design notes in
 * {@code dermicraft-gadget-notes.md}).
 *
 * <p>One class shared across materials for now, registered once per material in {@code ModItems}
 * -- per-material stats (damage multiplier, Bleed/decap chance, durability pool) aren't wired up
 * yet, that's a separate design decision once more than one material exists to design against.
 */
public class SunderChainItem extends Item {
    public SunderChainItem(Properties properties) {
        super(properties);
    }
}
