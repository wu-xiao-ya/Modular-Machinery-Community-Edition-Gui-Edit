package com.fushu.mmceguiext.client.model;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.util.CustomIdValidator;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Locale;

public final class CustomBlockTextureParser {
    private CustomBlockTextureParser() {
    }

    @Nullable
    public static ResourceLocation parse(@Nullable String raw) {
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
            return createTextureLocation(split[0], normalizeTexturePath(split[1]));
        }
        if (value.startsWith("assets/")) {
            String rest = value.substring("assets/".length());
            int slash = rest.indexOf('/');
            if (slash > 0) {
                return createTextureLocation(rest.substring(0, slash), normalizeTexturePath(rest.substring(slash + 1)));
            }
        }
        return createTextureLocation(MMCEGuiExt.MODID, normalizeTexturePath(value));
    }

    @Nullable
    private static ResourceLocation createTextureLocation(String namespace, String path) {
        return CustomIdValidator.createResourceLocation(namespace, path);
    }

    private static String normalizeTexturePath(String raw) {
        String path = raw == null ? "" : raw.trim().replace('\\', '/');
        while (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        return path;
    }
}
