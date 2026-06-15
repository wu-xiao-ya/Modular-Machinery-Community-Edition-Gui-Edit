# MMCEGE Wiki

**Modular Machinery: Community Edition — GUI Edit (MMCEGE)** · MC `1.12.2` · version `1.1.0-beta`

> Language / 语言: **English** below · [跳转到中文](#中文)

MMCEGE is an addon for **Modular Machinery: Community Edition** (the KasumiNova fork). It bundles four subsystems:

| Subsystem | What it gives you | Page |
|---|---|---|
| Controller GUI | Resizable, texture-driven, multi-panel controller GUIs (replace MMCE machine/factory controller screens) | [Controller GUI](Controller-GUI-EN) |
| Custom Hatches | Define new fluid/gas/item/energy hatch **blocks** from JSON, no code | [Custom Hatches](Custom-Hatches-EN) |
| Custom AE2 Buses | ME item input + mixed (item+fluid+gas) input/output buses, from JSON | [Custom AE2 Buses](Custom-AE-Buses-EN) |
| Long-Capacity Requirements | Fluid/gas recipe amounts beyond the vanilla `int` limit (~2.1 billion mB) | [Long-Capacity Requirements](Long-Capacity-Requirements-EN) |

### Dependencies

- **Required**: `modularmachinery` (CE), `appliedenergistics2` (AE2 Extended Life), `mekanism`, `mekeng` (Mekanism Energistics)
- **Optional**: The One Probe, AE2 Fluid Crafting Rework, GregTech CE, HEI/JEI, GeckoLib

### Config directories MMCEGE reads at startup

| Path | Purpose |
|---|---|
| `config/mmceguiext/client.cfg` | Global client config |
| `config/modularmachinery/machinery/*.json` (`mmce_gui_ext` node) | Per-machine controller GUI override |
| `config/mmceguiext/custom_hatches/*.json` | Custom hatch definitions |
| `config/mmceguiext/custom_ae_item_input_buses/*.json` | ME item input bus definitions |
| `config/mmceguiext/custom_ae_mixed_input_buses/*.json` | Mixed input bus definitions |
| `config/mmceguiext/custom_ae_mixed_output_buses/*.json` | Mixed output bus definitions |

> Hatch / bus JSON is scanned **at game start** — edits require a restart, not `/ct reload`. Controller GUI machine JSON reloads more eagerly; ZS lines can be `/ct reload`ed.

### Build

```powershell
.\mmce-src\gradlew.bat -p .\mmce-gui-ext build
# -> mmce-gui-ext/build/libs/MMCEGE-1.1.0-beta.jar
```

---

<a name="中文"></a>
# 中文

**Modular Machinery: Community Edition — GUI Edit (MMCEGE)** · MC `1.12.2` · 版本 `1.1.0-beta`

MMCEGE 是 **Modular Machinery: Community Edition**（KasumiNova 分支）的附属模组，包含四个子系统：

| 子系统 | 作用 | 页面 |
|---|---|---|
| 控制器 GUI | 可调整大小、贴图驱动、多信息区的控制器 GUI（替换 MMCE 普通/集成控制器界面） | [控制器 GUI](Controller-GUI-ZH) |
| 自定义仓口 | 用 JSON 定义新的流体/气体/物品/能量仓口**方块**，无需写代码 | [自定义仓口](Custom-Hatches-ZH) |
| 自定义 AE2 总线 | ME 物品输入 + 混合（物品+流体+气体）输入/输出总线，由 JSON 定义 | [自定义 AE2 总线](Custom-AE-Buses-ZH) |
| Long 容量配方需求 | 流体/气体配方量突破原版 `int` 上限（约 21 亿 mB） | [Long 容量配方需求](Long-Capacity-Requirements-ZH) |

### 依赖

- **必需**：`modularmachinery`（CE）、`appliedenergistics2`（AE2 Extended Life）、`mekanism`、`mekeng`（Mekanism Energistics）
- **可选**：The One Probe、AE2 Fluid Crafting Rework、GregTech CE、HEI/JEI、GeckoLib

### MMCEGE 启动时读取的配置目录

| 路径 | 用途 |
|---|---|
| `config/mmceguiext/client.cfg` | 全局客户端配置 |
| `config/modularmachinery/machinery/*.json`（`mmce_gui_ext` 节点） | 机器级控制器 GUI 覆盖 |
| `config/mmceguiext/custom_hatches/*.json` | 自定义仓口定义 |
| `config/mmceguiext/custom_ae_item_input_buses/*.json` | ME 物品输入总线定义 |
| `config/mmceguiext/custom_ae_mixed_input_buses/*.json` | 混合输入总线定义 |
| `config/mmceguiext/custom_ae_mixed_output_buses/*.json` | 混合输出总线定义 |

> 仓口/总线 JSON 在**游戏启动时**扫描——修改后需重启，`/ct reload` 无效。控制器 GUI 的机器 JSON 重载更积极；ZS 行可 `/ct reload`。

### 构建

```powershell
.\mmce-src\gradlew.bat -p .\mmce-gui-ext build
# -> mmce-gui-ext/build/libs/MMCEGE-1.1.0-beta.jar
```
