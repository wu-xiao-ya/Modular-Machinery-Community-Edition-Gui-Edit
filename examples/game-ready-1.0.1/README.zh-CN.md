# MMCEGE 1.0.1 正式版完整教程（可直接进游戏）

本目录提供的是“可直接加载”的示例，不是伪代码。

## 1. 文件清单

- `machine-mmcege-full-demo-1.0.1.json`
- `mmcege-full-demo-1.0.1.zs`
- `client.cfg.sample`

## 2. 直接使用步骤

1. 复制 `machine-mmcege-full-demo-1.0.1.json` 到：
   `.minecraft/config/modularmachinery/machinery/`
2. 复制 `mmcege-full-demo-1.0.1.zs` 到：
   `.minecraft/scripts/`
3. 可选：先备份原配置，再把 `client.cfg.sample` 覆盖到：
   `.minecraft/config/mmceguiext/client.cfg`
4. 启动/重启游戏（JSON 建议重启，ZS 可 `/ct reload`）。

机器 ID：`mmcege_full_feature_demo_101`

## 3. 这个示例覆盖了哪些功能

- 控制器 GUI 替换（机器/工厂双控制器）
- 自定义 GUI 尺寸、右侧扩展区
- 自定义面板布局 `customPanels`
- 面板路由输出 `[panel:id]`
- Smart Interface 默认编辑器
- Smart Interface 自定义编辑器（多输入框、隐藏标题/信息/按钮、隐藏输入框底板）
- 虚拟 DataPort（无实体端口也可写值）
- 与 MMCE 原生 DataPort 类似的读取方式（`getSmartInterfaceData` + `customData` 兜底）
- 多层贴图（背景层/前景层）
- 贴图优先级（`priority`）
- 贴图运行时控制（平移、缩放、旋转、显隐、动态优先级）
- 工厂核心线程背景色 `specialThreadBackgroundColor`

## 4. JSON 写法总览（推荐字段名）

机器 JSON 中使用：

```json
"mmce_gui_ext": {
  "machineController": { ... },
  "factoryController": { ... }
}
```

`machineController` / `factoryController` 常用字段：

- `backgroundTexture`
- `backgroundTextureOffsetX`
- `backgroundTextureOffsetY`
- `hideDefaultBackground`
- `guiWidth`
- `guiHeight`
- `enableRightExtension`
- `useNineSlice`
- `backgroundTextureWidth`
- `backgroundTextureHeight`
- `backgroundCorner`
- `specialThreadBackgroundColor`（仅工厂控制器有意义）
- `threadQueueX`
- `threadQueueY`
- `threadScrollbarX`
- `threadScrollbarY`
- `threadVisibleRows`
- `threadRowWidth`
- `threadRowHeight`
- `enableSmartInterfaceEditor`
- `smartInterfaceEditorX`
- `smartInterfaceEditorY`
- `smartInterfaceEditorInputWidth`
- `smartInterfaceEditorVirtualKey`
- `smartInterfaceEditorPriority`
- `foregroundContentPriority`
- `hideDefaultSmartInterfaceEditor`
- `defaultPanelId`
- `customPanels`
- `smartInterfaceEditors`
- `textureLayers` / `backgroundLayers` / `foregroundLayers`

### 4.1 `customPanels` 格式

每一项都必须是：

`id,x,y,width,height`

例如：

`"main,182,10,148,96"`

### 4.2 `smartInterfaceEditors` 格式

每个编辑器支持：

- `id`
- `x`
- `y`
- `inputWidth`
- `virtualKey`（可多个，逗号分隔）
- `title`
- `showTitle`
- `showInfo`
- `showControls`
- `inputBackground`
- `priority`

### 4.3 贴图层格式

每层支持：

- `id`（建议必须写；运行时指令靠它定位）
- `texture`
- `offsetX`
- `offsetY`
- `width`
- `height`
- `textureWidth`
- `textureHeight`
- `corner`
- `useNineSlice`
- `foreground`
- `priority`

说明：

- `backgroundLayers` 自动是背景层。
- `foregroundLayers` 自动是前景层。
- `textureLayers` 里可用 `foreground: true/false` 指定层级。

## 5. ZS 指令写法（`onControllerGUIRender` 的 `extraInfo`）

### 5.1 Smart Interface 显示控制

- `[mmcege:si.hide_key]`
- `[mmcege:si.show_key]`
- `[mmcege:si.hide_title]`
- `[mmcege:si.show_title]`
- `[mmcege:si.title=自定义标题 {index}/{count} {key}]`
- `[mmcege:si.clear_title]`

可用占位符：

- `{index}`
- `{count}`
- `{key}`

### 5.2 面板路由

- `[panel:main]这行显示在 main 面板`
- `[panel:log]这行显示在 log 面板`

### 5.3 贴图层运行时控制

`<id>` 是 JSON 中该层的 `id`。

- `[mmcege:layer.<id>.x=300]`
- `[mmcege:layer.<id>.y=186]`
- `[mmcege:layer.<id>.scale=1.25]`
- `[mmcege:layer.<id>.scaleX=1.25]`
- `[mmcege:layer.<id>.scaleY=0.90]`
- `[mmcege:layer.<id>.rotation=45]`
- `[mmcege:layer.<id>.priority=30]`
- `[mmcege:layer.<id>.visible=true]`
- `[mmcege:layer.<id>.reset]`
- `[mmcege:layer.reset_all]`

## 6. 数据端口读取建议（同时兼容原生和虚拟）

示例 `mmcege-full-demo-1.0.1.zs` 里的 `readVirtualOrNativePort(...)` 已包含推荐写法：

1. 先读 `ctrl.getSmartInterfaceData(key)`（原生风格）
2. 若为空再读 `ctrl.customData`（虚拟输入框值）

这就是“有实体端口则走原生，无实体端口也能跑”的兼容模式。

## 7. 全局配置文件写法

`client.cfg.sample` 覆盖了本模组全局可配项，可直接复制到：

`.minecraft/config/mmceguiext/client.cfg`

该文件主要控制：

- 全局开关 `enabled`
- 滚轮步长 `wheelStep`
- 机器控制器默认参数
- 工厂控制器默认参数（含 `queueVisibleRows`）

## 8. 注意事项

- 机器 JSON 与 ZS 中的机器 ID 必须一致。
- 建议所有可运行时控制的图层都写 `id`。
- 多个 `virtualKey` 建议用英文逗号分隔。
- `showControls=false` 时该输入框不显示左右切换和 OK 按钮。
- JSON 修改后建议重启游戏验证；ZS 可先试 `/ct reload`。
