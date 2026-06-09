package com.fushu.mmceguiext.client.config;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.client.gui.GlobalTextureLayerConfig;
import com.fushu.mmceguiext.client.gui.GuiRenderUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;
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
import java.util.List;

public final class GlobalGuiStyleManager {
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);
    private static final Path STYLE_DIR = resolveStyleDir();

    private GlobalGuiStyleManager() {
    }

    public static StyleFile load(@Nullable String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return StyleFile.EMPTY;
        }
        Path styleDir = STYLE_DIR.toAbsolutePath().normalize();
        Path path = styleDir.resolve(fileName.trim()).normalize();
        if (!path.startsWith(styleDir)) {
            LOGGER.warn("MMCE GUI ext style file escapes style dir: {}", fileName);
            return StyleFile.EMPTY;
        }
        if (!Files.exists(path)) {
            LOGGER.warn("MMCE GUI ext style file not found: {}", path);
            return StyleFile.EMPTY;
        }
        try {
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JsonObject root = new JsonParser().parse(text).getAsJsonObject();
            StyleFile style = new StyleFile();
            style.background = parseBackground(getObject(root, "background"));
            style.tank = parseRect(getObject(root, "tank"));
            style.texts = parseTexts(getArray(root, "texts"));
            style.layers = parseLayers(getArray(root, "layers"));
            return style;
        } catch (Exception ex) {
            LOGGER.warn("Failed to load MMCE GUI ext style file {}: {}", path, ex.getMessage());
            return StyleFile.EMPTY;
        }
    }

    private static Background parseBackground(@Nullable JsonObject obj) {
        if (obj == null) {
            return null;
        }
        Background bg = new Background();
        bg.texture = getString(obj, "texture");
        bg.textureWidth = getInt(obj, "textureWidth");
        bg.textureHeight = getInt(obj, "textureHeight");
        bg.offsetX = getInt(obj, "offsetX");
        bg.offsetY = getInt(obj, "offsetY");
        bg.corner = getInt(obj, "corner");
        bg.useNineSlice = getBoolean(obj, "useNineSlice");
        return bg;
    }

    @Nullable
    private static Rect parseRect(@Nullable JsonObject obj) {
        if (obj == null) {
            return null;
        }
        Rect rect = new Rect();
        rect.x = getInt(obj, "x");
        rect.y = getInt(obj, "y");
        rect.width = getInt(obj, "width");
        rect.height = getInt(obj, "height");
        rect.renderMode = normalizeRenderMode(
            getString(obj, "renderMode"),
            getString(obj, "render_mode"),
            getString(obj, "render"),
            getString(obj, "mode")
        );
        rect.alpha = normalizeAlpha(
            getFloat(obj, "alpha"),
            getFloat(obj, "opacity"),
            getFloat(obj, "transparency")
        );
        return rect;
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
            TextDef def = new TextDef();
            def.x = orZero(getInt(obj, "x"));
            def.y = orZero(getInt(obj, "y"));
            def.value = getString(obj, "value");
            def.color = getString(obj, "color");
            def.scale = getFloat(obj, "scale");
            def.align = normalizeTextAlign(getString(obj, "align"), getString(obj, "alignment"), getString(obj, "textAlign"), getString(obj, "text_align"));
            def.priority = getInt(obj, "priority");
            if (def.priority == null) def.priority = getInt(obj, "zIndex");
            if (def.priority == null) def.priority = getInt(obj, "z_index");
            if (def.priority == null) def.priority = getInt(obj, "z");
            if (def.priority == null) def.priority = getInt(obj, "layer");
            if (def.value != null && !def.value.trim().isEmpty()) {
                out.add(def);
            }
        }
        out.sort(java.util.Comparator.comparingInt(a -> a.priority == null ? 0 : a.priority.intValue()));
        return out;
    }

    private static List<GlobalTextureLayerConfig.LayerDef> parseLayers(@Nullable JsonArray array) {
        if (array == null || array.size() == 0) {
            return Collections.emptyList();
        }
        List<GlobalTextureLayerConfig.LayerDef> out = new ArrayList<GlobalTextureLayerConfig.LayerDef>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            ResourceLocation texture = GuiRenderUtils.parseLooseTexture(getString(obj, "texture"));
            if (texture == null) {
                continue;
            }
            GlobalTextureLayerConfig.LayerDef layer = new GlobalTextureLayerConfig.LayerDef();
            layer.foreground = "fg".equalsIgnoreCase(getString(obj, "type"));
            layer.texture = texture;
            layer.x = orZero(getInt(obj, "x"));
            layer.y = orZero(getInt(obj, "y"));
            layer.width = orZero(getInt(obj, "width"));
            layer.height = orZero(getInt(obj, "height"));
            layer.textureWidth = orZero(getInt(obj, "textureWidth"));
            layer.textureHeight = orZero(getInt(obj, "textureHeight"));
            layer.corner = getInt(obj, "corner") == null ? 8 : getInt(obj, "corner").intValue();
            layer.useNineSlice = getBoolean(obj, "useNineSlice") != null && getBoolean(obj, "useNineSlice").booleanValue();
            layer.priority = getInt(obj, "priority") == null ? 0 : getInt(obj, "priority").intValue();
            out.add(layer);
        }
        out.sort(java.util.Comparator.comparingInt(a -> a.priority));
        return out;
    }

    private static int orZero(@Nullable Integer value) {
        return value == null ? 0 : value.intValue();
    }

    @Nullable
    private static String getString(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        if (e == null || e.isJsonNull() || !e.isJsonPrimitive()) {
            return null;
        }
        try {
            return e.getAsString();
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static Integer getInt(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        if (e == null || e.isJsonNull() || !e.isJsonPrimitive()) {
            return null;
        }
        try {
            return Integer.valueOf(e.getAsInt());
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static Boolean getBoolean(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        if (e == null || e.isJsonNull() || !e.isJsonPrimitive()) {
            return null;
        }
        try {
            return Boolean.valueOf(e.getAsBoolean());
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static Float getFloat(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        if (e == null || e.isJsonNull() || !e.isJsonPrimitive()) {
            return null;
        }
        try {
            return Float.valueOf(e.getAsFloat());
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static JsonObject getObject(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    @Nullable
    private static JsonArray getArray(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : null;
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
            String text = value.trim().toLowerCase(java.util.Locale.ROOT);
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

    @Nullable
    private static String normalizeRenderMode(@Nullable String... values) {
        if (values == null) {
            return null;
        }
        for (@Nullable String value : values) {
            if (value == null) {
                continue;
            }
            String text = value.trim().toLowerCase(java.util.Locale.ROOT);
            if (!text.isEmpty()) {
                return text;
            }
        }
        return null;
    }

    @Nullable
    private static Float normalizeAlpha(@Nullable Float... values) {
        if (values == null) {
            return null;
        }
        for (@Nullable Float value : values) {
            if (value == null) {
                continue;
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
        return null;
    }

    private static Path resolveStyleDir() {
        Object mcHome = Launch.blackboard.get("mcLocation");
        Path base = mcHome instanceof java.io.File ? ((java.io.File) mcHome).toPath() : Paths.get(".");
        Path dir = base.resolve("config").resolve("mmceguiext").resolve("styles");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
        return dir;
    }

    public static class StyleFile {
        public static final StyleFile EMPTY = new StyleFile();
        @Nullable
        public Background background;
        @Nullable
        public Rect tank;
        public List<TextDef> texts = Collections.emptyList();
        public List<GlobalTextureLayerConfig.LayerDef> layers = Collections.emptyList();
    }

    public static class Background {
        @Nullable
        public String texture;
        @Nullable
        public Integer textureWidth;
        @Nullable
        public Integer textureHeight;
        @Nullable
        public Integer offsetX;
        @Nullable
        public Integer offsetY;
        @Nullable
        public Integer corner;
        @Nullable
        public Boolean useNineSlice;
    }

    public static class Rect {
        @Nullable
        public Integer x;
        @Nullable
        public Integer y;
        @Nullable
        public Integer width;
        @Nullable
        public Integer height;
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
        public String color;
        @Nullable
        public Float scale;
        @Nullable
        public String align;
        @Nullable
        public Integer priority;
    }
}
