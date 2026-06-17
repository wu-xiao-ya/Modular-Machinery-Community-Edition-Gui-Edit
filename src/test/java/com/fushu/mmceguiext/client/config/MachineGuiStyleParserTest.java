package com.fushu.mmceguiext.client.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MachineGuiStyleParserTest {
    @Test
    public void parseMachineJsonWarnsWhenRegistryNameIsMissing() {
        MachineGuiStyleParser.MachineFileParseResult result = MachineGuiStyleParser.parseMachineJson(
            "missing-registry.json",
            "{\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"machineController\": {\n" +
                "      \"backgroundTexture\": \"demo:textures/gui/test.png\"\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertNull(result.namespacedKey);
        assertTrue(containsWarning(result, "registryname is missing or empty"));
    }

    @Test
    public void parseMachineJsonSkipsInvalidEditorAndLayerEntriesWithWarnings() {
        MachineGuiStyleParser.MachineFileParseResult result = MachineGuiStyleParser.parseMachineJson(
            "invalid-fields.json",
            "{\n" +
                "  \"registryname\": \"demo_machine\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"machineController\": {\n" +
                "      \"guiWidth\": 0,\n" +
                "      \"smartInterfaceEditors\": [\n" +
                "        {\"x\": 12, \"y\": 8},\n" +
                "        {\"x\": 20, \"y\": -5, \"virtualKey\": \"demo\"},\n" +
                "        {\"x\": 24, \"y\": 12, \"virtualKey\": \"demo_valid\", \"inputWidth\": 0}\n" +
                "      ],\n" +
                "      \"foregroundLayers\": [\n" +
                "        123,\n" +
                "        {\"id\": \"missing_texture\"},\n" +
                "        {\"texture\": \"demo:textures/gui/layer.png\", \"width\": 0, \"opacity\": 128}\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertEquals("modularmachinery:demo_machine", result.namespacedKey);
        assertTrue(result.machineNodePresent);
        assertNotNull(result.machineStyle);
        assertNull(result.machineStyle.guiWidth);
        assertNotNull(result.machineStyle.smartInterfaceEditors);
        assertEquals(1, result.machineStyle.smartInterfaceEditors.size());
        assertEquals("demo_valid", result.machineStyle.smartInterfaceEditors.get(0).virtualKey);
        assertNull(result.machineStyle.smartInterfaceEditors.get(0).inputWidth);
        assertNotNull(result.machineStyle.textureLayers);
        assertEquals(1, result.machineStyle.textureLayers.size());
        assertEquals("demo:textures/gui/layer.png", result.machineStyle.textureLayers.get(0).texture);
        assertNull(result.machineStyle.textureLayers.get(0).width);
        assertEquals(Float.valueOf(128.0F / 255.0F), result.machineStyle.textureLayers.get(0).alpha);
        assertTrue(containsWarning(result, "machineController.guiWidth must be >= 1"));
        assertTrue(containsWarning(result, "smartInterfaceEditors[0] is missing required fields"));
        assertTrue(containsWarning(result, "smartInterfaceEditors[1].y must be >= 0"));
        assertTrue(containsWarning(result, "foregroundLayers[0] must be a string or object"));
        assertTrue(containsWarning(result, "foregroundLayers[1] is missing required field texture"));
        assertTrue(containsWarning(result, "foregroundLayers[2].width must be >= 1"));
    }

    @Test
    public void parseMachineJsonParsesValidFactoryFieldsAndColorAliases() {
        MachineGuiStyleParser.MachineFileParseResult result = MachineGuiStyleParser.parseMachineJson(
            "valid-factory.json",
            "{\n" +
                "  \"registryname\": \"demo:factory_machine\",\n" +
                "  \"mmceGuiExt\": {\n" +
                "    \"factory\": {\n" +
                "      \"backgroundTexture\": \"demo:textures/gui/factory.png\",\n" +
                "      \"guiWidth\": 420,\n" +
                "      \"guiHeight\": 240,\n" +
                "      \"enableRightExtension\": false,\n" +
                "      \"specialThreadBgColor\": \"7DD3FC\",\n" +
                "      \"threadQueueX\": 12,\n" +
                "      \"threadQueueY\": 14,\n" +
                "      \"threadScrollbarX\": 98,\n" +
                "      \"threadScrollbarY\": 22,\n" +
                "      \"threadVisibleRows\": 7,\n" +
                "      \"threadRowWidth\": 90,\n" +
                "      \"threadRowHeight\": 34,\n" +
                "      \"smartInterfaceEditors\": [\n" +
                "        {\"x\": 30, \"y\": 40, \"virtualKey\": \"factory_key\", \"showTitle\": false}\n" +
                "      ],\n" +
                "      \"backgroundLayers\": [\n" +
                "        {\"id\": \"bg\", \"texture\": \"demo:textures/gui/bg_layer.png\", \"texW\": 280, \"texH\": 213, \"alpha\": 0.5}\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertEquals("demo:factory_machine", result.namespacedKey);
        assertEquals("factory_machine", result.pathKey);
        assertTrue(result.factoryNodePresent);
        assertNotNull(result.factoryStyle);
        assertEquals(Integer.valueOf(420), result.factoryStyle.guiWidth);
        assertEquals(Integer.valueOf(240), result.factoryStyle.guiHeight);
        assertEquals(Boolean.TRUE, result.factoryStyle.disableRightExtension);
        assertEquals(Integer.valueOf(0xFF7DD3FC), result.factoryStyle.specialThreadBackgroundColor);
        assertEquals(Integer.valueOf(12), result.factoryStyle.threadQueueX);
        assertEquals(Integer.valueOf(14), result.factoryStyle.threadQueueY);
        assertEquals(Integer.valueOf(98), result.factoryStyle.threadScrollbarX);
        assertEquals(Integer.valueOf(22), result.factoryStyle.threadScrollbarY);
        assertEquals(Integer.valueOf(7), result.factoryStyle.threadVisibleRows);
        assertEquals(Integer.valueOf(90), result.factoryStyle.threadRowWidth);
        assertEquals(Integer.valueOf(34), result.factoryStyle.threadRowHeight);
        assertNotNull(result.factoryStyle.smartInterfaceEditors);
        assertEquals(1, result.factoryStyle.smartInterfaceEditors.size());
        assertEquals(Boolean.FALSE, result.factoryStyle.smartInterfaceEditors.get(0).showTitle);
        assertNotNull(result.factoryStyle.textureLayers);
        assertEquals(1, result.factoryStyle.textureLayers.size());
        assertNull(result.factoryStyle.textureLayers.get(0).foreground);
        assertEquals(Float.valueOf(0.5F), result.factoryStyle.textureLayers.get(0).alpha);
        assertTrue(result.warnings.isEmpty());
    }

    @Test
    public void parseMachineJsonAppliesThreadQueueAliasesAndDefaults() {
        MachineGuiStyleParser.MachineFileParseResult result = MachineGuiStyleParser.parseMachineJson(
            "thread-layout.json",
            "{\n" +
                "  \"registryname\": \"demo:thread_layout_machine\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"factoryController\": {\n" +
                "      \"queueX\": 16,\n" +
                "      \"queueY\": 18,\n" +
                "      \"queueScrollbarX\": 102,\n" +
                "      \"queueScrollbarY\": 24,\n" +
                "      \"visibleRows\": 9,\n" +
                "      \"queueRowWidth\": 88,\n" +
                "      \"queueRowHeight\": 36\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertEquals(Integer.valueOf(16), result.factoryStyle.threadQueueX);
        assertEquals(Integer.valueOf(18), result.factoryStyle.threadQueueY);
        assertEquals(Integer.valueOf(102), result.factoryStyle.threadScrollbarX);
        assertEquals(Integer.valueOf(24), result.factoryStyle.threadScrollbarY);
        assertEquals(Integer.valueOf(9), result.factoryStyle.threadVisibleRows);
        assertEquals(Integer.valueOf(88), result.factoryStyle.threadRowWidth);
        assertEquals(Integer.valueOf(36), result.factoryStyle.threadRowHeight);
        assertTrue(result.warnings.isEmpty());
    }

    @Test
    public void parseMachineJsonLeavesThreadQueueFieldsUnsetWhenOmitted() {
        MachineGuiStyleParser.MachineFileParseResult result = MachineGuiStyleParser.parseMachineJson(
            "thread-layout-defaults.json",
            "{\n" +
                "  \"registryname\": \"demo:thread_layout_defaults\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"factoryController\": {\n" +
                "      \"backgroundTexture\": \"demo:textures/gui/factory.png\"\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertNotNull(result.factoryStyle);
        assertNull(result.factoryStyle.threadQueueX);
        assertNull(result.factoryStyle.threadQueueY);
        assertNull(result.factoryStyle.threadScrollbarX);
        assertNull(result.factoryStyle.threadScrollbarY);
        assertNull(result.factoryStyle.threadVisibleRows);
        assertNull(result.factoryStyle.threadRowWidth);
        assertNull(result.factoryStyle.threadRowHeight);
        assertTrue(result.warnings.isEmpty());
    }

    @Test
    public void parseMachineJsonAcceptsQueueVisibleRowsAlias() {
        MachineGuiStyleParser.MachineFileParseResult result = MachineGuiStyleParser.parseMachineJson(
            "thread-layout-visible-rows.json",
            "{\n" +
                "  \"registryname\": \"demo:thread_layout_machine\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"factoryController\": {\n" +
                "      \"queueVisibleRows\": 8\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertEquals(Integer.valueOf(8), result.factoryStyle.threadVisibleRows);
        assertTrue(result.warnings.isEmpty());
    }

    @Test
    public void parseMachineJsonRejectsOutOfRangeThreadQueueSizes() {
        MachineGuiStyleParser.MachineFileParseResult result = MachineGuiStyleParser.parseMachineJson(
            "thread-layout-invalid.json",
            "{\n" +
                "  \"registryname\": \"demo:thread_layout_machine\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"factoryController\": {\n" +
                "      \"threadVisibleRows\": 0,\n" +
                "      \"threadRowWidth\": 23,\n" +
                "      \"threadRowHeight\": 15\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertNull(result.factoryStyle.threadVisibleRows);
        assertNull(result.factoryStyle.threadRowWidth);
        assertNull(result.factoryStyle.threadRowHeight);
        assertTrue(containsWarning(result, "factoryController.threadVisibleRows must be >= 1"));
        assertTrue(containsWarning(result, "factoryController.threadRowWidth must be >= 24"));
        assertTrue(containsWarning(result, "factoryController.threadRowHeight must be >= 16"));
    }

    @Test
    public void parseMachineJsonParsesProgressBarsAndAliases() {
        MachineGuiStyleParser.MachineFileParseResult result = MachineGuiStyleParser.parseMachineJson(
            "progress-bars.json",
            "{\n" +
                "  \"registryname\": \"demo:progress_machine\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"machineController\": {\n" +
                "      \"progressBars\": [\n" +
                "        {\n" +
                "          \"id\": \"main_progress\",\n" +
                "          \"x\": 10,\n" +
                "          \"y\": 12,\n" +
                "          \"width\": 120,\n" +
                "          \"height\": 14,\n" +
                "          \"backgroundColor\": \"33000000\",\n" +
                "          \"fillColor\": \"FF00AAFF\",\n" +
                "          \"borderColor\": \"FFFFFFFF\",\n" +
                "          \"texture\": \"demo:textures/gui/progress.png\",\n" +
                "          \"backgroundTexture\": \"demo:textures/gui/progress_bg.png\",\n" +
                "          \"fillTexture\": \"demo:textures/gui/progress_fill.png\",\n" +
                "          \"textureWidth\": 128,\n" +
                "          \"textureHeight\": 16,\n" +
                "          \"direction\": \"left_to_right\",\n" +
                "          \"source\": \"factory_average\",\n" +
                "          \"threadIndex\": 2,\n" +
                "          \"coreThreadId\": \"core_a\",\n" +
                "          \"min\": 0.0,\n" +
                "          \"max\": 100.0,\n" +
                "          \"priority\": 7,\n" +
                "          \"foreground\": true,\n" +
                "          \"visible\": false,\n" +
                "          \"page\": \"main\",\n" +
                "          \"showText\": true,\n" +
                "          \"textColor\": \"FF101010\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"factoryController\": {\n" +
                "      \"gui_progress_bars\": [\n" +
                "        {\n" +
                "          \"x\": 4,\n" +
                "          \"y\": 5,\n" +
                "          \"width\": 8,\n" +
                "          \"height\": 9,\n" +
                "          \"direction\": \"bottom_to_top\",\n" +
                "          \"source\": \"active_recipe\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertNotNull(result.machineStyle);
        assertNotNull(result.machineStyle.progressBars);
        assertEquals(1, result.machineStyle.progressBars.size());
        MachineGuiStyleManager.ProgressBarStyle bar = result.machineStyle.progressBars.get(0);
        assertEquals("main_progress", bar.id);
        assertEquals(10, bar.x);
        assertEquals(12, bar.y);
        assertEquals(120, bar.width);
        assertEquals(14, bar.height);
        assertEquals(Integer.valueOf(0x33000000), bar.backgroundColor);
        assertEquals(Integer.valueOf(0xFF00AAFF), bar.fillColor);
        assertEquals(Integer.valueOf(0xFFFFFFFF), bar.borderColor);
        assertEquals("demo:textures/gui/progress.png", bar.texture);
        assertEquals("demo:textures/gui/progress_bg.png", bar.backgroundTexture);
        assertEquals("demo:textures/gui/progress_fill.png", bar.fillTexture);
        assertEquals(Integer.valueOf(128), bar.textureWidth);
        assertEquals(Integer.valueOf(16), bar.textureHeight);
        assertEquals("left_to_right", bar.direction);
        assertEquals("factory_average", bar.source);
        assertEquals(Integer.valueOf(2), bar.threadIndex);
        assertEquals("core_a", bar.coreThreadId);
        assertEquals(Float.valueOf(0.0F), bar.min);
        assertEquals(Float.valueOf(100.0F), bar.max);
        assertEquals(Integer.valueOf(7), bar.priority);
        assertEquals(Boolean.TRUE, bar.foreground);
        assertEquals(Boolean.FALSE, bar.visible);
        assertEquals("main", bar.page);
        assertEquals(Boolean.TRUE, bar.showText);
        assertEquals(Integer.valueOf(0xFF101010), bar.textColor);
        assertNotNull(result.factoryStyle);
        assertNotNull(result.factoryStyle.progressBars);
        assertEquals(1, result.factoryStyle.progressBars.size());
        assertEquals("bottom_to_top", result.factoryStyle.progressBars.get(0).direction);
        assertEquals("machine_progress", result.factoryStyle.progressBars.get(0).source);
        assertTrue(result.warnings.isEmpty());
    }

    @Test
    public void parseMachineJsonRejectsInvalidProgressBarDirectionSourceAndSize() {
        MachineGuiStyleParser.MachineFileParseResult result = MachineGuiStyleParser.parseMachineJson(
            "progress-bars-invalid.json",
            "{\n" +
                "  \"registryname\": \"demo:progress_machine_invalid\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"machineController\": {\n" +
                "      \"progress_bars\": [\n" +
                "        {\n" +
                "          \"x\": 1,\n" +
                "          \"y\": 2,\n" +
                "          \"width\": 0,\n" +
                "          \"height\": 5000,\n" +
                "          \"direction\": \"sideways\",\n" +
                "          \"source\": \"unknown_source\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertNotNull(result.machineStyle);
        assertNull(result.machineStyle.progressBars);
        assertTrue(containsWarning(result, "progress_bars[0].width must be >= 1"));
        assertTrue(containsWarning(result, "progress_bars[0].height must be <= 4096"));
    }

    @Test
    public void controllerStyleCopiesAndMergesProgressBars() {
        MachineGuiStyleManager.ControllerStyle base = new MachineGuiStyleManager.ControllerStyle();
        MachineGuiStyleManager.ProgressBarStyle barA = new MachineGuiStyleManager.ProgressBarStyle();
        barA.id = "a";
        barA.x = 1;
        barA.y = 2;
        barA.width = 3;
        barA.height = 4;
        base.progressBars = java.util.Collections.singletonList(barA);

        MachineGuiStyleManager.ControllerStyle overlay = new MachineGuiStyleManager.ControllerStyle();
        MachineGuiStyleManager.ProgressBarStyle barB = new MachineGuiStyleManager.ProgressBarStyle();
        barB.id = "b";
        barB.x = 5;
        barB.y = 6;
        barB.width = 7;
        barB.height = 8;
        overlay.progressBars = java.util.Collections.singletonList(barB);

        MachineGuiStyleManager.ControllerStyle copy = MachineGuiStyleManager.ControllerStyle.copyOf(base);
        assertNotNull(copy.progressBars);
        assertEquals(1, copy.progressBars.size());
        assertEquals("a", copy.progressBars.get(0).id);

        MachineGuiStyleManager.ControllerStyle merged = MachineGuiStyleManager.ControllerStyle.copyOf(base).mergeFrom(overlay);
        assertNotNull(merged.progressBars);
        assertEquals(2, merged.progressBars.size());
        assertEquals("a", merged.progressBars.get(0).id);
        assertEquals("b", merged.progressBars.get(1).id);
        assertFalse(merged.isEmpty());
    }

    @Test
    public void parseMachineJsonParsesSlidersAndAliases() {
        MachineGuiStyleParser.MachineFileParseResult result = MachineGuiStyleParser.parseMachineJson(
            "sliders.json",
            "{\n" +
                "  \"registryname\": \"demo:slider_machine\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"machineController\": {\n" +
                "      \"sliders\": [\n" +
                "        {\n" +
                "          \"id\": \"speed_slider\",\n" +
                "          \"x\": 10,\n" +
                "          \"y\": 12,\n" +
                "          \"width\": 120,\n" +
                "          \"height\": 14,\n" +
                "          \"key\": \"speed\",\n" +
                "          \"min\": 0.0,\n" +
                "          \"max\": 10.0,\n" +
                "          \"step\": 0.5,\n" +
                "          \"value\": 2.5,\n" +
                "          \"direction\": \"horizontal\",\n" +
                "          \"trackColor\": \"33000000\",\n" +
                "          \"fillColor\": \"FF00AAFF\",\n" +
                "          \"thumbColor\": \"FFFFFFFF\",\n" +
                "          \"borderColor\": \"FF101010\",\n" +
                "          \"thumbWidth\": 8,\n" +
                "          \"thumbHeight\": 16,\n" +
                "          \"priority\": 7,\n" +
                "          \"foreground\": true,\n" +
                "          \"visible\": false,\n" +
                "          \"page\": \"main\",\n" +
                "          \"showText\": true,\n" +
                "          \"textColor\": \"FFEEDDCC\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"factoryController\": {\n" +
                "      \"gui_sliders\": [\n" +
                "        {\"x\": 4, \"y\": 5, \"width\": 8, \"height\": 90, \"dataPortKey\": \"pressure\", \"direction\": \"vertical\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertNotNull(result.machineStyle);
        assertNotNull(result.machineStyle.sliders);
        assertEquals(1, result.machineStyle.sliders.size());
        MachineGuiStyleManager.SliderStyle slider = result.machineStyle.sliders.get(0);
        assertEquals("speed_slider", slider.id);
        assertEquals(10, slider.x);
        assertEquals(12, slider.y);
        assertEquals(120, slider.width);
        assertEquals(14, slider.height);
        assertEquals("speed", slider.key);
        assertEquals(Float.valueOf(0.0F), slider.min);
        assertEquals(Float.valueOf(10.0F), slider.max);
        assertEquals(Float.valueOf(0.5F), slider.step);
        assertEquals(Float.valueOf(2.5F), slider.initialValue);
        assertEquals("horizontal", slider.direction);
        assertEquals(Integer.valueOf(0x33000000), slider.trackColor);
        assertEquals(Integer.valueOf(0xFF00AAFF), slider.fillColor);
        assertEquals(Integer.valueOf(0xFFFFFFFF), slider.thumbColor);
        assertEquals(Integer.valueOf(0xFF101010), slider.borderColor);
        assertEquals(Integer.valueOf(8), slider.thumbWidth);
        assertEquals(Integer.valueOf(16), slider.thumbHeight);
        assertEquals(Integer.valueOf(7), slider.priority);
        assertEquals(Boolean.TRUE, slider.foreground);
        assertEquals(Boolean.FALSE, slider.visible);
        assertEquals("main", slider.page);
        assertEquals(Boolean.TRUE, slider.showText);
        assertEquals(Integer.valueOf(0xFFEEDDCC), slider.textColor);
        assertNotNull(result.factoryStyle);
        assertNotNull(result.factoryStyle.sliders);
        assertEquals(1, result.factoryStyle.sliders.size());
        assertEquals("pressure", result.factoryStyle.sliders.get(0).key);
        assertEquals("vertical", result.factoryStyle.sliders.get(0).direction);
        assertTrue(result.warnings.isEmpty());
    }

    @Test
    public void parseMachineJsonRejectsInvalidSliders() {
        MachineGuiStyleParser.MachineFileParseResult result = MachineGuiStyleParser.parseMachineJson(
            "sliders-invalid.json",
            "{\n" +
                "  \"registryname\": \"demo:slider_machine_invalid\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"machineController\": {\n" +
                "      \"sliders\": [\n" +
                "        {\"x\": 1, \"y\": 2, \"width\": 0, \"height\": 5000, \"key\": \"speed\", \"direction\": \"diagonal\"},\n" +
                "        {\"x\": 1, \"y\": 2, \"width\": 10, \"height\": 10}\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertNotNull(result.machineStyle);
        assertNull(result.machineStyle.sliders);
        assertTrue(containsWarning(result, "sliders[0].width must be >= 1"));
        assertTrue(containsWarning(result, "sliders[0].height must be <= 4096"));
        assertTrue(containsWarning(result, "sliders[1] is missing required fields x, y, width, height or key."));
    }

    @Test
    public void controllerStyleCopiesAndMergesSliders() {
        MachineGuiStyleManager.ControllerStyle base = new MachineGuiStyleManager.ControllerStyle();
        MachineGuiStyleManager.SliderStyle sliderA = new MachineGuiStyleManager.SliderStyle();
        sliderA.id = "a";
        sliderA.key = "speed";
        sliderA.x = 1;
        sliderA.y = 2;
        sliderA.width = 3;
        sliderA.height = 4;
        base.sliders = java.util.Collections.singletonList(sliderA);

        MachineGuiStyleManager.ControllerStyle overlay = new MachineGuiStyleManager.ControllerStyle();
        MachineGuiStyleManager.SliderStyle sliderB = new MachineGuiStyleManager.SliderStyle();
        sliderB.id = "b";
        sliderB.key = "pressure";
        sliderB.x = 5;
        sliderB.y = 6;
        sliderB.width = 7;
        sliderB.height = 8;
        overlay.sliders = java.util.Collections.singletonList(sliderB);

        MachineGuiStyleManager.ControllerStyle copy = MachineGuiStyleManager.ControllerStyle.copyOf(base);
        assertNotNull(copy.sliders);
        assertEquals(1, copy.sliders.size());
        assertEquals("a", copy.sliders.get(0).id);

        MachineGuiStyleManager.ControllerStyle merged = MachineGuiStyleManager.ControllerStyle.copyOf(base).mergeFrom(overlay);
        assertNotNull(merged.sliders);
        assertEquals(2, merged.sliders.size());
        assertEquals("a", merged.sliders.get(0).id);
        assertEquals("b", merged.sliders.get(1).id);
        assertFalse(merged.isEmpty());
    }

    @Test
    public void parseMachineJsonParsesPanelOverridesForPerMachineGui() {
        MachineGuiStyleParser.MachineFileParseResult result = MachineGuiStyleParser.parseMachineJson(
            "panel-overrides.json",
            "{\n" +
                "  \"registryname\": \"demo:water_processor\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"machineController\": {\n" +
                "      \"defaultPanelId\": \"right\",\n" +
                "      \"customPanels\": [\n" +
                "        \"main,112,8,160,116\",\n" +
                "        \"right,279,8,86,166\",\n" +
                "        \"down,112,131,160,74\"\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertEquals("demo:water_processor", result.namespacedKey);
        assertTrue(result.machineNodePresent);
        assertNotNull(result.machineStyle);
        assertEquals("right", result.machineStyle.defaultPanelId);
        assertNotNull(result.machineStyle.customPanels);
        assertEquals(3, result.machineStyle.customPanels.size());
        assertEquals("right,279,8,86,166", result.machineStyle.customPanels.get(1));
        assertFalse(result.machineStyle.isEmpty());
        assertTrue(result.warnings.isEmpty());
    }

    @Test
    public void parseMachineJsonParsesButtonsAndPages() {
        MachineGuiStyleParser.MachineFileParseResult result = MachineGuiStyleParser.parseMachineJson(
            "button-pages.json",
            "{\n" +
                "  \"registryname\": \"demo:paged_machine\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"machineController\": {\n" +
                "      \"defaultPageId\": \"main\",\n" +
                "      \"texts\": [\n" +
                "        {\"x\": 8, \"y\": 8, \"value\": \"A\", \"page\": \"main\"},\n" +
                "        {\"x\": 8, \"y\": 18, \"value\": \"B\", \"page\": \"settings\"}\n" +
                "      ],\n" +
                "      \"buttons\": [\n" +
                "        {\"x\": 140, \"y\": 8, \"label\": \">\", \"action\": \"switch_state\", \"targetState\": \"settings\"},\n" +
                "        {\"x\": 140, \"y\": 24, \"label\": \"+\", \"action\": \"data_port_add\", \"dataPortKey\": \"pulse\", \"value\": 1.0, \"state\": \"settings\"},\n" +
                "        {\"x\": 140, \"y\": 40, \"label\": \"Reset\", \"action\": \"set\", \"key\": \"pulse\", \"value\": 0.0, \"min\": 0.0, \"max\": 10.0},\n" +
                "        {\"x\": 140, \"y\": 56, \"label\": \"Run\", \"action\": \"event\", \"buttonId\": \"run_event\", \"visible\": false}\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertEquals("demo:paged_machine", result.namespacedKey);
        assertNotNull(result.machineStyle);
        assertEquals("main", result.machineStyle.defaultPageId);
        assertNotNull(result.machineStyle.texts);
        assertEquals("settings", result.machineStyle.texts.get(1).page);
        assertNotNull(result.machineStyle.buttons);
        assertEquals(4, result.machineStyle.buttons.size());
        assertEquals("page", result.machineStyle.buttons.get(0).action);
        assertEquals("settings", result.machineStyle.buttons.get(0).targetPage);
        assertEquals("smart_add", result.machineStyle.buttons.get(1).action);
        assertEquals("pulse", result.machineStyle.buttons.get(1).key);
        assertEquals("settings", result.machineStyle.buttons.get(1).page);
        assertEquals("smart_set", result.machineStyle.buttons.get(2).action);
        assertEquals(Float.valueOf(0.0F), result.machineStyle.buttons.get(2).value);
        assertEquals(Float.valueOf(0.0F), result.machineStyle.buttons.get(2).min);
        assertEquals(Float.valueOf(10.0F), result.machineStyle.buttons.get(2).max);
        assertEquals("event", result.machineStyle.buttons.get(3).action);
        assertEquals("run_event", result.machineStyle.buttons.get(3).buttonId);
        assertEquals(Boolean.FALSE, result.machineStyle.buttons.get(3).visible);
        assertTrue(result.warnings.isEmpty());
    }

    @Test
    public void parseMachineJsonParsesButtonHotkeysAndGuiOnlyHotkeys() {
        MachineGuiStyleParser.MachineFileParseResult result = MachineGuiStyleParser.parseMachineJson(
            "button-hotkeys.json",
            "{\n" +
                "  \"registryname\": \"demo:hotkey_machine\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"machineController\": {\n" +
                "      \"buttons\": [\n" +
                "        {\"x\": 140, \"y\": 8, \"label\": \"Run\", \"action\": \"event\", \"buttonId\": \"run_event\", \"hotkey\": \"C\"}\n" +
                "      ],\n" +
                "      \"hotkeys\": [\n" +
                "        {\"key\": \"ctrl+r\", \"action\": \"event\", \"buttonId\": \"reset_event\", \"page\": \"settings\"},\n" +
                "        {\"hotkeys\": [\"G\", \"shift+G\"], \"action\": \"smart_add\", \"dataPortKey\": \"gear\", \"value\": 1.0, \"consumeHotkey\": false}\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertNotNull(result.machineStyle);
        assertNotNull(result.machineStyle.buttons);
        assertEquals(3, result.machineStyle.buttons.size());
        assertEquals("event", result.machineStyle.buttons.get(0).action);
        assertEquals("C", result.machineStyle.buttons.get(0).hotkeys.get(0));
        assertEquals("event", result.machineStyle.buttons.get(1).action);
        assertEquals("reset_event", result.machineStyle.buttons.get(1).buttonId);
        assertEquals(Boolean.FALSE, result.machineStyle.buttons.get(1).visible);
        assertEquals("ctrl+r", result.machineStyle.buttons.get(1).hotkeys.get(0));
        assertEquals("settings", result.machineStyle.buttons.get(1).page);
        assertEquals("smart_add", result.machineStyle.buttons.get(2).action);
        assertEquals("gear", result.machineStyle.buttons.get(2).key);
        assertEquals(2, result.machineStyle.buttons.get(2).hotkeys.size());
        assertEquals(Boolean.FALSE, result.machineStyle.buttons.get(2).consumeHotkey);
        assertTrue(result.warnings.isEmpty());
    }

    @Test
    public void parseMachineJsonSupportsSmartSetStringValuesAndNumericAdds() {
        MachineGuiStyleParser.MachineFileParseResult result = MachineGuiStyleParser.parseMachineJson(
            "smart-string-values.json",
            "{\n" +
                "  \"registryname\": \"demo:smart_values_machine\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"machineController\": {\n" +
                "      \"buttons\": [\n" +
                "        {\"x\": 12, \"y\": 8, \"label\": \"Set Text\", \"action\": \"smart_set\", \"key\": \"status\", \"value\": \"ready\"},\n" +
                "        {\"x\": 12, \"y\": 24, \"label\": \"+1\", \"action\": \"smart_add\", \"key\": \"count\", \"value\": 1.5}\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertNotNull(result.machineStyle);
        assertNotNull(result.machineStyle.buttons);
        assertEquals(2, result.machineStyle.buttons.size());
        assertEquals("smart_set", result.machineStyle.buttons.get(0).action);
        assertEquals("status", result.machineStyle.buttons.get(0).key);
        assertNull(result.machineStyle.buttons.get(0).value);
        assertEquals("ready", result.machineStyle.buttons.get(0).stringValue);
        assertEquals("smart_add", result.machineStyle.buttons.get(1).action);
        assertEquals("count", result.machineStyle.buttons.get(1).key);
        assertEquals(Float.valueOf(1.5F), result.machineStyle.buttons.get(1).value);
        assertNull(result.machineStyle.buttons.get(1).stringValue);
        assertTrue(result.warnings.isEmpty());
    }

    @Test
    public void parseMachineJsonSupportsSmartSetStringValueAliasesAndEventButtons() {
        MachineGuiStyleParser.MachineFileParseResult result = MachineGuiStyleParser.parseMachineJson(
            "smart-string-value-aliases.json",
            "{\n" +
                "  \"registryname\": \"demo:smart_value_aliases_machine\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"machineController\": {\n" +
                "      \"buttons\": [\n" +
                "        {\"x\": 12, \"y\": 8, \"label\": \"Set Text\", \"action\": \"smart_set\", \"key\": \"status\", \"stringValue\": \"ready\"},\n" +
                "        {\"x\": 12, \"y\": 24, \"label\": \"Ping\", \"action\": \"event\", \"buttonId\": \"evt_ping\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertNotNull(result.machineStyle);
        assertNotNull(result.machineStyle.buttons);
        assertEquals(2, result.machineStyle.buttons.size());
        assertEquals("smart_set", result.machineStyle.buttons.get(0).action);
        assertEquals("status", result.machineStyle.buttons.get(0).key);
        assertEquals("ready", result.machineStyle.buttons.get(0).stringValue);
        assertEquals("event", result.machineStyle.buttons.get(1).action);
        assertEquals("evt_ping", result.machineStyle.buttons.get(1).buttonId);
        assertTrue(result.warnings.isEmpty());
    }

    @Test
    public void parseMachineJsonSupportsNetworksBButtonFieldCombination() {
        MachineGuiStyleParser.MachineFileParseResult result = MachineGuiStyleParser.parseMachineJson(
            "networks-b-buttons.json",
            "{\n" +
                "  \"registryname\": \"networks-B\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"factoryController\": {\n" +
                "      \"buttons\": [\n" +
                "        {\"id\": \"open_modal_settings\", \"x\": 8, \"y\": 138, \"label\": \"Modal\", \"action\": \"subgui\", \"targetSubGui\": \"settings_modal\", \"openMode\": \"modal\"},\n" +
                "        {\"id\": \"event_ping\", \"buttonId\": \"event_ping\", \"x\": 128, \"y\": 138, \"label\": \"Evt+1\", \"action\": \"event\"},\n" +
                "        {\"id\": \"smart_count_add\", \"x\": 8, \"y\": 162, \"label\": \"Count+1\", \"action\": \"smart_add\", \"key\": \"test_count\", \"value\": 1},\n" +
                "        {\"id\": \"smart_text_ready\", \"x\": 68, \"y\": 162, \"label\": \"Set Text\", \"action\": \"smart_set\", \"key\": \"test_status\", \"value\": \"ready\"},\n" +
                "        {\"id\": \"modal_speed_plus\", \"x\": 40, \"y\": 124, \"label\": \"+1\", \"action\": \"data_port_add\", \"dataPortKey\": \"speed\", \"value\": 1.0, \"min\": 0.0, \"max\": 10.0}\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertNotNull(result.factoryStyle);
        assertNotNull(result.factoryStyle.buttons);
        assertEquals(5, result.factoryStyle.buttons.size());
        assertEquals("subgui", result.factoryStyle.buttons.get(0).action);
        assertEquals("settings_modal", result.factoryStyle.buttons.get(0).targetSubGui);
        assertEquals("modal", result.factoryStyle.buttons.get(0).openMode);
        assertEquals("event", result.factoryStyle.buttons.get(1).action);
        assertEquals("event_ping", result.factoryStyle.buttons.get(1).buttonId);
        assertEquals("smart_add", result.factoryStyle.buttons.get(2).action);
        assertEquals("test_count", result.factoryStyle.buttons.get(2).key);
        assertEquals(Float.valueOf(1.0F), result.factoryStyle.buttons.get(2).value);
        assertEquals("smart_set", result.factoryStyle.buttons.get(3).action);
        assertEquals("test_status", result.factoryStyle.buttons.get(3).key);
        assertEquals("ready", result.factoryStyle.buttons.get(3).stringValue);
        assertEquals("smart_add", result.factoryStyle.buttons.get(4).action);
        assertEquals("speed", result.factoryStyle.buttons.get(4).key);
        assertEquals(Float.valueOf(0.0F), result.factoryStyle.buttons.get(4).min);
        assertEquals(Float.valueOf(10.0F), result.factoryStyle.buttons.get(4).max);
        assertTrue(result.warnings.isEmpty());
    }

    @Test
    public void parseMachineJsonRejectsSmartAddStringValuesAndEmptySmartSetValues() {
        MachineGuiStyleParser.MachineFileParseResult result = MachineGuiStyleParser.parseMachineJson(
            "smart-invalid-values.json",
            "{\n" +
                "  \"registryname\": \"demo:smart_invalid_values_machine\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"machineController\": {\n" +
                "      \"buttons\": [\n" +
                "        {\"x\": 12, \"y\": 8, \"label\": \"Bad Add\", \"action\": \"smart_add\", \"key\": \"count\", \"value\": \"oops\"},\n" +
                "        {\"x\": 12, \"y\": 24, \"label\": \"Bad Set\", \"action\": \"smart_set\", \"key\": \"status\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertNotNull(result.machineStyle);
        assertNull(result.machineStyle.buttons);
        assertTrue(containsWarning(result, "smart_add action requires numeric value."));
        assertTrue(containsWarning(result, "smart_set action requires value."));
    }

    @Test
    public void parseSubGuiJsonCanBeMergedWithBaseStyleWithoutBreakingPageTargets() {
        MachineGuiStyleParser.MachineFileParseResult base = MachineGuiStyleParser.parseMachineJson(
            "base.json",
            "{\n" +
                "  \"registryname\": \"demo:dual_source_machine\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"machineController\": {\n" +
                "      \"defaultPageId\": \"main\",\n" +
                "      \"buttons\": [\n" +
                "        {\"x\": 8, \"y\": 8, \"label\": \"Go\", \"action\": \"page\", \"targetPage\": \"settings\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );
        MachineGuiStyleParser.MachineFileParseResult sub = SubGuiConfigLoader.parseSubGuiJson(
            "subgui.json",
            "{\n" +
                "  \"registryname\": \"demo:dual_source_machine\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"machineController\": {\n" +
                "      \"texts\": [\n" +
                "        {\"x\": 12, \"y\": 12, \"value\": \"Sub page\", \"page\": \"settings\"}\n" +
                "      ],\n" +
                "      \"buttons\": [\n" +
                "        {\"x\": 28, \"y\": 8, \"label\": \"Back\", \"action\": \"page\", \"targetPage\": \"main\", \"page\": \"settings\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        MachineGuiStyleManager.ControllerStyle merged = MachineGuiStyleManager.ControllerStyle
            .copyOf(base.machineStyle)
            .mergeFrom(sub.machineStyle);

        assertEquals("main", merged.defaultPageId);
        assertNotNull(merged.texts);
        assertEquals(1, merged.texts.size());
        assertEquals("settings", merged.texts.get(0).page);
        assertNotNull(merged.buttons);
        assertEquals(2, merged.buttons.size());
        assertEquals("page", merged.buttons.get(0).action);
        assertEquals("settings", merged.buttons.get(0).targetPage);
        assertEquals("main", merged.buttons.get(1).targetPage);
        assertEquals("settings", merged.buttons.get(1).page);
        assertTrue(base.warnings.isEmpty());
        assertTrue(sub.warnings.isEmpty());
    }

    @Test
    public void multipleSubGuiJsonOverlaysAppendInsteadOfReplacingExistingEntries() {
        MachineGuiStyleParser.MachineFileParseResult base = MachineGuiStyleParser.parseMachineJson(
            "base.json",
            "{\n" +
                "  \"registryname\": \"demo:multi_subgui_machine\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"factoryController\": {\n" +
                "      \"defaultPageId\": \"main\",\n" +
                "      \"buttons\": [\n" +
                "        {\"x\": 8, \"y\": 8, \"label\": \"A\", \"action\": \"page\", \"targetPage\": \"a\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );
        MachineGuiStyleParser.MachineFileParseResult subA = SubGuiConfigLoader.parseSubGuiJson(
            "sub-a.json",
            "{\n" +
                "  \"registryname\": \"demo:multi_subgui_machine\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"factoryController\": {\n" +
                "      \"texts\": [\n" +
                "        {\"x\": 8, \"y\": 20, \"value\": \"Sub A\", \"page\": \"a\"}\n" +
                "      ],\n" +
                "      \"buttons\": [\n" +
                "        {\"x\": 8, \"y\": 32, \"label\": \"B\", \"action\": \"page\", \"targetPage\": \"b\", \"page\": \"a\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );
        MachineGuiStyleParser.MachineFileParseResult subB = SubGuiConfigLoader.parseSubGuiJson(
            "sub-b.json",
            "{\n" +
                "  \"registryname\": \"demo:multi_subgui_machine\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"factoryController\": {\n" +
                "      \"texts\": [\n" +
                "        {\"x\": 8, \"y\": 20, \"value\": \"Sub B\", \"page\": \"b\"}\n" +
                "      ],\n" +
                "      \"buttons\": [\n" +
                "        {\"x\": 8, \"y\": 32, \"label\": \"Main\", \"action\": \"page\", \"targetPage\": \"main\", \"page\": \"b\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        MachineGuiStyleManager.ControllerStyle merged = MachineGuiStyleManager.ControllerStyle
            .copyOf(base.factoryStyle)
            .mergeFrom(subA.factoryStyle)
            .mergeFrom(subB.factoryStyle);

        assertEquals("main", merged.defaultPageId);
        assertNotNull(merged.texts);
        assertEquals(2, merged.texts.size());
        assertEquals("a", merged.texts.get(0).page);
        assertEquals("b", merged.texts.get(1).page);
        assertNotNull(merged.buttons);
        assertEquals(3, merged.buttons.size());
        assertEquals("a", merged.buttons.get(0).targetPage);
        assertEquals("b", merged.buttons.get(1).targetPage);
        assertEquals("main", merged.buttons.get(2).targetPage);
        assertTrue(base.warnings.isEmpty());
        assertTrue(subA.warnings.isEmpty());
        assertTrue(subB.warnings.isEmpty());
    }

    @Test
    public void parseMachineJsonParsesEmbeddedSubGuisAndButtonTargets() {
        MachineGuiStyleParser.MachineFileParseResult result = MachineGuiStyleParser.parseMachineJson(
            "embedded-subguis.json",
            "{\n" +
                "  \"registryname\": \"demo:controller_with_subgui\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"machineController\": {\n" +
                "      \"buttons\": [\n" +
                "        {\"x\": 8, \"y\": 8, \"label\": \"Open\", \"action\": \"subgui\", \"targetSubGui\": \"details\", \"openMode\": \"modal\"},\n" +
                "        {\"x\": 8, \"y\": 24, \"label\": \"Close\", \"action\": \"close_subgui\"}\n" +
                "      ],\n" +
                "      \"subGuis\": [\n" +
                "        {\n" +
                "          \"id\": \"details\",\n" +
                "          \"mode\": \"replace\",\n" +
                "          \"x\": 12,\n" +
                "          \"y\": 16,\n" +
                "          \"draggable\": true,\n" +
                "          \"dragHandle\": true,\n" +
                "          \"dragX\": 2,\n" +
                "          \"dragY\": 3,\n" +
                "          \"dragWidth\": 116,\n" +
                "          \"dragHeight\": 12,\n" +
                "          \"width\": 120,\n" +
                "          \"height\": 80,\n" +
                "          \"backgroundTexture\": \"demo:textures/gui/sub.png\",\n" +
                "          \"defaultPageId\": \"main\",\n" +
                "          \"texts\": [\n" +
                "            {\"x\": 4, \"y\": 4, \"value\": \"Sub\"}\n" +
                "          ],\n" +
                "          \"buttons\": [\n" +
                "            {\"x\": 90, \"y\": 4, \"label\": \"X\", \"action\": \"close_subgui\"}\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertNotNull(result.machineStyle);
        assertNotNull(result.machineStyle.buttons);
        assertEquals("subgui", result.machineStyle.buttons.get(0).action);
        assertEquals("details", result.machineStyle.buttons.get(0).targetSubGui);
        assertEquals("modal", result.machineStyle.buttons.get(0).openMode);
        assertEquals("close_subgui", result.machineStyle.buttons.get(1).action);
        assertNotNull(result.machineStyle.subGuis);
        assertEquals(1, result.machineStyle.subGuis.size());
        MachineGuiStyleManager.SubGuiStyle subGui = result.machineStyle.subGuis.get(0);
        assertEquals("details", subGui.id);
        assertEquals("replace", subGui.mode);
        assertEquals(Integer.valueOf(12), subGui.x);
        assertEquals(Integer.valueOf(16), subGui.y);
        assertEquals(Boolean.TRUE, subGui.draggable);
        assertEquals(Boolean.TRUE, subGui.dragHandle);
        assertEquals(Integer.valueOf(2), subGui.dragX);
        assertEquals(Integer.valueOf(3), subGui.dragY);
        assertEquals(Integer.valueOf(116), subGui.dragWidth);
        assertEquals(Integer.valueOf(12), subGui.dragHeight);
        assertEquals(Integer.valueOf(120), subGui.width);
        assertEquals(Integer.valueOf(80), subGui.height);
        assertNotNull(subGui.style);
        assertEquals("demo:textures/gui/sub.png", subGui.style.backgroundTexture);
        assertEquals(Integer.valueOf(120), subGui.style.guiWidth);
        assertEquals(Integer.valueOf(80), subGui.style.guiHeight);
        assertEquals("main", subGui.style.defaultPageId);
        assertNotNull(subGui.style.texts);
        assertEquals("Sub", subGui.style.texts.get(0).value);
        assertNotNull(subGui.style.buttons);
        assertEquals("close_subgui", subGui.style.buttons.get(0).action);
        assertTrue(result.warnings.isEmpty());
    }

    @Test
    public void parseStandaloneSubGuiJsonMergesWithBaseControllerStyle() {
        MachineGuiStyleParser.MachineFileParseResult base = MachineGuiStyleParser.parseMachineJson(
            "base-controller.json",
            "{\n" +
                "  \"registryname\": \"demo:standalone_subgui_machine\",\n" +
                "  \"mmce_gui_ext\": {\n" +
                "    \"factoryController\": {\n" +
                "      \"buttons\": [\n" +
                "        {\"x\": 6, \"y\": 6, \"label\": \"Cfg\", \"action\": \"subgui\", \"targetSubGui\": \"cfg\", \"openMode\": \"replace\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );
        MachineGuiStyleParser.MachineFileParseResult sub = SubGuiConfigLoader.parseSubGuiJson(
            "standalone-subgui.json",
            "{\n" +
                "  \"registryname\": \"demo:standalone_subgui_machine\",\n" +
                "  \"controller\": \"factory\",\n" +
                "  \"id\": \"cfg\",\n" +
                "  \"mode\": \"modal\",\n" +
                "  \"x\": 18,\n" +
                "  \"y\": 20,\n" +
                "  \"backgroundTexture\": \"demo:textures/gui/cfg.png\",\n" +
                "  \"defaultPageId\": \"cfg_main\",\n" +
                "  \"texts\": [\n" +
                "    {\"x\": 5, \"y\": 5, \"value\": \"Config\"}\n" +
                "  ],\n" +
                "  \"buttons\": [\n" +
                "    {\"x\": 90, \"y\": 5, \"label\": \"Done\", \"action\": \"close_subgui\"}\n" +
                "  ]\n" +
                "}"
        );

        MachineGuiStyleManager.ControllerStyle merged = MachineGuiStyleManager.ControllerStyle
            .copyOf(base.factoryStyle)
            .mergeFrom(sub.factoryStyle);

        assertEquals("demo:standalone_subgui_machine", sub.namespacedKey);
        assertTrue(sub.factoryNodePresent);
        assertNotNull(merged.buttons);
        assertEquals(1, merged.buttons.size());
        assertNotNull(merged.subGuis);
        assertEquals(1, merged.subGuis.size());
        MachineGuiStyleManager.SubGuiStyle subGui = merged.subGuis.get(0);
        assertEquals("cfg", subGui.id);
        assertEquals("modal", subGui.mode);
        assertEquals(Integer.valueOf(18), subGui.x);
        assertEquals(Integer.valueOf(20), subGui.y);
        assertNotNull(subGui.style);
        assertEquals("demo:textures/gui/cfg.png", subGui.style.backgroundTexture);
        assertEquals("cfg_main", subGui.style.defaultPageId);
        assertNotNull(subGui.style.buttons);
        assertEquals("close_subgui", subGui.style.buttons.get(0).action);
        assertTrue(base.warnings.isEmpty());
        assertTrue(sub.warnings.isEmpty());
    }

    private static boolean containsWarning(MachineGuiStyleParser.MachineFileParseResult result, String fragment) {
        for (String warning : result.warnings) {
            if (warning.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
