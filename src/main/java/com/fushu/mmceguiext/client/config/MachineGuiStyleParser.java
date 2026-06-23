package com.fushu.mmceguiext.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class MachineGuiStyleParser {
    private static final int MAX_ARRAY_ENTRIES = 512;
    private static final int MAX_TEXTURE_LAYERS = 256;
    private static final int MAX_WARNINGS = 256;
    private static final int MAX_GUI_SIZE = 4096;
    private static final int MAX_COMPONENT_SIZE = 4096;
    private static final int MAX_CORNER = 128;
    private static final float MIN_TEXT_SCALE = 0.05F;
    private static final float MAX_TEXT_SCALE = 8.0F;

    private MachineGuiStyleParser() {
    }

    static MachineFileParseResult parseMachineJson(String sourceName, String content) {
        MachineFileParseResult result = new MachineFileParseResult(sourceName);

        JsonElement parsedRoot;
        try {
            parsedRoot = new JsonParser().parse(content);
        } catch (Exception ex) {
            result.warn("Failed to parse JSON: " + summarizeException(ex));
            return result;
        }

        if (parsedRoot == null || !parsedRoot.isJsonObject()) {
            result.warn("Machine JSON root must be an object.");
            return result;
        }

        JsonObject root = parsedRoot.getAsJsonObject();
        JsonObject extNode = getObject(root, result, "root", "mmceGuiExt", "mmce_gui_ext", "mmce-gui-ext");
        if (extNode == null) {
            return result;
        }

        String registryName = getTrimmedString(root, result, "root", "registryname", "registryName");
        if (registryName == null || registryName.isEmpty()) {
            result.warn("Found mmce_gui_ext data, but registryname is missing or empty.");
            return result;
        }

        String normalizedRegistryName = registryName.toLowerCase(Locale.ROOT);
        result.namespacedKey = normalizedRegistryName.contains(":")
            ? normalizedRegistryName
            : "modularmachinery:" + normalizedRegistryName;
        result.allowPathFallback = !normalizedRegistryName.contains(":");
        result.pathKey = result.namespacedKey.contains(":")
            ? result.namespacedKey.substring(result.namespacedKey.indexOf(':') + 1)
            : result.namespacedKey;

        JsonObject machineNode = getObject(extNode, result, "mmce_gui_ext", "machineController", "machine_controller", "machine");
        if (machineNode != null) {
            result.machineNodePresent = true;
            result.machineRegistryName = registryName;
            result.machineStyle = parseStyle(machineNode, result, "machineController");
        }

        JsonObject factoryNode = getObject(extNode, result, "mmce_gui_ext", "factoryController", "factory_controller", "factory");
        if (factoryNode != null) {
            result.factoryNodePresent = true;
            result.factoryRegistryName = registryName;
            result.factoryStyle = parseStyle(factoryNode, result, "factoryController");
        }

        return result;
    }

    private static MachineGuiStyleManager.ControllerStyle parseStyle(
        JsonObject node,
        MachineFileParseResult result,
        String scope
    ) {
        MachineGuiStyleManager.ControllerStyle style = new MachineGuiStyleManager.ControllerStyle();

        style.backgroundTexture = getTrimmedString(node, result, scope, "backgroundTexture", "texture", "guiTexture");
        style.backgroundTextureOffsetX = validateMinInt(
            getInt(node, result, scope, "backgroundTextureOffsetX", "background_texture_offset_x", "textureOffsetX",
                "texture_offset_x", "offsetX", "offset_x", "textureOriginX", "texture_origin_x"),
            0,
            result,
            scope,
            "backgroundTextureOffsetX"
        );
        style.backgroundTextureOffsetY = validateMinInt(
            getInt(node, result, scope, "backgroundTextureOffsetY", "background_texture_offset_y", "textureOffsetY",
                "texture_offset_y", "offsetY", "offset_y", "textureOriginY", "texture_origin_y"),
            0,
            result,
            scope,
            "backgroundTextureOffsetY"
        );
        style.centerFullGui = getBoolean(
            node,
            result,
            scope,
            "centerFullGui",
            "center_full_gui",
            "centerEntireGui",
            "center_entire_gui",
            "centerVisualBounds",
            "center_visual_bounds",
            "centerGui",
            "center_gui"
        );
        style.hideDefaultBackground = getBoolean(node, result, scope, "hideDefaultBackground", "hideDefault", "disableDefaultTexture");
        style.guiWidth = validateRangeInt(
            getInt(node, result, scope, "guiWidth", "gui_width", "width"),
            1,
            MAX_GUI_SIZE,
            result,
            scope,
            "guiWidth"
        );
        style.guiHeight = validateRangeInt(
            getInt(node, result, scope, "guiHeight", "gui_height", "height"),
            1,
            MAX_GUI_SIZE,
            result,
            scope,
            "guiHeight"
        );
        style.allowOffscreenGui = getBoolean(
            node,
            result,
            scope,
            "allowOffscreenGui",
            "allow_offscreen_gui",
            "allowOffscreen",
            "allow_offscreen",
            "allowOversizedGui",
            "allow_oversized_gui",
            "disableScreenClamp",
            "disable_screen_clamp"
        );
        style.coordinateWidth = validateRangeInt(
            getInt(node, result, scope, "coordinateWidth", "coordinate_width", "logicalWidth", "logical_width",
                "canvasWidth", "canvas_width"),
            1,
            MAX_GUI_SIZE,
            result,
            scope,
            "coordinateWidth"
        );
        style.coordinateHeight = validateRangeInt(
            getInt(node, result, scope, "coordinateHeight", "coordinate_height", "logicalHeight", "logical_height",
                "canvasHeight", "canvas_height"),
            1,
            MAX_GUI_SIZE,
            result,
            scope,
            "coordinateHeight"
        );
        style.backgroundTextureWidth = validateRangeInt(
            getInt(node, result, scope, "backgroundTextureWidth", "background_texture_width", "textureWidth", "texture_width"),
            1,
            MAX_COMPONENT_SIZE,
            result,
            scope,
            "backgroundTextureWidth"
        );
        style.backgroundTextureHeight = validateRangeInt(
            getInt(node, result, scope, "backgroundTextureHeight", "background_texture_height", "textureHeight", "texture_height"),
            1,
            MAX_COMPONENT_SIZE,
            result,
            scope,
            "backgroundTextureHeight"
        );
        style.backgroundCorner = validateRangeInt(
            getInt(node, result, scope, "backgroundCorner", "background_corner", "corner", "cornerSize", "corner_size"),
            1,
            MAX_CORNER,
            result,
            scope,
            "backgroundCorner"
        );
        style.useNineSlice = getBoolean(node, result, scope, "useNineSlice", "use_nine_slice", "nineSlice", "nine_slice");
        style.specialThreadBackgroundColor = getColor(
            node,
            result,
            scope,
            "specialThreadBackgroundColor",
            "specialThreadBgColor",
            "coreThreadBackgroundColor",
            "coreThreadBgColor"
        );
        style.threadQueueX = getInt(
            node,
            result,
            scope,
            "threadQueueX",
            "thread_queue_x",
            "queueX",
            "queue_x",
            "threadX",
            "thread_x"
        );
        style.threadQueueY = getInt(
            node,
            result,
            scope,
            "threadQueueY",
            "thread_queue_y",
            "queueY",
            "queue_y",
            "threadY",
            "thread_y"
        );
        style.threadScrollbarX = getInt(
            node,
            result,
            scope,
            "threadScrollbarX",
            "thread_scrollbar_x",
            "queueScrollbarX",
            "queue_scrollbar_x"
        );
        style.threadScrollbarY = getInt(
            node,
            result,
            scope,
            "threadScrollbarY",
            "thread_scrollbar_y",
            "queueScrollbarY",
            "queue_scrollbar_y"
        );
        style.threadScrollbar = parseThreadScrollbar(node, result, scope);
        style.threadVisibleRows = validateRangeInt(
            getInt(
                node,
                result,
                scope,
                "threadVisibleRows",
                "thread_visible_rows",
                "queueVisibleRows",
                "queue_visible_rows",
                "visibleRows",
                "visible_rows"
            ),
            1,
            MAX_ARRAY_ENTRIES,
            result,
            scope,
            "threadVisibleRows"
        );
        style.threadRowWidth = validateRangeInt(
            getInt(
                node,
                result,
                scope,
                "threadRowWidth",
                "thread_row_width",
                "queueRowWidth",
                "queue_row_width",
                "threadElementWidth",
                "thread_element_width"
            ),
            24,
            MAX_COMPONENT_SIZE,
            result,
            scope,
            "threadRowWidth"
        );
        style.threadRowHeight = validateRangeInt(
            getInt(
                node,
                result,
                scope,
                "threadRowHeight",
                "thread_row_height",
                "queueRowHeight",
                "queue_row_height",
                "threadElementHeight",
                "thread_element_height"
            ),
            16,
            MAX_COMPONENT_SIZE,
            result,
            scope,
            "threadRowHeight"
        );
        style.disableRightExtension = getBoolean(
            node,
            result,
            scope,
            "disableRightExtension",
            "disableRightExpansion",
            "disableRightExpandedArea",
            "noRightExtension"
        );
        style.enableSmartInterfaceEditor = getBoolean(
            node,
            result,
            scope,
            "enableSmartInterfaceEditor",
            "enable_smart_interface_editor",
            "enableDataPortEditor",
            "enable_data_port_editor",
            "enableDataPort"
        );
        style.smartInterfaceEditorX = getInt(
            node,
            result,
            scope,
            "smartInterfaceEditorX",
            "smart_interface_editor_x",
            "dataPortEditorX",
            "data_port_editor_x",
            "dataPortX",
            "data_port_x"
        );
        style.smartInterfaceEditorY = getInt(
            node,
            result,
            scope,
            "smartInterfaceEditorY",
            "smart_interface_editor_y",
            "dataPortEditorY",
            "data_port_editor_y",
            "dataPortY",
            "data_port_y"
        );
        style.smartInterfaceEditorInputWidth = validateRangeInt(
            getInt(
                node,
                result,
                scope,
                "smartInterfaceEditorInputWidth",
                "smart_interface_editor_input_width",
                "dataPortEditorInputWidth",
                "data_port_editor_input_width",
                "dataPortWidth",
                "data_port_width"
            ),
            1,
            MAX_COMPONENT_SIZE,
            result,
            scope,
            "smartInterfaceEditorInputWidth"
        );
        style.smartInterfaceEditorVirtualKey = getTrimmedString(
            node,
            result,
            scope,
            "smartInterfaceEditorVirtualKey",
            "smart_interface_editor_virtual_key",
            "dataPortEditorVirtualKey",
            "data_port_editor_virtual_key",
            "virtualDataPortKey",
            "virtual_data_port_key"
        );
        style.smartInterfaceEditorPriority = getInt(
            node,
            result,
            scope,
            "smartInterfaceEditorPriority",
            "smart_interface_editor_priority",
            "dataPortEditorPriority",
            "data_port_editor_priority",
            "editorPriority",
            "editor_priority"
        );
        style.foregroundContentPriority = getInt(
            node,
            result,
            scope,
            "foregroundContentPriority",
            "foreground_content_priority",
            "contentPriority",
            "content_priority",
            "baseContentPriority",
            "base_content_priority"
        );
        style.hideDefaultSmartInterfaceEditor = getBoolean(
            node,
            result,
            scope,
            "hideDefaultSmartInterfaceEditor",
            "hide_default_smart_interface_editor",
            "hideDefaultDataPortEditor",
            "hide_default_data_port_editor"
        );
        style.hidePlayerInventory = getBoolean(
            node,
            result,
            scope,
            "hidePlayerInventory",
            "hide_player_inventory",
            "hideInventory",
            "hide_inventory"
        );
        style.showBlueprintInfo = getBoolean(
            node,
            result,
            scope,
            "showBlueprintInfo",
            "show_blueprint_info",
            "showBlueprint",
            "show_blueprint"
        );
        style.showStructureInfo = getBoolean(
            node,
            result,
            scope,
            "showStructureInfo",
            "show_structure_info",
            "showStructure",
            "show_structure"
        );
        style.showStatusInfo = getBoolean(
            node,
            result,
            scope,
            "showStatusInfo",
            "show_status_info",
            "showStatus",
            "show_status"
        );
        style.showParallelismInfo = getBoolean(
            node,
            result,
            scope,
            "showParallelismInfo",
            "show_parallelism_info",
            "showParallelism",
            "show_parallelism",
            "showThreadInfo",
            "show_thread_info"
        );
        style.showPerformanceInfo = getBoolean(
            node,
            result,
            scope,
            "showPerformanceInfo",
            "show_performance_info",
            "showPerformance",
            "show_performance"
        );
        style.defaultPageId = getTrimmedString(
            node,
            result,
            scope,
            "defaultPageId",
            "default_page_id",
            "pageId",
            "page_id",
            "defaultStateId",
            "default_state_id",
            "stateId",
            "state_id"
        );
        style.defaultPanelId = getTrimmedString(node, result, scope, "defaultPanelId", "default_panel_id", "panelId", "panel_id");
        style.customPanels = parseStringArray(node, result, scope, "customPanels", "custom_panels", "panels");
        style.infoSections = parseInfoSections(node, result, scope);
        style.texts = parseTexts(node, result, scope);
        style.smartInterfaceEditors = parseSmartInterfaceEditors(node, result, scope);
        style.buttons = parseButtons(node, result, scope);
        style.textureLayers = parseTextureLayers(node, result, scope);
        style.progressBars = parseProgressBars(node, result, scope);
        style.sliders = parseSliders(node, result, scope);
        style.dynamicVisuals = parseDynamicVisuals(node, result, scope);
        style.subGuis = parseSubGuis(node, result, scope);

        Boolean useDefaultBackground = getBoolean(node, result, scope, "useDefaultBackground");
        if (useDefaultBackground != null) {
            style.hideDefaultBackground = !useDefaultBackground.booleanValue();
        }

        Boolean enableRightExtension = getBoolean(node, result, scope, "enableRightExtension");
        if (enableRightExtension != null) {
            style.disableRightExtension = !enableRightExtension.booleanValue();
        }

        return style.isEmpty() ? MachineGuiStyleManager.ControllerStyle.EMPTY : style;
    }

    @Nullable
    private static MachineGuiStyleManager.ThreadScrollbarStyle parseThreadScrollbar(
        JsonObject node,
        MachineFileParseResult result,
        String scope
    ) {
        JsonObject obj = getObject(node, result, scope, "threadScrollbar", "thread_scrollbar");
        if (obj == null) {
            return null;
        }
        String itemScope = field(scope, "threadScrollbar");
        MachineGuiStyleManager.ThreadScrollbarStyle style = new MachineGuiStyleManager.ThreadScrollbarStyle();
        style.x = validateRangeInt(getInt(obj, result, itemScope, "x"), -MAX_COMPONENT_SIZE, MAX_COMPONENT_SIZE, result, itemScope, "x");
        style.y = validateRangeInt(getInt(obj, result, itemScope, "y"), -MAX_COMPONENT_SIZE, MAX_COMPONENT_SIZE, result, itemScope, "y");
        style.width = validateRangeInt(getInt(obj, result, itemScope, "width", "w"), 1, MAX_COMPONENT_SIZE, result, itemScope, "width");
        style.height = validateRangeInt(getInt(obj, result, itemScope, "height", "h"), 1, MAX_COMPONENT_SIZE, result, itemScope, "height");
        style.trackTexture = getTrimmedString(obj, result, itemScope, "trackTexture", "track_texture", "texture");
        style.thumbTexture = getTrimmedString(obj, result, itemScope, "thumbTexture", "thumb_texture", "handleTexture", "handle_texture");
        style.trackColor = getColor(obj, result, itemScope, "trackColor", "track_color", "backgroundColor", "background_color", "bgColor", "bg_color");
        style.thumbColor = getColor(obj, result, itemScope, "thumbColor", "thumb_color", "handleColor", "handle_color");
        style.textureWidth = validateRangeInt(getInt(obj, result, itemScope, "textureWidth", "texture_width", "trackTextureWidth", "track_texture_width", "texW"), 1, MAX_COMPONENT_SIZE, result, itemScope, "textureWidth");
        style.textureHeight = validateRangeInt(getInt(obj, result, itemScope, "textureHeight", "texture_height", "trackTextureHeight", "track_texture_height", "texH"), 1, MAX_COMPONENT_SIZE, result, itemScope, "textureHeight");
        style.thumbTextureWidth = validateRangeInt(getInt(obj, result, itemScope, "thumbTextureWidth", "thumb_texture_width", "handleTextureWidth", "handle_texture_width"), 1, MAX_COMPONENT_SIZE, result, itemScope, "thumbTextureWidth");
        style.thumbTextureHeight = validateRangeInt(getInt(obj, result, itemScope, "thumbTextureHeight", "thumb_texture_height", "handleTextureHeight", "handle_texture_height"), 1, MAX_COMPONENT_SIZE, result, itemScope, "thumbTextureHeight");
        style.thumbMinHeight = validateRangeInt(getInt(obj, result, itemScope, "thumbMinHeight", "thumb_min_height", "minThumbHeight", "min_thumb_height"), 1, MAX_COMPONENT_SIZE, result, itemScope, "thumbMinHeight");
        style.visible = getBoolean(obj, result, itemScope, "visible", "show", "enabled");
        return style;
    }

    @Nullable
    private static List<MachineGuiStyleManager.InfoSectionStyle> parseInfoSections(
        JsonObject node,
        MachineFileParseResult result,
        String scope
    ) {
        MatchedElement match = findElement(node, "infoSections", "info_sections", "infoRoutes", "info_routes");
        if (match == null) {
            return null;
        }
        if (!match.element.isJsonArray()) {
            result.warnForMachine(scope, field(scope, match.key) + " must be an array.");
            return null;
        }

        List<MachineGuiStyleManager.InfoSectionStyle> out = new ArrayList<MachineGuiStyleManager.InfoSectionStyle>();
        JsonArray array = match.element.getAsJsonArray();
        int limit = cappedArraySize(array, result, scope, match.key, MAX_ARRAY_ENTRIES);
        for (int i = 0; i < limit; i++) {
            JsonElement child = array.get(i);
            String itemScope = field(scope, match.key + "[" + i + "]");
            if (child == null || !child.isJsonObject()) {
                result.warnForMachine(scope, itemScope + " must be an object.");
                continue;
            }
            JsonObject obj = child.getAsJsonObject();
            MachineGuiStyleManager.InfoSectionStyle section = new MachineGuiStyleManager.InfoSectionStyle();
            section.id = getTrimmedString(obj, result, itemScope, "id", "type", "section", "name");
            section.panel = getTrimmedString(obj, result, itemScope, "panel", "panelId", "panel_id", "target");
            section.visible = getBoolean(obj, result, itemScope, "visible", "show", "enabled");
            if (section.id == null || section.id.trim().isEmpty()) {
                result.warnForMachine(scope, itemScope + " is missing required field id.");
                continue;
            }
            out.add(section);
        }
        return out.isEmpty() ? null : out;
    }

    @Nullable
    private static List<MachineGuiStyleManager.TextStyle> parseTexts(
        JsonObject node,
        MachineFileParseResult result,
        String scope
    ) {
        MatchedElement match = findElement(node, "texts", "text", "labels", "customTexts", "custom_texts");
        if (match == null) {
            return null;
        }
        if (!match.element.isJsonArray()) {
            result.warnForMachine(scope, field(scope, match.key) + " must be an array.");
            return null;
        }

        List<MachineGuiStyleManager.TextStyle> texts = new ArrayList<MachineGuiStyleManager.TextStyle>();
        JsonArray array = match.element.getAsJsonArray();
        int limit = cappedArraySize(array, result, scope, match.key, MAX_ARRAY_ENTRIES);
        for (int i = 0; i < limit; i++) {
            JsonElement child = array.get(i);
            String itemScope = field(scope, match.key + "[" + i + "]");
            if (child == null || !child.isJsonObject()) {
                result.warnForMachine(scope, itemScope + " must be an object.");
                continue;
            }

            JsonObject obj = child.getAsJsonObject();
            Integer x = getInt(obj, result, itemScope, "x");
            Integer y = getInt(obj, result, itemScope, "y");
            String value = getTrimmedString(obj, result, itemScope, "value", "key", "text");
            if (x == null || y == null || value == null || value.isEmpty()) {
                result.warnForMachine(scope, itemScope + " is missing required fields x, y or value.");
                continue;
            }

            MachineGuiStyleManager.TextStyle text = new MachineGuiStyleManager.TextStyle();
            text.id = getTrimmedString(obj, result, itemScope, "id", "name");
            text.x = x.intValue();
            text.y = y.intValue();
            text.value = value;
            text.color = getColor(obj, result, itemScope, "color", "textColor", "text_color");
            text.scale = normalizeScale(getFloat(obj, result, itemScope, "scale"));
            text.priority = getInt(obj, result, itemScope, "priority", "zIndex", "z_index", "z", "layer");
            text.shadow = getBoolean(obj, result, itemScope, "shadow", "withShadow", "with_shadow");
            text.visible = getBoolean(obj, result, itemScope, "visible", "show", "enabled");
            text.page = getTrimmedString(obj, result, itemScope, "page", "pageId", "page_id", "tab", "state", "stateId", "state_id", "guiState", "gui_state");
            text.align = normalizeTextAlign(getTrimmedString(obj, result, itemScope, "align", "alignment", "textAlign", "text_align"));
            texts.add(text);
        }

        return texts.isEmpty() ? null : texts;
    }

    @Nullable
    private static List<MachineGuiStyleManager.SmartInterfaceEditorStyle> parseSmartInterfaceEditors(
        JsonObject node,
        MachineFileParseResult result,
        String scope
    ) {
        MatchedElement match = findElement(node, "smartInterfaceEditors", "smart_interface_editors", "dataPortEditors", "data_port_editors");
        if (match == null) {
            return null;
        }
        if (!match.element.isJsonArray()) {
            result.warnForMachine(scope, field(scope, match.key) + " must be an array.");
            return null;
        }

        List<MachineGuiStyleManager.SmartInterfaceEditorStyle> editors = new ArrayList<MachineGuiStyleManager.SmartInterfaceEditorStyle>();
        JsonArray array = match.element.getAsJsonArray();
        int limit = cappedArraySize(array, result, scope, match.key, MAX_ARRAY_ENTRIES);
        for (int i = 0; i < limit; i++) {
            JsonElement child = array.get(i);
            String itemScope = field(scope, match.key + "[" + i + "]");
            if (child == null || !child.isJsonObject()) {
                result.warnForMachine(scope, itemScope + " must be an object.");
                continue;
            }

            JsonObject editorObj = child.getAsJsonObject();
            Integer x = validateMinInt(getInt(editorObj, result, itemScope, "x", "editorX", "editor_x"), 0, result, itemScope, "x");
            Integer y = validateMinInt(getInt(editorObj, result, itemScope, "y", "editorY", "editor_y"), 0, result, itemScope, "y");
            String virtualKey = getTrimmedString(editorObj, result, itemScope, "virtualKey", "virtual_key", "key", "type");
            if (x == null || y == null || virtualKey == null || virtualKey.isEmpty()) {
                result.warnForMachine(scope, itemScope + " is missing required fields x, y or virtualKey.");
                continue;
            }

            MachineGuiStyleManager.SmartInterfaceEditorStyle editor = new MachineGuiStyleManager.SmartInterfaceEditorStyle();
            editor.id = getTrimmedString(editorObj, result, itemScope, "id", "name");
            editor.x = x.intValue();
            editor.y = y.intValue();
            editor.inputWidth = validateRangeInt(getInt(editorObj, result, itemScope, "inputWidth", "input_width", "width"), 1, MAX_COMPONENT_SIZE, result, itemScope, "inputWidth");
            editor.virtualKey = virtualKey;
            editor.title = getTrimmedString(editorObj, result, itemScope, "title", "label");
            editor.showTitle = getBoolean(editorObj, result, itemScope, "showTitle", "show_title");
            editor.showInfo = getBoolean(editorObj, result, itemScope, "showInfo", "show_info");
            editor.showControls = getBoolean(editorObj, result, itemScope, "showControls", "show_controls");
            editor.inputBackground = getBoolean(editorObj, result, itemScope, "inputBackground", "input_background");
            editor.priority = getInt(editorObj, result, itemScope, "priority", "zIndex", "z_index", "z", "layer");
            editor.page = getTrimmedString(editorObj, result, itemScope, "page", "pageId", "page_id", "tab", "state", "stateId", "state_id", "guiState", "gui_state");
            editors.add(editor);
        }

        return editors.isEmpty() ? null : editors;
    }

    @Nullable
    private static List<MachineGuiStyleManager.ButtonStyle> parseButtons(
        JsonObject node,
        MachineFileParseResult result,
        String scope
    ) {
        List<MachineGuiStyleManager.ButtonStyle> buttons = new ArrayList<MachineGuiStyleManager.ButtonStyle>();
        appendButtons(buttons, findElement(node, "buttons", "buttonList", "button_list", "guiButtons", "gui_buttons"), false, result, scope);
        appendButtons(buttons, findElement(node, "hotkeys", "hotKeys", "guiHotkeys", "gui_hotkeys", "shortcuts"), true, result, scope);

        return buttons.isEmpty() ? null : buttons;
    }

    private static void appendButtons(
        List<MachineGuiStyleManager.ButtonStyle> buttons,
        @Nullable MatchedElement match,
        boolean hotkeyOnly,
        MachineFileParseResult result,
        String scope
    ) {
        if (match == null) {
            return;
        }
        if (!match.element.isJsonArray()) {
            result.warnForMachine(scope, field(scope, match.key) + " must be an array.");
            return;
        }

        JsonArray array = match.element.getAsJsonArray();
        int limit = cappedArraySize(array, result, scope, match.key, MAX_ARRAY_ENTRIES);
        for (int i = 0; i < limit; i++) {
            MachineGuiStyleManager.ButtonStyle button = parseButtonStyle(
                array.get(i),
                hotkeyOnly,
                result,
                field(scope, match.key + "[" + i + "]")
            );
            if (button != null) {
                buttons.add(button);
            }
        }
    }

    @Nullable
    private static MachineGuiStyleManager.ButtonStyle parseButtonStyle(
        @Nullable JsonElement node,
        boolean hotkeyOnly,
        MachineFileParseResult result,
        String itemScope
    ) {
        if (node == null || !node.isJsonObject()) {
            result.warnForMachine(itemScope, itemScope + " must be an object.");
            return null;
        }

        JsonObject obj = node.getAsJsonObject();
        Integer x = validateMinInt(getInt(obj, result, itemScope, "x"), 0, result, itemScope, "x");
        Integer y = validateMinInt(getInt(obj, result, itemScope, "y"), 0, result, itemScope, "y");
        String label = getTrimmedString(obj, result, itemScope, "label", "text", "title");
        String texture = getTrimmedString(obj, result, itemScope, "texture", "buttonTexture", "button_texture", "backgroundTexture", "background_texture");
        String hoverTexture = getTrimmedString(obj, result, itemScope, "hoverTexture", "hover_texture", "highlightTexture", "highlight_texture");
        String disabledTexture = getTrimmedString(obj, result, itemScope, "disabledTexture", "disabled_texture", "inactiveTexture", "inactive_texture");
        String action = getTrimmedString(obj, result, itemScope, "action", "mode", "type");
        String buttonId = getTrimmedString(obj, result, itemScope, "buttonId", "button_id", "eventId", "event_id", "id", "name");
        String targetPage = getTrimmedString(
            obj,
            result,
            itemScope,
            "targetPage",
            "target_page",
            "pageTarget",
            "page_target",
            "targetState",
            "target_state",
            "stateTarget",
            "state_target",
            "targetGuiState",
            "target_gui_state"
        );
        String targetSubGui = getTrimmedString(
            obj,
            result,
            itemScope,
            "targetSubGui",
            "target_sub_gui",
            "targetSubgui",
            "target_subgui",
            "subGui",
            "sub_gui",
            "subgui",
            "subGuiId",
            "sub_gui_id",
            "subguiId",
            "subgui_id"
        );
        String openMode = normalizeSubGuiMode(getTrimmedString(
            obj,
            result,
            itemScope,
            "openMode",
            "open_mode",
            "targetMode",
            "target_mode",
            "mode"
        ));
        String key = hotkeyOnly
            ? getTrimmedString(
                obj,
                result,
                itemScope,
                "dataPortKey",
                "data_port_key",
                "portKey",
                "port_key",
                "dataPort",
                "data_port",
                "virtualKey",
                "virtual_key",
                "interfaceType",
                "interface_type"
            )
            : getTrimmedString(
                obj,
                result,
                itemScope,
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
        Float value = getOptionalFloat(obj, "value", "delta", "amount");
        String stringValue = value == null
            ? getValueAsString(obj, result, itemScope, "value", "textValue", "text_value", "stringValue", "string_value")
            : null;
        List<String> hotkeys = parseHotkeys(obj, result, itemScope, hotkeyOnly);
        boolean hasButtonTexture = (texture != null && !texture.isEmpty())
            || (hoverTexture != null && !hoverTexture.isEmpty())
            || (disabledTexture != null && !disabledTexture.isEmpty());
        if (!hotkeyOnly && (x == null || y == null || ((label == null || label.isEmpty()) && !hasButtonTexture))) {
            result.warnForMachine(itemScope, itemScope + " is missing required fields x, y, and label or texture.");
            return null;
        }
        if (hotkeyOnly && hotkeys.isEmpty()) {
            result.warnForMachine(itemScope, itemScope + " is missing required field key or hotkey.");
            return null;
        }
        if ((action == null || action.isEmpty()) && targetPage != null && !targetPage.isEmpty()) {
            action = "page";
        }
        if (action == null || action.isEmpty()) {
            result.warnForMachine(itemScope, itemScope + " is missing required field action.");
            return null;
        }

        String normalizedAction = normalizeButtonAction(action);
        if (normalizedAction == null) {
            result.warnForMachine(itemScope, itemScope + ".action is invalid: " + action);
            return null;
        }
        if ("page".equals(normalizedAction) && (targetPage == null || targetPage.isEmpty())) {
            result.warnForMachine(itemScope, itemScope + " page action requires targetPage.");
            return null;
        }
        if ("event".equals(normalizedAction) && (buttonId == null || buttonId.isEmpty())) {
            result.warnForMachine(itemScope, itemScope + " event action requires buttonId.");
            return null;
        }
        if ("subgui".equals(normalizedAction) && (targetSubGui == null || targetSubGui.isEmpty())) {
            result.warnForMachine(itemScope, itemScope + " subgui action requires targetSubGui.");
            return null;
        }
        if (("smart_set".equals(normalizedAction) || "smart_add".equals(normalizedAction))
            && (key == null || key.isEmpty())) {
            result.warnForMachine(itemScope, itemScope + " smart action requires key.");
            return null;
        }
        if ("smart_set".equals(normalizedAction) && value == null && stringValue == null) {
            result.warnForMachine(itemScope, itemScope + " smart_set action requires value.");
            return null;
        }
        if ("smart_add".equals(normalizedAction) && value == null) {
            result.warnForMachine(itemScope, itemScope + " smart_add action requires numeric value.");
            return null;
        }

        MachineGuiStyleManager.ButtonStyle button = new MachineGuiStyleManager.ButtonStyle();
        button.id = getTrimmedString(obj, result, itemScope, "id", "name");
        button.x = x == null ? 0 : x.intValue();
        button.y = y == null ? 0 : y.intValue();
        button.width = validateRangeInt(getInt(obj, result, itemScope, "width", "w"), 1, MAX_COMPONENT_SIZE, result, itemScope, "width");
        button.height = validateRangeInt(getInt(obj, result, itemScope, "height", "h"), 1, MAX_COMPONENT_SIZE, result, itemScope, "height");
        button.label = label;
        button.action = normalizedAction;
        button.buttonId = buttonId;
        button.key = key;
        button.value = value;
        // Modifier-key variant values (data-port smart actions only). Absent -> falls back to value.
        button.shiftValue = getOptionalFloat(obj, "shiftValue", "shift_value", "shiftDelta", "shift_delta");
        button.ctrlValue = getOptionalFloat(obj, "ctrlValue", "ctrl_value", "controlValue", "control_value");
        button.ctrlShiftValue = getOptionalFloat(obj, "ctrlShiftValue", "ctrl_shift_value",
            "shiftCtrlValue", "shift_ctrl_value", "ctrlShiftDelta", "ctrl_shift_delta");
        button.stringValue = stringValue;
        button.min = getFloat(obj, result, itemScope, "min", "minimum");
        button.max = getFloat(obj, result, itemScope, "max", "maximum");
        button.targetPage = targetPage;
        button.targetSubGui = targetSubGui;
        button.openMode = openMode;
        button.priority = getInt(obj, result, itemScope, "priority", "zIndex", "z_index", "z", "layer");
        button.visible = hotkeyOnly ? Boolean.FALSE : getBoolean(obj, result, itemScope, "visible", "show", "enabled");
        button.hotkeys = hotkeys.isEmpty() ? null : hotkeys;
        button.consumeHotkey = getBoolean(obj, result, itemScope, "consumeHotkey", "consume_hotkey", "consume", "cancelKey", "cancel_key");
        button.page = getTrimmedString(obj, result, itemScope, "page", "pageId", "page_id", "tab", "state", "stateId", "state_id", "guiState", "gui_state");
        button.texture = texture;
        button.hoverTexture = hoverTexture;
        button.disabledTexture = disabledTexture;
        button.textureWidth = validateRangeInt(getInt(obj, result, itemScope, "textureWidth", "texture_width", "texW"), 1, MAX_COMPONENT_SIZE, result, itemScope, "textureWidth");
        button.textureHeight = validateRangeInt(getInt(obj, result, itemScope, "textureHeight", "texture_height", "texH"), 1, MAX_COMPONENT_SIZE, result, itemScope, "textureHeight");
        button.u = validateMinInt(getInt(obj, result, itemScope, "u", "textureU", "texture_u"), 0, result, itemScope, "u");
        button.v = validateMinInt(getInt(obj, result, itemScope, "v", "textureV", "texture_v"), 0, result, itemScope, "v");
        button.hoverU = validateMinInt(getInt(obj, result, itemScope, "hoverU", "hover_u", "textureHoverU", "texture_hover_u"), 0, result, itemScope, "hoverU");
        button.hoverV = validateMinInt(getInt(obj, result, itemScope, "hoverV", "hover_v", "textureHoverV", "texture_hover_v"), 0, result, itemScope, "hoverV");
        button.disabledU = validateMinInt(getInt(obj, result, itemScope, "disabledU", "disabled_u", "textureDisabledU", "texture_disabled_u"), 0, result, itemScope, "disabledU");
        button.disabledV = validateMinInt(getInt(obj, result, itemScope, "disabledV", "disabled_v", "textureDisabledV", "texture_disabled_v"), 0, result, itemScope, "disabledV");
        button.useNineSlice = getBoolean(obj, result, itemScope, "useNineSlice", "use_nine_slice", "nineSlice", "nine_slice");
        button.corner = validateRangeInt(getInt(obj, result, itemScope, "corner", "cornerSize", "corner_size"), 1, MAX_CORNER, result, itemScope, "corner");
        button.textColor = getColor(obj, result, itemScope, "textColor", "text_color", "labelColor", "label_color");
        button.hoverTextColor = getColor(obj, result, itemScope, "hoverTextColor", "hover_text_color", "labelHoverColor", "label_hover_color");
        button.disabledTextColor = getColor(obj, result, itemScope, "disabledTextColor", "disabled_text_color", "labelDisabledColor", "label_disabled_color");
        button.drawLabel = getBoolean(obj, result, itemScope, "drawLabel", "draw_label", "showLabel", "show_label");
        return button;
    }

    @Nullable
    private static List<MachineGuiStyleManager.TextureLayerStyle> parseTextureLayers(
        JsonObject node,
        MachineFileParseResult result,
        String scope
    ) {
        List<MachineGuiStyleManager.TextureLayerStyle> out = new ArrayList<MachineGuiStyleManager.TextureLayerStyle>();
        appendTextureLayers(out, findElement(node, "textureLayers", "texture_layers", "guiLayers", "gui_layers"), false, result, scope);
        appendTextureLayers(out, findElement(node, "backgroundLayers", "background_layers"), false, result, scope);
        appendTextureLayers(out, findElement(node, "foregroundLayers", "foreground_layers"), true, result, scope);
        return out.isEmpty() ? null : out;
    }

    @Nullable
    private static List<MachineGuiStyleManager.ProgressBarStyle> parseProgressBars(
        JsonObject node,
        MachineFileParseResult result,
        String scope
    ) {
        MatchedElement match = findElement(node, "progressBars", "progress_bars", "guiProgressBars", "gui_progress_bars");
        if (match == null) {
            return null;
        }
        if (!match.element.isJsonArray()) {
            result.warnForMachine(scope, field(scope, match.key) + " must be an array.");
            return null;
        }

        List<MachineGuiStyleManager.ProgressBarStyle> progressBars = new ArrayList<MachineGuiStyleManager.ProgressBarStyle>();
        JsonArray array = match.element.getAsJsonArray();
        int limit = cappedArraySize(array, result, scope, match.key, MAX_ARRAY_ENTRIES);
        for (int i = 0; i < limit; i++) {
            JsonElement child = array.get(i);
            String itemScope = field(scope, match.key + "[" + i + "]");
            if (child == null || !child.isJsonObject()) {
                result.warnForMachine(scope, itemScope + " must be an object.");
                continue;
            }

            JsonObject obj = child.getAsJsonObject();
            Integer x = getInt(obj, result, itemScope, "x");
            Integer y = getInt(obj, result, itemScope, "y");
            Integer width = validateRangeInt(getInt(obj, result, itemScope, "width"), 1, MAX_COMPONENT_SIZE, result, itemScope, "width");
            Integer height = validateRangeInt(getInt(obj, result, itemScope, "height"), 1, MAX_COMPONENT_SIZE, result, itemScope, "height");
            if (x == null || y == null || width == null || height == null) {
                result.warnForMachine(scope, itemScope + " is missing required fields x, y, width or height.");
                continue;
            }

            MachineGuiStyleManager.ProgressBarStyle bar = new MachineGuiStyleManager.ProgressBarStyle();
            bar.id = getTrimmedString(obj, result, itemScope, "id", "name");
            bar.x = x.intValue();
            bar.y = y.intValue();
            bar.width = width.intValue();
            bar.height = height.intValue();
            bar.backgroundColor = getColor(obj, result, itemScope, "backgroundColor", "background_color", "bgColor", "bg_color");
            bar.fillColor = getColor(obj, result, itemScope, "fillColor", "fill_color", "color", "barColor", "bar_color");
            bar.borderColor = getColor(obj, result, itemScope, "borderColor", "border_color", "frameColor", "frame_color");
            bar.texture = getTrimmedString(obj, result, itemScope, "texture", "barTexture", "bar_texture");
            bar.backgroundTexture = getTrimmedString(obj, result, itemScope, "backgroundTexture", "background_texture");
            bar.fillTexture = getTrimmedString(obj, result, itemScope, "fillTexture", "fill_texture");
            bar.textureWidth = validateRangeInt(getInt(obj, result, itemScope, "textureWidth", "texture_width"), 1, MAX_COMPONENT_SIZE, result, itemScope, "textureWidth");
            bar.textureHeight = validateRangeInt(getInt(obj, result, itemScope, "textureHeight", "texture_height"), 1, MAX_COMPONENT_SIZE, result, itemScope, "textureHeight");
            bar.direction = normalizeProgressBarDirection(getTrimmedString(obj, result, itemScope, "direction", "fillDirection", "fill_direction"), result, itemScope);
            bar.source = normalizeProgressBarSource(getTrimmedString(obj, result, itemScope, "source"), result, itemScope);
            bar.threadIndex = getInt(obj, result, itemScope, "threadIndex", "thread_index", "thread");
            bar.coreThreadId = getTrimmedString(obj, result, itemScope, "coreThreadId", "core_thread_id", "coreThread", "core_thread");
            bar.min = getFloat(obj, result, itemScope, "min");
            bar.max = getFloat(obj, result, itemScope, "max");
            bar.priority = getInt(obj, result, itemScope, "priority", "zIndex", "z_index", "z", "layer");
            bar.foreground = getBoolean(obj, result, itemScope, "foreground", "front", "drawForeground");
            bar.visible = getBoolean(obj, result, itemScope, "visible", "show", "enabled");
            bar.page = getTrimmedString(obj, result, itemScope, "page", "pageId", "page_id", "tab", "state", "stateId", "state_id", "guiState", "gui_state");
            bar.showText = getBoolean(obj, result, itemScope, "showText", "show_text", "displayText", "display_text");
            bar.textColor = getColor(obj, result, itemScope, "textColor", "text_color");
            progressBars.add(bar);
        }
        return progressBars.isEmpty() ? null : progressBars;
    }

    @Nullable
    private static List<MachineGuiStyleManager.SliderStyle> parseSliders(
        JsonObject node,
        MachineFileParseResult result,
        String scope
    ) {
        MatchedElement match = findElement(node, "sliders", "guiSliders", "gui_sliders", "rangeControls", "range_controls");
        if (match == null) {
            return null;
        }
        if (!match.element.isJsonArray()) {
            result.warnForMachine(scope, field(scope, match.key) + " must be an array.");
            return null;
        }

        List<MachineGuiStyleManager.SliderStyle> sliders = new ArrayList<MachineGuiStyleManager.SliderStyle>();
        JsonArray array = match.element.getAsJsonArray();
        int limit = cappedArraySize(array, result, scope, match.key, MAX_ARRAY_ENTRIES);
        for (int i = 0; i < limit; i++) {
            JsonElement child = array.get(i);
            String itemScope = field(scope, match.key + "[" + i + "]");
            if (child == null || !child.isJsonObject()) {
                result.warnForMachine(scope, itemScope + " must be an object.");
                continue;
            }

            JsonObject obj = child.getAsJsonObject();
            Integer x = getInt(obj, result, itemScope, "x");
            Integer y = getInt(obj, result, itemScope, "y");
            Integer width = validateRangeInt(getInt(obj, result, itemScope, "width", "w"), 1, MAX_COMPONENT_SIZE, result, itemScope, "width");
            Integer height = validateRangeInt(getInt(obj, result, itemScope, "height", "h"), 1, MAX_COMPONENT_SIZE, result, itemScope, "height");
            String key = getTrimmedString(
                obj,
                result,
                itemScope,
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
            if (x == null || y == null || width == null || height == null || key == null || key.isEmpty()) {
                result.warnForMachine(scope, itemScope + " is missing required fields x, y, width, height or key.");
                continue;
            }

            MachineGuiStyleManager.SliderStyle slider = new MachineGuiStyleManager.SliderStyle();
            slider.id = getTrimmedString(obj, result, itemScope, "id", "name");
            slider.x = x.intValue();
            slider.y = y.intValue();
            slider.width = width.intValue();
            slider.height = height.intValue();
            slider.key = key;
            slider.min = getFloat(obj, result, itemScope, "min", "minimum");
            slider.max = getFloat(obj, result, itemScope, "max", "maximum");
            slider.step = getFloat(obj, result, itemScope, "step", "increment");
            slider.initialValue = getFloat(obj, result, itemScope, "value", "initialValue", "initial_value", "defaultValue", "default_value");
            slider.direction = normalizeSliderDirection(getTrimmedString(obj, result, itemScope, "direction", "axis", "orientation"), result, itemScope);
            slider.trackColor = getColor(obj, result, itemScope, "trackColor", "track_color", "backgroundColor", "background_color", "bgColor", "bg_color");
            slider.fillColor = getColor(obj, result, itemScope, "fillColor", "fill_color", "color", "barColor", "bar_color");
            slider.thumbColor = getColor(obj, result, itemScope, "thumbColor", "thumb_color", "handleColor", "handle_color");
            slider.borderColor = getColor(obj, result, itemScope, "borderColor", "border_color", "frameColor", "frame_color");
            slider.thumbWidth = validateRangeInt(getInt(obj, result, itemScope, "thumbWidth", "thumb_width", "handleWidth", "handle_width"), 1, MAX_COMPONENT_SIZE, result, itemScope, "thumbWidth");
            slider.thumbHeight = validateRangeInt(getInt(obj, result, itemScope, "thumbHeight", "thumb_height", "handleHeight", "handle_height"), 1, MAX_COMPONENT_SIZE, result, itemScope, "thumbHeight");
            slider.priority = getInt(obj, result, itemScope, "priority", "zIndex", "z_index", "z", "layer");
            slider.foreground = getBoolean(obj, result, itemScope, "foreground", "front", "drawForeground");
            slider.visible = getBoolean(obj, result, itemScope, "visible", "show", "enabled");
            slider.page = getTrimmedString(obj, result, itemScope, "page", "pageId", "page_id", "tab", "state", "stateId", "state_id", "guiState", "gui_state");
            slider.showText = getBoolean(obj, result, itemScope, "showText", "show_text", "displayText", "display_text");
            slider.textColor = getColor(obj, result, itemScope, "textColor", "text_color");
            sliders.add(slider);
        }
        return sliders.isEmpty() ? null : sliders;
    }

    @Nullable
    private static List<MachineGuiStyleManager.DynamicVisualStyle> parseDynamicVisuals(
        JsonObject node,
        MachineFileParseResult result,
        String scope
    ) {
        MatchedElement match = findElement(node, "dynamicVisuals", "dynamic_visuals", "visuals", "dynamicWidgets", "dynamic_widgets");
        if (match == null) {
            return null;
        }
        if (!match.element.isJsonArray()) {
            result.warnForMachine(scope, field(scope, match.key) + " must be an array.");
            return null;
        }

        List<MachineGuiStyleManager.DynamicVisualStyle> visuals = new ArrayList<MachineGuiStyleManager.DynamicVisualStyle>();
        JsonArray array = match.element.getAsJsonArray();
        int limit = cappedArraySize(array, result, scope, match.key, MAX_ARRAY_ENTRIES);
        for (int i = 0; i < limit; i++) {
            JsonElement child = array.get(i);
            String itemScope = field(scope, match.key + "[" + i + "]");
            if (child == null || !child.isJsonObject()) {
                result.warnForMachine(scope, itemScope + " must be an object.");
                continue;
            }

            JsonObject obj = child.getAsJsonObject();
            Integer x = getInt(obj, result, itemScope, "x");
            Integer y = getInt(obj, result, itemScope, "y");
            Integer width = validateRangeInt(getInt(obj, result, itemScope, "width", "w"), 1, MAX_COMPONENT_SIZE, result, itemScope, "width");
            Integer height = validateRangeInt(getInt(obj, result, itemScope, "height", "h"), 1, MAX_COMPONENT_SIZE, result, itemScope, "height");
            if (x == null || y == null || width == null || height == null) {
                result.warnForMachine(scope, itemScope + " is missing required fields x, y, width or height.");
                continue;
            }

            MachineGuiStyleManager.DynamicVisualStyle visual = new MachineGuiStyleManager.DynamicVisualStyle();
            visual.id = getTrimmedString(obj, result, itemScope, "id", "name");
            visual.x = x.intValue();
            visual.y = y.intValue();
            visual.width = width.intValue();
            visual.height = height.intValue();
            visual.priority = getInt(obj, result, itemScope, "priority", "zIndex", "z_index", "z", "layer");
            visual.foreground = getBoolean(obj, result, itemScope, "foreground", "front", "drawForeground");
            visual.visible = getBoolean(obj, result, itemScope, "visible", "show", "enabled");
            visual.page = getTrimmedString(obj, result, itemScope, "page", "pageId", "page_id", "tab", "state", "stateId", "state_id", "guiState", "gui_state");
            visual.transform = parseDynamicVisualTransform(obj, result, itemScope);
            visual.transformByValue = parseDynamicVisualTransformByValue(obj, result, itemScope);
            visual.source = parseDynamicVisualSource(obj, result, itemScope);
            visual.history = parseDynamicVisualHistory(obj, result, itemScope);
            visual.renderer = parseDynamicVisualRenderer(obj, result, itemScope);
            if (visual.renderer == null) {
                result.warnForMachine(scope, itemScope + " is missing required renderer.");
                continue;
            }
            visuals.add(visual);
        }
        return visuals.isEmpty() ? null : visuals;
    }

    @Nullable
    private static MachineGuiStyleManager.DynamicVisualTransformStyle parseDynamicVisualTransform(
        JsonObject visualObj,
        MachineFileParseResult result,
        String scope
    ) {
        JsonObject transformObj = getObject(visualObj, result, scope, "transform", "visualTransform", "visual_transform");
        MachineGuiStyleManager.DynamicVisualTransformStyle transform = new MachineGuiStyleManager.DynamicVisualTransformStyle();
        transform.offsetX = getFloat(visualObj, result, scope, "offsetX", "offset_x", "dx");
        transform.offsetY = getFloat(visualObj, result, scope, "offsetY", "offset_y", "dy");
        transform.scale = normalizePositiveScale(getFloat(visualObj, result, scope, "scale"));
        transform.scaleX = normalizePositiveScale(getFloat(visualObj, result, scope, "scaleX", "scale_x"));
        transform.scaleY = normalizePositiveScale(getFloat(visualObj, result, scope, "scaleY", "scale_y"));
        transform.rotation = getFloat(visualObj, result, scope, "rotation", "rotate", "angle");
        transform.alpha = normalizeAlpha(getFloat(visualObj, result, scope, "alpha", "opacity", "transparency"));
        transform.origin = normalizeDynamicTransformOrigin(getTrimmedString(visualObj, result, scope, "origin", "pivot", "anchor"), result, scope);
        if (transformObj != null) {
            String transformScope = field(scope, "transform");
            Float offsetX = getFloat(transformObj, result, transformScope, "offsetX", "offset_x", "dx");
            Float offsetY = getFloat(transformObj, result, transformScope, "offsetY", "offset_y", "dy");
            Float scale = normalizePositiveScale(getFloat(transformObj, result, transformScope, "scale"));
            Float scaleX = normalizePositiveScale(getFloat(transformObj, result, transformScope, "scaleX", "scale_x"));
            Float scaleY = normalizePositiveScale(getFloat(transformObj, result, transformScope, "scaleY", "scale_y"));
            Float rotation = getFloat(transformObj, result, transformScope, "rotation", "rotate", "angle");
            Float alpha = normalizeAlpha(getFloat(transformObj, result, transformScope, "alpha", "opacity", "transparency"));
            String origin = normalizeDynamicTransformOrigin(getTrimmedString(transformObj, result, transformScope, "origin", "pivot", "anchor"), result, transformScope);
            if (offsetX != null) transform.offsetX = offsetX;
            if (offsetY != null) transform.offsetY = offsetY;
            if (scale != null) transform.scale = scale;
            if (scaleX != null) transform.scaleX = scaleX;
            if (scaleY != null) transform.scaleY = scaleY;
            if (rotation != null) transform.rotation = rotation;
            if (alpha != null) transform.alpha = alpha;
            if (origin != null) transform.origin = origin;
        }
        if (transform.offsetX == null
            && transform.offsetY == null
            && transform.scale == null
            && transform.scaleX == null
            && transform.scaleY == null
            && transform.rotation == null
            && transform.alpha == null
            && transform.origin == null) {
            return null;
        }
        return transform;
    }

    @Nullable
    private static MachineGuiStyleManager.DynamicVisualTransformByValueStyle parseDynamicVisualTransformByValue(
        JsonObject visualObj,
        MachineFileParseResult result,
        String scope
    ) {
        JsonObject transformObj = getObject(visualObj, result, scope, "transformByValue", "transform_by_value", "dynamicTransform", "dynamic_transform");
        if (transformObj == null) {
            return null;
        }
        MachineGuiStyleManager.DynamicVisualTransformByValueStyle transform = new MachineGuiStyleManager.DynamicVisualTransformByValueStyle();
        transform.offsetX = parseDynamicDrivenValue(transformObj, result, field(scope, "transformByValue"), "offsetX", "offset_x", "dx");
        transform.offsetY = parseDynamicDrivenValue(transformObj, result, field(scope, "transformByValue"), "offsetY", "offset_y", "dy");
        transform.scale = parseDynamicDrivenValue(transformObj, result, field(scope, "transformByValue"), "scale");
        transform.scaleX = parseDynamicDrivenValue(transformObj, result, field(scope, "transformByValue"), "scaleX", "scale_x");
        transform.scaleY = parseDynamicDrivenValue(transformObj, result, field(scope, "transformByValue"), "scaleY", "scale_y");
        transform.rotation = parseDynamicDrivenValue(transformObj, result, field(scope, "transformByValue"), "rotation", "rotate", "angle");
        transform.alpha = parseDynamicDrivenValue(transformObj, result, field(scope, "transformByValue"), "alpha", "opacity", "transparency");
        if (transform.offsetX == null
            && transform.offsetY == null
            && transform.scale == null
            && transform.scaleX == null
            && transform.scaleY == null
            && transform.rotation == null
            && transform.alpha == null) {
            return null;
        }
        return transform;
    }

    @Nullable
    private static MachineGuiStyleManager.DynamicVisualDrivenValueStyle parseDynamicDrivenValue(
        JsonObject parentObj,
        MachineFileParseResult result,
        String scope,
        String... keys
    ) {
        MatchedElement match = findElement(parentObj, keys);
        if (match == null) {
            return null;
        }
        String itemScope = field(scope, match.key);
        if (!match.element.isJsonObject()) {
            result.warnForMachine(scope, itemScope + " must be an object.");
            return null;
        }
        JsonObject obj = match.element.getAsJsonObject();
        MachineGuiStyleManager.DynamicVisualDrivenValueStyle driven = new MachineGuiStyleManager.DynamicVisualDrivenValueStyle();
        driven.min = getFloat(obj, result, itemScope, "min", "minimum", "from");
        driven.max = getFloat(obj, result, itemScope, "max", "maximum", "to");
        if (isLikelyDynamicVisualSourceObject(obj)) {
            driven.source = parseDynamicVisualSource(obj, result, itemScope);
        } else {
            JsonObject sourceObj = getObject(obj, result, itemScope, "source", "dataSource", "data_source");
            if (sourceObj != null) {
                driven.source = parseDynamicVisualSource(sourceObj, result, itemScope + ".source");
            }
        }
        if (driven.min == null && driven.max == null && driven.source == null) {
            return null;
        }
        return driven;
    }

    private static boolean isLikelyDynamicVisualSourceObject(JsonObject obj) {
        return obj.has("type")
            || obj.has("sourceType")
            || obj.has("source_type")
            || obj.has("key")
            || obj.has("customKey")
            || obj.has("custom_key")
            || obj.has("metric")
            || obj.has("machineMetric")
            || obj.has("machine_metric")
            || obj.has("default")
            || obj.has("defaultValue")
            || obj.has("default_value")
            || obj.has("fallback")
            || obj.has("fallbackValue")
            || obj.has("fallback_value");
    }

    private static MachineGuiStyleManager.DynamicVisualSourceStyle parseDynamicVisualSource(
        JsonObject visualObj,
        MachineFileParseResult result,
        String scope
    ) {
        JsonObject sourceObj = getObject(visualObj, result, scope, "source", "dataSource", "data_source");
        if (sourceObj == null) {
            sourceObj = visualObj;
        }
        MachineGuiStyleManager.DynamicVisualSourceStyle source = new MachineGuiStyleManager.DynamicVisualSourceStyle();
        source.type = normalizeDynamicSourceType(getTrimmedString(sourceObj, result, scope, "type", "sourceType", "source_type"), result, scope);
        source.key = getTrimmedString(
            sourceObj,
            result,
            scope,
            "key",
            "customKey",
            "custom_key",
            "virtualKey",
            "virtual_key",
            "dataPortKey",
            "data_port_key"
        );
        source.metric = normalizeDynamicMetric(getTrimmedString(sourceObj, result, scope, "metric", "machineMetric", "machine_metric"), result, scope);
        source.defaultValue = getFloat(sourceObj, result, scope, "default", "defaultValue", "default_value", "fallback", "fallbackValue", "fallback_value");
        source.min = getFloat(sourceObj, result, scope, "min", "minimum");
        source.max = getFloat(sourceObj, result, scope, "max", "maximum");
        source.clamp = getBoolean(sourceObj, result, scope, "clamp", "bounded");
        source.invert = getBoolean(sourceObj, result, scope, "invert", "inverted", "reverse");
        if (source.type == null) {
            source.type = source.key == null || source.key.isEmpty() ? "machine" : "customData";
        }
        if ("customData".equals(source.type) && (source.key == null || source.key.isEmpty())) {
            result.warnForMachine(scope, field(scope, "source.key") + " is required for customData dynamic visuals.");
        }
        if ("machine".equals(source.type) && (source.metric == null || source.metric.isEmpty())) {
            source.metric = "recipeProgress";
        }
        return source;
    }

    @Nullable
    private static MachineGuiStyleManager.DynamicVisualHistoryStyle parseDynamicVisualHistory(
        JsonObject visualObj,
        MachineFileParseResult result,
        String scope
    ) {
        JsonObject historyObj = getObject(visualObj, result, scope, "history", "sampleHistory", "sample_history");
        if (historyObj == null) {
            return null;
        }
        MachineGuiStyleManager.DynamicVisualHistoryStyle history = new MachineGuiStyleManager.DynamicVisualHistoryStyle();
        history.enabled = getBoolean(historyObj, result, scope, "enabled", "enable", "visible");
        history.samples = validateRangeInt(getInt(historyObj, result, scope, "samples", "sampleCount", "sample_count"), 2, MAX_ARRAY_ENTRIES, result, scope, "samples");
        history.intervalTicks = validateRangeInt(getInt(historyObj, result, scope, "intervalTicks", "interval_ticks", "interval", "tickInterval", "tick_interval"), 1, 1200, result, scope, "intervalTicks");
        return history;
    }

    @Nullable
    private static MachineGuiStyleManager.DynamicVisualRendererStyle parseDynamicVisualRenderer(
        JsonObject visualObj,
        MachineFileParseResult result,
        String scope
    ) {
        JsonObject rendererObj = getObject(visualObj, result, scope, "renderer", "render", "visual");
        if (rendererObj == null) {
            rendererObj = visualObj;
        }
        MachineGuiStyleManager.DynamicVisualRendererStyle renderer = new MachineGuiStyleManager.DynamicVisualRendererStyle();
        renderer.type = normalizeDynamicRendererType(getTrimmedString(rendererObj, result, scope, "type", "rendererType", "renderer_type"), result, scope);
        if (renderer.type == null) {
            renderer.type = "fill";
        }
        renderer.direction = normalizeDynamicFillDirection(getTrimmedString(rendererObj, result, scope, "direction", "fillDirection", "fill_direction"), result, scope);
        renderer.backgroundTexture = getTrimmedString(rendererObj, result, scope, "backgroundTexture", "background_texture", "emptyTexture", "empty_texture");
        renderer.fillTexture = getTrimmedString(rendererObj, result, scope, "fillTexture", "fill_texture", "texture", "fullTexture", "full_texture");
        renderer.fallbackTexture = getTrimmedString(rendererObj, result, scope, "fallbackTexture", "fallback_texture", "defaultTexture", "default_texture");
        renderer.backgroundColor = getColor(rendererObj, result, scope, "backgroundColor", "background_color", "bgColor", "bg_color");
        renderer.fillColor = getColor(rendererObj, result, scope, "fillColor", "fill_color");
        renderer.borderColor = getColor(rendererObj, result, scope, "borderColor", "border_color", "frameColor", "frame_color");
        renderer.color = getColor(rendererObj, result, scope, "color", "pieColor", "pie_color");
        renderer.lineColor = getColor(rendererObj, result, scope, "lineColor", "line_color");
        renderer.gridColor = getColor(rendererObj, result, scope, "gridColor", "grid_color");
        renderer.textureWidth = validateRangeInt(getInt(rendererObj, result, scope, "textureWidth", "texture_width", "texW"), 1, MAX_COMPONENT_SIZE, result, scope, "textureWidth");
        renderer.textureHeight = validateRangeInt(getInt(rendererObj, result, scope, "textureHeight", "texture_height", "texH"), 1, MAX_COMPONENT_SIZE, result, scope, "textureHeight");
        renderer.mode = normalizeDynamicPieMode(getTrimmedString(rendererObj, result, scope, "mode", "pieMode", "pie_mode"), result, scope);
        renderer.startAngle = getFloat(rendererObj, result, scope, "startAngle", "start_angle", "angle");
        renderer.innerRadius = validateRangeInt(getInt(rendererObj, result, scope, "innerRadius", "inner_radius"), 0, MAX_COMPONENT_SIZE, result, scope, "innerRadius");
        renderer.segments = validateRangeInt(getInt(rendererObj, result, scope, "segments", "segmentCount", "segment_count"), 3, 360, result, scope, "segments");
        renderer.lineWidth = validateRangeInt(getInt(rendererObj, result, scope, "lineWidth", "line_width"), 1, 16, result, scope, "lineWidth");
        renderer.showGrid = getBoolean(rendererObj, result, scope, "showGrid", "show_grid", "grid");
        renderer.frames = parseDynamicVisualFrames(rendererObj, result, scope);
        return renderer;
    }

    @Nullable
    private static List<MachineGuiStyleManager.DynamicVisualFrameStyle> parseDynamicVisualFrames(
        JsonObject rendererObj,
        MachineFileParseResult result,
        String scope
    ) {
        MatchedElement match = findElement(rendererObj, "frames", "states", "textures");
        if (match == null) {
            return null;
        }
        if (!match.element.isJsonArray()) {
            result.warnForMachine(scope, field(scope, match.key) + " must be an array.");
            return null;
        }
        List<MachineGuiStyleManager.DynamicVisualFrameStyle> frames = new ArrayList<MachineGuiStyleManager.DynamicVisualFrameStyle>();
        JsonArray array = match.element.getAsJsonArray();
        int limit = cappedArraySize(array, result, scope, match.key, MAX_ARRAY_ENTRIES);
        for (int i = 0; i < limit; i++) {
            JsonElement child = array.get(i);
            String itemScope = field(scope, match.key + "[" + i + "]");
            if (child == null || !child.isJsonObject()) {
                result.warnForMachine(scope, itemScope + " must be an object.");
                continue;
            }
            JsonObject obj = child.getAsJsonObject();
            MachineGuiStyleManager.DynamicVisualFrameStyle frame = new MachineGuiStyleManager.DynamicVisualFrameStyle();
            frame.min = getFloat(obj, result, itemScope, "min", "minimum");
            frame.max = getFloat(obj, result, itemScope, "max", "maximum");
            frame.equals = getFloat(obj, result, itemScope, "equals", "eq", "value");
            frame.texture = getTrimmedString(obj, result, itemScope, "texture", "path", "resource");
            if (frame.texture == null || frame.texture.isEmpty()) {
                result.warnForMachine(scope, itemScope + " is missing required field texture.");
                continue;
            }
            frames.add(frame);
        }
        return frames.isEmpty() ? null : frames;
    }

    @Nullable
    private static List<MachineGuiStyleManager.SubGuiStyle> parseSubGuis(
        JsonObject node,
        MachineFileParseResult result,
        String scope
    ) {
        MatchedElement match = findElement(node, "subGuis", "sub_guis", "subGuiList", "sub_gui_list");
        if (match == null) {
            return null;
        }
        if (!match.element.isJsonArray()) {
            result.warnForMachine(scope, field(scope, match.key) + " must be an array.");
            return null;
        }

        List<MachineGuiStyleManager.SubGuiStyle> subGuis = new ArrayList<MachineGuiStyleManager.SubGuiStyle>();
        JsonArray array = match.element.getAsJsonArray();
        int limit = cappedArraySize(array, result, scope, match.key, MAX_ARRAY_ENTRIES);
        for (int i = 0; i < limit; i++) {
            MachineGuiStyleManager.SubGuiStyle subGui = parseSubGuiStyle(
                array.get(i),
                result,
                field(scope, match.key + "[" + i + "]")
            );
            if (subGui != null) {
                subGuis.add(subGui);
            }
        }
        return subGuis.isEmpty() ? null : subGuis;
    }

    @Nullable
    private static MachineGuiStyleManager.SubGuiStyle parseSubGuiStyle(
        @Nullable JsonElement node,
        MachineFileParseResult result,
        String scope
    ) {
        if (node == null || !node.isJsonObject()) {
            result.warnForMachine(scope, scope + " must be an object.");
            return null;
        }

        JsonObject obj = node.getAsJsonObject();
        String id = getTrimmedString(obj, result, scope, "id", "name", "subGuiId", "sub_gui_id", "subguiId", "subgui_id");
        if (id == null || id.isEmpty()) {
            result.warnForMachine(scope, scope + " is missing required field id.");
            return null;
        }

        MachineGuiStyleManager.SubGuiStyle subGui = new MachineGuiStyleManager.SubGuiStyle();
        subGui.id = id;
        subGui.mode = normalizeSubGuiMode(getTrimmedString(obj, result, scope, "mode", "openMode", "open_mode", "displayMode", "display_mode"));
        subGui.draggable = getBoolean(obj, result, scope, "draggable", "subGuiDraggable", "sub_gui_draggable");
        subGui.dragHandle = getBoolean(obj, result, scope, "dragHandle", "drag_handle", "useDragHandle", "use_drag_handle");
        subGui.dragX = getInt(obj, result, scope, "dragX", "drag_x", "dragHandleX", "drag_handle_x");
        subGui.dragY = getInt(obj, result, scope, "dragY", "drag_y", "dragHandleY", "drag_handle_y");
        subGui.dragWidth = validateRangeInt(getInt(obj, result, scope, "dragWidth", "drag_width", "dragHandleWidth", "drag_handle_width"), 1, MAX_COMPONENT_SIZE, result, scope, "dragWidth");
        subGui.dragHeight = validateRangeInt(getInt(obj, result, scope, "dragHeight", "drag_height", "dragHandleHeight", "drag_handle_height"), 1, MAX_COMPONENT_SIZE, result, scope, "dragHeight");
        subGui.x = getInt(obj, result, scope, "x");
        subGui.y = getInt(obj, result, scope, "y");
        subGui.width = validateRangeInt(getInt(obj, result, scope, "width", "w", "guiWidth", "gui_width"), 1, MAX_COMPONENT_SIZE, result, scope, "width");
        subGui.height = validateRangeInt(getInt(obj, result, scope, "height", "h", "guiHeight", "gui_height"), 1, MAX_COMPONENT_SIZE, result, scope, "height");

        JsonObject styleNode = getObject(obj, result, scope, "style", "gui", "controllerStyle", "controller_style");
        if (styleNode != null) {
            subGui.style = parseStyle(styleNode, result, scope + ".style");
        } else {
            MachineGuiStyleManager.ControllerStyle inlineStyle = parseStyle(obj, result, scope);
            subGui.style = inlineStyle == MachineGuiStyleManager.ControllerStyle.EMPTY ? null : inlineStyle;
        }

        if (subGui.style != null) {
            if (subGui.width != null) {
                subGui.style.guiWidth = subGui.width;
            }
            if (subGui.height != null) {
                subGui.style.guiHeight = subGui.height;
            }
        }

        return subGui.isEmpty() ? null : subGui;
    }

    @Nullable
    private static List<String> parseStringArray(
        JsonObject node,
        MachineFileParseResult result,
        String scope,
        String... keys
    ) {
        MatchedElement match = findElement(node, keys);
        if (match == null) {
            return null;
        }
        if (!match.element.isJsonArray()) {
            result.warnForMachine(scope, field(scope, match.key) + " must be an array.");
            return null;
        }

        List<String> values = new ArrayList<String>();
        JsonArray array = match.element.getAsJsonArray();
        int limit = cappedArraySize(array, result, scope, match.key, MAX_ARRAY_ENTRIES);
        for (int i = 0; i < limit; i++) {
            JsonElement child = array.get(i);
            String itemScope = field(scope, match.key + "[" + i + "]");
            if (child == null || !child.isJsonPrimitive() || !child.getAsJsonPrimitive().isString()) {
                result.warnForMachine(scope, itemScope + " must be a string.");
                continue;
            }

            String value = safeTrim(child.getAsString());
            if (value.isEmpty()) {
                result.warnForMachine(scope, itemScope + " must not be empty.");
                continue;
            }
            values.add(value);
        }

        return values.isEmpty() ? null : values;
    }

    private static List<String> parseHotkeys(
        JsonObject node,
        MachineFileParseResult result,
        String scope,
        boolean includeKeyAlias
    ) {
        List<String> values = new ArrayList<String>();
        appendHotkeyValues(values, node, result, scope, "hotkey", "hotKey", "shortcut");
        appendHotkeyValues(values, node, result, scope, "hotkeys", "hotKeys", "shortcuts");
        if (includeKeyAlias) {
            appendHotkeyValues(values, node, result, scope, "key");
        }
        return values;
    }

    private static void appendHotkeyValues(
        List<String> values,
        JsonObject node,
        MachineFileParseResult result,
        String scope,
        String... keys
    ) {
        MatchedElement match = findElement(node, keys);
        if (match == null) {
            return;
        }
        if (match.element.isJsonArray()) {
            JsonArray array = match.element.getAsJsonArray();
            int limit = cappedArraySize(array, result, scope, match.key, MAX_ARRAY_ENTRIES);
            for (int i = 0; i < limit; i++) {
                addHotkeyValue(values, array.get(i), result, field(scope, match.key + "[" + i + "]"));
            }
            return;
        }
        addHotkeyValue(values, match.element, result, field(scope, match.key));
    }

    private static void addHotkeyValue(
        List<String> values,
        @Nullable JsonElement element,
        MachineFileParseResult result,
        String scope
    ) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            result.warn(scope + " must be a string.");
            return;
        }
        String value = safeTrim(element.getAsString());
        if (value.isEmpty()) {
            result.warn(scope + " must not be empty.");
            return;
        }
        if (!values.contains(value)) {
            values.add(value);
        }
    }

    private static void appendTextureLayers(
        List<MachineGuiStyleManager.TextureLayerStyle> out,
        @Nullable MatchedElement match,
        boolean forceForeground,
        MachineFileParseResult result,
        String scope
    ) {
        if (match == null) {
            return;
        }
        if (!match.element.isJsonArray()) {
            result.warnForMachine(scope, field(scope, match.key) + " must be an array.");
            return;
        }

        JsonArray array = match.element.getAsJsonArray();
        int remaining = MAX_TEXTURE_LAYERS - out.size();
        if (remaining <= 0) {
            result.warnForMachine(scope, "Ignoring " + field(scope, match.key) + " because max texture layers is " + MAX_TEXTURE_LAYERS + ".");
            return;
        }
        int limit = cappedArraySize(array, result, scope, match.key, remaining);
        for (int i = 0; i < limit; i++) {
            MachineGuiStyleManager.TextureLayerStyle layer = parseTextureLayer(
                array.get(i),
                forceForeground,
                result,
                field(scope, match.key + "[" + i + "]")
            );
            if (layer != null) {
                out.add(layer);
            }
        }
    }

    @Nullable
    private static MachineGuiStyleManager.TextureLayerStyle parseTextureLayer(
        @Nullable JsonElement node,
        boolean forceForeground,
        MachineFileParseResult result,
        String scope
    ) {
        if (node == null) {
            result.warnForMachine(scope, scope + " is null.");
            return null;
        }
        if (node.isJsonPrimitive()) {
            if (!node.getAsJsonPrimitive().isString()) {
                result.warnForMachine(scope, scope + " must be a string or object.");
                return null;
            }
            String raw = safeTrim(node.getAsString());
            if (raw.isEmpty()) {
                result.warnForMachine(scope, scope + " must not be an empty texture path.");
                return null;
            }
            MachineGuiStyleManager.TextureLayerStyle layer = new MachineGuiStyleManager.TextureLayerStyle();
            layer.texture = raw;
            layer.id = null;
            layer.foreground = Boolean.valueOf(forceForeground);
            return layer;
        }
        if (!node.isJsonObject()) {
            result.warnForMachine(scope, scope + " must be a string or object.");
            return null;
        }

        JsonObject obj = node.getAsJsonObject();
        String texture = getTrimmedString(obj, result, scope, "texture", "backgroundTexture", "resource", "path");
        if (texture == null || texture.isEmpty()) {
            result.warnForMachine(scope, scope + " is missing required field texture.");
            return null;
        }

        MachineGuiStyleManager.TextureLayerStyle layer = new MachineGuiStyleManager.TextureLayerStyle();
        layer.id = getTrimmedString(obj, result, scope, "id", "name", "layerId", "layer_id");
        layer.texture = texture;
        layer.offsetX = getInt(obj, result, scope, "offsetX", "offset_x", "x");
        layer.offsetY = getInt(obj, result, scope, "offsetY", "offset_y", "y");
        layer.width = validateRangeInt(getInt(obj, result, scope, "width", "drawWidth", "draw_width"), 1, MAX_COMPONENT_SIZE, result, scope, "width");
        layer.height = validateRangeInt(getInt(obj, result, scope, "height", "drawHeight", "draw_height"), 1, MAX_COMPONENT_SIZE, result, scope, "height");
        layer.textureWidth = validateRangeInt(getInt(obj, result, scope, "textureWidth", "texture_width", "texW"), 1, MAX_COMPONENT_SIZE, result, scope, "textureWidth");
        layer.textureHeight = validateRangeInt(getInt(obj, result, scope, "textureHeight", "texture_height", "texH"), 1, MAX_COMPONENT_SIZE, result, scope, "textureHeight");
        layer.corner = validateRangeInt(getInt(obj, result, scope, "corner", "cornerSize", "corner_size"), 1, MAX_CORNER, result, scope, "corner");
        layer.useNineSlice = getBoolean(obj, result, scope, "useNineSlice", "use_nine_slice", "nineSlice");
        layer.priority = getInt(obj, result, scope, "priority", "zIndex", "z_index", "z", "layer");
        layer.alpha = normalizeAlpha(getFloat(obj, result, scope, "alpha", "opacity", "transparency"));
        layer.foreground = forceForeground
            ? Boolean.TRUE
            : getBoolean(obj, result, scope, "foreground", "front", "drawForeground");
        layer.page = getTrimmedString(obj, result, scope, "page", "pageId", "page_id", "tab", "state", "stateId", "state_id", "guiState", "gui_state");
        return layer;
    }

    @Nullable
    private static String normalizeButtonAction(@Nullable String raw) {
        String text = safeTrim(raw).toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return null;
        }
        if ("page".equals(text) || "switchpage".equals(text) || "switch_page".equals(text)
            || "page_switch".equals(text) || "setpage".equals(text) || "set_page".equals(text)
            || "state".equals(text) || "switch_state".equals(text) || "set_state".equals(text)
            || "gui_state".equals(text) || "switch_gui_state".equals(text) || "set_gui_state".equals(text)) {
            return "page";
        }
        if ("event".equals(text) || "click_event".equals(text) || "button_event".equals(text)) {
            return "event";
        }
        if ("subgui".equals(text) || "sub_gui".equals(text) || "open_subgui".equals(text) || "open_sub_gui".equals(text)) {
            return "subgui";
        }
        if ("close_subgui".equals(text) || "close_sub_gui".equals(text) || "subgui_close".equals(text) || "sub_gui_close".equals(text)) {
            return "close_subgui";
        }
        if ("smart_set".equals(text) || "smartset".equals(text) || "set".equals(text)
            || "data_set".equals(text) || "data_port_set".equals(text) || "port_set".equals(text)) {
            return "smart_set";
        }
        if ("smart_add".equals(text) || "smartadd".equals(text) || "add".equals(text)
            || "increment".equals(text) || "inc".equals(text)
            || "data_add".equals(text) || "data_port_add".equals(text) || "port_add".equals(text)) {
            return "smart_add";
        }
        return null;
    }

    @Nullable
    private static String normalizeSubGuiMode(@Nullable String raw) {
        String text = safeTrim(raw).toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return null;
        }
        if ("modal".equals(text) || "overlay".equals(text) || "dialog".equals(text) || "popup".equals(text)) {
            return "modal";
        }
        if ("replace".equals(text) || "swap".equals(text) || "fullscreen".equals(text) || "full".equals(text)) {
            return "replace";
        }
        return null;
    }

    @Nullable
    private static JsonObject getObject(JsonObject obj, MachineFileParseResult result, String scope, String... keys) {
        MatchedElement match = findElement(obj, keys);
        if (match == null) {
            return null;
        }
        if (!match.element.isJsonObject()) {
            result.warn(field(scope, match.key) + " must be an object.");
            return null;
        }
        return match.element.getAsJsonObject();
    }

    @Nullable
    private static Integer getInt(JsonObject obj, MachineFileParseResult result, String scope, String... keys) {
        MatchedElement match = findElement(obj, keys);
        if (match == null) {
            return null;
        }
        if (!match.element.isJsonPrimitive()) {
            result.warn(field(scope, match.key) + " must be a number.");
            return null;
        }
        try {
            return Integer.valueOf(match.element.getAsInt());
        } catch (Exception ex) {
            result.warn(field(scope, match.key) + " must be a valid integer.");
            return null;
        }
    }

    @Nullable
    private static String getTrimmedString(JsonObject obj, MachineFileParseResult result, String scope, String... keys) {
        MatchedElement match = findElement(obj, keys);
        if (match == null) {
            return null;
        }
        if (!match.element.isJsonPrimitive()) {
            result.warn(field(scope, match.key) + " must be a string.");
            return null;
        }
        if (!match.element.getAsJsonPrimitive().isString()) {
            result.warn(field(scope, match.key) + " must be a string.");
            return null;
        }
        try {
            return safeTrim(match.element.getAsString());
        } catch (Exception ex) {
            result.warn(field(scope, match.key) + " must be a valid string.");
            return null;
        }
    }

    @Nullable
    private static String getValueAsString(JsonObject obj, MachineFileParseResult result, String scope, String... keys) {
        MatchedElement match = findElement(obj, keys);
        if (match == null) {
            return null;
        }
        if (!match.element.isJsonPrimitive()) {
            result.warn(field(scope, match.key) + " must be a primitive value.");
            return null;
        }
        try {
            return match.element.getAsString();
        } catch (Exception ex) {
            result.warn(field(scope, match.key) + " must be a valid primitive value.");
            return null;
        }
    }

    @Nullable
    private static Boolean getBoolean(JsonObject obj, MachineFileParseResult result, String scope, String... keys) {
        MatchedElement match = findElement(obj, keys);
        if (match == null) {
            return null;
        }
        if (!match.element.isJsonPrimitive()) {
            result.warn(field(scope, match.key) + " must be a boolean.");
            return null;
        }
        try {
            if (match.element.getAsJsonPrimitive().isBoolean()) {
                return Boolean.valueOf(match.element.getAsBoolean());
            }
            String raw = safeTrim(match.element.getAsString());
            if ("true".equalsIgnoreCase(raw)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(raw)) {
                return Boolean.FALSE;
            }
        } catch (Exception ignored) {
        }
        result.warn(field(scope, match.key) + " must be true or false.");
        return null;
    }

    @Nullable
    private static Float getFloat(JsonObject obj, MachineFileParseResult result, String scope, String... keys) {
        MatchedElement match = findElement(obj, keys);
        if (match == null) {
            return null;
        }
        if (!match.element.isJsonPrimitive()) {
            result.warn(field(scope, match.key) + " must be a number.");
            return null;
        }
        try {
            float value = match.element.getAsFloat();
            if (!Float.isFinite(value)) {
                result.warn(field(scope, match.key) + " must be a finite number.");
                return null;
            }
            return Float.valueOf(value);
        } catch (Exception ex) {
            result.warn(field(scope, match.key) + " must be a valid number.");
            return null;
        }
    }

    @Nullable
    private static Float getOptionalFloat(JsonObject obj, String... keys) {
        MatchedElement match = findElement(obj, keys);
        if (match == null || !match.element.isJsonPrimitive()) {
            return null;
        }
        try {
            float value = match.element.getAsFloat();
            return Float.isFinite(value) ? Float.valueOf(value) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static Float normalizeAlpha(@Nullable Float value) {
        if (value == null) {
            return null;
        }
        float alpha = value.floatValue();
        if (alpha > 1.0F && alpha <= 255.0F) {
            alpha /= 255.0F;
        }
        if (alpha < 0.0F) {
            alpha = 0.0F;
        }
        if (alpha > 1.0F) {
            alpha = 1.0F;
        }
        return Float.valueOf(alpha);
    }

    @Nullable
    private static Float normalizeScale(@Nullable Float value) {
        if (value == null) {
            return null;
        }
        return Float.valueOf(Math.max(MIN_TEXT_SCALE, Math.min(MAX_TEXT_SCALE, value.floatValue())));
    }

    @Nullable
    private static Integer getColor(JsonObject obj, MachineFileParseResult result, String scope, String... keys) {
        MatchedElement match = findElement(obj, keys);
        if (match == null) {
            return null;
        }
        if (!match.element.isJsonPrimitive()) {
            result.warn(field(scope, match.key) + " must be a number or hex string color.");
            return null;
        }

        try {
            if (match.element.getAsJsonPrimitive().isNumber()) {
                int value = match.element.getAsInt();
                if ((value & 0xFF000000) == 0) {
                    value |= 0xFF000000;
                }
                return Integer.valueOf(value);
            }

            Integer parsed = parseColorString(match.element.getAsString());
            if (parsed != null) {
                return parsed;
            }
        } catch (Exception ignored) {
        }

        result.warn(field(scope, match.key) + " must be hex RGB/RGBA, for example B2E5FF or FFB2E5FF.");
        return null;
    }

    @Nullable
    private static Integer parseColorString(@Nullable String raw) {
        String text = safeTrim(raw);
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
                return Integer.valueOf((int) (0xFF000000L | Long.parseLong(text, 16)));
            }
            if (text.length() == 8) {
                return Integer.valueOf((int) Long.parseLong(text, 16));
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    @Nullable
    private static Integer validateMinInt(
        @Nullable Integer value,
        int min,
        MachineFileParseResult result,
        String scope,
        String fieldName
    ) {
        if (value == null) {
            return null;
        }
        if (value.intValue() < min) {
            result.warn(field(scope, fieldName) + " must be >= " + min + ".");
            return null;
        }
        return value;
    }

    @Nullable
    private static Integer validateRangeInt(
        @Nullable Integer value,
        int min,
        int max,
        MachineFileParseResult result,
        String scope,
        String fieldName
    ) {
        if (value == null) {
            return null;
        }
        if (value.intValue() < min) {
            result.warn(field(scope, fieldName) + " must be >= " + min + ".");
            return null;
        }
        if (value.intValue() > max) {
            result.warn(field(scope, fieldName) + " must be <= " + max + ".");
            return null;
        }
        return value;
    }

    @Nullable
    private static MatchedElement findElement(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (!obj.has(key)) {
                continue;
            }
            JsonElement element = obj.get(key);
            if (element != null) {
                return new MatchedElement(key, element);
            }
        }
        return null;
    }

    private static String field(String scope, String key) {
        return scope + "." + key;
    }

    private static String safeTrim(@Nullable String text) {
        return text == null ? "" : text.trim();
    }

    private static int cappedArraySize(
        JsonArray array,
        MachineFileParseResult result,
        String scope,
        String key,
        int maxEntries
    ) {
        int limit = Math.min(array.size(), maxEntries);
        if (array.size() > maxEntries) {
            result.warnForMachine(scope, field(scope, key) + " has " + array.size() + " entries; only first " + maxEntries + " are used.");
        }
        return limit;
    }

    @Nullable
    private static String normalizeTextAlign(@Nullable String raw) {
        String text = safeTrim(raw).toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return null;
        }
        if ("left".equals(text) || "start".equals(text)) {
            return "left";
        }
        if ("center".equals(text) || "centre".equals(text) || "middle".equals(text)) {
            return "center";
        }
        if ("right".equals(text) || "end".equals(text)) {
            return "right";
        }
        return null;
    }

    @Nullable
    private static String normalizeProgressBarDirection(
        @Nullable String raw,
        MachineFileParseResult result,
        String scope
    ) {
        String text = safeTrim(raw).toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return null;
        }
        if ("left_to_right".equals(text) || "ltr".equals(text)) {
            return "left_to_right";
        }
        if ("right_to_left".equals(text) || "rtl".equals(text)) {
            return "right_to_left";
        }
        if ("top_to_bottom".equals(text) || "ttb".equals(text)) {
            return "top_to_bottom";
        }
        if ("bottom_to_top".equals(text) || "btt".equals(text)) {
            return "bottom_to_top";
        }
        result.warnForMachine(scope, field(scope, "direction") + " must be left_to_right, right_to_left, top_to_bottom or bottom_to_top.");
        return null;
    }

    @Nullable
    private static String normalizeProgressBarSource(
        @Nullable String raw,
        MachineFileParseResult result,
        String scope
    ) {
        String text = ProgressBarStyleSupport.normalizeProgressBarSource(raw);
        if (text != null) {
            return text;
        }
        result.warnForMachine(scope, field(scope, "source") + " must be machine_progress, active_recipe, factory_first, factory_thread, factory_core, factory_average or factory_max.");
        return null;
    }

    @Nullable
    private static String normalizeSliderDirection(
        @Nullable String raw,
        MachineFileParseResult result,
        String scope
    ) {
        String text = safeTrim(raw).toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return null;
        }
        if ("horizontal".equals(text) || "x".equals(text) || "left_to_right".equals(text) || "ltr".equals(text)) {
            return "horizontal";
        }
        if ("vertical".equals(text) || "y".equals(text) || "bottom_to_top".equals(text) || "btt".equals(text)) {
            return "vertical";
        }
        result.warnForMachine(scope, field(scope, "direction") + " must be horizontal or vertical.");
        return null;
    }

    @Nullable
    private static String normalizeDynamicSourceType(
        @Nullable String raw,
        MachineFileParseResult result,
        String scope
    ) {
        String text = safeTrim(raw).toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return null;
        }
        if ("customdata".equals(text) || "custom_data".equals(text) || "custom".equals(text)
            || "smartinterface".equals(text) || "smart_interface".equals(text) || "smart".equals(text)
            || "data_port".equals(text) || "dataport".equals(text)) {
            return "customData";
        }
        if ("machine".equals(text) || "metric".equals(text) || "builtin".equals(text) || "built_in".equals(text)) {
            return "machine";
        }
        result.warnForMachine(scope, field(scope, "source.type") + " must be customData or machine.");
        return null;
    }

    @Nullable
    private static String normalizeDynamicMetric(
        @Nullable String raw,
        MachineFileParseResult result,
        String scope
    ) {
        String text = safeTrim(raw);
        if (text.isEmpty()) {
            return null;
        }
        String normalized = text.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        if ("recipeprogress".equals(normalized)) return "recipeProgress";
        if ("recipemaxprogress".equals(normalized) || "recipetotaltick".equals(normalized) || "recipetotalticks".equals(normalized)) return "recipeMaxProgress";
        if ("energystored".equals(normalized) || "energy".equals(normalized)) return "energyStored";
        if ("energycapacity".equals(normalized) || "maxenergy".equals(normalized) || "maxenergystored".equals(normalized)) return "energyCapacity";
        if ("energyratio".equals(normalized) || "energyprogress".equals(normalized)) return "energyRatio";
        if ("parallelism".equals(normalized) || "currentparallelism".equals(normalized)) return "parallelism";
        if ("threadcount".equals(normalized)) return "threadCount";
        if ("activethreadcount".equals(normalized)) return "activeThreadCount";
        if ("idlethreadcount".equals(normalized)) return "idleThreadCount";
        if ("factorythreadcount".equals(normalized)) return "factoryThreadCount";
        if ("factoryactivethreadcount".equals(normalized)) return "factoryActiveThreadCount";
        if ("factoryidlethreadcount".equals(normalized)) return "factoryIdleThreadCount";
        result.warnForMachine(scope, field(scope, "source.metric") + " has unknown dynamic visual metric: " + text + ".");
        return text;
    }

    @Nullable
    private static String normalizeDynamicRendererType(
        @Nullable String raw,
        MachineFileParseResult result,
        String scope
    ) {
        String text = safeTrim(raw).toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return null;
        }
        if ("textureswitch".equals(text) || "texture_switch".equals(text) || "switch".equals(text) || "state_texture".equals(text)) {
            return "textureSwitch";
        }
        if ("fill".equals(text) || "bar".equals(text) || "progress".equals(text) || "progressbar".equals(text) || "progress_bar".equals(text)) {
            return "fill";
        }
        if ("pie".equals(text) || "ring".equals(text) || "donut".equals(text) || "doughnut".equals(text)) {
            return "pie";
        }
        if ("linechart".equals(text) || "line_chart".equals(text) || "chart".equals(text) || "line".equals(text) || "area".equals(text)) {
            return "lineChart";
        }
        result.warnForMachine(scope, field(scope, "renderer.type") + " must be textureSwitch, fill, pie or lineChart.");
        return null;
    }

    @Nullable
    private static String normalizeDynamicFillDirection(
        @Nullable String raw,
        MachineFileParseResult result,
        String scope
    ) {
        String text = safeTrim(raw).toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return null;
        }
        if ("right".equals(text) || "left_to_right".equals(text) || "ltr".equals(text)) return "right";
        if ("left".equals(text) || "right_to_left".equals(text) || "rtl".equals(text)) return "left";
        if ("down".equals(text) || "top_to_bottom".equals(text) || "ttb".equals(text)) return "down";
        if ("up".equals(text) || "bottom_to_top".equals(text) || "btt".equals(text)) return "up";
        result.warnForMachine(scope, field(scope, "renderer.direction") + " must be right, left, up or down.");
        return null;
    }

    @Nullable
    private static String normalizeDynamicPieMode(
        @Nullable String raw,
        MachineFileParseResult result,
        String scope
    ) {
        String text = safeTrim(raw).toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return null;
        }
        if ("pie".equals(text) || "solid".equals(text)) {
            return "pie";
        }
        if ("ring".equals(text) || "donut".equals(text) || "doughnut".equals(text)) {
            return "ring";
        }
        result.warnForMachine(scope, field(scope, "renderer.mode") + " must be pie or ring.");
        return null;
    }

    @Nullable
    private static Float normalizePositiveScale(@Nullable Float value) {
        if (value == null) {
            return null;
        }
        float scale = value.floatValue();
        if (!Float.isFinite(scale)) {
            return null;
        }
        return Float.valueOf(Math.max(0.01F, scale));
    }

    @Nullable
    private static String normalizeDynamicTransformOrigin(
        @Nullable String raw,
        MachineFileParseResult result,
        String scope
    ) {
        String text = safeTrim(raw).toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return null;
        }
        if ("center".equals(text) || "centre".equals(text) || "middle".equals(text)) {
            return "center";
        }
        if ("topleft".equals(text) || "top_left".equals(text) || "top-left".equals(text)
            || "origin".equals(text) || "default".equals(text)) {
            return "topLeft";
        }
        result.warnForMachine(scope, field(scope, "origin") + " must be center or topLeft.");
        return null;
    }

    private static String summarizeException(Exception ex) {
        String message = safeTrim(ex.getMessage());
        return message.isEmpty() ? ex.getClass().getSimpleName() : message;
    }

    static final class MachineFileParseResult {
        final String sourceName;
        final List<String> warnings = new ArrayList<String>();
        @Nullable
        String namespacedKey;
        @Nullable
        String pathKey;
        boolean allowPathFallback;
        boolean machineNodePresent;
        boolean factoryNodePresent;
        @Nullable
        String machineRegistryName;
        @Nullable
        String factoryRegistryName;
        @Nullable
        MachineGuiStyleManager.ControllerStyle machineStyle;
        @Nullable
        MachineGuiStyleManager.ControllerStyle factoryStyle;

        MachineFileParseResult(String sourceName) {
            this.sourceName = sourceName;
        }

        void warn(String message) {
            if (warnings.size() >= MAX_WARNINGS) {
                return;
            }
            warnings.add(sourceName + ": " + message);
        }

        void warnForMachine(String controllerKind, String message) {
            if (warnings.size() >= MAX_WARNINGS) {
                return;
            }
            String registry = "machine".equals(controllerKind) ? machineRegistryName : factoryRegistryName;
            if (registry != null && !registry.trim().isEmpty()) {
                warnings.add(sourceName + " [" + registry + "/" + controllerKind + "]: " + message);
            } else {
                warnings.add(sourceName + " [" + controllerKind + "]: " + message);
            }
        }
    }

    private static final class MatchedElement {
        final String key;
        final JsonElement element;

        private MatchedElement(String key, JsonElement element) {
            this.key = key;
            this.element = element;
        }
    }
}
