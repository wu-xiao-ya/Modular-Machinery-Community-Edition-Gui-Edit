# Long 容量配方需求

[首页](Home) · [English](Long-Capacity-Requirements-EN)

## 问题

MMCE 的 `RequirementFluid` / `RequirementGas` 用 Java `int` 存储配方 `amount`。`int` 超过 **2,147,483,647**（约 21 亿 mB）就会溢出。一旦你的机器和自定义仓口进入十亿/万亿 mB 级别，原版 MMCE 配方会悄然出错。

## MMCEGE 的做法

MMCEGE 通过 Mixin 改造需求系统，使流体/气体配方量以 **`long`** 解析与处理（上限约 920 京）。覆盖整个配方生命周期：

- 解析（`createRequirement`）
- “能否开始合成”判定
- 消耗输入（`startCrafting`）
- 产出填充（`finishCrafting`）
- 最大并行度计算

原版小数值配方不受影响——它们继续走原本的 `int` 路径。

## 如何使用

**无需任何配置，也不需要额外的模组节点。** 直接在普通的 MMCE 配方 JSON 里写大数值即可：

```json
{
  "type": "fluid",
  "io": "input",
  "fluid": "water",
  "amount": 5000000000
}
```

```json
{
  "type": "gas",
  "io": "output",
  "gas": "hydrogen",
  "amount": 8000000000
}
```

两者都会以 `long` 解析并运行。每 tick 的流体/气体需求同理。

## 它为何重要

正是这一改造，让 **long 容量的自定义仓口** 与 **AE2 混合总线** 能在配方中真正可用——它们的储罐以 `long` 存储数量，配方也必须能够无溢出地索取/产出对应的 `long` 数量。参见 [自定义仓口](Custom-Hatches-ZH) 与 [自定义 AE2 总线](Custom-AE-Buses-ZH)。

## 注意与限制

- 改造目标是 MMCE 的 `RequirementFluid`、`RequirementGas`、它们的需求**类型**（JSON 解析）以及 `RecipeCraftingContext` 的组件查找。
- 只有当绑定的 handler 支持时，需求才走 `long` 路径（自定义仓口/混合总线支持）。原版仓口的容量仍受 `int` 限制——配方索取的量超过原版仓口能容纳的上限时，该仓口就无法满足它。
- `FluidStack` / `GasStack` 本身仍是 `int` 基础；MMCEGE 额外维护一个并行的 `long` 值，仅在把数据交给原版 `int` API 时才向下截断。对配方作者透明，但调试时值得知道。

## 实现细节（贡献者向）

- `mixin/MixinRequirementTypeFluid` / `MixinRequirementTypeGas` —— `@Overwrite createRequirement`，用 `getAsLong()` 读 `amount`，保存完整 `long`。
- `mixin/MixinRequirementFluid` / `MixinRequirementGas` —— 向 canStart/start/finish/getMaxParallelism 注入 `long` 路径，委托 `common/requirement/LongRequirementIO`（含用于安全模拟的快照 handler）。
- `mixin/MixinRecipeCraftingContext` —— 当 MMCE 的组件查找返回空时，补回混合输入总线的内部 handler。
- `common/requirement/LongAmountRequirement` —— 在被改造的需求上携带 `long` 值的接口。
