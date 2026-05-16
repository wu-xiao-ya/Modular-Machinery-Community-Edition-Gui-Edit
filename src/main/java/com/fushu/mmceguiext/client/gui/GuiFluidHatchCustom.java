package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.fushu.mmceguiext.client.config.GlobalGuiStyleManager;
import hellfirepvp.modularmachinery.client.gui.GuiContainerFluidHatch;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

public class GuiFluidHatchCustom extends GuiContainerFluidHatch {
    private static final ResourceLocation DEFAULT_TEXTURE =
        new ResourceLocation("modularmachinery", "textures/gui/guibar.png");

    @Nullable
    private ResourceLocation customBackgroundTexture;
    private java.util.List<GlobalTextureLayerConfig.LayerDef> textureLayers;
    private GlobalGuiStyleManager.StyleFile styleFile = GlobalGuiStyleManager.StyleFile.EMPTY;
    private final Object owner;

    public GuiFluidHatchCustom(Object tileFluidTank, EntityPlayer opening) {
        super((hellfirepvp.modularmachinery.common.tiles.base.TileFluidTank) tileFluidTank, opening);
        this.owner = tileFluidTank;
    }

    @Override
    public void initGui() {
        this.styleFile = GlobalGuiStyleManager.load(MMCEGuiExtConfig.fluidHatch.styleFile);
        this.customBackgroundTexture = resolveBackgroundTexture();
        this.textureLayers = !this.styleFile.layers.isEmpty()
            ? this.styleFile.layers
            : GlobalTextureLayerConfig.parse(MMCEGuiExtConfig.fluidHatch.textureLayers);
        super.initGui();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        int offsetX = this.styleFile.background != null && this.styleFile.background.offsetX != null
            ? this.styleFile.background.offsetX.intValue()
            : MMCEGuiExtConfig.fluidHatch.backgroundTextureOffsetX;
        int offsetY = this.styleFile.background != null && this.styleFile.background.offsetY != null
            ? this.styleFile.background.offsetY.intValue()
            : MMCEGuiExtConfig.fluidHatch.backgroundTextureOffsetY;
        int texW = this.styleFile.background != null && this.styleFile.background.textureWidth != null
            ? this.styleFile.background.textureWidth.intValue()
            : MMCEGuiExtConfig.fluidHatch.backgroundTextureWidth;
        int texH = this.styleFile.background != null && this.styleFile.background.textureHeight != null
            ? this.styleFile.background.textureHeight.intValue()
            : MMCEGuiExtConfig.fluidHatch.backgroundTextureHeight;
        int corner = this.styleFile.background != null && this.styleFile.background.corner != null
            ? this.styleFile.background.corner.intValue()
            : MMCEGuiExtConfig.fluidHatch.backgroundCorner;
        boolean useNineSlice = this.styleFile.background != null && this.styleFile.background.useNineSlice != null
            ? this.styleFile.background.useNineSlice.booleanValue()
            : MMCEGuiExtConfig.fluidHatch.useNineSlice;

        this.mc.getTextureManager().bindTexture(this.customBackgroundTexture);
        if (useNineSlice) {
            GuiRenderUtils.drawNineSlice(i - offsetX, j - offsetY, this.xSize + offsetX, this.ySize + offsetY, texW, texH, corner);
        } else {
            this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);
        }
        GlobalTextureLayerConfig.drawLayers(this.textureLayers, false, i, j, offsetX, offsetY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        drawConfiguredTexts();
        GlobalTextureLayerConfig.drawLayers(
            this.textureLayers,
            true,
            this.guiLeft,
            this.guiTop,
            MMCEGuiExtConfig.fluidHatch.backgroundTextureOffsetX,
            MMCEGuiExtConfig.fluidHatch.backgroundTextureOffsetY
        );
    }

    private ResourceLocation resolveBackgroundTexture() {
        if (this.styleFile.background != null && this.styleFile.background.texture != null && !this.styleFile.background.texture.trim().isEmpty()) {
            return GuiRenderUtils.parseTexture(this.styleFile.background.texture, DEFAULT_TEXTURE);
        }
        return GuiRenderUtils.parseTexture(MMCEGuiExtConfig.fluidHatch.backgroundTexture, DEFAULT_TEXTURE);
    }

    private void drawConfiguredTexts() {
        if (this.styleFile.texts == null || this.styleFile.texts.isEmpty()) {
            return;
        }
        FluidStack fluid = getFluid();
        int amount = fluid == null ? 0 : fluid.amount;
        int capacity = getCapacity();
        for (GlobalGuiStyleManager.TextDef text : this.styleFile.texts) {
            String value = resolveTextValue(text.value, fluid, amount, capacity);
            if (value == null || value.isEmpty()) {
                continue;
            }
            int color = GuiRenderUtils.parseColorARGBOrDefault(text.color, 0xFFFFFF);
            if (text.scale != null && text.scale.floatValue() > 0F && text.scale.floatValue() != 1F) {
                GlStateManager.pushMatrix();
                GlStateManager.scale(text.scale.floatValue(), text.scale.floatValue(), 1.0F);
                this.fontRenderer.drawStringWithShadow(
                    value,
                    Math.round(text.x / text.scale.floatValue()),
                    Math.round(text.y / text.scale.floatValue()),
                    color
                );
                GlStateManager.popMatrix();
            } else {
                this.fontRenderer.drawStringWithShadow(value, text.x, text.y, color);
            }
        }
    }

    @Nullable
    private String resolveTextValue(@Nullable String key, @Nullable FluidStack fluid, int amount, int capacity) {
        if (key == null) {
            return null;
        }
        if ("fluid.name".equalsIgnoreCase(key)) {
            return fluid == null ? I18n.format("tooltip.fluidhatch.empty") : fluid.getLocalizedName();
        }
        if ("tank.amount".equalsIgnoreCase(key)) {
            return Integer.toString(amount);
        }
        if ("tank.capacity".equalsIgnoreCase(key)) {
            return Integer.toString(capacity);
        }
        if ("tank.amount_capacity".equalsIgnoreCase(key)) {
            return amount + " / " + capacity;
        }
        if ("tank.empty".equalsIgnoreCase(key)) {
            return fluid == null ? I18n.format("tooltip.fluidhatch.empty") : "";
        }
        return key;
    }

    @Nullable
    private FluidStack getFluid() {
        try {
            Object tank = getTankObject();
            if (tank == null) {
                return null;
            }
            Method m = tank.getClass().getMethod("getFluid");
            return (FluidStack) m.invoke(tank);
        } catch (Exception ignored) {
            return null;
        }
    }

    private int getCapacity() {
        try {
            Object tank = getTankObject();
            if (tank == null) {
                return 0;
            }
            Method m = tank.getClass().getMethod("getCapacity");
            return ((Integer) m.invoke(tank)).intValue();
        } catch (Exception ignored) {
            return 0;
        }
    }

    @Nullable
    private Object getTankObject() {
        try {
            Method m = this.owner.getClass().getMethod("getTank");
            return m.invoke(this.owner);
        } catch (Exception ignored) {
            return null;
        }
    }
}
