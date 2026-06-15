#loader crafttweaker reloadable

import crafttweaker.data.IData;
import mods.modularmachinery.ControllerButtonClickEvent;
import mods.modularmachinery.ControllerGUIRenderEvent;
import mods.modularmachinery.MMEvents;

val BUTTON_TEST_MACHINE = "mmcege_controller_button_test";

function readFloat(data as IData, key as string) as float {
    return data.getFloat(key, 0.0);
}

MMEvents.onControllerButtonClick(BUTTON_TEST_MACHINE, function(event as ControllerButtonClickEvent) {
    if (event.buttonId != "event_ping") {
        return;
    }
    val ctrl = event.controller;
    val data = ctrl.customData;
    val count = readFloat(data, "event_ping_count") + 1.0;
    val status = count % 2.0 == 0.0 ? "done" : "working";
    data.asMap()["event_ping_count"] = count;
    data.asMap()["status"] = status;
    ctrl.customData = data;
    ctrl.markForUpdateSync();
});

MMEvents.onControllerGUIRender(BUTTON_TEST_MACHINE, function(event as ControllerGUIRenderEvent) {
    val data = event.controller.customData;
    var info as string[] = [
        "[panel:main]MMCEGE button test",
        "[panel:main]event_ping_count = " + readFloat(data, "event_ping_count"),
        "[panel:main]status = " + data.getString("status", "unset"),
        "[panel:settings]speed = " + readFloat(data, "speed"),
        "[panel:settings]status = " + data.getString("status", "unset"),
        "[panel:settings]Use -1 / +1 / 0 buttons to change speed."
    ];
    event.extraInfo = info;
});
