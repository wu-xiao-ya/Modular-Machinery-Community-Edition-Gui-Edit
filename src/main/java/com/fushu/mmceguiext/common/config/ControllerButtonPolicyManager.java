package com.fushu.mmceguiext.common.config;

import com.fushu.mmceguiext.MMCEGuiExt;
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
    private static final String MACHINERY_DIR = "modularmachinery/machinery";
    private static final int MAX_BUTTONS_PER_CONTROLLER = 256;
    private static final int MAX_STRING_LENGTH = 128;
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);

    private static final Object LOCK = new Object();
    private static final Map<String, List<ButtonPolicy>> MACHINE_BUTTONS = new HashMap<String, List<ButtonPolicy>>();
    private static final Map<String, List<ButtonPolicy>> FACTORY_BUTTONS = new HashMap<String, List<ButtonPolicy>>();
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
            if (floatsMatch(policy.value, value)) {
                return policy;
            }
        }
        return null;
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

        Path machineryDir = Loader.instance().getConfigDir().toPath().resolve(MACHINERY_DIR);
        if (!Files.exists(machineryDir)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(machineryDir)) {
            stream
                .filter(Files::isRegularFile)
                .filter(ControllerButtonPolicyManager::isMachineJson)
                .forEach(ControllerButtonPolicyManager::loadMachineJson);
        } catch (IOException ex) {
            LOGGER.warn("Failed to scan MMCE GUI ext button policies under {}: {}", machineryDir, ex.getMessage());
        }
    }

    private static boolean isMachineJson(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".json") && !name.endsWith(".var.json");
    }

    private static void loadMachineJson(Path path) {
        try {
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
            putButtons(MACHINE_BUTTONS, namespacedKey, pathKey, allowPathFallback,
                parseButtons(getObject(extNode, "machineController", "machine_controller", "machine")));
            putButtons(FACTORY_BUTTONS, namespacedKey, pathKey, allowPathFallback,
                parseButtons(getObject(extNode, "factoryController", "factory_controller", "factory")));
        } catch (Exception ex) {
            LOGGER.warn("Failed to read MMCE GUI ext button policy {}: {}", path, ex.getMessage());
        }
    }

    private static void putButtons(
        Map<String, List<ButtonPolicy>> target,
        String namespacedKey,
        String pathKey,
        boolean allowPathFallback,
        List<ButtonPolicy> buttons
    ) {
        if (buttons.isEmpty()) {
            return;
        }
        target.put(namespacedKey, buttons);
        if (allowPathFallback) {
            target.put(pathKey, buttons);
        }
    }

    private static List<ButtonPolicy> parseButtons(@Nullable JsonObject controllerNode) {
        if (controllerNode == null) {
            return java.util.Collections.emptyList();
        }
        JsonElement buttonsElement = getElement(controllerNode, "buttons", "buttonList", "button_list", "guiButtons", "gui_buttons");
        if (buttonsElement == null || !buttonsElement.isJsonArray()) {
            return java.util.Collections.emptyList();
        }

        List<ButtonPolicy> out = new ArrayList<ButtonPolicy>();
        JsonArray buttons = buttonsElement.getAsJsonArray();
        int limit = Math.min(buttons.size(), MAX_BUTTONS_PER_CONTROLLER);
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
        return out;
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
        policy.key = getString(obj, "key", "virtualKey", "virtual_key", "interfaceType", "interface_type");
        Float value = getFloat(obj, "value", "delta", "amount");
        policy.value = value == null ? ("smart_add".equals(action) ? 1.0F : 0.0F) : value.floatValue();
        policy.min = getFloat(obj, "min", "minimum");
        policy.max = getFloat(obj, "max", "maximum");

        if ("event".equals(action)) {
            return policy.buttonId == null ? null : policy;
        }
        if (policy.key == null || !Float.isFinite(policy.value)) {
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
        if ("smart_set".equals(text) || "smartset".equals(text) || "set".equals(text)) {
            return "smart_set";
        }
        if ("smart_add".equals(text) || "smartadd".equals(text) || "add".equals(text)
            || "increment".equals(text) || "inc".equals(text)) {
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
        public float value;
        @Nullable
        public Float min;
        @Nullable
        public Float max;

        private ButtonPolicy(String action) {
            this.action = action;
        }
    }
}
