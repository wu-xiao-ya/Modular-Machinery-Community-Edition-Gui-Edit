# Long-Capacity Requirements

[Home](Home) · [中文版](Long-Capacity-Requirements-ZH)

## The problem

MMCE stores the recipe `amount` of `RequirementFluid` / `RequirementGas` as a Java `int`. An `int` overflows above **2,147,483,647** (~2.1 billion mB). Once your machines and custom hatches operate in the billions/trillions of mB, vanilla MMCE recipes silently break.

## What MMCEGE does

MMCEGE patches the requirement system (via Mixins) so fluid/gas recipe amounts are parsed and processed as **`long`** (up to 9.2 quintillion). This runs across the whole recipe lifecycle:

- parse (`createRequirement`)
- "can start crafting" check
- input consumption (`startCrafting`)
- output filling (`finishCrafting`)
- max-parallelism calculation

Vanilla, small-amount recipes are unaffected — they keep using the original `int` path.

## How to use it

**No configuration. No extra mod node.** Just write a large `amount` in your normal MMCE recipe JSON:

```json
{
  "type": "fluid",
  "io": "input",
  "fluid": "water",
  "amount": 5000000000
}
```

```json
{
  "type": "gas",
  "io": "output",
  "gas": "hydrogen",
  "amount": 8000000000
}
```

Both parse and run as `long`. Note: this fix covers normal `fluid` / `gas` requirements; per-tick fluid/gas requirements are not claimed as supported yet and will be handled separately.

## Why it matters here

This patch is what makes the **long-capacity custom hatches** and **AE2 mixed buses** actually usable in recipes — their tanks store `long` amounts, and recipes must be able to demand/produce matching `long` amounts without overflow. See [Custom Hatches](Custom-Hatches-EN) and [Custom AE2 Buses](Custom-AE-Buses-EN).

## Notes & limits

- The patch targets MMCE's `RequirementFluid`, `RequirementGas`, their requirement **types** (JSON parsing), and `RecipeCraftingContext` component lookup.
- A requirement only takes the `long` path when the bound handler supports it (custom hatches / mixed buses do). Plain vanilla hatches still cap at `int` capacity, so a recipe demanding more than a vanilla hatch can hold simply won't be satisfiable by that hatch.
- `FluidStack` / `GasStack` themselves remain `int`-based; MMCEGE keeps a parallel `long` value and only down-casts when handing data to vanilla `int` APIs. This is transparent for recipe authors but worth knowing when debugging.

## Under the hood (for contributors)

- `mixin/MixinRequirementTypeFluid` / `MixinRequirementTypeGas` — `@Overwrite createRequirement`, read `amount` with `getAsLong()`, store the full `long`.
- `mixin/MixinRequirementFluid` / `MixinRequirementGas` — inject `long` paths into canStart/start/finish/getMaxParallelism, delegating to `common/requirement/LongRequirementIO` (with snapshot handlers for safe simulation).
- `mixin/MixinRecipeCraftingContext` — restores mixed-input-bus internal handlers when MMCE's component lookup returns empty.
- `common/requirement/LongAmountRequirement` — the interface that carries the `long` value on patched requirements.
