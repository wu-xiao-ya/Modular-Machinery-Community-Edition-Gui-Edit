package com.fushu.mmceguiext.client.gui;

import sun.misc.Unsafe;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GuiMachineControllerResizableTest {
    @Test
    public void topmostSliderPrefersForegroundThenPriorityThenLatestEntry() throws Exception {
        GuiMachineControllerResizable gui = allocateGui();
        Object background = newSlider("background", false, 100, 10, 10, 80, 12);
        Object foregroundLow = newSlider("foregroundLow", true, 0, 10, 10, 80, 12);
        Object foregroundHigh = newSlider("foregroundHigh", true, 7, 10, 10, 80, 12);
        Object foregroundLater = newSlider("foregroundLater", true, 7, 10, 10, 80, 12);

        List<Object> sliders = new ArrayList<Object>();
        sliders.add(background);
        sliders.add(foregroundLow);
        sliders.add(foregroundHigh);
        sliders.add(foregroundLater);
        set(gui, "customSliders", sliders);
        set(gui, "activePageId", "main");

        Object hit = invoke(gui, "findTopmostSliderAt", new Class<?>[] {int.class, int.class}, Integer.valueOf(20), Integer.valueOf(15));
        assertSame(foregroundLater, hit);
    }

    @Test
    public void modalSliderReleaseClearsDraggingStateOnRuntimeSnapshot() throws Exception {
        GuiMachineControllerResizable gui = allocateGui();
        Object dragged = newSlider("dragged", true, 5, 10, 10, 80, 12);
        Object runtime = newRuntimeState();
        set(runtime, "draggingSlider", dragged);
        set(gui, "modalSubGuiStack", new ArrayList<Object>(Collections.singletonList(runtime)));
        set(gui, "currentRuntimeState", newRuntimeState());
        set(gui, "draggingSlider", dragged);

        invoke(gui, "handleTopModalMouseReleased", new Class<?>[] {int.class, int.class, int.class}, Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0));

        assertNull(get(runtime, "draggingSlider"));
        assertNull(get(gui, "draggingSlider"));
    }

    @Test
    public void hotkeyMatcherAcceptsNamedKeysAndExactModifiers() throws Exception {
        assertEquals(Boolean.TRUE, invokeStatic(
            "matchesHotkey",
            new Class<?>[] {String.class, char.class, int.class, boolean.class, boolean.class, boolean.class},
            "C",
            Character.valueOf('c'),
            Integer.valueOf(0),
            Boolean.FALSE,
            Boolean.FALSE,
            Boolean.FALSE
        ));
        assertEquals(Boolean.TRUE, invokeStatic(
            "matchesHotkey",
            new Class<?>[] {String.class, char.class, int.class, boolean.class, boolean.class, boolean.class},
            "ctrl+KEY_C",
            Character.valueOf('c'),
            Integer.valueOf(0),
            Boolean.FALSE,
            Boolean.TRUE,
            Boolean.FALSE
        ));
        assertEquals(Boolean.FALSE, invokeStatic(
            "matchesHotkey",
            new Class<?>[] {String.class, char.class, int.class, boolean.class, boolean.class, boolean.class},
            "ctrl+KEY_C",
            Character.valueOf('c'),
            Integer.valueOf(0),
            Boolean.FALSE,
            Boolean.FALSE,
            Boolean.FALSE
        ));
    }

    private static GuiMachineControllerResizable allocateGui() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        GuiMachineControllerResizable gui = (GuiMachineControllerResizable) unsafe.allocateInstance(GuiMachineControllerResizable.class);
        set(gui, "customSliders", new ArrayList<Object>());
        set(gui, "customButtons", new ArrayList<Object>());
        set(gui, "customSmartEditors", new ArrayList<Object>());
        set(gui, "backgroundTextureLayers", new ArrayList<Object>());
        set(gui, "foregroundTextureLayers", new ArrayList<Object>());
        set(gui, "layerRuntimeStates", new java.util.HashMap<Object, Object>());
        set(gui, "textureLayerIds", new java.util.HashSet<Object>());
        set(gui, "panelScroll", new java.util.HashMap<Object, Object>());
        set(gui, "panelMaxScroll", new java.util.HashMap<Object, Object>());
        set(gui, "smartInterfaceVirtualInputCache", new java.util.HashMap<Object, Object>());
        set(gui, "modalSubGuiStack", new ArrayList<Object>());
        set(gui, "replaceSubGuiStack", new ArrayList<Object>());
        return gui;
    }

    private static Object newSlider(String id, boolean foreground, int priority, int x, int y, int width, int height) throws Exception {
        Class<?> sliderClass = Class.forName("com.fushu.mmceguiext.client.gui.GuiMachineControllerResizable$CustomSlider");
        Constructor<?> ctor = sliderClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object slider = ctor.newInstance();
        set(slider, "id", id);
        set(slider, "key", id + "_key");
        set(slider, "foreground", Boolean.valueOf(foreground));
        set(slider, "priority", Integer.valueOf(priority));
        set(slider, "x", Integer.valueOf(x));
        set(slider, "y", Integer.valueOf(y));
        set(slider, "width", Integer.valueOf(width));
        set(slider, "height", Integer.valueOf(height));
        set(slider, "min", Float.valueOf(0.0F));
        set(slider, "max", Float.valueOf(10.0F));
        set(slider, "step", Float.valueOf(0.0F));
        set(slider, "value", Float.valueOf(5.0F));
        set(slider, "visible", Boolean.TRUE);
        set(slider, "page", "main");
        return slider;
    }

    private static Object newRuntimeState() throws Exception {
        Class<?> stateClass = Class.forName("com.fushu.mmceguiext.client.gui.GuiMachineControllerResizable$RuntimeGuiState");
        Constructor<?> ctor = stateClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Object invokeStatic(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = GuiMachineControllerResizable.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    private static void set(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object get(Object target, String fieldName) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
