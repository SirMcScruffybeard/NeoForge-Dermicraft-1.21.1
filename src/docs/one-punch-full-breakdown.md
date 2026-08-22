# One Punch — Full Mechanics Breakdown

Assault's Tier 1+ suit-integrated signature weapon. This doc supersedes/expands the One Punch entry in `dermicraft-suit-notes.md` (Addendum 2) — fold this level of detail in when updating that file.

**Name origin:** "One Punch" references both the mechanic itself (all banked power discharges in a single hit) and the anime it's named after — chosen specifically for the shared idea of overwhelming, final force delivered in one decisive blow.

**H.E.A.T. = High Energy Accumulation and Transfer** (full name confirmed — see `dermicraft-suit-notes.md` → Suit Designations & Naming). **The bank is now called H.E.A.T. and is suit-wide, not One-Punch-exclusive.** Everything below under "Charge" describes how **H.E.A.T. is gained, stored, and lost** — that part is a shared, suit-wide system. What's specific to *this* document is **One Punch's own spending pattern**: it's one of (now) two confirmed H.E.A.T. consumers, alongside the new **Juggernaut** add-on (see `dermicraft-suit-notes.md`, Assault's exclusive add-ons). **Spending patterns are per-consumer** — One Punch spends the entire bank at once, all-or-nothing (see Release, below); Juggernaut instead draws H.E.A.T. continuously and partially while active. Nothing about One Punch's own mechanics changes from this generalization — only the underlying resource's name and scope.

---

## Control Scheme

- **Trigger input:** the player's normal attack input (left-click), while the **main hand is empty**.
- This is the **same input** used for an ordinary unarmed punch — there is no separate button or action to deliberately "activate" One Punch. What the punch *does* depends on the suit's current state:
  - **No H.E.A.T. banked:** the left-click punch behaves as a normal unarmed hit, using Assault's current base unarmed damage (which grows via tier upgrades and point investment).
  - **H.E.A.T. banked:** the left-click punch instead becomes a **One Punch release** — the full stored H.E.A.T. discharges on that single hit.
- There is no partial spend and no manual charge/hold input. The player doesn't choose *how much* to release or *whether* to release once H.E.A.T. exists and they throw a punch — landing any empty-hand punch while charged consumes the full bank on that hit.
- Practical implication: a player who wants to preserve H.E.A.T. for a specific moment (e.g. saving it for a boss, or for breaking through a specific wall) must avoid throwing any empty-hand punches until they intend to spend it — since the very next unarmed hit will automatically be the release.

---

## H.E.A.T. — How It's Gained

- **Source:** Every **direct-contact/outside-in** hit the player takes while wearing the Assault suit contributes to H.E.A.T. — combat hits, fall damage, and Thermal Hazard exposure all feed the same bank as one general damage-soak system, no special-casing needed between those three. **Deterioration/inside-out hazard damage (Radiation, Biohazard) is the deliberate exception — it never generates H.E.A.T. even though it deals real HP damage** — see `dermicraft-hazard-effects-notes.md` → H.E.A.T. Generation Rule for the full outside-in/inside-out distinction and why.
- **Amount banked per hit:** the **unarmored-equivalent damage** of that hit — calculated *before* the suit's own defense and Toughness mitigation are applied. This means H.E.A.T. reflects genuine danger survived, not just raw hit count:
  - A powerful hit that would have been lethal (or close to it) to an unarmored player banks a large amount of H.E.A.T.
  - A weak hit that would have barely scratched an unarmored player banks very little.
- H.E.A.T. accumulates continuously across multiple hits — each qualifying hit adds to whatever is already banked, up to the capacity cap (see Storage & Loss, below).
- **Deliberate hazard-fueling is a valid strategy (confirmed) — at least for Thermal Hazard.** Because generation is based on raw/unarmored damage rather than something the player can only take by accident, standing in a hazard on purpose (the literal example: pouring a bucket of lava at your own feet) is an intended way to farm H.E.A.T., not an exploit to patch out.
- **Which hazards feed H.E.A.T. at all is decided by one general rule, not a per-hazard-tag table (confirmed) — see `dermicraft-hazard-effects-notes.md` → H.E.A.T. Generation Rule for full detail.** Outside-in/direct-contact damage generates H.E.A.T. (Thermal Hazard: continuously, per tick, up to the capacity cap); inside-out/deterioration damage never does, even when it deals real HP damage (Radiation Severe's Wither-based hit and Biohazard both fall here). Biohazard additionally becomes **fully resisted** at high Assault tiers (via Hazard Resistance/Defense reaching zero-effect) rather than merely not-converting — the one hazard confirmed to reach full block rather than just reduced effect.
- **Hazard Defense (confirmed suit-wide stat, scaled off Toughness for Assault — see `dermicraft-suit-notes.md` and `dermicraft-hazard-effects-notes.md`) — reduces damage from any damaging hazard, and is the lever for H.E.A.T.-specific diminishing returns on Thermal Hazard.** Supersedes the earlier "Heat Resistance" name/scope. Hazard Defense discounts the *raw* baseline for **any damaging hazard** before general damage reduction applies — deliberately **separate from Assault's general Toughness/defense growth** (Hazard Defense is itself *scaled off* Toughness, but is its own distinct number). Its effect on H.E.A.T. specifically only matters for hazards that generate H.E.A.T. in the first place (Thermal Hazard, per the general rule above) — for deterioration hazards (Radiation, Biohazard), Hazard Defense still reduces the damage taken, it just has nothing to discount pre-H.E.A.T. since those never bank H.E.A.T. regardless. Low Hazard Defense means lava-fueling stays efficient; as it grows, higher tiers intentionally make Thermal Hazard exposure both safer *and* less efficient to farm — a deliberate tradeoff, not an accidental side effect of general defense scaling.

---

## H.E.A.T. — Storage & Loss

- **Persistence:** H.E.A.T. does not decay over time. Once banked, it stays available indefinitely until either spent or lost.
- **Loss conditions (H.E.A.T. is fully wiped):**
  - **Suit removal** (assisted or unassisted).
  - **Player death** — this applies even from Tier 1 onward, where the Assault suit itself now survives death and stays equipped through respawn. The suit stays on; the banked H.E.A.T. does not carry over.
- There is no partial loss condition (e.g. no gradual decay, no percentage lost on minor events) for **passive loss** — H.E.A.T. is either fully intact or fully wiped by the conditions above. (Active *spending*, e.g. Juggernaut's continuous partial draw, is a separate, per-consumer mechanic — see that add-on's entry.)

**Capacity cap = damage-soak cap (new, confirmed concept — resolves the "implies a H.E.A.T. cap exists" open question flagged in `dermicraft-suit-notes.md`'s H.E.A.T. Sink entry).** H.E.A.T. storage has a maximum capacity, and that same number *is* how much incoming damage the suit can soak before it starts really hurting — one value doing double duty, not two separate systems:
- **While there's room in the bank:** incoming damage (of any kind — see "How It's Gained" above) is soaked into H.E.A.T. instead of hurting the player. This is what "damage soak" means literally for Assault — banking *is* the mitigation.
- **Once the bank is full:** further damage stops being absorbed and "sets in" for real — the player actually takes it, the same as any other suit would.
- **Practical effect:** this makes *draining* H.E.A.T. (via One Punch, Juggernaut, etc.) a real gameplay loop, not just a resource to hoard — an empty-ish bank has room to soak the next hit; a full bank offers no further protection until spent.
- **H.E.A.T. Sink Add-on directly raises this cap** (see `dermicraft-suit-notes.md`, Exo — Cross-Suit Compatibility Add-ons) — confirmed to be the mechanism behind its "adds more H.E.A.T. capacity" effect on Assault.
- **A separate conversion ceiling is also likely needed (flagged, not yet designed).** Realized during this same discussion: an uncapped *rate* of H.E.A.T. gain could let a large single burst (or rapid repeated small hits) fill the bank near-instantly regardless of overall capacity — a conversion ceiling (a per-tick or per-hit cap on how much of a single damage instance can convert) is likely necessary alongside the storage cap, but the exact shape isn't decided. Deferred to the tier/equipment discussion, same status as the rest of H.E.A.T.'s numeric values.

---

## Release — What Happens

- **Timing:** The full H.E.A.T. bank discharges in a single hit, on impact — the moment the punch connects.
- **Damage model:** The release applies as a **multiplier** on Assault's current base unarmed damage, not as a flat separate number. This means:
  - One Punch's overall power ceiling rises automatically as Assault's baseline unarmed damage grows through tier upgrades and point spending — the mechanic scales with the suit's whole progression rather than being self-contained.
  - The size of the multiplier itself is driven by how much H.E.A.T. was banked at the moment of release (per the unarmored-equivalent accumulation above).
- **Tier 1 tuning:** Available starting at Tier 1, but deliberately weak there — enough to hint at its potential without being a maxed-out power spike from the moment Assault's specialization begins.

---

## Release — Hitting a Mob vs. a Block

**Against a mob:** the full multiplied damage applies directly to the target, as a normal (if enormous) melee hit.

**Against a block:** rather than a flat "destroys everything in range regardless of charge" effect, One Punch applies an **AoE effect scaled to the H.E.A.T. spent** — mirroring Drill Hammer's mining-logic approach:
- A small/weak bank produces a correspondingly small AoE.
- A large bank produces a correspondingly larger AoE.
- This keeps a lightly-charged release from being disproportionately powerful against terrain compared to what the same amount would do against a mob.

**Block-break staggering (performance + visual):**
- Rather than breaking every affected block in the same tick, the actual break events are spread across several ticks.
- Order radiates **outward from the impact point** — blocks closest to where the punch landed break first, with the effect spreading outward from there.
- **Purpose:** this is a deliberate dual-benefit design choice:
  - **Performance:** avoids a single-tick spike of chunk updates, block updates, and drop-entity spawns all firing simultaneously — a known lag-spike risk for large-area instant block breaks.
  - **Visual:** produces a rippling, shockwave-style destruction effect emanating from the point of impact, rather than an instant flat clear — reinforces the "overwhelming single release" fantasy the weapon is built around.
- Exact tick-spacing and any hard cap on total blocks affected (as a safety valve for extreme H.E.A.T. values) are deferred to Code.

---

## Summary Flow (for quick reference)

1. Player takes hits while wearing Assault → each hit's unarmored-equivalent damage banks as H.E.A.T.
2. H.E.A.T. sits indefinitely, no decay, until either spent or lost (suit removal / player death).
3. Player left-clicks with an empty main hand:
   - No H.E.A.T. → normal unarmed punch.
   - H.E.A.T. present → full bank releases as a single hit, multiplying Assault's current unarmed damage.
4. On a mob: multiplied damage applied directly.
5. On a block: H.E.A.T.-scaled AoE, block-breaks staggered outward from impact for both performance and shockwave visual effect.
6. Full H.E.A.T. always consumed on release — no partial spend (Juggernaut, a separate consumer, spends differently — see `dermicraft-suit-notes.md`).
