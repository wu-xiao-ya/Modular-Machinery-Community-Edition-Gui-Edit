# Modular Machinery: Community Edition Gui Edit (MMCEGE, 1.12.2)

MMCEGE is an addon for **Modular Machinery: Community Edition** (the KasumiNova fork).
MMCEGE 是 **Modular Machinery: Community Edition**（KasumiNova 分支）的附属模组。

It started as a controller-GUI editor, but now bundles four subsystems:
它最初只是控制器 GUI 编辑器，目前已包含四个子系统：

1. **Controller GUI replacement / customization** — resizable, texture-driven, multi-panel controller GUIs.
   **控制器 GUI 替换 / 自定义** — 可调整大小、贴图驱动、多信息区的控制器 GUI。
2. **JSON-defined custom hatches** — define new fluid/gas/item/energy hatch blocks from config, no code.
   **JSON 定义的自定义仓口（hatch）** — 用配置文件定义新的流体/气体/物品/能量仓口方块，无需写代码。
3. **JSON-defined custom AE2 buses** — ME item input, and mixed (item+fluid+gas) input/output buses.
   **JSON 定义的自定义 AE2 总线** — ME 物品输入总线，以及混合（物品+流体+气体）输入/输出总线。
4. **Long-capacity recipe requirements** — fluid/gas recipe amounts beyond the vanilla `int` limit.
   **Long 容量配方需求** — 流体/气体配方量可突破原版 `int`（约 21 亿 mB）上限。

Current version / 当前版本: **`1.1.0-beta`** · MC `1.12.2` · author / 作者: WuXiaoYa

---

## Dependencies | 依赖

Required (hard) / 必需依赖：

- `modularmachinery` (Community Edition)
- `appliedenergistics2` (AE2 Extended Life)
- `mekanism`
- `mekeng` (Mekanism Energistics — provides the AE gas storage channel / 提供 AE 气体存储通道)

Optional (soft, compile-only) / 可选依赖：

- The One Probe (hatch probe info / 仓口探针信息)
- AE2 Fluid Crafting Rework, GregTech CE, HEI/JEI, GeckoLib

---

## Build | 构建

The project is built with the MMCE source Gradle wrapper.
本项目使用 MMCE 源码的 Gradle wrapper 构建。

From the repo root / 在仓库根目录执行：

```powershell
.\mmce-src\gradlew.bat -p .\mmce-gui-ext build
```

Output / 产物：

- `mmce-gui-ext/build/libs/MMCEGE-1.1.0-beta.jar`

GitHub Actions also builds every push to `main` and every pull request. Open the latest **Build** workflow run on GitHub and download the `MMCEGE-<commit-sha>` artifact to let testers build without using the local machine.
GitHub Actions 也会在每次推送到 `main` 和 PR 时构建。让测试者打开 GitHub 最新的 **Build** workflow 运行，下载 `MMCEGE-<commit-sha>` artifact 即可，不需要本机编译。

MMCEGE is a coremod (`FMLCorePlugin = com.fushu.mmceguiext.core.MMCEGuiExtEarlyMixinLoader`); its Mixins load before MMCE.
MMCEGE 是一个 coremod（`FMLCorePlugin`），其 Mixin 会在 MMCE 之前加载。

---

## Docs & Examples | 文档与示例

- Full tutorial (Chinese) / 完整教程（中文）: `docs/TUTORIAL.zh-CN.md`
- Field quick reference / 字段速查表: `examples/MMCE_GUI_EXT_FIELD_REFERENCE.md`
- Quick-start snippets / 快速上手片段: `examples/quick-start/`
- Game-ready demo pack / 可直接使用的示例包: `examples/game-ready-1.0.1/`
- Default-machine overrides demo / 原版机器覆盖示例: `examples/default-machines-with-mmce-gui-ext/`, `examples/DEFAULT_MMCE_MACHINES_DEMO.md`
- Custom hatch example / 自定义仓口示例: `examples/custom_hatches/`
- Smart Interface editor demo / Smart Interface 编辑器示例: `examples/smart-interface-editor-demo.json` + `.zs`
- Controller slider demo / 控制器滑块示例: `examples/quick-start/sliders.json`

Config directories MMCEGE reads at startup / MMCEGE 启动时读取的配置目录：

| Path / 路径 | Purpose / 用途 |
|---|---|
| `config/mmceguiext/client.cfg` | Global client config / 全局客户端配置 |
| `config/modularmachinery/machinery/*.json` | Per-machine controller GUI override (`mmce_gui_ext` node) / 机器级控制器 GUI 覆盖 |
| `config/mmceguiext/custom_hatches/*.json` | Custom hatch definitions / 自定义仓口定义 |
| `config/mmceguiext/custom_ae_item_input_buses/*.json` | Custom ME item input bus definitions / 自定义 ME 物品输入总线 |
| `config/mmceguiext/custom_ae_mixed_input_buses/*.json` | Custom mixed input bus definitions / 自定义混合输入总线 |
| `config/mmceguiext/custom_ae_mixed_output_buses/*.json` | Custom mixed output bus definitions / 自定义混合输出总线 |

---

# Part 1 — Controller GUI | 第一部分 · 控制器 GUI

MMCEGE replaces the vanilla Machine / Factory controller GUI **only when** a custom texture, hidden default background, or per-machine style override is present. It hooks Forge's `GuiOpenEvent` and does **not** patch any MMCE GUI class.
MMCEGE 仅在存在自定义贴图、隐藏默认背景或机器级样式覆盖时才替换原版普通/集成控制器 GUI。它挂接 Forge 的 `GuiOpenEvent`，**不修改** 任何 MMCE 的 GUI 类。

## Global Config | 全局配置

File / 配置文件：`config/mmceguiext/client.cfg`

Important keys / 重要键：

- `machineController.backgroundTexture`
- `machineController.backgroundTextureWidth`
- `machineController.backgroundTextureHeight`
- `machineController.useNineSlice`
- `machineController.backgroundCorner`
- `machineController.hideDefaultBackground`
- `machineController.disableRightExtension` (force width to MMCE base width, no right-side expanded area / 强制宽度为 MMCE 基础宽度，不启用右侧扩展区)
- `machineController.defaultPanelId`
- `machineController.customPanels` (format / 格式: `id,x,y,width,height`)
- `machineController.enableSmartInterfaceEditor`
- `machineController.smartInterfaceEditorX` (`-1` = auto right-bottom)
- `machineController.smartInterfaceEditorY` (`-1` = auto right-bottom)
- `machineController.smartInterfaceEditorInputWidth`
- `machineController.smartInterfaceEditorVirtualKey` (used when no bound DataPort, writes to controller `customData[key]`; supports multiple keys split by `,` or `;`)
- `factoryController.*` — same keys as `machineController.*` / 与 `machineController.*` 同名键
- `factoryController.specialThreadBackgroundColor` (hex `RRGGBB` or `AARRGGBB`, for core/special thread row tint / 十六进制颜色，用于核心/特殊线程行着色)
- `factoryController.threadQueueX/Y`, `threadVisibleRows`, `threadRowWidth/Height` customize the integrated-controller thread queue and can trigger self-proxy replacement by themselves.
  / 这些字段自定义集成控制器线程队列位置，也能单独触发自代理替换。
- `factoryController.threadScrollbar` is the preferred structured scrollbar config for the integrated-controller thread queue: `x`, `y`, `width`, `height`, `trackTexture`, `thumbTexture`, `trackColor`, `thumbColor`, `textureWidth`, `textureHeight`, `thumbTextureWidth`, `thumbTextureHeight`, `thumbMinHeight`, `visible`.
  / `factoryController.threadScrollbar` 是集成控制器线程队列滚动条的推荐结构化配置，可设置位置、尺寸、轨道/滑块贴图、颜色、贴图源尺寸、最小滑块高度和显隐。
- `showBlueprintInfo`, `showStructureInfo`, `showStatusInfo`, `showParallelismInfo`, `showPerformanceInfo` hide only MMCE built-in/default info lines. They do **not** hide CraftTweaker `ControllerGUIRenderEvent.extraInfo[]`, `[panel:id]` routed text, or custom panels.
  / 这些显示开关只隐藏 MMCE 内置/默认信息行，**不会**隐藏 CraftTweaker `ControllerGUIRenderEvent.extraInfo[]`、`[panel:id]` 路由文本或自定义信息区。

Rules / 规则：

- If a custom texture exists, custom texture is used. / 若存在自定义贴图，则使用自定义贴图。
- If custom texture is empty, MMCE default texture is used. / 若未配置自定义贴图，则使用 MMCE 默认贴图。
- If custom texture is empty and `hideDefaultBackground = true`, default texture is not drawn. / 若未配置自定义贴图且 `hideDefaultBackground = true`，则不绘制默认贴图。
- If MMCE default texture is used, info text stays in MMCE original info area. / 使用 MMCE 默认贴图时，信息文本仍在 MMCE 原信息区域渲染。
- Multi-panel mode is enabled only in custom-texture mode. / 多信息区功能仅在自定义贴图模式下启用。

## GUI Texture Size Guide | GUI 贴图尺寸指南

- Machine Controller, right extension disabled: `176 x 213` / 普通控制器关闭右扩展：`176 x 213`
- Machine Controller, right extension enabled: use runtime `guiWidth x guiHeight` (min `176 x 213`) / 普通控制器启用右扩展：使用运行时尺寸（最小 `176 x 213`）
- Factory Controller, right extension disabled: `280 x 213` / 集成控制器关闭右扩展：`280 x 213`
- Factory Controller, right extension enabled: use runtime `guiWidth x guiHeight` (min `280 x 213`) / 集成控制器启用右扩展：使用运行时尺寸（最小 `280 x 213`）

`guiWidth/guiHeight` can be set globally in `client.cfg`, or per machine in `mmce_gui_ext`; per-machine value has higher priority.
`guiWidth/guiHeight` 可在 `client.cfg` 全局设置，也可在每台机器的 `mmce_gui_ext` 中单独设置；机器级配置优先。

- 9-slice mode (`useNineSlice = true`): source texture can stay at base size and scales cleanly. / 9-slice 模式下源贴图可保持基础尺寸并无损拉伸。
- Direct mode (`useNineSlice = false`): use exact target size to avoid stretching. / 直接绘制模式请使用目标精确尺寸以避免拉伸。

## Per-machine JSON Override | 机器级 JSON 覆盖

Put override fields inside each MMCE machine JSON (`config/modularmachinery/machinery/*.json`):
将覆盖字段写入每台 MMCE 机器的 JSON（位于 `config/modularmachinery/machinery/*.json`）：

```json
{
  "registryname": "power_transformer",
  "localizedname": "Power Transformer",
  "parts": [],
  "mmce_gui_ext": {
    "machineController": {
      "backgroundTexture": "yourmod:textures/gui/pt_machine.png",
      "hideDefaultBackground": false,
      "guiWidth": 360,
      "guiHeight": 213,
      "disableRightExtension": true
    },
    "factoryController": {
      "backgroundTexture": "yourmod:textures/gui/pt_factory.png",
      "hideDefaultBackground": true,
      "guiWidth": 520,
      "guiHeight": 240,
      "disableRightExtension": true,
      "specialThreadBackgroundColor": "B2E5FF",
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

Supported aliases / 支持的别名：

- Root node / 根节点: `mmceGuiExt`, `mmce_gui_ext`, `mmce-gui-ext`
- Controller node / 控制器节点: `machineController` / `machine_controller` / `machine`, `factoryController` / `factory_controller` / `factory`
- Size keys / 尺寸键名: `guiWidth` / `gui_width` / `width`, `guiHeight` / `gui_height` / `height`

Per-machine values override global config for that machine. Since `1.0.1+`, if a machine defines any `mmce_gui_ext` node, unspecified GUI size falls back to MMCE base size (`176x213` / `280x213`) first, reducing coupling to global `client.cfg`.
机器级值会覆盖该机器对应的全局配置。自 `1.0.1+` 起，只要机器定义了 `mmce_gui_ext` 节点，未显式填写的 GUI 尺寸会优先回退到 MMCE 基础尺寸，从而降低对全局 `client.cfg` 的耦合。

For the full per-machine field list (panels, buttons, progress bars, sliders, sub GUIs, texts, texture layers, Smart Interface editors), see `examples/MMCE_GUI_EXT_FIELD_REFERENCE.md`.
完整的机器级字段列表（信息区、按钮、滑块、子 GUI、文字、纹理图层、Smart Interface 编辑器）见 `examples/MMCE_GUI_EXT_FIELD_REFERENCE.md`。

### Texture Offset For Left/Top Decorations | 左上装饰偏移

Use these to align textures whose main frame is not at `(0,0)`:
用于让“主框不在贴图 0,0”的贴图正确对齐：

- `backgroundTextureOffsetX` (and aliases `offsetX`, `textureOriginX`, …)
- `backgroundTextureOffsetY` (and aliases `offsetY`, `textureOriginY`, …)
- `backgroundTextureWidth` / `backgroundTextureHeight`
- `useNineSlice` / `backgroundCorner`
- `centerFullGui` — horizontally center by full visual bounds (useful with a left-extended background) / 按完整视觉边界水平居中（背景向左扩展时有用）

## Runtime ZS Directives | 运行时 ZS 指令

These directives are read from text lines pushed into MMCE's `ControllerGUIRenderEvent.extraInfo[]` (e.g. via CraftTweaker/ZenScript). They are **client-side only**.
以下指令读取自推入 MMCE `ControllerGUIRenderEvent.extraInfo[]` 的文本行（例如通过 CraftTweaker/ZenScript），**仅作用于客户端**。

**Panel routing / 信息区路由**

- `[panel:panel_id]your text` — send a line to a specific panel; no prefix → `defaultPanelId`. / 把该行发送到指定信息区；无前缀则进入 `defaultPanelId`。

**Smart Interface editor / Smart Interface 编辑器**

- `[mmcege:si.hide_key]` / `[mmcege:si.hide_info]` — hide bottom info line / 隐藏底部信息行
- `[mmcege:si.show_key]` / `[mmcege:si.show_info]` — show it (default) / 显示（默认）
- `[mmcege:si.hide_title]` / `[mmcege:si.show_title]` — hide / show top title / 隐藏 / 显示顶部标题
- `[mmcege:si.title=Your Title]` — set custom title; placeholders `{index}` `{count}` `{key}` / 设置自定义标题，支持占位符
- `[mmcege:si.clear_title]` — restore default title / 恢复默认标题

**Texture layer control / 纹理图层控制**

- `[mmcege:layer.<layerId>.<action>=<value>]` where action ∈ `x`, `y`, `scale`, `scaleX`, `scaleY`, `rotation`, `alpha`, `opacity`, `transparency`, `priority`, `visible`, `reset`, `clear`
  / action 可为 `x`、`y`、`scale`、`scaleX`、`scaleY`、`rotation`、`alpha`、`opacity`、`transparency`、`priority`、`visible`、`reset`、`clear`
- `[mmcege:layer.reset_all]` / `[mmcege:layer.clear_all]` — reset all layer states / 重置所有图层状态

## Controller Buttons | 控制器按钮

Buttons are defined per-machine (`buttons[]`) and validated server-side by a policy manager to prevent forged actions. Button actions:
按钮在机器级 JSON（`buttons[]`）中定义，由服务端策略管理器校验以防止伪造。按钮动作类型：

- `page` — client-side page switch / 纯客户端页面切换
- `subgui` — open a configured sub GUI / 打开已配置的子 GUI
- `close_subgui` — close the current sub GUI / 关闭当前子 GUI
- `event` — fire MMCE `ControllerButtonClickEvent` server-side / 在服务端触发 MMCE 按钮点击事件
- `smart_set` / `smart_add` — set / add a Smart Interface value (with optional min/max clamp) / 设置 / 累加 Smart Interface 值（可选 min/max 限幅）

See `examples/quick-start/buttons-and-pages.json`, `event-button-test.json`, `controller-button-test.json`, `subgui-page-reference.json`.
示例见 `examples/quick-start/` 下相应文件。

## Controller Progress Bars | 控制器进度条

Progress bars are defined per-machine with `progressBars[]` (aliases: `progress_bars`, `guiProgressBars`, `gui_progress_bars`). The main mode uses two textures: an empty/background texture and a full/fill texture. MMCEGE draws the empty texture first, then clips the full texture by the current progress. This works in both Machine and Factory controller GUIs.
进度条在机器级 JSON 中通过 `progressBars[]` 定义（别名：`progress_bars`、`guiProgressBars`、`gui_progress_bars`）。主模式使用两张贴图：空槽/背景贴图和满槽/填充贴图。MMCEGE 会先绘制空槽，再按当前进度裁剪满槽。普通控制器和集成控制器都支持。

Minimal two-texture example / 双贴图最小示例：

```json
{
  "progressBars": [
    {
      "id": "recipe_progress",
      "x": 80,
      "y": 38,
      "width": 64,
      "height": 12,
      "source": "machine_progress",
      "direction": "left_to_right",
      "backgroundTexture": "yourmod:textures/gui/progress_empty.png",
      "fillTexture": "yourmod:textures/gui/progress_full.png",
      "textureWidth": 64,
      "textureHeight": 12,
      "showText": true
    }
  ]
}
```

Supported directions / 支持方向：`left_to_right`, `right_to_left`, `top_to_bottom`, `bottom_to_top`.

Supported sources / 支持数据源：

- Machine controller / 普通控制器：`machine_progress` (aliases: `active_recipe`, `recipe_progress`, `current_recipe`, `default`)
- Factory controller / 集成控制器：`factory_first`, `factory_average`, `factory_max`, `factory_thread` (with `threadIndex`), `factory_core` (with `coreThreadId`)

Supported fields / 支持字段：`id`, `x`, `y`, `width`, `height`, `source`, `direction`, `backgroundTexture`, `fillTexture`, `texture`, `textureWidth`, `textureHeight`, `backgroundColor`, `fillColor`, `borderColor`, `threadIndex`, `coreThreadId`, `min`, `max`, `priority`, `foreground`, `visible`, `page`, `showText`, `textColor`.

If no textures are configured, MMCEGE falls back to colored rectangles (`backgroundColor`, `fillColor`, `borderColor`).
若未配置贴图，MMCEGE 会退回使用纯色矩形（`backgroundColor`、`fillColor`、`borderColor`）。

See `examples/quick-start/progress-bars.json`.
示例见 `examples/quick-start/progress-bars.json`。

## Controller Sliders | 控制器滑块

Sliders are defined per-machine with `sliders[]` (aliases: `guiSliders`, `gui_sliders`, `rangeControls`, `range_controls`). They work in both Machine and Factory controller GUIs, can be placed on pages/sub GUIs, and write numeric values to the Smart Interface / virtual DataPort key directly.
滑块在机器级 JSON 中通过 `sliders[]` 定义（别名：`guiSliders`、`gui_sliders`、`rangeControls`、`range_controls`）。普通控制器和集成控制器都支持，可放在分页/子 GUI 内，并直接把数值写入 Smart Interface / 虚拟 DataPort key。

Minimal example / 最小示例：

```json
{
  "sliders": [
    {
      "id": "speed_slider",
      "x": 20,
      "y": 40,
      "width": 120,
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
```

Supported fields / 支持字段：`id`, `x`, `y`, `width`, `height`, `key`, `min`, `max`, `step`, `value`, `direction`, `trackColor`, `fillColor`, `thumbColor`, `borderColor`, `thumbWidth`, `thumbHeight`, `priority`, `foreground`, `visible`, `page`, `showText`, `textColor`.

See `examples/quick-start/sliders.json`.
示例见 `examples/quick-start/sliders.json`。

---

# Part 2 — Custom Hatches | 第二部分 · 自定义仓口

Drop a `.json` into `config/mmceguiext/custom_hatches/` and MMCEGE registers a new block + item + tile at startup. Each hatch acts as an MMCE multiblock component (input/output for fluid / gas / item / energy, or a combined component) and uses **long** capacities (beyond the `int` limit).
在 `config/mmceguiext/custom_hatches/` 放入一个 `.json`，MMCEGE 会在启动时注册新的方块 + 物品 + tile。每个仓口作为 MMCE 多方块组件（流体/气体/物品/能量的输入或输出，或组合组件），容量使用 **long**（突破 `int` 上限）。

Key top-level fields / 关键顶层字段：

- `id` — unique id; block registers as `mmceguiext:<id>` / 唯一 id，方块注册为 `mmceguiext:<id>`
- `displayName` — block/item display name / 方块与物品的显示名
- `componentType` — `item` / `fluid` / `gas` / `energy`, or combined: `mixed` / `hybrid` / `item_fluid` / `item_fluid_gas`
- `ioType` — `input` / `output`
- `capacity` — default capacity; `fluidCapacity` / `gasCapacity` / `energyCapacity` / `energyTransfer` override per type / 默认容量；可按类型单独覆盖
- `components[]` — MMCE machine components this hatch provides, each `{ "type": "...", "io": "..." }` / 该仓口提供的 MMCE 组件
- `block` — block appearance: `model`, `texture`, plus optional `textureLevels[]` that swap the block texture by fill ratio (`content`, `minFillRatio`, `texture`/`model`) / 方块外观，`textureLevels` 可按填充比例切换贴图
- `guiStyleFile` / `gui` — GUI layout (see below) / GUI 布局（见下）
- `tips` / `tooltip` — item tooltip lines / 物品提示行

The `gui` object holds `width`/`height`/`coordinateWidth`/`coordinateHeight` and a `components[]` list of GUI elements (`type`: `slot`, `tank`, `text`, `player_inventory`, …). Tank/text values support placeholders like `fluid.name`, `tank.amount_capacity`, `{amount_capacity_formatted}`.
`gui` 对象包含 `width`/`height`/`coordinateWidth`/`coordinateHeight` 与 `components[]`（`type` 可为 `slot`/`tank`/`text`/`player_inventory` 等）。tank/text 的值支持占位符，如 `fluid.name`、`tank.amount_capacity`、`{amount_capacity_formatted}`。

Minimal example / 最小示例（fluid input hatch）:

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

Working examples / 可用示例：`config/mmceguiext/custom_hatches/custom_gas_input_hatch.json`, `examples/custom_hatches/fluid_meter_hatch_test.json`.

Hatch GUIs / 仓口 GUI：

- Fluid-processor GUI — full custom GUI with tanks/slots and (when applicable) AE inventory access. / 完整自定义 GUI，含储罐/槽位与（适用时）AE 库存访问。
- Fluid-hatch GUI — extends MMCE's native fluid hatch GUI. / 扩展 MMCE 原版流体仓 GUI。
- Upgrade-bus GUI — custom layout over MMCE's upgrade bus. / MMCE 升级总线的自定义布局。

Probe info / 探针信息: when The One Probe is installed, hatch fill levels and capacities are shown on the probe. / 安装 The One Probe 后，探针会显示仓口的填充与容量。

---

# Part 3 — Custom AE2 Buses (experimental) | 第三部分 · 自定义 AE2 总线（实验性）

> These are advanced/experimental. JSON shape mirrors the hatch system; capabilities depend on AE2 + Mekanism Energistics being present. Treat the `Def` parsing in the registry classes as the source of truth.
> 这些功能较新/实验性。JSON 结构与仓口系统同构；能力依赖 AE2 + Mekanism Energistics。字段以注册表类中的 `Def` 解析为准。

Each bus type is JSON-defined (one `.json` per definition) and registered as a block + tile at startup, connecting to the AE2 ME network via an `AENetworkProxy` (requires a channel, consumes network power).
每种总线均由 JSON 定义（每个定义一个 `.json`），启动时注册为方块 + tile，通过 `AENetworkProxy` 接入 AE2 ME 网络（需要频道，消耗网络能量）。

| Directory / 目录 | Bus / 总线 | What it does / 作用 |
|---|---|---|
| `custom_ae_item_input_buses/` | ME Item Input Bus | Item-only input; dynamic slot count (≤4096) and oversized stacks; config/storage pre-fetch from the ME network. / 纯物品输入；动态槽位数、超大堆叠、从 ME 网络预拉取。 |
| `custom_ae_mixed_input_buses/` | Mixed Input Bus | One block inputs **item + fluid + gas** at once via three AE storage channels; exposed to MMCE as three grouped components. / 一个方块同时输入物品+流体+气体，对 MMCE 暴露为三个分组组件。 |
| `custom_ae_mixed_output_buses/` | Mixed Output Bus | Buffers recipe outputs (item+fluid+gas) and pushes them to the ME network; exposed as MMCE-CE's combined `item_fluid_gas` component. / 缓冲配方产物并推送至 ME 网络，对外为 MMCE-CE 的组合组件。 |

Common JSON fields / 通用 JSON 字段：`id`, `displayName`, the GUI layout (`gui.components[]` with `slot` / `tank` entries, or legacy flat `configSlots` / `storageSlots`), block model/texture. The mixed buses additionally describe item/fluid/gas config + storage regions.
通用字段：`id`、`displayName`、GUI 布局（`gui.components[]` 含 `slot`/`tank`，或旧式扁平 `configSlots`/`storageSlots`）、方块模型/贴图。混合总线另外描述物品/流体/气体的 config + storage 区域。

---

# Part 4 — Long-Capacity Recipe Requirements | 第四部分 · Long 容量配方需求

MMCE's `RequirementFluid` / `RequirementGas` store the recipe `amount` as `int`, which overflows above ~2.1 billion mB. MMCEGE patches the requirement system (via Mixins) so fluid/gas amounts are parsed and processed as `long`.
MMCE 的 `RequirementFluid` / `RequirementGas` 以 `int` 存储配方 `amount`，超过约 21 亿 mB 会溢出。MMCEGE 通过 Mixin 改造需求系统，使流体/气体量以 `long` 解析与处理。

**No configuration needed** — just write large `amount` values in your MMCE recipe JSON and they will be respected at parse, craft-start, finish, and parallelism checks (vanilla recipes are unaffected; int paths still work).
**无需任何配置** — 直接在 MMCE 配方 JSON 中写大数值即可，解析、开始合成、完成、并行度计算均会正确处理（原版配方不受影响，int 路径照常工作）。

This is what makes the long-capacity custom hatches / AE buses above actually usable in recipes.
正是这一改造，让前面 long 容量的自定义仓口 / AE 总线能在配方中真正可用。

---

## License | 许可

See the MMCE source license. / 见 MMCE 源码许可。

## Dynamic visuals / 动态可视化组件

`dynamicVisuals[]` can be used under both `machineController` and `factoryController`. It uses one unified pipeline: `source` -> normalized value -> optional `history` -> `renderer`.

Common fields: `id`, `x`, `y`, `width`, `height`, `priority`, `foreground`, `page`, `visible`, `source`, `history`, `renderer`, `transform`, `transformByValue`.

Supported sources:
- `customData`: reads a numeric value from controller custom data / Smart Interface virtual key: `key`, `default`, `min`, `max`, `clamp`, `invert`.
- `machine`: built-in metrics: `recipeProgress`, `recipeMaxProgress`, `energyStored`, `energyCapacity`, `energyRatio`, `parallelism`, `threadCount`, `activeThreadCount`, `idleThreadCount`; factory also supports `factoryThreadCount`, `factoryActiveThreadCount`, `factoryIdleThreadCount`.

Supported renderers:
- `textureSwitch`: ordered `frames[]` using `min`, `max`, `equals`, `texture`, plus optional `fallbackTexture`.
- `fill`: progress-style fill with `backgroundTexture`, `fillTexture`, `direction` (`right`, `left`, `up`, `down`).
- `pie`: color pie/ring chart with `mode` (`pie`/`ring`), `startAngle`, `innerRadius`, `segments`, `color`, `backgroundColor`.
- `lineChart`: uses `history.enabled`, `samples`, `intervalTicks`; renderer fields include `lineColor`, `fillColor`, `gridColor`, `lineWidth`, `showGrid`.

Optional transforms:
- `transform`: static `offsetX`, `offsetY`, `scale`, `scaleX`, `scaleY`, `rotation`, `alpha`, `pivotX`, `pivotY`, `pivotUnit`, and legacy `origin` (`topLeft`, `topCenter`, `topRight`, `centerLeft`, `center`, `centerRight`, `bottomLeft`, `bottomCenter`, `bottomRight`).
- `transformByValue`: drive `offsetX`, `offsetY`, `scale`, `scaleX`, `scaleY`, `rotation`, `alpha` from the normalized source value.
- `pivotX` / `pivotY` only work inside static `transform`. When present, they override `origin` for that visual.
- `pivotUnit` defaults to `ratio`. Use `ratio` for 0..1 relative coordinates, or `px` for absolute pixel coordinates.
- each `transformByValue` channel accepts `{ "min": ..., "max": ... }` and may define its own independent `source`.

Example:

```json
"dynamicVisuals": [
  {
    "id": "heat_icon",
    "x": 120,
    "y": 30,
    "width": 16,
    "height": 16,
    "priority": 20,
    "page": "main",
    "source": { "type": "customData", "key": "heat", "default": 0, "min": 0, "max": 100 },
    "renderer": {
      "type": "textureSwitch",
      "frames": [
        { "max": 30, "texture": "pack:textures/gui/heat_low.png" },
        { "max": 70, "texture": "pack:textures/gui/heat_mid.png" },
        { "texture": "pack:textures/gui/heat_high.png" }
      ]
    }
  },
  {
    "id": "recipe_fill",
    "x": 20,
    "y": 90,
    "width": 64,
    "height": 8,
    "source": { "type": "machine", "metric": "recipeProgress", "min": 0, "max": 1 },
    "renderer": {
      "type": "fill",
      "backgroundTexture": "pack:textures/gui/bar_empty.png",
      "fillTexture": "pack:textures/gui/bar_full.png",
      "direction": "right"
    }
  },
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
]
```
