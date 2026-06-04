import mods.modularmachinery.ControllerButtonClickEvent;
import mods.modularmachinery.ControllerGUIRenderEvent;

val TEST_MACHINE = <modularmachinery:demo:event_button_test>;

function readFloatTag(ctrl as any, key as string) as float {
    val tag = ctrl.getCustomDataTag();
    if (tag == null || !tag.hasKey(key)) {
        return 0.0;
    }
    return tag.getFloat(key);
}

MMEvents.onControllerButtonClick(TEST_MACHINE, function(event as ControllerButtonClickEvent) {
    val ctrl = event.controller;
    if (event.buttonId == "event_test_button") {
        val current = readFloatTag(ctrl, "click_count");
        val tag = ctrl.getCustomDataTag();
        tag.setFloat("click_count", current + 1.0);
        ctrl.setCustomDataTag(tag);
        ctrl.markForUpdateSync();
    }
});

MMEvents.onControllerGUIRender(TEST_MACHINE, function(event as ControllerGUIRenderEvent) {
    val ctrl = event.controller;
    var info = event.extraInfo;
    if (info == null) {
        info = [] as string[];
    }
    info += "Event clicks: " + readFloatTag(ctrl, "click_count");
    info += "Pulse counter: " + readFloatTag(ctrl, "pulse_counter");
    event.extraInfo = info;
});
