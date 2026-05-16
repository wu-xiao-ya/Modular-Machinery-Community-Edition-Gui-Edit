package com.fushu.mmceguiext.client.gui;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class GlobalTextureLayerConfig {
    private GlobalTextureLayerConfig() {
    }

    public static List<LayerDef> parse(String[] entries) {
        if (entries == null || entries.length == 0) {
            return Collections.emptyList();
        }
        List<LayerDef> layers = new ArrayList<LayerDef>();
        for (String entry : entries) {
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
            def.width = Integer.parseInt(parts[4].trim());
            def.height = Integer.parseInt(parts[5].trim());
            def.textureWidth = Integer.parseInt(parts[6].trim());
            def.textureHeight = Integer.parseInt(parts[7].trim());
            def.corner = Integer.parseInt(parts[8].trim());
            def.useNineSlice = Boolean.parseBoolean(parts[9].trim());
            def.priority = parts.length >= 11 ? Integer.parseInt(parts[10].trim()) : 0;
            return def;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void drawLayers(List<LayerDef> layers, boolean foreground, int guiLeft, int guiTop, int originOffsetX, int originOffsetY) {
        for (LayerDef layer : layers) {
            if (layer.foreground != foreground) {
                continue;
            }
            net.minecraft.client.Minecraft.getMinecraft().getTextureManager().bindTexture(layer.texture);
            int baseX = guiLeft - originOffsetX;
            int baseY = guiTop - originOffsetY;
            int drawX = foreground ? baseX + layer.x : baseX + layer.x;
            int drawY = foreground ? baseY + layer.y : baseY + layer.y;
            if (layer.useNineSlice) {
                GuiRenderUtils.drawNineSlice(drawX, drawY, layer.width, layer.height, layer.textureWidth, layer.textureHeight, layer.corner);
            } else {
                net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(drawX, drawY, 0, 0, layer.width, layer.height, layer.textureWidth, layer.textureHeight);
            }
        }
    }

    public static class LayerDef {
        public boolean foreground;
        public ResourceLocation texture;
        public int x;
        public int y;
        public int width;
        public int height;
        public int textureWidth;
        public int textureHeight;
        public int corner;
        public boolean useNineSlice;
        public int priority;
    }
}
