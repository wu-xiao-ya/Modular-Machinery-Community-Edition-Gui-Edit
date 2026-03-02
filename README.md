# Modular Machinery: Community Edition Gui Edit (MMCEGE, 1.12.2)

MMCEGE addon for MMCE controller GUIs.  
MMCEGE 是一个用于 MMCE 控制器 GUI 的附属模组。

## Docs | 文档

- Full tutorial (Chinese): `docs/TUTORIAL.zh-CN.md`
- Game-ready example pack: `examples/game-ready-1.0.1/`
- Quick start in example folder: `examples/game-ready-1.0.1/README.zh-CN.md`

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

- `mmce-gui-ext/build/libs/MMCEGE-1.0.1.jar`

## Global Config | 全局配置

File / 配置文件：

- `.minecraft/config/mmceguiext/client.cfg`

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
- `factoryController.backgroundTexture`
- `factoryController.backgroundTextureWidth`
- `factoryController.backgroundTextureHeight`
- `factoryController.useNineSlice`
- `factoryController.backgroundCorner`
- `factoryController.hideDefaultBackground`
- `factoryController.disableRightExtension` (force width to MMCE base width, no right-side expanded area / 强制宽度为 MMCE 基础宽度，不启用右侧扩展区)
- `factoryController.specialThreadBackgroundColor` (hex `RRGGBB` or `AARRGGBB`, for core/special thread row tint / 十六进制颜色，用于核心/特殊线程行着色)
- `factoryController.defaultPanelId`
- `factoryController.customPanels` (format / 格式: `id,x,y,width,height`)
- `factoryController.enableSmartInterfaceEditor`
- `factoryController.smartInterfaceEditorX` (`-1` = auto right-bottom)
- `factoryController.smartInterfaceEditorY` (`-1` = auto right-bottom)
- `factoryController.smartInterfaceEditorInputWidth`
- `factoryController.smartInterfaceEditorVirtualKey` (used when no bound DataPort, writes to controller `customData[key]`; supports multiple keys split by `,` or `;`)

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

Since `1.0.1+`, if a machine defines any `mmce_gui_ext` style node, unspecified GUI size falls back to MMCE base size first (`176x213` / `280x213`) to reduce global `client.cfg` coupling.  
从 `1.0.1+` 开始，只要机器定义了 `mmce_gui_ext` 样式节点，未显式填写的 GUI 尺寸会优先回退到 MMCE 基础尺寸（`176x213` / `280x213`），从而降低对全局 `client.cfg` 的耦合。

If a machine uses custom texture but does not set `backgroundTextureWidth/Height`, renderer auto-uses runtime target size to avoid accidental scaling from global texture size.  
若机器使用了自定义贴图但未设置 `backgroundTextureWidth/Height`，渲染器会自动使用运行时目标尺寸，避免被全局贴图尺寸误触发缩放。



## 1.0.1 Offset For Left/Top Decorations | 1.0.1 左上装饰偏移

Use these keys to align custom textures that place the main frame at (offsetX, offsetY):
使用以下键让“主框不在贴图 0,0”的贴图正确对齐：

- `machineController.backgroundTextureOffsetX`
- `machineController.backgroundTextureOffsetY`
- `factoryController.backgroundTextureOffsetX`
- `factoryController.backgroundTextureOffsetY`

Per-machine JSON override keys:
机器 JSON 覆盖键：

- `backgroundTextureOffsetX` / `background_texture_offset_x` / `textureOffsetX` / `texture_offset_x` / `offsetX` / `offset_x` / `textureOriginX` / `texture_origin_x`
- `backgroundTextureOffsetY` / `background_texture_offset_y` / `textureOffsetY` / `texture_offset_y` / `offsetY` / `offset_y` / `textureOriginY` / `texture_origin_y`
- `backgroundTextureWidth` / `background_texture_width` / `textureWidth` / `texture_width`
- `backgroundTextureHeight` / `background_texture_height` / `textureHeight` / `texture_height`
- `useNineSlice` / `use_nine_slice` / `nineSlice` / `nine_slice`
- `backgroundCorner` / `background_corner` / `corner` / `cornerSize` / `corner_size`
- `enableSmartInterfaceEditor` / `enable_smart_interface_editor` / `enableDataPortEditor` / `enable_data_port_editor` / `enableDataPort`
- `smartInterfaceEditorX` / `smart_interface_editor_x` / `dataPortEditorX` / `data_port_editor_x` / `dataPortX` / `data_port_x`
- `smartInterfaceEditorY` / `smart_interface_editor_y` / `dataPortEditorY` / `data_port_editor_y` / `dataPortY` / `data_port_y`
- `smartInterfaceEditorInputWidth` / `smart_interface_editor_input_width` / `dataPortEditorInputWidth` / `data_port_editor_input_width` / `dataPortWidth` / `data_port_width`
- `smartInterfaceEditorVirtualKey` / `smart_interface_editor_virtual_key` / `dataPortEditorVirtualKey` / `data_port_editor_virtual_key` / `virtualDataPortKey` / `virtual_data_port_key`

Smart Interface editor ZS directives (put in `ControllerGUIRenderEvent.extraInfo[]`):
Smart Interface 编辑器 ZS 指令（写在 `ControllerGUIRenderEvent.extraInfo[]`）：

- `[mmcege:si.hide_key]` / `[mmcege:si.hide_info]` hide the bottom info line (`Key: ...` or value info)
- `[mmcege:si.show_key]` / `[mmcege:si.show_info]` show the bottom info line (default)
- `[mmcege:si.hide_title]` hide top title line
- `[mmcege:si.show_title]` show top title line (default)
- `[mmcege:si.title=Your Title]` set custom top title; supports placeholders `{index}` `{count}` `{key}`
- `[mmcege:si.clear_title]` clear custom title and restore default
