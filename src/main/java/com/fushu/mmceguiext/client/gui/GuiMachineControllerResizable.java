package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import com.fushu.mmceguiext.client.config.ProgressBarStyleSupport;
import com.fushu.mmceguiext.common.network.PktControllerButtonAction;
import com.fushu.mmceguiext.common.network.PktControllerSmartInterfaceUpdate;
import com.fushu.mmceguiext.common.util.ControllerCustomDataAccess;
import github.kasuminova.mmce.common.event.client.ControllerGUIRenderEvent;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.gui.GuiContainerBase;
import hellfirepvp.modularmachinery.common.container.ContainerController;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceType;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.IllegalFormatException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class GuiMachineControllerResizable extends GuiContainerBase<ContainerController> {
    private static final int BASE_WIDTH = 176;
    private static final int BASE_HEIGHT = 213;
    private static final int PLAYER_INVENTORY_LEFT = 8;
    private static final int PLAYER_INVENTORY_TOP = 131;
    private static final int PLAYER_HOTBAR_TOP = 189;
    private static final int PLAYER_INVENTORY_WIDTH = 162;
    private static final int PLAYER_INVENTORY_HEIGHT = 76;
    private static final int HIDDEN_PLAYER_INVENTORY_TOP = 9999;
    private static final int PLAYER_INVENTORY_MASK_COLOR = 0xFF555555;
    private static final double FONT_SCALE = 0.72D;

    private static final ResourceLocation DEFAULT_BACKGROUND = new ResourceLocation(
        ModularMachinery.MODID,
        "textures/gui/guicontroller_large.png"
    );
    private static final String DEFAULT_BACKGROUND_STR = "modularmachinery:textures/gui/guicontroller_large.png";
    private static final int SMART_EDITOR_BUTTON_W = 10;
    private static final int SMART_EDITOR_BUTTON_H = 12;
    private static final int SMART_EDITOR_APPLY_W = 20;
    private static final int SMART_EDITOR_INPUT_H = 12;
    private static final int CUSTOM_EDITOR_BUTTON_ID_BASE = 4000;
    private static final int CUSTOM_BUTTON_ID_BASE = 5000;
    private static final int DEFAULT_RENDER_PRIORITY = 0;
    private static final int DEFAULT_SMART_EDITOR_PRIORITY = 10;
    private static final int DEFAULT_BUTTON_WIDTH = 20;
    private static final int DEFAULT_BUTTON_HEIGHT = 20;
    private static final int DEFAULT_SLIDER_THUMB_SIZE = 8;

    private final TileMachineController controller;
    private MachineGuiStyleManager.ControllerStyle styleOverride = MachineGuiStyleManager.ControllerStyle.EMPTY;
    @Nullable
    private ResourceLocation customBackgroundTexture = null;

    private int renderWidth = BASE_WIDTH;
    private int renderHeight = BASE_HEIGHT;

    // Scheme B: opt-in logical coordinate space + uniform GL scale.
    // When coordinateWidth/Height are set, the whole GUI is laid out in a fixed
    // logical canvas (guiLeft/guiTop forced to 0) and rendered through an outer
    // translate(renderOrigin)+scale(renderScale) wrapper, so layout never depends
    // on the screen-fit clamp. Mouse input is converted back to logical space.
    private boolean guiScaleMode = false;
    private int logicalWidth = BASE_WIDTH;
    private int logicalHeight = BASE_HEIGHT;
    private float renderScale = 1.0F;
    private int renderOriginX = 0;
    private int renderOriginY = 0;
    private boolean suppressDefaultBackground = false;
    private boolean deferTooltip = false;

    private final Map<String, Integer> panelScroll = new HashMap<String, Integer>();
    private final Map<String, Integer> panelMaxScroll = new HashMap<String, Integer>();
    @Nullable
    private String draggingPanelId = null;
    private int infoScrollbarDragOffset = 0;
    @Nullable
    private GuiTextField smartInterfaceEditorInput = null;
    @Nullable
    private GuiButton smartInterfacePrevButton = null;
    @Nullable
    private GuiButton smartInterfaceNextButton = null;
    @Nullable
    private GuiButton smartInterfaceApplyButton = null;
    private int smartInterfaceIndex = 0;
    private int smartInterfaceEditorX = 0;
    private int smartInterfaceEditorY = 0;
    private int smartInterfaceEditorPriority = DEFAULT_SMART_EDITOR_PRIORITY;
    private final Map<String, String> smartInterfaceVirtualInputCache = new HashMap<String, String>();
    @Nullable
    private String smartInterfaceActiveVirtualKey = null;
    private boolean smartInterfaceHideInfoText = false;
    private boolean smartInterfaceHideTitleText = false;
    @Nullable
    private String smartInterfaceCustomTitleText = null;
    private boolean hideDefaultSmartInterfaceEditor = false;
    private final List<CustomSmartEditor> customSmartEditors = new ArrayList<CustomSmartEditor>();
    private final List<CustomButton> customButtons = new ArrayList<CustomButton>();
    private final List<CustomSlider> customSliders = new ArrayList<CustomSlider>();
    private final List<TextureLayerDef> backgroundTextureLayers = new ArrayList<TextureLayerDef>();
    private final List<TextureLayerDef> foregroundTextureLayers = new ArrayList<TextureLayerDef>();
    private final Map<String, LayerRuntimeState> layerRuntimeStates = new HashMap<String, LayerRuntimeState>();
    private final Set<String> textureLayerIds = new HashSet<String>();
    private String activePageId = "main";
    private MachineGuiStyleManager.ControllerStyle baseStyleOverride = MachineGuiStyleManager.ControllerStyle.EMPTY;
    @Nullable
    private RuntimeGuiState currentRuntimeState = null;
    private final List<RuntimeGuiState> replaceSubGuiStack = new ArrayList<RuntimeGuiState>();
    private final List<RuntimeGuiState> modalSubGuiStack = new ArrayList<RuntimeGuiState>();
    private boolean subGuiContextSwitching = false;
    @Nullable
    private Boolean modalSubGuiDraggable = null;
    @Nullable
    private Boolean modalSubGuiDragHandle = null;
    private boolean modalSubGuiDragging = false;
    private int modalSubGuiDragOffsetX = 0;
    private int modalSubGuiDragOffsetY = 0;
    private int modalSubGuiDragX = 0;
    private int modalSubGuiDragY = 0;
    private int modalSubGuiDragWidth = 0;
    private int modalSubGuiDragHeight = 0;
    @Nullable
    private CustomSlider draggingSlider = null;

    public GuiMachineControllerResizable(ContainerController container) {
        super(container);
        this.controller = container.getOwner();
    }

    @Override
    public void initGui() {
        this.baseStyleOverride = MachineGuiStyleManager.resolveMachineController(resolveMachine());
        this.replaceSubGuiStack.clear();
        this.modalSubGuiStack.clear();
        this.styleOverride = this.baseStyleOverride;
        this.customBackgroundTexture = resolveCustomTexture();
        resolveGuiScaleConfig();
        super.initGui();
        initializeCurrentContext(null, false, null, this.activePageId);
        this.currentRuntimeState = captureRuntimeState(null, false);
        this.modalSubGuiDragging = false;
        this.modalSubGuiDragOffsetX = 0;
        this.modalSubGuiDragOffsetY = 0;
    }

    @Override
    protected void setWidthHeight() {
        MMCEGuiExtConfig.MachineController cfg = MMCEGuiExtConfig.machineController;
        if (this.guiScaleMode) {
            this.renderWidth = this.logicalWidth;
            this.renderHeight = this.logicalHeight;
            this.xSize = BASE_WIDTH;
            this.ySize = BASE_HEIGHT;
            return;
        }
        int baseLeft = (this.width - BASE_WIDTH) / 2;
        int baseTop = (this.height - BASE_HEIGHT) / 2;
        int maxWidth = Math.max(BASE_WIDTH, this.width - baseLeft - 8);
        int maxHeight = Math.max(BASE_HEIGHT, this.height - baseTop - 8);
        int requestedWidth = getRequestedGuiWidth(cfg);
        int requestedHeight = getRequestedGuiHeight(cfg);
        int targetWidth = getDisableRightExtension() ? BASE_WIDTH : requestedWidth;
        this.renderWidth = MathHelper.clamp(targetWidth, BASE_WIDTH, maxWidth);
        this.renderHeight = MathHelper.clamp(requestedHeight, BASE_HEIGHT, maxHeight);

        this.xSize = BASE_WIDTH;
        this.ySize = BASE_HEIGHT;
    }

    private void centerFullGuiIfNeeded(MMCEGuiExtConfig.MachineController cfg) {
        if (!getCenterFullGui()) {
            return;
        }
        double[] bounds = new double[] {
            this.customBackgroundTexture == null ? 0.0D : -getBackgroundTextureOffsetX(cfg),
            this.renderWidth
        };
        // Only background layers share the guiLeft-relative coordinate space used here.
        // Foreground layers are drawn at absolute screen X (see drawConfiguredTextureLayers),
        // so including them would corrupt the centering origin.
        includeTextureLayerBounds(this.backgroundTextureLayers, bounds);
        double center = (bounds[0] + bounds[1]) * 0.5D;
        this.guiLeft = (int) Math.round(this.width * 0.5D - center);
    }

    private void includeTextureLayerBounds(List<TextureLayerDef> layers, double[] bounds) {
        for (TextureLayerDef layer : layers) {
            if (!isLayerVisible(layer) || !isPageVisible(layer.page)) {
                continue;
            }
            int offX = resolveLayerOffsetX(layer);
            int width = layer.width == null ? this.renderWidth : Math.max(1, layer.width.intValue());
            int height = layer.height == null ? this.renderHeight : Math.max(1, layer.height.intValue());
            float scaleX = Math.max(0.01F, resolveLayerScaleX(layer));
            float scaleY = Math.max(0.01F, resolveLayerScaleY(layer));
            double rotation = Math.toRadians(resolveLayerRotation(layer));
            double scaledWidth = width * (double) scaleX;
            double scaledHeight = height * (double) scaleY;
            double boundWidth = Math.abs(Math.cos(rotation)) * scaledWidth + Math.abs(Math.sin(rotation)) * scaledHeight;
            double layerCenterX = offX + width * 0.5D;
            bounds[0] = Math.min(bounds[0], layerCenterX - boundWidth * 0.5D);
            bounds[1] = Math.max(bounds[1], layerCenterX + boundWidth * 0.5D);
        }
    }

    // ===== Scheme B: logical coordinate space + uniform scale =====

    private void resolveGuiScaleConfig() {
        this.guiScaleMode = false;
        this.renderScale = 1.0F;
        this.renderOriginX = 0;
        this.renderOriginY = 0;
        if (styleOverride.coordinateWidth != null && styleOverride.coordinateHeight != null) {
            this.logicalWidth = Math.max(BASE_WIDTH, styleOverride.coordinateWidth.intValue());
            this.logicalHeight = Math.max(BASE_HEIGHT, styleOverride.coordinateHeight.intValue());
            this.guiScaleMode = true;
        }
    }

    private void computeGuiScale() {
        int margin = 4;
        float availW = Math.max(1.0F, this.width - margin * 2);
        float availH = Math.max(1.0F, this.height - margin * 2);
        float s = Math.min(availW / this.logicalWidth, availH / this.logicalHeight);
        if (s > 1.0F || s <= 0.0F) {
            s = 1.0F;
        }
        this.renderScale = s;
        this.renderOriginX = Math.round((this.width - this.logicalWidth * s) * 0.5F);
        this.renderOriginY = Math.round((this.height - this.logicalHeight * s) * 0.5F);
    }

    private int toLogicalMouseX(int mouseX) {
        if (!this.guiScaleMode) {
            return mouseX;
        }
        return Math.round((mouseX - this.renderOriginX) / this.renderScale);
    }

    private int toLogicalMouseY(int mouseY) {
        if (!this.guiScaleMode) {
            return mouseY;
        }
        return Math.round((mouseY - this.renderOriginY) / this.renderScale);
    }

    // Logical GUI coords -> on-screen (this.width-space) coords, for GL scissor under the scale wrapper.
    private int scissorScreenX(int logicalX) {
        return this.guiScaleMode ? Math.round(this.renderOriginX + logicalX * this.renderScale) : logicalX;
    }

    private int scissorScreenY(int logicalY) {
        return this.guiScaleMode ? Math.round(this.renderOriginY + logicalY * this.renderScale) : logicalY;
    }

    private int scissorLen(int logicalLen) {
        return this.guiScaleMode ? Math.max(0, Math.round(logicalLen * this.renderScale)) : logicalLen;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!this.guiScaleMode) {
            super.drawScreen(mouseX, mouseY, partialTicks);
            drawActiveModalSubGui(mouseX, mouseY, partialTicks);
            return;
        }
        int logicalMouseX = toLogicalMouseX(mouseX);
        int logicalMouseY = toLogicalMouseY(mouseY);
        this.drawDefaultBackground();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) this.renderOriginX, (float) this.renderOriginY, 0.0F);
        GlStateManager.scale(this.renderScale, this.renderScale, 1.0F);
        this.suppressDefaultBackground = true;
        this.deferTooltip = true;
        super.drawScreen(logicalMouseX, logicalMouseY, partialTicks);
        this.suppressDefaultBackground = false;
        this.deferTooltip = false;
        GlStateManager.popMatrix();
        super.renderHoveredToolTip(mouseX, mouseY);
        drawActiveModalSubGui(mouseX, mouseY, partialTicks);
    }

    @Override
    public void drawDefaultBackground() {
        if (this.suppressDefaultBackground) {
            return;
        }
        super.drawDefaultBackground();
    }

    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        if (this.deferTooltip) {
            return;
        }
        super.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        MMCEGuiExtConfig.MachineController cfg = MMCEGuiExtConfig.machineController;
        if (isUsingDefaultBackground(cfg)) {
            this.mc.getTextureManager().bindTexture(DEFAULT_BACKGROUND);
            Gui.drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, BASE_WIDTH, BASE_HEIGHT, BASE_WIDTH, BASE_HEIGHT);
            if (getHidePlayerInventory(cfg)) {
                maskPlayerInventoryArea();
            }
            drawConfiguredTextureLayers(false, cfg);
            drawBackgroundProgressBars();
            drawBackgroundSliders();
            drawNegativeForegroundTextureLayers(cfg);
            return;
        }

        boolean useNineSlice = getUseNineSlice(cfg);
        boolean hideDefaultBackground = getHideDefaultBackground(cfg);
        int texW = getBackgroundTextureWidth(cfg);
        int texH = getBackgroundTextureHeight(cfg);
        int textureOffsetX = getBackgroundTextureOffsetX(cfg);
        int textureOffsetY = getBackgroundTextureOffsetY(cfg);

        if (this.customBackgroundTexture != null) {
            this.mc.getTextureManager().bindTexture(this.customBackgroundTexture);
            if (this.guiScaleMode) {
                // Scale mode: the background fills the logical canvas 1:1; screen-fit scaling
                // is handled by the outer GL transform, so neither the offset nor a
                // screen-clamped width applies here (avoids texture repeat/clip on resize).
                int sTexW = styleOverride.backgroundTextureWidth != null
                    ? Math.max(16, styleOverride.backgroundTextureWidth.intValue()) : this.renderWidth;
                int sTexH = styleOverride.backgroundTextureHeight != null
                    ? Math.max(16, styleOverride.backgroundTextureHeight.intValue()) : this.renderHeight;
                drawResizableArea(this.guiLeft, this.guiTop, this.renderWidth, this.renderHeight,
                    useNineSlice, sTexW, sTexH, getBackgroundCorner(cfg));
            } else {
                int drawWidth = Math.max(1, this.renderWidth + textureOffsetX);
                int drawHeight = Math.max(1, this.renderHeight + textureOffsetY);
                drawResizableArea(
                    this.guiLeft - textureOffsetX,
                    this.guiTop - textureOffsetY,
                    drawWidth,
                    drawHeight,
                    useNineSlice,
                    texW,
                    texH,
                    getBackgroundCorner(cfg)
                );
            }
        } else {
            if (!hideDefaultBackground) {
                this.mc.getTextureManager().bindTexture(DEFAULT_BACKGROUND);
                Gui.drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, BASE_WIDTH, BASE_HEIGHT, BASE_WIDTH, BASE_HEIGHT);
            }
            if (this.renderWidth > BASE_WIDTH || this.renderHeight > BASE_HEIGHT) {
                drawExtensionFallback();
            }
        }

        if (getHidePlayerInventory(cfg) && this.customBackgroundTexture == null) {
            maskPlayerInventoryArea();
        }

        // Custom panel backgrounds are intentionally not rendered.
        // Users should draw panel areas directly in their custom GUI textures.
        drawConfiguredTextureLayers(false, cfg);
        drawBackgroundProgressBars();
        drawBackgroundSliders();
        drawNegativeForegroundTextureLayers(cfg);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        resetSmartInterfaceEditorHints();
        MMCEGuiExtConfig.MachineController cfg = MMCEGuiExtConfig.machineController;
        int contentPriority = resolveForegroundContentPriority();
        int mouseScreenX = mouseX + this.guiLeft;
        int mouseScreenY = mouseY + this.guiTop;
        TreeSet<Integer> priorities = collectForegroundRenderPriorities();
        for (Integer priorityValue : priorities) {
            int priority = priorityValue.intValue();
            if (priority == contentPriority) {
                if (isUsingDefaultBackground(cfg)) {
                    drawDefaultForeground();
                } else {
                    List<PanelDef> panels = getActivePanels(cfg);
                    cleanupPanelState(panels);

                    Map<String, List<String>> linesByPanel = buildPanelInfoLines(panels, cfg);
                    int lineHeight = Math.max(1, MathHelper.ceil((float) ((this.fontRenderer.FONT_HEIGHT + 2) * FONT_SCALE)));

                    for (PanelDef panel : panels) {
                        List<String> lines = linesByPanel.get(panel.id);
                        if (lines == null) {
                            lines = new ArrayList<String>();
                        }

                        int contentHeight = lines.size() * lineHeight;
                        int viewportHeight = Math.max(1, panel.rect.height - 4);
                        int maxScroll = Math.max(0, contentHeight - viewportHeight);
                        panelMaxScroll.put(panel.id, maxScroll);

                        int scroll = MathHelper.clamp(getPanelScroll(panel.id), 0, maxScroll);
                        panelScroll.put(panel.id, scroll);

                        int textX = MathHelper.floor((float) ((panel.rect.x + 3) / FONT_SCALE));
                        int textY = MathHelper.floor((float) ((panel.rect.y + 2 - scroll) / FONT_SCALE));
                        int clipX = this.guiLeft + panel.rect.x + 1;
                        int clipY = this.guiTop + panel.rect.y + 1;
                        int clipWidth = panel.rect.width - 2;
                        int clipHeight = panel.rect.height - 2;

                        GuiRenderUtils.enableScissor(this.mc, scissorScreenX(clipX), scissorScreenY(clipY),
                            scissorLen(clipWidth), scissorLen(clipHeight));
                        GlStateManager.pushMatrix();
                        GlStateManager.scale((float) FONT_SCALE, (float) FONT_SCALE, 1.0F);
                        for (String line : lines) {
                            this.fontRenderer.drawStringWithShadow(line, textX, textY, 0xFFFFFF);
                            textY += this.fontRenderer.FONT_HEIGHT + 2;
                        }
                        GlStateManager.popMatrix();
                        GuiRenderUtils.disableScissor();

                        drawPanelScrollBar(panel);
                    }
                }
            }
            drawProgressBars(Integer.valueOf(priority));
            drawCustomSliders(Integer.valueOf(priority));
            drawConfiguredTexts(Integer.valueOf(priority));
            if (priority >= 0) {
                drawConfiguredTextureLayers(true, cfg, Integer.valueOf(priority));
            }
            if (this.smartInterfaceEditorPriority == priority) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(-this.guiLeft, -this.guiTop, 0.0F);
                drawSmartInterfaceEditorBackground(mouseScreenX, mouseScreenY);
                GlStateManager.popMatrix();
                drawSmartInterfaceEditorForeground();
            }
            GlStateManager.pushMatrix();
            GlStateManager.translate(-this.guiLeft, -this.guiTop, 0.0F);
            drawCustomButtons(mouseScreenX, mouseScreenY, Integer.valueOf(priority));
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.translate(-this.guiLeft, -this.guiTop, 0.0F);
            drawCustomSmartInterfaceEditorsBackground(mouseScreenX, mouseScreenY, Integer.valueOf(priority));
            GlStateManager.popMatrix();
            drawCustomSmartInterfaceEditorsForeground(Integer.valueOf(priority));
        }
        resetForegroundRenderState();
    }

    private void drawNegativeForegroundTextureLayers(MMCEGuiExtConfig.MachineController cfg) {
        for (Integer priority : collectForegroundRenderPriorities()) {
            if (priority.intValue() >= 0) {
                continue;
            }
            drawProgressBars(priority);
            drawCustomSliders(priority);
            drawConfiguredTextureLayers(true, cfg, priority);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        if (!this.subGuiContextSwitching && hasActiveModalSubGui() && handleTopModalMouseWheel()) {
            return;
        }
        super.handleMouseInput();

        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        if (isUsingDefaultBackground(MMCEGuiExtConfig.machineController)) {
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        if (this.guiScaleMode) {
            mouseX = toLogicalMouseX(mouseX);
            mouseY = toLogicalMouseY(mouseY);
        }

        String panelId = findHoveredPanelId(mouseX, mouseY, getActivePanels(MMCEGuiExtConfig.machineController));
        if (panelId == null) {
            return;
        }

        int step = Math.max(2, MMCEGuiExtConfig.wheelStep);
        int delta = wheel > 0 ? -step : step;
        int newScroll = getPanelScroll(panelId) + delta;
        int max = getPanelMaxScroll(panelId);
        panelScroll.put(panelId, MathHelper.clamp(newScroll, 0, max));
    }

    @Override
    public void updateScreen() {
        if (!this.subGuiContextSwitching && hasActiveModalSubGui()) {
            withRuntimeState(getTopModalSubGui(), new Runnable() {
                @Override
                public void run() {
                    updateCurrentContextScreen();
                }
            });
        }
        super.updateScreen();
        if (this.smartInterfaceEditorInput != null && !this.smartInterfaceEditorInput.isFocused()) {
            syncSmartInterfaceEditorInput();
        }
        for (CustomSmartEditor editor : this.customSmartEditors) {
            if (editor.input != null && !editor.input.isFocused()) {
                syncCustomSmartEditorInput(editor);
            }
        }
        updateCurrentContextScreen();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (SubGuiBackKeyPolicy.isBackKey(keyCode) && hasReplaceSubGui()) {
            closeActiveSubGui();
            return;
        }
        if (!this.subGuiContextSwitching && hasActiveModalSubGui() && handleTopModalKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (handleCustomButtonKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (handleCustomSmartInterfaceKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (handleSmartInterfaceKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (!this.subGuiContextSwitching && hasActiveModalSubGui() && handleTopModalMouseClicked(mouseX, mouseY, mouseButton)) {
            return;
        }
        if (this.guiScaleMode) {
            mouseX = toLogicalMouseX(mouseX);
            mouseY = toLogicalMouseY(mouseY);
        }
        if (handleCustomButtonMouseClicked(mouseX, mouseY, mouseButton)) {
            return;
        }
        if (handleCustomSliderMouseClicked(mouseX, mouseY, mouseButton)) {
            return;
        }
        if (handleCustomSmartInterfaceMouseClicked(mouseX, mouseY, mouseButton)) {
            return;
        }
        if (handleSmartInterfaceMouseClicked(mouseX, mouseY, mouseButton)) {
            return;
        }
        if (isUsingDefaultBackground(MMCEGuiExtConfig.machineController)) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        if (mouseButton == 0) {
            int localX = mouseX - this.guiLeft;
            int localY = mouseY - this.guiTop;
            for (PanelDef panel : getActivePanels(MMCEGuiExtConfig.machineController)) {
                if (isOnPanelThumb(localX, localY, panel)) {
                    this.draggingPanelId = panel.id;
                    this.infoScrollbarDragOffset = localY - getPanelThumbY(panel.id, panel.rect);
                    break;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (!this.subGuiContextSwitching && hasActiveModalSubGui()) {
            handleTopModalMouseReleased(mouseX, mouseY, state);
            return;
        }
        if (this.guiScaleMode) {
            mouseX = toLogicalMouseX(mouseX);
            mouseY = toLogicalMouseY(mouseY);
        }
        this.draggingPanelId = null;
        this.draggingSlider = null;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (!this.subGuiContextSwitching && hasActiveModalSubGui()) {
            handleTopModalMouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
            return;
        }
        if (this.guiScaleMode) {
            mouseX = toLogicalMouseX(mouseX);
            mouseY = toLogicalMouseY(mouseY);
        }
        if (isUsingDefaultBackground(MMCEGuiExtConfig.machineController)) {
            super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
            return;
        }

        if (clickedMouseButton == 0 && this.draggingSlider != null) {
            updateSliderFromMouse(this.draggingSlider, mouseX, mouseY, false);
            return;
        }

        if (clickedMouseButton == 0 && this.draggingPanelId != null) {
            PanelDef panel = findPanelById(this.draggingPanelId, getActivePanels(MMCEGuiExtConfig.machineController));
            if (panel != null) {
                int localY = mouseY - this.guiTop;
                updatePanelScrollFromMouse(panel.id, panel.rect, localY);
            }
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    private void drawDefaultForeground() {
        MMCEGuiExtConfig.MachineController cfg = MMCEGuiExtConfig.machineController;
        GlStateManager.pushMatrix();
        GlStateManager.scale(FONT_SCALE, FONT_SCALE, FONT_SCALE);

        int offsetX = 12;
        int offsetY = 12;

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int redstone = controller.getWorld().getStrongPower(controller.getPos());
        if (redstone > 0) {
            List<String> out = this.fontRenderer.listFormattedStringToWidth(
                I18n.format("gui.controller.status.redstone_stopped"),
                MathHelper.floor(135 * (1.0D / FONT_SCALE))
            );
            for (String draw : out) {
                offsetY += 10;
                this.fontRenderer.drawStringWithShadow(draw, offsetX, offsetY, 0xFFFFFF);
                offsetY += 10;
            }
            GlStateManager.popMatrix();
            return;
        }

        DynamicMachine machine = controller.getBlueprintMachine();
        if (getShowBlueprintInfo(cfg) && machine != null) {
            this.fontRenderer.drawStringWithShadow(I18n.format("gui.controller.blueprint", ""), offsetX, offsetY, 0xFFFFFF);
            List<String> out = this.fontRenderer.listFormattedStringToWidth(
                machine.getLocalizedName(),
                MathHelper.floor(135 * (1.0D / FONT_SCALE))
            );
            for (String draw : out) {
                offsetY += 10;
                this.fontRenderer.drawStringWithShadow(draw, offsetX, offsetY, 0xFFFFFF);
            }
            offsetY += 15;
        } else if (getShowBlueprintInfo(cfg) && !controller.isStructureFormed()) {
            this.fontRenderer.drawStringWithShadow(
                I18n.format("gui.controller.blueprint", I18n.format("gui.controller.blueprint.none")),
                offsetX,
                offsetY,
                0xFFFFFF
            );
            offsetY += 15;
        }

        DynamicMachine found = controller.getFoundMachine();
        if (getShowStructureInfo(cfg)) {
            if (found != null) {
                this.fontRenderer.drawStringWithShadow(I18n.format("gui.controller.structure", ""), offsetX, offsetY, 0xFFFFFF);
                List<String> out = this.fontRenderer.listFormattedStringToWidth(
                    found.getLocalizedName(),
                    MathHelper.floor(135 * (1.0D / FONT_SCALE))
                );
                for (String draw : out) {
                    offsetY += 10;
                    this.fontRenderer.drawStringWithShadow(draw, offsetX, offsetY, 0xFFFFFF);
                }

                ControllerGUIRenderEvent event = new ControllerGUIRenderEvent(controller);
                event.postEvent();
                String[] extraInfo = event.getExtraInfo();
                if (extraInfo.length != 0) {
                    List<String> waitForDraw = new ArrayList<String>();
                    for (String s : extraInfo) {
                        if (consumeGuiDirective(s)) {
                            continue;
                        }
                        RoutedText routed = parseRoutedText(s, "main");
                        waitForDraw.addAll(
                            this.fontRenderer.listFormattedStringToWidth(
                                routed.text,
                                MathHelper.floor(135 * (1.0D / FONT_SCALE))
                            )
                        );
                    }
                    offsetY += 5;
                    for (String s : waitForDraw) {
                        offsetY += 10;
                        this.fontRenderer.drawStringWithShadow(s, offsetX, offsetY, 0xFFFFFF);
                    }
                }
            } else {
                this.fontRenderer.drawStringWithShadow(
                    I18n.format("gui.controller.structure", I18n.format("gui.controller.structure.none")),
                    offsetX,
                    offsetY,
                    0xFFFFFF
                );
            }
            offsetY += 15;
        } else if (found != null) {
            ControllerGUIRenderEvent event = new ControllerGUIRenderEvent(controller);
            event.postEvent();
        }

        if (getShowStatusInfo(cfg)) {
            this.fontRenderer.drawStringWithShadow(I18n.format("gui.controller.status"), offsetX, offsetY, 0xFFFFFF);
            List<String> statusOut = this.fontRenderer.listFormattedStringToWidth(
                I18n.format(controller.getControllerStatus().getUnlocMessage()),
                MathHelper.floor(135 * (1.0D / FONT_SCALE))
            );
            for (String draw : statusOut) {
                offsetY += 10;
                this.fontRenderer.drawStringWithShadow(draw, offsetX, offsetY, 0xFFFFFF);
            }
            offsetY += 15;
        }

        ActiveMachineRecipe activeRecipe = controller.getActiveRecipe();
        if (getShowStatusInfo(cfg) && activeRecipe != null) {
            if (activeRecipe.getTotalTick() > 0) {
                int progress = (activeRecipe.getTick() * 100) / activeRecipe.getTotalTick();
                this.fontRenderer.drawStringWithShadow(
                    I18n.format("gui.controller.status.crafting.progress", progress + "%"),
                    offsetX,
                    offsetY,
                    0xFFFFFF
                );
                offsetY += 15;
            }
        }

        if (getShowParallelismInfo(cfg) && activeRecipe != null) {
            int parallelism = activeRecipe.getParallelism();
            int maxParallelism = activeRecipe.getMaxParallelism();
            if (parallelism > 1) {
                this.fontRenderer.drawStringWithShadow(
                    I18n.format("gui.controller.parallelism", parallelism),
                    offsetX,
                    offsetY,
                    0xFFFFFF
                );
                offsetY += 10;
                this.fontRenderer.drawStringWithShadow(
                    I18n.format("gui.controller.max_parallelism", maxParallelism),
                    offsetX,
                    offsetY,
                    0xFFFFFF
                );
                offsetY += 15;
            }
        }

        if (getShowPerformanceInfo(cfg)) {
            int usedTimeCache = TileMultiblockMachineController.usedTimeCache;
            float searchUsedTimeCache = TileMultiblockMachineController.searchUsedTimeCache;
            String workMode = TileMultiblockMachineController.workModeCache.getDisplayName();
            List<String> perf = this.fontRenderer.listFormattedStringToWidth(
                String.format(
                    "Avg: %dus/t (Search: %sms), WorkMode: %s",
                    usedTimeCache,
                    MiscUtils.formatFloat(searchUsedTimeCache / 1000F, 2),
                    workMode
                ),
                MathHelper.floor(135 * (1.0D / FONT_SCALE))
            );
            for (String draw : perf) {
                offsetY += 10;
                this.fontRenderer.drawStringWithShadow(draw, offsetX, offsetY, 0xFFFFFF);
            }
        }

        GlStateManager.popMatrix();
    }

    private void drawConfiguredTexts(@Nullable Integer priorityFilter) {
        if (styleOverride.texts == null || styleOverride.texts.isEmpty()) {
            return;
        }
        for (MachineGuiStyleManager.TextStyle text : styleOverride.texts) {
            if (text == null || text.value == null || text.value.trim().isEmpty()) {
                continue;
            }
            if (text.visible != null && !text.visible.booleanValue()) {
                continue;
            }
            if (!isPageVisible(text.page)) {
                continue;
            }
            int priority = text.priority == null ? resolveForegroundContentPriority() : text.priority.intValue();
            if (priorityFilter != null && priority != priorityFilter.intValue()) {
                continue;
            }
            String value = resolveConfiguredTextValue(text.value);
            if (value == null || value.isEmpty()) {
                continue;
            }
            int color = text.color == null ? 0xFFFFFF : text.color.intValue();
            float scale = text.scale == null ? 1.0F : Math.max(0.05F, text.scale.floatValue());
            boolean shadow = text.shadow == null || text.shadow.booleanValue();
            int alignedX = GuiRenderUtils.resolveAlignedTextX(text.x, Math.round(this.fontRenderer.getStringWidth(value) * scale), text.align);
            GlStateManager.pushMatrix();
            GlStateManager.scale(scale, scale, 1.0F);
            int drawX = MathHelper.floor((float) (alignedX / scale));
            int drawY = MathHelper.floor((float) (text.y / scale));
            if (shadow) {
                this.fontRenderer.drawStringWithShadow(value, drawX, drawY, color);
            } else {
                this.fontRenderer.drawString(value, drawX, drawY, color);
            }
            GlStateManager.popMatrix();
        }
    }

    private void drawProgressBars(@Nullable Integer priorityFilter) {
        if (styleOverride.progressBars == null || styleOverride.progressBars.isEmpty()) {
            return;
        }
        for (MachineGuiStyleManager.ProgressBarStyle bar : styleOverride.progressBars) {
            if (bar == null) {
                continue;
            }
            if (bar.visible != null && !bar.visible.booleanValue()) {
                continue;
            }
            if (!isPageVisible(bar.page)) {
                continue;
            }
            int priority = bar.priority == null ? resolveForegroundContentPriority() : bar.priority.intValue();
            if (priorityFilter != null && priority != priorityFilter.intValue()) {
                continue;
            }
            drawProgressBar(bar, resolveProgressBarValue(bar));
        }
    }

    private void drawBackgroundProgressBars() {
        if (styleOverride.progressBars == null || styleOverride.progressBars.isEmpty()) {
            return;
        }
        for (MachineGuiStyleManager.ProgressBarStyle bar : styleOverride.progressBars) {
            if (bar == null || !Boolean.FALSE.equals(bar.foreground)) {
                continue;
            }
            if (bar.visible != null && !bar.visible.booleanValue()) {
                continue;
            }
            if (!isPageVisible(bar.page)) {
                continue;
            }
            drawProgressBar(bar, resolveProgressBarValue(bar));
        }
    }

    private float resolveProgressBarValue(MachineGuiStyleManager.ProgressBarStyle bar) {
        String source = ProgressBarStyleSupport.normalizeProgressBarSource(bar.source);
        if (source == null || "machine_progress".equals(source)) {
            return ProgressBarStyleSupport.normalizeProgressValue(bar, ProgressBarStyleSupport.recipeProgress(controller.getActiveRecipe()));
        }
        return ProgressBarStyleSupport.normalizeProgressValue(bar, 0.0F);
    }

    private void drawProgressBar(MachineGuiStyleManager.ProgressBarStyle bar, float progress) {
        drawProgressBarBody(bar, progress);
        if (Boolean.TRUE.equals(bar.showText)) {
            String text = Math.round(progress * 100.0F) + "%";
            int color = bar.textColor == null ? 0xFFFFFFFF : bar.textColor.intValue();
            int textX = bar.x + Math.max(0, (bar.width - this.fontRenderer.getStringWidth(text)) / 2);
            int textY = bar.y + Math.max(0, (bar.height - this.fontRenderer.FONT_HEIGHT) / 2);
            this.fontRenderer.drawStringWithShadow(text, textX, textY, color);
        }
    }

    private void drawProgressBarBody(MachineGuiStyleManager.ProgressBarStyle bar, float progress) {
        int textureWidth = bar.textureWidth == null ? bar.width : Math.max(1, bar.textureWidth.intValue());
        int textureHeight = bar.textureHeight == null ? bar.height : Math.max(1, bar.textureHeight.intValue());
        int bgX = bar.x;
        int bgY = bar.y;
        ResourceLocation backgroundTexture = resolveProgressBarTexture(bar, false);
        if (backgroundTexture != null) {
            this.mc.getTextureManager().bindTexture(backgroundTexture);
            Gui.drawModalRectWithCustomSizedTexture(bgX, bgY, 0, 0, bar.width, bar.height, textureWidth, textureHeight);
        } else {
            int bg = bar.backgroundColor == null ? 0x66000000 : bar.backgroundColor.intValue();
            drawRect(bgX, bgY, bgX + bar.width, bgY + bar.height, bg);
        }
        if (bar.borderColor != null) {
            drawProgressBorder(bar.x, bar.y, bar.width, bar.height, bar.borderColor.intValue());
        }

        int[] fillBounds = ProgressBarStyleSupport.computeFillBounds(bar.x, bar.y, bar.width, bar.height, bar.direction, progress);
        if (fillBounds[2] <= 0 || fillBounds[3] <= 0) {
            return;
        }
        ResourceLocation fillTexture = resolveProgressBarTexture(bar, true);
        if (fillTexture != null) {
            this.mc.getTextureManager().bindTexture(fillTexture);
            Gui.drawModalRectWithCustomSizedTexture(fillBounds[0], fillBounds[1], 0, 0, fillBounds[2], fillBounds[3], textureWidth, textureHeight);
        } else {
            int fill = bar.fillColor == null ? 0xFF55CC66 : bar.fillColor.intValue();
            drawRect(fillBounds[0], fillBounds[1], fillBounds[0] + fillBounds[2], fillBounds[1] + fillBounds[3], fill);
        }
    }

    @Nullable
    private ResourceLocation resolveProgressBarTexture(MachineGuiStyleManager.ProgressBarStyle bar, boolean fillLayer) {
        String raw = fillLayer ? bar.fillTexture : bar.backgroundTexture;
        if (raw == null || raw.trim().isEmpty()) {
            raw = bar.texture;
        }
        return GuiRenderUtils.parseOptionalTexture(raw);
    }

    private void drawProgressBorder(int x, int y, int width, int height, int color) {
        drawRect(x, y, x + width, y + 1, color);
        drawRect(x, y + height - 1, x + width, y + height, color);
        drawRect(x, y, x + 1, y + height, color);
        drawRect(x + width - 1, y, x + width, y + height, color);
    }

    private void drawExtensionFallback() {
        int rightWidth = this.renderWidth - BASE_WIDTH + 1;
        if (rightWidth > 0) {
            drawRect(
                this.guiLeft + BASE_WIDTH - 1,
                this.guiTop,
                this.guiLeft + BASE_WIDTH - 1 + rightWidth,
                this.guiTop + this.renderHeight,
                0xA0101010
            );
        }

        int bottomHeight = this.renderHeight - BASE_HEIGHT + 1;
        if (bottomHeight > 0) {
            drawRect(
                this.guiLeft,
                this.guiTop + BASE_HEIGHT - 1,
                this.guiLeft + BASE_WIDTH,
                this.guiTop + BASE_HEIGHT - 1 + bottomHeight,
                0xA0101010
            );
        }
    }

    private void drawResizableArea(
        int x,
        int y,
        int width,
        int height,
        boolean nineSlice,
        int texW,
        int texH,
        int corner
    ) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (nineSlice) {
            GuiRenderUtils.drawNineSlice(x, y, width, height, texW, texH, Math.max(2, corner));
            return;
        }
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, texW, texH);
    }

    private void drawLayerWithTransform(
        int x,
        int y,
        int width,
        int height,
        boolean nineSlice,
        int texW,
        int texH,
        int corner,
        float scaleX,
        float scaleY,
        float rotation
    ) {
        float sx = Math.max(0.01F, scaleX);
        float sy = Math.max(0.01F, scaleY);
        boolean transformed = Math.abs(sx - 1.0F) > 1.0E-4F || Math.abs(sy - 1.0F) > 1.0E-4F || Math.abs(rotation) > 1.0E-4F;
        if (!transformed) {
            drawResizableArea(x, y, width, height, nineSlice, texW, texH, corner);
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(x + width * 0.5F, y + height * 0.5F, 0.0F);
        if (Math.abs(rotation) > 1.0E-4F) {
            GlStateManager.rotate(rotation, 0.0F, 0.0F, 1.0F);
        }
        GlStateManager.scale(sx, sy, 1.0F);
        drawResizableArea(-width / 2, -height / 2, width, height, nineSlice, texW, texH, corner);
        GlStateManager.popMatrix();
    }

    private int resolveLayerOffsetX(TextureLayerDef layer) {
        LayerRuntimeState runtime = this.layerRuntimeStates.get(layer.id);
        return runtime != null && runtime.offsetX != null ? runtime.offsetX.intValue() : layer.offsetX;
    }

    private int resolveLayerOffsetY(TextureLayerDef layer) {
        LayerRuntimeState runtime = this.layerRuntimeStates.get(layer.id);
        return runtime != null && runtime.offsetY != null ? runtime.offsetY.intValue() : layer.offsetY;
    }

    private float resolveLayerScaleX(TextureLayerDef layer) {
        LayerRuntimeState runtime = this.layerRuntimeStates.get(layer.id);
        if (runtime == null) {
            return 1.0F;
        }
        if (runtime.scaleX != null) {
            return runtime.scaleX.floatValue();
        }
        if (runtime.scale != null) {
            return runtime.scale.floatValue();
        }
        return 1.0F;
    }

    private float resolveLayerScaleY(TextureLayerDef layer) {
        LayerRuntimeState runtime = this.layerRuntimeStates.get(layer.id);
        if (runtime == null) {
            return 1.0F;
        }
        if (runtime.scaleY != null) {
            return runtime.scaleY.floatValue();
        }
        if (runtime.scale != null) {
            return runtime.scale.floatValue();
        }
        return 1.0F;
    }

    private float resolveLayerRotation(TextureLayerDef layer) {
        LayerRuntimeState runtime = this.layerRuntimeStates.get(layer.id);
        return runtime != null && runtime.rotation != null ? runtime.rotation.floatValue() : 0.0F;
    }

    private float resolveLayerAlpha(TextureLayerDef layer) {
        LayerRuntimeState runtime = this.layerRuntimeStates.get(layer.id);
        return runtime != null && runtime.alpha != null ? normalizeLayerAlpha(runtime.alpha.floatValue()) : layer.alpha;
    }

    private static float normalizeLayerAlpha(float value) {
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

    private int resolveLayerPriority(TextureLayerDef layer) {
        LayerRuntimeState runtime = this.layerRuntimeStates.get(layer.id);
        return runtime != null && runtime.priority != null ? runtime.priority.intValue() : layer.priority;
    }

    private void applyPlayerInventoryVisibility(MMCEGuiExtConfig.MachineController cfg) {
        boolean hidePlayerInventory = getHidePlayerInventory(cfg);
        if (this.inventorySlots == null || this.inventorySlots.inventorySlots == null) {
            return;
        }

        int visibleSlots = Math.min(36, this.inventorySlots.inventorySlots.size());
        for (int i = 0; i < visibleSlots; i++) {
            Slot slot = this.inventorySlots.inventorySlots.get(i);
            if (slot == null) {
                continue;
            }

            if (hidePlayerInventory) {
                slot.xPos = -1000;
                slot.yPos = -1000;
            } else if (i < 27) {
                slot.xPos = PLAYER_INVENTORY_LEFT + (i % 9) * 18;
                slot.yPos = PLAYER_INVENTORY_TOP + (i / 9) * 18;
            } else {
                slot.xPos = PLAYER_INVENTORY_LEFT + (i - 27) * 18;
                slot.yPos = PLAYER_HOTBAR_TOP;
            }
        }
    }

    private void maskPlayerInventoryArea() {
        drawRect(
            PLAYER_INVENTORY_LEFT - 1,
            PLAYER_INVENTORY_TOP - 1,
            PLAYER_INVENTORY_LEFT + PLAYER_INVENTORY_WIDTH + 1,
            PLAYER_INVENTORY_TOP + PLAYER_INVENTORY_HEIGHT + 1,
            PLAYER_INVENTORY_MASK_COLOR
        );
    }

    private boolean isLayerVisible(TextureLayerDef layer) {
        LayerRuntimeState runtime = this.layerRuntimeStates.get(layer.id);
        return runtime == null || runtime.visible == null || runtime.visible.booleanValue();
    }

    private String allocateUniqueLayerId(String requested) {
        if (!this.textureLayerIds.contains(requested)) {
            return requested;
        }
        int suffix = 2;
        String candidate = requested + "_" + suffix;
        while (this.textureLayerIds.contains(candidate)) {
            suffix++;
            candidate = requested + "_" + suffix;
        }
        return candidate;
    }

    private List<PanelDef> getActivePanels(MMCEGuiExtConfig.MachineController cfg) {
        List<PanelDef> panels = new ArrayList<PanelDef>();
        if (isUsingDefaultBackground(cfg)) {
            panels.add(new PanelDef(resolveConfiguredDefaultPanelId(cfg), getDefaultPanelRect(cfg), null));
            return panels;
        }

        String[] configuredPanels = getConfiguredPanels(cfg);
        if (configuredPanels != null) {
            LinkedHashMap<String, PanelDef> map = new LinkedHashMap<String, PanelDef>();
            for (String entry : configuredPanels) {
                PanelDef parsed = parsePanelEntry(entry);
                if (parsed != null) {
                    map.put(parsed.id, parsed);
                }
            }
            panels.addAll(map.values());
        }

        if (panels.isEmpty()) {
            int x = this.renderWidth > BASE_WIDTH + 24 ? BASE_WIDTH + 6 : 10;
            int y = Math.max(4, cfg.panelY);
            int width = Math.max(48, this.renderWidth - x - 8);
            int height = Math.max(24, this.renderHeight - y - 4);
            panels.add(new PanelDef("main", new Rectangle(x, y, width, height), null));
        }

        panels.removeIf(panel -> !isPageVisible(panel.page));
        return panels;
    }

    private Rectangle getDefaultPanelRect(MMCEGuiExtConfig.MachineController cfg) {
        int panelX = cfg.panelX > 0 ? cfg.panelX : 10;
        panelX = Math.max(0, Math.min(panelX, Math.max(0, BASE_WIDTH - 32)));

        int panelY = Math.max(4, cfg.panelY);
        panelY = Math.min(panelY, getPlayerInventoryTop(cfg) - 24);

        int panelWidth = cfg.panelWidth > 0 ? cfg.panelWidth : 140;
        panelWidth = Math.max(48, Math.min(panelWidth, Math.max(48, BASE_WIDTH - panelX - 4)));
        panelWidth = Math.min(panelWidth, 140);

        int maxHeight = Math.max(24, getPlayerInventoryTop(cfg) - panelY - 4);
        int panelHeight = cfg.panelHeight > 0 ? cfg.panelHeight : 112;
        panelHeight = Math.max(24, Math.min(panelHeight, maxHeight));

        return new Rectangle(panelX, panelY, panelWidth, panelHeight);
    }

    @Nullable
    private PanelDef parsePanelEntry(@Nullable String entry) {
        if (entry == null) {
            return null;
        }
        String trimmed = entry.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String[] split = trimmed.split(",");
        if (split.length != 5 && split.length != 6) {
            return null;
        }

        String id = normalizePanelId(split[0]);
        if (id.isEmpty()) {
            return null;
        }

        try {
            int x = Integer.parseInt(split[1].trim());
            int y = Integer.parseInt(split[2].trim());
            int width = Integer.parseInt(split[3].trim());
            int height = Integer.parseInt(split[4].trim());

            if (width < 24 || height < 24) {
                return null;
            }

            x = Math.max(0, x);
            y = Math.max(0, y);
            if (x >= this.renderWidth - 4 || y >= this.renderHeight - 4) {
                return null;
            }

            width = Math.max(24, Math.min(width, this.renderWidth - x - 2));
            height = Math.max(24, Math.min(height, this.renderHeight - y - 2));

            String page = split.length >= 6 ? normalizePageIdOrNull(split[5]) : null;
            return new PanelDef(id, new Rectangle(x, y, width, height), page);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Map<String, List<String>> buildPanelInfoLines(List<PanelDef> panels, MMCEGuiExtConfig.MachineController cfg) {
        Map<String, List<String>> linesByPanel = new HashMap<String, List<String>>();
        Map<String, PanelDef> panelMap = toPanelMap(panels);
        for (PanelDef panel : panels) {
            linesByPanel.put(panel.id, new ArrayList<String>());
        }

        String defaultPanelId = resolveDefaultPanelId(panels, resolveConfiguredDefaultPanelId(cfg));
        String blueprintPanel = resolveInfoSectionPanel(defaultPanelId, "blueprint", "blueprint_info");
        String structurePanel = resolveInfoSectionPanel(defaultPanelId, "structure", "structure_info");
        String statusPanel = resolveInfoSectionPanel(defaultPanelId, "status", "status_info");
        String parallelismPanel = resolveInfoSectionPanel(defaultPanelId, "parallelism", "parallelism_info", "threads", "thread_info");
        String performancePanel = resolveInfoSectionPanel(defaultPanelId, "performance", "performance_info", "perf");

        int redstone = controller.getWorld().getStrongPower(controller.getPos());
        if (redstone > 0) {
            if (getShowStatusInfo(cfg)) {
                addWrapped(linesByPanel, panelMap, statusPanel, I18n.format("gui.controller.status.redstone_stopped"), defaultPanelId);
            }
            return linesByPanel;
        }

        DynamicMachine machine = controller.getBlueprintMachine();
        if (getShowBlueprintInfo(cfg) && machine != null) {
            addWrapped(linesByPanel, panelMap, blueprintPanel, I18n.format("gui.controller.blueprint", ""), defaultPanelId);
            addWrapped(linesByPanel, panelMap, blueprintPanel, machine.getLocalizedName(), defaultPanelId);
            addBlank(linesByPanel, blueprintPanel);
        } else if (getShowBlueprintInfo(cfg) && !controller.isStructureFormed()) {
            addWrapped(
                linesByPanel,
                panelMap,
                blueprintPanel,
                I18n.format("gui.controller.blueprint", I18n.format("gui.controller.blueprint.none")),
                defaultPanelId
            );
            addBlank(linesByPanel, blueprintPanel);
        }

        DynamicMachine found = controller.getFoundMachine();
        if (getShowStructureInfo(cfg) && found != null) {
            addWrapped(linesByPanel, panelMap, structurePanel, I18n.format("gui.controller.structure", ""), defaultPanelId);
            addWrapped(linesByPanel, panelMap, structurePanel, found.getLocalizedName(), defaultPanelId);
            ControllerGUIRenderEvent event = new ControllerGUIRenderEvent(controller);
            event.postEvent();
            for (String extra : event.getExtraInfo()) {
                if (consumeGuiDirective(extra)) {
                    continue;
                }
                RoutedText routed = parseRoutedText(extra, defaultPanelId);
                String targetPanel = panelMap.containsKey(routed.panelId) ? routed.panelId : structurePanel;
                addWrapped(linesByPanel, panelMap, targetPanel, routed.text, defaultPanelId);
            }
            addBlank(linesByPanel, structurePanel);
        } else if (getShowStructureInfo(cfg)) {
            addWrapped(
                linesByPanel,
                panelMap,
                structurePanel,
                I18n.format("gui.controller.structure", I18n.format("gui.controller.structure.none")),
                defaultPanelId
            );
            addBlank(linesByPanel, structurePanel);
        } else if (found != null) {
            ControllerGUIRenderEvent event = new ControllerGUIRenderEvent(controller);
            event.postEvent();
            for (String extra : event.getExtraInfo()) {
                consumeGuiDirective(extra);
            }
        }

        if (getShowStatusInfo(cfg)) {
            addWrapped(linesByPanel, panelMap, statusPanel, I18n.format("gui.controller.status"), defaultPanelId);
            addWrapped(linesByPanel, panelMap, statusPanel, I18n.format(controller.getControllerStatus().getUnlocMessage()), defaultPanelId);
            addBlank(linesByPanel, statusPanel);
        }

        ActiveMachineRecipe activeRecipe = controller.getActiveRecipe();
        if (getShowStatusInfo(cfg) && activeRecipe != null) {
            if (activeRecipe.getTotalTick() > 0) {
                int progress = (activeRecipe.getTick() * 100) / activeRecipe.getTotalTick();
                addWrapped(
                    linesByPanel,
                    panelMap,
                    statusPanel,
                    I18n.format("gui.controller.status.crafting.progress", progress + "%"),
                    defaultPanelId
                );
                addBlank(linesByPanel, statusPanel);
            }
        }

        if (getShowParallelismInfo(cfg) && activeRecipe != null) {
            int parallelism = activeRecipe.getParallelism();
            int maxParallelism = activeRecipe.getMaxParallelism();
            if (parallelism > 1) {
                addWrapped(linesByPanel, panelMap, parallelismPanel, I18n.format("gui.controller.parallelism", parallelism), defaultPanelId);
                addWrapped(
                    linesByPanel,
                    panelMap,
                    parallelismPanel,
                    I18n.format("gui.controller.max_parallelism", maxParallelism),
                    defaultPanelId
                );
                addBlank(linesByPanel, parallelismPanel);
            }
        }

        if (getShowPerformanceInfo(cfg)) {
            int usedTimeCache = TileMultiblockMachineController.usedTimeCache;
            float searchUsedTimeCache = TileMultiblockMachineController.searchUsedTimeCache;
            String workMode = TileMultiblockMachineController.workModeCache.getDisplayName();
            addWrapped(
                linesByPanel,
                panelMap,
                performancePanel,
                String.format(
                    "Avg: %dus/t (Search: %sms), WorkMode: %s",
                    usedTimeCache,
                    MiscUtils.formatFloat(searchUsedTimeCache / 1000F, 2),
                    workMode
                ),
                defaultPanelId
            );
        }

        return linesByPanel;
    }

    private void addWrapped(
        Map<String, List<String>> linesByPanel,
        Map<String, PanelDef> panelMap,
        String requestedPanel,
        @Nullable String text,
        String defaultPanelId
    ) {
        if (text == null) {
            return;
        }
        String targetPanel = panelMap.containsKey(requestedPanel) ? requestedPanel : defaultPanelId;
        List<String> target = linesByPanel.get(targetPanel);
        if (target == null) {
            return;
        }
        if (text.isEmpty()) {
            target.add("");
            return;
        }

        PanelDef panel = panelMap.get(targetPanel);
        if (panel == null) {
            return;
        }
        int wrapWidth = Math.max(24, MathHelper.floor((float) ((panel.rect.width - 8) / FONT_SCALE)));
        target.addAll(this.fontRenderer.listFormattedStringToWidth(text, wrapWidth));
    }

    private void addBlank(Map<String, List<String>> linesByPanel, String panelId) {
        List<String> target = linesByPanel.get(panelId);
        if (target != null) {
            target.add("");
        }
    }

    private void drawPanelScrollBar(PanelDef panel) {
        int maxScroll = getPanelMaxScroll(panel.id);
        if (maxScroll <= 0) {
            return;
        }

        int barX = getPanelBarX(panel.rect);
        int barTop = getPanelBarTop(panel.rect);
        int barWidth = getPanelBarWidth();
        int barHeight = getPanelBarHeight(panel.rect);
        int thumbY = getPanelThumbY(panel.id, panel.rect);
        int thumbHeight = getPanelThumbHeight(panel.id, panel.rect);

        drawRect(barX, barTop, barX + barWidth, barTop + barHeight, 0x50000000);
        drawRect(
            barX,
            thumbY,
            barX + barWidth,
            thumbY + thumbHeight,
            panel.id.equals(this.draggingPanelId) ? 0xFFFFFFFF : 0xE0FFFFFF
        );
    }

    private String findHoveredPanelId(int mouseX, int mouseY, List<PanelDef> panels) {
        for (PanelDef panel : panels) {
            if (GuiRenderUtils.isMouseInPanel(mouseX, mouseY, panel.rect, this.guiLeft, this.guiTop)) {
                return panel.id;
            }
        }
        return null;
    }

    @Nullable
    private PanelDef findPanelById(String panelId, List<PanelDef> panels) {
        for (PanelDef panel : panels) {
            if (panel.id.equals(panelId)) {
                return panel;
            }
        }
        return null;
    }

    private void cleanupPanelState(List<PanelDef> panels) {
        List<String> validIds = new ArrayList<String>();
        for (PanelDef panel : panels) {
            validIds.add(panel.id);
        }

        this.panelScroll.keySet().removeIf(id -> !validIds.contains(id));
        this.panelMaxScroll.keySet().removeIf(id -> !validIds.contains(id));
        if (this.draggingPanelId != null && !validIds.contains(this.draggingPanelId)) {
            this.draggingPanelId = null;
        }
    }

    private Map<String, PanelDef> toPanelMap(List<PanelDef> panels) {
        Map<String, PanelDef> map = new HashMap<String, PanelDef>();
        for (PanelDef panel : panels) {
            map.put(panel.id, panel);
        }
        return map;
    }

    private String resolveDefaultPanelId(List<PanelDef> panels, @Nullable String configured) {
        String wanted = normalizePanelId(configured);
        if (!wanted.isEmpty()) {
            for (PanelDef panel : panels) {
                if (panel.id.equals(wanted)) {
                    return panel.id;
                }
            }
        }
        return panels.isEmpty() ? "main" : panels.get(0).id;
    }

    private String normalizePanelId(@Nullable String id) {
        if (id == null) {
            return "main";
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? "main" : normalized;
    }

    private String normalizePageId(@Nullable String id) {
        if (id == null) {
            return "main";
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? "main" : normalized;
    }

    @Nullable
    private String normalizePageIdOrNull(@Nullable String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return normalizePageId(id);
    }

    private boolean isPageVisible(@Nullable String page) {
        return page == null || page.isEmpty() || normalizePageId(page).equals(this.activePageId);
    }

    private boolean isOnPanelThumb(int localX, int localY, PanelDef panel) {
        if (getPanelMaxScroll(panel.id) <= 0) {
            return false;
        }
        int barX = getPanelBarX(panel.rect);
        int barTop = getPanelBarTop(panel.rect);
        int barWidth = getPanelBarWidth();
        int thumbY = getPanelThumbY(panel.id, panel.rect);
        int thumbHeight = getPanelThumbHeight(panel.id, panel.rect);
        return localX >= barX && localX < barX + barWidth && localY >= thumbY && localY < thumbY + thumbHeight
               && localY >= barTop && localY < barTop + getPanelBarHeight(panel.rect);
    }

    private int getPanelBarX(Rectangle panel) {
        return panel.x + panel.width - 4;
    }

    private int getPanelBarTop(Rectangle panel) {
        return panel.y + 1;
    }

    private int getPanelBarWidth() {
        return 3;
    }

    private int getPanelBarHeight(Rectangle panel) {
        return panel.height - 2;
    }

    private int getPanelThumbHeight(String panelId, Rectangle panel) {
        return Math.max(12, (panel.height * panel.height) / (panel.height + getPanelMaxScroll(panelId) + 1));
    }

    private int getPanelThumbY(String panelId, Rectangle panel) {
        int barTop = getPanelBarTop(panel);
        int barHeight = getPanelBarHeight(panel);
        int thumbHeight = getPanelThumbHeight(panelId, panel);
        int maxTravel = Math.max(1, barHeight - thumbHeight);
        return barTop + (getPanelScroll(panelId) * maxTravel) / Math.max(1, getPanelMaxScroll(panelId));
    }

    private void updatePanelScrollFromMouse(String panelId, Rectangle panel, int localMouseY) {
        int barTop = getPanelBarTop(panel);
        int barHeight = getPanelBarHeight(panel);
        int thumbHeight = getPanelThumbHeight(panelId, panel);
        int maxTravel = Math.max(1, barHeight - thumbHeight);

        int thumbTop = MathHelper.clamp(localMouseY - this.infoScrollbarDragOffset, barTop, barTop + maxTravel);
        int travel = thumbTop - barTop;
        int newScroll = (travel * getPanelMaxScroll(panelId)) / maxTravel;
        panelScroll.put(panelId, MathHelper.clamp(newScroll, 0, getPanelMaxScroll(panelId)));
    }

    private int getPanelScroll(String panelId) {
        Integer v = panelScroll.get(panelId);
        return v == null ? 0 : v.intValue();
    }

    private int getPanelMaxScroll(String panelId) {
        Integer v = panelMaxScroll.get(panelId);
        return v == null ? 0 : v.intValue();
    }

    @Nullable
    private DynamicMachine resolveMachine() {
        DynamicMachine found = controller.getFoundMachine();
        if (found != null) {
            return found;
        }
        return controller.getBlueprintMachine();
    }

    @Nullable
    private ResourceLocation resolveCustomTexture() {
        String overrideTexture = styleOverride.backgroundTexture;
        if (overrideTexture != null && !overrideTexture.trim().isEmpty()) {
            if (DEFAULT_BACKGROUND_STR.equalsIgnoreCase(overrideTexture.trim())) {
                return null;
            }
            return GuiRenderUtils.parseOptionalTexture(overrideTexture);
        }

        String configTexture = MMCEGuiExtConfig.machineController.backgroundTexture;
        if (configTexture == null || configTexture.trim().isEmpty()) {
            return null;
        }
        if (DEFAULT_BACKGROUND_STR.equalsIgnoreCase(configTexture.trim())) {
            return null;
        }
        return GuiRenderUtils.parseOptionalTexture(configTexture);
    }

    private boolean getUseNineSlice(MMCEGuiExtConfig.MachineController cfg) {
        if (styleOverride.useNineSlice != null) {
            return styleOverride.useNineSlice.booleanValue();
        }
        return cfg.useNineSlice;
    }

    private int getBackgroundTextureWidth(MMCEGuiExtConfig.MachineController cfg) {
        if (styleOverride.backgroundTextureWidth != null) {
            return Math.max(16, styleOverride.backgroundTextureWidth.intValue());
        }
        if (hasMachineStyleOverride() && this.customBackgroundTexture != null) {
            return Math.max(16, this.renderWidth + getBackgroundTextureOffsetX(cfg));
        }
        return Math.max(16, cfg.backgroundTextureWidth);
    }

    private int getBackgroundTextureHeight(MMCEGuiExtConfig.MachineController cfg) {
        if (styleOverride.backgroundTextureHeight != null) {
            return Math.max(16, styleOverride.backgroundTextureHeight.intValue());
        }
        if (hasMachineStyleOverride() && this.customBackgroundTexture != null) {
            return Math.max(16, this.renderHeight + getBackgroundTextureOffsetY(cfg));
        }
        return Math.max(16, cfg.backgroundTextureHeight);
    }

    private int getBackgroundCorner(MMCEGuiExtConfig.MachineController cfg) {
        if (styleOverride.backgroundCorner != null) {
            return Math.max(2, styleOverride.backgroundCorner.intValue());
        }
        return Math.max(2, cfg.backgroundCorner);
    }

    private boolean getHideDefaultBackground(MMCEGuiExtConfig.MachineController cfg) {
        if (styleOverride.hideDefaultBackground != null) {
            return styleOverride.hideDefaultBackground.booleanValue();
        }
        return cfg.hideDefaultBackground;
    }

    private boolean getCenterFullGui() {
        return styleOverride.centerFullGui != null && styleOverride.centerFullGui.booleanValue();
    }

    private int getBackgroundTextureOffsetX(MMCEGuiExtConfig.MachineController cfg) {
        if (styleOverride.backgroundTextureOffsetX != null) {
            return Math.max(0, styleOverride.backgroundTextureOffsetX.intValue());
        }
        if (hasMachineStyleOverride()) {
            return 0;
        }
        return Math.max(0, cfg.backgroundTextureOffsetX);
    }

    private int getBackgroundTextureOffsetY(MMCEGuiExtConfig.MachineController cfg) {
        if (styleOverride.backgroundTextureOffsetY != null) {
            return Math.max(0, styleOverride.backgroundTextureOffsetY.intValue());
        }
        if (hasMachineStyleOverride()) {
            return 0;
        }
        return Math.max(0, cfg.backgroundTextureOffsetY);
    }

    private int getRequestedGuiWidth(MMCEGuiExtConfig.MachineController cfg) {
        if (styleOverride.guiWidth != null) {
            return styleOverride.guiWidth.intValue();
        }
        if (hasMachineStyleOverride()) {
            return BASE_WIDTH;
        }
        return cfg.guiWidth;
    }

    private int getRequestedGuiHeight(MMCEGuiExtConfig.MachineController cfg) {
        if (styleOverride.guiHeight != null) {
            return styleOverride.guiHeight.intValue();
        }
        if (hasMachineStyleOverride()) {
            return BASE_HEIGHT;
        }
        return cfg.guiHeight;
    }

    private boolean getDisableRightExtension() {
        return styleOverride.disableRightExtension != null && styleOverride.disableRightExtension.booleanValue();
    }

    private boolean isUsingDefaultBackground(MMCEGuiExtConfig.MachineController cfg) {
        return this.customBackgroundTexture == null && !getHideDefaultBackground(cfg) && !hasPerMachineSizeOverride();
    }

    private boolean hasPerMachineSizeOverride() {
        return styleOverride.guiWidth != null || styleOverride.guiHeight != null;
    }

    private boolean getHidePlayerInventory(MMCEGuiExtConfig.MachineController cfg) {
        if (styleOverride.hidePlayerInventory != null) {
            return styleOverride.hidePlayerInventory.booleanValue();
        }
        return false;
    }

    private boolean getShowBlueprintInfo(MMCEGuiExtConfig.MachineController cfg) {
        Boolean visible = resolveInfoSectionVisible("blueprint", "blueprint_info");
        if (visible != null) {
            return visible.booleanValue();
        }
        if (styleOverride.showBlueprintInfo != null) {
            return styleOverride.showBlueprintInfo.booleanValue();
        }
        return cfg.showBlueprintInfo;
    }

    private boolean getShowStructureInfo(MMCEGuiExtConfig.MachineController cfg) {
        Boolean visible = resolveInfoSectionVisible("structure", "structure_info");
        if (visible != null) {
            return visible.booleanValue();
        }
        if (styleOverride.showStructureInfo != null) {
            return styleOverride.showStructureInfo.booleanValue();
        }
        return cfg.showStructureInfo;
    }

    private boolean getShowStatusInfo(MMCEGuiExtConfig.MachineController cfg) {
        Boolean visible = resolveInfoSectionVisible("status", "status_info");
        if (visible != null) {
            return visible.booleanValue();
        }
        if (styleOverride.showStatusInfo != null) {
            return styleOverride.showStatusInfo.booleanValue();
        }
        return cfg.showStatusInfo;
    }

    private boolean getShowParallelismInfo(MMCEGuiExtConfig.MachineController cfg) {
        Boolean visible = resolveInfoSectionVisible("parallelism", "parallelism_info", "threads", "thread_info");
        if (visible != null) {
            return visible.booleanValue();
        }
        if (styleOverride.showParallelismInfo != null) {
            return styleOverride.showParallelismInfo.booleanValue();
        }
        return cfg.showParallelismInfo;
    }

    private boolean getShowPerformanceInfo(MMCEGuiExtConfig.MachineController cfg) {
        Boolean visible = resolveInfoSectionVisible("performance", "performance_info", "perf");
        if (visible != null) {
            return visible.booleanValue();
        }
        if (styleOverride.showPerformanceInfo != null) {
            return styleOverride.showPerformanceInfo.booleanValue();
        }
        return cfg.showPerformanceInfo;
    }

    private String resolveConfiguredTextValue(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return "";
        }
        if (text.indexOf('{') >= 0 && text.indexOf('}') > text.indexOf('{')) {
            StringBuilder out = new StringBuilder();
            int index = 0;
            while (index < text.length()) {
                int start = text.indexOf('{', index);
                if (start < 0) {
                    out.append(text.substring(index));
                    break;
                }
                int end = text.indexOf('}', start + 1);
                if (end < 0) {
                    out.append(text.substring(index));
                    break;
                }
                out.append(text, index, start);
                out.append(resolveControllerInfoKey(text.substring(start + 1, end)));
                index = end + 1;
            }
            return out.toString();
        }
        return resolveControllerInfoKey(text);
    }

    private String resolveControllerInfoKey(@Nullable String rawKey) {
        String key = rawKey == null ? "" : rawKey.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) {
            return "";
        }

        DynamicMachine blueprint = controller.getBlueprintMachine();
        DynamicMachine found = controller.getFoundMachine();
        ActiveMachineRecipe activeRecipe = controller.getActiveRecipe();

        if ("blueprint.name".equals(key) || "blueprint".equals(key)) {
            return blueprint == null ? I18n.format("gui.controller.blueprint.none") : blueprint.getLocalizedName();
        }
        if ("structure.name".equals(key) || "structure".equals(key) || "machine.name".equals(key)) {
            return found == null ? I18n.format("gui.controller.structure.none") : found.getLocalizedName();
        }
        if ("structure.formed".equals(key)) {
            return String.valueOf(controller.isStructureFormed());
        }
        if ("status.text".equals(key) || "status".equals(key)) {
            int redstone = controller.getWorld().getStrongPower(controller.getPos());
            if (redstone > 0) {
                return I18n.format("gui.controller.status.redstone_stopped");
            }
            return I18n.format(controller.getControllerStatus().getUnlocMessage());
        }
        if ("status.progress_percent".equals(key) || "progress.percent".equals(key)) {
            return String.valueOf(getProgressPercent(activeRecipe));
        }
        if ("status.progress_line".equals(key) || "progress.line".equals(key)) {
            return I18n.format("gui.controller.status.crafting.progress", getProgressPercent(activeRecipe) + "%");
        }
        if ("parallelism.current".equals(key) || "parallelism".equals(key)) {
            return String.valueOf(activeRecipe == null ? 1 : activeRecipe.getParallelism());
        }
        if ("parallelism.max".equals(key)) {
            return String.valueOf(activeRecipe == null ? 1 : activeRecipe.getMaxParallelism());
        }
        if ("parallelism.line".equals(key)) {
            int current = activeRecipe == null ? 1 : activeRecipe.getParallelism();
            return I18n.format("gui.controller.parallelism", current);
        }
        if ("parallelism.max_line".equals(key)) {
            int max = activeRecipe == null ? 1 : activeRecipe.getMaxParallelism();
            return I18n.format("gui.controller.max_parallelism", max);
        }
        if ("performance.avg_us".equals(key) || "perf.avg_us".equals(key) || "latency.avg_us".equals(key)) {
            return String.valueOf(TileMultiblockMachineController.usedTimeCache);
        }
        if ("performance.search_ms".equals(key) || "perf.search_ms".equals(key) || "latency.search_ms".equals(key)) {
            return MiscUtils.formatFloat(TileMultiblockMachineController.searchUsedTimeCache / 1000F, 2);
        }
        if ("performance.work_mode".equals(key) || "perf.work_mode".equals(key) || "work_mode".equals(key)) {
            return TileMultiblockMachineController.workModeCache.getDisplayName();
        }
        if ("performance.line".equals(key) || "perf.line".equals(key) || "latency.line".equals(key)) {
            return String.format(
                "Avg: %dus/t (Search: %sms), WorkMode: %s",
                TileMultiblockMachineController.usedTimeCache,
                MiscUtils.formatFloat(TileMultiblockMachineController.searchUsedTimeCache / 1000F, 2),
                TileMultiblockMachineController.workModeCache.getDisplayName()
            );
        }
        return rawKey == null ? "" : rawKey;
    }

    private static int getProgressPercent(@Nullable ActiveMachineRecipe activeRecipe) {
        if (activeRecipe == null || activeRecipe.getTotalTick() <= 0) {
            return 0;
        }
        return (activeRecipe.getTick() * 100) / activeRecipe.getTotalTick();
    }

    private String resolveInfoSectionPanel(String fallback, String... ids) {
        if (styleOverride.infoSections == null || styleOverride.infoSections.isEmpty()) {
            return fallback;
        }
        for (MachineGuiStyleManager.InfoSectionStyle section : styleOverride.infoSections) {
            if (section == null || section.panel == null || section.panel.trim().isEmpty()) {
                continue;
            }
            if (matchesInfoSection(section.id, ids)) {
                return section.panel.trim();
            }
        }
        return fallback;
    }

    @Nullable
    private Boolean resolveInfoSectionVisible(String... ids) {
        if (styleOverride.infoSections == null || styleOverride.infoSections.isEmpty()) {
            return null;
        }
        for (MachineGuiStyleManager.InfoSectionStyle section : styleOverride.infoSections) {
            if (section == null || section.visible == null) {
                continue;
            }
            if (matchesInfoSection(section.id, ids)) {
                return section.visible;
            }
        }
        return null;
    }

    private boolean matchesInfoSection(@Nullable String id, String... expected) {
        if (id == null) {
            return false;
        }
        String normalized = id.trim().toLowerCase(java.util.Locale.ROOT);
        for (String item : expected) {
            if (normalized.equals(item)) {
                return true;
            }
        }
        return false;
    }

    private int getPlayerInventoryTop(MMCEGuiExtConfig.MachineController cfg) {
        return getHidePlayerInventory(cfg) ? HIDDEN_PLAYER_INVENTORY_TOP : PLAYER_INVENTORY_TOP;
    }

    private boolean hasMachineStyleOverride() {
        return styleOverride != MachineGuiStyleManager.ControllerStyle.EMPTY;
    }

    private void initializeCurrentContext(
        @Nullable MachineGuiStyleManager.SubGuiStyle subGui,
        boolean modal,
        @Nullable RuntimeGuiState parentState,
        @Nullable String existingPage
    ) {
        MMCEGuiExtConfig.MachineController cfg = MMCEGuiExtConfig.machineController;
        this.customBackgroundTexture = resolveCustomTexture();
        resolveGuiScaleConfig();
        if (modal) {
            this.guiScaleMode = false;
            this.renderScale = 1.0F;
            this.renderOriginX = 0;
            this.renderOriginY = 0;
        }

        if (!modal) {
            setWidthHeight();
            this.guiLeft = (this.width - this.xSize) / 2;
            this.guiTop = (this.height - this.ySize) / 2;
        }

        if (subGui != null) {
            if (subGui.width != null) {
                this.renderWidth = Math.max(modal ? 24 : BASE_WIDTH, subGui.width.intValue());
            }
            if (subGui.height != null) {
                this.renderHeight = Math.max(modal ? 24 : BASE_HEIGHT, subGui.height.intValue());
            }
        }

        if (modal) {
            this.xSize = Math.max(1, this.renderWidth);
            this.ySize = Math.max(1, this.renderHeight);
        }

        if (modal) {
            int parentLeft = parentState == null ? this.guiLeft : parentState.guiLeft;
            int parentTop = parentState == null ? this.guiTop : parentState.guiTop;
            int offsetX = subGui != null && subGui.x != null ? subGui.x.intValue() : Math.max(0, (BASE_WIDTH - this.renderWidth) / 2);
            int offsetY = subGui != null && subGui.y != null ? subGui.y.intValue() : 8;
            this.guiLeft = parentLeft + offsetX;
            this.guiTop = parentTop + offsetY;
        } else if (this.guiScaleMode) {
            this.guiLeft = 0;
            this.guiTop = 0;
            computeGuiScale();
        } else {
            centerFullGuiIfNeeded(cfg);
        }

        this.panelScroll.clear();
        this.panelMaxScroll.clear();
        this.draggingPanelId = null;
        this.infoScrollbarDragOffset = 0;
        initTextureLayers(cfg);
        this.activePageId = resolveInitialPageId(cfg, existingPage);
        initSmartInterfaceEditor();
        initCustomSmartInterfaceEditors(cfg);
        initCustomSliders();
        initCustomButtons(cfg);
        if (!modal) {
            applyPlayerInventoryVisibility(cfg);
        }
    }

    private void updateCurrentContextScreen() {
        if (this.smartInterfaceEditorInput != null) {
            this.smartInterfaceEditorInput.updateCursorCounter();
        }
        for (CustomSmartEditor editor : this.customSmartEditors) {
            if (editor.input != null) {
                editor.input.updateCursorCounter();
            }
        }
    }

    private void drawActiveModalSubGui(int mouseX, int mouseY, float partialTicks) {
        if (!hasActiveModalSubGui()) {
            return;
        }
        final RuntimeGuiState modalState = getTopModalSubGui();
        drawGradientRect(0, 0, this.width, this.height, 0x60000000, 0x60000000);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        try {
            withRuntimeState(modalState, new Runnable() {
                @Override
                public void run() {
                    int localMouseX = mouseX - guiLeft;
                    int localMouseY = mouseY - guiTop;
                    drawGuiContainerBackgroundLayer(partialTicks, localMouseX, localMouseY);
                    drawGuiContainerForegroundLayer(localMouseX, localMouseY);
                }
            });
        } finally {
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
        }
    }

    private boolean handleTopModalMouseWheel() {
        final int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return false;
        }
        withRuntimeState(getTopModalSubGui(), new Runnable() {
            @Override
            public void run() {
                if (isUsingDefaultBackground(MMCEGuiExtConfig.machineController)) {
                    return;
                }
                int mouseX = Mouse.getEventX() * width / mc.displayWidth;
                int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
                String panelId = findHoveredPanelId(mouseX, mouseY, getActivePanels(MMCEGuiExtConfig.machineController));
                if (panelId == null) {
                    return;
                }
                int step = Math.max(2, MMCEGuiExtConfig.wheelStep);
                int delta = wheel > 0 ? -step : step;
                int newScroll = getPanelScroll(panelId) + delta;
                int max = getPanelMaxScroll(panelId);
                panelScroll.put(panelId, MathHelper.clamp(newScroll, 0, max));
            }
        });
        return true;
    }

    private boolean handleTopModalMouseClicked(final int mouseX, final int mouseY, final int mouseButton) {
        final boolean[] handled = new boolean[] {true};
        withRuntimeState(getTopModalSubGui(), new Runnable() {
            @Override
            public void run() {
                if (handleCustomButtonMouseClicked(mouseX, mouseY, mouseButton)) {
                    return;
                }
                if (handleCustomSmartInterfaceMouseClicked(mouseX, mouseY, mouseButton)) {
                    return;
                }
                if (handleSmartInterfaceMouseClicked(mouseX, mouseY, mouseButton)) {
                    return;
                }
                if (!isUsingDefaultBackground(MMCEGuiExtConfig.machineController) && mouseButton == 0) {
                    int localX = mouseX - guiLeft;
                    int localY = mouseY - guiTop;
                    for (PanelDef panel : getActivePanels(MMCEGuiExtConfig.machineController)) {
                        if (isOnPanelThumb(localX, localY, panel)) {
                            draggingPanelId = panel.id;
                            infoScrollbarDragOffset = localY - getPanelThumbY(panel.id, panel.rect);
                            return;
                        }
                    }
                }
                if (mouseButton == 0 && isModalSubGuiDragEnabled() && isMouseInModalSubGuiDragHandle(mouseX, mouseY)) {
                    startModalSubGuiDrag(mouseX, mouseY);
                    return;
                }
            }
        });
        return handled[0];
    }

    private boolean handleTopModalKeyTyped(final char typedChar, final int keyCode) {
        final boolean[] handled = new boolean[] {false};
        withRuntimeState(getTopModalSubGui(), new Runnable() {
            @Override
            public void run() {
                if (SubGuiBackKeyPolicy.isBackKey(keyCode)) {
                    handled[0] = closeActiveModalSubGui();
                    return;
                }
                if (handleCustomButtonKeyTyped(typedChar, keyCode)
                    || handleCustomSmartInterfaceKeyTyped(typedChar, keyCode)
                    || handleSmartInterfaceKeyTyped(typedChar, keyCode)) {
                    handled[0] = true;
                    return;
                }
            }
        });
        return handled[0];
    }

    private void handleTopModalMouseReleased(final int mouseX, final int mouseY, final int state) {
        withRuntimeState(getTopModalSubGui(), new Runnable() {
            @Override
            public void run() {
                modalSubGuiDragging = false;
                draggingPanelId = null;
                draggingSlider = null;
            }
        });
        this.draggingSlider = null;
    }

    private void handleTopModalMouseClickMove(final int mouseX, final int mouseY, final int clickedMouseButton, final long timeSinceLastClick) {
        withRuntimeState(getTopModalSubGui(), new Runnable() {
            @Override
            public void run() {
                if (clickedMouseButton == 0 && modalSubGuiDragging) {
                    updateModalSubGuiPosition(mouseX - modalSubGuiDragOffsetX, mouseY - modalSubGuiDragOffsetY);
                    return;
                }
                if (clickedMouseButton == 0 && draggingSlider != null) {
                    updateSliderFromMouse(draggingSlider, mouseX, mouseY, false);
                    return;
                }
                if (isUsingDefaultBackground(MMCEGuiExtConfig.machineController)) {
                    return;
                }
                if (clickedMouseButton == 0 && draggingPanelId != null) {
                    PanelDef panel = findPanelById(draggingPanelId, getActivePanels(MMCEGuiExtConfig.machineController));
                    if (panel != null) {
                        int localY = mouseY - guiTop;
                        updatePanelScrollFromMouse(panel.id, panel.rect, localY);
                    }
                }
            }
        });
    }

    private void updateModalSubGuiPosition(int left, int top) {
        int deltaX = left - this.guiLeft;
        int deltaY = top - this.guiTop;
        if (deltaX == 0 && deltaY == 0) {
            return;
        }
        this.guiLeft = left;
        this.guiTop = top;
        moveCurrentGuiElementsBy(deltaX, deltaY);
        if (this.currentRuntimeState != null && this.currentRuntimeState.modal) {
            this.currentRuntimeState.guiLeft = left;
            this.currentRuntimeState.guiTop = top;
        }
        this.modalSubGuiDragging = true;
    }

    private void moveCurrentGuiElementsBy(int deltaX, int deltaY) {
        moveTextField(this.smartInterfaceEditorInput, deltaX, deltaY);
        moveButton(this.smartInterfacePrevButton, deltaX, deltaY);
        moveButton(this.smartInterfaceNextButton, deltaX, deltaY);
        moveButton(this.smartInterfaceApplyButton, deltaX, deltaY);
        for (CustomButton button : this.customButtons) {
            if (button != null) {
                moveButton(button.button, deltaX, deltaY);
            }
        }
        for (CustomSmartEditor editor : this.customSmartEditors) {
            if (editor != null) {
                moveTextField(editor.input, deltaX, deltaY);
                moveButton(editor.prev, deltaX, deltaY);
                moveButton(editor.next, deltaX, deltaY);
                moveButton(editor.apply, deltaX, deltaY);
            }
        }
    }

    private static void moveTextField(@Nullable GuiTextField textField, int deltaX, int deltaY) {
        if (textField == null) {
            return;
        }
        textField.x += deltaX;
        textField.y += deltaY;
    }

    private static void moveButton(@Nullable GuiButton button, int deltaX, int deltaY) {
        if (button == null) {
            return;
        }
        button.x += deltaX;
        button.y += deltaY;
    }

    private boolean isModalSubGuiDragEnabled() {
        RuntimeGuiState state = getTopModalSubGui();
        return Boolean.TRUE.equals(state.modalSubGuiDraggable);
    }

    private boolean isMouseInModalSubGuiDragHandle(int mouseX, int mouseY) {
        RuntimeGuiState state = getTopModalSubGui();
        if (!state.getBounds().contains(mouseX, mouseY)) {
            return false;
        }
        if (Boolean.FALSE.equals(state.modalSubGuiDragHandle)) {
            return true;
        }
        int handleLeft = this.guiLeft + state.modalSubGuiDragX;
        int handleTop = this.guiTop + state.modalSubGuiDragY;
        int handleWidth = state.modalSubGuiDragWidth > 0 ? state.modalSubGuiDragWidth : Math.max(1, this.renderWidth);
        int handleHeight = state.modalSubGuiDragHeight > 0 ? state.modalSubGuiDragHeight : Math.min(16, Math.max(1, this.renderHeight));
        return mouseX >= handleLeft && mouseX < handleLeft + handleWidth && mouseY >= handleTop && mouseY < handleTop + handleHeight;
    }

    private void startModalSubGuiDrag(int mouseX, int mouseY) {
        this.modalSubGuiDragging = true;
        this.modalSubGuiDragOffsetX = mouseX - this.guiLeft;
        this.modalSubGuiDragOffsetY = mouseY - this.guiTop;
    }

    private void openSubGui(@Nullable String targetSubGui, @Nullable String requestedMode) {
        MachineGuiStyleManager.SubGuiStyle subGui = findSubGuiStyle(targetSubGui);
        if (subGui == null) {
            return;
        }
        String mode = normalizeSubGuiOpenMode(requestedMode);
        if ("replace".equals(mode)) {
            storeCurrentRuntimeState();
            RuntimeGuiState previous = this.currentRuntimeState;
            if (previous != null) {
                this.replaceSubGuiStack.add(previous);
            }
            RuntimeGuiState nextState = buildRuntimeState(subGui, false, previous, null);
            applyRuntimeState(nextState);
            this.currentRuntimeState = nextState;
            return;
        }
        if (hasActiveModalSubGui()) {
            storeRuntimeState(getTopModalSubGui());
        } else {
            storeCurrentRuntimeState();
        }
        RuntimeGuiState parent = hasActiveModalSubGui() ? getTopModalSubGui() : this.currentRuntimeState;
        this.modalSubGuiStack.add(buildRuntimeState(subGui, true, parent, null));
    }

    private void closeActiveSubGui() {
        if (closeActiveModalSubGui()) {
            return;
        }
        if (this.replaceSubGuiStack.isEmpty()) {
            return;
        }
        RuntimeGuiState previous = this.replaceSubGuiStack.remove(this.replaceSubGuiStack.size() - 1);
        applyRuntimeState(previous);
        this.currentRuntimeState = previous;
    }

    private boolean closeActiveModalSubGui() {
        if (this.modalSubGuiStack.isEmpty()) {
            return false;
        }
        this.modalSubGuiStack.remove(this.modalSubGuiStack.size() - 1);
        return true;
    }

    @Nullable
    private MachineGuiStyleManager.SubGuiStyle findSubGuiStyle(@Nullable String targetSubGui) {
        String targetId = normalizeSubGuiId(targetSubGui);
        if (targetId.isEmpty() || this.baseStyleOverride.subGuis == null) {
            return null;
        }
        for (MachineGuiStyleManager.SubGuiStyle subGui : this.baseStyleOverride.subGuis) {
            if (subGui == null) {
                continue;
            }
            if (normalizeSubGuiId(subGui.id).equals(targetId)) {
                return subGui;
            }
        }
        return null;
    }

    private RuntimeGuiState buildRuntimeState(
        MachineGuiStyleManager.SubGuiStyle subGui,
        boolean modal,
        @Nullable RuntimeGuiState parentState,
        @Nullable String existingPage
    ) {
        RuntimeGuiState previous = captureRuntimeState(null, false);
        MachineGuiStyleManager.ControllerStyle nextStyle = subGui.style == null
            ? new MachineGuiStyleManager.ControllerStyle()
            : MachineGuiStyleManager.ControllerStyle.copyOf(subGui.style);
        if ((nextStyle.subGuis == null || nextStyle.subGuis.isEmpty()) && this.baseStyleOverride.subGuis != null) {
            nextStyle.subGuis = this.baseStyleOverride.subGuis;
        }
        this.styleOverride = nextStyle;
        initializeCurrentContext(subGui, modal, parentState, existingPage);
        this.modalSubGuiDraggable = subGui.draggable;
        this.modalSubGuiDragHandle = Boolean.valueOf(subGui.dragHandle == null || subGui.dragHandle.booleanValue());
        this.modalSubGuiDragX = subGui.dragX == null ? 0 : subGui.dragX.intValue();
        this.modalSubGuiDragY = subGui.dragY == null ? 0 : subGui.dragY.intValue();
        this.modalSubGuiDragWidth = subGui.dragWidth == null ? 0 : subGui.dragWidth.intValue();
        this.modalSubGuiDragHeight = subGui.dragHeight == null ? 0 : subGui.dragHeight.intValue();
        RuntimeGuiState built = captureRuntimeState(normalizeSubGuiId(subGui.id), modal);
        applyRuntimeState(previous);
        return built;
    }

    private void withRuntimeState(RuntimeGuiState state, Runnable action) {
        RuntimeGuiState previous = captureRuntimeState(null, false);
        this.subGuiContextSwitching = true;
        applyRuntimeState(state);
        try {
            action.run();
            storeRuntimeState(state);
        } finally {
            applyRuntimeState(previous);
            this.subGuiContextSwitching = false;
        }
    }

    private void storeCurrentRuntimeState() {
        if (this.currentRuntimeState != null) {
            storeRuntimeState(this.currentRuntimeState);
        }
    }

    private void storeRuntimeState(RuntimeGuiState state) {
        state.styleOverride = this.styleOverride;
        state.customBackgroundTexture = this.customBackgroundTexture;
        state.renderWidth = this.renderWidth;
        state.renderHeight = this.renderHeight;
        state.guiScaleMode = this.guiScaleMode;
        state.logicalWidth = this.logicalWidth;
        state.logicalHeight = this.logicalHeight;
        state.renderScale = this.renderScale;
        state.renderOriginX = this.renderOriginX;
        state.renderOriginY = this.renderOriginY;
        state.panelScroll = new HashMap<String, Integer>(this.panelScroll);
        state.panelMaxScroll = new HashMap<String, Integer>(this.panelMaxScroll);
        state.draggingPanelId = this.draggingPanelId;
        state.infoScrollbarDragOffset = this.infoScrollbarDragOffset;
        state.modalSubGuiDraggable = this.modalSubGuiDraggable;
        state.modalSubGuiDragHandle = this.modalSubGuiDragHandle;
        state.modalSubGuiDragging = this.modalSubGuiDragging;
        state.modalSubGuiDragOffsetX = this.modalSubGuiDragOffsetX;
        state.modalSubGuiDragOffsetY = this.modalSubGuiDragOffsetY;
        state.modalSubGuiDragX = this.modalSubGuiDragX;
        state.modalSubGuiDragY = this.modalSubGuiDragY;
        state.modalSubGuiDragWidth = this.modalSubGuiDragWidth;
        state.modalSubGuiDragHeight = this.modalSubGuiDragHeight;
        state.draggingSlider = this.draggingSlider;
        state.smartInterfaceEditorInput = this.smartInterfaceEditorInput;
        state.smartInterfacePrevButton = this.smartInterfacePrevButton;
        state.smartInterfaceNextButton = this.smartInterfaceNextButton;
        state.smartInterfaceApplyButton = this.smartInterfaceApplyButton;
        state.smartInterfaceIndex = this.smartInterfaceIndex;
        state.smartInterfaceEditorX = this.smartInterfaceEditorX;
        state.smartInterfaceEditorY = this.smartInterfaceEditorY;
        state.smartInterfaceEditorPriority = this.smartInterfaceEditorPriority;
        state.smartInterfaceVirtualInputCache = new HashMap<String, String>(this.smartInterfaceVirtualInputCache);
        state.smartInterfaceActiveVirtualKey = this.smartInterfaceActiveVirtualKey;
        state.smartInterfaceHideInfoText = this.smartInterfaceHideInfoText;
        state.smartInterfaceHideTitleText = this.smartInterfaceHideTitleText;
        state.smartInterfaceCustomTitleText = this.smartInterfaceCustomTitleText;
        state.hideDefaultSmartInterfaceEditor = this.hideDefaultSmartInterfaceEditor;
        state.customSmartEditors = new ArrayList<CustomSmartEditor>(this.customSmartEditors);
        state.customButtons = new ArrayList<CustomButton>(this.customButtons);
        state.customSliders = new ArrayList<CustomSlider>(this.customSliders);
        state.backgroundTextureLayers = new ArrayList<TextureLayerDef>(this.backgroundTextureLayers);
        state.foregroundTextureLayers = new ArrayList<TextureLayerDef>(this.foregroundTextureLayers);
        state.layerRuntimeStates = new HashMap<String, LayerRuntimeState>(this.layerRuntimeStates);
        state.textureLayerIds = new HashSet<String>(this.textureLayerIds);
        state.activePageId = this.activePageId;
        state.guiLeft = this.guiLeft;
        state.guiTop = this.guiTop;
        state.xSize = this.xSize;
        state.ySize = this.ySize;
    }

    private RuntimeGuiState captureRuntimeState(@Nullable String subGuiId, boolean modal) {
        RuntimeGuiState state = new RuntimeGuiState();
        state.subGuiId = subGuiId;
        state.modal = modal;
        storeRuntimeState(state);
        return state;
    }

    private void applyRuntimeState(RuntimeGuiState state) {
        this.styleOverride = state.styleOverride;
        this.customBackgroundTexture = state.customBackgroundTexture;
        this.renderWidth = state.renderWidth;
        this.renderHeight = state.renderHeight;
        this.guiScaleMode = state.guiScaleMode;
        this.logicalWidth = state.logicalWidth;
        this.logicalHeight = state.logicalHeight;
        this.renderScale = state.renderScale;
        this.renderOriginX = state.renderOriginX;
        this.renderOriginY = state.renderOriginY;
        this.draggingPanelId = state.draggingPanelId;
        this.infoScrollbarDragOffset = state.infoScrollbarDragOffset;
        this.modalSubGuiDraggable = state.modalSubGuiDraggable;
        this.modalSubGuiDragHandle = state.modalSubGuiDragHandle;
        this.modalSubGuiDragging = state.modalSubGuiDragging;
        this.modalSubGuiDragOffsetX = state.modalSubGuiDragOffsetX;
        this.modalSubGuiDragOffsetY = state.modalSubGuiDragOffsetY;
        this.modalSubGuiDragX = state.modalSubGuiDragX;
        this.modalSubGuiDragY = state.modalSubGuiDragY;
        this.modalSubGuiDragWidth = state.modalSubGuiDragWidth;
        this.modalSubGuiDragHeight = state.modalSubGuiDragHeight;
        this.draggingSlider = state.draggingSlider;
        this.smartInterfaceEditorInput = state.smartInterfaceEditorInput;
        this.smartInterfacePrevButton = state.smartInterfacePrevButton;
        this.smartInterfaceNextButton = state.smartInterfaceNextButton;
        this.smartInterfaceApplyButton = state.smartInterfaceApplyButton;
        this.smartInterfaceIndex = state.smartInterfaceIndex;
        this.smartInterfaceEditorX = state.smartInterfaceEditorX;
        this.smartInterfaceEditorY = state.smartInterfaceEditorY;
        this.smartInterfaceEditorPriority = state.smartInterfaceEditorPriority;
        this.smartInterfaceActiveVirtualKey = state.smartInterfaceActiveVirtualKey;
        this.smartInterfaceHideInfoText = state.smartInterfaceHideInfoText;
        this.smartInterfaceHideTitleText = state.smartInterfaceHideTitleText;
        this.smartInterfaceCustomTitleText = state.smartInterfaceCustomTitleText;
        this.hideDefaultSmartInterfaceEditor = state.hideDefaultSmartInterfaceEditor;
        this.activePageId = state.activePageId;
        this.guiLeft = state.guiLeft;
        this.guiTop = state.guiTop;
        this.xSize = state.xSize;
        this.ySize = state.ySize;

        this.panelScroll.clear();
        this.panelScroll.putAll(state.panelScroll);
        this.panelMaxScroll.clear();
        this.panelMaxScroll.putAll(state.panelMaxScroll);
        this.smartInterfaceVirtualInputCache.clear();
        this.smartInterfaceVirtualInputCache.putAll(state.smartInterfaceVirtualInputCache);
        this.customSmartEditors.clear();
        this.customSmartEditors.addAll(state.customSmartEditors);
        this.customButtons.clear();
        this.customButtons.addAll(state.customButtons);
        this.customSliders.clear();
        this.customSliders.addAll(state.customSliders);
        this.backgroundTextureLayers.clear();
        this.backgroundTextureLayers.addAll(state.backgroundTextureLayers);
        this.foregroundTextureLayers.clear();
        this.foregroundTextureLayers.addAll(state.foregroundTextureLayers);
        this.layerRuntimeStates.clear();
        this.layerRuntimeStates.putAll(state.layerRuntimeStates);
        this.textureLayerIds.clear();
        this.textureLayerIds.addAll(state.textureLayerIds);
    }

    private boolean hasActiveModalSubGui() {
        return !this.modalSubGuiStack.isEmpty();
    }

    private boolean hasReplaceSubGui() {
        if (this.currentRuntimeState == null || this.replaceSubGuiStack.isEmpty()) {
            return false;
        }
        RuntimeGuiState current = this.currentRuntimeState;
        RuntimeGuiState top = this.replaceSubGuiStack.get(this.replaceSubGuiStack.size() - 1);
        return current != top;
    }

    private RuntimeGuiState getTopModalSubGui() {
        return this.modalSubGuiStack.get(this.modalSubGuiStack.size() - 1);
    }

    private String normalizeSubGuiId(@Nullable String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeSubGuiOpenMode(@Nullable String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return "modal".equals(normalized) ? "modal" : "replace";
    }

    @Nullable
    public Rectangle getJeiRightExtensionArea() {
        if (this.guiScaleMode) {
            int extra = this.renderWidth - BASE_WIDTH;
            if (extra <= 0) {
                return null;
            }
            return new Rectangle(
                Math.round(this.renderOriginX + BASE_WIDTH * this.renderScale),
                this.renderOriginY,
                Math.round(extra * this.renderScale),
                Math.round(this.renderHeight * this.renderScale)
            );
        }
        if (getDisableRightExtension()) {
            return null;
        }
        int extraWidth = this.renderWidth - BASE_WIDTH;
        if (extraWidth <= 0) {
            return null;
        }
        return new Rectangle(this.guiLeft + BASE_WIDTH, this.guiTop, extraWidth, this.renderHeight);
    }

    private void initTextureLayers(MMCEGuiExtConfig.MachineController cfg) {
        this.backgroundTextureLayers.clear();
        this.foregroundTextureLayers.clear();
        this.layerRuntimeStates.clear();
        this.textureLayerIds.clear();
        if (styleOverride.textureLayers == null || styleOverride.textureLayers.isEmpty()) {
            return;
        }
        int bgIndex = 0;
        int fgIndex = 0;
        for (MachineGuiStyleManager.TextureLayerStyle layer : styleOverride.textureLayers) {
            if (layer == null || layer.texture == null || layer.texture.trim().isEmpty()) {
                continue;
            }
            ResourceLocation texture = GuiRenderUtils.parseOptionalTexture(layer.texture);
            if (texture == null) {
                continue;
            }
            TextureLayerDef def = new TextureLayerDef();
            def.texture = texture;
            def.offsetX = layer.offsetX == null ? 0 : layer.offsetX.intValue();
            def.offsetY = layer.offsetY == null ? 0 : layer.offsetY.intValue();
            def.width = layer.width;
            def.height = layer.height;
            def.textureWidth = layer.textureWidth;
            def.textureHeight = layer.textureHeight;
            def.corner = layer.corner;
            def.useNineSlice = layer.useNineSlice;
            def.priority = layer.priority == null ? DEFAULT_RENDER_PRIORITY : layer.priority.intValue();
            def.alpha = layer.alpha == null ? 1.0F : normalizeLayerAlpha(layer.alpha.floatValue());
            def.page = normalizePageIdOrNull(layer.page);
            boolean foreground = layer.foreground != null && layer.foreground.booleanValue();
            String defaultId = foreground ? "fg_" + fgIndex++ : "bg_" + bgIndex++;
            String requestedId = layer.id == null ? defaultId : layer.id.trim();
            if (requestedId.isEmpty()) {
                requestedId = defaultId;
            }
            def.id = allocateUniqueLayerId(requestedId);
            this.textureLayerIds.add(def.id);
            if (foreground) {
                this.foregroundTextureLayers.add(def);
            } else {
                this.backgroundTextureLayers.add(def);
            }
        }
    }

    private void drawConfiguredTextureLayers(boolean foreground, MMCEGuiExtConfig.MachineController cfg) {
        drawConfiguredTextureLayers(foreground, cfg, null);
    }

    private void drawConfiguredTextureLayers(boolean foreground, MMCEGuiExtConfig.MachineController cfg, @Nullable Integer priorityFilter) {
        List<TextureLayerDef> layers = foreground ? this.foregroundTextureLayers : this.backgroundTextureLayers;
        if (layers.isEmpty()) {
            return;
        }
        List<TextureLayerDef> ordered = new ArrayList<TextureLayerDef>(layers);
        ordered.sort((a, b) -> Integer.compare(resolveLayerPriority(a), resolveLayerPriority(b)));
        for (TextureLayerDef layer : ordered) {
            int renderPriority = resolveLayerPriority(layer);
            if (priorityFilter != null && renderPriority != priorityFilter.intValue()) {
                continue;
            }
            if (!isLayerVisible(layer)) {
                continue;
            }
            if (!isPageVisible(layer.page)) {
                continue;
            }
            this.mc.getTextureManager().bindTexture(layer.texture);
            int offX = resolveLayerOffsetX(layer);
            int offY = resolveLayerOffsetY(layer);
            int width = layer.width == null ? this.renderWidth : Math.max(1, layer.width.intValue());
            int height = layer.height == null ? this.renderHeight : Math.max(1, layer.height.intValue());
            int texW = layer.textureWidth == null ? getBackgroundTextureWidth(cfg) : Math.max(16, layer.textureWidth.intValue());
            int texH = layer.textureHeight == null ? getBackgroundTextureHeight(cfg) : Math.max(16, layer.textureHeight.intValue());
            int corner = layer.corner == null ? getBackgroundCorner(cfg) : Math.max(2, layer.corner.intValue());
            boolean useNineSlice = layer.useNineSlice == null ? getUseNineSlice(cfg) : layer.useNineSlice.booleanValue();
            int drawX = foreground ? offX : this.guiLeft + offX;
            int drawY = foreground ? offY : this.guiTop + offY;
            float scaleX = resolveLayerScaleX(layer);
            float scaleY = resolveLayerScaleY(layer);
            float rotation = resolveLayerRotation(layer);
            float alpha = resolveLayerAlpha(layer);
            GlStateManager.pushMatrix();
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
            drawLayerWithTransform(drawX, drawY, width, height, useNineSlice, texW, texH, corner, scaleX, scaleY, rotation);
            GlStateManager.popMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            if (alpha < 1.0F) {
                GlStateManager.disableBlend();
            }
        }
    }

    private void resetForegroundRenderState() {
        GuiRenderUtils.disableScissor();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void initCustomSmartInterfaceEditors(MMCEGuiExtConfig.MachineController cfg) {
        this.customSmartEditors.clear();
        this.hideDefaultSmartInterfaceEditor = styleOverride.hideDefaultSmartInterfaceEditor != null
            && styleOverride.hideDefaultSmartInterfaceEditor.booleanValue();
        if (styleOverride.smartInterfaceEditors == null || styleOverride.smartInterfaceEditors.isEmpty()) {
            return;
        }

        int buttonId = CUSTOM_EDITOR_BUTTON_ID_BASE;
        for (MachineGuiStyleManager.SmartInterfaceEditorStyle style : styleOverride.smartInterfaceEditors) {
            List<String> keys = parseVirtualKeys(style.virtualKey);
            if (keys.isEmpty()) {
                continue;
            }
            CustomSmartEditor editor = new CustomSmartEditor();
            editor.id = style.id == null ? "editor_" + this.customSmartEditors.size() : style.id.trim();
            editor.keys = keys;
            editor.index = 0;
            editor.showTitle = style.showTitle == null || style.showTitle.booleanValue();
            editor.showInfo = style.showInfo == null || style.showInfo.booleanValue();
            editor.showControls = style.showControls == null || style.showControls.booleanValue();
            editor.inputBackground = style.inputBackground == null || style.inputBackground.booleanValue();
            editor.title = style.title;
            editor.priority = style.priority == null ? DEFAULT_SMART_EDITOR_PRIORITY : style.priority.intValue();
            editor.page = normalizePageIdOrNull(style.page);

            int inputWidth = style.inputWidth == null ? getSmartInterfaceEditorInputWidth(cfg) : Math.max(4, style.inputWidth.intValue());
            int editorX = MathHelper.clamp(style.x, 2, Math.max(2, this.renderWidth - 2));
            int editorY = MathHelper.clamp(style.y, 12, Math.max(12, this.renderHeight - SMART_EDITOR_INPUT_H - 2));
            int absX = this.guiLeft + editorX;
            int absY = this.guiTop + editorY;

            editor.x = editorX;
            editor.y = editorY;
            editor.inputWidth = inputWidth;
            int leftControls = editor.showControls ? (SMART_EDITOR_BUTTON_W * 2 + 3) : 0;
            editor.input = new GuiTextField(0, this.fontRenderer, absX + leftControls, absY, inputWidth, SMART_EDITOR_INPUT_H);
            editor.input.setEnableBackgroundDrawing(editor.inputBackground);
            editor.input.setMaxStringLength(1024);

            if (editor.showControls) {
                editor.prev = new GuiButton(buttonId++, absX, absY, SMART_EDITOR_BUTTON_W, SMART_EDITOR_BUTTON_H, "<");
                editor.next = new GuiButton(buttonId++, absX + SMART_EDITOR_BUTTON_W + 1, absY, SMART_EDITOR_BUTTON_W, SMART_EDITOR_BUTTON_H, ">");
                editor.apply = new GuiButton(
                    buttonId++,
                    absX + leftControls + inputWidth + 2,
                    absY,
                    SMART_EDITOR_APPLY_W,
                    SMART_EDITOR_BUTTON_H,
                    "OK"
                );
            }
            syncCustomSmartEditorInput(editor);
            this.customSmartEditors.add(editor);
        }
    }

    private List<String> parseVirtualKeys(@Nullable String raw) {
        List<String> keys = new ArrayList<String>();
        if (raw == null) {
            return keys;
        }
        String normalized = raw.replace('\r', ',')
            .replace('\n', ',')
            .replace(';', ',')
            .replace('\uFF0C', ',')
            .replace('\uFF1B', ',');
        for (String split : normalized.split(",")) {
            String key = split == null ? "" : split.trim();
            if (key.isEmpty() || keys.contains(key)) {
                continue;
            }
            keys.add(key);
        }
        return keys;
    }

    private void initSmartInterfaceEditor() {
        MMCEGuiExtConfig.MachineController cfg = MMCEGuiExtConfig.machineController;
        this.smartInterfaceEditorPriority = resolveSmartInterfaceEditorPriority();
        this.hideDefaultSmartInterfaceEditor = styleOverride.hideDefaultSmartInterfaceEditor != null
            && styleOverride.hideDefaultSmartInterfaceEditor.booleanValue();
        if (!shouldRenderSmartInterfaceEditor(cfg)) {
            this.smartInterfaceEditorInput = null;
            this.smartInterfacePrevButton = null;
            this.smartInterfaceNextButton = null;
            this.smartInterfaceApplyButton = null;
            return;
        }

        int inputWidth = getSmartInterfaceEditorInputWidth(cfg);
        int totalWidth = SMART_EDITOR_BUTTON_W + 1 + SMART_EDITOR_BUTTON_W + 2 + inputWidth + 2 + SMART_EDITOR_APPLY_W;
        this.smartInterfaceEditorX = resolveSmartInterfaceEditorX(cfg, totalWidth);
        this.smartInterfaceEditorY = resolveSmartInterfaceEditorY(cfg);

        int absX = this.guiLeft + this.smartInterfaceEditorX;
        int absY = this.guiTop + this.smartInterfaceEditorY;
        this.smartInterfaceEditorInput = new GuiTextField(0, this.fontRenderer, absX + SMART_EDITOR_BUTTON_W * 2 + 3, absY, inputWidth, SMART_EDITOR_INPUT_H);
        this.smartInterfaceEditorInput.setMaxStringLength(1024);

        this.smartInterfacePrevButton = new GuiButton(101, absX, absY, SMART_EDITOR_BUTTON_W, SMART_EDITOR_BUTTON_H, "<");
        this.smartInterfaceNextButton = new GuiButton(102, absX + SMART_EDITOR_BUTTON_W + 1, absY, SMART_EDITOR_BUTTON_W, SMART_EDITOR_BUTTON_H, ">");
        this.smartInterfaceApplyButton = new GuiButton(
            103,
            absX + SMART_EDITOR_BUTTON_W * 2 + 5 + inputWidth,
            absY,
            SMART_EDITOR_APPLY_W,
            SMART_EDITOR_BUTTON_H,
            "OK"
        );
        syncSmartInterfaceEditorInput();
    }

    private boolean shouldRenderSmartInterfaceEditor(MMCEGuiExtConfig.MachineController cfg) {
        if (this.hideDefaultSmartInterfaceEditor) {
            return false;
        }
        if (!getSmartInterfaceEditorEnabled(cfg)) {
            return false;
        }
        int configuredX = getSmartInterfaceEditorX(cfg);
        int configuredY = getSmartInterfaceEditorY(cfg);
        if (configuredX >= 0 || configuredY >= 0) {
            return true;
        }
        return this.renderWidth > BASE_WIDTH;
    }

    private int resolveSmartInterfaceEditorPriority() {
        if (styleOverride.smartInterfaceEditorPriority != null) {
            return styleOverride.smartInterfaceEditorPriority.intValue();
        }
        return DEFAULT_SMART_EDITOR_PRIORITY;
    }

    private int resolveForegroundContentPriority() {
        if (styleOverride.foregroundContentPriority != null) {
            return styleOverride.foregroundContentPriority.intValue();
        }
        return DEFAULT_RENDER_PRIORITY;
    }

    private SmartInterfaceData[] getSmartInterfaceDataList() {
        if (this.controller == null || !this.controller.isStructureFormed()) {
            return new SmartInterfaceData[0];
        }
        SmartInterfaceData[] list = this.controller.getSmartInterfaceDataList();
        if (list == null) {
            return new SmartInterfaceData[0];
        }
        return list;
    }

    private void drawSmartInterfaceEditorBackground(int mouseX, int mouseY) {
        if (this.smartInterfaceEditorInput == null || this.smartInterfacePrevButton == null
            || this.smartInterfaceNextButton == null || this.smartInterfaceApplyButton == null) {
            return;
        }

        MMCEGuiExtConfig.MachineController cfg = MMCEGuiExtConfig.machineController;
        SmartInterfaceData[] dataList = getSmartInterfaceDataList();
        List<String> virtualKeys = getSmartInterfaceEditorVirtualKeys(cfg);
        boolean useVirtualKeys = !virtualKeys.isEmpty();
        boolean hasData = !useVirtualKeys && dataList.length > 0;
        int selectableCount = hasData ? dataList.length : virtualKeys.size();
        if (selectableCount > 0) {
            clampSmartInterfaceIndex(selectableCount);
        }

        this.smartInterfacePrevButton.enabled = selectableCount > 1;
        this.smartInterfaceNextButton.enabled = selectableCount > 1;
        this.smartInterfaceApplyButton.enabled = hasData || useVirtualKeys;

        this.smartInterfaceEditorInput.drawTextBox();
        this.smartInterfacePrevButton.drawButton(this.mc, mouseX, mouseY, 0F);
        this.smartInterfaceNextButton.drawButton(this.mc, mouseX, mouseY, 0F);
        this.smartInterfaceApplyButton.drawButton(this.mc, mouseX, mouseY, 0F);
    }

    private void drawSmartInterfaceEditorForeground() {
        if (this.smartInterfaceEditorInput == null) {
            return;
        }

        MMCEGuiExtConfig.MachineController cfg = MMCEGuiExtConfig.machineController;
        SmartInterfaceData[] dataList = getSmartInterfaceDataList();
        List<String> virtualKeys = getSmartInterfaceEditorVirtualKeys(cfg);
        boolean useVirtualKeys = !virtualKeys.isEmpty();
        boolean hasData = !useVirtualKeys && dataList.length > 0;
        int selectableCount = hasData ? dataList.length : virtualKeys.size();
        if (selectableCount > 0) {
            clampSmartInterfaceIndex(selectableCount);
        }

        SmartInterfaceData current = hasData ? dataList[this.smartInterfaceIndex] : null;
        String virtualKey = useVirtualKeys ? getSelectedSmartInterfaceVirtualKey(virtualKeys) : "";
        int currentIndexDisplay = selectableCount <= 0 ? 0 : (this.smartInterfaceIndex + 1);

        String title = resolveSmartInterfaceEditorTitle(hasData, selectableCount, currentIndexDisplay, virtualKey);
        if (!this.smartInterfaceHideTitleText) {
            this.fontRenderer.drawStringWithShadow(title, this.smartInterfaceEditorX, this.smartInterfaceEditorY - 10, 0xE0E0E0);
        }

        if (!this.smartInterfaceHideInfoText) {
            String infoText;
            int infoColor;
            if (hasData && current != null) {
                infoText = buildSmartInterfaceValueInfo(current);
                infoColor = 0xBFD3FF;
            } else if (!virtualKey.isEmpty()) {
                infoText = buildVirtualSmartInterfaceValueInfo(virtualKey);
                infoColor = 0xBFD3FF;
            } else {
                infoText = "No DataPort bound";
                infoColor = 0xB0B0B0;
            }
            this.fontRenderer.drawStringWithShadow(
                infoText,
                this.smartInterfaceEditorX,
                this.smartInterfaceEditorY + SMART_EDITOR_INPUT_H + 2,
                infoColor
            );
        }
    }

    private String buildSmartInterfaceValueInfo(SmartInterfaceData data) {
        DynamicMachine machine = resolveMachine();
        if (machine == null) {
            return data.getType() + ": " + data.getValue();
        }
        SmartInterfaceType type = machine.getSmartInterfaceType(data.getType());
        if (type == null) {
            return data.getType() + ": " + data.getValue();
        }

        String valueInfo;
        try {
            valueInfo = type.getValueInfo().isEmpty()
                        ? I18n.format("gui.smartinterface.value", data.getValue())
                        : String.format(type.getValueInfo(), data.getValue());
        } catch (IllegalFormatException ignored) {
            valueInfo = I18n.format("gui.smartinterface.value", data.getValue());
        }
        return data.getType() + " | " + valueInfo;
    }

    private String buildVirtualSmartInterfaceValueInfo(String key) {
        Float numeric = ControllerCustomDataAccess.readNumber(this.controller, key);
        if (numeric != null) {
            return key + " | " + I18n.format("gui.smartinterface.value", numeric.floatValue());
        }
        String text = ControllerCustomDataAccess.readString(this.controller, key);
        if (text != null) {
            return key + " | " + text;
        }
        return "Key: " + key;
    }

    private boolean handleSmartInterfaceMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return false;
        }
        if (this.smartInterfaceEditorInput == null || this.smartInterfacePrevButton == null
            || this.smartInterfaceNextButton == null || this.smartInterfaceApplyButton == null) {
            return false;
        }

        MMCEGuiExtConfig.MachineController cfg = MMCEGuiExtConfig.machineController;
        SmartInterfaceData[] dataList = getSmartInterfaceDataList();
        List<String> virtualKeys = getSmartInterfaceEditorVirtualKeys(cfg);
        boolean useVirtualKeys = !virtualKeys.isEmpty();
        boolean hasData = !useVirtualKeys && dataList.length > 0;
        int selectableCount = hasData ? dataList.length : virtualKeys.size();
        if (selectableCount > 0) {
            clampSmartInterfaceIndex(selectableCount);
        }

        if (this.smartInterfaceEditorInput.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        if (selectableCount > 1 && this.smartInterfacePrevButton.mousePressed(this.mc, mouseX, mouseY)) {
            this.smartInterfaceIndex = (this.smartInterfaceIndex + selectableCount - 1) % selectableCount;
            syncSmartInterfaceEditorInput();
            return true;
        }
        if (selectableCount > 1 && this.smartInterfaceNextButton.mousePressed(this.mc, mouseX, mouseY)) {
            this.smartInterfaceIndex = (this.smartInterfaceIndex + 1) % selectableCount;
            syncSmartInterfaceEditorInput();
            return true;
        }
        if ((hasData || useVirtualKeys) && this.smartInterfaceApplyButton.mousePressed(this.mc, mouseX, mouseY)) {
            applySmartInterfaceEditorValue();
            return true;
        }
        return false;
    }

    private boolean handleSmartInterfaceKeyTyped(char typedChar, int keyCode) {
        if (this.smartInterfaceEditorInput == null || !this.smartInterfaceEditorInput.isFocused()) {
            return false;
        }

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            applySmartInterfaceEditorValue();
            return true;
        }

        this.smartInterfaceEditorInput.textboxKeyTyped(typedChar, keyCode);
        return true;
    }

    private void applySmartInterfaceEditorValue() {
        if (this.smartInterfaceEditorInput == null) {
            return;
        }
        MMCEGuiExtConfig.MachineController cfg = MMCEGuiExtConfig.machineController;
        SmartInterfaceData[] dataList = getSmartInterfaceDataList();
        List<String> virtualKeys = getSmartInterfaceEditorVirtualKeys(cfg);
        boolean useVirtualKeys = !virtualKeys.isEmpty();
        boolean hasData = !useVirtualKeys && dataList.length > 0;
        int selectableCount = hasData ? dataList.length : virtualKeys.size();
        if (selectableCount > 0) {
            clampSmartInterfaceIndex(selectableCount);
        }

        String text = this.smartInterfaceEditorInput.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        ParsedDataPortValue parsed = parseDataPortValue(text);
        String interfaceType;
        if (hasData) {
            if (!parsed.numeric) {
                return;
            }
            SmartInterfaceData current = dataList[this.smartInterfaceIndex];
            current.setValue(parsed.number);
            interfaceType = current.getType();
        } else {
            interfaceType = getSelectedSmartInterfaceVirtualKey(virtualKeys);
            if (interfaceType.isEmpty()) {
                return;
            }
        }

        if (parsed.numeric) {
            MMCEGuiExt.NET_CHANNEL.sendToServer(new PktControllerSmartInterfaceUpdate(this.controller.getPos(), interfaceType, parsed.number));
            if (useVirtualKeys) {
                this.smartInterfaceEditorInput.setText(Float.toString(parsed.number));
                this.smartInterfaceVirtualInputCache.put(interfaceType, Float.toString(parsed.number));
            }
        } else {
            MMCEGuiExt.NET_CHANNEL.sendToServer(new PktControllerSmartInterfaceUpdate(this.controller.getPos(), interfaceType, parsed.text));
            if (useVirtualKeys) {
                this.smartInterfaceEditorInput.setText(parsed.text);
            }
            this.smartInterfaceVirtualInputCache.put(interfaceType, parsed.text);
        }
    }

    private void syncSmartInterfaceEditorInput() {
        if (this.smartInterfaceEditorInput == null) {
            return;
        }
        SmartInterfaceData[] dataList = getSmartInterfaceDataList();
        MMCEGuiExtConfig.MachineController cfg = MMCEGuiExtConfig.machineController;
        List<String> virtualKeys = getSmartInterfaceEditorVirtualKeys(cfg);
        if (!virtualKeys.isEmpty()) {
            clampSmartInterfaceIndex(virtualKeys.size());
            String selectedKey = getSelectedSmartInterfaceVirtualKey(virtualKeys);
            String cached = this.smartInterfaceVirtualInputCache.get(selectedKey);
            boolean switched = this.smartInterfaceActiveVirtualKey == null || !this.smartInterfaceActiveVirtualKey.equals(selectedKey);
            this.smartInterfaceActiveVirtualKey = selectedKey;
            Float numeric = ControllerCustomDataAccess.readNumber(this.controller, selectedKey);
            if (numeric != null) {
                String normalized = Float.toString(numeric.floatValue());
                this.smartInterfaceEditorInput.setText(normalized);
                this.smartInterfaceVirtualInputCache.put(selectedKey, normalized);
            } else {
                String text = ControllerCustomDataAccess.readString(this.controller, selectedKey);
                if (text != null) {
                    this.smartInterfaceEditorInput.setText(text);
                    this.smartInterfaceVirtualInputCache.put(selectedKey, text);
                } else if (cached != null) {
                    this.smartInterfaceEditorInput.setText(cached);
                } else if (switched || this.smartInterfaceEditorInput.getText() == null) {
                    this.smartInterfaceEditorInput.setText("");
                }
            }
            return;
        }
        if (dataList.length <= 0) {
            this.smartInterfaceActiveVirtualKey = null;
            if (this.smartInterfaceEditorInput.getText() == null) {
                this.smartInterfaceEditorInput.setText("");
            }
            return;
        }
        this.smartInterfaceActiveVirtualKey = null;
        clampSmartInterfaceIndex(dataList.length);
        SmartInterfaceData current = dataList[this.smartInterfaceIndex];
        this.smartInterfaceEditorInput.setText(Float.toString(current.getValue()));
    }

    private void drawCustomSmartInterfaceEditorsBackground(int mouseX, int mouseY, @Nullable Integer priorityFilter) {
        for (CustomSmartEditor editor : this.customSmartEditors) {
            if (priorityFilter != null && editor.priority != priorityFilter.intValue()) {
                continue;
            }
            if (!isPageVisible(editor.page)) {
                continue;
            }
            if (editor.input == null) {
                continue;
            }
            int keyCount = editor.keys.size();
            if (editor.prev != null) {
                editor.prev.enabled = keyCount > 1;
                editor.prev.drawButton(this.mc, mouseX, mouseY, 0F);
            }
            if (editor.next != null) {
                editor.next.enabled = keyCount > 1;
                editor.next.drawButton(this.mc, mouseX, mouseY, 0F);
            }
            if (editor.apply != null) {
                editor.apply.enabled = keyCount > 0;
                editor.apply.drawButton(this.mc, mouseX, mouseY, 0F);
            }
            editor.input.drawTextBox();
        }
    }

    private void drawCustomSmartInterfaceEditorsForeground(@Nullable Integer priorityFilter) {
        for (CustomSmartEditor editor : this.customSmartEditors) {
            if (priorityFilter != null && editor.priority != priorityFilter.intValue()) {
                continue;
            }
            if (!isPageVisible(editor.page)) {
                continue;
            }
            if (editor.input == null) {
                continue;
            }
            String activeKey = getCustomEditorActiveKey(editor);
            int index = editor.keys.isEmpty() ? 0 : editor.index + 1;
            int count = editor.keys.size();
            if (editor.showTitle) {
                String title = editor.title == null || editor.title.trim().isEmpty()
                    ? "Virtual DataPort (" + index + "/" + count + ")"
                    : editor.title
                        .replace("{index}", Integer.toString(index))
                        .replace("{count}", Integer.toString(count))
                        .replace("{key}", activeKey);
                this.fontRenderer.drawStringWithShadow(title, editor.x, editor.y - 10, 0xE0E0E0);
            }
            if (editor.showInfo) {
                this.fontRenderer.drawStringWithShadow("Key: " + activeKey, editor.x, editor.y + SMART_EDITOR_INPUT_H + 2, 0xBFD3FF);
            }
        }
    }

    private boolean handleCustomSmartInterfaceMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return false;
        }
        for (CustomSmartEditor editor : this.customSmartEditors) {
            if (!isPageVisible(editor.page)) {
                continue;
            }
            if (editor.input == null) {
                continue;
            }
            if (editor.input.mouseClicked(mouseX, mouseY, mouseButton)) {
                return true;
            }
            if (editor.prev != null && editor.keys.size() > 1 && editor.prev.mousePressed(this.mc, mouseX, mouseY)) {
                editor.index = (editor.index + editor.keys.size() - 1) % editor.keys.size();
                syncCustomSmartEditorInput(editor);
                return true;
            }
            if (editor.next != null && editor.keys.size() > 1 && editor.next.mousePressed(this.mc, mouseX, mouseY)) {
                editor.index = (editor.index + 1) % editor.keys.size();
                syncCustomSmartEditorInput(editor);
                return true;
            }
            if (editor.apply != null && editor.apply.mousePressed(this.mc, mouseX, mouseY)) {
                applyCustomSmartEditorValue(editor);
                return true;
            }
        }
        return false;
    }

    private boolean handleCustomSmartInterfaceKeyTyped(char typedChar, int keyCode) {
        for (CustomSmartEditor editor : this.customSmartEditors) {
            if (!isPageVisible(editor.page)) {
                continue;
            }
            if (editor.input == null || !editor.input.isFocused()) {
                continue;
            }
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                applyCustomSmartEditorValue(editor);
                return true;
            }
            editor.input.textboxKeyTyped(typedChar, keyCode);
            return true;
        }
        return false;
    }

    private void applyCustomSmartEditorValue(CustomSmartEditor editor) {
        if (editor.input == null || editor.keys.isEmpty()) {
            return;
        }
        String key = getCustomEditorActiveKey(editor);
        if (key.isEmpty()) {
            return;
        }
        String text = editor.input.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        ParsedDataPortValue parsed = parseDataPortValue(text);
        if (parsed.numeric) {
            MMCEGuiExt.NET_CHANNEL.sendToServer(new PktControllerSmartInterfaceUpdate(this.controller.getPos(), key, parsed.number));
            String normalized = Float.toString(parsed.number);
            editor.input.setText(normalized);
            this.smartInterfaceVirtualInputCache.put(key, normalized);
        } else {
            MMCEGuiExt.NET_CHANNEL.sendToServer(new PktControllerSmartInterfaceUpdate(this.controller.getPos(), key, parsed.text));
            this.smartInterfaceVirtualInputCache.put(key, parsed.text);
        }
    }

    private void syncCustomSmartEditorInput(CustomSmartEditor editor) {
        if (editor.input == null || editor.keys.isEmpty()) {
            return;
        }
        if (editor.index < 0 || editor.index >= editor.keys.size()) {
            editor.index = 0;
        }
        String key = editor.keys.get(editor.index);
        String cached = this.smartInterfaceVirtualInputCache.get(key);
        boolean switched = editor.activeKey == null || !editor.activeKey.equals(key);
        editor.activeKey = key;
        Float numeric = ControllerCustomDataAccess.readNumber(this.controller, key);
        if (numeric != null) {
            String normalized = Float.toString(numeric.floatValue());
            editor.input.setText(normalized);
            this.smartInterfaceVirtualInputCache.put(key, normalized);
            return;
        }
        String text = ControllerCustomDataAccess.readString(this.controller, key);
        if (text != null) {
            editor.input.setText(text);
            this.smartInterfaceVirtualInputCache.put(key, text);
            return;
        }
        if (cached != null) {
            editor.input.setText(cached);
        } else if (switched || editor.input.getText() == null) {
            editor.input.setText("");
        }
    }

    private String getCustomEditorActiveKey(CustomSmartEditor editor) {
        if (editor.keys.isEmpty()) {
            return "";
        }
        if (editor.index < 0 || editor.index >= editor.keys.size()) {
            editor.index = 0;
        }
        return editor.keys.get(editor.index);
    }

    private void initCustomSliders() {
        this.customSliders.clear();
        if (styleOverride.sliders == null) {
            return;
        }
        for (MachineGuiStyleManager.SliderStyle style : styleOverride.sliders) {
            if (style == null || style.key == null || style.key.trim().isEmpty()) {
                continue;
            }
            CustomSlider slider = new CustomSlider();
            slider.id = style.id == null || style.id.trim().isEmpty() ? "slider_" + this.customSliders.size() : style.id.trim();
            slider.key = style.key.trim();
            slider.x = MathHelper.clamp(style.x, 0, Math.max(0, this.renderWidth - style.width));
            slider.y = MathHelper.clamp(style.y, 0, Math.max(0, this.renderHeight - style.height));
            slider.width = Math.max(1, style.width);
            slider.height = Math.max(1, style.height);
            slider.min = style.min == null || !Float.isFinite(style.min.floatValue()) ? 0.0F : style.min.floatValue();
            slider.max = style.max == null || !Float.isFinite(style.max.floatValue()) ? 1.0F : style.max.floatValue();
            if (slider.min > slider.max) {
                float swap = slider.min;
                slider.min = slider.max;
                slider.max = swap;
            }
            slider.step = style.step == null || !Float.isFinite(style.step.floatValue()) || style.step.floatValue() <= 0.0F
                ? 0.0F : style.step.floatValue();
            slider.value = resolveSliderValue(style, slider);
            slider.vertical = "vertical".equals(style.direction);
            slider.trackColor = style.trackColor == null ? 0x66000000 : style.trackColor.intValue();
            slider.fillColor = style.fillColor == null ? 0xFF55CC66 : style.fillColor.intValue();
            slider.thumbColor = style.thumbColor == null ? 0xFFFFFFFF : style.thumbColor.intValue();
            slider.borderColor = style.borderColor;
            slider.thumbWidth = style.thumbWidth == null ? (slider.vertical ? slider.width : DEFAULT_SLIDER_THUMB_SIZE) : Math.max(1, style.thumbWidth.intValue());
            slider.thumbHeight = style.thumbHeight == null ? (slider.vertical ? DEFAULT_SLIDER_THUMB_SIZE : slider.height) : Math.max(1, style.thumbHeight.intValue());
            slider.priority = style.priority == null ? DEFAULT_SMART_EDITOR_PRIORITY : style.priority.intValue();
            slider.foreground = style.foreground == null || style.foreground.booleanValue();
            slider.visible = style.visible == null || style.visible.booleanValue();
            slider.page = normalizePageIdOrNull(style.page);
            slider.showText = style.showText != null && style.showText.booleanValue();
            slider.textColor = style.textColor == null ? 0xFFFFFFFF : style.textColor.intValue();
            this.customSliders.add(slider);
        }
    }

    private float resolveSliderValue(MachineGuiStyleManager.SliderStyle style, CustomSlider slider) {
        Float customValue = ControllerCustomDataAccess.readNumber(this.controller, slider.key);
        if (customValue != null && Float.isFinite(customValue.floatValue())) {
            return clampSliderValue(slider, customValue.floatValue());
        }
        SmartInterfaceData current = controller.getSmartInterfaceData(slider.key);
        if (current != null) {
            return clampSliderValue(slider, current.getValue());
        }
        if (style.initialValue != null && Float.isFinite(style.initialValue.floatValue())) {
            return clampSliderValue(slider, style.initialValue.floatValue());
        }
        return clampSliderValue(slider, slider.min);
    }

    private void drawBackgroundSliders() {
        drawSliders(null, false);
    }

    private void drawCustomSliders(@Nullable Integer priorityFilter) {
        drawSliders(priorityFilter, true);
    }

    private void drawSliders(@Nullable Integer priorityFilter, boolean foreground) {
        for (CustomSlider slider : this.customSliders) {
            if (!slider.visible || slider.foreground != foreground || !isPageVisible(slider.page)) {
                continue;
            }
            if (priorityFilter != null && slider.priority != priorityFilter.intValue()) {
                continue;
            }
            drawSlider(slider);
        }
    }

    private void drawSlider(CustomSlider slider) {
        int x = slider.x;
        int y = slider.y;
        drawRect(x, y, x + slider.width, y + slider.height, slider.trackColor);
        if (slider.borderColor != null) {
            drawProgressBorder(x, y, slider.width, slider.height, slider.borderColor.intValue());
        }

        float ratio = sliderRatio(slider);
        if (slider.vertical) {
            int fillHeight = MathHelper.floor(slider.height * ratio);
            drawRect(x, y + slider.height - fillHeight, x + slider.width, y + slider.height, slider.fillColor);
        } else {
            int fillWidth = MathHelper.floor(slider.width * ratio);
            drawRect(x, y, x + fillWidth, y + slider.height, slider.fillColor);
        }

        Rectangle thumb = sliderThumbRect(slider);
        drawRect(thumb.x, thumb.y, thumb.x + thumb.width, thumb.y + thumb.height, slider.thumbColor);
        if (slider.showText) {
            String text = formatSliderValue(slider.value);
            int textX = x + Math.max(0, (slider.width - this.fontRenderer.getStringWidth(text)) / 2);
            int textY = y + Math.max(0, (slider.height - this.fontRenderer.FONT_HEIGHT) / 2);
            this.fontRenderer.drawStringWithShadow(text, textX, textY, slider.textColor);
        }
    }

    private boolean handleCustomSliderMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return false;
        }
        CustomSlider slider = findTopmostSliderAt(mouseX, mouseY);
        if (slider == null) {
            return false;
        }
        this.draggingSlider = slider;
        updateSliderFromMouse(slider, mouseX, mouseY, true);
        return true;
    }

    @Nullable
    private CustomSlider findTopmostSliderAt(int mouseX, int mouseY) {
        List<IndexedSlider> ordered = new ArrayList<IndexedSlider>(this.customSliders.size());
        for (int i = 0; i < this.customSliders.size(); i++) {
            ordered.add(new IndexedSlider(this.customSliders.get(i), i));
        }
        ordered.sort((left, right) -> {
            int foregroundCmp = Boolean.compare(left.slider.foreground, right.slider.foreground);
            if (foregroundCmp != 0) {
                return -foregroundCmp;
            }
            int priorityCmp = Integer.compare(left.slider.priority, right.slider.priority);
            if (priorityCmp != 0) {
                return -priorityCmp;
            }
            return Integer.compare(right.index, left.index);
        });
        for (IndexedSlider candidate : ordered) {
            CustomSlider slider = candidate.slider;
            if (!slider.visible || !isPageVisible(slider.page)) {
                continue;
            }
            if (isPointInSlider(slider, mouseX, mouseY)) {
                return slider;
            }
        }
        return null;
    }

    private void updateSliderFromMouse(CustomSlider slider, int mouseX, int mouseY, boolean forceSend) {
        float ratio;
        if (slider.vertical) {
            ratio = 1.0F - ((float) (mouseY - this.guiTop - slider.y) / (float) Math.max(1, slider.height));
        } else {
            ratio = (float) (mouseX - this.guiLeft - slider.x) / (float) Math.max(1, slider.width);
        }
        ratio = MathHelper.clamp(ratio, 0.0F, 1.0F);
        float value = slider.min + (slider.max - slider.min) * ratio;
        if (slider.step > 0.0F) {
            value = slider.min + Math.round((value - slider.min) / slider.step) * slider.step;
        }
        value = clampSliderValue(slider, value);
        if (!forceSend && Math.abs(value - slider.value) < 1.0E-6F) {
            return;
        }
        slider.value = value;
        this.smartInterfaceVirtualInputCache.put(slider.key, Float.toString(value));
        MMCEGuiExt.NET_CHANNEL.sendToServer(new PktControllerSmartInterfaceUpdate(this.controller.getPos(), slider.key, value));
    }

    private boolean isPointInSlider(CustomSlider slider, int mouseX, int mouseY) {
        int x = this.guiLeft + slider.x;
        int y = this.guiTop + slider.y;
        return mouseX >= x && mouseY >= y && mouseX < x + slider.width && mouseY < y + slider.height;
    }

    private Rectangle sliderThumbRect(CustomSlider slider) {
        float ratio = sliderRatio(slider);
        if (slider.vertical) {
            int thumbX = slider.x + Math.max(0, (slider.width - slider.thumbWidth) / 2);
            int travel = Math.max(0, slider.height - slider.thumbHeight);
            int thumbY = slider.y + travel - MathHelper.floor(travel * ratio);
            return new Rectangle(thumbX, thumbY, slider.thumbWidth, slider.thumbHeight);
        }
        int thumbY = slider.y + Math.max(0, (slider.height - slider.thumbHeight) / 2);
        int travel = Math.max(0, slider.width - slider.thumbWidth);
        int thumbX = slider.x + MathHelper.floor(travel * ratio);
        return new Rectangle(thumbX, thumbY, slider.thumbWidth, slider.thumbHeight);
    }

    private float sliderRatio(CustomSlider slider) {
        if (Math.abs(slider.max - slider.min) <= 1.0E-6F) {
            return 0.0F;
        }
        return MathHelper.clamp((slider.value - slider.min) / (slider.max - slider.min), 0.0F, 1.0F);
    }

    private static float clampSliderValue(CustomSlider slider, float value) {
        return MathHelper.clamp(Float.isFinite(value) ? value : slider.min, slider.min, slider.max);
    }

    private static String formatSliderValue(float value) {
        if (Math.abs(value - Math.round(value)) < 1.0E-4F) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private void initCustomButtons(MMCEGuiExtConfig.MachineController cfg) {
        this.customButtons.clear();
        if (styleOverride.buttons != null) {
            int buttonId = CUSTOM_BUTTON_ID_BASE;
            for (MachineGuiStyleManager.ButtonStyle style : styleOverride.buttons) {
                if (style == null || style.action == null || style.action.trim().isEmpty()) {
                    continue;
                }
                boolean visible = style.visible == null || style.visible.booleanValue();
                boolean hasLabel = style.label != null && !style.label.trim().isEmpty();
                boolean hasHotkey = style.hotkeys != null && !style.hotkeys.isEmpty();
                if (!hasLabel && !hasHotkey) {
                    continue;
                }
                int width = style.width == null ? DEFAULT_BUTTON_WIDTH : Math.max(8, style.width.intValue());
                int height = style.height == null ? DEFAULT_BUTTON_HEIGHT : Math.max(8, style.height.intValue());
                int x = MathHelper.clamp(style.x, 0, Math.max(0, this.renderWidth - width));
                int y = MathHelper.clamp(style.y, 0, Math.max(0, this.renderHeight - height));

                CustomButton button = new CustomButton();
                button.id = style.id == null || style.id.trim().isEmpty() ? "button_" + this.customButtons.size() : style.id.trim();
                button.action = style.action;
                button.buttonId = style.buttonId == null || style.buttonId.trim().isEmpty() ? button.id : style.buttonId.trim();
                button.key = style.key;
                button.value = style.value == null ? ("smart_add".equals(style.action) ? 1.0F : 0.0F) : style.value.floatValue();
                button.shiftValue = style.shiftValue;
                button.ctrlValue = style.ctrlValue;
                button.ctrlShiftValue = style.ctrlShiftValue;
                button.stringValue = style.stringValue;
                button.min = style.min;
                button.max = style.max;
                button.targetPage = normalizePageId(style.targetPage);
                button.targetSubGui = normalizeSubGuiId(style.targetSubGui);
                button.openMode = normalizeSubGuiOpenMode(style.openMode);
                button.priority = style.priority == null ? DEFAULT_SMART_EDITOR_PRIORITY : style.priority.intValue();
                button.page = normalizePageIdOrNull(style.page);
                button.visible = visible;
                button.hotkeys = style.hotkeys == null ? Collections.<String>emptyList() : new ArrayList<String>(style.hotkeys);
                button.consumeHotkey = style.consumeHotkey == null || style.consumeHotkey.booleanValue();
                if (visible && hasLabel) {
                    button.button = new GuiButton(buttonId++, this.guiLeft + x, this.guiTop + y, width, height, style.label);
                }
                this.customButtons.add(button);
            }
        }
    }

    private void drawCustomButtons(int mouseX, int mouseY, @Nullable Integer priorityFilter) {
        for (CustomButton button : this.customButtons) {
            if (!button.visible) {
                continue;
            }
            if (priorityFilter != null && button.priority != priorityFilter.intValue()) {
                continue;
            }
            if (!isPageVisible(button.page) || button.button == null) {
                continue;
            }
            button.button.enabled = !"page".equals(button.action) || !button.targetPage.equals(this.activePageId);
            button.button.drawButton(this.mc, mouseX, mouseY, 0F);
        }
    }

    private boolean handleCustomButtonMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return false;
        }
        List<CustomButton> ordered = new ArrayList<CustomButton>(this.customButtons);
        ordered.sort((a, b) -> Integer.compare(b.priority, a.priority));
        for (CustomButton button : ordered) {
            if (!button.visible || !isPageVisible(button.page) || button.button == null) {
                continue;
            }
            button.button.enabled = !"page".equals(button.action) || !button.targetPage.equals(this.activePageId);
            if (button.button.mousePressed(this.mc, mouseX, mouseY)) {
                button.button.playPressSound(this.mc.getSoundHandler());
                activateCustomButton(button);
                return true;
            }
        }
        return false;
    }

    private boolean handleCustomButtonKeyTyped(char typedChar, int keyCode) {
        if (hasFocusedTextInput()) {
            return false;
        }
        List<CustomButton> ordered = new ArrayList<CustomButton>(this.customButtons);
        ordered.sort((a, b) -> Integer.compare(b.priority, a.priority));
        for (CustomButton button : ordered) {
            if (!isPageVisible(button.page) || button.hotkeys.isEmpty()) {
                continue;
            }
            if ("page".equals(button.action) && button.targetPage.equals(this.activePageId)) {
                continue;
            }
            if (matchesCustomButtonHotkey(button, typedChar, keyCode)) {
                activateCustomButton(button);
                return button.consumeHotkey;
            }
        }
        return false;
    }

    private boolean hasFocusedTextInput() {
        if (this.smartInterfaceEditorInput != null && this.smartInterfaceEditorInput.isFocused()) {
            return true;
        }
        for (CustomSmartEditor editor : this.customSmartEditors) {
            if (editor.input != null && editor.input.isFocused()) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesCustomButtonHotkey(CustomButton button, char typedChar, int keyCode) {
        for (String hotkey : button.hotkeys) {
            if (matchesHotkey(hotkey, typedChar, keyCode)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesHotkey(@Nullable String raw, char typedChar, int keyCode) {
        return matchesHotkey(raw, typedChar, keyCode, isShiftKeyDown(), isCtrlKeyDown(), isAltKeyDown());
    }

    private static boolean matchesHotkey(@Nullable String raw, char typedChar, int keyCode, boolean shiftDown, boolean ctrlDown, boolean altDown) {
        HotkeySpec spec = parseHotkeySpec(raw);
        if (spec == null) {
            return false;
        }
        if (spec.shiftRequired != shiftDown) {
            return false;
        }
        if (spec.ctrlRequired != ctrlDown) {
            return false;
        }
        if (spec.altRequired != altDown) {
            return false;
        }
        if (spec.keyCode > 0) {
            return keyCode == spec.keyCode;
        }
        return spec.character != 0 && Character.toUpperCase(typedChar) == Character.toUpperCase(spec.character);
    }

    @Nullable
    private static HotkeySpec parseHotkeySpec(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return null;
        }
        HotkeySpec spec = new HotkeySpec();
        String keyToken = null;
        String[] parts = text.split("\\+");
        for (String part : parts) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            String normalized = token.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            if ("CTRL".equals(normalized) || "CONTROL".equals(normalized) || "LCONTROL".equals(normalized) || "RCONTROL".equals(normalized)) {
                spec.ctrlRequired = true;
            } else if ("SHIFT".equals(normalized) || "LSHIFT".equals(normalized) || "RSHIFT".equals(normalized)) {
                spec.shiftRequired = true;
            } else if ("ALT".equals(normalized) || "LMENU".equals(normalized) || "RMENU".equals(normalized)) {
                spec.altRequired = true;
            } else {
                keyToken = normalized;
            }
        }
        if (keyToken == null || keyToken.isEmpty()) {
            return null;
        }
        if (keyToken.startsWith("KEY_")) {
            keyToken = keyToken.substring(4);
        }
        keyToken = normalizeHotkeyToken(keyToken);
        int code = getKeyboardKeyIndex(keyToken);
        if (code > 0) {
            spec.keyCode = code;
            return spec;
        }
        if (keyToken.length() == 1) {
            spec.character = keyToken.charAt(0);
            return spec;
        }
        return null;
    }

    private static String normalizeHotkeyToken(String token) {
        if ("ESC".equals(token)) {
            return "ESCAPE";
        }
        if ("ENTER".equals(token)) {
            return "RETURN";
        }
        if ("SPACEBAR".equals(token)) {
            return "SPACE";
        }
        if ("PLUS".equals(token)) {
            return "ADD";
        }
        if ("NUM_ENTER".equals(token) || "NUMPAD_ENTER".equals(token)) {
            return "NUMPADENTER";
        }
        return token;
    }

    private static int getKeyboardKeyIndex(String token) {
        try {
            return Keyboard.getKeyIndex(token);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private void activateCustomButton(CustomButton button) {
        if ("page".equals(button.action)) {
            this.activePageId = button.targetPage;
            return;
        }
        if ("subgui".equals(button.action)) {
            openSubGui(button.targetSubGui, button.openMode);
            return;
        }
        if ("close_subgui".equals(button.action)) {
            closeActiveSubGui();
            return;
        }
        if ("event".equals(button.action)) {
            MMCEGuiExt.NET_CHANNEL.sendToServer(PktControllerButtonAction.event(this.controller.getPos(), button.buttonId));
            return;
        }
        if (!"smart_set".equals(button.action) && !"smart_add".equals(button.action)) {
            return;
        }
        if (button.key == null || button.key.trim().isEmpty()) {
            return;
        }
        if ("smart_set".equals(button.action) && button.stringValue != null) {
            MMCEGuiExt.NET_CHANNEL.sendToServer(PktControllerButtonAction.smart(
                this.controller.getPos(),
                button.key,
                button.stringValue
            ));
            return;
        }
        float effectiveValue = resolveButtonValue(button);
        if (!Float.isFinite(effectiveValue)) {
            return;
        }
        MMCEGuiExt.NET_CHANNEL.sendToServer(PktControllerButtonAction.smart(
            this.controller.getPos(),
            button.key,
            "smart_add".equals(button.action),
            effectiveValue,
            button.min,
            button.max
        ));
    }

    // Picks the value to apply based on held modifier keys (data-port smart actions).
    // Missing modifier variants fall back to the base value.
    private float resolveButtonValue(CustomButton button) {
        boolean shift = isShiftKeyDown();
        boolean ctrl = isCtrlKeyDown();
        if (ctrl && shift && button.ctrlShiftValue != null) {
            return button.ctrlShiftValue.floatValue();
        }
        if (shift && button.shiftValue != null) {
            return button.shiftValue.floatValue();
        }
        if (ctrl && button.ctrlValue != null) {
            return button.ctrlValue.floatValue();
        }
        return button.value;
    }

    private TreeSet<Integer> collectForegroundRenderPriorities() {
        TreeSet<Integer> priorities = new TreeSet<Integer>();
        priorities.add(Integer.valueOf(resolveForegroundContentPriority()));
        for (TextureLayerDef layer : this.foregroundTextureLayers) {
            priorities.add(Integer.valueOf(resolveLayerPriority(layer)));
        }
        if (this.smartInterfaceEditorInput != null) {
            priorities.add(Integer.valueOf(this.smartInterfaceEditorPriority));
        }
        for (CustomButton button : this.customButtons) {
            priorities.add(Integer.valueOf(button.priority));
        }
        for (CustomSmartEditor editor : this.customSmartEditors) {
            priorities.add(Integer.valueOf(editor.priority));
        }
        if (styleOverride.texts != null) {
            for (MachineGuiStyleManager.TextStyle text : styleOverride.texts) {
                if (text != null && text.priority != null) {
                    priorities.add(text.priority);
                }
            }
        }
        if (styleOverride.progressBars != null) {
            for (MachineGuiStyleManager.ProgressBarStyle bar : styleOverride.progressBars) {
                if (bar != null && (bar.foreground == null || bar.foreground.booleanValue())) {
                    priorities.add(Integer.valueOf(bar.priority == null ? resolveForegroundContentPriority() : bar.priority.intValue()));
                }
            }
        }
        for (CustomSlider slider : this.customSliders) {
            if (slider.foreground) {
                priorities.add(Integer.valueOf(slider.priority));
            }
        }
        return priorities;
    }

    private void clampSmartInterfaceIndex(int size) {
        if (size <= 0) {
            this.smartInterfaceIndex = 0;
            return;
        }
        this.smartInterfaceIndex = MathHelper.clamp(this.smartInterfaceIndex, 0, size - 1);
    }

    private ParsedDataPortValue parseDataPortValue(String text) {
        String normalized = text == null ? "" : text.trim();
        try {
            float number = Float.parseFloat(normalized);
            if (Float.isFinite(number)) {
                return new ParsedDataPortValue(number);
            }
        } catch (NumberFormatException ignored) {
        }
        return new ParsedDataPortValue(normalized);
    }

    private boolean getSmartInterfaceEditorEnabled(MMCEGuiExtConfig.MachineController cfg) {
        if (styleOverride.enableSmartInterfaceEditor != null) {
            return styleOverride.enableSmartInterfaceEditor.booleanValue();
        }
        return cfg.enableSmartInterfaceEditor;
    }

    private String resolveConfiguredDefaultPanelId(MMCEGuiExtConfig.MachineController cfg) {
        if (styleOverride.defaultPanelId != null && !styleOverride.defaultPanelId.trim().isEmpty()) {
            return styleOverride.defaultPanelId;
        }
        return cfg.defaultPanelId;
    }

    private String resolveConfiguredDefaultPageId() {
        if (styleOverride.defaultPageId != null && !styleOverride.defaultPageId.trim().isEmpty()) {
            return normalizePageId(styleOverride.defaultPageId);
        }
        return "main";
    }

    private String resolveInitialPageId(MMCEGuiExtConfig.MachineController cfg, @Nullable String current) {
        LinkedHashMap<String, Boolean> pages = new LinkedHashMap<String, Boolean>();
        pages.put(resolveConfiguredDefaultPageId(), Boolean.TRUE);
        String[] configuredPanels = getConfiguredPanels(cfg);
        if (configuredPanels != null) {
            for (String entry : configuredPanels) {
                PanelDef panel = parsePanelEntry(entry);
                if (panel != null && panel.page != null) {
                    pages.put(panel.page, Boolean.TRUE);
                }
            }
        }
        if (styleOverride.texts != null) {
            for (MachineGuiStyleManager.TextStyle text : styleOverride.texts) {
                if (text != null && text.page != null && !text.page.trim().isEmpty()) {
                    pages.put(normalizePageId(text.page), Boolean.TRUE);
                }
            }
        }
        if (styleOverride.smartInterfaceEditors != null) {
            for (MachineGuiStyleManager.SmartInterfaceEditorStyle editor : styleOverride.smartInterfaceEditors) {
                if (editor != null && editor.page != null && !editor.page.trim().isEmpty()) {
                    pages.put(normalizePageId(editor.page), Boolean.TRUE);
                }
            }
        }
        if (styleOverride.textureLayers != null) {
            for (MachineGuiStyleManager.TextureLayerStyle layer : styleOverride.textureLayers) {
                if (layer != null && layer.page != null && !layer.page.trim().isEmpty()) {
                    pages.put(normalizePageId(layer.page), Boolean.TRUE);
                }
            }
        }
        if (styleOverride.buttons != null) {
            for (MachineGuiStyleManager.ButtonStyle button : styleOverride.buttons) {
                if (button == null) {
                    continue;
                }
                if (button.page != null && !button.page.trim().isEmpty()) {
                    pages.put(normalizePageId(button.page), Boolean.TRUE);
                }
                if (button.targetPage != null && !button.targetPage.trim().isEmpty()) {
                    pages.put(normalizePageId(button.targetPage), Boolean.TRUE);
                }
            }
        }
        if (current != null) {
            String normalizedCurrent = normalizePageId(current);
            if (pages.containsKey(normalizedCurrent)) {
                return normalizedCurrent;
            }
        }
        String configured = resolveConfiguredDefaultPageId();
        if (pages.containsKey(configured)) {
            return configured;
        }
        return pages.isEmpty() ? "main" : pages.keySet().iterator().next();
    }

    @Nullable
    private String[] getConfiguredPanels(MMCEGuiExtConfig.MachineController cfg) {
        if (styleOverride.customPanels != null && !styleOverride.customPanels.isEmpty()) {
            return styleOverride.customPanels.toArray(new String[0]);
        }
        return cfg.customPanels;
    }

    private int getSmartInterfaceEditorX(MMCEGuiExtConfig.MachineController cfg) {
        if (styleOverride.smartInterfaceEditorX != null) {
            return styleOverride.smartInterfaceEditorX.intValue();
        }
        return cfg.smartInterfaceEditorX;
    }

    private int getSmartInterfaceEditorY(MMCEGuiExtConfig.MachineController cfg) {
        if (styleOverride.smartInterfaceEditorY != null) {
            return styleOverride.smartInterfaceEditorY.intValue();
        }
        return cfg.smartInterfaceEditorY;
    }

    private int getSmartInterfaceEditorInputWidth(MMCEGuiExtConfig.MachineController cfg) {
        if (styleOverride.smartInterfaceEditorInputWidth != null) {
            return Math.max(4, styleOverride.smartInterfaceEditorInputWidth.intValue());
        }
        return Math.max(4, cfg.smartInterfaceEditorInputWidth);
    }

    private List<String> getSmartInterfaceEditorVirtualKeys(MMCEGuiExtConfig.MachineController cfg) {
        String raw = styleOverride.smartInterfaceEditorVirtualKey != null
            ? styleOverride.smartInterfaceEditorVirtualKey
            : cfg.smartInterfaceEditorVirtualKey;
        return parseVirtualKeys(raw);
    }

    private String getSelectedSmartInterfaceVirtualKey(List<String> virtualKeys) {
        if (virtualKeys.isEmpty()) {
            return "";
        }
        clampSmartInterfaceIndex(virtualKeys.size());
        return virtualKeys.get(this.smartInterfaceIndex);
    }

    private String resolveSmartInterfaceEditorTitle(boolean hasData, int count, int currentIndex, @Nullable String virtualKey) {
        if (this.smartInterfaceCustomTitleText != null && !this.smartInterfaceCustomTitleText.isEmpty()) {
            String key = virtualKey == null ? "" : virtualKey;
            return this.smartInterfaceCustomTitleText
                .replace("{count}", Integer.toString(Math.max(0, count)))
                .replace("{index}", Integer.toString(Math.max(0, currentIndex)))
                .replace("{key}", key);
        }
        if (hasData) {
            return I18n.format("gui.smartinterface.title", count, currentIndex);
        }
        if (virtualKey != null && !virtualKey.isEmpty()) {
            if (count > 1) {
                return "Virtual DataPort (" + currentIndex + "/" + count + ")";
            }
            return "Virtual DataPort";
        }
        return "Smart Interface (0/0)";
    }

    private int resolveSmartInterfaceEditorX(MMCEGuiExtConfig.MachineController cfg, int totalWidth) {
        int configured = getSmartInterfaceEditorX(cfg);
        if (configured >= 0) {
            return MathHelper.clamp(configured, 2, Math.max(2, this.renderWidth - 2));
        }
        int autoX = this.renderWidth - totalWidth - 6;
        if (this.renderWidth > BASE_WIDTH) {
            // Keep default auto placement in right extension area first.
            return Math.max(BASE_WIDTH + 2, autoX);
        }
        return Math.max(2, autoX);
    }

    private int resolveSmartInterfaceEditorY(MMCEGuiExtConfig.MachineController cfg) {
        int configured = getSmartInterfaceEditorY(cfg);
        if (configured >= 0) {
            return MathHelper.clamp(configured, 12, Math.max(12, this.renderHeight - SMART_EDITOR_INPUT_H - 14));
        }
        int autoY = this.renderHeight - SMART_EDITOR_INPUT_H - 6;
        return MathHelper.clamp(autoY, 12, Math.max(12, this.renderHeight - SMART_EDITOR_INPUT_H - 14));
    }

    private void resetSmartInterfaceEditorHints() {
        this.smartInterfaceHideInfoText = false;
        this.smartInterfaceHideTitleText = false;
        this.smartInterfaceCustomTitleText = null;
    }

    private boolean consumeGuiDirective(@Nullable String raw) {
        return consumeSmartInterfaceEditorDirective(raw) || consumeTextureLayerDirective(raw);
    }

    private boolean consumeTextureLayerDirective(@Nullable String raw) {
        if (raw == null) {
            return false;
        }
        String text = raw.trim();
        if (!text.startsWith("[") || !text.endsWith("]") || text.length() <= 2) {
            return false;
        }
        String body = text.substring(1, text.length() - 1).trim();
        if (body.isEmpty()) {
            return false;
        }

        String lowerBody = body.toLowerCase(Locale.ROOT);
        String prefix = "mmcege:layer.";
        if (!lowerBody.startsWith(prefix)) {
            return false;
        }

        String payload = body.substring(prefix.length()).trim();
        if (payload.isEmpty()) {
            return true;
        }
        if ("reset_all".equalsIgnoreCase(payload) || "clear_all".equalsIgnoreCase(payload)) {
            this.layerRuntimeStates.clear();
            return true;
        }

        int eqIdx = payload.indexOf('=');
        String left = eqIdx >= 0 ? payload.substring(0, eqIdx).trim() : payload;
        String right = eqIdx >= 0 ? payload.substring(eqIdx + 1).trim() : "";

        int dotIdx = left.lastIndexOf('.');
        if (dotIdx <= 0 || dotIdx >= left.length() - 1) {
            return true;
        }

        String layerId = left.substring(0, dotIdx).trim();
        String action = left.substring(dotIdx + 1).trim().toLowerCase(Locale.ROOT);
        if (layerId.isEmpty()) {
            return true;
        }
        if (!this.textureLayerIds.contains(layerId)) {
            return true;
        }

        if ("reset".equals(action) || "clear".equals(action)) {
            this.layerRuntimeStates.remove(layerId);
            return true;
        }

        LayerRuntimeState state = this.layerRuntimeStates.get(layerId);
        if (state == null) {
            state = new LayerRuntimeState();
            this.layerRuntimeStates.put(layerId, state);
        }

        if ("x".equals(action)) {
            Integer value = parseDirectiveInt(right);
            if (value != null) {
                state.offsetX = value;
            }
            return true;
        }
        if ("y".equals(action)) {
            Integer value = parseDirectiveInt(right);
            if (value != null) {
                state.offsetY = value;
            }
            return true;
        }
        if ("scale".equals(action)) {
            Float value = parseDirectiveFloat(right);
            if (value != null) {
                state.scale = value;
            }
            return true;
        }
        if ("scalex".equals(action) || "scale_x".equals(action)) {
            Float value = parseDirectiveFloat(right);
            if (value != null) {
                state.scaleX = value;
            }
            return true;
        }
        if ("scaley".equals(action) || "scale_y".equals(action)) {
            Float value = parseDirectiveFloat(right);
            if (value != null) {
                state.scaleY = value;
            }
            return true;
        }
        if ("rotation".equals(action) || "rotate".equals(action) || "rot".equals(action)) {
            Float value = parseDirectiveFloat(right);
            if (value != null) {
                state.rotation = value;
            }
            return true;
        }
        if ("alpha".equals(action) || "opacity".equals(action) || "transparency".equals(action)) {
            Float value = parseDirectiveFloat(right);
            if (value != null) {
                state.alpha = Float.valueOf(normalizeLayerAlpha(value.floatValue()));
            }
            return true;
        }
        if ("priority".equals(action) || "z".equals(action) || "zindex".equals(action) || "z_index".equals(action)) {
            Integer value = parseDirectiveInt(right);
            if (value != null) {
                state.priority = value;
            }
            return true;
        }
        if ("visible".equals(action) || "show".equals(action)) {
            Boolean value = parseDirectiveBoolean(right);
            if (value != null) {
                state.visible = value;
            }
            return true;
        }
        return true;
    }

    @Nullable
    private Integer parseDirectiveInt(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(raw.trim()));
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private Float parseDirectiveFloat(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        try {
            float value = Float.parseFloat(raw.trim());
            return Float.isFinite(value) ? Float.valueOf(value) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private Boolean parseDirectiveBoolean(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return null;
        }
        if ("1".equals(text) || "true".equals(text) || "yes".equals(text) || "on".equals(text) || "show".equals(text)) {
            return Boolean.TRUE;
        }
        if ("0".equals(text) || "false".equals(text) || "no".equals(text) || "off".equals(text) || "hide".equals(text)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private boolean consumeSmartInterfaceEditorDirective(@Nullable String raw) {
        if (raw == null) {
            return false;
        }
        String text = raw.trim();
        if (!text.startsWith("[") || !text.endsWith("]") || text.length() <= 2) {
            return false;
        }
        String body = text.substring(1, text.length() - 1).trim();
        if (body.isEmpty()) {
            return false;
        }

        String lower = body.toLowerCase(Locale.ROOT);
        if ("mmcege:si.hide_key".equals(lower) || "mmcege:si.hide_info".equals(lower)) {
            this.smartInterfaceHideInfoText = true;
            return true;
        }
        if ("mmcege:si.show_key".equals(lower) || "mmcege:si.show_info".equals(lower)) {
            this.smartInterfaceHideInfoText = false;
            return true;
        }
        if ("mmcege:si.hide_title".equals(lower)) {
            this.smartInterfaceHideTitleText = true;
            return true;
        }
        if ("mmcege:si.show_title".equals(lower)) {
            this.smartInterfaceHideTitleText = false;
            return true;
        }
        if ("mmcege:si.clear_title".equals(lower)) {
            this.smartInterfaceCustomTitleText = null;
            return true;
        }

        String titlePrefix = "mmcege:si.title=";
        if (lower.startsWith(titlePrefix)) {
            String custom = body.substring(titlePrefix.length()).trim();
            this.smartInterfaceCustomTitleText = custom.isEmpty() ? null : custom;
            this.smartInterfaceHideTitleText = false;
            return true;
        }
        return false;
    }

    private RoutedText parseRoutedText(@Nullable String raw, String defaultPanelId) {
        if (raw == null) {
            return new RoutedText(defaultPanelId, "");
        }
        String text = raw.trim();
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.startsWith("[panel:")) {
            int end = text.indexOf(']');
            if (end > 7) {
                String id = normalizePanelId(text.substring(7, end));
                String content = text.substring(end + 1).trim();
                return new RoutedText(id, content);
            }
        }
        return new RoutedText(defaultPanelId, text);
    }

    private static class RoutedText {
        private final String panelId;
        private final String text;

        private RoutedText(String panelId, String text) {
            this.panelId = panelId;
            this.text = text;
        }
    }

    private static class TextureLayerDef {
        private String id;
        private ResourceLocation texture;
        private int offsetX;
        private int offsetY;
        private int priority = DEFAULT_RENDER_PRIORITY;
        private float alpha = 1.0F;
        @Nullable
        private String page;
        @Nullable
        private Integer width;
        @Nullable
        private Integer height;
        @Nullable
        private Integer textureWidth;
        @Nullable
        private Integer textureHeight;
        @Nullable
        private Integer corner;
        @Nullable
        private Boolean useNineSlice;
    }

    private static class LayerRuntimeState {
        @Nullable
        private Integer offsetX;
        @Nullable
        private Integer offsetY;
        @Nullable
        private Float scale;
        @Nullable
        private Float scaleX;
        @Nullable
        private Float scaleY;
        @Nullable
        private Float rotation;
        @Nullable
        private Integer priority;
        @Nullable
        private Float alpha;
        @Nullable
        private Boolean visible;
    }

    private static class CustomSmartEditor {
        private String id;
        private int x;
        private int y;
        private int priority = DEFAULT_SMART_EDITOR_PRIORITY;
        @Nullable
        private String page;
        private int inputWidth;
        private List<String> keys = new ArrayList<String>();
        private int index = 0;
        @Nullable
        private GuiTextField input;
        @Nullable
        private GuiButton prev;
        @Nullable
        private GuiButton next;
        @Nullable
        private GuiButton apply;
        @Nullable
        private String activeKey;
        @Nullable
        private String title;
        private boolean showTitle = true;
        private boolean showInfo = true;
        private boolean showControls = true;
        private boolean inputBackground = true;
    }

    private static class CustomButton {
        private String id;
        private String action;
        private String buttonId;
        @Nullable
        private String key;
        private float value;
        @Nullable
        private Float shiftValue;
        @Nullable
        private Float ctrlValue;
        @Nullable
        private Float ctrlShiftValue;
        @Nullable
        private String stringValue;
        @Nullable
        private Float min;
        @Nullable
        private Float max;
        private String targetPage = "main";
        private String targetSubGui = "";
        private String openMode = "replace";
        private int priority = DEFAULT_SMART_EDITOR_PRIORITY;
        @Nullable
        private String page;
        private boolean visible = true;
        private List<String> hotkeys = Collections.emptyList();
        private boolean consumeHotkey = true;
        @Nullable
        private GuiButton button;
    }

    private static class HotkeySpec {
        private int keyCode;
        private char character;
        private boolean shiftRequired;
        private boolean ctrlRequired;
        private boolean altRequired;
    }

    private static class CustomSlider {
        private String id;
        private String key;
        private int x;
        private int y;
        private int width;
        private int height;
        private float min;
        private float max;
        private float step;
        private float value;
        private boolean vertical;
        private int trackColor;
        private int fillColor;
        private int thumbColor;
        @Nullable
        private Integer borderColor;
        private int thumbWidth;
        private int thumbHeight;
        private int priority = DEFAULT_SMART_EDITOR_PRIORITY;
        private boolean foreground = true;
        private boolean visible = true;
        @Nullable
        private String page;
        private boolean showText;
        private int textColor = 0xFFFFFFFF;
    }

    private static class IndexedSlider {
        private final CustomSlider slider;
        private final int index;

        private IndexedSlider(CustomSlider slider, int index) {
            this.slider = slider;
            this.index = index;
        }
    }

    private static class ParsedDataPortValue {
        private final boolean numeric;
        private final float number;
        private final String text;

        private ParsedDataPortValue(float number) {
            this.numeric = true;
            this.number = number;
            this.text = "";
        }

        private ParsedDataPortValue(String text) {
            this.numeric = false;
            this.number = 0.0F;
            this.text = text == null ? "" : text;
        }
    }

    private static class RuntimeGuiState {
        @Nullable
        private String subGuiId;
        private boolean modal;
        private MachineGuiStyleManager.ControllerStyle styleOverride = MachineGuiStyleManager.ControllerStyle.EMPTY;
        @Nullable
        private ResourceLocation customBackgroundTexture;
        private int renderWidth;
        private int renderHeight;
        private boolean guiScaleMode;
        private int logicalWidth;
        private int logicalHeight;
        private float renderScale;
        private int renderOriginX;
        private int renderOriginY;
        private Map<String, Integer> panelScroll = new HashMap<String, Integer>();
        private Map<String, Integer> panelMaxScroll = new HashMap<String, Integer>();
        @Nullable
        private String draggingPanelId;
        private int infoScrollbarDragOffset;
        @Nullable
        private Boolean modalSubGuiDraggable;
        @Nullable
        private Boolean modalSubGuiDragHandle;
        private boolean modalSubGuiDragging;
        private int modalSubGuiDragOffsetX;
        private int modalSubGuiDragOffsetY;
        private int modalSubGuiDragX;
        private int modalSubGuiDragY;
        private int modalSubGuiDragWidth;
        private int modalSubGuiDragHeight;
        @Nullable
        private CustomSlider draggingSlider;
        @Nullable
        private GuiTextField smartInterfaceEditorInput;
        @Nullable
        private GuiButton smartInterfacePrevButton;
        @Nullable
        private GuiButton smartInterfaceNextButton;
        @Nullable
        private GuiButton smartInterfaceApplyButton;
        private int smartInterfaceIndex;
        private int smartInterfaceEditorX;
        private int smartInterfaceEditorY;
        private int smartInterfaceEditorPriority;
        private Map<String, String> smartInterfaceVirtualInputCache = new HashMap<String, String>();
        @Nullable
        private String smartInterfaceActiveVirtualKey;
        private boolean smartInterfaceHideInfoText;
        private boolean smartInterfaceHideTitleText;
        @Nullable
        private String smartInterfaceCustomTitleText;
        private boolean hideDefaultSmartInterfaceEditor;
        private List<CustomSmartEditor> customSmartEditors = Collections.emptyList();
        private List<CustomButton> customButtons = Collections.emptyList();
        private List<CustomSlider> customSliders = Collections.emptyList();
        private List<TextureLayerDef> backgroundTextureLayers = Collections.emptyList();
        private List<TextureLayerDef> foregroundTextureLayers = Collections.emptyList();
        private Map<String, LayerRuntimeState> layerRuntimeStates = new HashMap<String, LayerRuntimeState>();
        private Set<String> textureLayerIds = new HashSet<String>();
        private String activePageId = "main";
        private int guiLeft;
        private int guiTop;
        private int xSize;
        private int ySize;

        private Rectangle getBounds() {
            return new Rectangle(this.guiLeft, this.guiTop, Math.max(1, this.renderWidth), Math.max(1, this.renderHeight));
        }
    }

    private static class PanelDef {
        private final String id;
        private final Rectangle rect;
        @Nullable
        private final String page;

        private PanelDef(String id, Rectangle rect, @Nullable String page) {
            this.id = id;
            this.rect = rect;
            this.page = page;
        }
    }
}
