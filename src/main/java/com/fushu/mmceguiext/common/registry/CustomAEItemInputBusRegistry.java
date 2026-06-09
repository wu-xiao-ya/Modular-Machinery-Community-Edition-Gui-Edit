package com.fushu.mmceguiext.common.registry;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

public final class CustomAEItemInputBusRegistry {
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);
    private static final Path BUS_DIR = resolveBusDir();
    private static final long MAX_CONFIG_BYTES = 1024L * 1024L;
    private static final int MAX_SLOT_POINTS = 4096;
    private static final List<Def> CACHE = new ArrayList<Def>();
    private static final Map<String, Def> REGISTERED = new LinkedHashMap<String, Def>();

    private CustomAEItemInputBusRegistry() {
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
                if (def != null && !REGISTERED.containsKey(normalizeId(def.id))) {
                    CACHE.add(def);
                    REGISTERED.put(normalizeId(def.id), def);
                }
            });
        } catch (IOException ex) {
            LOGGER.warn("Failed to scan custom AE item input bus dir {}: {}", BUS_DIR, ex.getMessage());
        }
        return new ArrayList<Def>(CACHE);
    }

    public static List<Def> getCached() {
        return new ArrayList<Def>(CACHE);
    }

    public static List<Def> getRegistered() {
        return new ArrayList<Def>(REGISTERED.values());
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
            if (Files.size(path) > MAX_CONFIG_BYTES) {
                LOGGER.warn("Skipping custom AE item input bus {} because it is larger than {} bytes.", path, MAX_CONFIG_BYTES);
                return null;
            }
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JsonObject root = new JsonParser().parse(text).getAsJsonObject();
            Def def = new Def();
            def.id = getString(root, "id");
            def.displayName = getString(root, "displayName");
            def.guiBackgroundTexture = getString(root, "guiBackgroundTexture");
            def.configSlots = parseSlotPoints(getArray(root, "configSlots"));
            def.storageSlots = parseSlotPoints(getArray(root, "storageSlots"));
            def.playerInventoryX = getInt(root, "playerInventoryX", 0);
            def.playerInventoryY = getInt(root, "playerInventoryY", 123);
            def.playerHotbarY = getInt(root, "playerHotbarY", 181);
            def.blockTexture = getString(root, "blockTexture");
            def.blockModel = getString(root, "blockModel");
            def.block = parseBlock(root);
            return def.id == null || def.id.trim().isEmpty() ? null : def;
        } catch (Exception ex) {
            LOGGER.warn("Failed to parse custom AE item input bus {}: {}", path, ex.getMessage());
            return null;
        }
    }

    private static CustomHatchRegistry.BlockDef parseBlock(JsonObject root) {
        CustomHatchRegistry.BlockDef block = new CustomHatchRegistry.BlockDef();
        JsonObject obj = getObject(root, "block");
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

    private static List<SlotPoint> parseSlotPoints(@Nullable com.google.gson.JsonArray array) {
        if (array == null || array.size() == 0) {
            return Collections.emptyList();
        }
        List<SlotPoint> out = new ArrayList<SlotPoint>();
        int limit = Math.min(array.size(), MAX_SLOT_POINTS);
        if (array.size() > MAX_SLOT_POINTS) {
            LOGGER.warn("Skipping {} extra AE item input slot points; max is {}", array.size() - MAX_SLOT_POINTS, MAX_SLOT_POINTS);
        }
        for (int i = 0; i < limit; i++) {
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

    @Nullable
    private static com.google.gson.JsonArray getArray(@Nullable JsonObject obj, String key) {
        JsonElement e = obj == null ? null : obj.get(key);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : null;
    }

    @Nullable
    private static JsonObject getObject(@Nullable JsonObject obj, String key) {
        JsonElement e = obj == null ? null : obj.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    @Nullable
    private static String getString(JsonObject obj, String key) {
        if (obj == null) {
            return null;
        }
        JsonElement e = obj.get(key);
        if (e == null || e.isJsonNull() || !e.isJsonPrimitive()) {
            return null;
        }
        try {
            return e.getAsString();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String getString(@Nullable JsonObject primary, @Nullable JsonObject fallbackObj, String key, String fallback) {
        String value = getString(primary, key);
        if (value == null) {
            value = getString(fallbackObj, key);
        }
        return value == null ? fallback : value;
    }

    private static int getInt(@Nullable JsonObject obj, String key, int fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static int getInt(@Nullable JsonObject primary, @Nullable JsonObject fallbackObj, String key, int fallback) {
        if (primary != null && primary.has(key) && !primary.get(key).isJsonNull()) {
            return getInt(primary, key, fallback);
        }
        return getInt(fallbackObj, key, fallback);
    }

    private static float getFloat(@Nullable JsonObject obj, String key, float fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsFloat();
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static float getFloat(@Nullable JsonObject primary, @Nullable JsonObject fallbackObj, String key, float fallback) {
        if (primary != null && primary.has(key) && !primary.get(key).isJsonNull()) {
            return getFloat(primary, key, fallback);
        }
        return getFloat(fallbackObj, key, fallback);
    }

    private static boolean getBoolean(@Nullable JsonObject obj, String key, boolean fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsBoolean();
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static boolean getBoolean(@Nullable JsonObject primary, @Nullable JsonObject fallbackObj, String key, boolean fallback) {
        if (primary != null && primary.has(key) && !primary.get(key).isJsonNull()) {
            return getBoolean(primary, key, fallback);
        }
        return getBoolean(fallbackObj, key, fallback);
    }

    private static Path resolveBusDir() {
        Path dir = Loader.instance().getConfigDir().toPath().resolve("mmceguiext").resolve("custom_ae_item_input_buses");
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
        public List<SlotPoint> configSlots = Collections.emptyList();
        public List<SlotPoint> storageSlots = Collections.emptyList();
        public int playerInventoryX = 0;
        public int playerInventoryY = 123;
        public int playerHotbarY = 181;
        public String blockTexture;
        public String blockModel;
        public CustomHatchRegistry.BlockDef block = new CustomHatchRegistry.BlockDef();
    }

    public static class SlotPoint {
        public int x;
        public int y;
    }
}
