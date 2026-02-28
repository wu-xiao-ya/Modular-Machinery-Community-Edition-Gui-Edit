# Modular Machinery: Community Edition Gui Edit (MMCEGE, 1.12.2)

MMCEGE addon for MMCE controller GUIs.  
MMCEGE 是一个用于 MMCE 控制器 GUI 的附属模组。

## Features | 功能

- Replace Machine Controller and Factory Controller GUIs. / 替换普通控制器与集成控制器 GUI。
- Resizable extra canvas area (without breaking slot alignment). / 支持可调整大小的扩展区域（不会破坏槽位对齐）。
- Scrollable and draggable info panel. / 支持信息区域滚动与拖拽。
- Optional custom background texture. / 支持自定义背景贴图。
- Optional hide of MMCE default background. / 支持隐藏 MMCE 默认背景。
- Per-machine override from each MMCE machine JSON. / 支持在每台机器的 JSON 中单独覆盖配置。

## Build | 构建

From `C:\mc_modding\MMCE-FUSHU`:  
在 `C:\mc_modding\MMCE-FUSHU` 目录执行：

```powershell
.\mmce-src\gradlew.bat -p .\mmce-gui-ext build
```

Output / 产物：

- `mmce-gui-ext/build/libs/MMCEGE-1.0.0.jar`

## Global Config | 全局配置

File / 配置文件：

- `.minecraft/config/mmceguiext/client.cfg`

Important keys / 重要键：

- `machineController.backgroundTexture`
- `machineController.hideDefaultBackground`
- `machineController.disableRightExtension` (force width to MMCE base width, no right-side expanded area / 强制宽度为 MMCE 基础宽度，不启用右侧扩展区)
- `machineController.defaultPanelId`
- `machineController.customPanels` (format / 格式: `id,x,y,width,height`)
- `factoryController.backgroundTexture`
- `factoryController.hideDefaultBackground`
- `factoryController.disableRightExtension` (force width to MMCE base width, no right-side expanded area / 强制宽度为 MMCE 基础宽度，不启用右侧扩展区)
- `factoryController.specialThreadBackgroundColor` (hex `RRGGBB` or `AARRGGBB`, for core/special thread row tint / 十六进制颜色，用于核心/特殊线程行着色)
- `factoryController.defaultPanelId`
- `factoryController.customPanels` (format / 格式: `id,x,y,width,height`)

Rules / 规则：

- If a custom texture exists, custom texture is used. / 若存在自定义贴图，则使用自定义贴图。
- If custom texture is empty, MMCE default texture is used. / 若未配置自定义贴图，则使用 MMCE 默认贴图。
- If custom texture is empty and `hideDefaultBackground = true`, default texture is not drawn. / 若未配置自定义贴图且 `hideDefaultBackground = true`，则不绘制默认贴图。
- If MMCE default texture is used, info text stays in MMCE original info area. / 使用 MMCE 默认贴图时，信息文本仍在 MMCE 原信息区域渲染。
- Multi-panel mode is enabled only in custom-texture mode. / 多信息区功能仅在自定义贴图模式下启用。
- Special/core factory thread row color can be overridden. / 工厂控制器核心/特殊线程行颜色支持覆盖。

## GUI Texture Size Guide | GUI 贴图尺寸指南

Use these sizes when drawing controller GUI textures:  
绘制控制器 GUI 贴图时建议使用以下尺寸：

- Machine Controller, right extension disabled: `176 x 213` / 普通控制器关闭右扩展：`176 x 213`
- Machine Controller, right extension enabled: use actual runtime size `guiWidth x guiHeight` (minimum `176 x 213`) / 普通控制器启用右扩展：使用运行时尺寸 `guiWidth x guiHeight`（最小 `176 x 213`）
- Factory Controller, right extension disabled: `280 x 213` / 集成控制器关闭右扩展：`280 x 213`
- Factory Controller, right extension enabled: use actual runtime size `guiWidth x guiHeight` (minimum `280 x 213`) / 集成控制器启用右扩展：使用运行时尺寸 `guiWidth x guiHeight`（最小 `280 x 213`）

`guiWidth/guiHeight` can be set globally in `client.cfg`, or per machine in `mmce_gui_ext`.  
`guiWidth/guiHeight` 可在 `client.cfg` 全局设置，也可在每台机器的 `mmce_gui_ext` 中单独设置。  
Per-machine value has higher priority.  
机器级配置优先级更高。

If `disableRightExtension = true`, width is forced to MMCE base width (`176` machine / `280` factory), so your texture should be made at base width.  
若 `disableRightExtension = true`，宽度会强制为 MMCE 基础宽度（普通 `176` / 集成 `280`），贴图应按基础宽度制作。

For 9-slice mode (`useNineSlice = true`), source texture can stay at base size and scales cleanly.  
9-slice 模式（`useNineSlice = true`）下，源贴图可保持基础尺寸并进行无损拉伸。  
For direct mode (`useNineSlice = false`), use exact target size to avoid stretching.  
直接绘制模式（`useNineSlice = false`）下，请使用目标精确尺寸以避免拉伸变形。

## ZS Panel Selection | ZS 信息区选择

In Controller GUI extra info, route a line to a target panel with:  
在控制器 GUI 扩展信息中，可使用以下前缀把文本发送到指定信息区：

- `[panel:panel_id]your text`

If no prefix is provided, text goes to `defaultPanelId`.  
如果不写前缀，文本会进入 `defaultPanelId` 指定的信息区。

Example / 示例：

- `[panel:main]Main status line`
- `[panel:debug]Debug line`
- `no prefix -> default panel` / `无前缀 -> 默认信息区`

## Per-machine JSON Override | 机器级 JSON 覆盖

Put override fields inside each MMCE machine JSON (under `config/modularmachinery/machinery/*.json`):  
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
      "specialThreadBackgroundColor": "B2E5FF"
    }
  }
}
```

Supported aliases / 支持的别名：

- `mmceGuiExt`, `mmce_gui_ext`, `mmce-gui-ext`
- `machineController`, `machine_controller`, `machine`
- `factoryController`, `factory_controller`, `factory`
- size keys / 尺寸键名: `guiWidth` / `gui_width` / `width`, `guiHeight` / `gui_height` / `height`

Per-machine JSON values override global config values for that machine.  
机器 JSON 中的值会覆盖该机器对应的全局配置值。
