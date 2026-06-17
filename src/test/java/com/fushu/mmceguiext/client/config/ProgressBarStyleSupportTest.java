package com.fushu.mmceguiext.client.config;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ProgressBarStyleSupportTest {
    @Test
    public void normalizeProgressBarSourceAcceptsLegacyAndFriendlyAliases() {
        assertEquals("machine_progress", ProgressBarStyleSupport.normalizeProgressBarSource(null));
        assertEquals("machine_progress", ProgressBarStyleSupport.normalizeProgressBarSource("active_recipe"));
        assertEquals("machine_progress", ProgressBarStyleSupport.normalizeProgressBarSource("Active Recipe"));
        assertEquals("factory_average", ProgressBarStyleSupport.normalizeProgressBarSource("factory_average"));
        assertNull(ProgressBarStyleSupport.normalizeProgressBarSource("unknown_source"));
    }

    @Test
    public void normalizeProgressValueClampsAndAppliesBounds() {
        MachineGuiStyleManager.ProgressBarStyle bar = new MachineGuiStyleManager.ProgressBarStyle();
        bar.min = Float.valueOf(10.0F);
        bar.max = Float.valueOf(110.0F);
        assertEquals(0.0F, ProgressBarStyleSupport.normalizeProgressValue(bar, 5.0F), 0.0001F);
        assertEquals(0.5F, ProgressBarStyleSupport.normalizeProgressValue(bar, 60.0F), 0.0001F);
        assertEquals(1.0F, ProgressBarStyleSupport.normalizeProgressValue(bar, 130.0F), 0.0001F);
    }

    @Test
    public void computeFillBoundsRespectsDirection() {
        assertArrayEquals(new int[] {10, 20, 60, 8}, ProgressBarStyleSupport.computeFillBounds(10, 20, 120, 8, "left_to_right", 0.5F));
        assertArrayEquals(new int[] {70, 20, 60, 8}, ProgressBarStyleSupport.computeFillBounds(10, 20, 120, 8, "right_to_left", 0.5F));
        assertArrayEquals(new int[] {10, 20, 120, 4}, ProgressBarStyleSupport.computeFillBounds(10, 20, 120, 8, "top_to_bottom", 0.5F));
        assertArrayEquals(new int[] {10, 24, 120, 4}, ProgressBarStyleSupport.computeFillBounds(10, 20, 120, 8, "bottom_to_top", 0.5F));
    }
}
