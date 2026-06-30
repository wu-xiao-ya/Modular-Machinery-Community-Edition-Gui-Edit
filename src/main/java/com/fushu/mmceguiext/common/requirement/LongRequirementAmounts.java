package com.fushu.mmceguiext.common.requirement;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;

import java.util.List;

public final class LongRequirementAmounts {
    private LongRequirementAmounts() {
    }

    public static long applyModifiers(List<RecipeModifier> modifiers, ComponentRequirement<?, ?> requirement, long amount) {
        return sanitizeAmount(Math.round(RecipeModifier.applyModifiers(modifiers, requirement, (double) Math.max(0L, amount), false)));
    }

    public static long applyModifiers(RecipeCraftingContext context,
                                      ComponentRequirement<?, ?> requirement,
                                      long amount) {
        return sanitizeAmount(Math.round(RecipeModifier.applyModifiers(context, requirement, (double) Math.max(0L, amount), false)));
    }

    public static long sanitizeAmount(long amount) {
        return Math.max(0L, amount);
    }

    public static int downcastAmount(long value) {
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }

    public static long saturatedMultiply(long left, int right) {
        if (left <= 0L || right <= 0) {
            return 0L;
        }
        if (left > Long.MAX_VALUE / (long) right) {
            return Long.MAX_VALUE;
        }
        return left * (long) right;
    }

    public static long saturatedAdd(long left, long right) {
        if (left <= 0L) {
            return Math.max(0L, right);
        }
        if (right <= 0L) {
            return left;
        }
        if (left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
