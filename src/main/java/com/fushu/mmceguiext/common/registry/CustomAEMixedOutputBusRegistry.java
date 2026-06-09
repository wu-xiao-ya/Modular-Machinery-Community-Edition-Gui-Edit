package com.fushu.mmceguiext.common.registry;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.config.TextureLayerDef;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.ResourceLocation;
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

public final class CustomAEMixedOutputBusRegistry {
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);
    private static final Path BUS_DIR = resolveBusDir();
    private static final int MAX_GUI_COMPONENTS = 2048;
    private static final int MAX_COMPONENT_INDEX = 4095;
    private static final List<Def> CACHE = new ArrayList<Def>();
    private static final Map<String, Def> REGISTERED = new LinkedHashMap<String, Def>();

    private CustomAEMixedOutputBusRegistry() {
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
            LOGGER.warn("Failed to scan custom AE mixed output bus dir {}: {}", BUS_DIR, ex.getMessage());
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
            def.blockTexture = getString(root, "blockTexture");
            def.blockModel = getBlockModel(root);
            def.gui = parseGui(root.getAsJsonObject("gui"));
            applyGuiComponents(def);
            return def.id == null || def.id.trim().isEmpty() ? null : def;
        } catch (Exception ex) {
            LOGGER.warn("[MMCEGE-NEW] Failed to parse custom AE mixed output bus {}", path, ex);
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

    private static void applyGuiComponents(Def def) {
        if (def == null || def.gui == null || def.gui.components == null || def.gui.components.isEmpty()) {
            return;
        }
        int itemIndex = 0;
        int fluidIndex = 0;
        int gasIndex = 0;
        for (ComponentDef component : def.gui.components) {
            if (component == null) {
                continue;
            }
            if ("slot".equals(component.type)) {
                String role = component.role == null ? "" : component.role;
                if ("item_storage".equals(role) || "item_output".equals(role)) {
                    if (component.index < 0) {
                        component.index = itemIndex;
                    }
                    if (!isValidComponentIndex(component, def)) {
                        continue;
                    }
                    itemIndex = Math.max(itemIndex, component.index + 1);
                }
            } else if ("tank".equals(component.type)) {
                if ("fluid_storage".equals(component.role)) {
                    if (component.index < 0) {
                        component.index = fluidIndex;
                    }
                    if (!isValidComponentIndex(component, def)) {
                        continue;
                    }
                    fluidIndex = Math.max(fluidIndex, component.index + 1);
                } else if ("gas_storage".equals(component.role)) {
                    if (component.index < 0) {
                        component.index = gasIndex;
                    }
                    if (!isValidComponentIndex(component, def)) {
                        continue;
                    }
                    gasIndex = Math.max(gasIndex, component.index + 1);
                }
            }
        }
    }

    private static boolean isValidComponentIndex(ComponentDef component, Def def) {
        if (component.index >= 0 && component.index <= MAX_COMPONENT_INDEX) {
            return true;
        }
        LOGGER.warn("Skipping AE mixed output component with invalid index {} in {}", component.index, def == null ? "<unknown>" : def.id);
        return false;
    }

    private static List<ComponentDef> parseComponents(@Nullable com.google.gson.JsonArray array) {
        if (array == null || array.size() == 0) {
            return Collections.emptyList();
        }
        List<ComponentDef> out = new ArrayList<ComponentDef>();
        int limit = Math.min(array.size(), MAX_GUI_COMPONENTS);
        if (array.size() > MAX_GUI_COMPONENTS) {
            LOGGER.warn("Skipping {} extra AE mixed output GUI components; max is {}", array.size() - MAX_GUI_COMPONENTS, MAX_GUI_COMPONENTS);
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

    @Nullable
    private static String getString(@Nullable JsonObject obj, String key) {
        if (obj == null) {
            return null;
        }
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
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
        Path dir = Loader.instance().getConfigDir().toPath().resolve("mmceguiext").resolve("custom_ae_mixed_output_buses");
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
        public String blockTexture;
        public String blockModel;
        public List<TextureLayerDef> textureLayers = Collections.emptyList();
        public GuiDef gui = new GuiDef();
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
