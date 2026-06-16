# MMCEGE 1.1.0-beta 完整教程（可直接进游戏）

本目录提供的是“可直接加载”的示例，不是伪代码。

MMCEGE 目前包含四大功能，本教程按部分组织：

- **第一部分 · 控制器 GUI**（第 1–8 章）：替换 / 自定义普通与集成控制器的 GUI。
- **第二部分 · 自定义仓口（Hatch）**（第 9 章）：用 JSON 定义新的流体 / 气体 / 物品 / 能量仓口方块。
- **第三部分 · 自定义 AE2 总线（实验性）**（第 10 章）：ME 物品输入、混合（物品+流体+气体）输入 / 输出总线。
- **第四部分 · Long 容量配方需求**（第 11 章）：让流体 / 气体配方量突破原版 `int` 上限，自动生效。

> 快速字段速查见 `examples/MMCE_GUI_EXT_FIELD_REFERENCE.md`。

---

# 第一部分 · 控制器 GUI

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
- `subGuis`

### 4.4 `subGuis` 格式

`subGuis` 是控制器下的子界面数组。每个条目至少写：

- `id`
- `mode` / `openMode`，取值 `modal` 或 `replace`
- `x`
- `y`
- `guiWidth`
- `guiHeight`
- `backgroundTexture`
- `hideDefaultBackground`

每个子界面条目还可以继续写与主 controller 同级的功能字段，例如：

- `buttons`
- `texts`
- `customPanels`
- `infoSections`
- `smartInterfaceEditors`
- `textureLayers`

按钮打开子 GUI 时使用：

- `action: "subgui"`
- `targetSubGui`
- `openMode`

关闭当前子 GUI 时使用：

- `action: "close_subgui"`

旧的 `action: "page"` 仍然保留，用于主 controller 内部页切换。

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

### 5.3 子 GUI 打开与关闭

- 子 GUI 通过 JSON 按钮 `action: "subgui"` 打开，不是运行时指令。
- `subgui` 按钮必须配 `targetSubGui`，可选再写 `openMode`。
- `openMode` 和子 GUI 自身的 `mode` 建议统一写成 `modal` 或 `replace`。
- `action: "close_subgui"` 用来关闭当前子 GUI。

### 5.4 贴图层运行时控制

`<id>` 是 JSON 中该层的 `id`。

- `[mmcege:layer.<id>.x=300]`
- `[mmcege:layer.<id>.y=186]`
- `[mmcege:layer.<id>.scale=1.25]`
- `[mmcege:layer.<id>.scaleX=1.25]`
- `[mmcege:layer.<id>.scaleY=0.90]`
- `[mmcege:layer.<id>.rotation=45]`
- `[mmcege:layer.<id>.alpha=0.5]`
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

---

# 第二部分 · 自定义仓口（Hatch）

用一个 JSON 文件就能定义一个全新的仓口方块（含方块 + 物品 + tile），并作为 MMCE 多方块的输入 / 输出组件参与配方。容量使用 **long**，可远超原版 `int`（约 21 亿）上限。

## 9. 自定义仓口

### 9.1 放置位置

把 `.json` 放进：

`.minecraft/config/mmceguiext/custom_hatches/`

模组在**启动时**扫描该目录并注册每个仓口（修改后需重启游戏，不能 `/ct reload`）。

可直接参考的真实示例：

- `config/mmceguiext/custom_hatches/custom_gas_input_hatch.json`（气体输入仓）
- `examples/custom_hatches/fluid_meter_hatch_test.json`（按填充比例切换方块贴图的流体仓）

### 9.2 顶层字段

- `id`：唯一 id；方块会注册为 `mmceguiext:<id>`。
- `displayName`：方块 / 物品显示名。
- `componentType`：组件类型。
  - 单一类型：`item` / `fluid` / `gas` / `energy`
  - 组合类型：`mixed` / `hybrid` / `item_fluid` / `item_fluid_gas`
- `ioType`：`input` 或 `output`。
- `capacity`：默认容量；可按类型单独覆盖：
  - `fluidCapacity` / `gasCapacity` / `energyCapacity` / `energyTransfer`
- `components`：该仓口对外提供的 MMCE 组件数组，每项 `{ "type": "...", "io": "..." }`。
- `block`：方块外观（见 9.3）。
- `guiStyleFile` 或 `gui`：GUI 布局（见 9.4）。
- `tips` / `tooltip`：物品提示行。

### 9.3 `block` 方块外观

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

- `model` / `texture`：基础模型与贴图。
- `textureLevels`：按填充比例（`minFillRatio`，0~1）切换方块外观，`content` 指定看哪种内容（`fluid` / `gas` / `energy` 等）的填充率。
- 其它可选物理属性：`material`、`hardness`、`resistance`、`harvestTool`、`harvestLevel`、`lightLevel`、`unbreakable` 等。

### 9.4 `gui` 界面布局

```json
"gui": {
  "width": 176, "height": 166,
  "coordinateWidth": 176, "coordinateHeight": 166,
  "components": [
    { "type": "slot", "role": "input", "x": 112, "y": 14 },
    { "type": "tank", "content": "fluid", "x": 80, "y": 15, "width": 18, "height": 61, "overlay": true,
      "tips": [ "Fluid: {amount_capacity_formatted} mB" ] },
    { "type": "text", "x": 28, "y": 15, "value": "fluid.name", "color": "FFFFFF", "scale": 1.0 },
    { "type": "player_inventory", "x": 8, "y": 84, "hotbarY": 142 }
  ]
}
```

- `width` / `height`：GUI 实际像素尺寸；`coordinateWidth` / `coordinateHeight`：坐标参考尺寸。
- `components[].type` 常用值：`slot`、`tank`、`text`、`player_inventory`。
  - `slot`：`role` 可为 `input` / `output`。
  - `tank`：`content`（`fluid` / `gas`）、`renderMode`、`alpha`、`overlay`、`style` 等。
  - `text` / `tank` 的文本支持占位符，如 `fluid.name`、`tank.amount_capacity`、`{amount_capacity_formatted}`。

### 9.5 完整最小示例（流体输入仓）

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

### 9.6 配合 MMCE 机器使用

仓口注册后会得到方块 id `mmceguiext:<id>`，像普通 MMCE hatch 一样写进机器结构（`parts` / 结构方块），并在配方里用对应的 `requirement`（流体 / 气体 / 物品 / 能量）即可。

### 9.7 探针信息

安装 The One Probe 时，仓口的填充量与容量会显示在探针上，无需额外配置。

---

# 第三部分 · 自定义 AE2 总线（实验性）

> 这是较新 / 实验性功能，JSON 结构与仓口系统同构，能力依赖 **AE2 + Mekanism Energistics** 同时在场。字段以注册表类中的 `Def` 解析为准，未来可能调整。

每种总线一个 `.json`，启动时注册为方块 + tile，通过 `AENetworkProxy` 接入 AE2 ME 网络（需要频道、消耗网络能量）。

## 10. 三种总线

| 放置目录 | 总线 | 作用 |
|---|---|---|
| `config/mmceguiext/custom_ae_item_input_buses/` | ME 物品输入总线 | 纯物品输入；动态槽位数（≤4096）、超大堆叠、从 ME 网络按 config 槽预拉取到 storage 槽。 |
| `config/mmceguiext/custom_ae_mixed_input_buses/` | 混合输入总线 | 一个方块同时输入**物品 + 流体 + 气体**（三条 AE 存储通道），对 MMCE 暴露为三个分组组件。 |
| `config/mmceguiext/custom_ae_mixed_output_buses/` | 混合输出总线 | 缓冲配方产物（物品+流体+气体）并推送到 ME 网络，对外为 MMCE-CE 的组合组件 `item_fluid_gas`。 |

通用 JSON 字段：

- `id`、`displayName`、方块模型 / 贴图。
- GUI 布局：新式 `gui.components[]`（含 `slot` / `tank`），或旧式扁平的 `configSlots` / `storageSlots` 数组（模组会自动转换）。
- 混合总线另需描述物品 / 流体 / 气体各自的 config 区与 storage 区。

> 目前仓库内尚无开箱示例 JSON。要写这类总线，建议对照源码：
> `common/registry/CustomAEItemInputBusRegistry.java`、`CustomAEMixedInputBusRegistry.java`、`CustomAEMixedOutputBusRegistry.java` 中的 `Def` / `load(...)`。

---

# 第四部分 · Long 容量配方需求

## 11. 突破 int 上限的流体 / 气体配方

MMCE 的 `RequirementFluid` / `RequirementGas` 用 `int` 存配方 `amount`，超过约 **21 亿 mB** 会溢出。MMCEGE 通过 Mixin 改造需求系统，使流体 / 气体量以 **long** 解析与处理。

**无需任何配置** —— 直接在 MMCE 配方 JSON 里写大数值即可：

```json
{
  "type": "fluid",
  "io": "input",
  "fluid": "water",
  "amount": 5000000000
}
```

解析、判定能否开始合成、消耗输入、产出、计算最大并行度，全程走 long 路径；原版小数值配方不受影响（仍走原 int 逻辑）。

正是这一改造，让第二 / 三部分中 long 容量的自定义仓口 / AE 总线能在配方中真正可用。

## 12. 注意事项（第二~四部分）

- 仓口 / 总线 JSON 修改后必须**重启游戏**（启动时注册，`/ct reload` 无效）。
- `id` 一旦被机器结构或存档引用，不要随意改名，否则方块会丢失。
- AE 总线为实验性，升级模组版本后请验证存档与配方是否仍正常。
