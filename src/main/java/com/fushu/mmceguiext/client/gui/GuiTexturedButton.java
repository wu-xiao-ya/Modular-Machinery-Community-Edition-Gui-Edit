package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public class GuiTexturedButton extends GuiButton {
    @Nullable
    private final ResourceLocation texture;
    @Nullable
    private final ResourceLocation hoverTexture;
    @Nullable
    private final ResourceLocation pressedTexture;
    @Nullable
    private final ResourceLocation disabledTexture;
    private final int u;
    private final int v;
    private final int hoverU;
    private final int hoverV;
    private final int pressedU;
    private final int pressedV;
    private final int disabledU;
    private final int disabledV;
    private final int textureWidth;
    private final int textureHeight;
    private final boolean useNineSlice;
    private final int corner;
    private final int textColor;
    private final int hoverTextColor;
    private final int disabledTextColor;
    private final boolean drawLabel;

    private GuiTexturedButton(Builder builder) {
        super(builder.id, builder.x, builder.y, builder.width, builder.height, builder.label == null ? "" : builder.label);
        this.texture = builder.texture;
        this.hoverTexture = builder.hoverTexture;
        this.pressedTexture = builder.pressedTexture;
        this.disabledTexture = builder.disabledTexture;
        this.u = builder.u;
        this.v = builder.v;
        this.hoverU = builder.hoverU;
        this.hoverV = builder.hoverV;
        this.pressedU = builder.pressedU;
        this.pressedV = builder.pressedV;
        this.disabledU = builder.disabledU;
        this.disabledV = builder.disabledV;
        this.textureWidth = Math.max(1, builder.textureWidth);
        this.textureHeight = Math.max(1, builder.textureHeight);
        this.useNineSlice = builder.useNineSlice;
        this.corner = Math.max(1, builder.corner);
        this.textColor = builder.textColor;
        this.hoverTextColor = builder.hoverTextColor;
        this.disabledTextColor = builder.disabledTextColor;
        this.drawLabel = builder.drawLabel;
    }

    public static Builder builder(int id, int x, int y, int width, int height, @Nullable String label) {
        return new Builder(id, x, y, width, height, label);
    }

    public static GuiButton forStyle(
        int id,
        int x,
        int y,
        int width,
        int height,
        @Nullable String label,
        MachineGuiStyleManager.ButtonStyle style
    ) {
        ResourceLocation texture = GuiRenderUtils.parseOptionalTexture(style.texture);
        ResourceLocation hoverTexture = GuiRenderUtils.parseOptionalTexture(style.hoverTexture);
        ResourceLocation pressedTexture = GuiRenderUtils.parseOptionalTexture(style.pressedTexture);
        ResourceLocation disabledTexture = GuiRenderUtils.parseOptionalTexture(style.disabledTexture);
        if (texture == null && hoverTexture == null && pressedTexture == null && disabledTexture == null) {
            return new GuiButton(id, x, y, width, height, label);
        }

        int textureWidth = style.textureWidth == null ? width : Math.max(1, style.textureWidth.intValue());
        int textureHeight = style.textureHeight == null ? height : Math.max(1, style.textureHeight.intValue());
        int normalU = style.u == null ? 0 : Math.max(0, style.u.intValue());
        int normalV = style.v == null ? 0 : Math.max(0, style.v.intValue());
        int hoverU = style.hoverU == null ? normalU : Math.max(0, style.hoverU.intValue());
        int hoverV = style.hoverV == null ? normalV : Math.max(0, style.hoverV.intValue());
        int pressedU = style.pressedU == null ? hoverU : Math.max(0, style.pressedU.intValue());
        int pressedV = style.pressedV == null ? hoverV : Math.max(0, style.pressedV.intValue());
        int disabledU = style.disabledU == null ? normalU : Math.max(0, style.disabledU.intValue());
        int disabledV = style.disabledV == null ? normalV : Math.max(0, style.disabledV.intValue());
        int normalTextColor = style.textColor == null ? 0xE0E0E0 : style.textColor.intValue();
        int hoverTextColor = style.hoverTextColor == null ? 0xFFFFA0 : style.hoverTextColor.intValue();
        int disabledTextColor = style.disabledTextColor == null ? 0xA0A0A0 : style.disabledTextColor.intValue();
        boolean useNineSlice = style.useNineSlice != null && style.useNineSlice.booleanValue();
        int corner = style.corner == null ? 4 : Math.max(1, style.corner.intValue());
        boolean drawLabel = style.drawLabel == null || style.drawLabel.booleanValue();

        return builder(id, x, y, width, height, label)
            .texture(texture)
            .hoverTexture(hoverTexture)
            .pressedTexture(pressedTexture)
            .disabledTexture(disabledTexture)
            .uv(normalU, normalV)
            .hoverUv(hoverU, hoverV)
            .pressedUv(pressedU, pressedV)
            .disabledUv(disabledU, disabledV)
            .textureSize(textureWidth, textureHeight)
            .nineSlice(useNineSlice, corner)
            .textColors(normalTextColor, hoverTextColor, disabledTextColor)
            .drawLabel(drawLabel)
            .build();
    }

    public static GuiButton forStyle(
        int id,
        int x,
        int y,
        int width,
        int height,
        @Nullable String label,
        MachineGuiStyleManager.ButtonStyle style,
        @Nullable MachineGuiStyleManager.ButtonCycleStateStyle stateStyle
    ) {
        if (stateStyle == null) {
            return forStyle(id, x, y, width, height, label, style);
        }
        MachineGuiStyleManager.ButtonStyle merged = new MachineGuiStyleManager.ButtonStyle();
        merged.texture = stateStyle.texture != null ? stateStyle.texture : style.texture;
        merged.hoverTexture = stateStyle.hoverTexture != null ? stateStyle.hoverTexture : style.hoverTexture;
        merged.pressedTexture = stateStyle.pressedTexture != null ? stateStyle.pressedTexture : style.pressedTexture;
        merged.disabledTexture = stateStyle.disabledTexture != null ? stateStyle.disabledTexture : style.disabledTexture;
        merged.textureWidth = stateStyle.textureWidth != null ? stateStyle.textureWidth : style.textureWidth;
        merged.textureHeight = stateStyle.textureHeight != null ? stateStyle.textureHeight : style.textureHeight;
        merged.u = stateStyle.u != null ? stateStyle.u : style.u;
        merged.v = stateStyle.v != null ? stateStyle.v : style.v;
        merged.hoverU = stateStyle.hoverU != null ? stateStyle.hoverU : style.hoverU;
        merged.hoverV = stateStyle.hoverV != null ? stateStyle.hoverV : style.hoverV;
        merged.pressedU = stateStyle.pressedU != null ? stateStyle.pressedU : style.pressedU;
        merged.pressedV = stateStyle.pressedV != null ? stateStyle.pressedV : style.pressedV;
        merged.disabledU = stateStyle.disabledU != null ? stateStyle.disabledU : style.disabledU;
        merged.disabledV = stateStyle.disabledV != null ? stateStyle.disabledV : style.disabledV;
        merged.useNineSlice = style.useNineSlice;
        merged.corner = style.corner;
        merged.textColor = stateStyle.textColor != null ? stateStyle.textColor : style.textColor;
        merged.hoverTextColor = stateStyle.hoverTextColor != null ? stateStyle.hoverTextColor : style.hoverTextColor;
        merged.disabledTextColor = stateStyle.disabledTextColor != null ? stateStyle.disabledTextColor : style.disabledTextColor;
        merged.drawLabel = stateStyle.drawLabel != null ? stateStyle.drawLabel : style.drawLabel;
        String effectiveLabel = stateStyle.label != null ? stateStyle.label : label;
        return forStyle(id, x, y, width, height, effectiveLabel, merged);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }
        if (this.texture == null && this.hoverTexture == null && this.pressedTexture == null && this.disabledTexture == null) {
            super.drawButton(mc, mouseX, mouseY, partialTicks);
            return;
        }

        this.hovered = mouseX >= this.x && mouseY >= this.y
            && mouseX < this.x + this.width && mouseY < this.y + this.height;
        ResourceLocation selectedTexture = selectTexture();
        if (selectedTexture != null) {
            mc.getTextureManager().bindTexture(selectedTexture);
            GlStateManager.color(1.0F, 1.0F, 1.0F, this.enabled ? 1.0F : 0.55F);
            int drawU = selectU();
            int drawV = selectV();
            if (this.useNineSlice) {
                GuiRenderUtils.drawNineSlice(
                    this.x, this.y, this.width, this.height, drawU, drawV, this.textureWidth, this.textureHeight, this.corner
                );
            } else {
                Gui.drawModalRectWithCustomSizedTexture(
                    this.x, this.y, drawU, drawV, this.width, this.height, this.textureWidth, this.textureHeight
                );
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }

        if (this.drawLabel && this.displayString != null && !this.displayString.isEmpty()) {
            int color = this.enabled ? (this.hovered ? this.hoverTextColor : this.textColor) : this.disabledTextColor;
            this.drawCenteredString(mc.fontRenderer, this.displayString, this.x + this.width / 2,
                this.y + (this.height - 8) / 2, color);
        }
    }

    @Nullable
    private ResourceLocation selectTexture() {
        if (!this.enabled && this.disabledTexture != null) {
            return this.disabledTexture;
        }
        if (isPressedState() && this.pressedTexture != null) {
            return this.pressedTexture;
        }
        if (this.enabled && this.hovered && this.hoverTexture != null) {
            return this.hoverTexture;
        }
        return this.texture;
    }

    private int selectU() {
        if (!this.enabled && this.disabledTexture != null) {
            return this.disabledU;
        }
        if (!this.enabled) {
            return this.disabledU;
        }
        if (isPressedState()) {
            return this.pressedU;
        }
        if (this.enabled && this.hovered) {
            return this.hoverU;
        }
        return this.u;
    }

    private int selectV() {
        if (!this.enabled && this.disabledTexture != null) {
            return this.disabledV;
        }
        if (!this.enabled) {
            return this.disabledV;
        }
        if (isPressedState()) {
            return this.pressedV;
        }
        if (this.enabled && this.hovered) {
            return this.hoverV;
        }
        return this.v;
    }

    private boolean isPressedState() {
        return this.enabled && this.hovered && org.lwjgl.input.Mouse.isButtonDown(0);
    }

    public static final class Builder {
        private final int id;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        @Nullable
        private final String label;
        @Nullable
        private ResourceLocation texture;
        @Nullable
        private ResourceLocation hoverTexture;
        @Nullable
        private ResourceLocation pressedTexture;
        @Nullable
        private ResourceLocation disabledTexture;
        private int u;
        private int v;
        private int hoverU;
        private int hoverV;
        private int pressedU;
        private int pressedV;
        private int disabledU;
        private int disabledV;
        private int textureWidth;
        private int textureHeight;
        private boolean useNineSlice;
        private int corner = 4;
        private int textColor = 0xE0E0E0;
        private int hoverTextColor = 0xFFFFA0;
        private int disabledTextColor = 0xA0A0A0;
        private boolean drawLabel = true;

        private Builder(int id, int x, int y, int width, int height, @Nullable String label) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.label = label;
            this.textureWidth = width;
            this.textureHeight = height;
            this.hoverU = width;
            this.pressedU = width;
            this.disabledU = width * 2;
        }

        public Builder texture(@Nullable ResourceLocation value) {
            this.texture = value;
            return this;
        }

        public Builder hoverTexture(@Nullable ResourceLocation value) {
            this.hoverTexture = value;
            return this;
        }

        public Builder pressedTexture(@Nullable ResourceLocation value) {
            this.pressedTexture = value;
            return this;
        }

        public Builder disabledTexture(@Nullable ResourceLocation value) {
            this.disabledTexture = value;
            return this;
        }

        public Builder uv(int valueU, int valueV) {
            this.u = valueU;
            this.v = valueV;
            return this;
        }

        public Builder hoverUv(int valueU, int valueV) {
            this.hoverU = valueU;
            this.hoverV = valueV;
            return this;
        }

        public Builder pressedUv(int valueU, int valueV) {
            this.pressedU = valueU;
            this.pressedV = valueV;
            return this;
        }

        public Builder disabledUv(int valueU, int valueV) {
            this.disabledU = valueU;
            this.disabledV = valueV;
            return this;
        }

        public Builder textureSize(int valueWidth, int valueHeight) {
            this.textureWidth = Math.max(1, valueWidth);
            this.textureHeight = Math.max(1, valueHeight);
            return this;
        }

        public Builder nineSlice(boolean value, int valueCorner) {
            this.useNineSlice = value;
            this.corner = Math.max(1, valueCorner);
            return this;
        }

        public Builder textColors(int normal, int hover, int disabled) {
            this.textColor = normal;
            this.hoverTextColor = hover;
            this.disabledTextColor = disabled;
            return this;
        }

        public Builder drawLabel(boolean value) {
            this.drawLabel = value;
            return this;
        }

        public GuiTexturedButton build() {
            return new GuiTexturedButton(this);
        }
    }
}
