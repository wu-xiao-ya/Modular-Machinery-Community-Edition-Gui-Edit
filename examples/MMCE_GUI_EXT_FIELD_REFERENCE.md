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

## 5. Layer overlays / 图层叠加

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

## 6. Factory-only / 工厂专用

- `specialThreadBackgroundColor`
  - CN: 工厂核心线程背景色，格式可用 `RRGGBB` 或 `AARRGGBB`。
  - EN: Factory core/thread background color. Use `RRGGBB` or `AARRGGBB`.
- `queueVisibleRows`
  - CN: 左侧队列可见行数。
  - EN: Number of visible rows in the left queue.

## 7. Common patterns / 常见写法

- CN: 只改背景时，通常只写 `backgroundTexture` + `hideDefaultBackground`。
- EN: For background-only edits, usually only set `backgroundTexture` and `hideDefaultBackground`.
- CN: 只改编辑器时，保留默认背景，重点调 `smartInterfaceEditors`。
- EN: For editor-only edits, keep the default background and focus on `smartInterfaceEditors`.
- CN: 只改叠层时，优先用 `backgroundLayers` / `foregroundLayers`。
- EN: For overlay-only edits, use `backgroundLayers` / `foregroundLayers`.

## 8. Fake progress bar example / 假进度条示例

CN: 下面这个例子用一个蓝色方块贴图拉成长条，看起来像进度条。
EN: The example below stretches a blue square texture into a long bar, which can look like a progress bar.

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
