package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import com.fushu.mmceguiext.client.config.ProgressBarStyleSupport;
import com.fushu.mmceguiext.common.util.ControllerCustomDataAccess;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DynamicVisualRenderer {
    public interface MetricProvider {
        float getMachineMetric(String metric, float fallback);
    }

    private static final float EPSILON = 1.0E-4F;
    private final Map<String, HistoryState> histories = new HashMap<String, HistoryState>();

    public void reset() {
        this.histories.clear();
    }

    public void render(
        @Nullable List<MachineGuiStyleManager.DynamicVisualStyle> visuals,
        @Nullable TileMultiblockMachineController controller,
        MetricProvider metricProvider,
        int originX,
        int originY,
        boolean foreground,
        @Nullable Integer priorityFilter,
        PagePredicate pagePredicate,
        int defaultPriority
    ) {
        if (visuals == null || visuals.isEmpty()) {
            return;
        }
        for (MachineGuiStyleManager.DynamicVisualStyle visual : visuals) {
            if (!shouldRender(visual, foreground, priorityFilter, pagePredicate, defaultPriority)) {
                continue;
            }
            float raw = resolveRawValue(visual.source, controller, metricProvider);
            float normalized = normalizeValue(raw, visual.source);
            updateHistoryIfNeeded(visual, normalized, controller);
            renderVisual(visual, raw, normalized, originX, originY);
        }
    }

    public void collectForegroundPriorities(
        @Nullable List<MachineGuiStyleManager.DynamicVisualStyle> visuals,
        java.util.Set<Integer> out,
        int defaultPriority
    ) {
        if (visuals == null || out == null) {
            return;
        }
        for (MachineGuiStyleManager.DynamicVisualStyle visual : visuals) {
            if (visual != null && (visual.foreground == null || visual.foreground.booleanValue())) {
                out.add(Integer.valueOf(resolvePriority(visual, defaultPriority)));
            }
        }
    }

    private boolean shouldRender(
        @Nullable MachineGuiStyleManager.DynamicVisualStyle visual,
        boolean foreground,
        @Nullable Integer priorityFilter,
        PagePredicate pagePredicate,
        int defaultPriority
    ) {
        if (visual == null || visual.renderer == null) {
            return false;
        }
        if (visual.visible != null && !visual.visible.booleanValue()) {
            return false;
        }
        boolean visualForeground = visual.foreground == null || visual.foreground.booleanValue();
        if (visualForeground != foreground) {
            return false;
        }
        if (priorityFilter != null && resolvePriority(visual, defaultPriority) != priorityFilter.intValue()) {
            return false;
        }
        return pagePredicate == null || pagePredicate.isVisible(visual.page);
    }

    private int resolvePriority(MachineGuiStyleManager.DynamicVisualStyle visual, int defaultPriority) {
        return visual.priority == null ? defaultPriority : visual.priority.intValue();
    }

    private float resolveRawValue(
        @Nullable MachineGuiStyleManager.DynamicVisualSourceStyle source,
        @Nullable TileMultiblockMachineController controller,
        MetricProvider metricProvider
    ) {
        float fallback = source == null || source.defaultValue == null ? 0.0F : source.defaultValue.floatValue();
        if (source == null) {
            return fallback;
        }
        String type = source.type == null ? "machine" : source.type;
        if ("customData".equals(type)) {
            if (source.key == null || source.key.trim().isEmpty()) {
                return fallback;
            }
            Float value = ControllerCustomDataAccess.readNumber(controller, source.key.trim());
            return value == null || !Float.isFinite(value.floatValue()) ? fallback : value.floatValue();
        }
        String metric = source.metric == null || source.metric.trim().isEmpty() ? "recipeProgress" : source.metric.trim();
        return metricProvider == null ? fallback : metricProvider.getMachineMetric(metric, fallback);
    }

    private float normalizeValue(float raw, @Nullable MachineGuiStyleManager.DynamicVisualSourceStyle source) {
        float value = Float.isFinite(raw) ? raw : 0.0F;
        float min = source == null || source.min == null ? 0.0F : source.min.floatValue();
        float max = source == null || source.max == null ? 1.0F : source.max.floatValue();
        if (Math.abs(max - min) > EPSILON) {
            value = (value - min) / (max - min);
        }
        boolean clamp = source == null || source.clamp == null || source.clamp.booleanValue();
        if (clamp) {
            value = ProgressBarStyleSupport.clamp01(value);
        }
        if (source != null && Boolean.TRUE.equals(source.invert)) {
            value = 1.0F - value;
            if (clamp) {
                value = ProgressBarStyleSupport.clamp01(value);
            }
        }
        return value;
    }

    private void renderVisual(MachineGuiStyleManager.DynamicVisualStyle visual, float rawValue, float normalized, int originX, int originY) {
        MachineGuiStyleManager.DynamicVisualRendererStyle renderer = visual.renderer;
        String type = renderer.type == null ? "fill" : renderer.type;
        int x = originX + visual.x;
        int y = originY + visual.y;
        if ("textureSwitch".equals(type)) {
            drawTextureSwitch(visual, rawValue, x, y);
        } else if ("pie".equals(type)) {
            drawPie(renderer, normalized, x, y, visual.width, visual.height);
        } else if ("lineChart".equals(type)) {
            drawLineChart(visual, renderer, normalized, x, y, visual.width, visual.height);
        } else {
            drawFill(renderer, normalized, x, y, visual.width, visual.height);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
    }

    private void drawTextureSwitch(MachineGuiStyleManager.DynamicVisualStyle visual, float value, int x, int y) {
        MachineGuiStyleManager.DynamicVisualRendererStyle renderer = visual.renderer;
        String texture = null;
        if (renderer.frames != null) {
            for (MachineGuiStyleManager.DynamicVisualFrameStyle frame : renderer.frames) {
                if (frame == null || frame.texture == null || frame.texture.trim().isEmpty()) {
                    continue;
                }
                if (matchesFrame(frame, value)) {
                    texture = frame.texture;
                    break;
                }
            }
        }
        if (texture == null || texture.trim().isEmpty()) {
            texture = renderer.fallbackTexture;
        }
        ResourceLocation resource = GuiRenderUtils.parseOptionalTexture(texture);
        if (resource == null) {
            return;
        }
        int texW = renderer.textureWidth == null ? visual.width : Math.max(1, renderer.textureWidth.intValue());
        int texH = renderer.textureHeight == null ? visual.height : Math.max(1, renderer.textureHeight.intValue());
        Minecraft.getMinecraft().getTextureManager().bindTexture(resource);
        GuiRenderUtils.drawTexturedRect(x, y, 0, 0, visual.width, visual.height, texW, texH);
    }

    private boolean matchesFrame(MachineGuiStyleManager.DynamicVisualFrameStyle frame, float value) {
        if (frame.equals != null && Math.abs(value - frame.equals.floatValue()) > EPSILON) {
            return false;
        }
        if (frame.min != null && value < frame.min.floatValue()) {
            return false;
        }
        if (frame.max != null && value > frame.max.floatValue()) {
            return false;
        }
        return true;
    }

    private void drawFill(MachineGuiStyleManager.DynamicVisualRendererStyle renderer, float progress, int x, int y, int width, int height) {
        int textureWidth = renderer.textureWidth == null ? width : Math.max(1, renderer.textureWidth.intValue());
        int textureHeight = renderer.textureHeight == null ? height : Math.max(1, renderer.textureHeight.intValue());
        ResourceLocation background = GuiRenderUtils.parseOptionalTexture(renderer.backgroundTexture);
        if (background != null) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(background);
            GuiRenderUtils.drawTexturedRect(x, y, 0, 0, width, height, textureWidth, textureHeight);
        } else if (renderer.backgroundColor != null) {
            Gui.drawRect(x, y, x + width, y + height, renderer.backgroundColor.intValue());
        }
        if (renderer.borderColor != null) {
            drawBorder(x, y, width, height, renderer.borderColor.intValue());
        }
        int[] bounds = computeFillBounds(x, y, width, height, renderer.direction, progress);
        if (bounds[2] <= 0 || bounds[3] <= 0) {
            return;
        }
        ResourceLocation fill = GuiRenderUtils.parseOptionalTexture(renderer.fillTexture);
        if (fill != null) {
            int[] uv = computeFillTextureBounds(textureWidth, textureHeight, renderer.direction, progress);
            Minecraft.getMinecraft().getTextureManager().bindTexture(fill);
            GuiRenderUtils.drawScaledTexturedRect(bounds[0], bounds[1], bounds[2], bounds[3], uv[0], uv[1], uv[2], uv[3], textureWidth, textureHeight);
        } else {
            Gui.drawRect(bounds[0], bounds[1], bounds[0] + bounds[2], bounds[1] + bounds[3], renderer.fillColor == null ? 0xFF55CC66 : renderer.fillColor.intValue());
        }
    }

    private int[] computeFillBounds(int x, int y, int width, int height, @Nullable String direction, float progress) {
        float clamped = ProgressBarStyleSupport.clamp01(progress);
        int fillWidth = Math.max(0, Math.min(width, (int) Math.floor(width * clamped)));
        int fillHeight = Math.max(0, Math.min(height, (int) Math.floor(height * clamped)));
        if ("left".equals(direction)) {
            return new int[] {x + width - fillWidth, y, fillWidth, height};
        }
        if ("down".equals(direction)) {
            return new int[] {x, y, width, fillHeight};
        }
        if ("up".equals(direction)) {
            return new int[] {x, y + height - fillHeight, width, fillHeight};
        }
        return new int[] {x, y, fillWidth, height};
    }

    private int[] computeFillTextureBounds(int textureWidth, int textureHeight, @Nullable String direction, float progress) {
        int safeW = Math.max(1, textureWidth);
        int safeH = Math.max(1, textureHeight);
        float clamped = ProgressBarStyleSupport.clamp01(progress);
        int fillW = Math.max(0, Math.min(safeW, (int) Math.floor(safeW * clamped)));
        int fillH = Math.max(0, Math.min(safeH, (int) Math.floor(safeH * clamped)));
        if ("left".equals(direction)) {
            return new int[] {safeW - fillW, 0, fillW, safeH};
        }
        if ("down".equals(direction)) {
            return new int[] {0, 0, safeW, fillH};
        }
        if ("up".equals(direction)) {
            return new int[] {0, safeH - fillH, safeW, fillH};
        }
        return new int[] {0, 0, fillW, safeH};
    }

    private void drawPie(MachineGuiStyleManager.DynamicVisualRendererStyle renderer, float progress, int x, int y, int width, int height) {
        int bg = renderer.backgroundColor == null ? 0x33000000 : renderer.backgroundColor.intValue();
        int color = renderer.color == null ? 0xFFFFAA00 : renderer.color.intValue();
        int segments = renderer.segments == null ? 64 : MathHelper.clamp(renderer.segments.intValue(), 3, 360);
        float startAngle = renderer.startAngle == null ? -90.0F : renderer.startAngle.floatValue();
        boolean ring = "ring".equals(renderer.mode);
        float cx = x + width * 0.5F;
        float cy = y + height * 0.5F;
        float radius = Math.max(1.0F, Math.min(width, height) * 0.5F);
        float inner = ring ? MathHelper.clamp(renderer.innerRadius == null ? radius * 0.55F : renderer.innerRadius.floatValue(), 0.0F, radius - 1.0F) : 0.0F;
        drawArc(cx, cy, radius, inner, startAngle, 360.0F, segments, bg);
        float sweep = 360.0F * ProgressBarStyleSupport.clamp01(progress);
        if (sweep > 0.0F) {
            drawArc(cx, cy, radius, inner, startAngle, sweep, Math.max(1, (int) Math.ceil(segments * sweep / 360.0F)), color);
        }
    }

    private void drawArc(float cx, float cy, float radius, float innerRadius, float startAngle, float sweepAngle, int segments, int color) {
        if (sweepAngle <= 0.0F || radius <= 0.0F) {
            return;
        }
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GuiRenderUtils.applyColorARGB(color);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        if (innerRadius <= 0.0F) {
            buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
            buffer.pos(cx, cy, 0.0D).endVertex();
            for (int i = 0; i <= segments; i++) {
                double angle = Math.toRadians(startAngle + sweepAngle * i / (double) segments);
                buffer.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0.0D).endVertex();
            }
        } else {
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION);
            for (int i = 0; i <= segments; i++) {
                double angle = Math.toRadians(startAngle + sweepAngle * i / (double) segments);
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                buffer.pos(cx + cos * radius, cy + sin * radius, 0.0D).endVertex();
                buffer.pos(cx + cos * innerRadius, cy + sin * innerRadius, 0.0D).endVertex();
            }
        }
        tessellator.draw();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    private void drawLineChart(MachineGuiStyleManager.DynamicVisualStyle visual, MachineGuiStyleManager.DynamicVisualRendererStyle renderer, float current, int x, int y, int width, int height) {
        List<Float> values = getHistoryValues(visual, current);
        if (renderer.backgroundColor != null) {
            Gui.drawRect(x, y, x + width, y + height, renderer.backgroundColor.intValue());
        }
        if (renderer.showGrid == null || renderer.showGrid.booleanValue()) {
            int grid = renderer.gridColor == null ? 0x22000000 : renderer.gridColor.intValue();
            for (int i = 1; i < 4; i++) {
                int gx = x + width * i / 4;
                int gy = y + height * i / 4;
                Gui.drawRect(gx, y, gx + 1, y + height, grid);
                Gui.drawRect(x, gy, x + width, gy + 1, grid);
            }
        }
        if (values.isEmpty()) {
            return;
        }
        int fillColor = renderer.fillColor == null ? 0 : renderer.fillColor.intValue();
        int lineColor = renderer.lineColor == null ? 0xFF55CCFF : renderer.lineColor.intValue();
        int lineWidth = renderer.lineWidth == null ? 1 : Math.max(1, renderer.lineWidth.intValue());
        int lastX = x;
        int lastY = valueToY(values.get(0).floatValue(), y, height);
        if ((fillColor >>> 24) != 0) {
            for (int i = 0; i < values.size(); i++) {
                int px = x + Math.round((width - 1) * (values.size() == 1 ? 0.0F : (float) i / (float) (values.size() - 1)));
                int py = valueToY(values.get(i).floatValue(), y, height);
                Gui.drawRect(px, py, px + 1, y + height, fillColor);
            }
        }
        for (int i = 1; i < values.size(); i++) {
            int px = x + Math.round((width - 1) * (float) i / (float) (values.size() - 1));
            int py = valueToY(values.get(i).floatValue(), y, height);
            drawLine(lastX, lastY, px, py, lineWidth, lineColor);
            lastX = px;
            lastY = py;
        }
        if (values.size() == 1) {
            Gui.drawRect(x, lastY, x + width, lastY + lineWidth, lineColor);
        }
        if (renderer.borderColor != null) {
            drawBorder(x, y, width, height, renderer.borderColor.intValue());
        }
    }

    private int valueToY(float value, int y, int height) {
        return y + height - 1 - Math.round((height - 1) * ProgressBarStyleSupport.clamp01(value));
    }

    private void drawLine(int x0, int y0, int x1, int y1, int width, int color) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GuiRenderUtils.applyColorARGB(color);
        GL11.glLineWidth((float) Math.max(1, width));
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        buffer.pos(x0, y0, 0.0D).endVertex();
        buffer.pos(x1, y1, 0.0D).endVertex();
        tessellator.draw();
        GL11.glLineWidth(1.0F);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    private void drawBorder(int x, int y, int width, int height, int color) {
        Gui.drawRect(x, y, x + width, y + 1, color);
        Gui.drawRect(x, y + height - 1, x + width, y + height, color);
        Gui.drawRect(x, y, x + 1, y + height, color);
        Gui.drawRect(x + width - 1, y, x + width, y + height, color);
    }

    private void updateHistoryIfNeeded(MachineGuiStyleManager.DynamicVisualStyle visual, float normalized, @Nullable TileMultiblockMachineController controller) {
        if (!isHistoryEnabled(visual)) {
            return;
        }
        int samples = visual.history == null || visual.history.samples == null ? 60 : Math.max(2, visual.history.samples.intValue());
        int interval = visual.history == null || visual.history.intervalTicks == null ? 5 : Math.max(1, visual.history.intervalTicks.intValue());
        HistoryState state = histories.get(historyKey(visual));
        if (state == null || state.capacity != samples) {
            state = new HistoryState(samples);
            histories.put(historyKey(visual), state);
        }
        long tick = getWorldTime(controller);
        if (state.values.isEmpty() || tick - state.lastTick >= interval) {
            state.add(Float.valueOf(ProgressBarStyleSupport.clamp01(normalized)));
            state.lastTick = tick;
        }
    }

    private List<Float> getHistoryValues(MachineGuiStyleManager.DynamicVisualStyle visual, float current) {
        if (!isHistoryEnabled(visual)) {
            List<Float> singleton = new ArrayList<Float>(1);
            singleton.add(Float.valueOf(ProgressBarStyleSupport.clamp01(current)));
            return singleton;
        }
        HistoryState state = histories.get(historyKey(visual));
        if (state == null || state.values.isEmpty()) {
            List<Float> singleton = new ArrayList<Float>(1);
            singleton.add(Float.valueOf(ProgressBarStyleSupport.clamp01(current)));
            return singleton;
        }
        return new ArrayList<Float>(state.values);
    }

    private boolean isHistoryEnabled(MachineGuiStyleManager.DynamicVisualStyle visual) {
        return visual.history != null && (visual.history.enabled == null || visual.history.enabled.booleanValue());
    }

    private String historyKey(MachineGuiStyleManager.DynamicVisualStyle visual) {
        if (visual.id != null && !visual.id.trim().isEmpty()) {
            return visual.id.trim();
        }
        return visual.x + ":" + visual.y + ":" + visual.width + ":" + visual.height;
    }

    private long getWorldTime(@Nullable TileMultiblockMachineController controller) {
        if (controller != null && controller.getWorld() != null) {
            return controller.getWorld().getTotalWorldTime();
        }
        return Minecraft.getMinecraft().world == null ? 0L : Minecraft.getMinecraft().world.getTotalWorldTime();
    }

    public static float reflectMetric(Object target, float fallback, String... names) {
        if (target == null) {
            return fallback;
        }
        for (String name : names) {
            try {
                Method method = target.getClass().getMethod(name);
                Object value = method.invoke(target);
                if (value instanceof Number) {
                    return ((Number) value).floatValue();
                }
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    public interface PagePredicate {
        boolean isVisible(@Nullable String page);
    }

    private static final class HistoryState {
        private final int capacity;
        private long lastTick = Long.MIN_VALUE;
        private final List<Float> values = new ArrayList<Float>();

        private HistoryState(int capacity) {
            this.capacity = capacity;
        }

        private void add(Float value) {
            while (values.size() >= capacity) {
                values.remove(0);
            }
            values.add(value);
        }
    }
}
