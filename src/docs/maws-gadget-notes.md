# MAWS — Mobile Autonomous Ware Storage

**Target file for merge:** `dermicraft-gadget-notes.md` — place as a new entry alongside other gadget concepts (Nose on a Stick, Eater, Lobber, etc.)

**Status:** Concept fully specified. Not yet implemented.

**Origin note:** Started as a proposed item-side mirror to Sipper (Sipper ⇄ Drinker :: this ⇄ Eater). Abandoned that framing — nothing in the mirror earned its own purpose, since Machine item slots are already hand- and duct-accessible (no fluid-style manual-interaction gap for items to fill). Landed on a genuinely different niche instead: portable mass storage, closer conceptually to a carry-around Craw than to Sipper. Name settled after several passes — final form leans into the "devouring mouth" pun implied by the acronym while dodging the "deployable structure" connotation "Warehouse" would have implied.

## Core concept

A handheld gadget for mass item storage that never needs to be placed down to use.

## Interaction model

- Right-click (in hand, in air) → opens a GUI.
- Right-click an inventory (e.g. a chest) → deposits all valid items from the gadget into that inventory.
- Right-click an inventory empty-handed while holding this gadget → behaves like an empty-handed right-click, but items draw into the gadget instead of the player's main inventory.
- **Auto Pickup toggle:** when on, world item pickups go into the gadget instead of player inventory.
- **Disposal (void) slot(s):** items placed here are voided. Originated as the actual itch behind the original "item Sipper" idea (no existing item-voiding tool in the mod); folded in as a feature rather than being the gadget's whole reason to exist.

## Capacity model

- **Storage slots come from a fixed per-tier grant** (not point-purchased), following the suit convention of fixed structural growth vs. point-purchased stats:
  - T1: 9 slots
  - T2: 18 slots
  - T3: 27 slots
  - (+9 slots per tier — "add a row" logic; T3 lands exactly on a chest's 27-slot count, though total capacity exceeds a chest well before T3 once stack cap points are spent)
- **Stack cap starts at 1** (base, no points spent) and is a **device-wide multiplier**, not per-slot — buying stack cap points applies uniformly across all slots, including slots gained later via tier-up. Points already spent are never diluted or wasted by a later tier upgrade.

## Upgrade mechanic

Suit-style point-spend — reuses the suit add-on point-menu **mechanic**, but MAWS has its **own separate point pool**, not a shared currency with the suit (a broader pattern now settled: every complex gadget and the suit each get their own independent pool; only the point-spend mechanic itself is shared across them). Uniform 1-point cost per rank across all categories, matching the suits' own point-menu convention (13 categories, all 1 point/rank, magnitude-per-rank tuned individually rather than cross-balanced).

### Point-spend menu (all 1 point per rank)

1. **Stack cap** — +1 stack per slot, device-wide, inherited automatically by slots gained via tier-up.
2. **Storage-slot filtering** — unlocks filtering capability across all storage slots at once (device-wide).
3. **Void-slot filtering** — unlocks filtering capability across all disposal slots at once (device-wide); inherited automatically by disposal slots bought after the toggle is unlocked.
4. **Extra disposal slot** — +1 disposal slot per point.

## Bladder hot-swap

Automatic inventory-management feature: when a bladder in the player's inventory (not inside MAWS) hits empty, it's swapped for a full one of matching type, provided one is in MAWS storage.

- **Applies to Ammo and Feeder bladders only.** Fluid bladders are explicitly excluded — nothing in the current design drains a fluid bladder in a way this mechanic assumes, so it doesn't apply.
- **Matching:** Ammo and Feeder bladders are purpose-specific containers (unlike Fluid bladders, which function more like a generic bucket), so swap-matching requires the *specific* ammo type or feed type, not just container class.
- **Trigger:** fully automatic, no player action required.
- **On trigger:**
  - If room available in MAWS storage → empty bladder is pulled into MAWS, full matching bladder takes its place in player inventory.
  - If no room in MAWS storage → full matching bladder is still added to player inventory; the empty one stays put (not pulled in).

## Resolved (previously open)

- ~~Bladder-sharing (fuel/ammo access for other gadgets via MAWS as a hub)~~ — **Cut**, replaced by the bladder hot-swap feature above, which solves the practical annoyance (walking around with a dead bladder) without turning MAWS into a hub other gadgets depend on.
- ~~Name~~ — **MAWS: Mobile Autonomous Ware Storage.**

## Remaining open items

- None currently — full concept specified. Implementation details (Code discussion) not yet raised.
