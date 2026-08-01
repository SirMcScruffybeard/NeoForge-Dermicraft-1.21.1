# Dermicraft Gadget Notes

Running log of decided design choices for Gadgets and the reasoning behind them. Add a new entry per gadget as decisions get made.

---

## Gadget naming convention

Each gadget's official name follows an acronym + suffix pattern (`[ACRONYM].Gadget` or `[ACRONYM].Rig`). The last letter of the acronym is the first letter of the gadget's full written-out word (exact full words not given yet). The two carried-over gadgets keep their old acronym names for now; renaming is part of the rework but not finalized, and it's undecided whether a rename will change the in-game display name, just the internal code identifier, or both.

**Simplified discussion names:** Because the acronym names are tedious to type repeatedly, simplified shorthand names will be used in conversation going forward. The acronym names above remain the canonical/real names for documentation purposes — this doc tracks entries under the real names regardless of what shorthand gets used in a given session. Confirmed so far: D.R.I.N.K.E.Rig = "Drinker," S.I.P.P.I.N.Gadget = "Sipper." Eater, Lobber, and Grapple (G.R.A.P.P.L.E) are also confirmed acronyms (full expansions still pending); Chainsword, Drill Hammer, and Capture Net Gun are discussion shorthand only, not yet confirmed as acronyms.

## Shared infrastructure

### Bulk item storage (`IHaveItemData` / `BulkItemData` / `BulkSlot`)

**Status:** Built 2026-07-29, wired to Eater's base tier as of 2026-07-31 (see Eater's entry below) and confirmed working in-game. Portable Mass Storage, the other planned consumer, hasn't started yet.

**What it is:** The item-side twin of `IHaveFluidData`, for gadgets that carry a multi-slot bulk item buffer inside their own held `ItemStack`. Built while planning Eater's internal item buffer, once it became clear Eater and the not-yet-built **Portable Mass Storage** gadget (see `portable-mass-storage-gadget-notes.md`) would both need this and should share one implementation rather than diverge.

**Why not vanilla's `DataComponents.CONTAINER`:** vanilla's container component (used by Bundles/Shulker Box items) validates every stack through the standard `ItemStack` codec, which caps count at 99 and crashes with `IllegalStateException` on save above that. Portable Mass Storage's per-slot stack multiplier (2–5× a normal stack, scaling with upgrades) can exceed 99, so vanilla's component can't back it — same problem Craw (`CrawBlockEntity`) already hit and fixed for block-entity storage.

**The fix, applied to a held item instead of a block entity:** `BulkSlot` keeps an item's identity as a count-1 template (component data and all) plus the real count tracked separately as a plain int, exactly like Craw's pattern. `BulkItemData` is a fixed-size (resizable via `withSize`, for tier upgrades) list of `BulkSlot`s, registered as the `BULK_ITEM_DATA` data component. `IHaveItemData.BulkItemHandler` wraps that into a normal `IItemHandlerModifiable`, parameterized by slot count, per-slot capacity, and an optional filter predicate.

**Decision — single shared backing for both consumers, not two:** Eater's slots (small, fixed at 4, never expected to exceed a normal stack) could have used plain vanilla container storage instead. Went with the bulk-capable store for both anyway: it costs Eater nothing at small numbers, future-proofs it if a later tier ever scales capacity past 99 (avoiding a breaking migration), and keeps one handler implementation to trust rather than two parallel ones drifting apart — consistent with the mod's existing preference for shared bases across tiers/families.

**Not yet decided by this groundwork:** how a "disposal slot" (Portable Mass Storage's void-on-insert feature) is expressed — no dedicated Disposal handler class was built here, since items have no mod-wide hazard-profile concept to gate against the way fluids do; left to whichever gadget needs it (a filter that always rejects, paired with the gadget discarding on insert, or something else) rather than baked into the shared interface.

**Open questions:** Whether Portable Mass Storage's filter-per-slot and disposal-slot features end up needing anything beyond what `BulkItemHandler`'s constructor already exposes (capacity, filter). Whether Eater's eventual registration wants a plain `BulkItemHandler` or needs its own thin subclass for any Eater-specific insert behavior (e.g. interaction with its Storage/Transfer/Disposal modes).

## Known gadgets

### D.R.I.N.K.E.Rig ("Drinker")

**Status:** Carried over from the old (pre-rebuild) version; needs reworking (including eventual rename — see naming convention above). Core function, capacity, fluid restriction, modes, and a durability quirk all established.

**What it is:** A fluid vacuum — drains fluid directly from world fluid blocks into its internal tank.

**Capacity:** `1000 mB` internal tank.

**Fluid restriction:** Tier 1 (see `dermicraft-project-primer.md` Stage structure) — cannot handle hazardous fluids like lava. Confirmed behavior: it simply will not attempt to drain a hazardous fluid at all, rather than draining it and failing/breaking afterward.

**Additional pull source (under review):** Currently can also pull from fluid-handling blocks (tanks, other machines with fluid capability), not just raw world fluid blocks. This may be removed — not finalized.

**Modes:**
- **Disposal** — erases fluid (void/trash mode, no recovery).
- **Storage** — holds fluid in the internal tank.
- **Transfer** — moves fluid to a fluid-handling item in the player's inventory if one is available; if not, falls back to storing the fluid in the internal tank until something becomes available.

**Mode switching:** Right-click cycles through the three modes. Whether the player is crouching or standing while right-clicking determines which direction the cycle moves — lets the player step forward or backward through Disposal/Storage/Transfer without overshooting. If fluid is currently stored, attempting to change mode starts a confirmation timer — repeat the action to confirm the change, or wait and it cancels (stays on the current mode). Prevents accidentally dumping/disposing of stored fluid with a stray right-click.

**Durability:** Takes damage from being dropped on the ground. Currently, if it breaks while holding fluid, the fluid simply disappears (no spill) — a spilling mechanic is being considered for the future but not yet implemented.

**Tier 2 requirement — hazardous mob-sourced fluids (confirmed):** Drinker's Tier 1 hazardous-fluid refusal now has a confirmed real case: **Dragon's Milk** (harvested from the Ender Dragon — see `dermicraft-crafting-notes.md`) is hazardous, so a base Tier 1 Drinker will refuse to draw it outright — same "won't even attempt it" pattern as any other hazardous fluid. A **Tier 2 Drinker** is required to harvest it at all. This is the first confirmed case of a Tier 2 Gadget requirement gating *access* to a fluid entirely, rather than adding a bonus capability on top of an already-functional Tier 1 base (compare Sprayer's Tier 2, below).

**Mob-target harvesting (confirmed):** Drinker's hold-to-use draw mechanic, previously only used on world fluid blocks, also works on a living mob target — right-click-and-hold on the Ender Dragon to harvest Dragon's Milk directly, no cooldown between harvests. First confirmed non-block fluid source for Drinker.

**Open questions:** How much damage from a drop (how many drops before it breaks)? Whether pulling from fluid-handling blocks stays or gets removed. Whether Tier 2 Drinker changes anything besides hazard access (capacity, mode set). Exact per-tick drain rate while holding on a mob target — deferred to Code.

### S.I.P.P.I.N.Gadget ("Sipper")

**Status:** Carried over from the old (pre-rebuild) version; needs reworking. Core function, shape, and capacity established.

**What it is:** Shaped like a tin can. Placed into a Machine to either transfer or dispose of the fluid currently inside that Machine — functionally the machine-facing counterpart to Drinker's world-facing fluid pull.

**Capacity:** `1000 mB` — same as Drinker.

**Modes:** Two of Drinker's three — **Disposal** and **Storage** — behaving identically to their Drinker counterparts (Disposal erases the fluid, Storage holds it in the internal tank). No Transfer mode.

**Mode switching:** Right-click toggles between the two modes (with only two modes, crouch/stand direction-switching like Drinker's doesn't come into play). Same confirmation-timer safeguard as Drinker: if fluid is stored, changing mode requires repeating the action to confirm, or waiting for it to cancel.

**Difference from Drinker:** A Machine can pull fluid directly from a Sipper while it's holding fluid (i.e. Storage mode's contents are accessible to the Machine it's placed in, not just a one-way dump into the Sipper). Not stated as something Drinker supports.

**Durability:** Doesn't currently have Drinker's drop-damage mechanic — Sipper was built before that mechanic existed. Planned to be added when Sipper gets reworked for the new version.

**Open questions:** How it physically interacts with a Machine (placed in a slot? right-clicked on the Machine block?). Does it share Drinker's Tier 1 hazardous-fluid restriction?

### S.P.R.A.Y.E.Rig ("Sprayer")

**Status:** Concept stage only — not yet built, mechanic ideas only.

**What it is:** A spray gun. Effect produced depends on the fluid currently in its tank — the same "fluid defines behavior" pattern seen with Flask variants, here applied to a Gadget instead.

**Known fluid effects:**
- **Water** — acts like a flamethrower specifically against Endermen; also usable to water crops.
- **Calcium Blend** — area-of-effect bone meal (an AoE version of Flask of Calcium Blend's single-application effect).

**Tiers:** Base version is Tier 1 (per the general Gadget/Machine rule — can't handle hazardous fluids). A **Tier 2** version is planned that can use **Lava** as fuel, functioning as an actual flamethrower — the first confirmed example of a Tier 2 Gadget. Tier 2's defining upgrade over Tier 1 is specifically lava capability (see `dermicraft-project-primer.md` Stage structure).

**Dragon's Milk — further-evolved tier (confirmed):** A **second Tier 2+ evolution**, beyond the existing Lava-flamethrower Tier 2, unlocks Dragon's Milk (see `dermicraft-crafting-notes.md`) as a Sprayer fuel. Effect: a **direct damage/effect spray** — not a lingering AoE cloud like vanilla Dragon's Breath or lingering potions — fired straight at whatever the player targets. Establishes Sprayer's evolution line as **branching at Tier 2** rather than linear (the Lava tier and the Dragon's-Milk tier are parallel further evolutions, not one leading to the other) — first confirmed branching evolution path in the mod, as opposed to the Effluentcer's straight-line 2→3→4 input progression. Recipe logic: consistent with Sprayer's "fluid defines the effect" identity — Dragon's Milk being sourced from the Ender Dragon justifies a dragon's-breath-flavored effect directly, no invented justification needed.

**Open questions:** What other fluids might get a Sprayer effect (other Slurries, Catalysts, or Crafting Blends)? Capacity? Mode-switching mechanic, if any (right-click cycle like Drinker/Sipper, or does the effect just follow whatever fluid is loaded with no separate "mode")? What's the actual process for a player to get/craft the Tier 2 version (separate item, an upgrade process, something forced-evolution-style like Machines)? Exact damage/effect numbers and range for the Dragon's Milk spray. Name for this evolved tier. Whether this evolution also requires Tier 2 hazard-handling the same way Drinker's did.

### Eater

**Status:** Base tier BUILT and working end-to-end (2026-07-31) — item logic, buffer, modes, GeckoLib rig, and animation all in place and tested in-game. One known cosmetic issue left unresolved (see below); everything else confirmed working.

**What it is:** The item-side counterpart to Drinker (which vacuums fluid) — Eater vacuums loose dropped items, and at higher tiers, blocks and ore. Confirmed as an acronym, expansion not yet decided (deliberately left untyped for now).

**Base tier:** Vacuums loose dropped items on the ground.

**Base-tier mechanic (built):**
- **Activation:** hold right-click, same held-trigger identity as Drinker. A real target always wins over crouch/stand gesture handling; crouching with nothing to vacuum acts on the buffer per mode (Transfer pushes it to the player, Disposal voids it behind an arm/confirm dance identical to Drinker's, Storage just reports itself inert), standing with nothing to vacuum cycles mode.
- **Targeting (revised from the original design):** 4-block range, narrowed to a 60°-half-angle forward cone (120° total) off the player's look vector — not a flat 360° sphere as first planned; in-game testing showed the sphere felt untargeted. A separate close-range "capture bubble" (1.5 blocks) bypasses the cone check entirely once an item is already this close, so an item mid-pull can't "escape" the cone as it nears the player and drops below eye level — without this, a pulled item would stall at the player's feet requiring the player to look back down at it.
- **Pull-then-consume, not instant vacuum:** items outside a 0.75-block "consume distance" ease toward the player (40% of remaining distance per tick, easing out rather than constant speed) instead of vanishing the instant they're detected — a deliberate cosmetic tail. Both the pull target and the consume-distance check are anchored to the same point (roughly chest height, not the feet) after an early bug where the two disagreed and items stalled forever just outside the threshold.
- **20-tick windup:** holding the trigger starts the mouth-bloom animation immediately, but actual pulling/consuming is gated behind `player.getTicksUsingItem() >= 20` so the covers have time to visibly open before anything moves. Vacuum candidates still get their vanilla pickup-delay refreshed for the whole windup (not just once pulling starts), otherwise vanilla's own walk-over-item pickup wins the race and grabs items before Eater ever touches them.
- **Modes:** all three of Drinker's — Storage, Transfer, Disposal — full family parity, including Disposal's arm/confirm safeguard.
- **Internal buffer:** 4-slot bulk item handler (see Shared infrastructure above), each slot a normal stack cap. Reasoning: a fluid buffer is "one fluid, variable quantity" so Drinker's single tank works, but a dropped-item pile is usually mixed (e.g. a skeleton's bones/arrows/string/gunpowder) — a single slot would jam on the first item type touched and ignore the rest of the same pile.
- **HP:** 10, matching Drinker.

**Model & rig (built):** GeckoLib model with a wide intake maw, four corner cover plates that iris open on activate (not the originally-imagined straight-line open), three gill-like partial ribs that flutter at idle and pulse in sync while active, and a top-mounted 2×2 grid of "screen" bones (one per buffer slot) that slide flush and show a live floating `ItemStack` render (via a custom `GeoRenderLayer`, not a baked icon) when their slot is occupied, staying dark/recessed when empty. Mode lights mirror Drinker's mutually-exclusive lit-texture swap.
- **Animation controllers:** one `Body` controller drives idle ↔ activate→active_hold (a single authored direction, relying on `transitionLength` blending for the return rather than a mirrored deactivate clip) plus the rib/mouth flutter; one controller per screen bone drives its own slide, gated on that slot's occupancy.
- **Fixed bug:** the four screen controllers' idle fallback originally reused the `idle` animation, which also contains the mouth-cover flutter keyframes — since GeckoLib doesn't blend bone writes across separate controllers, every empty screen's controller was silently re-applying `idle`'s mouth pose after `Body`'s own pass each frame, permanently masking the activate animation. Fixed by giving the screen controllers a genuinely empty `none` fallback clip instead.
- **Known cosmetic issue, not pursued further:** the `active_hold` loop visibly flickers at the exact moment an item lands in the buffer. Investigated and ruled out the obvious cause (GeckoLib's `AnimationController` compares `RawAnimation` by content equality, not reference, per its own source/javadoc, so rebuilding an equivalent `RawAnimation` each tick — which both Eater's and Drinker's controllers already do — isn't it). No confirmed root cause without live debugging tools; decided not worth fighting the engine over for a cosmetic-only, already-rare flicker.

**Open questions:** Exact upgrade system/currency (likely the shared Gadget customization system used elsewhere). Cone dimensions at the later block/ore tiers (unaffected by the base-tier targeting above). Root cause of the `active_hold` flicker, if it ever becomes worth revisiting. Recipes/crafting chain — not yet touched.

**Mid tier — loose block vacuuming:** Can suck up "loose" blocks (dirt, sand, gravel) directly. This tier's primary upgrade axis is **speed** — how quickly a block is sucked up. Cone-size upgrades for this tier are their own separate, later addition (see below).

**Later tier — ore vacuuming:** Can pull material directly out of an ore block. When drained, the block **converts to its base stone-equivalent** (e.g. Iron Ore → Stone) rather than being removed entirely — Eater hollows the ore out, it doesn't mine it away.
- **First version:** only pulls ore that is directly exposed/visible.
- **Fully upgraded version:** can pull an entire connected vein, visible or not (implies vein/flood-fill detection).

**Area of effect — cone, directional:** Eater's block/ore-affecting tiers work in a cone in front of the player, which **widens and grows taller with distance** from the player (narrow near the player, larger area farther out). This cone is upgradeable, independent of the loose-block tier's speed upgrades — meaning loose-block vacuuming and ore vacuuming each scale on their own separate axis (speed vs. reach/depth), even though they share the same underlying cone shape.

**Upgrade axes, summarized:**
- **Loose blocks:** speed only; cone growth is its own separate upgrade track.
- **Ore:** cone size/reach, and vein-detection depth (visible-only → full vein) — two independent tracks.

**Open questions:** Exact upgrade system/currency (likely the shared Gadget customization system used elsewhere). Cone dimensions at each stage.

### Lobber (Fluid Grenade Launcher)

**Status:** Concept stage — core mechanics defined; name confirmed as an intentional acronym (expansion not yet written out).

**What it is:** A grenade launcher whose ammo is filled with various fluids that shatter and release an AoE effect on impact. Fluid effects **mirror Sprayer's fluid-effect list exactly** (e.g. Water, Calcium Blend) — same fluid identity, different delivery method (sustained spray vs. burst impact).

**Ammo:** A new, dedicated shatter-on-impact container — explicitly not the Flask. Name/material not yet decided.

**Aiming:** No separate mode toggle — purely aim-based. Aiming high lobs the grenade in an arc (for AoE placement at range); aiming flat/level fires it directly at short range.

**Reload:** Manual — right-click to load a vial. Single-shot to start; a magazine capacity is a planned later upgrade, mechanism deferred to the broader Gadget upgrade system (its own future design pass).

**Tier:** Not yet decided whether Lobber gets a Tier 2 lava-based upgrade mirroring Sprayer's.

**Dragon's Milk — lingering AoE cloud (confirmed):** Where Sprayer's evolved Dragon's Milk tier delivers a direct damage/effect spray, Lobber's version of the same fluid produces a **lingering AoE cloud** on impact — matching vanilla Dragon's Breath/lingering-potion behavior. Consistent with Lobber's core rule that its fluid effects mirror Sprayer's list exactly in *identity* but differ in *delivery* (sustained spray vs. burst impact) — this is the first case where that difference in delivery also changes the effect's *shape* (instant spray vs. lingering cloud), not just how it's fired. **Tier implication:** since Dragon's Milk is hazardous, this presumably requires an evolved/Tier 2+ Lobber, same pattern as Sprayer's own Dragon's Milk tier — this recipe may be what actually forces that Tier 2 Lobber into existence, given Lobber doesn't have a confirmed Tier 2 otherwise yet.

**Open questions:** What the full acronym expansion is. What AoE effects map to which fluids beyond Sprayer's existing list. Magazine-upgrade mechanism. Tier restriction (a Tier 2 version using lava for an AoE fire effect would fit the established Tier 2 = lava-capability pattern, similar to Sprayer). Cloud radius, duration, and damage/effect tick rate for the Dragon's Milk cloud. Whether Dragon's Milk is the same evolution step that brings Lobber's long-flagged Tier 2/lava question along with it, or a separate one.

### G.R.A.P.P.L.E ("Grapple")

**Status:** Concept stage — core mechanics defined. Name confirmed as the acronym **G.R.A.P.P.L.E** (shorthand "Grapple"); full expansion still pending. A suit **Grapple Add-on** reuses this same logic — see `dermicraft-suit-notes.md` (Universal Add-Ons).

**What it is:** A traversal Gadget modeled on Deep Rock Galactic's grapple — not a pendulum swing, but a **direct pull toward the anchor point**: fire at a valid target, get pulled straight to it at speed. What happens at arrival and how the tether can be exited is configurable — see **Tether States & Gadget Bench Options** and **Momentum Retention** under Tier Upgrades below.

**Projectile drop:** Deliberately little-to-no drop by design — this is a core identity trait of the Grapple's projectile, not a stat. This is also why fuel grade does **not** affect drop (unlike the Capture Net Gun, where drop is a real aiming factor at range).

**Fuel:** Slurry-powered, **Main Line only — no Serums** (Serums' defining unconditional-burn behavior fits a machine's continuous fuel tank, not a reload-cost item that sits loaded between shots). Cost is charged **at reload**; if the cost can't be paid, reload simply doesn't happen (no partial charge, no failure state beyond "can't reload yet"). Better fuel grade improves Range, Projectile Speed, and Pull Speed, using the Main Line's existing effective-speed multiplier (see `dermicraft-slurry-notes.md`) — no separate fuel curve invented for the Grapple.

**Base Tier-0 (Crude) values:**
- **Range:** 16 blocks (deliberately enough to clear a chunk in one pull).
- **Reload cost:** 100 mB per shot (matches the mod's existing single-use discrete-action cost, e.g. tumor implant recipes), before Chambered Fuel Cell's bulk-cell pricing applies.
- **Projectile speed:** anchored faster than a thrown Trident — concept locked, exact tick value deferred to Code.
- **Pull speed:** 2× player sprint speed.

**Fuel grade scaling** (Range shown as reference; Projectile Speed and Pull Speed scale by the same multiplier):

| Grade | Multiplier | Range (Tier 0) |
|---|---|---|
| Crude | ×1.00 | 16 |
| Concentrated | ×1.25 | 20 |
| Refined | ×1.75 | 28 |
| Enriched | ×2.50 | 40 |
| Superior | ×3.50 | 56 |

Fuel-scaled results round **up** to the nearest whole block wherever the multiplier doesn't land cleanly (not triggered at this particular base value, but the rule stands for future retuning).

**Fuel type lock:** the loaded fuel type/grade can only be changed while the Grapple is **empty** — one of several planned "plan-ahead" mechanics meant to reward preparation rather than penalize on-the-fly flexibility (more of this family planned elsewhere in the mod).

**Wall/impact handling:** No dedicated collision-damage code needed — vanilla Minecraft doesn't apply damage for horizontal collision with a wall regardless of speed, so pulling into a wall is naturally harmless. The only real risk is a **vertical component** to a pull (grappling up onto a ledge or down into a pit) — a landing after that kind of pull is subject to normal fall-damage rules, same as jumping off something, and interacts with whatever fall-damage mitigation the player's suit/add-ons already provide rather than needing a bespoke exemption.

**Mob interaction:** The Grapple can target mobs as anchor points, not just blocks. Resolution is based on **mass**, not combat power or the Net Gun's threat-based tier system (a deliberately different axis from the Net Gun) — deferred to Code for exact values/thresholds:
- **Mob mass < player mass:** the mob is pulled to the player.
- **Mob mass > player mass:** the player is pulled to the mob.
- **Override — Grapple Anchor add-on:** with the suit's **Grapple Anchor** add-on equipped (see `dermicraft-suit-notes.md`), **crouching before firing** at a mob **braces and holds it in place** — the mob is immobilized while the brace is active, rather than either party being pulled. Costs **extra fuel while active**. (Pulling a *heavier* mob to the player, rather than just holding it, is flagged as a possible future companion add-on — not part of Grapple Anchor itself.)

**PvP grappling — via Grapple Anchor only.** The base Grapple is mob-only. The **Grapple Anchor** add-on additionally enables grappling **other players** and pulling them in **harmlessly** (no crouch required for player targets). This is the confirmed mechanism for PvP grappling; without the add-on it remains disallowed.

**Primary combat use case:** Closing distance on targets that are otherwise hard to reach, especially **flying bosses** (the Ender Dragon being the clearest example) — since a boss's mass will always exceed the player's, grappling a boss pulls the player to it, letting a mobile, melee-focused loadout force an engagement mid-flight rather than waiting for the target to come down.

**Design goal note:** Per the mod's stated endgame-equipment philosophy — the player should feel **godlike by the end, but it has to be earned** — tier upgrades below are budgeted as **capability leaps** (new verbs, new thresholds), while fuel grade owns all **smooth numeric scaling**. Pull Strength is the one exception, a straightforward per-tier magnitude stat (precedent: Assault's Toughness/knockback resistance).

**Tier Ladder — 6 tiers total,** mirroring the vanilla 6-rung armor-material scale (Leather→Chainmail→Iron→Gold→Diamond→Netherite) the suit tier system is already modeled on:

| Tier | Unlock |
|---|---|
| 1 | Chambered Fuel Cell — introduced |
| 2 | Chambered Fuel Cell — improved |
| 3 | Chambered Fuel Cell — improved again |
| 4 | Chain Firing (requires Chambered Fuel Cell) |
| 5 | Harder Pull |
| 6 | Momentum Retention |

**1–3. Chambered Fuel Cell:** reload loads a whole multi-chamber **cell** instead of a single charge. Loading the cell costs fuel once, proportionally cheaper than paying full price per shot the old way — savings come from buying in bulk, not a free shot. Each fire burns one chamber; only once the cell is fully spent does the player need to reload. Chamber count doubles each tier; per-chamber cost drops each tier:

| Tier | Chambers | Cost/chamber | Cell total | Cost/shot |
|---|---|---|---|---|
| 0 (no cell) | 1 | — | 100 mB | 100 |
| 1 | 2 | 75 mB | 150 mB | 75 |
| 2 | 4 | 50 mB | 200 mB | 50 |
| 3 | 8 | 25 mB | 200 mB | 25 |

(Tier 2→3 doubling chamber count at the same total cell price is a happy accident of these particular numbers, not a deliberate rule — kept as-is since it lands as a satisfying jump right before Chain Firing unlocks.)

**4. Chain Firing** (requires Chambered Fuel Cell): removes the manual "advance to next chamber" step entirely — fire, release, fire again almost instantly, gated only by remaining fuel. Reload cost is identical whether triggered manually or automatically. Since chambers are pre-paid at load time, if the player's live fuel source runs dry mid-use, any chambers still loaded keep firing regardless — an emergency reserve that only exists because Chambered Fuel Cell front-loads the cost.

**5. Harder Pull:** per-tier stat increase to pull force in both directions of the existing mass rule (mob-to-player and player-to-mob) — the mass-vs-player threshold logic itself is unchanged, only the strength of the pull scales.

**6. Momentum Retention:** grants a **manual Release** action (its own dedicated keybind, distinct from Fire and Lock) usable **while actively mid-pull only** — cuts the pull short on command and launches the player off with retained velocity, instead of riding it out to natural arrival. Retention is what makes this worth using: without it, cutting a pull short would just dump speed like the normal stop does. Placed last since it's the tier most likely to catch a player off guard the first time it fires.

**Per-Tier Stat Growth:** applies the suit tier system's existing layering rule (base → tier bump → new base → fuel multiplier applies on top of *that*) to Range, Projectile Speed, Pull Speed, and Pull Strength. Deliberately modest and accelerating — the tier's headline reward is always the capability above, this is a bonus layered on top, not the main draw.

Range / Projectile Speed / Pull Speed (cumulative % bonus to the Tier-0 base):

| Tier | 1 | 2 | 3 | 4 | 5 | 6 |
|---|---|---|---|---|---|---|
| Bonus | +5% | +11% | +18% | +27% | +38% | +51% |

Pull Strength (bigger curve — no fuel multiplier of its own to lean on):

| Tier | 1 | 2 | 3 | 4 | 5 | 6 |
|---|---|---|---|---|---|---|
| Bonus | +10% | +22% | +36% | +54% | +76% | +102% |

All stat-growth values provisional and easy to retune without breaking the underlying accelerating-curve pattern (same framing used for Slurry's own progression numbers).

**Tether States & Gadget Bench Options:** independent of the tier ladder above — available from **Tier 0**.
- **Hanging:** once a pull ends, the tether either auto-detaches or leaves the player suspended at the anchor point ("Hanging"), governed by a **Gadget Bench** toggle:
  - **Auto Release on Stop** (Yes/No, default **Yes**): Yes = tether detaches the instant the pull ends (today's baseline behavior, no Hanging state). No = the player remains tethered and Hanging at the anchor after arrival, enabling Controlled Drop below.
- **Controlled Drop:** while Hanging, holding crouch slowly lowers the player down the tether. Available at **Tier 0** — not treated as a meaningful power upgrade, just an early utility. **Fuel-free**; fuel only ever pays for firing/reload, never for holding a state.
- **Controlled Drop distance:** capped at the Grapple's **current effective Range** (same tier+fuel-scaled number that governs firing distance) — one continuous tether, same maximum length in either direction, no separate stat needed. Descent speed itself still needs a value (own flat rate, or a fraction of Pull Speed — open question).
- **Running out of tether before reaching the ground:** the player simply stays Hanging at full extension — same as any other Hanging state, no forced auto-release.
  - **Tier 0 (no cell):** no spare charge — Release commits to the remaining fall under normal fall-damage rules.
  - **Tier 1+ (Chambered Fuel Cell):** the original pull only spent one chamber, so a spare is already loaded — Release then Fire again immediately to grab a new, lower anchor, turning one long fall into a series of controlled drops. An early, manual preview of what Chain Firing (Tier 4) later automates fully.
- **Auto Release on Ground Touch** (Yes/No, only relevant when Auto Release on Stop = No): Yes = touching the ground during a Controlled Drop auto-detaches the tether; No = the player must manually press Release even once grounded.
- **Release, dual context:** the Release action means different things depending on tether state — mid-pull (requires Momentum Retention, retains velocity) vs. Hanging/stationary (available from Tier 0, no velocity to retain, just detaches).
- **Gadget Bench:** working name for wherever this kind of gadget customization/configuration happens — may end up being its own block/structure, or a function folded into the existing Suit Dock. Not yet decided.

**Add-on Frame:** the suit's **Grapple Add-on** (see `dermicraft-suit-notes.md`) is not a separate upgradeable item — it's this same physical Grapple, docked into a mounting frame occupying a suit add-on slot. Single source of truth: tiers, loaded cell, and fuel grade all live on the one item whether it's held or docked, so there is no duplicate upgrade path to maintain. Nesting for full removal is Player → Suit → Add-on (frame) → Grapple. The existing Suit Dock simplifies the outer two layers (suit removal, and swapping the add-on/frame without a full undress); extracting/inserting the Grapple itself into its frame is a more deliberate, specialized-station operation (possibly also the Dock — undecided), and depends on the not-yet-designed **Gadget Upgrade System**.

**Open questions:** Exact mass thresholds/values. (PvP grappling resolved — enabled via the Grapple Anchor add-on; see Mob interaction above.) Exact Controlled Drop descent speed (own flat rate vs. a fraction of Pull Speed). Whether the frame stays in the suit slot (as an empty, non-functional socket) when the Grapple is pulled out, or the slot frees entirely. Exact Grapple↔frame install/remove interaction and station, pending the Gadget Upgrade System (working name: Gadget Bench). Recipes and materials — deliberately deferred until the rest of the Grapple's structure is settled.

### Capture Net Gun

**Status:** Concept stage — core mechanics and tier roster defined.

**What it is:** A non-lethal capture Gadget — fires a net that captures a mob into a spawn-egg-style item rather than killing it. Continues the mod's existing thread of non-lethal mob interaction (alongside Syringe's Squid/Creeper effects and Nose on a Stick's harvest-ready mob logic).

**Fuel:** Slurry-powered, same reload-cost rule as Grappling Hook (charged at reload, fails to reload if unaffordable). Better fuel grade improves range, projectile speed, and reduces projectile drop (same scaling as Grapple).

**Capture mechanic:** Guaranteed on hit, provided the net's tier is equal to or above the target's required tier — no chance roll, no HP/condition dependency. Skill-based (land the shot) rather than RNG-based.

**Net tiers** (material progression: additive, mod-standard escalation — e.g. String → +Copper → further materials upward). Each tier can also capture everything at or below it:
1. **Passive mobs**
2. **Early hostiles** — Creepers, Skeletons, Spiders, Zombies, most Illagers
3. **Basic Nether mobs**
4. **Powerful non-boss mobs** — Blaze, Ghast, Enderman, and any remaining overworld mobs not covered above
5. **Bosses** — Ender Dragon, Wither, Warden

**Boss capture timing:** Guaranteed on hit, but only once the boss is genuinely battle-ready — respects each boss's existing vanilla vulnerable/not-yet-active window rather than allowing an early cheese capture:
- **Ender Dragon:** capturable from the start of the fight (no comparable vulnerable-spawn window exists).
- **Wither:** must wait until after its charge-up/summon invulnerability phase ends.
- **Warden:** must wait until it has finished emerging from the ground.

**On capture:** The mob becomes a spawn-egg-style item, **automatically added to the player's inventory** (drops nearby as overflow only if the inventory is full, matching standard item-give behavior).

**Release:** Right-click the ground with the captured-mob item to release it, following the same interaction pattern as vanilla spawn eggs.

**Primary intended uses:** Relocating a mob (including a boss) rather than killing it, or — see future idea below — feeding it into a controllable mob farm.

**Future idea (not yet designed):** A Dermicraft-native mob spawner block that a captured mob item can be fed into, turning it into a live, controllable spawn source. Control scheme would follow the standard redstone-toggle behavior already established by vanilla spawners and other mods' spawner blocks, rather than inventing new redstone logic. This would extend the Net Gun from a one-off capture tool into the front end of a real mob-farming/automation system — including, notably, boss-tier mob farms.

**Open questions:** Exact net-tier material progression beyond String/Copper. Exact fuel-cost values per reload.

## Weapons

Housed under Gadgets as their own subsection — melee/combat-focused Gadgets sharing the Slurry-fuel-heals-durability pattern.

### Chainsword

**Status:** Concept stage — most core mechanics defined.

**What it is:** A melee weapon in the Weapons subsection of Gadgets, inspired by Warhammer 40k's chainsword but built organic to fit Dermicraft's aesthetic. OT-crafted; recipe explicitly flagged as easy to iterate/change.

**Utility:** One-hits 1-block-wide trees.

**Combat:**
- High chance to inflict Bleed on hit, where the target is bleed-capable (exact implementation deferred to Code).
- Guaranteed decapitation on a killing blow, where the target has a head — **mechanical**, not cosmetic: the head drops as an item alongside the mob's standard drops.

**Fuel (Slurry-powered):**
- Fuel grade affects performance (mirrors the Main Line's speed scaling) — better fuel, better swing/cutting performance.
- Fuel also **heals** the Chainsword — while fueled, it does not take conventional durability damage.
- **Unfueled behavior** (base): drains the player's hunger **per swing**, hit or miss.
  - **Safety upgrade:** changes the drain to **per hit only** (whiffing costs nothing).
  - **Alternate upgrade ("mundane sword" mode):** removes hunger drain entirely; instead the Chainsword functions like a normal sword with reduced stats, and **does** take durability damage in this state.
  - These two upgrades are **mutually exclusive** — a player picks one path, not both.

**Open questions:** Exact Bleed chance/implementation. Exact fuel-grade performance curve.

### Drill Hammer

**Status:** Concept stage — most core mechanics defined.

**What it is:** A slow, heavy-swinging Weapons-subsection Gadget built for both mining and combat, filed as a Weapon. Same Slurry fuel relationship as Chainsword (grade affects performance, heals with fuel, mirrored unfueled fallback options).

**Area of effect:** 3×3 face, with the center column striking **2 blocks deep** and the surrounding 8 blocks striking **1 block deep**. This AoE was originally designed for mining and applies identically in combat.

**Targeting logic** (governs whether the AoE hits blocks or mobs):
- **Hit a mob:** AoE only affects mobs — no incidental block damage while fighting.
- **Miss:** the swing connects with the first block in its line, and the AoE applies to blocks on that impact.
- **Exception:** if the player is flying and farther than the hammer's reach from any block, the swing does nothing.
- **PvP:** other players only need to worry about direct hits — no AoE risk to them.
- **Risk:** no dedicated damage mechanic against the player — the danger is purely environmental (e.g. removing the ground under your own feet, breaking something unintended).

**Combat effects:**
- Heavy bonus damage against skeletons and wither skeletons, possibly extra drops, and possibly an additional status effect (not yet decided).
- Heavy bonus durability damage against armor, **except chainmail**, which resists it.

**Heads (material-based, swappable):**
- Heads swap via the same system used for other Gadget customization/upgrades.
- Damage scaling follows a vanilla-inspired material curve (numbers not yet set).
- Vanilla materials (Iron, Diamond, Netherite, etc.) are the confirmed starting point; non-vanilla/exotic head materials not yet decided.

**Open questions:** Exact bonus-damage/status-effect values vs. skeletons. Non-vanilla head materials.

### The Tablet

**Status:** Concept stage, core identity and construction path decided. Capabilities largely undecided — deliberately left open. See `dermicraft-misc-notes.md` for the full narrative/guide-system context this gadget is part of.

**What it is:** An intelligent, **living** guide device — the second half of the mod's two-part in-game guide system (the first being the Notebook, a non-living book-style item; see `dermicraft-misc-notes.md`). Unlike every other Gadget so far, the Tablet is self-authoring: it actively records and writes its own notes as the player works, rather than presenting fixed, pre-written content the way the Notebook does.

**Naming convention note:** "The Tablet" doesn't yet follow the established acronym + suffix pattern (`[ACRONYM].Gadget`/`[ACRONYM].Rig`) used by Drinker/Sipper/Sprayer — open question whether it gets folded into that convention later or stays a deliberate naming exception (a case could be made either way: consistency vs. the Tablet being narratively/mechanically unlike the others).

**Construction — confirmed as OT-crafted, not hand-crafted via Syringe.** Built by the **Operating Theater** from the **Notebook plus other ingredients** — the first confirmed example of an OT crafting recipe, and the first confirmed OT-built item made from **living materials**. The Notebook is consumed in the process, but its information **carries over** into the Tablet rather than being lost — the player's early guide becomes incorporated into the new one, not discarded.

**Living, but constructed differently than other living things:** Every other living thing in the mod so far (Inert Tumor chain, Machines) is built through the hand-crafted tumor-genesis process via Syringe. The Tablet is OT-assembled instead — raising an open question (flagged in `dermicraft-machine-notes.md` Operating Theater entry) about whether the OT runs its own distinct construction logic for living things, or secretly automates the same underlying tumor-chain mechanic.

**Physical connection to the player (lore-confirmed, mechanically undetermined):** The Tablet is described as physically connected to the player — likely not something shown through dedicated graphics, but through other in-game means not yet decided (e.g. possible interaction with the player's own health/state, though this is speculative and unconfirmed). A genuinely new relationship type, distinct from every other item in the mod so far.

**Decluttering via lock/unlock:** The player can manually lock and unlock sections of the Tablet's content, purely for personal organization — no mechanical cost or restriction tied to it, just letting the player control how much information is visible at once.

**First concretely-scoped guide content: fluid hazard info (confirmed).** Which specific hazard tag(s) a fluid carries is shown in tooltips whenever the Notebook or Tablet is anywhere in the player's inventory (possession, not held) — vanilla lava excepted, which needs no gate. The Tablet inherits the Notebook's Stage-2-capped knowledge wholesale on construction, then grows past it only through in-world discovery (many-to-one — one find can unlock several entries). Full detail, including the narrative reasoning, lives in `dermicraft-misc-notes.md` → In-game Guide System.

**Operating cost — tied to capabilities, not yet set.** If the Tablet's role stays purely informational (guide/reference only), it carries **no operating cost** — the core promise of an always-available in-game reference stays protected. Any cost only enters the picture if/when the Tablet gains active (non-informational) capabilities beyond guide functions; those costs would be balanced against whatever those capabilities turn out to be.

**Open questions:** Full capability list beyond the guide function. Exact mechanic for the physical player-connection. Does it fit the acronym naming convention or stay an exception? Operating cost, once capabilities are set. Whether OT-built living items are a one-off (just the Tablet) or a pattern for future OT-crafted Gadgets.

## Template for new entries

```
### [ACRONYM].[Gadget/Rig]

**Status:**

**What it is:**

**Capacity:**

**Modes:** (if applicable)

**Open questions:**
```
