#loader crafttweaker reloadable

import crafttweaker.data.IData;
import mods.modularmachinery.MMEvents;
import mods.modularmachinery.FactoryRecipeThread;
import mods.modularmachinery.MachineModifier;
import mods.modularmachinery.MachineTickEvent;
import mods.modularmachinery.ControllerGUIRenderEvent;

// Demo machine id must match JSON "registryname"
// 示例机器 ID 必须与 JSON 的 "registryname" 一致
val DEMO_MACHINE = "mmcege_demo_multi_port";

function clampDemoLevel(v as int) as int {
    if (v < 1) return 1;
    if (v > 9) return 9;
    return v;
}

// Read from virtual DataPort values written by MMCEGE GUI editor.
// 从 MMCEGE GUI 编辑器写入的虚拟数据端口值读取
function readDemoPort(ctrl, data as IData, key as string) as int {
    var value = data.getFloat(key, 0.0) as int;
    if (value < 1 || value > 9) {
        value = data.getInt(key, 0);
    }
    if (value < 1 || value > 9) {
        return 0;
    }
    return clampDemoLevel(value);
}

MMEvents.onMachinePostTick(DEMO_MACHINE, function(event as MachineTickEvent) {
    val ctrl = event.controller;
    val data = ctrl.customData;

    val a = readDemoPort(ctrl, data, "demo_port_a");
    val b = readDemoPort(ctrl, data, "demo_port_b");
    val c = readDemoPort(ctrl, data, "demo_port_c");

    if (a > 0) data.asMap()["demo_level_a"] = a;
    if (b > 0) data.asMap()["demo_level_b"] = b;
    if (c > 0) data.asMap()["demo_level_c"] = c;

    ctrl.customData = data;
});

MMEvents.onControllerGUIRender(DEMO_MACHINE, function(event as ControllerGUIRenderEvent) {
    val ctrl = event.controller;
    val data = ctrl.customData;

    val a = readDemoPort(ctrl, data, "demo_port_a");
    val b = readDemoPort(ctrl, data, "demo_port_b");
    val c = readDemoPort(ctrl, data, "demo_port_c");

    // SmartInterface editor directives (new feature):
    // [mmcege:si.hide_key]   -> hide bottom key/value text
    // [mmcege:si.hide_title] -> hide top title
    // [mmcege:si.title=...]  -> custom top title, placeholders {index} {count} {key}
    // SmartInterface 编辑器指令（新功能）：
    // [mmcege:si.hide_key]   -> 隐藏底部 key / value 文本
    // [mmcege:si.hide_title] -> 隐藏顶部标题
    // [mmcege:si.title=...]  -> 自定义顶部标题，可用占位符 {index} {count} {key}
    var info as string[] = [
        "[mmcege:si.title=Demo Port / 示例端口 {index}/{count} -> {key}]",
        // "[mmcege:si.hide_key]",
        // "[mmcege:si.hide_title]",
        "[panel:main]Demo virtual ports / 虚拟端口示例",
        "[panel:main]demo_port_a / 端口A = " + a,
        "[panel:main]demo_port_b / 端口B = " + b,
        "[panel:main]demo_port_c / 端口C = " + c
    ];
    event.extraInfo = info;
});

// Optional: if you need recipes/threads, register as usual.
// 可选：如果你需要配方/线程，按常规方式注册
MachineModifier.setMaxThreads(DEMO_MACHINE, 0);
MachineModifier.addCoreThread(DEMO_MACHINE, FactoryRecipeThread.createCoreThread("demo_thread"));