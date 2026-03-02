package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import com.fushu.mmceguiext.common.network.PktControllerSmartInterfaceUpdate;
import github.kasuminova.mmce.common.event.client.ControllerGUIRenderEvent;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.gui.GuiContainerBase;
import hellfirepvp.modularmachinery.client.gui.widget.GuiScrollbar;
import hellfirepvp.modularmachinery.common.container.ContainerFactoryController;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceType;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.IllegalFormatException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class GuiFactoryControllerResizable extends GuiContainerBase<ContainerFactoryController> {
    private static final int BASE_WIDTH = 280;
    private static final int BASE_HEIGHT = 213;
    private static final int PLAYER_INVENTORY_TOP = 131;

    private static final ResourceLocation DEFAULT_BACKGROUND = new ResourceLocation(
        ModularMachinery.MODID,
        "textures/gui/guifactory.png"
    );
    private static final String DEFAULT_BACKGROUND_STR = "modularmachinery:textures/gui/guifactory.png";
    private static final ResourceLocation TEXTURES_FACTORY_ELEMENTS = new ResourceLocation(
        ModularMachinery.MODID,
        "textures/gui/guifactoryelements.png"
    );

    private static final double FONT_SCALE = 0.72;
    private static final int SCROLLBAR_TOP = 8;
    private static final int SCROLLBAR_LEFT = 94;
    private static final int SCROLLBAR_HEIGHT = 197;
    private static final int MAX_PAGE_ELEMENTS = 6;
    private static final int FACTORY_ELEMENT_WIDTH = 86;
    private static final int FACTORY_ELEMENT_HEIGHT = 32;
    private static final int TEXT_DRAW_OFFSET_X = 113;
    private static final int TEXT_DRAW_OFFSET_Y = 12;
    private static final int RECIPE_QUEUE_OFFSET_X = 8;
    private static final int RECIPE_QUEUE_OFFSET_Y = 8;
    private static final int DEFAULT_SPECIAL_THREAD_BG_COLOR = 0xFFB2E5FF;
    private static final int SMART_EDITOR_BUTTON_W = 10;
    private static final int SMART_EDITOR_BUTTON_H = 12;
    private static final int SMART_EDITOR_APPLY_W = 20;
    private static final int SMART_EDITOR_INPUT_H = 12;
    private static final int CUSTOM_EDITOR_BUTTON_ID_BASE = 6000;
    private static final int DEFAULT_RENDER_PRIORITY = 0;
    private static final int DEFAULT_SMART_EDITOR_PRIORITY = 10;

    private final GuiScrollbar recipeScrollbar = new GuiScrollbar();
    private final TileFactoryController factory;

    private MachineGuiStyleManager.ControllerStyle styleOverride = MachineGuiStyleManager.ControllerStyle.EMPTY;
    @Nullable
    private ResourceLocation customBackgroundTexture = null;
    private int specialThreadBgColor = DEFAULT_SPECIAL_THREAD_BG_COLOR;

    private int renderWidth = BASE_WIDTH;
    private int renderHeight = BASE_HEIGHT;

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
    private final List<TextureLayerDef> backgroundTextureLayers = new ArrayList<TextureLayerDef>();
    private final List<TextureLayerDef> foregroundTextureLayers = new ArrayList<TextureLayerDef>();
    private final Map<String, LayerRuntimeState> layerRuntimeStates = new HashMap<String, LayerRuntimeState>();
    private final Set<String> textureLayerIds = new HashSet<String>();

    public GuiFactoryControllerResizable(ContainerFactoryController container) {
        super(container);
        this.factory = container.getOwner();
    }

    @Override
    public void initGui() {
        this.styleOverride = MachineGuiStyleManager.resolveFactoryController(resolveMachine());
        this.customBackgroundTexture = resolveCustomTexture();
        this.specialThreadBgColor = resolveSpecialThreadBgColor();
        super.initGui();
        initTextureLayers(MMCEGuiExtConfig.factoryController);
        this.panelScroll.clear();
        this.panelMaxScroll.clear();
        this.draggingPanelId = null;
        this.infoScrollbarDragOffset = 0;
        initSmartInterfaceEditor();
        initCustomSmartInterfaceEditors(MMCEGuiExtConfig.factoryController);
    }

    @Override
    protected void setWidthHeight() {
        MMCEGuiExtConfig.FactoryController cfg = MMCEGuiExtConfig.factoryController;
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

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        resetSmartInterfaceEditorHints();
        MMCEGuiExtConfig.FactoryController cfg = MMCEGuiExtConfig.factoryController;
        int contentPriority = resolveForegroundContentPriority();
        int mouseScreenX = mouseX + this.guiLeft;
        int mouseScreenY = mouseY + this.guiTop;
        TreeSet<Integer> priorities = collectForegroundRenderPriorities();
        for (Integer priorityValue : priorities) {
            int priority = priorityValue.intValue();
            if (priority == contentPriority) {
                drawRecipeQueue();
                if (isUsingDefaultBackground(cfg)) {
                    drawDefaultFactoryStatus();
                } else {
                    drawStatusPanels();
                }
            }
            drawConfiguredTextureLayers(true, cfg, Integer.valueOf(priority));
            if (this.smartInterfaceEditorPriority == priority) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(-this.guiLeft, -this.guiTop, 0.0F);
                drawSmartInterfaceEditorBackground(mouseScreenX, mouseScreenY);
                GlStateManager.popMatrix();
                drawSmartInterfaceEditorForeground();
            }
            GlStateManager.pushMatrix();
            GlStateManager.translate(-this.guiLeft, -this.guiTop, 0.0F);
            drawCustomSmartInterfaceEditorsBackground(mouseScreenX, mouseScreenY, Integer.valueOf(priority));
            GlStateManager.popMatrix();
            drawCustomSmartInterfaceEditorsForeground(Integer.valueOf(priority));
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        MMCEGuiExtConfig.FactoryController cfg = MMCEGuiExtConfig.factoryController;
        if (isUsingDefaultBackground(cfg)) {
            this.mc.getTextureManager().bindTexture(DEFAULT_BACKGROUND);
            Gui.drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, BASE_WIDTH, BASE_HEIGHT, BASE_WIDTH, BASE_HEIGHT);
            drawConfiguredTextureLayers(false, cfg);
            updateRecipeScrollbar(this.guiLeft, this.guiTop);
            recipeScrollbar.draw(this, mc);
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
            drawResizableArea(
                this.guiLeft - textureOffsetX,
                this.guiTop - textureOffsetY,
                this.renderWidth + textureOffsetX,
                this.renderHeight + textureOffsetY,
                useNineSlice,
                texW,
                texH,
                getBackgroundCorner(cfg)
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
        drawConfiguredTextureLayers(false, cfg);

        updateRecipeScrollbar(this.guiLeft, this.guiTop);
        recipeScrollbar.draw(this, mc);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        if (isUsingDefaultBackground(MMCEGuiExtConfig.factoryController)) {
            recipeScrollbar.wheel(wheel);
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        String panelId = findHoveredPanelId(mouseX, mouseY, getActivePanels(MMCEGuiExtConfig.factoryController));
        if (panelId != null) {
            int step = Math.max(2, MMCEGuiExtConfig.wheelStep);
            int delta = wheel > 0 ? -step : step;
            int newScroll = getPanelScroll(panelId) + delta;
            int max = getPanelMaxScroll(panelId);
            panelScroll.put(panelId, MathHelper.clamp(newScroll, 0, max));
            return;
        }

        recipeScrollbar.wheel(wheel);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.smartInterfaceEditorInput != null) {
            this.smartInterfaceEditorInput.updateCursorCounter();
        }
        for (CustomSmartEditor editor : this.customSmartEditors) {
            if (editor.input != null) {
                editor.input.updateCursorCounter();
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
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
        if (handleCustomSmartInterfaceMouseClicked(mouseX, mouseY, mouseButton)) {
            return;
        }
        if (handleSmartInterfaceMouseClicked(mouseX, mouseY, mouseButton)) {
            return;
        }
        if (isUsingDefaultBackground(MMCEGuiExtConfig.factoryController)) {
            recipeScrollbar.click(mouseX, mouseY);
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        if (mouseButton == 0) {
            int localX = mouseX - this.guiLeft;
            int localY = mouseY - this.guiTop;
            for (PanelDef panel : getActivePanels(MMCEGuiExtConfig.factoryController)) {
                if (isOnPanelThumb(localX, localY, panel)) {
                    this.draggingPanelId = panel.id;
                    this.infoScrollbarDragOffset = localY - getPanelThumbY(panel.id, panel.rect);
                    super.mouseClicked(mouseX, mouseY, mouseButton);
                    return;
                }
            }
        }

        recipeScrollbar.click(mouseX, mouseY);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isUsingDefaultBackground(MMCEGuiExtConfig.factoryController)) {
            recipeScrollbar.click(mouseX, mouseY);
            super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
            return;
        }

        if (clickedMouseButton == 0 && this.draggingPanelId != null) {
            PanelDef panel = findPanelById(this.draggingPanelId, getActivePanels(MMCEGuiExtConfig.factoryController));
            if (panel != null) {
                int localY = mouseY - this.guiTop;
                updatePanelScrollFromMouse(panel.id, panel.rect, localY);
            }
        } else {
            recipeScrollbar.click(mouseX, mouseY);
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        this.draggingPanelId = null;
        super.mouseReleased(mouseX, mouseY, state);
    }

    private void drawRecipeQueue() {
        int offsetY = RECIPE_QUEUE_OFFSET_Y;
        int currentScroll = recipeScrollbar.getCurrentScroll();

        Collection<FactoryRecipeThread> coreThreadList = factory.getCoreRecipeThreads().values();
        List<FactoryRecipeThread> threadList = factory.getFactoryRecipeThreadList();
        List<FactoryRecipeThread> recipeThreadList = new ArrayList<FactoryRecipeThread>(
            (int) ((coreThreadList.size() + threadList.size()) * 1.5D)
        );
        recipeThreadList.addAll(coreThreadList);
        recipeThreadList.addAll(threadList);

        int visibleRows = isUsingDefaultBackground(MMCEGuiExtConfig.factoryController) ? MAX_PAGE_ELEMENTS : getVisibleQueueRows();
        int drawableSize = Math.min(visibleRows, Math.max(0, recipeThreadList.size() - currentScroll));
        for (int i = 0; i < drawableSize; i++) {
            FactoryRecipeThread thread = recipeThreadList.get(i + currentScroll);
            drawRecipeInfo(thread, i + currentScroll, offsetY);
            offsetY += FACTORY_ELEMENT_HEIGHT + 1;
        }
    }

    private void drawRecipeInfo(FactoryRecipeThread thread, int id, int offsetY) {
        CraftingStatus status = thread.getStatus();
        ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();

        this.mc.getTextureManager().bindTexture(TEXTURES_FACTORY_ELEMENTS);

        if (thread.isCoreThread()) {
            GuiRenderUtils.applyColorARGB(this.specialThreadBgColor);
        } else {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
        drawTexturedModalRect(RECIPE_QUEUE_OFFSET_X, offsetY, 0, 0, FACTORY_ELEMENT_WIDTH, FACTORY_ELEMENT_HEIGHT);

        if (status.isCrafting()) {
            GlStateManager.color(0.6F, 1.0F, 0.75F, 1.0F);
        } else {
            GlStateManager.color(1.0F, 0.6F, 0.6F, 1.0F);
        }

        if (activeRecipe != null && activeRecipe.getTotalTick() > 0) {
            float progress = (float) activeRecipe.getTick() / (float) activeRecipe.getTotalTick();
            drawTexturedModalRect(
                RECIPE_QUEUE_OFFSET_X,
                offsetY,
                0,
                0,
                (int) (FACTORY_ELEMENT_WIDTH * progress),
                FACTORY_ELEMENT_HEIGHT
            );
        }

        drawRecipeStatus(thread, id, offsetY + 2);
    }

    private void drawRecipeStatus(FactoryRecipeThread thread, int id, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.scale(FONT_SCALE, FONT_SCALE, FONT_SCALE);

        int offsetX = (int) (RECIPE_QUEUE_OFFSET_X / FONT_SCALE) + 2;
        int offsetY = (int) (y / FONT_SCALE);

        ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
        CraftingStatus status = thread.getStatus();
        int parallelism = activeRecipe == null ? 1 : activeRecipe.getParallelism();

        String threadName;
        if (thread.isCoreThread()) {
            String name = thread.getThreadName();
            threadName = I18n.hasKey(name) ? I18n.format(name) : name;
        } else {
            threadName = I18n.format("gui.factory.thread", id);
        }

        if (parallelism > 1) {
            this.fontRenderer.drawString(
                threadName + " (" + I18n.format("gui.controller.parallelism", parallelism) + ")",
                offsetX,
                offsetY,
                0x222222
            );
        } else {
            this.fontRenderer.drawString(threadName, offsetX, offsetY, 0x222222);
        }
        offsetY += 12;

        List<String> out = this.fontRenderer.listFormattedStringToWidth(
            I18n.format(status.getUnlocMessage()),
            (int) ((FACTORY_ELEMENT_WIDTH - 6) / FONT_SCALE)
        );
        for (String draw : out) {
            this.fontRenderer.drawString(draw, offsetX, offsetY, 0x222222);
            offsetY += 10;
        }

        if (activeRecipe != null && activeRecipe.getTotalTick() > 0) {
            int progress = (activeRecipe.getTick() * 100) / activeRecipe.getTotalTick();
            this.fontRenderer.drawString(
                I18n.format("gui.controller.status.crafting.progress", progress + "%"),
                offsetX,
                offsetY,
                0x222222
            );
        }

        GlStateManager.popMatrix();
    }

    private void drawDefaultFactoryStatus() {
        GlStateManager.pushMatrix();
        GlStateManager.scale(FONT_SCALE, FONT_SCALE, FONT_SCALE);

        int offsetX = (int) (TEXT_DRAW_OFFSET_X / FONT_SCALE);
        int offsetY = TEXT_DRAW_OFFSET_Y;

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int redstone = factory.getWorld().getStrongPower(factory.getPos());
        if (redstone > 0) {
            List<String> out = this.fontRenderer.listFormattedStringToWidth(
                I18n.format("gui.controller.status.redstone_stopped"),
                MathHelper.floor(135 * (1.0D / FONT_SCALE))
            );
            for (String draw : out) {
                this.fontRenderer.drawStringWithShadow(draw, offsetX, offsetY, 0xFFFFFF);
                offsetY += 10;
            }
            GlStateManager.popMatrix();
            return;
        }

        DynamicMachine machine = factory.getBlueprintMachine();
        offsetY = drawDefaultBlueprintInfo(offsetX, offsetY, machine, factory.isStructureFormed());

        DynamicMachine found = factory.getFoundMachine();
        offsetY = drawDefaultStructureInfo(offsetX, offsetY, found);

        if (!factory.isStructureFormed()) {
            GlStateManager.popMatrix();
            return;
        }
        offsetY += 15;

        offsetY = drawDefaultFactoryRecipeSearchStatusInfo(offsetX, offsetY);

        int tmp = offsetY;
        offsetY = drawDefaultFactoryThreadsInfo(offsetX, offsetY);
        offsetY = drawDefaultParallelismInfo(offsetX, offsetY);
        if (tmp != offsetY) {
            offsetY += 5;
        }

        int usedTimeCache = TileMultiblockMachineController.usedTimeCache;
        float searchUsedTimeCache = TileMultiblockMachineController.searchUsedTimeCache;
        String workMode = TileMultiblockMachineController.workModeCache.getDisplayName();
        this.fontRenderer.drawStringWithShadow(
            String.format(
                "Avg: %dus/t (Search: %sms), WorkMode: %s",
                usedTimeCache,
                MiscUtils.formatFloat(searchUsedTimeCache / 1000F, 2),
                workMode
            ),
            offsetX,
            offsetY,
            0xFFFFFF
        );

        GlStateManager.popMatrix();
    }

    private int drawDefaultBlueprintInfo(int offsetX, int y, @Nullable DynamicMachine machine, boolean formed) {
        int offsetY = y;
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
        } else if (!formed) {
            this.fontRenderer.drawStringWithShadow(
                I18n.format("gui.controller.blueprint", I18n.format("gui.controller.blueprint.none")),
                offsetX,
                offsetY,
                0xFFFFFF
            );
            offsetY += 15;
        }
        return offsetY;
    }

    private int drawDefaultStructureInfo(int offsetX, int y, @Nullable DynamicMachine found) {
        int offsetY = y;
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
            offsetY = drawDefaultExtraInfo(offsetX, offsetY);
        } else {
            this.fontRenderer.drawStringWithShadow(
                I18n.format("gui.controller.structure", I18n.format("gui.controller.structure.none")),
                offsetX,
                offsetY,
                0xFFFFFF
            );
            offsetY += 15;
        }
        return offsetY;
    }

    private int drawDefaultExtraInfo(int offsetX, int y) {
        int offsetY = y;
        ControllerGUIRenderEvent event = new ControllerGUIRenderEvent(factory);
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
        return offsetY;
    }

    private int drawDefaultFactoryRecipeSearchStatusInfo(int offsetX, int y) {
        if (!factory.hasIdleThread()) {
            return y;
        }
        int offsetY = y;

        this.fontRenderer.drawStringWithShadow(I18n.format("gui.controller.status"), offsetX, offsetY, 0xFFFFFF);
        List<String> out = this.fontRenderer.listFormattedStringToWidth(
            I18n.format(factory.getControllerStatus().getUnlocMessage()),
            MathHelper.floor(135 * (1.0D / FONT_SCALE))
        );
        for (String draw : out) {
            offsetY += 10;
            this.fontRenderer.drawStringWithShadow(draw, offsetX, offsetY, 0xFFFFFF);
        }
        return offsetY + 15;
    }

    private int drawDefaultFactoryThreadsInfo(int offsetX, int offsetY) {
        if (factory.getMaxThreads() <= 0) {
            return offsetY;
        }
        this.fontRenderer.drawStringWithShadow(
            I18n.format("gui.factory.threads", factory.getFactoryRecipeThreadList().size(), factory.getMaxThreads()),
            offsetX,
            offsetY,
            0xFFFFFF
        );
        return offsetY + 10;
    }

    private int drawDefaultParallelismInfo(int offsetX, int y) {
        int offsetY = y;

        int parallelism = 1;
        int maxParallelism = factory.getTotalParallelism();
        if (maxParallelism <= 1) {
            return offsetY;
        }

        for (FactoryRecipeThread thread : factory.getFactoryRecipeThreadList()) {
            ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
            if (activeRecipe != null) {
                parallelism += activeRecipe.getParallelism() - 1;
            }
        }
        for (FactoryRecipeThread thread : factory.getCoreRecipeThreads().values()) {
            ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
            if (activeRecipe != null) {
                parallelism += activeRecipe.getParallelism() - 1;
            }
        }

        if (parallelism <= 1) {
            return offsetY;
        }

        this.fontRenderer.drawStringWithShadow(I18n.format("gui.controller.parallelism", parallelism), offsetX, offsetY, 0xFFFFFF);
        offsetY += 10;
        this.fontRenderer.drawStringWithShadow(
            I18n.format("gui.controller.max_parallelism", maxParallelism),
            offsetX,
            offsetY,
            0xFFFFFF
        );
        offsetY += 10;
        return offsetY;
    }

    private void drawStatusPanels() {
        MMCEGuiExtConfig.FactoryController cfg = MMCEGuiExtConfig.factoryController;
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
            int viewportHeight = Math.max(1, panel.rect.height - 4);
            int maxScroll = Math.max(0, contentHeight - viewportHeight);
            panelMaxScroll.put(panel.id, maxScroll);

            int scroll = MathHelper.clamp(getPanelScroll(panel.id), 0, maxScroll);
            panelScroll.put(panel.id, scroll);

            int textX = panel.rect.x + 3;
            int textY = panel.rect.y + 2 - scroll;
            int clipX = this.guiLeft + panel.rect.x + 1;
            int clipY = this.guiTop + panel.rect.y + 1;
            int clipWidth = panel.rect.width - 2;
            int clipHeight = panel.rect.height - 2;

            GuiRenderUtils.enableScissor(this.mc, clipX, clipY, clipWidth, clipHeight);
            for (String line : lines) {
                this.fontRenderer.drawStringWithShadow(line, textX, textY, 0xFFFFFF);
                textY += lineHeight;
            }
            GuiRenderUtils.disableScissor();

            drawPanelScrollBar(panel);
        }
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

    private int resolveLayerPriority(TextureLayerDef layer) {
        LayerRuntimeState runtime = this.layerRuntimeStates.get(layer.id);
        return runtime != null && runtime.priority != null ? runtime.priority.intValue() : layer.priority;
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

    private List<PanelDef> getActivePanels(MMCEGuiExtConfig.FactoryController cfg) {
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
            int x = this.renderWidth > BASE_WIDTH + 24 ? BASE_WIDTH + 6 : 113;
            int y = Math.max(4, cfg.panelY);
            int width = Math.max(48, this.renderWidth - x - 8);
            int height = Math.max(24, this.renderHeight - y - 4);
            panels.add(new PanelDef("main", new Rectangle(x, y, width, height)));
        }
        return panels;
    }

    private Rectangle getDefaultPanelRect(MMCEGuiExtConfig.FactoryController cfg) {
        int panelX = cfg.panelX > 0 ? cfg.panelX : 113;
        panelX = Math.max(0, Math.min(panelX, Math.max(0, BASE_WIDTH - 32)));

        int panelY = Math.max(4, cfg.panelY);
        panelY = Math.min(panelY, PLAYER_INVENTORY_TOP - 24);

        int panelWidth = cfg.panelWidth > 0 ? cfg.panelWidth : 159;
        panelWidth = Math.max(48, Math.min(panelWidth, Math.max(48, BASE_WIDTH - panelX - 4)));
        panelWidth = Math.min(panelWidth, 159);

        int maxHeight = Math.max(24, PLAYER_INVENTORY_TOP - panelY - 4);
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

            return new PanelDef(id, new Rectangle(x, y, width, height));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Map<String, List<String>> buildPanelInfoLines(List<PanelDef> panels, MMCEGuiExtConfig.FactoryController cfg) {
        Map<String, List<String>> linesByPanel = new HashMap<String, List<String>>();
        Map<String, PanelDef> panelMap = toPanelMap(panels);
        for (PanelDef panel : panels) {
            linesByPanel.put(panel.id, new ArrayList<String>());
        }

        String defaultPanelId = resolveDefaultPanelId(panels, cfg.defaultPanelId);

        int redstone = factory.getWorld().getStrongPower(factory.getPos());
        if (redstone > 0) {
            addWrapped(linesByPanel, panelMap, defaultPanelId, I18n.format("gui.controller.status.redstone_stopped"), defaultPanelId);
            return linesByPanel;
        }

        DynamicMachine machine = factory.getBlueprintMachine();
        if (machine != null) {
            addWrapped(linesByPanel, panelMap, defaultPanelId, I18n.format("gui.controller.blueprint", ""), defaultPanelId);
            addWrapped(linesByPanel, panelMap, defaultPanelId, machine.getLocalizedName(), defaultPanelId);
            addBlank(linesByPanel, defaultPanelId);
        } else if (!factory.isStructureFormed()) {
            addWrapped(
                linesByPanel,
                panelMap,
                defaultPanelId,
                I18n.format("gui.controller.blueprint", I18n.format("gui.controller.blueprint.none")),
                defaultPanelId
            );
            addBlank(linesByPanel, defaultPanelId);
        }

        DynamicMachine found = factory.getFoundMachine();
        if (found != null) {
            addWrapped(linesByPanel, panelMap, defaultPanelId, I18n.format("gui.controller.structure", ""), defaultPanelId);
            addWrapped(linesByPanel, panelMap, defaultPanelId, found.getLocalizedName(), defaultPanelId);

            ControllerGUIRenderEvent event = new ControllerGUIRenderEvent(factory);
            event.postEvent();
            for (String extra : event.getExtraInfo()) {
                if (consumeGuiDirective(extra)) {
                    continue;
                }
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
            addBlank(linesByPanel, defaultPanelId);
        }

        if (!factory.isStructureFormed()) {
            return linesByPanel;
        }

        addBlank(linesByPanel, defaultPanelId);
        if (factory.hasIdleThread()) {
            addWrapped(linesByPanel, panelMap, defaultPanelId, I18n.format("gui.controller.status"), defaultPanelId);
            addWrapped(linesByPanel, panelMap, defaultPanelId, I18n.format(factory.getControllerStatus().getUnlocMessage()), defaultPanelId);
            addBlank(linesByPanel, defaultPanelId);
        }

        if (factory.getMaxThreads() > 0) {
            addWrapped(
                linesByPanel,
                panelMap,
                defaultPanelId,
                I18n.format("gui.factory.threads", factory.getFactoryRecipeThreadList().size(), factory.getMaxThreads()),
                defaultPanelId
            );
        }

        int parallelism = 1;
        int maxParallelism = factory.getTotalParallelism();
        if (maxParallelism > 1) {
            for (FactoryRecipeThread thread : factory.getFactoryRecipeThreadList()) {
                ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
                if (activeRecipe != null) {
                    parallelism += activeRecipe.getParallelism() - 1;
                }
            }
            for (FactoryRecipeThread thread : factory.getCoreRecipeThreads().values()) {
                ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
                if (activeRecipe != null) {
                    parallelism += activeRecipe.getParallelism() - 1;
                }
            }
            if (parallelism > 1) {
                addWrapped(linesByPanel, panelMap, defaultPanelId, I18n.format("gui.controller.parallelism", parallelism), defaultPanelId);
                addWrapped(
                    linesByPanel,
                    panelMap,
                    defaultPanelId,
                    I18n.format("gui.controller.max_parallelism", maxParallelism),
                    defaultPanelId
                );
            }
        }
        addBlank(linesByPanel, defaultPanelId);

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
        int wrapWidth = Math.max(24, panel.rect.width - 8);
        target.addAll(this.fontRenderer.listFormattedStringToWidth(text, wrapWidth));
    }

    private void addBlank(Map<String, List<String>> linesByPanel, String panelId) {
        List<String> target = linesByPanel.get(panelId);
        if (target != null) {
            target.add("");
        }
    }

    private void updateRecipeScrollbar(int displayX, int displayY) {
        boolean defaultBackground = isUsingDefaultBackground(MMCEGuiExtConfig.factoryController);
        int visibleRows = defaultBackground ? MAX_PAGE_ELEMENTS : getVisibleQueueRows();
        int scrollbarHeight = defaultBackground ? SCROLLBAR_HEIGHT : visibleRows * (FACTORY_ELEMENT_HEIGHT + 1) - 1;
        recipeScrollbar
            .setLeft(SCROLLBAR_LEFT + displayX)
            .setTop(SCROLLBAR_TOP + displayY)
            .setHeight(scrollbarHeight);

        Map<String, FactoryRecipeThread> coreThreads = factory.getCoreRecipeThreads();
        List<FactoryRecipeThread> threadList = factory.getFactoryRecipeThreadList();
        recipeScrollbar.setRange(0, Math.max(0, coreThreads.size() + threadList.size() - visibleRows), 1);
    }

    private int getVisibleQueueRows() {
        int maxByHeight = getMaxQueueRowsByHeight();
        int requested = Math.max(1, MMCEGuiExtConfig.factoryController.queueVisibleRows);
        return Math.min(requested, maxByHeight);
    }

    private int getMaxQueueRowsByHeight() {
        int available = Math.max(FACTORY_ELEMENT_HEIGHT, this.renderHeight - RECIPE_QUEUE_OFFSET_Y - 8);
        return Math.max(1, (available + 1) / (FACTORY_ELEMENT_HEIGHT + 1));
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
        DynamicMachine found = factory.getFoundMachine();
        if (found != null) {
            return found;
        }
        return factory.getBlueprintMachine();
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

        String configTexture = MMCEGuiExtConfig.factoryController.backgroundTexture;
        if (configTexture == null || configTexture.trim().isEmpty()) {
            return null;
        }
        if (DEFAULT_BACKGROUND_STR.equalsIgnoreCase(configTexture.trim())) {
            return null;
        }
        return GuiRenderUtils.parseOptionalTexture(configTexture);
    }

    private boolean getUseNineSlice(MMCEGuiExtConfig.FactoryController cfg) {
        if (styleOverride.useNineSlice != null) {
            return styleOverride.useNineSlice.booleanValue();
        }
        return cfg.useNineSlice;
    }

    private int getBackgroundTextureWidth(MMCEGuiExtConfig.FactoryController cfg) {
        if (styleOverride.backgroundTextureWidth != null) {
            return Math.max(16, styleOverride.backgroundTextureWidth.intValue());
        }
        if (hasMachineStyleOverride() && this.customBackgroundTexture != null) {
            return Math.max(16, this.renderWidth + getBackgroundTextureOffsetX(cfg));
        }
        return Math.max(16, cfg.backgroundTextureWidth);
    }

    private int getBackgroundTextureHeight(MMCEGuiExtConfig.FactoryController cfg) {
        if (styleOverride.backgroundTextureHeight != null) {
            return Math.max(16, styleOverride.backgroundTextureHeight.intValue());
        }
        if (hasMachineStyleOverride() && this.customBackgroundTexture != null) {
            return Math.max(16, this.renderHeight + getBackgroundTextureOffsetY(cfg));
        }
        return Math.max(16, cfg.backgroundTextureHeight);
    }

    private int getBackgroundCorner(MMCEGuiExtConfig.FactoryController cfg) {
        if (styleOverride.backgroundCorner != null) {
            return Math.max(2, styleOverride.backgroundCorner.intValue());
        }
        return Math.max(2, cfg.backgroundCorner);
    }

    private boolean getHideDefaultBackground(MMCEGuiExtConfig.FactoryController cfg) {
        if (styleOverride.hideDefaultBackground != null) {
            return styleOverride.hideDefaultBackground.booleanValue();
        }
        return cfg.hideDefaultBackground;
    }

    private int getBackgroundTextureOffsetX(MMCEGuiExtConfig.FactoryController cfg) {
        if (styleOverride.backgroundTextureOffsetX != null) {
            return Math.max(0, styleOverride.backgroundTextureOffsetX.intValue());
        }
        if (hasMachineStyleOverride()) {
            return 0;
        }
        return Math.max(0, cfg.backgroundTextureOffsetX);
    }

    private int getBackgroundTextureOffsetY(MMCEGuiExtConfig.FactoryController cfg) {
        if (styleOverride.backgroundTextureOffsetY != null) {
            return Math.max(0, styleOverride.backgroundTextureOffsetY.intValue());
        }
        if (hasMachineStyleOverride()) {
            return 0;
        }
        return Math.max(0, cfg.backgroundTextureOffsetY);
    }

    private int getRequestedGuiWidth(MMCEGuiExtConfig.FactoryController cfg) {
        if (styleOverride.guiWidth != null) {
            return styleOverride.guiWidth.intValue();
        }
        if (hasMachineStyleOverride()) {
            return BASE_WIDTH;
        }
        return cfg.guiWidth;
    }

    private int getRequestedGuiHeight(MMCEGuiExtConfig.FactoryController cfg) {
        if (styleOverride.guiHeight != null) {
            return styleOverride.guiHeight.intValue();
        }
        if (hasMachineStyleOverride()) {
            return BASE_HEIGHT;
        }
        return cfg.guiHeight;
    }

    private int resolveSpecialThreadBgColor() {
        if (styleOverride.specialThreadBackgroundColor != null) {
            return styleOverride.specialThreadBackgroundColor.intValue();
        }
        return GuiRenderUtils.parseColorARGBOrDefault(
            MMCEGuiExtConfig.factoryController.specialThreadBackgroundColor,
            DEFAULT_SPECIAL_THREAD_BG_COLOR
        );
    }

    private boolean getDisableRightExtension() {
        return styleOverride.disableRightExtension != null && styleOverride.disableRightExtension.booleanValue();
    }

    private boolean isUsingDefaultBackground(MMCEGuiExtConfig.FactoryController cfg) {
        return this.customBackgroundTexture == null && !getHideDefaultBackground(cfg) && !hasPerMachineSizeOverride();
    }

    private boolean hasPerMachineSizeOverride() {
        return styleOverride.guiWidth != null || styleOverride.guiHeight != null;
    }

    private boolean hasMachineStyleOverride() {
        return styleOverride != MachineGuiStyleManager.ControllerStyle.EMPTY;
    }

    @Nullable
    public Rectangle getJeiRightExtensionArea() {
        int extraWidth = this.renderWidth - BASE_WIDTH;
        if (extraWidth <= 0) {
            return null;
        }
        return new Rectangle(this.guiLeft + BASE_WIDTH, this.guiTop, extraWidth, this.renderHeight);
    }

    private void initTextureLayers(MMCEGuiExtConfig.FactoryController cfg) {
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

    private void drawConfiguredTextureLayers(boolean foreground, MMCEGuiExtConfig.FactoryController cfg) {
        drawConfiguredTextureLayers(foreground, cfg, null);
    }

    private void drawConfiguredTextureLayers(boolean foreground, MMCEGuiExtConfig.FactoryController cfg, @Nullable Integer priorityFilter) {
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
            drawLayerWithTransform(drawX, drawY, width, height, useNineSlice, texW, texH, corner, scaleX, scaleY, rotation);
        }
    }

    private void initCustomSmartInterfaceEditors(MMCEGuiExtConfig.FactoryController cfg) {
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

            int inputWidth = style.inputWidth == null ? getSmartInterfaceEditorInputWidth(cfg) : Math.max(40, style.inputWidth.intValue());
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
            editor.input.setMaxStringLength(24);

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
        MMCEGuiExtConfig.FactoryController cfg = MMCEGuiExtConfig.factoryController;
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
        this.smartInterfaceEditorInput.setMaxStringLength(24);

        this.smartInterfacePrevButton = new GuiButton(201, absX, absY, SMART_EDITOR_BUTTON_W, SMART_EDITOR_BUTTON_H, "<");
        this.smartInterfaceNextButton = new GuiButton(202, absX + SMART_EDITOR_BUTTON_W + 1, absY, SMART_EDITOR_BUTTON_W, SMART_EDITOR_BUTTON_H, ">");
        this.smartInterfaceApplyButton = new GuiButton(
            203,
            absX + SMART_EDITOR_BUTTON_W * 2 + 5 + inputWidth,
            absY,
            SMART_EDITOR_APPLY_W,
            SMART_EDITOR_BUTTON_H,
            "OK"
        );
        syncSmartInterfaceEditorInput();
    }

    private boolean shouldRenderSmartInterfaceEditor(MMCEGuiExtConfig.FactoryController cfg) {
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
        if (this.factory == null || !this.factory.isStructureFormed()) {
            return new SmartInterfaceData[0];
        }
        SmartInterfaceData[] list = this.factory.getSmartInterfaceDataList();
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

        MMCEGuiExtConfig.FactoryController cfg = MMCEGuiExtConfig.factoryController;
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

        MMCEGuiExtConfig.FactoryController cfg = MMCEGuiExtConfig.factoryController;
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
                infoText = "Key: " + virtualKey;
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

    private boolean handleSmartInterfaceMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return false;
        }
        if (this.smartInterfaceEditorInput == null || this.smartInterfacePrevButton == null
            || this.smartInterfaceNextButton == null || this.smartInterfaceApplyButton == null) {
            return false;
        }

        MMCEGuiExtConfig.FactoryController cfg = MMCEGuiExtConfig.factoryController;
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

        if (Character.isDigit(typedChar) || typedChar == '.' || typedChar == '-' || typedChar == 'E' || typedChar == 'e'
            || MiscUtils.isTextBoxKey(keyCode)) {
            this.smartInterfaceEditorInput.textboxKeyTyped(typedChar, keyCode);
            return true;
        }
        return true;
    }

    private void applySmartInterfaceEditorValue() {
        if (this.smartInterfaceEditorInput == null) {
            return;
        }
        MMCEGuiExtConfig.FactoryController cfg = MMCEGuiExtConfig.factoryController;
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

        try {
            float parsed = Float.parseFloat(text.trim());
            if (!Float.isFinite(parsed)) {
                return;
            }

            String interfaceType;
            if (hasData) {
                SmartInterfaceData current = dataList[this.smartInterfaceIndex];
                current.setValue(parsed);
                interfaceType = current.getType();
            } else {
                interfaceType = getSelectedSmartInterfaceVirtualKey(virtualKeys);
                if (interfaceType.isEmpty()) {
                    return;
                }
            }

            MMCEGuiExt.NET_CHANNEL.sendToServer(new PktControllerSmartInterfaceUpdate(this.factory.getPos(), interfaceType, parsed));
            this.smartInterfaceEditorInput.setText(Float.toString(parsed));
            if (useVirtualKeys) {
                this.smartInterfaceVirtualInputCache.put(interfaceType, Float.toString(parsed));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void syncSmartInterfaceEditorInput() {
        if (this.smartInterfaceEditorInput == null) {
            return;
        }
        SmartInterfaceData[] dataList = getSmartInterfaceDataList();
        MMCEGuiExtConfig.FactoryController cfg = MMCEGuiExtConfig.factoryController;
        List<String> virtualKeys = getSmartInterfaceEditorVirtualKeys(cfg);
        if (!virtualKeys.isEmpty()) {
            clampSmartInterfaceIndex(virtualKeys.size());
            String selectedKey = getSelectedSmartInterfaceVirtualKey(virtualKeys);
            String cached = this.smartInterfaceVirtualInputCache.get(selectedKey);
            boolean switched = this.smartInterfaceActiveVirtualKey == null || !this.smartInterfaceActiveVirtualKey.equals(selectedKey);
            this.smartInterfaceActiveVirtualKey = selectedKey;
            if (cached != null) {
                this.smartInterfaceEditorInput.setText(cached);
            } else if (switched || this.smartInterfaceEditorInput.getText() == null) {
                this.smartInterfaceEditorInput.setText("");
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
        this.smartInterfaceEditorInput.setText(Float.toString(dataList[this.smartInterfaceIndex].getValue()));
    }

    private void drawCustomSmartInterfaceEditorsBackground(int mouseX, int mouseY, @Nullable Integer priorityFilter) {
        for (CustomSmartEditor editor : this.customSmartEditors) {
            if (priorityFilter != null && editor.priority != priorityFilter.intValue()) {
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
            if (editor.input == null || !editor.input.isFocused()) {
                continue;
            }
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                applyCustomSmartEditorValue(editor);
                return true;
            }
            if (Character.isDigit(typedChar) || typedChar == '.' || typedChar == '-' || typedChar == 'E' || typedChar == 'e'
                || MiscUtils.isTextBoxKey(keyCode)) {
                editor.input.textboxKeyTyped(typedChar, keyCode);
                return true;
            }
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
        try {
            float parsed = Float.parseFloat(text.trim());
            if (!Float.isFinite(parsed)) {
                return;
            }
            MMCEGuiExt.NET_CHANNEL.sendToServer(new PktControllerSmartInterfaceUpdate(this.factory.getPos(), key, parsed));
            String normalized = Float.toString(parsed);
            editor.input.setText(normalized);
            this.smartInterfaceVirtualInputCache.put(key, normalized);
        } catch (NumberFormatException ignored) {
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

    private TreeSet<Integer> collectForegroundRenderPriorities() {
        TreeSet<Integer> priorities = new TreeSet<Integer>();
        priorities.add(Integer.valueOf(resolveForegroundContentPriority()));
        for (TextureLayerDef layer : this.foregroundTextureLayers) {
            priorities.add(Integer.valueOf(resolveLayerPriority(layer)));
        }
        if (this.smartInterfaceEditorInput != null) {
            priorities.add(Integer.valueOf(this.smartInterfaceEditorPriority));
        }
        for (CustomSmartEditor editor : this.customSmartEditors) {
            priorities.add(Integer.valueOf(editor.priority));
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

    private boolean getSmartInterfaceEditorEnabled(MMCEGuiExtConfig.FactoryController cfg) {
        if (styleOverride.enableSmartInterfaceEditor != null) {
            return styleOverride.enableSmartInterfaceEditor.booleanValue();
        }
        return cfg.enableSmartInterfaceEditor;
    }

    private int getSmartInterfaceEditorX(MMCEGuiExtConfig.FactoryController cfg) {
        if (styleOverride.smartInterfaceEditorX != null) {
            return styleOverride.smartInterfaceEditorX.intValue();
        }
        return cfg.smartInterfaceEditorX;
    }

    private int getSmartInterfaceEditorY(MMCEGuiExtConfig.FactoryController cfg) {
        if (styleOverride.smartInterfaceEditorY != null) {
            return styleOverride.smartInterfaceEditorY.intValue();
        }
        return cfg.smartInterfaceEditorY;
    }

    private int getSmartInterfaceEditorInputWidth(MMCEGuiExtConfig.FactoryController cfg) {
        if (styleOverride.smartInterfaceEditorInputWidth != null) {
            return Math.max(40, styleOverride.smartInterfaceEditorInputWidth.intValue());
        }
        return Math.max(40, cfg.smartInterfaceEditorInputWidth);
    }

    private List<String> getSmartInterfaceEditorVirtualKeys(MMCEGuiExtConfig.FactoryController cfg) {
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

    private int resolveSmartInterfaceEditorX(MMCEGuiExtConfig.FactoryController cfg, int totalWidth) {
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

    private int resolveSmartInterfaceEditorY(MMCEGuiExtConfig.FactoryController cfg) {
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
        private Boolean visible;
    }

    private static class CustomSmartEditor {
        private String id;
        private int x;
        private int y;
        private int priority = DEFAULT_SMART_EDITOR_PRIORITY;
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

    private static class PanelDef {
        private final String id;
        private final Rectangle rect;

        private PanelDef(String id, Rectangle rect) {
            this.id = id;
            this.rect = rect;
        }
    }
}


