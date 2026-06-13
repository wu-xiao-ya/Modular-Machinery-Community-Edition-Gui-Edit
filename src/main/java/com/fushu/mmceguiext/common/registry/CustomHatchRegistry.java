package com.fushu.mmceguiext.common.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.fushu.mmceguiext.MMCEGuiExt;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class CustomHatchRegistry {
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);
    private static final Path HATCH_DIR = resolveHatchDir();
    private static final long MAX_CONFIG_BYTES = 1024L * 1024L;
    private static final int MAX_GRID_ROWS = 256;
    private static final int MAX_GRID_COLUMNS = 256;
    private static final int MAX_GRID_SLOTS = 4096;
    private static final int MAX_TEXTS = 512;
    private static final int MAX_GUI_COMPONENTS = 4096;
    private static final int MAX_MACHINE_COMPONENTS = 256;
    private static final int MAX_BLOCK_TEXTURE_LEVELS = 256;
    private static final int MAX_COMPONENT_SIZE = 4096;
    private static final int MAX_SLOT_SPACING = 256;
    private static final long MAX_HATCH_CAPACITY = Long.MAX_VALUE;
    private static final long MAX_ENERGY_CAPACITY = Long.MAX_VALUE;
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
        try (Stream<Path> stream = Files.list(HATCH_DIR)) {
            stream.filter(p -> p.toString().endsWith(".json")).forEach(path -> {
                CustomHatchDef def = load(path);
                if (def != null && !REGISTERED.containsKey(normalizeId(def.id))) {
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
            if (Files.size(path) > MAX_CONFIG_BYTES) {
                LOGGER.warn("Skipping custom hatch {} because it is larger than {} bytes.", path, MAX_CONFIG_BYTES);
                return null;
            }
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
            def.outputSlotLock = getFirstBoolean(root, true, "outputSlotLock", "output_slot_lock", "lockOutputSlots", "lock_output_slots");
            def.machineComponents = parseMachineComponents(getArray(root, "components"));
            def.capacity = clampLong(getLong(root, "capacity", 1000L), 1L, MAX_HATCH_CAPACITY);
            def.fluidCapacity = clampLong(getLong(root, "fluidCapacity", def.capacity), 1L, MAX_HATCH_CAPACITY);
            def.gasCapacity = clampLong(getLong(root, "gasCapacity", def.capacity), 1L, MAX_HATCH_CAPACITY);
            def.energyCapacity = clampLong(getLong(root, "energyCapacity", getLong(root, "energy", def.capacity)), 1L, MAX_ENERGY_CAPACITY);
            def.energyTransfer = clampLong(getLong(root, "energyTransfer", getLong(root, "energyTransferLimit", def.energyCapacity)), 1L, MAX_ENERGY_CAPACITY);
            def.tips = parseStringList(root, "tips", "tooltip", "tooltips");
            def.inputSlot = parseSlot(getObject(root, "inputSlot"));
            def.outputSlot = parseSlot(getObject(root, "outputSlot"));
            def.tank = parseTank(getObject(root, "tank"));
            def.texts = parseTexts(getArray(root, "texts"));
            def.gui = parseGui(getObject(root, "gui"));
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
        JsonObject obj = getObject(root, "block");
        block.model = getString(obj, root, "model", getString(root, "blockModel"));
        block.texture = getString(obj, root, "texture", getString(root, "blockTexture"));
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
        String defaultTextureContent = lower(getFirstString(obj, "textureContent", "texture_content", "content"));
        if (defaultTextureContent == null || defaultTextureContent.trim().isEmpty()) {
            defaultTextureContent = lower(getFirstString(root, "blockTextureContent", "block_texture_content", "textureContent", "texture_content"));
        }
        if (defaultTextureContent == null || defaultTextureContent.trim().isEmpty()) {
            defaultTextureContent = "fluid";
        }
        block.textureLevels = parseBlockTextureLevels(firstArray(
            getArray(obj, "textureLevels"),
            getArray(obj, "texture_levels"),
            getArray(root, "blockTextureLevels"),
            getArray(root, "block_texture_levels"),
            getArray(root, "blockTextures")
        ), defaultTextureContent);
        return block;
    }

    private static List<BlockTextureLevelDef> parseBlockTextureLevels(@Nullable JsonArray array, @Nullable String defaultContent) {
        if (array == null || array.size() == 0) {
            return Collections.emptyList();
        }
        List<BlockTextureLevelDef> out = new ArrayList<BlockTextureLevelDef>();
        int limit = Math.min(array.size(), MAX_BLOCK_TEXTURE_LEVELS);
        if (array.size() > MAX_BLOCK_TEXTURE_LEVELS) {
            LOGGER.warn("Skipping {} extra custom hatch block texture levels; max is {}", array.size() - MAX_BLOCK_TEXTURE_LEVELS, MAX_BLOCK_TEXTURE_LEVELS);
        }
        for (int i = 0; i < limit; i++) {
            JsonElement element = array.get(i);
            BlockTextureLevelDef def = new BlockTextureLevelDef();
            if (element != null && element.isJsonObject()) {
                JsonObject level = element.getAsJsonObject();
                def.content = lower(getFirstString(level, "content", "resource", "source", "type", "storage"));
                def.texture = getFirstString(level, "texture", "blockTexture", "block_texture");
                def.model = getFirstString(level, "model", "blockModel", "block_model");
                def.minFillRatio = normalizeFillRatio(getFirstDouble(level, "minFillRatio", "min_fill_ratio", "minRatio", "min_ratio", "ratio", "threshold", "min", "from", "level", "percent"));
            } else {
                def.texture = asString(element, null);
                def.minFillRatio = limit <= 1 ? 0.0D : ((double) i / (double) (limit - 1));
            }
            if (def.content == null || def.content.trim().isEmpty()) {
                def.content = defaultContent;
            }
            if (def.texture == null && def.model == null) {
                continue;
            }
            out.add(def);
        }
        out.sort((a, b) -> Double.compare(a.minFillRatio, b.minFillRatio));
        return out.isEmpty() ? Collections.<BlockTextureLevelDef>emptyList() : out;
    }

    private static TankDef parseTank(@Nullable JsonObject obj) {
        TankDef tank = new TankDef();
        if (obj == null) {
            return tank;
        }
        tank.x = getInt(obj, "x", 0);
        tank.y = getInt(obj, "y", 0);
        tank.width = clamp(getInt(obj, "width", 16), 1, MAX_COMPONENT_SIZE);
        tank.height = clamp(getInt(obj, "height", 48), 1, MAX_COMPONENT_SIZE);
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
        int limit = Math.min(array.size(), MAX_TEXTS);
        if (array.size() > MAX_TEXTS) {
            LOGGER.warn("Skipping {} extra custom hatch texts; max is {}", array.size() - MAX_TEXTS, MAX_TEXTS);
        }
        for (int i = 0; i < limit; i++) {
            JsonElement element = array.get(i);
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
        gui.width = clamp(getInt(obj, "width", 176), 1, 4096);
        gui.height = clamp(getInt(obj, "height", 166), 1, 4096);
        gui.coordinateWidth = clampCoordinateSize(getInt(obj, "coordinateWidth", -1));
        gui.coordinateHeight = clampCoordinateSize(getInt(obj, "coordinateHeight", -1));
        gui.components = parseComponents(getArray(obj, "components"));
        return gui;
    }

    private static List<ComponentDef> parseComponents(@Nullable JsonArray array) {
        if (array == null || array.size() == 0) {
            return Collections.emptyList();
        }
        List<ComponentDef> out = new ArrayList<ComponentDef>();
        int limit = Math.min(array.size(), MAX_GUI_COMPONENTS);
        if (array.size() > MAX_GUI_COMPONENTS) {
            LOGGER.warn("Skipping {} extra custom hatch GUI components; max is {}", array.size() - MAX_GUI_COMPONENTS, MAX_GUI_COMPONENTS);
        }
        int nextAutoSlotIndex = 0;
        for (int i = 0; i < limit; i++) {
            JsonElement element = array.get(i);
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
            def.width = clampOptionalSize(getInt(obj, "width", 0));
            def.height = clampOptionalSize(getInt(obj, "height", 0));
            def.hotbarY = getInt(obj, "hotbarY", -1);
            def.rows = getInt(obj, "rows", 0);
            def.columns = getInt(obj, "columns", 0);
            def.visibleRows = clampOptionalSize(getInt(obj, "visibleRows", getInt(obj, "visible_rows", 0)));
            def.visibleColumns = clampOptionalSize(getInt(obj, "visibleColumns", getInt(obj, "visible_columns", 0)));
            def.spacingX = clamp(getInt(obj, "spacingX", getInt(obj, "spacing_x", 2)), 0, MAX_SLOT_SPACING);
            def.spacingY = clamp(getInt(obj, "spacingY", getInt(obj, "spacing_y", 2)), 0, MAX_SLOT_SPACING);
            def.slotSize = clamp(getInt(obj, "slotSize", getInt(obj, "slot_size", getInt(obj, "size", 16))), 1, 256);
            def.scrollMode = lower(getString(obj, "scrollMode"));
            def.scrollbar = getBoolean(obj, "scrollbar");
            def.scrollbarX = getInt(obj, "scrollbarX", getInt(obj, "scrollbar_x", 0));
            def.scrollbarY = getInt(obj, "scrollbarY", getInt(obj, "scrollbar_y", 0));
            def.scrollbarHeight = clampOptionalSize(getInt(obj, "scrollbarHeight", getInt(obj, "scrollbar_height", 0)));
            def.scrollbarWidth = clamp(getInt(obj, "scrollbarWidth", getInt(obj, "scrollbar_width", 12)), 1, MAX_COMPONENT_SIZE);
            def.scrollbarThumbHeight = clamp(getInt(obj, "scrollbarThumbHeight", getInt(obj, "scrollbar_thumb_height", 15)), 1, MAX_COMPONENT_SIZE);
            def.scrollbarTexture = getString(obj, "scrollbarTexture");
            def.scrollbarHoverTexture = getString(obj, "scrollbarHoverTexture");
            def.scrollbarPressedTexture = getString(obj, "scrollbarPressedTexture");
            def.scrollbarDisabledTexture = getString(obj, "scrollbarDisabledTexture");
            def.scrollbarTextureWidth = clamp(getInt(obj, "scrollbarTextureWidth", getInt(obj, "scrollbar_texture_width", 256)), 1, MAX_COMPONENT_SIZE);
            def.scrollbarTextureHeight = clamp(getInt(obj, "scrollbarTextureHeight", getInt(obj, "scrollbar_texture_height", 256)), 1, MAX_COMPONENT_SIZE);
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
            def.itemOverlayTextureWidth = clamp(getInt(obj, "itemOverlayTextureWidth", getInt(obj, "item_overlay_texture_width", 16)), 1, MAX_COMPONENT_SIZE);
            def.itemOverlayTextureHeight = clamp(getInt(obj, "itemOverlayTextureHeight", getInt(obj, "item_overlay_texture_height", 16)), 1, MAX_COMPONENT_SIZE);
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
            def.tips = parseStringList(obj, "tips", "tooltip", "tooltips");
            def.priority = getInt(obj, "priority", getInt(obj, "zIndex", getInt(obj, "z_index", getInt(obj, "z", getInt(obj, "layer", 0)))));
            if (def.type != null && !def.type.trim().isEmpty()) {
                if ("slot_grid".equals(def.type) || "slots".equals(def.type)) {
                    nextAutoSlotIndex = expandSlotGrid(obj, def, out, nextAutoSlotIndex);
                } else {
                    if ("slot".equals(def.type) && isRuntimeSlotRole(def.role)) {
                        if (def.index < 0) {
                            def.index = nextAutoSlotIndex;
                        }
                        nextAutoSlotIndex = Math.max(nextAutoSlotIndex, def.index + 1);
                    }
                    out.add(def);
                }
            }
        }
        return out;
    }

    private static int expandSlotGrid(JsonObject obj, ComponentDef grid, List<ComponentDef> out, int nextAutoSlotIndex) {
        int rows = Math.max(1, getFirstInt(obj, 1, "rows", "rowCount", "yCount"));
        int columns = Math.max(1, getFirstInt(obj, 1, "columns", "cols", "columnCount", "xCount"));
        if (rows > MAX_GRID_ROWS || columns > MAX_GRID_COLUMNS || (long) rows * (long) columns > MAX_GRID_SLOTS) {
            LOGGER.warn("Skipping slot grid with size {}x{}; max cells is {}", rows, columns, MAX_GRID_SLOTS);
            return nextAutoSlotIndex;
        }
        int spacingX = clamp(getFirstInt(obj, 2, "spacingX", "xSpacing", "gapX"), 0, MAX_SLOT_SPACING);
        int spacingY = clamp(getFirstInt(obj, 2, "spacingY", "ySpacing", "gapY"), 0, MAX_SLOT_SPACING);
        int slotSize = clamp(getFirstInt(obj, 16, "slotSize", "size"), 1, 256);
        int visibleRows = clampOptionalSize(getFirstInt(obj, 0, "visibleRows", "visible_rows"));
        int visibleColumns = clampOptionalSize(getFirstInt(obj, 0, "visibleColumns", "visible_columns"));
        int scrollbarX = getFirstInt(obj, 0, "scrollbarX", "scrollbar_x");
        int scrollbarY = getFirstInt(obj, 0, "scrollbarY", "scrollbar_y");
        int scrollbarHeight = clampOptionalSize(getFirstInt(obj, 0, "scrollbarHeight", "scrollbar_height"));
        int scrollbarWidth = clamp(getFirstInt(obj, 12, "scrollbarWidth", "scrollbar_width"), 1, MAX_COMPONENT_SIZE);
        int scrollbarThumbHeight = clamp(getFirstInt(obj, 15, "scrollbarThumbHeight", "scrollbar_thumb_height"), 1, MAX_COMPONENT_SIZE);
        String scrollbarTexture = getString(obj, "scrollbarTexture");
        String scrollbarHoverTexture = getString(obj, "scrollbarHoverTexture");
        String scrollbarPressedTexture = getString(obj, "scrollbarPressedTexture");
        String scrollbarDisabledTexture = getString(obj, "scrollbarDisabledTexture");
        int scrollbarTextureWidth = clamp(getFirstInt(obj, 256, "scrollbarTextureWidth", "scrollbar_texture_width"), 1, MAX_COMPONENT_SIZE);
        int scrollbarTextureHeight = clamp(getFirstInt(obj, 256, "scrollbarTextureHeight", "scrollbar_texture_height"), 1, MAX_COMPONENT_SIZE);
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
        int itemOverlayTextureWidth = clamp(getFirstInt(obj, 16, "itemOverlayTextureWidth", "item_overlay_texture_width"), 1, MAX_COMPONENT_SIZE);
        int itemOverlayTextureHeight = clamp(getFirstInt(obj, 16, "itemOverlayTextureHeight", "item_overlay_texture_height"), 1, MAX_COMPONENT_SIZE);
        int itemOverlayU = getFirstInt(obj, 0, "itemOverlayU", "item_overlay_u");
        int itemOverlayV = getFirstInt(obj, 0, "itemOverlayV", "item_overlay_v");
        String scrollMode = lower(getString(obj, "scrollMode"));
        Boolean scrollbar = getBoolean(obj, "scrollbar");
        int baseIndex = grid.index >= 0 ? grid.index : nextAutoSlotIndex;
        if (baseIndex > Integer.MAX_VALUE - (rows * columns)) {
            LOGGER.warn("Skipping slot grid with overflowing base index {}", baseIndex);
            return nextAutoSlotIndex;
        }
        if (out.size() > MAX_GUI_COMPONENTS - (rows * columns)) {
            LOGGER.warn("Skipping slot grid with {} cells because custom hatch GUI component cap is {}", rows * columns, MAX_GUI_COMPONENTS);
            return nextAutoSlotIndex;
        }
        int ordinal = 0;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                ComponentDef slot = new ComponentDef();
                slot.type = "slot";
                slot.role = grid.role;
                slot.index = baseIndex + ordinal;
                slot.x = grid.x + column * (slotSize + spacingX);
                slot.y = grid.y + row * (slotSize + spacingY);
                slot.gridBaseIndex = baseIndex;
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
                slot.tips = grid.tips;
                out.add(slot);
                ordinal++;
            }
        }
        return Math.max(nextAutoSlotIndex, baseIndex + rows * columns);
    }

    private static List<MachineComponentDef> parseMachineComponents(@Nullable JsonArray array) {
        if (array == null || array.size() == 0) {
            return Collections.emptyList();
        }
        List<MachineComponentDef> out = new ArrayList<MachineComponentDef>();
        int limit = Math.min(array.size(), MAX_MACHINE_COMPONENTS);
        if (array.size() > MAX_MACHINE_COMPONENTS) {
            LOGGER.warn("Skipping {} extra custom hatch machine components; max is {}", array.size() - MAX_MACHINE_COMPONENTS, MAX_MACHINE_COMPONENTS);
        }
        for (int i = 0; i < limit; i++) {
            JsonElement element = array.get(i);
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

    private static List<String> parseStringList(JsonObject obj, String... keys) {
        if (obj == null || keys == null) {
            return Collections.emptyList();
        }
        for (String key : keys) {
            JsonElement element = obj.get(key);
            if (element == null || element.isJsonNull()) {
                continue;
            }
            List<String> out = new ArrayList<String>();
            if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                for (int i = 0; i < array.size() && i < MAX_TEXTS; i++) {
                    String value = asString(array.get(i), null);
                    if (value != null && !value.trim().isEmpty()) {
                        out.add(value.trim());
                    }
                }
            } else {
                String value = asString(element, null);
                if (value != null && !value.trim().isEmpty()) {
                    out.add(value.trim());
                }
            }
            return out.isEmpty() ? Collections.<String>emptyList() : out;
        }
        return Collections.emptyList();
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

    private static boolean isRuntimeSlotRole(@Nullable String role) {
        return "input".equalsIgnoreCase(role) || "output".equalsIgnoreCase(role);
    }

    @Nullable
    private static String getString(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        return asString(e, null);
    }

    @Nullable
    private static JsonObject getObject(@Nullable JsonObject obj, String key) {
        JsonElement e = obj == null ? null : obj.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    @Nullable
    private static JsonArray getArray(@Nullable JsonObject obj, String key) {
        JsonElement e = obj == null ? null : obj.get(key);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : null;
    }

    private static String getString(@Nullable JsonObject primary, JsonObject fallbackObj, String key, String fallback) {
        JsonElement e = primary == null ? null : primary.get(key);
        if (e == null || e.isJsonNull()) {
            e = fallbackObj.get(key);
        }
        return asString(e, fallback);
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
        return asInt(e, fallback);
    }

    private static long getLong(JsonObject obj, String key, long fallback) {
        JsonElement e = obj.get(key);
        return asLong(e, fallback);
    }

    private static int getInt(@Nullable JsonObject primary, JsonObject fallbackObj, String key, int fallback) {
        JsonElement e = primary == null ? null : primary.get(key);
        if (e == null || e.isJsonNull()) {
            e = fallbackObj.get(key);
        }
        return asInt(e, fallback);
    }

    private static int getFirstInt(JsonObject obj, int fallback, String... keys) {
        for (String key : keys) {
            JsonElement e = obj.get(key);
            if (e != null && !e.isJsonNull()) {
                return asInt(e, fallback);
            }
        }
        return fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long clampLong(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampCoordinateSize(int value) {
        return value <= 0 ? -1 : clamp(value, 1, 4096);
    }

    private static int clampOptionalSize(int value) {
        return value <= 0 ? 0 : clamp(value, 1, MAX_COMPONENT_SIZE);
    }

    @Nullable
    private static String getFirstString(JsonObject obj, String... keys) {
        if (obj == null) {
            return null;
        }
        for (String key : keys) {
            JsonElement e = obj.get(key);
            if (e != null && !e.isJsonNull()) {
                return asString(e, null);
            }
        }
        return null;
    }

    @Nullable
    private static Boolean getBoolean(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        return asBoolean(e, null);
    }

    private static boolean getFirstBoolean(JsonObject obj, boolean fallback, String... keys) {
        if (obj == null) {
            return fallback;
        }
        for (String key : keys) {
            JsonElement e = obj.get(key);
            if (e != null && !e.isJsonNull()) {
                Boolean value = asBoolean(e, null);
                return value == null ? fallback : value.booleanValue();
            }
        }
        return fallback;
    }

    private static boolean getBoolean(@Nullable JsonObject primary, JsonObject fallbackObj, String key, boolean fallback) {
        JsonElement e = primary == null ? null : primary.get(key);
        if (e == null || e.isJsonNull()) {
            e = fallbackObj.get(key);
        }
        Boolean value = asBoolean(e, null);
        return value == null ? fallback : value.booleanValue();
    }

    @Nullable
    private static Float getFloat(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        return asFloat(e, null);
    }

    @Nullable
    private static Float getFirstFloat(JsonObject obj, String... keys) {
        for (String key : keys) {
            JsonElement e = obj.get(key);
            if (e != null && !e.isJsonNull()) {
                return asFloat(e, null);
            }
        }
        return null;
    }

    @Nullable
    private static Double getFirstDouble(JsonObject obj, String... keys) {
        if (obj == null) {
            return null;
        }
        for (String key : keys) {
            JsonElement e = obj.get(key);
            if (e != null && !e.isJsonNull()) {
                return asDouble(e, null);
            }
        }
        return null;
    }

    @Nullable
    private static JsonArray firstArray(@Nullable JsonArray... arrays) {
        if (arrays == null) {
            return null;
        }
        for (JsonArray array : arrays) {
            if (array != null) {
                return array;
            }
        }
        return null;
    }

    private static float getFloat(@Nullable JsonObject primary, JsonObject fallbackObj, String key, float fallback) {
        JsonElement e = primary == null ? null : primary.get(key);
        if (e == null || e.isJsonNull()) {
            e = fallbackObj.get(key);
        }
        Float value = asFloat(e, null);
        return value == null ? fallback : value.floatValue();
    }

    private static int asInt(@Nullable JsonElement e, int fallback) {
        if (e == null || e.isJsonNull()) {
            return fallback;
        }
        try {
            return e.getAsInt();
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static long asLong(@Nullable JsonElement e, long fallback) {
        if (e == null || e.isJsonNull()) {
            return fallback;
        }
        try {
            return e.getAsLong();
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    @Nullable
    private static Float asFloat(@Nullable JsonElement e, @Nullable Float fallback) {
        if (e == null || e.isJsonNull()) {
            return fallback;
        }
        try {
            return Float.valueOf(e.getAsFloat());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    @Nullable
    private static Double asDouble(@Nullable JsonElement e, @Nullable Double fallback) {
        if (e == null || e.isJsonNull()) {
            return fallback;
        }
        try {
            return Double.valueOf(e.getAsDouble());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    @Nullable
    private static Boolean asBoolean(@Nullable JsonElement e, @Nullable Boolean fallback) {
        if (e == null || e.isJsonNull()) {
            return fallback;
        }
        try {
            return Boolean.valueOf(e.getAsBoolean());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    @Nullable
    private static String asString(@Nullable JsonElement e, @Nullable String fallback) {
        if (e == null || e.isJsonNull()) {
            return fallback;
        }
        try {
            return e.getAsString();
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static Path resolveHatchDir() {
        Path dir = Loader.instance().getConfigDir().toPath().resolve("mmceguiext").resolve("custom_hatches");
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
        public boolean outputSlotLock = true;
        public List<MachineComponentDef> machineComponents = Collections.emptyList();
        public long capacity;
        public long fluidCapacity;
        public long gasCapacity;
        public long energyCapacity;
        public long energyTransfer;
        public List<String> tips = Collections.emptyList();
        public SlotDef inputSlot = new SlotDef();
        public SlotDef outputSlot = new SlotDef();
        public TankDef tank = new TankDef();
        public List<TextDef> texts = Collections.emptyList();
        public GuiDef gui = new GuiDef();
    }

    public static class BlockDef {
        @Nullable
        public String model;
        @Nullable
        public String texture;
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
        public List<BlockTextureLevelDef> textureLevels = Collections.emptyList();
    }

    public static class BlockTextureLevelDef {
        @Nullable
        public String content;
        @Nullable
        public String texture;
        @Nullable
        public String model;
        public double minFillRatio;
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
        public List<String> tips = Collections.emptyList();
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

    private static double normalizeFillRatio(@Nullable Double value) {
        if (value == null) {
            return 0.0D;
        }
        double ratio = value.doubleValue();
        if (Double.isNaN(ratio) || Double.isInfinite(ratio)) {
            return 0.0D;
        }
        if (ratio < 0.0D) {
            return 0.0D;
        }
        if (ratio > 1.0D) {
            return 1.0D;
        }
        return ratio;
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
