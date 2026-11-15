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

## GUI design (decided with the user, 2026-11-14)

- **Two tabs, Home and Field**, using the shared tab bar (`AbstractModScreen` tabs, `AbstractModMenu` tab state; tab clicks
  use button ids 200 and up, so every other button id in these menus must stay below 200).
- **Home:** the fuel slot and gauge, the output buffer row, and a readout at the top that now says "Field" and shows the
  field size (for example "Field: 5 x 7"), replacing the old "Range: NxN". The preview dots leave Home.
- **Field:** the field-size steppers (Width and Length, 1 to 9 each, plus/minus with the hold-to-repeat the Shepherd's cap
  buttons already use) and the preview dots, now drawn width by length, larger than today's 3 pixel cells, with a faint
  outline of the 9 x 9 cap. The fuel-cost readout also lives here once the formula exists. The player inventory can be
  dropped on this tab for room; slots on the other tab are hidden while it is active (the existing tabbed menus already
  do this).
- **Mr. Shepherd:** the XP side strip is removed. The XP tank and its slot become a horizontal row above the fuel row,
  identical in style (the horizontal gauge renderer already exists). The population cap moves off Home. Either onto the
  Field tab or onto a tab of its own: the user has not chosen between the two yet.
- The server owns the values. The block entity stores the width and length, the buttons send a packet and the server
  clamps, and the menu syncs the result back. The close-GUI particle preview uses the same two numbers.

## Shared code (decided direction, 2026-11-14)

Goal: as much shared as possible, so Mr. Logger and Mr. Hunter are cheap to add. Planned layers:
- An **abstract farm block entity** over `MachineBaseBlockEntity`: fuel tank and fuel-slot handling, the buffer row and its
  insertion, field width and length, the wave timer and pacing, the range preview, the facing and ACTIVE blockstate
  handling, the automation handler, the Gate channels and the save and load of all of it.
- An **abstract farm menu** (fuel slot, buffer row, tabs, field buttons) and an **abstract farm screen** (the Home and
  Field layouts, the field-size control), so each machine only adds its own slots and extras (Farmer: seed reserve and
  crop logic; Shepherd: food slots, XP row, population cap; later Logger and Hunter).
- A shared farm **block** base for the facing and ACTIVE properties.

### Overlap found when reading the code (2026-11-14)

Mr. Farmer's block entity is about 990 lines and Mr. Shepherd's about 780. Both extend `MachineBaseBlockEntity` directly and
repeat the same machinery, much of it line for line:
- `getRange()` is identical in both (fuel tier to 1, 3, 5, 7 or 9, capped at tier 4), plus `getFacing`, `setActive`,
  `pacingSpeed`, `currentWaveStepTicks` and `fuelPerStep`, and the same "stop without enough fuel" gate at the top of the tick.
- The fuel tank factory and the item handler factory with the fluid-transfer-from-container logic (about 90 lines each)
  are the same code. The two copies have **drifted**: the Shepherd's is the newer one, generalized to take any tank (so it also
  serves the XP tank), while the Farmer's keeps a defensive multi-container fallback that the Shepherd's lacks. Merging them
  resolves that drift (the unified version should keep the fallback).
- The automation item handler (extract-only buffer, player-only fuel slot), `describeFace`, `describeFluidFace`,
  `getChannels` (fuel first, buffer last), `drops`, display name and `createMenu` are the same shape; only the slot ranges and
  channel names differ.
- The range preview (`startRangePreview`, the preview timer, particle count) exists in both, with different footprints.
- The two block classes (`MrFarmerBlock` 123 lines, `MrShepherdBlock` 146) differ by about 47 diff lines once the names are
  normalized.

What genuinely differs: the Farmer's wave-by-wave crop work, seed reserve and lights; the Shepherd's box scan of animals, item and
XP pickup, shearing, breeding, food slots, the XP tank and the population cap and cull.

Also noted: the Farmer walks the field in ring-by-ring or row-by-row waves from a single `range`, and the Shepherd scans a box
from the same `range`. Both have to be converted to a width and a length. See the open geometry questions below.

## Open (geometry and fuel, before the build)

- Field geometry: is the field centered on the machine, or offset in front of it (the Farmer today works rows in front when
  the machine faces sideways, rings when it faces up or down)? With independent width and length, an even size cannot be
  centered, so either only odd sizes are allowed or even sizes shift to one side.
- What happens at no fuel: today an unfueled machine reaches only its own block. With the field set by the player, does an
  unfueled machine do nothing, work at a tiny default, or keep working a 1 x 1 field?
- What fuel tier means now that it no longer sets the range (the cost formula is deferred, per the earlier ruling).
- Whether the population cap is on the Field tab or its own tab.
