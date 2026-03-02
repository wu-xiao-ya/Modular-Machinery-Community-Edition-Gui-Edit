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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

            JsonObject machineNode = getObject(extNode, "machineController", "machine_controller", "machine");
            if (machineNode != null) {
                ControllerStyle machineStyle = parseStyle(machineNode);
                if (machineStyle == ControllerStyle.EMPTY) {
                    // Keep an explicit marker when a per-machine node exists, so GUI sizing won't fall back to global config.
                    machineStyle = new ControllerStyle();
                }
                MACHINE_CONTROLLER_STYLES.put(namespacedKey, machineStyle);
                MACHINE_CONTROLLER_STYLES.put(pathKey, machineStyle);
            }

            JsonObject factoryNode = getObject(extNode, "factoryController", "factory_controller", "factory");
            if (factoryNode != null) {
                ControllerStyle factoryStyle = parseStyle(factoryNode);
                if (factoryStyle == ControllerStyle.EMPTY) {
                    // Keep an explicit marker when a per-machine node exists, so GUI sizing won't fall back to global config.
                    factoryStyle = new ControllerStyle();
                }
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
        style.backgroundTextureOffsetX = getInt(
            node,
            "backgroundTextureOffsetX",
            "background_texture_offset_x",
            "textureOffsetX",
            "texture_offset_x",
            "offsetX",
            "offset_x",
            "textureOriginX",
            "texture_origin_x"
        );
        style.backgroundTextureOffsetY = getInt(
            node,
            "backgroundTextureOffsetY",
            "background_texture_offset_y",
            "textureOffsetY",
            "texture_offset_y",
            "offsetY",
            "offset_y",
            "textureOriginY",
            "texture_origin_y"
        );
        style.hideDefaultBackground = getBoolean(node, "hideDefaultBackground", "hideDefault", "disableDefaultTexture");
        style.guiWidth = getInt(node, "guiWidth", "gui_width", "width");
        style.guiHeight = getInt(node, "guiHeight", "gui_height", "height");
        style.backgroundTextureWidth = getInt(
            node,
            "backgroundTextureWidth",
            "background_texture_width",
            "textureWidth",
            "texture_width"
        );
        style.backgroundTextureHeight = getInt(
            node,
            "backgroundTextureHeight",
            "background_texture_height",
            "textureHeight",
            "texture_height"
        );
        style.backgroundCorner = getInt(
            node,
            "backgroundCorner",
            "background_corner",
            "corner",
            "cornerSize",
            "corner_size"
        );
        style.useNineSlice = getBoolean(node, "useNineSlice", "use_nine_slice", "nineSlice", "nine_slice");
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
        style.enableSmartInterfaceEditor = getBoolean(
            node,
            "enableSmartInterfaceEditor",
            "enable_smart_interface_editor",
            "enableDataPortEditor",
            "enable_data_port_editor",
            "enableDataPort"
        );
        style.smartInterfaceEditorX = getInt(
            node,
            "smartInterfaceEditorX",
            "smart_interface_editor_x",
            "dataPortEditorX",
            "data_port_editor_x",
            "dataPortX",
            "data_port_x"
        );
        style.smartInterfaceEditorY = getInt(
            node,
            "smartInterfaceEditorY",
            "smart_interface_editor_y",
            "dataPortEditorY",
            "data_port_editor_y",
            "dataPortY",
            "data_port_y"
        );
        style.smartInterfaceEditorInputWidth = getInt(
            node,
            "smartInterfaceEditorInputWidth",
            "smart_interface_editor_input_width",
            "dataPortEditorInputWidth",
            "data_port_editor_input_width",
            "dataPortWidth",
            "data_port_width"
        );
        style.smartInterfaceEditorVirtualKey = getString(
            node,
            "smartInterfaceEditorVirtualKey",
            "smart_interface_editor_virtual_key",
            "dataPortEditorVirtualKey",
            "data_port_editor_virtual_key",
            "virtualDataPortKey",
            "virtual_data_port_key"
        );
        style.smartInterfaceEditorPriority = getInt(
            node,
            "smartInterfaceEditorPriority",
            "smart_interface_editor_priority",
            "dataPortEditorPriority",
            "data_port_editor_priority",
            "editorPriority",
            "editor_priority"
        );
        style.foregroundContentPriority = getInt(
            node,
            "foregroundContentPriority",
            "foreground_content_priority",
            "contentPriority",
            "content_priority",
            "baseContentPriority",
            "base_content_priority"
        );
        style.hideDefaultSmartInterfaceEditor = getBoolean(
            node,
            "hideDefaultSmartInterfaceEditor",
            "hide_default_smart_interface_editor",
            "hideDefaultDataPortEditor",
            "hide_default_data_port_editor"
        );
        style.smartInterfaceEditors = parseSmartInterfaceEditors(node);
        style.textureLayers = parseTextureLayers(node);

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
    private static JsonElement getElement(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (!obj.has(key)) {
                continue;
            }
            JsonElement element = obj.get(key);
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    @Nullable
    private static Boolean getBoolean(JsonObject obj, boolean fallback, String... keys) {
        Boolean value = getBoolean(obj, keys);
        return value == null ? Boolean.valueOf(fallback) : value;
    }

    @Nullable
    private static List<SmartInterfaceEditorStyle> parseSmartInterfaceEditors(JsonObject node) {
        JsonElement element = getElement(
            node,
            "smartInterfaceEditors",
            "smart_interface_editors",
            "dataPortEditors",
            "data_port_editors"
        );
        if (element == null || !element.isJsonArray()) {
            return null;
        }

        List<SmartInterfaceEditorStyle> editors = new ArrayList<SmartInterfaceEditorStyle>();
        for (JsonElement child : element.getAsJsonArray()) {
            if (child == null || !child.isJsonObject()) {
                continue;
            }
            JsonObject editorObj = child.getAsJsonObject();
            Integer x = getInt(editorObj, "x", "editorX", "editor_x");
            Integer y = getInt(editorObj, "y", "editorY", "editor_y");
            String virtualKey = getString(editorObj, "virtualKey", "virtual_key", "key", "type");
            if (x == null || y == null || virtualKey == null || virtualKey.trim().isEmpty()) {
                continue;
            }

            SmartInterfaceEditorStyle editor = new SmartInterfaceEditorStyle();
            editor.id = getString(editorObj, "id", "name");
            editor.x = x.intValue();
            editor.y = y.intValue();
            editor.inputWidth = getInt(editorObj, "inputWidth", "input_width", "width");
            editor.virtualKey = virtualKey;
            editor.title = getString(editorObj, "title", "label");
            editor.showTitle = getBoolean(editorObj, true, "showTitle", "show_title");
            editor.showInfo = getBoolean(editorObj, true, "showInfo", "show_info");
            editor.showControls = getBoolean(editorObj, true, "showControls", "show_controls");
            editor.inputBackground = getBoolean(editorObj, true, "inputBackground", "input_background");
            editor.priority = getInt(editorObj, "priority", "zIndex", "z_index", "z", "layer");
            editors.add(editor);
        }
        return editors.isEmpty() ? null : editors;
    }

    @Nullable
    private static List<TextureLayerStyle> parseTextureLayers(JsonObject node) {
        List<TextureLayerStyle> out = new ArrayList<TextureLayerStyle>();
        appendTextureLayers(out, getElement(node, "textureLayers", "texture_layers", "guiLayers", "gui_layers"), false);
        appendTextureLayers(out, getElement(node, "backgroundLayers", "background_layers"), false);
        appendTextureLayers(out, getElement(node, "foregroundLayers", "foreground_layers"), true);
        return out.isEmpty() ? null : out;
    }

    private static void appendTextureLayers(List<TextureLayerStyle> out, @Nullable JsonElement root, boolean forceForeground) {
        if (root == null || !root.isJsonArray()) {
            return;
        }
        for (JsonElement child : root.getAsJsonArray()) {
            TextureLayerStyle layer = parseTextureLayer(child, forceForeground);
            if (layer != null) {
                out.add(layer);
            }
        }
    }

    @Nullable
    private static TextureLayerStyle parseTextureLayer(@Nullable JsonElement node, boolean forceForeground) {
        if (node == null) {
            return null;
        }
        if (node.isJsonPrimitive()) {
            String raw = node.getAsString();
            if (raw == null || raw.trim().isEmpty()) {
                return null;
            }
            TextureLayerStyle layer = new TextureLayerStyle();
            layer.texture = raw.trim();
            layer.id = null;
            layer.foreground = Boolean.valueOf(forceForeground);
            return layer;
        }
        if (!node.isJsonObject()) {
            return null;
        }

        JsonObject obj = node.getAsJsonObject();
        String texture = getString(obj, "texture", "backgroundTexture", "resource", "path");
        if (texture == null || texture.trim().isEmpty()) {
            return null;
        }
        TextureLayerStyle layer = new TextureLayerStyle();
        layer.id = getString(obj, "id", "name", "layerId", "layer_id");
        layer.texture = texture.trim();
        layer.offsetX = getInt(obj, "offsetX", "offset_x", "x");
        layer.offsetY = getInt(obj, "offsetY", "offset_y", "y");
        layer.width = getInt(obj, "width", "drawWidth", "draw_width");
        layer.height = getInt(obj, "height", "drawHeight", "draw_height");
        layer.textureWidth = getInt(obj, "textureWidth", "texture_width", "texW");
        layer.textureHeight = getInt(obj, "textureHeight", "texture_height", "texH");
        layer.corner = getInt(obj, "corner", "cornerSize", "corner_size");
        layer.useNineSlice = getBoolean(obj, "useNineSlice", "use_nine_slice", "nineSlice");
        layer.priority = getInt(obj, "priority", "zIndex", "z_index", "z", "layer");
        layer.foreground = forceForeground
            ? Boolean.TRUE
            : getBoolean(obj, "foreground", "front", "drawForeground");
        return layer;
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
        public Integer backgroundTextureOffsetX;
        @Nullable
        public Integer backgroundTextureOffsetY;
        @Nullable
        public Boolean hideDefaultBackground;
        @Nullable
        public Integer guiWidth;
        @Nullable
        public Integer guiHeight;
        @Nullable
        public Integer backgroundTextureWidth;
        @Nullable
        public Integer backgroundTextureHeight;
        @Nullable
        public Integer backgroundCorner;
        @Nullable
        public Boolean useNineSlice;
        @Nullable
        public Integer specialThreadBackgroundColor;
        @Nullable
        public Boolean disableRightExtension;
        @Nullable
        public Boolean enableSmartInterfaceEditor;
        @Nullable
        public Integer smartInterfaceEditorX;
        @Nullable
        public Integer smartInterfaceEditorY;
        @Nullable
        public Integer smartInterfaceEditorInputWidth;
        @Nullable
        public String smartInterfaceEditorVirtualKey;
        @Nullable
        public Integer smartInterfaceEditorPriority;
        @Nullable
        public Integer foregroundContentPriority;
        @Nullable
        public Boolean hideDefaultSmartInterfaceEditor;
        @Nullable
        public List<SmartInterfaceEditorStyle> smartInterfaceEditors;
        @Nullable
        public List<TextureLayerStyle> textureLayers;

        public boolean isEmpty() {
            return (backgroundTexture == null || backgroundTexture.trim().isEmpty())
                   && backgroundTextureOffsetX == null
                   && backgroundTextureOffsetY == null
                   && hideDefaultBackground == null
                   && guiWidth == null
                   && guiHeight == null
                   && backgroundTextureWidth == null
                   && backgroundTextureHeight == null
                   && backgroundCorner == null
                   && useNineSlice == null
                   && specialThreadBackgroundColor == null
                   && disableRightExtension == null
                   && enableSmartInterfaceEditor == null
                   && smartInterfaceEditorX == null
                   && smartInterfaceEditorY == null
                   && smartInterfaceEditorInputWidth == null
                   && (smartInterfaceEditorVirtualKey == null || smartInterfaceEditorVirtualKey.trim().isEmpty())
                   && smartInterfaceEditorPriority == null
                   && foregroundContentPriority == null
                   && hideDefaultSmartInterfaceEditor == null
                   && (smartInterfaceEditors == null || smartInterfaceEditors.isEmpty())
                   && (textureLayers == null || textureLayers.isEmpty());
        }
    }

    public static class SmartInterfaceEditorStyle {
        @Nullable
        public String id;
        public int x;
        public int y;
        @Nullable
        public Integer inputWidth;
        public String virtualKey;
        @Nullable
        public String title;
        @Nullable
        public Boolean showTitle;
        @Nullable
        public Boolean showInfo;
        @Nullable
        public Boolean showControls;
        @Nullable
        public Boolean inputBackground;
        @Nullable
        public Integer priority;
    }

    public static class TextureLayerStyle {
        @Nullable
        public String id;
        public String texture;
        @Nullable
        public Integer offsetX;
        @Nullable
        public Integer offsetY;
        @Nullable
        public Integer width;
        @Nullable
        public Integer height;
        @Nullable
        public Integer textureWidth;
        @Nullable
        public Integer textureHeight;
        @Nullable
        public Integer corner;
        @Nullable
        public Boolean useNineSlice;
        @Nullable
        public Boolean foreground;
        @Nullable
        public Integer priority;
    }
}
