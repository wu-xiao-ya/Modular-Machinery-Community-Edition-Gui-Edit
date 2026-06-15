# Custom Hatches

[Home](Home) · [中文版](Custom-Hatches-ZH)

Define a brand-new hatch from one JSON file (a **block + item + tile**), acting as an MMCE multiblock input/output component in recipes. Capacities use **long**, far beyond the vanilla `int` limit (~2.1 billion) — pair it with [Long-Capacity Requirements](Long-Capacity-Requirements-EN).

- Directory: `config/mmceguiext/custom_hatches/*.json`
- Scanned: **at game start** (edits need a restart, not `/ct reload`)
- Max file size: 1 MB
- Registers the block as `mmceguiext:<id>`

Working examples:
- `config/mmceguiext/custom_hatches/custom_gas_input_hatch.json`
- `examples/custom_hatches/fluid_meter_hatch_test.json`

---

## 1. Top-level fields

| Field | Aliases | Type | Default | Notes |
|---|---|---|---|---|
| `id` | — | string | **required** | Unique id (lowercased); block registers as `mmceguiext:<id>` |
| `displayName` | — | string | null | Block/item display name |
| `componentType` | — | string | `fluid` | Default component type (used when `components` is empty), values in §1.1 |
| `ioType` | — | string | `input` | Default IO (used when `components` is empty): `input` / `output` (alias `out`) |
| `capacity` | — | long | `1000` | Base capacity, fallback default for the capacities below, range `[1, Long.MAX]` |
| `fluidCapacity` | — | long | =`capacity` | Fluid tank capacity (mB) |
| `gasCapacity` | — | long | =`capacity` | Gas tank capacity (mB, needs Mekanism) |
| `energyCapacity` | — | long | =`capacity` (also falls back to `energy`) | Energy storage cap (FE) |
| `energyTransfer` | — | long | =`energyCapacity` (also `energyTransferLimit`) | Per-tick energy transfer cap (FE) |
| `components` | — | array | `[]` | MMCE components this hatch exposes, see §2, max 256 |
| `block` | — | object | see §3 | Block appearance & physical properties |
| `gui` | — | object | see §4 | GUI layout |
| `guiStyleFile` | — | string | null | External GUI style file (loaded by GlobalGuiStyleManager) |
| `tips` | `tooltip`,`tooltips` | string[]/string | `[]` | Item/tank tooltip lines (placeholders, see §5), max 512 |
| `outputSlotLock` | `output_slot_lock`,`lockOutputSlots`,`lock_output_slots` | boolean | `true` | Lock output slots (same item only stacks into existing slots) |
| `blockTexture` / `blockModel` | — | string | null | Top-level shortcut, overridden by `block.texture`/`block.model` |
| `inputSlot` / `outputSlot` | — | `{x,y}` | `{0,0}` | Legacy slot coords (used without `gui.components`) |
| `tank` | — | object | see §3.3 | Legacy tank render params (used without a gui tank component) |
| `texts` | — | array | `[]` | Legacy static text list (overridden by gui text components) |

### 1.1 `componentType` values

- Single: `item` / `fluid` / `gas` / `energy` (aliases `power` / `fe` / `rf`)
- Combined: `mixed` / `hybrid` / `item_fluid` / `item_fluid_gas` (auto-creates a combined component)

---

## 2. `components[]` — MMCE component declarations

Each item is `{ "type": "...", "io": "..." }`:

| Field | Type | Default | Notes |
|---|---|---|---|
| `type` | string | **required** | `fluid` / `gas` / `item` / `energy` (aliases `power`/`fe`/`rf`) / `mixed` (aliases `hybrid`/`item_fluid`/`item_fluid_gas`) |
| `io` | string | `input` | `input` / `output` (alias `out`) |

> Combined behavior: if the array contains any `mixed`/`hybrid`/`item_fluid`/`item_fluid_gas`, the system auto-creates a pair (input and/or output) of combined components by their `io`, and skips other same-type entries.

```json
"components": [
  { "type": "fluid", "io": "input" },
  { "type": "energy", "io": "input" }
]
```

---

## 3. `block` — appearance & properties

Each field may also be written at top level (as fallback; `block` wins).

| Field | Aliases | Type | Default | Notes |
|---|---|---|---|---|
| `model` | top-level `blockModel` | string | null | Block model resource path |
| `texture` | top-level `blockTexture` | string | null | Block texture resource path |
| `material` | — | string | `iron` | Material type |
| `hardness` | — | float | `2.0` | Hardness |
| `resistance` | — | float | `10.0` | Blast resistance |
| `harvestTool` | — | string | `pickaxe` | Harvest tool |
| `harvestLevel` | — | int | `1` | Harvest level |
| `soundType` | — | string | `metal` | Sound type |
| `lightLevel` | — | float | `0.0` | Light emission |
| `lightOpacity` | — | int | `255` | Light opacity |
| `slipperiness` | — | float | `0.6` | Surface slipperiness |
| `unbreakable` | — | boolean | `false` | Unbreakable |
| `textureLevels` | `texture_levels`; top-level `blockTextureLevels`/`block_texture_levels`/`blockTextures` | array | `[]` | Swap appearance by fill ratio, see §3.1, max 256 |

### 3.1 `block.textureLevels[]` — swap block appearance by fill ratio

Each item can be an object, or a plain texture string (then `minFillRatio` is spread evenly 0..1 by index).

| Field | Aliases | Type | Default | Notes |
|---|---|---|---|---|
| `content` | `resource`,`source`,`type`,`storage` | string | inherits parent (default `fluid`) | Which content's fill ratio to watch: `fluid` / `gas` / `energy` |
| `minFillRatio` | `min_fill_ratio`,`minRatio`,`ratio`,`threshold`,`min`,`from`,`level`,`percent` | double | `0.0` | Min fill ratio to trigger this level `[0,1]` |
| `texture` | `blockTexture`,`block_texture` | string | null | Texture for this level |
| `model` | `blockModel`,`block_model` | string | null | Model for this level |

> At render time the highest level with matching content and `minFillRatio <= fillRatio` is chosen; the three contents are evaluated separately and the highest-priority result wins.

```json
"block": {
  "model": "mmceguiext:hatch/mix_in_lv1",
  "texture": "minecraft:textures/blocks/glass.png",
  "textureLevels": [
    { "content": "fluid", "minFillRatio": 0.25, "texture": "minecraft:textures/blocks/wool_colored_light_blue.png" },
    { "content": "fluid", "minFillRatio": 1.0,  "texture": "minecraft:textures/blocks/diamond_block.png" }
  ]
}
```

### 3.3 Legacy `tank` object (when no gui tank component)

| Field | Aliases | Type | Default | Notes |
|---|---|---|---|---|
| `x`,`y` | — | int | `0` | Render coords |
| `width` | — | int | `16` | Width `[1,4096]` |
| `height` | — | int | `48` | Height `[1,4096]` |
| `content` | — | string | `fluid` | `fluid` / `gas` / `fluid_gas` (show gas if fluid empty) / `energy` (aliases `power`/`fe`) |
| `renderMode` | `render_mode`,`render`,`mode` | string | null | `solid`(`flat`/`color`) / `texture`(`textured`/`sprite`) |
| `alpha` | `opacity`,`transparency` | float | null (1.0) | Opacity `[0,1]` (if >1 and ≤255, auto-divided by 255) |

---

## 4. `gui` — layout

| Field | Type | Default | Notes |
|---|---|---|---|
| `width` | int | `176` | GUI width `[1,4096]` |
| `height` | int | `166` | GUI height `[1,4096]` |
| `coordinateWidth` | int | `-1` (off) | Logical coordinate width (enables coordinate scaling) |
| `coordinateHeight` | int | `-1` (off) | Logical coordinate height |
| `components` | array | `[]` | GUI components, see §4.1, max 4096 (after expansion) |

### 4.1 `gui.components[]`

Shared fields: `type` (required), `role`, `x`, `y`, `width`, `height`, `priority` (aliases `zIndex`/`z_index`/`z`/`layer`).

`type` values: `slot` / `slot_grid` (alias `slots`) / `tank` / `text` / `player_inventory`.

**`slot`** — single slot
- `role`: `input` / `output` (only these participate in runtime auto-index assignment)
- `index`: slot index, `-1` auto-increments, max 4095

**`slot_grid` / `slots`** — slot grid (auto-expands into multiple slots)
- `rows` (`rowCount`/`yCount`, default 1, ≤256), `columns` (`cols`/`columnCount`/`xCount`, default 1, ≤256)
- `spacingX` (`xSpacing`/`gapX`, default 2), `spacingY` (`ySpacing`/`gapY`, default 2)
- `slotSize` (`size`, default 16)
- `visibleRows`/`visibleColumns` (default 0=all visible, for scrolling), `scrollMode`, `scrollbar`
- Scrollbar set: `scrollbarX/Y/Width/Height`, `scrollbarThumbHeight`, `scrollbarTexture` (+ hover/pressed/disabled), `scrollbarTextureWidth/Height`, `scrollbarU/V` (+ hover/pressed/disabled variants) — all support snake_case
- Item overlay: `itemOverlay`, `itemOverlayTexture`, `itemOverlayTextureWidth/Height`, `itemOverlayU/V`

**`tank`** — tank region
- `content`: `fluid` / `gas` / `fluid_gas` / `energy` (`power`/`fe`)
- `renderMode` (`render_mode`/`render`/`mode`), `alpha` (`opacity`/`transparency`), `overlay` (default true, MMCE-style frame)
- `color`: energy bar ARGB hex (default green `0xFF3DDC84`)
- `tips` (`tooltip`/`tooltips`): tank tooltip, supports placeholders
- `x`/`y`/`width`/`height`

**`text`** — dynamic text
- `value`: text or placeholder key (see §5)
- `color`: ARGB hex (default white `0xFFFFFF`)
- `scale`: scale (default 1.0)
- `align` (`alignment`/`textAlign`/`text_align`): `left`/`center`/`right` (aliases `start`/`middle`/`end`)
- `content`: chooses the placeholder data source

**`player_inventory`** — player inventory
- `x`, `y` (3×9 main), `hotbarY` (`-1` → auto = `y+58`)

---

## 5. Text placeholders

Two mechanisms:

### 5.1 `text.value` direct key (whole string equals the key)

Amounts: `tank.amount` / `tank.capacity` / `tank.amount_capacity`, add `_formatted` (or `.compact`) for compact form (e.g. `1.2K`).
Names: `fluid.name`, `gas.name`, `tank.name`.
Gas: `gas.amount`, `gas.capacity`, `gas.amount_capacity` (+ `_formatted`).
Energy: `energy.name`, `energy.amount` (`energy.stored`), `energy.capacity` (`energy.max`), `energy.amount_capacity`, `energy.transfer` (all with `_formatted`).
Slots: `input.slot` → "Input", `output.slot` → "Output".
A non-matching value is shown verbatim.

### 5.2 Template inline placeholders (for `tips`/`tooltip` strings)

Like `Fluid: {amount_capacity_formatted} {unit}`. Common tokens:
`{name}`, `{amount}`, `{capacity}`, `{amount_capacity}` (each with `_formatted`), `{energy}`, `{energy.capacity}`, `{energy.amount_capacity}`, `{energy.transfer}` (each with `_formatted`), `{unit}` (`FE` for energy, else `mB`). The data source is chosen by the component's `content`.

---

## 6. Complete minimal example

```json
{
  "id": "fluid_meter_hatch_test",
  "displayName": "Fluid Meter Hatch Test",
  "componentType": "fluid",
  "ioType": "input",
  "capacity": 4000,
  "fluidCapacity": 4000,
  "block": { "model": "mmceguiext:hatch/mix_in_lv1", "texture": "minecraft:textures/blocks/glass.png" },
  "components": [ { "type": "fluid", "io": "input" } ],
  "gui": {
    "width": 176, "height": 166, "coordinateWidth": 176, "coordinateHeight": 166,
    "components": [
      { "type": "tank", "x": 80, "y": 15, "width": 18, "height": 61, "content": "fluid", "overlay": true,
        "tips": [ "Fluid: {amount_capacity_formatted} mB" ] },
      { "type": "player_inventory", "x": 8, "y": 84, "hotbarY": 142 }
    ]
  }
}
```

---

## 7. Using it in a machine & probe

- Use the block id `mmceguiext:<id>` in your machine structure like any MMCE hatch, and demand/produce the matching `requirement` (fluid/gas/item/energy) in recipes.
- With The One Probe installed, the hatch's fill level and capacity show on the probe automatically.

## 8. Notes

- Editing JSON requires a **game restart**.
- Don't rename an `id` once referenced by a structure or save — the block will be lost.
- Three GUI implementations: fluid-processor hatch (full custom, with AE inventory access), fluid hatch (extends MMCE's native), upgrade bus (custom layout over MMCE's upgrade bus).
