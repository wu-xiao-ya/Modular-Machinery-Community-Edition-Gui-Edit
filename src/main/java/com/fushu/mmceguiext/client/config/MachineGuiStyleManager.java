package com.fushu.mmceguiext.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class MachineGuiStyleManager {
    private static final long RELOAD_INTERVAL_MS = 5000L;
    private static final String MACHINERY_DIR = "modularmachinery/machinery";

    private static final Object LOCK = new Object();

    private static long lastLoadTime = 0L;
    private static final Map<String, ControllerStyle> MACHINE_CONTROLLER_STYLES = new HashMap<String, ControllerStyle>();
    private static final Map<String, ControllerStyle> FACTORY_CONTROLLER_STYLES = new HashMap<String, ControllerStyle>();

    private MachineGuiStyleManager() {
    }

    public static ControllerStyle resolveMachineController(@Nullable DynamicMachine machine) {
        ensureLoaded();
        return resolve(machine, MACHINE_CONTROLLER_STYLES);
    }

    public static ControllerStyle resolveFactoryController(@Nullable DynamicMachine machine) {
        ensureLoaded();
        return resolve(machine, FACTORY_CONTROLLER_STYLES);
    }

    private static ControllerStyle resolve(@Nullable DynamicMachine machine, Map<String, ControllerStyle> source) {
        if (machine == null || machine.getRegistryName() == null) {
            return ControllerStyle.EMPTY;
        }

        String fullKey = machine.getRegistryName().toString().toLowerCase(Locale.ROOT);
        ControllerStyle fullMatch = source.get(fullKey);
        if (fullMatch != null) {
            return fullMatch;
        }

        String pathKey = machine.getRegistryName().getPath().toLowerCase(Locale.ROOT);
        ControllerStyle pathMatch = source.get(pathKey);
        return pathMatch == null ? ControllerStyle.EMPTY : pathMatch;
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
        MACHINE_CONTROLLER_STYLES.clear();
        FACTORY_CONTROLLER_STYLES.clear();

        Path machineryDir = Loader.instance().getConfigDir().toPath().resolve(MACHINERY_DIR);
        if (!Files.exists(machineryDir)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(machineryDir)) {
            stream
                .filter(Files::isRegularFile)
                .filter(MachineGuiStyleManager::isMachineJson)
                .forEach(MachineGuiStyleManager::loadMachineJson);
        } catch (IOException ignored) {
        }
    }

    private static boolean isMachineJson(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".json") && !name.endsWith(".var.json");
    }

    private static void loadMachineJson(Path path) {
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JsonObject root = new JsonParser().parse(content).getAsJsonObject();

            String registryName = getString(root, "registryname", "registryName");
            if (registryName == null || registryName.trim().isEmpty()) {
                return;
            }
            registryName = registryName.trim().toLowerCase(Locale.ROOT);
            String namespacedKey = registryName.contains(":") ? registryName : "modularmachinery:" + registryName;
            String pathKey = namespacedKey.contains(":") ? namespacedKey.substring(namespacedKey.indexOf(':') + 1) : namespacedKey;

            JsonObject extNode = getObject(root, "mmceGuiExt", "mmce_gui_ext", "mmce-gui-ext");
            if (extNode == null) {
                return;
            }

            ControllerStyle machineStyle = parseStyle(getObject(extNode, "machineController", "machine_controller", "machine"));
            if (!machineStyle.isEmpty()) {
                MACHINE_CONTROLLER_STYLES.put(namespacedKey, machineStyle);
                MACHINE_CONTROLLER_STYLES.put(pathKey, machineStyle);
            }

            ControllerStyle factoryStyle = parseStyle(getObject(extNode, "factoryController", "factory_controller", "factory"));
            if (!factoryStyle.isEmpty()) {
                FACTORY_CONTROLLER_STYLES.put(namespacedKey, factoryStyle);
                FACTORY_CONTROLLER_STYLES.put(pathKey, factoryStyle);
            }
        } catch (Exception ignored) {
        }
    }

    private static ControllerStyle parseStyle(@Nullable JsonObject node) {
        if (node == null) {
            return ControllerStyle.EMPTY;
        }

        ControllerStyle style = new ControllerStyle();
        style.backgroundTexture = getString(node, "backgroundTexture", "texture", "guiTexture");
        style.hideDefaultBackground = getBoolean(node, "hideDefaultBackground", "hideDefault", "disableDefaultTexture");
        style.guiWidth = getInt(node, "guiWidth", "gui_width", "width");
        style.guiHeight = getInt(node, "guiHeight", "gui_height", "height");
        style.specialThreadBackgroundColor = getColor(
            node,
            "specialThreadBackgroundColor",
            "specialThreadBgColor",
            "coreThreadBackgroundColor",
            "coreThreadBgColor"
        );
        style.disableRightExtension = getBoolean(
            node,
            "disableRightExtension",
            "disableRightExpansion",
            "disableRightExpandedArea",
            "noRightExtension"
        );

        Boolean useDefaultBackground = getBoolean(node, "useDefaultBackground");
        if (useDefaultBackground != null) {
            style.hideDefaultBackground = !useDefaultBackground.booleanValue();
        }

        Boolean enableRightExtension = getBoolean(node, "enableRightExtension");
        if (enableRightExtension != null) {
            style.disableRightExtension = !enableRightExtension.booleanValue();
        }

        return style.isEmpty() ? ControllerStyle.EMPTY : style;
    }

    @Nullable
    private static JsonObject getObject(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (!obj.has(key)) {
                continue;
            }
            JsonElement element = obj.get(key);
            if (element != null && element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        }
        return null;
    }

    @Nullable
    private static Integer getInt(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (!obj.has(key)) {
                continue;
            }
            JsonElement element = obj.get(key);
            if (element == null || !element.isJsonPrimitive()) {
                continue;
            }
            try {
                return element.getAsInt();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @Nullable
    private static String getString(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (!obj.has(key)) {
                continue;
            }
            JsonElement element = obj.get(key);
            if (element != null && element.isJsonPrimitive()) {
                return element.getAsString();
            }
        }
        return null;
    }

    @Nullable
    private static Boolean getBoolean(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (!obj.has(key)) {
                continue;
            }
            JsonElement element = obj.get(key);
            if (element != null && element.isJsonPrimitive()) {
                return element.getAsBoolean();
            }
        }
        return null;
    }

    @Nullable
    private static Integer getColor(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (!obj.has(key)) {
                continue;
            }
            JsonElement element = obj.get(key);
            if (element == null || !element.isJsonPrimitive()) {
                continue;
            }
            try {
                if (element.getAsJsonPrimitive().isNumber()) {
                    int value = element.getAsInt();
                    if ((value & 0xFF000000) == 0) {
                        value |= 0xFF000000;
                    }
                    return value;
                }
                String raw = element.getAsString();
                Integer parsed = parseColorString(raw);
                if (parsed != null) {
                    return parsed;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @Nullable
    private static Integer parseColorString(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return null;
        }

        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        if (text.startsWith("0x") || text.startsWith("0X")) {
            text = text.substring(2);
        }

        try {
            if (text.length() == 6) {
                return (int) (0xFF000000L | Long.parseLong(text, 16));
            }
            if (text.length() == 8) {
                return (int) Long.parseLong(text, 16);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    public static class ControllerStyle {
        public static final ControllerStyle EMPTY = new ControllerStyle();

        @Nullable
        public String backgroundTexture;
        @Nullable
        public Boolean hideDefaultBackground;
        @Nullable
        public Integer guiWidth;
        @Nullable
        public Integer guiHeight;
        @Nullable
        public Integer specialThreadBackgroundColor;
        @Nullable
        public Boolean disableRightExtension;

        public boolean isEmpty() {
            return (backgroundTexture == null || backgroundTexture.trim().isEmpty())
                   && hideDefaultBackground == null
                   && guiWidth == null
                   && guiHeight == null
                   && specialThreadBackgroundColor == null
                   && disableRightExtension == null;
        }
    }
}
