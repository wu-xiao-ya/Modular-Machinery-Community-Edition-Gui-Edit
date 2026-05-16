#loader crafttweaker reloadable

import crafttweaker.data.IData;
import mods.modularmachinery.MMEvents;
import mods.modularmachinery.FactoryRecipeThread;
import mods.modularmachinery.MachineModifier;
import mods.modularmachinery.MachineTickEvent;
import mods.modularmachinery.ControllerGUIRenderEvent;
import mods.modularmachinery.SmartInterfaceType;

val DEMO_MACHINE = "mmcege_full_feature_demo_101";

function clampInt(v as int, min as int, max as int) as int {
    if (v < min) return min;
    if (v > max) return max;
    return v;
}

function readVirtualOrNativePort(ctrl, data as IData, key as string, min as int, max as int, defaultValue as int) as int {
    var smartData = ctrl.getSmartInterfaceData(key);
    if (isNull(smartData) == false) {
        val nativeValue = clampInt(smartData.value as int, min, max);
        smartData.value = nativeValue;
        data.asMap()[key] = nativeValue;
        return nativeValue;
    }

    var value = data.getFloat(key, defaultValue as float) as int;
    if (value < min || value > max) {
        value = data.getInt(key, defaultValue);
    }
    value = clampInt(value, min, max);
    data.asMap()[key] = value;
    return value;
}

MMEvents.onMachinePostTick(DEMO_MACHINE, function(event as MachineTickEvent) {
    val ctrl = event.controller;
    val data = ctrl.customData;

    val portMain = readVirtualOrNativePort(ctrl, data, "demo_port_main", 0, 99, 12);
    val portHiddenA = readVirtualOrNativePort(ctrl, data, "demo_port_hidden_a", 0, 99, 5);
    val portHiddenB = readVirtualOrNativePort(ctrl, data, "demo_port_hidden_b", 0, 99, 7);
    val portQuick = readVirtualOrNativePort(ctrl, data, "demo_port_quick", 0, 99, 9);

    data.asMap()["demo_total"] = portMain + portHiddenA + portHiddenB + portQuick;
    ctrl.customData = data;
});

MMEvents.onControllerGUIRender(DEMO_MACHINE, function(event as ControllerGUIRenderEvent) {
    val ctrl = event.controller;
    val data = ctrl.customData;

    val portMain = readVirtualOrNativePort(ctrl, data, "demo_port_main", 0, 99, 12);
    val portHiddenA = readVirtualOrNativePort(ctrl, data, "demo_port_hidden_a", 0, 99, 5);
    val portHiddenB = readVirtualOrNativePort(ctrl, data, "demo_port_hidden_b", 0, 99, 7);
    val portQuick = readVirtualOrNativePort(ctrl, data, "demo_port_quick", 0, 99, 9);
    val total = portMain + portHiddenA + portHiddenB + portQuick;
    ctrl.customData = data;

    // 4-phase animation cycle:
    // 1) move left, 2) scale up, 3) move right, 4) scale down.
    val tick = ctrl.ticksExisted % 200;
    var animX = 300;
    var animY = 186;
    var animScale = 1.0 as float;
    var animRotate = 0.0 as float;

    if (tick < 50) {
        val t = (tick as float) / 49.0;
        animX = (300.0 - 40.0 * t) as int;
        animScale = 1.0;
        animRotate = -15.0 * t;
    } else if (tick < 100) {
        val t = ((tick - 50) as float) / 49.0;
        animX = 260;
        animScale = 1.0 + 0.8 * t;
        animRotate = -15.0 + 105.0 * t;
    } else if (tick < 150) {
        val t = ((tick - 100) as float) / 49.0;
        animX = (260.0 + 40.0 * t) as int;
        animScale = 1.8;
        animRotate = 90.0 + 180.0 * t;
    } else {
        val t = ((tick - 150) as float) / 49.0;
        animX = 300;
        animScale = 1.8 - 0.8 * t;
        animRotate = 270.0 + 90.0 * t;
    }

    val hideKey = tick >= 100 && tick < 150;
    val hideTitle = tick >= 150;
    val keyDirective = hideKey ? "[mmcege:si.hide_key]" : "[mmcege:si.show_key]";
    val titleDirective = hideTitle ? "[mmcege:si.hide_title]" : "[mmcege:si.show_title]";
    val shadowVisibleText = ((tick % 40) < 30) ? "true" : "false";
    val dynamicPriority = tick < 100 ? 22 : 30;

    var info as string[] = [
        "[mmcege:si.title=Demo Port {index}/{count} ({key})]",
        keyDirective,
        titleDirective,
        "[panel:main]MMCEGE 1.0.1 full feature demo",
        "[panel:main]demo_port_main = " + portMain,
        "[panel:main]demo_port_hidden_a = " + portHiddenA,
        "[panel:main]demo_port_hidden_b = " + portHiddenB,
        "[panel:main]demo_port_quick = " + portQuick,
        "[panel:main]demo_total = " + total,
        "[panel:log]tick = " + tick,
        "[panel:log]animX = " + animX + " scale = " + animScale,
        "[panel:log]rotation = " + animRotate + " priority = " + dynamicPriority,
        "[panel:log]shadowVisible = " + shadowVisibleText,
        "[mmcege:layer.badge_shadow.visible=" + shadowVisibleText + "]",
        "[mmcege:layer.badge_shadow.x=" + (animX - 6) + "]",
        "[mmcege:layer.badge_shadow.y=" + (animY - 6) + "]",
        "[mmcege:layer.badge_shadow.priority=18]",
        "[mmcege:layer.badge_anim.x=" + animX + "]",
        "[mmcege:layer.badge_anim.y=" + animY + "]",
        "[mmcege:layer.badge_anim.scale=" + animScale + "]",
        "[mmcege:layer.badge_anim.rotation=" + animRotate + "]",
        "[mmcege:layer.badge_anim.priority=" + dynamicPriority + "]",
        "[mmcege:layer.badge_anim.visible=true]"
    ];
    event.extraInfo = info;
});

// Register SmartInterface keys so the machine can read values
// in MMCE-like style (ctrl.getSmartInterfaceData(...)).
MachineModifier.addSmartInterfaceType(DEMO_MACHINE,
    SmartInterfaceType.create("demo_port_main", 12)
        .setHeaderInfo("Main Port")
        .setValueInfo("Main: %.0f")
        .setJeiTooltip("Range: 0 ~ 99", 0)
);
MachineModifier.addSmartInterfaceType(DEMO_MACHINE,
    SmartInterfaceType.create("demo_port_hidden_a", 5)
        .setHeaderInfo("Hidden A")
        .setValueInfo("A: %.0f")
        .setJeiTooltip("Range: 0 ~ 99", 0)
);
MachineModifier.addSmartInterfaceType(DEMO_MACHINE,
    SmartInterfaceType.create("demo_port_hidden_b", 7)
        .setHeaderInfo("Hidden B")
        .setValueInfo("B: %.0f")
        .setJeiTooltip("Range: 0 ~ 99", 0)
);
MachineModifier.addSmartInterfaceType(DEMO_MACHINE,
    SmartInterfaceType.create("demo_port_quick", 9)
        .setHeaderInfo("Quick")
        .setValueInfo("Quick: %.0f")
        .setJeiTooltip("Range: 0 ~ 99", 0)
);

MachineModifier.setMaxThreads(DEMO_MACHINE, 0);
MachineModifier.addCoreThread(DEMO_MACHINE, FactoryRecipeThread.createCoreThread("MMCEGE Demo Thread"));
