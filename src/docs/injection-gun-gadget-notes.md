# Injection Gun

**Target file for merge:** `dermicraft-gadget-notes.md` — place as a new entry under Known Gadgets, alongside Drinker/Sipper/Eater/Lobber/Grapple.

**Status:** Concept fully specified. Not yet implemented.

**Origin note:** A craft-up replacement for the plain Syringe — multiple fluid cells the player can cycle through, so injecting different fluids (or injecting several doses) no longer means carrying multiple syringes or walking back to a source between each use. Real-world inspiration: the Ped-O-Jet, a rapid-repeat mass-vaccination jet injector. Framed as a bridge between Tool and Gadget rather than a fully "living" gadget like Drinker/Sipper — it's purely mechanical (no Proto Brain/processor), but complex enough to warrant Gadget-tier presentation (GeckoLib model, HP).

## Core concept

A handheld multi-cell injector: 5 independent fluid cells, one active at a time, cycled through like a revolver's chambers. Shaped like a revolver.

## Interaction model

- **Cycle:** tap with no valid target cycles the active cell. A valid fill or injection target always wins over cycling — same precedence Drinker uses between siphon targets and its mode cycle.
- **Fill (world):** tap on a valid, `injectable`-tagged fluid source draws one 100 mB dose instantly (same feel as the old Syringe's single click). Continuing to hold draws another dose every ~1–1.5s (20–30 ticks) — no artificial cooldown needed on the tap path, since manual click speed can't reliably beat that interval anyway, so holding naturally wins over spam-clicking. Works on any fluid handler, not just Drinker. A cell that already holds a different fluid refuses the fill outright (no mixing, no auto-switching cells).
- **Inject:** single tap, one dose, consumed from the active cell into whatever target currently accepts `IInject` (mirrors the current Syringe's injection behavior exactly).
- **GUI:** crouch-right-click opens a screen with 5 fluid slots, one per cell, filtered by the same `injectable` tag (the filter is never bypassed here) — lets the player bulk-fill/drain cells from inventory containers instead of hunting down a world source. This is the first item-held GUI in the mod; every existing Menu/Screen pair is opened from a block entity, not a stack, so the menu-backed-by-item-data plumbing is new, though the fill/drain math itself should go through the existing `ModFluidUtil`/`IFluidHandlerItem` helpers rather than reinventing transfer logic. Each cell's slot also gets a "make active" button, so the GUI can select the active cell directly instead of only cycling in the field. GUI slot access **overrides** the "only the active cell is exposed to world fluid handlers" rule — it's an explicit 1:1 mapping, not the ambient world-interaction gesture, so all 5 cells are reachable at once through it.

## Capacity

- **5 cells**, all the same size: 500 mB each (5 doses of 100 mB), for 25 total doses across up to 5 different fluids.
- **No upgrade tree.** Deliberately full capability from creation — specialized enough that it doesn't need a tier ladder the way machines/other gadgets do.

## Fluid gating

- **Own `injectable` fluid tag**, independent of the mod-wide `HazardProfile` system. An injector isn't a general hauler like Drinker — it only ever needs to move fluids actually meant to go into a tumor, so gating by general hazard tolerance doesn't fit; a positive allow-list does.
- A fluid can be both `injectable` and separately tagged hazardous (e.g. something under `EXTREME_HEAT`) — the injector can knowingly carry something Drinker would refuse to touch.

## Presentation

- **GeckoLib model, Gadget-tier presentation:** HP + death flourish (`onGadgetDeath`), matching Drinker/Sipper's `IGadget` pattern, despite being framed as a Tool/Gadget bridge rather than a full living gadget.
- **One window** near the top of the model, reflecting only the **active** cell (not all 5 at once): dark/greyscale when that cell is empty, swaps texture and tints per-fluid when filled — reuses the same tint-per-fluid approach as the bucket fill-layer (`registerBucketTint`, keyed off the fluid's `getTintColor()`) rather than inventing new per-fluid color data. Shows fluid **identity only**, not amount.
- **Tooltip** reports the selected cell's exact fluid and amount; an action-bar message also fires on cycle/fill, matching Drinker/Sipper's existing UX.
- New gauge and button textures needed for the GUI — five cell gauges, five matching item slots, and per-cell activate buttons should follow the mod's existing [[feedback_gauge_slot_tooltip_naming]] convention (gauges get bare role names, item slots get a Container/Ingredient/Pattern/Result-style suffix) for tooltip consistency with the machine screens.

## Naming

**Injection Gun** — plain mechanical name rather than a full D.R.I.N.K.E.Rig-style acronym, deliberately marking it as the Tool/Gadget bridge it is rather than overselling how "alive" it feels. Inspired by the real-world Ped-O-Jet mass-vaccination jet injector.

## Fabrication recipe

**Gadget Fabricating**, Tier 1, 60 seconds.

- **Items:** 1× Chassis (gadget frame), 1× Syringe (craft-up requirement — the plain Syringe becomes an ingredient, not a dead end), 5× Glass Flask (one per cell — the flask maps 1:1 onto each fluid cell, same as the model's window will visually).
- **Fluids:** 3000 mB Ferrous Blend (3 iron ingots' worth — pricing the revolver-shaped metal housing/mechanism; landed between the Iron Sunder Chain's 3000 mB and a much heavier item like the Cauldron's 7000 mB).
- **No Proto Brain.** Explicitly not needed — Proto Brain represents a processor/"mind," and the Injection Gun is purely mechanical with no wiring or processing to justify one.

## Open questions

- None currently blocking — full concept specified through the design conversation. Implementation details (data component shape for the 5-cell array + active index, the new item-held-GUI plumbing, actual model/animation work) not yet raised.
