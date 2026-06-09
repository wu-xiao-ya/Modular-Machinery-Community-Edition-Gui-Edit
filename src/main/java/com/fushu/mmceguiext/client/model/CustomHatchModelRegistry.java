package com.fushu.mmceguiext.client.model;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.registry.CustomHatchRegistry;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CustomHatchModelRegistry {
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<String, ResourceLocation>();
    private static final Map<String, ResourceLocation> SOURCE_MODELS = new HashMap<String, ResourceLocation>();

    private CustomHatchModelRegistry() {
    }

    public static void rebuild() {
        TEXTURES.clear();
        SOURCE_MODELS.clear();
        List<CustomHatchRegistry.CustomHatchDef> defs = CustomHatchRegistry.getRegistered();
        if (defs.isEmpty()) {
            defs = CustomHatchRegistry.getCached();
        }
        for (CustomHatchRegistry.CustomHatchDef def : defs) {
            if (def == null || def.id == null || def.id.trim().isEmpty()) {
                continue;
            }
            ResourceLocation texture = CustomBlockTextureParser.parse(def.blockTexture);
            if (texture != null) {
                TEXTURES.put(normalizeId(def.id), texture);
            }
            ResourceLocation sourceModel = parseDirectModel(def.block == null ? null : def.block.model);
            if (sourceModel == null) {
                sourceModel = parseDirectModel(def.blockModel);
            }
            if (sourceModel != null) {
                SOURCE_MODELS.put(normalizeId(def.id), sourceModel);
            }
        }
    }

    @Nullable
    public static ResourceLocation getTexture(@Nullable String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeId(id);
        ResourceLocation direct = TEXTURES.get(normalized);
        if (direct != null) {
            return direct;
        }
        String path = normalized.contains(":") ? normalized.substring(normalized.indexOf(':') + 1) : normalized;
        return TEXTURES.get(path);
    }

    @Nullable
    public static ResourceLocation getSourceModel(@Nullable String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeId(id);
        ResourceLocation direct = SOURCE_MODELS.get(normalized);
        if (direct != null) {
            return direct;
        }
        String path = normalized.contains(":") ? normalized.substring(normalized.indexOf(':') + 1) : normalized;
        return SOURCE_MODELS.get(path);
    }

    @Nullable
    private static ResourceLocation parseDirectModel(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replace('\\', '/');
        if (value.isEmpty()) {
            return null;
        }
        if (value.contains("#")) {
            value = value.substring(0, value.indexOf('#'));
        } else if (value.contains("[")) {
            value = value.substring(0, value.indexOf('['));
        }
        if (value.endsWith(".json")) {
            value = value.substring(0, value.length() - 5);
        }
        if (value.startsWith("assets/")) {
            String rest = value.substring("assets/".length());
            int slash = rest.indexOf('/');
            if (slash > 0) {
                return createModelLocation(rest.substring(0, slash), normalizeModelPath(rest.substring(slash + 1)));
            }
        }
        if (value.contains(":")) {
            String[] split = value.split(":", 2);
            return createModelLocation(split[0], normalizeModelPath(split[1]));
        }
        return createModelLocation(MMCEGuiExt.MODID, normalizeModelPath(value));
    }

    @Nullable
    private static ResourceLocation createModelLocation(String namespace, String path) {
        return com.fushu.mmceguiext.common.util.CustomIdValidator.createResourceLocation(namespace, path);
    }

    private static String normalizeModelPath(String raw) {
        String path = raw == null ? "" : raw.trim().replace('\\', '/');
        while (path.startsWith("models/")) {
            path = path.substring("models/".length());
        }
        if (!path.startsWith("block/")) {
            path = "block/" + path;
        }
        return path;
    }

    private static String normalizeId(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }

}
