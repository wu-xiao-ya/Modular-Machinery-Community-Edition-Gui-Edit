# Custom AE2 Buses (experimental)

[Home](Home) · [中文版](Custom-AE-Buses-ZH)

> ⚠️ **Experimental.** The JSON shape mirrors the hatch system, but this is newer and fields may change. Capabilities depend on **AE2 + Mekanism Energistics** both being present. Re-verify saves and recipes after updating the mod.

Three bus types, one directory each, one `.json` per bus, registered as a block + tile **at game start**, connecting to the AE2 ME network via an `AENetworkProxy` (requires a channel, consumes network power). Max file size 1 MB each.

| Bus | Directory | Direction | Item | Fluid | Gas | config slots |
|---|---|---|:--:|:--:|:--:|:--:|
| ME Item Input Bus | `custom_ae_item_input_buses/` | input | ✅ | ❌ | ❌ | ✅ |
| Mixed Input Bus | `custom_ae_mixed_input_buses/` | input | ✅ | ✅ | ✅ | ✅ |
| Mixed Output Bus | `custom_ae_mixed_output_buses/` | output | ✅ | ✅ | ✅ | ❌ |

> No shipped example JSON yet. Fields on this page follow the registry source `Def` / `load()` parsing.

Two ways to define the GUI:
- **New style**: a `gui` object + `components[]` (use `type`+`role` to distinguish item/fluid/gas config/storage).
- **Legacy style**: flat `configSlots`/`storageSlots`/`fluid*`/`gas*` fields. The mod auto-converts them to new style via `buildLegacyGui`.

Per-bus support: ME Item Input = legacy only; Mixed Input = both (new style wins); Mixed Output = new style only.

---

## 1. ME Item Input Bus

Item-only input; dynamic slot count, oversized stacks, config/storage pre-fetch from the ME network.

### Top-level fields

| Field | Type | Default | Notes |
|---|---|---|---|
| `id` | string | **required** | Unique id (supports `namespace:path`) |
| `displayName` | string | null | Display name |
| `guiBackgroundTexture` | string | null | GUI background texture |
| `configSlots` | `{x,y}[]` | `[]` | Config slot coords (max 4096 each) |
| `storageSlots` | `{x,y}[]` | `[]` | Storage slot coords |
| `playerInventoryX` | int | 0 | Player inventory X |
| `playerInventoryY` | int | 123 | Player inventory Y |
| `playerHotbarY` | int | 181 | Hotbar Y |
| `blockTexture` | string | null | Block texture |
| `blockModel` | string | null | Block model (falls back to `block.model`) |
| `block` | object | — | Block properties, see below |

`block` sub-object (each field can also be at top level): `model`, `material`(iron), `hardness`(2.0), `resistance`(10.0), `harvestTool`(pickaxe), `harvestLevel`(1), `soundType`(metal), `lightLevel`(0.0), `lightOpacity`(255), `slipperiness`(0.6), `unbreakable`(false).

### GUI

Legacy only: `configSlots` and `storageSlots` pair up by index (`configSlots[i]` ↔ `storageSlots[i]`, the "config" and "storage" positions of the same slot). A slot is active only when both indices are non-null. Slot count = max of the two array lengths.

```json
{
  "id": "my_item_input_bus",
  "displayName": "My Item Input Bus",
  "configSlots":  [ {"x":8,"y":18}, {"x":26,"y":18} ],
  "storageSlots": [ {"x":8,"y":54}, {"x":26,"y":54} ],
  "playerInventoryX": 8, "playerInventoryY": 123, "playerHotbarY": 181
}
```

---

## 2. Mixed Input Bus

One block inputs **item + fluid + gas** at once (three AE storage channels), exposed to MMCE as three grouped components sharing a groupId.

### Top-level fields (new + legacy)

| Field | Type | Default | Notes |
|---|---|---|---|
| `id` | string | **required** | Unique id |
| `displayName` | string | null | Display name |
| `guiBackgroundTexture` | string | null | GUI background texture |
| `guiWidth` / `guiHeight` | int | 176 / 235 | GUI size (legacy), `[1,4096]` |
| `backgroundTextureWidth/Height` | int | =gui size | Actual background pixel size (for scaling) |
| `textureLayers` | array | `[]` | Extra texture layers, see below, max 256 |
| `playerInventoryX/Y` | int | 8 / 141 | Player inventory offset |
| `playerHotbarY` | int | 199 | Hotbar Y |
| `blockTexture` / `blockModel` | string | null | Block texture / model (model uses `block.model` first) |
| `gui` | object | — | **New-style GUI** (wins over legacy) |

**Legacy slot/tank fields** (auto-converted to new style):
- Item: `configSlots`, `storageSlots` (`{x,y}[]`)
- Fluid: `fluidConfigTank(s)`, `fluidStorageTank(s)` (`{x,y,width,height}`; singular is sugar for plural)
- Gas: `gasConfigTank(s)`, `gasStorageTank(s)`
- Older aliases: `fluidConfigSlot`/`gasConfigSlot` (`{x,y}`), `fluidStorageTank`/`gasStorageTank` (`{x,y,w,h}`)

> Don't write singular and plural together. `TankRect` fields: `x`/`y` (default 0), `width`/`height` (default 16, `[1,4096]`).

`textureLayers[]` each: `texture`(required), `foreground`(false), `x`/`y`(0), `width`/`height`(16), `textureWidth/Height`(=w/h), `corner`(0, `[0,1024]`), `useNineSlice`(false), `priority`(0).

### New-style `gui`

`gui`: `width`(176), `height`(235), `components[]` (max 2048).

Each `components[]`: `type`(required), `role`, `x`/`y`(0), `width`/`height`(16, `[1,4096]`), `index`(-1 auto-increment, `[0,4095]`).

**type + role combinations**:

| type | role | Meaning |
|---|---|---|
| `slot` | `item_config` | Item config slot |
| `slot` | `item_output` / `item_storage` | Item storage slot (equivalent) |
| `slot` | `fluid_config` | Fluid config slot |
| `slot` | `gas_config` | Gas config slot |
| `slot` | `capacity_card` | Capacity-card slot; accepts AE capacity cards and custom registered capacity-card items |
| `tank` | `fluid_storage` | Fluid storage tank |
| `tank` | `gas_storage` | Gas storage tank |
| `player_inventory` | — | Player inventory (takes component x/y) |

> Note: for fluid/gas, **config uses `slot`, storage uses `tank`**.

```json
{
  "id": "my_mixed_input",
  "displayName": "My Mixed Input Bus",
  "gui": {
    "width": 176, "height": 235,
    "components": [
      { "type": "slot", "role": "item_config",  "index": 0, "x": 8,  "y": 18 },
      { "type": "slot", "role": "item_storage", "index": 0, "x": 8,  "y": 54 },
      { "type": "slot", "role": "fluid_config", "index": 0, "x": 44, "y": 18 },
      { "type": "tank", "role": "fluid_storage","index": 0, "x": 44, "y": 40, "width": 18, "height": 60 },
      { "type": "slot", "role": "gas_config",   "index": 0, "x": 80, "y": 18 },
      { "type": "tank", "role": "gas_storage",  "index": 0, "x": 80, "y": 40, "width": 18, "height": 60 },
      { "type": "slot", "role": "capacity_card", "index": 0, "x": 116, "y": 18 },
      { "type": "player_inventory", "x": 8, "y": 141 }
    ]
  }
}
```

Base capacity: fluid/gas tanks 8000 mB each; capacity cards increase fluid/gas per-slot capacity only. Item slots are not affected. If capacity drops while connected to AE, overflow is pushed back to AE; if disconnected, overflow stays temporarily until the bus can rebalance.

---

## 3. Mixed Output Bus

Buffers recipe outputs (item+fluid+gas) and pushes them to the ME network, exposed as MMCE-CE's combined `item_fluid_gas` component. It **only receives outputs**, so there are no config slots.

### Top-level fields

| Field | Type | Default | Notes |
|---|---|---|---|
| `id` | string | **required** | Unique id |
| `displayName` | string | null | Display name |
| `guiBackgroundTexture` | string | null | GUI background texture |
| `guiWidth` / `guiHeight` | int | 176 / 235 | GUI size |
| `backgroundTextureWidth/Height` | int | =gui size | Background pixel size |
| `textureLayers` | array | `[]` | Extra texture layers (same fields as Mixed Input) |
| `blockTexture` / `blockModel` | string | null | Block texture / model |
| `gui` | object | — | **Required** (no legacy fields, no buildLegacyGui) |

> The output bus has **no** `configSlots`/`storageSlots`/`fluid*`/`gas*` legacy fields and **no** `playerInventory*`. You must use `gui.components`, or it has no usable slots.

### New-style `gui`

`gui`: `width`(176), `height`(235), `components[]` (max 2048). Component fields same as Mixed Input.

**type + role combinations** (**storage only, no config, no player_inventory**):

| type | role | Meaning |
|---|---|---|
| `slot` | `item_storage` / `item_output` | Item output slot (equivalent) |
| `slot` | `capacity_card` | Capacity-card slot; accepts AE capacity cards and custom registered capacity-card items |
| `tank` | `fluid_storage` | Fluid storage tank |
| `tank` | `gas_storage` | Gas storage tank |

```json
{
  "id": "my_mixed_output",
  "displayName": "My Mixed Output Bus",
  "gui": {
    "width": 176, "height": 235,
    "components": [
      { "type": "slot", "role": "item_storage", "index": 0, "x": 8,  "y": 18 },
      { "type": "slot", "role": "item_storage", "index": 1, "x": 26, "y": 18 },
      { "type": "tank", "role": "fluid_storage","index": 0, "x": 80, "y": 18, "width": 18, "height": 60 },
      { "type": "tank", "role": "gas_storage",  "index": 0, "x": 104,"y": 18, "width": 18, "height": 60 },
      { "type": "slot", "role": "capacity_card", "index": 0, "x": 132, "y": 18 }
    ]
  }
}
```

## 4. Capacity Cards

Mixed input/output buses support `slot` components with `role: "capacity_card"`. AE capacity cards are accepted by default. Custom cards can be registered through JSON files in `config/mmceguiext/capacity_cards/*.json`:

```json
{
  "item": "appliedenergistics2:material",
  "meta": 35,
  "multiplier": 1.5,
  "flatFluid": 1000,
  "flatGas": 1000
}
```

CraftTweaker entry:

```zenscript
mods.mmceguiext.MMCEGEEvents.registerCapacityCardWithFlat("appliedenergistics2:material", 1.5, 1000, 1000);
```

Capacity constants: fluid/gas tanks 8000 mB each; default item slots 25, tanks 1; dynamic cap 4096.

---

## 4. Constants

| Constant | Value | Applies to |
|---|---|---|
| Max file size | 1 MB | all |
| `MAX_SLOT_POINTS` | 4096 | item input bus |
| `MAX_GUI_COMPONENTS` | 2048 | mixed input/output |
| `MAX_COMPONENT_INDEX` | 4095 | mixed input/output |
| `MAX_COMPONENT_SIZE` | 4096 | mixed input/output |
| `MAX_TEXTURE_LAYERS` | 256 | mixed input/output |

## 5. Notes

- Editing JSON requires a **game restart**.
- Don't rename an `id` once referenced by a structure or save.
- These are experimental; storing/extracting large fluid/gas amounts in recipes needs [Long-Capacity Requirements](Long-Capacity-Requirements-EN).
