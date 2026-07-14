# Dermicraft Misc Notes

Running log of decided design choices for ideas that don't fit cleanly into the other per-category companion docs (Catalysts, Slurries, Crafting, Flask, Beaker, Gadgets, Machines, Tools). Add a new entry per idea as decisions get made — the "why" matters as much as the "what."

---

## In-game Guide System (Notebook → Tablet)

**Status:** Overall arc and tone decided. Individual item mechanics (Notebook scope/unlock, Tablet capabilities) still largely open — Tablet's own gadget-specific detail lives in `dermicraft-gadget-notes.md`; this entry covers the system's shared narrative/tone framework and the parts that don't fit a single category doc.

**What it is:** Dermicraft's in-game guide is split into two sequential items rather than one flat reference, giving the guide itself a progression arc tied to the mod's own Tier 1 → OT milestone:
- **The Notebook** — a book-style item, available from early game, guiding the player toward building the Operating Theater. Non-living, fixed/finished content.
- **The Tablet** — an intelligent, living Gadget crafted by the OT (see `dermicraft-gadget-notes.md`), built from the Notebook plus other ingredients. The Notebook is consumed but its information carries over. Self-authoring — actively records and writes new notes as the player works, rather than presenting only pre-written content.

An online/external guide is still expected to be necessary for full reference, but the goal is for the in-game guide to cover most of what a player needs without leaving the mod.

**Narrative framing — the unsettled scientist.** Both guides are framed as the work of a single recurring figure: an unsettled/unstable scientist whose lab and research the player is stepping into throughout the whole mod. The Notebook is their already-written, left-behind notes — the player starts out **reconstructing** this scientist's earlier work rather than inventing it themselves. Reaching the OT and crafting the Tablet marks a deliberate narrative turn: the player shifts from reconstructing the scientist's past work to **carrying it onward** into new territory the scientist never finished or reached. This retroactively reinforces existing framing already on record — the Tier 1 → Tier 2 wall as "leaving the tutorial phase, entering the real game," and the OT as the mod's central milestone — by giving that transition an actual narrative beat, not just a mechanical one.

**Tone rule — flavor never garbles function.** The scientist's unsettled voice is meant to come through in framing, asides, and margin-note-style commentary, but the mod's existing "aesthetic flavor over punishing/obscuring mechanics" principle (see primer's tone target) extends directly to the guide's own readability: actual recipe/mechanic instructions must stay clean and clearly distinguishable from flavor text (e.g. a visually/structurally distinct "notes" vs. "procedure" formatting), so a player skimming for practical information never has to wade through unreliable prose to find it. The unsettling tone is something a player can choose to read into, not an obstacle to using the guide.

**Scientist's fate — a genuinely dark, currently-presumed outcome, confirmed to resurface as the final boss.** Not yet detailed beyond that. Open questions this raises, flagged for a future session: whether the Tablet's own voice reflects any awareness of what happened to the scientist (or is equally in the dark until a late-game reveal); whether early hints of wrongness should surface gradually through the Notebook's tone or stay fully hidden until the reveal; and what the scientist's transformation/fate actually was mechanically (a "living machine" possibility was floated, unconfirmed).

**Tablet declutter mechanic:** Player can manually lock/unlock sections of the Tablet's content for personal organization — no mechanical cost, purely presentation control. Full detail in `dermicraft-gadget-notes.md`.

**Open questions:** Notebook's exact scope (critical-path-to-OT only, or does it also cover side content the player might encounter earlier?) and unlock model (available from the start vs. discovery-gated). Whether the Tablet's voice shifts once "carrying on" begins, or the content shifts while the voice stays consistent. Exact nature of the Tablet's physical connection to the player (see `dermicraft-gadget-notes.md`). Whether/when the player starts suspecting the scientist's fate before the final-boss reveal.

---

## Template for new entries

```
### [Idea Name]

**Status:**

**What it is:**

**Recipe/logic:**

**Open questions:**
```
