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
            if (!resolveVisibilityByValue(visual.visibleByValue, normalized, controller, metricProvider)) {
                continue;
            }
            updateHistoryIfNeeded(visual, normalized, controller);
            MachineGuiStyleManager.DynamicVisualRendererStyle renderer = resolveRendererStyle(visual, normalized, controller, metricProvider);
            renderVisual(visual, renderer, raw, normalized, controller, metricProvider, originX, originY);
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

    private void renderVisual(
        MachineGuiStyleManager.DynamicVisualStyle visual,
        MachineGuiStyleManager.DynamicVisualRendererStyle renderer,
        float rawValue,
        float normalized,
        @Nullable TileMultiblockMachineController controller,
        MetricProvider metricProvider,
        int originX,
        int originY
    ) {
        String type = renderer.type == null ? "fill" : renderer.type;
        ResolvedTransform transform = resolveTransform(visual, normalized, controller, metricProvider);
        if (transform.alpha <= EPSILON) {
            return;
        }
        int x = originX + visual.x + Math.round(transform.offsetX);
        int y = originY + visual.y + Math.round(transform.offsetY);
        beginVisualRender(transform.alpha);
        try {
            if (transform.requiresMatrix()) {
                float pivotX = transform.resolvePivotX(visual.width);
                float pivotY = transform.resolvePivotY(visual.height);
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.translate(x + pivotX, y + pivotY, 0.0F);
                    applyTransformMatrix(transform);
                    drawResolvedVisual(
                        type,
                        visual,
                        renderer,
                        rawValue,
                        normalized,
                        -Math.round(pivotX),
                        -Math.round(pivotY),
                        visual.width,
                        visual.height,
                        transform.alpha
                    );
                } finally {
                    GlStateManager.popMatrix();
                }
            } else {
                drawResolvedVisual(type, visual, renderer, rawValue, normalized, x, y, visual.width, visual.height, transform.alpha);
            }
        } finally {
            endVisualRender(transform.alpha);
        }
    }

    private boolean resolveVisibilityByValue(
        @Nullable MachineGuiStyleManager.DynamicVisualVisibilityByValueStyle visibility,
        float fallbackNormalized,
        @Nullable TileMultiblockMachineController controller,
        MetricProvider metricProvider
    ) {
        if (visibility == null) {
            return true;
        }
        float value = resolveNormalizedInput(visibility.source, fallbackNormalized, controller, metricProvider);
        boolean visible = true;
        if (visibility.equals != null) {
            visible = Math.abs(value - visibility.equals.floatValue()) <= EPSILON;
        }
        if (visibility.min != null && value < visibility.min.floatValue()) {
            visible = false;
        }
        if (visibility.max != null && value > visibility.max.floatValue()) {
            visible = false;
        }
        if (Boolean.TRUE.equals(visibility.invert)) {
            visible = !visible;
        }
        return visible;
    }

    private MachineGuiStyleManager.DynamicVisualRendererStyle resolveRendererStyle(
        MachineGuiStyleManager.DynamicVisualStyle visual,
        float fallbackNormalized,
        @Nullable TileMultiblockMachineController controller,
        MetricProvider metricProvider
    ) {
        MachineGuiStyleManager.DynamicVisualRendererStyle renderer = MachineGuiStyleManager.DynamicVisualRendererStyle.copyOf(visual.renderer);
        if (renderer == null) {
            renderer = new MachineGuiStyleManager.DynamicVisualRendererStyle();
        }
        MachineGuiStyleManager.DynamicVisualRendererByValueStyle dynamic = visual.rendererByValue;
        if (dynamic == null) {
            return renderer;
        }
        renderer.backgroundColor = resolveDrivenColor(dynamic.backgroundColor, renderer.backgroundColor, fallbackNormalized, controller, metricProvider);
        renderer.fillColor = resolveDrivenColor(dynamic.fillColor, renderer.fillColor, fallbackNormalized, controller, metricProvider);
        renderer.borderColor = resolveDrivenColor(dynamic.borderColor, renderer.borderColor, fallbackNormalized, controller, metricProvider);
        renderer.color = resolveDrivenColor(dynamic.color, renderer.color, fallbackNormalized, controller, metricProvider);
        renderer.lineColor = resolveDrivenColor(dynamic.lineColor, renderer.lineColor, fallbackNormalized, controller, metricProvider);
        renderer.gridColor = resolveDrivenColor(dynamic.gridColor, renderer.gridColor, fallbackNormalized, controller, metricProvider);
        return renderer;
    }

    private void drawResolvedVisual(
        String type,
        MachineGuiStyleManager.DynamicVisualStyle visual,
        MachineGuiStyleManager.DynamicVisualRendererStyle renderer,
        float rawValue,
        float normalized,
        int x,
        int y,
        int width,
        int height,
        float alpha
    ) {
        if ("textureSwitch".equals(type)) {
            drawTextureSwitch(visual, rawValue, x, y, width, height);
        } else if ("pie".equals(type)) {
            drawPie(renderer, normalized, x, y, width, height, alpha);
        } else if ("lineChart".equals(type)) {
            drawLineChart(visual, renderer, normalized, x, y, width, height, alpha);
        } else {
            drawFill(renderer, normalized, x, y, width, height, alpha);
        }
    }

    private ResolvedTransform resolveTransform(
        MachineGuiStyleManager.DynamicVisualStyle visual,
        float normalized,
        @Nullable TileMultiblockMachineController controller,
        MetricProvider metricProvider
    ) {
        ResolvedTransform transform = new ResolvedTransform();
        MachineGuiStyleManager.DynamicVisualTransformStyle base = visual.transform;
        if (base != null) {
            if (base.offsetX != null) {
                transform.offsetX = base.offsetX.floatValue();
            }
            if (base.offsetY != null) {
                transform.offsetY = base.offsetY.floatValue();
            }
            if (base.scale != null) {
                float scale = normalizeRuntimeScale(base.scale.floatValue());
                transform.scaleX = scale;
                transform.scaleY = scale;
            }
            if (base.scaleX != null) {
                transform.scaleX = normalizeRuntimeScale(base.scaleX.floatValue());
            }
            if (base.scaleY != null) {
                transform.scaleY = normalizeRuntimeScale(base.scaleY.floatValue());
            }
            if (base.rotation != null) {
                transform.rotation = base.rotation.floatValue();
            }
            if (base.alpha != null) {
                transform.alpha = normalizeRuntimeAlpha(base.alpha.floatValue());
            }
            if (base.origin != null && !base.origin.trim().isEmpty()) {
                transform.origin = base.origin;
            }
            if (base.pivotX != null) {
                transform.pivotX = base.pivotX.floatValue();
            }
            if (base.pivotY != null) {
                transform.pivotY = base.pivotY.floatValue();
            }
            if (base.pivotUnit != null && !base.pivotUnit.trim().isEmpty()) {
                transform.pivotUnit = base.pivotUnit;
            }
        }

        MachineGuiStyleManager.DynamicVisualTransformByValueStyle dynamic = visual.transformByValue;
        if (dynamic != null) {
            if (dynamic.offsetX != null) {
                transform.offsetX += resolveDrivenValue(dynamic.offsetX, normalized, controller, metricProvider);
            }
            if (dynamic.offsetY != null) {
                transform.offsetY += resolveDrivenValue(dynamic.offsetY, normalized, controller, metricProvider);
            }
            if (dynamic.scale != null) {
                float scale = normalizeRuntimeScale(resolveDrivenValue(dynamic.scale, normalized, controller, metricProvider));
                transform.scaleX *= scale;
                transform.scaleY *= scale;
            }
            if (dynamic.scaleX != null) {
                transform.scaleX *= normalizeRuntimeScale(resolveDrivenValue(dynamic.scaleX, normalized, controller, metricProvider));
            }
            if (dynamic.scaleY != null) {
                transform.scaleY *= normalizeRuntimeScale(resolveDrivenValue(dynamic.scaleY, normalized, controller, metricProvider));
            }
            if (dynamic.rotation != null) {
                transform.rotation += resolveDrivenValue(dynamic.rotation, normalized, controller, metricProvider);
            }
            if (dynamic.alpha != null) {
                transform.alpha *= normalizeRuntimeAlpha(resolveDrivenValue(dynamic.alpha, normalized, controller, metricProvider));
            }
            if (dynamic.pivotX != null) {
                transform.pivotX = Float.valueOf(resolveDrivenValue(dynamic.pivotX, normalized, controller, metricProvider));
            }
            if (dynamic.pivotY != null) {
                transform.pivotY = Float.valueOf(resolveDrivenValue(dynamic.pivotY, normalized, controller, metricProvider));
            }
        }

        transform.scaleX = normalizeRuntimeScale(transform.scaleX);
        transform.scaleY = normalizeRuntimeScale(transform.scaleY);
        transform.alpha = normalizeRuntimeAlpha(transform.alpha);
        return transform;
    }

    private float resolveDrivenValue(
        MachineGuiStyleManager.DynamicVisualDrivenValueStyle driven,
        float fallbackNormalized,
        @Nullable TileMultiblockMachineController controller,
        MetricProvider metricProvider
    ) {
        float input = resolveNormalizedInput(driven.source, fallbackNormalized, controller, metricProvider);
        float min = driven.min == null ? 0.0F : driven.min.floatValue();
        float max = driven.max == null ? 1.0F : driven.max.floatValue();
        if (!Float.isFinite(input)) {
            input = 0.0F;
        }
        if (Math.abs(max - min) <= EPSILON) {
            return min;
        }
        return min + (max - min) * input;
    }

    private float resolveNormalizedInput(
        @Nullable MachineGuiStyleManager.DynamicVisualSourceStyle source,
        float fallbackNormalized,
        @Nullable TileMultiblockMachineController controller,
        MetricProvider metricProvider
    ) {
        float input = fallbackNormalized;
        if (source != null) {
            float raw = resolveRawValue(source, controller, metricProvider);
            input = normalizeValue(raw, source);
        }
        return Float.isFinite(input) ? input : 0.0F;
    }

    @Nullable
    private Integer resolveDrivenColor(
        @Nullable MachineGuiStyleManager.DynamicVisualDrivenColorStyle driven,
        @Nullable Integer baseColor,
        float fallbackNormalized,
        @Nullable TileMultiblockMachineController controller,
        MetricProvider metricProvider
    ) {
        if (driven == null) {
            return baseColor;
        }
        float input = ProgressBarStyleSupport.clamp01(resolveNormalizedInput(driven.source, fallbackNormalized, controller, metricProvider));
        Integer fromColor = driven.fromColor != null ? driven.fromColor : (baseColor != null ? baseColor : driven.toColor);
        Integer toColor = driven.toColor != null ? driven.toColor : (baseColor != null ? baseColor : driven.fromColor);
        if (fromColor == null && toColor == null) {
            return baseColor;
        }
        if (fromColor == null) {
            fromColor = toColor;
        }
        if (toColor == null) {
            toColor = fromColor;
        }
        return Integer.valueOf(interpolateColor(fromColor.intValue(), toColor.intValue(), input));
    }

    private int interpolateColor(int fromColor, int toColor, float progress) {
        float clamped = ProgressBarStyleSupport.clamp01(progress);
        int fromA = (fromColor >>> 24) & 0xFF;
        int fromR = (fromColor >>> 16) & 0xFF;
        int fromG = (fromColor >>> 8) & 0xFF;
        int fromB = fromColor & 0xFF;
        int toA = (toColor >>> 24) & 0xFF;
        int toR = (toColor >>> 16) & 0xFF;
        int toG = (toColor >>> 8) & 0xFF;
        int toB = toColor & 0xFF;
        int outA = MathHelper.clamp(Math.round(fromA + (toA - fromA) * clamped), 0, 255);
        int outR = MathHelper.clamp(Math.round(fromR + (toR - fromR) * clamped), 0, 255);
        int outG = MathHelper.clamp(Math.round(fromG + (toG - fromG) * clamped), 0, 255);
        int outB = MathHelper.clamp(Math.round(fromB + (toB - fromB) * clamped), 0, 255);
        return (outA << 24) | (outR << 16) | (outG << 8) | outB;
    }

    private void applyTransformMatrix(ResolvedTransform transform) {
        if (Math.abs(transform.rotation) > EPSILON) {
            GlStateManager.rotate(transform.rotation, 0.0F, 0.0F, 1.0F);
        }
        if (Math.abs(transform.scaleX - 1.0F) > EPSILON || Math.abs(transform.scaleY - 1.0F) > EPSILON) {
            GlStateManager.scale(transform.scaleX, transform.scaleY, 1.0F);
        }
    }

    private void beginVisualRender(float alpha) {
        if (alpha < 1.0F - EPSILON) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
            );
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, normalizeRuntimeAlpha(alpha));
    }

    private void endVisualRender(float alpha) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        if (alpha < 1.0F - EPSILON) {
            GlStateManager.disableBlend();
        }
    }

    private void drawTextureSwitch(MachineGuiStyleManager.DynamicVisualStyle visual, float value, int x, int y, int width, int height) {
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
        GuiRenderUtils.drawTexturedRect(x, y, 0, 0, width, height, texW, texH);
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

    private void drawFill(MachineGuiStyleManager.DynamicVisualRendererStyle renderer, float progress, int x, int y, int width, int height, float alpha) {
        int textureWidth = renderer.textureWidth == null ? width : Math.max(1, renderer.textureWidth.intValue());
        int textureHeight = renderer.textureHeight == null ? height : Math.max(1, renderer.textureHeight.intValue());
        ResourceLocation background = GuiRenderUtils.parseOptionalTexture(renderer.backgroundTexture);
        if (background != null) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(background);
            GuiRenderUtils.drawTexturedRect(x, y, 0, 0, width, height, textureWidth, textureHeight);
        } else if (renderer.backgroundColor != null) {
            Gui.drawRect(x, y, x + width, y + height, multiplyColorAlpha(renderer.backgroundColor.intValue(), alpha));
        }
        if (renderer.borderColor != null) {
            drawBorder(x, y, width, height, multiplyColorAlpha(renderer.borderColor.intValue(), alpha));
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
            int fillColor = renderer.fillColor == null ? 0xFF55CC66 : renderer.fillColor.intValue();
            Gui.drawRect(bounds[0], bounds[1], bounds[0] + bounds[2], bounds[1] + bounds[3], multiplyColorAlpha(fillColor, alpha));
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

    private void drawPie(MachineGuiStyleManager.DynamicVisualRendererStyle renderer, float progress, int x, int y, int width, int height, float alpha) {
        int bg = renderer.backgroundColor == null ? 0x33000000 : multiplyColorAlpha(renderer.backgroundColor.intValue(), alpha);
        int color = renderer.color == null ? multiplyColorAlpha(0xFFFFAA00, alpha) : multiplyColorAlpha(renderer.color.intValue(), alpha);
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

    private void drawLineChart(MachineGuiStyleManager.DynamicVisualStyle visual, MachineGuiStyleManager.DynamicVisualRendererStyle renderer, float current, int x, int y, int width, int height, float alpha) {
        List<Float> values = getHistoryValues(visual, current);
        if (renderer.backgroundColor != null) {
            Gui.drawRect(x, y, x + width, y + height, multiplyColorAlpha(renderer.backgroundColor.intValue(), alpha));
        }
        if (renderer.showGrid == null || renderer.showGrid.booleanValue()) {
            int grid = renderer.gridColor == null ? multiplyColorAlpha(0x22000000, alpha) : multiplyColorAlpha(renderer.gridColor.intValue(), alpha);
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
        int fillColor = renderer.fillColor == null ? 0 : multiplyColorAlpha(renderer.fillColor.intValue(), alpha);
        int lineColor = renderer.lineColor == null ? multiplyColorAlpha(0xFF55CCFF, alpha) : multiplyColorAlpha(renderer.lineColor.intValue(), alpha);
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
            drawBorder(x, y, width, height, multiplyColorAlpha(renderer.borderColor.intValue(), alpha));
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

    private int multiplyColorAlpha(int color, float alpha) {
        float normalized = normalizeRuntimeAlpha(alpha);
        if (normalized >= 1.0F - EPSILON) {
            return color;
        }
        int baseAlpha = (color >>> 24) & 0xFF;
        int outAlpha = MathHelper.clamp(Math.round(baseAlpha * normalized), 0, 255);
        return (color & 0x00FFFFFF) | (outAlpha << 24);
    }

    private float normalizeRuntimeScale(float value) {
        if (!Float.isFinite(value)) {
            return 1.0F;
        }
        return Math.max(0.01F, value);
    }

    private float normalizeRuntimeAlpha(float value) {
        if (!Float.isFinite(value)) {
            return 1.0F;
        }
        float alpha = value;
        if (alpha > 1.0F) {
            alpha = alpha / 255.0F;
        }
        return ProgressBarStyleSupport.clamp01(alpha);
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

    private static final class ResolvedTransform {
        private float offsetX = 0.0F;
        private float offsetY = 0.0F;
        private float scaleX = 1.0F;
        private float scaleY = 1.0F;
        private float rotation = 0.0F;
        private float alpha = 1.0F;
        private String origin = "topLeft";
        @Nullable
        private Float pivotX;
        @Nullable
        private Float pivotY;
        private String pivotUnit = "ratio";

        private float resolvePivotX(int width) {
            if (this.pivotX != null) {
                return resolveExplicitPivot(this.pivotX.floatValue(), width);
            }
            if ("center".equals(this.origin)
                || "topCenter".equals(this.origin)
                || "bottomCenter".equals(this.origin)) {
                return width * 0.5F;
            }
            if ("topRight".equals(this.origin)
                || "centerRight".equals(this.origin)
                || "bottomRight".equals(this.origin)) {
                return (float) width;
            }
            return 0.0F;
        }

        private float resolvePivotY(int height) {
            if (this.pivotY != null) {
                return resolveExplicitPivot(this.pivotY.floatValue(), height);
            }
            if ("center".equals(this.origin)
                || "centerLeft".equals(this.origin)
                || "centerRight".equals(this.origin)) {
                return height * 0.5F;
            }
            if ("bottomLeft".equals(this.origin)
                || "bottomCenter".equals(this.origin)
                || "bottomRight".equals(this.origin)) {
                return (float) height;
            }
            return 0.0F;
        }

        private float resolveExplicitPivot(float value, int size) {
            if ("px".equals(this.pivotUnit)) {
                return value;
            }
            return value * size;
        }

        private boolean requiresMatrix() {
            return Math.abs(this.scaleX - 1.0F) > EPSILON
                || Math.abs(this.scaleY - 1.0F) > EPSILON
                || Math.abs(this.rotation) > EPSILON;
        }
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
