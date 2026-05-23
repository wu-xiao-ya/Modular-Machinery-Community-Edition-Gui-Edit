package com.fushu.mmceguiext.common.registry;

import com.fushu.mmceguiext.client.gui.GlobalTextureLayerConfig;
import com.fushu.mmceguiext.MMCEGuiExt;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.util.ResourceLocation;

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

public final class CustomAEMixedInputBusRegistry {
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);
    private static final Path BUS_DIR = resolveBusDir();
    private static final List<Def> CACHE = new ArrayList<Def>();
    private static final Map<String, Def> REGISTERED = new LinkedHashMap<String, Def>();

    private CustomAEMixedInputBusRegistry() {
    }

    public static List<Def> loadAll() {
        CACHE.clear();
        REGISTERED.clear();
        if (!Files.isDirectory(BUS_DIR)) {
            return Collections.emptyList();
        }
        try {
            Files.list(BUS_DIR).filter(p -> p.toString().endsWith(".json")).forEach(path -> {
                Def def = load(path);
                if (def != null) {
                    CACHE.add(def);
                    REGISTERED.put(normalizeId(def.id), def);
                }
            });
        } catch (IOException ex) {
            LOGGER.warn("Failed to scan custom AE mixed input bus dir {}: {}", BUS_DIR, ex.getMessage());
        }
        return new ArrayList<Def>(CACHE);
    }

    public static List<Def> getCached() {
        return new ArrayList<Def>(CACHE);
    }

    @Nullable
    public static Def findById(@Nullable String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeId(id);
        Def direct = REGISTERED.get(normalized);
        if (direct != null) {
            return direct;
        }
        String pathId = normalized.contains(":") ? normalized.substring(normalized.indexOf(':') + 1) : normalized;
        direct = REGISTERED.get(pathId);
        if (direct != null) {
            return direct;
        }
        for (Def def : CACHE) {
            if (def != null && def.id != null) {
                String defId = normalizeId(def.id);
                if (defId.equals(normalized) || defId.equals(pathId) || defId.endsWith(":" + pathId)) {
                    return def;
                }
            }
        }
        return null;
    }

    @Nullable
    public static Def load(Path path) {
        try {
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JsonObject root = new JsonParser().parse(text).getAsJsonObject();
            Def def = new Def();
            def.id = getString(root, "id");
            def.displayName = getString(root, "displayName");
            def.guiBackgroundTexture = getString(root, "guiBackgroundTexture");
            def.guiWidth = getInt(root, "guiWidth", 176);
            def.guiHeight = getInt(root, "guiHeight", 235);
            def.backgroundTextureWidth = getInt(root, "backgroundTextureWidth", def.guiWidth);
            def.backgroundTextureHeight = getInt(root, "backgroundTextureHeight", def.guiHeight);
            def.textureLayers = parseTextureLayers(root.getAsJsonArray("textureLayers"));
            def.playerInventoryX = getInt(root, "playerInventoryX", 8);
            def.playerInventoryY = getInt(root, "playerInventoryY", 141);
            def.playerHotbarY = getInt(root, "playerHotbarY", 199);
            def.configSlots = new ArrayList<SlotPoint>(parseSlotPoints(root.getAsJsonArray("configSlots")));
            def.storageSlots = new ArrayList<SlotPoint>(parseSlotPoints(root.getAsJsonArray("storageSlots")));
            def.fluidConfigSlot = parseSlotPoint(root.getAsJsonObject("fluidConfigSlot"));
            def.fluidStorageTank = parseTankRect(root.getAsJsonObject("fluidStorageTank"));
            def.gasConfigSlot = parseSlotPoint(root.getAsJsonObject("gasConfigSlot"));
            def.gasStorageTank = parseTankRect(root.getAsJsonObject("gasStorageTank"));
            def.fluidConfigTank = parseTankRect(root.getAsJsonObject("fluidConfigTank"));
            def.gasConfigTank = parseTankRect(root.getAsJsonObject("gasConfigTank"));
            def.blockTexture = getString(root, "blockTexture");
            def.blockModel = getString(root, "blockModel");
            def.gui = parseGui(root.getAsJsonObject("gui"));
            if (def.gui != null && def.gui.components != null && !def.gui.components.isEmpty()) {
                applyGuiComponents(def);
            } else {
                def.gui = buildLegacyGui(def);
            }
            return def.id == null || def.id.trim().isEmpty() ? null : def;
        } catch (Exception ex) {
            LOGGER.warn("[MMCEGE-NEW] Failed to parse custom AE mixed input bus {}", path, ex);
            return null;
        }
    }

    private static GuiDef parseGui(@Nullable JsonObject obj) {
        GuiDef gui = new GuiDef();
        if (obj == null) {
            return gui;
        }
        gui.width = getInt(obj, "width", 176);
        gui.height = getInt(obj, "height", 235);
        gui.components = parseComponents(obj.getAsJsonArray("components"));
        return gui;
    }

    private static List<ComponentDef> parseComponents(@Nullable com.google.gson.JsonArray array) {
        if (array == null || array.size() == 0) {
            return Collections.emptyList();
        }
        List<ComponentDef> out = new ArrayList<ComponentDef>();
        for (int i = 0; i < array.size(); i++) {
            if (!array.get(i).isJsonObject()) {
                continue;
            }
            JsonObject obj = array.get(i).getAsJsonObject();
            ComponentDef def = new ComponentDef();
            def.type = lower(getString(obj, "type"));
            def.role = lower(getString(obj, "role"));
            def.x = getInt(obj, "x", 0);
            def.y = getInt(obj, "y", 0);
            def.width = getInt(obj, "width", 16);
            def.height = getInt(obj, "height", 16);
            def.index = getInt(obj, "index", -1);
            if (def.type != null && !def.type.trim().isEmpty()) {
                out.add(def);
            }
        }
        return out;
    }

    private static List<GlobalTextureLayerConfig.LayerDef> parseTextureLayers(@Nullable com.google.gson.JsonArray array) {
        if (array == null || array.size() == 0) {
            return Collections.emptyList();
        }
        List<GlobalTextureLayerConfig.LayerDef> out = new ArrayList<GlobalTextureLayerConfig.LayerDef>();
        for (int i = 0; i < array.size(); i++) {
            if (!array.get(i).isJsonObject()) {
                continue;
            }
            JsonObject obj = array.get(i).getAsJsonObject();
            ResourceLocation texture = com.fushu.mmceguiext.client.gui.GuiRenderUtils.parseOptionalTexture(getString(obj, "texture"));
            if (texture == null) {
                continue;
            }
            GlobalTextureLayerConfig.LayerDef def = new GlobalTextureLayerConfig.LayerDef();
            def.foreground = obj.has("foreground") && !obj.get("foreground").isJsonNull() && obj.get("foreground").getAsBoolean();
            def.texture = texture;
            def.x = getInt(obj, "x", 0);
            def.y = getInt(obj, "y", 0);
            def.width = getInt(obj, "width", 16);
            def.height = getInt(obj, "height", 16);
            def.textureWidth = getInt(obj, "textureWidth", def.width);
            def.textureHeight = getInt(obj, "textureHeight", def.height);
            def.corner = getInt(obj, "corner", 0);
            def.useNineSlice = obj.has("useNineSlice") && !obj.get("useNineSlice").isJsonNull() && obj.get("useNineSlice").getAsBoolean();
            def.priority = getInt(obj, "priority", 0);
            out.add(def);
        }
        return out;
    }

    private static void applyGuiComponents(Def def) {
        for (ComponentDef component : def.gui.components) {
            if (component == null || component.type == null) {
                continue;
            }
            if ("slot".equals(component.type)) {
                if ("item_config".equals(component.role)) {
                    if (component.index >= 0) {
                        ensureListSize(def.configSlots, component.index + 1);
                        def.configSlots.set(component.index, toSlotPoint(component));
                    }
                } else if ("item_output".equals(component.role) || "item_storage".equals(component.role)) {
                    if (component.index >= 0) {
                        ensureListSize(def.storageSlots, component.index + 1);
                        def.storageSlots.set(component.index, toSlotPoint(component));
                    }
                } else if ("fluid_config".equals(component.role)) {
                    def.fluidConfigSlot = toSlotPoint(component);
                    def.fluidConfigTank = toTankRect(component);
                } else if ("gas_config".equals(component.role)) {
                    def.gasConfigSlot = toSlotPoint(component);
                    def.gasConfigTank = toTankRect(component);
                }
            } else if ("tank".equals(component.type)) {
                if ("fluid_storage".equals(component.role)) {
                    def.fluidStorageTank = toTankRect(component);
                } else if ("gas_storage".equals(component.role)) {
                    def.gasStorageTank = toTankRect(component);
                }
            } else if ("player_inventory".equals(component.type)) {
                def.playerInventoryX = component.x;
                def.playerInventoryY = component.y;
            }
        }
    }

    private static GuiDef buildLegacyGui(Def def) {
        GuiDef gui = new GuiDef();
        gui.width = def.guiWidth;
        gui.height = def.guiHeight;
        List<ComponentDef> components = new ArrayList<ComponentDef>();
        for (int i = 0; i < def.configSlots.size(); i++) {
            SlotPoint point = def.configSlots.get(i);
            if (point == null) {
                continue;
            }
            ComponentDef component = new ComponentDef();
            component.type = "slot";
            component.role = "item_config";
            component.index = i;
            component.x = point.x;
            component.y = point.y;
            component.width = 16;
            component.height = 16;
            components.add(component);
        }
        for (int i = 0; i < def.storageSlots.size(); i++) {
            SlotPoint point = def.storageSlots.get(i);
            if (point == null) {
                continue;
            }
            ComponentDef component = new ComponentDef();
            component.type = "slot";
            component.role = "item_storage";
            component.index = i;
            component.x = point.x;
            component.y = point.y;
            component.width = 16;
            component.height = 16;
            components.add(component);
        }
        if (def.fluidConfigTank != null) {
            ComponentDef component = new ComponentDef();
            component.type = "slot";
            component.role = "fluid_config";
            component.index = 0;
            component.x = def.fluidConfigTank.x;
            component.y = def.fluidConfigTank.y;
            component.width = def.fluidConfigTank.width;
            component.height = def.fluidConfigTank.height;
            components.add(component);
        }
        if (def.fluidStorageTank != null) {
            ComponentDef component = new ComponentDef();
            component.type = "tank";
            component.role = "fluid_storage";
            component.x = def.fluidStorageTank.x;
            component.y = def.fluidStorageTank.y;
            component.width = def.fluidStorageTank.width;
            component.height = def.fluidStorageTank.height;
            components.add(component);
        }
        if (def.gasConfigTank != null) {
            ComponentDef component = new ComponentDef();
            component.type = "slot";
            component.role = "gas_config";
            component.index = 0;
            component.x = def.gasConfigTank.x;
            component.y = def.gasConfigTank.y;
            component.width = def.gasConfigTank.width;
            component.height = def.gasConfigTank.height;
            components.add(component);
        }
        if (def.gasStorageTank != null) {
            ComponentDef component = new ComponentDef();
            component.type = "tank";
            component.role = "gas_storage";
            component.x = def.gasStorageTank.x;
            component.y = def.gasStorageTank.y;
            component.width = def.gasStorageTank.width;
            component.height = def.gasStorageTank.height;
            components.add(component);
        }
        ComponentDef playerInv = new ComponentDef();
        playerInv.type = "player_inventory";
        playerInv.x = def.playerInventoryX;
        playerInv.y = def.playerInventoryY;
        components.add(playerInv);
        gui.components = components;
        return gui;
    }

    private static void ensureListSize(List<SlotPoint> list, int targetSize) {
        while (list.size() < targetSize) {
            list.add(null);
        }
    }

    private static SlotPoint toSlotPoint(ComponentDef component) {
        SlotPoint point = new SlotPoint();
        point.x = component.x;
        point.y = component.y;
        return point;
    }

    private static TankRect toTankRect(ComponentDef component) {
        TankRect rect = new TankRect();
        rect.x = component.x;
        rect.y = component.y;
        rect.width = component.width;
        rect.height = component.height;
        return rect;
    }

    private static List<SlotPoint> parseSlotPoints(@Nullable com.google.gson.JsonArray array) {
        if (array == null || array.size() == 0) {
            return Collections.emptyList();
        }
        List<SlotPoint> out = new ArrayList<SlotPoint>();
        for (int i = 0; i < array.size(); i++) {
            if (!array.get(i).isJsonObject()) {
                continue;
            }
            JsonObject obj = array.get(i).getAsJsonObject();
            SlotPoint point = new SlotPoint();
            point.x = getInt(obj, "x", 0);
            point.y = getInt(obj, "y", 0);
            out.add(point);
        }
        return out;
    }

    private static SlotPoint parseSlotPoint(@Nullable JsonObject obj) {
        if (obj == null) {
            return null;
        }
        SlotPoint point = new SlotPoint();
        point.x = getInt(obj, "x", 0);
        point.y = getInt(obj, "y", 0);
        return point;
    }

    private static TankRect parseTankRect(@Nullable JsonObject obj) {
        if (obj == null) {
            return null;
        }
        TankRect rect = new TankRect();
        rect.x = getInt(obj, "x", 0);
        rect.y = getInt(obj, "y", 0);
        rect.width = getInt(obj, "width", 16);
        rect.height = getInt(obj, "height", 16);
        return rect;
    }

    @Nullable
    private static String getString(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    private static int getInt(@Nullable JsonObject obj, String key, int fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        return obj.get(key).getAsInt();
    }

    @Nullable
    private static String lower(@Nullable String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private static Path resolveBusDir() {
        Path dir = Paths.get("config").resolve("mmceguiext").resolve("custom_ae_mixed_input_buses");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
        return dir;
    }

    private static String normalizeId(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }

    public static class Def {
        public String id;
        public String displayName;
        public String guiBackgroundTexture;
        public int guiWidth = 176;
        public int guiHeight = 235;
        public int backgroundTextureWidth = 176;
        public int backgroundTextureHeight = 235;
        public List<GlobalTextureLayerConfig.LayerDef> textureLayers = Collections.emptyList();
        public int playerInventoryX = 8;
        public int playerInventoryY = 141;
        public int playerHotbarY = 199;
        public List<SlotPoint> configSlots = Collections.emptyList();
        public List<SlotPoint> storageSlots = Collections.emptyList();
        public SlotPoint fluidConfigSlot;
        public TankRect fluidStorageTank;
        public SlotPoint gasConfigSlot;
        public TankRect gasStorageTank;
        public TankRect fluidConfigTank;
        public TankRect gasConfigTank;
        public String blockTexture;
        public String blockModel;
        public GuiDef gui = new GuiDef();
    }

    public static class SlotPoint {
        public int x;
        public int y;
    }

    public static class TankRect {
        public int x;
        public int y;
        public int width;
        public int height;
    }

    public static class GuiDef {
        public int width = 176;
        public int height = 235;
        public List<ComponentDef> components = Collections.emptyList();
    }

    public static class ComponentDef {
        public String type;
        public String role;
        public int x;
        public int y;
        public int width = 16;
        public int height = 16;
        public int index = -1;
    }
}
