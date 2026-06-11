package com.fushu.mmceguiext.client.gui;

import appeng.client.gui.AEBaseGui;
import appeng.container.slot.SlotDisabled;
import com.fushu.mmceguiext.common.container.ContainerCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusRegistry;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedOutputBus;
import github.kasuminova.mmce.common.util.AEFluidInventoryUpgradeable;
import mekanism.api.gas.GasStack;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GuiCustomAEMixedOutputBus extends AEBaseGui {
    private static final ResourceLocation DEFAULT_TEXTURE =
        new ResourceLocation("modularmachinery", "textures/gui/mefluidoutputbus.png");

    private final TileCustomAEMixedOutputBus owner;
    private final CustomAEMixedOutputBusRegistry.Def definition;
    @Nullable
    private final ResourceLocation backgroundTexture;

    public GuiCustomAEMixedOutputBus(TileCustomAEMixedOutputBus owner, EntityPlayer player, CustomAEMixedOutputBusRegistry.Def definition) {
        super(new ContainerCustomAEMixedOutputBus(owner, player));
        this.owner = owner;
        this.definition = definition;
        this.xSize = definition.gui != null && definition.gui.width > 0 ? definition.gui.width : definition.guiWidth;
        this.ySize = definition.gui != null && definition.gui.height > 0 ? definition.gui.height : definition.guiHeight;
        this.backgroundTexture = resolveTexture(definition);
    }

    @Override
    public void initGui() {
        super.initGui();
        applySlotLayout();
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        java.util.SortedSet<Integer> priorities = GlobalTextureLayerConfig.collectPriorities(this.definition.textureLayers, true, 0);
        for (Integer priority : priorities) {
            if (priority.intValue() < 0) {
                continue;
            }
            if (priority.intValue() == 0) {
                String title = this.definition.displayName == null || this.definition.displayName.trim().isEmpty()
                    ? I18n.format("gui.meitemoutputbus.title")
                    : this.definition.displayName;
                this.fontRenderer.drawString(title, 28, 6, 0x404040);
            }
            GlobalTextureLayerConfig.drawLayers(this.definition.textureLayers, true, this.guiLeft, this.guiTop, 0, 0, priority);
        }
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(this.backgroundTexture == null ? DEFAULT_TEXTURE : this.backgroundTexture);
        int texW = this.definition.backgroundTextureWidth > 0 ? this.definition.backgroundTextureWidth : this.xSize;
        int texH = this.definition.backgroundTextureHeight > 0 ? this.definition.backgroundTextureHeight : this.ySize;
        drawModalRectWithCustomSizedTexture(offsetX, offsetY, 0, 0, this.xSize, this.ySize, texW, texH);
        GlobalTextureLayerConfig.drawLayers(this.definition.textureLayers, false, offsetX, offsetY, 0, 0);
        drawNegativeForegroundLayers(offsetX, offsetY);
        drawFluidTanks(offsetX, offsetY);
        drawGasTanks(offsetX, offsetY);
    }

    private void drawNegativeForegroundLayers(int guiLeft, int guiTop) {
        for (Integer priority : GlobalTextureLayerConfig.collectPriorities(this.definition.textureLayers, true, 0)) {
            if (priority.intValue() >= 0) {
                continue;
            }
            GlobalTextureLayerConfig.drawLayers(this.definition.textureLayers, true, guiLeft, guiTop, 0, 0, priority);
        }
    }

    private void drawFluidTanks(int guiLeft, int guiTop) {
        List<CustomAEMixedOutputBusRegistry.ComponentDef> fluidComponents = findComponents("tank", "fluid_storage");
        int fallbackIndex = 0;
        for (CustomAEMixedOutputBusRegistry.ComponentDef fluidStorage : fluidComponents) {
            int slot = fluidStorage.index >= 0 ? fluidStorage.index : fallbackIndex++;
            if (slot < 0 || slot >= this.owner.getActiveFluidSlots()) {
                continue;
            }
            appeng.api.storage.data.IAEFluidStack fluid = this.owner.getFluidTanks().getFluidInSlot(slot);
            if (fluid == null || fluid.getFluidStack() == null || fluid.getStackSize() <= 0) {
                continue;
            }
            int capacity = Math.max(1, this.owner.getFluidTanks().getCapacity());
            int filled = MathHelper.ceil((float) fluid.getStackSize() / (float) capacity * fluidStorage.height);
            ResourceLocation still = fluid.getFluidStack().getFluid().getStill(fluid.getFluidStack());
            TextureAtlasSprite sprite = this.mc.getTextureMapBlocks().getTextureExtry(still.toString());
            if (sprite == null) {
                sprite = this.mc.getTextureMapBlocks().getMissingSprite();
            }
            int color = fluid.getFluidStack().getFluid().getColor(fluid.getFluidStack());
            GlStateManager.color(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F, 1.0F);
            this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            drawTiledSprite(guiLeft + fluidStorage.x, guiTop + fluidStorage.y + fluidStorage.height - filled, fluidStorage.width, filled, sprite);
        }
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private void applySlotLayout() {
        List<Slot> slots = this.inventorySlots.inventorySlots;
        for (Slot slot : slots) {
            if (slot instanceof SlotDisabled) {
                int idx = slot.getSlotIndex();
                CustomAEMixedOutputBusRegistry.ComponentDef component = findIndexedComponent("slot", "item_storage", idx);
                if (component == null) {
                    component = findIndexedComponent("slot", "item_output", idx);
                }
                if (component != null) {
                    slot.xPos = component.x;
                    slot.yPos = component.y;
                }
            }
        }
    }

    private void drawGasTanks(int guiLeft, int guiTop) {
        List<CustomAEMixedOutputBusRegistry.ComponentDef> gasComponents = findComponents("tank", "gas_storage");
        int fallbackIndex = 0;
        for (CustomAEMixedOutputBusRegistry.ComponentDef gasStorage : gasComponents) {
            int slot = gasStorage.index >= 0 ? gasStorage.index : fallbackIndex++;
            if (slot < 0 || slot >= this.owner.getActiveGasSlots()) {
                continue;
            }
            GasStack gas = this.owner.getGasTanks().getGasStack(slot);
            if (gas == null || gas.amount <= 0) {
                continue;
            }
            int capacity = Math.max(1, this.owner.getGasTanks().getTanks()[slot].getMaxGas());
            int filled = MathHelper.ceil((float) gas.amount / (float) capacity * gasStorage.height);
            TextureAtlasSprite sprite = gas.getGas().getSprite();
            if (sprite == null) {
                sprite = this.mc.getTextureMapBlocks().getMissingSprite();
            }
            int color = gas.getGas().getTint();
            GlStateManager.color(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F, 1.0F);
            this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            drawTiledSprite(guiLeft + gasStorage.x, guiTop + gasStorage.y + gasStorage.height - filled, gasStorage.width, filled, sprite);
        }
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private void drawTiledSprite(int x, int y, int width, int height, TextureAtlasSprite sprite) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int remainingHeight = height;
        int drawY = y;
        while (remainingHeight > 0) {
            int tileHeight = Math.min(16, remainingHeight);
            drawTexturedModalRect(x, drawY, sprite, width, tileHeight);
            drawY += tileHeight;
            remainingHeight -= tileHeight;
        }
    }

    @Nullable
    private CustomAEMixedOutputBusRegistry.ComponentDef findIndexedComponent(String type, String role, int index) {
        if (this.definition.gui == null || this.definition.gui.components == null) {
            return null;
        }
        for (CustomAEMixedOutputBusRegistry.ComponentDef component : this.definition.gui.components) {
            if (component == null) {
                continue;
            }
            if (!type.equalsIgnoreCase(component.type == null ? "" : component.type)) {
                continue;
            }
            if (!role.equalsIgnoreCase(component.role == null ? "" : component.role)) {
                continue;
            }
            if (component.index == index) {
                return component;
            }
        }
        return null;
    }

    private List<CustomAEMixedOutputBusRegistry.ComponentDef> findComponents(String type, String role) {
        List<CustomAEMixedOutputBusRegistry.ComponentDef> result = new ArrayList<>();
        if (this.definition.gui == null || this.definition.gui.components == null) {
            return result;
        }
        for (CustomAEMixedOutputBusRegistry.ComponentDef component : this.definition.gui.components) {
            if (component == null) {
                continue;
            }
            if (!type.equalsIgnoreCase(component.type == null ? "" : component.type)) {
                continue;
            }
            if (!role.equalsIgnoreCase(component.role == null ? "" : component.role)) {
                continue;
            }
            result.add(component);
        }
        result.sort(Comparator.comparingInt(component -> component.index < 0 ? Integer.MAX_VALUE : component.index));
        return result;
    }

    @Nullable
    private static ResourceLocation resolveTexture(CustomAEMixedOutputBusRegistry.Def definition) {
        if (definition != null && definition.guiBackgroundTexture != null && !definition.guiBackgroundTexture.trim().isEmpty()) {
            return GuiRenderUtils.parseOptionalTexture(definition.guiBackgroundTexture);
        }
        return null;
    }
}
