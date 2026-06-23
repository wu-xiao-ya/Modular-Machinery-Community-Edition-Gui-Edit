# 控制器 GUI

[首页](Home) · [English](Controller-GUI-EN)

替换/自定义 MMCE 普通控制器与集成（工厂）控制器的 GUI：可调整大小、自定义背景贴图、多信息区、纹理图层、Smart Interface 编辑器、自定义按钮、滑块与页面。

## 工作机制

MMCEGE 挂接 Forge 的 `GuiOpenEvent`，在 MMCE 打开原版 `GuiMachineController` / `GuiFactoryController` 的瞬间，**仅当**存在自定义贴图、隐藏默认背景或机器级样式覆盖时，才替换为可调整大小的版本。它**不修改** MMCE 的任何 GUI 类。

样式来自两层，机器级优先、回退到全局：
- **全局**：`config/mmceguiext/client.cfg`
- **机器级**：`config/modularmachinery/machinery/*.json` 里的 `mmce_gui_ext` 节点

> 字段超多，完整速查见仓库 `examples/MMCE_GUI_EXT_FIELD_REFERENCE.md`；可运行示例见 `examples/quick-start/` 与 `examples/game-ready-1.0.1/`。

---

## 1. 全局配置 `client.cfg`

`machineController.*` 与 `factoryController.*` 各有一组同名键：

- 背景：`backgroundTexture`、`backgroundTextureWidth`、`backgroundTextureHeight`、`useNineSlice`、`backgroundCorner`、`hideDefaultBackground`
- 尺寸：`disableRightExtension`（强制为 MMCE 基础宽度，关闭右扩展）
- 面板：`defaultPanelId`、`customPanels`（格式 `id,x,y,width,height`）
- Smart Interface 编辑器：`enableSmartInterfaceEditor`、`smartInterfaceEditorX`/`Y`（`-1`=自动右下）、`smartInterfaceEditorInputWidth`、`smartInterfaceEditorVirtualKey`（无绑定 DataPort 时写入控制器 `customData[key]`，多个 key 用 `,` 或 `;` 分隔）
- 仅工厂：`factoryController.specialThreadBackgroundColor`（十六进制 `RRGGBB` 或 `AARRGGBB`，核心/特殊线程行着色）

全局开关 `enabled`、滚轮步长 `wheelStep` 等也在此文件。可参考 `examples/game-ready-1.0.1/client.cfg.sample`。

**背景规则**：有自定义贴图 → 用自定义贴图；没有 → 用 MMCE 默认贴图；没有且 `hideDefaultBackground=true` → 不画背景。使用 MMCE 默认贴图时信息文本仍在原信息区渲染；多信息区仅在自定义贴图模式下启用。

---

## 2. 机器级覆盖 `mmce_gui_ext`

写进每台机器 JSON：

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

别名：
- 根节点：`mmceGuiExt` / `mmce_gui_ext` / `mmce-gui-ext`
- 控制器节点：`machineController`/`machine_controller`/`machine`，`factoryController`/`factory_controller`/`factory`
- 尺寸：`guiWidth`/`gui_width`/`width`，`guiHeight`/`gui_height`/`height`

机器级值覆盖全局。自 `1.0.1+`，只要机器定义了 `mmce_gui_ext` 节点，未填的 GUI 尺寸优先回退到 MMCE 基础尺寸（`176x213` / `280x213`），降低对全局 cfg 的耦合。

机器级常用字段（详见速查表）：`backgroundTexture`、`backgroundTextureOffsetX/Y`、`hideDefaultBackground`、`guiWidth`、`guiHeight`、`enableRightExtension`、`useNineSlice`、`backgroundTextureWidth/Height`、`backgroundCorner`、`centerFullGui`、`specialThreadBackgroundColor`、`enableSmartInterfaceEditor`、`smartInterfaceEditorX/Y`、`smartInterfaceEditorInputWidth`、`smartInterfaceEditorVirtualKey`、`smartInterfaceEditorPriority`、`foregroundContentPriority`、`hideDefaultSmartInterfaceEditor`、`defaultPanelId`、`customPanels`、`smartInterfaceEditors`、`sliders`、`textureLayers`/`backgroundLayers`/`foregroundLayers`、`buttons`。
工厂线程队列布局字段是 `threadQueueX`、`threadQueueY`、`threadVisibleRows`、`threadRowWidth`、`threadRowHeight`。滚动条推荐使用结构化对象 `threadScrollbar`，可配置位置、尺寸、轨道/滑块贴图、颜色、贴图源尺寸、最小滑块高度和显隐；旧的 `threadScrollbarX/Y` 仍兼容。自定义这些字段也会触发集成控制器自代理。

---

## 3. 尺寸与贴图

- 普通控制器关闭右扩展：`176x213`；集成关闭右扩展：`280x213`。
- 启用右扩展：使用运行时 `guiWidth x guiHeight`（最小同上）。`guiWidth/Height` 可全局或机器级设置，机器级优先。
- 9-slice（`useNineSlice=true`）：源贴图可保持基础尺寸并无损拉伸。直接模式（false）：用目标精确尺寸避免拉伸。

**左上装饰偏移**（贴图主框不在 0,0 时对齐）：`backgroundTextureOffsetX/Y`（别名 `offsetX`/`textureOriginX` 等）、`backgroundTextureWidth/Height`、`useNineSlice`、`backgroundCorner`、`centerFullGui`（按完整视觉边界水平居中，背景向左扩展时有用）。

---

## 4. 信息区（Panel）

- `customPanels` 每项：`id,x,y,width,height`，如 `"main,182,10,148,96"`。
- 每个面板独立滚动；多信息区仅在自定义贴图模式下启用。
- 文本通过 ZS 的 `[panel:id]` 前缀路由到指定面板；无前缀进入 `defaultPanelId`。

---

## 5. 纹理图层（textureLayers）

`textureLayers` / `backgroundLayers`（自动背景层）/ `foregroundLayers`（自动前景层）。每层字段：
`id`（建议必写，运行时指令靠它定位）、`texture`、`offsetX`、`offsetY`、`width`、`height`、`textureWidth`、`textureHeight`、`alpha`/`opacity`/`transparency`、`corner`、`useNineSlice`、`foreground`（在 `textureLayers` 里用它指定层级）、`priority`。

`alpha` 支持 `0.0` 到 `1.0`，也支持 `0` 到 `255`；运行时可用 `[mmcege:layer.<id>.alpha=0.5]` 修改。

---

## 6. Smart Interface 编辑器

默认编辑器：`< >` 按钮 + 输入框 + OK，位于右下角，编辑控制器绑定的 SmartInterface 或虚拟 key。
自定义编辑器：`smartInterfaceEditors[]`，每个支持 `id`、`x`、`y`、`inputWidth`、`virtualKey`（可多个逗号分隔）、`title`、`showTitle`、`showInfo`、`showControls`、`inputBackground`、`priority`。可用 `hideDefaultSmartInterfaceEditor` 隐藏默认编辑器。

虚拟 DataPort：无实体端口也可写值。推荐读取方式（兼容原生+虚拟）：先读 `ctrl.getSmartInterfaceData(key)`，为空再读 `ctrl.customData`。

---

## 7. 自定义按钮与页面

`buttons[]` 在机器级定义，服务端策略管理器校验以防伪造。动作类型：
- `page` — 纯客户端页面切换。
- `event` — 服务端触发 MMCE `ControllerButtonClickEvent`。
- `smart_set` / `smart_add` — 设置/累加 Smart Interface 值（可选 min/max 限幅）。

示例：`examples/quick-start/buttons-and-pages.json`、`event-button-test.json`、`controller-button-test.json`。

---

## 8. 控制器滑块

`sliders[]` 在机器级定义，普通控制器和集成控制器都支持。别名：`guiSliders`、`gui_sliders`、`rangeControls`、`range_controls`。滑块拖动后会把数值写入 `key` 指定的 Smart Interface / 虚拟 DataPort。

最小示例：

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

常用字段：`id`、`x`、`y`、`width`、`height`、`key`、`min`、`max`、`step`、`value`、`direction`、`trackColor`、`fillColor`、`thumbColor`、`borderColor`、`thumbWidth`、`thumbHeight`、`priority`、`foreground`、`visible`、`page`、`showText`、`textColor`。

示例：`examples/quick-start/sliders.json`。

---

## 9. 运行时 ZS 指令

从推入 MMCE `ControllerGUIRenderEvent.extraInfo[]` 的文本行读取（例如 CraftTweaker/ZenScript 的 `onControllerGUIRender`），**仅客户端**。

**面板路由**
- `[panel:panel_id]文本` —— 发送到指定信息区；无前缀进 `defaultPanelId`。

**Smart Interface 编辑器**
- `[mmcege:si.hide_key]` / `[mmcege:si.show_key]`（= `si.hide_info` / `si.show_info`）—— 隐藏/显示底部信息行
- `[mmcege:si.hide_title]` / `[mmcege:si.show_title]` —— 隐藏/显示顶部标题
- `[mmcege:si.title=自定义标题 {index}/{count} {key}]` —— 设置标题（占位符 `{index}`/`{count}`/`{key}`）
- `[mmcege:si.clear_title]` —— 恢复默认标题

**纹理图层控制**（`<id>` 为该层 id）
- `[mmcege:layer.<id>.x=300]`、`.y=186`、`.scale=1.25`、`.scaleX=1.25`、`.scaleY=0.90`、`.rotation=45`、`.alpha=0.5`、`.opacity=128`、`.transparency=0.25`、`.priority=30`、`.visible=true`、`.reset`、`.clear`
- `[mmcege:layer.reset_all]` / `[mmcege:layer.clear_all]` —— 重置所有图层状态

---

## 10. 注意事项

- 机器 JSON 与 ZS 中的机器 ID 必须一致。
- 建议所有可运行时控制的图层都写 `id`。
- 多个 `virtualKey` 用英文逗号分隔；`showControls=false` 时该输入框不显示左右切换和 OK。
- JSON 修改后建议重启验证；ZS 可先 `/ct reload`。

## 9. 动态可视化组件

`dynamicVisuals[]` 在普通控制器和集成控制器样式里都支持。它是变量驱动视觉的统一系统：`source` -> 归一化 -> 可选 `history` -> `renderer`。

当前 renderer 支持：`textureSwitch`、`fill`、`pie`/`ring`、`lineChart`。source 可以读取控制器 `customData` / Smart Interface 数值，也可以读取内置机器指标，例如 `recipeProgress`、`energyRatio`、`parallelism`、`threadCount`，以及工厂线程数量指标。

还支持可选变换：
- `transform`：静态 `offsetX`、`offsetY`、`scale`、`scaleX`、`scaleY`、`rotation`、`alpha`、`pivotX`、`pivotY`、`pivotUnit`，以及兼容旧写法的 `origin`（`topLeft`、`topCenter`、`topRight`、`centerLeft`、`center`、`centerRight`、`bottomLeft`、`bottomCenter`、`bottomRight`）。
- `transformByValue`：按变量驱动 `offsetX`、`offsetY`、`scale`、`scaleX`、`scaleY`、`rotation`、`alpha`、`pivotX`、`pivotY`。
- 动态 `pivotX` / `pivotY` 的单位仍由静态 `transform.pivotUnit` 决定；如果没写，默认就是 `ratio`。`ratio` 表示基于宽高的 `0..1` 相对坐标，`px` 表示绝对像素坐标。
- 每个 `transformByValue` 通道都可写独立 `source`，不写时默认复用当前 visual 的主 `source`。

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

`foreground`、`priority`、`page`、`visible` 与其他控制器组件规则一致。

变量驱动旋转示例：

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

这个示例里静态 `pivotUnit` 是 `ratio`，所以动态 `pivotX` / `pivotY` 也按相对坐标解释。
