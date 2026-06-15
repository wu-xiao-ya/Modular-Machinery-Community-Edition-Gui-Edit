package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class GuiFactoryControllerRuntimeStateTest {
    @Test
    public void runtimeStateRestoresParentCollectionsWithoutSubGuiPollution() throws Exception {
        GuiFactoryControllerResizable gui = allocateGui();
        List<Object> parentButtons = new ArrayList<Object>();
        parentButtons.add(new Object());
        Map<String, String> parentVirtualCache = new HashMap<String, String>();
        parentVirtualCache.put("status", "ready");
        Map<String, Integer> parentScroll = new HashMap<String, Integer>();
        parentScroll.put("main", Integer.valueOf(2));

        set(gui, "customButtons", parentButtons);
        set(gui, "customSmartEditors", new ArrayList<Object>());
        set(gui, "backgroundTextureLayers", new ArrayList<Object>());
        set(gui, "foregroundTextureLayers", new ArrayList<Object>());
        set(gui, "layerRuntimeStates", new HashMap<Object, Object>());
        set(gui, "textureLayerIds", new java.util.HashSet<Object>());
        set(gui, "subGuiStyleIndex", new HashMap<Object, Object>());
        set(gui, "styleOverride", MachineGuiStyleManager.ControllerStyle.EMPTY);
        set(gui, "smartInterfaceVirtualInputCache", parentVirtualCache);
        set(gui, "panelScroll", parentScroll);
        set(gui, "panelMaxScroll", new HashMap<String, Integer>());
        set(gui, "activePageId", "main");

        Object parentState = invoke(gui, "captureCurrentRuntimeState");

        parentButtons.clear();
        parentButtons.add(new Object());
        parentButtons.add(new Object());
        parentVirtualCache.put("status", "subgui");
        parentScroll.put("main", Integer.valueOf(99));
        set(gui, "activePageId", "settings");

        invoke(gui, "applyRuntimeState", parentState);

        List<?> restoredButtons = (List<?>) get(gui, "customButtons");
        Map<?, ?> restoredVirtualCache = (Map<?, ?>) get(gui, "smartInterfaceVirtualInputCache");
        Map<?, ?> restoredScroll = (Map<?, ?>) get(gui, "panelScroll");

        assertEquals(1, restoredButtons.size());
        assertEquals("ready", restoredVirtualCache.get("status"));
        assertEquals(Integer.valueOf(2), restoredScroll.get("main"));
        assertEquals("main", get(gui, "activePageId"));
        assertNotSame(parentButtons, restoredButtons);
        assertNotSame(parentVirtualCache, restoredVirtualCache);
        assertNotSame(parentScroll, restoredScroll);
    }

    private static GuiFactoryControllerResizable allocateGui() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        return (GuiFactoryControllerResizable) unsafe.allocateInstance(GuiFactoryControllerResizable.class);
    }

    private static Object invoke(GuiFactoryControllerResizable target, String methodName, Object... args) throws Exception {
        Method method;
        if (args.length == 0) {
            method = GuiFactoryControllerResizable.class.getDeclaredMethod(methodName);
        } else {
            method = GuiFactoryControllerResizable.class.getDeclaredMethod(methodName, args[0].getClass());
        }
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static void set(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object get(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
