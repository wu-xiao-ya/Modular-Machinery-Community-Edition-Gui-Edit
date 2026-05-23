package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.fushu.mmceguiext.common.registry.CustomAEItemInputBusRegistry;
import com.fushu.mmceguiext.common.tile.TileCustomMEItemInputBus;
import appeng.container.slot.SlotDisabled;
import appeng.container.slot.SlotFake;
import github.kasuminova.mmce.client.gui.GuiMEItemInputBus;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

public class GuiMEItemInputBusCustom extends GuiMEItemInputBus {
    private static final ResourceLocation DEFAULT_TEXTURE =
        new ResourceLocation(ModularMachinery.MODID, "textures/gui/meiteminputbus.png");

    @Nullable
    private final ResourceLocation customBackgroundTexture;
    @Nullable
    private final CustomAEItemInputBusRegistry.Def definition;

    public GuiMEItemInputBusCustom(final MEItemInputBus te, final EntityPlayer player) {
        super(te, player);
        this.definition = te instanceof TileCustomMEItemInputBus ? ((TileCustomMEItemInputBus) te).getDefinition() : null;
        this.customBackgroundTexture = resolveTexture();
    }

    @Override
    public void initGui() {
        super.initGui();
        applySlotLayout();
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(this.customBackgroundTexture == null ? DEFAULT_TEXTURE : this.customBackgroundTexture);
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        String title = this.definition != null && this.definition.displayName != null && !this.definition.displayName.trim().isEmpty()
            ? this.definition.displayName
            : I18n.format("gui.meiteminputbus.title");
        this.fontRenderer.drawString(title, 8, 8, 0x404040);
        this.fontRenderer.drawString(appeng.core.localization.GuiText.Config.getLocal(), 8, 6 + 11 + 7, 0x404040);
        this.fontRenderer.drawString(appeng.core.localization.GuiText.StoredItems.getLocal(), 97, 6 + 11 + 7, 0x404040);
        this.fontRenderer.drawString(appeng.core.localization.GuiText.inventory.getLocal(), 8, this.ySize - 93, 0x404040);
    }

    @Nullable
    private ResourceLocation resolveTexture() {
        if (this.definition != null && this.definition.guiBackgroundTexture != null && !this.definition.guiBackgroundTexture.trim().isEmpty()) {
            ResourceLocation texture = GuiRenderUtils.parseOptionalTexture(this.definition.guiBackgroundTexture);
            if (texture != null) {
                return texture;
            }
        }
        String raw = MMCEGuiExtConfig.aeBus.itemInputBackgroundTexture;
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return GuiRenderUtils.parseOptionalTexture(raw);
    }

    private void applySlotLayout() {
        if (this.definition == null || this.inventorySlots == null || this.inventorySlots.inventorySlots == null) {
            return;
        }
        List<Slot> slots = this.inventorySlots.inventorySlots;
        for (Slot slot : slots) {
            if (slot instanceof SlotFake) {
                int idx = slot.getSlotIndex();
                if (idx >= 0 && idx < this.definition.configSlots.size()) {
                    CustomAEItemInputBusRegistry.SlotPoint point = this.definition.configSlots.get(idx);
                    slot.xPos = point.x;
                    slot.yPos = point.y;
                }
            } else if (slot instanceof SlotDisabled) {
                int idx = slot.getSlotIndex();
                if (idx >= 0 && idx < this.definition.storageSlots.size()) {
                    CustomAEItemInputBusRegistry.SlotPoint point = this.definition.storageSlots.get(idx);
                    slot.xPos = point.x;
                    slot.yPos = point.y;
                }
            } else if (slot.slotNumber < 27) {
                slot.xPos = this.definition.playerInventoryX + (slot.slotNumber % 9) * 18;
                slot.yPos = this.definition.playerInventoryY + (slot.slotNumber / 9) * 18;
            } else if (slot.slotNumber < 36) {
                slot.xPos = this.definition.playerInventoryX + (slot.slotNumber - 27) * 18;
                slot.yPos = this.definition.playerHotbarY;
            }
        }
    }
}
