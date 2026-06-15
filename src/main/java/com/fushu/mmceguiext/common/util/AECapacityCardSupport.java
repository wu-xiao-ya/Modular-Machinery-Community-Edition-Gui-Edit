package com.fushu.mmceguiext.common.util;

import com.fushu.mmceguiext.common.registry.CustomCapacityCardRegistry;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public final class AECapacityCardSupport {
    private AECapacityCardSupport() {
    }

    public static boolean isCapacityCard(@Nonnull ItemStack stack) {
        return !stack.isEmpty() && !CustomCapacityCardRegistry.resolveModifier(stack).isEmpty();
    }
}
