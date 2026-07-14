# Dermicraft Gear Stations Notes

Running log of decided design choices for **Gear Stations** — the set of three blocks (Dock, Workbench, Growth Chamber) that handle suit and gadget maintenance/progression. Companion doc to `dermicraft-suit-notes.md` and `dermicraft-gadget-notes.md`; also ties into `dermicraft-machine-notes.md`'s Operating Theater (OT) section, since Gear Stations reuse the OT's Floor/Knitting systems.

---

## Overview — the multiblock concept

- **No controller block.** Unlike the OT (which has a dedicated Brain/Core control block), Gear Stations bind together purely through the **OT Floor network** — placing/removing a station on a connected floor footprint triggers the same event-driven **Knitting** recompute the OT uses (structural-change-triggered, not per-tick).
- **Footprint:** a connected **5×5 area of OT Floor blocks** is the structure's footprint. Floor connectivity/reach rules are identical to the OT's own (Tier 1–2 adjacent-only, Tier 3–4 one-block gap; fluid-type capability and throughput are weakest-link per path).
- **Partial formation allowed:** any **2 of the 3** stations present on a connected footprint merge into a shared pool + GUI; the third can join later and extends the merge — no requirement that all three exist upfront.
- **Shared resource pool:** all resources (fluids and items) are shared across connected stations, gated only by what the connecting floor tiles can actually conduct (same hazard/throughput rules as the OT).
- **GUI access:** right-clicking **any** connected station opens the shared, tabbed GUI — one tab per currently-networked station — defaulting to the tab of whichever station was clicked.
- **Graceful degradation:** removing one station only drops its own tab/capability; the remaining stations keep working (standalone or still-merged with whichever remain).
- **Relationship to a full OT:** Gear Stations can sit on/connect to an OT's floor network for **resource sharing and recipe access only** (covered for free by the OT's existing "any attached Machine's recipes are OT-craftable" rule). The OT Core's own GUI does **not** get direct interactive control over Gear Station functions (no remote suit removal/point-spend/etc. from the Core). **Decided (closed): no remote status/monitoring tab on the Core GUI either** — a read-only pool-level readout was considered as a middle ground, but the final call is to keep Gear Station interaction fully separate from the OT Core GUI, full stop.
- **Station tiering:** each Gear Station has its **own tier**, separate from Floor tier, which **caps the suit/gadget tier it can process** — mirrors the general Machine-tier concept (all current Machines are Tier 1) and the OT Floor's weakest-link tier-gating pattern, but as its own distinct axis: **Floor tier gates what fluids/materials can physically reach the station; station tier gates the suit/gadget tier ceiling directly.**
  - **Station evolution:** each station's screen shows its own evolution fluid requirements. Once the shared pool meets them, the player presses a button to begin — a **timed** process (mirrors "Growth" language used elsewhere). **If interrupted, the consumed fluids are lost, but the station block itself is never damaged/destroyed.**
- **"Smart" status and the Brain requirement (confirmed, see `dermicraft-machine-notes.md` → Machine Evolution — Smart vs. Dumb):** Gear Stations qualify for the willing/button-press evolution model specifically because the **Brain is a required ingredient in each station's initial construction recipe** — this is the mod-wide concrete test for "smart" vs. "dumb" evolution, not a one-off exception. The Brain only needs to be present at initial build; it isn't required again in the station's own tier-up/evolution recipes. Full construction mechanic (paired two-block implant process) below under Construction; exact per-half ingredient quantities still TBD (see Open questions).
- **Fuel-empty behavior (confirmed, same rule as the OT):** Gear Stations have **no HP pool** and **know when to stop** — any fuel-driven process running at a Gear Station (Dock's refuel/repair/player-heal, Growth Chamber's tier-ups, Workbench's recharge/durability-repair) **halts immediately and preserves progress** when the shared pool runs dry, rather than degrading to reduced speed and draining HP the way a "dumb" Machine would. See Machine Evolution — Smart vs. Dumb, above, for the full mod-wide rule.

---

## Naming glossary

- **Grafting** — the act of putting the suit on.
- **Grafted** — the state of currently wearing the suit.
- **Field Amputation** — rushed/unassisted suit removal; costs the player damage (per the existing formula in `dermicraft-suit-notes.md` → Unassisted vs. Assisted Removal).
- **Assisted Amputation** — Dock-based suit removal; costs time instead of damage.

---

## Dock

**What it is:** The suit's rack/locker. The player steps in/on the block and its screen automatically opens (no separate right-click needed). While docked, the **suit is deactivated** and reactivates only on Grafting — it doesn't passively run/consume anything while stored. **Rack storage is networked** across the shared pool, same as Workbench's gadget storage — a player can Graft a stored suit from any connected Dock on the platform, not just the one it was Amputated at.

**Duties:**
1. **Grafting / Amputation.**
   - **Assisted Amputation:** locked-in channel, **1 second per point of would-be damage** (reuses the existing unassisted-removal damage formula from `dermicraft-suit-notes.md` as the time basis). The **player remains damageable** while locked in — the Dock does not grant safety from combat, only from the removal mechanic itself. A dedicated **keybind cancels the channel at any time with zero penalty** (wasted time only, no partial-damage cost).
   - **Field Amputation** (rushed, no Dock needed) is unchanged — costs damage per the existing formula.
2. **Add-on (Frame) slotting** — inserting/removing a suit add-on's Frame into/out of the suit's body slots. **Near-instant (~1 second), no fuel/material cost.** Lore framing: the add-on is injected into or drawn out of the suit.
3. **Automatic passive refuel + repair.** Runs continuously and automatically while docked (no button needed to start it), drawing from the shared pool. A **click-and-hold button** applies a **stepped whole-number speed multiplier** (2×, 3×, etc.) at proportionally scaled fuel cost; releasing drops back to normal (1×) speed — it never stops the process, only speeds it up. This is the **universal fallback** available to every suit regardless of whether the Auto Heal add-on is equipped — no mechanical overlap with Auto Heal (which remains the *field* option for players who don't want to return to a station at all). **Repair only ever concerns the suit's own durability** — add-ons/gadgets have no durability stat at all; fuel cost during use is their "wear" equivalent instead (Workbench's "repair-while-stored" duty is listed for future-proofing in case that ever changes — see Open questions).
4. **Player-heal reward while worn and docked.** If the player stands **still**, still wearing the suit, inside the Dock while repair/refuel is running, the Dock also **continuously refills the player's hunger bar**, letting vanilla's own regeneration (fast at full hunger) do the actual healing — no separate heal-rate system. The player can freely interact with the GUI while standing still for this. Costs a **slight additional fuel draw** on top of the base repair cost, using the mod-wide **Fluid-to-Hunger Conversion rate** (50 mB Crude per hunger point, scaling down with Heal modifier for better fuel — see `dermicraft-slurry-notes.md`). This heal-bonus rate is **fixed** — it does not scale with the hold-to-speed-up multiplier used for repair/refuel.
5. **Point-spend menu — primary home.** Suit point-spending (the 13-category list in `dermicraft-suit-notes.md`) is done here. The **Tablet** offers secondary/portable access to the same menu (see Tablet section below).
6. **Suit respec.** Requires the suit be **docked**. A **time-locked channel** (same family as Assisted Amputation), duration scaling with **how many points are being reallocated**, plus a **mild fuel draw** from the shared pool while it runs. Resets the suit to a **full tier-baseline snapshot** — i.e., exactly how it was configured the moment it left the Growth Chamber at the end of its last tier-up. This includes **slot relocations** — no special-case permanence; slot relocation is just point-spend category #1 and resets like everything else. Goal: give respec real weight without punishing "wrong" choices (no lost points/materials, only time + a mild fuel cost).
   - **Duration formula (confirmed): 1 second per point**, reusing Assisted Amputation's exact rate rather than a new number. **Flat/fixed** — unlike Growth Chamber's fuel-Speed-scaled processes, respec time does not change with fuel grade; fuel is only a flat draw cost layered on top, matching Assisted Amputation's own model.
   - **Point count = total points currently spent on the suit**, not a net-change count — since respec always resets everything to tier-baseline, duration is based on the full amount invested, and is therefore knowable to the player *before* the channel starts.
   - **Pre-commit warning (confirmed):** before the channel begins, the Dock shows the player the **calculated time cost** and requires an explicit **Confirm or Cancel** — separate from the mid-channel cancel keybind (which stops an already-running channel with zero penalty). No surprises: the player always knows the full cost before committing, same "no surprises, plan ahead" pattern used elsewhere in the suit system (e.g. the OT's last-resort-recipe warning).

---

## Workbench

**What it is:** The gadget maintenance hub — "more manual/handy work" than the automated Dock.

**Duties:**
1. **Internal gadget storage.** No physical slot display in the initial design (**a physical display is a likely future update**, not part of the current mechanical design). Storage is accessible to Dock and Growth Chamber via the shared pool. **Capacity (confirmed): starts at 9 slots** (one row of a single Chest), **doubling at every tier upgrade** (9 → 18 → 36 → 72...) — same mod-wide multiplicative-growth convention used for Chitin Tank, rather than a bespoke curve. Deliberately tight at the start, consistent with the "scarce, valuable slots" philosophy already established for suit add-ons. **Timing rationale:** Gear Stations are expected to typically get built near the **end of Stage 1**, so the first tier-up (and its capacity bump) should follow relatively soon after construction in normal play — a player who rushes to build Gear Stations earlier than that simply lives with the tight 9-slot window longer, a deliberate consequence of that choice rather than something the base number needs to compensate for.
2. **Passive recharge-while-stored** *(renamed from "repair-while-stored")*. Gadgets/add-ons have **no durability stat** — fuel cost during use is their only "wear" mechanic (confirmed, see `dermicraft-suit-notes.md`) — so this duty passively **tops off a stored gadget's own internal fuel/charge** (e.g. the Grapple's Chambered Fuel Cell) from the shared pool, mirroring Dock's passive suit refuel. A no-op for gadgets with no fuel dependency of their own (e.g. Bio Vision Goggles) — same as Dock's repair doing nothing to an already-undamaged suit.
3. **General-purpose durability repair (new).** Separate from #2 — the Workbench also passively repairs **any stored item that carries actual vanilla-style durability**, not limited to gadgets/suit equipment. **No allowlist/restriction (confirmed twice):** a stored vanilla tool/weapon, one of the mod's own Tools (Scalpel/Syringe), or in principle any `Damageable` item from any source all qualify equally. **Cost model (confirmed):** not a flat fuel-per-point charge — instead it **reuses the standard Slurry Speed/Use-rate model** every Machine already runs on. Baseline anchor at Crude Slurry: **1 durability point repaired per cycle** (10 ticks / 0.5s — the same "effective 1.0 = 1 progress/cycle" convention used mod-wide, see Machine health and fuel system). Repair speed scales with the loaded fuel's Speed modifier, fuel drain scales with its Use-rate modifier — better Slurry grades repair faster *and* cheaper, same curve as every other Machine, no bespoke formula needed. `duration = points needed ÷ speed`, `fuel spent = duration × use-rate`. Starting anchor only — open to a flat modifier later if 1 pt/cycle feels off in practice.
4. **Frame loading/unloading** — inserting/extracting the physical gadget into/out of its own Add-on Frame. **Instant, crafting-table-style interaction** (click to swap), no time cost.
5. **Exchangeable parts** (e.g. Goggle Frame's swappable vision modules). Same instant, crafting-table-style interaction as Frame loading.
6. **Insertion only — no crafting.** The Workbench only inserts already-made parts/gadgets; actual fabrication of Frames/parts/gadgets routes through the **OT**, consistent with its role as the universal crafting front-end.

---

## Growth Chamber

**What it is:** Handles **all tier upgrades** — both suit tiers and gadget-internal tiers (e.g. leveling up the Grapple itself). Named for the mod's "Growth/Evolution" theming.

**Mechanics:**
- **Timed process**, not instant — consistent with "each upgrade is growth/evolution" and the general expectation that machines take time to work.
- **Fluid-only cost.** The upgrade is **injected into or printed onto** the existing suit/gadget — reuses the OT's existing "printing" recipe method (materializing a result from fluid) rather than a combine-style recipe.
- **Tanks holding the required fluids must be present on the same platform**, connected via the standard floor network — subject to the Floor's fluid-hazard-tier gating (a Tier 1 floor can't conduct a fluid a high-tier upgrade needs, same weakest-link rule as the OT).
- **Time is driven by fluid volume required, which itself scales with tier** — no separate time-per-tier formula. This makes tier "look like" the reason for the time increase by construction, since the recipe's own fluid cost is what scales.
- **Gadget tier-ups:** fluid-only, **independent recipe per gadget** (each gadget defines its own upgrade cost).
- **Suit tier-ups:** fluid-driven; exact recipe/material families not yet decided.
- **Extraction requirement (gadgets only):** a gadget must be pulled from its Add-on Frame at the **Workbench** before the Growth Chamber can upgrade it — no in-frame upgrading. This does **not** apply to the suit's own base tier — the suit itself can be tier-upgraded with its add-ons still docked, since the suit's own tier and an individual add-on's tier are different objects.
- **Full gadget-upgrade flow, worn gadget (corrected):** Dock (unslot the Frame from the suit) → Workbench (extract the gadget from the Frame) → Growth Chamber (upgrade) → Workbench (reinsert gadget into Frame) → Dock (reslot Frame into suit). The Dock steps only apply if the gadget was actually worn/slotted at the time.
- **Standalone (never-docked) gadgets** skip the Frame-related steps entirely — with nothing to extract, they go straight to the Growth Chamber.
- **Visual:** the suit/gadget is shown physically sitting inside the chamber during the process; the actual interaction is GUI-driven.

---

## Tablet — expanded role

- Already established (`dermicraft-suit-notes.md` / `dermicraft-machine-notes.md`) as the OT's first living-material Gadget.
- **New duty:** secondary/portable access to the point-spend menu (Dock remains the primary home).
- **Confirmed direction:** the Tablet may grow into a broader equipment-management hub over time (e.g. eventually the deferred remote-monitoring idea), but **only add a function when it clearly belongs there and nowhere else** — a deliberate soft rule to avoid overloading the player with a junk-drawer menu.

---

## Construction (confirmed — paired-implant mechanic)

**Status:** Fully decided — mechanic and all six recipe halves (see Full recipe set, below).

**Physical form:** Each Gear Station is **at least two blocks tall**, a bottom half and a top half, each with its **own separate implant recipe/process** (its own Inert Tumor, sutured and injected independently) — not one recipe with an auto-placed companion block. Reuses the mod's existing **implant recipe type/tooling** twice per station rather than inventing a new recipe type.

**Pairing/transformation:** Follows the **vanilla door precedent** — once both halves exist in the correct paired position (bottom directly below top) and both have independently completed their own implant process, they **transform together** into the finished Gear Station, the same "paired blockstates validate and resolve" logic vanilla already uses for doors/tall grass/beds. No strict build order between the two halves.

**Cost split (confirmed):** Only **one Brain Block total per station**, not one per half — **the bottom half's recipe carries the Brain requirement** (mirrors the OT's own Floor network always touching ground — the "base" half is the natural place for the station's defining smart-ingredient), matching the "same cost as the OT's own control block" target exactly rather than doubling it. The top half uses an ordinary implant shape (defining item + flesh + Primitive Catalyst, no Brain).

**Whole-structure pickup/placement:** Once formed, the Gear Station is **picked up and placed as a single unit** (both halves together), same as a vanilla door — not two independently Forceps-recoverable blocks.

**Full recipe set (confirmed, all three stations):** all halves inject with the standard **100 mB Primitive Catalyst**, sutured, following the mod's standard implant shape.

| Station | Half | Defining item(s) | Flesh | Brain? |
|---|---|---|---|---|
| **Dock** | Bottom | 1 Anvil | 2 Nerve Cluster | Yes (1 Brain Block) |
| **Dock** | Top | 2 Iron Bars | 2 Dense Muscle + 2 Nerve Cluster | No |
| **Workbench** | Bottom | 1 Chest | 2 Dense Muscle + 2 Nerve Cluster | Yes (1 Brain Block) |
| **Workbench** | Top | 1 Crafting Table + 4 Iron Bars | 2 Dense Muscle + 2 Nerve Cluster | No |
| **Growth Chamber** | Bottom | 1 Copper Block | None | Yes (1 Brain Block) |
| **Growth Chamber** | Top | 1 Glass Block + 1 Copper Block | None | No |

**Design notes on the picks:**
- **Dock:** Anvil (repair identity) pairs with the Brain and 2 Nerve Cluster on the bottom (the "thinking/control" half, housing the point-spend menu); the top's Iron Bars + Muscle + Nerve reads as "bars = rack frame, muscle = moves the suit into place, nerve = controls it" — reasoned-from-logic flavor, not arbitrary.
- **Workbench:** Chest bottom mirrors the Craw's own defining item (same bulk-storage lineage). Crafting Table + Iron Bars top reinforces "hands-on work," and the Iron Bars choice echoes Dock Top's — an unplanned but welcome recurring "Iron Bars = holds/racks equipment" visual motif across the two stations.
- **Growth Chamber:** deliberately **no organic flesh at all**, unique among every Machine/Gear Station recipe in the mod so far (all others use Dense Muscle/Nerve Cluster as living "control tissue"). Reads as coherent rather than an oversight: the Brain itself supplies the intelligence/control this station needs, so there's no separate nerve tissue to add. Both halves are solid Copper Block (bottom) and Copper Block + Glass (top, the viewing window) — giving the whole chamber solid copper inner walls, and resolving the earlier concern about reusing Drooling Cauldron's own defining item (Cauldron was dropped entirely in favor of Copper Block).

## Open questions

- Exact station-tier requirement numbers and evolution-fluid recipes for Dock/Workbench/Growth Chamber themselves.
- Suit tier-up fluid recipe/material families for the Growth Chamber (gadget tier-ups are separately confirmed fluid-only, independent per gadget).
- Exact respec channel duration formula (points-reallocated → time).
- How far the Tablet's management-hub role eventually grows, and in what order.
