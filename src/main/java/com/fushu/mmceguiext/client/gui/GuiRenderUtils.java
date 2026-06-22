package com.fushu.mmceguiext.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.awt.Rectangle;

public final class GuiRenderUtils {
    private GuiRenderUtils() {
    }

    public static ResourceLocation parseTexture(String value, ResourceLocation fallback) {
        ResourceLocation texture = parseOptionalTexture(value);
        return texture == null ? fallback : texture;
    }

    @Nullable
    public static ResourceLocation parseOptionalTexture(String value) {
        String raw = normalizeRawTexturePath(value);
        if (raw == null) {
            return null;
        }
        try {
            ResourceLocation original = new ResourceLocation(raw);
            if (resourceExists(original)) {
                return original;
            }

            String normalized = normalizeGuiTexturePath(raw);
            if (normalized != null && !normalized.equals(raw)) {
                ResourceLocation guiTexture = new ResourceLocation(normalized);
                if (resourceExists(guiTexture)) {
                    return guiTexture;
                }
            }

            return original;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static ResourceLocation parseLooseTexture(String value) {
        return parseOptionalTexture(value);
    }

    public static ResourceLocation parseTexture(String value) {
        return parseOptionalTexture(value);
    }

    public static boolean hasTexture(String value) {
        return parseOptionalTexture(value) != null;
    }

    @Nullable
    private static String normalizeRawTexturePath(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim().replace('\\', '/');
        if (text.isEmpty() || text.contains("..") || text.startsWith("/") || text.matches("^[A-Za-z]:.*")) {
            return null;
        }
        return text;
    }

    @Nullable
    private static String normalizeGuiTexturePath(String value) {
        String text = normalizeRawTexturePath(value);
        if (text == null) {
            return null;
        }
        int separator = text.indexOf(':');
        if (separator < 0) {
            return text;
        }

        String domain = text.substring(0, separator).trim();
        String path = text.substring(separator + 1).trim();
        if (domain.isEmpty() || path.isEmpty()) {
            return null;
        }
        if (!path.startsWith("textures/")) {
            path = path.startsWith("gui/") ? "textures/" + path : "textures/gui/" + path;
        }
        return domain + ":" + path;
    }

    private static boolean resourceExists(ResourceLocation texture) {
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft == null) {
                return false;
            }
            IResourceManager manager = minecraft.getResourceManager();
            if (manager == null) {
                return false;
            }
            manager.getResource(texture);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static void enableScissor(Minecraft mc, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        ScaledResolution resolution = new ScaledResolution(mc);
        int scale = resolution.getScaleFactor();
        int scissorX = x * scale;
        int scissorY = mc.displayHeight - (y + height) * scale;
        int scissorWidth = width * scale;
        int scissorHeight = height * scale;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    public static void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static boolean isMouseInPanel(int mouseX, int mouseY, Rectangle panel, int guiLeft, int guiTop) {
        int left = guiLeft + panel.x;
        int top = guiTop + panel.y;
        int right = left + panel.width;
        int bottom = top + panel.height;
        return mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
    }

    public static void drawNineSlice(int x, int y, int width, int height, int textureWidth, int textureHeight, int corner) {
        drawNineSlice(x, y, width, height, 0, 0, textureWidth, textureHeight, corner);
    }

    public static void drawTexturedRect(
        int x,
        int y,
        int u,
        int v,
        int width,
        int height,
        int textureWidth,
        int textureHeight
    ) {
        if (width <= 0 || height <= 0 || textureWidth <= 0 || textureHeight <= 0) {
            return;
        }
        Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public static void drawScaledTexturedRect(
        int x,
        int y,
        int width,
        int height,
        int u,
        int v,
        int sourceWidth,
        int sourceHeight,
        int textureWidth,
        int textureHeight
    ) {
        if (width <= 0 || height <= 0 || sourceWidth <= 0 || sourceHeight <= 0 || textureWidth <= 0 || textureHeight <= 0) {
            return;
        }

        double u0 = (double) u / (double) textureWidth;
        double v0 = (double) v / (double) textureHeight;
        double u1 = (double) (u + sourceWidth) / (double) textureWidth;
        double v1 = (double) (v + sourceHeight) / (double) textureHeight;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos((double) x, (double) (y + height), 0.0D).tex(u0, v1).endVertex();
        buffer.pos((double) (x + width), (double) (y + height), 0.0D).tex(u1, v1).endVertex();
        buffer.pos((double) (x + width), (double) y, 0.0D).tex(u1, v0).endVertex();
        buffer.pos((double) x, (double) y, 0.0D).tex(u0, v0).endVertex();
        tessellator.draw();
    }

    public static void drawNineSlice(
        int x,
        int y,
        int width,
        int height,
        int u,
        int v,
        int textureWidth,
        int textureHeight,
        int corner
    ) {
        if (width <= 0 || height <= 0 || textureWidth <= 0 || textureHeight <= 0) {
            return;
        }

        int maxCorner = Math.max(1, Math.min(textureWidth, textureHeight) / 2);
        int srcCorner = Math.max(1, Math.min(corner, maxCorner));
        int destCorner = Math.max(1, Math.min(srcCorner, Math.min(width, height) / 2));

        int srcMiddleWidth = Math.max(1, textureWidth - srcCorner * 2);
        int srcMiddleHeight = Math.max(1, textureHeight - srcCorner * 2);
        int destMiddleWidth = Math.max(0, width - destCorner * 2);
        int destMiddleHeight = Math.max(0, height - destCorner * 2);

        Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, destCorner, destCorner, textureWidth, textureHeight);
        Gui.drawModalRectWithCustomSizedTexture(
            x + width - destCorner, y, u + textureWidth - srcCorner, v, destCorner, destCorner, textureWidth, textureHeight
        );
        Gui.drawModalRectWithCustomSizedTexture(
            x, y + height - destCorner, u, v + textureHeight - srcCorner, destCorner, destCorner, textureWidth, textureHeight
        );
        Gui.drawModalRectWithCustomSizedTexture(
            x + width - destCorner, y + height - destCorner,
            u + textureWidth - srcCorner, v + textureHeight - srcCorner,
            destCorner, destCorner, textureWidth, textureHeight
        );

        if (destMiddleWidth > 0) {
            Gui.drawModalRectWithCustomSizedTexture(
                x + destCorner, y, u + srcCorner, v, destMiddleWidth, destCorner, textureWidth, textureHeight
            );
            Gui.drawModalRectWithCustomSizedTexture(
                x + destCorner, y + height - destCorner,
                u + srcCorner, v + textureHeight - srcCorner,
                destMiddleWidth, destCorner, textureWidth, textureHeight
            );
        }

        if (destMiddleHeight > 0) {
            Gui.drawModalRectWithCustomSizedTexture(
                x, y + destCorner, u, v + srcCorner, destCorner, destMiddleHeight, textureWidth, textureHeight
            );
            Gui.drawModalRectWithCustomSizedTexture(
                x + width - destCorner, y + destCorner,
                u + textureWidth - srcCorner, v + srcCorner, destCorner, destMiddleHeight, textureWidth, textureHeight
            );
        }

        if (destMiddleWidth > 0 && destMiddleHeight > 0) {
            Gui.drawModalRectWithCustomSizedTexture(
                x + destCorner, y + destCorner,
                u + srcCorner, v + srcCorner, destMiddleWidth, destMiddleHeight, textureWidth, textureHeight
            );
        }
    }

    public static int parseColorARGBOrDefault(@Nullable String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return fallback;
        }

        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        if (text.startsWith("0x") || text.startsWith("0X")) {
            text = text.substring(2);
        }

        try {
            if (text.length() == 6) {
                return (int) (0xFF000000L | Long.parseLong(text, 16));
            }
            if (text.length() == 8) {
                return (int) Long.parseLong(text, 16);
            }
        } catch (NumberFormatException ignored) {
        }
        return fallback;
    }

    public static void applyColorARGB(int color) {
        float a = ((color >>> 24) & 0xFF) / 255.0F;
        float r = ((color >>> 16) & 0xFF) / 255.0F;
        float g = ((color >>> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        GlStateManager.color(r, g, b, a);
    }

    public static int resolveAlignedTextX(int anchorX, int textWidth, @Nullable String align) {
        if (textWidth <= 0 || align == null) {
            return anchorX;
        }
        if ("center".equalsIgnoreCase(align)) {
            return anchorX - textWidth / 2;
        }
        if ("right".equalsIgnoreCase(align)) {
            return anchorX - textWidth;
        }
        return anchorX;
    }
}
