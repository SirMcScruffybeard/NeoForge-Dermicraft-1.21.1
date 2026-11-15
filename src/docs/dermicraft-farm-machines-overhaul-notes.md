# Farm machines overhaul (Mr. Farmer, Mr. Shepherd, Mr. Logger, Mr. Hunter)

Started 2026-11-14. This is the planning doc for the overhaul of the farming automation family. Only the goal list is
settled so far; goals 1 and 2 get detailed in later sessions (the user is going into them next). Nothing here is built.

The older design material for the family is in `dermicraft-machine-notes.md` under "Farming automation concepts (early
planning)" (shared mechanics, Mr. Farmer, Mr. Shepherd, Mr. Logger, Mr. Hunter). Where this doc and that section disagree,
this doc is newer. Mr. Farmer and Mr. Shepherd are built in code today; Mr. Logger and Mr. Hunter are not.

## Goals (user, 2026-11-14)

1. **GUI work.** Details to come from the user.
2. **Redo the fuel mechanic.** Details to come from the user. What exists today, and what the user already decided for it
   (backlog item 3, 2026-10-25), is under "Fuel mechanic: starting point" below.
3. **Model redo.** New models for the machines. (The early-planning note was a shared body model with per-block textures
   to begin with, individual models later.)
4. **Add Mr. Logger and Mr. Hunter.** Both exist only as early-planning concepts in the machine notes. They join the
   family in this overhaul.

## Fuel mechanic: starting point

Today (machine notes, "Shared mechanics"): fuel decides the machine's range. Unfueled, the range is the single block the
machine sits on or above. Each fuel tier (the `BiofuelProperties.tier` field) adds one ring: tier 1 is 3x3, tier 2 is 5x5,
tier 3 is 7x7, tier 4 is 9x9, capped at 9x9.

Decided 2026-10-25 (backlog item 3), and the reason this is goal 2: the user wants to move away from fuel deciding the
field size.
- The player sets the field size in the machine GUI.
- Cap is 9 blocks each way. The field need not be square: width and length are set independently.
- Fuel cost then adapts to the chosen size.
- The fuel cost formula is deliberately deferred until the overhaul starts (that is now; it comes with goal 2).

Whether the same field-size control applies to Mr. Logger and Mr. Hunter is not decided. Mr. Hunter in particular may not
fit a ring or row pattern at all (see its open questions in the machine notes).

## Mr. Logger and Mr. Hunter: what is already written down

From the machine notes, as the starting point for goal 4:
- **Mr. Logger** (formerly "Lumbering Jack"): automates tree farming. Auto-plants saplings and auto-harvests grown trees
  in range, with the ring-by-ring wave behavior for both passes. Harvest is triggered by a tree-growth event, likely
  NeoForge's `BlockGrowFeatureEvent`, not yet checked against this project's NeoForge version. Open: visual design,
  whether it clears leaves, whether it handles several wood types at once.
- **Mr. Hunter:** automates mob grinding. A bare concept pitch with everything open: valid targets, how it kills, whether it
  needs a spawning platform, targeting model, loot handling.

## Open (to fill in as the user goes through goals 1 and 2)

- GUI: layout, field-size controls, how it fits the composited machine screen template
  (`feedback_machine_gui_composited_layout` in memory).
- Fuel: the cost formula, what fuel tier means once it no longer sets the range, and whether the fuel-optional/HP pattern
  applies to this family (the machine notes say this family's fuel-driven range "replaces" it).
- Models: what the new models should look like, one shared body or one per machine.
- Naming: renamed from "Mr. Shepard" to "Mr. Shepherd" on 2026-11-14 (code, registry ids, files, lang and docs); the user confirmed the correct spelling.
- Where Mr. Logger and Mr. Hunter sit in the stage and tier timeline (undecided for all four).
