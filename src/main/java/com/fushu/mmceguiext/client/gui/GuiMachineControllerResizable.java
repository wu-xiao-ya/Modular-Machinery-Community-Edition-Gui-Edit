package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import github.kasuminova.mmce.common.event.client.ControllerGUIRenderEvent;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.gui.GuiContainerBase;
import hellfirepvp.modularmachinery.common.container.ContainerController;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GuiMachineControllerResizable extends GuiContainerBase<ContainerController> {
    private static final int BASE_WIDTH = 176;
    private static final int BASE_HEIGHT = 213;
    private static final int PLAYER_INVENTORY_TOP = 131;
    private static final double FONT_SCALE = 0.72D;

    private static final ResourceLocation DEFAULT_BACKGROUND = new ResourceLocation(
        ModularMachinery.MODID,
        "textures/gui/guicontroller_large.png"
    );
    private static final String DEFAULT_BACKGROUND_STR = "modularmachinery:textures/gui/guicontroller_large.png";

    private final TileMachineController controller;
    private MachineGuiStyleManager.ControllerStyle styleOverride = MachineGuiStyleManager.ControllerStyle.EMPTY;
    @Nullable
    private ResourceLocation customBackgroundTexture = null;

    private int renderWidth = BASE_WIDTH;
    private int renderHeight = BASE_HEIGHT;

    private final Map<String, Integer> panelScroll = new HashMap<String, Integer>();
    private final Map<String, Integer> panelMaxScroll = new HashMap<String, Integer>();
    @Nullable
    private String draggingPanelId = null;
    private int infoScrollbarDragOffset = 0;

    public GuiMachineControllerResizable(ContainerController container) {
        super(container);
        this.controller = container.getOwner();
    }

    @Override
    public void initGui() {
        this.styleOverride = MachineGuiStyleManager.resolveMachineController(resolveMachine());
        this.customBackgroundTexture = resolveCustomTexture();
        super.initGui();
        this.panelScroll.clear();
        this.panelMaxScroll.clear();
        this.draggingPanelId = null;
        this.infoScrollbarDragOffset = 0;
    }

    @Override
    protected void setWidthHeight() {
        MMCEGuiExtConfig.MachineController cfg = MMCEGuiExtConfig.machineController;
        int baseLeft = (this.width - BASE_WIDTH) / 2;
        int baseTop = (this.height - BASE_HEIGHT) / 2;
        int maxWidth = Math.max(BASE_WIDTH, this.width - baseLeft - 8);
        int maxHeight = Math.max(BASE_HEIGHT, this.height - baseTop - 8);
        int requestedWidth = styleOverride.guiWidth != null ? styleOverride.guiWidth.intValue() : cfg.guiWidth;
        int requestedHeight = styleOverride.guiHeight != null ? styleOverride.guiHeight.intValue() : cfg.guiHeight;
        int targetWidth = getDisableRightExtension() ? BASE_WIDTH : requestedWidth;
        this.renderWidth = MathHelper.clamp(targetWidth, BASE_WIDTH, maxWidth);
        this.renderHeight = MathHelper.clamp(requestedHeight, BASE_HEIGHT, maxHeight);

        this.xSize = BASE_WIDTH;
        this.ySize = BASE_HEIGHT;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        MMCEGuiExtConfig.MachineController cfg = MMCEGuiExtConfig.machineController;
        if (isUsingDefaultBackground(cfg)) {
            this.mc.getTextureManager().bindTexture(DEFAULT_BACKGROUND);
            Gui.drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, BASE_WIDTH, BASE_HEIGHT, BASE_WIDTH, BASE_HEIGHT);
            return;
        }

        boolean useNineSlice = getUseNineSlice(cfg);
        boolean hideDefaultBackground = getHideDefaultBackground(cfg);
        int texW = Math.max(16, cfg.backgroundTextureWidth);
        int texH = Math.max(16, cfg.backgroundTextureHeight);

        if (this.customBackgroundTexture != null) {
            this.mc.getTextureManager().bindTexture(this.customBackgroundTexture);
            drawResizableArea(
                this.guiLeft,
                this.guiTop,
                this.renderWidth,
                this.renderHeight,
                useNineSlice,
                texW,
                texH,
                cfg.backgroundCorner
            );
        } else {
            if (!hideDefaultBackground) {
                this.mc.getTextureManager().bindTexture(DEFAULT_BACKGROUND);
                Gui.drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, BASE_WIDTH, BASE_HEIGHT, BASE_WIDTH, BASE_HEIGHT);
            }
            if (this.renderWidth > BASE_WIDTH || this.renderHeight > BASE_HEIGHT) {
                drawExtensionFallback();
            }
        }

        // Custom panel backgrounds are intentionally not rendered.
        // Users should draw panel areas directly in their custom GUI textures.
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        MMCEGuiExtConfig.MachineController cfg = MMCEGuiExtConfig.machineController;
        if (isUsingDefaultBackground(cfg)) {
            drawDefaultForeground();
            return;
        }

        List<PanelDef> panels = getActivePanels(cfg);
        cleanupPanelState(panels);

        Map<String, List<String>> linesByPanel = buildPanelInfoLines(panels, cfg);
        int lineHeight = this.fontRenderer.FONT_HEIGHT + 2;

        for (PanelDef panel : panels) {
            List<String> lines = linesByPanel.get(panel.id);
            if (lines == null) {
                lines = new ArrayList<String>();
            }

            int contentHeight = lines.size() * lineHeight;
            int viewportHeight = Math.max(1, panel.rect.getHeight() - 4);
            int maxScroll = Math.max(0, contentHeight - viewportHeight);
            panelMaxScroll.put(panel.id, maxScroll);

            int scroll = MathHelper.clamp(getPanelScroll(panel.id), 0, maxScroll);
            panelScroll.put(panel.id, scroll);

            int textX = panel.rect.getX() + 3;
            int textY = panel.rect.getY() + 2 - scroll;
            int clipX = this.guiLeft + panel.rect.getX() + 1;
            int clipY = this.guiTop + panel.rect.getY() + 1;
            int clipWidth = panel.rect.getWidth() - 2;
            int clipHeight = panel.rect.getHeight() - 2;

            GuiRenderUtils.enableScissor(this.mc, clipX, clipY, clipWidth, clipHeight);
            for (String line : lines) {
                this.fontRenderer.drawStringWithShadow(line, textX, textY, 0xFFFFFF);
                textY += lineHeight;
            }
            GuiRenderUtils.disableScissor();

            drawPanelScrollBar(panel);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
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
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
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
        this.draggingPanelId = null;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isUsingDefaultBackground(MMCEGuiExtConfig.machineController)) {
            super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
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
        if (machine != null) {
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
        } else if (!controller.isStructureFormed()) {
            this.fontRenderer.drawStringWithShadow(
                I18n.format("gui.controller.blueprint", I18n.format("gui.controller.blueprint.none")),
                offsetX,
                offsetY,
                0xFFFFFF
            );
            offsetY += 15;
        }

        DynamicMachine found = controller.getFoundMachine();
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

        ActiveMachineRecipe activeRecipe = controller.getActiveRecipe();
        if (activeRecipe != null) {
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

        GlStateManager.popMatrix();
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

    private List<PanelDef> getActivePanels(MMCEGuiExtConfig.MachineController cfg) {
        List<PanelDef> panels = new ArrayList<PanelDef>();
        if (isUsingDefaultBackground(cfg)) {
            panels.add(new PanelDef(normalizePanelId(cfg.defaultPanelId), getDefaultPanelRect(cfg)));
            return panels;
        }

        String[] configuredPanels = cfg.customPanels;
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
            panels.add(new PanelDef("main", new PanelRect(x, y, width, height)));
        }

        return panels;
    }

    private PanelRect getDefaultPanelRect(MMCEGuiExtConfig.MachineController cfg) {
        int panelX = cfg.panelX > 0 ? cfg.panelX : 10;
        panelX = Math.max(0, Math.min(panelX, Math.max(0, BASE_WIDTH - 32)));

        int panelY = Math.max(4, cfg.panelY);
        panelY = Math.min(panelY, PLAYER_INVENTORY_TOP - 24);

        int panelWidth = cfg.panelWidth > 0 ? cfg.panelWidth : 140;
        panelWidth = Math.max(48, Math.min(panelWidth, Math.max(48, BASE_WIDTH - panelX - 4)));
        panelWidth = Math.min(panelWidth, 140);

        int maxHeight = Math.max(24, PLAYER_INVENTORY_TOP - panelY - 4);
        int panelHeight = cfg.panelHeight > 0 ? cfg.panelHeight : 112;
        panelHeight = Math.max(24, Math.min(panelHeight, maxHeight));

        return new PanelRect(panelX, panelY, panelWidth, panelHeight);
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
        if (split.length != 5) {
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

            return new PanelDef(id, new PanelRect(x, y, width, height));
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

        String defaultPanelId = resolveDefaultPanelId(panels, cfg.defaultPanelId);

        int redstone = controller.getWorld().getStrongPower(controller.getPos());
        if (redstone > 0) {
            addWrapped(linesByPanel, panelMap, defaultPanelId, I18n.format("gui.controller.status.redstone_stopped"), defaultPanelId);
            return linesByPanel;
        }

        DynamicMachine machine = controller.getBlueprintMachine();
        if (machine != null) {
            addWrapped(linesByPanel, panelMap, defaultPanelId, I18n.format("gui.controller.blueprint", ""), defaultPanelId);
            addWrapped(linesByPanel, panelMap, defaultPanelId, machine.getLocalizedName(), defaultPanelId);
            addBlank(linesByPanel, defaultPanelId);
        } else if (!controller.isStructureFormed()) {
            addWrapped(
                linesByPanel,
                panelMap,
                defaultPanelId,
                I18n.format("gui.controller.blueprint", I18n.format("gui.controller.blueprint.none")),
                defaultPanelId
            );
            addBlank(linesByPanel, defaultPanelId);
        }

        DynamicMachine found = controller.getFoundMachine();
        if (found != null) {
            addWrapped(linesByPanel, panelMap, defaultPanelId, I18n.format("gui.controller.structure", ""), defaultPanelId);
            addWrapped(linesByPanel, panelMap, defaultPanelId, found.getLocalizedName(), defaultPanelId);

            ControllerGUIRenderEvent event = new ControllerGUIRenderEvent(controller);
            event.postEvent();
            for (String extra : event.getExtraInfo()) {
                RoutedText routed = parseRoutedText(extra, defaultPanelId);
                String targetPanel = panelMap.containsKey(routed.panelId) ? routed.panelId : defaultPanelId;
                addWrapped(linesByPanel, panelMap, targetPanel, routed.text, defaultPanelId);
            }
        } else {
            addWrapped(
                linesByPanel,
                panelMap,
                defaultPanelId,
                I18n.format("gui.controller.structure", I18n.format("gui.controller.structure.none")),
                defaultPanelId
            );
        }
        addBlank(linesByPanel, defaultPanelId);

        addWrapped(linesByPanel, panelMap, defaultPanelId, I18n.format("gui.controller.status"), defaultPanelId);
        addWrapped(linesByPanel, panelMap, defaultPanelId, I18n.format(controller.getControllerStatus().getUnlocMessage()), defaultPanelId);
        addBlank(linesByPanel, defaultPanelId);

        ActiveMachineRecipe activeRecipe = controller.getActiveRecipe();
        if (activeRecipe != null) {
            if (activeRecipe.getTotalTick() > 0) {
                int progress = (activeRecipe.getTick() * 100) / activeRecipe.getTotalTick();
                addWrapped(
                    linesByPanel,
                    panelMap,
                    defaultPanelId,
                    I18n.format("gui.controller.status.crafting.progress", progress + "%"),
                    defaultPanelId
                );
                addBlank(linesByPanel, defaultPanelId);
            }

            int parallelism = activeRecipe.getParallelism();
            int maxParallelism = activeRecipe.getMaxParallelism();
            if (parallelism > 1) {
                addWrapped(linesByPanel, panelMap, defaultPanelId, I18n.format("gui.controller.parallelism", parallelism), defaultPanelId);
                addWrapped(
                    linesByPanel,
                    panelMap,
                    defaultPanelId,
                    I18n.format("gui.controller.max_parallelism", maxParallelism),
                    defaultPanelId
                );
                addBlank(linesByPanel, defaultPanelId);
            }
        }

        int usedTimeCache = TileMultiblockMachineController.usedTimeCache;
        float searchUsedTimeCache = TileMultiblockMachineController.searchUsedTimeCache;
        String workMode = TileMultiblockMachineController.workModeCache.getDisplayName();
        addWrapped(
            linesByPanel,
            panelMap,
            defaultPanelId,
            String.format(
                "Avg: %dus/t (Search: %sms), WorkMode: %s",
                usedTimeCache,
                MiscUtils.formatFloat(searchUsedTimeCache / 1000F, 2),
                workMode
            ),
            defaultPanelId
        );

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
        int wrapWidth = Math.max(24, panel.rect.getWidth() - 8);
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

    private int getPanelBarX(PanelRect panel) {
        return panel.getRight() - 4;
    }

    private int getPanelBarTop(PanelRect panel) {
        return panel.getY() + 1;
    }

    private int getPanelBarWidth() {
        return 3;
    }

    private int getPanelBarHeight(PanelRect panel) {
        return panel.getHeight() - 2;
    }

    private int getPanelThumbHeight(String panelId, PanelRect panel) {
        return Math.max(12, (panel.getHeight() * panel.getHeight()) / (panel.getHeight() + getPanelMaxScroll(panelId) + 1));
    }

    private int getPanelThumbY(String panelId, PanelRect panel) {
        int barTop = getPanelBarTop(panel);
        int barHeight = getPanelBarHeight(panel);
        int thumbHeight = getPanelThumbHeight(panelId, panel);
        int maxTravel = Math.max(1, barHeight - thumbHeight);
        return barTop + (getPanelScroll(panelId) * maxTravel) / Math.max(1, getPanelMaxScroll(panelId));
    }

    private void updatePanelScrollFromMouse(String panelId, PanelRect panel, int localMouseY) {
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
        return cfg.useNineSlice;
    }

    private boolean getHideDefaultBackground(MMCEGuiExtConfig.MachineController cfg) {
        if (styleOverride.hideDefaultBackground != null) {
            return styleOverride.hideDefaultBackground.booleanValue();
        }
        return cfg.hideDefaultBackground;
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

    private static class PanelDef {
        private final String id;
        private final PanelRect rect;

        private PanelDef(String id, PanelRect rect) {
            this.id = id;
            this.rect = rect;
        }
    }
}
