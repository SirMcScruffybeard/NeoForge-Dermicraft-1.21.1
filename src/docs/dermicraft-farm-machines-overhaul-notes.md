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

## Decisions (user, 2026-11-14)

- **Odd field sizes only**, 1 to 9 each way (a stepper step is 2 blocks), so a field can always be centered on the machine.
- **An unfueled machine does nothing.** The field size shown does not change with fuel: the displayed field stays what the
  player set. Getting away from fuel deciding the field size is a main goal of this overhaul.
- **Fuel use and speed effects stay exactly as they are** (fuel burned per step with real work, fuel speed paces the work).
  The fuel tier no longer has any effect on the field.
- **Population cap gets its own tab** on Mr. Shepherd (Home, Field, Population).

## Built (2026-11-14, not yet tested in game)

- `AbstractFarmBlock`, `AbstractFarmBlockEntity` (block/entity/custom/base), `AbstractFarmMenu`, `AbstractFarmScreen` and
  `FarmLayout` (screen package, pure constants shared by the menu and the screen). Mr. Farmer and Mr. Shepherd now extend them;
  the Farmer's block entity went from about 990 lines to about 765 and the Shepherd's from about 780 to about 580, with the
  block classes down to codec, block entity and block entity type.
- The field (`fieldWidth`, `fieldLength`, default 3 x 3, saved) lives on the base block entity, with `setFieldSize` (snaps to an
  odd 1 to 9), `stepFieldWidth` and `stepFieldLength`. `getRange()` is gone.
- Farmer geometry: horizontal facing works rows in front, `length` deep and `width` wide, centered; vertical facing works a
  `width` (X) by `length` (Z) rectangle centered under or over the machine, swept as concentric rings clipped to the rectangle.
- Shepherd geometry: horizontal facing covers the machine's own row through `length` ahead and `width` across; vertical facing
  uses the older offset box with width along X and length along Z.
- Screens: Home (field readout, fuel slot and gauge, output buffer; the Shepherd adds the feed row and an XP row above the fuel
  row) and Field (Width and Length steppers with hold-to-repeat, and the preview grid at 6 pixel cells inside a faint 9 x 9
  outline). The Shepherd's XP side strip is gone. Steppers are registered per tab, so the Population tab reuses them.
- Menu button ids: width and length 10 to 13, the Shepherd's cap 0 and 1 (all below the tab ids from 200).

## To check in game

- (Resolved 2026-11-14) The tabs are now the 24 pixel tabs the Innards Node uses, stacked flush with the panel's top, each marked with an initial (H, F, P, from lang keys so a translation can pick its own) and showing the full name when hovered. Three of them are 80 pixels, so they no longer hang below the panel. The helper (`renderSmallTabs`, `smallTabClickedAt`, `getStackedSmallTabArea`) is in `AbstractModScreen`, shared by any screen.
- Whether the Field tab layout reads well (label positions, grid size), and whether the XP row sits right above the fuel row.
- A vertically facing machine has no "sideways", so width is world X and length is world Z there; confirm that feels acceptable.
- The Shepherd's automation extract used to allow pulling the XP container slot out; the shared handler now only extracts from the output buffer (all container slots are player-only).
- **Mr. Shepherd's bottom face is an output only (user, 2026-11-14):** the XP tank (Knowledge Colloid) as a drain-only fluid view and the output buffer as an extract-only item view; nothing can be inserted there (no fuel, no feed). Every other face is as before. The Gate's xp channel stays, and the Gate face descriptions mirror the bottom face.
- **Mr. Farmer's bottom face is an output only (user, 2026-11-14):** the harvest buffer as an extract-only item view, and no fluid access at all (its only tank is the fuel tank, so nothing goes in). Other faces as before; the Gate channels (fuel in, harvest out) stay.
  the output buffer (all container slots are player-only).
- The default field is 3 x 3 and nothing migrates old saves.

## Fuel cost follows the work (user, 2026-11-14, built, untested)

Why: before this, the Farmer burned one fuel step per wave step with any work (a whole row, so width was free) and the Shepherd one
step per cycle whatever the field, so with fuel no longer setting the field the cheapest play was always the widest field.

- **Mr. Farmer:** pays per farmland cell actually worked in a step; a 3-cell row is one fuel step. A 3 x 3 field therefore costs what
  it did before, a wider field costs proportionally more, and an unplanted or unfarmable field costs nothing.
- **Mr. Shepherd (revised, user ruling):** pays per work, not per field area. Each kind of work that did something in a cycle (item pick-up, XP pick-up, shearing, growing babies, breeding, culling) costs a third of a fuel step, so a typical cycle of about three kinds costs one step, as a cycle with any work did before, and a busy one costs up to two. The 3 is `WORK_KINDS_PER_STEP`, my calibration, to tune in play.
- Both go through `burnFuel(steps)` on the shared block entity, which carries a fractional step over to the next burn and never burns
  more than the tank holds. The "enough fuel for one step" gate and the fuel speed pacing are unchanged.
- Fuel tier (`BiofuelProperties.tier`) now does nothing to the field, and nothing else uses it yet. It stays unused for now.
- **Rule per machine (user, 2026-11-14): Mr. Farmer and Mr. Logger pay per cell, Mr. Shepherd and Mr. Hunter pay per work.** Mr. Logger and Mr. Hunter are not built yet; when they are, follow this.
- Open: where Mr. Logger and Mr. Hunter sit in the stage and tier timeline, and whether Hunter uses a field at all.
  Mr. Logger and Mr. Hunter use the same per-work rule, and where they sit in the stage and tier timeline.

## Models and textures (2026-11-14)

- Textures go in `textures/block/farm_machine/<machine>/`, named `<machine>_front`, `_back`, `_left`, `_right`, `_top`. Left and
  right are as seen looking at the front. The machines share the simple machines' L-tier plate, the l1 light mount, the on and off
  light, and the l1 port ring and center (the bottom face), referenced in place from `simple_machine/`.
- **Mr. Farmer is built** (`FarmMachineModels`, generated into `models/block/mr_farmer.json` and `mr_farmer_on.json`, replacing the
  hand-authored models): the plate as a base cube, the machine's faces over it, the light mount on the front centered across with
  its top edge 4 pixels down (x 5 to 10, y 6 to 11), the light inside it (off, or lit and emissive when the machine runs), the port
  ring and center on the bottom. The model is authored with its front on the north face and the existing blockstate turns it to
  the facing. The item model now parents to the block model; the old flat icon, the old `mr_farmer.png` and `mr_farmer_on.png`
  textures and the old icon are no longer referenced.
- Mr. Shepherd still uses its hand-authored models until its textures exist; `FarmMachineModels` takes the machine name, so it is
  one call per state once they do.

## Mr. Logger decisions (user, 2026-11-16)

- **Detection: scan route.** Each cycle Logger scans its field for grown trees and fells them. No growth event (`BlockGrowFeatureEvent`
  is dropped), so existing forests work too.
- **Leaves: a toggle.** Clearing leaves is a player toggle, on by default (clear them). With it off, leaves are left for vanilla decay.
  Open: the user said "clearing and harvesting a toggle"; recorded as one toggle covering the leaf pass and what leaves drop
  (saplings, sticks, apples). Confirm whether harvesting is meant to be a separate toggle from clearing.
- **Saplings: same split as Mr. Farmer's carrots and potatoes.** Of the saplings a felled tree and its leaves drop, the greater half
  (rounded up) goes to the output buffer and the rest to the hidden reserve, which replanting draws from.
- **Planting: fill the whole field** as long as enough saplings are stored. No spacing rule. User testing with bonemeal showed
  single-sapling trees grow clustered or staggered and 2x2 trees (jungle, dark oak) also grow clustered, so neither is a problem.
- **Light: the status light like Mr. Farmer**, on the front, centered in both directions (mount x 5, y 5 on the 16-pixel front).
- **Field size: starts 3 x 3 like the others, maximum 9 x 9 like the others** (revised 2026-11-17; it was 15 x 15). The user is considering tier-ups for the farm machines alongside the processing machines, not decided, so field maximums may later grow with tier.
- **Fuel only, no tool slot.** Per cell, per the family rule above.
- **Output: items only,** no fluid output. Bottom face output-only like Mr. Farmer.
- **Personified** like the other two.
- **Blocked on: field geometry for all four machines.** The user has noticed problems with how fields are laid out and wants them worked
  out as a whole before Logger (or Hunter) is built. Details to come from the user.
- **Leaf toggle, clarified (2026-11-16):** gathering items (saplings, sticks, apples) is always on. The toggle only decides whether
  leaves get broken (harvested); it is not two toggles. This replaces the "Open" note above.

## Field geometry problems found by the user (2026-11-16, not yet fixed)

- **Vertical facing does not work for Mr. Farmer.** Every farm machine is meant to tend its field when turned up or down. Exact
  symptom still to be pinned down (code review notes: Farmer's vertical field is found by probing 1 to 5 blocks along the facing axis
  for the first farmland, with the crop plane above it, so farmland directly next to the machine puts the crop cell on the machine).
- **Vertical field distance depends on field size (Mr. Shepherd).** Its vertical box is centered `max(halfWidth, halfLength) + 1` blocks
  out, so a bigger field also moves away from the machine. Seen via the preview particles. (Mr. Farmer's vertical field distance is
  found by probing and does not use the size.)
- **Idea (user): a distance ticker on the Field tab** for the distance from the machine, with room for it. Thoughts recorded in the
  chat; nothing built.
- Field geometry for all four machines gets worked out as a whole before Mr. Logger or Mr. Hunter is built.

## Field distance (built 2026-11-16)

- Every farm machine has a **Distance** setting, 1 to 8 (default 1), a third stepper row on the Field tab under Width and Length
  (`AbstractFarmBlockEntity.fieldDistance`, saved, menu buttons 14 and 15).
- **Vertical facing:** distance is the number of blocks from the machine to the work layer, the block right above the ground (the
  crop cells for Mr. Farmer; the layer the animals stand on for Mr. Shepherd). 1 is the block touching the machine. It no longer
  depends on the field size (Shepherd's old `max(halfWidth, halfLength) + 1` offset is gone), and Mr. Farmer no longer searches 1 to
  5 blocks for farmland: the layer is exactly where the setting says. A Farmer facing UP therefore needs a distance of at least 2,
  since the farmland sits under its crop layer.
- **Horizontal facing:** distance is where the field starts. Farmer's first row is `distance` blocks out (was always 1). Shepherd's
  box starts `distance - 1` blocks out, so the default still includes the machine's own row.
- Fuel cost does not use the distance.
- Mr. Logger (later) will use the same setting, with the same maximums as the others (1 to 8); for it the distance is where the work starts.

## Farm machine tiers and modules (direction, 2026-11-17)

User decisions: the farm machines get a **Module tab** and use the **same Evolution Modules as the processing machines**; their
evolution **follows what the processing machines do** (in-place evolution state: tier, Overgrowth, hazard levels, the dedicated
Evolution slot, gradual progress from real work, the creeping progress overlay, the Upgrade Stick, the `EVOLUTION_STATE` item
component). Their stats become **configurable like the rest** (a `MachineStatKey` section each, family fallback, `StatConfig`).
New textures come later: reuse the shared parts and fill in the tier accents with the textures that exist now.

Not decided: which stats a tier changes, and whether the farm machines get hazard-gated block variants (Charred-style).

Build order agreed in chat: tiers and modules first, then Mr. Logger, then Mr. Hunter (needs a design pass), then playtest.

Plan (code, in order):
1. `AbstractFarmBlockEntity` moves from `MachineBaseBlockEntity` to `EvolvingMachineBlockEntity`, opts in with
   `usesEvolutionState()`, returns a `MachineStatKey`. Mr. Farmer and Mr. Shepherd get keys (and a `FARM` family so the two, and
   Logger and Hunter later, share a table with per-machine overrides).
2. Stats read through `SimpleMachineStats`/`StatConfig`: speed and fuel use reuse the existing tier tables; **max field size** and
   **max field distance** become per-tier stats, replacing `MAX_FIELD_SIZE` and `MAX_FIELD_DISTANCE`. Saved sizes clamp through the
   tier's maximum, so a machine that loses tier (module removed, config change) shrinks its field safely.
3. A Module tab in the shared farm menu and screen: the Evolution slot and the general Module slots (count from the stat table),
   following the processing machines' Module tab.
4. Gradual evolution progress from real work (Farmer: a step that worked farmland; Shepherd: a cycle that did work), the shared
   overlay renderer, Upgrade Stick support, the `EVOLUTION_STATE` component on the block items.
5. Placeholder tier accents on the existing shared parts; real textures later.
