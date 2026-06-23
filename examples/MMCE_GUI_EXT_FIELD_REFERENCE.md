# MMCE GUI Ext Field Reference

This file is a quick reference for pack authors.
这份文件给整合包作者做速查表。

## 1. Controller size / 控制器尺寸

- `guiWidth`
  - CN: GUI 宽度，改大后右侧会有更多空间。
  - EN: GUI width. Increasing it gives more room on the right.
- `guiHeight`
  - CN: GUI 高度，影响整个界面纵向尺寸。
  - EN: GUI height. Controls the vertical size of the whole screen.
- `enableRightExtension`
  - CN: 是否启用右侧扩展区域。
  - EN: Whether the right-side extension area is enabled.
- `centerFullGui`
  - CN: 机器 JSON/styleFile 字段。是否按完整视觉左右边界水平居中 GUI。适合背景贴图使用 `backgroundTextureOffsetX` 向左扩展时避免整体偏右。
  - EN: Machine JSON/styleFile field. Whether to horizontally center the GUI by its full visual left/right bounds. Useful when `backgroundTextureOffsetX` extends the background left.

## 2. Background / 背景

- `backgroundTexture`
  - CN: 背景贴图路径。留空则走默认底图。
  - EN: Background texture path. Empty means use the default base texture.
- `backgroundTextureOffsetX`
  - CN: 背景贴图 X 偏移。正数会让贴图向左扩。
  - EN: Background texture X offset. Positive values extend the texture to the left.
- `backgroundTextureOffsetY`
  - CN: 背景贴图 Y 偏移。正数会让贴图向上扩。
  - EN: Background texture Y offset. Positive values extend the texture upward.
- `hideDefaultBackground`
  - CN: 是否隐藏 MMCE 默认底图。
  - EN: Whether to hide the MMCE default background.
- `useNineSlice`
  - CN: 是否使用 9 宫格缩放。
  - EN: Whether to use 9-slice scaling.
- `backgroundTextureWidth`
  - CN: 9 宫格源贴图宽度。
  - EN: Source texture width for 9-slice rendering.
- `backgroundTextureHeight`
  - CN: 9 宫格源贴图高度。
  - EN: Source texture height for 9-slice rendering.
- `backgroundCorner`
  - CN: 9 宫格角大小。
  - EN: Corner size for 9-slice rendering.

## 3. Panels / 信息区分区

- `panelX`
  - CN: 默认信息区 X 坐标。
  - EN: Default info panel X position.
- `panelY`
  - CN: 默认信息区 Y 坐标。
  - EN: Default info panel Y position.
- `panelWidth`
  - CN: 默认信息区宽度，0 表示自动。
  - EN: Default info panel width. `0` means auto.
- `panelHeight`
  - CN: 默认信息区高度。
  - EN: Default info panel height.
- `defaultPanelId`
  - CN: 没写 `[panel:id]` 时默认落到哪个面板。
  - EN: Which panel to use when no `[panel:id]` prefix is present.
- `customPanels`
  - CN: 自定义面板列表。格式：`id,x,y,width,height`
  - EN: Custom panel list. Format: `id,x,y,width,height`

## 4. Smart Interface editor / Smart Interface 编辑器

- `enableSmartInterfaceEditor`
  - CN: 是否启用默认 Smart Interface 编辑器。
  - EN: Whether to enable the default Smart Interface editor.
- `hideDefaultSmartInterfaceEditor`
  - CN: 是否隐藏默认编辑器，只显示自定义条目。
  - EN: Whether to hide the default editor and show only custom entries.
- `smartInterfaceEditorX`
  - CN: 默认编辑器 X 坐标，`-1` 表示自动摆放。
  - EN: Default editor X position. `-1` means auto placement.
- `smartInterfaceEditorY`
  - CN: 默认编辑器 Y 坐标，`-1` 表示自动摆放。
  - EN: Default editor Y position. `-1` means auto placement.
- `smartInterfaceEditorInputWidth`
  - CN: 默认编辑器输入框宽度。
  - EN: Default editor input width.
- `smartInterfaceEditorVirtualKey`
  - CN: 没绑定 DataPort 时使用的虚拟 key。
  - EN: Virtual key used when no DataPort is bound.
- `smartInterfaceEditors`
  - CN: 自定义编辑器条目数组，每个对象一个条目。
  - EN: Array of custom editor entries. One object per slot.

### `smartInterfaceEditors` fields / 条目字段

- `id`
  - CN: 条目 ID，用来区分多个条目。
  - EN: Entry ID used to distinguish multiple entries.
- `x`
  - CN: 条目 X 坐标。
  - EN: Entry X position.
- `y`
  - CN: 条目 Y 坐标。
  - EN: Entry Y position.
- `inputWidth`
  - CN: 输入框宽度。
  - EN: Input box width.
- `virtualKey`
  - CN: 绑定的虚拟 key。
  - EN: Bound virtual key.
- `title`
  - CN: 显示标题。
  - EN: Display title.
- `showTitle`
  - CN: 是否显示标题。
  - EN: Whether to show the title.
- `showInfo`
  - CN: 是否显示说明信息。
  - EN: Whether to show info text.
- `showControls`
  - CN: 是否显示左右切换按钮。
  - EN: Whether to show previous/next buttons.
- `inputBackground`
  - CN: 是否显示输入框背景。
  - EN: Whether to show the input background.
- `priority`
  - CN: 渲染优先级，越大越后画。
  - EN: Render priority. Higher values draw later.

## 5. Controller pages and buttons / 控制器分页与按钮

- Sub GUI overlay file / 子 GUI 独立文件
  - CN: 主机器 JSON 仍放在 `config/modularmachinery/machinery`。子 GUI overlay JSON 放在 `config/mmceguiext/subgui`，不要放到 `modularmachinery/machinery` 子目录，避免被 MM 机器加载器当成机器定义扫描。
  - EN: Main machine JSON still lives in `config/modularmachinery/machinery`. Sub GUI overlay JSON lives in `config/mmceguiext/subgui`; do not put it under `modularmachinery/machinery`, or the MM machine loader may scan it as a machine definition.
  - CN: 子 GUI 文件使用同一个 `registryname`，内容仍写在 `mmce_gui_ext.machineController` 或 `mmce_gui_ext.factoryController` 中，会追加合并到主 GUI。
  - EN: A sub GUI file uses the same `registryname`; its contents still go under `mmce_gui_ext.machineController` or `mmce_gui_ext.factoryController` and are appended onto the main GUI style.
- `subGuis`
  - CN: 控制器下的子 GUI 数组。每一项都是一个可打开的子界面，子项可直接写与主 controller 同级的字段，也可以包在 `style` / `gui` / `controller` 里。
  - EN: Array of sub GUI definitions under the controller. Each entry is an openable sub screen. You may write controller-level fields directly on the entry, or wrap them in `style` / `gui` / `controller`.
- `defaultPageId`
  - CN: GUI 打开时默认显示的页。未填写时通常使用 `main`。
  - EN: Page shown when the GUI opens. Usually `main` when omitted.
- `page`
  - CN: 可写在 `customPanels` 第 6 段、`texts`、`smartInterfaceEditors`、图层和按钮上，用来限制它们只在某一页显示。`state` / `guiState` 可作为别名。
  - EN: Can be used on the 6th segment of `customPanels`, `texts`, `smartInterfaceEditors`, layers, and buttons to show them only on one page. `state` / `guiState` are accepted aliases.
- `buttons`
  - CN: 自定义控制器按钮数组，每个对象一个按钮。
  - EN: Custom controller button array. One object per button.

### `subGuis` fields / 子 GUI 条目字段

- `id`
  - CN: 子 GUI ID，按钮用 `targetSubGui` 指向它。
  - EN: Sub GUI ID. Buttons point to it with `targetSubGui`.
- `mode` / `openMode`
  - CN: 打开模式，支持 `modal` 和 `replace`。`displayMode` 等别名也会被兼容。
  - EN: Open mode. Supports `modal` and `replace`. Aliases such as `displayMode` are also accepted.
- `x`, `y`
  - CN: 子 GUI 左上角坐标。
  - EN: Sub GUI top-left position.
- `guiWidth`, `guiHeight`
  - CN: 子 GUI 尺寸。也兼容 `width` / `height`。
  - EN: Sub GUI size. `width` / `height` are also accepted.
- `draggable`
  - CN: 仅对 `modal` 浮窗子 GUI 生效；设为 `true` 后允许拖动。
  - EN: Modal sub GUI only. Set `true` to allow dragging.
- `dragHandle`
  - CN: 可选。`false` 表示整个浮窗可拖；省略或 `true` 表示只允许拖动拖拽区域。
  - EN: Optional. `false` makes the whole modal draggable; omitted or `true` uses a drag handle.
- `dragX`, `dragY`, `dragWidth`, `dragHeight`
  - CN: 可选拖拽区域，坐标相对子 GUI 左上角。未设置时默认顶部 16 像素。
  - EN: Optional drag handle rectangle relative to the sub GUI top-left. Defaults to the top 16 pixels.
- `backgroundTexture`
  - CN: 子 GUI 背景贴图。
  - EN: Sub GUI background texture.
- `hideDefaultBackground`
  - CN: 是否隐藏默认底图。
  - EN: Whether to hide the default background.
- `buttons`
  - CN: 子 GUI 内按钮，字段与主 controller 相同。
  - EN: Buttons inside the sub GUI. Same fields as the main controller.
- `texts`
  - CN: 子 GUI 文本，字段与主 controller 相同。
  - EN: Texts inside the sub GUI. Same fields as the main controller.
- `customPanels`
  - CN: 子 GUI 自定义信息区，格式仍为 `id,x,y,width,height`。
  - EN: Sub GUI custom panels. Format is still `id,x,y,width,height`.
- `infoSections`
  - CN: 子 GUI 信息区条目，规则与主 controller 相同。
  - EN: Sub GUI info sections. Same rules as the main controller.
- `smartInterfaceEditors`
  - CN: 子 GUI Smart Interface 编辑器条目，规则与主 controller 相同。
  - EN: Sub GUI Smart Interface editor entries. Same rules as the main controller.
- `textureLayers`
  - CN: 子 GUI 纹理图层，规则与主 controller 相同。
  - EN: Sub GUI texture layers. Same rules as the main controller.

### `buttons` fields / 按钮字段

- `x`, `y`
  - CN: 按钮左上角坐标，必填。
  - EN: Button top-left position. Required.
- `width`, `height`
  - CN: 按钮尺寸。可省略，使用默认尺寸。
  - EN: Button size. Optional; defaults are used when omitted.
- `label`
  - CN: 按钮显示文字，必填。
  - EN: Text shown on the button. Required.
- `action`
  - CN: 按钮行为。支持 `page`、`subgui`、`close_subgui`、`smart_add`、`smart_set`、`event`。`switch_state` / `set_state` 会兼容为 `page`；`data_port_add` / `data_port_set` 会兼容为数值按钮；`open_subgui` / `close_sub_gui` 等别名也会被兼容。
  - EN: Button action. Supports `page`, `subgui`, `close_subgui`, `smart_add`, `smart_set`, and `event`. `switch_state` / `set_state` alias to `page`; `data_port_add` / `data_port_set` alias to numeric data-port buttons; aliases such as `open_subgui` / `close_sub_gui` are also accepted.
- `targetPage`
  - CN: `page` 按钮跳转到的页。只写 `targetPage` 且不写 `action` 时，会自动按 `page` 按钮处理。`targetState` / `targetGuiState` 可作为别名。
  - EN: Destination page for `page` buttons. If `targetPage` is set and `action` is omitted, the button is treated as a `page` button. `targetState` / `targetGuiState` are accepted aliases.
- `targetSubGui`
  - CN: `subgui` 按钮要打开的子 GUI ID。只写 `targetSubGui` 且不写 `action` 时，会自动按 `subgui` 按钮处理。
  - EN: Sub GUI ID to open for `subgui` buttons. If `targetSubGui` is set and `action` is omitted, the button is treated as a `subgui` button.
- `openMode`
  - CN: `subgui` 按钮打开方式，支持 `modal` 和 `replace`。未填时优先用目标子 GUI 自己的 `mode`。
  - EN: Open mode for `subgui` buttons. Supports `modal` and `replace`. When omitted, the target sub GUI's own `mode` is used first.
- `key`
  - CN: `smart_add` / `smart_set` 操作的 Smart Interface / DataPort 虚拟 key。`dataPortKey` / `portKey` 可作为别名。
  - EN: Smart Interface / DataPort virtual key used by `smart_add` / `smart_set`. `dataPortKey` / `portKey` are accepted aliases.
- `value`
  - CN: `smart_add` 时为增量，省略时默认为 `1.0`；`smart_set` 时可填数值或字符串。数值会写入 Smart Interface 数值通道，字符串会写入控制器 `customData[key]` 作为兜底读取值。
  - EN: Delta for `smart_add`, defaulting to `1.0` when omitted; `smart_set` accepts either numbers or strings. Numbers go through the Smart Interface numeric path, while strings are written to controller `customData[key]` for fallback reads.
- `min`, `max`
  - CN: `smart_add` / `smart_set` 后限制结果范围。
  - EN: Clamp the result after `smart_add` / `smart_set`.
- `buttonId`
  - CN: `event` 按钮发送到脚本事件的 ID，必填。也可用 `id`、`eventId` 等别名。
  - EN: ID sent to the script event for `event` buttons. Required. Aliases such as `id` and `eventId` are accepted.
- `id`
  - CN: 按钮配置 ID；`event` 按钮未写 `buttonId` 时也会作为事件 ID。
  - EN: Button config ID; also used as the event ID when an `event` button omits `buttonId`.
- `visible`
  - CN: 是否显示按钮。
  - EN: Whether the button is visible.
- `priority`
  - CN: 按钮排序优先级，越大越后处理。
  - EN: Button ordering priority. Higher values are handled later.

```json
{
  "mmce_gui_ext": {
    "machineController": {
      "defaultPageId": "main",
      "texts": [
        { "x": 186, "y": 14, "value": "Main Page", "page": "main" },
        { "x": 186, "y": 14, "value": "Settings", "page": "settings" }
      ],
      "buttons": [
        { "x": 186, "y": 96, "label": "Settings", "action": "page", "targetPage": "settings", "page": "main" },
        { "x": 238, "y": 96, "label": "+1", "action": "smart_add", "key": "pulse_counter", "value": 1.0, "page": "main" },
        { "x": 186, "y": 96, "label": "Reset", "action": "smart_set", "key": "pulse_counter", "value": 0.0, "page": "settings" },
        { "x": 238, "y": 96, "label": "Click", "action": "event", "buttonId": "custom_click", "page": "settings" }
      ]
    }
  }
}
```

## 6. Controller sliders / 控制器滑块

- `sliders`
  - CN: 控制器滑块数组，每个对象一个可拖拽数值控件。
  - EN: Controller slider array. One object per draggable numeric control.
- `guiSliders` / `gui_sliders` / `rangeControls` / `range_controls`
  - CN: `sliders` 的别名。
  - EN: Aliases for `sliders`.

### `sliders` fields / 滑块字段

- `id`
  - CN: 滑块 ID。可省略，省略时自动生成。
  - EN: Slider ID. Optional; generated when omitted.
- `x`, `y`, `width`, `height`
  - CN: 滑块矩形区域，必填。
  - EN: Slider rectangle. Required.
- `key`
  - CN: 写入的 Smart Interface / 虚拟 DataPort key，必填。`virtualKey` / `dataPortKey` / `portKey` 可作为别名。
  - EN: Smart Interface / virtual DataPort key to write. Required. `virtualKey` / `dataPortKey` / `portKey` aliases are accepted.
- `min`, `max`
  - CN: 数值范围。默认 `0` 到 `1`。
  - EN: Value range. Defaults to `0` to `1`.
- `step`
  - CN: 步进。大于 `0` 时会把拖拽结果吸附到步进值。
  - EN: Step size. When greater than `0`, dragged values snap to this interval.
- `value`
  - CN: 初始值。若控制器已有同名 Smart Interface / `customData` 值，会优先读取现有值。
  - EN: Initial value. Existing Smart Interface / `customData` values for the same key are read first.
- `direction`
  - CN: `horizontal` 或 `vertical`。`axis` / `orientation` 可作为别名。
  - EN: `horizontal` or `vertical`. `axis` / `orientation` aliases are accepted.
- `trackColor`, `fillColor`, `thumbColor`, `borderColor`
  - CN: 轨道、填充、滑块手柄、边框颜色。支持 `RRGGBB` / `AARRGGBB`。
  - EN: Track, fill, thumb, and border colors. `RRGGBB` / `AARRGGBB` are supported.
- `thumbWidth`, `thumbHeight`
  - CN: 滑块手柄尺寸。`handleWidth` / `handleHeight` 可作为别名。
  - EN: Thumb size. `handleWidth` / `handleHeight` aliases are accepted.
- `foreground`
  - CN: 是否作为前景控件绘制。默认 `true`。
  - EN: Whether to render as a foreground control. Defaults to `true`.
- `priority`
  - CN: 渲染和点击优先级，越大越靠后绘制、越优先命中。
  - EN: Render and hit-test priority. Higher values draw later and are hit first.
- `visible`
  - CN: 是否显示并可交互。
  - EN: Whether the slider is visible and interactive.
- `page`
  - CN: 限制滑块只在指定页显示。
  - EN: Restrict the slider to a specific page.
- `showText`, `textColor`
  - CN: 是否显示当前数值，以及数值文本颜色。
  - EN: Whether to draw the current value and the value text color.

```json
{
  "mmce_gui_ext": {
    "machineController": {
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
    }
  }
}
```

## 7. Layer overlays / 图层叠加

- `textureLayers`
  - CN: 通用图层数组，可做背景或前景。
  - EN: General overlay list. Can be background or foreground.
- `backgroundLayers`
  - CN: 背景图层数组，自动按背景层画。
  - EN: Background layer list. Always drawn as background.
- `foregroundLayers`
  - CN: 前景图层数组，自动按前景层画。
  - EN: Foreground layer list. Always drawn as foreground.

### `textureLayers` / layer fields / 图层字段

- `id`
  - CN: 图层 ID。
  - EN: Layer ID.
- `texture`
  - CN: 图层贴图路径。
  - EN: Layer texture path.
- `offsetX`
  - CN: 绘制偏移 X。
  - EN: Draw offset X.
- `offsetY`
  - CN: 绘制偏移 Y。
  - EN: Draw offset Y.
- `width`
  - CN: 实际绘制宽度。
  - EN: Draw width.
- `height`
  - CN: 实际绘制高度。
  - EN: Draw height.
- `textureWidth`
  - CN: 源贴图宽度。
  - EN: Source texture width.
- `textureHeight`
  - CN: 源贴图高度。
  - EN: Source texture height.
- `alpha` / `opacity` / `transparency`
  - CN: 图层整体透明度。支持 `0.0` 到 `1.0`，也支持 `0` 到 `255`。
  - EN: Whole-layer alpha. Accepts `0.0` to `1.0`, or `0` to `255`.
- `corner`
  - CN: 9 宫格角大小。
  - EN: 9-slice corner size.
- `useNineSlice`
  - CN: 是否使用 9 宫格。
  - EN: Whether to use 9-slice scaling.
- `foreground`
  - CN: 是否强制作为前景层。
  - EN: Force this layer to render in foreground.
- `priority`
  - CN: 图层优先级，越大越晚画。
  - EN: Layer priority. Higher values draw later.

## 8. Factory-only / 工厂专用

- `specialThreadBackgroundColor`
  - CN: 工厂核心线程背景色，格式可用 `RRGGBB` 或 `AARRGGBB`。
  - EN: Factory core/thread background color. Use `RRGGBB` or `AARRGGBB`.
- `threadQueueX` / `threadQueueY`
  - CN: 工厂线程队列左上角坐标。可用 `queueX` / `queueY` 作为别名。
  - EN: Top-left position of the factory thread queue. `queueX` / `queueY` aliases are accepted.
- `threadScrollbarX` / `threadScrollbarY`
  - CN: 工厂线程队列滚动条左上角坐标。旧扁平字段仍可用；新 JSON 推荐使用下面的 `threadScrollbar` 对象。
  - EN: Top-left position of the factory thread queue scrollbar. Legacy flat fields remain supported; new JSON styles should use the `threadScrollbar` object below.
- `threadScrollbar`
  - CN: 集成控制器线程队列滚动条结构化配置。只写这个对象也会触发集成控制器自代理 GUI。
  - EN: Structured scrollbar config for the factory thread queue. This object alone can trigger the integrated-controller self proxy.
  - Fields: `x`, `y`, `width`, `height`, `trackTexture`, `thumbTexture`, `trackColor`, `thumbColor`, `textureWidth`, `textureHeight`, `thumbTextureWidth`, `thumbTextureHeight`, `thumbMinHeight`, `visible`.
  - CN: `trackTexture` / `thumbTexture` 为空时分别回退到 `trackColor` / `thumbColor`；`height` 不写或全局 cfg 设 `-1` 时按可见行数自动计算。
  - EN: Empty `trackTexture` / `thumbTexture` fall back to `trackColor` / `thumbColor`; missing `height` or global cfg `-1` uses automatic height from visible rows.
- `threadVisibleRows`
  - CN: 工厂线程队列可见行数，优先级高于全局 `queueVisibleRows`。
  - EN: Visible rows in the factory thread queue. Overrides global `queueVisibleRows`.
- `threadRowWidth` / `threadRowHeight`
  - CN: 工厂线程队列每行宽度与高度，用于绘制、点击与滚动范围计算。
  - EN: Per-row width and height for the factory thread queue, used for rendering, hit testing, and scroll math.
- `queueVisibleRows`
  - CN: 左侧队列可见行数。
  - EN: Number of visible rows in the left queue.

### Factory thread queue copy-paste example / 工厂线程队列可直接照抄示例

```json
{
  "mmce_gui_ext": {
    "factoryController": {
      "threadQueueX": 12,
      "threadQueueY": 14,
      "threadVisibleRows": 7,
      "threadRowWidth": 90,
      "threadRowHeight": 34,
      "threadScrollbar": {
        "x": 98,
        "y": 22,
        "width": 8,
        "height": 197,
        "trackTexture": "yourmod:textures/gui/scroll_track.png",
        "thumbTexture": "yourmod:textures/gui/scroll_thumb.png",
        "trackColor": "66000000",
        "thumbColor": "FFFFFFFF",
        "textureWidth": 8,
        "textureHeight": 197,
        "thumbTextureWidth": 8,
        "thumbTextureHeight": 16,
        "thumbMinHeight": 15,
        "visible": true
      }
    }
  }
}
```

## 9. AE mixed bus capacity cards / AE 混合总线容量卡

- `capacityCardSlots`
  - CN: 旧式容量卡槽坐标数组，元素为 `{ "x": 116, "y": 18 }`。
  - EN: Legacy capacity-card slot coordinate array, each entry like `{ "x": 116, "y": 18 }`.
- `gui.components[].role = "capacity_card"`
  - CN: 新式 GUI 容量卡槽。`type` 必须为 `slot`，`index` 可省略自动递增。
  - EN: New-style GUI capacity-card slot. `type` must be `slot`; `index` can be omitted for auto-increment.
- `config/mmceguiext/capacity_cards/*.json`
  - CN: 自定义容量卡注册目录。支持 `item`、`meta`/`damage`、`nbt`、`matchNbt`、`multiplier`、`flatFluid`、`flatGas`。
  - EN: Custom capacity-card registry directory. Supports `item`, `meta`/`damage`, `nbt`, `matchNbt`, `multiplier`, `flatFluid`, and `flatGas`.

```json
{
  "gui": {
    "components": [
      { "type": "slot", "role": "capacity_card", "index": 0, "x": 116, "y": 18 }
    ]
  }
}
```

```json
{
  "item": "appliedenergistics2:material",
  "meta": 35,
  "multiplier": 1.5,
  "flatFluid": 1000,
  "flatGas": 1000
}
```

- CN: 这组字段可以直接拷到机器 JSON 里。`queueX` / `queueY` / `queueVisibleRows` / `queueRowWidth` / `queueRowHeight` 这些别名也能用；滚动条外观请使用 `threadScrollbar`。
- EN: You can copy this block directly into a machine JSON. Aliases such as `queueX` / `queueY` / `queueVisibleRows` / `queueRowWidth` / `queueRowHeight` are also accepted; scrollbar visuals should use `threadScrollbar`.

## 10. Common patterns / 常见写法

- CN: 只改背景时，通常只写 `backgroundTexture` + `hideDefaultBackground`。
- EN: For background-only edits, usually only set `backgroundTexture` and `hideDefaultBackground`.
- CN: 只改编辑器时，保留默认背景，重点调 `smartInterfaceEditors`。
- EN: For editor-only edits, keep the default background and focus on `smartInterfaceEditors`.
- CN: 只改叠层时，优先用 `backgroundLayers` / `foregroundLayers`。
- EN: For overlay-only edits, use `backgroundLayers` / `foregroundLayers`.

## 11. Fake progress bar example / 假进度条示例

CN: 下面这个例子用一个蓝色方块贴图拉成长条，看起来像进度条。
EN: The example below stretches a blue square texture into a long bar, which can look like a progress bar.

## 12. Custom hatch block texture levels / 自定义仓室方块贴图分级

- `block.texture`
  - ZH: 自定义仓室方块默认贴图。未命中任何分级时使用它。
  - EN: Default block texture used when no texture level matches.
- `block.textureLevels`
  - ZH: 根据当前仓量比例切换方块贴图或模型的列表。旧配置可省略。
  - EN: Optional list that switches the hatch block texture or model by current fill ratio.
- `outputSlotLock`
  - ZH: 是否锁定流体/气体自动处理的输出槽模板，默认 `true`。开启后，输出槽第一次产出某种容器后，即使被取空，也只会继续接收同类容器，避免多输出槽串位。
  - EN: Locks output slot templates for automatic fluid/gas container handling. Default: `true`.

### `block.textureLevels[]` fields / 分级字段

- `content`
  - ZH: 监视的仓类型。可填 `fluid`、`gas`、`energy`；当前重点支持流体仓。
  - EN: Storage type to monitor. Supported values are `fluid`, `gas`, and `energy`; fluid is the primary use case now.
- `minFillRatio`
  - ZH: 生效下限，范围 `0.0` 到 `1.0`。例如 `0.75` 表示存量达到 75% 后启用。
  - EN: Inclusive lower bound in the `0.0` to `1.0` range. Example: `0.75` means active from 75 percent filled.
- `texture`
  - ZH: 命中该分级后覆盖使用的方块贴图。
  - EN: Replacement block texture used when this level matches.
- `model`
  - ZH: 命中该分级后覆盖使用的方块模型；可与 `texture` 二选一，也可同时提供。
  - EN: Replacement block model used when this level matches; optional and can be combined with `texture`.

```json
{
  "id": "demo:fluid_meter_hatch",
  "componentType": "fluid",
  "fluidCapacity": 64000,
  "block": {
    "texture": "demo:textures/blocks/hatch_empty.png",
    "textureLevels": [
      { "content": "fluid", "minFillRatio": 0.25, "texture": "demo:textures/blocks/hatch_q1.png" },
      { "content": "fluid", "minFillRatio": 0.50, "texture": "demo:textures/blocks/hatch_q2.png" },
      { "content": "fluid", "minFillRatio": 0.75, "texture": "demo:textures/blocks/hatch_q3.png" },
      { "content": "fluid", "minFillRatio": 1.00, "texture": "demo:textures/blocks/hatch_full.png" }
    ]
  }
}
```

```json
{
  "mmce_gui_ext": {
    "machineController": {
      // 这张贴图可以是一个 16x16 的蓝色方块。
      // This texture can be a simple 16x16 blue square.
      "foregroundLayers": [
        {
          // 图层 ID，方便后续调试。
          // Layer ID for debugging.
          "id": "progress_bar_fake",
          // 蓝色方块贴图路径，换成你自己的资源也可以。
          // Blue square texture path. Replace with your own asset if needed.
          "texture": "yourmod:textures/gui/blue_square.png",
          // 从贴图左上角开始取图。
          // Sample from the top-left of the texture.
          "offsetX": 0,
          "offsetY": 0,
          // 实际绘制宽高。这里拉成长条，看起来像进度条。
          // Draw width and height. Stretch it into a long bar to fake a progress bar.
          "width": 120,
          "height": 8,
          // 源贴图宽高。方块图一般就是 16x16。
          // Source texture size. A square texture is usually 16x16.
          "textureWidth": 16,
          "textureHeight": 16,
          // 越大的优先级越晚绘制，方便盖在其他内容上。
          // Higher priority draws later, so it can sit on top of other content.
          "priority": 30
        }
      ]
    }
  }
}
```

CN: 常见改法：
- `width` 控制条的长度
- `height` 控制条的厚度
- `priority` 控制它盖在什么上面

EN: Common tweaks:
- `width` controls the bar length
- `height` controls the bar thickness
- `priority` controls what it draws over

## 10. Dynamic visuals / 动态可视化组件

- `dynamicVisuals`
  - CN: 控制器动态可视化数组，普通控制器和集成控制器都支持。统一支持贴图切换、动态填充、圆饼/环形图、曲线图。
  - EN: Dynamic visual component array for both Machine and Factory controllers. Supports texture switching, fills, pie/ring charts and line charts.
- aliases / 别名: `dynamic_visuals`, `visuals`, `dynamicWidgets`, `dynamic_widgets`.

### `dynamicVisuals` fields / 条目字段

- `id`: optional stable id, also used by chart history.
- `x`, `y`, `width`, `height`: required rectangle.
- `priority`: foreground render priority. Defaults to normal controller content priority.
- `foreground`: `true`/omitted = foreground; `false` = background.
- `page`, `visible`: same page/visibility rules as other controller widgets.
- `source`: value source object.
- `history`: optional sampling config for charts.
- `renderer`: renderer object.

### `source` fields / 数据源字段

```json
"source": { "type": "customData", "key": "heat", "default": 0, "min": 0, "max": 100, "clamp": true, "invert": false }
```

```json
"source": { "type": "machine", "metric": "recipeProgress", "default": 0, "min": 0, "max": 1 }
```

Metrics: `recipeProgress`, `recipeMaxProgress`, `energyStored`, `energyCapacity`, `energyRatio`, `parallelism`, `threadCount`, `activeThreadCount`, `idleThreadCount`; factory additionally accepts `factoryThreadCount`, `factoryActiveThreadCount`, `factoryIdleThreadCount`.

### renderer: `textureSwitch`

```json
"renderer": {
  "type": "textureSwitch",
  "fallbackTexture": "pack:textures/gui/unknown.png",
  "frames": [
    { "max": 30, "texture": "pack:textures/gui/low.png" },
    { "max": 70, "texture": "pack:textures/gui/mid.png" },
    { "texture": "pack:textures/gui/high.png" }
  ]
}
```

### renderer: `fill`

```json
"renderer": {
  "type": "fill",
  "backgroundTexture": "pack:textures/gui/tank_empty.png",
  "fillTexture": "pack:textures/gui/tank_full.png",
  "direction": "up"
}
```

`direction`: `right`, `left`, `up`, `down`.

### renderer: `pie`

```json
"renderer": {
  "type": "pie",
  "mode": "ring",
  "startAngle": -90,
  "innerRadius": 10,
  "color": "FFFFAA00",
  "backgroundColor": "33000000",
  "segments": 64
}
```

### renderer: `lineChart`

```json
"history": { "enabled": true, "samples": 60, "intervalTicks": 5 },
"renderer": {
  "type": "lineChart",
  "lineColor": "FF55CCFF",
  "fillColor": "3355CCFF",
  "gridColor": "22000000",
  "lineWidth": 1,
  "showGrid": true
}
```
