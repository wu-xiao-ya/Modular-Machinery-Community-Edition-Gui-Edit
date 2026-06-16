package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.common.config.TextureLayerDef;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public final class GlobalTextureLayerConfig {
    private static final int MAX_LAYERS = 256;
    private static final int MAX_TEXTURE_SIZE = 4096;
    private static final int MAX_CORNER = 128;

    private GlobalTextureLayerConfig() {
    }

    public static List<LayerDef> parse(String[] entries) {
        if (entries == null || entries.length == 0) {
            return Collections.emptyList();
        }
        List<LayerDef> layers = new ArrayList<LayerDef>();
        int limit = Math.min(entries.length, MAX_LAYERS);
        for (int i = 0; i < limit; i++) {
            String entry = entries[i];
            LayerDef def = parseEntry(entry);
            if (def != null) {
                layers.add(def);
            }
        }
        layers.sort(Comparator.comparingInt(a -> a.priority));
        return layers;
    }

    @Nullable
    private static LayerDef parseEntry(String entry) {
        if (entry == null) {
            return null;
        }
        String text = entry.trim();
        if (text.isEmpty()) {
            return null;
        }
        String[] parts = text.split(",");
        if (parts.length < 10) {
            return null;
        }
        ResourceLocation texture = GuiRenderUtils.parseLooseTexture(parts[1].trim());
        if (texture == null) {
            return null;
        }
        try {
            LayerDef def = new LayerDef();
            def.foreground = "fg".equalsIgnoreCase(parts[0].trim());
            def.texture = texture;
            def.x = Integer.parseInt(parts[2].trim());
            def.y = Integer.parseInt(parts[3].trim());
            def.width = clamp(Integer.parseInt(parts[4].trim()), 1, MAX_TEXTURE_SIZE);
            def.height = clamp(Integer.parseInt(parts[5].trim()), 1, MAX_TEXTURE_SIZE);
            def.textureWidth = clamp(Integer.parseInt(parts[6].trim()), 1, MAX_TEXTURE_SIZE);
            def.textureHeight = clamp(Integer.parseInt(parts[7].trim()), 1, MAX_TEXTURE_SIZE);
            def.corner = clamp(Integer.parseInt(parts[8].trim()), 0, MAX_CORNER);
            def.useNineSlice = Boolean.parseBoolean(parts[9].trim());
            def.priority = parts.length >= 11 ? Integer.parseInt(parts[10].trim()) : 0;
            def.alpha = parts.length >= 12 ? normalizeAlpha(Float.parseFloat(parts[11].trim())) : 1.0F;
            return def;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float normalizeAlpha(float value) {
        float alpha = value;
        if (alpha > 1.0F && alpha <= 255.0F) {
            alpha /= 255.0F;
        }
        if (alpha < 0.0F) {
            alpha = 0.0F;
        }
        if (alpha > 1.0F) {
            alpha = 1.0F;
        }
        return alpha;
    }

    public static void drawLayers(List<? extends TextureLayerDef> layers, boolean foreground, int guiLeft, int guiTop, int originOffsetX, int originOffsetY) {
        drawLayers(layers, foreground, guiLeft, guiTop, originOffsetX, originOffsetY, null);
    }

    public static void drawLayers(
        List<? extends TextureLayerDef> layers,
        boolean foreground,
        int guiLeft,
        int guiTop,
        int originOffsetX,
        int originOffsetY,
        @Nullable Integer priorityFilter
    ) {
        for (TextureLayerDef layer : layers) {
            if (layer.foreground != foreground) {
                continue;
            }
            if (priorityFilter != null && layer.priority != priorityFilter.intValue()) {
                continue;
            }
            net.minecraft.client.Minecraft.getMinecraft().getTextureManager().bindTexture(layer.texture);
            float alpha = normalizeAlpha(layer.alpha);
            if (alpha < 1.0F) {
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO
                );
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
            int baseX = guiLeft - originOffsetX;
            int baseY = guiTop - originOffsetY;
            int drawX = foreground ? baseX + layer.x : baseX + layer.x;
            int drawY = foreground ? baseY + layer.y : baseY + layer.y;
            if (layer.useNineSlice) {
                GuiRenderUtils.drawNineSlice(drawX, drawY, layer.width, layer.height, layer.textureWidth, layer.textureHeight, layer.corner);
            } else {
                net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(drawX, drawY, 0, 0, layer.width, layer.height, layer.textureWidth, layer.textureHeight);
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            if (alpha < 1.0F) {
                GlStateManager.disableBlend();
            }
        }
    }

    public static SortedSet<Integer> collectPriorities(List<? extends TextureLayerDef> layers, boolean foreground, int basePriority) {
        SortedSet<Integer> priorities = new TreeSet<Integer>();
        priorities.add(Integer.valueOf(basePriority));
        for (TextureLayerDef layer : layers) {
            if (layer != null && layer.foreground == foreground) {
                priorities.add(Integer.valueOf(layer.priority));
            }
        }
        return priorities;
    }

    public static class LayerDef extends TextureLayerDef {
    }
}
