package com.fushu.mmceguiext.client.config;

import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;

import javax.annotation.Nullable;
import java.util.Locale;

public final class ProgressBarStyleSupport {
    private ProgressBarStyleSupport() {
    }

    @Nullable
    public static String normalizeProgressBarSource(@Nullable String raw) {
        String text = safeTrim(raw);
        if (text.isEmpty()) {
            return "machine_progress";
        }
        text = text.toLowerCase(Locale.ROOT);
        if ("machine_progress".equals(text)
            || "active_recipe".equals(text)
            || "active recipe".equals(text)
            || "active".equals(text)
            || "recipe_progress".equals(text)
            || "current_recipe".equals(text)
            || "current recipe".equals(text)
            || "default".equals(text)) {
            return "machine_progress";
        }
        if ("factory_first".equals(text)
            || "factory_thread".equals(text)
            || "factory_core".equals(text)
            || "factory_average".equals(text)
            || "factory_max".equals(text)) {
            return text;
        }
        return null;
    }

    public static float normalizeProgressValue(@Nullable MachineGuiStyleManager.ProgressBarStyle bar, float raw) {
        float value = Float.isFinite(raw) ? raw : 0.0F;
        if (bar == null) {
            return clamp01(value);
        }
        float min = bar.min == null ? 0.0F : bar.min.floatValue();
        float max = bar.max == null ? 1.0F : bar.max.floatValue();
        if (Math.abs(max - min) > 1.0E-6F) {
            value = (value - min) / (max - min);
        }
        return clamp01(value);
    }

    public static float recipeProgress(@Nullable ActiveMachineRecipe recipe) {
        if (recipe == null || recipe.getTotalTick() <= 0) {
            return 0.0F;
        }
        return (float) recipe.getTick() / (float) recipe.getTotalTick();
    }

    public static float averageProgress(@Nullable java.util.List<? extends ActiveMachineRecipe> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return 0.0F;
        }
        float total = 0.0F;
        int count = 0;
        for (ActiveMachineRecipe recipe : recipes) {
            if (recipe == null || recipe.getTotalTick() <= 0) {
                continue;
            }
            total += recipeProgress(recipe);
            count++;
        }
        return count <= 0 ? 0.0F : total / count;
    }

    public static float maxProgress(@Nullable java.util.List<? extends ActiveMachineRecipe> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return 0.0F;
        }
        float max = 0.0F;
        for (ActiveMachineRecipe recipe : recipes) {
            if (recipe == null || recipe.getTotalTick() <= 0) {
                continue;
            }
            max = Math.max(max, recipeProgress(recipe));
        }
        return max;
    }

    public static int[] computeFillBounds(
        int x,
        int y,
        int width,
        int height,
        @Nullable String direction,
        float progress
    ) {
        int fillWidth = Math.max(0, Math.min(width, (int) Math.floor(width * clamp01(progress))));
        int fillHeight = Math.max(0, Math.min(height, (int) Math.floor(height * clamp01(progress))));
        String trimmedDirection = safeTrim(direction);
        String normalizedDirection = trimmedDirection.isEmpty() ? "" : trimmedDirection.toLowerCase(Locale.ROOT);
        if ("right_to_left".equals(normalizedDirection)) {
            return new int[] {x + width - fillWidth, y, fillWidth, height};
        }
        if ("top_to_bottom".equals(normalizedDirection)) {
            return new int[] {x, y, width, fillHeight};
        }
        if ("bottom_to_top".equals(normalizedDirection)) {
            return new int[] {x, y + height - fillHeight, width, fillHeight};
        }
        return new int[] {x, y, fillWidth, height};
    }

    public static int[] computeFillTextureBounds(
        int textureWidth,
        int textureHeight,
        @Nullable String direction,
        float progress
    ) {
        int safeTextureWidth = Math.max(1, textureWidth);
        int safeTextureHeight = Math.max(1, textureHeight);
        int fillWidth = Math.max(0, Math.min(safeTextureWidth, (int) Math.floor(safeTextureWidth * clamp01(progress))));
        int fillHeight = Math.max(0, Math.min(safeTextureHeight, (int) Math.floor(safeTextureHeight * clamp01(progress))));
        String trimmedDirection = safeTrim(direction);
        String normalizedDirection = trimmedDirection.isEmpty() ? "" : trimmedDirection.toLowerCase(Locale.ROOT);
        if ("right_to_left".equals(normalizedDirection)) {
            return new int[] {safeTextureWidth - fillWidth, 0, fillWidth, safeTextureHeight};
        }
        if ("top_to_bottom".equals(normalizedDirection)) {
            return new int[] {0, 0, safeTextureWidth, fillHeight};
        }
        if ("bottom_to_top".equals(normalizedDirection)) {
            return new int[] {0, safeTextureHeight - fillHeight, safeTextureWidth, fillHeight};
        }
        return new int[] {0, 0, fillWidth, safeTextureHeight};
    }

    public static float clamp01(float value) {
        return value < 0.0F ? 0.0F : Math.min(1.0F, value);
    }

    public static String safeTrim(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
