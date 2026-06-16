package com.fushu.mmceguiext.client;

import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClientGuiEventHandlerTest {
    @Test
    public void detectsFactoryThreadLayoutOverridesFromStyleAndGlobalConfig() throws Exception {
        int oldThreadQueueX = MMCEGuiExtConfig.factoryController.threadQueueX;
        int oldThreadQueueY = MMCEGuiExtConfig.factoryController.threadQueueY;
        int oldThreadScrollbarX = MMCEGuiExtConfig.factoryController.threadScrollbarX;
        int oldThreadScrollbarY = MMCEGuiExtConfig.factoryController.threadScrollbarY;
        int oldQueueVisibleRows = MMCEGuiExtConfig.factoryController.queueVisibleRows;
        int oldThreadRowWidth = MMCEGuiExtConfig.factoryController.threadRowWidth;
        int oldThreadRowHeight = MMCEGuiExtConfig.factoryController.threadRowHeight;

        try {
            resetFactoryThreadLayoutDefaults();

            ClientGuiEventHandler handler = new ClientGuiEventHandler();
            MachineGuiStyleManager.ControllerStyle style = new MachineGuiStyleManager.ControllerStyle();

            assertFalse(hasCustomFactoryThreadLayout(handler, style));

            style.threadQueueX = Integer.valueOf(16);
            assertTrue(hasCustomFactoryThreadLayout(handler, style));

            style.threadQueueX = null;
            MMCEGuiExtConfig.factoryController.threadQueueY = 18;
            assertTrue(hasCustomFactoryThreadLayout(handler, style));
        } finally {
            MMCEGuiExtConfig.factoryController.threadQueueX = oldThreadQueueX;
            MMCEGuiExtConfig.factoryController.threadQueueY = oldThreadQueueY;
            MMCEGuiExtConfig.factoryController.threadScrollbarX = oldThreadScrollbarX;
            MMCEGuiExtConfig.factoryController.threadScrollbarY = oldThreadScrollbarY;
            MMCEGuiExtConfig.factoryController.queueVisibleRows = oldQueueVisibleRows;
            MMCEGuiExtConfig.factoryController.threadRowWidth = oldThreadRowWidth;
            MMCEGuiExtConfig.factoryController.threadRowHeight = oldThreadRowHeight;
        }
    }

    private static void resetFactoryThreadLayoutDefaults() {
        MMCEGuiExtConfig.factoryController.threadQueueX = 8;
        MMCEGuiExtConfig.factoryController.threadQueueY = 8;
        MMCEGuiExtConfig.factoryController.threadScrollbarX = 94;
        MMCEGuiExtConfig.factoryController.threadScrollbarY = 8;
        MMCEGuiExtConfig.factoryController.queueVisibleRows = 6;
        MMCEGuiExtConfig.factoryController.threadRowWidth = 86;
        MMCEGuiExtConfig.factoryController.threadRowHeight = 32;
    }

    private static boolean hasCustomFactoryThreadLayout(
        ClientGuiEventHandler handler,
        MachineGuiStyleManager.ControllerStyle style
    ) throws Exception {
        Method method = ClientGuiEventHandler.class.getDeclaredMethod(
            "hasCustomFactoryThreadLayout",
            MachineGuiStyleManager.ControllerStyle.class
        );
        method.setAccessible(true);
        return ((Boolean) method.invoke(handler, style)).booleanValue();
    }
}
