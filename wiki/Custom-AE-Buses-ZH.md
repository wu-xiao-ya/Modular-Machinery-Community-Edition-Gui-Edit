# 自定义 AE2 总线（实验性）

[首页](Home) · [English](Custom-AE-Buses-EN)

> ⚠️ **实验性功能。** JSON 结构与仓口系统同构，但功能较新，未来字段可能调整。能力依赖 **AE2 + Mekanism Energistics** 同时在场。升级模组版本后请验证存档与配方。

三种总线，各自一个目录，每个 `.json` 定义一个总线，**游戏启动时**注册为方块 + tile，通过 `AENetworkProxy` 接入 AE2 ME 网络（需要频道、消耗网络能量）。单文件上限均为 1 MB。

| 总线 | 目录 | 方向 | 物品 | 流体 | 气体 | config 槽 |
|---|---|---|:--:|:--:|:--:|:--:|
| ME 物品输入总线 | `custom_ae_item_input_buses/` | 输入 | ✅ | ❌ | ❌ | ✅ |
| 混合输入总线 | `custom_ae_mixed_input_buses/` | 输入 | ✅ | ✅ | ✅ | ✅ |
| 混合输出总线 | `custom_ae_mixed_output_buses/` | 输出 | ✅ | ✅ | ✅ | ❌ |

> 仓库内暂无开箱示例 JSON。本页字段以注册表源码 `Def` / `load()` 解析为准。

GUI 有两种写法：
- **新式**：`gui` 对象 + `components[]`（用 `type`+`role` 区分物品/流体/气体的 config/storage）。
- **旧式**：扁平的 `configSlots`/`storageSlots`/`fluid*`/`gas*` 字段。模组会通过 `buildLegacyGui` 自动转换为新式。

各总线支持情况：ME 物品输入 = 仅旧式；混合输入 = 两者皆可（新式优先）；混合输出 = 仅新式。

---

## 一、ME 物品输入总线

纯物品输入；动态槽位数、超大堆叠、从 ME 网络按 config 槽预拉取到 storage 槽。

### 顶层字段

| 字段 | 类型 | 默认 | 说明 |
|---|---|---|---|
| `id` | string | **必填** | 唯一 id（支持 `namespace:path`） |
| `displayName` | string | null | 显示名 |
| `guiBackgroundTexture` | string | null | GUI 背景贴图 |
| `configSlots` | `{x,y}[]` | `[]` | 配置槽坐标数组（每项最多 4096） |
| `storageSlots` | `{x,y}[]` | `[]` | 存储槽坐标数组 |
| `playerInventoryX` | int | 0 | 玩家背包 X |
| `playerInventoryY` | int | 123 | 玩家背包 Y |
| `playerHotbarY` | int | 181 | 快捷栏 Y |
| `blockTexture` | string | null | 方块贴图 |
| `blockModel` | string | null | 方块模型（回退到 `block.model`） |
| `block` | object | — | 方块属性，见下 |

`block` 子对象（每个字段也可写顶层）：`model`、`material`(iron)、`hardness`(2.0)、`resistance`(10.0)、`harvestTool`(pickaxe)、`harvestLevel`(1)、`soundType`(metal)、`lightLevel`(0.0)、`lightOpacity`(255)、`slipperiness`(0.6)、`unbreakable`(false)。

### GUI

仅支持旧式：`configSlots` 与 `storageSlots` 按下标一一对应（`configSlots[i]` ↔ `storageSlots[i]`，同一槽的“配置位”与“存储位”）。只有两者同下标都非空时该槽才生效。槽位数 = 两数组长度较大者。

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

## 二、混合输入总线

一个方块同时输入**物品 + 流体 + 气体**（三条 AE 存储通道），对 MMCE 暴露为三个共享 groupId 的分组组件。

### 顶层字段（新式 + 旧式通用）

| 字段 | 类型 | 默认 | 说明 |
|---|---|---|---|
| `id` | string | **必填** | 唯一 id |
| `displayName` | string | null | 显示名 |
| `guiBackgroundTexture` | string | null | GUI 背景贴图 |
| `guiWidth` / `guiHeight` | int | 176 / 235 | GUI 尺寸（旧式用），`[1,4096]` |
| `backgroundTextureWidth/Height` | int | =gui 尺寸 | 背景贴图实际像素尺寸（支持缩放） |
| `textureLayers` | array | `[]` | 额外贴图层，见下，最多 256 |
| `playerInventoryX/Y` | int | 8 / 141 | 玩家背包偏移 |
| `playerHotbarY` | int | 199 | 快捷栏 Y |
| `blockTexture` / `blockModel` | string | null | 方块贴图 / 模型（模型先取 `block.model`） |
| `gui` | object | — | **新式 GUI 定义**（优先于旧式） |

**旧式槽位/储罐字段**（会被自动转新式）：
- 物品：`configSlots`、`storageSlots`（`{x,y}[]`）
- 流体：`fluidConfigTank(s)`、`fluidStorageTank(s)`（`{x,y,width,height}`，单数是复数的语法糖）
- 气体：`gasConfigTank(s)`、`gasStorageTank(s)`
- 兼容旧字段：`fluidConfigSlot`/`gasConfigSlot`（`{x,y}`）、`fluidStorageTank`/`gasStorageTank`（`{x,y,w,h}`）

> 单数与复数不要同时写。`TankRect` 字段：`x`/`y`（默认 0）、`width`/`height`（默认 16，`[1,4096]`）。

`textureLayers[]` 每项：`texture`(必填)、`foreground`(false)、`x`/`y`(0)、`width`/`height`(16)、`textureWidth/Height`(=w/h)、`corner`(0,`[0,1024]`)、`useNineSlice`(false)、`priority`(0)。

### 新式 `gui`

`gui`：`width`(176)、`height`(235)、`components[]`（最多 2048）。

`components[]` 每项：`type`(必填)、`role`、`x`/`y`(0)、`width`/`height`(16，`[1,4096]`)、`index`(-1 自动递增，`[0,4095]`)。

**type + role 组合**：

| type | role | 含义 |
|---|---|---|
| `slot` | `item_config` | 物品配置槽 |
| `slot` | `item_output` / `item_storage` | 物品存储槽（两者等价） |
| `slot` | `fluid_config` | 流体配置槽 |
| `slot` | `gas_config` | 气体配置槽 |
| `slot` | `capacity_card` | 容量卡槽；可插 AE 容量卡与已注册的自定义容量卡物品 |
| `tank` | `fluid_storage` | 流体存储罐 |
| `tank` | `gas_storage` | 气体存储罐 |
| `player_inventory` | — | 玩家背包（取组件 x/y） |

> 注意：流体/气体的 **config 用 `slot`，storage 用 `tank`**。

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

基础容量：流体/气体罐各 8000 mB；容量卡只提升流体/气体单格容量，不影响物品槽。容量降低且已连接 AE 时会优先把超出部分退回 AE；未连接 AE 时保留临时超容，待后续重平衡。

---

## 三、混合输出总线

缓冲配方产物（物品+流体+气体）并推送到 ME 网络，对外为 MMCE-CE 的组合组件 `item_fluid_gas`。**只接收产物**，因此没有 config 槽。

### 顶层字段

| 字段 | 类型 | 默认 | 说明 |
|---|---|---|---|
| `id` | string | **必填** | 唯一 id |
| `displayName` | string | null | 显示名 |
| `guiBackgroundTexture` | string | null | GUI 背景贴图 |
| `guiWidth` / `guiHeight` | int | 176 / 235 | GUI 尺寸 |
| `backgroundTextureWidth/Height` | int | =gui 尺寸 | 背景贴图像素尺寸 |
| `textureLayers` | array | `[]` | 额外贴图层（字段同混合输入） |
| `blockTexture` / `blockModel` | string | null | 方块贴图 / 模型 |
| `gui` | object | — | **必须使用**（无旧式字段、无 buildLegacyGui） |

> 输出总线**没有** `configSlots`/`storageSlots`/`fluid*`/`gas*` 旧式字段，也**没有** `playerInventory*`。必须用 `gui.components` 定义布局，否则没有可用槽位。

### 新式 `gui`

`gui`：`width`(176)、`height`(235)、`components[]`（最多 2048）。组件字段同混合输入。

**type + role 组合**（**只有 storage，无 config，无 player_inventory**）：

| type | role | 含义 |
|---|---|---|
| `slot` | `item_storage` / `item_output` | 物品输出槽（两者等价） |
| `slot` | `capacity_card` | 容量卡槽；可插 AE 容量卡与已注册的自定义容量卡物品 |
| `tank` | `fluid_storage` | 流体存储罐 |
| `tank` | `gas_storage` | 气体存储罐 |

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

## 四、容量卡

混合输入/输出总线支持 `type: "slot"` 且 `role: "capacity_card"` 的容量卡槽。AE 容量卡默认可用；自定义容量卡可通过 `config/mmceguiext/capacity_cards/*.json` 注册：

```json
{
  "item": "appliedenergistics2:material",
  "meta": 35,
  "multiplier": 1.5,
  "flatFluid": 1000,
  "flatGas": 1000
}
```

CraftTweaker 写法：

```zenscript
mods.mmceguiext.MMCEGEEvents.registerCapacityCardWithFlat("appliedenergistics2:material", 1.5, 1000, 1000);
```

容量常量：流体/气体罐各 8000 mB；默认物品槽 25、罐 1；动态上限 4096。

---

## 四、常量速查

| 常量 | 值 | 适用 |
|---|---|---|
| 单文件上限 | 1 MB | 全部 |
| `MAX_SLOT_POINTS` | 4096 | 物品输入总线 |
| `MAX_GUI_COMPONENTS` | 2048 | 混合输入/输出 |
| `MAX_COMPONENT_INDEX` | 4095 | 混合输入/输出 |
| `MAX_COMPONENT_SIZE` | 4096 | 混合输入/输出 |
| `MAX_TEXTURE_LAYERS` | 256 | 混合输入/输出 |

## 五、注意事项

- 修改 JSON 必须**重启游戏**。
- `id` 一旦被结构或存档引用，不要改名。
- 这些是实验性功能，配方中真正存取大额流体/气体需配合 [Long 容量配方需求](Long-Capacity-Requirements-ZH)。
