package com.fushu.mmceguiext.common.config;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class ControllerButtonPolicyManager {
    private static final long RELOAD_INTERVAL_MS = 5000L;
    private static final long MAX_MACHINE_CONFIG_BYTES = 1024L * 1024L;
    private static final String MACHINERY_DIR = "modularmachinery/machinery";
    private static final String SUBGUI_DIR = "mmceguiext/subgui";
    private static final int MAX_BUTTONS_PER_CONTROLLER = 256;
    private static final int MAX_SMART_EDITOR_KEYS_PER_CONTROLLER = 256;
    private static final int MAX_STRING_LENGTH = 128;
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);

    private static final Object LOCK = new Object();
    private static final Map<String, List<ButtonPolicy>> MACHINE_BUTTONS = new HashMap<String, List<ButtonPolicy>>();
    private static final Map<String, List<ButtonPolicy>> FACTORY_BUTTONS = new HashMap<String, List<ButtonPolicy>>();
    private static final Map<String, List<String>> MACHINE_EDITOR_KEYS = new HashMap<String, List<String>>();
    private static final Map<String, List<String>> FACTORY_EDITOR_KEYS = new HashMap<String, List<String>>();
    private static long lastLoadTime = 0L;

    private ControllerButtonPolicyManager() {
    }

    @Nullable
    public static ButtonPolicy matchEvent(TileMultiblockMachineController controller, String buttonId) {
        String normalizedButtonId = normalize(buttonId);
        if (normalizedButtonId == null) {
            return null;
        }
        for (ButtonPolicy policy : resolve(controller)) {
            if ("event".equals(policy.action) && normalizedButtonId.equals(policy.buttonId)) {
                return policy;
            }
        }
        return null;
    }

    @Nullable
    public static ButtonPolicy matchSmart(TileMultiblockMachineController controller, byte kind, String key, float value) {
        String normalizedAction = kind == 1 ? "smart_add" : kind == 0 ? "smart_set" : null;
        String normalizedKey = normalize(key);
        if (normalizedAction == null || normalizedKey == null || !Float.isFinite(value)) {
            return null;
        }
        for (ButtonPolicy policy : resolve(controller)) {
            if (!normalizedAction.equals(policy.action) || !normalizedKey.equals(policy.key)) {
                continue;
            }
            if (policy.value != null && floatsMatch(policy.value.floatValue(), value)) {
                return policy;
            }
            for (Float alt : policy.altValues) {
                if (alt != null && floatsMatch(alt.floatValue(), value)) {
                    return policy;
                }
            }
        }
        return null;
    }

    private static void addAltValue(ButtonPolicy policy, @Nullable Float value) {
        if (value != null && Float.isFinite(value.floatValue())) {
            policy.altValues.add(value);
        }
    }

    @Nullable
    public static ButtonPolicy matchSmart(TileMultiblockMachineController controller, byte kind, String key, String value) {
        String normalizedAction = kind == 1 ? "smart_add" : kind == 0 ? "smart_set" : null;
        String normalizedKey = normalize(key);
        if (!"smart_set".equals(normalizedAction) || normalizedKey == null || value == null) {
            return null;
        }
        for (ButtonPolicy policy : resolve(controller)) {
            if (!normalizedAction.equals(policy.action) || !normalizedKey.equals(policy.key)) {
                continue;
            }
            if (policy.stringValue != null && policy.stringValue.equals(value)) {
                return policy;
            }
        }
        return null;
    }

    public static boolean isConfiguredSmartKey(TileMultiblockMachineController controller, String key) {
        String normalizedKey = normalize(key);
        if (normalizedKey == null) {
            return false;
        }
        for (ButtonPolicy policy : resolve(controller)) {
            if (normalizedKey.equals(policy.key)) {
                return true;
            }
        }
        return resolveEditorKeys(controller).contains(normalizedKey);
    }

    private static List<ButtonPolicy> resolve(TileMultiblockMachineController controller) {
        ensureLoaded();
        if (controller == null) {
            return java.util.Collections.emptyList();
        }
        DynamicMachine machine = controller.getFoundMachine();
        if (machine == null) {
            machine = controller.getBlueprintMachine();
        }
        if (machine == null || machine.getRegistryName() == null) {
            return java.util.Collections.emptyList();
        }

        Map<String, List<ButtonPolicy>> source = controller instanceof TileFactoryController ? FACTORY_BUTTONS : MACHINE_BUTTONS;
        String fullKey = machine.getRegistryName().toString().toLowerCase(Locale.ROOT);
        List<ButtonPolicy> fullMatch = source.get(fullKey);
        if (fullMatch != null) {
            return fullMatch;
        }
        List<ButtonPolicy> pathMatch = source.get(machine.getRegistryName().getPath().toLowerCase(Locale.ROOT));
        return pathMatch == null ? java.util.Collections.emptyList() : pathMatch;
    }

    private static List<String> resolveEditorKeys(TileMultiblockMachineController controller) {
        ensureLoaded();
        if (controller == null) {
            return java.util.Collections.emptyList();
        }
        DynamicMachine machine = controller.getFoundMachine();
        if (machine == null) {
            machine = controller.getBlueprintMachine();
        }
        if (machine == null || machine.getRegistryName() == null) {
            return configuredFallbackEditorKeys(controller);
        }

        Map<String, List<String>> source = controller instanceof TileFactoryController ? FACTORY_EDITOR_KEYS : MACHINE_EDITOR_KEYS;
        String fullKey = machine.getRegistryName().toString().toLowerCase(Locale.ROOT);
        List<String> fullMatch = source.get(fullKey);
        if (fullMatch != null) {
            return fullMatch;
        }
        List<String> pathMatch = source.get(machine.getRegistryName().getPath().toLowerCase(Locale.ROOT));
        return pathMatch == null ? configuredFallbackEditorKeys(controller) : pathMatch;
    }

    private static void ensureLoaded() {
        long now = System.currentTimeMillis();
        if (now - lastLoadTime < RELOAD_INTERVAL_MS) {
            return;
        }
        synchronized (LOCK) {
            if (now - lastLoadTime < RELOAD_INTERVAL_MS) {
                return;
            }
            reload();
            lastLoadTime = now;
        }
    }

    private static void reload() {
        MACHINE_BUTTONS.clear();
        FACTORY_BUTTONS.clear();
        MACHINE_EDITOR_KEYS.clear();
        FACTORY_EDITOR_KEYS.clear();

        Path configDir = Loader.instance().getConfigDir().toPath();
        scanPolicyDir(configDir.resolve(MACHINERY_DIR));
        scanPolicyDir(configDir.resolve(SUBGUI_DIR));
    }

    private static void scanPolicyDir(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(dir)) {
            stream
                .filter(Files::isRegularFile)
                .filter(ControllerButtonPolicyManager::isMachineJson)
                .forEach(ControllerButtonPolicyManager::loadMachineJson);
        } catch (IOException ex) {
            LOGGER.warn("Failed to scan MMCE GUI ext button policies under {}: {}", dir, ex.getMessage());
        }
    }

    private static boolean isMachineJson(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".json") && !name.endsWith(".var.json");
    }

    private static void loadMachineJson(Path path) {
        try {
            if (Files.size(path) > MAX_MACHINE_CONFIG_BYTES) {
                LOGGER.warn("Skipping MMCE GUI ext button policy {} because it is larger than {} bytes.", path, MAX_MACHINE_CONFIG_BYTES);
                return;
            }
            JsonElement rootElement = new JsonParser().parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
            if (rootElement == null || !rootElement.isJsonObject()) {
                return;
            }
            JsonObject root = rootElement.getAsJsonObject();
            String registryName = getString(root, "registryname", "registryName");
            if (registryName == null) {
                return;
            }
            String normalizedRegistryName = registryName.toLowerCase(Locale.ROOT);
            String namespacedKey = normalizedRegistryName.contains(":")
                ? normalizedRegistryName
                : "modularmachinery:" + normalizedRegistryName;
            String pathKey = namespacedKey.substring(namespacedKey.indexOf(':') + 1);
            boolean allowPathFallback = !normalizedRegistryName.contains(":");

            JsonObject extNode = getObject(root, "mmceGuiExt", "mmce_gui_ext", "mmce-gui-ext");
            if (extNode == null) {
                return;
            }
            JsonObject machineNode = getObject(extNode, "machineController", "machine_controller", "machine");
            JsonObject factoryNode = getObject(extNode, "factoryController", "factory_controller", "factory");
            mergeButtons(MACHINE_BUTTONS, namespacedKey, pathKey, allowPathFallback, parseButtons(machineNode));
            mergeButtons(FACTORY_BUTTONS, namespacedKey, pathKey, allowPathFallback, parseButtons(factoryNode));
            mergeEditorKeys(MACHINE_EDITOR_KEYS, namespacedKey, pathKey, allowPathFallback, parseEditorKeys(machineNode));
            mergeEditorKeys(FACTORY_EDITOR_KEYS, namespacedKey, pathKey, allowPathFallback, parseEditorKeys(factoryNode));
        } catch (Exception ex) {
            LOGGER.warn("Failed to read MMCE GUI ext button policy {}: {}", path, ex.getMessage());
        }
    }

    private static void mergeButtons(
        Map<String, List<ButtonPolicy>> target,
        String namespacedKey,
        String pathKey,
        boolean allowPathFallback,
        List<ButtonPolicy> buttons
    ) {
        if (buttons.isEmpty()) {
            return;
        }
        mergeButtonList(target, namespacedKey, buttons);
        if (allowPathFallback) {
            mergeButtonList(target, pathKey, buttons);
        }
    }

    private static void mergeEditorKeys(
        Map<String, List<String>> target,
        String namespacedKey,
        String pathKey,
        boolean allowPathFallback,
        List<String> keys
    ) {
        if (keys.isEmpty()) {
            return;
        }
        mergeStringList(target, namespacedKey, keys);
        if (allowPathFallback) {
            mergeStringList(target, pathKey, keys);
        }
    }

    private static void mergeButtonList(Map<String, List<ButtonPolicy>> target, String key, List<ButtonPolicy> buttons) {
        List<ButtonPolicy> existing = target.get(key);
        if (existing == null) {
            List<ButtonPolicy> copy = new ArrayList<ButtonPolicy>();
            for (ButtonPolicy button : buttons) {
                if (copy.size() >= MAX_BUTTONS_PER_CONTROLLER) {
                    break;
                }
                copy.add(button);
            }
            target.put(key, copy);
            return;
        }
        for (ButtonPolicy button : buttons) {
            if (existing.size() >= MAX_BUTTONS_PER_CONTROLLER) {
                break;
            }
            existing.add(button);
        }
    }

    private static void mergeStringList(Map<String, List<String>> target, String key, List<String> values) {
        List<String> existing = target.get(key);
        if (existing == null) {
            target.put(key, new ArrayList<String>(values));
            return;
        }
        for (String value : values) {
            if (existing.size() >= MAX_SMART_EDITOR_KEYS_PER_CONTROLLER) {
                break;
            }
            if (!existing.contains(value)) {
                existing.add(value);
            }
        }
    }

    private static List<ButtonPolicy> parseButtons(@Nullable JsonObject controllerNode) {
        if (controllerNode == null) {
            return java.util.Collections.emptyList();
        }
        List<ButtonPolicy> out = new ArrayList<ButtonPolicy>();
        collectButtons(controllerNode, out);
        return out;
    }

    private static List<String> parseEditorKeys(@Nullable JsonObject controllerNode) {
        if (controllerNode == null) {
            return java.util.Collections.emptyList();
        }
        List<String> out = new ArrayList<String>();
        collectEditorKeys(controllerNode, out);
        return out.isEmpty() ? java.util.Collections.emptyList() : out;
    }

    private static void collectButtons(JsonObject controllerNode, List<ButtonPolicy> out) {
        if (controllerNode == null || out.size() >= MAX_BUTTONS_PER_CONTROLLER) {
            return;
        }
        JsonElement buttonsElement = getElement(controllerNode, "buttons", "buttonList", "button_list", "guiButtons", "gui_buttons");
        if (buttonsElement != null && buttonsElement.isJsonArray()) {
            JsonArray buttons = buttonsElement.getAsJsonArray();
            int limit = Math.min(buttons.size(), MAX_BUTTONS_PER_CONTROLLER - out.size());
            for (int i = 0; i < limit; i++) {
                JsonElement child = buttons.get(i);
                if (child == null || !child.isJsonObject()) {
                    continue;
                }
                ButtonPolicy policy = parseButton(child.getAsJsonObject());
                if (policy != null) {
                    out.add(policy);
                }
            }
        }
        if (out.size() >= MAX_BUTTONS_PER_CONTROLLER) {
            return;
        }
        JsonElement subGuisElement = getElement(controllerNode, "subGuis", "sub_guis", "subGui", "sub_gui");
        if (subGuisElement == null || !subGuisElement.isJsonArray()) {
            return;
        }
        JsonArray subGuis = subGuisElement.getAsJsonArray();
        int limit = Math.min(subGuis.size(), MAX_BUTTONS_PER_CONTROLLER - out.size());
        for (int i = 0; i < limit; i++) {
            JsonElement child = subGuis.get(i);
            if (child == null || !child.isJsonObject()) {
                continue;
            }
            JsonObject subGuiNode = child.getAsJsonObject();
            collectButtons(subGuiNode, out);
            if (out.size() >= MAX_BUTTONS_PER_CONTROLLER) {
                break;
            }
        }
    }

    private static void collectEditorKeys(JsonObject controllerNode, List<String> out) {
        if (controllerNode == null || out.size() >= MAX_SMART_EDITOR_KEYS_PER_CONTROLLER) {
            return;
        }
        addVirtualKeys(out, getString(controllerNode,
            "smartInterfaceEditorVirtualKey",
            "smart_interface_editor_virtual_key",
            "dataPortEditorVirtualKey",
            "data_port_editor_virtual_key",
            "virtualDataPortKey",
            "virtual_data_port_key"
        ));

        JsonElement editorsElement = getElement(controllerNode,
            "smartInterfaceEditors",
            "smart_interface_editors",
            "dataPortEditors",
            "data_port_editors"
        );
        if (editorsElement != null && editorsElement.isJsonArray()) {
            JsonArray editors = editorsElement.getAsJsonArray();
            int limit = Math.min(editors.size(), MAX_SMART_EDITOR_KEYS_PER_CONTROLLER - out.size());
            for (int i = 0; i < limit; i++) {
                JsonElement child = editors.get(i);
                if (child == null || !child.isJsonObject()) {
                    continue;
                }
                addVirtualKeys(out, getString(child.getAsJsonObject(), "virtualKey", "virtual_key", "key", "type"));
            }
        }
        if (out.size() >= MAX_SMART_EDITOR_KEYS_PER_CONTROLLER) {
            return;
        }
        JsonElement slidersElement = getElement(controllerNode, "sliders", "guiSliders", "gui_sliders", "rangeControls", "range_controls");
        if (slidersElement != null && slidersElement.isJsonArray()) {
            JsonArray sliders = slidersElement.getAsJsonArray();
            int limit = Math.min(sliders.size(), MAX_SMART_EDITOR_KEYS_PER_CONTROLLER - out.size());
            for (int i = 0; i < limit; i++) {
                JsonElement child = sliders.get(i);
                if (child == null || !child.isJsonObject()) {
                    continue;
                }
                addVirtualKeys(out, getString(
                    child.getAsJsonObject(),
                    "key",
                    "virtualKey",
                    "virtual_key",
                    "interfaceType",
                    "interface_type",
                    "dataPortKey",
                    "data_port_key",
                    "portKey",
                    "port_key",
                    "dataPort",
                    "data_port"
                ));
            }
        }
        if (out.size() >= MAX_SMART_EDITOR_KEYS_PER_CONTROLLER) {
            return;
        }
        JsonElement subGuisElement = getElement(controllerNode, "subGuis", "sub_guis", "subGui", "sub_gui");
        if (subGuisElement == null || !subGuisElement.isJsonArray()) {
            return;
        }
        JsonArray subGuis = subGuisElement.getAsJsonArray();
        int limit = Math.min(subGuis.size(), MAX_SMART_EDITOR_KEYS_PER_CONTROLLER - out.size());
        for (int i = 0; i < limit; i++) {
            JsonElement child = subGuis.get(i);
            if (child == null || !child.isJsonObject()) {
                continue;
            }
            collectEditorKeys(child.getAsJsonObject(), out);
            if (out.size() >= MAX_SMART_EDITOR_KEYS_PER_CONTROLLER) {
                break;
            }
        }
    }

    private static void addVirtualKeys(List<String> out, @Nullable String raw) {
        if (raw == null) {
            return;
        }
        String[] split = raw.split("[,;\\r\\n，；]");
        for (String value : split) {
            String key = normalize(value);
            if (key != null && !out.contains(key) && out.size() < MAX_SMART_EDITOR_KEYS_PER_CONTROLLER) {
                out.add(key);
            }
        }
    }

    private static List<String> configuredFallbackEditorKeys(TileMultiblockMachineController controller) {
        String raw = controller instanceof TileFactoryController
            ? MMCEGuiExtConfig.factoryController.smartInterfaceEditorVirtualKey
            : MMCEGuiExtConfig.machineController.smartInterfaceEditorVirtualKey;
        List<String> out = new ArrayList<String>();
        addVirtualKeys(out, raw);
        return out.isEmpty() ? java.util.Collections.emptyList() : out;
    }

    @Nullable
    private static ButtonPolicy parseButton(JsonObject obj) {
        Boolean visible = getBoolean(obj, "visible", "show", "enabled");
        if (visible != null && !visible.booleanValue()) {
            return null;
        }

        String action = normalizeAction(getString(obj, "action", "mode", "type"));
        if (action == null) {
            String targetPage = getString(obj, "targetPage", "target_page", "pageTarget", "page_target");
            action = targetPage == null ? null : "page";
        }
        if (!"event".equals(action) && !"smart_set".equals(action) && !"smart_add".equals(action)) {
            return null;
        }

        ButtonPolicy policy = new ButtonPolicy(action);
        policy.buttonId = getString(obj, "buttonId", "button_id", "eventId", "event_id", "id", "name");
        policy.key = getString(
            obj,
            "key",
            "virtualKey",
            "virtual_key",
            "interfaceType",
            "interface_type",
            "dataPortKey",
            "data_port_key",
            "portKey",
            "port_key",
            "dataPort",
            "data_port"
        );
        Float value = getFloat(obj, "value", "delta", "amount");
        String stringValue = getStringAllowNonString(obj, "value", "textValue", "text_value", "stringValue", "string_value");
        if (value != null) {
            policy.value = value;
        } else if ("smart_add".equals(action)) {
            policy.value = Float.valueOf(1.0F);
        } else if ("smart_set".equals(action) && stringValue == null) {
            policy.value = Float.valueOf(0.0F);
        } else {
            policy.value = null;
        }
        policy.stringValue = value == null && "smart_set".equals(action) ? stringValue : null;
        if (policy.value != null) {
            // Register modifier-key variant values so the server accepts them too.
            addAltValue(policy, getFloat(obj, "shiftValue", "shift_value", "shiftDelta", "shift_delta"));
            addAltValue(policy, getFloat(obj, "ctrlValue", "ctrl_value", "controlValue", "control_value"));
            addAltValue(policy, getFloat(obj, "ctrlShiftValue", "ctrl_shift_value",
                "shiftCtrlValue", "shift_ctrl_value", "ctrlShiftDelta", "ctrl_shift_delta"));
        }
        policy.min = getFloat(obj, "min", "minimum");
        policy.max = getFloat(obj, "max", "maximum");

        if ("event".equals(action)) {
            return policy.buttonId == null ? null : policy;
        }
        if (policy.key == null) {
            return null;
        }
        if ("smart_add".equals(action) && (policy.value == null || !Float.isFinite(policy.value.floatValue()))) {
            return null;
        }
        if ("smart_set".equals(action) && policy.value == null && policy.stringValue == null) {
            return null;
        }
        return policy;
    }

    @Nullable
    private static String normalizeAction(@Nullable String raw) {
        String text = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return null;
        }
        if ("event".equals(text) || "click_event".equals(text) || "button_event".equals(text)) {
            return "event";
        }
        if ("page".equals(text) || "switch_state".equals(text) || "set_state".equals(text)
            || "subgui".equals(text) || "sub_gui".equals(text)
            || "close_subgui".equals(text) || "close_sub_gui".equals(text)) {
            return "page";
        }
        if ("smart_set".equals(text) || "smartset".equals(text) || "set".equals(text)
            || "data_set".equals(text) || "data-port-set".equals(text) || "data_port_set".equals(text)
            || "port_set".equals(text) || "port-set".equals(text)) {
            return "smart_set";
        }
        if ("smart_add".equals(text) || "smartadd".equals(text) || "add".equals(text)
            || "increment".equals(text) || "inc".equals(text)
            || "data_add".equals(text) || "data-port-add".equals(text) || "data_port_add".equals(text)
            || "port_add".equals(text) || "port-add".equals(text)) {
            return "smart_add";
        }
        return null;
    }

    @Nullable
    private static JsonObject getObject(JsonObject obj, String... keys) {
        JsonElement element = getElement(obj, keys);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    @Nullable
    private static JsonElement getElement(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key)) {
                return obj.get(key);
            }
        }
        return null;
    }

    @Nullable
    private static String getString(JsonObject obj, String... keys) {
        JsonElement element = getElement(obj, keys);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return null;
        }
        String value = normalize(element.getAsString());
        return value == null ? null : value;
    }

    @Nullable
    private static String getStringAllowNonString(JsonObject obj, String... keys) {
        JsonElement element = getElement(obj, keys);
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        try {
            String value = element.getAsString();
            return value == null || value.length() > MAX_STRING_LENGTH ? null : value;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static Float getFloat(JsonObject obj, String... keys) {
        JsonElement element = getElement(obj, keys);
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        try {
            float value = element.getAsFloat();
            return Float.isFinite(value) ? Float.valueOf(value) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static Boolean getBoolean(JsonObject obj, String... keys) {
        JsonElement element = getElement(obj, keys);
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        try {
            if (element.getAsJsonPrimitive().isBoolean()) {
                return Boolean.valueOf(element.getAsBoolean());
            }
            String value = element.getAsString();
            if ("true".equalsIgnoreCase(value)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(value)) {
                return Boolean.FALSE;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Nullable
    private static String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || trimmed.length() > MAX_STRING_LENGTH ? null : trimmed;
    }

    private static boolean floatsMatch(float expected, float actual) {
        return Float.isFinite(expected) && Float.isFinite(actual) && Math.abs(expected - actual) <= 0.0001F;
    }

    public static final class ButtonPolicy {
        public final String action;
        @Nullable
        public String buttonId;
        @Nullable
        public String key;
        @Nullable
        public Float value;
        // Extra numeric values allowed for this button via modifier-key clicks (shift/ctrl/ctrl+shift).
        public final List<Float> altValues = new ArrayList<Float>();
        @Nullable
        public String stringValue;
        @Nullable
        public Float min;
        @Nullable
        public Float max;

        private ButtonPolicy(String action) {
            this.action = action;
        }
    }
}
