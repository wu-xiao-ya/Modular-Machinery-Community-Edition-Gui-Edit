package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import net.minecraft.client.gui.GuiButton;
import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
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
        set(gui, "customSliders", new ArrayList<Object>());
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

    @Test
    public void runtimeStateRestoresModalDragState() throws Exception {
        GuiFactoryControllerResizable gui = allocateGuiWithRuntimeDefaults();
        set(gui, "modalSubGuiDraggable", Boolean.TRUE);
        set(gui, "modalSubGuiDragHandle", Boolean.TRUE);
        set(gui, "modalSubGuiDragX", Integer.valueOf(4));
        set(gui, "modalSubGuiDragY", Integer.valueOf(5));
        set(gui, "modalSubGuiDragWidth", Integer.valueOf(120));
        set(gui, "modalSubGuiDragHeight", Integer.valueOf(14));
        set(gui, "draggingModalSubGui", Boolean.TRUE);
        set(gui, "modalSubGuiDragOffsetX", Integer.valueOf(33));
        set(gui, "modalSubGuiDragOffsetY", Integer.valueOf(44));

        Object dragState = invoke(gui, "captureCurrentRuntimeState");

        set(gui, "modalSubGuiDraggable", Boolean.FALSE);
        set(gui, "modalSubGuiDragHandle", Boolean.FALSE);
        set(gui, "modalSubGuiDragX", Integer.valueOf(0));
        set(gui, "modalSubGuiDragY", Integer.valueOf(0));
        set(gui, "modalSubGuiDragWidth", Integer.valueOf(0));
        set(gui, "modalSubGuiDragHeight", Integer.valueOf(0));
        set(gui, "draggingModalSubGui", Boolean.FALSE);
        set(gui, "modalSubGuiDragOffsetX", Integer.valueOf(0));
        set(gui, "modalSubGuiDragOffsetY", Integer.valueOf(0));

        invoke(gui, "applyRuntimeState", dragState);

        assertEquals(Boolean.TRUE, get(gui, "modalSubGuiDraggable"));
        assertEquals(Boolean.TRUE, get(gui, "modalSubGuiDragHandle"));
        assertEquals(Integer.valueOf(4), get(gui, "modalSubGuiDragX"));
        assertEquals(Integer.valueOf(5), get(gui, "modalSubGuiDragY"));
        assertEquals(Integer.valueOf(120), get(gui, "modalSubGuiDragWidth"));
        assertEquals(Integer.valueOf(14), get(gui, "modalSubGuiDragHeight"));
        assertEquals(Boolean.TRUE, get(gui, "draggingModalSubGui"));
        assertEquals(Integer.valueOf(33), get(gui, "modalSubGuiDragOffsetX"));
        assertEquals(Integer.valueOf(44), get(gui, "modalSubGuiDragOffsetY"));
    }

    @Test
    public void modalDragHandleHitTestUsesTopHandleUnlessWholeWindowIsRequested() throws Exception {
        GuiFactoryControllerResizable gui = allocateGuiWithRuntimeDefaults();
        set(gui, "activeSubGui", newActiveSubGui("modal"));
        set(gui, "guiLeft", Integer.valueOf(10));
        set(gui, "guiTop", Integer.valueOf(20));
        set(gui, "renderWidth", Integer.valueOf(120));
        set(gui, "renderHeight", Integer.valueOf(80));
        set(gui, "modalSubGuiDragHandle", Boolean.TRUE);
        set(gui, "modalSubGuiDragX", Integer.valueOf(0));
        set(gui, "modalSubGuiDragY", Integer.valueOf(0));
        set(gui, "modalSubGuiDragWidth", Integer.valueOf(0));
        set(gui, "modalSubGuiDragHeight", Integer.valueOf(0));

        assertEquals(Boolean.TRUE, invoke(gui, "isMouseInModalSubGuiDragHandle", new Class<?>[] {int.class, int.class}, Integer.valueOf(15), Integer.valueOf(25)));
        assertEquals(Boolean.FALSE, invoke(gui, "isMouseInModalSubGuiDragHandle", new Class<?>[] {int.class, int.class}, Integer.valueOf(15), Integer.valueOf(45)));

        set(gui, "modalSubGuiDragHandle", Boolean.FALSE);
        assertEquals(Boolean.TRUE, invoke(gui, "isMouseInModalSubGuiDragHandle", new Class<?>[] {int.class, int.class}, Integer.valueOf(15), Integer.valueOf(45)));

        set(gui, "activeSubGui", newActiveSubGui("replace"));
        assertEquals(Boolean.FALSE, invoke(gui, "isMouseInModalSubGuiDragHandle", new Class<?>[] {int.class, int.class}, Integer.valueOf(15), Integer.valueOf(25)));
    }

    @Test
    public void movingCurrentGuiAlsoMovesInteractiveControls() throws Exception {
        GuiFactoryControllerResizable gui = allocateGuiWithRuntimeDefaults();
        GuiButton defaultPrev = new GuiButton(1, 12, 22, 10, 10, "<");
        GuiButton customButton = new GuiButton(2, 30, 40, 20, 10, "Run");
        Object custom = newCustomButton(customButton);
        List<Object> customButtons = new ArrayList<Object>();
        customButtons.add(custom);

        set(gui, "guiLeft", Integer.valueOf(10));
        set(gui, "guiTop", Integer.valueOf(20));
        set(gui, "smartInterfacePrevButton", defaultPrev);
        set(gui, "customButtons", customButtons);

        invoke(gui, "moveCurrentGuiTo", new Class<?>[] {int.class, int.class}, Integer.valueOf(25), Integer.valueOf(27));

        assertEquals(Integer.valueOf(25), get(gui, "guiLeft"));
        assertEquals(Integer.valueOf(27), get(gui, "guiTop"));
        assertEquals(27, defaultPrev.x);
        assertEquals(29, defaultPrev.y);
        assertEquals(45, customButton.x);
        assertEquals(47, customButton.y);
    }

    private static GuiFactoryControllerResizable allocateGui() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        return (GuiFactoryControllerResizable) unsafe.allocateInstance(GuiFactoryControllerResizable.class);
    }

    private static GuiFactoryControllerResizable allocateGuiWithRuntimeDefaults() throws Exception {
        GuiFactoryControllerResizable gui = allocateGui();
        set(gui, "customButtons", new ArrayList<Object>());
        set(gui, "customSmartEditors", new ArrayList<Object>());
        set(gui, "customSliders", new ArrayList<Object>());
        set(gui, "backgroundTextureLayers", new ArrayList<Object>());
        set(gui, "foregroundTextureLayers", new ArrayList<Object>());
        set(gui, "layerRuntimeStates", new HashMap<Object, Object>());
        set(gui, "textureLayerIds", new java.util.HashSet<Object>());
        set(gui, "subGuiStyleIndex", new HashMap<Object, Object>());
        set(gui, "styleOverride", MachineGuiStyleManager.ControllerStyle.EMPTY);
        set(gui, "smartInterfaceVirtualInputCache", new HashMap<String, String>());
        set(gui, "panelScroll", new HashMap<String, Integer>());
        set(gui, "panelMaxScroll", new HashMap<String, Integer>());
        set(gui, "activePageId", "main");
        return gui;
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

    private static Object invoke(GuiFactoryControllerResizable target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = GuiFactoryControllerResizable.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Object newActiveSubGui(String mode) throws Exception {
        Class<?> runtimeStateClass = Class.forName("com.fushu.mmceguiext.client.gui.GuiFactoryControllerResizable$GuiRuntimeState");
        Constructor<?> runtimeStateConstructor = runtimeStateClass.getDeclaredConstructor();
        runtimeStateConstructor.setAccessible(true);
        Object runtimeState = runtimeStateConstructor.newInstance();
        Class<?> activeSubGuiClass = Class.forName("com.fushu.mmceguiext.client.gui.GuiFactoryControllerResizable$ActiveSubGui");
        Constructor<?> activeSubGuiConstructor = activeSubGuiClass.getDeclaredConstructor(String.class, String.class, runtimeStateClass);
        activeSubGuiConstructor.setAccessible(true);
        return activeSubGuiConstructor.newInstance("details", mode, runtimeState);
    }

    private static Object newCustomButton(GuiButton guiButton) throws Exception {
        Class<?> customButtonClass = Class.forName("com.fushu.mmceguiext.client.gui.GuiFactoryControllerResizable$CustomButton");
        Constructor<?> constructor = customButtonClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object customButton = constructor.newInstance();
        set(customButton, "button", guiButton);
        return customButton;
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
