# 自定义仓口（Custom Hatches）

[首页](Home) · [English](Custom-Hatches-EN)

用一个 JSON 文件定义一个全新的仓口方块（含**方块 + 物品 + tile**），作为 MMCE 多方块的输入/输出组件参与配方。容量使用 **long**，可远超原版 `int`（约 21 亿）上限——配合 [Long 容量配方需求](Long-Capacity-Requirements-ZH) 使用。

- 放置目录：`config/mmceguiext/custom_hatches/*.json`
- 扫描时机：**游戏启动时**（修改后需重启，`/ct reload` 无效）
- 单文件上限：1 MB
- 注册结果：方块注册为 `mmceguiext:<id>`

真实可参考示例：
- `config/mmceguiext/custom_hatches/custom_gas_input_hatch.json`
- `examples/custom_hatches/fluid_meter_hatch_test.json`

---

## 1. 顶层字段

| 字段 | 别名 | 类型 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | — | string | **必填** | 唯一 id（自动转小写）；方块注册为 `mmceguiext:<id>` |
| `displayName` | — | string | null | 方块/物品显示名 |
| `componentType` | — | string | `fluid` | 默认组件类型（`components` 为空时用），取值见 §1.1 |
| `ioType` | — | string | `input` | 默认 IO 方向（`components` 为空时用）：`input` / `output`（别名 `out`） |
| `capacity` | — | long | `1000` | 基础容量，作为下列各容量的回退默认值，范围 `[1, Long.MAX]` |
| `fluidCapacity` | — | long | =`capacity` | 流体储罐容量（mB） |
| `gasCapacity` | — | long | =`capacity` | 气体储罐容量（mB，需 Mekanism） |
| `energyCapacity` | — | long | =`capacity`（也回退 `energy`） | 能量存储上限（FE） |
| `energyTransfer` | — | long | =`energyCapacity`（也回退 `energyTransferLimit`） | 每 tick 能量传输上限（FE） |
| `components` | — | array | `[]` | 对外提供的 MMCE 组件，见 §2，最多 256 项 |
| `block` | — | object | 见 §3 | 方块外观与物理属性 |
| `gui` | — | object | 见 §4 | GUI 布局 |
| `guiStyleFile` | — | string | null | 外链 GUI 样式文件（由 GlobalGuiStyleManager 加载） |
| `tips` | `tooltip`,`tooltips` | string[]/string | `[]` | 物品/储罐提示行（支持占位符，见 §5），最多 512 |
| `outputSlotLock` | `output_slot_lock`,`lockOutputSlots`,`lock_output_slots` | boolean | `true` | 锁定输出槽（同类物品只堆叠到已有槽位） |
| `blockTexture` / `blockModel` | — | string | null | 顶层快捷写法，被 `block.texture`/`block.model` 覆盖 |
| `inputSlot` / `outputSlot` | — | `{x,y}` | `{0,0}` | 旧式槽位坐标（无 `gui.components` 时用） |
| `tank` | — | object | 见 §3.3 | 旧式储罐渲染参数（无 gui tank 组件时用） |
| `texts` | — | array | `[]` | 旧式静态文本列表（被 gui text 组件覆盖） |

### 1.1 `componentType` 取值

- 单一：`item` / `fluid` / `gas` / `energy`（别名 `power` / `fe` / `rf`）
- 组合：`mixed` / `hybrid` / `item_fluid` / `item_fluid_gas`（自动创建组合组件）

---

## 2. `components[]` — MMCE 组件声明

每项 `{ "type": "...", "io": "..." }`：

| 字段 | 类型 | 默认 | 说明 |
|---|---|---|---|
| `type` | string | **必填** | `fluid` / `gas` / `item` / `energy`（别名 `power`/`fe`/`rf`）/ `mixed`（别名 `hybrid`/`item_fluid`/`item_fluid_gas`） |
| `io` | string | `input` | `input` / `output`（别名 `out`） |

> 组合行为：当数组中存在任何 `mixed`/`hybrid`/`item_fluid`/`item_fluid_gas` 时，系统按其 `io` 自动创建一对（input 和/或 output）组合组件，其余同类型条目不再重复创建。

```json
"components": [
  { "type": "fluid", "io": "input" },
  { "type": "energy", "io": "input" }
]
```

---

## 3. `block` — 方块外观与属性

每个字段也可写在顶层（作为回退；`block` 内优先）。

| 字段 | 别名 | 类型 | 默认 | 说明 |
|---|---|---|---|---|
| `model` | 顶层 `blockModel` | string | null | 方块模型资源路径 |
| `texture` | 顶层 `blockTexture` | string | null | 方块纹理资源路径 |
| `material` | — | string | `iron` | 材质类型 |
| `hardness` | — | float | `2.0` | 硬度 |
| `resistance` | — | float | `10.0` | 爆炸抗性 |
| `harvestTool` | — | string | `pickaxe` | 采集工具 |
| `harvestLevel` | — | int | `1` | 采集等级 |
| `soundType` | — | string | `metal` | 声音类型 |
| `lightLevel` | — | float | `0.0` | 发光等级 |
| `lightOpacity` | — | int | `255` | 光线不透明度 |
| `slipperiness` | — | float | `0.6` | 表面滑度 |
| `unbreakable` | — | boolean | `false` | 不可破坏 |
| `textureLevels` | `texture_levels`；顶层 `blockTextureLevels`/`block_texture_levels`/`blockTextures` | array | `[]` | 按填充比例切换外观，见 §3.1，最多 256 |

### 3.1 `block.textureLevels[]` — 按填充比例切换方块外观

每项可以是对象，也可以是纯纹理字符串（此时 `minFillRatio` 按索引在 0~1 均匀分布）。

| 字段 | 别名 | 类型 | 默认 | 说明 |
|---|---|---|---|---|
| `content` | `resource`,`source`,`type`,`storage` | string | 继承上层（默认 `fluid`） | 看哪种内容的填充率：`fluid` / `gas` / `energy` |
| `minFillRatio` | `min_fill_ratio`,`minRatio`,`ratio`,`threshold`,`min`,`from`,`level`,`percent` | double | `0.0` | 触发该等级的最小填充比 `[0,1]` |
| `texture` | `blockTexture`,`block_texture` | string | null | 该等级纹理 |
| `model` | `blockModel`,`block_model` | string | null | 该等级模型 |

> 渲染时按 `fillRatio` 取 content 匹配且 `minFillRatio <= fillRatio` 的最高等级；三种 content 分别评估后取优先级最高者。

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

### 3.3 旧式 `tank` 对象（无 gui tank 组件时）

| 字段 | 别名 | 类型 | 默认 | 说明 |
|---|---|---|---|---|
| `x`,`y` | — | int | `0` | 渲染坐标 |
| `width` | — | int | `16` | 宽 `[1,4096]` |
| `height` | — | int | `48` | 高 `[1,4096]` |
| `content` | — | string | `fluid` | `fluid` / `gas` / `fluid_gas`（流体空则显示气体）/ `energy`（别名 `power`/`fe`） |
| `renderMode` | `render_mode`,`render`,`mode` | string | null | `solid`(`flat`/`color`) / `texture`(`textured`/`sprite`) |
| `alpha` | `opacity`,`transparency` | float | null（1.0） | 透明度 `[0,1]`（>1 且 ≤255 自动除以 255） |

---

## 4. `gui` — 界面布局

| 字段 | 类型 | 默认 | 说明 |
|---|---|---|---|
| `width` | int | `176` | GUI 宽 `[1,4096]` |
| `height` | int | `166` | GUI 高 `[1,4096]` |
| `coordinateWidth` | int | `-1`（不启用） | 逻辑坐标系宽度（启用坐标缩放映射） |
| `coordinateHeight` | int | `-1`（不启用） | 逻辑坐标系高度 |
| `components` | array | `[]` | GUI 组件，见 §4.1，最多 4096（展开后） |

### 4.1 `gui.components[]`

共用字段：`type`（必填）、`role`、`x`、`y`、`width`、`height`、`priority`（别名 `zIndex`/`z_index`/`z`/`layer`）。

`type` 取值：`slot` / `slot_grid`（别名 `slots`）/ `tank` / `text` / `player_inventory`。

**`slot`** — 单个槽位
- `role`：`input` / `output`（仅这两个值参与运行时自动索引分配）
- `index`：槽位索引，`-1` 自动递增，最大 4095

**`slot_grid` / `slots`** — 槽位网格（自动展开为多个 slot）
- `rows`（`rowCount`/`yCount`，默认 1，≤256）、`columns`（`cols`/`columnCount`/`xCount`，默认 1，≤256）
- `spacingX`（`xSpacing`/`gapX`，默认 2）、`spacingY`（`ySpacing`/`gapY`，默认 2）
- `slotSize`（`size`，默认 16）
- `visibleRows`/`visibleColumns`（默认 0=全部可见，用于滚动）、`scrollMode`、`scrollbar`
- 滚动条系列：`scrollbarX/Y/Width/Height`、`scrollbarThumbHeight`、`scrollbarTexture`（含 hover/pressed/disabled）、`scrollbarTextureWidth/Height`、`scrollbarU/V`（含 hover/pressed/disabled 变体）——均支持 snake_case
- 物品覆盖层：`itemOverlay`、`itemOverlayTexture`、`itemOverlayTextureWidth/Height`、`itemOverlayU/V`

**`tank`** — 储罐区域
- `content`：`fluid` / `gas` / `fluid_gas` / `energy`（`power`/`fe`）
- `renderMode`（`render_mode`/`render`/`mode`）、`alpha`（`opacity`/`transparency`）、`overlay`（默认 true，MMCE 风格边框）
- `color`：能量条 ARGB 十六进制（默认绿色 `0xFF3DDC84`）
- `tips`（`tooltip`/`tooltips`）：储罐提示，支持占位符
- `x`/`y`/`width`/`height`

**`text`** — 动态文本
- `value`：文本或占位符 key（见 §5）
- `color`：ARGB 十六进制（默认白 `0xFFFFFF`）
- `scale`：缩放（默认 1.0）
- `align`（`alignment`/`textAlign`/`text_align`）：`left`/`center`/`right`（含别名 `start`/`middle`/`end`）
- `content`：决定占位符数据源

**`player_inventory`** — 玩家背包
- `x`、`y`（主区 3×9）、`hotbarY`（`-1` 时自动 = `y+58`）

---

## 5. 文本占位符

两套机制：

### 5.1 `text.value` 直接 key（整串等于 key 时替换）

数量类：`tank.amount` / `tank.capacity` / `tank.amount_capacity`，加 `_formatted`（或 `.compact`）得紧凑格式（如 `1.2K`）。
名称类：`fluid.name`、`gas.name`、`tank.name`。
气体类：`gas.amount`、`gas.capacity`、`gas.amount_capacity`（+ `_formatted`）。
能量类：`energy.name`、`energy.amount`（`energy.stored`）、`energy.capacity`（`energy.max`）、`energy.amount_capacity`、`energy.transfer`（均有 `_formatted`）。
槽位：`input.slot` → "Input"，`output.slot` → "Output"。
不匹配则原样显示。

### 5.2 模板内联占位符（用于 `tips`/`tooltip` 字符串）

形如 `Fluid: {amount_capacity_formatted} {unit}`，常用 token：
`{name}`、`{amount}`、`{capacity}`、`{amount_capacity}`（各有 `_formatted`）、`{energy}`、`{energy.capacity}`、`{energy.amount_capacity}`、`{energy.transfer}`（各有 `_formatted`）、`{unit}`（能量为 `FE`，否则 `mB`）。数据源由组件 `content` 决定。

---

## 6. 完整最小示例

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

## 7. 在机器里使用 & 探针

- 用方块 id `mmceguiext:<id>` 像普通 MMCE hatch 一样写进机器结构，在配方里用对应 `requirement`（流体/气体/物品/能量）。
- 安装 The One Probe 时，仓口的填充量与容量自动显示在探针上。

## 8. 注意事项

- 修改 JSON 必须**重启游戏**。
- `id` 一旦被机器结构或存档引用，不要改名，否则方块会丢失。
- 三种 GUI 实现：流体处理 hatch（完整自定义，含 AE 库存访问）、流体 hatch（继承 MMCE 原版）、升级总线（MMCE 升级总线的自定义布局）。
