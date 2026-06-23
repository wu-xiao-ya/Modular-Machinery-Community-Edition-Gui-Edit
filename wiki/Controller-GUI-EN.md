# Controller GUI

[Home](Home) · [中文版](Controller-GUI-ZH)

Replace / customize the MMCE Machine and Factory controller GUIs: resizable, custom background textures, multi-panel info areas, texture layers, Smart Interface editors, custom buttons, sliders, and pages.

## How it works

MMCEGE hooks Forge's `GuiOpenEvent` and, the moment MMCE opens the vanilla `GuiMachineController` / `GuiFactoryController`, swaps in the resizable version — **only when** a custom texture, hidden default background, or per-machine style override is present. It does **not** patch any MMCE GUI class.

Styles come from two layers; per-machine wins, falling back to global:
- **Global**: `config/mmceguiext/client.cfg`
- **Per-machine**: the `mmce_gui_ext` node inside `config/modularmachinery/machinery/*.json`

> There are a lot of fields — the full quick reference is `examples/MMCE_GUI_EXT_FIELD_REFERENCE.md`; runnable examples are in `examples/quick-start/` and `examples/game-ready-1.0.1/`.

---

## 1. Global config `client.cfg`

`machineController.*` and `factoryController.*` each have a matching set of keys:

- Background: `backgroundTexture`, `backgroundTextureWidth`, `backgroundTextureHeight`, `useNineSlice`, `backgroundCorner`, `hideDefaultBackground`
- Size: `disableRightExtension` (force MMCE base width, no right extension)
- Panels: `defaultPanelId`, `customPanels` (format `id,x,y,width,height`)
- Smart Interface editor: `enableSmartInterfaceEditor`, `smartInterfaceEditorX`/`Y` (`-1` = auto bottom-right), `smartInterfaceEditorInputWidth`, `smartInterfaceEditorVirtualKey` (writes to controller `customData[key]` when no DataPort is bound; multiple keys split by `,` or `;`)
- Factory only: `factoryController.specialThreadBackgroundColor` (hex `RRGGBB` or `AARRGGBB`, tint for core/special thread rows)

The global switch `enabled`, scroll step `wheelStep`, etc. live here too. See `examples/game-ready-1.0.1/client.cfg.sample`.

**Background rules**: custom texture present → use it; absent → use MMCE default; absent and `hideDefaultBackground=true` → draw no background. When the MMCE default texture is used, info text stays in the original info area; multi-panel mode is enabled only in custom-texture mode.

---

## 2. Per-machine override `mmce_gui_ext`

Write it into each machine JSON:

```json
{
  "registryname": "power_transformer",
  "localizedname": "Power Transformer",
  "parts": [],
  "mmce_gui_ext": {
    "machineController": {
      "backgroundTexture": "yourmod:textures/gui/pt_machine.png",
      "guiWidth": 360, "guiHeight": 213,
      "disableRightExtension": true
    },
    "factoryController": {
      "backgroundTexture": "yourmod:textures/gui/pt_factory.png",
      "hideDefaultBackground": true,
      "guiWidth": 520, "guiHeight": 240,
      "specialThreadBackgroundColor": "B2E5FF",
      "threadQueueX": 12, "threadQueueY": 14,
      "threadVisibleRows": 7,
      "threadRowWidth": 90, "threadRowHeight": 34,
      "threadScrollbar": {
        "x": 98, "y": 22, "width": 8, "height": 197,
        "trackTexture": "yourmod:textures/gui/scroll_track.png",
        "thumbTexture": "yourmod:textures/gui/scroll_thumb.png",
        "trackColor": "66000000", "thumbColor": "FFFFFFFF",
        "textureWidth": 8, "textureHeight": 197,
        "thumbTextureWidth": 8, "thumbTextureHeight": 16,
        "thumbMinHeight": 15, "visible": true
      }
    }
  }
}
```

Aliases:
- Root node: `mmceGuiExt` / `mmce_gui_ext` / `mmce-gui-ext`
- Controller node: `machineController`/`machine_controller`/`machine`, `factoryController`/`factory_controller`/`factory`
- Size: `guiWidth`/`gui_width`/`width`, `guiHeight`/`gui_height`/`height`

Per-machine values override global. Since `1.0.1+`, if a machine defines any `mmce_gui_ext` node, unspecified GUI size falls back to the MMCE base size (`176x213` / `280x213`) first, reducing coupling to global cfg.

Common per-machine fields (see the quick reference for the full list): `backgroundTexture`, `backgroundTextureOffsetX/Y`, `hideDefaultBackground`, `guiWidth`, `guiHeight`, `enableRightExtension`, `useNineSlice`, `backgroundTextureWidth/Height`, `backgroundCorner`, `centerFullGui`, `specialThreadBackgroundColor`, `enableSmartInterfaceEditor`, `smartInterfaceEditorX/Y`, `smartInterfaceEditorInputWidth`, `smartInterfaceEditorVirtualKey`, `smartInterfaceEditorPriority`, `foregroundContentPriority`, `hideDefaultSmartInterfaceEditor`, `defaultPanelId`, `customPanels`, `smartInterfaceEditors`, `sliders`, `textureLayers`/`backgroundLayers`/`foregroundLayers`, `buttons`.
For the factory thread queue, use `threadQueueX`, `threadQueueY`, `threadVisibleRows`, `threadRowWidth`, and `threadRowHeight` for the row layout. Use the preferred structured `threadScrollbar` object for scrollbar position, size, track/thumb textures, colors, texture source sizes, minimum thumb height, and visibility. Legacy `threadScrollbarX/Y` remain supported. Customizing these fields also triggers the integrated-controller self proxy.

---

## 3. Size & textures

- Machine controller, right extension off: `176x213`; factory off: `280x213`.
- Right extension on: use runtime `guiWidth x guiHeight` (min as above). `guiWidth/Height` can be set globally or per-machine; per-machine wins.
- 9-slice (`useNineSlice=true`): source texture can stay at base size and scales cleanly. Direct mode (false): use exact target size to avoid stretching.

**Top-left decoration offset** (align textures whose main frame isn't at 0,0): `backgroundTextureOffsetX/Y` (aliases `offsetX`/`textureOriginX` etc.), `backgroundTextureWidth/Height`, `useNineSlice`, `backgroundCorner`, `centerFullGui` (center horizontally by full visual bounds, useful with a left-extended background).

---

## 4. Info panels

- `customPanels` each item: `id,x,y,width,height`, e.g. `"main,182,10,148,96"`.
- Each panel scrolls independently; multi-panel mode is enabled only in custom-texture mode.
- Text is routed to a panel via the ZS `[panel:id]` prefix; no prefix → `defaultPanelId`.

---

## 5. Texture layers

`textureLayers` / `backgroundLayers` (auto background) / `foregroundLayers` (auto foreground). Per-layer fields:
`id` (strongly recommended — runtime directives locate by it), `texture`, `offsetX`, `offsetY`, `width`, `height`, `textureWidth`, `textureHeight`, `alpha`/`opacity`/`transparency`, `corner`, `useNineSlice`, `foreground` (use it inside `textureLayers` to set the layer tier), `priority`.

`alpha` accepts `0.0` to `1.0`, or `0` to `255`; runtime directives can change it with `[mmcege:layer.<id>.alpha=0.5]`.

---

## 6. Smart Interface editors

Default editor: `< >` buttons + input + OK at the bottom-right, editing the controller's bound SmartInterface or virtual keys.
Custom editors: `smartInterfaceEditors[]`, each supports `id`, `x`, `y`, `inputWidth`, `virtualKey` (comma-separated for multiple), `title`, `showTitle`, `showInfo`, `showControls`, `inputBackground`, `priority`. Use `hideDefaultSmartInterfaceEditor` to hide the default one.

Virtual DataPort: write values even without a physical port. Recommended read (compatible with both native + virtual): read `ctrl.getSmartInterfaceData(key)` first, then fall back to `ctrl.customData`.

---

## 7. Custom buttons & pages

`buttons[]` are defined per-machine and validated server-side by a policy manager to prevent forged actions. Action types:
- `page` — client-side page switch.
- `event` — fire MMCE `ControllerButtonClickEvent` server-side.
- `smart_set` / `smart_add` — set / add a Smart Interface value (optional min/max clamp).

Examples: `examples/quick-start/buttons-and-pages.json`, `event-button-test.json`, `controller-button-test.json`.

---

## 8. Controller sliders

`sliders[]` are defined per-machine and work in both Machine and Factory controller GUIs. Aliases: `guiSliders`, `gui_sliders`, `rangeControls`, `range_controls`. Dragging a slider writes a numeric value to the Smart Interface / virtual DataPort key named by `key`.

Minimal example:

```json
"sliders": [
  {
    "id": "speed_slider",
    "x": 186,
    "y": 72,
    "width": 96,
    "height": 12,
    "key": "speed",
    "min": 0,
    "max": 10,
    "step": 0.5,
    "value": 2,
    "showText": true
  }
]
```

Common fields: `id`, `x`, `y`, `width`, `height`, `key`, `min`, `max`, `step`, `value`, `direction`, `trackColor`, `fillColor`, `thumbColor`, `borderColor`, `thumbWidth`, `thumbHeight`, `priority`, `foreground`, `visible`, `page`, `showText`, `textColor`.

Example: `examples/quick-start/sliders.json`.

---

## 9. Runtime ZS directives

Read from text lines pushed into MMCE's `ControllerGUIRenderEvent.extraInfo[]` (e.g. CraftTweaker/ZenScript's `onControllerGUIRender`), **client-side only**.

**Panel routing**
- `[panel:panel_id]text` — send to a specific panel; no prefix → `defaultPanelId`.

**Smart Interface editor**
- `[mmcege:si.hide_key]` / `[mmcege:si.show_key]` (= `si.hide_info` / `si.show_info`) — hide/show the bottom info line
- `[mmcege:si.hide_title]` / `[mmcege:si.show_title]` — hide/show the top title
- `[mmcege:si.title=Custom {index}/{count} {key}]` — set the title (placeholders `{index}`/`{count}`/`{key}`)
- `[mmcege:si.clear_title]` — restore default title

**Texture layer control** (`<id>` is the layer's id)
- `[mmcege:layer.<id>.x=300]`, `.y=186`, `.scale=1.25`, `.scaleX=1.25`, `.scaleY=0.90`, `.rotation=45`, `.alpha=0.5`, `.opacity=128`, `.transparency=0.25`, `.priority=30`, `.visible=true`, `.reset`, `.clear`
- `[mmcege:layer.reset_all]` / `[mmcege:layer.clear_all]` — reset all layer states

---

## 10. Notes

- The machine ID in the machine JSON and in ZS must match.
- Give every runtime-controllable layer an `id`.
- Separate multiple `virtualKey`s with commas; `showControls=false` hides the left/right and OK buttons for that input.
- Restart to verify JSON changes; ZS can be `/ct reload`ed.

## 9. Dynamic visuals

`dynamicVisuals[]` works in both Machine and Factory controller styles. It is the unified system for variable-driven visuals: `source` -> normalization -> optional visibility / transform / color overrides -> optional `history` -> `renderer`.

Renderers supported now: `textureSwitch`, `fill`, `pie`/`ring`, and `lineChart`. Sources can read controller `customData` / Smart Interface numeric values, or built-in machine metrics such as `recipeProgress`, `energyRatio`, `parallelism`, `threadCount`, plus factory thread-count metrics.

Optional transforms are also supported:
- `transform`: static `offsetX`, `offsetY`, `scale`, `scaleX`, `scaleY`, `rotation`, `alpha`, `pivotX`, `pivotY`, `pivotUnit`, and legacy `origin` (`topLeft`, `topCenter`, `topRight`, `centerLeft`, `center`, `centerRight`, `bottomLeft`, `bottomCenter`, `bottomRight`).
- `transformByValue`: variable-driven `offsetX`, `offsetY`, `scale`, `scaleX`, `scaleY`, `rotation`, `alpha`, `pivotX`, `pivotY`.
- Dynamic `pivotX` / `pivotY` use the static `transform.pivotUnit` for their units. If `pivotUnit` is omitted, it defaults to `ratio`; use `ratio` for 0..1 relative coordinates, or `px` for absolute pixel coordinates.
- each `transformByValue` channel may define its own independent `source`; otherwise it reuses the visual's main `source`.

Optional visibility / color overrides are also supported:
- `visibleByValue`: conditional visibility driven by the normalized value. Supports `min`, `max`, `equals`, `invert`, and an optional independent `source`.
- `rendererByValue`: variable-driven color interpolation for color-capable renderers. Supported channels: `backgroundColor`, `fillColor`, `borderColor`, `color`, `lineColor`, `gridColor`.
- each `rendererByValue` channel accepts `{ "fromColor": ..., "toColor": ... }` and may define its own independent `source`.
- if one color endpoint is omitted, the static renderer color is used as the fallback endpoint first; if that is also absent, the provided endpoint is reused.

```json
"dynamicVisuals": [
  {
    "id": "energy_ring",
    "x": 120,
    "y": 24,
    "width": 32,
    "height": 32,
    "source": { "type": "machine", "metric": "energyRatio", "min": 0, "max": 1 },
    "renderer": {
      "type": "pie",
      "mode": "ring",
      "startAngle": -90,
      "innerRadius": 10,
      "color": "FFFFAA00",
      "backgroundColor": "33000000"
    }
  }
]
```

Use `foreground`, `priority`, `page`, `visible`, `visibleByValue`, and `rendererByValue` exactly like other controller widgets.

Variable-driven rotation example:

```json
{
  "id": "fan",
  "x": 120,
  "y": 30,
  "width": 32,
  "height": 32,
  "source": { "type": "customData", "key": "speed", "default": 0, "min": 0, "max": 100 },
  "transform": { "pivotX": 0.5, "pivotY": 0.5, "pivotUnit": "ratio", "alpha": 0.6 },
  "transformByValue": {
    "rotation": { "min": 0, "max": 360 },
    "pivotX": { "min": 0.35, "max": 0.65 },
    "pivotY": { "min": 0.35, "max": 0.65 },
    "scale": { "min": 0.85, "max": 1.15 },
    "alpha": {
      "min": 0.4,
      "max": 1.0,
      "source": { "type": "customData", "key": "warning", "default": 0, "min": 0, "max": 1 }
    }
  },
  "renderer": {
    "type": "textureSwitch",
    "fallbackTexture": "pack:textures/gui/fan.png",
    "frames": [
      { "texture": "pack:textures/gui/fan.png" }
    ]
  }
}
```

In this example, the static `pivotUnit` is `ratio`, so the dynamic `pivotX` / `pivotY` values are also interpreted as relative coordinates.

Visibility + color example:

```json
{
  "id": "warning_ring",
  "x": 160,
  "y": 28,
  "width": 28,
  "height": 28,
  "source": { "type": "customData", "key": "warning", "default": 0, "min": 0, "max": 1 },
  "visibleByValue": { "min": 0.05 },
  "renderer": {
    "type": "pie",
    "mode": "ring",
    "innerRadius": 8,
    "backgroundColor": "22000000",
    "color": "FF44FF44"
  },
  "rendererByValue": {
    "color": {
      "fromColor": "FF44FF44",
      "toColor": "FFFF4444"
    }
  }
}
```

This example stays hidden near zero, then fades from green to red as `warning` rises.
