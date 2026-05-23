package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.fushu.mmceguiext.client.config.GlobalGuiStyleManager;
import com.fushu.mmceguiext.common.container.ContainerFluidProcessorHatchCustom;
import com.fushu.mmceguiext.common.registry.CustomHatchRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fluids.FluidStack;
import mekanism.api.gas.GasStack;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.input.Mouse;

public class GuiFluidProcessorHatchCustom extends GuiContainer {
    private static final ResourceLocation DEFAULT_TEXTURE =
        new ResourceLocation("modularmachinery", "textures/gui/guibar.png");
    private static final ResourceLocation MMCE_FLUID_HATCH_TEXTURE =
        new ResourceLocation("modularmachinery", "textures/gui/guibar.png");
    private static final int PLAYER_INV_TOP = 84;
    private static final int PLAYER_HOTBAR_TOP = 142;

    private final TileEntity owner;
    private final CustomHatchRegistry.CustomHatchDef definition;
    private final java.util.List<CustomHatchRegistry.ComponentDef> components;
    private GlobalGuiStyleManager.StyleFile styleFile = GlobalGuiStyleManager.StyleFile.EMPTY;
    @Nullable
    private ResourceLocation backgroundTexture;
    private List<GlobalTextureLayerConfig.LayerDef> textureLayers = Collections.emptyList();
    private int backgroundOffsetX;
    private int backgroundOffsetY;
    private int backgroundTextureWidth = 176;
    private int backgroundTextureHeight = 166;
    private int coordinateWidth = 176;
    private int coordinateHeight = 166;
    private final Map<String, SlotGridState> slotGridStates = new HashMap<String, SlotGridState>();

    public GuiFluidProcessorHatchCustom(TileEntity owner, EntityPlayer opening, CustomHatchRegistry.CustomHatchDef definition) {
        super(new ContainerFluidProcessorHatchCustom(owner, opening, definition));
        this.owner = owner;
        this.definition = definition;
        this.components = definition.gui == null ? Collections.emptyList() : definition.gui.components;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        this.styleFile = GlobalGuiStyleManager.load(definition.guiStyleFile);
        this.backgroundTexture = resolveBackgroundTexture();
        this.textureLayers = !this.styleFile.layers.isEmpty()
            ? this.styleFile.layers
            : Collections.<GlobalTextureLayerConfig.LayerDef>emptyList();
        updateBackgroundMetrics();
        if (this.definition.gui != null) {
            this.xSize = this.definition.gui.width > 0 ? this.definition.gui.width : this.backgroundTextureWidth;
            this.ySize = this.definition.gui.height > 0 ? this.definition.gui.height : this.backgroundTextureHeight;
        }
        updateCoordinateMetrics();
        super.initGui();
        updateSlotGridScrollbars();
        applyConfiguredSlotPositions();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        int offsetX = this.backgroundOffsetX;
        int offsetY = this.backgroundOffsetY;
        int texW = this.backgroundTextureWidth;
        int texH = this.backgroundTextureHeight;
        int corner = this.styleFile.background != null && this.styleFile.background.corner != null
            ? this.styleFile.background.corner.intValue()
            : 8;
        boolean useNineSlice = this.styleFile.background != null
            && this.styleFile.background.useNineSlice != null
            && this.styleFile.background.useNineSlice.booleanValue();

        this.mc.getTextureManager().bindTexture(this.backgroundTexture == null ? DEFAULT_TEXTURE : this.backgroundTexture);
        if (useNineSlice) {
            GuiRenderUtils.drawNineSlice(left - offsetX, top - offsetY, this.xSize + offsetX, this.ySize + offsetY, texW, texH, corner);
        } else {
            Gui.drawModalRectWithCustomSizedTexture(left - offsetX, top - offsetY, 0, 0, this.xSize + offsetX, this.ySize + offsetY, texW, texH);
        }
        GlobalTextureLayerConfig.drawLayers(this.textureLayers, false, left, top, offsetX, offsetY);
        drawTankOverlays(left, top);
        drawSlotGridScrollbars(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        drawConfiguredTexts();
        GlobalTextureLayerConfig.drawLayers(this.textureLayers, true, 0, 0, 0, 0);
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        handleSlotGridWheel(mouseX, mouseY, wheel);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        if (mouseButton == 0) {
            clickSlotGridScrollbars(mouseX, mouseY);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        releaseSlotGridScrollbars();
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        super.renderHoveredToolTip(mouseX, mouseY);
        CustomHatchRegistry.ComponentDef tankComponent = findHoveredTankComponent(mouseX, mouseY);
        if (tankComponent == null) {
            return;
        }
        FluidStack fluid = getFluid();
        GasStack gas = getGas();
        boolean useGas = shouldUseGas(tankComponent, fluid, gas);
        int amount = useGas ? (gas == null ? 0 : gas.amount) : (fluid == null ? 0 : fluid.amount);
        int capacity = Math.max(1, getTankCapacity(tankComponent, useGas));
        List<String> tooltip = new ArrayList<String>();
        tooltip.add(useGas
            ? gas == null ? I18n.format("tooltip.fluidhatch.empty") : gas.getGas().getLocalizedName()
            : fluid == null ? I18n.format("tooltip.fluidhatch.empty") : fluid.getLocalizedName());
        tooltip.add(I18n.format("tooltip.fluidhatch.tank", String.valueOf(amount), String.valueOf(capacity)));
        drawHoveringText(tooltip, mouseX, mouseY, this.fontRenderer);
    }

    private void drawConfiguredTexts() {
        FluidStack fluid = getFluid();
        GasStack gas = getGas();
        boolean useGas = shouldUseGas(null, fluid, gas);
        int amount = useGas ? (gas == null ? 0 : gas.amount) : (fluid == null ? 0 : fluid.amount);
        int capacity = getTankCapacity(null, useGas);
        for (CustomHatchRegistry.ComponentDef component : this.components) {
            if (component == null || component.value == null || !("text".equalsIgnoreCase(component.type))) {
                continue;
            }
            String value = resolveTextValue(component.value, fluid, gas, useGas, amount, capacity);
            if (value == null || value.isEmpty()) {
                continue;
            }
            int color = GuiRenderUtils.parseColorARGBOrDefault(component.color, 0xFFFFFF);
            float scale = component.scale == null ? 1.0F : component.scale.floatValue();
            int alignedX = GuiRenderUtils.resolveAlignedTextX(
                componentGuiX(component),
                Math.round(this.fontRenderer.getStringWidth(value) * scale),
                component.align
            );
            if (scale != 1.0F) {
                GlStateManager.pushMatrix();
                GlStateManager.scale(scale, scale, 1.0F);
                this.fontRenderer.drawStringWithShadow(value, Math.round(alignedX / scale), Math.round(componentGuiY(component) / scale), color);
                GlStateManager.popMatrix();
            } else {
                this.fontRenderer.drawStringWithShadow(value, alignedX, componentGuiY(component), color);
            }
        }
    }

    private void drawTankOverlays(int left, int top) {
        boolean drewConfiguredTank = false;
        for (CustomHatchRegistry.ComponentDef component : this.components) {
            if (component == null || !"tank".equalsIgnoreCase(component.type)) {
                continue;
            }
            drawTankOverlay(left, top, component);
            drewConfiguredTank = true;
        }
        if (!drewConfiguredTank && definition.tank.width > 0 && definition.tank.height > 0) {
            drawTankOverlay(left, top, null);
        }
    }

    private void drawTankOverlay(int left, int top, @Nullable CustomHatchRegistry.ComponentDef component) {
        FluidStack fluid = getFluid();
        GasStack gas = getGas();
        boolean useGas = shouldUseGas(component, fluid, gas);
        int amount = useGas ? (gas == null ? 0 : gas.amount) : (fluid == null ? 0 : fluid.amount);
        int capacity = Math.max(1, getTankCapacity(component, useGas));
        int rawX = component == null ? definition.tank.x : component.x;
        int rawY = component == null ? definition.tank.y : component.y;
        int rawWidth = component == null ? definition.tank.width : component.width;
        int rawHeight = component == null ? definition.tank.height : component.height;
        if (rawWidth <= 0 || rawHeight <= 0) {
            return;
        }
        int tankX = left + scaledX(rawX) - this.backgroundOffsetX;
        int tankY = top + scaledY(rawY) - this.backgroundOffsetY;
        int tankWidth = scaledWidth(rawWidth);
        int tankHeight = scaledHeight(rawHeight);
        if (useGas && gas != null && amount > 0) {
            int gasColor = gas.getGas().getTint();
            float red = (gasColor >> 16 & 0xFF) / 255F;
            float green = (gasColor >> 8 & 0xFF) / 255F;
            float blue = (gasColor & 0xFF) / 255F;
            float filledPercent = MathHelper.clamp(amount / (float) capacity, 0F, 1F);
            int filled = MathHelper.ceil(filledPercent * tankHeight);
            TextureAtlasSprite sprite = gas.getGas().getSprite();
            if (sprite == null) {
                sprite = Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
            }

            GlStateManager.color(red, green, blue, 1.0F);
            this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            drawTiledFluidSprite(tankX, tankY + tankHeight - filled, tankWidth, filled, sprite);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        } else if (fluid != null && amount > 0) {
            int fluidColor = fluid.getFluid().getColor(fluid);
            float red = (fluidColor >> 16 & 0xFF) / 255F;
            float green = (fluidColor >> 8 & 0xFF) / 255F;
            float blue = (fluidColor & 0xFF) / 255F;

            float filledPercent = MathHelper.clamp(amount / (float) capacity, 0F, 1F);
            int filled = MathHelper.ceil(filledPercent * tankHeight);
            ResourceLocation still = fluid.getFluid().getStill(fluid);
            TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getTextureExtry(still.toString());
            if (sprite == null) {
                sprite = Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
            }

            GlStateManager.color(red, green, blue, 1.0F);
            this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            drawTiledFluidSprite(tankX, tankY + tankHeight - filled, tankWidth, filled, sprite);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }

        boolean overlay = component == null ? true : Boolean.TRUE.equals(component.overlay);
        if (overlay) {
            this.mc.getTextureManager().bindTexture(MMCE_FLUID_HATCH_TEXTURE);
            this.drawTexturedModalRect(tankX, tankY, 176, 0, tankWidth, tankHeight);
        }
    }

    private void drawTiledFluidSprite(int x, int y, int width, int height, TextureAtlasSprite sprite) {
        int remainingHeight = height;
        int drawY = y;
        while (remainingHeight > 0) {
            int tileHeight = Math.min(16, remainingHeight);
            int remainingWidth = width;
            int drawX = x;
            while (remainingWidth > 0) {
                int tileWidth = Math.min(16, remainingWidth);
                drawTexturedModalRect(drawX, drawY, sprite, tileWidth, tileHeight);
                drawX += tileWidth;
                remainingWidth -= tileWidth;
            }
            drawY += tileHeight;
            remainingHeight -= tileHeight;
        }
    }

    @Nullable
    private CustomHatchRegistry.ComponentDef findHoveredTankComponent(int mouseX, int mouseY) {
        for (CustomHatchRegistry.ComponentDef component : this.components) {
            if (component == null || !"tank".equalsIgnoreCase(component.type)) {
                continue;
            }
            if (isMouseOverTank(mouseX, mouseY, component.x, component.y, component.width, component.height)) {
                return component;
            }
        }
        return isMouseOverTank(mouseX, mouseY, definition.tank.x, definition.tank.y, definition.tank.width, definition.tank.height) ? null : null;
    }

    private boolean isMouseOverTank(int mouseX, int mouseY, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return false;
        }
        int left = this.guiLeft + scaledX(x) - this.backgroundOffsetX;
        int top = this.guiTop + scaledY(y) - this.backgroundOffsetY;
        int scaledWidth = scaledWidth(width);
        int scaledHeight = scaledHeight(height);
        return mouseX >= left && mouseX < left + scaledWidth
            && mouseY >= top && mouseY < top + scaledHeight;
    }

    @Nullable
    private String resolveTextValue(@Nullable String key, @Nullable FluidStack fluid, @Nullable GasStack gas, boolean useGas, int amount, int capacity) {
        if (key == null) {
            return null;
        }
        if ("fluid.name".equalsIgnoreCase(key)) {
            return fluid == null ? I18n.format("tooltip.fluidhatch.empty") : fluid.getLocalizedName();
        }
        if ("gas.name".equalsIgnoreCase(key)) {
            return gas == null ? I18n.format("tooltip.fluidhatch.empty") : gas.getGas().getLocalizedName();
        }
        if ("tank.name".equalsIgnoreCase(key)) {
            return useGas
                ? gas == null ? I18n.format("tooltip.fluidhatch.empty") : gas.getGas().getLocalizedName()
                : fluid == null ? I18n.format("tooltip.fluidhatch.empty") : fluid.getLocalizedName();
        }
        if ("tank.amount".equalsIgnoreCase(key)) {
            return Integer.toString(amount);
        }
        if ("gas.amount".equalsIgnoreCase(key)) {
            return Integer.toString(gas == null ? 0 : gas.amount);
        }
        if ("tank.capacity".equalsIgnoreCase(key)) {
            return Integer.toString(capacity);
        }
        if ("gas.capacity".equalsIgnoreCase(key)) {
            return Integer.toString(getGasCapacity());
        }
        if ("tank.amount_capacity".equalsIgnoreCase(key)) {
            return amount + " / " + capacity;
        }
        if ("gas.amount_capacity".equalsIgnoreCase(key)) {
            return (gas == null ? 0 : gas.amount) + " / " + getGasCapacity();
        }
        if ("input.slot".equalsIgnoreCase(key)) {
            return "Input";
        }
        if ("output.slot".equalsIgnoreCase(key)) {
            return "Output";
        }
        return key;
    }

    private boolean shouldUseGas(@Nullable CustomHatchRegistry.ComponentDef component, @Nullable FluidStack fluid, @Nullable GasStack gas) {
        String content = component != null && component.content != null ? component.content : definition.tank.content;
        content = content == null ? "fluid" : content;
        if ("gas".equalsIgnoreCase(content)) {
            return true;
        }
        return "fluid_gas".equalsIgnoreCase(content) && (fluid == null || fluid.amount <= 0) && gas != null && gas.amount > 0;
    }

    private int getTankCapacity(@Nullable CustomHatchRegistry.ComponentDef component, boolean useGas) {
        String content = component != null && component.content != null ? component.content : definition.tank.content;
        content = content == null ? "fluid" : content;
        if ("gas".equalsIgnoreCase(content)) {
            return getGasCapacity();
        }
        if ("fluid_gas".equalsIgnoreCase(content)) {
            return getFluidCapacity();
        }
        return useGas ? getGasCapacity() : getFluidCapacity();
    }

    @Nullable
    private ResourceLocation resolveBackgroundTexture() {
        if (this.styleFile.background != null && this.styleFile.background.texture != null && !this.styleFile.background.texture.trim().isEmpty()) {
            ResourceLocation texture = parseCustomHatchTexture(this.styleFile.background.texture);
            if (texture != null) {
                return texture;
            }
        }
        if (MMCEGuiExtConfig.fluidHatch.backgroundTexture != null && !MMCEGuiExtConfig.fluidHatch.backgroundTexture.trim().isEmpty()) {
            return GuiRenderUtils.parseOptionalTexture(MMCEGuiExtConfig.fluidHatch.backgroundTexture);
        }
        return DEFAULT_TEXTURE;
    }

    @Nullable
    private FluidStack getFluid() {
        try {
            Method method = owner.getClass().getMethod("getFluidStack");
            Object result = method.invoke(owner);
            return result instanceof FluidStack ? ((FluidStack) result).copy() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private int getCapacity() {
        return getFluidCapacity();
    }

    private int getFluidCapacity() {
        try {
            Method method = owner.getClass().getMethod("getFluidCapacity");
            Object result = method.invoke(owner);
            return result instanceof Integer ? ((Integer) result).intValue() : 0;
        } catch (Exception ignored) {
            try {
                Method method = owner.getClass().getMethod("getCapacity");
                Object result = method.invoke(owner);
                return result instanceof Integer ? ((Integer) result).intValue() : 0;
            } catch (Exception ignoredAgain) {
                return 0;
            }
        }
    }

    private int getGasCapacity() {
        try {
            Method method = owner.getClass().getMethod("getGasCapacity");
            Object result = method.invoke(owner);
            return result instanceof Integer ? ((Integer) result).intValue() : getFluidCapacity();
        } catch (Exception ignored) {
            return getFluidCapacity();
        }
    }

    @Nullable
    @Optional.Method(modid = "mekanism")
    private GasStack getGas() {
        try {
            Method method = owner.getClass().getMethod("getGasStack");
            Object result = method.invoke(owner);
            return result instanceof GasStack ? ((GasStack) result).copy() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void applyConfiguredSlotPositions() {
        CustomHatchRegistry.ComponentDef playerInventory = findComponent("player_inventory");
        int playerInvX = 8;
        int playerInvTop = Math.max(84, this.ySize - 82);
        int playerHotbarTop = playerInvTop + 58;
        if (playerInventory != null) {
            playerInvX = componentGuiX(playerInventory);
            playerInvTop = componentGuiY(playerInventory);
            playerHotbarTop = playerInventory.hotbarY >= 0
                ? scaledY(playerInventory.hotbarY) - this.backgroundOffsetY
                : playerInvTop + 58;
        }
        for (int i = 0; i < this.inventorySlots.inventorySlots.size(); i++) {
            net.minecraft.inventory.Slot slot = this.inventorySlots.inventorySlots.get(i);
            if (i < 27) {
                slot.xPos = playerInvX + (i % 9) * 18;
                slot.yPos = playerInvTop + ((i / 9) * 18);
            } else if (i < 36) {
                slot.xPos = playerInvX + (i - 27) * 18;
                slot.yPos = playerHotbarTop;
            } else {
                CustomHatchRegistry.ComponentDef component = findSlotComponent(i - 36);
                if (component != null) {
                    positionCustomSlot(slot, component);
                } else if (i == 36) {
                    slot.xPos = scaledX(definition.inputSlot.x) - this.backgroundOffsetX;
                    slot.yPos = scaledY(definition.inputSlot.y) - this.backgroundOffsetY;
                } else if (i == 37) {
                    slot.xPos = scaledX(definition.outputSlot.x) - this.backgroundOffsetX;
                    slot.yPos = scaledY(definition.outputSlot.y) - this.backgroundOffsetY;
                }
            }
        }
    }

    private void positionCustomSlot(net.minecraft.inventory.Slot slot, CustomHatchRegistry.ComponentDef component) {
        SlotGridState state = getSlotGridState(component);
        if (state == null) {
            slot.xPos = componentGuiX(component);
            slot.yPos = componentGuiY(component);
            return;
        }
        int localIndex = component.index - state.baseIndex;
        if (localIndex < 0) {
            slot.xPos = componentGuiX(component);
            slot.yPos = componentGuiY(component);
            return;
        }
        int row = localIndex / state.columns;
        int column = localIndex % state.columns;
        if (column >= state.visibleColumns || row < state.scrollOffset || row >= state.scrollOffset + state.visibleRows) {
            slot.xPos = -10000;
            slot.yPos = -10000;
            return;
        }
        int visibleRow = row - state.scrollOffset;
        int stepX = state.slotSize + state.spacingX;
        int stepY = state.slotSize + state.spacingY;
        slot.xPos = scaledX(state.baseX + column * stepX) - this.backgroundOffsetX;
        slot.yPos = scaledY(state.baseY + visibleRow * stepY) - this.backgroundOffsetY;
    }

    @Nullable
    private CustomHatchRegistry.ComponentDef findSlotComponent(int guiSlotOrdinal) {
        int ordinal = 0;
        for (CustomHatchRegistry.ComponentDef component : this.components) {
            if (component == null || !"slot".equalsIgnoreCase(component.type)) {
                continue;
            }
            if (ordinal++ == guiSlotOrdinal) {
                return component;
            }
        }
        return null;
    }

    @Nullable
    private CustomHatchRegistry.ComponentDef findComponent(String type) {
        for (CustomHatchRegistry.ComponentDef component : this.components) {
            if (component != null && type.equalsIgnoreCase(component.type)) {
                return component;
            }
        }
        return null;
    }

    @Nullable
    private SlotGridState getSlotGridState(@Nullable CustomHatchRegistry.ComponentDef component) {
        if (component == null || component.index < 0) {
            return null;
        }
        String key = buildSlotGridKey(component);
        return this.slotGridStates.get(key);
    }

    private void updateSlotGridScrollbars() {
        this.slotGridStates.clear();
        for (CustomHatchRegistry.ComponentDef component : this.components) {
            if (component == null || !"slot".equalsIgnoreCase(component.type) || component.index < 0) {
                continue;
            }
            if (component.rows <= 0 || component.columns <= 0 || component.visibleRows <= 0 || component.visibleRows >= component.rows) {
                continue;
            }
            String key = buildSlotGridKey(component);
            if (this.slotGridStates.containsKey(key)) {
                continue;
            }
            SlotGridState state = new SlotGridState();
            state.key = key;
            state.baseIndex = component.gridBaseIndex;
            state.baseX = component.gridBaseX;
            state.baseY = component.gridBaseY;
            state.rows = Math.max(1, component.rows);
            state.columns = Math.max(1, component.columns);
            state.visibleRows = Math.max(1, Math.min(component.visibleRows, component.rows));
            state.visibleColumns = component.visibleColumns > 0 ? Math.min(component.visibleColumns, component.columns) : component.columns;
            state.spacingX = component.spacingX;
            state.spacingY = component.spacingY;
            state.slotSize = Math.max(1, component.slotSize);
            state.scrollMode = "page".equalsIgnoreCase(component.scrollMode) ? ScrollMode.PAGE : ScrollMode.ROW;
            state.maxScroll = Math.max(0, state.rows - state.visibleRows);
            state.scrollbarEnabled = component.scrollbar == null || component.scrollbar.booleanValue();
            int scrollbarX = component.scrollbarX != 0 ? component.scrollbarX : state.baseX + state.visibleColumns * (state.slotSize + state.spacingX) + 2;
            int scrollbarY = component.scrollbarY != 0 ? component.scrollbarY : state.baseY;
            int scrollbarHeight = component.scrollbarHeight > 0 ? component.scrollbarHeight : state.visibleRows * (state.slotSize + state.spacingY) - state.spacingY;
            state.scrollbar = new CustomScrollbarState();
            state.scrollbar.left = this.guiLeft + scaledX(scrollbarX) - this.backgroundOffsetX;
            state.scrollbar.top = this.guiTop + scaledY(scrollbarY) - this.backgroundOffsetY;
            state.scrollbar.height = Math.max(15, scaledHeight(scrollbarHeight));
            state.scrollbar.width = Math.max(4, scaledWidth(component.scrollbarWidth));
            state.scrollbar.thumbHeight = Math.max(8, scaledHeight(component.scrollbarThumbHeight));
            state.scrollbar.texture = component.scrollbarTexture == null ? null : GuiRenderUtils.parseOptionalTexture(component.scrollbarTexture);
            state.scrollbar.textureWidth = Math.max(1, component.scrollbarTextureWidth);
            state.scrollbar.textureHeight = Math.max(1, component.scrollbarTextureHeight);
            state.scrollbar.u = component.scrollbarU;
            state.scrollbar.v = component.scrollbarV;
            state.scrollbar.hoverU = component.scrollbarHoverU;
            state.scrollbar.hoverV = component.scrollbarHoverV;
            state.scrollbar.pressedU = component.scrollbarPressedU;
            state.scrollbar.pressedV = component.scrollbarPressedV;
            state.scrollbar.disabledU = component.scrollbarDisabledU;
            state.scrollbar.disabledV = component.scrollbarDisabledV;
            state.scrollbar.setRange(0, state.maxScroll, state.scrollMode == ScrollMode.PAGE ? state.visibleRows : 1);
            this.slotGridStates.put(key, state);
        }
    }

    private void handleSlotGridWheel(int mouseX, int mouseY, int wheel) {
        for (SlotGridState state : this.slotGridStates.values()) {
            if (!isMouseOverSlotGrid(mouseX, mouseY, state)) {
                continue;
            }
            state.scrollbar.wheel(wheel);
            state.scrollOffset = state.scrollbar.currentScroll;
            applyConfiguredSlotPositions();
            return;
        }
    }

    private void clickSlotGridScrollbars(int mouseX, int mouseY) {
        for (SlotGridState state : this.slotGridStates.values()) {
            if (!state.scrollbarEnabled || state.scrollbar == null) {
                continue;
            }
            if (!state.scrollbar.isMouseOver(mouseX, mouseY)) {
                continue;
            }
            state.scrollbar.click(mouseX, mouseY);
            state.scrollOffset = state.scrollbar.currentScroll;
            applyConfiguredSlotPositions();
            return;
        }
    }

    private void releaseSlotGridScrollbars() {
        for (SlotGridState state : this.slotGridStates.values()) {
            if (state.scrollbar != null) {
                state.scrollbar.release();
            }
        }
    }

    private void drawSlotGridScrollbars(int mouseX, int mouseY) {
        for (SlotGridState state : this.slotGridStates.values()) {
            if (!state.scrollbarEnabled || state.scrollbar == null || state.maxScroll <= 0) {
                continue;
            }
            state.scrollbar.draw(this, this.mc);
        }
    }

    private boolean isMouseOverSlotGrid(int mouseX, int mouseY, SlotGridState state) {
        int left = this.guiLeft + scaledX(state.baseX) - this.backgroundOffsetX;
        int top = this.guiTop + scaledY(state.baseY) - this.backgroundOffsetY;
        int width = scaledWidth(state.visibleColumns * state.slotSize + Math.max(0, state.visibleColumns - 1) * state.spacingX);
        int height = scaledHeight(state.visibleRows * state.slotSize + Math.max(0, state.visibleRows - 1) * state.spacingY);
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private String buildSlotGridKey(CustomHatchRegistry.ComponentDef component) {
        return component.gridBaseIndex + "|" + component.gridBaseX + "|" + component.gridBaseY + "|" + component.rows + "|" + component.columns + "|" + component.role;
    }

    private void updateBackgroundMetrics() {
        this.backgroundOffsetX = this.styleFile.background != null && this.styleFile.background.offsetX != null
            ? this.styleFile.background.offsetX.intValue()
            : 0;
        this.backgroundOffsetY = this.styleFile.background != null && this.styleFile.background.offsetY != null
            ? this.styleFile.background.offsetY.intValue()
            : 0;
        this.backgroundTextureWidth = this.styleFile.background != null && this.styleFile.background.textureWidth != null
            ? Math.max(1, this.styleFile.background.textureWidth.intValue())
            : 176;
        this.backgroundTextureHeight = this.styleFile.background != null && this.styleFile.background.textureHeight != null
            ? Math.max(1, this.styleFile.background.textureHeight.intValue())
            : 166;
    }

    private void updateCoordinateMetrics() {
        if (this.definition.gui != null && this.definition.gui.coordinateWidth > 0) {
            this.coordinateWidth = this.definition.gui.coordinateWidth;
        } else {
            this.coordinateWidth = this.backgroundTextureWidth;
        }
        if (this.definition.gui != null && this.definition.gui.coordinateHeight > 0) {
            this.coordinateHeight = this.definition.gui.coordinateHeight;
        } else {
            this.coordinateHeight = this.backgroundTextureHeight;
        }
    }

    private int componentGuiX(CustomHatchRegistry.ComponentDef component) {
        return scaledX(component.x) - this.backgroundOffsetX;
    }

    private int componentGuiY(CustomHatchRegistry.ComponentDef component) {
        return scaledY(component.y) - this.backgroundOffsetY;
    }

    private int scaledX(int textureX) {
        return Math.round(textureX * (this.xSize / (float) Math.max(1, this.coordinateWidth)));
    }

    private int scaledY(int textureY) {
        return Math.round(textureY * (this.ySize / (float) Math.max(1, this.coordinateHeight)));
    }

    private int scaledWidth(int textureWidth) {
        return Math.max(1, Math.round(textureWidth * (this.xSize / (float) Math.max(1, this.coordinateWidth))));
    }

    private int scaledHeight(int textureHeight) {
        return Math.max(1, Math.round(textureHeight * (this.ySize / (float) Math.max(1, this.coordinateHeight))));
    }

    @Nullable
    private static ResourceLocation parseCustomHatchTexture(String value) {
        return GuiRenderUtils.parseOptionalTexture(value);
    }

    private enum ScrollMode {
        ROW,
        PAGE
    }

    private static class SlotGridState {
        private String key;
        private int baseIndex;
        private int baseX;
        private int baseY;
        private int rows;
        private int columns;
        private int visibleRows;
        private int visibleColumns;
        private int spacingX;
        private int spacingY;
        private int slotSize;
        private int maxScroll;
        private int scrollOffset;
        private boolean scrollbarEnabled;
        private ScrollMode scrollMode;
        private CustomScrollbarState scrollbar;
    }

    private static class CustomScrollbarState {
        private static final ResourceLocation DEFAULT_SCROLLBAR_TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/container/creative_inventory/tabs.png");

        private int left;
        private int top;
        private int width = 12;
        private int height = 16;
        private int thumbHeight = 15;
        private int pageSize = 1;
        private int minScroll = 0;
        private int maxScroll = 0;
        private int currentScroll = 0;
        @Nullable
        private ResourceLocation texture;
        private int textureWidth = 256;
        private int textureHeight = 256;
        private int u = 232;
        private int v = 0;
        private int hoverU = 232;
        private int hoverV = 0;
        private int pressedU = 232;
        private int pressedV = 0;
        private int disabledU = 244;
        private int disabledV = 0;
        private boolean pressed;

        private void setRange(int min, int max, int pageSize) {
            this.minScroll = min;
            this.maxScroll = Math.max(min, max);
            this.pageSize = Math.max(1, pageSize);
            this.applyRange();
        }

        private void applyRange() {
            this.currentScroll = Math.max(Math.min(this.currentScroll, this.maxScroll), this.minScroll);
        }

        private int getRange() {
            return this.maxScroll - this.minScroll;
        }

        private boolean isMouseOver(int x, int y) {
            return x > this.left && x <= this.left + this.width && y > this.top && y <= this.top + this.height;
        }

        private void click(int x, int y) {
            if (this.getRange() == 0) {
                return;
            }
            if (isMouseOver(x, y)) {
                this.pressed = true;
                this.currentScroll = (y - this.top);
                this.currentScroll = this.minScroll + ((this.currentScroll * 2 * this.getRange() / this.height));
                this.currentScroll = (this.currentScroll + 1) >> 1;
                this.applyRange();
            }
        }

        private void release() {
            this.pressed = false;
        }

        private void wheel(int delta) {
            delta = Math.max(Math.min(-delta, 1), -1);
            this.currentScroll += delta * this.pageSize;
            this.applyRange();
        }

        private void draw(Gui gui, Minecraft mc) {
            ResourceLocation tex = this.texture == null ? DEFAULT_SCROLLBAR_TEXTURE : this.texture;
            mc.getTextureManager().bindTexture(tex);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

            if (this.getRange() == 0) {
                Gui.drawModalRectWithCustomSizedTexture(this.left, this.top, this.disabledU, this.disabledV, this.width, this.thumbHeight, this.textureWidth, this.textureHeight);
                return;
            }

            int mouseX = org.lwjgl.input.Mouse.getX() * mc.currentScreen.width / mc.displayWidth;
            int mouseY = mc.currentScreen.height - org.lwjgl.input.Mouse.getY() * mc.currentScreen.height / mc.displayHeight - 1;
            int available = Math.max(1, this.height - this.thumbHeight);
            int offset = (this.currentScroll - this.minScroll) * available / this.getRange();
            int drawU = this.u;
            int drawV = this.v;
            if (this.pressed) {
                drawU = this.pressedU;
                drawV = this.pressedV;
            } else if (isMouseOver(mouseX, mouseY)) {
                drawU = this.hoverU;
                drawV = this.hoverV;
            }
            Gui.drawModalRectWithCustomSizedTexture(this.left, this.top + offset, drawU, drawV, this.width, this.thumbHeight, this.textureWidth, this.textureHeight);
        }
    }
}
