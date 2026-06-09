package com.fushu.mmceguiext.client.registry;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.util.CustomIdValidator;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public final class CustomBlockModelResolver {
    private CustomBlockModelResolver() {
    }

    public static ModelResourceLocation resolve(@Nullable String rawModel, ResourceLocation fallbackLocation, String fallbackVariant) {
        ModelBinding parsed = parseModelBinding(rawModel);
        if (parsed != null) {
            return new ModelResourceLocation(parsed.location, parsed.variant);
        }
        return new ModelResourceLocation(fallbackLocation, fallbackVariant);
    }

    @Nullable
    private static ModelBinding parseModelBinding(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replace('\\', '/');
        if (value.isEmpty()) {
            return null;
        }
        String variant = "normal";
        if (value.contains("#")) {
            int split = value.indexOf('#');
            variant = value.substring(split + 1).trim();
            value = value.substring(0, split);
        } else if (value.contains("[")) {
            int split = value.indexOf('[');
            int end = value.lastIndexOf(']');
            if (end > split) {
                variant = value.substring(split + 1, end).trim();
                value = value.substring(0, split);
            }
        }
        if (value.endsWith(".json")) {
            value = value.substring(0, value.length() - 5);
        }
        if (value.startsWith("assets/")) {
            String rest = value.substring("assets/".length());
            int slash = rest.indexOf('/');
            if (slash > 0) {
                String namespace = rest.substring(0, slash);
                String path = normalizeModelPath(rest.substring(slash + 1));
                return createModelBinding(namespace, path, variant);
            }
        }
        if (value.contains(":")) {
            String[] split = value.split(":", 2);
            return createModelBinding(split[0], normalizeModelPath(split[1]), variant);
        }
        return createModelBinding(MMCEGuiExt.MODID, normalizeModelPath(value), variant);
    }

    @Nullable
    private static ModelBinding createModelBinding(String namespace, String path, @Nullable String variant) {
        String normalizedVariant = normalizeVariant(variant);
        if (!CustomIdValidator.isValidNamespace(namespace)
            || !CustomIdValidator.isValidPath(path)
            || !CustomIdValidator.isValidVariant(normalizedVariant)) {
            return null;
        }
        try {
            return new ModelBinding(new ResourceLocation(namespace, path), normalizedVariant);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String normalizeModelPath(String raw) {
        String path = raw == null ? "" : raw.trim();
        while (path.startsWith("blockstates/")) {
            path = path.substring("blockstates/".length());
        }
        while (path.startsWith("models/")) {
            path = path.substring("models/".length());
        }
        return path;
    }

    private static String normalizeVariant(@Nullable String raw) {
        String value = raw == null || raw.trim().isEmpty() ? "normal" : raw.trim().toLowerCase();
        return CustomIdValidator.isValidVariant(value) ? value : "normal";
    }

    private static final class ModelBinding {
        private final ResourceLocation location;
        private final String variant;

        private ModelBinding(ResourceLocation location, String variant) {
            this.location = location;
            this.variant = variant;
        }
    }
}
