package com.fushu.mmceguiext.common.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ControllerButtonPolicyManagerTest {
    @Test
    public void parseButtonAcceptsStringSmartSetWithoutNumericUnboxing() throws Exception {
        ControllerButtonPolicyManager.ButtonPolicy policy = parseButton(
            "{ \"action\": \"smart_set\", \"key\": \"status\", \"value\": \"ready\" }"
        );

        assertNotNull(policy);
        assertEquals("smart_set", policy.action);
        assertEquals("status", policy.key);
        assertNull(policy.value);
        assertEquals("ready", policy.stringValue);
    }

    @Test
    public void parseButtonAcceptsNumericSmartSetAndSmartAdd() throws Exception {
        ControllerButtonPolicyManager.ButtonPolicy setPolicy = parseButton(
            "{ \"action\": \"smart_set\", \"key\": \"count\", \"value\": 0, \"min\": 0, \"max\": 10 }"
        );
        ControllerButtonPolicyManager.ButtonPolicy addPolicy = parseButton(
            "{ \"action\": \"smart_add\", \"key\": \"count\", \"value\": 1 }"
        );

        assertNotNull(setPolicy);
        assertEquals(Float.valueOf(0.0F), setPolicy.value);
        assertNull(setPolicy.stringValue);
        assertEquals(Float.valueOf(0.0F), setPolicy.min);
        assertEquals(Float.valueOf(10.0F), setPolicy.max);

        assertNotNull(addPolicy);
        assertEquals("smart_add", addPolicy.action);
        assertEquals("count", addPolicy.key);
        assertEquals(Float.valueOf(1.0F), addPolicy.value);
    }

    @Test
    public void parseButtonAcceptsEventButtonsWithoutSmartValue() throws Exception {
        ControllerButtonPolicyManager.ButtonPolicy policy = parseButton(
            "{ \"action\": \"event\", \"buttonId\": \"evt_plus_one\" }"
        );

        assertNotNull(policy);
        assertEquals("event", policy.action);
        assertEquals("evt_plus_one", policy.buttonId);
        assertNull(policy.value);
        assertNull(policy.stringValue);
    }

    @Test
    public void parseButtonsCollectsSubGuiDataPortAliases() throws Exception {
        List<ControllerButtonPolicyManager.ButtonPolicy> policies = parseButtons(
            "{\n" +
                "  \"subGuis\": [\n" +
                "    {\n" +
                "      \"buttons\": [\n" +
                "        {\"id\": \"modal_speed_plus\", \"action\": \"data_port_add\", \"dataPortKey\": \"speed\", \"value\": 1.0, \"min\": 0.0, \"max\": 10.0},\n" +
                "        {\"id\": \"modal_speed_reset\", \"action\": \"data_port_set\", \"dataPortKey\": \"speed\", \"value\": 0.0}\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}"
        );

        assertEquals(2, policies.size());
        assertEquals("smart_add", policies.get(0).action);
        assertEquals("speed", policies.get(0).key);
        assertEquals(Float.valueOf(1.0F), policies.get(0).value);
        assertEquals(Float.valueOf(0.0F), policies.get(0).min);
        assertEquals(Float.valueOf(10.0F), policies.get(0).max);
        assertEquals("smart_set", policies.get(1).action);
        assertEquals("speed", policies.get(1).key);
        assertEquals(Float.valueOf(0.0F), policies.get(1).value);
    }

    @Test
    public void parseEditorKeysCollectsSliderKeys() throws Exception {
        List<String> keys = parseEditorKeys(
            "{\n" +
                "  \"sliders\": [\n" +
                "    {\"key\": \"speed\"},\n" +
                "    {\"dataPortKey\": \"pressure\"}\n" +
                "  ],\n" +
                "  \"subGuis\": [\n" +
                "    {\"gui_sliders\": [{\"virtualKey\": \"sub_speed\"}]}\n" +
                "  ]\n" +
                "}"
        );

        assertEquals(3, keys.size());
        assertEquals("speed", keys.get(0));
        assertEquals("pressure", keys.get(1));
        assertEquals("sub_speed", keys.get(2));
    }

    private static ControllerButtonPolicyManager.ButtonPolicy parseButton(String json) throws Exception {
        Method method = ControllerButtonPolicyManager.class.getDeclaredMethod("parseButton", JsonObject.class);
        method.setAccessible(true);
        return (ControllerButtonPolicyManager.ButtonPolicy) method.invoke(
            null,
            new JsonParser().parse(json).getAsJsonObject()
        );
    }

    @SuppressWarnings("unchecked")
    private static List<ControllerButtonPolicyManager.ButtonPolicy> parseButtons(String json) throws Exception {
        Method method = ControllerButtonPolicyManager.class.getDeclaredMethod("parseButtons", JsonObject.class);
        method.setAccessible(true);
        return (List<ControllerButtonPolicyManager.ButtonPolicy>) method.invoke(
            null,
            new JsonParser().parse(json).getAsJsonObject()
        );
    }

    @SuppressWarnings("unchecked")
    private static List<String> parseEditorKeys(String json) throws Exception {
        Method method = ControllerButtonPolicyManager.class.getDeclaredMethod("parseEditorKeys", JsonObject.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(
            null,
            new JsonParser().parse(json).getAsJsonObject()
        );
    }
}
