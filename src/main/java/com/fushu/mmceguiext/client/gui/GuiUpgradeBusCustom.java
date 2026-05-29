package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.fushu.mmceguiext.client.config.GlobalGuiStyleManager;
import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import github.kasuminova.mmce.common.upgrade.UpgradeType;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.gui.GuiContainerBase;
import hellfirepvp.modularmachinery.client.gui.widget.GuiScrollbar;
import hellfirepvp.modularmachinery.common.container.ContainerUpgradeBus;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.TileUpgradeBus;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;

import javax.annotation.Nullable;
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GuiUpgradeBusCustom extends GuiContainerBase<ContainerUpgradeBus> {
    private static final int BASE_WIDTH = 176;
    private static final int BASE_HEIGHT = 213;
    private static final int PLAYER_INVENTORY_MASK_COLOR = 0xFF555555;
    private static final int HIDDEN_SLOT_POS = -1000;
    private static final ResourceLocation DEFAULT_TEXTURE =
        new ResourceLocation(ModularMachinery.MODID, "textures/gui/guiupgradebus.png");

    private final GuiScrollbar scrollbar = new GuiScrollbar();
    private final TileUpgradeBus upgradeBus;
    @Nullable
    private ResourceLocation customBackgroundTexture;
    private List<Point> slotLayout = new ArrayList<Point>();
    private List<GlobalTextureLayerConfig.LayerDef> textureLayers = new ArrayList<GlobalTextureLayerConfig.LayerDef>();
    private GlobalGuiStyleManager.StyleFile styleFile = GlobalGuiStyleManager.StyleFile.EMPTY;

    public GuiUpgradeBusCustom(TileUpgradeBus upgradeBus, EntityPlayer player) {
        super(new ContainerUpgradeBus(upgradeBus, player));
        this.upgradeBus = upgradeBus;
    }

    @Override
    public void initGui() {
        this.styleFile = GlobalGuiStyleManager.load(MMCEGuiExtConfig.upgradeBus.styleFile);
        this.customBackgroundTexture = resolveCustomTexture();
        this.slotLayout = ItemBusGuiLayout.resolveConfiguredLayout(
            MMCEGuiExtConfig.upgradeBus.slotLayout,
            upgradeBus.getInventory().getSlots(),
            buildDefaultLayout(upgradeBus.getInventory().getSlots())
        );
        this.textureLayers = !this.styleFile.layers.isEmpty()
            ? this.styleFile.layers
            : GlobalTextureLayerConfig.parse(MMCEGuiExtConfig.upgradeBus.textureLayers);
        super.initGui();
        applySlotLayout();
    }

    @Override
    protected void setWidthHeight() {
        this.xSize = MathHelper.clamp(MMCEGuiExtConfig.upgradeBus.guiWidth, BASE_WIDTH, 1024);
        this.ySize = MathHelper.clamp(MMCEGuiExtConfig.upgradeBus.guiHeight, BASE_HEIGHT, 1024);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(final int mouseX, final int mouseY) {
        int layerOffsetX = this.styleFile.background != null && this.styleFile.background.offsetX != null
            ? this.styleFile.background.offsetX.intValue()
            : MMCEGuiExtConfig.upgradeBus.backgroundTextureOffsetX;
        int layerOffsetY = this.styleFile.background != null && this.styleFile.background.offsetY != null
            ? this.styleFile.background.offsetY.intValue()
            : MMCEGuiExtConfig.upgradeBus.backgroundTextureOffsetY;
        java.util.SortedSet<Integer> priorities = GlobalTextureLayerConfig.collectPriorities(this.textureLayers, true, 0);
        for (Integer priority : priorities) {
            if (priority.intValue() == 0) {
                drawUpgradeBusForegroundContent();
            }
            GlobalTextureLayerConfig.drawLayers(
                this.textureLayers,
                true,
                this.guiLeft,
                this.guiTop,
                layerOffsetX,
                layerOffsetY,
                priority
            );
        }
    }

    private void drawUpgradeBusForegroundContent() {
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        TileUpgradeBus.UpgradeBusProvider component = upgradeBus.provideComponent();
        FontRenderer fr = this.fontRenderer;
        fr.drawStringWithShadow(I18n.format("gui.upgradebus.title"), 7, 5, 0xFFFFFF);

        GlStateManager.scale(0.72F, 0.72F, 0.72F);

        List<String> description = new ArrayList<String>();
        Map<BlockPos, DynamicMachine> boundedMachine = component.getBoundedMachine();
        Map<UpgradeType, List<MachineUpgrade>> upgrades = component.getUpgrades(null);

        collectBoundedMachineDescriptions(description, boundedMachine, upgrades);
        collectUpgradeDescriptions(component, description, upgrades);

        List<String> wrappedDesc = description.stream()
            .flatMap(s -> fr.listFormattedStringToWidth(s, (int) (MMCEGuiExtConfig.upgradeBus.textWidth / 0.72F)).stream())
            .collect(Collectors.toList());

        updateScrollbar(Math.max(0, wrappedDesc.size() - MMCEGuiExtConfig.upgradeBus.maxDescLines));

        int offsetY = MMCEGuiExtConfig.upgradeBus.textY;
        for (int i = scrollbar.getCurrentScroll(); i < Math.min(wrappedDesc.size(), MMCEGuiExtConfig.upgradeBus.maxDescLines + scrollbar.getCurrentScroll()); i++) {
            fr.drawStringWithShadow(wrappedDesc.get(i), MMCEGuiExtConfig.upgradeBus.textX, offsetY, 0xFFFFFF);
            offsetY += 10;
        }

        GlStateManager.popMatrix();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(final float partialTicks, final int mouseX, final int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;
        int offsetX = this.styleFile.background != null && this.styleFile.background.offsetX != null
            ? this.styleFile.background.offsetX.intValue()
            : MMCEGuiExtConfig.upgradeBus.backgroundTextureOffsetX;
        int offsetY = this.styleFile.background != null && this.styleFile.background.offsetY != null
            ? this.styleFile.background.offsetY.intValue()
            : MMCEGuiExtConfig.upgradeBus.backgroundTextureOffsetY;
        int texW = this.styleFile.background != null && this.styleFile.background.textureWidth != null
            ? this.styleFile.background.textureWidth.intValue()
            : MMCEGuiExtConfig.upgradeBus.backgroundTextureWidth;
        int texH = this.styleFile.background != null && this.styleFile.background.textureHeight != null
            ? this.styleFile.background.textureHeight.intValue()
            : MMCEGuiExtConfig.upgradeBus.backgroundTextureHeight;
        int corner = this.styleFile.background != null && this.styleFile.background.corner != null
            ? this.styleFile.background.corner.intValue()
            : MMCEGuiExtConfig.upgradeBus.backgroundCorner;
        boolean useNineSlice = this.styleFile.background != null && this.styleFile.background.useNineSlice != null
            ? this.styleFile.background.useNineSlice.booleanValue()
            : MMCEGuiExtConfig.upgradeBus.useNineSlice;

        if (this.customBackgroundTexture != null) {
            this.mc.getTextureManager().bindTexture(this.customBackgroundTexture);
            drawResizableArea(
                guiLeft - offsetX,
                guiTop - offsetY,
                this.xSize + offsetX,
                this.ySize + offsetY,
                useNineSlice,
                texW,
                texH,
                corner
            );
        } else if (!MMCEGuiExtConfig.upgradeBus.hideDefaultBackground) {
            this.mc.getTextureManager().bindTexture(DEFAULT_TEXTURE);
            Gui.drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, this.xSize, this.ySize, BASE_WIDTH, BASE_HEIGHT);
        }

        for (Point point : slotLayout) {
            this.drawTexturedModalRect(guiLeft + point.x - 1, guiTop + point.y - 1, 7, 130, 18, 18);
        }

        if (MMCEGuiExtConfig.upgradeBus.hidePlayerInventory) {
            maskPlayerInventoryArea(guiLeft, guiTop);
        }
        GlobalTextureLayerConfig.drawLayers(
            this.textureLayers,
            false,
            guiLeft,
            guiTop,
            offsetX,
            offsetY
        );
        scrollbar.draw(this, mc);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scrollbar.wheel(wheel);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        scrollbar.click(mouseX, mouseY);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        scrollbar.click(mouseX, mouseY);
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    private void applySlotLayout() {
        if (this.inventorySlots == null || this.inventorySlots.inventorySlots == null) {
            return;
        }

        List<Slot> slots = this.inventorySlots.inventorySlots;
        int total = slots.size();
        int busSlotCount = upgradeBus.getInventory().getSlots();
        int playerSlotCount = 36;
        if (total < playerSlotCount + busSlotCount) {
            return;
        }

        for (int index = 0; index < playerSlotCount; index++) {
            Slot slot = slots.get(index);
            if (slot == null) {
                continue;
            }
            if (MMCEGuiExtConfig.upgradeBus.hidePlayerInventory) {
                slot.xPos = HIDDEN_SLOT_POS;
                slot.yPos = HIDDEN_SLOT_POS;
            } else if (index < 27) {
                slot.xPos = MMCEGuiExtConfig.upgradeBus.playerInventoryX + (index % 9) * 18;
                slot.yPos = MMCEGuiExtConfig.upgradeBus.playerInventoryY + (index / 9) * 18;
            } else {
                slot.xPos = MMCEGuiExtConfig.upgradeBus.playerInventoryX + (index - 27) * 18;
                slot.yPos = MMCEGuiExtConfig.upgradeBus.playerHotbarY;
            }
        }

        for (int index = 0; index < busSlotCount; index++) {
            Slot slot = slots.get(playerSlotCount + index);
            if (slot == null) {
                continue;
            }
            Point point = index < slotLayout.size() ? slotLayout.get(index) : null;
            if (point == null) {
                slot.xPos = HIDDEN_SLOT_POS;
                slot.yPos = HIDDEN_SLOT_POS;
            } else {
                slot.xPos = point.x;
                slot.yPos = point.y;
            }
        }
    }

    private void updateScrollbar(int range) {
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;
        scrollbar.setLeft(guiLeft + MMCEGuiExtConfig.upgradeBus.scrollbarX)
            .setTop(guiTop + MMCEGuiExtConfig.upgradeBus.scrollbarY)
            .setHeight(MMCEGuiExtConfig.upgradeBus.scrollbarHeight)
            .setRange(0, range, 1);
    }

    private void drawResizableArea(int x, int y, int width, int height, boolean useNineSlice, int texW, int texH, int corner) {
        if (!useNineSlice) {
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, texW, texH);
            return;
        }
        GuiRenderUtils.drawNineSlice(x, y, width, height, texW, texH, corner);
    }

    private void maskPlayerInventoryArea(int guiLeft, int guiTop) {
        Gui.drawRect(
            guiLeft + MMCEGuiExtConfig.upgradeBus.playerInventoryX,
            guiTop + MMCEGuiExtConfig.upgradeBus.playerInventoryY,
            guiLeft + MMCEGuiExtConfig.upgradeBus.playerInventoryX + 162,
            guiTop + MMCEGuiExtConfig.upgradeBus.playerInventoryY + 76,
            PLAYER_INVENTORY_MASK_COLOR
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
        String raw = MMCEGuiExtConfig.upgradeBus.backgroundTexture == null ? "" : MMCEGuiExtConfig.upgradeBus.backgroundTexture.trim();
        return GuiRenderUtils.parseOptionalTexture(raw);
    }

    private static List<Point> buildDefaultLayout(int slotCount) {
        List<Point> points = new ArrayList<Point>(slotCount);
        int x = 8;
        int y = 17;
        for (int i = 0; i < slotCount; i++) {
            points.add(new Point(x, y));
            x += 18;
            if ((i + 1) % 3 == 0) {
                x = 8;
                y += 18;
            }
        }
        return points;
    }

    private static void collectBoundedMachineDescriptions(final List<String> desc,
                                                          final Map<BlockPos, DynamicMachine> boundedMachine,
                                                          final Map<UpgradeType, List<MachineUpgrade>> founded) {
        if (boundedMachine.isEmpty()) {
            desc.add(I18n.format("gui.upgradebus.bounded.empty"));
            return;
        } else {
            desc.add(I18n.format("gui.upgradebus.bounded", boundedMachine.size()));
        }

        boundedMachine.forEach((pos, machine) -> {
            desc.add(String.format("%s (%s)", machine.getLocalizedName(), MiscUtils.posToString(pos)));
            founded.forEach((type, upgrades) -> {
                for (final MachineUpgrade upgrade : upgrades) {
                    if (type.isCompatible(machine)) {
                        return;
                    }

                    desc.add("   " + I18n.format(
                        "gui.upgradebus.incompatible", upgrade.getType().getLocalizedName()));
                }
            });
        });
        desc.add("");
    }

    private static void collectUpgradeDescriptions(final TileUpgradeBus.UpgradeBusProvider component,
                                                   final List<String> desc,
                                                   final Map<UpgradeType, List<MachineUpgrade>> founded) {
        founded.values().forEach(upgrades -> upgrades.forEach(upgrade -> {
            upgrade.readNBT(component.getUpgradeCustomData(upgrade));

            int stackSize = upgrade.getStackSize();
            desc.add(stackSize + "x " + upgrade.getType().getLocalizedName());

            List<String> busDesc = upgrade.getBusGUIDescriptions();
            if (busDesc.isEmpty()) {
                return;
            }

            desc.addAll(busDesc);
            desc.add("");
        }));
    }
}
