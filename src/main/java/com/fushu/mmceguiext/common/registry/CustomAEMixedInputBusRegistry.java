package com.fushu.mmceguiext.common.registry;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.config.TextureLayerDef;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

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

public final class CustomAEMixedInputBusRegistry {
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);
    private static final Path BUS_DIR = resolveBusDir();
    private static final int MAX_GUI_COMPONENTS = 2048;
    private static final int MAX_COMPONENT_INDEX = 4095;
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
        try (Stream<Path> stream = Files.list(BUS_DIR)) {
            stream.filter(p -> p.toString().endsWith(".json")).forEach(path -> {
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
            def.fluidConfigTanks = new ArrayList<TankRect>(parseTankRects(root.getAsJsonArray("fluidConfigTanks")));
            def.gasConfigTanks = new ArrayList<TankRect>(parseTankRects(root.getAsJsonArray("gasConfigTanks")));
            def.fluidConfigTank = parseTankRect(root.getAsJsonObject("fluidConfigTank"));
            def.gasConfigTank = parseTankRect(root.getAsJsonObject("gasConfigTank"));
            def.fluidStorageTanks = new ArrayList<TankRect>(parseTankRects(root.getAsJsonArray("fluidStorageTanks")));
            def.gasStorageTanks = new ArrayList<TankRect>(parseTankRects(root.getAsJsonArray("gasStorageTanks")));
            if (def.fluidConfigTank != null && def.fluidConfigTanks.isEmpty()) {
                def.fluidConfigTanks.add(def.fluidConfigTank);
            }
            if (def.gasConfigTank != null && def.gasConfigTanks.isEmpty()) {
                def.gasConfigTanks.add(def.gasConfigTank);
            }
            if (def.fluidStorageTank != null && def.fluidStorageTanks.isEmpty()) {
                def.fluidStorageTanks.add(def.fluidStorageTank);
            }
            if (def.gasStorageTank != null && def.gasStorageTanks.isEmpty()) {
                def.gasStorageTanks.add(def.gasStorageTank);
            }
            def.blockTexture = getString(root, "blockTexture");
            def.blockModel = getBlockModel(root);
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
        int limit = Math.min(array.size(), MAX_GUI_COMPONENTS);
        if (array.size() > MAX_GUI_COMPONENTS) {
            LOGGER.warn("Skipping {} extra AE mixed input GUI components; max is {}", array.size() - MAX_GUI_COMPONENTS, MAX_GUI_COMPONENTS);
        }
        for (int i = 0; i < limit; i++) {
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

    private static List<TextureLayerDef> parseTextureLayers(@Nullable com.google.gson.JsonArray array) {
        if (array == null || array.size() == 0) {
            return Collections.emptyList();
        }
        List<TextureLayerDef> out = new ArrayList<TextureLayerDef>();
        for (int i = 0; i < array.size(); i++) {
            if (!array.get(i).isJsonObject()) {
                continue;
            }
            JsonObject obj = array.get(i).getAsJsonObject();
            ResourceLocation texture = parseOptionalTexture(getString(obj, "texture"));
            if (texture == null) {
                continue;
            }
            TextureLayerDef def = new TextureLayerDef();
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

    @Nullable
    private static ResourceLocation parseOptionalTexture(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String raw = value.trim().replace('\\', '/');
        if (raw.isEmpty() || raw.contains("..") || raw.startsWith("/")) {
            return null;
        }
        if (raw.endsWith(".png")) {
            raw = raw.substring(0, raw.length() - 4);
        }
        if (raw.startsWith("assets/")) {
            String rest = raw.substring("assets/".length());
            int slash = rest.indexOf('/');
            if (slash > 0) {
                String namespace = rest.substring(0, slash);
                String path = rest.substring(slash + 1);
                while (path.startsWith("textures/")) {
                    path = path.substring("textures/".length());
                }
                return createTextureLocation(namespace, "textures/" + path + ".png");
            }
        }
        if (raw.contains(":")) {
            String[] split = raw.split(":", 2);
            if (split[0].trim().isEmpty() || split[1].trim().isEmpty()) {
                return null;
            }
            String path = split[1];
            if (path.startsWith("textures/")) {
                return createTextureLocation(split[0], path + ".png");
            }
            return createTextureLocation(split[0], "textures/" + path + ".png");
        }
        return createTextureLocation(MMCEGuiExt.MODID, "textures/" + raw + ".png");
    }

    @Nullable
    private static ResourceLocation createTextureLocation(String namespace, String path) {
        if (namespace == null || namespace.trim().isEmpty() || path == null || path.trim().isEmpty()
            || path.contains("..") || path.startsWith("/")) {
            return null;
        }
        try {
            return new ResourceLocation(namespace, path);
        } catch (RuntimeException ex) {
            LOGGER.warn("Ignoring invalid texture location {}:{}", namespace, path);
            return null;
        }
    }

    private static void applyGuiComponents(Def def) {
        int itemConfigIndex = 0;
        int itemStorageIndex = 0;
        int fluidConfigIndex = 0;
        int fluidStorageIndex = 0;
        int gasConfigIndex = 0;
        int gasStorageIndex = 0;
        for (ComponentDef component : def.gui.components) {
            if (component == null || component.type == null) {
                continue;
            }
            if ("slot".equals(component.type)) {
                if ("item_config".equals(component.role)) {
                    if (component.index < 0) {
                        component.index = itemConfigIndex;
                    }
                    if (!isValidComponentIndex(component, def)) {
                        continue;
                    }
                    itemConfigIndex = Math.max(itemConfigIndex, component.index + 1);
                    if (!ensureListSize(def.configSlots, component.index + 1)) {
                        continue;
                    }
                    def.configSlots.set(component.index, toSlotPoint(component));
                } else if ("item_output".equals(component.role) || "item_storage".equals(component.role)) {
                    if (component.index < 0) {
                        component.index = itemStorageIndex;
                    }
                    if (!isValidComponentIndex(component, def)) {
                        continue;
                    }
                    itemStorageIndex = Math.max(itemStorageIndex, component.index + 1);
                    if (!ensureListSize(def.storageSlots, component.index + 1)) {
                        continue;
                    }
                    def.storageSlots.set(component.index, toSlotPoint(component));
                } else if ("fluid_config".equals(component.role)) {
                    int index = component.index >= 0 ? component.index : fluidConfigIndex;
                    component.index = index;
                    if (!isValidComponentIndex(component, def)) {
                        continue;
                    }
                    fluidConfigIndex = Math.max(fluidConfigIndex, index + 1);
                    if (!ensureTankListSize(def.fluidConfigTanks, index + 1)) {
                        continue;
                    }
                    def.fluidConfigTanks.set(index, toTankRect(component));
                    if (def.fluidConfigTank == null || index == 0) {
                        def.fluidConfigSlot = toSlotPoint(component);
                        def.fluidConfigTank = toTankRect(component);
                    }
                } else if ("gas_config".equals(component.role)) {
                    int index = component.index >= 0 ? component.index : gasConfigIndex;
                    component.index = index;
                    if (!isValidComponentIndex(component, def)) {
                        continue;
                    }
                    gasConfigIndex = Math.max(gasConfigIndex, index + 1);
                    if (!ensureTankListSize(def.gasConfigTanks, index + 1)) {
                        continue;
                    }
                    def.gasConfigTanks.set(index, toTankRect(component));
                    if (def.gasConfigTank == null || index == 0) {
                        def.gasConfigSlot = toSlotPoint(component);
                        def.gasConfigTank = toTankRect(component);
                    }
                }
            } else if ("tank".equals(component.type)) {
                if ("fluid_storage".equals(component.role)) {
                    int index = component.index >= 0 ? component.index : fluidStorageIndex;
                    component.index = index;
                    if (!isValidComponentIndex(component, def)) {
                        continue;
                    }
                    fluidStorageIndex = Math.max(fluidStorageIndex, index + 1);
                    if (!ensureTankListSize(def.fluidStorageTanks, index + 1)) {
                        continue;
                    }
                    def.fluidStorageTanks.set(index, toTankRect(component));
                    if (def.fluidStorageTank == null || index == 0) {
                        def.fluidStorageTank = toTankRect(component);
                    }
                } else if ("gas_storage".equals(component.role)) {
                    int index = component.index >= 0 ? component.index : gasStorageIndex;
                    component.index = index;
                    if (!isValidComponentIndex(component, def)) {
                        continue;
                    }
                    gasStorageIndex = Math.max(gasStorageIndex, index + 1);
                    if (!ensureTankListSize(def.gasStorageTanks, index + 1)) {
                        continue;
                    }
                    def.gasStorageTanks.set(index, toTankRect(component));
                    if (def.gasStorageTank == null || index == 0) {
                        def.gasStorageTank = toTankRect(component);
                    }
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
            for (int i = 0; i < def.fluidConfigTanks.size(); i++) {
                TankRect tank = def.fluidConfigTanks.get(i);
                if (tank == null) {
                    continue;
                }
                ComponentDef component = new ComponentDef();
                component.type = "slot";
                component.role = "fluid_config";
                component.index = i;
                component.x = tank.x;
                component.y = tank.y;
                component.width = tank.width;
                component.height = tank.height;
                components.add(component);
            }
        }
        if (def.fluidStorageTank != null) {
            for (int i = 0; i < def.fluidStorageTanks.size(); i++) {
                TankRect tank = def.fluidStorageTanks.get(i);
                if (tank == null) {
                    continue;
                }
                ComponentDef component = new ComponentDef();
                component.type = "tank";
                component.role = "fluid_storage";
                component.index = i;
                component.x = tank.x;
                component.y = tank.y;
                component.width = tank.width;
                component.height = tank.height;
                components.add(component);
            }
        }
        if (def.gasConfigTank != null) {
            for (int i = 0; i < def.gasConfigTanks.size(); i++) {
                TankRect tank = def.gasConfigTanks.get(i);
                if (tank == null) {
                    continue;
                }
                ComponentDef component = new ComponentDef();
                component.type = "slot";
                component.role = "gas_config";
                component.index = i;
                component.x = tank.x;
                component.y = tank.y;
                component.width = tank.width;
                component.height = tank.height;
                components.add(component);
            }
        }
        if (def.gasStorageTank != null) {
            for (int i = 0; i < def.gasStorageTanks.size(); i++) {
                TankRect tank = def.gasStorageTanks.get(i);
                if (tank == null) {
                    continue;
                }
                ComponentDef component = new ComponentDef();
                component.type = "tank";
                component.role = "gas_storage";
                component.index = i;
                component.x = tank.x;
                component.y = tank.y;
                component.width = tank.width;
                component.height = tank.height;
                components.add(component);
            }
        }
        ComponentDef playerInv = new ComponentDef();
        playerInv.type = "player_inventory";
        playerInv.x = def.playerInventoryX;
        playerInv.y = def.playerInventoryY;
        components.add(playerInv);
        gui.components = components;
        return gui;
    }

    private static boolean isValidComponentIndex(ComponentDef component, Def def) {
        if (component.index >= 0 && component.index <= MAX_COMPONENT_INDEX) {
            return true;
        }
        LOGGER.warn("Skipping AE mixed input component with invalid index {} in {}", component.index, def == null ? "<unknown>" : def.id);
        return false;
    }

    private static boolean ensureListSize(List<SlotPoint> list, int targetSize) {
        if (targetSize < 0 || targetSize > MAX_COMPONENT_INDEX + 1) {
            return false;
        }
        while (list.size() < targetSize) {
            list.add(null);
        }
        return true;
    }

    private static boolean ensureTankListSize(List<TankRect> list, int targetSize) {
        if (targetSize < 0 || targetSize > MAX_COMPONENT_INDEX + 1) {
            return false;
        }
        while (list.size() < targetSize) {
            list.add(null);
        }
        return true;
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

    private static List<TankRect> parseTankRects(@Nullable com.google.gson.JsonArray array) {
        if (array == null || array.size() == 0) {
            return Collections.emptyList();
        }
        List<TankRect> out = new ArrayList<TankRect>();
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i).isJsonObject()) {
                TankRect rect = parseTankRect(array.get(i).getAsJsonObject());
                if (rect != null) {
                    out.add(rect);
                }
            }
        }
        return out;
    }

    @Nullable
    private static String getString(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    @Nullable
    private static String getBlockModel(JsonObject root) {
        JsonObject block = root == null ? null : root.getAsJsonObject("block");
        String nested = getString(block, "model");
        return nested == null || nested.trim().isEmpty() ? getString(root, "blockModel") : nested;
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
        Path dir = Loader.instance().getConfigDir().toPath().resolve("mmceguiext").resolve("custom_ae_mixed_input_buses");
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
        public List<TextureLayerDef> textureLayers = Collections.emptyList();
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
        public List<TankRect> fluidConfigTanks = Collections.emptyList();
        public List<TankRect> gasConfigTanks = Collections.emptyList();
        public List<TankRect> fluidStorageTanks = Collections.emptyList();
        public List<TankRect> gasStorageTanks = Collections.emptyList();
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
