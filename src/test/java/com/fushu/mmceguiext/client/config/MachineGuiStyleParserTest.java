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
                "        {\"texture\": \"demo:textures/gui/layer.png\", \"width\": 0}\n" +
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
                "      \"smartInterfaceEditors\": [\n" +
                "        {\"x\": 30, \"y\": 40, \"virtualKey\": \"factory_key\", \"showTitle\": false}\n" +
                "      ],\n" +
                "      \"backgroundLayers\": [\n" +
                "        {\"id\": \"bg\", \"texture\": \"demo:textures/gui/bg_layer.png\", \"texW\": 280, \"texH\": 213}\n" +
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
        assertNotNull(result.factoryStyle.smartInterfaceEditors);
        assertEquals(1, result.factoryStyle.smartInterfaceEditors.size());
        assertEquals(Boolean.FALSE, result.factoryStyle.smartInterfaceEditors.get(0).showTitle);
        assertNotNull(result.factoryStyle.textureLayers);
        assertEquals(1, result.factoryStyle.textureLayers.size());
        assertNull(result.factoryStyle.textureLayers.get(0).foreground);
        assertTrue(result.warnings.isEmpty());
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
                "        {\"x\": 140, \"y\": 8, \"label\": \">\", \"action\": \"page\", \"targetPage\": \"settings\"},\n" +
                "        {\"x\": 140, \"y\": 24, \"label\": \"+\", \"action\": \"add\", \"key\": \"pulse\", \"value\": 1.0, \"page\": \"settings\"}\n" +
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
        assertEquals(2, result.machineStyle.buttons.size());
        assertEquals("page", result.machineStyle.buttons.get(0).action);
        assertEquals("settings", result.machineStyle.buttons.get(0).targetPage);
        assertEquals("smart_add", result.machineStyle.buttons.get(1).action);
        assertEquals("pulse", result.machineStyle.buttons.get(1).key);
        assertEquals("settings", result.machineStyle.buttons.get(1).page);
        assertTrue(result.warnings.isEmpty());
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
