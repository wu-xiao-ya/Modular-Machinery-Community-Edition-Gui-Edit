package com.fushu.mmceguiext.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

public final class GuiRenderUtils {
    private GuiRenderUtils() {
    }

    public static ResourceLocation parseTexture(String value, ResourceLocation fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return new ResourceLocation(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    @Nullable
    public static ResourceLocation parseOptionalTexture(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new ResourceLocation(value.trim());
        } catch (Exception ignored) {
            return null;
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

    public static boolean isMouseInPanel(int mouseX, int mouseY, PanelRect panel, int guiLeft, int guiTop) {
        int left = guiLeft + panel.getX();
        int top = guiTop + panel.getY();
        int right = left + panel.getWidth();
        int bottom = top + panel.getHeight();
        return mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
    }

    public static void drawNineSlice(int x, int y, int width, int height, int textureWidth, int textureHeight, int corner) {
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

        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, destCorner, destCorner, textureWidth, textureHeight);
        Gui.drawModalRectWithCustomSizedTexture(
            x + width - destCorner, y, textureWidth - srcCorner, 0, destCorner, destCorner, textureWidth, textureHeight
        );
        Gui.drawModalRectWithCustomSizedTexture(
            x, y + height - destCorner, 0, textureHeight - srcCorner, destCorner, destCorner, textureWidth, textureHeight
        );
        Gui.drawModalRectWithCustomSizedTexture(
            x + width - destCorner, y + height - destCorner,
            textureWidth - srcCorner, textureHeight - srcCorner,
            destCorner, destCorner, textureWidth, textureHeight
        );

        if (destMiddleWidth > 0) {
            Gui.drawModalRectWithCustomSizedTexture(
                x + destCorner, y, srcCorner, 0, destMiddleWidth, destCorner, textureWidth, textureHeight
            );
            Gui.drawModalRectWithCustomSizedTexture(
                x + destCorner, y + height - destCorner,
                srcCorner, textureHeight - srcCorner,
                destMiddleWidth, destCorner, textureWidth, textureHeight
            );
        }

        if (destMiddleHeight > 0) {
            Gui.drawModalRectWithCustomSizedTexture(
                x, y + destCorner, 0, srcCorner, destCorner, destMiddleHeight, textureWidth, textureHeight
            );
            Gui.drawModalRectWithCustomSizedTexture(
                x + width - destCorner, y + destCorner,
                textureWidth - srcCorner, srcCorner, destCorner, destMiddleHeight, textureWidth, textureHeight
            );
        }

        if (destMiddleWidth > 0 && destMiddleHeight > 0) {
            Gui.drawModalRectWithCustomSizedTexture(
                x + destCorner, y + destCorner,
                srcCorner, srcCorner, destMiddleWidth, destMiddleHeight, textureWidth, textureHeight
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
}
