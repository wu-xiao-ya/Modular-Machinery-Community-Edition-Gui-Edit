package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.fushu.mmceguiext.client.config.GlobalGuiStyleManager;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.gui.GuiContainerBase;
import hellfirepvp.modularmachinery.common.block.prop.ItemBusSize;
import hellfirepvp.modularmachinery.common.container.ContainerItemBus;
import hellfirepvp.modularmachinery.common.tiles.base.TileItemBus;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;
import java.awt.Point;
import java.util.List;

public class GuiItemBusCustom extends GuiContainerBase<ContainerItemBus> {
    private static final int BASE_WIDTH = 176;
    private static final int BASE_HEIGHT = 166;
    private static final int PLAYER_INVENTORY_MASK_COLOR = 0xFF555555;
    private static final int HIDDEN_SLOT_POS = -1000;

    private final TileItemBus itemBus;
    @Nullable
    private ResourceLocation customBackgroundTexture;
    private List<Point> slotLayout;
    private List<GlobalTextureLayerConfig.LayerDef> textureLayers;
    private GlobalGuiStyleManager.StyleFile styleFile = GlobalGuiStyleManager.StyleFile.EMPTY;

    public GuiItemBusCustom(TileItemBus itemBus, net.minecraft.entity.player.EntityPlayer opening) {
        super(new ContainerItemBus(itemBus, opening));
        this.itemBus = itemBus;
        this.slotLayout = ItemBusGuiLayout.getDefaultSlots(itemBus.getSize());
    }

    @Override
    public void initGui() {
        this.styleFile = GlobalGuiStyleManager.load(MMCEGuiExtConfig.itemBus.styleFile);
        this.customBackgroundTexture = resolveCustomTexture();
        this.slotLayout = ItemBusGuiLayout.resolveConfiguredLayout(MMCEGuiExtConfig.itemBus.slotLayouts, getBusSize());
        this.textureLayers = !this.styleFile.layers.isEmpty()
            ? this.styleFile.layers
            : GlobalTextureLayerConfig.parse(MMCEGuiExtConfig.itemBus.textureLayers);
        super.initGui();
        applySlotLayout();
    }

    @Override
    protected void setWidthHeight() {
        MMCEGuiExtConfig.ItemBus cfg = MMCEGuiExtConfig.itemBus;
        this.xSize = MathHelper.clamp(cfg.guiWidth, BASE_WIDTH, 1024);
        this.ySize = MathHelper.clamp(cfg.guiHeight, BASE_HEIGHT, 1024);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        MMCEGuiExtConfig.ItemBus cfg = MMCEGuiExtConfig.itemBus;
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;

        if (this.customBackgroundTexture != null) {
            int offsetX = this.styleFile.background != null && this.styleFile.background.offsetX != null
                ? this.styleFile.background.offsetX.intValue()
                : cfg.backgroundTextureOffsetX;
            int offsetY = this.styleFile.background != null && this.styleFile.background.offsetY != null
                ? this.styleFile.background.offsetY.intValue()
                : cfg.backgroundTextureOffsetY;
            int corner = this.styleFile.background != null && this.styleFile.background.corner != null
                ? this.styleFile.background.corner.intValue()
                : cfg.backgroundCorner;
            boolean useNineSlice = this.styleFile.background != null && this.styleFile.background.useNineSlice != null
                ? this.styleFile.background.useNineSlice.booleanValue()
                : cfg.useNineSlice;
            this.mc.getTextureManager().bindTexture(this.customBackgroundTexture);
            drawResizableArea(
                guiLeft - offsetX,
                guiTop - offsetY,
                this.xSize + offsetX,
                this.ySize + offsetY,
                useNineSlice,
                getTextureWidth(),
                getTextureHeight(),
                corner
            );
        } else if (!cfg.hideDefaultBackground) {
            this.mc.getTextureManager().bindTexture(getDefaultTexture());
            Gui.drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, this.xSize, this.ySize, BASE_WIDTH, BASE_HEIGHT);
        }

        if (cfg.hidePlayerInventory) {
            maskPlayerInventoryArea(guiLeft, guiTop);
        }

        GlobalTextureLayerConfig.drawLayers(
            this.textureLayers,
            false,
            guiLeft,
            guiTop,
            this.styleFile.background != null && this.styleFile.background.offsetX != null ? this.styleFile.background.offsetX.intValue() : cfg.backgroundTextureOffsetX,
            this.styleFile.background != null && this.styleFile.background.offsetY != null ? this.styleFile.background.offsetY.intValue() : cfg.backgroundTextureOffsetY
        );
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        java.util.SortedSet<Integer> priorities = GlobalTextureLayerConfig.collectPriorities(this.textureLayers, true, 0);
        int offsetX = this.styleFile.background != null && this.styleFile.background.offsetX != null ? this.styleFile.background.offsetX.intValue() : MMCEGuiExtConfig.itemBus.backgroundTextureOffsetX;
        int offsetY = this.styleFile.background != null && this.styleFile.background.offsetY != null ? this.styleFile.background.offsetY.intValue() : MMCEGuiExtConfig.itemBus.backgroundTextureOffsetY;
        for (Integer priority : priorities) {
            GlobalTextureLayerConfig.drawLayers(
                this.textureLayers,
                true,
                this.guiLeft,
                this.guiTop,
                offsetX,
                offsetY,
                priority
            );
        }
    }

    private void applySlotLayout() {
        if (this.inventorySlots == null || this.inventorySlots.inventorySlots == null) {
            return;
        }

        List<Slot> slots = this.inventorySlots.inventorySlots;
        int total = slots.size();
        int busSlotCount = getBusSize().getSlotCount();
        int playerSlotCount = 36;
        if (total < playerSlotCount + busSlotCount) {
            return;
        }

        for (int index = 0; index < playerSlotCount; index++) {
            Slot slot = slots.get(index);
            if (slot == null) {
                continue;
            }
            if (MMCEGuiExtConfig.itemBus.hidePlayerInventory) {
                slot.xPos = HIDDEN_SLOT_POS;
                slot.yPos = HIDDEN_SLOT_POS;
            } else if (index < 27) {
                slot.xPos = MMCEGuiExtConfig.itemBus.playerInventoryX + (index % 9) * 18;
                slot.yPos = MMCEGuiExtConfig.itemBus.playerInventoryY + (index / 9) * 18;
            } else {
                slot.xPos = MMCEGuiExtConfig.itemBus.playerInventoryX + (index - 27) * 18;
                slot.yPos = MMCEGuiExtConfig.itemBus.playerHotbarY;
            }
        }

        for (int index = 0; index < busSlotCount; index++) {
            Slot slot = slots.get(playerSlotCount + index);
            if (slot == null) {
                continue;
            }
            Point point = index < this.slotLayout.size() ? this.slotLayout.get(index) : null;
            if (point == null) {
                slot.xPos = HIDDEN_SLOT_POS;
                slot.yPos = HIDDEN_SLOT_POS;
            } else {
                slot.xPos = point.x;
                slot.yPos = point.y;
            }
        }
    }

    private void drawResizableArea(int x, int y, int width, int height, boolean useNineSlice, int texW, int texH, int corner) {
        if (!useNineSlice) {
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, texW, texH);
            return;
        }
        GuiRenderUtils.drawNineSlice(x, y, width, height, texW, texH, corner);
    }

    private void maskPlayerInventoryArea(int guiLeft, int guiTop) {
        int width = 162;
        int height = 76;
        Gui.drawRect(
            guiLeft + MMCEGuiExtConfig.itemBus.playerInventoryX,
            guiTop + MMCEGuiExtConfig.itemBus.playerInventoryY,
            guiLeft + MMCEGuiExtConfig.itemBus.playerInventoryX + width,
            guiTop + MMCEGuiExtConfig.itemBus.playerInventoryY + height,
            PLAYER_INVENTORY_MASK_COLOR
        );
    }

    private ResourceLocation getDefaultTexture() {
        return new ResourceLocation(
            ModularMachinery.MODID,
            "textures/gui/inventory_" + getBusSize().name().toLowerCase() + ".png"
        );
    }

    @Nullable
    private ResourceLocation resolveCustomTexture() {
        if (this.styleFile.background != null && this.styleFile.background.texture != null && !this.styleFile.background.texture.trim().isEmpty()) {
            ResourceLocation texture = GuiRenderUtils.parseOptionalTexture(this.styleFile.background.texture);
            if (texture != null) {
                return texture;
            }
        }
        String raw = ItemBusGuiLayout.resolveConfiguredValue(
            MMCEGuiExtConfig.itemBus.backgroundTextures,
            getBusSize().name(),
            ""
        );
        return GuiRenderUtils.parseOptionalTexture(raw);
    }

    private ItemBusSize getBusSize() {
        return this.itemBus.getSize();
    }

    private int getTextureWidth() {
        if (this.styleFile.background != null && this.styleFile.background.textureWidth != null) {
            return this.styleFile.background.textureWidth.intValue();
        }
        return MMCEGuiExtConfig.itemBus.backgroundTextureWidth > 0
            ? MMCEGuiExtConfig.itemBus.backgroundTextureWidth
            : ItemBusGuiLayout.estimateTextureWidth(this.slotLayout, this.xSize);
    }

    private int getTextureHeight() {
        if (this.styleFile.background != null && this.styleFile.background.textureHeight != null) {
            return this.styleFile.background.textureHeight.intValue();
        }
        return MMCEGuiExtConfig.itemBus.backgroundTextureHeight > 0
            ? MMCEGuiExtConfig.itemBus.backgroundTextureHeight
            : ItemBusGuiLayout.estimateTextureHeight(this.slotLayout, this.ySize);
    }
}
