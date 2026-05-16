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

    private CustomHatchModelRegistry() {
    }

    public static void rebuild() {
        TEXTURES.clear();
        List<CustomHatchRegistry.CustomHatchDef> defs = CustomHatchRegistry.getRegistered();
        if (defs.isEmpty()) {
            defs = CustomHatchRegistry.getCached();
        }
        for (CustomHatchRegistry.CustomHatchDef def : defs) {
            if (def == null || def.id == null || def.id.trim().isEmpty()) {
                continue;
            }
            ResourceLocation texture = parseBlockTexture(def.blockTexture);
            if (texture == null) {
                continue;
            }
            TEXTURES.put(normalizeId(def.id), texture);
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

    private static String normalizeId(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }

    @Nullable
    private static ResourceLocation parseBlockTexture(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replace('\\', '/');
        if (value.isEmpty()) {
            return null;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("暂时保留字段") || lower.contains("currently unused") || lower.contains("not yet wired")) {
            return null;
        }
        if (value.contains(":")) {
            String[] split = value.split(":", 2);
            String namespace = split[0];
            String path = split[1];
            if (path.startsWith("textures/")) {
                path = path.substring("textures/".length());
            }
            if (path.endsWith(".png")) {
                path = path.substring(0, path.length() - 4);
            }
            return new ResourceLocation(namespace, path);
        }
        if (value.startsWith("assets/")) {
            String rest = value.substring("assets/".length());
            int slash = rest.indexOf('/');
            if (slash > 0) {
                String namespace = rest.substring(0, slash);
                String path = rest.substring(slash + 1);
                if (path.startsWith("textures/")) {
                    path = path.substring("textures/".length());
                }
                if (path.endsWith(".png")) {
                    path = path.substring(0, path.length() - 4);
                }
                return new ResourceLocation(namespace, path);
            }
        }
        return new ResourceLocation(MMCEGuiExt.MODID, value);
    }
}
