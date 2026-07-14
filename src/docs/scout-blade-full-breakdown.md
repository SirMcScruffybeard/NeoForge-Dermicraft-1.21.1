# Scout Blade — Full Mechanics Breakdown

Recon's Tier 1+ suit-integrated signature weapon. This doc supersedes/expands the Blade entry in `dermicraft-suit-notes.md` (Addendum 2) — fold this level of detail in when updating that file. Name confirmed as final: **Scout Blade** (renamed from "Recon Blade"), not a placeholder.

---

## Control Scheme

- **Trigger input:** the player's normal attack input (left-click), while the **main hand is empty**.
- This is the **same input** used for an ordinary unarmed punch — the Scout Blade isn't a separately-activated weapon, it simply *is* what Recon's empty-hand attack becomes once unlocked at Tier 1. There is no charge-and-release cycle here (unlike Assault's One Punch) — every qualifying left-click swings the Scout Blade, and its damage is calculated fresh each time based on the player's speed at that moment.

---

## Damage Model

- **Base:** the Scout Blade has a base damage value, same as any melee weapon.
- **Speed multiplier:** that base damage is multiplied by a value derived from the player's speed — the faster the player is moving, the harder the hit lands. A stationary or slow-moving swing still deals the base amount; it isn't reduced or made weaker for standing still, it simply doesn't get the bonus.
- **Scaling:** linear, and intentionally **uncapped** by design — there is no hard ceiling on how much the multiplier can grow. Any real-world limit comes from the engine's own maximum player speed, which is a Code-level concern rather than a design-level cap.

---

## Speed Sampling

- **What's measured:** the player's **peak speed** reached during a short recent window **before** the hit connects — not the exact instantaneous speed at the moment of impact.
- **Why peak, not instant:** this rewards a hit landed just after a burst of speed (a dash, a fall, a grapple pull) even if the player has slowed slightly during the final approach to the target — matching the feel of "carrying speed into a hit" rather than requiring the player to somehow still be accelerating at full pace on the exact frame of contact.
- **Contrast with Assault's Slipstream trait:** Recon's other speed-based mechanic (the passive evasion trait) samples speed differently — one tick prior to impact, a fixed single-point snapshot rather than a peak-over-a-window. The two mechanics intentionally use different sampling methods since they serve different purposes: the Blade rewards a recent burst of momentum carried into an attack, while the evasion trait cares about how fast the player is moving at the literal instant a hit would land.

---

## Tier 1 Tuning

- Available starting at Tier 1, but deliberately weak there — enough to demonstrate the concept and hint at its long-term potential without providing a maxed-out damage spike from the moment Recon's specialization begins.
- Grows more dangerous over time not through any change to the Scout Blade itself, but because it directly benefits from Recon's own broader progression: Dash Speed, stacked Flight/Climbing add-ons, and any other mobility-boosting investment all raise the player's achievable peak speed, which raises the Scout Blade's damage multiplier in turn. Every mobility choice a Recon player makes indirectly becomes a damage choice as well.

---

## Open / Undecided

- **Block/terrain interaction:** unlike One Punch, no rule has been established for what happens if the Scout Blade connects with a block instead of a mob (e.g. does it do nothing to blocks, deal normal weapon damage, or something else). Worth deciding whenever it comes up, though it may simply not need one if the Scout Blade's role is understood as combat-only.
- **Interaction with Slipstream in the same swing:** no conflict currently exists since one is offensive and the other defensive, but worth keeping in mind that a fully speed-optimized Recon build is stacking benefits from both mechanics simultaneously off the same underlying stat (speed) — a natural, intended synergy rather than a problem, just worth remembering when evaluating overall power at high speed.

---

## Summary Flow (for quick reference)

1. Player reaches Tier 1+ on Recon — the Scout Blade becomes the suit's empty-hand attack.
2. Player left-clicks with an empty main hand → Scout Blade swings.
3. Damage = base damage × multiplier derived from the player's **peak speed** over the short window before the hit landed.
4. No charge, no bank, no separate release — every swing is calculated independently, in the moment.
5. Damage output scales upward over time purely as a byproduct of Recon's other mobility investments raising achievable peak speed, not through any upgrade to the Scout Blade itself.
