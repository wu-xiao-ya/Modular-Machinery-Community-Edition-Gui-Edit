package com.fushu.mmceguiext.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.common.Loader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class SubGuiConfigLoader {
    private static final String SUBGUI_DIR = "mmceguiext/subgui";

    private SubGuiConfigLoader() {
    }

    static Path getSubGuiRootDir() {
        return Loader.instance().getConfigDir().toPath().resolve(SUBGUI_DIR);
    }

    static MachineGuiStyleParser.MachineFileParseResult parseSubGuiJson(String sourceName, String content) {
        MachineGuiStyleParser.MachineFileParseResult direct = MachineGuiStyleParser.parseMachineJson(sourceName, content);
        if (direct.machineNodePresent || direct.factoryNodePresent) {
            return direct;
        }
        return parseSingleSubGuiJson(sourceName, content);
    }

    static MachineGuiStyleParser.MachineFileParseResult loadSubGuiJson(Path path) throws Exception {
        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        return parseSubGuiJson(path.toString(), content);
    }

    private static MachineGuiStyleParser.MachineFileParseResult parseSingleSubGuiJson(String sourceName, String content) {
        MachineGuiStyleParser.MachineFileParseResult result = new MachineGuiStyleParser.MachineFileParseResult(sourceName);
        JsonElement parsedRoot;
        try {
            parsedRoot = new JsonParser().parse(content);
        } catch (Exception ex) {
            result.warn("Failed to parse JSON: " + ex.getMessage());
            return result;
        }
        if (parsedRoot == null || !parsedRoot.isJsonObject()) {
            result.warn("Standalone subGUI JSON root must be an object.");
            return result;
        }

        JsonObject root = parsedRoot.getAsJsonObject();
        String registryName = getTrimmedString(root, "registryname", "registryName");
        if (registryName == null || registryName.isEmpty()) {
            result.warn("Standalone subGUI JSON is missing required field registryname.");
            return result;
        }

        String normalizedRegistryName = registryName.toLowerCase(java.util.Locale.ROOT);
        result.namespacedKey = normalizedRegistryName.contains(":")
            ? normalizedRegistryName
            : "modularmachinery:" + normalizedRegistryName;
        result.allowPathFallback = !normalizedRegistryName.contains(":");
        result.pathKey = result.namespacedKey.contains(":")
            ? result.namespacedKey.substring(result.namespacedKey.indexOf(':') + 1)
            : result.namespacedKey;

        String controllerKey = normalizeControllerKey(getTrimmedString(root, "controller", "controllerType", "controller_type", "targetController", "target_controller"));
        JsonObject wrappedRoot = new JsonObject();
        wrappedRoot.addProperty("registryname", registryName);

        JsonObject extNode = new JsonObject();
        JsonObject controllerNode = new JsonObject();
        controllerNode.add("subGuis", singleElementArray(root.deepCopy()));
        extNode.add(controllerKey, controllerNode);
        wrappedRoot.add("mmce_gui_ext", extNode);

        MachineGuiStyleParser.MachineFileParseResult wrappedResult =
            MachineGuiStyleParser.parseMachineJson(sourceName, wrappedRoot.toString());
        wrappedResult.namespacedKey = result.namespacedKey;
        wrappedResult.pathKey = result.pathKey;
        wrappedResult.allowPathFallback = result.allowPathFallback;
        wrappedResult.machineRegistryName = registryName;
        wrappedResult.factoryRegistryName = registryName;
        return wrappedResult;
    }

    private static com.google.gson.JsonArray singleElementArray(JsonObject object) {
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        array.add(object);
        return array;
    }

    private static String normalizeControllerKey(String raw) {
        if (raw == null) {
            return "machineController";
        }
        String normalized = raw.trim().toLowerCase(java.util.Locale.ROOT);
        if ("factory".equals(normalized) || "factorycontroller".equals(normalized) || "factory_controller".equals(normalized)) {
            return "factoryController";
        }
        return "machineController";
    }

    private static String getTrimmedString(JsonObject object, String... keys) {
        for (String key : keys) {
            if (!object.has(key)) {
                continue;
            }
            JsonElement element = object.get(key);
            if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                continue;
            }
            return element.getAsString().trim();
        }
        return null;
    }
}
