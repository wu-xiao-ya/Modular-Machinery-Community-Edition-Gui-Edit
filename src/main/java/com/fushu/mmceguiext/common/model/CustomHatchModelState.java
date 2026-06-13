package com.fushu.mmceguiext.common.model;

import net.minecraftforge.common.property.IUnlistedProperty;

public final class CustomHatchModelState {
    public static final IUnlistedProperty<CustomHatchRenderState> RENDER_STATE = new RenderStateProperty();

    private CustomHatchModelState() {
    }

    private static final class RenderStateProperty implements IUnlistedProperty<CustomHatchRenderState> {
        @Override
        public String getName() {
            return "render_state";
        }

        @Override
        public boolean isValid(CustomHatchRenderState value) {
            return true;
        }

        @Override
        public Class<CustomHatchRenderState> getType() {
            return CustomHatchRenderState.class;
        }

        @Override
        public String valueToString(CustomHatchRenderState value) {
            return value == null ? "null" : value.toString();
        }
    }
}
