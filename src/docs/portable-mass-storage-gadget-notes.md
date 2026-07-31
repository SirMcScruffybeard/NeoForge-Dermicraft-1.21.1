# Portable Mass Storage Gadget *(name TBD)*

**Target file for merge:** `dermicraft-gadget-notes.md` — place as a new entry alongside other gadget concepts (Nose on a Stick, Eater, Lobber, etc.)

**Status:** Concept established, not yet implemented. Capacity numbers and full feature-point costs still open.

**Origin note:** Started as a proposed item-side mirror to Sipper (Sipper ⇄ Drinker :: this ⇄ Eater). Abandoned that framing — nothing in the mirror earned its own purpose, since Machine item slots are already hand- and duct-accessible (no fluid-style manual-interaction gap for items to fill). Landed on a genuinely different niche instead: portable mass storage, closer conceptually to a carry-around Craw than to Sipper.

## Core concept

A handheld gadget for mass item storage that never needs to be placed down to use.

## Interaction model

- Right-click (in hand, in air) → opens a GUI.
- Right-click an inventory (e.g. a chest) → deposits all valid items from the gadget into that inventory.
- Right-click an inventory empty-handed while holding this gadget → behaves like an empty-handed right-click, but items draw into the gadget instead of the player's main inventory.
- **Auto Pickup toggle:** when on, world item pickups go into the gadget instead of player inventory.
- **Disposal slot:** included from the base concept — items placed here are voided. Originated as the actual itch behind the original "item Sipper" idea (no existing item-voiding tool in the mod); ended up folded in as a feature rather than being the gadget's whole reason to exist.

## Capacity model

Fewer visible slots than a chest (27), but total storage capacity matching or exceeding a chest via a per-slot stack multiplier (each slot holds multiple stacks, e.g. 2–5). Both slot count and per-slot stack cap scale with upgrades.

Exact Tier 1–3 numbers not yet locked — mid-negotiation options included matching a chest exactly at Tier 1 (9 slots × 3 stacks = 27) with a target of +1 chest's worth of capacity (+27 stacks) per subsequent tier; clean whole-number math for T2/T3 under that target didn't resolve cleanly, discussion moved to upgrade mechanic before finishing.

## Upgrade mechanic

Suit-style point-spend — draws from the **same upgrade point pool as suit add-ons** (not a separate currency). Justified by feature count: this gadget has enough qualitatively different optional features to warrant a point-spend system rather than a fixed tier curve.

### Confirmed point-spend candidates

- Slot count
- Per-slot stack cap
- Filter toggle per slot (including the disposal slot)
- Additional filtered disposal slots

### Parked/open idea — bladder-sharing

Allow other gadgets to draw fuel/ammo from bladders stored in this gadget. Flagged as structurally different from the other features — this would make the gadget a hub other gadgets pull from (closer to a Machine's role) rather than pure personal storage. Not yet decided whether it belongs in this gadget's point tree or should be split into its own design topic.

## Open questions

- Final Tier 1–3 capacity numbers (slot count × stack cap).
- Point costs for each spendable feature.
- Whether bladder-sharing is in-scope here or elsewhere.
- Name.
