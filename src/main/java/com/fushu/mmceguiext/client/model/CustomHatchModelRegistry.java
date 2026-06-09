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
            ResourceLocation texture = CustomBlockTextureParser.parse(def.blockTexture);
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

}
