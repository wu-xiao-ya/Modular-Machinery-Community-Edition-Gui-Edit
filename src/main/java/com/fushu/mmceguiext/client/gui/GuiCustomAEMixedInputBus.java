package com.fushu.mmceguiext.client.gui;

import appeng.client.gui.AEBaseGui;
import appeng.container.slot.SlotDisabled;
import appeng.container.slot.SlotFake;
import com.fushu.mmceguiext.common.container.ContainerCustomAEMixedInputBus;
import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.network.PktCustomAEMixedSlotUpdate;
import com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedInputBus;
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
import java.io.IOException;
import java.util.List;

public class GuiCustomAEMixedInputBus extends AEBaseGui {
    private static final ResourceLocation DEFAULT_TEXTURE =
        new ResourceLocation("modularmachinery", "textures/gui/meiteminputbus.png");

    private final TileCustomAEMixedInputBus owner;
    private final CustomAEMixedInputBusRegistry.Def definition;
    @Nullable
    private final ResourceLocation backgroundTexture;

    public GuiCustomAEMixedInputBus(TileCustomAEMixedInputBus owner, EntityPlayer opening, CustomAEMixedInputBusRegistry.Def definition) {
        super(new ContainerCustomAEMixedInputBus(owner, opening));
        this.owner = owner;
        this.definition = definition;
        this.xSize = definition.gui != null && definition.gui.width > 0
            ? definition.gui.width
            : definition.guiWidth > 0 ? definition.guiWidth : 176;
        this.ySize = definition.gui != null && definition.gui.height > 0
            ? definition.gui.height
            : definition.guiHeight > 0 ? definition.guiHeight : 235;
        this.backgroundTexture = resolveTexture(definition);
    }

    @Override
    public void initGui() {
        super.initGui();
        applySlotLayout();
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        String title = this.definition.displayName == null || this.definition.displayName.trim().isEmpty()
            ? I18n.format("gui.meiteminputbus.title")
            : this.definition.displayName;
        this.fontRenderer.drawString(title, 28, 6, 0x404040);
        GlobalTextureLayerConfig.drawLayers(this.definition.textureLayers, true, this.guiLeft, this.guiTop, 0, 0);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(this.backgroundTexture == null ? DEFAULT_TEXTURE : this.backgroundTexture);
        int texW = this.definition.backgroundTextureWidth > 0 ? this.definition.backgroundTextureWidth : this.xSize;
        int texH = this.definition.backgroundTextureHeight > 0 ? this.definition.backgroundTextureHeight : this.ySize;
        drawModalRectWithCustomSizedTexture(offsetX, offsetY, 0, 0, this.xSize, this.ySize, texW, texH);
        GlobalTextureLayerConfig.drawLayers(this.definition.textureLayers, false, offsetX, offsetY, 0, 0);
        drawFluidConfig(offsetX, offsetY);
        drawFluidTank(offsetX, offsetY);
        drawGasConfig(offsetX, offsetY);
        drawGasTank(offsetX, offsetY);
    }

    private void applySlotLayout() {
        List<Slot> slots = this.inventorySlots.inventorySlots;
        for (Slot slot : slots) {
            if (slot instanceof SlotFake) {
                int idx = slot.getSlotIndex();
                if (idx >= 0 && idx < 15) {
                    CustomAEMixedInputBusRegistry.ComponentDef component = findIndexedComponent("slot", "item_config", idx);
                    if (component != null) {
                        slot.xPos = component.x;
                        slot.yPos = component.y;
                    } else if (idx < this.definition.configSlots.size()) {
                        CustomAEMixedInputBusRegistry.SlotPoint point = this.definition.configSlots.get(idx);
                        if (point != null) {
                            slot.xPos = point.x;
                            slot.yPos = point.y;
                        }
                    }
                }
                continue;
            }
            if (slot instanceof SlotDisabled) {
                int idx = slot.getSlotIndex();
                if (idx >= 0 && idx < 15) {
                    CustomAEMixedInputBusRegistry.ComponentDef component = findIndexedComponent("slot", "item_storage", idx);
                    if (component == null) {
                        component = findIndexedComponent("slot", "item_output", idx);
                    }
                    if (component != null) {
                        slot.xPos = component.x;
                        slot.yPos = component.y;
                    } else if (idx < this.definition.storageSlots.size()) {
                        CustomAEMixedInputBusRegistry.SlotPoint point = this.definition.storageSlots.get(idx);
                        if (point != null) {
                            slot.xPos = point.x;
                            slot.yPos = point.y;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        CustomAEMixedInputBusRegistry.ComponentDef fluidConfig = findFirstComponent("slot", "fluid_config");
        CustomAEMixedInputBusRegistry.ComponentDef gasConfig = findFirstComponent("slot", "gas_config");
        if (mouseButton == 0) {
            CustomAEMixedInputBusRegistry.TankRect fluidRect = fluidConfig != null ? toTankRect(fluidConfig) : this.definition.fluidConfigTank;
            if (fluidRect != null && isMouseOverTank(mouseX, mouseY, fluidRect)) {
                MMCEGuiExt.NET_CHANNEL.sendToServer(new PktCustomAEMixedSlotUpdate(this.owner.getPos(), PktCustomAEMixedSlotUpdate.TARGET_FLUID));
                return;
            }
            CustomAEMixedInputBusRegistry.TankRect gasRect = gasConfig != null ? toTankRect(gasConfig) : this.definition.gasConfigTank;
            if (gasRect != null && isMouseOverTank(mouseX, mouseY, gasRect)) {
                MMCEGuiExt.NET_CHANNEL.sendToServer(new PktCustomAEMixedSlotUpdate(this.owner.getPos(), PktCustomAEMixedSlotUpdate.TARGET_GAS));
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean isMouseOverTank(int mouseX, int mouseY, CustomAEMixedInputBusRegistry.TankRect rect) {
        int left = this.guiLeft + rect.x;
        int top = this.guiTop + rect.y;
        return mouseX >= left && mouseX < left + rect.width && mouseY >= top && mouseY < top + rect.height;
    }

    private void drawFluidConfig(int guiLeft, int guiTop) {
        if (this.definition.fluidConfigTank == null) {
            return;
        }
        appeng.api.storage.data.IAEFluidStack fluid = this.owner.getFluidConfig().getFluidInSlot(0);
        if (fluid == null || fluid.getFluidStack() == null || fluid.getStackSize() <= 0) {
            return;
        }
        ResourceLocation still = fluid.getFluidStack().getFluid().getStill(fluid.getFluidStack());
        TextureAtlasSprite sprite = this.mc.getTextureMapBlocks().getTextureExtry(still.toString());
        if (sprite == null) {
            sprite = this.mc.getTextureMapBlocks().getMissingSprite();
        }
        int color = fluid.getFluidStack().getFluid().getColor(fluid.getFluidStack());
        float red = (color >> 16 & 0xFF) / 255F;
        float green = (color >> 8 & 0xFF) / 255F;
        float blue = (color & 0xFF) / 255F;
        GlStateManager.color(red, green, blue, 1.0F);
        this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        drawTiledSprite(guiLeft + this.definition.fluidConfigTank.x, guiTop + this.definition.fluidConfigTank.y, this.definition.fluidConfigTank.width, this.definition.fluidConfigTank.height, sprite);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(this.backgroundTexture == null ? DEFAULT_TEXTURE : this.backgroundTexture);
    }

    private void drawFluidTank(int guiLeft, int guiTop) {
        if (this.definition.fluidStorageTank == null) {
            return;
        }
        appeng.api.storage.data.IAEFluidStack fluid = this.owner.getFluidTanks().getFluidInSlot(0);
        if (fluid == null || fluid.getFluidStack() == null || fluid.getStackSize() <= 0) {
            return;
        }
        int capacity = Math.max(1, ((AEFluidInventoryUpgradeable) this.owner.getFluidTanks()).getCapacity());
        int filled = MathHelper.ceil((float) fluid.getStackSize() / (float) capacity * this.definition.fluidStorageTank.height);
        ResourceLocation still = fluid.getFluidStack().getFluid().getStill(fluid.getFluidStack());
        TextureAtlasSprite sprite = this.mc.getTextureMapBlocks().getTextureExtry(still.toString());
        if (sprite == null) {
            sprite = this.mc.getTextureMapBlocks().getMissingSprite();
        }
        int color = fluid.getFluidStack().getFluid().getColor(fluid.getFluidStack());
        float red = (color >> 16 & 0xFF) / 255F;
        float green = (color >> 8 & 0xFF) / 255F;
        float blue = (color & 0xFF) / 255F;
        GlStateManager.color(red, green, blue, 1.0F);
        this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        drawTiledSprite(
            guiLeft + this.definition.fluidStorageTank.x,
            guiTop + this.definition.fluidStorageTank.y + this.definition.fluidStorageTank.height - filled,
            this.definition.fluidStorageTank.width,
            filled,
            sprite
        );
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(this.backgroundTexture == null ? DEFAULT_TEXTURE : this.backgroundTexture);
    }

    private void drawGasConfig(int guiLeft, int guiTop) {
        if (this.definition.gasConfigTank == null) {
            return;
        }
        GasStack gas = this.owner.getGasConfig().getGasStack(0);
        if (gas == null || gas.amount <= 0) {
            return;
        }
        TextureAtlasSprite sprite = gas.getGas().getSprite();
        if (sprite == null) {
            sprite = this.mc.getTextureMapBlocks().getMissingSprite();
        }
        int color = gas.getGas().getTint();
        float red = (color >> 16 & 0xFF) / 255F;
        float green = (color >> 8 & 0xFF) / 255F;
        float blue = (color & 0xFF) / 255F;
        GlStateManager.color(red, green, blue, 1.0F);
        this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        drawTiledSprite(guiLeft + this.definition.gasConfigTank.x, guiTop + this.definition.gasConfigTank.y, this.definition.gasConfigTank.width, this.definition.gasConfigTank.height, sprite);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(this.backgroundTexture == null ? DEFAULT_TEXTURE : this.backgroundTexture);
    }

    private void drawGasTank(int guiLeft, int guiTop) {
        if (this.definition.gasStorageTank == null) {
            return;
        }
        GasStack gas = this.owner.getGasTanks().getGasStack(0);
        if (gas == null || gas.amount <= 0) {
            return;
        }
        int capacity = Math.max(1, this.owner.getGasTanks().getTanks()[0].getMaxGas());
        int filled = MathHelper.ceil((float) gas.amount / (float) capacity * this.definition.gasStorageTank.height);
        TextureAtlasSprite sprite = gas.getGas().getSprite();
        if (sprite == null) {
            sprite = this.mc.getTextureMapBlocks().getMissingSprite();
        }
        int color = gas.getGas().getTint();
        float red = (color >> 16 & 0xFF) / 255F;
        float green = (color >> 8 & 0xFF) / 255F;
        float blue = (color & 0xFF) / 255F;
        GlStateManager.color(red, green, blue, 1.0F);
        this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        drawTiledSprite(
            guiLeft + this.definition.gasStorageTank.x,
            guiTop + this.definition.gasStorageTank.y + this.definition.gasStorageTank.height - filled,
            this.definition.gasStorageTank.width,
            filled,
            sprite
        );
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(this.backgroundTexture == null ? DEFAULT_TEXTURE : this.backgroundTexture);
    }

    private void drawTiledSprite(int x, int y, int width, int height, TextureAtlasSprite sprite) {
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
    private CustomAEMixedInputBusRegistry.ComponentDef findIndexedComponent(String type, String role, int index) {
        if (this.definition.gui == null || this.definition.gui.components == null) {
            return null;
        }
        for (CustomAEMixedInputBusRegistry.ComponentDef component : this.definition.gui.components) {
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

    @Nullable
    private CustomAEMixedInputBusRegistry.ComponentDef findFirstComponent(String type, String role) {
        if (this.definition.gui == null || this.definition.gui.components == null) {
            return null;
        }
        for (CustomAEMixedInputBusRegistry.ComponentDef component : this.definition.gui.components) {
            if (component == null) {
                continue;
            }
            if (!type.equalsIgnoreCase(component.type == null ? "" : component.type)) {
                continue;
            }
            if (!role.equalsIgnoreCase(component.role == null ? "" : component.role)) {
                continue;
            }
            return component;
        }
        return null;
    }

    private CustomAEMixedInputBusRegistry.TankRect toTankRect(CustomAEMixedInputBusRegistry.ComponentDef component) {
        CustomAEMixedInputBusRegistry.TankRect rect = new CustomAEMixedInputBusRegistry.TankRect();
        rect.x = component.x;
        rect.y = component.y;
        rect.width = component.width;
        rect.height = component.height;
        return rect;
    }

    @Nullable
    private static ResourceLocation resolveTexture(CustomAEMixedInputBusRegistry.Def definition) {
        if (definition != null && definition.guiBackgroundTexture != null && !definition.guiBackgroundTexture.trim().isEmpty()) {
            return GuiRenderUtils.parseOptionalTexture(definition.guiBackgroundTexture);
        }
        return null;
    }
}
