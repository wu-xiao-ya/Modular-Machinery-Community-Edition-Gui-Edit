package com.fushu.mmceguiext.common.model;

import javax.annotation.Nullable;

public final class CustomHatchRenderState {
    @Nullable
    public final String definitionId;
    public final double fluidFillRatio;
    public final double gasFillRatio;
    public final double energyFillRatio;

    public CustomHatchRenderState(@Nullable String definitionId, double fluidFillRatio, double gasFillRatio, double energyFillRatio) {
        this.definitionId = definitionId;
        this.fluidFillRatio = clamp(fluidFillRatio);
        this.gasFillRatio = clamp(gasFillRatio);
        this.energyFillRatio = clamp(energyFillRatio);
    }

    private static double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    @Override
    public String toString() {
        return (this.definitionId == null ? "" : this.definitionId)
            + "|" + this.fluidFillRatio
            + "|" + this.gasFillRatio
            + "|" + this.energyFillRatio;
    }
}
