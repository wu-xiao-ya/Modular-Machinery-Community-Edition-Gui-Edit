package com.fushu.mmceguiext.common.util;

import com.fushu.mmceguiext.MMCEGuiExt;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Locale;

public final class CustomIdValidator {
    private static final int MAX_ID_LENGTH = 128;
    private static final String NAMESPACE_PATTERN = "[a-z0-9_.-]+";
    private static final String PATH_PATTERN = "[a-z0-9_./-]+";
    private static final String VARIANT_PATTERN = "[a-z0-9_=,./-]+";

    private CustomIdValidator() {
    }

    public static String normalizePath(@Nullable String id, String fallback) {
        String value = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        if (value.contains(":")) {
            value = value.substring(value.indexOf(':') + 1);
        }
        return isValidPath(value) ? value : fallback;
    }

    public static boolean isValidPath(@Nullable String path) {
        return path != null
            && !path.trim().isEmpty()
            && path.length() <= MAX_ID_LENGTH
            && !path.startsWith("/")
            && !path.contains("..")
            && !path.contains("//")
            && path.matches(PATH_PATTERN)
            && !hasInvalidPathSegment(path);
    }

    public static boolean isValidNamespace(@Nullable String namespace) {
        return namespace != null
            && !namespace.trim().isEmpty()
            && namespace.length() <= MAX_ID_LENGTH
            && namespace.matches(NAMESPACE_PATTERN);
    }

    public static boolean isValidVariant(@Nullable String variant) {
        return variant != null
            && !variant.trim().isEmpty()
            && variant.length() <= MAX_ID_LENGTH
            && !variant.contains("..")
            && variant.matches(VARIANT_PATTERN);
    }

    public static boolean isValidResourceLocation(@Nullable String id) {
        return sanitizeResourceLocation(id) != null;
    }

    @Nullable
    public static String sanitizeResourceLocation(@Nullable String id) {
        if (id == null) {
            return null;
        }
        String value = id.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty() || value.length() > MAX_ID_LENGTH) {
            return null;
        }
        String namespace = MMCEGuiExt.MODID;
        String path = value;
        if (value.contains(":")) {
            String[] split = value.split(":", 2);
            namespace = split[0];
            path = split[1];
        }
        if (!isValidNamespace(namespace) || !isValidPath(path)) {
            return null;
        }
        return namespace + ":" + path;
    }

    @Nullable
    public static String readSanitizedString(NBTTagCompound tag, String key) {
        if (tag == null || !tag.hasKey(key) || !(tag.getTag(key) instanceof NBTTagString)) {
            return null;
        }
        return sanitizeResourceLocation(tag.getString(key));
    }

    @Nullable
    public static ResourceLocation createResourceLocation(@Nullable String namespace, @Nullable String path) {
        if (!isValidNamespace(namespace) || !isValidPath(path)) {
            return null;
        }
        try {
            return new ResourceLocation(namespace, path);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static boolean hasInvalidPathSegment(String path) {
        String[] segments = path.split("/");
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                return true;
            }
        }
        return false;
    }
}
