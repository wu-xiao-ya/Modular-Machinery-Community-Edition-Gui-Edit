# MMCE Default Machine Examples

This folder shows how to copy MMCE GUI extension config into common machine JSON files.

## What you can copy

- `background-only.json`
- `smart-interface-only.json`
- `factory-queue-only.json`
- `error-cases.json`

## What the original examples show

- `alloy_furnace` (`alloy_furnace.json`)
- `assembly_line` (`assembly_line.json`)
- `iron_centrifuge` (`iron_centrifuge.json`)
- `transformer` (file name `power_transformer.json`, registryname `transformer`)

对应机器：

- `alloy_furnace`（`alloy_furnace.json`）
- `assembly_line`（`assembly_line.json`）
- `iron_centrifuge`（`iron_centrifuge.json`）
- `transformer`（文件名是 `power_transformer.json`，registryname 是 `transformer`）

## File location

Generated under:

- `mmce-gui-ext/examples/default-machines-with-mmce-gui-ext/`

You will see JSON templates that keep the machine structure small and only add `mmce_gui_ext` fields.

## How to use

1. Back up your current `config/modularmachinery/machinery`.
2. Copy the needed JSON into `.minecraft/config/modularmachinery/machinery/`.
3. Start the game and check the GUI.

## Example fields

```json
"mmce_gui_ext": {
  "machineController": {
    "backgroundTexture": "modularmachinery:textures/gui/guicontroller_large.png",
    "hideDefaultBackground": false
  },
  "factoryController": {
    "backgroundTexture": "modularmachinery:textures/gui/guifactory.png",
    "hideDefaultBackground": false
  }
}
```

## When replacing your own textures

Only change `backgroundTexture`, for example:

- `yourmod:textures/gui/your_machine_controller.png`
- `yourmod:textures/gui/your_factory_controller.png`

If you want custom texture only, set `hideDefaultBackground` to `true`.
