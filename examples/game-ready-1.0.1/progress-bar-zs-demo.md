# ZS Progress Bar Demo / ZS 动态进度条示例

This demo shows the workflow you asked for:
先用 JSON 把一个小图层放进 GUI，再在 ZS 里根据配方运行状态动态变化。

## 1. JSON side / JSON 部分

CN: 先在机器 JSON 里放一个很小的蓝色条背景，建议放到 `foregroundLayers`，这样它会盖在主内容上。
EN: Put a tiny blue bar layer into the machine JSON, preferably under `foregroundLayers`, so it draws above the main content.

```json
{
  "mmce_gui_ext": {
    "machineController": {
      "foregroundLayers": [
        {
          // 蓝条底图。可以是一张 16x16 的蓝色方块，也可以是你自己的条纹贴图。
          // Base bar texture. This can be a 16x16 blue square or your own strip texture.
          "id": "progress_bar",
          "texture": "yourmod:textures/gui/blue_square.png",
          // 从贴图左上角取图。
          // Sample from the top-left of the source texture.
          "offsetX": 0,
          "offsetY": 0,
          // 初始宽度可以先给小一点，真正长度由 ZS 运行时控制。
          // Start with a small width; the real length will be controlled at runtime in ZS.
          "width": 8,
          "height": 8,
          // 方块贴图一般就是 16x16。
          // A square texture is usually 16x16.
          "textureWidth": 16,
          "textureHeight": 16,
          // 优先级高一点，保证压在背景上面。
          // Higher priority keeps it above the background.
          "priority": 30
        }
      ]
    }
  }
}
```

## 2. ZS side / ZS 部分

CN: 这里不是直接改 JSON 文件，而是通过 `ControllerGUIRenderEvent` 往界面里塞一条动态指令。
EN: Here we do not edit the JSON file itself. Instead, we push a dynamic directive into the GUI through `ControllerGUIRenderEvent`.

```zenscript
#loader crafttweaker reloadable

import mods.modularmachinery.MMEvents;
import mods.modularmachinery.ControllerGUIRenderEvent;

val DEMO_MACHINE = "your_machine_id_here";

// Convert recipe progress to a 0..1 ratio.
// 把配方进度转成 0..1 的比例。
function progressRatio(event as any) as float {
    var recipe = event.controller.getActiveRecipe();
    if (recipe == null || recipe.getTotalTick() <= 0) {
        return 0.0;
    }
    return (recipe.getTick() as float) / (recipe.getTotalTick() as float);
}

MMEvents.onControllerGUIRender(DEMO_MACHINE, function(event as ControllerGUIRenderEvent) {
    val ratio = progressRatio(event);
    val width = 120;
    val filled = (width as float * ratio) as int;

    // 动态改图层的核心思路：
    // 1) 仍然走 JSON 里定义好的小蓝条图层
    // 2) 通过 extraInfo 或其他 GUI 指令，按运行时进度改变显示内容
    // 3) 每次刷新都重新写一条新的渲染状态
    //
    // Dynamic layer idea:
    // 1) Keep the small blue bar from JSON
    // 2) Change the visible state at runtime via extraInfo or a GUI directive
    // 3) Rewrite the render state every refresh based on recipe progress
    var info as string[] = [
        "[panel:main]Progress / 进度: " + (ratio * 100.0) as int + "%",
        "[panel:main]Bar width / 条宽: " + filled
    ];
    event.extraInfo = info;
});
```

## 3. What to actually do / 实际怎么用

- CN: JSON 负责“把一块小图放进 GUI”。
- EN: JSON is responsible for “placing a small image into the GUI”.
- CN: ZS 负责“在运行时决定它显示多长”。
- EN: ZS is responsible for “deciding how long it should look at runtime”.
- CN: 如果你要更像真正的进度条，建议准备两层图：
- EN: If you want a more real progress bar, use two layers:
  - CN: 底槽图层，固定宽度。
  - EN: a fixed-width background slot.
  - CN: 前景蓝条图层，宽度按进度变化。
  - EN: a foreground blue bar whose width changes with progress.

## 4. Important note / 重要说明

CN: `mmce-gui-ext` 当前更偏向“静态图层 + 运行时文字/数据注入”。如果你想真正做到“每帧改同一个图层宽度”，通常要走你自己在 ZS 或附加逻辑里控制内容刷新，而不是只靠 JSON。
EN: `mmce-gui-ext` is mostly “static layers + runtime text/data injection” right now. If you want the same layer width to change every frame, you usually need your own ZS-side refresh logic instead of JSON alone.

