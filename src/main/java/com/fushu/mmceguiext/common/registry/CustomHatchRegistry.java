package com.fushu.mmceguiext.common.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.fushu.mmceguiext.MMCEGuiExt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CustomHatchRegistry {
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);
    private static final Path HATCH_DIR = resolveHatchDir();
    private static final List<CustomHatchDef> CACHE = new ArrayList<CustomHatchDef>();
    private static final Map<String, CustomHatchDef> REGISTERED = new LinkedHashMap<String, CustomHatchDef>();

    private CustomHatchRegistry() {
    }

    public static List<CustomHatchDef> loadAll() {
        CACHE.clear();
        REGISTERED.clear();
        if (!Files.isDirectory(HATCH_DIR)) {
            return Collections.emptyList();
        }
        try {
            Files.list(HATCH_DIR).filter(p -> p.toString().endsWith(".json")).forEach(path -> {
                CustomHatchDef def = load(path);
                if (def != null) {
                    CACHE.add(def);
                    REGISTERED.put(normalizeId(def.id), def);
                }
            });
        } catch (IOException ex) {
            LOGGER.warn("Failed to scan custom hatch dir {}: {}", HATCH_DIR, ex.getMessage());
        }
        return new ArrayList<CustomHatchDef>(CACHE);
    }

    public static List<CustomHatchDef> registerAll() {
        return getCached();
    }

    public static List<CustomHatchDef> getCached() {
        return new ArrayList<CustomHatchDef>(CACHE);
    }

    public static List<CustomHatchDef> getRegistered() {
        return new ArrayList<CustomHatchDef>(REGISTERED.values());
    }

    @Nullable
    public static CustomHatchDef findById(@Nullable String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeId(id);
        CustomHatchDef direct = REGISTERED.get(normalized);
        if (direct != null) {
            return direct;
        }
        String pathId = normalized.contains(":") ? normalized.substring(normalized.indexOf(':') + 1) : normalized;
        direct = REGISTERED.get(pathId);
        if (direct != null) {
            return direct;
        }
        for (CustomHatchDef def : CACHE) {
            if (def == null || def.id == null) {
                continue;
            }
            if (normalizeId(def.id).equals(normalized) || normalizeId(def.id).endsWith(":" + pathId) || normalizeId(def.id).equals(pathId)) {
                return def;
            }
        }
        return null;
    }

    public static BlockSpec buildBlockSpec(CustomHatchDef def) {
        return new BlockSpec(def == null ? null : normalizeId(def.id), def == null ? null : def.displayName);
    }

    @Nullable
    public static CustomHatchDef load(Path path) {
        try {
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JsonObject root = new JsonParser().parse(text).getAsJsonObject();
            CustomHatchDef def = new CustomHatchDef();
            def.id = getString(root, "id");
            def.displayName = getString(root, "displayName");
            def.blockTexture = getString(root, "blockTexture");
            def.blockModel = getString(root, "blockModel");
            def.block = parseBlock(root);
            def.guiStyleFile = getString(root, "guiStyleFile");
            def.componentType = lower(getString(root, "componentType"));
            def.ioType = lower(getString(root, "ioType"));
            def.machineComponents = parseMachineComponents(root.getAsJsonArray("components"));
            def.capacity = getInt(root, "capacity", 1000);
            def.fluidCapacity = getInt(root, "fluidCapacity", def.capacity);
            def.gasCapacity = getInt(root, "gasCapacity", def.capacity);
            def.inputSlot = parseSlot(root.getAsJsonObject("inputSlot"));
            def.outputSlot = parseSlot(root.getAsJsonObject("outputSlot"));
            def.tank = parseTank(root.getAsJsonObject("tank"));
            def.texts = parseTexts(root.getAsJsonArray("texts"));
            def.gui = parseGui(root.getAsJsonObject("gui"));
            if (def.gui != null && !def.gui.components.isEmpty()) {
                applyGuiComponents(def);
            }
            return def.id == null || def.id.trim().isEmpty() ? null : def;
        } catch (Exception ex) {
            LOGGER.warn("Failed to parse custom hatch {}: {}", path, ex.getMessage());
            return null;
        }
    }

    private static SlotDef parseSlot(@Nullable JsonObject obj) {
        SlotDef slot = new SlotDef();
        if (obj == null) {
            return slot;
        }
        slot.x = getInt(obj, "x", 0);
        slot.y = getInt(obj, "y", 0);
        return slot;
    }

    private static BlockDef parseBlock(JsonObject root) {
        BlockDef block = new BlockDef();
        JsonObject obj = root.getAsJsonObject("block");
        block.model = getString(obj, root, "model", getString(root, "blockModel"));
        block.material = getString(obj, root, "material", block.material);
        block.hardness = getFloat(obj, root, "hardness", block.hardness);
        block.resistance = getFloat(obj, root, "resistance", block.resistance);
        block.harvestTool = getString(obj, root, "harvestTool", block.harvestTool);
        block.harvestLevel = getInt(obj, root, "harvestLevel", block.harvestLevel);
        block.soundType = getString(obj, root, "soundType", block.soundType);
        block.lightLevel = getFloat(obj, root, "lightLevel", block.lightLevel);
        block.lightOpacity = getInt(obj, root, "lightOpacity", block.lightOpacity);
        block.slipperiness = getFloat(obj, root, "slipperiness", block.slipperiness);
        block.unbreakable = getBoolean(obj, root, "unbreakable", block.unbreakable);
        return block;
    }

    private static TankDef parseTank(@Nullable JsonObject obj) {
        TankDef tank = new TankDef();
        if (obj == null) {
            return tank;
        }
        tank.x = getInt(obj, "x", 0);
        tank.y = getInt(obj, "y", 0);
        tank.width = getInt(obj, "width", 16);
        tank.height = getInt(obj, "height", 48);
        tank.content = lower(getString(obj, "content"));
        tank.renderMode = lower(getFirstString(obj, "renderMode", "render_mode", "render", "mode"));
        tank.alpha = normalizeAlpha(getFirstFloat(obj, "alpha", "opacity", "transparency"));
        return tank;
    }

    private static List<TextDef> parseTexts(@Nullable JsonArray array) {
        if (array == null || array.size() == 0) {
            return Collections.emptyList();
        }
        List<TextDef> out = new ArrayList<TextDef>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            TextDef text = new TextDef();
            text.x = getInt(obj, "x", 0);
            text.y = getInt(obj, "y", 0);
            text.value = getString(obj, "value");
            text.align = normalizeTextAlign(getString(obj, "align"), getString(obj, "alignment"), getString(obj, "textAlign"), getString(obj, "text_align"));
            text.priority = getFirstInt(obj, 0, "priority", "zIndex", "z_index", "z", "layer");
            out.add(text);
        }
        out.sort(java.util.Comparator.comparingInt(a -> a.priority));
        return out;
    }

    private static GuiDef parseGui(@Nullable JsonObject obj) {
        GuiDef gui = new GuiDef();
        if (obj == null) {
            return gui;
        }
        gui.width = getInt(obj, "width", 176);
        gui.height = getInt(obj, "height", 166);
        gui.coordinateWidth = getInt(obj, "coordinateWidth", -1);
        gui.coordinateHeight = getInt(obj, "coordinateHeight", -1);
        gui.components = parseComponents(obj.getAsJsonArray("components"));
        return gui;
    }

    private static List<ComponentDef> parseComponents(@Nullable JsonArray array) {
        if (array == null || array.size() == 0) {
            return Collections.emptyList();
        }
        List<ComponentDef> out = new ArrayList<ComponentDef>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            ComponentDef def = new ComponentDef();
            def.type = lower(getString(obj, "type"));
            def.role = lower(getString(obj, "role"));
            def.x = getInt(obj, "x", 0);
            def.y = getInt(obj, "y", 0);
            def.index = getInt(obj, "index", -1);
            def.width = getInt(obj, "width", 0);
            def.height = getInt(obj, "height", 0);
            def.hotbarY = getInt(obj, "hotbarY", -1);
            def.rows = getInt(obj, "rows", 0);
            def.columns = getInt(obj, "columns", 0);
            def.visibleRows = getInt(obj, "visibleRows", getInt(obj, "visible_rows", 0));
            def.visibleColumns = getInt(obj, "visibleColumns", getInt(obj, "visible_columns", 0));
            def.spacingX = getInt(obj, "spacingX", getInt(obj, "spacing_x", 2));
            def.spacingY = getInt(obj, "spacingY", getInt(obj, "spacing_y", 2));
            def.slotSize = getInt(obj, "slotSize", getInt(obj, "slot_size", getInt(obj, "size", 16)));
            def.scrollMode = lower(getString(obj, "scrollMode"));
            def.scrollbar = getBoolean(obj, "scrollbar");
            def.scrollbarX = getInt(obj, "scrollbarX", getInt(obj, "scrollbar_x", 0));
            def.scrollbarY = getInt(obj, "scrollbarY", getInt(obj, "scrollbar_y", 0));
            def.scrollbarHeight = getInt(obj, "scrollbarHeight", getInt(obj, "scrollbar_height", 0));
            def.scrollbarWidth = getInt(obj, "scrollbarWidth", getInt(obj, "scrollbar_width", 12));
            def.scrollbarThumbHeight = getInt(obj, "scrollbarThumbHeight", getInt(obj, "scrollbar_thumb_height", 15));
            def.scrollbarTexture = getString(obj, "scrollbarTexture");
            def.scrollbarHoverTexture = getString(obj, "scrollbarHoverTexture");
            def.scrollbarPressedTexture = getString(obj, "scrollbarPressedTexture");
            def.scrollbarDisabledTexture = getString(obj, "scrollbarDisabledTexture");
            def.scrollbarTextureWidth = getInt(obj, "scrollbarTextureWidth", getInt(obj, "scrollbar_texture_width", 256));
            def.scrollbarTextureHeight = getInt(obj, "scrollbarTextureHeight", getInt(obj, "scrollbar_texture_height", 256));
            def.scrollbarU = getInt(obj, "scrollbarU", getInt(obj, "scrollbar_u", 232));
            def.scrollbarV = getInt(obj, "scrollbarV", getInt(obj, "scrollbar_v", 0));
            def.scrollbarHoverU = getInt(obj, "scrollbarHoverU", getInt(obj, "scrollbar_hover_u", def.scrollbarU));
            def.scrollbarHoverV = getInt(obj, "scrollbarHoverV", getInt(obj, "scrollbar_hover_v", def.scrollbarV));
            def.scrollbarPressedU = getInt(obj, "scrollbarPressedU", getInt(obj, "scrollbar_pressed_u", def.scrollbarU));
            def.scrollbarPressedV = getInt(obj, "scrollbarPressedV", getInt(obj, "scrollbar_pressed_v", def.scrollbarV));
            def.scrollbarDisabledU = getInt(obj, "scrollbarDisabledU", getInt(obj, "scrollbar_disabled_u", 244));
            def.scrollbarDisabledV = getInt(obj, "scrollbarDisabledV", getInt(obj, "scrollbar_disabled_v", 0));
            def.itemOverlay = getBoolean(obj, "itemOverlay");
            def.itemOverlayTexture = getString(obj, "itemOverlayTexture");
            def.itemOverlayTextureWidth = getInt(obj, "itemOverlayTextureWidth", getInt(obj, "item_overlay_texture_width", 16));
            def.itemOverlayTextureHeight = getInt(obj, "itemOverlayTextureHeight", getInt(obj, "item_overlay_texture_height", 16));
            def.itemOverlayU = getInt(obj, "itemOverlayU", getInt(obj, "item_overlay_u", 0));
            def.itemOverlayV = getInt(obj, "itemOverlayV", getInt(obj, "item_overlay_v", 0));
            def.value = getString(obj, "value");
            def.style = lower(getString(obj, "style"));
            def.content = lower(getString(obj, "content"));
            def.renderMode = lower(getFirstString(obj, "renderMode", "render_mode", "render", "mode"));
            def.alpha = normalizeAlpha(getFirstFloat(obj, "alpha", "opacity", "transparency"));
            def.color = getString(obj, "color");
            def.scale = getFloat(obj, "scale");
            def.align = normalizeTextAlign(getString(obj, "align"), getString(obj, "alignment"), getString(obj, "textAlign"), getString(obj, "text_align"));
            def.overlay = getBoolean(obj, "overlay");
            def.priority = getInt(obj, "priority", getInt(obj, "zIndex", getInt(obj, "z_index", getInt(obj, "z", getInt(obj, "layer", 0)))));
            if (def.type != null && !def.type.trim().isEmpty()) {
                if ("slot_grid".equals(def.type) || "slots".equals(def.type)) {
                    expandSlotGrid(obj, def, out);
                } else {
                    out.add(def);
                }
            }
        }
        return out;
    }

    private static void expandSlotGrid(JsonObject obj, ComponentDef grid, List<ComponentDef> out) {
        int rows = Math.max(1, getFirstInt(obj, 1, "rows", "rowCount", "yCount"));
        int columns = Math.max(1, getFirstInt(obj, 1, "columns", "cols", "columnCount", "xCount"));
        int spacingX = getFirstInt(obj, 2, "spacingX", "xSpacing", "gapX");
        int spacingY = getFirstInt(obj, 2, "spacingY", "ySpacing", "gapY");
        int slotSize = Math.max(1, getFirstInt(obj, 16, "slotSize", "size"));
        int visibleRows = getFirstInt(obj, 0, "visibleRows", "visible_rows");
        int visibleColumns = getFirstInt(obj, 0, "visibleColumns", "visible_columns");
        int scrollbarX = getFirstInt(obj, 0, "scrollbarX", "scrollbar_x");
        int scrollbarY = getFirstInt(obj, 0, "scrollbarY", "scrollbar_y");
        int scrollbarHeight = getFirstInt(obj, 0, "scrollbarHeight", "scrollbar_height");
        int scrollbarWidth = getFirstInt(obj, 12, "scrollbarWidth", "scrollbar_width");
        int scrollbarThumbHeight = getFirstInt(obj, 15, "scrollbarThumbHeight", "scrollbar_thumb_height");
        String scrollbarTexture = getString(obj, "scrollbarTexture");
        String scrollbarHoverTexture = getString(obj, "scrollbarHoverTexture");
        String scrollbarPressedTexture = getString(obj, "scrollbarPressedTexture");
        String scrollbarDisabledTexture = getString(obj, "scrollbarDisabledTexture");
        int scrollbarTextureWidth = getFirstInt(obj, 256, "scrollbarTextureWidth", "scrollbar_texture_width");
        int scrollbarTextureHeight = getFirstInt(obj, 256, "scrollbarTextureHeight", "scrollbar_texture_height");
        int scrollbarU = getFirstInt(obj, 232, "scrollbarU", "scrollbar_u");
        int scrollbarV = getFirstInt(obj, 0, "scrollbarV", "scrollbar_v");
        int scrollbarHoverU = getFirstInt(obj, scrollbarU, "scrollbarHoverU", "scrollbar_hover_u");
        int scrollbarHoverV = getFirstInt(obj, scrollbarV, "scrollbarHoverV", "scrollbar_hover_v");
        int scrollbarPressedU = getFirstInt(obj, scrollbarU, "scrollbarPressedU", "scrollbar_pressed_u");
        int scrollbarPressedV = getFirstInt(obj, scrollbarV, "scrollbarPressedV", "scrollbar_pressed_v");
        int scrollbarDisabledU = getFirstInt(obj, 244, "scrollbarDisabledU", "scrollbar_disabled_u");
        int scrollbarDisabledV = getFirstInt(obj, 0, "scrollbarDisabledV", "scrollbar_disabled_v");
        Boolean itemOverlay = getBoolean(obj, "itemOverlay");
        String itemOverlayTexture = getString(obj, "itemOverlayTexture");
        int itemOverlayTextureWidth = getFirstInt(obj, 16, "itemOverlayTextureWidth", "item_overlay_texture_width");
        int itemOverlayTextureHeight = getFirstInt(obj, 16, "itemOverlayTextureHeight", "item_overlay_texture_height");
        int itemOverlayU = getFirstInt(obj, 0, "itemOverlayU", "item_overlay_u");
        int itemOverlayV = getFirstInt(obj, 0, "itemOverlayV", "item_overlay_v");
        String scrollMode = lower(getString(obj, "scrollMode"));
        Boolean scrollbar = getBoolean(obj, "scrollbar");
        int baseIndex = grid.index;
        int ordinal = 0;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                ComponentDef slot = new ComponentDef();
                slot.type = "slot";
                slot.role = grid.role;
                slot.index = baseIndex >= 0 ? baseIndex + ordinal : -1;
                slot.x = grid.x + column * (slotSize + spacingX);
                slot.y = grid.y + row * (slotSize + spacingY);
                slot.gridBaseIndex = baseIndex >= 0 ? baseIndex : 0;
                slot.gridBaseX = grid.x;
                slot.gridBaseY = grid.y;
                slot.rows = rows;
                slot.columns = columns;
                slot.visibleRows = visibleRows;
                slot.visibleColumns = visibleColumns;
                slot.spacingX = spacingX;
                slot.spacingY = spacingY;
                slot.slotSize = slotSize;
                slot.scrollMode = scrollMode;
                slot.scrollbar = scrollbar;
                slot.scrollbarX = scrollbarX;
                slot.scrollbarY = scrollbarY;
                slot.scrollbarHeight = scrollbarHeight;
                slot.scrollbarWidth = scrollbarWidth;
                slot.scrollbarThumbHeight = scrollbarThumbHeight;
                slot.scrollbarTexture = scrollbarTexture;
                slot.scrollbarHoverTexture = scrollbarHoverTexture;
                slot.scrollbarPressedTexture = scrollbarPressedTexture;
                slot.scrollbarDisabledTexture = scrollbarDisabledTexture;
                slot.scrollbarTextureWidth = scrollbarTextureWidth;
                slot.scrollbarTextureHeight = scrollbarTextureHeight;
                slot.scrollbarU = scrollbarU;
                slot.scrollbarV = scrollbarV;
                slot.scrollbarHoverU = scrollbarHoverU;
                slot.scrollbarHoverV = scrollbarHoverV;
                slot.scrollbarPressedU = scrollbarPressedU;
                slot.scrollbarPressedV = scrollbarPressedV;
                slot.scrollbarDisabledU = scrollbarDisabledU;
                slot.scrollbarDisabledV = scrollbarDisabledV;
                slot.itemOverlay = itemOverlay;
                slot.itemOverlayTexture = itemOverlayTexture;
                slot.itemOverlayTextureWidth = itemOverlayTextureWidth;
                slot.itemOverlayTextureHeight = itemOverlayTextureHeight;
                slot.itemOverlayU = itemOverlayU;
                slot.itemOverlayV = itemOverlayV;
                out.add(slot);
                ordinal++;
            }
        }
    }

    private static List<MachineComponentDef> parseMachineComponents(@Nullable JsonArray array) {
        if (array == null || array.size() == 0) {
            return Collections.emptyList();
        }
        List<MachineComponentDef> out = new ArrayList<MachineComponentDef>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            MachineComponentDef def = new MachineComponentDef();
            def.type = lower(getString(obj, "type"));
            def.io = lower(getString(obj, "io"));
            if (def.type != null && !def.type.trim().isEmpty()) {
                out.add(def);
            }
        }
        return out;
    }

    private static void applyGuiComponents(CustomHatchDef def) {
        List<TextDef> texts = new ArrayList<TextDef>();
        for (ComponentDef component : def.gui.components) {
            if (component == null || component.type == null) {
                continue;
            }
            if ("slot".equals(component.type)) {
                if ("input".equals(component.role)) {
                    def.inputSlot.x = component.x;
                    def.inputSlot.y = component.y;
                } else if ("output".equals(component.role)) {
                    def.outputSlot.x = component.x;
                    def.outputSlot.y = component.y;
                }
            } else if ("tank".equals(component.type)) {
                def.tank.x = component.x;
                def.tank.y = component.y;
                def.tank.width = component.width > 0 ? component.width : def.tank.width;
                def.tank.height = component.height > 0 ? component.height : def.tank.height;
                def.tank.content = component.content;
                if (component.renderMode != null && !component.renderMode.trim().isEmpty()) {
                    def.tank.renderMode = component.renderMode;
                }
                if (component.alpha != null) {
                    def.tank.alpha = component.alpha;
                }
            } else if ("text".equals(component.type) && component.value != null && !component.value.trim().isEmpty()) {
                TextDef text = new TextDef();
                text.x = component.x;
                text.y = component.y;
                text.value = component.value;
                text.align = component.align;
                text.priority = component.priority;
                texts.add(text);
            }
        }
        if (!texts.isEmpty()) {
            texts.sort(java.util.Comparator.comparingInt(a -> a.priority));
            def.texts = texts;
        }
    }

    @Nullable
    private static String getString(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    private static String getString(@Nullable JsonObject primary, JsonObject fallbackObj, String key, String fallback) {
        JsonElement e = primary == null ? null : primary.get(key);
        if (e == null || e.isJsonNull()) {
            e = fallbackObj.get(key);
        }
        return e == null || e.isJsonNull() ? fallback : e.getAsString();
    }

    @Nullable
    private static String normalizeTextAlign(@Nullable String... values) {
        if (values == null) {
            return null;
        }
        for (@Nullable String value : values) {
            if (value == null) {
                continue;
            }
            String text = value.trim().toLowerCase(Locale.ROOT);
            if (text.isEmpty()) {
                continue;
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
        }
        return null;
    }

    private static int getInt(JsonObject obj, String key, int fallback) {
        JsonElement e = obj.get(key);
        return e == null || e.isJsonNull() ? fallback : e.getAsInt();
    }

    private static int getInt(@Nullable JsonObject primary, JsonObject fallbackObj, String key, int fallback) {
        JsonElement e = primary == null ? null : primary.get(key);
        if (e == null || e.isJsonNull()) {
            e = fallbackObj.get(key);
        }
        return e == null || e.isJsonNull() ? fallback : e.getAsInt();
    }

    private static int getFirstInt(JsonObject obj, int fallback, String... keys) {
        for (String key : keys) {
            JsonElement e = obj.get(key);
            if (e != null && !e.isJsonNull()) {
                return e.getAsInt();
            }
        }
        return fallback;
    }

    @Nullable
    private static String getFirstString(JsonObject obj, String... keys) {
        for (String key : keys) {
            JsonElement e = obj.get(key);
            if (e != null && !e.isJsonNull()) {
                return e.getAsString();
            }
        }
        return null;
    }

    @Nullable
    private static Boolean getBoolean(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        return e == null || e.isJsonNull() ? null : Boolean.valueOf(e.getAsBoolean());
    }

    private static boolean getBoolean(@Nullable JsonObject primary, JsonObject fallbackObj, String key, boolean fallback) {
        JsonElement e = primary == null ? null : primary.get(key);
        if (e == null || e.isJsonNull()) {
            e = fallbackObj.get(key);
        }
        return e == null || e.isJsonNull() ? fallback : e.getAsBoolean();
    }

    @Nullable
    private static Float getFloat(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        return e == null || e.isJsonNull() ? null : Float.valueOf(e.getAsFloat());
    }

    @Nullable
    private static Float getFirstFloat(JsonObject obj, String... keys) {
        for (String key : keys) {
            JsonElement e = obj.get(key);
            if (e != null && !e.isJsonNull()) {
                return Float.valueOf(e.getAsFloat());
            }
        }
        return null;
    }

    private static float getFloat(@Nullable JsonObject primary, JsonObject fallbackObj, String key, float fallback) {
        JsonElement e = primary == null ? null : primary.get(key);
        if (e == null || e.isJsonNull()) {
            e = fallbackObj.get(key);
        }
        return e == null || e.isJsonNull() ? fallback : e.getAsFloat();
    }

    private static Path resolveHatchDir() {
        Path dir = Paths.get("config").resolve("mmceguiext").resolve("custom_hatches");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
        return dir;
    }

    public static class CustomHatchDef {
        public String id;
        public String displayName;
        public String blockTexture;
        public String blockModel;
        public BlockDef block = new BlockDef();
        public String guiStyleFile;
        public String componentType = "fluid";
        public String ioType = "input";
        public List<MachineComponentDef> machineComponents = Collections.emptyList();
        public int capacity;
        public int fluidCapacity;
        public int gasCapacity;
        public SlotDef inputSlot = new SlotDef();
        public SlotDef outputSlot = new SlotDef();
        public TankDef tank = new TankDef();
        public List<TextDef> texts = Collections.emptyList();
        public GuiDef gui = new GuiDef();
    }

    public static class BlockDef {
        @Nullable
        public String model;
        public String material = "iron";
        public float hardness = 2.0F;
        public float resistance = 10.0F;
        public String harvestTool = "pickaxe";
        public int harvestLevel = 1;
        public String soundType = "metal";
        public float lightLevel = 0.0F;
        public int lightOpacity = 255;
        public float slipperiness = 0.6F;
        public boolean unbreakable = false;
    }

    public static class GuiDef {
        public int width = 176;
        public int height = 166;
        public int coordinateWidth = -1;
        public int coordinateHeight = -1;
        public List<ComponentDef> components = Collections.emptyList();
    }

    public static class ComponentDef {
        public String type;
        public String role;
        public int x;
        public int y;
        public int index = -1;
        public int width;
        public int height;
        public int hotbarY = -1;
        public int rows;
        public int columns;
        public int gridBaseIndex;
        public int gridBaseX;
        public int gridBaseY;
        public int visibleRows;
        public int visibleColumns;
        public int spacingX = 2;
        public int spacingY = 2;
        public int slotSize = 16;
        @Nullable
        public String scrollMode;
        @Nullable
        public Boolean scrollbar;
        public int scrollbarX;
        public int scrollbarY;
        public int scrollbarHeight;
        public int scrollbarWidth = 12;
        public int scrollbarThumbHeight = 15;
        @Nullable
        public String scrollbarTexture;
        @Nullable
        public String scrollbarHoverTexture;
        @Nullable
        public String scrollbarPressedTexture;
        @Nullable
        public String scrollbarDisabledTexture;
        public int scrollbarTextureWidth = 256;
        public int scrollbarTextureHeight = 256;
        public int scrollbarU = 232;
        public int scrollbarV = 0;
        public int scrollbarHoverU = 232;
        public int scrollbarHoverV = 0;
        public int scrollbarPressedU = 232;
        public int scrollbarPressedV = 0;
        public int scrollbarDisabledU = 244;
        public int scrollbarDisabledV = 0;
        @Nullable
        public Boolean itemOverlay;
        @Nullable
        public String itemOverlayTexture;
        public int itemOverlayTextureWidth = 16;
        public int itemOverlayTextureHeight = 16;
        public int itemOverlayU = 0;
        public int itemOverlayV = 0;
        public int priority = 0;
        public String value;
        public String style;
        public String content;
        @Nullable
        public String renderMode;
        @Nullable
        public Float alpha;
        public String color;
        public Float scale;
        @Nullable
        public String align;
        public Boolean overlay;
    }

    public static class MachineComponentDef {
        public String type;
        public String io;
    }

    public static class SlotDef {
        public int x;
        public int y;
    }

    public static class TankDef {
        public int x;
        public int y;
        public int width;
        public int height;
        public String content = "fluid";
        @Nullable
        public String renderMode;
        @Nullable
        public Float alpha;
    }

    public static class TextDef {
        public int x;
        public int y;
        public String value;
        @Nullable
        public String align;
        public int priority = 0;
    }

    @Nullable
    private static String lower(@Nullable String value) {
        return value == null ? null : value.trim().toLowerCase();
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

    private static String normalizeId(@Nullable String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static final class BlockSpec {
        public final String registryPath;
        public final String displayName;

        public BlockSpec(@Nullable String registryPath, @Nullable String displayName) {
            this.registryPath = registryPath == null ? "" : registryPath;
            this.displayName = displayName == null ? "" : displayName;
        }
    }
}
