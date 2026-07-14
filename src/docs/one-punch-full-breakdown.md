# One Punch — Full Mechanics Breakdown

Assault's Tier 1+ suit-integrated signature weapon. This doc supersedes/expands the One Punch entry in `dermicraft-suit-notes.md` (Addendum 2) — fold this level of detail in when updating that file.

**Name origin:** "One Punch" references both the mechanic itself (all banked power discharges in a single hit) and the anime it's named after — chosen specifically for the shared idea of overwhelming, final force delivered in one decisive blow.

**The bank is now called H.E.A.T. and is suit-wide, not One-Punch-exclusive.** Everything below under "Charge" describes how **H.E.A.T. is gained, stored, and lost** — that part is a shared, suit-wide system. What's specific to *this* document is **One Punch's own spending pattern**: it's one of (now) two confirmed H.E.A.T. consumers, alongside the new **Juggernaut** add-on (see `dermicraft-suit-notes.md`, Assault's exclusive add-ons). **Spending patterns are per-consumer** — One Punch spends the entire bank at once, all-or-nothing (see Release, below); Juggernaut instead draws H.E.A.T. continuously and partially while active. Nothing about One Punch's own mechanics changes from this generalization — only the underlying resource's name and scope.

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

- **Source:** Every hit the player takes while wearing the Assault suit contributes to H.E.A.T.
- **Amount banked per hit:** the **unarmored-equivalent damage** of that hit — calculated *before* the suit's own defense and Toughness mitigation are applied. This means H.E.A.T. reflects genuine danger survived, not just raw hit count:
  - A powerful hit that would have been lethal (or close to it) to an unarmored player banks a large amount of H.E.A.T.
  - A weak hit that would have barely scratched an unarmored player banks very little.
- H.E.A.T. accumulates continuously across multiple hits — it is not reset or capped per-hit; each qualifying hit adds to whatever is already banked.

---

## H.E.A.T. — Storage & Loss

- **Persistence:** H.E.A.T. does not decay over time. Once banked, it stays available indefinitely until either spent or lost.
- **Loss conditions (H.E.A.T. is fully wiped):**
  - **Suit removal** (assisted or unassisted).
  - **Player death** — this applies even from Tier 1 onward, where the Assault suit itself now survives death and stays equipped through respawn. The suit stays on; the banked H.E.A.T. does not carry over.
- There is no partial loss condition (e.g. no gradual decay, no percentage lost on minor events) for **passive loss** — H.E.A.T. is either fully intact or fully wiped by the conditions above. (Active *spending*, e.g. Juggernaut's continuous partial draw, is a separate, per-consumer mechanic — see that add-on's entry.)

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
